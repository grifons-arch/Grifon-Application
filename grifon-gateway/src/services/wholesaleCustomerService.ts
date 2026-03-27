import { PrestaShopClient } from "../clients/PrestaShopClient";
import { extractResourceList } from "./prestashopParser";
import { getLocalizedValue, toBooleanFlag } from "../utils/prestashopFields";
import { toLimitParam } from "../utils/pagination";
import { getConfiguredWholesaleGroupIds } from "./priceAccessService";

interface WholesaleGroupItem {
  id: number;
  name: string | null;
  showPrices: boolean;
}

export interface WholesaleCustomerItem {
  customerId: number;
  shopId: number;
  email: string | null;
  firstName: string | null;
  lastName: string | null;
  company: string | null;
  active: boolean;
  defaultGroupId: number | null;
  defaultGroupName: string | null;
  wholesaleGroupIds: number[];
  wholesaleGroupNames: string[];
}

const fetchGroups = async (
  client: PrestaShopClient,
  lang?: number
): Promise<Map<number, WholesaleGroupItem>> => {
  const data = await client.get("groups", { display: "full", sort: "[id_ASC]" });
  const groups = extractResourceList<any>("groups", data);
  const groupsById = new Map<number, WholesaleGroupItem>();

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

const resolveWholesaleGroupIds = (groupsById: Map<number, WholesaleGroupItem>): Set<number> => {
  const configuredWholesaleGroupIds = getConfiguredWholesaleGroupIds();
  const wholesaleGroupIds = Array.from(groupsById.values())
    .filter(
      (group) =>
        group.showPrices &&
        (configuredWholesaleGroupIds.length === 0 ||
          configuredWholesaleGroupIds.includes(group.id))
    )
    .map((group) => group.id);

  return new Set(wholesaleGroupIds);
};

export const listWholesaleCustomers = async (
  client: PrestaShopClient,
  shopId: number,
  lang?: number
): Promise<WholesaleCustomerItem[]> => {
  const groupsById = await fetchGroups(client, lang);
  const wholesaleGroupIds = resolveWholesaleGroupIds(groupsById);
  const customers: WholesaleCustomerItem[] = [];
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

      const normalizedGroupIds = Array.from(
        new Set(
          (defaultGroupId ? [defaultGroupId, ...associationGroupIds] : associationGroupIds).filter(
            (groupId) => groupId > 0
          )
        )
      );

      const matchedWholesaleGroupIds = normalizedGroupIds.filter((groupId) =>
        wholesaleGroupIds.has(groupId)
      );

      if (!matchedWholesaleGroupIds.length) {
        continue;
      }

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
        wholesaleGroupIds: matchedWholesaleGroupIds,
        wholesaleGroupNames: matchedWholesaleGroupIds
          .map((groupId) => groupsById.get(groupId)?.name)
          .filter((name): name is string => typeof name === "string" && name.length > 0)
      });
    }

    if (items.length < pageSize) {
      break;
    }

    page += 1;
  }

  return customers;
};
