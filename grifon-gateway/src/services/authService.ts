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

const PASSWORD_SALT_ROUNDS = 10;

const resolveSyncUrl = (countryIso: string): string => {
  const shopId = countryIso.trim().toUpperCase() === "SE" ? 1 : 4;
  const baseUrl = config.shopBaseUrls[shopId] || config.prestashopBaseUrl;
  const url = new URL(baseUrl);
  const rootUrl = url.origin + url.pathname.replace(/\/api\/?$/, "");
  return `${rootUrl}/index.php?fc=module&module=grifoncustomersync&controller=sync`;
};

/**
 * Υπολογισμός υπογραφής HMAC-SHA256 (HEX)
 * Σημαντικό: Ο τρόπος υπολογισμού (timestamp + payload) πρέπει να είναι ΙΔΙΟΣ στην PHP πλευρά.
 */
const createSignature = (payload: string, secret: string): { timestamp: string, signature: string } => {
  const timestamp = Math.floor(Date.now() / 1000).toString();
  const signature = crypto
    .createHmac("sha256", secret)
    .update(timestamp + payload) 
    .digest("hex");
  return { timestamp, signature };
};

export const registerCustomer = async (request: RegisterRequest): Promise<RegisterResponse> => {
  const email = request.email.trim().toLowerCase();
  
  // Το password πρέπει να σταλεί όπως είναι (raw) ή hashed αν το module κάνει direct SQL insert.
  // Συνήθως τα PrestaShop modules προτιμούν το raw και κάνουν hash εσωτερικά με το shop key.
  // Δοκιμάζουμε αποστολή RAW password πρώτα (πιο κοινό για API sync).
  const passwordToSend = request.password; 
  
  const payload = {
    externalCustomerId: email,
    customer: {
      email,
      firstname: request.firstName,
      lastname: request.lastName,
      password: passwordToSend,
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
  
  // Το secret πρέπει να είναι το GRIFON_CUSTOMER_SYNC_SECRET από το .env του Gateway
  // και να είναι το ΙΔΙΟ με αυτό που έχει οριστεί στο module settings στο PrestaShop.
  const secret = config.customerSyncSecret || config.prestashopApiKey;
  
  const { timestamp, signature } = createSignature(body, secret);
  const syncUrl = resolveSyncUrl(request.countryIso);

  console.log("--- Registration Request ---");
  console.log("Email:", email);
  console.log("Sync URL:", syncUrl);
  console.log("Secret length:", secret.length);
  console.log("Timestamp:", timestamp);
  console.log("Signature (HEX):", signature);
  console.log("Payload:", body);

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

    console.log("PrestaShop Status:", response.status);
    console.log("PrestaShop Body:", JSON.stringify(response.data));

    if (response.status === 200 && response.data?.ok !== false) {
      return {
        customerId: String(response.data?.id_customer || email),
        status: "SUCCESS",
        message: "Η εγγραφή ολοκληρώθηκε! Αναμένεται έγκριση από τη Grifon."
      };
    }

    const errorDetail = response.data?.error || response.data?.message || `Error ${response.status}`;
    throw new Error(errorDetail);
  } catch (error: any) {
    console.error("Sync Error detail:", error.message);
    throw { status: 502, message: `Σφάλμα εγγραφής: ${error.message}.` };
  }
};
