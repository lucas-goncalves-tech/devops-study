import { FastifyInstance } from "fastify";
import { z } from "zod";
import { ordersService } from "./orders.service.js";

const checkoutSchema = z.object({
  items: z
    .array(
      z.object({
        productId: z.string().uuid(),
        quantity: z.number().int().positive(),
      })
    )
    .min(1),
  idempotencyKey: z.string().optional(),
});

export async function ordersRoutes(app: FastifyInstance) {
  app.post(
    "/checkout",
    { preHandler: [app.authenticate] },
    async (request, reply) => {
      const parseResult = checkoutSchema.safeParse(request.body);
      if (!parseResult.success) {
        return reply.status(400).send({
          error: "Bad Request",
          message: "Dados de checkout inválidos",
          issues: parseResult.error.issues,
        });
      }

      try {
        const result = await ordersService.checkout({
          customerId: request.user.id,
          items: parseResult.data.items,
          idempotencyKey: parseResult.data.idempotencyKey,
        });

        return reply.status(201).send(result);
      } catch (err: any) {
        if (err.message?.startsWith("INSUFFICIENT_STOCK")) {
          return reply.status(409).send({
            error: "Conflict",
            message: err.message,
          });
        }
        if (err.message?.startsWith("PRODUCT_NOT_AVAILABLE")) {
          return reply.status(404).send({
            error: "Not Found",
            message: err.message,
          });
        }
        request.log.error(err);
        return reply.status(500).send({ error: "Internal Server Error" });
      }
    }
  );

  app.get(
    "/",
    { preHandler: [app.authenticate] },
    async (request, reply) => {
      try {
        if (request.user.role === "admin") {
          const allOrders = await ordersService.listAllOrders();
          return reply.status(200).send({ orders: allOrders });
        } else {
          const myOrders = await ordersService.listCustomerOrders(request.user.id);
          return reply.status(200).send({ orders: myOrders });
        }
      } catch (err: any) {
        request.log.error(err);
        return reply.status(500).send({ error: "Internal Server Error" });
      }
    }
  );
}
