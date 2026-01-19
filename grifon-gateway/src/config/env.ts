import { z } from "zod";
import dotenv from "dotenv";

dotenv.config();

const envSchema = z.object({
  PORT: z.string().default("3000"),
  ALLOWED_ORIGINS: z.string().default("*"),
  PRESTASHOP_API_KEY: z.string().min(1),
  SHOP_GR_BASE_URL: z.string().url().default("https://grifon.gr/api"),
  SHOP_SE_BASE_URL: z.string().url().default("http://grifon.se/api"),
  CACHE_TTL_CATEGORIES_SECONDS: z.string().default("600"),
  CACHE_TTL_PRODUCTS_SECONDS: z.string().default("120"),
  TIMEOUT_MS: z.string().default("8000"),
  RATE_LIMIT_PER_MIN: z.string().default("120"),
  REDIS_URL: z.string().optional().default("")
});

const parsed = envSchema.safeParse(process.env);

if (!parsed.success) {
  // eslint-disable-next-line no-console
  console.error("Invalid environment configuration", parsed.error.flatten());
  process.exit(1);
}

const env = parsed.data;

export const config = {
  port: Number(env.PORT),
  allowedOrigins: env.ALLOWED_ORIGINS,
  prestashopApiKey: env.PRESTASHOP_API_KEY,
  shopBaseUrls: {
    4: env.SHOP_GR_BASE_URL,
    1: env.SHOP_SE_BASE_URL
  },
  cacheTtlCategoriesSeconds: Number(env.CACHE_TTL_CATEGORIES_SECONDS),
  cacheTtlProductsSeconds: Number(env.CACHE_TTL_PRODUCTS_SECONDS),
  timeoutMs: Number(env.TIMEOUT_MS),
  rateLimitPerMin: Number(env.RATE_LIMIT_PER_MIN),
  redisUrl: env.REDIS_URL
};

export type ShopId = 1 | 4;

export const shops = [
  { id: 4 as ShopId, code: "GR", domain: "grifon.gr", baseUrl: env.SHOP_GR_BASE_URL },
  { id: 1 as ShopId, code: "SE", domain: "grifon.se", baseUrl: env.SHOP_SE_BASE_URL }
];
