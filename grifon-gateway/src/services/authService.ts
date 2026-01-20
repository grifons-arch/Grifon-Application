import { PrestaShopClient } from "../clients/PrestaShopClient";
import { config } from "../config/env";
import { extractResourceItem, extractResourceList } from "./prestashopParser";
import { buildXmlFromJson } from "../utils/xml";

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  countryIso: string;
  phone?: string;
  company?: string;
}

export interface RegisterResponse {
  customerId: string;
  status: string;
  message: string;
}

const PENDING_STATUS = "PENDING_WHOLESALE_APPROVAL";

const buildCustomerPayload = (
  schema: unknown,
  request: RegisterRequest,
  groupIds: number[]
): Record<string, unknown> => {
  const root = (schema as any)?.prestashop ?? schema;
  const baseCustomer = (root as any)?.customer ?? {};
  const associations = groupIds.length
    ? {
        groups: {
          group: groupIds.map((id) => ({ id }))
        }
      }
    : undefined;
  const customerPayload: Record<string, unknown> = {
    ...baseCustomer,
    firstname: request.firstName,
    lastname: request.lastName,
    email: request.email,
    passwd: request.password,
    active: "0",
    id_default_group: config.pendingWholesaleGroupId ?? baseCustomer.id_default_group,
    id_shop: config.defaultShopId,
    associations
  };

  if ("company" in baseCustomer && request.company) {
    customerPayload.company = request.company;
  }
  if ("phone" in baseCustomer && request.phone) {
    customerPayload.phone = request.phone;
  }

  return {
    prestashop: {
      customer: customerPayload
    }
  };
};

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

  const schema = await client.get("customers", { schema: "blank" });
  const groupIds = resolveGroupIds(request.countryIso);
  const payload = buildCustomerPayload(
    schema,
    {
      ...request,
      email
    },
    groupIds
  );
  const xmlBody = buildXmlFromJson(payload);

  try {
    const created = await client.postXml("customers", xmlBody);
    const customer = extractResourceItem<any>("customer", created);
    const customerId = customer?.id ?? customer?.id_customer;
    if (!customerId) {
      throw {
        status: 502,
        code: "UPSTREAM_ERROR",
        message: "Missing customer id in Prestashop response"
      };
    }
    return {
      customerId: String(customerId),
      status: PENDING_STATUS,
      message: "Η αίτηση καταχωρήθηκε και βρίσκεται σε αναμονή έγκρισης."
    };
  } catch (error: any) {
    const message = String(error?.message ?? "Prestashop error");
    if (message.toLowerCase().includes("email") && message.toLowerCase().includes("exists")) {
      throw {
        status: 409,
        code: "EMAIL_EXISTS",
        message: "Email already registered"
      };
    }
    if (error?.status) {
      throw error;
    }
    throw {
      status: 502,
      code: "UPSTREAM_ERROR",
      message: "Failed to create customer"
    };
  }
};
