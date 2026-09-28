---
aliases: [estudo-01]
tags: [estudo]
issue: 01
---

# Estudos — Issue 01: Linux Runtime, Processo de Longa Duração e Shutdown Gracioso

> Material de apoio da Issue 01. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Processo e ambiente

- Sinais POSIX e shutdown gracioso em Node
  - https://man7.org/linux/man-pages/man7/signal.7.html
  - https://nodejs.org/api/process.html#signal-events
- Laço de leitura bloqueante e por que ele atrasa a saída
  - https://redis.io/docs/latest/develop/data-types/streams/
  - https://github.com/redis/ioredis
- Unidades de serviço, arquivo de ambiente e política de restart
  - https://www.freedesktop.org/software/systemd/man/latest/systemd.service.html
  - https://www.freedesktop.org/software/systemd/man/latest/systemd.exec.html#EnvironmentFile=
- Variáveis de ambiente e defaults em código
  - https://www.gnu.org/software/bash/manual/html_node/Environment.html

**FIM:** sei explicar SIGTERM vs kill forçado, e por que um laço com `BLOCK` precisa de janela de parada.

---

### B — Verificação de processo

- Estado de uma unidade: `active`, `MainPID` e o que cada um prova
  - https://www.freedesktop.org/software/systemd/man/latest/systemctl.html
- Sockets TCP e portas em uso, para distinguir processo vivo de conexão com o Redis
  - https://man7.org/linux/man-pages/man8/ss.8.html
- Ciclo de vida do cliente Redis: `PING`, `QUIT` e `disconnect`
  - https://redis.io/docs/latest/commands/ping/
  - https://redis.io/docs/latest/commands/quit/
  - https://github.com/redis/ioredis

**FIM:** sei diagnosticar "processo no ar mas sem Redis" de "processo morto", e dizer o que cada verificação prova e o que não prova.
