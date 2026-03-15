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
  wholesaleRequested?: boolean;
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

  // Χρήση ΑΦΜ αν υπάρχει, αλλιώς ένα σταθερό 8-ψήφιο (π.χ. 12345678)
  // Πολύ σημαντικό: Μερικά PrestaShop δεν δέχονται το "000000000"
  const dniValue = (request.vatNumber && request.vatNumber.trim().length > 5)
    ? request.vatNumber.trim()
    : "12345678";

  const payload = {
    externalCustomerId: email,
    customer: {
      email,
      firstname: request.firstName,
      lastname: request.lastName,
      password: request.password, 
      company: request.company || "",
      newsletter: request.newsletter ? 1 : 0,
      active: 1,
      is_wholesale: request.wholesaleRequested ? 1 : 0,
      siret: request.vatNumber || "",
      ape: "",
      website: ""
    },
    addresses: [{
      externalAddressId: `addr_${email}`,
      alias: "Default",
      firstname: request.firstName, 
      lastname: request.lastName,
      address1: request.street || "Δεν δηλώθηκε οδός",
      address2: "",
      postcode: (request.postalCode || "00000").replace(/\s/g, ""),
      city: request.city || "Δεν δηλώθηκε πόλη",
      countryIso: (request.countryIso || "GR").toUpperCase(),
      phone: request.phone || "0000000000",
      phone_mobile: request.phone || "",
      vat_number: request.vatNumber || "",
      // Στέλνουμε το DNI σε όλα τα πιθανά πεδία που μπορεί να το περιμένει το PrestaShop
      dni: dniValue,
      identification_number: dniValue,
      dni_number: dniValue
    }]
  };

  return sendToPrestaShop(payload, request.countryIso || "GR");
};

export const updateProfile = async (request: RegisterRequest): Promise<any> => {
  const payload = {
    action: "sync",
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
  throw new Error(response.data?.message || response.data?.error || `PrestaShop Error: ${response.status}`);
}
