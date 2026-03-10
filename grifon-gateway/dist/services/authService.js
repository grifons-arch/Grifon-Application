"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.loginCustomer = exports.updateProfile = exports.registerCustomer = void 0;
const axios_1 = __importDefault(require("axios"));
const crypto_1 = __importDefault(require("crypto"));
const env_1 = require("../config/env");
const resolveSyncUrl = (countryIso = "GR") => {
    const shopId = countryIso.trim().toUpperCase() === "SE" ? 1 : 4;
    const baseUrl = env_1.config.shopBaseUrls[shopId] || env_1.config.prestashopBaseUrl;
    const url = new URL(baseUrl);
    const rootUrl = url.origin + url.pathname.replace(/\/api\/?$/, "");
    return `${rootUrl}/index.php?fc=module&module=grifoncustomersync&controller=sync`;
};
const createSignature = (payload, secret) => {
    const timestamp = Math.floor(Date.now() / 1000).toString();
    const base = timestamp + payload;
    const signature = crypto_1.default.createHmac("sha256", secret).update(base).digest("base64");
    return { timestamp, signature };
};
const registerCustomer = async (request) => {
    const email = request.email.trim().toLowerCase();
    const payload = {
        externalCustomerId: email,
        customer: {
            email,
            firstname: request.firstName,
            lastname: request.lastName,
            password: request.password,
            company: request.company || "",
            newsletter: request.newsletter ? 1 : 0,
            active: 0
        },
        addresses: [{
                externalAddressId: `addr_${email}`,
                alias: "Default",
                firstname: request.firstName,
                lastname: request.lastName,
                address1: request.street || "",
                postcode: request.postalCode || "",
                city: request.city || "",
                countryIso: request.countryIso || "GR",
                vat_number: request.vatNumber || "",
                dni: request.vatNumber || "000000000"
            }]
    };
    return sendToPrestaShop(payload, request.countryIso || "GR");
};
exports.registerCustomer = registerCustomer;
/**
 * Ενημέρωση Προφίλ Χρήστη
 */
const updateProfile = async (request) => {
    const payload = {
        action: "sync", // Επαναχρησιμοποιούμε τη handleSync της PHP που κάνει upsert
        externalCustomerId: request.email.trim().toLowerCase(),
        customer: {
            email: request.email.trim().toLowerCase(),
            firstname: request.firstName,
            lastname: request.lastName,
            company: request.company || "",
            newsletter: request.newsletter ? 1 : 0,
            siret: request.vatNumber || ""
        }
    };
    return sendToPrestaShop(payload, "GR");
};
exports.updateProfile = updateProfile;
const loginCustomer = async (email, pass) => {
    const payload = { action: "login", email: email.trim().toLowerCase(), password: pass };
    return sendToPrestaShop(payload, "GR");
};
exports.loginCustomer = loginCustomer;
async function sendToPrestaShop(payload, countryIso) {
    const body = JSON.stringify(payload);
    const secret = env_1.config.customerSyncSecret || env_1.config.prestashopApiKey;
    const { timestamp, signature } = createSignature(body, secret);
    const syncUrl = resolveSyncUrl(countryIso);
    const response = await axios_1.default.post(syncUrl, body, {
        timeout: 15000,
        headers: {
            "Content-Type": "application/json",
            "X-Grifon-Timestamp": timestamp,
            "X-Grifon-Signature": signature
        },
        transformRequest: [(data) => data],
        validateStatus: () => true
    });
    if (response.status === 200 && response.data?.ok === true)
        return response.data;
    throw new Error(response.data?.message || response.data?.error || "Communication error");
}
