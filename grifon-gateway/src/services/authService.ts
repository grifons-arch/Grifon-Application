import axios from "axios";
import bcrypt from "bcryptjs";
import crypto from "crypto";
import { config } from "../config/env";

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  countryIso: string;
  street: string;
  city: string;
  postalCode: string;
  phone?: string;
  company?: string;
  vatNumber?: string;
  iban?: string;
  newsletter?: boolean;
  partnerOffers?: boolean;
}

export interface RegisterResponse {
  customerId: string;
  status: string;
  message: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  customerId: string;
  firstName: string;
  lastName: string;
}

const PASSWORD_SALT_ROUNDS = 10;

const resolveSyncUrl = (countryIso: string = "GR"): string => {
  const shopId = countryIso.trim().toUpperCase() === "SE" ? 1 : 4;
  const baseUrl = config.shopBaseUrls[shopId] || config.prestashopBaseUrl;
  const url = new URL(baseUrl);
  const rootUrl = url.origin + url.pathname.replace(/\/api\/?$/, "");
  return `${rootUrl}/index.php?fc=module&module=grifoncustomersync&controller=sync`;
};

/**
 * Creates a signature compatible with the PrestaShop grifoncustomersync module.
 * Format: base64(HMAC_SHA256("<timestamp>\n<body>", secret))
 */
const createSignature = (payload: string, secret: string): { timestamp: string, signature: string } => {
  const timestamp = Math.floor(Date.now() / 1000).toString();
  const signature = crypto
    .createHmac("sha256", secret)
    .update(timestamp + "\n" + payload) // Added \n to match PHP side: $ts . "\n" . $rawBody
    .digest("base64");
  return { timestamp, signature };
};

export const registerCustomer = async (request: RegisterRequest): Promise<RegisterResponse> => {
  const email = request.email.trim().toLowerCase();
  const hashedPassword = await bcrypt.hash(request.password, PASSWORD_SALT_ROUNDS);
  
  const payload = {
    action: "register",
    externalCustomerId: email,
    customer: {
      email,
      firstname: request.firstName,
      lastname: request.lastName,
      password: hashedPassword,
      company: request.company || "",
      newsletter: request.newsletter ? 1 : 0,
      optin: request.partnerOffers ? 1 : 0,
      active: 0
    },
    groups: { default: 3, list: [3] },
    addresses: [{
      alias: "Default",
      address1: request.street,
      postcode: request.postalCode,
      city: request.city,
      countryIso: request.countryIso,
      vat_number: request.vatNumber || "",
      phone: request.phone || ""
    }]
  };

  return await sendToPrestaShop(payload, request.countryIso);
};

export const loginCustomer = async (request: LoginRequest): Promise<LoginResponse> => {
  const payload = {
    action: "login",
    email: request.email.trim().toLowerCase(),
    password: request.password
  };

  try {
    const response = await sendToPrestaShop(payload, "GR");

    if (response.status === "SUCCESS") {
      return {
        token: response.token || "fake-jwt-token",
        customerId: response.customerId,
        firstName: response.firstName || "",
        lastName: response.lastName || ""
      };
    }

    const err: any = new Error(response.message || "Login failed");
    err.status = 401;
    throw err;
  } catch (error: any) {
    if (error.status) throw error;
    const err: any = new Error(error.message || "Authentication failed");
    err.status = 401;
    throw err;
  }
};

const sendToPrestaShop = async (payload: any, countryIso: string): Promise<any> => {
  const body = JSON.stringify(payload);
  const secret = process.env.GRIFON_CUSTOMER_SYNC_SECRET || config.prestashopApiKey;
  const { timestamp, signature } = createSignature(body, secret);
  const syncUrl = resolveSyncUrl(countryIso);

  try {
    const response = await axios.post(syncUrl, body, {
      timeout: 15000,
      headers: {
        "Content-Type": "application/json",
        "X-Grifon-Timestamp": timestamp,
        "X-Grifon-Signature": signature
      },
      validateStatus: () => true
    });

    if (response.status >= 200 && response.status < 300) {
      return response.data;
    }

    const errorDetail = response.data?.message || response.data?.error || `PrestaShop Error ${response.status}`;
    const err: any = new Error(errorDetail);
    err.status = response.status === 401 || response.status === 403 ? 401 : 502;
    throw err;
  } catch (error: any) {
    if (error.status) throw error;
    const err: any = new Error(error.message || "Failed to communicate with PrestaShop");
    err.status = 502;
    throw err;
  }
};
