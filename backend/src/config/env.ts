import dotenv from 'dotenv';
import { z } from 'zod';

dotenv.config();

const envSchema = z.object({
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
  PORT: z.coerce.number().default(3000),
  PRESTASHOP_BASE_URL: z.string().url().default('https://grifon.gr/api'),
  PRESTASHOP_API_KEY: z.string().min(1).default('test-key'),
  CACHE_TTL_SECONDS: z.coerce.number().default(60),
  ALLOWED_ORIGINS: z.string().default('*')
});

export const env = envSchema.parse({
  NODE_ENV: process.env.NODE_ENV,
  PORT: process.env.PORT,
  PRESTASHOP_BASE_URL: process.env.PRESTASHOP_BASE_URL,
  PRESTASHOP_API_KEY: process.env.PRESTASHOP_API_KEY,
  CACHE_TTL_SECONDS: process.env.CACHE_TTL_SECONDS,
  ALLOWED_ORIGINS: process.env.ALLOWED_ORIGINS
});
