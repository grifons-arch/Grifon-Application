import fs from "fs/promises";
import path from "path";
import { PrestaShopClient } from "../clients/PrestaShopClient";
import { CategoryItem } from "../services/categoryService";
import { ShopId } from "../config/env";
import { extractResourceList } from "../services/prestashopParser";
import { getLocalizedValue, toNumber } from "../utils/prestashopFields";
import { toLimitParam } from "../utils/pagination";

interface CliOptions {
  shopId: ShopId;
  lang?: number;
  output: string;
  pageSize: number;
}

interface ExportTreeNode extends CategoryItem {
  children: ExportTreeNode[];
}

const parseArgs = (argv: string[]): CliOptions => {
  const options: Record<string, string> = {};

  for (const arg of argv) {
    if (!arg.startsWith("--")) {
      continue;
    }

    const [rawKey, rawValue] = arg.slice(2).split("=");
    if (!rawKey) {
      continue;
    }

    options[rawKey] = rawValue ?? "";
  }

  const shopIdRaw = Number(options.shopId ?? "4");
  const shopId: ShopId = shopIdRaw === 1 ? 1 : 4;

  const langRaw = options.lang ? Number(options.lang) : undefined;
  const lang = Number.isInteger(langRaw) && (langRaw as number) > 0 ? (langRaw as number) : undefined;

  const pageSizeRaw = Number(options.pageSize ?? "200");
  const pageSize = Number.isInteger(pageSizeRaw) && pageSizeRaw > 0 ? pageSizeRaw : 200;

  const output = options.output?.trim() || "exports/categories-tree.json";

  return {
    shopId,
    lang,
    output,
    pageSize
  };
};

const buildRows = (nodes: ExportTreeNode[], level = 0, parentName = ""): string[] => {
  const rows: string[] = [];

  for (const node of nodes) {
    const safeName = (node.name ?? "").replace(/"/g, '""');
    const safeParentName = parentName.replace(/"/g, '""');
    rows.push(`"${node.id}","${safeName}","${safeParentName}","${level}"`);
    rows.push(...buildRows(node.children, level + 1, node.name ?? ""));
  }

  return rows;
};

const fetchCategoryPage = async (
  client: PrestaShopClient,
  page: number,
  pageSize: number,
  lang?: number
): Promise<CategoryItem[]> => {
  const data = await client.get("categories", {
    "filter[active]": 1,
    sort: "[position_ASC]",
    limit: toLimitParam(page, pageSize)
  });

  const categories = extractResourceList<Record<string, unknown>>("categories", data);

  return categories.map((category) => ({
    id: Number(category.id),
    parentId: toNumber(category.id_parent),
    name: getLocalizedValue(category.name, lang),
    position: toNumber(category.position),
    active: toNumber(category.active),
    slug: getLocalizedValue(category.link_rewrite, lang)
  }));
};

const run = async () => {
  const options = parseArgs(process.argv.slice(2));
  const client = new PrestaShopClient({ shopId: options.shopId, lang: options.lang });

  const allItems: CategoryItem[] = [];
  let page = 1;

  while (true) {
    const items = await fetchCategoryPage(client, page, options.pageSize, options.lang);
    allItems.push(...items);

    if (items.length < options.pageSize) {
      break;
    }

    page += 1;
  }

  const uniqueById = new Map<number, CategoryItem>();
  for (const item of allItems) {
    uniqueById.set(item.id, item);
  }

  const nodeMap = new Map<number, ExportTreeNode>();
  uniqueById.forEach((item) => {
    nodeMap.set(item.id, { ...item, children: [] });
  });

  const tree: ExportTreeNode[] = [];
  nodeMap.forEach((node) => {
    if (node.parentId && nodeMap.has(node.parentId)) {
      nodeMap.get(node.parentId)?.children.push(node);
    } else {
      tree.push(node);
    }
  });

  const outputPath = path.resolve(options.output);
  await fs.mkdir(path.dirname(outputPath), { recursive: true });
  await fs.writeFile(
    outputPath,
    JSON.stringify(
      {
        shopId: options.shopId,
        lang: options.lang,
        total: uniqueById.size,
        items: Array.from(uniqueById.values()),
        tree
      },
      null,
      2
    ),
    "utf8"
  );

  const csvOutputPath = outputPath.replace(/\.json$/i, ".csv");
  const csvRows = ["id,name,parent_name,level", ...buildRows(tree)];
  await fs.writeFile(csvOutputPath, `${csvRows.join("\n")}\n`, "utf8");

  console.log(
    `Exported ${uniqueById.size} active categories for shop ${options.shopId} to ${outputPath} and ${csvOutputPath}`
  );
};

run().catch((error: unknown) => {
  console.error("Category export failed", error);
  process.exit(1);
});
