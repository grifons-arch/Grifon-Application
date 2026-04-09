"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getGroupMembersCount = exports.getPriceAccess = void 0;
const prestashopParser_1 = require("./prestashopParser");
const prestashopFields_1 = require("../utils/prestashopFields");
const wholesaleGroups_1 = require("../utils/wholesaleGroups");
const extractGroupNames = (group) => {
    const rawName = group?.name;
    if (!rawName) {
        return [];
    }
    if (typeof rawName === "string") {
        return rawName.trim() ? [rawName.trim()] : [];
    }
    const languageEntries = rawName.language;
    if (!languageEntries) {
        return [];
    }
    const entries = Array.isArray(languageEntries) ? languageEntries : [languageEntries];
    return entries
        .map((entry) => entry?.value ?? entry?.text ?? null)
        .filter((value) => typeof value === "string")
        .map((value) => value.trim())
        .filter((value) => value.length > 0);
};
const getPriceAccess = async (client, customerId) => {
    const data = await client.getById("customers", customerId, { display: "full" });
    const customer = (0, prestashopParser_1.extractResourceItem)("customers", data);
    if (!customer) {
        return {
            customerId,
            active: false,
            defaultGroupId: null,
            groupIds: [],
            groupNames: [],
            groupShowPrices: false,
            hasWholesaleGroup: false,
            allowed: false
        };
    }
    const active = (0, prestashopFields_1.toBooleanFlag)(customer.active);
    const defaultGroupId = customer.id_default_group ? Number(customer.id_default_group) : null;
    let groupShowPrices = false;
    const associationGroupIds = customer.associations?.groups
        ? (0, prestashopParser_1.extractResourceList)("groups", customer.associations)
            .map((group) => Number(group.id))
            .filter((groupId) => !Number.isNaN(groupId) && groupId > 0)
        : [];
    const groupIds = Array.from(new Set((defaultGroupId ? [defaultGroupId, ...associationGroupIds] : associationGroupIds).filter((groupId) => groupId > 0)));
    const groupNames = [];
    for (const groupId of groupIds) {
        const groupData = await client.getById("groups", groupId, { display: "full" });
        const group = (0, prestashopParser_1.extractResourceItem)("groups", groupData);
        if (!group) {
            continue;
        }
        if (groupId === defaultGroupId) {
            groupShowPrices = (0, prestashopFields_1.toBooleanFlag)(group.show_prices);
        }
        groupNames.push(...extractGroupNames(group));
    }
    const hasWholesaleGroup = groupNames.some((groupName) => (0, wholesaleGroups_1.hasWholesaleKeyword)(groupName));
    const allowed = active && hasWholesaleGroup;
    return {
        customerId,
        active,
        defaultGroupId,
        groupIds,
        groupNames,
        groupShowPrices,
        hasWholesaleGroup,
        allowed
    };
};
exports.getPriceAccess = getPriceAccess;
const getGroupMembersCount = async (client, groupId) => {
    const pageSize = 100;
    let offset = 0;
    let total = 0;
    while (true) {
        const data = await client.get("customers", {
            "filter[active]": 1,
            "filter[id_default_group]": groupId,
            display: "[id]",
            limit: `${offset},${pageSize}`
        });
        const customers = (0, prestashopParser_1.extractResourceList)("customers", data);
        total += customers.length;
        if (customers.length < pageSize) {
            break;
        }
        offset += pageSize;
    }
    return total;
};
exports.getGroupMembersCount = getGroupMembersCount;
