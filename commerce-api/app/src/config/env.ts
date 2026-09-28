import { z } from "zod";
import dotenv from "dotenv";

dotenv.config();

const envSchema = z.object({
  NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
  PORT: z.coerce.number().default(3000),
  HOST: z.string().default("0.0.0.0"),
  DATABASE_URL: z
    .string()
    .default("postgresql://commerce_user:commerce_pass@localhost:5432/commerce_db"),
  JWT_SECRET: z.string().min(16).default("super-secret-production-change-me-key-32chars"),
  CORS_ORIGIN: z.string().default("*"),
});

export const env = envSchema.parse(process.env);
