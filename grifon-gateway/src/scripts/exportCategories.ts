import fs from "fs/promises";
import path from "path";
import { PrestaShopClient } from "../clients/PrestaShopClient";
import { CategoryItem } from "../services/categoryService";
import { ShopId } from "../config/env";
import { extractResourceItem, extractResourceList } from "../services/prestashopParser";
import { getLocalizedValue, toNumber } from "../utils/prestashopFields";
import { toLimitParam } from "../utils/pagination";

interface ProductItem {
  id: number;
  name: string | null;
  reference: string | null;
  price: number | null;
  categories: number[];
}

interface CliOptions {
  shopId: ShopId;
  lang?: number;
  output: string;
  pageSize: number;
  includeProducts: boolean;
}

interface ExportTreeNode extends CategoryItem {
  products: ProductItem[];
  children: ExportTreeNode[];
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
    pageSize: Number(options.pageSize) || 200,
    includeProducts: options.products !== "false"
  };
};

const fetchAllProducts = async (client: PrestaShopClient, lang?: number): Promise<ProductItem[]> => {
  const allProducts: ProductItem[] = [];
  let page = 1;
  const pageSize = 50; 

  console.log("Fetching product IDs list...");
  const productIds: number[] = [];
  while(true) {
    const data = await client.get("products", { display: "[id]", limit: toLimitParam(page, pageSize) });
    const list = extractResourceList<any>("products", data);
    if (!list.length) break;
    list.forEach(item => productIds.push(Number(item.id)));
    process.stdout.write(`\rFound ${productIds.length} product IDs...`);
    if (list.length < pageSize) break;
    page++;
  }

  console.log("\nFetching details for each product (Safe Mode)...");
  for (let i = 0; i < productIds.length; i++) {
    const id = productIds[i];
    try {
      const detailData = await client.getById("products", id);
      const p = extractResourceItem<any>("product", detailData) || extractResourceItem<any>("products", detailData);
      
      if (p) {
        allProducts.push({
          id: Number(p.id),
          name: getLocalizedValue(p.name, lang),
          reference: p.reference || null,
          price: toNumber(p.price),
          categories: extractResourceList<any>("categories", p.associations || {}).map((c: any) => Number(c.id))
        });
      }
      
      if (i % 10 === 0 || i === productIds.length - 1) {
        process.stdout.write(`\rProgress: ${i + 1}/${productIds.length} products fetched...`);
      }
    } catch (error) {
      // Skip failed products
    }
  }
  console.log("\nProduct fetching complete.");
  return allProducts;
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
      console.log(`Found Category: [ID: ${item.id}] ${item.name}`); // Προσθήκη Log για κάθε κατηγορία
      return item;
    });
    
    allItems.push(...mapped);
    
    if (categories.length < pageSize) break;
    page++;
  }
  console.log(`Fetched total ${allItems.length} categories.`);
  return allItems;
};

const run = async () => {
  const options = parseArgs(process.argv.slice(2));
  const client = new PrestaShopClient({ shopId: options.shopId, lang: options.lang });

  const categories = await fetchAllCategories(client, options.pageSize, options.lang);
  const products = options.includeProducts ? await fetchAllProducts(client, options.lang) : [];

  const nodeMap = new Map<number, ExportTreeNode>();
  categories.forEach((cat) => {
    nodeMap.set(cat.id, { ...cat, products: [], children: [] });
  });

  products.forEach(product => {
    product.categories.forEach(catId => {
      const node = nodeMap.get(catId);
      if (node) node.products.push(product);
    });
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

  const outputPath = path.resolve(options.output);
  await fs.mkdir(path.dirname(outputPath), { recursive: true });
  await fs.writeFile(outputPath, JSON.stringify({ 
    shopId: options.shopId, 
    totalProducts: products.length, 
    tree 
  }, null, 2), "utf8");

  console.log(`Successfully exported to ${outputPath}`);
};

run().catch((error) => {
  console.error("Fatal error:", error);
  process.exit(1);
});
