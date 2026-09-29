---
aliases: [estudo-12]
tags: [estudo]
issue: 12
---

# Estudos — Issue 12: Integração na Stack de Produção

> Material de apoio da Issue 12. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Composição multi-app

- Serviços de dois diretórios numa stack só
  - https://docs.docker.com/compose/how-tos/multiple-compose-files/merge/
  - https://docs.docker.com/reference/compose-file/services/
- Ordem de subida e `service_healthy`
  - https://docs.docker.com/reference/compose-file/services/#depends_on

**FIM:** sei dizer quem sube primeiro e o que garante isso.

---

### B — Flag de ativação e saúde

- Propriedade condicional no Spring Boot
  - https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-applications.conditionally
- Comportamento do healthcheck quando a dependência muda
  - https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html

**FIM:** sei prever o que acontece com `/actuator/health` quando `REDIS_ENABLED` liga e o Redis não está.

---

### C — Fronteira entre trilhas

- Dono de cada arquivo na integração
  - `AGENTS.md` da raiz — regra "Trilha ≠ tecnologia" e a exceção
  - `BOARD.md` — seção "Produção e staging"
- Rollback de stack como saída de integração
  - https://docs.docker.com/compose/how-tos/start/services/

**FIM:** sei justificar por que esta é a única Issue com dependência cross-app e o que faria isso virar problema.
