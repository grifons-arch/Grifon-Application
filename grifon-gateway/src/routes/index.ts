import { Router } from "express";
import { config, shops } from "../config/env";
import { validateQuery, validateParams } from "../middleware/validate";
import {
  categoryIdSchema,
  customerIdSchema,
  paginationSchema,
  productIdSchema,
  productPaginationSchema,
  shopQuerySchema
} from "./schemas";
import { PrestaShopClient } from "../clients/PrestaShopClient";
import { listCategories } from "../services/categoryService";
import { listPages } from "../services/pageService";
import { listProductsByCategory, getProductDetail } from "../services/productService";
import { listGroupsWithMembers } from "../services/groupService";
import { cache, buildCacheKey } from "../utils/cache";
import { getPriceAccess } from "../services/priceAccessService";

export const apiRouter = Router();

apiRouter.get("/health", (_req, res) => {
  res.json({ ok: true });
});

apiRouter.get("/v1/shops", (_req, res) => {
  res.json(shops);
});

apiRouter.get(
  "/v1/categories",
  validateQuery(shopQuerySchema.merge(paginationSchema)),
  async (req, res, next) => {
    try {
      const { shopId, lang, page, pageSize } = req.query as any;
      const cacheKey = buildCacheKey({
        route: "categories",
        shopId,
        lang,
        page,
        pageSize
      });
      const cached = await cache.get(cacheKey);
      if (cached) {
        res.json(cached);
        return;
      }

      const client = new PrestaShopClient({ shopId, lang });
      const { items, tree } = await listCategories(client, page, pageSize, lang);
      const response = { page, pageSize, items, tree };
      await cache.set(cacheKey, response, config.cacheTtlCategoriesSeconds);
      res.json(response);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/categories/:categoryId/products",
  validateParams(categoryIdSchema),
  validateQuery(shopQuerySchema.merge(productPaginationSchema)),
  async (req, res, next) => {
    try {
      const { shopId, lang, page, pageSize, sort } = req.query as any;
      const { categoryId } = req.params as any;

      const cacheKey = buildCacheKey({
        route: `categories/${categoryId}/products`,
        shopId,
        lang,
        page,
        pageSize,
        sort
      });
      const cached = await cache.get(cacheKey);
      if (cached) {
        res.json(cached);
        return;
      }

      const client = new PrestaShopClient({ shopId, lang });
      const items = await listProductsByCategory(
        client,
        shopId,
        Number(categoryId),
        page,
        pageSize,
        sort,
        lang
      );
      const response = { page, pageSize, items };
      await cache.set(cacheKey, response, config.cacheTtlProductsSeconds);
      res.json(response);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/products/:productId",
  validateParams(productIdSchema),
  validateQuery(shopQuerySchema.merge(customerIdSchema.partial())),
  async (req, res, next) => {
    try {
      const { shopId, lang, customerId } = req.query as any;
      const { productId } = req.params as any;

      const cacheKey = buildCacheKey({
        route: `products/${productId}`,
        shopId,
        lang,
        customerId
      });
      const cached = await cache.get(cacheKey);
      if (cached) {
        res.json(cached);
        return;
      }

      const client = new PrestaShopClient({ shopId, lang });
      let allowPrice = false;
      if (customerId) {
        const access = await getPriceAccess(client, Number(customerId));
        allowPrice = access.allowed;
      }
      const item = await getProductDetail(client, shopId, Number(productId), lang, allowPrice);
      if (!item) {
        res.status(404).json({
          error: { code: "NOT_FOUND", message: "Product not found", details: { productId } }
        });
        return;
      }
      await cache.set(cacheKey, item, config.cacheTtlProductsSeconds);
      res.json(item);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/pages",
  validateQuery(shopQuerySchema),
  async (req, res, next) => {
    try {
      const { shopId, lang } = req.query as any;
      const cacheKey = buildCacheKey({ route: "pages", shopId, lang });
      const cached = await cache.get(cacheKey);
      if (cached) {
        res.json(cached);
        return;
      }

      const client = new PrestaShopClient({ shopId, lang });
      const items = await listPages(client, lang);
      const response = { items };
      await cache.set(cacheKey, response, config.cacheTtlCategoriesSeconds);
      res.json(response);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/customer-groups",
  validateQuery(shopQuerySchema),
  async (req, res, next) => {
    try {
      const { shopId, lang } = req.query as any;
      const client = new PrestaShopClient({ shopId, lang });
      const items = await listGroupsWithMembers(client, lang);
      res.json({ items });
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/customers/:customerId/price-access",
  validateParams(customerIdSchema),
  validateQuery(shopQuerySchema),
  async (req, res, next) => {
    try {
      const { shopId, lang } = req.query as any;
      const { customerId } = req.params as any;
      const client = new PrestaShopClient({ shopId, lang });
      const result = await getPriceAccess(client, Number(customerId));
      res.json(result);
    } catch (error) {
      next(error);
    }
  }
);
