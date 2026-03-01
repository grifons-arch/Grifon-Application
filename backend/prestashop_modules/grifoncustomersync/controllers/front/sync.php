<?php
/**
 * POST /module/grifoncustomersync/sync
 */

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
            $this->respond(405, ['ok' => false, 'status' => 'ERROR', 'message' => 'METHOD_NOT_ALLOWED']);
        }

        $raw = (string)file_get_contents('php://input');
        $payload = json_decode($raw, true);
        if (!is_array($payload)) {
            $this->respond(400, ['ok' => false, 'status' => 'ERROR', 'message' => 'INVALID_JSON']);
        }

        // Auth
        $secret = (string)Configuration::get(Grifoncustomersync::CFG_SECRET);
        $skew = (int)Configuration::get(Grifoncustomersync::CFG_TIME_SKEW_SEC);
        $this->requireAuth($secret, $skew, $raw);

        $action = isset($payload['action']) ? trim((string)$payload['action']) : 'sync';

        try {
            if ($action === 'login') {
                $this->handleLogin($payload);
            } else {
                $this->handleSync($payload);
            }
        } catch (Exception $e) {
            $this->respond(500, ['ok' => false, 'status' => 'ERROR', 'message' => $e->getMessage()]);
        } catch (Error $err) {
            $this->respond(500, ['ok' => false, 'status' => 'ERROR', 'message' => $err->getMessage()]);
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

        $crypto = new Hashing();
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
            $this->respond(400, ['ok' => false, 'status' => 'ERROR', 'message' => 'MISSING_externalCustomerId']);
        }

        $email = isset($customerData['email']) ? trim((string)$customerData['email']) : '';
        if ($email === '' || !Validate::isEmail($email)) {
            $this->respond(400, ['ok' => false, 'status' => 'ERROR', 'message' => 'INVALID_email']);
        }

        $result = [
            'ok' => true,
            'status' => 'SUCCESS',
            'created' => false,
            'updated' => false,
            'psCustomerId' => null,
            'id_customer' => null,
            'psAddressIds' => [],
        ];

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
    }

    private function requireAuth($secret, $maxSkew, $rawBody)
    {
        $headers = $this->getHeadersLower();
        $ts = isset($headers['x-grifon-timestamp']) ? (int)$headers['x-grifon-timestamp'] : 0;
        $sig = isset($headers['x-grifon-signature']) ? trim((string)$headers['x-grifon-signature']) : '';

        if ($ts <= 0 || $sig === '' || $secret === '') {
            $this->respond(401, ['ok' => false, 'status' => 'ERROR', 'message' => 'UNAUTHORIZED_MISSING_HEADERS']);
        }

        $now = time();
        if (abs($now - $ts) > (int)$maxSkew) {
            $this->respond(401, ['ok' => false, 'status' => 'ERROR', 'message' => 'STALE_TIMESTAMP']);
        }

        $baseWithNL = $ts . "\n" . $rawBody;
        $baseNoNL = $ts . $rawBody;

        $calcWithNL = base64_encode(hash_hmac('sha256', $baseWithNL, $secret, true));
        $calcNoNL = base64_encode(hash_hmac('sha256', $baseNoNL, $secret, true));

        if (!hash_equals($calcWithNL, $sig) && !hash_equals($calcNoNL, $sig)) {
            $this->respond(401, ['ok' => false, 'status' => 'ERROR', 'message' => 'BAD_SIGNATURE']);
        }
    }

    private function getHeadersLower()
    {
        $headers = [];
        if (function_exists('getallheaders')) {
            $headers = getallheaders();
        } else {
            foreach ($_SERVER as $name => $value) {
                if (substr($name, 0, 5) == 'HTTP_') {
                    $key = str_replace(' ', '-', ucwords(strtolower(str_replace('_', ' ', substr($name, 5)))));
                    $headers[$key] = $value;
                }
            }
        }

        $out = [];
        foreach ($headers as $k => $v) {
            $out[strtolower((string)$k)] = $v;
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

        $customer = $idCustomer ? new Customer($idCustomer) : new Customer();
        if (!$idCustomer) {
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

        if (!$idCustomer || $passHashed !== '' || $passPlain !== '') {
            if ($passHashed !== '') {
                $customer->passwd = $passHashed;
            } elseif ($passPlain !== '') {
                $crypto = new Hashing();
                $customer->passwd = $crypto->hash($passPlain);
            }
        }

        $defaultGroup = isset($groups['default']) ? (int)$groups['default'] : (int)Configuration::get(Grifoncustomersync::CFG_DEFAULT_GROUP);
        $list = (isset($groups['list']) && is_array($groups['list'])) ? $groups['list'] : [];

        if (!$idCustomer) {
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
        $countryIso = isset($addr['countryIso']) ? strtoupper(trim((string)$addr['countryIso'])) : 'GR';

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
        $q = new DbQuery();
        $q->select('id_customer');
        $q->from('customer');
        $q->where('email = \'' . pSQL($email) . '\'');
        return (int)Db::getInstance(_PS_USE_SQL_SLAVE_)->getValue($q);
    }

    private function getCustomerMapByExternal($externalCustomerId)
    {
        $q = new DbQuery();
        $q->from('grifon_customer_map');
        $q->where('external_customer_id = \'' . pSQL($externalCustomerId) . '\'');
        return Db::getInstance(_PS_USE_SQL_SLAVE_)->getRow($q);
    }

    private function upsertCustomerMap($externalCustomerId, $idCustomer, $email)
    {
        $now = date('Y-m-d H:i:s');
        $row = $this->getCustomerMapByExternal($externalCustomerId);

        $data = [
            'id_customer' => (int)$idCustomer,
            'email' => pSQL($email),
            'date_upd' => pSQL($now)
        ];

        if ($row) {
            Db::getInstance()->update('grifon_customer_map', $data, 'external_customer_id = \'' . pSQL($externalCustomerId) . '\'', 0, false, true, true);
        } else {
            $data['external_customer_id'] = pSQL($externalCustomerId);
            $data['date_add'] = pSQL($now);
            Db::getInstance()->insert('grifon_customer_map', $data, false, true, Db::INSERT, true);
        }
    }

    private function getAddressMapByExternal($externalAddressId)
    {
        $q = new DbQuery();
        $q->from('grifon_address_map');
        $q->where('external_address_id = \'' . pSQL($externalAddressId) . '\'');
        return Db::getInstance(_PS_USE_SQL_SLAVE_)->getRow($q);
    }

    private function upsertAddressMap($externalAddressId, $idAddress, $idCustomer, $alias)
    {
        $now = date('Y-m-d H:i:s');
        $row = $this->getAddressMapByExternal($externalAddressId);

        $data = [
            'id_address' => (int)$idAddress,
            'id_customer' => (int)$idCustomer,
            'alias' => pSQL($alias),
            'date_upd' => pSQL($now)
        ];

        if ($row) {
            Db::getInstance()->update('grifon_address_map', $data, 'external_address_id = \'' . pSQL($externalAddressId) . '\'', 0, false, true, true);
        } else {
            $data['external_address_id'] = pSQL($externalAddressId);
            $data['date_add'] = pSQL($now);
            Db::getInstance()->insert('grifon_address_map', $data, false, true, Db::INSERT, true);
        }
    }

    private function replaceCustomerGroups($idCustomer, $groupIds, $defaultGroup)
    {
        Db::getInstance()->delete('customer_group', 'id_customer = '.(int)$idCustomer, 0, true, true);
        $groupIds[] = (int)$defaultGroup;
        $groupIds = array_unique(array_filter(array_map('intval', $groupIds)));
        foreach ($groupIds as $idGroup) {
            Db::getInstance()->insert('customer_group', [
                'id_customer' => (int)$idCustomer,
                'id_group' => (int)$idGroup
            ], false, true, Db::INSERT, true);
        }
        Db::getInstance()->update('customer', ['id_default_group' => (int)$defaultGroup], 'id_customer = '.(int)$idCustomer, 0, false, true, true);
    }

    private function respond($statusCode, $data)
    {
        http_response_code((int)$statusCode);
        echo json_encode($data);
        exit;
    }
}
