import { eq, desc, sql } from "drizzle-orm";
import { db as defaultDb } from "../../db/connection.js";
import { orders, orderItems, products, inventoryAuditLogs } from "../../db/schema.js";

export interface CheckoutItem {
  productId: string;
  quantity: number;
}

export class OrdersService {
  constructor(private db = defaultDb) {}

  async checkout(data: {
    customerId: string;
    items: CheckoutItem[];
    idempotencyKey?: string;
  }) {
    if (!data.items || data.items.length === 0) {
      throw new Error("EMPTY_CART");
    }

    return await this.db.transaction(async (tx) => {
      // 1. Verificação de idempotência
      if (data.idempotencyKey) {
        const existingOrder = await tx.query.orders.findFirst({
          where: eq(orders.idempotencyKey, data.idempotencyKey),
        });
        if (existingOrder) {
          const items = await tx.query.orderItems.findMany({
            where: eq(orderItems.orderId, existingOrder.id),
          });
          return { order: existingOrder, items, isIdempotentReplay: true };
        }
      }

      // 2. Validação e trava de estoque de cada produto
      const validatedProducts: { product: any; quantity: number }[] = [];
      let totalAmountCents = 0;

      for (const item of data.items) {
        if (item.quantity <= 0) {
          throw new Error("INVALID_ITEM_QUANTITY");
        }

        const product = await tx.query.products.findFirst({
          where: eq(products.id, item.productId),
        });

        if (!product || !product.isActive) {
          throw new Error(`PRODUCT_NOT_AVAILABLE: ${item.productId}`);
        }

        if (product.stockQuantity < item.quantity) {
          throw new Error(
            `INSUFFICIENT_STOCK: Produto "${product.name}" possui apenas ${product.stockQuantity} unidades disponíveis.`
          );
        }

        validatedProducts.push({ product, quantity: item.quantity });
        totalAmountCents += product.priceCents * item.quantity;
      }

      // 3. Criação do Pedido
      const [order] = await tx
        .insert(orders)
        .values({
          customerId: data.customerId,
          totalAmountCents,
          status: "paid",
          idempotencyKey: data.idempotencyKey,
        })
        .returning();

      // 4. Baixa atômica de estoque e inserção dos itens
      const insertedItems = [];

      for (const { product, quantity } of validatedProducts) {
        // Baixa no saldo e incremento de versão de concorrência
        await tx
          .update(products)
          .set({
            stockQuantity: sql`stock_quantity - ${quantity}`,
            version: sql`version + 1`,
            updatedAt: new Date(),
          })
          .where(eq(products.id, product.id));

        const [orderItem] = await tx
          .insert(orderItems)
          .values({
            orderId: order.id,
            productId: product.id,
            quantity,
            unitPriceCents: product.priceCents,
          })
          .returning();

        insertedItems.push(orderItem);

        // Registro em auditoria contábil de inventário
        await tx.insert(inventoryAuditLogs).values({
          productId: product.id,
          orderId: order.id,
          deltaQuantity: -quantity,
          reason: "CHECKOUT_ORDER_RESERVATION",
        });
      }

      return {
        order,
        items: insertedItems,
        isIdempotentReplay: false,
      };
    });
  }

  async listCustomerOrders(customerId: string) {
    return await this.db.query.orders.findMany({
      where: eq(orders.customerId, customerId),
      orderBy: [desc(orders.createdAt)],
    });
  }

  async listAllOrders() {
    return await this.db.query.orders.findMany({
      orderBy: [desc(orders.createdAt)],
    });
  }
}

export const ordersService = new OrdersService();
