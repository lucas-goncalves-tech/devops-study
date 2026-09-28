---
aliases: [estudo-10]
tags: [estudo]
issue: 10
---

# Estudos — Issue 10: Multi-Service Containers, Redis Streams e Webhook Gateway

> Material de apoio da Issue 10. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Streams

- Redis Streams e consumer groups
  - https://redis.io/docs/latest/develop/data-types/streams/

**FIM:** sei explicar offset e pending.

---

### B — Padrões

- Mensageria assíncrona
  - https://microservices.io/patterns/communication-with-messaging.html
- Webhooks confiáveis
  - https://docs.github.com/en/webhooks/using-webhooks/best-practices-for-using-webhooks
- Neste app o consumidor é `webhook-gateway/app/src/consumer.ts` (`ioredis`): cria o group com `MKSTREAM`, tolera `BUSYGROUP` e faz `XACK` por evento — é o que permite retomar do ponto em que parou após restart
  - https://github.com/redis/ioredis

**FIM:** sei decidir fila vs síncrona.

