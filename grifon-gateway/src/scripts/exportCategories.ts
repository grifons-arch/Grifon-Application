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

interface CategoryNamesExport {
  mainCategoryName: string | null;
  subcategoryNames: string[];
}

const parseArgs = (argv: string[]): CliOptions => {
  const options: Record<string, string> = {};
  for (const arg of argv) {
    if (!arg.startsWith("--")) continue;
    const [rawKey, rawValue] = arg.slice(2).split("=");
    if (!rawKey) continue;
    options[rawKey] = rawValue ?? "";
  }
  const shopIdRaw = Number(options.shopId ?? "4");
  return {
    shopId: shopIdRaw === 1 ? 1 : 4,
    lang: options.lang ? Number(options.lang) : 1,
    output: options.output?.trim() || "exports/full-catalog.json",
    pageSize: Number(options.pageSize) || 200
  };
};

const fetchAllCategories = async (client: PrestaShopClient, pageSize: number, lang?: number): Promise<CategoryItem[]> => {
  const allItems: CategoryItem[] = [];
  let page = 1;
  console.log("Fetching categories...");

  while (true) {
    const data = await client.get("categories", {
      sort: "[id_ASC]",
      limit: toLimitParam(page, pageSize),
      display: "full"
    });
    const categories = extractResourceList<any>("categories", data);
    if (!categories.length) break;
    
    const mapped = categories.map((c: any) => {
      const item = {
        id: Number(c.id),
        parentId: toNumber(c.id_parent),
        name: getLocalizedValue(c.name, lang),
        position: toNumber(c.position),
        active: toNumber(c.active),
        slug: getLocalizedValue(c.link_rewrite, lang)
      };
      console.log(`Found Category: [ID: ${item.id}] ${item.name}`);
      return item;
    });
    
    allItems.push(...mapped);
    
    if (categories.length < pageSize) break;
    page++;
  }
  console.log(`Fetched total ${allItems.length} categories.`);
  return allItems;
};

const collectSubcategoryNames = (node: ExportTreeNode): string[] => {
  const names: string[] = [];
  for (const child of node.children) {
    if (child.name) {
      names.push(child.name);
    }
    names.push(...collectSubcategoryNames(child));
  }
  return names;
};

const run = async () => {
  const options = parseArgs(process.argv.slice(2));
  const client = new PrestaShopClient({ shopId: options.shopId, lang: options.lang });

  const categories = await fetchAllCategories(client, options.pageSize, options.lang);

  const nodeMap = new Map<number, ExportTreeNode>();
  categories.forEach((cat) => {
    nodeMap.set(cat.id, { ...cat, children: [] });
  });

  const tree: ExportTreeNode[] = [];
  nodeMap.forEach((node) => {
    if (node.parentId && nodeMap.has(node.parentId) && node.parentId !== node.id && node.id > 2) {
      const parent = nodeMap.get(node.parentId);
      parent?.children.push(node);
    } else {
      if (node.id >= 2) tree.push(node);
    }
  });
  
  console.log(`Final tree built with ${tree.length} top-level categories.`);

  const categoriesWithSubcategories: CategoryNamesExport[] = tree.map((mainCategory) => ({
    mainCategoryName: mainCategory.name,
    subcategoryNames: collectSubcategoryNames(mainCategory)
  }));

  const outputPath = path.resolve(options.output);
  await fs.mkdir(path.dirname(outputPath), { recursive: true });
  await fs.writeFile(
    outputPath,
    JSON.stringify(
      {
        shopId: options.shopId,
        categories: categoriesWithSubcategories
      },
      null,
      2
    ),
    "utf8"
  );

  console.log(`Successfully exported to ${outputPath}`);
};

run().catch((error) => {
  console.error("Fatal error:", error);
  process.exit(1);
});
