<?php
/**
 * Grifon Customer Sync (PrestaShop 8.1.2)
 * Endpoint: POST /module/grifoncustomersync/sync
 */

if (!defined('_PS_VERSION_')) {
    exit;
}

class Grifoncustomersync extends Module
{
    const CFG_SECRET        = 'GRIFONCSYNC_SECRET';
    const CFG_ALLOWED_IPS   = 'GRIFONCSYNC_ALLOWED_IPS';
    const CFG_DEFAULT_GROUP = 'GRIFONCSYNC_DEFAULT_GROUP';
    const CFG_TIME_SKEW_SEC = 'GRIFONCSYNC_TIME_SKEW_SEC';

    // Default shared secret (άλλαξέ το από Configure)
    const DEFAULT_SECRET = 'GRIFON_SYNC_2026_CHANGE_ME';

    public function __construct()
    {
        $this->name = 'grifoncustomersync';
        $this->tab = 'administration';
        $this->version = '1.1.0';
        $this->author = 'Grifon';
        $this->need_instance = 0;
        $this->bootstrap = true;

        parent::__construct();

        $this->displayName = $this->l('Grifon Customer Sync');
        $this->description = $this->l('Secure endpoint to sync customers/addresses/company/VAT/groups from your backend (supports hashed passwords).');

        $this->ps_versions_compliancy = [
            'min' => '8.0.0',
            'max' => '8.99.99',
        ];
    }

    public function install()
    {
        return parent::install()
            && $this->installDb()
            && $this->installConfig();
    }

    public function uninstall()
    {
        return $this->uninstallConfig()
            && $this->uninstallDb()
            && parent::uninstall();
    }

    private function installConfig()
    {
        return Configuration::updateValue(self::CFG_SECRET, self::DEFAULT_SECRET)
            && Configuration::updateValue(self::CFG_ALLOWED_IPS, '')
            && Configuration::updateValue(self::CFG_DEFAULT_GROUP, (int)Configuration::get('PS_CUSTOMER_GROUP'))
            && Configuration::updateValue(self::CFG_TIME_SKEW_SEC, 300);
    }

    private function uninstallConfig()
    {
        return Configuration::deleteByName(self::CFG_SECRET)
            && Configuration::deleteByName(self::CFG_ALLOWED_IPS)
            && Configuration::deleteByName(self::CFG_DEFAULT_GROUP)
            && Configuration::deleteByName(self::CFG_TIME_SKEW_SEC);
    }

    private function installDb()
    {
        $sqls = [];

        $sqls[] = 'CREATE TABLE IF NOT EXISTS `'._DB_PREFIX_.'grifon_customer_map` (
            `id_grifon_customer_map` INT UNSIGNED NOT NULL AUTO_INCREMENT,
            `external_customer_id` VARCHAR(64) NOT NULL,
            `id_customer` INT UNSIGNED NOT NULL,
            `email` VARCHAR(255) NULL,
            `date_add` DATETIME NOT NULL,
            `date_upd` DATETIME NOT NULL,
            PRIMARY KEY (`id_grifon_customer_map`),
            UNIQUE KEY `uniq_external_customer_id` (`external_customer_id`),
            KEY `idx_id_customer` (`id_customer`)
        ) ENGINE='._MYSQL_ENGINE_.' DEFAULT CHARSET=utf8mb4;';

        $sqls[] = 'CREATE TABLE IF NOT EXISTS `'._DB_PREFIX_.'grifon_address_map` (
            `id_grifon_address_map` INT UNSIGNED NOT NULL AUTO_INCREMENT,
            `external_address_id` VARCHAR(64) NOT NULL,
            `id_address` INT UNSIGNED NOT NULL,
            `id_customer` INT UNSIGNED NOT NULL,
            `alias` VARCHAR(128) NULL,
            `date_add` DATETIME NOT NULL,
            `date_upd` DATETIME NOT NULL,
            PRIMARY KEY (`id_grifon_address_map`),
            UNIQUE KEY `uniq_external_address_id` (`external_address_id`),
            KEY `idx_id_customer` (`id_customer`),
            KEY `idx_id_address` (`id_address`)
        ) ENGINE='._MYSQL_ENGINE_.' DEFAULT CHARSET=utf8mb4;';

        foreach ($sqls as $sql) {
            if (!Db::getInstance()->execute($sql)) {
                return false;
            }
        }
        return true;
    }

    private function uninstallDb()
    {
        // Αν θέλεις να κρατήσεις τα mappings μετά το uninstall, σχολίασε τα DROP.
        $sqls = [
            'DROP TABLE IF EXISTS `'._DB_PREFIX_.'grifon_address_map`;',
            'DROP TABLE IF EXISTS `'._DB_PREFIX_.'grifon_customer_map`;',
        ];

        foreach ($sqls as $sql) {
            if (!Db::getInstance()->execute($sql)) {
                return false;
            }
        }
        return true;
    }

    public function getContent()
    {
        $output = '';

        if (Tools::isSubmit('submitGrifonCustomerSync')) {
            $secret = (string)Tools::getValue(self::CFG_SECRET);
            $allowedIps = (string)Tools::getValue(self::CFG_ALLOWED_IPS);
            $defaultGroup = (int)Tools::getValue(self::CFG_DEFAULT_GROUP);
            $timeSkew = (int)Tools::getValue(self::CFG_TIME_SKEW_SEC);

            if (Tools::strlen($secret) < 16) {
                $output .= $this->displayError($this->l('Secret must be at least 16 characters.'));
            } else {
                Configuration::updateValue(self::CFG_SECRET, $secret);
                Configuration::updateValue(self::CFG_ALLOWED_IPS, trim($allowedIps));
                Configuration::updateValue(self::CFG_DEFAULT_GROUP, max(1, $defaultGroup));
                Configuration::updateValue(self::CFG_TIME_SKEW_SEC, max(30, $timeSkew));

                $output .= $this->displayConfirmation($this->l('Settings updated.'));
            }
        }

        return $output . $this->renderForm();
    }

    private function renderForm()
    {
        $fieldsForm = [];

        $fieldsForm[0]['form'] = [
            'legend' => [
                'title' => $this->l('Settings'),
                'icon'  => 'icon-cogs',
            ],
            'input' => [
                [
                    'type' => 'text',
                    'label' => $this->l('Shared secret (HMAC)'),
                    'name' => self::CFG_SECRET,
                    'required' => true,
                    'desc' => $this->l('Κωδικός υπογραφής από backend. ΑΛΛΑΞΕ τον πριν production.'),
                ],
                [
                    'type' => 'text',
                    'label' => $this->l('Allowed IPs'),
                    'name' => self::CFG_ALLOWED_IPS,
                    'required' => false,
                    'desc' => $this->l('Comma-separated IPs που επιτρέπονται. Άδειο = όλα (όχι προτεινόμενο).'),
                ],
                [
                    'type' => 'text',
                    'label' => $this->l('Default group id (fallback)'),
                    'name' => self::CFG_DEFAULT_GROUP,
                    'required' => true,
                ],
                [
                    'type' => 'text',
                    'label' => $this->l('Max time skew (seconds)'),
                    'name' => self::CFG_TIME_SKEW_SEC,
                    'required' => true,
                ],
            ],
            'submit' => [
                'title' => $this->l('Save'),
            ],
        ];

        $helper = new HelperForm();
        $helper->show_toolbar = false;
        $helper->module = $this;
        $helper->default_form_language = (int)Configuration::get('PS_LANG_DEFAULT');
        $helper->allow_employee_form_lang = (int)Configuration::get('PS_BO_ALLOW_EMPLOYEE_FORM_LANG');
        $helper->identifier = $this->identifier;
        $helper->submit_action = 'submitGrifonCustomerSync';
        $helper->currentIndex = AdminController::$currentIndex . '&configure=' . $this->name;
        $helper->token = Tools::getAdminTokenLite('AdminModules');

        $helper->fields_value[self::CFG_SECRET] = Configuration::get(self::CFG_SECRET);
        $helper->fields_value[self::CFG_ALLOWED_IPS] = Configuration::get(self::CFG_ALLOWED_IPS);
        $helper->fields_value[self::CFG_DEFAULT_GROUP] = (int)Configuration::get(self::CFG_DEFAULT_GROUP);
        $helper->fields_value[self::CFG_TIME_SKEW_SEC] = (int)Configuration::get(self::CFG_TIME_SKEW_SEC);

        return $helper->generateForm($fieldsForm);
    }

    public static function getAllowedIps()
    {
        $raw = (string)Configuration::get(self::CFG_ALLOWED_IPS);
        if (!trim($raw)) {
            return [];
        }
        $parts = array_map('trim', explode(',', $raw));
        return array_values(array_filter($parts));
    }
}
