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
  return {
    id,
    name: getLocalizedValue(product.name, lang),
    price: allowPrice ? toNumber(product.price) : null,
    reference: product.reference ?? null,
    defaultImage: idImage ? { id: idImage, url: buildImageUrl(shopId, id, idImage) } : null,
    active: toNumber(product.active)
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
    limit: toLimitParam(page, pageSize), // Χρήση δυναμικού limit για σελιδοποίηση
    display: "full" 
  });

  const items = extractResourceList<any>("products", data);
  return items.map((product) => normalizeProduct(product, shopId, lang, allowPrice));
};

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
  if (categoryId === 2) {
    return listAllProducts(client, shopId, page, pageSize, sort, lang, allowPrice);
  }

  const data = await client.get("products", {
    "filter[active]": 1,
    "filter[id_category_default]": categoryId,
    sort,
    limit: toLimitParam(page, pageSize),
    display: "full"
  });

  const items = extractResourceList<any>("products", data);
  return items.map((product) => normalizeProduct(product, shopId, lang, allowPrice));
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
