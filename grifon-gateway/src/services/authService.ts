import axios from "axios";
import crypto from "crypto";
import dns from "dns";
import http from "http";
import https from "https";
import { PrestaShopClient } from "../clients/PrestaShopClient";
import { config } from "../config/env";
import { extractResourceList } from "./prestashopParser";

export interface RegisterRequest {
  email: string;
  password: string;
  socialTitle?: "mr" | "mrs";
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
  customerDataPrivacyAccepted?: boolean;
  newsletter?: boolean;
  termsAndPrivacyAccepted?: boolean;
  partnerOffers?: boolean;
}

export interface RegisterResponse {
  customerId: string;
  status: string;
  message: string;
}

const PENDING_STATUS = "PENDING_WHOLESALE_APPROVAL";

const resolveGroupIds = (countryIso: string): number[] => {
  const groupIds = new Set<number>();
  if (config.pendingWholesaleGroupId) {
    groupIds.add(config.pendingWholesaleGroupId);
  }
  const countryGroupId = config.countryGroupMap[countryIso.toUpperCase()];
  if (countryGroupId) {
    groupIds.add(countryGroupId);
  }
  return Array.from(groupIds);
};

const findCustomerByEmail = async (client: PrestaShopClient, email: string): Promise<boolean> => {
  const payload = await client.get("customers", {
    "filter[email]": `[${email}]`,
    display: "[id,email]",
    limit: 1
  });
  const customers = extractResourceList<any>("customers", payload);
  return customers.length > 0;
};

const createDnsLookup = (): dns.LookupFunction | undefined => {
  const alias = config.replicaHostname?.trim();
  const resolveTo = config.replicaResolveTo?.trim();

  if (!alias || !resolveTo) {
    return undefined;
  }

  const normalizedAlias = alias.toLowerCase();

  return (hostname, options, callback) => {
    const host = String(hostname).toLowerCase();
    const targetHost = host === normalizedAlias ? resolveTo : String(hostname);
    return dns.lookup(targetHost, options, callback as any);
  };
};

const resolveSyncUrl = (): string => {
  const baseUrl = config.prestashopBaseUrl || config.shopBaseUrls[config.defaultShopId];
  const url = new URL(baseUrl);
  const configuredPath = config.customerSyncPath.startsWith("/")
    ? config.customerSyncPath
    : `/${config.customerSyncPath}`;

  const pathname = url.pathname.replace(/\/+$/, "");
  if (pathname.endsWith("/api")) {
    url.pathname = `${pathname.slice(0, -4)}${configuredPath}`;
  } else {
    url.pathname = `${pathname}${configuredPath}`;
  }

  return url.toString();
};

const createModuleHeaders = (payload: string): Record<string, string> => {
  if (!config.customerSyncSecret) {
    throw {
      status: 500,
      code: "CONFIG_ERROR",
      message: "GRIFON_CUSTOMER_SYNC_SECRET is required for customer registration sync"
    };
  }

  const timestamp = Math.floor(Date.now() / 1000).toString();
  const signatureBase = `${timestamp}\n${payload}`;
  const signature = crypto
    .createHmac("sha256", config.customerSyncSecret)
    .update(signatureBase, "utf8")
    .digest("base64");

  return {
    "Content-Type": "application/json",
    "X-Grifon-Timestamp": timestamp,
    "X-Grifon-Signature": signature
  };
};

const buildSyncPayload = (request: RegisterRequest): Record<string, unknown> => {
  const groupIds = resolveGroupIds(request.countryIso);

  return {
    externalCustomerId: request.email.trim().toLowerCase(),
    customer: {
      email: request.email.trim().toLowerCase(),
      firstname: request.firstName,
      lastname: request.lastName,
      password: request.password,
      company: request.company,
      newsletter: request.newsletter ? 1 : 0,
      optin: request.partnerOffers ? 1 : 0,
      active: 0
    },
    groups: {
      default: config.pendingWholesaleGroupId,
      list: groupIds
    },
    addresses: [
      {
        externalAddressId: `${request.email.trim().toLowerCase()}::default`,
        alias: "Default",
        address1: request.street,
        postcode: request.postalCode,
        city: request.city,
        countryIso: request.countryIso,
        vat_number: request.vatNumber,
        phone: request.phone,
        other: request.iban ? `IBAN: ${request.iban}` : undefined,
        company: request.company
      }
    ]
  };
};

export const registerCustomer = async (request: RegisterRequest): Promise<RegisterResponse> => {
  const client = new PrestaShopClient({ shopId: config.defaultShopId });
  const email = request.email.trim().toLowerCase();

  const exists = await findCustomerByEmail(client, email);
  if (exists) {
    throw {
      status: 409,
      code: "EMAIL_EXISTS",
      message: "Email already registered"
    };
  }

  const payload = buildSyncPayload({ ...request, email });
  const body = JSON.stringify(payload);
  const headers = createModuleHeaders(body);
  const lookup = createDnsLookup();

  try {
    const response = await axios.post(resolveSyncUrl(), body, {
      timeout: config.timeoutMs,
      headers,
      httpAgent: lookup ? new http.Agent({ lookup }) : undefined,
      httpsAgent: lookup ? new https.Agent({ lookup }) : undefined,
      validateStatus: () => true
    });

    if (response.status >= 200 && response.status < 300) {
      const customerId = String(
        response.data?.customerId ?? response.data?.id_customer ?? response.data?.id ?? email
      );

      return {
        customerId,
        status: response.data?.status ?? PENDING_STATUS,
        message:
          response.data?.message ?? "Η αίτηση καταχωρήθηκε και βρίσκεται σε αναμονή έγκρισης."
      };
    }

    const message = String(response.data?.message ?? "Failed to create customer");
    if (message.toLowerCase().includes("email") && message.toLowerCase().includes("exists")) {
      throw {
        status: 409,
        code: "EMAIL_EXISTS",
        message: "Email already registered"
      };
    }

    throw {
      status: response.status || 502,
      code: "UPSTREAM_ERROR",
      message,
      details: response.data
    };
  } catch (error: any) {
    if (error?.status) {
      throw error;
    }

    throw {
      status: 502,
      code: "UPSTREAM_ERROR",
      message: "Failed to create customer",
      details: String(error?.message ?? "Unknown error")
    };
  }
};
