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

        $result = ['ok' => true, 'created' => false, 'updated' => false, 'psCustomerId' => null];

        $idCustomer = $this->upsertCustomer($externalCustomerId, $customerData, $groups, $result);
        $result['psCustomerId'] = (int)$idCustomer;

        foreach ($addresses as $addr) {
            $this->upsertAddress($idCustomer, $addr, $result);
        }

        $this->respond(200, $result);
    }

    private function requireAuth($secret, $maxSkew, $rawBody)
    {
        $headers = $this->getHeadersLower();
        $ts = isset($headers['x-grifon-timestamp']) ? (int)$headers['x-grifon-timestamp'] : 0;
        $sig = isset($headers['x-grifon-signature']) ? trim((string)$headers['x-grifon-signature']) : '';

        if ($ts <= 0 || $sig === '' || !$secret) {
            throw new Exception('UNAUTHORIZED');
        }

        if (abs(time() - $ts) > (int)$maxSkew) {
            throw new Exception('STALE_TIMESTAMP');
        }

        $base = $ts . $rawBody;
        $calc = base64_encode(hash_hmac('sha256', $base, $secret, true));

        if (!hash_equals($calc, $sig)) {
            throw new Exception('BAD_SIGNATURE');
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
        $email = trim((string)$customerData['email']);
        $idCustomer = (int)$this->getCustomerIdByEmail($email);
        
        $customer = $idCustomer ? new Customer($idCustomer) : new Customer();
        $customer->email = $email;
        $customer->firstname = trim((string)$customerData['firstname']);
        $customer->lastname = trim((string)$customerData['lastname']);
        $customer->active = 1;
        $customer->is_guest = 0;

        // ΣΥΓΧΡΟΝΙΣΜΟΣ NEWSLETTER & PARTNER OFFERS
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
            $customer->siret = trim((string)$customerData['siret']); // Χρήση για VAT
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

        $this->upsertCustomerMap($externalCustomerId, $customer->id, $customer->email);
        return $customer->id;
    }

    private function upsertAddress($idCustomer, $addr, &$result)
    {
        $address = new Address();
        $address->id_customer = (int)$idCustomer;
        $address->firstname = trim((string)($addr['firstname'] ?? 'N/A'));
        $address->lastname = trim((string)($addr['lastname'] ?? 'N/A'));
        $address->address1 = trim((string)$addr['address1']);
        $address->city = trim((string)$addr['city']);
        $address->postcode = trim((string)$addr['postcode']);
        $address->id_country = (int)Country::getByIso($addr['countryIso']);
        if ($address->id_country <= 0) $address->id_country = (int)Configuration::get('PS_COUNTRY_DEFAULT');
        $address->alias = trim((string)($addr['alias'] ?? 'Default'));
        $address->dni = trim((string)($addr['dni'] ?? $addr['vat_number'] ?? '000000000'));
        $address->save(false);
    }

    private function getCustomerIdByEmail($email)
    {
        $q = new DbQuery();
        $q->select('id_customer');
        $q->from('customer');
        $q->where('email = \''.pSQL($email).'\'');
        return (int)Db::getInstance()->getValue($q);
    }

    private function upsertCustomerMap($externalCustomerId, $idCustomer, $email)
    {
        $q = new DbQuery();
        $q->select('id_grifon_customer_map');
        $q->from('grifon_customer_map');
        $q->where('external_customer_id = \''.pSQL($externalCustomerId).'\'');
        $id_map = (int)Db::getInstance()->getValue($q);

        $data = [
            'external_customer_id' => pSQL($externalCustomerId),
            'id_customer' => (int)$idCustomer,
            'email' => pSQL($email),
            'date_upd' => date('Y-m-d H:i:s')
        ];

        if ($id_map) {
            Db::getInstance()->update('grifon_customer_map', $data, 'id_grifon_customer_map = '.$id_map);
        } else {
            $data['date_add'] = date('Y-m-d H:i:s');
            Db::getInstance()->insert('grifon_customer_map', $data);
        }
    }

    private function respond($statusCode, $data)
    {
        http_response_code((int)$statusCode);
        echo json_encode($data);
        exit;
    }
}
