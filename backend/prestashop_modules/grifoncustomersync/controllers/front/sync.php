<?php
/**
 * POST /module/grifoncustomersync/sync
 *
 * Headers:
 *   X-Grifon-Timestamp: unix seconds
 *   X-Grifon-Signature: base64(HMAC_SHA256("<timestamp>\n<body>", secret))
 *
 * Password policy:
 *   - Prefer customer.password_hashed (bcrypt string like $2y$12$...)
 *   - Fallback to customer.password (plain) -> hashed inside PS
 */

use PrestaShop\PrestaShop\Adapter\ServiceLocator;
use PrestaShop\PrestaShop\Core\Crypto\Hashing;

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

        // Optional IP allowlist
        $allowed = Grifoncustomersync::getAllowedIps();
        if (!empty($allowed)) {
            $ip = Tools::getRemoteAddr();
            if (!in_array($ip, $allowed, true)) {
                $this->respond(403, ['ok' => false, 'error' => 'IP_NOT_ALLOWED']);
            }
        }

        $raw = (string)file_get_contents('php://input');
        $payload = json_decode($raw, true);
        if (!is_array($payload)) {
            $this->respond(400, ['ok' => false, 'error' => 'INVALID_JSON']);
        }

        // Auth
        $secret = (string)Configuration::get(Grifoncustomersync::CFG_SECRET);
        $skew = (int)Configuration::get(Grifoncustomersync::CFG_TIME_SKEW_SEC);
        $this->requireAuth($secret, $skew, $raw);

        $externalCustomerId = isset($payload['externalCustomerId']) ? trim((string)$payload['externalCustomerId']) : '';
        $customerData = (isset($payload['customer']) && is_array($payload['customer'])) ? $payload['customer'] : [];
        $addresses = (isset($payload['addresses']) && is_array($payload['addresses'])) ? $payload['addresses'] : [];
        $groups = (isset($payload['groups']) && is_array($payload['groups'])) ? $payload['groups'] : [];

        if ($externalCustomerId === '') {
            $this->respond(400, ['ok' => false, 'error' => 'MISSING_externalCustomerId']);
        }

        $email = isset($customerData['email']) ? trim((string)$customerData['email']) : '';
        $firstname = isset($customerData['firstname']) ? trim((string)$customerData['firstname']) : '';
        $lastname = isset($customerData['lastname']) ? trim((string)$customerData['lastname']) : '';

        if ($email === '' || !Validate::isEmail($email)) {
            $this->respond(400, ['ok' => false, 'error' => 'INVALID_email']);
        }
        if ($firstname === '' || $lastname === '') {
            $this->respond(400, ['ok' => false, 'error' => 'MISSING_firstname_or_lastname']);
        }

        $result = [
            'ok' => true,
            'created' => false,
            'updated' => false,
            'warnings' => [],
            'psCustomerId' => null,
            'psAddressIds' => [],
        ];

        try {
            $idCustomer = $this->upsertCustomer($externalCustomerId, $customerData, $groups, $result);
            $result['psCustomerId'] = (int)$idCustomer;

            foreach ($addresses as $addr) {
                if (!is_array($addr)) {
                    continue;
                }
                $idAddress = $this->upsertAddress($idCustomer, $addr, $result);
                if ($idAddress) {
                    $extAddrId = isset($addr['externalAddressId']) ? trim((string)$addr['externalAddressId']) : '';
                    if ($extAddrId !== '') {
                        $result['psAddressIds'][$extAddrId] = (int)$idAddress;
                    }
                }
            }

            $this->respond(200, $result);
        } catch (Exception $e) {
            $this->respond(500, ['ok' => false, 'error' => 'SERVER_ERROR', 'message' => $e->getMessage()]);
        }
    }

    private function requireAuth($secret, $maxSkew, $rawBody)
    {
        $headers = $this->getHeadersLower();
        $ts = isset($headers['x-grifon-timestamp']) ? (int)$headers['x-grifon-timestamp'] : 0;
        $sig = isset($headers['x-grifon-signature']) ? trim((string)$headers['x-grifon-signature']) : '';

        if ($ts <= 0 || $sig === '' || $secret === '') {
            $this->respond(401, ['ok' => false, 'error' => 'UNAUTHORIZED']);
        }

        $now = time();
        if (abs($now - $ts) > (int)$maxSkew) {
            $this->respond(401, ['ok' => false, 'error' => 'STALE_TIMESTAMP']);
        }

        $base = $ts . "\n" . $rawBody;
        $calc = base64_encode(hash_hmac('sha256', $base, $secret, true));

        if (!hash_equals($calc, $sig)) {
            $this->respond(401, ['ok' => false, 'error' => 'BAD_SIGNATURE']);
        }
    }

    private function getHeadersLower()
    {
        $out = [];
        $headers = function_exists('getallheaders') ? getallheaders() : [];
        if (!is_array($headers)) {
            $headers = [];
        }
        foreach ($headers as $k => $v) {
            $out[Tools::strtolower((string)$k)] = $v;
        }
        return $out;
    }

    private function isBcryptHash($value)
    {
        $value = (string)$value;
        if ($value === '') return false;

        // Αν υπάρχει validator στο PS, χρησιμοποίησέ το.
        if (method_exists('Validate', 'isHashedPassword')) {
            return (bool)Validate::isHashedPassword($value);
        }

        // Fallback: δέχεται $2y$ / $2b$ / $2a$ και 60 chars περίπου
        return (bool)preg_match('/^\$2[aby]\$\d{2}\$[\.\/A-Za-z0-9]{53}$/', $value);
    }

    private function upsertCustomer($externalCustomerId, $customerData, $groups, &$result)
    {
        $map = $this->getCustomerMapByExternal($externalCustomerId);
        $idCustomer = $map ? (int)$map['id_customer'] : 0;

        if (!$idCustomer) {
            $idCustomer = (int)$this->getCustomerIdByEmail((string)$customerData['email']);
        }

        $customer = null;
        $isCreate = false;

        if ($idCustomer) {
            $customer = new Customer($idCustomer);
            if (!Validate::isLoadedObject($customer)) {
                $idCustomer = 0;
            }
        }

        if (!$idCustomer) {
            $customer = new Customer();
            $isCreate = true;

            // Multishop safety
            if (property_exists($customer, 'id_shop')) {
                $customer->id_shop = (int)$this->context->shop->id;
            }
            if (property_exists($customer, 'id_shop_group')) {
                $customer->id_shop_group = (int)$this->context->shop->id_shop_group;
            }
        }

        $customer->email = trim((string)$customerData['email']);
        $customer->firstname = trim((string)$customerData['firstname']);
        $customer->lastname = trim((string)$customerData['lastname']);
        $customer->active = 1;
        $customer->is_guest = 0;

        // Optional B2B fields
        if (isset($customerData['company'])) { $customer->company = trim((string)$customerData['company']); }
        if (isset($customerData['website'])) { $customer->website = trim((string)$customerData['website']); }
        if (isset($customerData['siret'])) { $customer->siret = trim((string)$customerData['siret']); }
        if (isset($customerData['ape'])) { $customer->ape = trim((string)$customerData['ape']); }

        // ---- Password handling (Prefer hashed) ----
        $passHashed = isset($customerData['password_hashed']) ? trim((string)$customerData['password_hashed']) : '';
        $passPlain  = isset($customerData['password']) ? (string)$customerData['password'] : '';

        if ($isCreate) {
            // Στο create ΘΕΛΟΥΜΕ password_hashed (ή fallback password)
            if ($passHashed !== '') {
                if (!$this->isBcryptHash($passHashed)) {
                    throw new Exception('Invalid password_hashed (expected bcrypt $2y$...)');
                }
                $customer->passwd = $passHashed;
            } else {
                // fallback: plaintext -> hash μέσα στο PS
                if (Tools::strlen($passPlain) < 8) {
                    throw new Exception('Password missing or too short (min 8)');
                }
                /** @var Hashing $crypto */
                $crypto = ServiceLocator::get(Hashing::class);
                $customer->passwd = $crypto->hash($passPlain);
            }
        } else {
            // Update μόνο αν έρθει password_hashed ή password
            if ($passHashed !== '') {
                if (!$this->isBcryptHash($passHashed)) {
                    throw new Exception('Invalid password_hashed (expected bcrypt $2y$...)');
                }
                $customer->passwd = $passHashed;
            } elseif (Tools::strlen($passPlain) > 0) {
                if (Tools::strlen($passPlain) < 8) {
                    throw new Exception('Password too short (min 8)');
                }
                /** @var Hashing $crypto */
                $crypto = ServiceLocator::get(Hashing::class);
                $customer->passwd = $crypto->hash($passPlain);
            }
        }
        // ------------------------------------------

        // Groups
        $defaultGroup = isset($groups['default'])
            ? (int)$groups['default']
            : (int)Configuration::get(Grifoncustomersync::CFG_DEFAULT_GROUP);

        $list = (isset($groups['list']) && is_array($groups['list'])) ? $groups['list'] : [];

        $groupIds = [];
        if ($defaultGroup > 0) {
            $groupIds[] = (int)$defaultGroup;
            $customer->id_default_group = (int)$defaultGroup;
        }
        foreach ($list as $g) {
            $g = (int)$g;
            if ($g > 0) {
                $groupIds[] = $g;
            }
        }
        $groupIds = array_values(array_unique($groupIds));

        if ($isCreate) {
            if (!$customer->add()) {
                throw new Exception('Failed to create customer');
            }
            $result['created'] = true;
        } else {
            if (!$customer->update()) {
                throw new Exception('Failed to update customer');
            }
            $result['updated'] = true;
        }

        $idCustomer = (int)$customer->id;

        if (!empty($groupIds)) {
            $this->replaceCustomerGroups($idCustomer, $groupIds, $defaultGroup);
        }

        $this->upsertCustomerMap($externalCustomerId, $idCustomer, $customer->email);

        return $idCustomer;
    }

    private function upsertAddress($idCustomer, $addr, &$result)
    {
        $externalAddressId = isset($addr['externalAddressId']) ? trim((string)$addr['externalAddressId']) : '';
        if ($externalAddressId === '') {
            $result['warnings'][] = 'ADDRESS_SKIPPED_missing_externalAddressId';
            return 0;
        }

        $alias = isset($addr['alias']) ? trim((string)$addr['alias']) : '';
        $countryIso = isset($addr['countryIso']) ? Tools::strtoupper(trim((string)$addr['countryIso'])) : '';

        if ($alias === '' || $countryIso === '') {
            $result['warnings'][] = 'ADDRESS_SKIPPED_missing_alias_or_countryIso:' . $externalAddressId;
            return 0;
        }

        $idCountry = (int)Country::getByIso($countryIso);
        if ($idCountry <= 0) {
            $result['warnings'][] = 'ADDRESS_SKIPPED_unknown_countryIso:' . $countryIso;
            return 0;
        }

        if ((int)Country::getNeedZipCode($idCountry) === 1) {
            $pc = isset($addr['postcode']) ? trim((string)$addr['postcode']) : '';
            if ($pc === '') {
                $result['warnings'][] = 'ADDRESS_SKIPPED_missing_postcode_for_country:' . $externalAddressId;
                return 0;
            }
        }

        $map = $this->getAddressMapByExternal($externalAddressId);
        $idAddress = $map ? (int)$map['id_address'] : 0;

        $address = null;
        $isCreate = false;

        if ($idAddress) {
            $address = new Address($idAddress);
            if (!Validate::isLoadedObject($address) || (int)$address->id_customer !== (int)$idCustomer) {
                $idAddress = 0;
            }
        }

        if (!$idAddress) {
            $address = new Address();
            $isCreate = true;
        }

        $customer = new Customer((int)$idCustomer);

        $address->id_customer = (int)$idCustomer;
        $address->id_country = $idCountry;
        $address->alias = $alias;

        $address->firstname = (isset($addr['firstname']) && trim((string)$addr['firstname']) !== '')
            ? trim((string)$addr['firstname'])
            : (Validate::isLoadedObject($customer) ? $customer->firstname : 'N/A');

        $address->lastname = (isset($addr['lastname']) && trim((string)$addr['lastname']) !== '')
            ? trim((string)$addr['lastname'])
            : (Validate::isLoadedObject($customer) ? $customer->lastname : 'N/A');

        $address->address1 = isset($addr['address1']) ? trim((string)$addr['address1']) : '';
        $address->city = isset($addr['city']) ? trim((string)$addr['city']) : '';

        if ($address->address1 === '' || $address->city === '') {
            $result['warnings'][] = 'ADDRESS_SKIPPED_missing_address1_or_city:' . $externalAddressId;
            return 0;
        }

        if (isset($addr['address2'])) { $address->address2 = trim((string)$addr['address2']); }
        if (isset($addr['postcode'])) { $address->postcode = trim((string)$addr['postcode']); }
        if (isset($addr['company'])) { $address->company = trim((string)$addr['company']); }
        if (isset($addr['vat_number'])) { $address->vat_number = trim((string)$addr['vat_number']); }
        if (isset($addr['phone'])) { $address->phone = trim((string)$addr['phone']); }
        if (isset($addr['phone_mobile'])) { $address->phone_mobile = trim((string)$addr['phone_mobile']); }
        if (isset($addr['dni'])) { $address->dni = trim((string)$addr['dni']); }
        if (isset($addr['other'])) { $address->other = trim((string)$addr['other']); }

        if (isset($addr['stateIso']) && trim((string)$addr['stateIso']) !== '') {
            $stateIso = Tools::strtoupper(trim((string)$addr['stateIso']));
            $idState = (int)$this->getStateIdByIso($stateIso, $idCountry);
            if ($idState > 0) {
                $address->id_state = $idState;
            }
        }

        if ($isCreate) {
            if (!$address->add()) {
                $result['warnings'][] = 'ADDRESS_FAILED_create:' . $externalAddressId;
                return 0;
            }
        } else {
            if (!$address->update()) {
                $result['warnings'][] = 'ADDRESS_FAILED_update:' . $externalAddressId;
                return 0;
            }
        }

        $idAddress = (int)$address->id;
        $this->upsertAddressMap($externalAddressId, $idAddress, (int)$idCustomer, $alias);

        return $idAddress;
    }

    private function getStateIdByIso($stateIso, $idCountry)
    {
        $sql = 'SELECT `id_state` FROM `'._DB_PREFIX_.'state`
                WHERE `iso_code` = "'.pSQL($stateIso).'"
                AND `id_country` = '.(int)$idCountry.'
                LIMIT 1';
        return (int)Db::getInstance()->getValue($sql);
    }

    private function getCustomerIdByEmail($email)
    {
        $sql = 'SELECT `id_customer` FROM `'._DB_PREFIX_.'customer`
                WHERE `email` = "'.pSQL($email).'"
                LIMIT 1';
        return (int)Db::getInstance()->getValue($sql);
    }

    private function getCustomerMapByExternal($externalCustomerId)
    {
        $sql = 'SELECT * FROM `'._DB_PREFIX_.'grifon_customer_map`
                WHERE `external_customer_id` = "'.pSQL($externalCustomerId).'"
                LIMIT 1';
        return Db::getInstance()->getRow($sql);
    }

    private function upsertCustomerMap($externalCustomerId, $idCustomer, $email)
    {
        $now = date('Y-m-d H:i:s');
        $row = $this->getCustomerMapByExternal($externalCustomerId);

        if ($row) {
            Db::getInstance()->update('grifon_customer_map', [
                'id_customer' => (int)$idCustomer,
                'email' => pSQL($email),
                'date_upd' => pSQL($now),
            ], 'external_customer_id = "'.pSQL($externalCustomerId).'"');
        } else {
            Db::getInstance()->insert('grifon_customer_map', [
                'external_customer_id' => pSQL($externalCustomerId),
                'id_customer' => (int)$idCustomer,
                'email' => pSQL($email),
                'date_add' => pSQL($now),
                'date_upd' => pSQL($now),
            ]);
        }
    }

    private function getAddressMapByExternal($externalAddressId)
    {
        $sql = 'SELECT * FROM `'._DB_PREFIX_.'grifon_address_map`
                WHERE `external_address_id` = "'.pSQL($externalAddressId).'"
                LIMIT 1';
        return Db::getInstance()->getRow($sql);
    }

    private function upsertAddressMap($externalAddressId, $idAddress, $idCustomer, $alias)
    {
        $now = date('Y-m-d H:i:s');
        $row = $this->getAddressMapByExternal($externalAddressId);

        if ($row) {
            Db::getInstance()->update('grifon_address_map', [
                'id_address' => (int)$idAddress,
                'id_customer' => (int)$idCustomer,
                'alias' => pSQL($alias),
                'date_upd' => pSQL($now),
            ], 'external_address_id = "'.pSQL($externalAddressId).'"');
        } else {
            Db::getInstance()->insert('grifon_address_map', [
                'external_address_id' => pSQL($externalAddressId),
                'id_address' => (int)$idAddress,
                'id_customer' => (int)$idCustomer,
                'alias' => pSQL($alias),
                'date_add' => pSQL($now),
                'date_upd' => pSQL($now),
            ]);
        }
    }

    private function replaceCustomerGroups($idCustomer, $groupIds, $defaultGroup)
    {
        $idCustomer = (int)$idCustomer;
        if ($idCustomer <= 0) {
            return;
        }

        if ((int)$defaultGroup > 0 && !in_array((int)$defaultGroup, $groupIds, true)) {
            $groupIds[] = (int)$defaultGroup;
        }
        $groupIds = array_values(array_unique(array_map('intval', $groupIds)));

        Db::getInstance()->delete('customer_group', 'id_customer = '.$idCustomer);

        foreach ($groupIds as $idGroup) {
            $idGroup = (int)$idGroup;
            if ($idGroup <= 0) { continue; }

            Db::getInstance()->execute(
                'INSERT IGNORE INTO `'._DB_PREFIX_.'customer_group` (`id_customer`, `id_group`)
                 VALUES ('.$idCustomer.', '.$idGroup.')'
            );
        }

        if ((int)$defaultGroup > 0) {
            Db::getInstance()->update('customer', ['id_default_group' => (int)$defaultGroup], 'id_customer = '.$idCustomer);
        }
    }

    private function respond($statusCode, $data)
    {
        http_response_code((int)$statusCode);
        echo json_encode($data);
        exit;
    }
}
