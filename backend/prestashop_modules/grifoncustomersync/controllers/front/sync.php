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

    public function initContent()
    {
        parent::initContent();
        $this->ajax = true;
        $this->handle();
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
        $skew = (int)Configuration::get('GRIFONCSYNC_TIME_SKEW_SEC');
        
        try {
            $this->requireAuth($secret, $skew, $raw);

            if (isset($payload['action']) && $payload['action'] === 'login') {
                $this->handleLogin($payload);
            } elseif (isset($payload['action']) && $payload['action'] === 'toggle_favorite_product') {
                $this->handleFavoriteToggle($payload);
            } elseif (isset($payload['action']) && $payload['action'] === 'record_recent_product') {
                $this->handleRecentProduct($payload);
            } elseif (isset($payload['action']) && $payload['action'] === 'list_favorite_products') {
                $this->handleListFavoriteProducts($payload);
            } elseif (isset($payload['action']) && $payload['action'] === 'list_recent_products') {
                $this->handleListRecentProducts($payload);
            } elseif (isset($payload['action']) && $payload['action'] === 'clear_activity_tables') {
                $this->handleClearActivityTables();
            } else {
                $this->handleSync($payload);
            }

        } catch (Exception $e) {
            $this->respond(500, ['ok' => false, 'error' => 'SERVER_ERROR', 'message' => $e->getMessage()]);
        }
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

        // 1. Δημιουργία/Ενημέρωση Πελάτη
        $idCustomer = $this->upsertCustomer($externalCustomerId, $customerData, $groups, $primaryAddress, $application, $result);
        $result['psCustomerId'] = (int)$idCustomer;

        // 2. Δημιουργία/Ενημέρωση Διευθύνσεων
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

        if (isset($customerData['newsletter'])) {
            $customer->newsletter = (int)$customerData['newsletter'];
        }
        if (isset($customerData['optin'])) {
            $customer->optin = (int)$customerData['optin'];
        }
        if (isset($customerData['company'])) {
            $customer->company = trim((string)$customerData['company']);
        }
        if (isset($customerData['siret'])) {
            $customer->siret = trim((string)$customerData['siret']);
        }

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

        // ΚΑΤΑΧΩΡΗΣΗ ΑΙΤΗΣΗΣ ΧΟΝΔΡΙΚΗΣ (Wholesale Application)
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

        // ΔΙΟΡΘΩΣΗ ΣΦΑΛΜΑΤΟΣ DNI: Ανάθεση μόνο αν η χώρα το απαιτεί
        $dniValue = trim((string)($addr['dni'] ?? $addr['vat_number'] ?? ''));
        if (!empty($dniValue)) {
            $country = new Country($id_country);
            $definition = Address::$definition['fields'];

            // Αν η χώρα απαιτεί DNI ή αν το πεδίο υπάρχει και δεν είμαστε στην Ελλάδα (όπου το core Address->dni συχνά λείπει/μπλοκάρει)
            if (isset($definition['dni']) && ($country->need_identification_number || $country->iso_code !== 'GR')) {
                $address->dni = $dniValue;
            }

            if (isset($definition['vat_number'])) {
                $address->vat_number = $dniValue;
            }
        }

        // Αποθήκευση χωρίς αυστηρό validation αν η κανονική αποθήκευση αποτύχει
        try {
            if (!$address->save()) {
                $address->save(false);
            }
        } catch (Exception $e) {
            // Last resort: προσπάθεια χωρίς DNI αν το σφάλμα αφορά το DNI
            if (strpos($e->getMessage(), 'dni') !== false) {
                unset($address->dni);
            }
            $address->save(false);
        }
    }

    private function upsertWholesaleApplication($idCustomer, array $customerData, array $primaryAddress, array $application)
    {
        if (!$this->isWholesaleApplicationRequested($customerData, $application)) {
            return false;
        }

        $tableName = $this->findWholesaleApplicationTable();
        if ($tableName === '') {
            return false;
        }

        $columns = $this->getTableColumns($tableName);
        if (empty($columns)) {
            return false;
        }

        $email = trim((string)($customerData['email'] ?? $application['email'] ?? ''));
        $now = date('Y-m-d H:i:s');
        $row = $this->buildWholesaleApplicationRow($columns, $idCustomer, $customerData, $primaryAddress, $application, $now);

        if (empty($row)) {
            return false;
        }

        $table = $this->stripDbPrefix($tableName);
        $where = $this->buildWholesaleApplicationWhere($columns, $idCustomer, $email);

        if ($where !== '') {
            $existing = (int)Db::getInstance()->getValue('SELECT 1 FROM `'.bqSQL($tableName).'` WHERE '.$where.' LIMIT 1');
            if ($existing > 0) {
                return (bool)Db::getInstance()->update($table, $row, $where);
            }
        }

        return (bool)Db::getInstance()->insert($table, $row);
    }

    private function isWholesaleApplicationRequested(array $customerData, array $application)
    {
        return (!empty($application['requested']) || !empty($application['is_wholesale']) || !empty($customerData['is_wholesale']));
    }

    private function findWholesaleApplicationTable()
    {
        $candidateTables = [
            _DB_PREFIX_.'wholesale_b2b_application',
            _DB_PREFIX_.'wholesaleb2b_application',
            _DB_PREFIX_.'ets_wholesale_b2b_application',
            _DB_PREFIX_.'ets_wholesaleb2b_application',
            _DB_PREFIX_.'wholesale_application',
        ];

        foreach ($candidateTables as $candidateTable) {
            if ($this->tableExists($candidateTable)) return $candidateTable;
        }
        return '';
    }

    private function tableExists($tableName) {
        return (bool)Db::getInstance()->getValue('SHOW TABLES LIKE \''.pSQL($tableName).'\'');
    }

    private function getTableColumns($tableName) {
        $rows = Db::getInstance()->executeS('SHOW COLUMNS FROM `'.bqSQL($tableName).'`');
        $columns = [];
        if (is_array($rows)) {
            foreach ($rows as $row) {
                $columns[Tools::strtolower($row['Field'])] = ['name' => $row['Field'], 'type' => $row['Type']];
            }
        }
        return $columns;
    }

    private function buildWholesaleApplicationRow($columns, $idCustomer, $customerData, $primaryAddress, $application, $now)
    {
        $vat = trim((string)($primaryAddress['vat_number'] ?? $application['vatNumber'] ?? $customerData['siret'] ?? ''));
        $statusCol = $this->getColumnName($columns, 'status');

        $fieldMap = [
            'id_customer' => (int)$idCustomer,
            'email' => trim((string)($customerData['email'] ?? $application['email'] ?? '')),
            'firstname' => trim((string)($customerData['firstname'] ?? $application['firstName'] ?? '')),
            'lastname' => trim((string)($customerData['lastname'] ?? $application['lastName'] ?? '')),
            'company' => trim((string)($customerData['company'] ?? $application['company'] ?? '')),
            'vat_number' => $vat,
            'siret' => $vat,
            'status' => 'pending', // ΕΔΩ ΟΡΙΖΕΤΑΙ ΓΙΑ APPROVAL
            'active' => 1,
            'date_add' => $now,
            'date_upd' => $now,
            'submitted_at' => $now,
        ];

        $row = [];
        foreach ($fieldMap as $key => $val) {
            if (isset($columns[$key])) $row[$columns[$key]['name']] = $val;
        }
        return $row;
    }

    private function getColumnName($columns, $key) {
        return isset($columns[Tools::strtolower($key)]) ? Tools::strtolower($key) : null;
    }

    private function buildWholesaleApplicationWhere($columns, $idCustomer, $email) {
        if (isset($columns['id_customer'])) return 'id_customer = '.(int)$idCustomer;
        if (isset($columns['email'])) return 'email = \''.pSQL($email).'\'';
        return '';
    }

    private function stripDbPrefix($tableName) {
        return strpos($tableName, _DB_PREFIX_) === 0 ? substr($tableName, strlen(_DB_PREFIX_)) : $tableName;
    }

    private function getCustomerIdByEmail($email) {
        return (int)Db::getInstance()->getValue('SELECT id_customer FROM '._DB_PREFIX_.'customer WHERE email = \''.pSQL($email).'\'');
    }

    private function upsertCustomerMap($externalCustomerId, $idCustomer, $email) {
        $id = (int)Db::getInstance()->getValue('SELECT id_grifon_customer_map FROM '._DB_PREFIX_.'grifon_customer_map WHERE external_customer_id = \''.pSQL($externalCustomerId).'\'');
        $data = ['external_customer_id' => pSQL($externalCustomerId), 'id_customer' => (int)$idCustomer, 'email' => pSQL($email), 'updated_at' => date('Y-m-d H:i:s')];
        if ($id > 0) Db::getInstance()->update('grifon_customer_map', $data, 'id_grifon_customer_map = '.$id);
        else { $data['created_at'] = date('Y-m-d H:i:s'); Db::getInstance()->insert('grifon_customer_map', $data); }
    }

    private function resolveCustomerGroups($groups) {
        $def = (int)Configuration::get('GRIFONCSYNC_DEFAULT_GROUP') ?: (int)Configuration::get('PS_CUSTOMER_GROUP');
        return ['default' => $def, 'list' => [$def]];
    }

    private function syncCustomerGroups($idCustomer, $groups) {
        Db::getInstance()->delete('customer_group', 'id_customer = '.(int)$idCustomer);
        foreach ($groups['list'] as $idG) Db::getInstance()->insert('customer_group', ['id_customer' => (int)$idCustomer, 'id_group' => (int)$idG]);
    }

    private function respond($code, $data) {
        http_response_code($code);
        header('Content-Type: application/json');
        echo json_encode($data);
        exit;
    }

    private function requireAuth($secret, $skew, $raw) {
        $headers = array_change_key_case(getallheaders(), CASE_LOWER);
        $ts = (int)($headers['x-grifon-timestamp'] ?? 0);
        $sig = $headers['x-grifon-signature'] ?? '';
        if (!$ts || !$sig || abs(time() - $ts) > $skew) throw new Exception('UNAUTHORIZED');
        if (!hash_equals(base64_encode(hash_hmac('sha256', $ts.$raw, $secret, true)), $sig)) throw new Exception('INVALID_SIGNATURE');
    }

    // Favorite/Recent Products Methods (Stubbed or kept if needed)
    private function handleFavoriteToggle($p) { $this->respond(200, ['ok' => true]); }
    private function handleRecentProduct($p) { $this->respond(200, ['ok' => true]); }
    private function handleListFavoriteProducts($p) { $this->respond(200, ['ok' => true, 'items' => []]); }
    private function handleListRecentProducts($p) { $this->respond(200, ['ok' => true, 'items' => []]); }
    private function handleClearActivityTables() { $this->respond(200, ['ok' => true]); }
}
