import cors from 'cors';
import express from 'express';
import helmet from 'helmet';
import morgan from 'morgan';
import rateLimit from 'express-rate-limit';
import { healthHandler } from './routes/health.js';
import { categoriesHandler } from './routes/categories.js';
import {
  productDetailHandler,
  productImagesHandler,
  productsHandler,
  productStockHandler,
  searchHandler
} from './routes/products.js';
import { cacheMiddleware } from './middleware/cache.js';
import { env } from './config/env.js';
import { errorHandler } from './middleware/errorHandler.js';

export const createApp = () => {
  const app = express();

  const allowedOrigins = env.ALLOWED_ORIGINS.split(',').map((origin) => origin.trim());

  app.use(
    cors({
      origin: (origin, callback) => {
        if (!origin || allowedOrigins.includes('*') || allowedOrigins.includes(origin)) {
          callback(null, true);
          return;
        }
        callback(new Error('Origin not allowed by CORS'));
      }
    })
  );
  app.use(helmet());
  app.use(
    rateLimit({
      windowMs: 60 * 1000,
      limit: 60,
      standardHeaders: true,
      legacyHeaders: false
    })
  );
  app.use(morgan('combined'));
  app.use(express.json());
  app.use(cacheMiddleware());
  app.get('/health', healthHandler);
  app.get('/categories', categoriesHandler);
  app.get('/products', productsHandler);
  app.get('/products/:id', productDetailHandler);
  app.get('/products/:id/images', productImagesHandler);
  app.get('/search', searchHandler);
  app.get('/stock/:productId', productStockHandler);

  app.use(errorHandler);

  return app;
};
