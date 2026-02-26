import axios from "axios";
import crypto from "crypto";
import { config } from "../config/env";

export interface RegisterRequest {
  email: string;
  password?: string;
  firstName: string;
  lastName: string;
  countryIso?: string;
  street?: string;
  city?: string;
  postalCode?: string;
  phone?: string;
  company?: string;
  vatNumber?: string;
  newsletter?: boolean;
}

const resolveSyncUrl = (countryIso: string = "GR"): string => {
  const shopId = countryIso.trim().toUpperCase() === "SE" ? 1 : 4;
  const baseUrl = config.shopBaseUrls[shopId] || config.prestashopBaseUrl;
  const url = new URL(baseUrl);
  const rootUrl = url.origin + url.pathname.replace(/\/api\/?$/, "");
  return `${rootUrl}/index.php?fc=module&module=grifoncustomersync&controller=sync`;
};

const createSignature = (payload: string, secret: string): { timestamp: string, signature: string } => {
  const timestamp = Math.floor(Date.now() / 1000).toString();
  const base = timestamp + payload; 
  const signature = crypto.createHmac("sha256", secret).update(base).digest("base64");
  return { timestamp, signature };
};

export const registerCustomer = async (request: RegisterRequest): Promise<any> => {
  const email = request.email.trim().toLowerCase();
  const payload = {
    externalCustomerId: email,
    customer: {
      email,
      firstname: request.firstName,
      lastname: request.lastName,
      password: request.password, 
      company: request.company || "",
      newsletter: request.newsletter ? 1 : 0,
      active: 0
    },
    addresses: [{
      externalAddressId: `addr_${email}`,
      alias: "Default",
      firstname: request.firstName, 
      lastname: request.lastName,
      address1: request.street || "",
      postcode: request.postalCode || "",
      city: request.city || "",
      countryIso: request.countryIso || "GR",
      vat_number: request.vatNumber || "",
      dni: request.vatNumber || "000000000"
    }]
  };

  return sendToPrestaShop(payload, request.countryIso || "GR");
};

/**
 * Ενημέρωση Προφίλ Χρήστη
 */
export const updateProfile = async (request: RegisterRequest): Promise<any> => {
  const payload = {
    action: "sync", // Επαναχρησιμοποιούμε τη handleSync της PHP που κάνει upsert
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

export const loginCustomer = async (email: string, pass: string): Promise<any> => {
  const payload = { action: "login", email: email.trim().toLowerCase(), password: pass };
  return sendToPrestaShop(payload, "GR");
};

async function sendToPrestaShop(payload: any, countryIso: string) {
  const body = JSON.stringify(payload);
  const secret = config.customerSyncSecret || config.prestashopApiKey;
  const { timestamp, signature } = createSignature(body, secret);
  const syncUrl = resolveSyncUrl(countryIso);

  const response = await axios.post(syncUrl, body, {
    timeout: 15000,
    headers: {
      "Content-Type": "application/json",
      "X-Grifon-Timestamp": timestamp,
      "X-Grifon-Signature": signature
    },
    transformRequest: [(data) => data],
    validateStatus: () => true
  });

  if (response.status === 200 && response.data?.ok === true) return response.data;
  throw new Error(response.data?.message || response.data?.error || "Communication error");
}
