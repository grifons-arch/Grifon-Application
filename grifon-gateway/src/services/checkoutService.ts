import { ShopId, shops } from "../config/env";
import { PrestaShopClient } from "../clients/PrestaShopClient";
import { getPriceAccess } from "./priceAccessService";
import { capturePayPalCheckoutSession, createPayPalCheckoutSession } from "./paypalService";

export interface CheckoutPaymentMethod {
  code: string;
  title: string;
  type: "redirect" | "onsite" | "manual";
  available: boolean;
}

export interface CheckoutShippingMethod {
  code: string;
  title: string;
  type: "carrier_selection" | "shipping_quote" | "manual";
  available: boolean;
}

export interface CheckoutSession {
  shopId: ShopId;
  customerId: number | null;
  isLoggedIn: boolean;
  canViewPrices: boolean;
  canCheckout: boolean;
  mode: "storefront_handoff";
  storefrontBaseUrl: string;
  cartUrl: string;
  checkoutUrl: string;
  loginUrl: string;
  paymentMethods: CheckoutPaymentMethod[];
  shippingMethods: CheckoutShippingMethod[];
  notes: string[];
}

export interface CheckoutOrderAddress {
  recipient: string;
  email: string;
  phone: string;
  company?: string;
  street: string;
  city: string;
  postalCode: string;
  country: string;
}

export interface CheckoutOrderItem {
  productId: number;
  title: string;
  qty: number;
  unitPrice: number;
  currency: string;
}

export interface CreateCheckoutOrderInput {
  shopId: ShopId;
  customerId: number;
  paymentMethodCode: string;
  shippingMethodCode: string;
  address: CheckoutOrderAddress;
  items: CheckoutOrderItem[];
}

export interface CheckoutOrder {
  orderReference: string;
  shopId: ShopId;
  customerId: number;
  totalAmount: number;
  currency: string;
  paymentMethodCode: string;
  shippingMethodCode: string;
  paymentStatus: "requires_payment" | "paid";
  orderStatus: "draft" | "paid";
  paymentProvider: "paypal" | "stripe_cards" | "manual";
  paymentSessionStatus: "pending_bridge" | "approval_required" | "paid";
  paymentSessionMessage: string;
  paymentSessionUrl?: string;
  paymentSessionId?: string;
  paymentCaptureId?: string;
}

const withTrailingSlash = (value: string): string => {
  return value.endsWith("/") ? value : `${value}/`;
};

export const buildStorefrontBaseUrl = (shopId: ShopId): string => {
  const shop = shops.find((item) => item.id === shopId);
  const domain = shop?.domain ?? "grifon.gr";
  return withTrailingSlash(`https://${domain}`);
};

const buildPaymentMethods = (): CheckoutPaymentMethod[] => {
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

const buildShippingMethods = (): CheckoutShippingMethod[] => {
  return [
    {
      code: "prestashippingquote",
      title: "Shipping quote and carrier selection",
      type: "shipping_quote",
      available: true,
    },
  ];
};

const orderStore = new Map<string, CheckoutOrder>();

export const getCheckoutSession = async (
  client: PrestaShopClient,
  shopId: ShopId,
  customerId?: number
): Promise<CheckoutSession> => {
  const storefrontBaseUrl = buildStorefrontBaseUrl(shopId);
  const isLoggedIn = typeof customerId === "number" && customerId > 0;
  const canViewPrices = isLoggedIn
    ? (await getPriceAccess(client, customerId)).allowed
    : false;

  const notes: string[] = [];
  if (!isLoggedIn) {
    notes.push("Login is required before the checkout can continue.");
  } else if (!canViewPrices) {
    notes.push("Wholesale approval is required before prices and ordering become available.");
  } else {
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

export const getCheckoutHandoffUrl = (
  shopId: ShopId,
  target: "cart" | "checkout" | "login"
): string => {
  const storefrontBaseUrl = buildStorefrontBaseUrl(shopId);
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

const resolvePaymentProvider = (paymentMethodCode: string): "paypal" | "stripe_cards" | "manual" => {
  if (paymentMethodCode === "paypal") {
    return "paypal";
  }
  if (paymentMethodCode === "stripe_cards") {
    return "stripe_cards";
  }
  return "manual";
};

export const createCheckoutOrder = async (
  client: PrestaShopClient,
  input: CreateCheckoutOrderInput
): Promise<CheckoutOrder> => {
  const access = await getPriceAccess(client, input.customerId);
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

  const order: CheckoutOrder = {
    orderReference,
    shopId: input.shopId,
    customerId: input.customerId,
    totalAmount,
    currency,
    paymentMethodCode: input.paymentMethodCode,
    shippingMethodCode: input.shippingMethodCode,
    paymentStatus: "requires_payment",
    orderStatus: "draft",
    paymentProvider,
    paymentSessionStatus: "pending_bridge",
    paymentSessionMessage:
      paymentProvider === "paypal"
        ? "PayPal payment bridge is not configured yet in the gateway."
        : paymentProvider === "stripe_cards"
          ? "Stripe card payment bridge is not configured yet in the gateway."
          : "Payment bridge is not configured yet in the gateway."
  };

  if (paymentProvider === "paypal") {
    const paypalSession = await createPayPalCheckoutSession({
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

  orderStore.set(orderReference, order);
  return order;
};

export const getCheckoutOrder = (orderReference: string): CheckoutOrder | null => {
  return orderStore.get(orderReference) ?? null;
};

export const captureCheckoutOrder = async (orderReference: string): Promise<CheckoutOrder> => {
  const order = orderStore.get(orderReference);
  if (!order) {
    throw Object.assign(new Error("Checkout order was not found."), {
      status: 404,
      code: "CHECKOUT_ORDER_NOT_FOUND"
    });
  }

  if (order.paymentProvider !== "paypal") {
    throw Object.assign(new Error("Capture is supported only for PayPal checkout orders."), {
      status: 400,
      code: "PAYMENT_CAPTURE_NOT_SUPPORTED"
    });
  }

  if (!order.paymentSessionId) {
    throw Object.assign(new Error("PayPal payment session has not been created."), {
      status: 409,
      code: "PAYPAL_SESSION_MISSING"
    });
  }

  const capture = await capturePayPalCheckoutSession(order.paymentSessionId);
  order.paymentStatus = "paid";
  order.paymentSessionStatus = "paid";
  order.paymentSessionMessage = "PayPal payment completed successfully.";
  order.paymentCaptureId = capture.captureId ?? undefined;
  order.orderStatus = "paid";

  orderStore.set(orderReference, order);
  return order;
};
