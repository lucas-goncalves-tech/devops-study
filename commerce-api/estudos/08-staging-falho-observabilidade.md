---
aliases: [estudo-08]
tags: [estudo]
issue: 08
---

# Estudos — Issue 08: Staging Falho de Observabilidade

> Material de apoio da Issue 08. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.

### A — Separação de ambientes

- Ambientes e promoção (12-factor)
  - https://12factor.net/config
  - https://12factor.net/disposability
- Ambientes de pré-produção
  - https://sre.google/sre-book/monitoring-distributed-systems/
- Ambientes rotulados e Stackdriver/Cloud Monitoring
  - https://cloud.google.com/monitoring

**FIM:** sei dizer o que separa staging de produção (URL, credencial, dados, nome).

---

### B — Falhas injetadas de forma controlada

- Chaos engineering
  - https://principlesofchaos.org/
- Toxiproxy
  - https://github.com/Shopify/toxiproxy
- Injeção de latência e falha de rede
  - https://www.envoyproxy.io/docs/envoy/latest/
- Liveness, readiness e start probes
  - https://kubernetes.io/docs/concepts/configuration/liveness-readiness-startup-probes/

**FIM:** sei injetar latência, esgotar um pool e reverter a injeção por mecanismo declarado.

---

### C — Métricas de Node, pool e memória

- `prom-client` (métricas do processo Node)
  - https://github.com/siimon/prom-client
- `postgres` (postgres.js) e o pool de conexões
  - https://github.com/porsager/postgres
- Métricas de memória e RSS em Node
  - https://nodejs.org/api/process.html
- Grafana: painéis por sinal e alertas
  - https://grafana.com/docs/grafana/latest/dashboards/
  - https://grafana.com/docs/grafana/latest/alerting/
- Prometheus: histograma e percentis
  - https://prometheus.io/docs/practices/histograms/

**FIM:** sei apontar a métrica que mostra cada uma das três falhas antes de olhar o log.

---

### D — Diagnóstico e conserto com prova

- Postmortem e timeline de incidente
  - https://sre.google/sre-book/postmortem-culture/
- Debug e diagnóstico de performance Node
  - https://nodejs.org/en/learn/getting-started/debugging
  - https://nodejs.org/en/learn/getting-started/debugging
- Teste de regressão que prova a correção
  - https://martinfowler.com/articles/microservices.html

**FIM:** sei escrever sintoma → métrica → causa → correção → reexecução sem cair em tentativa e erro.
