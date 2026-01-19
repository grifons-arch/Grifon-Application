import type { NextFunction, Request, Response } from 'express';
import { env } from '../config/env.js';

type CacheEntry = {
  expiresAt: number;
  value: unknown;
};

const store = new Map<string, CacheEntry>();

export const cacheMiddleware = (ttlSeconds = env.CACHE_TTL_SECONDS) => {
  return (req: Request, res: Response, next: NextFunction) => {
    if (req.method !== 'GET') {
      return next();
    }

    const key = req.originalUrl;
    const cached = store.get(key);
    if (cached && cached.expiresAt > Date.now()) {
      return res.json(cached.value);
    }

    const originalJson = res.json.bind(res);
    res.json = (body: unknown) => {
      store.set(key, { expiresAt: Date.now() + ttlSeconds * 1000, value: body });
      return originalJson(body);
    };

    return next();
  };
};
