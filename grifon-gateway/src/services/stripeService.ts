import axios from "axios";
import { config } from "../config/env";

export interface StripeCheckoutSession {
  stripeSessionId: string;
  checkoutUrl: string;
}

export interface StripeCheckoutVerification {
  stripeSessionId: string;
  status: string;
  paymentStatus: string;
}

const isConfigured = (): boolean => Boolean(config.stripeSecretKey);

const withOrderReference = (baseUrl: string, orderReference: string): string => {
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
    Authorization: `Bearer ${config.stripeSecretKey}`,
    "Content-Type": "application/x-www-form-urlencoded"
  };
};

export const createStripeCheckoutSession = async (input: {
  orderReference: string;
  totalAmount: number;
  currency: string;
  itemTitle: string;
}): Promise<StripeCheckoutSession | null> => {
  if (!isConfigured()) {
    return null;
  }

  const amountInMinorUnits = Math.round(input.totalAmount * 100);
  const body = new URLSearchParams({
    mode: "payment",
    success_url: withOrderReference(config.stripeReturnUrl, input.orderReference),
    cancel_url: withOrderReference(config.stripeCancelUrl, input.orderReference),
    "line_items[0][quantity]": "1",
    "line_items[0][price_data][currency]": input.currency.toLowerCase(),
    "line_items[0][price_data][unit_amount]": amountInMinorUnits.toString(),
    "line_items[0][price_data][product_data][name]": input.itemTitle,
    client_reference_id: input.orderReference
  });

  const response = await axios.post(
    `${config.stripeApiBaseUrl}/v1/checkout/sessions`,
    body.toString(),
    { headers: authHeaders() }
  );

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

export const verifyStripeCheckoutSession = async (
  stripeSessionId: string
): Promise<StripeCheckoutVerification> => {
  const response = await axios.get(
    `${config.stripeApiBaseUrl}/v1/checkout/sessions/${stripeSessionId}`,
    { headers: authHeaders() }
  );

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
