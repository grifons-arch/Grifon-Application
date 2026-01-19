import { Request, Response, NextFunction } from "express";

export interface ApiError extends Error {
  status?: number;
  code?: string;
  details?: unknown;
}

export const errorHandler = (
  err: ApiError,
  _req: Request,
  res: Response,
  _next: NextFunction
): void => {
  const status = err.status ?? 500;
  const code = err.code ?? "INTERNAL_ERROR";
  const message = err.message ?? "Unexpected error";

  res.status(status).json({
    error: {
      code,
      message,
      details: err.details
    }
  });
};
