"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.extractResourceItem = exports.extractResourceList = void 0;
const resourceMap = {
    categories: "category",
    products: "product",
    customers: "customer",
    groups: "group",
    content_management_system: "content_management_system",
    images: "image",
    stock_availables: "stock_available"
};
const asArray = (value) => {
    if (!value)
        return [];
    return Array.isArray(value) ? value : [value];
};
const extractResourceList = (resource, payload) => {
    const root = payload?.prestashop ?? payload;
    // Προσπάθεια εύρεσης στον πληθυντικό (π.χ. payload.categories.category)
    const container = root?.[resource];
    if (container) {
        const itemKey = resourceMap[resource];
        if (itemKey && container[itemKey]) {
            return asArray(container[itemKey]);
        }
        if (Array.isArray(container))
            return container;
    }
    // Προσπάθεια εύρεσης στον ενικό (π.χ. payload.category) - για getById
    const singularKey = resourceMap[resource];
    if (singularKey && root?.[singularKey]) {
        return asArray(root[singularKey]);
    }
    return [];
};
exports.extractResourceList = extractResourceList;
const extractResourceItem = (resource, payload) => {
    const list = (0, exports.extractResourceList)(resource, payload);
    return list.length > 0 ? list[0] : null;
};
exports.extractResourceItem = extractResourceItem;
