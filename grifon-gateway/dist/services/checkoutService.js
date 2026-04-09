"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.captureCheckoutOrder = exports.getCheckoutOrder = exports.createCheckoutOrder = exports.getCheckoutHandoffUrl = exports.getCheckoutSession = exports.buildStorefrontBaseUrl = void 0;
const env_1 = require("../config/env");
const priceAccessService_1 = require("./priceAccessService");
const paypalService_1 = require("./paypalService");
const stripeService_1 = require("./stripeService");
const withTrailingSlash = (value) => {
    return value.endsWith("/") ? value : `${value}/`;
};
const buildStorefrontBaseUrl = (shopId) => {
    const shop = env_1.shops.find((item) => item.id === shopId);
    const domain = shop?.domain ?? "grifon.gr";
    return withTrailingSlash(`https://${domain}`);
};
exports.buildStorefrontBaseUrl = buildStorefrontBaseUrl;
const buildPaymentMethods = () => {
    return [
        {
            code: "paypal",
            title: "PayPal",
            type: "redirect",
            available: true,
        },
        {
            code: "stripe_cards",
            title: "Cards via Stripe",
            type: "onsite",
            available: true,
        },
    ];
};
const buildShippingMethods = () => {
    return [
        {
            code: "prestashippingquote",
            title: "Shipping quote and carrier selection",
            type: "shipping_quote",
            available: true,
        },
    ];
};
const orderStore = new Map();
const getCheckoutSession = async (client, shopId, customerId) => {
    const storefrontBaseUrl = (0, exports.buildStorefrontBaseUrl)(shopId);
    const isLoggedIn = typeof customerId === "number" && customerId > 0;
    const canViewPrices = isLoggedIn
        ? (await (0, priceAccessService_1.getPriceAccess)(client, customerId)).allowed
        : false;
    const notes = [];
    if (!isLoggedIn) {
        notes.push("Login is required before the checkout can continue.");
    }
    else if (!canViewPrices) {
        notes.push("Wholesale approval is required before prices and ordering become available.");
    }
    else {
        notes.push("Complete payment and order confirmation on the secure storefront checkout.");
    }
    return {
        shopId,
        customerId: isLoggedIn ? customerId ?? null : null,
        isLoggedIn,
        canViewPrices,
        canCheckout: isLoggedIn && canViewPrices,
        mode: "storefront_handoff",
        storefrontBaseUrl,
        cartUrl: `${storefrontBaseUrl}index.php?controller=cart`,
        checkoutUrl: `${storefrontBaseUrl}index.php?controller=order`,
        loginUrl: `${storefrontBaseUrl}index.php?controller=order&login=1`,
        paymentMethods: buildPaymentMethods(),
        shippingMethods: buildShippingMethods(),
        notes,
    };
};
exports.getCheckoutSession = getCheckoutSession;
const getCheckoutHandoffUrl = (shopId, target) => {
    const storefrontBaseUrl = (0, exports.buildStorefrontBaseUrl)(shopId);
    switch (target) {
        case "cart":
            return `${storefrontBaseUrl}index.php?controller=cart`;
        case "login":
            return `${storefrontBaseUrl}index.php?controller=order&login=1`;
        case "checkout":
        default:
            return `${storefrontBaseUrl}index.php?controller=order`;
    }
};
exports.getCheckoutHandoffUrl = getCheckoutHandoffUrl;
const resolvePaymentProvider = (paymentMethodCode) => {
    if (paymentMethodCode === "paypal") {
        return "paypal";
    }
    if (paymentMethodCode === "stripe_cards") {
        return "stripe_cards";
    }
    return "manual";
};
const createCheckoutOrder = async (client, input) => {
    const access = await (0, priceAccessService_1.getPriceAccess)(client, input.customerId);
    if (!access.allowed) {
        const reason = !access.active
            ? "customer_inactive"
            : !access.hasWholesaleGroup
                ? "missing_wholesale_group"
                : "checkout_not_allowed";
        throw Object.assign(new Error(`Wholesale approval is required before checkout. (${reason})`), {
            status: 403,
            code: "CHECKOUT_NOT_ALLOWED",
            details: access
        });
    }
    if (!input.items.length) {
        throw Object.assign(new Error("Cart is empty."), {
            status: 400,
            code: "EMPTY_CART"
        });
    }
    const totalAmount = input.items.reduce((sum, item) => sum + item.qty * item.unitPrice, 0);
    const currency = input.items[0]?.currency || "EUR";
    const orderReference = `ORD-${Date.now().toString().slice(-8)}`;
    const paymentProvider = resolvePaymentProvider(input.paymentMethodCode);
    const order = {
        orderReference,
        shopId: input.shopId,
        customerId: input.customerId,
        items: input.items,
        totalAmount,
        currency,
        paymentMethodCode: input.paymentMethodCode,
        shippingMethodCode: input.shippingMethodCode,
        paymentStatus: "requires_payment",
        orderStatus: "draft",
        paymentProvider,
        paymentSessionStatus: "pending_bridge",
        paymentSessionMessage: paymentProvider === "paypal"
            ? "PayPal payment bridge is not configured yet in the gateway."
            : paymentProvider === "stripe_cards"
                ? "Stripe card payment bridge is not configured yet in the gateway."
                : "Payment bridge is not configured yet in the gateway."
    };
    if (paymentProvider === "paypal") {
        const paypalSession = await (0, paypalService_1.createPayPalCheckoutSession)({
            orderReference,
            totalAmount,
            currency
        });
        if (paypalSession) {
            order.paymentSessionStatus = "approval_required";
            order.paymentSessionMessage = "Approve the PayPal payment inside the app to complete checkout.";
            order.paymentSessionUrl = paypalSession.approvalUrl;
            order.paymentSessionId = paypalSession.paypalOrderId;
        }
    }
    if (paymentProvider === "stripe_cards") {
        const itemTitle = input.items.length === 1
            ? input.items[0].title
            : `${input.items[0].title} + ${input.items.length - 1} more item(s)`;
        const stripeSession = await (0, stripeService_1.createStripeCheckoutSession)({
            orderReference,
            totalAmount,
            currency,
            itemTitle
        });
        if (stripeSession) {
            order.paymentSessionStatus = "approval_required";
            order.paymentSessionMessage = "Complete the Stripe card payment inside the app to finish checkout.";
            order.paymentSessionUrl = stripeSession.checkoutUrl;
            order.paymentSessionId = stripeSession.stripeSessionId;
        }
    }
    orderStore.set(orderReference, order);
    return order;
};
exports.createCheckoutOrder = createCheckoutOrder;
const getCheckoutOrder = (orderReference) => {
    return orderStore.get(orderReference) ?? null;
};
exports.getCheckoutOrder = getCheckoutOrder;
const captureCheckoutOrder = async (orderReference) => {
    const order = orderStore.get(orderReference);
    if (!order) {
        throw Object.assign(new Error("Checkout order was not found."), {
            status: 404,
            code: "CHECKOUT_ORDER_NOT_FOUND"
        });
    }
    if (!order.paymentSessionId) {
        throw Object.assign(new Error("Payment session has not been created."), {
            status: 409,
            code: "PAYMENT_SESSION_MISSING"
        });
    }
    if (order.paymentProvider === "paypal") {
        const capture = await (0, paypalService_1.capturePayPalCheckoutSession)(order.paymentSessionId);
        order.paymentStatus = "paid";
        order.paymentSessionStatus = "paid";
        order.paymentSessionMessage = "PayPal payment completed successfully.";
        order.paymentCaptureId = capture.captureId ?? undefined;
        order.orderStatus = "paid";
        orderStore.set(orderReference, order);
        return order;
    }
    if (order.paymentProvider === "stripe_cards") {
        const verification = await (0, stripeService_1.verifyStripeCheckoutSession)(order.paymentSessionId);
        if (verification.status !== "complete" || verification.paymentStatus !== "paid") {
            throw Object.assign(new Error("Stripe payment has not completed yet."), {
                status: 409,
                code: "STRIPE_PAYMENT_INCOMPLETE",
                details: verification
            });
        }
        order.paymentStatus = "paid";
        order.paymentSessionStatus = "paid";
        order.paymentSessionMessage = "Stripe card payment completed successfully.";
        order.paymentCaptureId = verification.stripeSessionId;
        order.orderStatus = "paid";
        orderStore.set(orderReference, order);
        return order;
    }
    throw Object.assign(new Error("Capture is not supported for this payment provider."), {
        status: 400,
        code: "PAYMENT_CAPTURE_NOT_SUPPORTED"
    });
};
exports.captureCheckoutOrder = captureCheckoutOrder;
