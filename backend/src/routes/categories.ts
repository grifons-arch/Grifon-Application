import type { Request, Response } from 'express';
import { PrestaShopClient } from '../clients/prestashopClient.js';
import { asyncHandler } from '../utils/asyncHandler.js';
import { buildPaginationLimit, ensureArray } from '../utils/prestashop.js';

const client = new PrestaShopClient();

export const categoriesHandler = asyncHandler(async (req: Request, res: Response) => {
  const page = Number(req.query.page ?? 1);
  const pageSize = Number(req.query.pageSize ?? 20);
  const display = String(req.query.display ?? 'id,name');

  const response = await client.get<{ categories?: { category?: unknown } }>('categories', {
    display,
    limit: buildPaginationLimit(page, pageSize)
  });

  const items = ensureArray((response as { categories?: { category?: unknown } }).categories?.category);

  res.json({ items, page, pageSize });
});
