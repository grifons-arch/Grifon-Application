"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.listCustomers = void 0;
const prestashopParser_1 = require("./prestashopParser");
const prestashopFields_1 = require("../utils/prestashopFields");
const pagination_1 = require("../utils/pagination");
const hasWholesaleKeyword = (name) => {
    const normalized = name?.trim().toLowerCase() ?? "";
    if (!normalized) {
        return false;
    }
    return (normalized.includes("wholesale") ||
        normalized.includes("wholesales") ||
        normalized.includes("whalesale") ||
        normalized.includes("whalesales"));
};
const fetchGroups = async (client, lang) => {
    const data = await client.get("groups", { display: "full", sort: "[id_ASC]" });
    const groups = (0, prestashopParser_1.extractResourceList)("groups", data);
    const groupsById = new Map();
    groups.forEach((group) => {
        const groupId = Number(group.id);
        if (Number.isNaN(groupId) || groupId <= 0) {
            return;
        }
        groupsById.set(groupId, {
            id: groupId,
            name: (0, prestashopFields_1.getLocalizedValue)(group.name, lang),
            showPrices: (0, prestashopFields_1.toBooleanFlag)(group.show_prices)
        });
    });
    return groupsById;
};
const resolveWholesaleGroupIds = (groupsById) => {
    const ids = Array.from(groupsById.values())
        .filter((group) => hasWholesaleKeyword(group.name))
        .map((group) => group.id);
    return new Set(ids);
};
const listCustomers = async (client, shopId, lang) => {
    const groupsById = await fetchGroups(client, lang);
    const wholesaleGroupIds = resolveWholesaleGroupIds(groupsById);
    const customers = [];
    const pageSize = 100;
    let page = 1;
    while (true) {
        const data = await client.get("customers", {
            display: "full",
            sort: "[id_ASC]",
            limit: (0, pagination_1.toLimitParam)(page, pageSize)
        });
        const items = (0, prestashopParser_1.extractResourceList)("customers", data);
        if (!items.length) {
            break;
        }
        for (const customer of items) {
            const customerId = Number(customer.id);
            if (Number.isNaN(customerId) || customerId <= 0) {
                continue;
            }
            const defaultGroupId = customer.id_default_group ? Number(customer.id_default_group) : null;
            const associationGroupIds = customer.associations?.groups
                ? (0, prestashopParser_1.extractResourceList)("groups", customer.associations)
                    .map((group) => Number(group.id))
                    .filter((groupId) => !Number.isNaN(groupId) && groupId > 0)
                : [];
            const groupIds = Array.from(new Set((defaultGroupId ? [defaultGroupId, ...associationGroupIds] : associationGroupIds).filter((groupId) => groupId > 0)));
            const matchedWholesaleGroupIds = groupIds.filter((groupId) => wholesaleGroupIds.has(groupId));
            customers.push({
                customerId,
                shopId,
                email: customer.email ?? null,
                firstName: customer.firstname ?? null,
                lastName: customer.lastname ?? null,
                company: customer.company ?? null,
                active: (0, prestashopFields_1.toBooleanFlag)(customer.active),
                defaultGroupId,
                defaultGroupName: defaultGroupId ? groupsById.get(defaultGroupId)?.name ?? null : null,
                groupIds,
                groupNames: groupIds
                    .map((groupId) => groupsById.get(groupId)?.name)
                    .filter((name) => typeof name === "string" && name.length > 0),
                wholesaleGroupIds: matchedWholesaleGroupIds,
                wholesaleGroupNames: matchedWholesaleGroupIds
                    .map((groupId) => groupsById.get(groupId)?.name)
                    .filter((name) => typeof name === "string" && name.length > 0),
                isWholesale: matchedWholesaleGroupIds.length > 0
            });
        }
        if (items.length < pageSize) {
            break;
        }
        page += 1;
    }
    return customers;
};
exports.listCustomers = listCustomers;
