"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.listCategories = void 0;
const prestashopParser_1 = require("./prestashopParser");
const prestashopFields_1 = require("../utils/prestashopFields");
const pagination_1 = require("../utils/pagination");
const listCategories = async (client, page, pageSize, lang) => {
    try {
        const data = await client.get("categories", {
            "filter[active]": 1,
            display: "full",
            sort: "[id_ASC]",
            limit: (0, pagination_1.toLimitParam)(page, pageSize)
        });
        const categories = (0, prestashopParser_1.extractResourceList)("categories", data);
        if (!categories || categories.length === 0) {
            return { items: [], tree: [] };
        }
        const items = categories.map((category) => ({
            id: Number(category.id),
            parentId: category.id_parent ? (0, prestashopFields_1.toNumber)(category.id_parent) : null,
            name: (0, prestashopFields_1.getLocalizedValue)(category.name, lang),
            position: (0, prestashopFields_1.toNumber)(category.position),
            active: (0, prestashopFields_1.toNumber)(category.active),
            slug: (0, prestashopFields_1.getLocalizedValue)(category.link_rewrite, lang)
        }));
        const nodeMap = new Map();
        items.forEach((item) => {
            nodeMap.set(item.id, { ...item, children: [] });
        });
        const tree = [];
        nodeMap.forEach((node) => {
            // Root categories in PS usually have parent ID 0, 1 (Root) or 2 (Home)
            if (node.parentId && node.parentId > 2 && nodeMap.has(node.parentId)) {
                nodeMap.get(node.parentId)?.children.push(node);
            }
            else if (node.id > 2) {
                tree.push(node);
            }
        });
        return { items, tree };
    }
    catch (error) {
        console.error("Category fetch failed:", {
            status: error.status,
            message: error.message,
            details: error.details
        });
        return { items: [], tree: [] };
    }
};
exports.listCategories = listCategories;
