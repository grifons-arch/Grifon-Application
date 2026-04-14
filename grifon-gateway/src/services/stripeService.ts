import axios from "axios";
import { config } from "../config/env";

export interface StripeCheckoutSession {
  stripeSessionId: string;
  checkoutUrl: string;
}

export interface StripeVerificationResult {
  stripeSessionId: string;
  status: string | null;
  paymentStatus: string | null;
}

const isStripeConfigured = (): boolean => {
  return Boolean(config.stripeSecretKey);
};

const toMinorUnits = (amount: number): number => {
  return Math.round(amount * 100);
};

export const createStripeCheckoutSession = async (input: {
  orderReference: string;
  totalAmount: number;
  currency: string;
  itemTitle: string;
}): Promise<StripeCheckoutSession | null> => {
  if (!isStripeConfigured()) {
    return null;
  }

  const body = new URLSearchParams({
    mode: "payment",
    success_url: config.stripeReturnUrl,
    cancel_url: config.stripeCancelUrl,
    client_reference_id: input.orderReference,
    "metadata[orderReference]": input.orderReference,
    "line_items[0][quantity]": "1",
    "line_items[0][price_data][currency]": input.currency.toLowerCase(),
    "line_items[0][price_data][unit_amount]": toMinorUnits(input.totalAmount).toString(),
    "line_items[0][price_data][product_data][name]": input.itemTitle
  });

  const response = await axios.post<any>(
    `${config.stripeApiBaseUrl}/v1/checkout/sessions`,
    body.toString(),
    {
      headers: {
        Authorization: `Bearer ${config.stripeSecretKey}`,
        "Content-Type": "application/x-www-form-urlencoded"
      }
    }
  );

  if (!response.data?.id || !response.data?.url) {
    throw new Error("Stripe did not return a checkout URL.");
  }

  return {
    stripeSessionId: response.data.id,
    checkoutUrl: response.data.url
  };
};

export const verifyStripeCheckoutSession = async (
  stripeSessionId: string
): Promise<StripeVerificationResult> => {
  if (!isStripeConfigured()) {
    throw new Error("Stripe card payment bridge is not configured.");
  }

  const response = await axios.get<any>(
    `${config.stripeApiBaseUrl}/v1/checkout/sessions/${stripeSessionId}`,
    {
      headers: {
        Authorization: `Bearer ${config.stripeSecretKey}`
      }
    }
  );

  return {
    stripeSessionId: response.data?.id ?? stripeSessionId,
    status: response.data?.status ?? null,
    paymentStatus: response.data?.payment_status ?? null
  };
};
