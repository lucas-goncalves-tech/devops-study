# commerce-api

API REST de e-commerce e inventário em **Node 20 · Fastify · Drizzle ORM · Postgres**. É o app da
**trilha AWS** do monorepo: a jornada vai do serviço rodando no Linux até a API em computação real
na nuvem, com estado Terraform remoto, pipeline que bloqueia merge e staging falho que prova a
observabilidade.

## Como rodar

Precisa de Node 20 e de um Postgres. Hoje **não há `docker-compose.yaml`** — ele chega com a
Issue 02 — então o banco sobe na máquina:

```bash
cd commerce-api/app
npm ci
npm run dev
```

Com o Postgres no host e o `DATABASE_URL` default, a API sobe em <http://localhost:3000>.
Conferir: `GET /health` responde `200` com `"status":"UP"` quando o banco responde, e
`503` com `"status":"DEGRADED"` quando não — o 503 é a resposta honesta de um serviço vivo com o
banco caído.

Testes, tipos e build:

```bash
npm test        # vitest run
npm run lint    # tsc --noEmit — o lint deste app é o compilador
npm run build   # tsc -> dist/
```

## Tracker

| # | Issue | Status |
|---|---|---|
| [01](issues/01-linux-runtime.md) | Linux runtime, healthcheck e shutdown gracioso | `todo` |
| [02](issues/02-docker-compose.md) | Compose com banco isolado e API non-root | `todo` |
| [03](issues/03-terraform-vpc.md) | VPC multi-tier em Terraform | `todo` |
| [04](issues/04-github-actions.md) | CI com testes, scan de imagem e gate de IaC | `todo` |
| [05](issues/05-observability.md) | Golden signals, dashboards e carga com SLO | `todo` |
| [06](issues/06-s3-reports-infra.md) | S3 para relatórios com IAM mínimo | `parked` |
| [07](issues/07-aws-production.md) | Estado Terraform remoto e computação real | `todo` |
| [08](issues/08-staging-falho-observabilidade.md) | Staging com falhas de observabilidade injetadas | `todo` |

Issue = uma capacidade, escrita como RFC. Não é tutorial: o passo a passo de cada uma está em
[`estudos/`](estudos/) e o status de todas as trilhas no [`BOARD.md`](../BOARD.md).

- [`issues/`](issues/) — as 8 Issues da trilha AWS
- [`estudos/`](estudos/) — material de estudo, um arquivo por Issue
- [`AGENTS.md`](AGENTS.md) — stack, variáveis de ambiente e gaps conhecidos, para quem vai mexer
