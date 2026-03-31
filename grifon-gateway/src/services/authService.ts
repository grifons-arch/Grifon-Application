import axios from "axios";
import crypto from "crypto";
import { config } from "../config/env";
import { PrestaShopClient } from "../clients/PrestaShopClient";
import { getPriceAccess } from "./priceAccessService";
import { notifyWholesaleRequest } from "./wholesaleNotificationService";

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

export interface ProductActivityRequest {
  customerId: number;
  shopId: 1 | 4;
  productId: number;
  isFavorite?: boolean;
  product?: {
    title?: string;
    price?: number | null;
    currency?: string;
    imageUrl?: string;
    brand?: string;
  };
}

export interface ProductActivityListRequest {
  customerId: number;
  shopId: 1 | 4;
  limit?: number;
}

export interface ProductActivityClearRequest {
  shopId: 1 | 4;
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
  const countryIso = (request.countryIso || "GR").trim().toUpperCase();

  const dniValue = (request.vatNumber && request.vatNumber.trim().length >= 9)
    ? request.vatNumber.trim()
    : "123456789";

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
      siret: dniValue,
      dni: dniValue
    },
    application: request.wholesaleRequested ? {
      requested: true,
      status: "pending",
      source: "grifon_gateway",
      submittedAt: new Date().toISOString(),
      email,
      firstName: request.firstName,
      lastName: request.lastName,
      phone: request.phone || "",
      company: request.company || "",
      vatNumber: request.vatNumber || dniValue,
      countryIso,
      city: request.city || "",
      street: request.street || "",
      postalCode: request.postalCode || ""
    } : undefined,
    addresses: [{
      externalAddressId: `addr_${email}`,
      alias: "Default",
      firstname: request.firstName,
      lastname: request.lastName,
      address1: request.street || "Δεν δηλώθηκε οδός",
      postcode: (request.postalCode || "00000").replace(/\s/g, ""),
      city: request.city || "Δεν δηλώθηκε πόλη",
      countryIso,
      phone: request.phone || "0000000000",
      vat_number: dniValue,
      dni: dniValue,
      identification_number: dniValue,
      dni_number: dniValue,
      identification: dniValue
    }]
  };

  const response = await sendToPrestaShop(payload, countryIso);

  try {
    await notifyWholesaleRequest(
      {
        ...request,
        email,
        countryIso
      },
      {
        customerId: response.psCustomerId?.toString(),
        countryIso
      }
    );
  } catch (error) {
    // eslint-disable-next-line no-console
    console.warn("Wholesale notification email failed to send:", error);
  }

  // ΜΕΤΑΤΡΟΠΗ ΑΠΑΝΤΗΣΗΣ ΓΙΑ ΤΗΝ ΕΦΑΡΜΟΓΗ
  return {
    customerId: response.psCustomerId?.toString() || "0",
    status: "success",
    message: response.message || "Registration successful"
  };
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

export const loginCustomer = async (
  email: string,
  pass: string,
  countryIso: string = "GR"
): Promise<any> => {
  const payload = { action: "login", email: email.trim().toLowerCase(), password: pass };
  const normalizedCountryIso = countryIso.trim().toUpperCase() === "SE" ? "SE" : "GR";
  const response = await sendToPrestaShop(payload, normalizedCountryIso);
  const customerId = response?.id_customer ? Number(response.id_customer) : null;

  if (!customerId) {
    return {
      ...response,
      can_view_prices: false,
    };
  }

  const client = new PrestaShopClient({ shopId: normalizedCountryIso === "SE" ? 1 : 4 });
  const priceAccess = await getPriceAccess(client, customerId);

  return {
    ...response,
    can_view_prices: priceAccess.allowed,
  };
};

export const syncFavoriteProduct = async (request: ProductActivityRequest): Promise<any> => {
  return sendToPrestaShop(
    {
      action: "toggle_favorite_product",
      customerId: request.customerId,
      productId: request.productId,
      shopId: request.shopId,
      isFavorite: request.isFavorite === true,
      product: request.product ?? {}
    },
    request.shopId === 1 ? "SE" : "GR"
  );
};

export const recordRecentProduct = async (request: ProductActivityRequest): Promise<any> => {
  return sendToPrestaShop(
    {
      action: "record_recent_product",
      customerId: request.customerId,
      productId: request.productId,
      shopId: request.shopId,
      product: request.product ?? {}
    },
    request.shopId === 1 ? "SE" : "GR"
  );
};

export const listFavoriteProducts = async (request: ProductActivityListRequest): Promise<any> => {
  return sendToPrestaShop(
    {
      action: "list_favorite_products",
      customerId: request.customerId,
      shopId: request.shopId
    },
    request.shopId === 1 ? "SE" : "GR"
  );
};

export const listRecentProducts = async (request: ProductActivityListRequest): Promise<any> => {
  return sendToPrestaShop(
    {
      action: "list_recent_products",
      customerId: request.customerId,
      shopId: request.shopId,
      limit: request.limit ?? 20
    },
    request.shopId === 1 ? "SE" : "GR"
  );
};

export const clearActivityTables = async (request: ProductActivityClearRequest): Promise<any> => {
  return sendToPrestaShop(
    {
      action: "clear_activity_tables",
      shopId: request.shopId
    },
    request.shopId === 1 ? "SE" : "GR"
  );
};

export const debugListWholesaleApplications = async (countryIso: string = "GR"): Promise<any> => {
  return sendToPrestaShop({ action: "list_wholesale_applications" }, countryIso);
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
