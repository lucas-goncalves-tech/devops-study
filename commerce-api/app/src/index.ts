import { buildApp } from "./app.js";
import { env } from "./config/env.js";
import { client } from "./db/connection.js";

async function start() {
  const app = await buildApp();

  try {
    await app.listen({
      port: env.PORT,
      host: env.HOST,
    });
    app.log.info(`Commerce API server listening on http://${env.HOST}:${env.PORT}`);
  } catch (err) {
    app.log.error(err);
    process.exit(1);
  }

  const gracefulShutdown = async (signal: string) => {
    app.log.info(`Received ${signal}. Initiating graceful shutdown...`);
    try {
      await app.close();
      await client.end({ timeout: 5 });
      app.log.info("Closed HTTP server and database pool. Exiting cleanly.");
      process.exit(0);
    } catch (err) {
      app.log.error({ err }, "Error during graceful shutdown");
      process.exit(1);
    }
  };

  process.on("SIGTERM", () => gracefulShutdown("SIGTERM"));
  process.on("SIGINT", () => gracefulShutdown("SIGINT"));
}

start();
