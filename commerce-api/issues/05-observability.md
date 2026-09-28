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

Estado final: Prometheus coletando do `/actuator/prometheus`, Grafana com quatro painéis de sinais dourados, e um teste de carga com thresholds que provam ausência de starvation do banco.

## Dependências

- Requer Issue 02 — a stack de API + banco precisa estar orquestrada na mesma rede do coletor

## Escopo

- Orquestração de coletor (`9090`) e dashboard (`3000`) na mesma rede da API
- Scraping de `/actuator/prometheus` com job dedicado
- Quatro painéis: RPS por status, p95/p99 do endpoint de transferência, HikariCP e heap JVM
- Carga k6 com thresholds de falha e latência

## Fora de escopo

- Jaeger com OpenTelemetry Collector, Chaos Engineering complexo, clusters ELK
- Foco exclusivo: scraping `/actuator/prometheus`, `prometheus.yml`, Grafana (RPS, p95/p99, HikariCP) e carga sem starvation

## Conhecimentos envolvidos

- Actuator e Micrometer
- Scraping e configuração do Prometheus
- Grafana e PromQL
- k6 e thresholds
- Pool de conexões HikariCP

## Estado atual

- App no escuro, sem métricas expostas
- Dados podem existir, mas ninguém vê gargalo
- Dashboard sem prova de resiliência sob carga

## Resultado esperado

- Métricas fluindo em tempo real do endpoint da aplicação
- Quatro painéis no ar com as métricas nomeadas
- Carga dentro dos thresholds, sem starvation de conexões
- Média baixa não escondendo cauda de latência

## Requisitos

- [ ] Orquestrar coletor em `9090` e dashboard em `3000` na mesma rede da API
- [ ] Expor `/actuator/prometheus` e criar job `ledger-service` com scraping de `5s`
- [ ] Painel de RPS por status (`http_server_requests_seconds_count`)
- [ ] Painel de p95/p99 de `/api/v1/payments/transfer`
- [ ] Painel de HikariCP (`hikaricp_connections_active`, `_idle`, `_pending`)
- [ ] Painel de heap JVM (`jvm_memory_used_bytes{area="heap"}`)
- [ ] Carga k6 de 50–100 VUs contra `/api/v1/payments/transfer` com `X-Idempotency-Key`
- [ ] Validar `http_req_failed < 0.01` e `http_req_duration` p95 abaixo de 500ms, observando `connection-timeout: 20s`

## Critérios de aceitação

- [ ] Os quatro painéis existem e recebem as métricas nomeadas acima
- [ ] Carga k6 termina com taxa de falha abaixo de 1% e p95 abaixo de 500ms
- [ ] Nenhuma requisição falha por esgotamento de conexões do pool durante a carga
- [ ] A distribuição de latência mostra cauda visível — não apenas média

## Validação

- Requisição ao `/actuator/prometheus` confirmando as métricas esperadas
- Dashboard do coletor mostrando as séries com valor
- Execução do script de carga com os thresholds dentro do esperado
- Inspecionar o painel de HikariCP durante a carga confirmando que `pending` não cresce sem limite

## Evidências

- Output da execução do teste de carga com os thresholds
- Captura ou exportação dos quatro painéis
- Série de métricas do pool durante o pico de carga
- Registro da latência p95/p99 medida

## Limitações / notas

- **Invariante:** `management.endpoints.web.exposure.include` precisa continuar contendo `prometheus` e `health`; `/actuator/**` precisa continuar `permitAll` — senão o scraping e o healthcheck param de funcionar
- `show-details: always` e `probes.enabled: true` precisam permanecer ligados: `healthcheck.sh` faz grep literal em `"status":"UP"`
- Os containers de coletor e dashboard entram na stack do Compose e passam a fazer parte da topologia de rede — por isso esta Issue vem antes de qualquer segmentação de rede da stack (ver [Issue 06 do `ledger-service`](../../ledger-service/issues/06-compose-isolation.md)), que precisa cobrir todos eles
- O teste de carga depende de Docker disponível para os testes de contêiner do projeto
