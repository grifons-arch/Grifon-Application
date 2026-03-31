"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.debugListWholesaleApplications = exports.clearActivityTables = exports.listRecentProducts = exports.listFavoriteProducts = exports.recordRecentProduct = exports.syncFavoriteProduct = exports.loginCustomer = exports.updateProfile = exports.registerCustomer = void 0;
const axios_1 = __importDefault(require("axios"));
const crypto_1 = __importDefault(require("crypto"));
const env_1 = require("../config/env");
const PrestaShopClient_1 = require("../clients/PrestaShopClient");
const priceAccessService_1 = require("./priceAccessService");
const wholesaleNotificationService_1 = require("./wholesaleNotificationService");
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
    const countryIso = (request.countryIso || "GR").trim().toUpperCase();
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
        application: request.wholesaleRequested ? {
            requested: true,
            status: "pending",
            source: "grifon_gateway",
            submittedAt: new Date().toISOString(),
            email,
            firstName: request.firstName,
            lastName: request.lastName,
            phone: request.phone || "",
            company: request.company || "",
            vatNumber: request.vatNumber || dniValue,
            countryIso,
            city: request.city || "",
            street: request.street || "",
            postalCode: request.postalCode || ""
        } : undefined,
        addresses: [{
                externalAddressId: `addr_${email}`,
                alias: "Default",
                firstname: request.firstName,
                lastname: request.lastName,
                address1: request.street || "Δεν δηλώθηκε οδός",
                postcode: (request.postalCode || "00000").replace(/\s/g, ""),
                city: request.city || "Δεν δηλώθηκε πόλη",
                countryIso,
                phone: request.phone || "0000000000",
                vat_number: dniValue,
                dni: dniValue,
                identification_number: dniValue,
                dni_number: dniValue,
                identification: dniValue
            }]
    };
    const response = await sendToPrestaShop(payload, countryIso);
    const wholesaleRequested = request.wholesaleRequested === true;
    const wholesaleApplicationRegistered = response?.wholesaleApplicationRegistered === true;
    const wholesaleApplicationError = typeof response?.wholesaleApplicationError === "string"
        ? response.wholesaleApplicationError.trim()
        : "";
    const message = wholesaleRequested
        ? (wholesaleApplicationRegistered
            ? "Η αίτηση χονδρικής καταχωρήθηκε και εκκρεμεί έγκριση."
            : "Ο λογαριασμός δημιουργήθηκε, αλλά η αίτηση χονδρικής χρειάζεται έλεγχο στο PrestaShop.")
        : (response.message || "Registration successful");
    try {
        await (0, wholesaleNotificationService_1.notifyWholesaleRequest)({
            ...request,
            email,
            countryIso
        }, {
            customerId: response.psCustomerId?.toString(),
            countryIso
        });
    }
    catch (error) {
        // eslint-disable-next-line no-console
        console.warn("Wholesale notification email failed to send:", error);
    }
    // ΜΕΤΑΤΡΟΠΗ ΑΠΑΝΤΗΣΗΣ ΓΙΑ ΤΗΝ ΕΦΑΡΜΟΓΗ
    return {
        customerId: response.psCustomerId?.toString() || "0",
        status: wholesaleRequested ? "pending_wholesale_approval" : "success",
        message,
        wholesaleApplicationRegistered,
        wholesaleApplicationError: wholesaleApplicationError || undefined
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
const loginCustomer = async (email, pass, countryIso = "GR") => {
    const payload = { action: "login", email: email.trim().toLowerCase(), password: pass };
    const normalizedCountryIso = countryIso.trim().toUpperCase() === "SE" ? "SE" : "GR";
    const response = await sendToPrestaShop(payload, normalizedCountryIso);
    const customerId = response?.id_customer ? Number(response.id_customer) : null;
    if (!customerId) {
        return {
            ...response,
            can_view_prices: false,
        };
    }
    const client = new PrestaShopClient_1.PrestaShopClient({ shopId: normalizedCountryIso === "SE" ? 1 : 4 });
    const priceAccess = await (0, priceAccessService_1.getPriceAccess)(client, customerId);
    return {
        ...response,
        can_view_prices: priceAccess.allowed,
    };
};
exports.loginCustomer = loginCustomer;
const syncFavoriteProduct = async (request) => {
    return sendToPrestaShop({
        action: "toggle_favorite_product",
        customerId: request.customerId,
        productId: request.productId,
        shopId: request.shopId,
        isFavorite: request.isFavorite === true,
        product: request.product ?? {}
    }, request.shopId === 1 ? "SE" : "GR");
};
exports.syncFavoriteProduct = syncFavoriteProduct;
const recordRecentProduct = async (request) => {
    return sendToPrestaShop({
        action: "record_recent_product",
        customerId: request.customerId,
        productId: request.productId,
        shopId: request.shopId,
        product: request.product ?? {}
    }, request.shopId === 1 ? "SE" : "GR");
};
exports.recordRecentProduct = recordRecentProduct;
const listFavoriteProducts = async (request) => {
    return sendToPrestaShop({
        action: "list_favorite_products",
        customerId: request.customerId,
        shopId: request.shopId
    }, request.shopId === 1 ? "SE" : "GR");
};
exports.listFavoriteProducts = listFavoriteProducts;
const listRecentProducts = async (request) => {
    return sendToPrestaShop({
        action: "list_recent_products",
        customerId: request.customerId,
        shopId: request.shopId,
        limit: request.limit ?? 20
    }, request.shopId === 1 ? "SE" : "GR");
};
exports.listRecentProducts = listRecentProducts;
const clearActivityTables = async (request) => {
    return sendToPrestaShop({
        action: "clear_activity_tables",
        shopId: request.shopId
    }, request.shopId === 1 ? "SE" : "GR");
};
exports.clearActivityTables = clearActivityTables;
const debugListWholesaleApplications = async (countryIso = "GR") => {
    return sendToPrestaShop({ action: "list_wholesale_applications" }, countryIso);
};
exports.debugListWholesaleApplications = debugListWholesaleApplications;
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
