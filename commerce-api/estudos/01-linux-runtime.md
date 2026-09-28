---
aliases: [estudo-01]
tags: [estudo]
issue: 01
---

# Estudos — Issue 01: Linux Runtime, Env, Healthcheck e Sinais POSIX

> Material de apoio da Issue 01. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Processos e ambiente

- Sinais POSIX e shutdown gracioso em Node
  - https://man7.org/linux/man-pages/man7/signal.7.html
  - https://nodejs.org/api/process.html#event-uncaughtexception
  - https://fastify.dev/docs/latest/Reference/Server/
- Unidades de serviço, arquivo de ambiente e política de restart
  - https://www.freedesktop.org/software/systemd/man/latest/systemd.service.html
  - https://www.freedesktop.org/software/systemd/man/latest/systemd.exec.html#EnvironmentFile=
- Variáveis de ambiente e validação de schema na inicialização
  - https://www.gnu.org/software/bash/manual/html_node/Environment.html
  - https://zod.dev/

**FIM:** sei explicar SIGTERM vs kill forçado, e por que `ExecStart` não deve ser `npm run dev`.

---

### B — Rede e saúde

- Sockets TCP e portas em uso
  - https://man7.org/linux/man-pages/man8/ss.8.html
- Liveness vs readiness em um servidor HTTP
  - https://kubernetes.io/docs/concepts/configuration/liveness-readiness-startup-probes/
  - https://fastify.dev/docs/latest/Reference/Server/
- PostgreSQL local e readiness
  - https://www.postgresql.org/docs/current/app-pg-isready.html

**FIM:** sei diagnosticar porta ocupada vs app fora do ar, e distinguir liveness de readiness numa rota `/health` que também checa o banco.
