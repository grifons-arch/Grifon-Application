"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getProductDetail = exports.listProductsByCategory = exports.listAllProducts = void 0;
const prestashopParser_1 = require("./prestashopParser");
const pagination_1 = require("../utils/pagination");
const prestashopFields_1 = require("../utils/prestashopFields");
const buildImageUrl = (shopId, productId, imageId) => {
    return `/v1/images/products/${productId}/${imageId}?shopId=${shopId}`;
};
const normalizeProduct = (product, shopId, lang, allowPrice = true) => {
    const id = Number(product.id);
    let idImage = (0, prestashopFields_1.toNumber)(product.id_default_image);
    if (!idImage && product.associations?.images) {
        const images = (0, prestashopParser_1.extractResourceList)("images", product.associations);
        if (images.length > 0)
            idImage = Number(images[0].id);
    }
    const categories = product.associations?.categories
        ? (0, prestashopParser_1.extractResourceList)("categories", product.associations).map(c => ({ id: (0, prestashopFields_1.toNumber)(c.id) }))
        : [];
    return {
        id,
        name: (0, prestashopFields_1.getLocalizedValue)(product.name, lang),
        price: allowPrice ? (0, prestashopFields_1.toNumber)(product.price) : null,
        reference: product.reference ?? null,
        defaultImage: idImage ? { id: idImage, url: buildImageUrl(shopId, id, idImage) } : null,
        active: (0, prestashopFields_1.toNumber)(product.active),
        categories
    };
};
const listAllProducts = async (client, shopId, page, pageSize, sort, lang, allowPrice = true) => {
    const data = await client.get("products", {
        "filter[active]": 1,
        sort,
        limit: (0, pagination_1.toLimitParam)(page, pageSize),
        display: "full"
    });
    const items = (0, prestashopParser_1.extractResourceList)("products", data);
    return items.map((product) => normalizeProduct(product, shopId, lang, allowPrice));
};
exports.listAllProducts = listAllProducts;
async function getAllCategoryDescendants(client, parentId) {
    const results = [parentId];
    const fetchChildren = async (id) => {
        try {
            const data = await client.get("categories", {
                "filter[id_parent]": id,
                "filter[active]": 1,
                display: "[id]",
                limit: "1000"
            });
            const children = (0, prestashopParser_1.extractResourceList)("categories", data);
            for (const child of children) {
                const childId = (0, prestashopFields_1.toNumber)(child.id);
                if (!results.includes(childId)) {
                    results.push(childId);
                    await fetchChildren(childId);
                }
            }
        }
        catch (e) { }
    };
    await fetchChildren(parentId);
    return results;
}
const listProductsByCategory = async (client, shopId, categoryId, page, pageSize, sort, lang, allowPrice = true) => {
    // 0. Λήψη συνολικού πλήθους προϊόντων καταστήματος (για log)
    try {
        const shopData = await client.get("products", {
            "filter[active]": 1,
            display: "[id]",
            limit: "1" // Quick check
        });
        // Σημείωση: Το PrestaShop API συνήθως δεν δίνει το total_results στο JSON body 
        // χωρίς ειδική ρύθμιση, αλλά μπορούμε να πάρουμε μια ιδέα από το "Όλα τα προϊόντα"
    }
    catch (e) { }
    console.log(`\n--- [CategoryFetch Start] ---`);
    console.log(`Target Category: ${categoryId} | Shop: ${shopId}`);
    if (categoryId === 2) {
        const items = await (0, exports.listAllProducts)(client, shopId, page, pageSize, sort, lang, allowPrice);
        console.log(`[CategoryFetch] Shop Root (2) Results: ${items.length} products`);
        console.log(`--- [CategoryFetch End] ---\n`);
        return items;
    }
    // 1. Get ALL categories in tree
    const categoryIds = await getAllCategoryDescendants(client, categoryId);
    console.log(`[CategoryFetch] Hierarchy: Found ${categoryIds.length} categories/subcategories`);
    // 2. Aggregate unique product IDs from ALL categories in the tree
    const productIdsSet = new Set();
    // Use id_category_default filter (Source A)
    try {
        const filterVal = `[${categoryIds.join("|")}]`;
        const dataDefault = await client.get("products", {
            "filter[id_category_default]": filterVal,
            "filter[active]": 1,
            display: "[id]",
            limit: "2000"
        });
        const itemsA = (0, prestashopParser_1.extractResourceList)("products", dataDefault);
        itemsA.forEach(p => productIdsSet.add((0, prestashopFields_1.toNumber)(p.id)));
        console.log(`[CategoryFetch] Source A (Default Category): Found ${itemsA.length} IDs`);
    }
    catch (e) { }
    // Then use associations for each category (Source B)
    const associationPromises = categoryIds.map(async (id) => {
        try {
            const catData = await client.get(`categories/${id}`, { display: "full" });
            const category = (0, prestashopParser_1.extractResourceItem)("categories", catData);
            if (category?.associations?.products) {
                const pIds = (0, prestashopParser_1.extractResourceList)("products", category.associations).map(p => (0, prestashopFields_1.toNumber)(p.id));
                pIds.forEach(pid => productIdsSet.add(pid));
                return pIds.length;
            }
        }
        catch (e) { }
        return 0;
    });
    const resultsB = await Promise.all(associationPromises);
    const totalRawB = resultsB.reduce((sum, val) => sum + val, 0);
    console.log(`[CategoryFetch] Source B (Associations): Found ${totalRawB} raw links`);
    const allIds = Array.from(productIdsSet);
    console.log(`[CategoryFetch] TOTAL UNIQUE PRODUCTS for this Category Tree: ${allIds.length}`);
    if (allIds.length === 0) {
        console.log(`[CategoryFetch] No products found.`);
        console.log(`--- [CategoryFetch End] ---\n`);
        return [];
    }
    // 3. Simple ID-based pagination
    const start = (page - 1) * pageSize;
    const pageIds = allIds.slice(start, start + pageSize);
    console.log(`[CategoryFetch] Page ${page}: Requesting full data for ${pageIds.length} items`);
    // 4. Fetch the actual product objects for this page
    const filterIds = `[${pageIds.join("|")}]`;
    const productsData = await client.get("products", {
        "filter[id]": filterIds,
        "filter[active]": 1,
        display: "full",
        limit: pageSize.toString()
    });
    const finalItems = (0, prestashopParser_1.extractResourceList)("products", productsData);
    console.log(`[CategoryFetch] Successfully retrieved ${finalItems.length} products`);
    console.log(`--- [CategoryFetch End] ---\n`);
    // Manual sorting
    if (sort.includes("price")) {
        const desc = sort.includes("DESC");
        finalItems.sort((a, b) => {
            const vA = (0, prestashopFields_1.toNumber)(a.price);
            const vB = (0, prestashopFields_1.toNumber)(b.price);
            return desc ? vB - vA : vA - vB;
        });
    }
    return finalItems.map((product) => normalizeProduct(product, shopId, lang, allowPrice));
};
exports.listProductsByCategory = listProductsByCategory;
const getProductDetail = async (client, shopId, productId, lang, allowPrice = true) => {
    const data = await client.getById("products", productId, { display: "full" });
    const product = (0, prestashopParser_1.extractResourceItem)("products", data);
    if (!product)
        return null;
    return normalizeProduct(product, shopId, lang, allowPrice);
};
exports.getProductDetail = getProductDetail;
