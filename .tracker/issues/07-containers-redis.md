---
aliases: [issue-07, containers-redis]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: media
---

# Issue 07 — Composição multi-serviço com Redis Streams e gateway de webhooks

## Contexto

O sistema é um serviço único com chamada síncrona frágil: se o consumidor cai, o produtor perde o evento. O backend já publica em `payment-events` via `RedisPaymentEventPublisher`, mas o serviço Redis não existe na stack e o publisher está desativado por padrão.

## Objetivo

Estado final: Redis na stack como buffer entre produtor e consumidor via Streams, múltiplos serviços orquestrados com isolamento de rede por perfil, e um gateway de webhooks com validação, idempotência e retry — com lag de consumer group observável.

## Dependências

- Requer Issue 02 — composição base com API e banco
- Requer Issue 06 — os painéis de métricas são a base para observar lag de consumer group

## Escopo

- Adicionar o serviço Redis à stack e ativar o publisher de eventos
- Orquestrar múltiplos serviços com isolamento de rede por perfil
- Gateway de webhooks com validação, idempotência e retry
- Observabilidade de lag de consumer group

## Fora de escopo

- Publicação da aplicação na internet, TLS e reverse proxy — Issues 04 e 05
- Segmentação final de redes e limites de recursos — Issue 08
- Gates de segurança no CI — Issues 13–16
- Kubernetes e Cloud — Issues 18 e 12

## Conhecimentos envolvidos

- Redis Streams e consumer groups
- Mensageria assíncrona e desacoplamento
- Webhooks confiáveis: validação, idempotência e retry
- Redes de Compose por perfil

## Estado atual

- Serviço único, chamada síncrona frágil
- `backend/docker-compose.yaml` não tem serviço Redis
- `REDIS_ENABLED` é `false` por padrão, então `NoOpPaymentEventPublisher` está ativo
- Webhooks sem validação nem retry

## Resultado esperado

- Produtor e consumidor desacoplados por Stream
- Restart do consumidor retoma de onde parou, sem duplicar efeito
- Lag de consumer group visível em tempo real
- Stack com múltiplos serviços em redes separadas por perfil

## Requisitos

- [ ] Adicionar serviço Redis ao `backend/docker-compose.yaml` sem publicar a porta `6379` no host
- [ ] Dar healthcheck ao Redis e ligá-lo à API por `depends_on` com `condition: service_healthy`
- [ ] Manter a API e o Redis na mesma rede interna, onde o DNS do serviço resolve
- [ ] Ativar `REDIS_ENABLED=true` e apontar `SPRING_DATA_REDIS_HOST` para o nome do serviço Redis
- [ ] Preservar o par de implementações complementares: `NoOpPaymentEventPublisher` com `matchIfMissing = true` e `RedisPaymentEventPublisher` com `havingValue = "true"`
- [ ] Preservar o `catch (Exception)` do publisher e a publicação síncrona dentro do `@Transactional`
- [ ] Preservar a chave de Stream `payment-events` e o formato de payload atual
- [ ] Orquestrar múltiplos serviços com isolamento de rede por perfil
- [ ] Criar gateway com validação, idempotência e retry
- [ ] Garantir entrega sem perda sob restart do consumidor
- [ ] Expor lag de consumer group em tempo real

## Critérios de aceitação

- [ ] `docker compose ps` mostra o Redis saudável e a API iniciando depois dele
- [ ] A porta `6379` não aceita conexão a partir de fora do host
- [ ] `/actuator/health` responde `200` com `"status":"UP"` com Redis no ar
- [ ] Um pagamento publica evento na Stream `payment-events` com `status=PROCESSED` e `eventId` iniciando por `evt_`
- [ ] Parar o Redis e repetir o pagamento: a transferência retorna sucesso e o log registra falha de publicação
- [ ] Reiniciar o consumidor: a entrega retoma sem duplicar efeito
- [ ] Lag do consumer group visível e recuperável após o consumidor voltar

## Validação

- Inspeção do Compose confirmando ausência de `ports:` no Redis e presença de healthcheck
- Requisição a `/actuator/health` com Redis no ar (esperado: 200 `UP`)
- Leitura da Stream confirmando o payload publicado
- **Build to break:** parar o contêiner Redis e executar um pagamento — a transferência deve continuar retornando sucesso, com erro registrado no log
- **Build to defend:** com Redis de volta, o health volta a `UP` e novos eventos fluem sem intervenção manual
- Reinício do consumidor com medição do lag antes e depois

## Evidências

- `docker compose ps` com Redis e API saudáveis
- Output de `/actuator/health` (200, `UP`) com Redis ativo
- Payload capturado da Stream `payment-events`
- Log do pagamento com Redis parado mostrando a falha capturada e a transferência concluída
- Série de lag do consumer group

## Limitações / notas

- **Contrato de Redis que não pode ser quebrado:**
  - A propriedade é `redis.enabled` (não `spring.redis.*`), com default `false`; os pares `@ConditionalOnProperty` precisam permanecer complementares, senão o contexto falha por ausência de bean
  - `management.health.redis.enabled` segue o **mesmo** flag: com `REDIS_ENABLED=true` e Redis inalcançável, `/actuator/health` responde **503** e `healthcheck.sh` sai com 1 — qualquer gate baseado em saúde, incluindo o rollback da Issue 17, passa a falhar
  - `publishPaymentProcessed` é chamado síncrono dentro de `@Transactional transfer`; mover para depois do commit ou deixar a exceção propagar acopla a disponibilidade do pagamento ao Redis
  - Os nomes de propriedade são `spring.data.redis.host` e `spring.data.redis.port`
- Neste ponto a stack está completa o bastante para a Issue 08 segmentar as redes — o Redis precisa entrar na rede interna, nunca numa rede exposta
- O perfil de teste mantém `redis.enabled: false`; os testes existentes não podem depender de Redis real
