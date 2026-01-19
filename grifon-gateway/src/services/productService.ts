import { PrestaShopClient } from "../clients/PrestaShopClient";
import { extractResourceItem, extractResourceList } from "./prestashopParser";
import { chunkArray, toLimitParam } from "../utils/pagination";
import { getLocalizedValue, toNumber, toBooleanFlag } from "../utils/prestashopFields";
import { config, ShopId } from "../config/env";

export interface ProductListItem {
  id: number;
  name: string | null;
  price: number | null;
  reference: string | null;
  defaultImage: { id: number; url: string } | null;
  active: number | null;
}

export interface ProductDetail {
  id: number;
  name: string | null;
  descriptionShort: string | null;
  description: string | null;
  reference: string | null;
  price: number | null;
  images: { id: number; url: string }[];
  manufacturer?: { id: number; name: string | null };
  categories?: { id: number }[];
  stock?: { quantity: number | null };
}

const buildImageUrl = (shopId: ShopId, productId: number, imageId: number): string => {
  const base = config.shopBaseUrls[shopId];
  return `${base}/images/products/${productId}/${imageId}`;
};

const normalizeProduct = (
  product: any,
  shopId: ShopId,
  lang?: number,
  allowPrice = true
): ProductListItem => {
  const id = Number(product.id);
  const idDefaultImage = toNumber(product.id_default_image);
  const priceValue = toNumber(product.price);

  return {
    id,
    name: getLocalizedValue(product.name, lang),
    price: allowPrice ? priceValue : null,
    reference: product.reference ?? null,
    defaultImage: idDefaultImage
      ? { id: idDefaultImage, url: buildImageUrl(shopId, id, idDefaultImage) }
      : null,
    active: toNumber(product.active)
  };
};

const normalizeProductDetail = (
  product: any,
  shopId: ShopId,
  lang?: number,
  allowPrice = true
): ProductDetail => {
  const id = Number(product.id);
  const images = extractResourceList<any>("images", product?.associations ?? {});
  const imageItems = images.map((image) => ({
    id: Number(image.id),
    url: buildImageUrl(shopId, id, Number(image.id))
  }));

  const categories = extractResourceList<any>("categories", product?.associations ?? {});
  const stock = extractResourceList<any>("stock_availables", product?.associations ?? {});
  const stockItem = stock[0];

  const priceValue = toNumber(product.price);

  return {
    id,
    name: getLocalizedValue(product.name, lang),
    descriptionShort: getLocalizedValue(product.description_short, lang),
    description: getLocalizedValue(product.description, lang),
    reference: product.reference ?? null,
    price: allowPrice ? priceValue : null,
    images: imageItems,
    manufacturer: product.id_manufacturer
      ? { id: Number(product.id_manufacturer), name: product.manufacturer_name ?? null }
      : undefined,
    categories: categories.length ? categories.map((category) => ({ id: Number(category.id) })) : undefined,
    stock: stockItem ? { quantity: toNumber(stockItem.quantity) } : undefined
  };
};

export const listProductsByCategory = async (
  client: PrestaShopClient,
  shopId: ShopId,
  categoryId: number,
  page: number,
  pageSize: number,
  sort: string,
  lang?: number,
  allowPrice = false
): Promise<ProductListItem[]> => {
  const baseParams = {
    "filter[active]": 1,
    sort,
    limit: toLimitParam(page, pageSize),
    display: "[id,name,price,reference,active,id_default_image]"
  } as Record<string, string | number>;

  const filteredData = await client.get("products", {
    ...baseParams,
    "filter[id_category_default]": categoryId
  });

  const filteredItems = extractResourceList<any>("products", filteredData);
  if (filteredItems.length > 0) {
    return filteredItems.map((product) => normalizeProduct(product, shopId, lang, allowPrice));
  }

  const categoryData = await client.getById("categories", categoryId);
  const category = extractResourceItem<any>("categories", categoryData);
  const associations = category?.associations?.products?.product;
  const productIds = Array.isArray(associations)
    ? associations.map((item: any) => Number(item.id))
    : associations
      ? [Number(associations.id)]
      : [];

  const pagedIds = productIds.slice((page - 1) * pageSize, page * pageSize);
  const chunks = chunkArray(pagedIds, 20);
  const results: ProductListItem[] = [];

  for (const chunk of chunks) {
    const chunkData = await client.get("products", {
      "filter[active]": 1,
      "filter[id]": `[${chunk.join("|")}]`,
      display: "[id,name,price,reference,active,id_default_image]"
    });
    const chunkItems = extractResourceList<any>("products", chunkData);
    results.push(...chunkItems.map((product) => normalizeProduct(product, shopId, lang, allowPrice)));
  }

  return results;
};

export const getProductDetail = async (
  client: PrestaShopClient,
  shopId: ShopId,
  productId: number,
  lang?: number,
  allowPrice = false
): Promise<ProductDetail | null> => {
  const data = await client.getById("products", productId, {
    display: "full"
  });
  const product = extractResourceItem<any>("products", data);
  if (!product) return null;

  const active = toBooleanFlag(product.active);
  if (!active) return null;

  return normalizeProductDetail(product, shopId, lang, allowPrice);
};
