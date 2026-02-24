import axios from "axios";
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

const resolveSyncUrl = (countryIso: string): string => {
  const shopId = countryIso.trim().toUpperCase() === "SE" ? 1 : 4;
  const baseUrl = config.shopBaseUrls[shopId] || config.prestashopBaseUrl;
  const url = new URL(baseUrl);
  const rootUrl = url.origin + url.pathname.replace(/\/api\/?$/, "");
  return `${rootUrl}/index.php?fc=module&module=grifoncustomersync&controller=sync`;
};

/**
 * Υπολογισμός υπογραφής HMAC-SHA256 (Base64)
 * Σύμφωνα με την αλλαγή στην PHP: $base = $ts . $rawBody;
 */
const createSignature = (payload: string, secret: string): { timestamp: string, signature: string } => {
  const timestamp = Math.floor(Date.now() / 1000).toString();
  // Ενώνουμε timestamp και payload απευθείας (χωρίς \n) για να συμβαδίζει με την PHP
  const base = timestamp + payload; 
  
  const signature = crypto
    .createHmac("sha256", secret)
    .update(base) 
    .digest("base64");
    
  return { timestamp, signature };
};

export const registerCustomer = async (request: RegisterRequest): Promise<RegisterResponse> => {
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

  const body = JSON.stringify(payload);
  const secret = config.customerSyncSecret || config.prestashopApiKey;
  const { timestamp, signature } = createSignature(body, secret);
  const syncUrl = resolveSyncUrl(request.countryIso);

  console.log("--- FINAL SYNC DEBUG ---");
  console.log("Secret used:", secret);
  console.log("Timestamp:", timestamp);
  console.log("Signature (Base64):", signature);

  try {
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

    console.log("PrestaShop Response:", JSON.stringify(response.data));

    if (response.status === 200 && response.data?.ok !== false) {
      return {
        customerId: String(response.data?.psCustomerId || email),
        status: "SUCCESS",
        message: "Η εγγραφή ολοκληρώθηκε! Αναμένεται έγκριση από τη Grifon."
      };
    }

    throw new Error(response.data?.error || response.data?.message || `Error ${response.status}`);
  } catch (error: any) {
    console.error("Sync Error:", error.message);
    throw { status: 502, message: `Σφάλμα εγγραφής: ${error.message}.` };
  }
};
