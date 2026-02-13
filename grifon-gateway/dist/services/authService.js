"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.registerCustomer = void 0;
const axios_1 = __importDefault(require("axios"));
const bcryptjs_1 = __importDefault(require("bcryptjs"));
const crypto_1 = __importDefault(require("crypto"));
const dns_1 = __importDefault(require("dns"));
const http_1 = __importDefault(require("http"));
const https_1 = __importDefault(require("https"));
const env_1 = require("../config/env");
const networkErrors_1 = require("../utils/networkErrors");
const PENDING_STATUS = "PENDING_WHOLESALE_APPROVAL";
const PASSWORD_SALT_ROUNDS = 10;
const resolveGroupIds = (countryIso) => {
    const groupIds = new Set();
    if (env_1.config.pendingWholesaleGroupId) {
        groupIds.add(env_1.config.pendingWholesaleGroupId);
    }
    const countryGroupId = env_1.config.countryGroupMap[countryIso.toUpperCase()];
    if (countryGroupId) {
        groupIds.add(countryGroupId);
    }
    return Array.from(groupIds);
};
const createDnsLookup = () => {
    const alias = env_1.config.replicaHostname?.trim();
    const resolveTo = env_1.config.replicaResolveTo?.trim();
    if (!alias || !resolveTo) {
        return undefined;
    }
    const normalizedAlias = alias.toLowerCase();
    return (hostname, options, callback) => {
        const host = String(hostname).toLowerCase();
        const targetHost = host === normalizedAlias ? resolveTo : String(hostname);
        return dns_1.default.lookup(targetHost, options, callback);
    };
};
const normalizeEnvKey = (key) => key.replace(/[^A-Za-z0-9]/g, "_").replace(/_+/g, "_").toUpperCase();
const readEnvAliasRuntime = (...keys) => {
    for (const key of keys) {
        const value = process.env[key];
        if (typeof value === "string" && value.trim().length > 0) {
            return value.trim();
        }
    }
    const normalizedCandidates = new Set(keys.map(normalizeEnvKey));
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
const resolveCustomerSyncSecret = () => (typeof env_1.config.customerSyncSecret === "string" && env_1.config.customerSyncSecret.trim().length > 0
    ? env_1.config.customerSyncSecret.trim()
    : undefined) ??
    readEnvAliasRuntime("GRIFON_CUSTOMER_SYNC_SECRET", "GRIFON.CUSTOMER.SYNC.SECRET", "GRIFON__CUSTOMER__SYNC__SECRET");
const resolveCustomerSyncPath = () => readEnvAliasRuntime("GRIFON_CUSTOMER_SYNC_PATH", "GRIFON.CUSTOMER.SYNC.PATH", "GRIFON__CUSTOMER__SYNC__PATH") ?? env_1.config.customerSyncPath ?? "/module/grifoncustomersync/sync";
const resolveShopIdForCountry = (countryIso) => countryIso.trim().toUpperCase() === "SE" ? 1 : 4;
const resolveSyncUrl = (countryIso) => {
    const shopId = resolveShopIdForCountry(countryIso);
    const baseUrl = env_1.config.shopBaseUrls[shopId] || env_1.config.shopBaseUrls[env_1.config.defaultShopId] || env_1.config.prestashopBaseUrl;
    const url = new URL(baseUrl);
    const customerSyncPath = resolveCustomerSyncPath();
    const configuredPath = customerSyncPath.startsWith("/")
        ? customerSyncPath
        : `/${customerSyncPath}`;
    const pathname = url.pathname.replace(/\/+$/, "");
    if (pathname.endsWith("/api")) {
        url.pathname = `${pathname.slice(0, -4)}${configuredPath}`;
    }
    else {
        url.pathname = `${pathname}${configuredPath}`;
    }
    const replicaHost = env_1.config.replicaHostname?.trim().toLowerCase();
    const replicaResolveTo = env_1.config.replicaResolveTo?.trim();
    const currentHost = url.hostname.toLowerCase();
    if (replicaHost && !replicaResolveTo && currentHost === replicaHost) {
        const segments = url.pathname.split("/").filter(Boolean);
        const candidateDomain = segments[0]?.toLowerCase();
        if (candidateDomain && candidateDomain.includes(".")) {
            url.hostname = candidateDomain;
            url.pathname = `/${segments.slice(1).join("/")}`;
        }
        else {
            const selectedShop = env_1.shops.find((shop) => shop.id === shopId);
            const defaultShop = env_1.shops.find((shop) => shop.id === env_1.config.defaultShopId);
            const fallbackDomain = selectedShop?.domain ?? defaultShop?.domain;
            if (fallbackDomain) {
                url.hostname = fallbackDomain;
            }
        }
    }
    return url.toString();
};
const resolveSyncHostname = (syncUrl) => {
    try {
        return new URL(syncUrl).hostname;
    }
    catch {
        return undefined;
    }
};
const createModuleHeaders = (payload) => {
    const customerSyncSecret = resolveCustomerSyncSecret();
    if (!customerSyncSecret) {
        throw {
            status: 500,
            code: "CONFIG_ERROR",
            message: "GRIFON.CUSTOMER.SYNC.SECRET is required for customer registration sync (aliases: GRIFON_CUSTOMER_SYNC_SECRET, GRIFON__CUSTOMER__SYNC__SECRET)"
        };
    }
    const timestamp = Math.floor(Date.now() / 1000).toString();
    const signatureBase = `${timestamp}\n${payload}`;
    const signature = crypto_1.default
        .createHmac("sha256", customerSyncSecret)
        .update(signatureBase, "utf8")
        .digest("base64");
    return {
        "Content-Type": "application/json",
        "X-Grifon-Timestamp": timestamp,
        "X-Grifon-Signature": signature
    };
};
const hashCustomerPassword = async (password) => bcryptjs_1.default.hash(password, PASSWORD_SALT_ROUNDS);
const buildSyncPayload = (request, hashedPassword) => {
    const groupIds = resolveGroupIds(request.countryIso);
    return {
        externalCustomerId: request.email.trim().toLowerCase(),
        customer: {
            email: request.email.trim().toLowerCase(),
            firstname: request.firstName,
            lastname: request.lastName,
            password: hashedPassword,
            company: request.company,
            newsletter: request.newsletter ? 1 : 0,
            optin: request.partnerOffers ? 1 : 0,
            active: 0
        },
        groups: {
            default: env_1.config.pendingWholesaleGroupId,
            list: groupIds
        },
        addresses: [
            {
                externalAddressId: `${request.email.trim().toLowerCase()}::default`,
                alias: "Default",
                address1: request.street,
                postcode: request.postalCode,
                city: request.city,
                countryIso: request.countryIso,
                vat_number: request.vatNumber,
                phone: request.phone,
                other: request.iban ? `IBAN: ${request.iban}` : undefined,
                company: request.company
            }
        ]
    };
};
const registerCustomer = async (request) => {
    const email = request.email.trim().toLowerCase();
    const hashedPassword = await hashCustomerPassword(request.password);
    const payload = buildSyncPayload({ ...request, email }, hashedPassword);
    const body = JSON.stringify(payload);
    const headers = createModuleHeaders(body);
    const lookup = createDnsLookup();
    const syncUrl = resolveSyncUrl(request.countryIso);
    try {
        const response = await axios_1.default.post(syncUrl, body, {
            timeout: env_1.config.timeoutMs,
            headers,
            httpAgent: lookup ? new http_1.default.Agent({ lookup }) : undefined,
            httpsAgent: lookup ? new https_1.default.Agent({ lookup }) : undefined,
            validateStatus: () => true
        });
        if (response.status >= 200 && response.status < 300) {
            const customerId = String(response.data?.customerId ?? response.data?.id_customer ?? response.data?.id ?? email);
            return {
                customerId,
                status: response.data?.status ?? PENDING_STATUS,
                message: response.data?.message ?? "Η αίτηση καταχωρήθηκε και βρίσκεται σε αναμονή έγκρισης."
            };
        }
        const message = String(response.data?.message ?? "Failed to create customer");
        if (message.toLowerCase().includes("email") && message.toLowerCase().includes("exists")) {
            throw {
                status: 409,
                code: "EMAIL_EXISTS",
                message: "Email already registered"
            };
        }
        throw {
            status: response.status || 502,
            code: "UPSTREAM_ERROR",
            message,
            details: response.data
        };
    }
    catch (error) {
        if (error?.status) {
            throw error;
        }
        const networkMessage = (0, networkErrors_1.normalizeNetworkErrorMessage)(error, {
            fallbackHostname: resolveSyncHostname(syncUrl)
        });
        throw {
            status: 502,
            code: "UPSTREAM_ERROR",
            message: networkMessage
                ? `Failed to create customer: ${networkMessage}`
                : "Failed to create customer",
            details: networkMessage ?? String(error?.message ?? "Unknown error")
        };
    }
};
exports.registerCustomer = registerCustomer;
