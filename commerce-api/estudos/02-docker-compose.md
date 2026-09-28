---
aliases: [estudo-02]
tags: [estudo]
issue: 02
---

# Estudos — Issue 02: Orquestração com Docker Compose e Postgres Isolado

> Material de apoio da Issue 02. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Imagem

- Multi-stage builds e boas práticas
  - https://docs.docker.com/build/building/multi-stage/
  - https://docs.docker.com/develop/develop-images/dockerfile_best-practices/
  - https://docs.docker.com/develop/develop-images/dockerfile_best-practices/#user
- Node em contêiner: `NODE_ENV=production`, dependências de produção e usuário não-root
  - https://docs.docker.com/samples/nodejs/containerize/
  - https://hub.docker.com/_/node

**FIM:** sei dizer o que engorda uma imagem Node e como provar tamanho e usuário de execução.

---

### B — Compose

- Ordem de subida e healthcheck
  - https://docs.docker.com/compose/how-tos/startup-order/
  - https://docs.docker.com/reference/compose-file/services/#healthcheck
- `env_file`, interpolação e precedência de variáveis
  - https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/
- PostgreSQL em contêiner e persistência de volume
  - https://hub.docker.com/_/postgres
  - https://docs.docker.com/compose/how-tos/volumes/

**FIM:** sei explicar por que dependência cega quebra o pool, e por que banco não entra em `ports:`.
