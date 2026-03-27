<?php

if (!defined('_PS_VERSION_')) {
    exit;
}

function upgrade_module_1_1_1($module)
{
    if (!Validate::isLoadedObject($module)) {
        return false;
    }

    return (bool)$module->upgradeSchema();
}
