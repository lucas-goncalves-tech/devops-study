---
aliases: [trilha1-02, compose]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 02 — Compose: a stack sobe na ordem certa e o banco fica fechado

## Contexto

A imagem existe (Issue 01), mas a app precisa de Postgres e Redis — e a diferença entre
`docker run` em três terminais e um `compose up` é a diferença entre "eu sei a ordem" e
"o sistema sabe a ordem". Sem dependência declarada por saúde, a API sobe antes do banco
aceitar conexão e morre no Flyway; sem porta fechada, qualquer um na rede fala com o
Postgres — e quando a VM virar VPS pública (Trilha 2+), isso é banco de venda exposto.
A preparação do lab entregou o Actuator justamente para o healthcheck ter **o que**
consultar: agora ele vira o sinal que ordena a subida.

## Objetivo

Estado final: `docker compose up -d` de máquina limpa sobe a stack completa — banco
**healthy primeiro**, API depois (`depends_on: condition: service_healthy`), com
`pg_isready` como probe do banco e o healthcheck da API no `actuator/health`; nenhuma
porta de banco publicada na host (`ss -tlnp` sem `:5432`), e config lida do ambiente
(`env_file`), trocando comportamento sem rebuild.

## Dependências

- **Requer Trilha1-01** — o compose orquestra a imagem `notes-api` construída lá; sem
  `imagem-notes-api` e `healthcheck-no-container` confirmados, não há o que ordenar.
- **pré-condição verificável:** `docker images notes-api` existe e
  `docker inspect ... Healthcheck` não é nulo.

## Escopo

- `compose.yaml` com serviços: `app` (imagem local da Issue 01), `db` (Postgres 15),
  `redis` (Redis 7)
- Healthchecks: `pg_isready` no banco, `redis-cli ping` no Redis, o da API herdado da
  imagem
- `depends_on` com `condition: service_healthy` (API espera os dois deps)
- Portas: **só** a da API publicada (e em loopback `127.0.0.1:` por padrão); `db` e
  `redis` sem seção `ports:`
- `env_file: .env` (o `.env.example` já modela os nomes) — nenhuma credencial hardcoded
- Volume nomeado para `pg_data` + `docker compose down -v` como limpeza explícita
- **assume pronto:** `imagem-notes-api`, `healthcheck-no-container` — da Issue 01
- **entrega:** `compose-ordenado`, `portas-do-banco-fechadas`, `config-por-env`

## Fora de escopo

- Valkey/RabbitMQ (não existem neste app) e qualquer serviço além dos 3
- Reverse proxy, TLS, exposição pública — Issue 03
- Backup do banco (o drill da Trilha 0-05 é de arquivos; `pg_dump` fica para o estágio
  futuro — nenhuma issue do tracker o cobre)
- `docker compose` com `deploy:`/swarm — modo simples apenas

## Conhecimentos envolvidos

- `depends_on` com e sem `condition: service_healthy` — a diferença entre "subiu" e
  "está pronto"
- HEALTHCHECK/`pg_isready`: readiness vs. liveness (conceito da Trilha 0-04 aplicado a
  container)
- Port mapping: publicar na host vs. rede interna do compose
- `env_file`, `.env` e por que credencial não vai no YAML (git é público)
- Redes default do compose: os serviços se resolvem por nome de serviço (`db:5432`)

## Estado atual

- Só existe a imagem `notes-api` (Issue 01); sem composição declarada
- Subir a app exige três terminais e conhecimento da ordem
- `.env.example` existe mas nada o consome (o compose foi removido na preparação)

## Resultado esperado

- `docker compose up -d` → log mostra `db` `healthy` **antes** de `app` `Started`
- `docker compose ps` → os três `healthy`/`running`
- `ss -tlnp | grep 5432` → vazio; `6379` → vazio; só a porta da API (loopback) aparece
- Mudar porta da API no `.env` + `up -d` → novo comportamento **sem** `docker build`
- `docker compose down` → containers e rede removidos; `-v` remove o volume

## Requisitos

- Healthcheck real em cada serviço: `pg_isready -U $POSTGRES_USER`, `redis-cli ping`,
  imagem da API com o `HEALTHCHECK` da Issue 01
- `depends_on.condition: service_healthy` na `app` para `db` **e** `redis`
- `ports` da app como `127.0.0.1:${APP_PORT:-8080}:8080`; **zero** `ports` em `db` e
  `redis`
- Config por `env_file` + `${VAR:-default}` — credencial nunca escrita no compose
- A app conecta nos deps **por nome de serviço** (`db`, `redis`), não `localhost`
  (dentro do container, localhost é ele mesmo)
- `restart: unless-stopped` declarado nos serviços de dados (auto-recuperação —
  conceito do `Restart=on-failure` da Trilha 0-04)

## Critérios de aceitação

- [ ] Pré-condição: `docker images --format '{{.Repository}}' | grep -c '^notes-api$'` →
      `1` **e** `docker inspect --format '{{.Config.Healthcheck}}' notes-api` ≠ `<nil>`
      (Issue 01) — sem os dois, pare aqui
- [ ] `docker compose up -d` → `docker compose ps` → `db`, `redis`, `app` todos
      `healthy`/`Up` (nenhum `starting` ao final)
- [ ] Ordem provada: `docker compose logs app | head` mostra o start da API **depois** do
      banco healthy (ou `docker compose events --timestamp` com o orden cronológico)
- [ ] `ss -tlnp | grep -E ':(5432|6379)\b'` → **vazio** na host; só a porta da app
      escuta, e em `127.0.0.1`
- [ ] `docker compose exec db pg_isready` → `accepting connections`; app responde
      `200` no `actuator/health` com `"db":{"status":"UP"}`
- [ ] Parar o banco (`docker compose stop db`) → `docker inspect` do health da `app` vira
      `unhealthy` em ≤ intervalo do healthcheck; `start db` → volta a `healthy`
- [ ] Trocar `APP_PORT` (ou a porta exposta) no `.env` + `docker compose up -d` →
      resposta na nova porta **sem** nenhum `docker build` (mesmo digest de imagem)
- [ ] `grep -iE 'password|secret' compose.yaml` → só `${...}` referenciado, nenhum valor
      literal
- [ ] `docker compose down -v --remove-orphans` → `docker volume ls` sem `pg_data`
      órfão

## Validação

- Limpar antes: `docker compose down -v` (se existir sujeira) → `up -d --build` não — a
  imagem é local, é `up -d` puro
- `docker compose ps` → tabela com os três `healthy`; `docker compose logs -t db | tail`
  vs. `logs -t app | head` → timestamps do db anteriores aos da app
- Portas: no host, `ss -tlnp | grep -E ':(5432|6379)\b'` → vazio; `curl` na porta da app
  → `200`
- Resiliência: `docker compose stop db` → espera o intervalo →
  `docker inspect --format '{{.State.Health.Status}}' <container da app>` → `unhealthy`;
  `start db` → volta a `healthy`
- Env: `APP_PORT=8090 docker compose up -d` → `curl 127.0.0.1:8090/api/v1/actuator/health`
  → `200`
- Reset: `docker compose down -v` → `docker volume ls | grep pg_data` → vazio

## Evidências

- `docker compose ps` com os três serviços healthy
- Trecho de log provando a ordem (db healthy antes do start da app)
- `ss -tlnp` do host sem `5432`/`6379`
- O par `unhealthy`/`healthy` da app com o banco parado/ligado
- Troca de porta por env com `docker image inspect` (digest inalterado)

## Limitações / notas

- `127.0.0.1:` na porta da app é o default daqui — a Issue 03 muda isso **atrás** do
  reverse proxy (o proxy fala com a app na rede interna; o mundo fala com o proxy)
- `condition: service_healthy` protege a **subida**, não a queda: se o banco cair no
  meio, a app não reinicia sozinha — ela fica `unhealthy` (evidência, não ação). Auto-
  recuperação de falha em runtime é assunto da Trilha 3 (monitoramento/alerta)
- `.env` está no `.gitignore`; o repo só leva `.env.example`. Credencial real nunca entra
  no git — mesmo sendo de laboratório (o contrato de segredos é estágio futuro)
- `pg_data` em volume nomeado sobrevive a `down` sem `-v` — é recurso, e é a armadilha:
  restore/estado antigo persiste silenciosamente. A Issue 05 da Trilha 0 já ensinou o
  drill; aqui o `down -v` é a limpeza explícita
