import axios from "axios";
import { config } from "../config/env";

export interface PayPalCheckoutSession {
  paypalOrderId: string;
  approvalUrl: string;
}

export interface PayPalCaptureResult {
  captureId: string | null;
}

interface PayPalAccessTokenResponse {
  access_token: string;
}

const isPayPalConfigured = (): boolean => {
  return Boolean(config.paypalClientId && config.paypalClientSecret);
};

const getPayPalAccessToken = async (): Promise<string> => {
  const credentials = Buffer.from(
    `${config.paypalClientId}:${config.paypalClientSecret}`
  ).toString("base64");

  const response = await axios.post<PayPalAccessTokenResponse>(
    `${config.paypalApiBaseUrl}/v1/oauth2/token`,
    "grant_type=client_credentials",
    {
      headers: {
        Authorization: `Basic ${credentials}`,
        "Content-Type": "application/x-www-form-urlencoded"
      }
    }
  );

  return response.data.access_token;
};

export const createPayPalCheckoutSession = async (input: {
  orderReference: string;
  totalAmount: number;
  currency: string;
}): Promise<PayPalCheckoutSession | null> => {
  if (!isPayPalConfigured()) {
    return null;
  }

  const accessToken = await getPayPalAccessToken();
  const response = await axios.post<any>(
    `${config.paypalApiBaseUrl}/v2/checkout/orders`,
    {
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
      application_context: {
        return_url: config.paypalReturnUrl,
        cancel_url: config.paypalCancelUrl
      }
    },
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json"
      }
    }
  );

  const approvalUrl = response.data?.links?.find((link: any) => link?.rel === "approve")?.href;
  if (!response.data?.id || !approvalUrl) {
    throw new Error("PayPal did not return an approval URL.");
  }

  return {
    paypalOrderId: response.data.id,
    approvalUrl
  };
};

export const capturePayPalCheckoutSession = async (
  paypalOrderId: string
): Promise<PayPalCaptureResult> => {
  if (!isPayPalConfigured()) {
    throw new Error("PayPal payment bridge is not configured.");
  }

  const accessToken = await getPayPalAccessToken();
  const response = await axios.post<any>(
    `${config.paypalApiBaseUrl}/v2/checkout/orders/${paypalOrderId}/capture`,
    {},
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json"
      }
    }
  );

  const captureId =
    response.data?.purchase_units?.[0]?.payments?.captures?.[0]?.id ??
    response.data?.id ??
    null;

  return { captureId };
};
