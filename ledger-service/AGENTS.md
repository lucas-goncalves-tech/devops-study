# ledger-service — trilha VPS

API de ledger (Spring Boot) que carrega a **trilha VPS** do monorepo:
`linux → hardening → caddy → backups → isolamento → deploy`. Java 21, Postgres, Redis.

Estado final da trilha: serviço endurecido numa VPS real, com entrada TLS única, rede segmentada,
backup off-site provado, deploy por pipeline verde com rollback e tráfego sintético com alerta real.

Antes de qualquer coisa neste app, as regras da casa (escopo de escrita, padrão de container,
skills e roteamento) valem: leia o [`AGENTS.md` da raiz](../AGENTS.md). Metodologia e política de
status → [`00-visao-geral.md`](../00-visao-geral.md). Status e ordem de execução → [`BOARD.md`](../BOARD.md),
seção `ledger-service · trilha VPS`.

## Trilha VPS — 8 Issues

O checkbox do board espelha o `status:` do frontmatter de cada Issue; a tabela abaixo é a mesma
fonte, então uma das duas está desatualizada se elas divergirem.

| # | Issue | Status |
|---|---|---|
| [01](issues/01-linux-runtime.md) | Backend sobe de forma previsível no Linux com healthcheck e shutdown gracioso | `done` |
| [02](issues/02-docker-compose.md) | Imagem enxuta non-root e Compose com banco isolado atrás de healthcheck | `done` |
| [03](issues/03-vps-hardening.md) | VPS endurecida com acesso só por chave, firewall mínimo e ban de brute-force | `todo` |
| [04](issues/04-caddy-reverse-proxy.md) | Entrada única via reverse proxy com TLS automático e headers de segurança | `todo` |
| [05](issues/05-db-backups-s3.md) | Backup off-site com retenção e restore provado | `todo` |
| [06](issues/06-compose-isolation.md) | Compose em redes segmentadas com banco inacessível e limites anti-OOM | `todo` |
| [07](issues/07-cicd-vps-deploy.md) | Deploy contínuo auditável na VPS só com pipeline verde | `todo` |
| [08](issues/08-trafego-sintetico-alertas.md) | Tráfego sintético com k6 agendado e alerta real disparando na stack de produção | `todo` |

**Próxima a entrar: `03`.** Quando for implementá-la, leia `## Dependências` da Issue — a trilha
declara as dependências reais dentro do app, não pela numeração.

## Arquitetura: portas de infraestrutura

O app não conhece Redis nem S3: fala com interfaces, e cada porta tem uma implementação `NoOp`
(padrão, roda sem infra) e uma real, ligada por `@ConditionalOnProperty`. Isso é o que permite a
Issue 02 fechar sem Redis e a Issue 05 plugar S3 sem tocar em regra de negócio.

| Porta | Interface | `NoOp` (padrão) | Real | Ligada por |
|---|---|---|---|---|
| Eventos | `PaymentEventPublisher` | `NoOpPaymentEventPublisher` | `RedisPaymentEventPublisher` | `REDIS_ENABLED=true` |
| Relatórios | `ReportRepository` | `NoOpReportRepository` | `S3ReportRepository` | `S3_ENABLED=true` |

Ligado: `REDIS_ENABLED=true` + `SPRING_DATA_REDIS_HOST=<host>` · `S3_ENABLED=true` +
`S3_BUCKET_NAME=<bucket>` + `AWS_REGION=<região>` + `S3_ENDPOINT_URL=<endpoint>` (LocalStack).

## Variáveis de ambiente

Contrato herdado das Issues 01 e 02 e lido por `application.yml` + `env_file` do Compose.

| Variável | Default | Consumida por |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/securepay_db` | PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | PostgreSQL |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | PostgreSQL |
| `PORT` | `8080` | API e mapeamento de porta do Compose |
| `JWT_SECRET` | embutido | assinatura JWT (256 bits em hex ou base64) |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis |
| `REDIS_ENABLED` | `false` | porta de eventos |
| `S3_ENABLED` | `false` | porta de relatórios |
| `S3_BUCKET_NAME` | `securepay-financial-reports` | S3 de relatórios |
| `AWS_REGION` | `sa-east-1` | região S3 |
| `S3_ENDPOINT_URL` | (vazio) | S3 — só em LocalStack |

## Comandos

Todos a partir de `ledger-service/app/`.

| Comando | Para quê | Concluído quando |
|---|---|---|
| `./mvnw test` | suíte | verde, sem teste ignorado |
| `./mvnw spring-boot:run` | sobe a API contra o Postgres | `/actuator/health` responde 200 `UP` |
| `docker compose up --build` | API + banco pelo Compose | `docker compose ps` mostra `database` `healthy` |
| `docker compose logs -f securepay_api` | log da API | sem stack trace na subida |
| `../../healthcheck.sh` | prova L4+L7 | exit 0 com o serviço no ar, exit 1 com ele parado |
| `../../healthcheck.sh <host> <porta>` | healthcheck de host não local | mesmo contrato |

**Armadilha da Issue 01 que continua valendo:** `.env.example` traz
`SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/...`, porque na Issue 01 o Postgres roda
no host. Dentro do Compose, `localhost` é a própria API — o host do banco passa a ser o nome do
serviço `database`. O procedimento está no [`README.md`](README.md); o mesmo contrato L4/L7 é
herdado por todas as Issues que mexem em runtime.

## Healthcheck: L4 e L7

[`healthcheck.sh`](../healthcheck.sh) prova duas coisas em ordem, e as duas importam:

- **L4** — `bash -c "</dev/tcp/$HOST/$PORT"`: a porta aceita conexão. Não prova que a aplicação
  responde; prova que algo está escutando.
- **L7** — `curl /actuator/health` exigindo HTTP 200 **e** `"status":"UP"`: a aplicação está de pé.
  L4 passando com L7 falhando é o cenário que o L4 sozinho esconderia.

Saída 0 no caso saudável, 1 no caso falho — e é esse código de saída que orquestradores e systemd
consomem. O contrato de porta e `SIGTERM` da Issue 01 é pressuposto de todo o resto da trilha.

## O que ainda não existe aqui

- **Não há `infra/` (Terraform).** A trilha VPS resolve a plataforma com um servidor real e
  hardening de host, não com código de nuvem. Terraform só entra pela trilha AWS, no
  [`commerce-api`](../commerce-api/AGENTS.md), onde a Issue 03 o cria. Não procure um diretório
  `infra/` neste app: procurá-lo é o sinal de que você leu a trilha errada.
- **Sem observabilidade e sem CI próprios até a Issue 07** — `07` cria o workflow de deploy e `08`
  o tráfego sintético com alerta.
- **Sem `docker-compose.yaml` além do de `app/`** — a Issue 06 é a que segmenta as redes e fecha os
  limites anti-OOM no mesmo arquivo.

## Onde o trabalho vive

| Caminho | O que é | Quando abrir |
|---|---|---|
| [`issues/`](issues/) | as 8 Issues da trilha, uma por capacidade, no template fixo | antes de implementar ou fechar qualquer Issue |
| [`estudos/`](estudos/) | material de estudo, um arquivo por Issue | quando precisar do passo a passo; a Issue nunca é a aula |
| [`../BOARD.md`](../BOARD.md) | status e ordem das 3 trilhas | para saber o que está feito e o que entra em seguida |
| [`../00-visao-geral.md`](../00-visao-geral.md) | metodologia, template, política de status | antes de escrever ou fechar uma Issue |

## Escopo de escrita

Gravável aqui: `issues/`, `estudos/`, `AGENTS.md` e `README.md`.

`app/` (código, `pom.xml`, `Dockerfile`, `docker-compose.yaml`), `../healthcheck.sh`,
`../.github/workflows/` e qualquer IaC são **construção do usuário** — proponha o diff e espere
o pedido explícito. Fechar uma Issue exige a saída real do comando de validação colada em
`## Evidências`; limitação de ambiente registrada em `Limitações / notas` não vale como evidência.
