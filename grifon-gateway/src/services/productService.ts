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
  images: Array<{ id: number; url: string }>;
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
  const associationImages = product.associations?.images
    ? extractResourceList<any>("images", product.associations)
    : [];
  const imageIds = associationImages
    .map((image) => toNumber(image?.id))
    .filter((imageId): imageId is number => typeof imageId === "number");

  if (!idImage && imageIds.length > 0) {
    idImage = imageIds[0];
  }

  const uniqueImageIds = Array.from(
    new Set(
      (idImage ? [idImage, ...imageIds] : imageIds).filter((imageId): imageId is number =>
        Number.isFinite(imageId)
      )
    )
  );

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
    images: uniqueImageIds.map((imageId) => ({
      id: imageId,
      url: buildImageUrl(shopId, id, imageId)
    })),
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

  // Χρησιμοποιούμε Set για να αποφύγουμε διπλότυπα προϊόντα
  const allProductsMap = new Map<number, any>();

  // ΜΕΘΟΔΟΣ 1: Προϊόντα της κύριας κατηγορίας
  try {
    const data = await client.get("products", {
      "filter[active]": 1,
      "filter[id_category_default]": categoryId,
      display: "full",
      limit: "250",
      sort
    });
    const mainProducts = extractResourceList<any>("products", data);
    mainProducts.forEach(p => allProductsMap.set(Number(p.id), p));
    console.log(`[Gateway] Method 1: Found ${mainProducts.length} products for category ${categoryId}`);
  } catch (e: any) {}

  // ΜΕΘΟΔΟΣ 2: Προϊόντα από υποκατηγορίες
  try {
    let childIds: number[] = [];

    // Ειδικές περιπτώσεις βάσει του categories_gr.json και του screenshot img_1.png
    if (categoryId === 4000) {
      // Ceramics Subcategories
      childIds = [4025, 4030];
    } else if (categoryId === 5000) {
      // Decorative Subcategories (Ενημερωμένο από img_1.png)
      childIds = [5015, 5025, 5030, 5040, 5080];
    } else if (categoryId === 4500) {
      // Statuettes Subcategories
      childIds = [4504, 4510, 4520, 4530, 4550];
    } else if (categoryId === 7000) {
      // Hobbies Subcategories
      childIds = [7025, 7040, 7030, 7035];
    } else if (categoryId === 7500) {
      // For Use Subcategories
      childIds = [7540, 7545];
    } else if (categoryId === 8000) {
      // Accessory Subcategories
      childIds = [8030];
    } else {
      const catResponse = await client.get("categories", { "filter[id]": categoryId, display: "full" });
      const category = extractResourceItem<any>("categories", catResponse);
      const subCategories = category?.associations?.categories;
      if (subCategories) {
        childIds = extractResourceList<any>("categories", { categories: subCategories })
          .map(c => toNumber(c.id))
          .filter(id => id !== null) as number[];
      }
    }

    if (childIds.length > 0) {
      console.log(`[Gateway] Fetching products for ${categoryId} subcategories: ${childIds.join(",")}`);
      const subData = await client.get("products", {
        "filter[active]": 1,
        "filter[id_category_default]": `[${childIds.join("|")}]`,
        display: "full",
        limit: "500",
        sort
      });
      const subProducts = extractResourceList<any>("products", subData);
      subProducts.forEach(p => allProductsMap.set(Number(p.id), p));
      console.log(`[Gateway] Method 2: Added ${subProducts.length} products from subcategories`);
    }
  } catch (e: any) {
    console.warn(`[Gateway] Method 2 failed for category ${categoryId}: ${e.message}`);
  }

  let productsRaw = Array.from(allProductsMap.values());
  const start = (page - 1) * pageSize;
  productsRaw = productsRaw.slice(start, start + pageSize);

  const normalized = productsRaw.map((p) => normalizeProduct(p, shopId, lang, allowPrice));
  const filtered = applyServerSideFilters(normalized, filters);

  console.log(`[Gateway] FINAL: Category ${categoryId} returns ${filtered.length} products (Total available: ${allProductsMap.size})`);
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
