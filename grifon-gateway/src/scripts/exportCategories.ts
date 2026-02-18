import fs from "fs/promises";
import path from "path";
import { PrestaShopClient } from "../clients/PrestaShopClient";
import { CategoryItem } from "../services/categoryService";
import { ShopId } from "../config/env";
import { extractResourceList } from "../services/prestashopParser";
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
    output: options.output?.trim() || "exports/catalog-export.json",
    pageSize: Number(options.pageSize) || 200,
    includeProducts: options.products !== "false"
  };
};

const fetchAllProducts = async (client: PrestaShopClient, lang?: number): Promise<ProductItem[]> => {
  const allProducts: ProductItem[] = [];
  let page = 1;
  const pageSize = 10; // ΠΟΛΥ ΜΙΚΡΟ BATCH ΓΙΑ ΜΕΓΙΣΤΗ ΣΤΑΘΕΡΟΤΗΤΑ

  console.log("Fetching products from PrestaShop API (Small batches)...");

  while (true) {
    try {
      const data = await client.get("products", {
        display: "[id,name,reference,price,associations]",
        limit: toLimitParam(page, pageSize)
      });

      const products = extractResourceList<any>("products", data);
      if (!products.length) break;

      const mapped = products.map((p: any) => ({
        id: Number(p.id),
        name: getLocalizedValue(p.name, lang),
        reference: p.reference || null,
        price: toNumber(p.price),
        categories: extractResourceList<any>("categories", p.associations || {}).map((c: any) => Number(c.id))
      }));

      allProducts.push(...mapped);
      process.stdout.write(`\rFetched ${allProducts.length} products...`);
      
      if (products.length < pageSize) break;
      page++;
    } catch (error: any) {
      console.error(`\nError on page ${page}:`, error.message || error);
      console.log("Waiting 3 seconds before retry...");
      await new Promise(res => setTimeout(res, 3000));
      // Αν αποτύχει πολλές φορές στην ίδια σελίδα, ίσως πρέπει να την προσπεράσουμε
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
    try {
        const data = await client.get("categories", {
        sort: "[id_ASC]",
        limit: toLimitParam(page, pageSize),
        display: "full"
      });

      const categories = extractResourceList<any>("categories", data);
      if (!categories.length) break;

      allItems.push(...categories.map((category: any) => ({
        id: Number(category.id),
        parentId: toNumber(category.id_parent),
        name: getLocalizedValue(category.name, lang),
        position: toNumber(category.position),
        active: toNumber(category.active),
        slug: getLocalizedValue(category.link_rewrite, lang)
      })));

      if (categories.length < pageSize) break;
      page++;
    } catch(e) {
        console.error('Error fetching categories batch.');
        break;
    }
  }
  console.log(`Fetched ${allItems.length} categories.`);
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

  if (products.length > 0) {
    products.forEach(product => {
      product.categories.forEach(catId => {
        const node = nodeMap.get(catId);
        if (node) {
          node.products.push(product);
        }
      });
    });
  }

  const tree: ExportTreeNode[] = [];
  nodeMap.forEach((node) => {
    if (node.parentId && nodeMap.has(node.parentId) && node.parentId !== node.id) {
      nodeMap.get(node.parentId)?.children.push(node);
    } else if (node.id !== 1 && node.id !== 0) {
      tree.push(node);
    }
  });

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
