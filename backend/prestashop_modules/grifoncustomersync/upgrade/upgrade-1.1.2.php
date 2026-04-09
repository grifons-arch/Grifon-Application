<?php

if (!defined('_PS_VERSION_')) {
    exit;
}

function upgrade_module_1_1_2($module)
{
    if (!Validate::isLoadedObject($module)) {
        return false;
    }

    if (!Configuration::hasKey('GRIFONCSYNC_WHOLESALE_APPLICATION_TABLE')) {
        Configuration::updateValue('GRIFONCSYNC_WHOLESALE_APPLICATION_TABLE', '');
    }

    return (bool)$module->upgradeSchema();
}
