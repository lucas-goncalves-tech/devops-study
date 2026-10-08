CREATE TABLE "users" (
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "display_name" VARCHAR(100) NOT NULL,
    "email" VARCHAR(150) NOT NULL UNIQUE,
    "password" VARCHAR(255) NOT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);