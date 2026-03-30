"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getProductDetail = exports.buildCatalogFacets = exports.listFacetProductsByCategory = exports.listProductsByCategory = exports.listAllProducts = void 0;
const prestashopParser_1 = require("./prestashopParser");
const pagination_1 = require("../utils/pagination");
const prestashopFields_1 = require("../utils/prestashopFields");
const buildImageUrl = (shopId, productId, imageId) => {
    return `/v1/images/products/${productId}/${imageId}?shopId=${shopId}`;
};
const normalizeProduct = (product, shopId, lang, allowPrice = true, resolvedAttributes = {}) => {
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
        attributes: resolvedAttributes,
        defaultImage: idImage ? { id: idImage, url: buildImageUrl(shopId, id, idImage) } : null,
        active: (0, prestashopFields_1.toNumber)(product.active),
        categories
    };
};
const buildProductAttributeLookup = async (client, products, lang) => {
    const pushAttributeValue = (target, key, value) => {
        const normalizedKey = key.trim();
        const normalizedValue = value.trim();
        if (!normalizedKey || !normalizedValue) {
            return;
        }
        const existing = target[normalizedKey] ?? [];
        if (!existing.includes(normalizedValue)) {
            target[normalizedKey] = [...existing, normalizedValue];
        }
    };
    const productFeaturePairs = products.map((product) => {
        const productId = (0, prestashopFields_1.toNumber)(product.id) ?? 0;
        const features = product.associations?.product_features
            ? (0, prestashopParser_1.extractResourceList)("product_features", product.associations)
            : [];
        return {
            productId,
            features: features.map((feature) => ({
                featureId: (0, prestashopFields_1.toNumber)(feature.id),
                featureValueId: (0, prestashopFields_1.toNumber)(feature.id_feature_value),
            })).filter((feature) => feature.featureId && feature.featureValueId),
        };
    });
    const featureIds = Array.from(new Set(productFeaturePairs.flatMap((entry) => entry.features.map((feature) => feature.featureId))));
    const featureValueIds = Array.from(new Set(productFeaturePairs.flatMap((entry) => entry.features.map((feature) => feature.featureValueId))));
    const featureNames = new Map();
    const featureValues = new Map();
    if (featureIds.length > 0) {
        const payload = await client.get("product_features", {
            "filter[id]": `[${featureIds.join("|")}]`,
            display: "full",
            limit: featureIds.length.toString(),
        });
        const items = (0, prestashopParser_1.extractResourceList)("product_features", payload);
        items.forEach((item) => {
            const id = (0, prestashopFields_1.toNumber)(item.id);
            const name = (0, prestashopFields_1.getLocalizedValue)(item.name, lang);
            if (id && name) {
                featureNames.set(id, name);
            }
        });
    }
    if (featureValueIds.length > 0) {
        const payload = await client.get("product_feature_values", {
            "filter[id]": `[${featureValueIds.join("|")}]`,
            display: "full",
            limit: featureValueIds.length.toString(),
        });
        const items = (0, prestashopParser_1.extractResourceList)("product_feature_values", payload);
        items.forEach((item) => {
            const id = (0, prestashopFields_1.toNumber)(item.id);
            const value = (0, prestashopFields_1.getLocalizedValue)(item.value, lang);
            if (id && value) {
                featureValues.set(id, value);
            }
        });
    }
    const attributeLookup = new Map();
    productFeaturePairs.forEach((entry) => {
        const attributes = {};
        entry.features.forEach((feature) => {
            const name = feature.featureId ? featureNames.get(feature.featureId) : null;
            const value = feature.featureValueId ? featureValues.get(feature.featureValueId) : null;
            if (name && value) {
                pushAttributeValue(attributes, name, value);
            }
        });
        attributeLookup.set(entry.productId, attributes);
    });
    const productCombinationPairs = products.map((product) => ({
        productId: (0, prestashopFields_1.toNumber)(product.id) ?? 0,
        combinationIds: product.associations?.combinations
            ? (0, prestashopParser_1.extractResourceList)("combinations", product.associations)
                .map((combination) => (0, prestashopFields_1.toNumber)(combination.id))
                .filter((id) => id !== null)
            : [],
    }));
    const combinationIds = Array.from(new Set(productCombinationPairs.flatMap((entry) => entry.combinationIds)));
    if (combinationIds.length === 0) {
        return attributeLookup;
    }
    const combinationsPayload = await client.get("combinations", {
        "filter[id]": `[${combinationIds.join("|")}]`,
        display: "full",
        limit: combinationIds.length.toString(),
    });
    const combinations = (0, prestashopParser_1.extractResourceList)("combinations", combinationsPayload);
    const optionValueIds = Array.from(new Set(combinations.flatMap((combination) => combination.associations?.product_option_values
        ? (0, prestashopParser_1.extractResourceList)("product_option_values", combination.associations)
            .map((optionValue) => (0, prestashopFields_1.toNumber)(optionValue.id))
            .filter((id) => id !== null)
        : [])));
    if (optionValueIds.length === 0) {
        return attributeLookup;
    }
    const optionValuesPayload = await client.get("product_option_values", {
        "filter[id]": `[${optionValueIds.join("|")}]`,
        display: "full",
        limit: optionValueIds.length.toString(),
    });
    const optionValues = (0, prestashopParser_1.extractResourceList)("product_option_values", optionValuesPayload);
    const optionGroupIds = Array.from(new Set(optionValues
        .map((optionValue) => (0, prestashopFields_1.toNumber)(optionValue.id_attribute_group))
        .filter((id) => id !== null)));
    const optionGroupNames = new Map();
    if (optionGroupIds.length > 0) {
        const optionGroupsPayload = await client.get("product_options", {
            "filter[id]": `[${optionGroupIds.join("|")}]`,
            display: "full",
            limit: optionGroupIds.length.toString(),
        });
        const optionGroups = (0, prestashopParser_1.extractResourceList)("product_options", optionGroupsPayload);
        optionGroups.forEach((optionGroup) => {
            const id = (0, prestashopFields_1.toNumber)(optionGroup.id);
            const name = (0, prestashopFields_1.getLocalizedValue)(optionGroup.public_name, lang) ??
                (0, prestashopFields_1.getLocalizedValue)(optionGroup.name, lang);
            if (id && name) {
                optionGroupNames.set(id, name);
            }
        });
    }
    const optionValueLookup = new Map();
    optionValues.forEach((optionValue) => {
        const id = (0, prestashopFields_1.toNumber)(optionValue.id);
        const groupId = (0, prestashopFields_1.toNumber)(optionValue.id_attribute_group);
        const value = (0, prestashopFields_1.getLocalizedValue)(optionValue.name, lang);
        const groupName = groupId ? optionGroupNames.get(groupId) : null;
        if (id && groupName && value) {
            optionValueLookup.set(id, { groupName, value });
        }
    });
    const combinationOptionValues = new Map();
    combinations.forEach((combination) => {
        const combinationId = (0, prestashopFields_1.toNumber)(combination.id);
        if (!combinationId) {
            return;
        }
        const ids = combination.associations?.product_option_values
            ? (0, prestashopParser_1.extractResourceList)("product_option_values", combination.associations)
                .map((optionValue) => (0, prestashopFields_1.toNumber)(optionValue.id))
                .filter((id) => id !== null)
            : [];
        combinationOptionValues.set(combinationId, ids);
    });
    productCombinationPairs.forEach((entry) => {
        const attributes = attributeLookup.get(entry.productId) ?? {};
        entry.combinationIds.forEach((combinationId) => {
            const ids = combinationOptionValues.get(combinationId) ?? [];
            ids.forEach((optionValueId) => {
                const option = optionValueLookup.get(optionValueId);
                if (option) {
                    pushAttributeValue(attributes, option.groupName, option.value);
                }
            });
        });
        attributeLookup.set(entry.productId, attributes);
    });
    return attributeLookup;
};
const listAllProducts = async (client, shopId, page, pageSize, sort, lang, allowPrice = true) => {
    const data = await client.get("products", {
        "filter[active]": 1,
        sort,
        limit: (0, pagination_1.toLimitParam)(page, pageSize),
        display: "full"
    });
    const items = (0, prestashopParser_1.extractResourceList)("products", data);
    const attributeLookup = await buildProductAttributeLookup(client, items, lang);
    return items.map((product) => normalizeProduct(product, shopId, lang, allowPrice, attributeLookup.get(Number(product.id)) ?? {}));
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
    const attributeLookup = await buildProductAttributeLookup(client, finalItems, lang);
    return finalItems.map((product) => normalizeProduct(product, shopId, lang, allowPrice, attributeLookup.get(Number(product.id)) ?? {}));
};
exports.listProductsByCategory = listProductsByCategory;
const listFacetProductsByCategory = async (client, shopId, categoryId, lang, allowPrice = true) => {
    return (0, exports.listProductsByCategory)(client, shopId, categoryId, 1, 1000, "[id_DESC]", lang, allowPrice);
};
exports.listFacetProductsByCategory = listFacetProductsByCategory;
const countByValue = (values) => {
    const counts = new Map();
    values.forEach((value) => {
        counts.set(value, (counts.get(value) ?? 0) + 1);
    });
    return counts;
};
const colorAliases = ["color", "colour", "χρώμα", "χρωματισμός", "χρωματισμοί", "färg"];
const isColorAttributeKey = (key) => {
    const normalized = key.trim().toLowerCase();
    return colorAliases.some((alias) => normalized.includes(alias));
};
const buildCatalogFacets = (products, lang) => {
    const facets = [];
    const colorValues = products.flatMap((product) => Object.entries(product.attributes)
        .filter(([key]) => isColorAttributeKey(key))
        .flatMap(([, values]) => values));
    const colorCounts = countByValue(colorValues);
    if (colorCounts.size > 0) {
        facets.push({
            key: "colors",
            title: lang === 2 ? "Χρωματισμοί" : "Colors",
            type: "color",
            options: Array.from(colorCounts.entries())
                .map(([value, count]) => ({
                key: value,
                label: value,
                count,
            }))
                .sort((a, b) => a.label.localeCompare(b.label)),
        });
    }
    const attributeMap = new Map();
    products.forEach((product) => {
        Object.entries(product.attributes).forEach(([key, values]) => {
            if (key.toLowerCase() === "reference" || isColorAttributeKey(key)) {
                return;
            }
            attributeMap.set(key, [...(attributeMap.get(key) ?? []), ...values]);
        });
    });
    Array.from(attributeMap.entries())
        .sort((a, b) => a[0].localeCompare(b[0]))
        .forEach(([key, values]) => {
        const counts = countByValue(values);
        if (counts.size === 0) {
            return;
        }
        facets.push({
            key,
            title: key,
            type: "attribute",
            options: Array.from(counts.entries())
                .map(([value, count]) => ({ key: value, label: value, count }))
                .sort((a, b) => a.label.localeCompare(b.label)),
        });
    });
    const prices = products.map((product) => product.price).filter((value) => value !== null);
    if (prices.length > 0) {
        facets.push({
            key: "price",
            title: lang === 2 ? "Τιμή" : "Price",
            type: "price",
            options: [],
            minValue: Math.min(...prices),
            maxValue: Math.max(...prices),
        });
    }
    return facets;
};
exports.buildCatalogFacets = buildCatalogFacets;
const getProductDetail = async (client, shopId, productId, lang, allowPrice = true) => {
    const data = await client.getById("products", productId, { display: "full" });
    const product = (0, prestashopParser_1.extractResourceItem)("products", data);
    if (!product)
        return null;
    const attributeLookup = await buildProductAttributeLookup(client, [product], lang);
    return normalizeProduct(product, shopId, lang, allowPrice, attributeLookup.get(Number(product.id)) ?? {});
};
exports.getProductDetail = getProductDetail;
