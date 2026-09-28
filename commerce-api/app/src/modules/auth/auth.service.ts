import bcrypt from "bcryptjs";
import { eq } from "drizzle-orm";
import { db as defaultDb } from "../../db/connection.js";
import { users } from "../../db/schema.js";

export class AuthService {
  constructor(private db = defaultDb) {}

  async register(data: { email: string; password: string; name: string; role?: string }) {
    const existing = await this.db.query.users.findFirst({
      where: eq(users.email, data.email.toLowerCase().trim()),
    });

    if (existing) {
      throw new Error("EMAIL_ALREADY_REGISTERED");
    }

    const salt = await bcrypt.genSalt(10);
    const passwordHash = await bcrypt.hash(data.password, salt);
    const role = data.role && ["admin", "customer"].includes(data.role) ? data.role : "customer";

    const [user] = await this.db
      .insert(users)
      .values({
        email: data.email.toLowerCase().trim(),
        name: data.name.trim(),
        passwordHash,
        role,
      })
      .returning();

    return {
      id: user.id,
      email: user.email,
      name: user.name,
      role: user.role,
      createdAt: user.createdAt,
    };
  }

  async login(data: { email: string; password: string }) {
    const user = await this.db.query.users.findFirst({
      where: eq(users.email, data.email.toLowerCase().trim()),
    });

    if (!user) {
      throw new Error("INVALID_CREDENTIALS");
    }

    const passwordMatch = await bcrypt.compare(data.password, user.passwordHash);
    if (!passwordMatch) {
      throw new Error("INVALID_CREDENTIALS");
    }

    return {
      id: user.id,
      email: user.email,
      name: user.name,
      role: user.role,
    };
  }

  async getProfile(userId: string) {
    const user = await this.db.query.users.findFirst({
      where: eq(users.id, userId),
    });

    if (!user) {
      throw new Error("USER_NOT_FOUND");
    }

    return {
      id: user.id,
      email: user.email,
      name: user.name,
      role: user.role,
      createdAt: user.createdAt,
    };
  }
}

export const authService = new AuthService();
