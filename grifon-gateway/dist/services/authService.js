"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.loginCustomer = exports.registerCustomer = void 0;
const axios_1 = __importDefault(require("axios"));
const bcryptjs_1 = __importDefault(require("bcryptjs"));
const crypto_1 = __importDefault(require("crypto"));
const env_1 = require("../config/env");
const PASSWORD_SALT_ROUNDS = 10;
const resolveSyncUrl = (countryIso = "GR") => {
    const shopId = countryIso.trim().toUpperCase() === "SE" ? 1 : 4;
    const baseUrl = env_1.config.shopBaseUrls[shopId] || env_1.config.prestashopBaseUrl;
    const url = new URL(baseUrl);
    const rootUrl = url.origin + url.pathname.replace(/\/api\/?$/, "");
    return `${rootUrl}/index.php?fc=module&module=grifoncustomersync&controller=sync`;
};
/**
 * Creates a signature compatible with the PrestaShop grifoncustomersync module.
 * Format: base64(HMAC_SHA256("<timestamp>\n<body>", secret))
 */
const createSignature = (payload, secret) => {
    const timestamp = Math.floor(Date.now() / 1000).toString();
    const signature = crypto_1.default
        .createHmac("sha256", secret)
        .update(timestamp + "\n" + payload) // Added \n to match PHP side: $ts . "\n" . $rawBody
        .digest("base64");
    return { timestamp, signature };
};
const registerCustomer = async (request) => {
    const email = request.email.trim().toLowerCase();
    const hashedPassword = await bcryptjs_1.default.hash(request.password, PASSWORD_SALT_ROUNDS);
    const payload = {
        action: "register",
        externalCustomerId: email,
        customer: {
            email,
            firstname: request.firstName,
            lastname: request.lastName,
            password: hashedPassword,
            company: request.company || "",
            newsletter: request.newsletter ? 1 : 0,
            optin: request.partnerOffers ? 1 : 0,
            active: 0
        },
        groups: { default: 3, list: [3] },
        addresses: [{
                alias: "Default",
                address1: request.street,
                postcode: request.postalCode,
                city: request.city,
                countryIso: request.countryIso,
                vat_number: request.vatNumber || "",
                phone: request.phone || ""
            }]
    };
    return await sendToPrestaShop(payload, request.countryIso);
};
exports.registerCustomer = registerCustomer;
const loginCustomer = async (request) => {
    const payload = {
        action: "login",
        email: request.email.trim().toLowerCase(),
        password: request.password
    };
    try {
        const response = await sendToPrestaShop(payload, "GR");
        if (response.status === "SUCCESS") {
            return {
                token: response.token || "fake-jwt-token",
                customerId: response.customerId,
                firstName: response.firstName || "",
                lastName: response.lastName || ""
            };
        }
        const err = new Error(response.message || "Login failed");
        err.status = 401;
        throw err;
    }
    catch (error) {
        if (error.status)
            throw error;
        const err = new Error(error.message || "Authentication failed");
        err.status = 401;
        throw err;
    }
};
exports.loginCustomer = loginCustomer;
const sendToPrestaShop = async (payload, countryIso) => {
    const body = JSON.stringify(payload);
    const secret = process.env.GRIFON_CUSTOMER_SYNC_SECRET || env_1.config.prestashopApiKey;
    const { timestamp, signature } = createSignature(body, secret);
    const syncUrl = resolveSyncUrl(countryIso);
    try {
        const response = await axios_1.default.post(syncUrl, body, {
            timeout: 15000,
            headers: {
                "Content-Type": "application/json",
                "X-Grifon-Timestamp": timestamp,
                "X-Grifon-Signature": signature
            },
            validateStatus: () => true
        });
        if (response.status >= 200 && response.status < 300) {
            return response.data;
        }
        const errorDetail = response.data?.message || response.data?.error || `PrestaShop Error ${response.status}`;
        const err = new Error(errorDetail);
        err.status = response.status === 401 || response.status === 403 ? 401 : 502;
        throw err;
    }
    catch (error) {
        if (error.status)
            throw error;
        const err = new Error(error.message || "Failed to communicate with PrestaShop");
        err.status = 502;
        throw err;
    }
};
