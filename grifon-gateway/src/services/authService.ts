import axios from "axios";
import bcrypt from "bcryptjs";
import crypto from "crypto";
import dns from "dns";
import http from "http";
import https from "https";
import { config, shops } from "../config/env";
import { normalizeNetworkErrorMessage } from "../utils/networkErrors";

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
const PASSWORD_SALT_ROUNDS = 10;

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


const normalizeEnvKey = (key: string): string =>
  key.replace(/[^A-Za-z0-9]/g, "_").replace(/_+/g, "_").toUpperCase();

const readEnvAliasRuntime = (...keys: string[]): string | undefined => {
  for (const key of keys) {
    const value = process.env[key];
    if (typeof value === "string" && value.trim().length > 0) {
      return value.trim();
    }
  }

  const normalizedCandidates = new Set(keys.map(normalizeEnvKey));
  for (const [key, value] of Object.entries(process.env)) {
    if (!normalizedCandidates.has(normalizeEnvKey(key))) {
      continue;
    }
    if (typeof value === "string" && value.trim().length > 0) {
      return value.trim();
    }
  }

  return undefined;
};

const resolveCustomerSyncSecret = (): string | undefined =>
  (typeof config.customerSyncSecret === "string" && config.customerSyncSecret.trim().length > 0
    ? config.customerSyncSecret.trim()
    : undefined) ??
  readEnvAliasRuntime(
    "GRIFON_CUSTOMER_SYNC_SECRET",
    "GRIFON.CUSTOMER.SYNC.SECRET",
    "GRIFON__CUSTOMER__SYNC__SECRET"
  );

const resolveCustomerSyncPath = (): string =>
  readEnvAliasRuntime(
    "GRIFON_CUSTOMER_SYNC_PATH",
    "GRIFON.CUSTOMER.SYNC.PATH",
    "GRIFON__CUSTOMER__SYNC__PATH"
  ) ?? config.customerSyncPath ?? "/module/grifoncustomersync/sync";

const resolveShopIdForCountry = (countryIso: string): 1 | 4 =>
  countryIso.trim().toUpperCase() === "SE" ? 1 : 4;

const resolveSyncUrl = (countryIso: string): string => {
  const shopId = resolveShopIdForCountry(countryIso);
  const baseUrl = config.shopBaseUrls[shopId] || config.shopBaseUrls[config.defaultShopId] || config.prestashopBaseUrl;
  const url = new URL(baseUrl);
  const customerSyncPath = resolveCustomerSyncPath();
  const configuredPath = customerSyncPath.startsWith("/")
    ? customerSyncPath
    : `/${customerSyncPath}`;

  const pathname = url.pathname.replace(/\/+$/, "");
  if (pathname.endsWith("/api")) {
    url.pathname = `${pathname.slice(0, -4)}${configuredPath}`;
  } else {
    url.pathname = `${pathname}${configuredPath}`;
  }

  const replicaHost = config.replicaHostname?.trim().toLowerCase();
  const replicaResolveTo = config.replicaResolveTo?.trim();
  const currentHost = url.hostname.toLowerCase();

  if (replicaHost && !replicaResolveTo && currentHost === replicaHost) {
    const segments = url.pathname.split("/").filter(Boolean);
    const candidateDomain = segments[0]?.toLowerCase();

    if (candidateDomain && candidateDomain.includes(".")) {
      url.hostname = candidateDomain;
      url.pathname = `/${segments.slice(1).join("/")}`;
    } else {
      const selectedShop = shops.find((shop) => shop.id === shopId);
      const defaultShop = shops.find((shop) => shop.id === config.defaultShopId);
      const fallbackDomain = selectedShop?.domain ?? defaultShop?.domain;

      if (fallbackDomain) {
        url.hostname = fallbackDomain;
      }
    }
  }

  return url.toString();
};


const resolveSyncHostname = (syncUrl: string): string | undefined => {
  try {
    return new URL(syncUrl).hostname;
  } catch {
    return undefined;
  }
};

const createModuleHeaders = (payload: string): Record<string, string> => {
  const customerSyncSecret = resolveCustomerSyncSecret();

  if (!customerSyncSecret) {
    throw {
      status: 500,
      code: "CONFIG_ERROR",
      message:
        "GRIFON.CUSTOMER.SYNC.SECRET is required for customer registration sync (aliases: GRIFON_CUSTOMER_SYNC_SECRET, GRIFON__CUSTOMER__SYNC__SECRET)"
    };
  }

  const timestamp = Math.floor(Date.now() / 1000).toString();
  const signatureBase = `${timestamp}\n${payload}`;
  const signature = crypto
        .createHmac("sha256", customerSyncSecret)
    .update(signatureBase, "utf8")
    .digest("base64");

  return {
    "Content-Type": "application/json",
    "X-Grifon-Timestamp": timestamp,
    "X-Grifon-Signature": signature
  };
};

const hashCustomerPassword = async (password: string): Promise<string> =>
  bcrypt.hash(password, PASSWORD_SALT_ROUNDS);

const buildSyncPayload = (request: RegisterRequest, hashedPassword: string): Record<string, unknown> => {
  const groupIds = resolveGroupIds(request.countryIso);

  return {
    externalCustomerId: request.email.trim().toLowerCase(),
    customer: {
      email: request.email.trim().toLowerCase(),
      firstname: request.firstName,
      lastname: request.lastName,
      password: hashedPassword,
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
  const email = request.email.trim().toLowerCase();

  const hashedPassword = await hashCustomerPassword(request.password);
  const payload = buildSyncPayload({ ...request, email }, hashedPassword);
  const body = JSON.stringify(payload);
  const headers = createModuleHeaders(body);
  const lookup = createDnsLookup();
  const syncUrl = resolveSyncUrl(request.countryIso);

  try {
    const response = await axios.post(syncUrl, body, {
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

    const networkMessage = normalizeNetworkErrorMessage(error, {
      fallbackHostname: resolveSyncHostname(syncUrl)
    });

    throw {
      status: 502,
      code: "UPSTREAM_ERROR",
      message: networkMessage
        ? `Failed to create customer: ${networkMessage}`
        : "Failed to create customer",
      details: networkMessage ?? String(error?.message ?? "Unknown error")
    };
  }
};
