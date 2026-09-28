---
aliases: [issue-10, containers-redis]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: media
---

# Issue 10 — Composição multi-serviço com Redis Streams e gateway de webhooks

## Contexto

**O alvo desta Issue é a stack de produção — `ledger-service/app/docker-compose.yaml` —, não o código deste app.** O `webhook-gateway` é o novo serviço que entra nessa stack; o contrato Redis/Java que aparece nos requisitos abaixo pertence ao `ledger-service`, e é explícito aqui para que ninguém procure `/actuator` no Node.

O sistema é um serviço único com chamada síncrona frágil: se o consumidor cai, o produtor perde o evento. O `ledger-service` já publica em `payment-events` via `RedisPaymentEventPublisher`, mas o serviço Redis não existe na stack e o publisher está desativado por padrão. O consumidor já existe: `webhook-gateway/app/src/consumer.ts` lê a Stream `payment-events` com `ioredis`, cria o consumer group `webhook-dispatcher-group` e assina cada payload com HMAC-SHA256 (`src/signer.ts`, com `crypto.timingSafeEqual`). `src/index.ts` lê a configuração do ambiente e trata `SIGTERM`/`SIGINT`, e a suíte de `tests/` (9 testes, `ioredis` mockado) é o que trava esse contrato.

O que **não** existe neste app ainda: HTTP server, `Dockerfile` e `docker-compose.yaml`. O serviço que esta Issue adiciona à stack depende de imagem e healthcheck, que são trabalho da Issue 02.

## Objetivo

Estado final: Redis na stack como buffer entre produtor e consumidor via Streams, múltiplos serviços orquestrados com isolamento de rede por perfil, e um gateway de webhooks com validação, idempotência e retry — com lag de consumer group observável.

## Dependências

- Requer Issue 02 — imagem e entrypoint próprios deste app, para que o serviço do gateway possa entrar na stack (a composição base com API e banco que esta Issue altera já existe: [Issue 02 do `ledger-service`](../../ledger-service/issues/02-docker-compose.md), concluída)
- Requer a observabilidade que existe no `commerce-api` — os painéis são a base para observar lag de consumer group ([Issue 05 do `commerce-api`](../../commerce-api/issues/05-observability.md))

## Escopo

- Adicionar o serviço Redis à stack e ativar o publisher de eventos
- Orquestrar múltiplos serviços com isolamento de rede por perfil
- Gateway de webhooks com validação, idempotência e retry
- Observabilidade de lag de consumer group

## Fora de escopo

- Publicação da aplicação na internet, TLS e reverse proxy — [Issue 03](../../ledger-service/issues/03-vps-hardening.md) e [Issue 04](../../ledger-service/issues/04-caddy-reverse-proxy.md) do `ledger-service`
- Segmentação final de redes e limites de recursos — [Issue 06 do `ledger-service`](../../ledger-service/issues/06-compose-isolation.md)
- Gates de segurança no CI — Issues 04, 05, 07 e 08
- Kubernetes — fora de escopo por decisão (arquivado no repositório)
- Cloud — [Issue 07 do `commerce-api`](../../commerce-api/issues/07-aws-production.md)

## Conhecimentos envolvidos

- Redis Streams e consumer groups
- Mensageria assíncrona e desacoplamento
- Webhooks confiáveis: validação, idempotência e retry
- Redes de Compose por perfil

## Estado atual

- Serviço único na stack de produção, chamada síncrona frágil
- `ledger-service/app/docker-compose.yaml` não tem serviço Redis
- No `ledger-service`, `REDIS_ENABLED` é `false` por padrão, então `NoOpPaymentEventPublisher` está ativo
- O consumidor deste app já assina e faz `XACK`, mas não há entrega HTTP para fora: nada é despachado como webhook, e não há idempotência nem retry de entrega

## Resultado esperado

- Produtor e consumidor desacoplados por Stream
- Restart do consumidor retoma de onde parou, sem duplicar efeito
- Lag de consumer group visível em tempo real
- Stack com múltiplos serviços em redes separadas por perfil

## Requisitos

**Lado `ledger-service` (stack de produção, `ledger-service/app/docker-compose.yaml`):**

- [ ] Adicionar serviço Redis ao `ledger-service/app/docker-compose.yaml` sem publicar a porta `6379` no host
- [ ] Dar healthcheck ao Redis e ligá-lo à API por `depends_on` com `condition: service_healthy`
- [ ] Manter a API e o Redis na mesma rede interna, onde o DNS do serviço resolve
- [ ] Ativar `REDIS_ENABLED=true` e apontar `SPRING_DATA_REDIS_HOST` para o nome do serviço Redis
- [ ] Preservar o par de implementações complementares: `NoOpPaymentEventPublisher` com `matchIfMissing = true` e `RedisPaymentEventPublisher` com `havingValue = "true"`
- [ ] Preservar o `catch (Exception)` do publisher e a publicação síncrona dentro do `@Transactional`
- [ ] Preservar a chave de Stream `payment-events` e o formato de payload atual (`eventId`, `orderId`, `amount`, `currency`, `status`, `timestamp`)

**Lado `webhook-gateway` (este app):**

- [ ] Orquestrar múltiplos serviços com isolamento de rede por perfil
- [ ] Criar gateway com validação, idempotência e retry
- [ ] Garantir entrega sem perda sob restart do consumidor
- [ ] Expor lag de consumer group em tempo real
- [ ] Preservar o contrato que o consumidor já cumpre: Stream `payment-events`, group `webhook-dispatcher-group`, `XACK` por evento e assinatura HMAC-SHA256 do payload — a suíte `npm test` (9 testes) é o que trava esse contrato
- [ ] Configurar o serviço por ambiente (`REDIS_URL`, `STREAM_KEY`, `GROUP_NAME`, `CONSUMER_NAME`, `WEBHOOK_SECRET`) sem valor real versionado

## Critérios de aceitação

**Lado `ledger-service` (stack de produção):**

- [ ] `docker compose ps` mostra o Redis saudável e a API iniciando depois dele
- [ ] A porta `6379` não aceita conexão a partir de fora do host
- [ ] `/actuator/health` responde `200` com `"status":"UP"` com Redis no ar
- [ ] Um pagamento publica evento na Stream `payment-events` com `status=PROCESSED` e `eventId` iniciando por `evt_`
- [ ] Parar o Redis e repetir o pagamento: a transferência retorna sucesso e o log registra falha de publicação

**Lado `webhook-gateway` (este app):**

- [ ] Reiniciar o consumidor: a entrega retoma sem duplicar efeito
- [ ] Lag do consumer group visível e recuperável após o consumidor voltar
- [ ] `npm test` continua verde (9 testes) — o contrato de Stream, group e assinatura não regrediu

## Validação

- Inspeção do Compose confirmando ausência de `ports:` no Redis e presença de healthcheck
- Requisição a `/actuator/health` com Redis no ar (esperado: 200 `UP`)
- Leitura da Stream confirmando o payload publicado
- **Build to break:** parar o contêiner Redis e executar um pagamento — a transferência deve continuar retornando sucesso, com erro registrado no log
- **Build to defend:** com Redis de volta, o health volta a `UP` e novos eventos fluem sem intervenção manual
- Reinício do consumidor com medição do lag antes e depois
- `npm test` em `webhook-gateway/app/` antes e depois da integração

## Evidências

- `docker compose ps` com Redis e API saudáveis
- Output de `/actuator/health` (200, `UP`) com Redis ativo
- Payload capturado da Stream `payment-events`
- Log do pagamento com Redis parado mostrando a falha capturada e a transferência concluída
- Série de lag do consumer group
- Saída do `npm test` do `webhook-gateway/app/`

## Limitações / notas

- **Contrato de Redis do `ledger-service` (código Java, stack de produção) que não pode ser quebrado:**
  - A propriedade é `redis.enabled` (não `spring.redis.*`), com default `false`; os pares `@ConditionalOnProperty` precisam permanecer complementares, senão o contexto falha por ausência de bean
  - `management.health.redis.enabled` segue o **mesmo** flag: com `REDIS_ENABLED=true` e Redis inalcançável, `/actuator/health` responde **503** e o `healthcheck.sh` da raiz do repo sai com 1 — qualquer gate baseado em saúde, incluindo o rollback da [Issue 07 do `ledger-service`](../../ledger-service/issues/07-cicd-vps-deploy.md), passa a falhar
  - `publishPaymentProcessed` é chamado síncrono dentro de `@Transactional transfer`; mover para depois do commit ou deixar a exceção propagar acopla a disponibilidade do pagamento ao Redis
  - Os nomes de propriedade são `spring.data.redis.host` e `spring.data.redis.port`
- **Contrato deste app que também não pode quebrar:** o consumidor cria o group com `MKSTREAM` e tolera `BUSYGROUP` (`src/consumer.ts`) — trocar isso faz o contêiner não subir em restart; e a verificação de assinatura usa `crypto.timingSafeEqual`, que exige buffer de mesmo tamanho — payloads malformados são tratados, não lançados
- Neste ponto a stack está completa o bastante para a [Issue 06 do `ledger-service`](../../ledger-service/issues/06-compose-isolation.md) segmentar as redes — o Redis precisa entrar na rede interna, nunca numa rede exposta
- O perfil de teste do `ledger-service` mantém `redis.enabled: false`; os testes existentes não podem depender de Redis real
- A suíte deste app (`npm test`, vitest) mocka o `ioredis` por completo — ela prova o contrato de Stream, mas não prova integração com Redis real; a prova de integração é o `docker compose ps` da stack de produção
