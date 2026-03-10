import { Router } from "express";
import rateLimit from "express-rate-limit";
import axios from "axios";
import { config, shops } from "../config/env";
import { validateQuery, validateParams, validateBody } from "../middleware/validate";
import {
  categoryIdSchema,
  customerIdSchema,
  loginBodySchema,
  paginationSchema,
  productIdSchema,
  productPaginationSchema,
  registerBodySchema,
  shopQuerySchema
} from "./schemas";
import { PrestaShopClient } from "../clients/PrestaShopClient";
import { listCategories } from "../services/categoryService";
import { listPages } from "../services/pageService";
import { listProductsByCategory, getProductDetail, listAllProducts } from "../services/productService";
import { listGroupsWithMembers } from "../services/groupService";
import { cache, buildCacheKey } from "../utils/cache";
import { getPriceAccess } from "../services/priceAccessService";
import { registerCustomer, loginCustomer } from "../services/authService";

export const apiRouter = Router();

const authRateLimiter = rateLimit({
  windowMs: 60 * 1000,
  limit: config.registerRateLimitPerMin,
  standardHeaders: true,
  legacyHeaders: false
});

/**
 * Ελέγχει αν ένας χρήστης δικαιούται να βλέπει τιμές (χονδρική).
 */
const checkPriceAccess = async (shopId: any, customerId?: number): Promise<boolean> => {
  if (!customerId) return false;
  try {
    const client = new PrestaShopClient({ shopId });
    const access = await getPriceAccess(client, customerId);
    return access.allowed;
  } catch {
    return false;
  }
};

apiRouter.get("/health", (_req, res) => {
  res.json({ ok: true });
});

apiRouter.get("/v1/shops", (_req, res) => {
  res.json(shops);
});

// IMAGE PROXY
apiRouter.get("/v1/images/products/:productId/:imageId", async (req, res, next) => {
  try {
    const { productId, imageId } = req.params;
    const shopId = (req.query.shopId as string) || "4";
    const base = config.shopBaseUrls[shopId as any];
    const key = config.prestashopApiKey;

    const response = await axios.get(`${base}/images/products/${productId}/${imageId}`, {
      auth: { username: key, password: "" },
      responseType: "stream"
    });

    res.setHeader("Content-Type", response.headers["content-type"]);
    response.data.pipe(res);
  } catch (error) {
    res.status(404).end();
  }
});

// LOGIN ROUTE (V1)
apiRouter.post(
  "/v1/auth/login",
  authRateLimiter,
  validateBody(loginBodySchema),
  async (req, res, next) => {
    try {
      const response = await loginCustomer(req.body as any);
      res.json(response);
    } catch (error) {
      next(error);
    }
  }
);

// REGISTER ROUTE (V1)
apiRouter.post(
  "/v1/auth/register",
  authRateLimiter,
  validateBody(registerBodySchema),
  async (req, res, next) => {
    try {
      const response = await registerCustomer(req.body as any);
      res.status(201).json(response);
    } catch (error) {
      next(error);
    }
  }
);

// ALL PRODUCTS ROUTE
apiRouter.get(
  "/v1/products",
  validateQuery(shopQuerySchema.merge(productPaginationSchema).merge(customerIdSchema.partial())),
  async (req, res, next) => {
    try {
      const { shopId, lang, page, pageSize, sort, search, minPrice, maxPrice, inStockOnly, customerId } = req.query as any;

      const allowPrice = await checkPriceAccess(shopId, customerId ? Number(customerId) : undefined);

      const client = new PrestaShopClient({ shopId, lang });
      const items = await listAllProducts(client, shopId, page, pageSize, sort, lang, allowPrice, {
        search,
        minPrice,
        maxPrice,
        inStockOnly
      });
      res.json({ page, pageSize, items });
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/categories",
  validateQuery(shopQuerySchema.merge(paginationSchema)),
  async (req, res, next) => {
    try {
      const { shopId, lang, page, pageSize } = req.query as any;
      const client = new PrestaShopClient({ shopId, lang });
      const { items, tree } = await listCategories(client, page, pageSize, lang);
      res.json({ page, pageSize, items, tree });
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/categories/:categoryId/products",
  validateParams(categoryIdSchema),
  validateQuery(shopQuerySchema.merge(productPaginationSchema).merge(customerIdSchema.partial())),
  async (req, res, next) => {
    try {
      const { shopId, lang, page, pageSize, sort, search, minPrice, maxPrice, inStockOnly, customerId } = req.query as any;
      const { categoryId } = req.params as any;

      const allowPrice = await checkPriceAccess(shopId, customerId ? Number(customerId) : undefined);

      const client = new PrestaShopClient({ shopId, lang });
      const items = await listProductsByCategory(client, shopId, Number(categoryId), page, pageSize, sort, lang, allowPrice, {
        search,
        minPrice,
        maxPrice,
        inStockOnly
      });
      res.json({ page, pageSize, items });
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

      const allowPrice = await checkPriceAccess(shopId, customerId ? Number(customerId) : undefined);

      const client = new PrestaShopClient({ shopId, lang });
      const item = await getProductDetail(client, shopId, Number(productId), lang, allowPrice);
      res.json(item);
    } catch (error) {
      next(error);
    }
  }
);

export default apiRouter;
