import { PrestaShopClient } from "../clients/PrestaShopClient";
import { extractResourceList } from "./prestashopParser";
import { getLocalizedValue, toNumber, toBooleanFlag } from "../utils/prestashopFields";
import { getGroupMembersCount } from "./priceAccessService";

export interface GroupItem {
  id: number;
  groupName: string | null;
  discountPercent: number | null;
  members: number;
  showPrices: boolean;
  creationDate: string | null;
}

export const listGroupsWithMembers = async (
  client: PrestaShopClient,
  lang?: number
): Promise<GroupItem[]> => {
  const data = await client.get("groups", {
    "filter[show_prices]": 1,
    display: "full"
  });

  const groups = extractResourceList<any>("groups", data);

  const results: GroupItem[] = [];
  for (const group of groups) {
    const groupId = Number(group.id);
    const members = await getGroupMembersCount(client, groupId);
    results.push({
      id: groupId,
      groupName: getLocalizedValue(group.name, lang),
      discountPercent: toNumber(group.price_display_method) ?? toNumber(group.reduction),
      members,
      showPrices: toBooleanFlag(group.show_prices),
      creationDate: group.date_add ?? null
    });
  }

  return results;
};
