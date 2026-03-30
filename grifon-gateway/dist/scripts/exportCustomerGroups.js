"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const promises_1 = __importDefault(require("fs/promises"));
const path_1 = __importDefault(require("path"));
const PrestaShopClient_1 = require("../clients/PrestaShopClient");
const prestashopParser_1 = require("../services/prestashopParser");
const prestashopFields_1 = require("../utils/prestashopFields");
const pagination_1 = require("../utils/pagination");
const parseArgs = (argv) => {
    const options = {};
    for (const arg of argv) {
        if (!arg.startsWith("--"))
            continue;
        const [rawKey, rawValue] = arg.slice(2).split("=");
        if (!rawKey)
            continue;
        options[rawKey] = rawValue ?? "";
    }
    const rawShopIds = (options.shopIds ?? "4,1")
        .split(",")
        .map((value) => Number(value.trim()))
        .filter((value) => value === 1 || value === 4);
    return {
        shopIds: rawShopIds.length ? Array.from(new Set(rawShopIds)) : [4, 1],
        lang: options.lang ? Number(options.lang) : 1,
        output: options.output?.trim() || "exports/customer-groups.json",
        pageSize: Number(options.pageSize) || 100
    };
};
const fetchGroups = async (client, lang) => {
    const data = await client.get("groups", { display: "full", sort: "[id_ASC]" });
    const groups = (0, prestashopParser_1.extractResourceList)("groups", data);
    const result = new Map();
    groups.forEach((group) => {
        const groupId = Number(group.id);
        result.set(groupId, {
            id: groupId,
            name: (0, prestashopFields_1.getLocalizedValue)(group.name, lang),
            showPrices: (0, prestashopFields_1.toBooleanFlag)(group.show_prices)
        });
    });
    return result;
};
const fetchCustomers = async (client, groupsById, pageSize) => {
    const customers = [];
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
            const defaultGroupId = customer.id_default_group ? Number(customer.id_default_group) : null;
            const groupIds = customer.associations?.groups
                ? (0, prestashopParser_1.extractResourceList)("groups", customer.associations)
                    .map((group) => Number(group.id))
                    .filter((id) => !Number.isNaN(id))
                : [];
            const normalizedGroupIds = Array.from(new Set((defaultGroupId ? [defaultGroupId, ...groupIds] : groupIds).filter((id) => id > 0)));
            customers.push({
                id: Number(customer.id),
                email: customer.email ?? null,
                firstName: customer.firstname ?? null,
                lastName: customer.lastname ?? null,
                active: (0, prestashopFields_1.toBooleanFlag)(customer.active),
                defaultGroupId,
                defaultGroupName: defaultGroupId ? groupsById.get(defaultGroupId)?.name ?? null : null,
                groupIds: normalizedGroupIds,
                groupNames: normalizedGroupIds
                    .map((groupId) => groupsById.get(groupId)?.name)
                    .filter((name) => typeof name === "string" && name.length > 0)
            });
        }
        if (items.length < pageSize) {
            break;
        }
        page += 1;
    }
    return customers;
};
const exportShopCustomerGroups = async (shopId, lang, pageSize) => {
    const client = new PrestaShopClient_1.PrestaShopClient({ shopId, lang });
    const groupsById = await fetchGroups(client, lang);
    const customers = await fetchCustomers(client, groupsById, pageSize);
    return {
        shopId,
        groups: Array.from(groupsById.values()),
        customers
    };
};
const run = async () => {
    const options = parseArgs(process.argv.slice(2));
    const shops = [];
    for (const shopId of options.shopIds) {
        // eslint-disable-next-line no-console
        console.log(`Exporting customer groups for shop ${shopId}...`);
        const shopExport = await exportShopCustomerGroups(shopId, options.lang, options.pageSize);
        shops.push(shopExport);
        // eslint-disable-next-line no-console
        console.log(`Shop ${shopId}: ${shopExport.customers.length} customers, ${shopExport.groups.length} groups`);
    }
    const outputPath = path_1.default.resolve(options.output);
    await promises_1.default.mkdir(path_1.default.dirname(outputPath), { recursive: true });
    await promises_1.default.writeFile(outputPath, JSON.stringify({
        generatedAt: new Date().toISOString(),
        shopIds: options.shopIds,
        shops
    }, null, 2), "utf8");
    // eslint-disable-next-line no-console
    console.log(`Customer groups exported to ${outputPath}`);
};
run().catch((error) => {
    // eslint-disable-next-line no-console
    console.error("Failed to export customer groups", error);
    process.exit(1);
});
