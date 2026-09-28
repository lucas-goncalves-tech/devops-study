---
aliases: [estudo-02]
tags: [estudo]
issue: 02
---

# Estudos — Issue 02: Imagem Node Non-Root, Rota de Saúde e Compose com Redis

> Material de apoio da Issue 02. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Imagem

- Multi-stage builds e usuário não-root
  - https://docs.docker.com/build/building/multi-stage/
  - https://docs.docker.com/develop/develop-images/dockerfile_best-practices/
  - https://docs.docker.com/develop/develop-images/dockerfile_best-practices/#user
- Node em contêiner: dependências de produção, `NODE_ENV` e o que não deve entrar no contexto de build
  - https://docs.docker.com/samples/nodejs/containerize/
  - https://hub.docker.com/_/node
- `HEALTHCHECK` em imagem e a dependência dele existir algo que responda
  - https://docs.docker.com/reference/dockerfile/#healthcheck

**FIM:** sei dizer o que engorda uma imagem Node e como provar tamanho e usuário de execução.

---

### B — Compose e Redis

- Ordem de subida, healthcheck de serviço e DNS de serviço
  - https://docs.docker.com/compose/how-tos/startup-order/
  - https://docs.docker.com/reference/compose-file/services/#healthcheck
- `env_file`, interpolação e a diferença entre host local e nome de serviço
  - https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/
- Redis em contêiner, healthcheck por `redis-cli ping` e por que a porta não entra em `ports:`
  - https://hub.docker.com/_/redis
  - https://redis.io/docs/latest/commands/ping/

**FIM:** sei explicar por que o consumidor não sobe antes do Redis, e por que publicar `6379` no host expõe a Stream de eventos.
