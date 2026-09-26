---
aliases: [estudo-01]
tags: [estudo]
issue: 01
---

# Estudos — Issue 01: Linux Runtime, Env, Healthcheck e Sinais POSIX

> Material de apoio da Issue 01. Não é escopo da Issue — a `teach-devops` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Processos e ambiente

- Sinais POSIX e variáveis de ambiente
  - https://man7.org/linux/man-pages/man7/signal.7.html
  - https://www.gnu.org/software/bash/manual/html_node/Environment.html

**FIM:** sei explicar SIGTERM vs kill forçado.

---

### B — Rede e saúde

- Sockets TCP e portas em uso
  - https://man7.org/linux/man-pages/man8/ss.8.html
- Health indicators do Spring Boot Actuator
  - https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- PostgreSQL local e readiness
  - https://www.postgresql.org/docs/current/app-pg-isready.html

**FIM:** sei diagnosticar porta ocupada vs app fora do ar.

