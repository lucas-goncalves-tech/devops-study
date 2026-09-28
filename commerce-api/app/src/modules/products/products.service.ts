import { eq, and, desc, sql } from "drizzle-orm";
import { db as defaultDb } from "../../db/connection.js";
import { products, inventoryAuditLogs } from "../../db/schema.js";

export class ProductsService {
  constructor(private db = defaultDb) {}

  async list(options: { page?: number; limit?: number; activeOnly?: boolean } = {}) {
    const page = Math.max(1, options.page || 1);
    const limit = Math.min(100, Math.max(1, options.limit || 20));
    const offset = (page - 1) * limit;

    const conditions = options.activeOnly ? [eq(products.isActive, true)] : [];

    const items = await this.db
      .select()
      .from(products)
      .where(conditions.length > 0 ? and(...conditions) : undefined)
      .orderBy(desc(products.createdAt))
      .limit(limit)
      .offset(offset);

    const [totalCountResult] = await this.db
      .select({ count: sql<number>`count(*)` })
      .from(products)
      .where(conditions.length > 0 ? and(...conditions) : undefined);

    const total = Number(totalCountResult?.count || 0);

    return {
      items,
      pagination: {
        page,
        limit,
        total,
        totalPages: Math.ceil(total / limit),
      },
    };
  }

  async getById(id: string) {
    const product = await this.db.query.products.findFirst({
      where: eq(products.id, id),
    });
    if (!product) {
      throw new Error("PRODUCT_NOT_FOUND");
    }
    return product;
  }

  async create(data: {
    name: string;
    description?: string;
    priceCents: number;
    stockQuantity: number;
  }) {
    const slug = data.name
      .toLowerCase()
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/(^-|-$)/g, "");

    const existing = await this.db.query.products.findFirst({
      where: eq(products.slug, slug),
    });

    const finalSlug = existing ? `${slug}-${Date.now().toString().slice(-4)}` : slug;

    const [product] = await this.db
      .insert(products)
      .values({
        name: data.name.trim(),
        slug: finalSlug,
        description: data.description?.trim(),
        priceCents: data.priceCents,
        stockQuantity: Math.max(0, data.stockQuantity),
      })
      .returning();

    if (data.stockQuantity > 0) {
      await this.db.insert(inventoryAuditLogs).values({
        productId: product.id,
        deltaQuantity: data.stockQuantity,
        reason: "INITIAL_STOCK_ENTRY",
      });
    }

    return product;
  }
}

export const productsService = new ProductsService();
