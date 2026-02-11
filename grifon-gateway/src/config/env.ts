import { z } from "zod";
import dotenv from "dotenv";

dotenv.config();

const normalizeEnvKey = (key: string): string =>
  key.replace(/[^A-Za-z0-9]/g, "_").replace(/_+/g, "_").toUpperCase();

const readEnvWithAliases = (...keys: string[]): string | undefined => {
  const normalizedCandidates = new Set(keys.map(normalizeEnvKey));

  for (const key of keys) {
    const value = process.env[key];
    if (typeof value === "string" && value.trim().length > 0) {
      return value.trim();
    }
  }

  for (const [key, value] of Object.entries(process.env)) {
    if (!normalizedCandidates.has(normalizeEnvKey(key))) {
      continue;
    }
    if (typeof value === "string" && value.trim().length > 0) {
      return value.trim();
    }
  }

  return undefined;
};

const customerSyncSecret = readEnvWithAliases(
  "GRIFON_CUSTOMER_SYNC_SECRET",
  "GRIFON.CUSTOMER.SYNC.SECRET",
  "GRIFON__CUSTOMER__SYNC__SECRET"
);

const customerSyncPath = readEnvWithAliases(
  "GRIFON_CUSTOMER_SYNC_PATH",
  "GRIFON.CUSTOMER.SYNC.PATH",
  "GRIFON__CUSTOMER__SYNC__PATH"
);

const envSchema = z.object({
  PORT: z.string().default("3000"),
  ALLOWED_ORIGINS: z.string().default("*"),
  PRESTASHOP_API_KEY: z.string().min(1),
  PRESTASHOP_BASE_URL: z.string().url().optional().default(""),
  DEFAULT_SHOP_ID: z.enum(["1", "4"]).default("4"),
  PENDING_WHOLESALE_GROUP_ID: z.string().optional().default(""),
  COUNTRY_GROUP_MAP: z.string().optional().default("{}"),
  SHOP_GR_BASE_URL: z.string().url().default("https://replica/grifon.gr/api"),
  SHOP_SE_BASE_URL: z.string().url().default("https://replica/grifon.se/api"),
  REPLICA_HOSTNAME: z.string().default("replica"),
  REPLICA_RESOLVE_TO: z.string().default(""),
  GRIFON_CUSTOMER_SYNC_SECRET: z.string().optional().default(customerSyncSecret ?? ""),
  GRIFON_CUSTOMER_SYNC_PATH: z
    .string()
    .default(customerSyncPath ?? "/module/grifoncustomersync/sync"),
  CACHE_TTL_CATEGORIES_SECONDS: z.string().default("600"),
  CACHE_TTL_PRODUCTS_SECONDS: z.string().default("120"),
  TIMEOUT_MS: z.string().default("8000"),
  RATE_LIMIT_PER_MIN: z.string().default("120"),
  REGISTER_RATE_LIMIT_PER_MIN: z.string().default("10"),
  REDIS_URL: z.string().optional().default("")
});

const parsed = envSchema.safeParse(process.env);

if (!parsed.success) {
  // eslint-disable-next-line no-console
  console.error("Invalid environment configuration", parsed.error.flatten());
  process.exit(1);
}

const env = parsed.data;

const parseCountryGroupMap = (value: string): Record<string, number> => {
  if (!value) return {};
  try {
    const parsedMap = JSON.parse(value) as Record<string, number>;
    if (typeof parsedMap !== "object" || parsedMap === null) {
      return {};
    }
    return Object.entries(parsedMap).reduce<Record<string, number>>((acc, [key, val]) => {
      const id = Number(val);
      if (Number.isNaN(id)) {
        return acc;
      }
      acc[key.toUpperCase()] = id;
      return acc;
    }, {});
  } catch {
    return {};
  }
};

export const config = {
  port: Number(env.PORT),
  allowedOrigins: env.ALLOWED_ORIGINS,
  prestashopApiKey: env.PRESTASHOP_API_KEY,
  prestashopBaseUrl: env.PRESTASHOP_BASE_URL || env.SHOP_GR_BASE_URL,
  shopBaseUrls: {
    4: env.SHOP_GR_BASE_URL,
    1: env.SHOP_SE_BASE_URL
  },
  replicaHostname: env.REPLICA_HOSTNAME,
  replicaResolveTo: env.REPLICA_RESOLVE_TO,
  customerSyncSecret: env.GRIFON_CUSTOMER_SYNC_SECRET,
  customerSyncPath: env.GRIFON_CUSTOMER_SYNC_PATH,
  defaultShopId: env.DEFAULT_SHOP_ID === "1" ? 1 : 4,
  pendingWholesaleGroupId: env.PENDING_WHOLESALE_GROUP_ID
    ? Number(env.PENDING_WHOLESALE_GROUP_ID)
    : undefined,
  countryGroupMap: parseCountryGroupMap(env.COUNTRY_GROUP_MAP),
  cacheTtlCategoriesSeconds: Number(env.CACHE_TTL_CATEGORIES_SECONDS),
  cacheTtlProductsSeconds: Number(env.CACHE_TTL_PRODUCTS_SECONDS),
  timeoutMs: Number(env.TIMEOUT_MS),
  rateLimitPerMin: Number(env.RATE_LIMIT_PER_MIN),
  registerRateLimitPerMin: Number(env.REGISTER_RATE_LIMIT_PER_MIN),
  redisUrl: env.REDIS_URL
};

export type ShopId = 1 | 4;

export const shops = [
  { id: 4 as ShopId, code: "GR", domain: "grifon.gr", baseUrl: env.SHOP_GR_BASE_URL },
  { id: 1 as ShopId, code: "SE", domain: "grifon.se", baseUrl: env.SHOP_SE_BASE_URL }
];
