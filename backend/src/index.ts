import { createApp } from './server.js';
import { env } from './config/env.js';

const app = createApp();

app.listen(env.PORT, () => {
  console.log(`Backend listening on port ${env.PORT}`);
});
