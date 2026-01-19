import { PrestaShopClient } from "../clients/PrestaShopClient";
import { extractResourceItem, extractResourceList } from "./prestashopParser";
import { toBooleanFlag } from "../utils/prestashopFields";

export interface PriceAccessResult {
  customerId: number;
  active: boolean;
  defaultGroupId: number | null;
  groupShowPrices: boolean;
  allowed: boolean;
}

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
      allowed: false
    };
  }

  const active = toBooleanFlag(customer.active);
  const defaultGroupId = customer.id_default_group ? Number(customer.id_default_group) : null;
  let groupShowPrices = false;

  if (defaultGroupId) {
    const groupData = await client.getById("groups", defaultGroupId, { display: "full" });
    const group = extractResourceItem<any>("groups", groupData);
    if (group) {
      groupShowPrices = toBooleanFlag(group.show_prices);
    }
  }

  const allowed = active && groupShowPrices;

  return {
    customerId,
    active,
    defaultGroupId,
    groupShowPrices,
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
