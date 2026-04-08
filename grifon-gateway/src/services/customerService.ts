import { PrestaShopClient } from "../clients/PrestaShopClient";
import { extractResourceList } from "./prestashopParser";
import { getLocalizedValue, toBooleanFlag } from "../utils/prestashopFields";
import { toLimitParam } from "../utils/pagination";

interface CustomerGroupItem {
  id: number;
  name: string | null;
  showPrices: boolean;
}

const hasWholesaleKeyword = (name: string | null): boolean => {
  const normalized = name?.trim().toLowerCase() ?? "";
  if (!normalized) {
    return false;
  }

  return (
    normalized.includes("wholesale") ||
    normalized.includes("wholesales") ||
    normalized.includes("whalesale") ||
    normalized.includes("whalesales")
  );
};

export interface CustomerItem {
  customerId: number;
  shopId: number;
  email: string | null;
  firstName: string | null;
  lastName: string | null;
  company: string | null;
  active: boolean;
  defaultGroupId: number | null;
  defaultGroupName: string | null;
  groupIds: number[];
  groupNames: string[];
  wholesaleGroupIds: number[];
  wholesaleGroupNames: string[];
  isWholesale: boolean;
}

const fetchGroups = async (
  client: PrestaShopClient,
  lang?: number
): Promise<Map<number, CustomerGroupItem>> => {
  const data = await client.get("groups", { display: "full", sort: "[id_ASC]" });
  const groups = extractResourceList<any>("groups", data);
  const groupsById = new Map<number, CustomerGroupItem>();

  groups.forEach((group) => {
    const groupId = Number(group.id);
    if (Number.isNaN(groupId) || groupId <= 0) {
      return;
    }

    groupsById.set(groupId, {
      id: groupId,
      name: getLocalizedValue(group.name, lang),
      showPrices: toBooleanFlag(group.show_prices)
    });
  });

  return groupsById;
};

const resolveWholesaleGroupIds = (groupsById: Map<number, CustomerGroupItem>): Set<number> => {
  const ids = Array.from(groupsById.values())
    .filter((group) => hasWholesaleKeyword(group.name))
    .map((group) => group.id);

  return new Set(ids);
};

export const listCustomers = async (
  client: PrestaShopClient,
  shopId: number,
  lang?: number
): Promise<CustomerItem[]> => {
  const groupsById = await fetchGroups(client, lang);
  const wholesaleGroupIds = resolveWholesaleGroupIds(groupsById);
  const customers: CustomerItem[] = [];
  const pageSize = 100;
  let page = 1;

  while (true) {
    const data = await client.get("customers", {
      display: "full",
      sort: "[id_ASC]",
      limit: toLimitParam(page, pageSize)
    });
    const items = extractResourceList<any>("customers", data);
    if (!items.length) {
      break;
    }

    for (const customer of items) {
      const customerId = Number(customer.id);
      if (Number.isNaN(customerId) || customerId <= 0) {
        continue;
      }

      const defaultGroupId = customer.id_default_group ? Number(customer.id_default_group) : null;
      const associationGroupIds = customer.associations?.groups
        ? extractResourceList<any>("groups", customer.associations)
            .map((group) => Number(group.id))
            .filter((groupId) => !Number.isNaN(groupId) && groupId > 0)
        : [];

      const groupIds = Array.from(
        new Set(
          (defaultGroupId ? [defaultGroupId, ...associationGroupIds] : associationGroupIds).filter(
            (groupId) => groupId > 0
          )
        )
      );

      const matchedWholesaleGroupIds = groupIds.filter((groupId) => wholesaleGroupIds.has(groupId));

      customers.push({
        customerId,
        shopId,
        email: customer.email ?? null,
        firstName: customer.firstname ?? null,
        lastName: customer.lastname ?? null,
        company: customer.company ?? null,
        active: toBooleanFlag(customer.active),
        defaultGroupId,
        defaultGroupName: defaultGroupId ? groupsById.get(defaultGroupId)?.name ?? null : null,
        groupIds,
        groupNames: groupIds
          .map((groupId) => groupsById.get(groupId)?.name)
          .filter((name): name is string => typeof name === "string" && name.length > 0),
        wholesaleGroupIds: matchedWholesaleGroupIds,
        wholesaleGroupNames: matchedWholesaleGroupIds
          .map((groupId) => groupsById.get(groupId)?.name)
          .filter((name): name is string => typeof name === "string" && name.length > 0),
        isWholesale: matchedWholesaleGroupIds.length > 0
      });
    }

    if (items.length < pageSize) {
      break;
    }

    page += 1;
  }

  return customers;
};
