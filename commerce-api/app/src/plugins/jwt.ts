import { FastifyInstance, FastifyReply, FastifyRequest } from "fastify";
import fastifyJwt from "@fastify/jwt";
import { env } from "../config/env.js";

declare module "@fastify/jwt" {
  interface FastifyJWT {
    payload: { id: string; email: string; role: string; name: string };
    user: { id: string; email: string; role: string; name: string };
  }
}

export async function registerJwt(app: FastifyInstance) {
  await app.register(fastifyJwt, {
    secret: env.JWT_SECRET,
  });

  app.decorate(
    "authenticate",
    async function (request: FastifyRequest, reply: FastifyReply) {
      try {
        await request.jwtVerify();
      } catch {
        reply.status(401).send({ error: "Unauthorized", message: "Token inválido ou ausente" });
      }
    }
  );

  app.decorate(
    "requireRole",
    function (allowedRoles: string[]) {
      return async function (request: FastifyRequest, reply: FastifyReply) {
        try {
          await request.jwtVerify();
          const user = request.user;
          if (!allowedRoles.includes(user.role)) {
            reply.status(403).send({
              error: "Forbidden",
              message: `Acesso restrito para perfis: ${allowedRoles.join(", ")}`,
            });
          }
        } catch {
          reply.status(401).send({ error: "Unauthorized", message: "Token inválido ou ausente" });
        }
      };
    }
  );
}

declare module "fastify" {
  interface FastifyInstance {
    authenticate: (request: FastifyRequest, reply: FastifyReply) => Promise<void>;
    requireRole: (
      allowedRoles: string[]
    ) => (request: FastifyRequest, reply: FastifyReply) => Promise<void>;
  }
}
