# Multi-Stage Dockerfile para Commerce API (Node.js 20 LTS / TypeScript)
# Estágio 1: Build da aplicação
FROM node:20-alpine AS builder

WORKDIR /build

COPY package*.json tsconfig.json ./
RUN npm ci

COPY src ./src
RUN npm run build

# Estágio 2: Runtime de Produção Enxuto (< 150MB, Non-Root)
FROM node:20-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000
ENV HOST=0.0.0.0

COPY package*.json ./
RUN npm ci --only=production && npm cache clean --force

COPY --from=builder /build/dist ./dist

USER node

EXPOSE 3000

HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:3000/health || exit 1

CMD ["node", "dist/index.js"]
