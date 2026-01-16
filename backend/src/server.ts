import express from 'express';
import { healthHandler } from './routes/health.js';

export const createApp = () => {
  const app = express();

  app.use(express.json());
  app.get('/health', healthHandler);

  return app;
};
