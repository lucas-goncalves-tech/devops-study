# SecurePay DevOps

AI-assisted payment API (ledger-service) built with Spring Boot 21, deployed through a progressive DevOps pipeline. The agent's role is to assist with infrastructure, CI/CD, observability, and operational tasks — not to modify business logic unless explicitly asked.

## Filetree

```
securepay-devops/
├── AGENTS.md
├── healthcheck.sh
├── backend/                    # Spring Boot payment API
│   ├── Dockerfile
│   ├── docker-compose.yaml
│   ├── pom.xml
│   └── src/
│       └── main/java/com/securepay/ledger/
│           ├── domain/         # Entities + repositories (JPA)
│           ├── event/          # PaymentEventPublisher (Redis/NoOp)
│           ├── report/         # ReportRepository (S3/NoOp) — infra-agnostic
│           ├── payment/        # PaymentService, WalletService
│           ├── security/       # JWT auth, SecurityConfig
│           └── infrastructure/ # GlobalExceptionHandler, OpenApiConfig
├── infra/                      # Terraform IaC
│   ├── provider.tf             # AWS provider + LocalStack endpoints
│   └── vpc.tf                  # VPC multi-tier + subnets
├── .tracker/                   # Obsidian-compatible issue tracker
│   ├── BOARD.md                # status e ordem de execução (01 → 18)
│   ├── 00-visao-geral.md       # contexto consolidado, template, política de status
│   ├── issues/                 # 01–18 — uma Issue por capacidade (template fixo)
│   └── estudos/                # material de estudo por Issue — fora do escopo das Issues
└── .agents/skills/             # Matt Pocock engineering skills
```

## DevOps Pipeline

Issues numbered `01 → 18` in `.tracker/issues/`. Status lives in `.tracker/BOARD.md`.

| # | Focus | Status |
|---|-------|--------|
| 01 | Linux runtime, env vars, healthcheck, POSIX signals | Done |
| 02 | Multi-stage Dockerfile, Docker Compose, non-root user | Done |
| 03 | Terraform HCL, VPC multi-tier, ALB, LocalStack | Done |
| 04 | VPS hardening — key-only SSH, minimal firewall, swap | To Do |
| 05 | Reverse proxy — Caddy, TLS, security headers | To Do |
| 06 | Off-site DB backups with retention + tested restore | To Do |
| 07 | Prometheus, Grafana dashboards, k6 load testing | To Do |
| 08 | Multi-service Compose, Redis Streams, webhook gateway | To Do |
| 09 | Network isolation, DB/Redis lockdown, resource limits | To Do |
| 10 | GitHub Actions CI, image scanning, IaC gate | To Do |
| 11 | S3 Reports Infra — bucket, IAM, endpoint | Parked |
| 12 | AWS production — remote state, minimal compute | To Do |
| 13 | Secrets hygiene — blocking secret scan | To Do |
| 14 | SAST Semgrep — blocking static analysis | To Do |
| 15 | Pipeline hardening — least-privilege, SHA pin | To Do |
| 16 | Consolidated DevSecOps gates, per-gate timing | To Do |
| 17 | CI/CD to VPS — gated deploy, rollback, audit trail | To Do |
| 18 | Kubernetes multi-node, Helm chart, probes, limits | To Do |

## Backend Architecture

### Infrastructure Ports (Interfaces)

The app uses ports to remain infrastructure-agnostic. Each port has a NoOp (default) and a real implementation activated via `@ConditionalOnProperty`.

| Port | Interface | NoOp | Real | Activated by |
|------|-----------|------|------|--------------|
| Events | `PaymentEventPublisher` | `NoOpPaymentEventPublisher` | `RedisPaymentEventPublisher` | `REDIS_ENABLED=true` |
| Reports | `ReportRepository` | `NoOpReportRepository` | `S3ReportRepository` | `S3_ENABLED=true` |

### Environment Variables (Infrastructure Contract)

When infra is ready, the app expects:

| Variable | Default | Used by |
|----------|---------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/securepay_db` | PostgreSQL |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis |
| `REDIS_ENABLED` | `false` | Event publisher |
| `S3_ENABLED` | `false` | Report repository |
| `S3_BUCKET_NAME` | `securepay-financial-reports` | S3 reports |
| `AWS_REGION` | `sa-east-1` | S3 region |
| `S3_ENDPOINT_URL` | (empty) | LocalStack only |
| `JWT_SECRET` | (built-in) | JWT signing |

### Infra → App Wiring

When infra is created, set these to activate real implementations:
- **Redis**: `REDIS_ENABLED=true`, `SPRING_DATA_REDIS_HOST=<redis-host>`
- **S3**: `S3_ENABLED=true`, `S3_BUCKET_NAME=<bucket>`, `AWS_REGION=<region>`, `S3_ENDPOINT_URL=<endpoint>` (LocalStack only)

## Startup

Invoke `using-superpowers` at session start to load the skill framework. The user triggers `/ask-matt` when they need a skill router.

## Agent Behavior

- Doubt → teach-anything: dúvida, não entendi, explica, como funciona, me ensina → tutor via `.agents/skills/teach-anything/SKILL.md` (READ-ONLY BLOCO → EXPLICAÇÃO in chat, never touches files; only `.md` via consolidate).
- Write scope (outside teaching): implement only in `backend/` and `.tracker/` — other paths are read-only (read, plan, propose diff, wait for explicit request). While teaching, skill overrides scope: zero writes everywhere.
  - Exception (course mode): the user-triggered `teach` skill (`.agents/skills/teach/`) may write only under `.learning/` — one workspace per mission (ex.: `.learning/07-observability/`).
- Routing: dúvida pontual → `teach-anything` (auto, read-only, chat); curso longo → usuário digita `/teach` (nunca o agente — a skill tem `disable-model-invocation`).
- Grilling: when using `grilling` or `grill-me` skills, always use the `question` tool to ask questions — never output questions as plain text in the response.
- Tracker: every card in `.tracker/issues/` follows the fixed template described in `00-visao-geral.md` (Contexto → Limitações / notas). Never add tutorials, FAQ, nav links (`Prev`/`Next`) or sub-steps (`1A`, `2B`) to an Issue — study material belongs in `.tracker/estudos/`.

## Conventions

- All infra targets LocalStack (`localhost:4566`) in `sa-east-1` — no real AWS costs
- Backend: Java 21, Maven, Spring Boot with Actuator endpoints
- Healthcheck: L4 port check + L7 `/actuator/health` (status UP)
- Security: non-root containers, no ports exposed to 0.0.0.0/0, JWT auth
- Infra isolation: backend uses interfaces (`ReportRepository`, `PaymentEventPublisher`) — never depends directly on S3, Redis, or other infra
- Infra activation: `@ConditionalOnProperty` switches NoOp ↔ real implementation
