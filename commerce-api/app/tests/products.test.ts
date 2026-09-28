import { describe, it, expect, beforeEach, vi } from "vitest";
import { buildApp } from "../src/app.js";
import { productsService } from "../src/modules/products/products.service.js";

describe("Products Module Integration Tests", () => {
  let app: any;

  beforeEach(async () => {
    vi.restoreAllMocks();
    app = await buildApp();
  });

  it("GET /api/v1/products deve listar produtos paginados publicamente sem token", async () => {
    const mockListResult = {
      items: [
        {
          id: "11111111-1111-1111-1111-111111111111",
          name: "Teclado Mecânico RGB",
          slug: "teclado-mecanico-rgb",
          priceCents: 35000,
          stockQuantity: 15,
          isActive: true,
          createdAt: new Date(),
        },
      ],
      pagination: {
        page: 1,
        limit: 20,
        total: 1,
        totalPages: 1,
      },
    };

    vi.spyOn(productsService, "list").mockResolvedValue(mockListResult as any);

    const response = await app.inject({
      method: "GET",
      url: "/api/v1/products",
    });

    expect(response.statusCode).toBe(200);
    const body = JSON.parse(response.body);
    expect(body.items).toHaveLength(1);
    expect(body.pagination.total).toBe(1);
  });

  it("POST /api/v1/products deve barrar usuário não autenticado com 401 Unauthorized", async () => {
    const response = await app.inject({
      method: "POST",
      url: "/api/v1/products",
      payload: {
        name: "Novo Produto",
        priceCents: 10000,
        stockQuantity: 5,
      },
    });

    expect(response.statusCode).toBe(401);
  });

  it("POST /api/v1/products deve barrar cliente comum (customer) com 403 Forbidden via RBAC", async () => {
    const customerToken = app.jwt.sign({
      id: "c1111111-1111-1111-1111-111111111111",
      email: "cliente@teste.com",
      role: "customer",
      name: "Cliente Comum",
    });

    const response = await app.inject({
      method: "POST",
      url: "/api/v1/products",
      headers: {
        authorization: `Bearer ${customerToken}`,
      },
      payload: {
        name: "Novo Produto Ilegal",
        priceCents: 10000,
        stockQuantity: 5,
      },
    });

    expect(response.statusCode).toBe(403);
    const body = JSON.parse(response.body);
    expect(body.error).toBe("Forbidden");
  });

  it("POST /api/v1/products deve permitir cadastro de produto para perfil admin", async () => {
    const adminToken = app.jwt.sign({
      id: "a1111111-1111-1111-1111-111111111111",
      email: "admin@loja.com",
      role: "admin",
      name: "Gerente da Loja",
    });

    const createdProduct = {
      id: "22222222-2222-2222-2222-222222222222",
      name: "Monitor Ultrawide 34",
      slug: "monitor-ultrawide-34",
      priceCents: 250000,
      stockQuantity: 8,
      version: 1,
      isActive: true,
      createdAt: new Date(),
    };

    vi.spyOn(productsService, "create").mockResolvedValue(createdProduct as any);

    const response = await app.inject({
      method: "POST",
      url: "/api/v1/products",
      headers: {
        authorization: `Bearer ${adminToken}`,
      },
      payload: {
        name: "Monitor Ultrawide 34",
        priceCents: 250000,
        stockQuantity: 8,
      },
    });

    expect(response.statusCode).toBe(201);
    const body = JSON.parse(response.body);
    expect(body.product.slug).toBe("monitor-ultrawide-34");
  });
});
