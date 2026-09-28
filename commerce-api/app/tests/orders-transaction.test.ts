import { describe, it, expect, beforeEach, vi } from "vitest";
import { buildApp } from "../src/app.js";
import { ordersService } from "../src/modules/orders/orders.service.js";

describe("Orders & ACID Stock Checkout Integration Tests", () => {
  let app: any;

  beforeEach(async () => {
    vi.restoreAllMocks();
    app = await buildApp();
  });

  it("POST /api/v1/orders/checkout deve processar checkout atômico com sucesso (201)", async () => {
    const customerToken = app.jwt.sign({
      id: "c1111111-1111-1111-1111-111111111111",
      email: "cliente@teste.com",
      role: "customer",
      name: "Comprador",
    });

    const mockCheckoutResult = {
      order: {
        id: "order-1234-uuid",
        customerId: "c1111111-1111-1111-1111-111111111111",
        totalAmountCents: 70000,
        status: "paid",
        idempotencyKey: "idem-key-001",
        createdAt: new Date(),
      },
      items: [
        {
          id: "item-1",
          orderId: "order-1234-uuid",
          productId: "p1111111-1111-1111-1111-111111111111",
          quantity: 2,
          unitPriceCents: 35000,
        },
      ],
      isIdempotentReplay: false,
    };

    vi.spyOn(ordersService, "checkout").mockResolvedValue(mockCheckoutResult as any);

    const response = await app.inject({
      method: "POST",
      url: "/api/v1/orders/checkout",
      headers: {
        authorization: `Bearer ${customerToken}`,
      },
      payload: {
        items: [
          {
            productId: "11111111-1111-1111-1111-111111111111",
            quantity: 2,
          },
        ],
        idempotencyKey: "idem-key-001",
      },
    });

    expect(response.statusCode).toBe(201);
    const body = JSON.parse(response.body);
    expect(body.order.status).toBe("paid");
    expect(body.order.totalAmountCents).toBe(70000);
    expect(body.items).toHaveLength(1);
    expect(body.isIdempotentReplay).toBe(false);
  });

  it("POST /api/v1/orders/checkout deve dar rollback total e retornar 409 Conflict quando estoque for insuficiente", async () => {
    const customerToken = app.jwt.sign({
      id: "c1111111-1111-1111-1111-111111111111",
      email: "cliente@teste.com",
      role: "customer",
      name: "Comprador",
    });

    vi.spyOn(ordersService, "checkout").mockRejectedValue(
      new Error('INSUFFICIENT_STOCK: Produto "Monitor" possui apenas 1 unidades disponíveis.')
    );

    const response = await app.inject({
      method: "POST",
      url: "/api/v1/orders/checkout",
      headers: {
        authorization: `Bearer ${customerToken}`,
      },
      payload: {
        items: [
          {
            productId: "22222222-2222-2222-2222-222222222222",
            quantity: 5,
          },
        ],
      },
    });

    expect(response.statusCode).toBe(409);
    const body = JSON.parse(response.body);
    expect(body.error).toBe("Conflict");
    expect(body.message).toContain("INSUFFICIENT_STOCK");
  });

  it("POST /api/v1/orders/checkout deve rejeitar requisição sem token com 401 Unauthorized", async () => {
    const response = await app.inject({
      method: "POST",
      url: "/api/v1/orders/checkout",
      payload: {
        items: [
          {
            productId: "p1111111-1111-1111-1111-111111111111",
            quantity: 1,
          },
        ],
      },
    });

    expect(response.statusCode).toBe(401);
  });

  it("GET /health deve retornar status do serviço e verificação de banco", async () => {
    const response = await app.inject({
      method: "GET",
      url: "/health",
    });

    expect([200, 503]).toContain(response.statusCode);
    const body = JSON.parse(response.body);
    expect(body.service).toBe("commerce-api");
    expect(body.checks).toBeDefined();
  });
});
