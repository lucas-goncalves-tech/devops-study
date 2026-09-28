import { FastifyInstance } from "fastify";
import { z } from "zod";
import { authService } from "./auth.service.js";

const registerSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
  name: z.string().min(2),
  role: z.enum(["admin", "customer"]).optional(),
});

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
});

export async function authRoutes(app: FastifyInstance) {
  app.post("/register", async (request, reply) => {
    const parseResult = registerSchema.safeParse(request.body);
    if (!parseResult.success) {
      return reply.status(400).send({
        error: "Bad Request",
        message: "Dados de cadastro inválidos",
        issues: parseResult.error.issues,
      });
    }

    try {
      const user = await authService.register(parseResult.data);
      const token = app.jwt.sign(
        { id: user.id, email: user.email, role: user.role, name: user.name },
        { expiresIn: "7d" }
      );

      return reply.status(201).send({ user, token });
    } catch (err: any) {
      if (err.message === "EMAIL_ALREADY_REGISTERED") {
        return reply.status(409).send({
          error: "Conflict",
          message: "Este e-mail já está cadastrado",
        });
      }
      request.log.error(err);
      return reply.status(500).send({ error: "Internal Server Error" });
    }
  });

  app.post("/login", async (request, reply) => {
    const parseResult = loginSchema.safeParse(request.body);
    if (!parseResult.success) {
      return reply.status(400).send({
        error: "Bad Request",
        message: "E-mail e senha são obrigatórios",
      });
    }

    try {
      const user = await authService.login(parseResult.data);
      const token = app.jwt.sign(
        { id: user.id, email: user.email, role: user.role, name: user.name },
        { expiresIn: "7d" }
      );

      return reply.status(200).send({ user, token });
    } catch (err: any) {
      if (err.message === "INVALID_CREDENTIALS") {
        return reply.status(401).send({
          error: "Unauthorized",
          message: "Credenciais inválidas",
        });
      }
      request.log.error(err);
      return reply.status(500).send({ error: "Internal Server Error" });
    }
  });

  app.get(
    "/me",
    { preHandler: [app.authenticate] },
    async (request, reply) => {
      try {
        const user = await authService.getProfile(request.user.id);
        return reply.status(200).send({ user });
      } catch (err: any) {
        if (err.message === "USER_NOT_FOUND") {
          return reply.status(404).send({ error: "Not Found", message: "Usuário não encontrado" });
        }
        return reply.status(500).send({ error: "Internal Server Error" });
      }
    }
  );
}
