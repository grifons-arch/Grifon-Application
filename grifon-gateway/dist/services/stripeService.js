"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.verifyStripeCheckoutSession = exports.createStripeCheckoutSession = void 0;
const axios_1 = __importDefault(require("axios"));
const env_1 = require("../config/env");
const isConfigured = () => Boolean(env_1.config.stripeSecretKey);
const withOrderReference = (baseUrl, orderReference) => {
    const url = new URL(baseUrl);
    url.searchParams.set("orderReference", orderReference);
    return url.toString();
};
const authHeaders = () => {
    if (!isConfigured()) {
        throw Object.assign(new Error("Stripe credentials are not configured."), {
            status: 503,
            code: "STRIPE_NOT_CONFIGURED"
        });
    }
    return {
        Authorization: `Bearer ${env_1.config.stripeSecretKey}`,
        "Content-Type": "application/x-www-form-urlencoded"
    };
};
const createStripeCheckoutSession = async (input) => {
    if (!isConfigured()) {
        return null;
    }
    const amountInMinorUnits = Math.round(input.totalAmount * 100);
    const body = new URLSearchParams({
        mode: "payment",
        success_url: withOrderReference(env_1.config.stripeReturnUrl, input.orderReference),
        cancel_url: withOrderReference(env_1.config.stripeCancelUrl, input.orderReference),
        "line_items[0][quantity]": "1",
        "line_items[0][price_data][currency]": input.currency.toLowerCase(),
        "line_items[0][price_data][unit_amount]": amountInMinorUnits.toString(),
        "line_items[0][price_data][product_data][name]": input.itemTitle,
        client_reference_id: input.orderReference
    });
    const response = await axios_1.default.post(`${env_1.config.stripeApiBaseUrl}/v1/checkout/sessions`, body.toString(), { headers: authHeaders() });
    const stripeSessionId = response.data?.id;
    const checkoutUrl = response.data?.url;
    if (typeof stripeSessionId !== "string" || typeof checkoutUrl !== "string") {
        throw Object.assign(new Error("Stripe checkout session response was invalid."), {
            status: 502,
            code: "STRIPE_SESSION_ERROR",
            details: response.data
        });
    }
    return {
        stripeSessionId,
        checkoutUrl
    };
};
exports.createStripeCheckoutSession = createStripeCheckoutSession;
const verifyStripeCheckoutSession = async (stripeSessionId) => {
    const response = await axios_1.default.get(`${env_1.config.stripeApiBaseUrl}/v1/checkout/sessions/${stripeSessionId}`, { headers: authHeaders() });
    const status = response.data?.status;
    const paymentStatus = response.data?.payment_status;
    if (typeof status !== "string" || typeof paymentStatus !== "string") {
        throw Object.assign(new Error("Stripe checkout verification response was invalid."), {
            status: 502,
            code: "STRIPE_VERIFY_ERROR",
            details: response.data
        });
    }
    return {
        stripeSessionId,
        status,
        paymentStatus
    };
};
exports.verifyStripeCheckoutSession = verifyStripeCheckoutSession;
