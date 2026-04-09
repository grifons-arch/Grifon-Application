const fs = require("fs/promises");
const path = require("path");
const { PrestaShopClient } = require("./dist/clients/PrestaShopClient.js");
const { extractResourceList } = require("./dist/services/prestashopParser.js");
const { getLocalizedValue, toBooleanFlag } = require("./dist/utils/prestashopFields.js");
const { toLimitParam } = require("./dist/utils/pagination.js");

const parseArgs = (argv) => {
  const options = {};
  for (const arg of argv) {
    if (!arg.startsWith("--")) continue;
    const [rawKey, rawValue] = arg.slice(2).split("=");
    if (!rawKey) continue;
    options[rawKey] = rawValue ?? "";
  }

  const rawShopIds = (options.shopIds ?? "4,1")
    .split(",")
    .map((value) => Number(value.trim()))
    .filter((value) => value === 1 || value === 4);

  return {
    shopIds: rawShopIds.length ? [...new Set(rawShopIds)] : [4, 1],
    lang: options.lang ? Number(options.lang) : 1,
    output: options.output?.trim() || "exports/customer-groups.json",
    pageSize: Number(options.pageSize) || 100
  };
};

const fetchGroups = async (client, lang) => {
  const data = await client.get("groups", { display: "full", sort: "[id_ASC]" });
  const groups = extractResourceList("groups", data);
  const result = new Map();

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

const fetchCustomers = async (client, groupsById, pageSize) => {
  const customers = [];
  let page = 1;

  while (true) {
    const data = await client.get("customers", {
      display: "full",
      sort: "[id_ASC]",
      limit: toLimitParam(page, pageSize)
    });
    const items = extractResourceList("customers", data);
    if (!items.length) break;

    for (const customer of items) {
      const defaultGroupId = customer.id_default_group ? Number(customer.id_default_group) : null;
      const groupIds = customer.associations?.groups
        ? extractResourceList("groups", customer.associations)
            .map((group) => Number(group.id))
            .filter((id) => !Number.isNaN(id))
        : [];

      const normalizedGroupIds = [...new Set((defaultGroupId ? [defaultGroupId, ...groupIds] : groupIds).filter((id) => id > 0))];

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
          .filter((name) => typeof name === "string" && name.length > 0)
      });
    }

    if (items.length < pageSize) break;
    page += 1;
  }

  return customers;
};

const run = async () => {
  const options = parseArgs(process.argv.slice(2));
  const shops = [];

  for (const shopId of options.shopIds) {
    console.log(`Exporting customer groups for shop ${shopId}...`);
    const client = new PrestaShopClient({ shopId, lang: options.lang });
    const groupsById = await fetchGroups(client, options.lang);
    const customers = await fetchCustomers(client, groupsById, options.pageSize);
    shops.push({
      shopId,
      groups: Array.from(groupsById.values()),
      customers
    });
    console.log(`Shop ${shopId}: ${customers.length} customers, ${groupsById.size} groups`);
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

  console.log(`Customer groups exported to ${outputPath}`);
};

run().catch((error) => {
  console.error("Failed to export customer groups", error);
  process.exit(1);
});
