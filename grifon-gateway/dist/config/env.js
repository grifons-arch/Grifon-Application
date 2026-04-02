"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.shops = exports.config = void 0;
const zod_1 = require("zod");
const dotenv_1 = __importDefault(require("dotenv"));
dotenv_1.default.config();
const normalizeEnvKey = (key) => key.replace(/[^A-Za-z0-9]/g, "_").replace(/_+/g, "_").toUpperCase();
const readEnvWithAliases = (...keys) => {
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
const trimToUndefined = (value) => {
    if (typeof value !== "string") {
        return undefined;
    }
    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : undefined;
};
const customerSyncSecret = readEnvWithAliases("GRIFON_CUSTOMER_SYNC_SECRET", "GRIFON.CUSTOMER.SYNC.SECRET", "GRIFON__CUSTOMER__SYNC__SECRET");
const customerSyncPath = readEnvWithAliases("GRIFON_CUSTOMER_SYNC_PATH", "GRIFON.CUSTOMER.SYNC.PATH", "GRIFON__CUSTOMER__SYNC__PATH");
const envSchema = zod_1.z.object({
    PORT: zod_1.z.string().default("3000"),
    ALLOWED_ORIGINS: zod_1.z.string().default("*"),
    PRESTASHOP_API_KEY: zod_1.z.string().min(1),
    PRESTASHOP_BASE_URL: zod_1.z.string().url().optional().default(""),
    DEFAULT_SHOP_ID: zod_1.z.enum(["1", "4"]).default("4"),
    PENDING_WHOLESALE_GROUP_ID: zod_1.z.string().optional().default(""),
    COUNTRY_GROUP_MAP: zod_1.z.string().optional().default("{}"),
    SHOP_GR_BASE_URL: zod_1.z.string().url().default("https://replica/grifon.gr/api"),
    SHOP_SE_BASE_URL: zod_1.z.string().url().default("https://replica/grifon.se/api"),
    REPLICA_HOSTNAME: zod_1.z.string().default("replica"),
    REPLICA_RESOLVE_TO: zod_1.z.string().default(""),
    UPSTREAM_HOST_ALIASES: zod_1.z.string().optional().default("{}"),
    GRIFON_CUSTOMER_SYNC_SECRET: zod_1.z.string().optional().default(customerSyncSecret ?? ""),
    GRIFON_CUSTOMER_SYNC_PATH: zod_1.z
        .string()
        .default(customerSyncPath ?? "/module/grifoncustomersync/sync"),
    CACHE_TTL_CATEGORIES_SECONDS: zod_1.z.string().default("600"),
    CACHE_TTL_PRODUCTS_SECONDS: zod_1.z.string().default("120"),
    TIMEOUT_MS: zod_1.z.string().default("8000"),
    RATE_LIMIT_PER_MIN: zod_1.z.string().default("120"),
    REGISTER_RATE_LIMIT_PER_MIN: zod_1.z.string().default("10"),
    REDIS_URL: zod_1.z.string().optional().default(""),
    WHOLESALE_NOTIFICATION_TRANSPORT: zod_1.z
        .enum(["auto", "smtp", "sendmail", "disabled"])
        .default("auto"),
    WHOLESALE_NOTIFICATION_TO: zod_1.z.string().optional().default("joanneper@yahoo.com"),
    WHOLESALE_NOTIFICATION_FROM: zod_1.z.string().optional().default("grifon-gateway@localhost"),
    SENDMAIL_PATH: zod_1.z.string().optional().default("/usr/sbin/sendmail"),
    SMTP_HOST: zod_1.z.string().optional().default(""),
    SMTP_PORT: zod_1.z.string().optional().default("587"),
    SMTP_SECURE: zod_1.z.string().optional().default("false"),
    SMTP_REQUIRE_TLS: zod_1.z.string().optional().default("false"),
    SMTP_USER: zod_1.z.string().optional().default(""),
    SMTP_PASS: zod_1.z.string().optional().default(""),
    SMTP_HELO_NAME: zod_1.z.string().optional().default("")
});
const parsed = envSchema.safeParse(process.env);
if (!parsed.success) {
    // eslint-disable-next-line no-console
    console.error("Invalid environment configuration", parsed.error.flatten());
    process.exit(1);
}
const env = parsed.data;
const parseBoolean = (value, fallback = false) => {
    const normalized = trimToUndefined(value)?.toLowerCase();
    if (!normalized) {
        return fallback;
    }
    return ["1", "true", "yes", "on"].includes(normalized);
};
const parseCountryGroupMap = (value) => {
    if (!value)
        return {};
    try {
        const parsedMap = JSON.parse(value);
        if (typeof parsedMap !== "object" || parsedMap === null) {
            return {};
        }
        return Object.entries(parsedMap).reduce((acc, [key, val]) => {
            const id = Number(val);
            if (Number.isNaN(id)) {
                return acc;
            }
            acc[key.toUpperCase()] = id;
            return acc;
        }, {});
    }
    catch {
        return {};
    }
};
const parseHostAliases = (value, legacyAlias, legacyResolveTo) => {
    const aliases = {};
    if (legacyAlias && legacyResolveTo) {
        const normalizedLegacyResolveTo = legacyResolveTo.trim();
        aliases[legacyAlias.trim().toLowerCase()] = normalizedLegacyResolveTo;
        aliases["prestashop-demo"] = normalizedLegacyResolveTo;
    }
    if (!value) {
        return aliases;
    }
    try {
        const parsedMap = JSON.parse(value);
        if (typeof parsedMap !== "object" || parsedMap === null) {
            return aliases;
        }
        for (const [hostname, resolveTo] of Object.entries(parsedMap)) {
            const normalizedHostname = hostname.trim().toLowerCase();
            const normalizedResolveTo = typeof resolveTo === "string" ? resolveTo.trim() : "";
            if (!normalizedHostname || !normalizedResolveTo) {
                continue;
            }
            aliases[normalizedHostname] = normalizedResolveTo;
        }
    }
    catch {
        return aliases;
    }
    return aliases;
};
exports.config = {
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
    upstreamHostAliases: parseHostAliases(env.UPSTREAM_HOST_ALIASES, env.REPLICA_HOSTNAME, env.REPLICA_RESOLVE_TO),
    customerSyncSecret: trimToUndefined(env.GRIFON_CUSTOMER_SYNC_SECRET) ?? customerSyncSecret ?? "",
    customerSyncPath: trimToUndefined(env.GRIFON_CUSTOMER_SYNC_PATH) ??
        customerSyncPath ??
        "/module/grifoncustomersync/sync",
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
    redisUrl: env.REDIS_URL,
    wholesaleNotificationTransport: env.WHOLESALE_NOTIFICATION_TRANSPORT,
    wholesaleNotificationTo: trimToUndefined(env.WHOLESALE_NOTIFICATION_TO) ?? "joanneper@yahoo.com",
    wholesaleNotificationFrom: trimToUndefined(env.WHOLESALE_NOTIFICATION_FROM) ?? "grifon-gateway@localhost",
    sendmailPath: trimToUndefined(env.SENDMAIL_PATH) ?? "/usr/sbin/sendmail",
    smtpHost: trimToUndefined(env.SMTP_HOST),
    smtpPort: Number(env.SMTP_PORT),
    smtpSecure: parseBoolean(env.SMTP_SECURE),
    smtpRequireTls: parseBoolean(env.SMTP_REQUIRE_TLS),
    smtpUser: trimToUndefined(env.SMTP_USER),
    smtpPass: trimToUndefined(env.SMTP_PASS),
    smtpHeloName: trimToUndefined(env.SMTP_HELO_NAME)
};
exports.shops = [
    { id: 4, code: "GR", domain: "grifon.gr", baseUrl: env.SHOP_GR_BASE_URL },
    { id: 1, code: "SE", domain: "grifon.se", baseUrl: env.SHOP_SE_BASE_URL }
];
