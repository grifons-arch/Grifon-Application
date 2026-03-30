"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.listCategories = void 0;
const prestashopParser_1 = require("./prestashopParser");
const prestashopFields_1 = require("../utils/prestashopFields");
const pagination_1 = require("../utils/pagination");
const listCategories = async (client, page, pageSize, lang) => {
    try {
        // Φέρνουμε όλες τις κατηγορίες (χωρίς active filter για το sync) 
        // με display=full για να έχουμε το id_parent
        const data = await client.get("categories", {
            display: "full",
            limit: (0, pagination_1.toLimitParam)(page, pageSize)
        });
        const categories = (0, prestashopParser_1.extractResourceList)("categories", data);
        console.log(`[Gateway] PrestaShop returned ${categories.length} raw categories`);
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
        // Build tree
        const nodeMap = new Map();
        items.forEach((item) => {
            nodeMap.set(item.id, { ...item, children: [] });
        });
        const tree = [];
        nodeMap.forEach((node) => {
            if (node.parentId && nodeMap.has(node.parentId) && node.parentId !== node.id) {
                nodeMap.get(node.parentId)?.children.push(node);
            }
            else {
                tree.push(node);
            }
        });
        return { items, tree };
    }
    catch (error) {
        console.error("[Gateway] Category fetch failed:", error);
        return { items: [], tree: [] };
    }
};
exports.listCategories = listCategories;
