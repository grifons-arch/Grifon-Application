import type { NextFunction, Request, Response } from 'express';
import { PrestaShopError } from '../clients/prestashopClient.js';

export const errorHandler = (err: Error, _req: Request, res: Response, _next: NextFunction) => {
  if (err instanceof PrestaShopError) {
    if (err.status === 401 || err.status === 403) {
      return res.status(502).json({ error: { code: 502, message: 'Upstream auth failed' } });
    }
    if (err.status === 404) {
      return res.status(404).json({ error: { code: 404, message: 'Not found' } });
    }
    if (err.status === 504) {
      return res.status(504).json({ error: { code: 504, message: 'Upstream timeout' } });
    }
    return res.status(502).json({ error: { code: 502, message: 'Upstream error' } });
  }

  return res.status(500).json({ error: { code: 500, message: 'Internal server error' } });
};
