---
aliases: [estudo-05]
tags: [estudo]
issue: 05
---

# Estudos — Issue 05: Observabilidade, Golden Signals e Teste de Carga

> Material de apoio da Issue 05. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Métricas

- `prom-client` e `/metrics` em Fastify
  - https://github.com/siimon/prom-client
  - https://fastify.dev/docs/latest/Reference/Server/
- Scraping Prometheus
  - https://prometheus.io/docs/prometheus/latest/configuration/configuration/

**FIM:** sei explicar o que cada métrica mede.

---

### B — Visualização e carga

- Grafana e PromQL
  - https://grafana.com/docs/grafana/latest/dashboards/
  - https://prometheus.io/docs/prometheus/latest/querying/basics/
- k6 e thresholds
  - https://grafana.com/docs/k6/
- Pool de conexões do driver `postgres` (postgres.js) e suas métricas
  - https://github.com/porsager/postgres
  - https://github.com/siimon/prom-client

**FIM:** sei apontar gargalo (GC, threads, contenção, pool) no gráfico.

