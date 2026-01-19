import express from "express";
import helmet from "helmet";
import cors from "cors";
import rateLimit from "express-rate-limit";
import { config } from "./config/env";
import { requestLogger } from "./middleware/requestId";
import { apiRouter } from "./routes";
import { errorHandler } from "./middleware/errorHandler";

const app = express();

const corsOptions = config.allowedOrigins === "*"
  ? { origin: true }
  : {
      origin: config.allowedOrigins.split(",").map((origin) => origin.trim())
    };

app.use(requestLogger);
app.use(helmet());
app.use(cors(corsOptions));
app.use(express.json({ limit: "1mb" }));

app.use(
  rateLimit({
    windowMs: 60 * 1000,
    limit: config.rateLimitPerMin
  })
);

app.use(apiRouter);

app.use(errorHandler);

app.listen(config.port, () => {
  // eslint-disable-next-line no-console
  console.log(`grifon-gateway running on port ${config.port}`);
});
