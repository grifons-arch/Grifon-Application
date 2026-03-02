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
  brand: string | null;
  quantity: number | null;
  inStock: boolean;
  defaultImage: { id: number; url: string } | null;
  active: number | null;
}

export interface ProductQueryFilters {
  search?: string;
  minPrice?: number;
  maxPrice?: number;
  inStockOnly?: boolean;
}

const buildImageUrl = (shopId: ShopId, productId: number, imageId: number): string => {
  const url = `/v1/images/products/${productId}/${imageId}?shopId=${shopId}`;
  return url;
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

  const quantity = toNumber(product.quantity);
  const parsedQuantity = typeof quantity === "number" ? quantity : null;
  const brandName = typeof product.manufacturer_name === "string" ? product.manufacturer_name : null;

  return {
    id,
    name: getLocalizedValue(product.name, lang),
    price: allowPrice ? toNumber(product.price) : null,
    reference: product.reference ?? null,
    brand: brandName,
    quantity: parsedQuantity,
    inStock: parsedQuantity === null ? true : parsedQuantity > 0,
    defaultImage: idImage ? { id: idImage, url: buildImageUrl(shopId, id, idImage) } : null,
    active: toNumber(product.active)
  };
};

const applyServerSideFilters = (
  items: ProductListItem[],
  filters: ProductQueryFilters
): ProductListItem[] => {
  const normalizedSearch = filters.search?.trim().toLowerCase();

  return items.filter((item) => {
    const matchesSearch =
      !normalizedSearch ||
      item.name?.toLowerCase().includes(normalizedSearch) ||
      item.reference?.toLowerCase().includes(normalizedSearch);

    const matchesMinPrice =
      filters.minPrice === undefined || (item.price !== null && item.price >= filters.minPrice);

    const matchesMaxPrice =
      filters.maxPrice === undefined || (item.price !== null && item.price <= filters.maxPrice);

    const matchesStock = !filters.inStockOnly || item.inStock;

    return Boolean(matchesSearch && matchesMinPrice && matchesMaxPrice && matchesStock);
  });
};

export const listAllProducts = async (
  client: PrestaShopClient,
  shopId: ShopId,
  page: number,
  pageSize: number,
  sort: string,
  lang?: number,
  allowPrice = true,
  filters: ProductQueryFilters = {}
): Promise<ProductListItem[]> => {
  const data = await client.get("products", {
    "filter[active]": 1,
    sort,
    limit: toLimitParam(page, pageSize),
    display: "full",
    ...(filters.search ? { "filter[name]": `%${filters.search}%` } : {}),
    ...(filters.minPrice !== undefined || filters.maxPrice !== undefined
      ? {
          "filter[price]": `[${filters.minPrice ?? ""},${filters.maxPrice ?? ""}]`
        }
      : {})
  });

  const items = extractResourceList<any>("products", data);
  const normalized = items.map((product) => normalizeProduct(product, shopId, lang, allowPrice));
  const filtered = applyServerSideFilters(normalized, filters);
  console.log(`[Gateway] listAllProducts: shopId=${shopId}, count=${filtered.length}`);
  return filtered;
};

export const listProductsByCategory = async (
  client: PrestaShopClient,
  shopId: ShopId,
  categoryId: number,
  page: number,
  pageSize: number,
  sort: string,
  lang?: number,
  allowPrice = true,
  filters: ProductQueryFilters = {}
): Promise<ProductListItem[]> => {
  console.log(`[Gateway] Fetching category ${categoryId} (Shop ${shopId})`);

  if (categoryId <= 2) {
    return listAllProducts(client, shopId, page, pageSize, sort, lang, allowPrice, filters);
  }

  let productsRaw: any[] = [];

  // ΜΕΘΟΔΟΣ 1: id_category_default (δοκιμασμένη)
  try {
    const data = await client.get("products", {
      "filter[active]": 1,
      "filter[id_category_default]": categoryId,
      display: "full",
      limit: toLimitParam(page, pageSize),
      sort
    });
    productsRaw = extractResourceList<any>("products", data);
    console.log(`[Gateway] Method 1 (default category) found ${productsRaw.length} products.`);
  } catch (e: any) {
    console.warn(`[Gateway] Method 1 failed: ${e.message}`);
  }

  // ΜΕΘΟΔΟΣ 2: Αναζήτηση μέσω συσχετίσεων (associations) αλλά με "get" αντί για "getById"
  if (productsRaw.length === 0) {
    try {
      console.log(`[Gateway] Method 2: Fetching category ${categoryId} info to find associations...`);
      const catResponse = await client.get("categories", {
        "filter[id]": categoryId,
        display: "full"
      });
      const category = extractResourceItem<any>("categories", catResponse);

      const associations = category?.associations?.products;
      if (associations) {
        const productList = extractResourceList<any>("products", { products: associations });
        const productIds = productList.map(p => toNumber(p.id)).filter(id => id !== null);
        console.log(`[Gateway] Method 2: Found ${productIds.length} IDs in associations for category ${categoryId}.`);

        if (productIds.length > 0) {
          const start = (page - 1) * pageSize;
          const slice = productIds.slice(start, start + pageSize);
          if (slice.length > 0) {
            const productsData = await client.get("products", {
              "filter[id]": `[${slice.join("|")}]`,
              display: "full",
              sort
            });
            productsRaw = extractResourceList<any>("products", productsData);
            console.log(`[Gateway] Method 2 success: Fetched ${productsRaw.length} products details.`);
          }
        }
      } else {
        console.log(`[Gateway] Method 2: No product associations found in category data.`);
      }
    } catch (e: any) {
      console.warn(`[Gateway] Method 2 failed: ${e.message}`);
    }
  }

  const normalized = productsRaw.map((p) => normalizeProduct(p, shopId, lang, allowPrice));
  const filtered = applyServerSideFilters(normalized, filters);

  console.log(`[Gateway] FINAL: Category ${categoryId} returns ${filtered.length} products.`);

  if (filtered.length > 0) {
      const sample = filtered[0];
      console.log(`[Gateway] Sample Product (ID: ${sample.id}): Name="${sample.name}", ImageURL="${sample.defaultImage?.url || "NONE"}"`);
  }

  return filtered;
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
