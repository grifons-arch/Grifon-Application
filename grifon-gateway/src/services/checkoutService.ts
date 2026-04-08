import { ShopId, shops } from "../config/env";
import { PrestaShopClient } from "../clients/PrestaShopClient";
import { getPriceAccess } from "./priceAccessService";

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
