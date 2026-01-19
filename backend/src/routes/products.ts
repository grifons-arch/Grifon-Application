import type { Request, Response } from 'express';
import { PrestaShopClient } from '../clients/prestashopClient.js';
import { asyncHandler } from '../utils/asyncHandler.js';
import { buildPaginationLimit, buildSortParam, ensureArray } from '../utils/prestashop.js';
import { env } from '../config/env.js';

const client = new PrestaShopClient();

const buildImageUrls = (productId: string | number, images: Array<{ id?: number | string }>) => {
  return images
    .map((image) => image.id)
    .filter((id): id is number | string => id !== undefined)
    .map((id) => ({
      id,
      url: `${env.PRESTASHOP_BASE_URL.replace(/\/$/, '')}/images/products/${productId}/${id}`
    }));
};

export const productsHandler = asyncHandler(async (req: Request, res: Response) => {
  const page = Number(req.query.page ?? 1);
  const pageSize = Number(req.query.pageSize ?? 20);
  const fields = String(req.query.fields ?? 'id,name,price,reference');
  const sort = buildSortParam(req.query.sort ? String(req.query.sort) : undefined);
  const search = req.query.search ? String(req.query.search) : undefined;
  const categoryId = req.query.categoryId ? String(req.query.categoryId) : undefined;

  const response = await client.get<{ products?: { product?: unknown } }>('products', {
    display: fields,
    limit: buildPaginationLimit(page, pageSize),
    sort,
    ...(search ? { 'filter[name]': `%${search}%` } : {}),
    ...(categoryId ? { 'filter[id_category_default]': categoryId } : {})
  });

  const items = ensureArray((response as { products?: { product?: unknown } }).products?.product);

  res.json({ items, page, pageSize });
});

export const productDetailHandler = asyncHandler(async (req: Request, res: Response) => {
  const productId = req.params.id;
  const response = await client.get<{ product?: Record<string, unknown> }>(`products/${productId}`, {
    display: 'full'
  });

  const product = response.product ?? {};
  const associations = (product as { associations?: Record<string, unknown> }).associations ?? {};
  const rawImages = ensureArray(
    (associations as { images?: { image?: { id?: number | string } | Array<{ id?: number | string }> } }).images?.image
  );
  const rawCategories = ensureArray(
    (associations as { categories?: { category?: { id?: number | string } | Array<{ id?: number | string }> } }).categories
      ?.category
  );

  const images = buildImageUrls(productId, rawImages);
  const categories = rawCategories.map((category) => category.id).filter((id): id is number | string => id !== undefined);

  let manufacturer = undefined;
  const manufacturerId = (product as { id_manufacturer?: number | string }).id_manufacturer;
  if (manufacturerId) {
    const manufacturerResponse = await client.get<{ manufacturer?: { id?: number | string; name?: string } }>(
      `manufacturers/${manufacturerId}`,
      { display: 'id,name' }
    );
    manufacturer = manufacturerResponse.manufacturer;
  }

  const stockResponse = await client.get<{ stock_availables?: { stock_available?: unknown } }>('stock_availables', {
    display: 'id,quantity,depends_on_stock,out_of_stock',
    'filter[id_product]': productId
  });
  const stockEntries = ensureArray(
    (stockResponse as { stock_availables?: { stock_available?: unknown } }).stock_availables?.stock_available
  );
  const stock = stockEntries[0] ?? undefined;

  res.json({
    ...product,
    images,
    categories,
    manufacturer,
    stock
  });
});

export const productImagesHandler = asyncHandler(async (req: Request, res: Response) => {
  const productId = req.params.id;
  const response = await client.get<{ product?: Record<string, unknown> }>(`products/${productId}`, {
    display: '[id]',
    'display[images]': '[id]'
  });

  const associations = (response.product as { associations?: Record<string, unknown> } | undefined)?.associations ?? {};
  const rawImages = ensureArray(
    (associations as { images?: { image?: { id?: number | string } | Array<{ id?: number | string }> } }).images?.image
  );

  res.json({ images: buildImageUrls(productId, rawImages) });
});

export const productStockHandler = asyncHandler(async (req: Request, res: Response) => {
  const productId = req.params.productId;
  const stockResponse = await client.get<{ stock_availables?: { stock_available?: unknown } }>('stock_availables', {
    display: 'id,quantity,depends_on_stock,out_of_stock',
    'filter[id_product]': productId
  });
  const stockEntries = ensureArray(
    (stockResponse as { stock_availables?: { stock_available?: unknown } }).stock_availables?.stock_available
  );
  const stock = stockEntries[0] as
    | { quantity?: number; out_of_stock?: number; depends_on_stock?: number }
    | undefined;

  res.json({
    productId,
    quantity: stock?.quantity ?? 0,
    outOfStockBehavior: stock?.out_of_stock,
    dependsOnStock: stock?.depends_on_stock
  });
});

export const searchHandler = asyncHandler(async (req: Request, res: Response) => {
  const query = req.query.q ? String(req.query.q) : '';
  if (!query) {
    return res.status(400).json({ error: { code: 400, message: 'Query parameter q is required' } });
  }
  const page = Number(req.query.page ?? 1);
  const pageSize = Number(req.query.pageSize ?? 20);
  const fields = String(req.query.fields ?? 'id,name,price,reference');

  const response = await client.get<{ products?: { product?: unknown } }>('products', {
    display: fields,
    limit: buildPaginationLimit(page, pageSize),
    'filter[name]': `%${query}%`
  });

  const items = ensureArray((response as { products?: { product?: unknown } }).products?.product);

  res.json({ items, page, pageSize });
});
