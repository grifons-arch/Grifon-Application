import fs from "fs/promises";
import path from "path";
import { ShopId } from "../config/env";
import { PrestaShopClient } from "../clients/PrestaShopClient";
import { extractResourceList } from "../services/prestashopParser";
import { getLocalizedValue, toBooleanFlag } from "../utils/prestashopFields";
import { toLimitParam } from "../utils/pagination";

interface CliOptions {
  shopIds: ShopId[];
  lang?: number;
  output: string;
  pageSize: number;
}

interface GroupExportItem {
  id: number;
  name: string | null;
  showPrices: boolean;
}

interface CustomerGroupExportItem {
  id: number;
  email: string | null;
  firstName: string | null;
  lastName: string | null;
  active: boolean;
  defaultGroupId: number | null;
  defaultGroupName: string | null;
  groupIds: number[];
  groupNames: string[];
}

interface ShopCustomerGroupsExport {
  shopId: ShopId;
  groups: GroupExportItem[];
  customers: CustomerGroupExportItem[];
}

const parseArgs = (argv: string[]): CliOptions => {
  const options: Record<string, string> = {};
  for (const arg of argv) {
    if (!arg.startsWith("--")) continue;
    const [rawKey, rawValue] = arg.slice(2).split("=");
    if (!rawKey) continue;
    options[rawKey] = rawValue ?? "";
  }

  const rawShopIds = (options.shopIds ?? "4,1")
    .split(",")
    .map((value) => Number(value.trim()))
    .filter((value) => value === 1 || value === 4) as ShopId[];

  return {
    shopIds: rawShopIds.length ? Array.from(new Set(rawShopIds)) : [4, 1],
    lang: options.lang ? Number(options.lang) : 1,
    output: options.output?.trim() || "exports/customer-groups.json",
    pageSize: Number(options.pageSize) || 100
  };
};

const fetchGroups = async (
  client: PrestaShopClient,
  lang?: number
): Promise<Map<number, GroupExportItem>> => {
  const data = await client.get("groups", { display: "full", sort: "[id_ASC]" });
  const groups = extractResourceList<any>("groups", data);
  const result = new Map<number, GroupExportItem>();

  groups.forEach((group) => {
    const groupId = Number(group.id);
    result.set(groupId, {
      id: groupId,
      name: getLocalizedValue(group.name, lang),
      showPrices: toBooleanFlag(group.show_prices)
    });
  });

  return result;
};

const fetchCustomers = async (
  client: PrestaShopClient,
  groupsById: Map<number, GroupExportItem>,
  pageSize: number
): Promise<CustomerGroupExportItem[]> => {
  const customers: CustomerGroupExportItem[] = [];
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
      const defaultGroupId = customer.id_default_group ? Number(customer.id_default_group) : null;
      const groupIds = customer.associations?.groups
        ? extractResourceList<any>("groups", customer.associations)
            .map((group) => Number(group.id))
            .filter((id) => !Number.isNaN(id))
        : [];

      const normalizedGroupIds = Array.from(
        new Set(
          (defaultGroupId ? [defaultGroupId, ...groupIds] : groupIds).filter((id) => id > 0)
        )
      );

      customers.push({
        id: Number(customer.id),
        email: customer.email ?? null,
        firstName: customer.firstname ?? null,
        lastName: customer.lastname ?? null,
        active: toBooleanFlag(customer.active),
        defaultGroupId,
        defaultGroupName: defaultGroupId ? groupsById.get(defaultGroupId)?.name ?? null : null,
        groupIds: normalizedGroupIds,
        groupNames: normalizedGroupIds
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

const exportShopCustomerGroups = async (
  shopId: ShopId,
  lang: number | undefined,
  pageSize: number
): Promise<ShopCustomerGroupsExport> => {
  const client = new PrestaShopClient({ shopId, lang });
  const groupsById = await fetchGroups(client, lang);
  const customers = await fetchCustomers(client, groupsById, pageSize);

  return {
    shopId,
    groups: Array.from(groupsById.values()),
    customers
  };
};

const run = async () => {
  const options = parseArgs(process.argv.slice(2));
  const shops = [];

  for (const shopId of options.shopIds) {
    // eslint-disable-next-line no-console
    console.log(`Exporting customer groups for shop ${shopId}...`);
    const shopExport = await exportShopCustomerGroups(shopId, options.lang, options.pageSize);
    shops.push(shopExport);
    // eslint-disable-next-line no-console
    console.log(
      `Shop ${shopId}: ${shopExport.customers.length} customers, ${shopExport.groups.length} groups`
    );
  }

  const outputPath = path.resolve(options.output);
  await fs.mkdir(path.dirname(outputPath), { recursive: true });
  await fs.writeFile(
    outputPath,
    JSON.stringify(
      {
        generatedAt: new Date().toISOString(),
        shopIds: options.shopIds,
        shops
      },
      null,
      2
    ),
    "utf8"
  );

  // eslint-disable-next-line no-console
  console.log(`Customer groups exported to ${outputPath}`);
};

run().catch((error) => {
  // eslint-disable-next-line no-console
  console.error("Failed to export customer groups", error);
  process.exit(1);
});
