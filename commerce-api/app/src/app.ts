import fastify, { FastifyInstance } from "fastify";
import cors from "@fastify/cors";
import { registerJwt } from "./plugins/jwt.js";
import { authRoutes } from "./modules/auth/auth.routes.js";
import { productsRoutes } from "./modules/products/products.routes.js";
import { ordersRoutes } from "./modules/orders/orders.routes.js";
import { checkDatabaseHealth } from "./db/connection.js";
import { env } from "./config/env.js";

export async function buildApp(): Promise<FastifyInstance> {
  const app = fastify({
    logger: env.NODE_ENV !== "test" ? { level: "info" } : false,
  });

  await app.register(cors, {
    origin: env.CORS_ORIGIN,
  });

  await registerJwt(app);

  // Healthcheck de infraestrutura (Liveness & Readiness)
  app.get("/health", async (_request, reply) => {
    const isDbConnected = await checkDatabaseHealth();
    const status = isDbConnected ? 200 : 503;

    return reply.status(status).send({
      status: isDbConnected ? "UP" : "DEGRADED",
      timestamp: new Date().toISOString(),
      service: "commerce-api",
      checks: {
        database: isDbConnected ? "UP" : "DOWN",
      },
    });
  });

  // Prefixo de API versionada
  await app.register(authRoutes, { prefix: "/api/v1/auth" });
  await app.register(productsRoutes, { prefix: "/api/v1/products" });
  await app.register(ordersRoutes, { prefix: "/api/v1/orders" });

  return app;
}
