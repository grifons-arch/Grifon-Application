"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.capturePayPalCheckoutSession = exports.createPayPalCheckoutSession = void 0;
const axios_1 = __importDefault(require("axios"));
const env_1 = require("../config/env");
const isConfigured = () => Boolean(env_1.config.paypalClientId && env_1.config.paypalClientSecret);
const getAccessToken = async () => {
    if (!isConfigured()) {
        throw Object.assign(new Error("PayPal credentials are not configured."), {
            status: 503,
            code: "PAYPAL_NOT_CONFIGURED"
        });
    }
    const credentials = Buffer.from(`${env_1.config.paypalClientId}:${env_1.config.paypalClientSecret}`).toString("base64");
    const response = await axios_1.default.post(`${env_1.config.paypalApiBaseUrl}/v1/oauth2/token`, new URLSearchParams({ grant_type: "client_credentials" }).toString(), {
        headers: {
            Authorization: `Basic ${credentials}`,
            "Content-Type": "application/x-www-form-urlencoded"
        }
    });
    const token = response.data?.access_token;
    if (typeof token !== "string" || token.trim().length === 0) {
        throw Object.assign(new Error("PayPal access token response was invalid."), {
            status: 502,
            code: "PAYPAL_AUTH_ERROR"
        });
    }
    return token;
};
const withOrderReference = (baseUrl, orderReference) => {
    const url = new URL(baseUrl);
    url.searchParams.set("orderReference", orderReference);
    return url.toString();
};
const createPayPalCheckoutSession = async (input) => {
    if (!isConfigured()) {
        return null;
    }
    const accessToken = await getAccessToken();
    const response = await axios_1.default.post(`${env_1.config.paypalApiBaseUrl}/v2/checkout/orders`, {
        intent: "CAPTURE",
        purchase_units: [
            {
                reference_id: input.orderReference,
                amount: {
                    currency_code: input.currency,
                    value: input.totalAmount.toFixed(2)
                }
            }
        ],
        payment_source: {
            paypal: {
                experience_context: {
                    return_url: withOrderReference(env_1.config.paypalReturnUrl, input.orderReference),
                    cancel_url: withOrderReference(env_1.config.paypalCancelUrl, input.orderReference),
                    user_action: "PAY_NOW",
                    shipping_preference: "NO_SHIPPING"
                }
            }
        }
    }, {
        headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "application/json"
        }
    });
    const paypalOrderId = response.data?.id;
    const links = Array.isArray(response.data?.links) ? response.data.links : [];
    const approvalUrl = links.find((link) => link?.rel == "payer-action")?.href
        ?? links.find((link) => link?.rel == "approve")?.href;
    if (typeof paypalOrderId !== "string" || typeof approvalUrl !== "string") {
        throw Object.assign(new Error("PayPal order session response was invalid."), {
            status: 502,
            code: "PAYPAL_SESSION_ERROR",
            details: response.data
        });
    }
    return {
        paypalOrderId,
        approvalUrl
    };
};
exports.createPayPalCheckoutSession = createPayPalCheckoutSession;
const capturePayPalCheckoutSession = async (paypalOrderId) => {
    const accessToken = await getAccessToken();
    const response = await axios_1.default.post(`${env_1.config.paypalApiBaseUrl}/v2/checkout/orders/${paypalOrderId}/capture`, {}, {
        headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "application/json"
        }
    });
    const status = response.data?.status;
    const purchaseUnits = Array.isArray(response.data?.purchase_units) ? response.data.purchase_units : [];
    const payments = purchaseUnits[0]?.payments;
    const captures = Array.isArray(payments?.captures) ? payments.captures : [];
    const captureId = typeof captures[0]?.id === "string" ? captures[0].id : null;
    if (typeof status !== "string" || status.trim().length === 0) {
        throw Object.assign(new Error("PayPal capture response was invalid."), {
            status: 502,
            code: "PAYPAL_CAPTURE_ERROR",
            details: response.data
        });
    }
    return {
        paypalOrderId,
        captureId,
        status
    };
};
exports.capturePayPalCheckoutSession = capturePayPalCheckoutSession;
