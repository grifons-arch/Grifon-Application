"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.apiRouter = void 0;
const express_1 = require("express");
const express_rate_limit_1 = __importDefault(require("express-rate-limit"));
const axios_1 = __importDefault(require("axios"));
const env_1 = require("../config/env");
const validate_1 = require("../middleware/validate");
const schemas_1 = require("./schemas");
const PrestaShopClient_1 = require("../clients/PrestaShopClient");
const categoryService_1 = require("../services/categoryService");
const productService_1 = require("../services/productService");
const authService_1 = require("../services/authService");
const priceAccessService_1 = require("../services/priceAccessService");
const wholesaleCustomerService_1 = require("../services/wholesaleCustomerService");
const customerService_1 = require("../services/customerService");
exports.apiRouter = (0, express_1.Router)();
const parseCsvValues = (value) => {
    if (typeof value !== "string") {
        return [];
    }
    return value
        .split(",")
        .map((item) => item.trim())
        .filter(Boolean);
};
const parseAttributeFilters = (value) => {
    if (typeof value !== "string" || value.trim() === "") {
        return {};
    }
    try {
        const parsed = JSON.parse(value);
        return Object.entries(parsed).reduce((acc, [key, rawValue]) => {
            if (Array.isArray(rawValue)) {
                const values = rawValue
                    .map((item) => (typeof item === "string" ? item.trim() : ""))
                    .filter(Boolean);
                if (values.length > 0) {
                    acc[key] = values;
                }
            }
            return acc;
        }, {});
    }
    catch {
        return {};
    }
};
const toBasicProductFilters = (query) => ({
    search: typeof query.search === "string" ? query.search.trim() : undefined,
    priceMin: typeof query.priceMin === "number" ? query.priceMin : undefined,
    priceMax: typeof query.priceMax === "number" ? query.priceMax : undefined,
    colors: parseCsvValues(query.colors),
    attributeFilters: parseAttributeFilters(query.attributes)
});
const resolveAllowPrice = async (client, customerId) => {
    if (!customerId)
        return false;
    const access = await (0, priceAccessService_1.getPriceAccess)(client, customerId);
    return access.allowed;
};
const authRateLimiter = (0, express_rate_limit_1.default)({
    windowMs: 60 * 1000,
    limit: env_1.config.registerRateLimitPerMin,
    standardHeaders: true,
    legacyHeaders: false
});
exports.apiRouter.get("/health", (_req, res) => {
    res.json({ ok: true });
});
exports.apiRouter.get("/v1/shops", (_req, res) => {
    res.json(env_1.shops);
});
// LOGIN ROUTE
exports.apiRouter.post("/v1/auth/login", authRateLimiter, (0, validate_1.validateBody)(schemas_1.loginBodySchema), async (req, res, next) => {
    try {
        const { email, password, countryIso } = req.body;
        const result = await (0, authService_1.loginCustomer)(email, password, countryIso);
        res.json(result);
    }
    catch (error) {
        next(error);
    }
});
// REGISTER ROUTE
exports.apiRouter.post("/v1/auth/register", authRateLimiter, (0, validate_1.validateBody)(schemas_1.registerBodySchema), async (req, res, next) => {
    try {
        const response = await (0, authService_1.registerCustomer)(req.body);
        res.status(201).json(response);
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.post("/v1/auth/wholesale-application", authRateLimiter, (0, validate_1.validateBody)(schemas_1.wholesaleApplicationBodySchema), async (req, res, next) => {
    try {
        const response = await (0, authService_1.submitWholesaleApplication)(req.body);
        res.status(201).json(response);
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.post("/v1/customer-activity/favorites", (0, validate_1.validateBody)(schemas_1.productActivityBodySchema), async (req, res, next) => {
    try {
        const result = await (0, authService_1.syncFavoriteProduct)(req.body);
        res.json(result);
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.post("/v1/customer-activity/recent-products", (0, validate_1.validateBody)(schemas_1.productActivityBodySchema.omit({ isFavorite: true })), async (req, res, next) => {
    try {
        const result = await (0, authService_1.recordRecentProduct)(req.body);
        res.json(result);
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.get("/v1/customer-activity/favorites", (0, validate_1.validateQuery)(schemas_1.customerActivityQuerySchema), async (req, res, next) => {
    try {
        const { customerId, shopId } = req.query;
        const result = await (0, authService_1.listFavoriteProducts)({
            customerId: Number(customerId),
            shopId: Number(shopId),
        });
        res.json(result);
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.get("/v1/customer-activity/recent-products", (0, validate_1.validateQuery)(schemas_1.customerActivityQuerySchema), async (req, res, next) => {
    try {
        const { customerId, shopId, limit } = req.query;
        const result = await (0, authService_1.listRecentProducts)({
            customerId: Number(customerId),
            shopId: Number(shopId),
            limit: limit ? Number(limit) : undefined,
        });
        res.json(result);
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.post("/v1/customer-activity/clear", (0, validate_1.validateBody)(schemas_1.customerActivityClearBodySchema), async (req, res, next) => {
    try {
        const { shopId } = req.body;
        const result = await (0, authService_1.clearActivityTables)({
            shopId: Number(shopId),
        });
        res.json(result);
    }
    catch (error) {
        next(error);
    }
});
// IMAGE PROXY
exports.apiRouter.get("/v1/images/products/:productId/:imageId", async (req, res, next) => {
    try {
        const { productId, imageId } = req.params;
        const shopId = req.query.shopId || "4";
        const base = env_1.config.shopBaseUrls[shopId];
        const key = env_1.config.prestashopApiKey;
        const response = await axios_1.default.get(`${base}/images/products/${productId}/${imageId}`, {
            auth: { username: key, password: "" },
            responseType: "stream"
        });
        res.setHeader("Content-Type", response.headers["content-type"]);
        response.data.pipe(res);
    }
    catch (error) {
        res.status(404).end();
    }
});
// ALL PRODUCTS ROUTE
exports.apiRouter.get("/v1/products", (0, validate_1.validateQuery)(schemas_1.shopQuerySchema
    .merge(schemas_1.productPaginationSchema)
    .merge(schemas_1.productFilterQuerySchema)
    .merge(schemas_1.customerIdSchema.partial())), async (req, res, next) => {
    try {
        const { shopId, lang, page, pageSize, sort, customerId } = req.query;
        const client = new PrestaShopClient_1.PrestaShopClient({ shopId, lang });
        const allowPrice = await resolveAllowPrice(client, customerId ? Number(customerId) : undefined);
        const items = await (0, productService_1.listAllProducts)(client, shopId, page, pageSize, sort, lang, allowPrice, toBasicProductFilters(req.query));
        res.json({ page, pageSize, items });
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.get("/v1/categories", (0, validate_1.validateQuery)(schemas_1.shopQuerySchema.merge(schemas_1.paginationSchema)), async (req, res, next) => {
    try {
        const { shopId, lang, page, pageSize } = req.query;
        const client = new PrestaShopClient_1.PrestaShopClient({ shopId, lang });
        const { items, tree } = await (0, categoryService_1.listCategories)(client, page, pageSize, lang);
        res.json({ page, pageSize, items, tree });
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.get("/v1/categories/:categoryId/filters", (0, validate_1.validateParams)(schemas_1.categoryIdSchema), (0, validate_1.validateQuery)(schemas_1.shopQuerySchema.merge(schemas_1.customerIdSchema.partial())), async (req, res, next) => {
    try {
        const { shopId, lang, customerId } = req.query;
        const { categoryId } = req.params;
        const client = new PrestaShopClient_1.PrestaShopClient({ shopId, lang });
        const allowPrice = await resolveAllowPrice(client, customerId ? Number(customerId) : undefined);
        const items = await (0, productService_1.listFacetProductsByCategory)(client, shopId, Number(categoryId), lang, allowPrice);
        const facets = (0, productService_1.buildCatalogFacets)(items, lang);
        res.json({ items: facets });
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.get("/v1/categories/:categoryId/products", (0, validate_1.validateParams)(schemas_1.categoryIdSchema), (0, validate_1.validateQuery)(schemas_1.shopQuerySchema
    .merge(schemas_1.productPaginationSchema)
    .merge(schemas_1.productFilterQuerySchema)
    .merge(schemas_1.customerIdSchema.partial())), async (req, res, next) => {
    try {
        const { shopId, lang, page, pageSize, sort, customerId } = req.query;
        const { categoryId } = req.params;
        const client = new PrestaShopClient_1.PrestaShopClient({ shopId, lang });
        const allowPrice = await resolveAllowPrice(client, customerId ? Number(customerId) : undefined);
        const items = await (0, productService_1.listProductsByCategory)(client, shopId, Number(categoryId), page, pageSize, sort, lang, allowPrice, toBasicProductFilters(req.query));
        res.json({ page, pageSize, items });
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.get("/v1/products/:productId", (0, validate_1.validateParams)(schemas_1.productIdSchema), (0, validate_1.validateQuery)(schemas_1.shopQuerySchema.merge(schemas_1.customerIdSchema.partial())), async (req, res, next) => {
    try {
        const { shopId, lang, customerId } = req.query;
        const { productId } = req.params;
        const client = new PrestaShopClient_1.PrestaShopClient({ shopId, lang });
        const allowPrice = await resolveAllowPrice(client, customerId ? Number(customerId) : undefined);
        const item = await (0, productService_1.getProductDetail)(client, shopId, Number(productId), lang, allowPrice);
        res.json(item);
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.get("/v1/wholesale-customers", (0, validate_1.validateQuery)(schemas_1.shopQuerySchema), async (req, res, next) => {
    try {
        const { shopId, lang } = req.query;
        const client = new PrestaShopClient_1.PrestaShopClient({ shopId, lang });
        const items = await (0, wholesaleCustomerService_1.listWholesaleCustomers)(client, Number(shopId), lang);
        res.json({ items });
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.get("/v1/customers", (0, validate_1.validateQuery)(schemas_1.shopQuerySchema), async (req, res, next) => {
    try {
        const { shopId, lang } = req.query;
        const client = new PrestaShopClient_1.PrestaShopClient({ shopId, lang });
        const items = await (0, customerService_1.listCustomers)(client, Number(shopId), lang);
        res.json({ items });
    }
    catch (error) {
        next(error);
    }
});
// DEBUG: List Wholesale Applications
exports.apiRouter.get("/v1/debug/wholesale-applications", async (req, res, next) => {
    try {
        const countryIso = req.query.countryIso || "GR";
        const result = await (0, authService_1.debugListWholesaleApplications)(countryIso);
        res.json(result);
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.get("/v1/debug/ets-wholesale/application/:customerId", (0, validate_1.validateParams)(schemas_1.customerIdSchema), async (req, res, next) => {
    try {
        const { customerId } = req.params;
        const countryIso = req.query.countryIso || "GR";
        const result = await (0, authService_1.debugInspectEtsWholesaleApplication)(Number(customerId), countryIso);
        res.json(result);
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.get("/v1/debug/ets-wholesale/application-visibility/:customerId", (0, validate_1.validateParams)(schemas_1.customerIdSchema), async (req, res, next) => {
    try {
        const { customerId } = req.params;
        const countryIso = req.query.countryIso || "GR";
        const result = await (0, authService_1.debugInspectEtsWholesaleApplicationVisibility)(Number(customerId), countryIso);
        res.json(result);
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.get("/v1/debug/ets-wholesale/form-fields", (0, validate_1.validateQuery)(schemas_1.etsWholesaleFormFieldsQuerySchema), async (req, res, next) => {
    try {
        const { formType } = req.query;
        const countryIso = req.query.countryIso || "GR";
        const result = await (0, authService_1.debugInspectEtsWholesaleFormFields)(formType, countryIso);
        res.json(result);
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.get("/v1/debug/modules/:moduleName", (0, validate_1.validateParams)(schemas_1.moduleNameSchema), async (req, res, next) => {
    try {
        const { moduleName } = req.params;
        const countryIso = req.query.countryIso || "GR";
        const result = await (0, authService_1.debugInspectPrestaShopModule)(moduleName, countryIso);
        res.json(result);
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.get("/v1/debug/tables/:tableName", (0, validate_1.validateParams)(schemas_1.tableNameSchema), async (req, res, next) => {
    try {
        const { tableName } = req.params;
        const countryIso = req.query.countryIso || "GR";
        const result = await (0, authService_1.debugInspectPrestaShopTable)(tableName, countryIso);
        res.json(result);
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.get("/v1/debug/modules/:moduleName/search", (0, validate_1.validateParams)(schemas_1.moduleNameSchema), (0, validate_1.validateQuery)(schemas_1.moduleSearchQuerySchema), async (req, res, next) => {
    try {
        const { moduleName } = req.params;
        const { pattern } = req.query;
        const countryIso = req.query.countryIso || "GR";
        const result = await (0, authService_1.debugSearchPrestaShopModuleCode)(moduleName, pattern, countryIso);
        res.json(result);
    }
    catch (error) {
        next(error);
    }
});
exports.apiRouter.get("/v1/debug/modules/:moduleName/file", (0, validate_1.validateParams)(schemas_1.moduleNameSchema), (0, validate_1.validateQuery)(schemas_1.moduleFileQuerySchema), async (req, res, next) => {
    try {
        const { moduleName } = req.params;
        const { path, start, lines } = req.query;
        const countryIso = req.query.countryIso || "GR";
        const result = await (0, authService_1.debugReadPrestaShopModuleFile)(moduleName, path, Number(start), Number(lines), countryIso);
        res.json(result);
    }
    catch (error) {
        next(error);
    }
});
exports.default = exports.apiRouter;
