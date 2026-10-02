---
aliases: [issue-05, observability]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 05 — Coleta de golden signals, dashboards e prova de carga com SLO

## Contexto

A API sobe e responde, mas ninguém enxerga latência, uso de pool nem falha sob concorrência. Sem métricas, "está funcionando" é impressão — e gargalo de banco só aparece em produção, quando já é tarde.

## Objetivo

Estado final: Prometheus coletando de `/metrics` (exposta pelo app com `prom-client`), Grafana com quatro painéis de sinais dourados, e um teste de carga com thresholds que provam ausência de starvation do banco.

## Dependências

- Requer Issue 02 — a stack de API + banco precisa estar orquestrada na mesma rede do coletor

## Escopo

- Orquestração de coletor (`9090`) e dashboard na mesma rede da API
- Scraping de `/metrics` com job dedicado
- Quatro painéis: RPS por status, p95/p99 do endpoint de checkout, pool de conexões do Postgres e memória do processo
- Carga k6 com thresholds de falha e latência

## Fora de escopo

- Jaeger com OpenTelemetry Collector, Chaos Engineering complexo, clusters ELK
- Foco exclusivo: rota `/metrics`, `prometheus.yml`, Grafana (RPS, p95/p99, pool do Postgres) e carga sem starvation

## Conhecimentos envolvidos

- `prom-client` e a exposição de `/metrics` em uma instância Fastify
- Scraping e configuração do Prometheus
- Grafana e PromQL
- k6 e thresholds
- Pool de conexões do driver `postgres` (postgres.js) e leitura das métricas de pool

## Estado atual

- App no escuro, sem métricas expostas: a única rota de introspecção hoje é `/health`
- Dados podem existir, mas ninguém vê gargalo
- Dashboard sem prova de resiliência sob carga

## Resultado esperado

- Métricas fluindo em tempo real do endpoint da aplicação
- Quatro painéis no ar com as métricas nomeadas
- Carga dentro dos thresholds, sem starvation de conexões
- Média baixa não escondendo cauda de latência

## Requisitos

- [ ] Orquestrar coletor em `9090` e dashboard na mesma rede da API, sem colisão de host: a API ocupa `PORT=3000`, então o dashboard precisa de porta própria ou ficar só na rede interna do Compose
- [ ] Registrar o client `prom-client` na instância Fastify, expor `/metrics` (registro padrão + contadores HTTP e do pool) e criar job `commerce-api` com scraping de `5s`
- [ ] Painel de RPS por status (métricas de requisição do app, por código de resposta)
- [ ] Painel de p95/p99 de `/api/v1/orders/checkout`
- [ ] Painel do pool do Postgres (conexões ativas, ociosas e pendentes de espera), expondo o limite real de `max: 10` do `src/db/connection.ts`
- [ ] Painel de memória do processo (`process_resident_memory_bytes` e `process_heap_bytes` do `prom-client`)
- [ ] Carga k6 de 50–100 VUs autenticados (token obtido em `/api/v1/auth/login`) contra `/api/v1/orders/checkout`, com `idempotencyKey` no corpo, sobre produtos semeados de teste com estoque alto — semeadura e limpeza declaradas nesta Issue
- [ ] Validar `http_req_failed < 0.01` e `http_req_duration` p95 abaixo de 500ms, observando `connect_timeout: 10s` e `idle_timeout: 20s` do client `postgres`
- [ ] Definir ao menos uma regra de alerta sobre as métricas coletadas (p95 ou taxa de erro), com PromQL, condição e `for`, ligada a um canal observável e registrar um disparo de teste

## Critérios de aceitação

- [ ] Os quatro painéis existem e recebem as métricas nomeadas acima
- [ ] Carga k6 termina com taxa de falha abaixo de 1% e p95 abaixo de 500ms
- [ ] Nenhuma requisição falha por esgotamento de conexões do pool durante a carga
- [ ] A distribuição de latência mostra cauda visível — não apenas média
- [ ] A regra existe em arquivo com PromQL e `for` visíveis e um disparo real foi observado no canal durante a carga
- [ ] A janela de carga não registra `409` de estoque esgotado e a carga não é reprovada por `http_req_failed` por falta de dados

## Validação

- Requisição a `/metrics` confirmando as métricas esperadas
- Dashboard do coletor mostrando as séries com valor
- Execução do script de carga com os thresholds dentro do esperado
- Inspecionar o painel do pool durante a carga confirmando que a fila de pendentes não cresce sem limite
- Conferir na saída da carga a ausência de `409` e registrar a semeadura e a limpeza dos dados de teste

## Evidências

- Output da execução do teste de carga com os thresholds
- Captura ou exportação dos quatro painéis
- Série de métricas do pool durante o pico de carga
- Registro da latência p95/p99 medida
- Regra de alerta em arquivo e registro do disparo no canal
- Saída da carga sem `409` e registro da semeadura/limpeza

## Limitações / notas

- **Painel aberto só é tolerável no laboratório:** Prometheus (`9090`) e Grafana entram sem autenticação porque a stack é local; em qualquer ambiente alcançável por rede eles exigem credencial ou restrição — registrar a escolha no momento em que a stack sair da máquina local
- **Invariante:** `/metrics` precisa continuar registrada sem `preHandler` de autenticação e o mesmo vale para `/health` — se qualquer uma das duas cair atrás do JWT, o scraping e o `HEALTHCHECK` do `Dockerfile` param de funcionar
- `/health` precisa continuar respondendo `200` com `"status":"UP"` quando o banco está de pé e `503`/`"DEGRADED"` quando não — o `HEALTHCHECK` do `Dockerfile` (linha de comando `wget --spider http://127.0.0.1:3000/health`) só falha por código de saída
- **Contrato equivalente na stack Java:** o `scripts/healthcheck.sh` do `ledger-service` (L4 + L7 em `/actuator/health`, porta `8080`) é daquela trilha e não é reaproveitado — a adaptação para esta app é o `HEALTHCHECK` do `Dockerfile` descrito acima, e o check desta trilha mora em `commerce-api/scripts/healthcheck.sh`
- Os containers de coletor e dashboard entram na stack do Compose e passam a fazer parte da topologia de rede — por isso esta Issue vem antes de qualquer segmentação de rede desta stack, que precisa cobrir todos eles
- O teste de carga precisa de Docker para subir a stack da Issue 02 (API + Postgres); a suíte `npm test` usa `app.inject()` e mocka o serviço, então não é ela que produz prova de carga
