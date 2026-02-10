"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.registerCustomer = void 0;
const axios_1 = require("axios");
const crypto_1 = require("crypto");
const dns_1 = require("dns");
const http_1 = require("http");
const https_1 = require("https");
const PrestaShopClient_1 = require("../clients/PrestaShopClient");
const env_1 = require("../config/env");
const prestashopParser_1 = require("./prestashopParser");
const PENDING_STATUS = "PENDING_WHOLESALE_APPROVAL";
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
const findCustomerByEmail = async (client, email) => {
    const payload = await client.get("customers", {
        "filter[email]": `[${email}]`,
        display: "[id,email]",
        limit: 1
    });
    const customers = (0, prestashopParser_1.extractResourceList)("customers", payload);
    return customers.length > 0;
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
        return dns_1.lookup(targetHost, options, callback);
    };
};
const resolveSyncUrl = () => {
    const baseUrl = env_1.config.prestashopBaseUrl || env_1.config.shopBaseUrls[env_1.config.defaultShopId];
    const url = new URL(baseUrl);
    const configuredPath = env_1.config.customerSyncPath.startsWith("/")
        ? env_1.config.customerSyncPath
        : `/${env_1.config.customerSyncPath}`;
    const pathname = url.pathname.replace(/\/+$/, "");
    if (pathname.endsWith("/api")) {
        url.pathname = `${pathname.slice(0, -4)}${configuredPath}`;
    }
    else {
        url.pathname = `${pathname}${configuredPath}`;
    }
    return url.toString();
};
const createModuleHeaders = (payload) => {
    if (!env_1.config.customerSyncSecret) {
        throw {
            status: 500,
            code: "CONFIG_ERROR",
            message: "GRIFON_CUSTOMER_SYNC_SECRET is required for customer registration sync"
        };
    }
    const timestamp = Math.floor(Date.now() / 1000).toString();
    const signatureBase = `${timestamp}\n${payload}`;
    const signature = crypto_1.default
        .createHmac("sha256", env_1.config.customerSyncSecret)
        .update(signatureBase, "utf8")
        .digest("base64");
    return {
        "Content-Type": "application/json",
        "X-Grifon-Timestamp": timestamp,
        "X-Grifon-Signature": signature
    };
};
const buildSyncPayload = (request) => {
    const groupIds = resolveGroupIds(request.countryIso);
    return {
        externalCustomerId: request.email.trim().toLowerCase(),
        customer: {
            email: request.email.trim().toLowerCase(),
            firstname: request.firstName,
            lastname: request.lastName,
            password: request.password,
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
    const client = new PrestaShopClient_1.PrestaShopClient({ shopId: env_1.config.defaultShopId });
    const email = request.email.trim().toLowerCase();
    const exists = await findCustomerByEmail(client, email);
    if (exists) {
        throw {
            status: 409,
            code: "EMAIL_EXISTS",
            message: "Email already registered"
        };
    }
    const payload = buildSyncPayload({ ...request, email });
    const body = JSON.stringify(payload);
    const headers = createModuleHeaders(body);
    const lookup = createDnsLookup();
    try {
        const response = await axios_1.default.post(resolveSyncUrl(), body, {
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
        throw {
            status: 502,
            code: "UPSTREAM_ERROR",
            message: "Failed to create customer",
            details: String(error?.message ?? "Unknown error")
        };
    }
};
exports.registerCustomer = registerCustomer;
