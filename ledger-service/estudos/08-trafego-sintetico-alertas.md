---
aliases: [estudo-08]
tags: [estudo]
issue: 08
---

# Estudos — Issue 08: Tráfego Sintético e Alertas Reais

> Material de apoio da Issue 08. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.

### A — k6

- k6
  - https://grafana.com/oss/k6/
  - https://grafana.com/docs/k6/latest/
- VUs, ritmo e stages
  - https://grafana.com/docs/k6/latest/using-k6/scenarios/
- Thresholds
  - https://grafana.com/docs/k6/latest/using-k6/thresholds/
- Autenticação e requisições
  - https://grafana.com/docs/k6/latest/examples/
  - https://grafana.com/docs/k6/latest/javascript-api/k6-http/
- Neste app o script fala com a stack Java: login em `/api/v1/auth/login`, transação em `/api/v1/payments/transfer` com `X-Idempotency-Key` e sondagem de `/actuator/health`

**FIM:** sei escrever um script com thresholds que reprovam a execução.

---

### B — Métricas, p95 e alertas

- Prometheus: histograma e percentis
  - https://prometheus.io/docs/concepts/metric_types/
  - https://prometheus.io/docs/practices/histograms/
- Alertmanager: regras, `for` e severidade
  - https://prometheus.io/docs/alerting/latest/configuration/
  - https://prometheus.io/docs/practices/alerting/
- Canais de notificação
  - https://prometheus.io/docs/alerting/latest/notifications/
- Grafana Alerting
  - https://grafana.com/docs/grafana/latest/alerting/
- Endpoint de métricas do Spring Boot Actuator
  - https://docs.spring.io/spring-boot/reference/actuator/metrics.html

**FIM:** sei dizer o que dispara um alerta e como o canal recebe o disparo.

---

### C — Agendamento e disciplina de carga

- crontab
  - https://man7.org/linux/man-pages/man5/crontab.5.html
  - https://man7.org/linux/man-pages/man1/systemd-run.1.html
- GitHub Actions: evento `schedule`
  - https://docs.github.com/en/actions/using-workflows/events-that-trigger-workflows
- systemd timers
  - https://man7.org/linux/man-pages/man1/crontab.1.html
- SLIs, SLOs e orçamento de erro
  - https://sre.google/sre-book/service-level-objectives/
  - https://sre.google/workbook/implementing-slos/

**FIM:** sei agendar a execução e declarar a vazão sem derrubar o que está no ar.
