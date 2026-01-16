import dotenv from 'dotenv';
import { z } from 'zod';

dotenv.config();

const envSchema = z.object({
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
  PORT: z.coerce.number().default(3000),
  JWT_SECRET: z.string().min(16).default('change-me-please-change-me'),
  PRESTASHOP_URL: z.string().url().optional(),
  PRESTASHOP_WS_KEY: z.string().optional(),
  REDIS_URL: z.string().url().optional()
});

export const env = envSchema.parse({
  NODE_ENV: process.env.NODE_ENV,
  PORT: process.env.PORT,
  JWT_SECRET: process.env.JWT_SECRET,
  PRESTASHOP_URL: process.env.PRESTASHOP_URL,
  PRESTASHOP_WS_KEY: process.env.PRESTASHOP_WS_KEY,
  REDIS_URL: process.env.REDIS_URL
});
