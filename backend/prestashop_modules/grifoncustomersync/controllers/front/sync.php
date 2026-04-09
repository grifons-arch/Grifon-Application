<?php
/**
 * Grifon Customer Sync & Auth Controller - Full Data Support
 */

if (!defined('_PS_VERSION_')) {
    exit;
}

class GrifoncustomersyncSyncModuleFrontController extends ModuleFrontController
{
    public $ssl = true;
    public $display_header = false;
    public $display_footer = false;

    /**
     * Παράκαμψη Maintenance Mode για το API
     */
    public function checkAccess()
    {
        return true;
    }

    public function displayMaintenancePage()
    {
        return;
    }

    public function postProcess()
    {
        $this->handle();
        exit;
    }

    private function handle()
    {
        header('Content-Type: application/json; charset=utf-8');

        if (Tools::strtoupper($_SERVER['REQUEST_METHOD']) !== 'POST') {
            $this->respond(405, ['ok' => false, 'error' => 'METHOD_NOT_ALLOWED']);
        }

        $raw = (string)file_get_contents('php://input');
        $payload = json_decode($raw, true);
        if (!is_array($payload)) {
            $this->respond(400, ['ok' => false, 'error' => 'INVALID_JSON']);
        }

        $secret = (string)Configuration::get('GRIFONCSYNC_SECRET');
        $skew = (int)Configuration::get('GRIFONCSYNC_TIME_SKEW_SEC') ?: 3600;
        
        try {
            $this->requireAuth($secret, $skew, $raw);

            $action = isset($payload['action']) ? $payload['action'] : '';

            switch ($action) {
                case 'login':
                    $this->handleLogin($payload);
                    break;
                case 'list_wholesale_applications':
                    $this->handleListWholesaleApplications();
                    break;
                case 'toggle_favorite_product':
                    $this->handleFavoriteToggle($payload);
                    break;
                case 'record_recent_product':
                    $this->handleRecentProduct($payload);
                    break;
                case 'list_favorite_products':
                    $this->handleListFavoriteProducts($payload);
                    break;
                case 'list_recent_products':
                    $this->handleListRecentProducts($payload);
                    break;
                case 'clear_activity_tables':
                    $this->handleClearActivityTables();
                    break;
                default:
                    $this->handleSync($payload);
                    break;
            }

        } catch (Throwable $e) {
            $this->respond(500, ['ok' => false, 'error' => 'SERVER_ERROR', 'message' => $e->getMessage()]);
        }
    }

    private function handleListWholesaleApplications()
    {
        $tableName = $this->findWholesaleApplicationTable();
        if ($tableName === '') {
            $this->respond(200, ['ok' => true, 'table' => null, 'items' => [], 'message' => 'No wholesale table found.']);
        }

        $sql = 'SELECT * FROM `'.bqSQL($tableName).'` ORDER BY 1 DESC LIMIT 50';
        $rows = Db::getInstance()->executeS($sql);

        $this->respond(200, [
            'ok' => true,
            'table' => $tableName,
            'items' => is_array($rows) ? $rows : []
        ]);
    }

    private function handleLogin($payload)
    {
        $email = isset($payload['email']) ? trim((string)$payload['email']) : '';
        $password = isset($payload['password']) ? (string)$payload['password'] : '';

        if (empty($email) || empty($password)) {
            $this->respond(400, ['ok' => false, 'error' => 'MISSING_CREDENTIALS']);
        }

        $customer = new Customer();
        $customer->getByEmail($email);

        if (!Validate::isLoadedObject($customer)) {
            $this->respond(401, ['ok' => false, 'error' => 'USER_NOT_FOUND']);
        }

        $crypto = PrestaShop\PrestaShop\Adapter\ServiceLocator::get(PrestaShop\PrestaShop\Core\Crypto\Hashing::class);
        if (!$crypto->checkHash($password, $customer->passwd)) {
            $this->respond(401, ['ok' => false, 'error' => 'INVALID_PASSWORD']);
        }

        if (!$customer->active) {
            $this->respond(403, ['ok' => false, 'error' => 'ACCOUNT_INACTIVE']);
        }

        $this->respond(200, [
            'ok' => true,
            'id_customer' => (int)$customer->id,
            'firstname' => $customer->firstname,
            'lastname' => $customer->lastname,
            'email' => $customer->email,
            'company' => $customer->company,
            'newsletter' => (int)$customer->newsletter,
            'optin' => (int)$customer->optin
        ]);
    }

    private function handleSync($payload)
    {
        $externalCustomerId = isset($payload['externalCustomerId']) ? trim((string)$payload['externalCustomerId']) : '';
        $customerData = (isset($payload['customer']) && is_array($payload['customer'])) ? $payload['customer'] : [];
        $addresses = (isset($payload['addresses']) && is_array($payload['addresses'])) ? $payload['addresses'] : [];
        $groups = (isset($payload['groups']) && is_array($payload['groups'])) ? $payload['groups'] : [];
        $application = (isset($payload['application']) && is_array($payload['application'])) ? $payload['application'] : [];

        $result = [
            'ok' => true,
            'created' => false,
            'updated' => false,
            'psCustomerId' => null,
            'wholesaleApplicationRegistered' => false,
        ];

        $primaryAddress = [];
        if (!empty($addresses) && isset($addresses[0]) && is_array($addresses[0])) {
            $primaryAddress = $addresses[0];
        }

        $idCustomer = $this->upsertCustomer($externalCustomerId, $customerData, $groups, $primaryAddress, $application, $result);
        $result['psCustomerId'] = (int)$idCustomer;

        foreach ($addresses as $addr) {
            $this->upsertAddress($idCustomer, $addr, $result);
        }

        $this->respond(200, $result);
    }

    private function upsertCustomer($externalCustomerId, $customerData, $groups, $primaryAddress, $application, &$result)
    {
        $email = trim((string)$customerData['email']);
        $idCustomer = (int)$this->getCustomerIdByEmail($email);
        $resolvedGroups = $this->resolveCustomerGroups($groups);
        
        $customer = $idCustomer ? new Customer($idCustomer) : new Customer();
        $customer->email = $email;
        $customer->firstname = trim((string)$customerData['firstname']);
        $customer->lastname = trim((string)$customerData['lastname']);
        $customer->active = 1;
        $customer->is_guest = 0;
        $customer->id_default_group = (int)$resolvedGroups['default'];

        if (isset($customerData['newsletter'])) $customer->newsletter = (int)$customerData['newsletter'];
        if (isset($customerData['optin'])) $customer->optin = (int)$customerData['optin'];
        if (isset($customerData['company'])) $customer->company = trim((string)$customerData['company']);
        if (isset($customerData['siret'])) $customer->siret = trim((string)$customerData['siret']);

        if (!$idCustomer) {
            $customer->id_shop = (int)Context::getContext()->shop->id;
            $customer->id_shop_group = (int)Context::getContext()->shop->id_shop_group;
        }

        if (isset($customerData['password']) && !empty($customerData['password'])) {
            $customer->passwd = Tools::hash($customerData['password']);
        }

        if (!$customer->id) {
            $customer->add();
            $result['created'] = true;
        } else {
            $customer->update();
            $result['updated'] = true;
        }

        $this->syncCustomerGroups((int)$customer->id, $resolvedGroups);
        $this->upsertCustomerMap($externalCustomerId, $customer->id, $customer->email);

        try {
            $result['wholesaleApplicationRegistered'] = $this->upsertWholesaleApplication(
                (int)$customer->id,
                is_array($customerData) ? $customerData : [],
                is_array($primaryAddress) ? $primaryAddress : [],
                is_array($application) ? $application : []
            );
        } catch (Exception $e) {
            $result['wholesaleApplicationError'] = $e->getMessage();
        }

        return $customer->id;
    }

    private function upsertAddress($idCustomer, $addr, &$result)
    {
        $address = new Address();
        $address->id_customer = (int)$idCustomer;
        $address->firstname = trim((string)($addr['firstname'] ?? 'N/A'));
        $address->lastname = trim((string)($addr['lastname'] ?? 'N/A'));
        $address->address1 = trim((string)($addr['address1'] ?? 'Default Street'));
        $address->city = trim((string)($addr['city'] ?? 'Default City'));
        $address->postcode = trim((string)($addr['postcode'] ?? '00000'));

        $id_country = (int)Country::getByIso($addr['countryIso'] ?? 'GR');
        if ($id_country <= 0) $id_country = (int)Configuration::get('PS_COUNTRY_DEFAULT');
        $address->id_country = $id_country;
        $address->alias = trim((string)($addr['alias'] ?? 'Default'));

        $dniValue = trim((string)($addr['dni'] ?? $addr['vat_number'] ?? ''));
        if (!empty($dniValue)) {
            $country = new Country($id_country);
            $definition = Address::$definition['fields'];
            if (isset($definition['dni']) && ($country->need_identification_number || $country->iso_code !== 'GR')) {
                $address->dni = $dniValue;
            }
            if (isset($definition['vat_number'])) {
                $address->vat_number = $dniValue;
            }
        }

        if (!$address->save()) {
            $address->save(false);
        }
    }

    private function upsertWholesaleApplication($idCustomer, array $customerData, array $primaryAddress, array $application)
    {
        if (!$this->isWholesaleApplicationRequested($customerData, $application)) return false;
        $tableName = $this->findWholesaleApplicationTable();
        if ($tableName === '') return false;
        $columns = $this->getTableColumns($tableName);
        if (empty($columns)) return false;

        $email = trim((string)($customerData['email'] ?? $application['email'] ?? ''));
        $row = $this->filterRowByColumns(
            $columns,
            $this->buildWholesaleApplicationRow($columns, $idCustomer, $customerData, $primaryAddress, $application)
        );
        if (empty($row)) return false;

        $table = $this->stripDbPrefix($tableName);
        $where = $this->buildWholesaleApplicationWhere($columns, $idCustomer, $email);

        if ($where !== '' && (int)Db::getInstance()->getValue('SELECT 1 FROM `'.bqSQL($tableName).'` WHERE '.$where) > 0) {
            return $this->writeTableRow('update', $table, $row, $where);
        }
        return $this->writeTableRow('insert', $table, $row);
    }

    private function isWholesaleApplicationRequested($customerData, $application)
    {
        return (!empty($application['requested']) || !empty($application['is_wholesale']) || !empty($customerData['is_wholesale']));
    }

    private function findWholesaleApplicationTable()
    {
        $configuredTable = $this->normalizeConfiguredTableName((string)Configuration::get('GRIFONCSYNC_WHOLESALE_APPLICATION_TABLE'));
        if ($configuredTable !== '' && $this->tableExists($configuredTable)) {
            return $configuredTable;
        }

        $candidates = [
            _DB_PREFIX_.'wholesale_b2b_application',
            _DB_PREFIX_.'wholesaleb2b_application',
            _DB_PREFIX_.'ets_wholesale_b2b_application',
            _DB_PREFIX_.'ets_wholesaleb2b_application',
            _DB_PREFIX_.'wholesale_application',
            _DB_PREFIX_.'wholesale_b2b_applications',
            _DB_PREFIX_.'wholesaleb2b_applications',
            _DB_PREFIX_.'ets_wholesale_b2b_applications',
            _DB_PREFIX_.'ets_wholesaleb2b_applications',
            _DB_PREFIX_.'wholesale_request',
            _DB_PREFIX_.'wholesale_requests',
            _DB_PREFIX_.'b2b_application',
            _DB_PREFIX_.'b2b_applications',
        ];
        foreach ($candidates as $candidate) {
            if ($this->tableExists($candidate)) return $candidate;
        }

        return $this->findWholesaleApplicationTableByScan();
    }

    private function buildWholesaleApplicationRow(array $columns, $idCustomer, array $customerData, array $primaryAddress, array $application)
    {
        $email = trim((string)($customerData['email'] ?? $application['email'] ?? ''));
        $now = date('Y-m-d H:i:s');
        $vat = trim((string)($primaryAddress['vat_number'] ?? $application['vatNumber'] ?? $customerData['siret'] ?? ''));
        $phone = trim((string)($primaryAddress['phone'] ?? $application['phone'] ?? ''));
        $address = trim((string)($primaryAddress['address1'] ?? $application['street'] ?? ''));
        $city = trim((string)($primaryAddress['city'] ?? $application['city'] ?? ''));
        $postcode = trim((string)($primaryAddress['postcode'] ?? $application['postalCode'] ?? ''));
        $countryIso = trim((string)($primaryAddress['countryIso'] ?? $application['countryIso'] ?? ''));
        $country = trim((string)($application['country'] ?? $countryIso));
        $source = trim((string)($application['source'] ?? 'grifoncustomersync'));
        $contactFullName = trim((string)($application['contactPersonFullName'] ?? $application['fullName'] ?? ''));
        $addressCoordinates = trim((string)($application['addressCoordinates'] ?? ''));
        $companyRegistration = trim((string)($application['companyRegistrationFileName'] ?? ''));
        $invoice = trim((string)($application['invoiceFileName'] ?? ''));

        $row = [];
        $this->applyColumnAliases($columns, $row, ['id_customer', 'customer_id'], (int)$idCustomer);
        $this->applyColumnAliases($columns, $row, ['email', 'customer_email'], $email);
        $this->applyColumnAliases($columns, $row, ['firstname', 'first_name'], trim((string)($customerData['firstname'] ?? $application['firstName'] ?? '')));
        $this->applyColumnAliases($columns, $row, ['lastname', 'last_name'], trim((string)($customerData['lastname'] ?? $application['lastName'] ?? '')));
        $this->applyColumnAliases($columns, $row, ['company', 'company_name'], trim((string)($customerData['company'] ?? $application['company'] ?? '')));
        $this->applyColumnAliases($columns, $row, ['vat_number', 'vat', 'tax_number', 'siret'], $vat);
        $this->applyColumnAliases($columns, $row, ['phone', 'phone_number', 'telephone'], $phone);
        $this->applyColumnAliases($columns, $row, ['address', 'address1', 'street'], $address);
        $this->applyColumnAliases($columns, $row, ['city'], $city);
        $this->applyColumnAliases($columns, $row, ['postcode', 'postal_code', 'zipcode', 'zip_code'], $postcode);
        $this->applyColumnAliases($columns, $row, ['country', 'country_name'], $country);
        $this->applyColumnAliases($columns, $row, ['country_iso', 'country_code'], $countryIso);
        $this->applyColumnAliases($columns, $row, ['full_name', 'contact_person_full_name', 'contact_name', 'contact_person'], $contactFullName);
        $this->applyColumnAliases($columns, $row, ['address_coordinates', 'coordinates', 'geo_coordinates', 'map_coordinates'], $addressCoordinates);
        $this->applyColumnAliases($columns, $row, ['company_registration', 'company_registration_file', 'company_registration_document', 'registration_document', 'business_registration'], $companyRegistration);
        $this->applyColumnAliases($columns, $row, ['invoice', 'invoice_file', 'invoice_document', 'invoice_reference'], $invoice);
        $this->applyColumnAliases($columns, $row, ['status', 'account_status', 'wholesale_customer_account_status'], 'pending');
        $this->applyColumnAliases($columns, $row, ['active'], 1);
        $this->applyColumnAliases($columns, $row, ['source'], $source);
        $this->applyColumnAliases($columns, $row, ['date_add', 'created_at', 'date_created'], $now);
        $this->applyColumnAliases($columns, $row, ['date_upd', 'updated_at', 'date_updated'], $now);
        $this->applyColumnAliases($columns, $row, ['submitted_at', 'requested_at', 'applied_at'], $now);

        return $row;
    }

    private function applyColumnAliases(array $columns, array &$row, array $aliases, $value)
    {
        foreach ($aliases as $alias) {
            $normalized = Tools::strtolower($alias);
            if (!isset($columns[$normalized])) {
                continue;
            }
            $row[$columns[$normalized]['name']] = $value;
            return;
        }
    }

    private function findWholesaleApplicationTableByScan()
    {
        $rows = Db::getInstance()->executeS('SHOW TABLES');
        if (!is_array($rows)) {
            return '';
        }

        $bestTable = '';
        $bestScore = 0;

        foreach ($rows as $row) {
            foreach ($row as $value) {
                if (!is_string($value) || $value === '') {
                    continue;
                }

                $tableName = trim($value);
                $score = $this->scoreWholesaleApplicationTableName($tableName);
                if ($score <= 0) {
                    continue;
                }

                $columns = $this->getTableColumns($tableName);
                if (empty($columns)) {
                    continue;
                }

                $score += $this->scoreWholesaleApplicationColumns($columns);
                if ($score > $bestScore) {
                    $bestScore = $score;
                    $bestTable = $tableName;
                }
            }
        }

        return $bestScore >= 7 ? $bestTable : '';
    }

    private function scoreWholesaleApplicationTableName($tableName)
    {
        $name = Tools::strtolower($tableName);
        $score = 0;

        if (strpos($name, 'wholesale') !== false) $score += 4;
        if (strpos($name, 'b2b') !== false) $score += 3;
        if (strpos($name, 'application') !== false || strpos($name, 'applications') !== false) $score += 4;
        if (strpos($name, 'request') !== false || strpos($name, 'requests') !== false) $score += 2;
        if (strpos($name, 'approval') !== false) $score += 2;
        if (strpos($name, 'customer') !== false) $score += 1;

        return $score;
    }

    private function scoreWholesaleApplicationColumns(array $columns)
    {
        $score = 0;

        if (isset($columns['status']) || isset($columns['account_status']) || isset($columns['wholesale_customer_account_status'])) $score += 4;
        if (isset($columns['id_customer']) || isset($columns['customer_id'])) $score += 2;
        if (isset($columns['email']) || isset($columns['customer_email'])) $score += 2;
        if (isset($columns['company']) || isset($columns['company_name'])) $score += 1;
        if (isset($columns['date_add']) || isset($columns['created_at']) || isset($columns['submitted_at'])) $score += 1;

        return $score;
    }

    private function normalizeConfiguredTableName($tableName)
    {
        $tableName = trim(str_replace('`', '', (string)$tableName));
        if ($tableName === '') {
            return '';
        }

        if (strpos($tableName, _DB_PREFIX_) === 0) {
            return $tableName;
        }

        return _DB_PREFIX_.$tableName;
    }

    private function tableExists($tableName)
    {
        $rows = Db::getInstance()->executeS('SHOW TABLES LIKE "'.pSQL($tableName).'"');
        return is_array($rows) && !empty($rows);
    }

    private function getTableColumns($tableName) {
        $rows = Db::getInstance()->executeS('SHOW COLUMNS FROM `'.bqSQL($tableName).'`');
        $res = [];
        if (is_array($rows)) foreach ($rows as $r) $res[strtolower($r['Field'])] = ['name' => $r['Field']];
        return $res;
    }

    private function filterRowByColumns(array $columns, array $row)
    {
        if (empty($columns) || empty($row)) {
            return [];
        }

        $allowedNames = [];
        foreach ($columns as $column) {
            if (!isset($column['name'])) {
                continue;
            }
            $allowedNames[$column['name']] = true;
        }

        $filtered = [];
        foreach ($row as $key => $value) {
            if (isset($allowedNames[$key])) {
                $filtered[$key] = $value;
            }
        }

        return $filtered;
    }

    private function writeTableRow($operation, $table, array $row, $where = '')
    {
        $attempt = $row;

        while (!empty($attempt)) {
            try {
                if ($operation === 'update') {
                    return (bool)Db::getInstance()->update($table, $attempt, $where);
                }

                return (bool)Db::getInstance()->insert($table, $attempt);
            } catch (Throwable $e) {
                $unknownColumn = $this->extractUnknownColumnFromException($e);
                if ($unknownColumn === '' || !array_key_exists($unknownColumn, $attempt)) {
                    throw $e;
                }

                unset($attempt[$unknownColumn]);
            }
        }

        return false;
    }

    private function extractUnknownColumnFromException(Throwable $e)
    {
        $message = $e->getMessage();
        if (preg_match("/Unknown column '([^']+)' in '(?:SET|field list)'/", $message, $matches)) {
            return $matches[1];
        }

        return '';
    }

    private function buildWholesaleApplicationWhere($columns, $idCustomer, $email) {
        if (isset($columns['id_customer'])) return 'id_customer = '.(int)$idCustomer;
        if (isset($columns['customer_id'])) return 'customer_id = '.(int)$idCustomer;
        if (isset($columns['email'])) return 'email = "'.pSQL($email).'"';
        if (isset($columns['customer_email'])) return 'customer_email = "'.pSQL($email).'"';
        return '';
    }

    private function stripDbPrefix($t) { return strpos($t, _DB_PREFIX_) === 0 ? substr($t, strlen(_DB_PREFIX_)) : $t; }
    private function getCustomerIdByEmail($e) { return (int)Db::getInstance()->getValue('SELECT id_customer FROM '._DB_PREFIX_.'customer WHERE email = "'.pSQL($e).'"'); }

    private function upsertCustomerMap($ext, $id, $email) {
        $exists = (int)Db::getInstance()->getValue('SELECT id_grifon_customer_map FROM '._DB_PREFIX_.'grifon_customer_map WHERE external_customer_id = "'.pSQL($ext).'"');
        $columns = $this->getTableColumns(_DB_PREFIX_.'grifon_customer_map');
        if (empty($columns)) {
            return;
        }
        $now = date('Y-m-d H:i:s');
        $data = [
            'external_customer_id' => pSQL($ext),
            'id_customer' => (int)$id,
            'email' => pSQL($email),
        ];

        if (isset($columns['date_upd'])) {
            $data['date_upd'] = $now;
        } elseif (isset($columns['updated_at'])) {
            $data['updated_at'] = $now;
        }

        $data = $this->filterRowByColumns($columns, $data);

        if ($exists) {
            $this->writeTableRow('update', 'grifon_customer_map', $data, 'id_grifon_customer_map = '.$exists);
            return;
        }

        if (isset($columns['date_add'])) {
            $data['date_add'] = $now;
        } elseif (isset($columns['created_at'])) {
            $data['created_at'] = $now;
        }

        $data = $this->filterRowByColumns($columns, $data);

        $this->writeTableRow('insert', 'grifon_customer_map', $data);
    }

    private function resolveCustomerGroups($g) {
        $d = (int)Configuration::get('GRIFONCSYNC_DEFAULT_GROUP') ?: (int)Configuration::get('PS_CUSTOMER_GROUP');
        return ['default' => $d, 'list' => [$d]];
    }

    private function syncCustomerGroups($id, $g) {
        Db::getInstance()->delete('customer_group', 'id_customer = '.(int)$id);
        foreach ($g['list'] as $idG) Db::getInstance()->insert('customer_group', ['id_customer' => (int)$id, 'id_group' => (int)$idG]);
    }

    private function respond($code, $data) {
        http_response_code($code);
        echo json_encode($data);
        exit;
    }

    private function requireAuth($secret, $skew, $raw) {
        $headers = [];
        foreach ($_SERVER as $key => $value) {
            if (strpos($key, 'HTTP_X_GRIFON_') === 0) {
                $headers[strtolower(str_replace('_', '-', substr($key, 5)))] = $value;
            }
        }
        if (empty($headers) && function_exists('getallheaders')) {
            $all = array_change_key_case(getallheaders(), CASE_LOWER);
            if (isset($all['x-grifon-timestamp'])) $headers['x-grifon-timestamp'] = $all['x-grifon-timestamp'];
            if (isset($all['x-grifon-signature'])) $headers['x-grifon-signature'] = $all['x-grifon-signature'];
        }

        $ts = (int)($headers['x-grifon-timestamp'] ?? 0);
        $sig = $headers['x-grifon-signature'] ?? '';
        if (!$ts || !$sig || abs(time() - $ts) > $skew) throw new Exception('UNAUTHORIZED');
        if (!hash_equals(base64_encode(hash_hmac('sha256', $ts.$raw, $secret, true)), $sig)) throw new Exception('INVALID_SIGNATURE');
    }

    private function handleFavoriteToggle($p) { $this->respond(200, ['ok' => true]); }
    private function handleRecentProduct($p) { $this->respond(200, ['ok' => true]); }
    private function handleListFavoriteProducts($p) { $this->respond(200, ['ok' => true, 'items' => []]); }
    private function handleListRecentProducts($p) { $this->respond(200, ['ok' => true, 'items' => []]); }
    private function handleClearActivityTables() { $this->respond(200, ['ok' => true]); }
}
