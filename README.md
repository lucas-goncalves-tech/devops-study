# SecurePay — 3 apps, 3 trilhas DevSecOps

Monorepo de estudo em DevSecOps: três aplicações independentes, cada uma com a **sua própria
trilha** de Issues — do serviço local até produção. Cada Issue descreve uma capacidade do ciclo
operacional (runtime, container, hardening, CI/CD, observabilidade, produção) e o repositório é a
infraestrutura real construída para prová-la.

Não é uma sequência global: entrar num app é fazer uma jornada inteira. Numeração de Issue
reinicia em `01` por app.

## Apps

| App | Stack | Trilha | Rodar | Docs |
|---|---|---|---|---|
| [`ledger-service/`](ledger-service/) | Java 21 · Spring Boot 3.3 · Postgres | **VPS** — Linux → hardening → Caddy → backups → isolamento → deploy | `cd ledger-service/app && cp .env.example .env && sed -i 's#^SPRING_DATASOURCE_URL=.*#SPRING_DATASOURCE_URL=jdbc:postgresql://database:5432/securepay_db#' .env && docker compose up --build` | [README](ledger-service/README.md) · [AGENTS](ledger-service/AGENTS.md) |
| [`commerce-api/`](commerce-api/) | Node 20 · Fastify · Postgres | **AWS** — Linux → compose → Terraform → CI → observabilidade → S3/EC2 → apply → deploy | `cd commerce-api/app && npm ci && npm run dev` | [README](commerce-api/README.md) · [AGENTS](commerce-api/AGENTS.md) |
| [`webhook-gateway/`](webhook-gateway/) | Node 20 · TypeScript · Redis Streams | **DevSecOps** — pipeline → secrets → SAST → SCA → hardening → gates → DAST → mensageria | `cd webhook-gateway/app && npm ci && npm run build && npm start` | [README](webhook-gateway/README.md) · [AGENTS](webhook-gateway/AGENTS.md) |

Pré-requisitos: Docker + Compose (`ledger-service`) e Node 20 (`commerce-api`, `webhook-gateway`).
Redis para o `webhook-gateway`, Postgres para os outros dois. Detalhes de ambiente, rotas e portas
no `README.md` de cada app.

O `sed -i` da linha do `ledger-service` é GNU/Linux; em macOS/BSD use `sed -i ''`. O comando existe
porque o `.env.example` aponta o banco para `localhost`, que dentro do Compose é a própria API — o
host do banco precisa ser o nome do serviço `database`.

## Onde começar

- **[BOARD.md](BOARD.md)** — status de cada Issue e a ordem de execução das 3 trilhas.
- **[00-visao-geral.md](00-visao-geral.md)** — metodologia, template de Issue e política de status.
- `docs/superpowers/` — specs e planos do trabalho em andamento.
- `archive/` — Issues arquivadas por decisão (Kubernetes).

## Princípios

- **Custo zero por regra:** nenhuma Issue exige recurso pago. Infra local, LocalStack e
  `terraform plan` cobrem o quase tudo; VPS pública e conta AWS só como prova final.
- **Evidência, não intenção:** uma Issue só fecha com a saída real do comando de validação
  registrada. Limitação de ambiente não vale como evidência.
- **Problema antes de ferramenta:** cada linha do board diz o que a Issue resolve; a tecnologia é
  consequência, nunca objetivo.
