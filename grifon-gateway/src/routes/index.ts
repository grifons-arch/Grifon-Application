import { Router } from "express";
import rateLimit from "express-rate-limit";
import axios from "axios";
import { config, shops } from "../config/env";
import { validateQuery, validateParams, validateBody } from "../middleware/validate";
import {
  customerActivityClearBodySchema,
  categoryIdSchema,
  checkoutOrderBodySchema,
  checkoutHandoffBodySchema,
  checkoutSessionQuerySchema,
  customerActivityQuerySchema,
  customerIdSchema,
  etsWholesaleFormFieldsQuerySchema,
  loginBodySchema,
  moduleFileQuerySchema,
  moduleNameSchema,
  moduleSearchQuerySchema,
  paginationSchema,
  productActivityBodySchema,
  productFilterQuerySchema,
  productIdSchema,
  productPaginationSchema,
  registerBodySchema,
  shopQuerySchema,
  tableNameSchema,
  wholesaleApplicationBodySchema
} from "./schemas";
import { PrestaShopClient } from "../clients/PrestaShopClient";
import { listCategories } from "../services/categoryService";
import {
  BasicProductFilters,
  listProductsByCategory,
  getProductDetail,
  listAllProducts,
  listFacetProductsByCategory,
  buildCatalogFacets
} from "../services/productService";
import {
  registerCustomer,
  submitWholesaleApplication,
  loginCustomer,
  syncFavoriteProduct,
  recordRecentProduct,
  listFavoriteProducts,
  listRecentProducts,
  clearActivityTables,
  debugListWholesaleApplications,
  debugInspectEtsWholesaleApplication,
  debugInspectEtsWholesaleApplicationVisibility,
  debugInspectEtsWholesaleFormFields,
  debugReadPrestaShopModuleFile,
  debugInspectPrestaShopModule,
  debugInspectPrestaShopTable,
  debugSearchPrestaShopModuleCode
} from "../services/authService";
import { getPriceAccess } from "../services/priceAccessService";
import { listWholesaleCustomers } from "../services/wholesaleCustomerService";
import { listCustomers } from "../services/customerService";
import { createCheckoutOrder, getCheckoutHandoffUrl, getCheckoutSession } from "../services/checkoutService";

export const apiRouter = Router();

const parseCsvValues = (value: unknown): string[] => {
  if (typeof value !== "string") {
    return [];
  }

  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
};

const parseAttributeFilters = (value: unknown): Record<string, string[]> => {
  if (typeof value !== "string" || value.trim() === "") {
    return {};
  }

  try {
    const parsed = JSON.parse(value) as Record<string, unknown>;
    return Object.entries(parsed).reduce<Record<string, string[]>>((acc, [key, rawValue]) => {
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
  } catch {
    return {};
  }
};

const toBasicProductFilters = (query: Record<string, unknown>): BasicProductFilters => ({
  search: typeof query.search === "string" ? query.search.trim() : undefined,
  priceMin: typeof query.priceMin === "number" ? query.priceMin : undefined,
  priceMax: typeof query.priceMax === "number" ? query.priceMax : undefined,
  colors: parseCsvValues(query.colors),
  attributeFilters: parseAttributeFilters(query.attributes)
});

const resolveAllowPrice = async (
  client: PrestaShopClient,
  customerId?: number
): Promise<boolean> => {
  if (!customerId) return false;
  const access = await getPriceAccess(client, customerId);
  return access.allowed;
};

const authRateLimiter = rateLimit({
  windowMs: 60 * 1000,
  limit: config.registerRateLimitPerMin,
  standardHeaders: true,
  legacyHeaders: false
});

apiRouter.get("/health", (_req, res) => {
  res.json({ ok: true });
});

apiRouter.get("/v1/shops", (_req, res) => {
  res.json(shops);
});

// LOGIN ROUTE
apiRouter.post(
  "/v1/auth/login",
  authRateLimiter,
  validateBody(loginBodySchema),
  async (req, res, next) => {
    try {
      const { email, password, countryIso } = req.body;
      const result = await loginCustomer(email, password, countryIso);
      res.json(result);
    } catch (error) {
      next(error);
    }
  }
);

// REGISTER ROUTE
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

apiRouter.post(
  "/v1/auth/wholesale-application",
  authRateLimiter,
  validateBody(wholesaleApplicationBodySchema),
  async (req, res, next) => {
    try {
      const response = await submitWholesaleApplication(req.body as any);
      res.status(201).json(response);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.post(
  "/v1/customer-activity/favorites",
  validateBody(productActivityBodySchema),
  async (req, res, next) => {
    try {
      const result = await syncFavoriteProduct(req.body as any);
      res.json(result);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.post(
  "/v1/customer-activity/recent-products",
  validateBody(productActivityBodySchema.omit({ isFavorite: true })),
  async (req, res, next) => {
    try {
      const result = await recordRecentProduct(req.body as any);
      res.json(result);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/customer-activity/favorites",
  validateQuery(customerActivityQuerySchema),
  async (req, res, next) => {
    try {
      const { customerId, shopId } = req.query as any;
      const result = await listFavoriteProducts({
        customerId: Number(customerId),
        shopId: Number(shopId),
      });
      res.json(result);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/customer-activity/recent-products",
  validateQuery(customerActivityQuerySchema),
  async (req, res, next) => {
    try {
      const { customerId, shopId, limit } = req.query as any;
      const result = await listRecentProducts({
        customerId: Number(customerId),
        shopId: Number(shopId),
        limit: limit ? Number(limit) : undefined,
      });
      res.json(result);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.post(
  "/v1/customer-activity/clear",
  validateBody(customerActivityClearBodySchema),
  async (req, res, next) => {
    try {
      const { shopId } = req.body as any;
      const result = await clearActivityTables({
        shopId: Number(shopId) as 1 | 4,
      });
      res.json(result);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/checkout/session",
  validateQuery(checkoutSessionQuerySchema),
  async (req, res, next) => {
    try {
      const { shopId, lang, customerId } = req.query as any;
      const client = new PrestaShopClient({ shopId, lang });
      const session = await getCheckoutSession(
        client,
        Number(shopId) as 1 | 4,
        customerId ? Number(customerId) : undefined
      );
      res.json(session);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.post(
  "/v1/checkout/orders",
  validateBody(checkoutOrderBodySchema),
  async (req, res, next) => {
    try {
      const { shopId, customerId, paymentMethodCode, shippingMethodCode, address, items } = req.body as any;
      const client = new PrestaShopClient({ shopId });
      const order = await createCheckoutOrder(client, {
        shopId: Number(shopId) as 1 | 4,
        customerId: Number(customerId),
        paymentMethodCode,
        shippingMethodCode,
        address,
        items,
      });
      res.status(201).json(order);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.post(
  "/v1/checkout/handoff",
  validateBody(checkoutHandoffBodySchema),
  async (req, res, next) => {
    try {
      const { shopId, customerId, target } = req.body as any;
      const resolvedTarget = customerId ? target : target === "checkout" ? "login" : target;
      res.json({
        ok: true,
        shopId,
        customerId: customerId ?? null,
        target: resolvedTarget,
        url: getCheckoutHandoffUrl(shopId, resolvedTarget),
      });
    } catch (error) {
      next(error);
    }
  }
);

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

// ALL PRODUCTS ROUTE
apiRouter.get(
  "/v1/products",
  validateQuery(
    shopQuerySchema
      .merge(productPaginationSchema)
      .merge(productFilterQuerySchema)
      .merge(customerIdSchema.partial())
  ),
  async (req, res, next) => {
    try {
      const { shopId, lang, page, pageSize, sort, customerId } = req.query as any;
      const client = new PrestaShopClient({ shopId, lang });
      const allowPrice = await resolveAllowPrice(client, customerId ? Number(customerId) : undefined);
      const items = await listAllProducts(
        client,
        shopId,
        page,
        pageSize,
        sort,
        lang,
        allowPrice,
        toBasicProductFilters(req.query as Record<string, unknown>)
      );
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
  "/v1/categories/:categoryId/filters",
  validateParams(categoryIdSchema),
  validateQuery(shopQuerySchema.merge(customerIdSchema.partial())),
  async (req, res, next) => {
    try {
      const { shopId, lang, customerId } = req.query as any;
      const { categoryId } = req.params as any;
      const client = new PrestaShopClient({ shopId, lang });
      const allowPrice = await resolveAllowPrice(client, customerId ? Number(customerId) : undefined);
      const items = await listFacetProductsByCategory(
        client,
        shopId,
        Number(categoryId),
        lang,
        allowPrice
      );
      const facets = buildCatalogFacets(items, lang);
      res.json({ items: facets });
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/categories/:categoryId/products",
  validateParams(categoryIdSchema),
  validateQuery(
    shopQuerySchema
      .merge(productPaginationSchema)
      .merge(productFilterQuerySchema)
      .merge(customerIdSchema.partial())
  ),
  async (req, res, next) => {
    try {
      const { shopId, lang, page, pageSize, sort, customerId } = req.query as any;
      const { categoryId } = req.params as any;
      const client = new PrestaShopClient({ shopId, lang });
      const allowPrice = await resolveAllowPrice(client, customerId ? Number(customerId) : undefined);
      const items = await listProductsByCategory(
        client,
        shopId,
        Number(categoryId),
        page,
        pageSize,
        sort,
        lang,
        allowPrice,
        toBasicProductFilters(req.query as Record<string, unknown>)
      );
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
      const client = new PrestaShopClient({ shopId, lang });
      const allowPrice = await resolveAllowPrice(client, customerId ? Number(customerId) : undefined);
      const item = await getProductDetail(client, shopId, Number(productId), lang, allowPrice);
      res.json(item);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get("/v1/wholesale-customers", validateQuery(shopQuerySchema), async (req, res, next) => {
  try {
    const { shopId, lang } = req.query as any;
    const client = new PrestaShopClient({ shopId, lang });
    const items = await listWholesaleCustomers(client, Number(shopId), lang);
    res.json({ items });
  } catch (error) {
    next(error);
  }
});

apiRouter.get("/v1/customers", validateQuery(shopQuerySchema), async (req, res, next) => {
  try {
    const { shopId, lang } = req.query as any;
    const client = new PrestaShopClient({ shopId, lang });
    const items = await listCustomers(client, Number(shopId), lang);
    res.json({ items });
  } catch (error) {
    next(error);
  }
});

// DEBUG: List Wholesale Applications
apiRouter.get("/v1/debug/wholesale-applications", async (req, res, next) => {
  try {
    const countryIso = (req.query.countryIso as string) || "GR";
    const result = await debugListWholesaleApplications(countryIso);
    res.json(result);
  } catch (error) {
    next(error);
  }
});

apiRouter.get(
  "/v1/debug/ets-wholesale/application/:customerId",
  validateParams(customerIdSchema),
  async (req, res, next) => {
    try {
      const { customerId } = req.params as any;
      const countryIso = (req.query.countryIso as string) || "GR";
      const result = await debugInspectEtsWholesaleApplication(Number(customerId), countryIso);
      res.json(result);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/debug/ets-wholesale/application-visibility/:customerId",
  validateParams(customerIdSchema),
  async (req, res, next) => {
    try {
      const { customerId } = req.params as any;
      const countryIso = (req.query.countryIso as string) || "GR";
      const result = await debugInspectEtsWholesaleApplicationVisibility(Number(customerId), countryIso);
      res.json(result);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/debug/ets-wholesale/form-fields",
  validateQuery(etsWholesaleFormFieldsQuerySchema),
  async (req, res, next) => {
    try {
      const { formType } = req.query as any;
      const countryIso = (req.query.countryIso as string) || "GR";
      const result = await debugInspectEtsWholesaleFormFields(formType, countryIso);
      res.json(result);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/debug/modules/:moduleName",
  validateParams(moduleNameSchema),
  async (req, res, next) => {
    try {
      const { moduleName } = req.params as any;
      const countryIso = (req.query.countryIso as string) || "GR";
      const result = await debugInspectPrestaShopModule(moduleName, countryIso);
      res.json(result);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/debug/tables/:tableName",
  validateParams(tableNameSchema),
  async (req, res, next) => {
    try {
      const { tableName } = req.params as any;
      const countryIso = (req.query.countryIso as string) || "GR";
      const result = await debugInspectPrestaShopTable(tableName, countryIso);
      res.json(result);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/debug/modules/:moduleName/search",
  validateParams(moduleNameSchema),
  validateQuery(moduleSearchQuerySchema),
  async (req, res, next) => {
    try {
      const { moduleName } = req.params as any;
      const { pattern } = req.query as any;
      const countryIso = (req.query.countryIso as string) || "GR";
      const result = await debugSearchPrestaShopModuleCode(moduleName, pattern, countryIso);
      res.json(result);
    } catch (error) {
      next(error);
    }
  }
);

apiRouter.get(
  "/v1/debug/modules/:moduleName/file",
  validateParams(moduleNameSchema),
  validateQuery(moduleFileQuerySchema),
  async (req, res, next) => {
    try {
      const { moduleName } = req.params as any;
      const { path, start, lines } = req.query as any;
      const countryIso = (req.query.countryIso as string) || "GR";
      const result = await debugReadPrestaShopModuleFile(
        moduleName,
        path,
        Number(start),
        Number(lines),
        countryIso
      );
      res.json(result);
    } catch (error) {
      next(error);
    }
  }
);

export default apiRouter;
