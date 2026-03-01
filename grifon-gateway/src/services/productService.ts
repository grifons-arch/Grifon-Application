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
  return applyServerSideFilters(normalized, filters);
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
  if (categoryId <= 2) {
    return listAllProducts(client, shopId, page, pageSize, sort, lang, allowPrice, filters);
  }

  let products: any[] = [];

  // Προσπάθεια 1: id_category_default (το πιο γρήγορο)
  try {
    const data = await client.get("products", {
      "filter[active]": 1,
      "filter[id_category_default]": categoryId,
      display: "full",
      limit: toLimitParam(page, pageSize),
      sort
    });
    products = extractResourceList<any>("products", data);
  } catch (e) {
    console.error("Method 1 (default category) failed:", e);
  }

  // Προσπάθεια 2: Αν δεν βρέθηκαν προϊόντα, ψάχνουμε μέσω associations
  if (products.length === 0) {
    try {
      // Δοκιμάζουμε απευθείας το ID της κατηγορίας
      const catData = await client.getById("categories", categoryId, { display: "full" });
      const category = extractResourceItem<any>("categories", catData);
      const associations = category?.associations?.products;
      const productIds = extractResourceList<any>("products", { products: associations })
        .map(p => p.id)
        .filter(id => id !== undefined);

      if (productIds.length > 0) {
        const start = (page - 1) * pageSize;
        const slice = productIds.slice(start, start + pageSize);
        if (slice.length > 0) {
          const productsData = await client.get("products", {
            "filter[id]": `[${slice.join("|")}]`,
            display: "full",
            sort
          });
          products = extractResourceList<any>("products", productsData);
        }
      }
    } catch (e) {
      console.error("Method 2 (associations) failed:", e);
    }
  }

  // Προσπάθεια 3: Αν ακόμα τίποτα, δοκιμάζουμε με φίλτρο στην κατηγορία αλλά με άλλο τρόπο
  if (products.length === 0) {
    try {
      const data = await client.get("products", {
        "filter[active]": 1,
        "filter[id_category]": categoryId, // Κάποια plugins υποστηρίζουν αυτό το φίλτρο
        display: "full",
        limit: toLimitParam(page, pageSize),
        sort
      });
      products = extractResourceList<any>("products", data);
    } catch (e) {
      // Αγνοούμε το σφάλμα εδώ
    }
  }

  const normalized = products.map((p) => normalizeProduct(p, shopId, lang, allowPrice));
  return applyServerSideFilters(normalized, filters);
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
