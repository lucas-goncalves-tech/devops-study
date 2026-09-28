import { FastifyInstance } from "fastify";
import { z } from "zod";
import { productsService } from "./products.service.js";

const createProductSchema = z.object({
  name: z.string().min(2),
  description: z.string().optional(),
  priceCents: z.number().int().positive(),
  stockQuantity: z.number().int().nonnegative(),
});

export async function productsRoutes(app: FastifyInstance) {
  app.get("/", async (request, reply) => {
    const query = request.query as { page?: string; limit?: string };
    const page = query.page ? parseInt(query.page, 10) : 1;
    const limit = query.limit ? parseInt(query.limit, 10) : 20;

    const result = await productsService.list({ page, limit, activeOnly: true });
    return reply.status(200).send(result);
  });

  app.get("/:id", async (request, reply) => {
    const { id } = request.params as { id: string };
    try {
      const product = await productsService.getById(id);
      return reply.status(200).send({ product });
    } catch (err: any) {
      if (err.message === "PRODUCT_NOT_FOUND") {
        return reply.status(404).send({ error: "Not Found", message: "Produto não encontrado" });
      }
      return reply.status(500).send({ error: "Internal Server Error" });
    }
  });

  app.post(
    "/",
    { preHandler: [app.requireRole(["admin"])] },
    async (request, reply) => {
      const parseResult = createProductSchema.safeParse(request.body);
      if (!parseResult.success) {
        return reply.status(400).send({
          error: "Bad Request",
          message: "Dados do produto inválidos",
          issues: parseResult.error.issues,
        });
      }

      try {
        const product = await productsService.create(parseResult.data);
        return reply.status(201).send({ product });
      } catch (err: any) {
        request.log.error(err);
        return reply.status(500).send({ error: "Internal Server Error" });
      }
    }
  );
}
