# ledger-service

API de ledger em **Java 21 · Spring Boot · Postgres**. É o app da **trilha VPS** do monorepo:
a jornada vai do serviço rodando no Linux até o serviço endurecido numa VPS real, com entrada TLS
única, rede segmentada, backup off-site provado e deploy por pipeline verde.

## Como rodar

Precisa de Docker + Compose. O banco e a API sobem juntos:

```bash
cd ledger-service/app
cp .env.example .env
# o .env.example aponta para localhost (banco no host); dentro do Compose o banco é o serviço "database"
sed -i 's#^SPRING_DATASOURCE_URL=.*#SPRING_DATASOURCE_URL=jdbc:postgresql://database:5432/securepay_db#' .env
docker compose up --build
```

Em macOS/BSD o `sed -i` precisa do sufixo: `sed -i '' 's#…#…#' .env`.

Conferir: <http://localhost:8080/actuator/health> deve responder `200` com `"status":"UP"`.
O mesmo contrato em script, com prova L4 (porta) e L7 (`/actuator/health`), é o
[`healthcheck.sh`](../healthcheck.sh) na raiz do repositório — exit 0 saudável, exit 1 falho:

```bash
../../healthcheck.sh
```

Sem Docker, o caminho da Issue 01 é Postgres no host + `./mvnw spring-boot:run`, com o
`SPRING_DATASOURCE_URL` do `.env` como está (host `localhost`).

## Tracker

| # | Issue | Status |
|---|---|---|
| [01](issues/01-linux-runtime.md) | Linux runtime, healthcheck L4/L7, `SIGTERM` | `done` |
| [02](issues/02-docker-compose.md) | Imagem non-root + Compose com banco isolado | `done` |
| [03](issues/03-vps-hardening.md) | VPS só por chave, firewall mínimo, ban de brute-force | `todo` |
| [04](issues/04-caddy-reverse-proxy.md) | Reverse proxy com TLS automático | `todo` |
| [05](issues/05-db-backups-s3.md) | Backup off-site com restore provado | `todo` |
| [06](issues/06-compose-isolation.md) | Redes segmentadas e limites anti-OOM | `todo` |
| [07](issues/07-cicd-vps-deploy.md) | Pipeline própria e deploy por pipeline verde, com rollback | `todo` |
| [08](issues/08-trafego-sintetico-alertas.md) | k6 agendado, coleta própria e alerta real | `todo` |

Issue = uma capacidade, escrita como RFC. Não é tutorial: o passo a passo de cada uma está em
[`estudos/`](estudos/) e o status de todas as trilhas no [`BOARD.md`](../BOARD.md).

- [`issues/`](issues/) — as 8 Issues da trilha VPS
- [`estudos/`](estudos/) — material de estudo, um arquivo por Issue
- [`AGENTS.md`](AGENTS.md) — arquitetura, variáveis de ambiente e comandos, para quem vai mexer
