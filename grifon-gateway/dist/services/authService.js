"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.loginCustomer = exports.updateProfile = exports.registerCustomer = void 0;
const axios_1 = __importDefault(require("axios"));
const crypto_1 = __importDefault(require("crypto"));
const env_1 = require("../config/env");
const PrestaShopClient_1 = require("../clients/PrestaShopClient");
const priceAccessService_1 = require("./priceAccessService");
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
    const dniValue = (request.vatNumber && request.vatNumber.trim().length >= 9)
        ? request.vatNumber.trim()
        : "123456789";
    const payload = {
        externalCustomerId: email,
        customer: {
            email,
            firstname: request.firstName,
            lastname: request.lastName,
            password: request.password,
            company: request.company || "",
            newsletter: request.newsletter ? 1 : 0,
            active: 1,
            is_wholesale: request.wholesaleRequested ? 1 : 0,
            siret: dniValue,
            dni: dniValue
        },
        addresses: [{
                externalAddressId: `addr_${email}`,
                alias: "Default",
                firstname: request.firstName,
                lastname: request.lastName,
                address1: request.street || "Δεν δηλώθηκε οδός",
                postcode: (request.postalCode || "00000").replace(/\s/g, ""),
                city: request.city || "Δεν δηλώθηκε πόλη",
                countryIso: (request.countryIso || "GR").toUpperCase(),
                phone: request.phone || "0000000000",
                vat_number: dniValue,
                dni: dniValue,
                identification_number: dniValue,
                dni_number: dniValue,
                identification: dniValue
            }]
    };
    const response = await sendToPrestaShop(payload, request.countryIso || "GR");
    // ΜΕΤΑΤΡΟΠΗ ΑΠΑΝΤΗΣΗΣ ΓΙΑ ΤΗΝ ΕΦΑΡΜΟΓΗ
    return {
        customerId: response.psCustomerId?.toString() || "0",
        status: "success",
        message: response.message || "Registration successful"
    };
};
exports.registerCustomer = registerCustomer;
const updateProfile = async (request) => {
    const payload = {
        action: "sync",
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
    const response = await sendToPrestaShop(payload, "GR");
    const customerId = response?.id_customer ? Number(response.id_customer) : null;
    if (!customerId) {
        return {
            ...response,
            can_view_prices: false,
        };
    }
    const client = new PrestaShopClient_1.PrestaShopClient({ shopId: 4 });
    const priceAccess = await (0, priceAccessService_1.getPriceAccess)(client, customerId);
    return {
        ...response,
        can_view_prices: priceAccess.allowed,
    };
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
    throw new Error(response.data?.message || response.data?.error || `PrestaShop Error: ${response.status}`);
}
