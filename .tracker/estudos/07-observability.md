---
aliases: [estudo-07]
tags: [estudo]
issue: 07
---

# Estudos — Issue 07: Observabilidade, Golden Signals e Teste de Carga

> Material de apoio da Issue 07. Não é escopo da Issue — a `teach-devops` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Métricas

- Actuator + Micrometer
  - https://docs.spring.io/spring-boot/reference/actuator/metrics.html
  - https://micrometer.io/docs
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
- HikariCP
  - https://github.com/brettwooldridge/HikariCP

**FIM:** sei apontar gargalo (GC, threads, contenção, pool) no gráfico.

