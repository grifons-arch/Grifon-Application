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
    const quantity = (0, prestashopFields_1.toNumber)(product.quantity);
    const parsedQuantity = typeof quantity === "number" ? quantity : null;
    const brandName = typeof product.manufacturer_name === "string" ? product.manufacturer_name : null;
    return {
        id,
        name: (0, prestashopFields_1.getLocalizedValue)(product.name, lang),
        price: allowPrice ? (0, prestashopFields_1.toNumber)(product.price) : null,
        reference: product.reference ?? null,
        brand: brandName,
        quantity: parsedQuantity,
        inStock: parsedQuantity === null ? true : parsedQuantity > 0,
        defaultImage: idImage ? { id: idImage, url: buildImageUrl(shopId, id, idImage) } : null,
        active: (0, prestashopFields_1.toNumber)(product.active)
    };
};
const applyServerSideFilters = (items, filters) => {
    const normalizedSearch = filters.search?.trim().toLowerCase();
    return items.filter((item) => {
        const matchesSearch = !normalizedSearch ||
            item.name?.toLowerCase().includes(normalizedSearch) ||
            item.reference?.toLowerCase().includes(normalizedSearch);
        const matchesMinPrice = filters.minPrice === undefined || (item.price !== null && item.price >= filters.minPrice);
        const matchesMaxPrice = filters.maxPrice === undefined || (item.price !== null && item.price <= filters.maxPrice);
        const matchesStock = !filters.inStockOnly || item.inStock;
        return Boolean(matchesSearch && matchesMinPrice && matchesMaxPrice && matchesStock);
    });
};
const listAllProducts = async (client, shopId, page, pageSize, sort, lang, allowPrice = true, filters = {}) => {
    const data = await client.get("products", {
        "filter[active]": 1,
        sort,
        limit: (0, pagination_1.toLimitParam)(page, pageSize),
        display: "full",
        ...(filters.search ? { "filter[name]": `%${filters.search}%` } : {}),
        ...(filters.minPrice !== undefined || filters.maxPrice !== undefined
            ? {
                "filter[price]": `[${filters.minPrice ?? ""},${filters.maxPrice ?? ""}]`
            }
            : {})
    });
    const items = (0, prestashopParser_1.extractResourceList)("products", data);
    const normalized = items.map((product) => normalizeProduct(product, shopId, lang, allowPrice));
    return applyServerSideFilters(normalized, filters);
};
exports.listAllProducts = listAllProducts;
const listProductsByCategory = async (client, shopId, categoryId, page, pageSize, sort, lang, allowPrice = true, filters = {}) => {
    if (categoryId === 2) {
        return (0, exports.listAllProducts)(client, shopId, page, pageSize, sort, lang, allowPrice, filters);
    }
    const data = await client.get("products", {
        "filter[active]": 1,
        "filter[id_category_default]": categoryId,
        sort,
        limit: (0, pagination_1.toLimitParam)(page, pageSize),
        display: "full",
        ...(filters.search ? { "filter[name]": `%${filters.search}%` } : {}),
        ...(filters.minPrice !== undefined || filters.maxPrice !== undefined
            ? {
                "filter[price]": `[${filters.minPrice ?? ""},${filters.maxPrice ?? ""}]`
            }
            : {})
    });
    const items = (0, prestashopParser_1.extractResourceList)("products", data);
    const normalized = items.map((product) => normalizeProduct(product, shopId, lang, allowPrice));
    return applyServerSideFilters(normalized, filters);
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
