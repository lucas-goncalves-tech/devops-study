# SecurePay — 3 apps, 3 trilhas DevSecOps

Monorepo de estudo em DevSecOps: três aplicações independentes, cada uma com a **sua própria
trilha** de Issues — do serviço local até produção. Cada Issue descreve uma capacidade do ciclo
operacional (runtime, container, hardening, CI/CD, observabilidade, produção) e o repositório é a
infraestrutura real construída para prová-la.

Não é uma sequência global: entrar num app é fazer uma jornada inteira. Numeração de Issue
reinicia em `01` por app.

## Apps

| App | Stack | Trilha | Rodar |
|---|---|---|---|
| [`ledger-service/`](ledger-service/) | Java 21 · Spring Boot 3.3 · Postgres | **VPS** — Linux → hardening → Caddy → backups → isolamento → deploy | `cd ledger-service/app && cp .env.example .env && docker compose up --build` |
| [`commerce-api/`](commerce-api/) | Node 22 · Fastify · Postgres | **AWS** — Linux → compose → Terraform → CI → observabilidade → S3/EC2 | `cd commerce-api/app && npm ci && npm run dev` |
| [`webhook-gateway/`](webhook-gateway/) | Node 22 · TypeScript · Redis Streams | **DevSecOps** — pipeline → secrets → SAST → SCA → hardening → gates → DAST → mensageria | `cd webhook-gateway/app && npm ci && npm start` |

Pré-requisitos: Docker (compose), Java 21 + Maven wrapper, Node 22. Redis para o `webhook-gateway`,
Postgres para os outros dois. Detalhes de ambiente, rotas e portas no `README.md` de cada app.

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
