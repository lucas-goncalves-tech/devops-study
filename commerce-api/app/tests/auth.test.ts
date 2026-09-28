import { describe, it, expect, beforeEach, vi } from "vitest";
import { buildApp } from "../src/app.js";
import { authService } from "../src/modules/auth/auth.service.js";

describe("Auth Module Integration Tests", () => {
  let app: any;

  beforeEach(async () => {
    vi.restoreAllMocks();
    app = await buildApp();
  });

  it("POST /api/v1/auth/register deve cadastrar novo usuário e retornar token JWT", async () => {
    const mockUser = {
      id: "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      email: "cliente@teste.com",
      name: "Cliente Teste",
      role: "customer",
      createdAt: new Date(),
    };

    vi.spyOn(authService, "register").mockResolvedValue(mockUser);

    const response = await app.inject({
      method: "POST",
      url: "/api/v1/auth/register",
      payload: {
        email: "cliente@teste.com",
        password: "senhaSegura123",
        name: "Cliente Teste",
      },
    });

    expect(response.statusCode).toBe(201);
    const body = JSON.parse(response.body);
    expect(body.user.email).toBe("cliente@teste.com");
    expect(body.token).toBeDefined();
  });

  it("POST /api/v1/auth/register deve rejeitar e-mail duplicado com 409 Conflict", async () => {
    vi.spyOn(authService, "register").mockRejectedValue(new Error("EMAIL_ALREADY_REGISTERED"));

    const response = await app.inject({
      method: "POST",
      url: "/api/v1/auth/register",
      payload: {
        email: "duplicado@teste.com",
        password: "senhaSegura123",
        name: "Cliente Duplicado",
      },
    });

    expect(response.statusCode).toBe(409);
    const body = JSON.parse(response.body);
    expect(body.error).toBe("Conflict");
  });

  it("POST /api/v1/auth/login deve autenticar com sucesso e retornar token JWT", async () => {
    const mockUser = {
      id: "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      email: "cliente@teste.com",
      name: "Cliente Teste",
      role: "customer",
    };

    vi.spyOn(authService, "login").mockResolvedValue(mockUser);

    const response = await app.inject({
      method: "POST",
      url: "/api/v1/auth/login",
      payload: {
        email: "cliente@teste.com",
        password: "senhaSegura123",
      },
    });

    expect(response.statusCode).toBe(200);
    const body = JSON.parse(response.body);
    expect(body.token).toBeDefined();
  });

  it("POST /api/v1/auth/login deve rejeitar credenciais inválidas com 401 Unauthorized", async () => {
    vi.spyOn(authService, "login").mockRejectedValue(new Error("INVALID_CREDENTIALS"));

    const response = await app.inject({
      method: "POST",
      url: "/api/v1/auth/login",
      payload: {
        email: "cliente@teste.com",
        password: "senhaErrada",
      },
    });

    expect(response.statusCode).toBe(401);
  });

  it("GET /api/v1/auth/me deve retornar 401 Unauthorized quando nenhum token for fornecido", async () => {
    const response = await app.inject({
      method: "GET",
      url: "/api/v1/auth/me",
    });

    expect(response.statusCode).toBe(401);
  });
});
