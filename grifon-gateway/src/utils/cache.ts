import { LRUCache } from "lru-cache";
import Redis from "ioredis";
import { config } from "../config/env";

export interface CacheStore {
  get<T>(key: string): Promise<T | undefined>;
  set<T>(key: string, value: T, ttlSeconds: number): Promise<void>;
}

class MemoryCache implements CacheStore {
  private cache: LRUCache<string, unknown>;

  constructor() {
    this.cache = new LRUCache({
      max: 5000,
      ttlAutopurge: true
    });
  }

  async get<T>(key: string): Promise<T | undefined> {
    return this.cache.get(key) as T | undefined;
  }

  async set<T>(key: string, value: T, ttlSeconds: number): Promise<void> {
    this.cache.set(key, value, { ttl: ttlSeconds * 1000 });
  }
}

class RedisCache implements CacheStore {
  private client: Redis;

  constructor(redisUrl: string) {
    this.client = new Redis(redisUrl, { maxRetriesPerRequest: 1 });
  }

  async get<T>(key: string): Promise<T | undefined> {
    const raw = await this.client.get(key);
    if (!raw) return undefined;
    return JSON.parse(raw) as T;
  }

  async set<T>(key: string, value: T, ttlSeconds: number): Promise<void> {
    await this.client.set(key, JSON.stringify(value), "EX", ttlSeconds);
  }
}

export const cache: CacheStore = config.redisUrl
  ? new RedisCache(config.redisUrl)
  : new MemoryCache();

export const buildCacheKey = (parts: Record<string, string | number | undefined>): string => {
  const entries = Object.entries(parts)
    .filter(([, value]) => value !== undefined)
    .map(([key, value]) => `${key}=${value}`)
    .join("&");
  return entries;
};
