import { PrestaShopClient } from "../clients/PrestaShopClient";
import { extractResourceItem, extractResourceList } from "./prestashopParser";
import { toBooleanFlag } from "../utils/prestashopFields";

export interface PriceAccessResult {
  customerId: number;
  active: boolean;
  defaultGroupId: number | null;
  groupShowPrices: boolean;
  hasWholesaleGroup: boolean;
  allowed: boolean;
}

const hasWholesaleKeyword = (name: string | null | undefined): boolean => {
  const normalized = name?.trim().toLowerCase() ?? "";
  if (!normalized) {
    return false;
  }

  return normalized.includes("wholesale");
};

export const getPriceAccess = async (
  client: PrestaShopClient,
  customerId: number
): Promise<PriceAccessResult> => {
  const data = await client.getById("customers", customerId, { display: "full" });
  const customer = extractResourceItem<any>("customers", data);
  if (!customer) {
    return {
      customerId,
      active: false,
      defaultGroupId: null,
      groupShowPrices: false,
      hasWholesaleGroup: false,
      allowed: false
    };
  }

  const active = toBooleanFlag(customer.active);
  const defaultGroupId = customer.id_default_group ? Number(customer.id_default_group) : null;
  let groupShowPrices = false;
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

  const groupNames: string[] = [];
  for (const groupId of groupIds) {
    const groupData = await client.getById("groups", groupId, { display: "full" });
    const group = extractResourceItem<any>("groups", groupData);
    if (!group) {
      continue;
    }

    if (groupId === defaultGroupId) {
      groupShowPrices = toBooleanFlag(group.show_prices);
    }

    const rawName = group.name?.language?.[0]?.value
      ?? group.name?.language?.value
      ?? group.name
      ?? null;
    if (typeof rawName === "string" && rawName.trim().length > 0) {
      groupNames.push(rawName);
    }
  }

  const hasWholesaleGroup = groupNames.some((groupName) => hasWholesaleKeyword(groupName));
  const allowed = active && hasWholesaleGroup;

  return {
    customerId,
    active,
    defaultGroupId,
    groupShowPrices,
    hasWholesaleGroup,
    allowed
  };
};

export const getGroupMembersCount = async (
  client: PrestaShopClient,
  groupId: number
): Promise<number> => {
  const pageSize = 100;
  let offset = 0;
  let total = 0;

  while (true) {
    const data = await client.get("customers", {
      "filter[active]": 1,
      "filter[id_default_group]": groupId,
      display: "[id]",
      limit: `${offset},${pageSize}`
    });
    const customers = extractResourceList<any>("customers", data);
    total += customers.length;
    if (customers.length < pageSize) {
      break;
    }
    offset += pageSize;
  }

  return total;
};
