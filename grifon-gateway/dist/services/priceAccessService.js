"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getGroupMembersCount = exports.getPriceAccess = void 0;
const prestashopParser_1 = require("./prestashopParser");
const prestashopFields_1 = require("../utils/prestashopFields");
const hasWholesaleKeyword = (name) => {
    const normalized = name?.trim().toLowerCase() ?? "";
    if (!normalized) {
        return false;
    }
    return normalized.includes("wholesale");
};
const getPriceAccess = async (client, customerId) => {
    const data = await client.getById("customers", customerId, { display: "full" });
    const customer = (0, prestashopParser_1.extractResourceItem)("customers", data);
    if (!customer) {
        return {
            customerId,
            active: false,
            defaultGroupId: null,
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
        const rawName = group.name?.language?.[0]?.value
            ?? group.name?.language?.value
            ?? group.name
            ?? null;
        if (typeof rawName === "string" && rawName.trim().length > 0) {
            groupNames.push(rawName);
        }
    }
    const hasWholesaleGroup = groupNames.some((groupName) => hasWholesaleKeyword(groupName));
    const allowed = active && hasWholesaleGroup;
    return {
        customerId,
        active,
        defaultGroupId,
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
