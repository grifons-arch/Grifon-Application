<?php
/**
 * POST /module/grifoncustomersync/sync
 *
 * Headers:
 *   X-Grifon-Timestamp: unix seconds
 *   X-Grifon-Signature: base64(HMAC_SHA256("<timestamp>\n<body>", secret))
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

        $raw = (string)file_get_contents('php://input');
        $payload = json_decode($raw, true);
        if (!is_array($payload)) {
            $this->respond(400, ['ok' => false, 'error' => 'INVALID_JSON']);
        }

        // Auth
        $secret = (string)Configuration::get(Grifoncustomersync::CFG_SECRET);
        $skew = (int)Configuration::get(Grifoncustomersync::CFG_TIME_SKEW_SEC);
        $this->requireAuth($secret, $skew, $raw);

        $action = isset($payload['action']) ? trim((string)$payload['action']) : 'sync';

        if ($action === 'login') {
            $this->handleLogin($payload);
        } else {
            // Default sync/register logic
            $this->handleSync($payload);
        }
    }

    private function handleLogin($payload)
    {
        $email = isset($payload['email']) ? trim((string)$payload['email']) : '';
        $password = isset($payload['password']) ? (string)$payload['password'] : '';

        if ($email === '' || $password === '') {
            $this->respond(400, ['status' => 'ERROR', 'message' => 'Email and password required']);
        }

        $idCustomer = (int)$this->getCustomerIdByEmail($email);
        if (!$idCustomer) {
            $this->respond(401, ['status' => 'ERROR', 'message' => 'User not found']);
        }

        $customer = new Customer($idCustomer);
        /** @var Hashing $crypto */
        $crypto = ServiceLocator::get(Hashing::class);

        if (!$crypto->checkHash($password, $customer->passwd)) {
            $this->respond(401, ['status' => 'ERROR', 'message' => 'Invalid password']);
        }

        if (!$customer->active) {
            $this->respond(403, ['status' => 'ERROR', 'message' => 'Account is disabled']);
        }

        $this->respond(200, [
            'status' => 'SUCCESS',
            'customerId' => (string)$customer->id,
            'firstName' => $customer->firstname,
            'lastName' => $customer->lastname,
            'token' => bin2hex(random_bytes(32))
        ]);
    }

    private function handleSync($payload)
    {
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

        $result = [
            'ok' => true,
            'status' => 'SUCCESS',
            'created' => false,
            'updated' => false,
            'warnings' => [],
            'psCustomerId' => null,
            'id_customer' => null,
            'psAddressIds' => [],
        ];

        try {
            $idCustomer = $this->upsertCustomer($externalCustomerId, $customerData, $groups, $result);
            $result['psCustomerId'] = (int)$idCustomer;
            $result['id_customer'] = (int)$idCustomer;

            foreach ($addresses as $addr) {
                if (!is_array($addr)) continue;
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
            $this->respond(500, ['ok' => false, 'status' => 'ERROR', 'message' => $e->getMessage()]);
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
        foreach ($headers as $k => $v) {
            $out[Tools::strtolower((string)$k)] = $v;
        }
        return $out;
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
            if (!Validate::isLoadedObject($customer)) $idCustomer = 0;
        }

        if (!$idCustomer) {
            $customer = new Customer();
            $isCreate = true;
            if (property_exists($customer, 'id_shop')) $customer->id_shop = (int)$this->context->shop->id;
        }

        $customer->email = trim((string)$customerData['email']);
        $customer->firstname = trim((string)$customerData['firstname']);
        $customer->lastname = trim((string)$customerData['lastname']);
        $customer->active = isset($customerData['active']) ? (int)$customerData['active'] : 1;
        $customer->is_guest = 0;

        if (isset($customerData['company'])) $customer->company = trim((string)$customerData['company']);

        $passHashed = isset($customerData['password_hashed']) ? trim((string)$customerData['password_hashed']) : '';
        $passPlain  = isset($customerData['password']) ? (string)$customerData['password'] : '';

        if ($isCreate || $passHashed !== '' || $passPlain !== '') {
            if ($passHashed !== '') {
                $customer->passwd = $passHashed;
            } elseif ($passPlain !== '') {
                /** @var Hashing $crypto */
                $crypto = ServiceLocator::get(Hashing::class);
                $customer->passwd = $crypto->hash($passPlain);
            }
        }

        $defaultGroup = isset($groups['default']) ? (int)$groups['default'] : (int)Configuration::get(Grifoncustomersync::CFG_DEFAULT_GROUP);
        $list = (isset($groups['list']) && is_array($groups['list'])) ? $groups['list'] : [];

        if ($isCreate) {
            $customer->add();
            $result['created'] = true;
        } else {
            $customer->update();
            $result['updated'] = true;
        }

        $idCustomer = (int)$customer->id;
        $this->replaceCustomerGroups($idCustomer, $list, $defaultGroup);
        $this->upsertCustomerMap($externalCustomerId, $idCustomer, $customer->email);

        return $idCustomer;
    }

    private function upsertAddress($idCustomer, $addr, &$result)
    {
        $externalAddressId = isset($addr['externalAddressId']) ? trim((string)$addr['externalAddressId']) : 'default_'.time();
        $alias = isset($addr['alias']) ? trim((string)$addr['alias']) : 'Main';
        $countryIso = isset($addr['countryIso']) ? Tools::strtoupper(trim((string)$addr['countryIso'])) : 'GR';

        $idCountry = (int)Country::getByIso($countryIso);
        if ($idCountry <= 0) return 0;

        $map = $this->getAddressMapByExternal($externalAddressId);
        $idAddress = $map ? (int)$map['id_address'] : 0;

        $address = $idAddress ? new Address($idAddress) : new Address();
        $address->id_customer = (int)$idCustomer;
        $address->id_country = $idCountry;
        $address->alias = $alias;
        $address->firstname = isset($addr['firstname']) ? $addr['firstname'] : 'N/A';
        $address->lastname = isset($addr['lastname']) ? $addr['lastname'] : 'N/A';
        $address->address1 = isset($addr['address1']) ? $addr['address1'] : 'N/A';
        $address->city = isset($addr['city']) ? $addr['city'] : 'N/A';
        $address->postcode = isset($addr['postcode']) ? $addr['postcode'] : '';
        $address->vat_number = isset($addr['vat_number']) ? $addr['vat_number'] : '';
        $address->phone = isset($addr['phone']) ? $addr['phone'] : '';

        if ($idAddress) $address->update(); else $address->add();

        $idAddress = (int)$address->id;
        $this->upsertAddressMap($externalAddressId, $idAddress, (int)$idCustomer, $alias);
        return $idAddress;
    }

    private function getCustomerIdByEmail($email)
    {
        $sql = 'SELECT `id_customer` FROM `'._DB_PREFIX_.'customer` WHERE `email` = "'.pSQL($email).'" LIMIT 1';
        return (int)Db::getInstance()->getValue($sql);
    }

    private function getCustomerMapByExternal($externalCustomerId)
    {
        $sql = 'SELECT * FROM `'._DB_PREFIX_.'grifon_customer_map` WHERE `external_customer_id` = "'.pSQL($externalCustomerId).'" LIMIT 1';
        return Db::getInstance()->getRow($sql);
    }

    private function upsertCustomerMap($externalCustomerId, $idCustomer, $email)
    {
        $now = date('Y-m-d H:i:s');
        $row = $this->getCustomerMapByExternal($externalCustomerId);
        if ($row) {
            Db::getInstance()->update('grifon_customer_map', ['id_customer' => (int)$idCustomer, 'email' => pSQL($email), 'date_upd' => pSQL($now)], 'external_customer_id = "'.pSQL($externalCustomerId).'"');
        } else {
            Db::getInstance()->insert('grifon_customer_map', ['external_customer_id' => pSQL($externalCustomerId), 'id_customer' => (int)$idCustomer, 'email' => pSQL($email), 'date_add' => pSQL($now), 'date_upd' => pSQL($now)]);
        }
    }

    private function getAddressMapByExternal($externalAddressId)
    {
        $sql = 'SELECT * FROM `'._DB_PREFIX_.'grifon_address_map` WHERE `external_address_id` = "'.pSQL($externalAddressId).'" LIMIT 1';
        return Db::getInstance()->getRow($sql);
    }

    private function upsertAddressMap($externalAddressId, $idAddress, $idCustomer, $alias)
    {
        $now = date('Y-m-d H:i:s');
        $row = $this->getAddressMapByExternal($externalAddressId);
        if ($row) {
            Db::getInstance()->update('grifon_address_map', ['id_address' => (int)$idAddress, 'id_customer' => (int)$idCustomer, 'alias' => pSQL($alias), 'date_upd' => pSQL($now)], 'external_address_id = "'.pSQL($externalAddressId).'"');
        } else {
            Db::getInstance()->insert('grifon_address_map', ['external_address_id' => pSQL($externalAddressId), 'id_address' => (int)$idAddress, 'id_customer' => (int)$idCustomer, 'alias' => pSQL($alias), 'date_add' => pSQL($now), 'date_upd' => pSQL($now)]);
        }
    }

    private function replaceCustomerGroups($idCustomer, $groupIds, $defaultGroup)
    {
        Db::getInstance()->delete('customer_group', 'id_customer = '.(int)$idCustomer);
        $groupIds[] = (int)$defaultGroup;
        $groupIds = array_unique(array_filter(array_map('intval', $groupIds)));
        foreach ($groupIds as $idGroup) {
            Db::getInstance()->execute('INSERT IGNORE INTO `'._DB_PREFIX_.'customer_group` (`id_customer`, `id_group`) VALUES ('.(int)$idCustomer.', '.(int)$idGroup.')');
        }
        Db::getInstance()->update('customer', ['id_default_group' => (int)$defaultGroup], 'id_customer = '.(int)$idCustomer);
    }

    private function respond($statusCode, $data)
    {
        http_response_code((int)$statusCode);
        echo json_encode($data);
        exit;
    }
}
