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

// Νέα λογική υπογραφής: Base64 με Timestamp (Standard Pattern)
const createSignature = (payload: string, secret: string): { timestamp: string, signature: string } => {
  const timestamp = Math.floor(Date.now() / 1000).toString();
  const signature = crypto
    .createHmac("sha256", secret)
    .update(timestamp + payload) 
    .digest("base64"); // Αλλαγή σε Base64
  return { timestamp, signature };
};

export const registerCustomer = async (request: RegisterRequest): Promise<RegisterResponse> => {
  const email = request.email.trim().toLowerCase();
  const hashedPassword = await bcrypt.hash(request.password, PASSWORD_SALT_ROUNDS);
  
  const payload = {
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

  const body = JSON.stringify(payload);
  const secret = process.env.GRIFON_CUSTOMER_SYNC_SECRET || config.prestashopApiKey;
  const { timestamp, signature } = createSignature(body, secret);
  const syncUrl = resolveSyncUrl(request.countryIso);

  console.log("Registration Attempt:", email);
  console.log("Using Signature (Base64):", signature);

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

    console.log("PrestaShop Response Status:", response.status);
    console.log("PrestaShop Response Data:", JSON.stringify(response.data));

    if (response.status >= 200 && response.status < 300 && response.data?.ok !== false) {
      return {
        customerId: String(response.data?.id_customer || email),
        status: "SUCCESS",
        message: "Η εγγραφή ολοκληρώθηκε! Αναμένεται έγκριση από τη Grifon."
      };
    }

    const errorDetail = response.data?.error || response.data?.message || `Error ${response.status}`;
    throw new Error(errorDetail);
  } catch (error: any) {
    console.error("Sync Error:", error.message);
    throw { status: 502, message: `Σφάλμα εγγραφής: ${error.message}.` };
  }
};
