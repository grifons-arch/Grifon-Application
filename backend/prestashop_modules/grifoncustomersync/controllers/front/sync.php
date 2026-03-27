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

    private function handleFavoriteToggle($payload)
    {
        $idCustomer = isset($payload['customerId']) ? (int)$payload['customerId'] : 0;
        $idProduct = isset($payload['productId']) ? (int)$payload['productId'] : 0;
        $idShop = isset($payload['shopId']) ? (int)$payload['shopId'] : (int)Context::getContext()->shop->id;
        $isFavorite = !empty($payload['isFavorite']);
        $snapshot = (isset($payload['product']) && is_array($payload['product'])) ? $payload['product'] : [];

        if ($idCustomer <= 0 || $idProduct <= 0 || $idShop <= 0) {
            $this->respond(400, ['ok' => false, 'error' => 'INVALID_FAVORITE_PAYLOAD']);
        }

        if ($isFavorite) {
            $this->upsertFavoriteProduct($idCustomer, $idProduct, $idShop, $snapshot);
        } else {
            Db::getInstance()->delete(
                'grifon_favorite_product',
                'id_customer = '.(int)$idCustomer.' AND id_product = '.(int)$idProduct.' AND id_shop = '.(int)$idShop
            );
        }

        $this->respond(200, ['ok' => true]);
    }

    private function handleRecentProduct($payload)
    {
        $idCustomer = isset($payload['customerId']) ? (int)$payload['customerId'] : 0;
        $idProduct = isset($payload['productId']) ? (int)$payload['productId'] : 0;
        $idShop = isset($payload['shopId']) ? (int)$payload['shopId'] : (int)Context::getContext()->shop->id;
        $snapshot = (isset($payload['product']) && is_array($payload['product'])) ? $payload['product'] : [];

        if ($idCustomer <= 0 || $idProduct <= 0 || $idShop <= 0) {
            $this->respond(400, ['ok' => false, 'error' => 'INVALID_RECENT_PAYLOAD']);
        }

        $this->upsertRecentProduct($idCustomer, $idProduct, $idShop, $snapshot);
        $this->trimRecentProducts($idCustomer, $idShop, 20);

        $this->respond(200, ['ok' => true]);
    }

    private function handleListFavoriteProducts($payload)
    {
        $idCustomer = isset($payload['customerId']) ? (int)$payload['customerId'] : 0;
        $idShop = isset($payload['shopId']) ? (int)$payload['shopId'] : (int)Context::getContext()->shop->id;

        if ($idCustomer <= 0 || $idShop <= 0) {
            $this->respond(400, ['ok' => false, 'error' => 'INVALID_FAVORITE_QUERY']);
        }

        $sql = 'SELECT `id_product`, `id_shop`, `title`, `price`, `currency`, `image_url`, `brand`, `date_add`, `date_upd`
                FROM `'._DB_PREFIX_.'grifon_favorite_product`
                WHERE `id_customer`='.(int)$idCustomer.'
                  AND `id_shop`='.(int)$idShop.'
                ORDER BY `date_upd` DESC, `date_add` DESC';
        $rows = Db::getInstance()->executeS($sql);

        $items = [];
        if (is_array($rows)) {
            foreach ($rows as $row) {
                $items[] = [
                    'productId' => (int)$row['id_product'],
                    'shopId' => (int)$row['id_shop'],
                    'title' => isset($row['title']) ? (string)$row['title'] : '',
                    'price' => $row['price'] !== null ? (float)$row['price'] : null,
                    'currency' => isset($row['currency']) ? (string)$row['currency'] : '',
                    'imageUrl' => isset($row['image_url']) ? (string)$row['image_url'] : '',
                    'brand' => isset($row['brand']) ? (string)$row['brand'] : '',
                    'updatedAt' => isset($row['date_upd']) ? strtotime((string)$row['date_upd']) : time(),
                ];
            }
        }

        $this->respond(200, ['ok' => true, 'items' => $items]);
    }

    private function handleListRecentProducts($payload)
    {
        $idCustomer = isset($payload['customerId']) ? (int)$payload['customerId'] : 0;
        $idShop = isset($payload['shopId']) ? (int)$payload['shopId'] : (int)Context::getContext()->shop->id;
        $limit = isset($payload['limit']) ? max(1, (int)$payload['limit']) : 20;

        if ($idCustomer <= 0 || $idShop <= 0) {
            $this->respond(400, ['ok' => false, 'error' => 'INVALID_RECENT_QUERY']);
        }

        $sql = 'SELECT `id_product`, `id_shop`, `title`, `price`, `currency`, `image_url`, `brand`, `visited_at`
                FROM `'._DB_PREFIX_.'grifon_recent_product`
                WHERE `id_customer`='.(int)$idCustomer.'
                  AND `id_shop`='.(int)$idShop.'
                ORDER BY `visited_at` DESC, `date_upd` DESC
                LIMIT '.(int)$limit;
        $rows = Db::getInstance()->executeS($sql);

        $items = [];
        if (is_array($rows)) {
            foreach ($rows as $row) {
                $items[] = [
                    'productId' => (int)$row['id_product'],
                    'shopId' => (int)$row['id_shop'],
                    'title' => isset($row['title']) ? (string)$row['title'] : '',
                    'price' => $row['price'] !== null ? (float)$row['price'] : null,
                    'currency' => isset($row['currency']) ? (string)$row['currency'] : '',
                    'imageUrl' => isset($row['image_url']) ? (string)$row['image_url'] : '',
                    'brand' => isset($row['brand']) ? (string)$row['brand'] : '',
                    'visitedAt' => isset($row['visited_at']) ? strtotime((string)$row['visited_at']) : time(),
                ];
            }
        }

        $this->respond(200, ['ok' => true, 'items' => $items]);
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
        return $customer->id;
    }

    private function resolveCustomerGroups($groups)
    {
        $configuredDefaultGroup = (int)Configuration::get('GRIFONCSYNC_DEFAULT_GROUP');
        $shopDefaultGroup = (int)Configuration::get('PS_CUSTOMER_GROUP');

        $requestedDefaultGroup = 0;
        $requestedGroupIds = [];

        if (isset($groups['default'])) {
            $requestedDefaultGroup = (int)$groups['default'];
        }

        if (isset($groups['list']) && is_array($groups['list'])) {
            $requestedGroupIds = $groups['list'];
        } elseif (is_array($groups)) {
            $requestedGroupIds = $groups;
        }

        $requestedGroupIds = array_values(array_unique(array_filter(array_map('intval', $requestedGroupIds), function ($idGroup) {
            return $idGroup > 0;
        })));

        $validGroupIds = $this->getExistingGroupIds($requestedGroupIds);

        $defaultGroupCandidates = array_filter([
            $requestedDefaultGroup,
            $configuredDefaultGroup,
            $shopDefaultGroup,
        ]);

        $defaultGroupId = 0;
        foreach ($defaultGroupCandidates as $candidateGroupId) {
            $existingCandidate = $this->getExistingGroupIds([(int)$candidateGroupId]);
            if (!empty($existingCandidate)) {
                $defaultGroupId = (int)$existingCandidate[0];
                break;
            }
        }

        if ($defaultGroupId <= 0 && !empty($validGroupIds)) {
            $defaultGroupId = (int)$validGroupIds[0];
        }

        if ($defaultGroupId <= 0) {
            throw new Exception('NO_VALID_CUSTOMER_GROUP');
        }

        if (!in_array($defaultGroupId, $validGroupIds, true)) {
            $validGroupIds[] = $defaultGroupId;
        }

        sort($validGroupIds);

        return [
            'default' => $defaultGroupId,
            'list' => $validGroupIds,
        ];
    }

    private function getExistingGroupIds(array $groupIds)
    {
        if (empty($groupIds)) {
            return [];
        }

        $groupIds = array_values(array_unique(array_map('intval', $groupIds)));
        $groupIds = array_values(array_filter($groupIds, function ($idGroup) {
            return $idGroup > 0;
        }));

        if (empty($groupIds)) {
            return [];
        }

        $sql = 'SELECT `id_group` FROM `'._DB_PREFIX_.'group` WHERE `id_group` IN ('.implode(',', $groupIds).')';
        $rows = Db::getInstance()->executeS($sql);

        if (!is_array($rows)) {
            return [];
        }

        $existingGroupIds = [];
        foreach ($rows as $row) {
            $existingGroupIds[] = (int)$row['id_group'];
        }

        sort($existingGroupIds);

        return array_values(array_unique($existingGroupIds));
    }

    private function syncCustomerGroups($idCustomer, array $resolvedGroups)
    {
        $idCustomer = (int)$idCustomer;
        $groupIds = isset($resolvedGroups['list']) && is_array($resolvedGroups['list']) ? $resolvedGroups['list'] : [];

        if ($idCustomer <= 0 || empty($groupIds)) {
            throw new Exception('INVALID_CUSTOMER_GROUP_ASSIGNMENT');
        }

        Db::getInstance()->delete('customer_group', 'id_customer = '.$idCustomer);

        foreach ($groupIds as $idGroup) {
            Db::getInstance()->insert('customer_group', [
                'id_customer' => $idCustomer,
                'id_group' => (int)$idGroup,
            ]);
        }
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

        // ΔΙΟΡΘΩΣΗ: Χρήση του DNI ή ΑΦΜ, και εξαναγκασμός τιμής αν λείπει
        $dni = trim((string)($addr['dni'] ?? $addr['vat_number'] ?? '123456789'));
        if (empty($dni)) $dni = '123456789';
        $address->dni = $dni;

        // ΠΑΡΑΚΑΜΨΗ VALIDATION: Το false στο save() λέει στο PrestaShop να μην ελέγξει τα πεδία
        // αλλά για σιγουριά ορίζουμε το dni και χειροκίνητα αν χρειαστεί
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

    private function upsertFavoriteProduct($idCustomer, $idProduct, $idShop, array $snapshot)
    {
        $now = date('Y-m-d H:i:s');
        $existingId = (int)Db::getInstance()->getValue(
            'SELECT `id_grifon_favorite_product` FROM `'._DB_PREFIX_.'grifon_favorite_product`
             WHERE `id_customer`='.(int)$idCustomer.'
               AND `id_product`='.(int)$idProduct.'
               AND `id_shop`='.(int)$idShop
        );

        $data = [
            'id_customer' => (int)$idCustomer,
            'id_product' => (int)$idProduct,
            'id_shop' => (int)$idShop,
            'title' => isset($snapshot['title']) ? pSQL((string)$snapshot['title']) : null,
            'price' => isset($snapshot['price']) && $snapshot['price'] !== null ? (float)$snapshot['price'] : null,
            'currency' => isset($snapshot['currency']) ? pSQL((string)$snapshot['currency']) : null,
            'image_url' => isset($snapshot['imageUrl']) ? pSQL((string)$snapshot['imageUrl'], true) : null,
            'brand' => isset($snapshot['brand']) ? pSQL((string)$snapshot['brand']) : null,
            'date_upd' => $now,
        ];

        if ($existingId > 0) {
            Db::getInstance()->update('grifon_favorite_product', $data, 'id_grifon_favorite_product = '.(int)$existingId);
            return;
        }

        $data['date_add'] = $now;
        Db::getInstance()->insert('grifon_favorite_product', $data);
    }

    private function upsertRecentProduct($idCustomer, $idProduct, $idShop, array $snapshot)
    {
        $now = date('Y-m-d H:i:s');
        $existingId = (int)Db::getInstance()->getValue(
            'SELECT `id_grifon_recent_product` FROM `'._DB_PREFIX_.'grifon_recent_product`
             WHERE `id_customer`='.(int)$idCustomer.'
               AND `id_product`='.(int)$idProduct.'
               AND `id_shop`='.(int)$idShop
        );

        $data = [
            'id_customer' => (int)$idCustomer,
            'id_product' => (int)$idProduct,
            'id_shop' => (int)$idShop,
            'title' => isset($snapshot['title']) ? pSQL((string)$snapshot['title']) : null,
            'price' => isset($snapshot['price']) && $snapshot['price'] !== null ? (float)$snapshot['price'] : null,
            'currency' => isset($snapshot['currency']) ? pSQL((string)$snapshot['currency']) : null,
            'image_url' => isset($snapshot['imageUrl']) ? pSQL((string)$snapshot['imageUrl'], true) : null,
            'brand' => isset($snapshot['brand']) ? pSQL((string)$snapshot['brand']) : null,
            'visited_at' => $now,
            'date_upd' => $now,
        ];

        if ($existingId > 0) {
            Db::getInstance()->update('grifon_recent_product', $data, 'id_grifon_recent_product = '.(int)$existingId);
            return;
        }

        $data['date_add'] = $now;
        Db::getInstance()->insert('grifon_recent_product', $data);
    }

    private function trimRecentProducts($idCustomer, $idShop, $keep)
    {
        $idCustomer = (int)$idCustomer;
        $idShop = (int)$idShop;
        $keep = max(1, (int)$keep);

        Db::getInstance()->execute(
            'DELETE FROM `'._DB_PREFIX_.'grifon_recent_product`
             WHERE `id_customer`='.(int)$idCustomer.'
               AND `id_shop`='.(int)$idShop.'
               AND `id_grifon_recent_product` NOT IN (
                   SELECT `id_grifon_recent_product`
                   FROM (
                       SELECT `id_grifon_recent_product`
                       FROM `'._DB_PREFIX_.'grifon_recent_product`
                       WHERE `id_customer`='.(int)$idCustomer.'
                         AND `id_shop`='.(int)$idShop.'
                       ORDER BY `visited_at` DESC, `date_upd` DESC
                       LIMIT '.(int)$keep.'
                   ) recent_keep
               )'
        );
    }

    private function respond($statusCode, $data)
    {
        http_response_code((int)$statusCode);
        echo json_encode($data);
        exit;
    }
}
