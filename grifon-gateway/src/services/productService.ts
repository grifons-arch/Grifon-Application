import { PrestaShopClient } from "../clients/PrestaShopClient";
import { extractResourceItem, extractResourceList } from "./prestashopParser";
import { toLimitParam } from "../utils/pagination";
import { getLocalizedValue, toNumber } from "../utils/prestashopFields";
import { ShopId } from "../config/env";

export interface ProductListItem {
  id: number;
  name: string | null;
  price: number | null;
  reference: string | null;
  defaultImage: { id: number; url: string } | null;
  active: number | null;
  categories: { id: number }[] | null;
}

const buildImageUrl = (shopId: ShopId, productId: number, imageId: number): string => {
  return `/v1/images/products/${productId}/${imageId}?shopId=${shopId}`;
};

const normalizeProduct = (
  product: any,
  shopId: ShopId,
  lang?: number,
  allowPrice = true
): ProductListItem => {
  const id = Number(product.id);
  let idImage = toNumber(product.id_default_image);
  if (!idImage && product.associations?.images) {
    const images = extractResourceList<any>("images", product.associations);
    if (images.length > 0) idImage = Number(images[0].id);
  }

  const categories = product.associations?.categories 
    ? extractResourceList<any>("categories", product.associations).map(c => ({ id: toNumber(c.id) }))
    : [];

  return {
    id,
    name: getLocalizedValue(product.name, lang),
    price: allowPrice ? toNumber(product.price) : null,
    reference: product.reference ?? null,
    defaultImage: idImage ? { id: idImage, url: buildImageUrl(shopId, id, idImage) } : null,
    active: toNumber(product.active),
    categories
  };
};

export const listAllProducts = async (
  client: PrestaShopClient,
  shopId: ShopId,
  page: number,
  pageSize: number,
  sort: string,
  lang?: number,
  allowPrice = true
): Promise<ProductListItem[]> => {
  const data = await client.get("products", {
    "filter[active]": 1,
    sort,
    limit: toLimitParam(page, pageSize),
    display: "full" 
  });

  const items = extractResourceList<any>("products", data);
  return items.map((product) => normalizeProduct(product, shopId, lang, allowPrice));
};

async function getAllCategoryDescendants(client: PrestaShopClient, parentId: number): Promise<number[]> {
  const results: number[] = [parentId];
  
  const fetchChildren = async (id: number) => {
    try {
      const data = await client.get("categories", {
        "filter[id_parent]": id,
        "filter[active]": 1,
        display: "[id]",
        limit: "1000"
      });
      const children = extractResourceList<any>("categories", data);
      for (const child of children) {
        const childId = toNumber(child.id);
        if (!results.includes(childId)) {
          results.push(childId);
          await fetchChildren(childId);
        }
      }
    } catch (e) {}
  };

  await fetchChildren(parentId);
  return results;
}

export const listProductsByCategory = async (
  client: PrestaShopClient,
  shopId: ShopId,
  categoryId: number,
  page: number,
  pageSize: number,
  sort: string,
  lang?: number,
  allowPrice = true
): Promise<ProductListItem[]> => {
  // 0. Λήψη συνολικού πλήθους προϊόντων καταστήματος (για log)
  try {
    const shopData = await client.get("products", {
      "filter[active]": 1,
      display: "[id]",
      limit: "1" // Quick check
    });
    // Σημείωση: Το PrestaShop API συνήθως δεν δίνει το total_results στο JSON body 
    // χωρίς ειδική ρύθμιση, αλλά μπορούμε να πάρουμε μια ιδέα από το "Όλα τα προϊόντα"
  } catch (e) {}

  console.log(`\n--- [CategoryFetch Start] ---`);
  console.log(`Target Category: ${categoryId} | Shop: ${shopId}`);

  if (categoryId === 2) {
    const items = await listAllProducts(client, shopId, page, pageSize, sort, lang, allowPrice);
    console.log(`[CategoryFetch] Shop Root (2) Results: ${items.length} products`);
    console.log(`--- [CategoryFetch End] ---\n`);
    return items;
  }

  // 1. Get ALL categories in tree
  const categoryIds = await getAllCategoryDescendants(client, categoryId);
  console.log(`[CategoryFetch] Hierarchy: Found ${categoryIds.length} categories/subcategories`);
  
  // 2. Aggregate unique product IDs from ALL categories in the tree
  const productIdsSet = new Set<number>();

  // Use id_category_default filter (Source A)
  try {
    const filterVal = `[${categoryIds.join("|")}]`;
    const dataDefault = await client.get("products", {
      "filter[id_category_default]": filterVal,
      "filter[active]": 1,
      display: "[id]",
      limit: "2000"
    });
    const itemsA = extractResourceList<any>("products", dataDefault);
    itemsA.forEach(p => productIdsSet.add(toNumber(p.id)));
    console.log(`[CategoryFetch] Source A (Default Category): Found ${itemsA.length} IDs`);
  } catch (e) {}

  // Then use associations for each category (Source B)
  const associationPromises = categoryIds.map(async (id) => {
    try {
      const catData = await client.get(`categories/${id}`, { display: "full" });
      const category = extractResourceItem<any>("categories", catData);
      if (category?.associations?.products) {
        const pIds = extractResourceList<any>("products", category.associations).map(p => toNumber(p.id));
        pIds.forEach(pid => productIdsSet.add(pid));
        return pIds.length;
      }
    } catch (e) {}
    return 0;
  });
  const resultsB = await Promise.all(associationPromises);
  const totalRawB = resultsB.reduce((sum, val) => sum + val, 0);
  console.log(`[CategoryFetch] Source B (Associations): Found ${totalRawB} raw links`);

  const allIds = Array.from(productIdsSet);
  console.log(`[CategoryFetch] TOTAL UNIQUE PRODUCTS for this Category Tree: ${allIds.length}`);

  if (allIds.length === 0) {
    console.log(`[CategoryFetch] No products found.`);
    console.log(`--- [CategoryFetch End] ---\n`);
    return [];
  }

  // 3. Simple ID-based pagination
  const start = (page - 1) * pageSize;
  const pageIds = allIds.slice(start, start + pageSize);
  console.log(`[CategoryFetch] Page ${page}: Requesting full data for ${pageIds.length} items`);

  // 4. Fetch the actual product objects for this page
  const filterIds = `[${pageIds.join("|")}]`;
  const productsData = await client.get("products", {
    "filter[id]": filterIds,
    "filter[active]": 1,
    display: "full",
    limit: pageSize.toString()
  });

  const finalItems = extractResourceList<any>("products", productsData);
  console.log(`[CategoryFetch] Successfully retrieved ${finalItems.length} products`);
  console.log(`--- [CategoryFetch End] ---\n`);

  // Manual sorting
  if (sort.includes("price")) {
    const desc = sort.includes("DESC");
    finalItems.sort((a, b) => {
      const vA = toNumber(a.price);
      const vB = toNumber(b.price);
      return desc ? vB - vA : vA - vB;
    });
  }

  return finalItems.map((product) => normalizeProduct(product, shopId, lang, allowPrice));
};

export const getProductDetail = async (
  client: PrestaShopClient,
  shopId: ShopId,
  productId: number,
  lang?: number,
  allowPrice = true
): Promise<any> => {
  const data = await client.getById("products", productId, { display: "full" });
  const product = extractResourceItem<any>("products", data);
  if (!product) return null;
  return normalizeProduct(product, shopId, lang, allowPrice);
};
