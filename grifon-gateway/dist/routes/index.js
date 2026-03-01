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
exports.apiRouter = (0, express_1.Router)();
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
// LOGIN ROUTE (V1)
exports.apiRouter.post("/v1/auth/login", authRateLimiter, (0, validate_1.validateBody)(schemas_1.loginBodySchema), async (req, res, next) => {
    try {
        const response = await (0, authService_1.loginCustomer)(req.body);
        res.json(response);
    }
    catch (error) {
        next(error);
    }
});
// REGISTER ROUTE (V1)
exports.apiRouter.post("/v1/auth/register", authRateLimiter, (0, validate_1.validateBody)(schemas_1.registerBodySchema), async (req, res, next) => {
    try {
        const response = await (0, authService_1.registerCustomer)(req.body);
        res.status(201).json(response);
    }
    catch (error) {
        next(error);
    }
});
// ALL PRODUCTS ROUTE
exports.apiRouter.get("/v1/products", (0, validate_1.validateQuery)(schemas_1.shopQuerySchema.merge(schemas_1.productPaginationSchema)), async (req, res, next) => {
    try {
        const { shopId, lang, page, pageSize, sort, search, minPrice, maxPrice, inStockOnly } = req.query;
        const client = new PrestaShopClient_1.PrestaShopClient({ shopId, lang });
        const items = await (0, productService_1.listAllProducts)(client, shopId, page, pageSize, sort, lang, true, {
            search,
            minPrice,
            maxPrice,
            inStockOnly
        });
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
exports.apiRouter.get("/v1/categories/:categoryId/products", (0, validate_1.validateParams)(schemas_1.categoryIdSchema), (0, validate_1.validateQuery)(schemas_1.shopQuerySchema.merge(schemas_1.productPaginationSchema)), async (req, res, next) => {
    try {
        const { shopId, lang, page, pageSize, sort, search, minPrice, maxPrice, inStockOnly } = req.query;
        const { categoryId } = req.params;
        const client = new PrestaShopClient_1.PrestaShopClient({ shopId, lang });
        const items = await (0, productService_1.listProductsByCategory)(client, shopId, Number(categoryId), page, pageSize, sort, lang, true, {
            search,
            minPrice,
            maxPrice,
            inStockOnly
        });
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
        const item = await (0, productService_1.getProductDetail)(client, shopId, Number(productId), lang, true);
        res.json(item);
    }
    catch (error) {
        next(error);
    }
});
exports.default = exports.apiRouter;
