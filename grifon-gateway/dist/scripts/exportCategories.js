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
    const shopIdRaw = Number(options.shopId ?? "4");
    return {
        shopId: shopIdRaw === 1 ? 1 : 4,
        lang: options.lang ? Number(options.lang) : 1,
        output: options.output?.trim() || "exports/full-catalog.json",
        pageSize: Number(options.pageSize) || 200
    };
};
const fetchAllCategories = async (client, pageSize, lang) => {
    const allItems = [];
    let page = 1;
    console.log("Fetching categories...");
    while (true) {
        const data = await client.get("categories", {
            sort: "[id_ASC]",
            limit: (0, pagination_1.toLimitParam)(page, pageSize),
            display: "full"
        });
        const categories = (0, prestashopParser_1.extractResourceList)("categories", data);
        if (!categories.length)
            break;
        const mapped = categories.map((c) => {
            const item = {
                id: Number(c.id),
                parentId: (0, prestashopFields_1.toNumber)(c.id_parent),
                name: (0, prestashopFields_1.getLocalizedValue)(c.name, lang),
                position: (0, prestashopFields_1.toNumber)(c.position),
                active: (0, prestashopFields_1.toNumber)(c.active),
                slug: (0, prestashopFields_1.getLocalizedValue)(c.link_rewrite, lang)
            };
            console.log(`Found Category: [ID: ${item.id}] ${item.name}`);
            return item;
        });
        allItems.push(...mapped);
        if (categories.length < pageSize)
            break;
        page++;
    }
    console.log(`Fetched total ${allItems.length} categories.`);
    return allItems;
};
const collectSubcategoryNames = (node) => {
    const names = [];
    for (const child of node.children) {
        if (child.name) {
            names.push(child.name);
        }
        names.push(...collectSubcategoryNames(child));
    }
    return names;
};
const run = async () => {
    const options = parseArgs(process.argv.slice(2));
    const client = new PrestaShopClient_1.PrestaShopClient({ shopId: options.shopId, lang: options.lang });
    const categories = await fetchAllCategories(client, options.pageSize, options.lang);
    const nodeMap = new Map();
    categories.forEach((cat) => {
        nodeMap.set(cat.id, { ...cat, children: [] });
    });
    const tree = [];
    nodeMap.forEach((node) => {
        if (node.parentId && nodeMap.has(node.parentId) && node.parentId !== node.id && node.id > 2) {
            const parent = nodeMap.get(node.parentId);
            parent?.children.push(node);
        }
        else {
            if (node.id >= 2)
                tree.push(node);
        }
    });
    console.log(`Final tree built with ${tree.length} top-level categories.`);
    const categoriesWithSubcategories = tree.map((mainCategory) => ({
        mainCategoryName: mainCategory.name,
        subcategoryNames: collectSubcategoryNames(mainCategory)
    }));
    const outputPath = path_1.default.resolve(options.output);
    await promises_1.default.mkdir(path_1.default.dirname(outputPath), { recursive: true });
    await promises_1.default.writeFile(outputPath, JSON.stringify({
        shopId: options.shopId,
        categories: categoriesWithSubcategories
    }, null, 2), "utf8");
    console.log(`Successfully exported to ${outputPath}`);
};
run().catch((error) => {
    console.error("Fatal error:", error);
    process.exit(1);
});
