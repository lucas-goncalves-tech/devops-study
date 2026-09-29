---
aliases: [issue-10, containers-redis]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: media
---

# Issue 10 — Composição multi-serviço com Redis Streams e gateway de webhooks

## Contexto

Este app é um consumidor sozinho: lê a Stream `payment-events`, cria o consumer group, assina cada payload com HMAC-SHA256 e dá `XACK` — mas não entrega nada para fora, não tem produtor nenhum na sua própria stack para consumir, e nunca foi montado como sistema multi-serviço com isolamento de rede. Sem produtor na stack, não existe como provocar a perda que esta Issue existe para provar: "evento não se perde quando o consumidor cai" é um comportamento deste app, e a prova tem de nascer aqui — com o que este app orquestra, não com stack, painel ou serviço de outro app.

## Objetivo

Estado final: stack multi-serviço deste app com Redis como buffer entre produtor e consumidor via Streams, redes separadas por perfil, um gateway de webhooks com validação, idempotência e retry, produtor sintético versionado que alimenta a Stream, e lag de consumer group observável por mecanismo declarado nesta Issue — tudo subindo a partir de `webhook-gateway/` e sem depender de Issue, arquivo ou serviço de outro app.

## Dependências

- Requer Issue 01 — o contrato de arquivo de ambiente, a política de restart e o fechamento sob `SIGTERM` que os serviços desta stack herdam
- Requer Issue 02 — a imagem, o `HEALTHCHECK` e o próprio Redis que formam a base desta stack

## Escopo

- Produtor sintético versionado publicando eventos na Stream da stack
- Redis como buffer entre produtor e consumidor via Streams
- Orquestração de múltiplos serviços com isolamento de rede por perfil
- Gateway de webhooks com validação, idempotência e retry
- Observabilidade de lag de consumer group por mecanismo próprio

## Fora de escopo

- Entrada do gateway na stack de produção do `ledger-service` e ativação do publisher real — [Issue 12](12-integracao-producao.md) desta trilha; esta Issue não sai da stack própria
- Publicação na internet, TLS e reverse proxy — fora desta Issue
- Gates de segurança no CI — Issues 04, 05, 07 e 08
- Kubernetes — fora de escopo por decisão (arquivado no repositório)
- Cloud e provisionamento — trilha AWS, fora desta Issue

## Conhecimentos envolvidos

- Redis Streams e consumer groups
- Mensageria assíncrona e desacoplamento
- Webhooks confiáveis: validação, idempotência e retry
- Redes de Compose por perfil
- Lag de consumer group: `XINFO GROUPS` e `XPENDING` como observação sem painel externo

## Estado atual

- O consumidor deste app assina e faz `XACK`, mas não há entrega HTTP para fora: nada é despachado como webhook, e não há idempotência nem retry de entrega
- A stack deste app tem consumidor e Redis, e nenhum produtor: nada publica eventos dentro dela
- Sem produtor na stack não há como acumular evento pendente e provar a retomada
- Nenhum mecanismo de lag está declarado neste app
- A integração com a stack de produção não existe e não é escopo desta Issue

## Resultado esperado

- Produtor e consumidor desacoplados por Stream dentro da stack deste app
- Reinício do consumidor retoma de onde parou, sem duplicar efeito
- Destino de entrega indisponível não custa evento: retry com backoff declarado
- Lag de consumer group visível antes e depois do consumidor voltar
- Stack com múltiplos serviços em redes separadas por perfil

## Requisitos

**Stack deste app (`webhook-gateway/`):**

- [ ] Criar produtor sintético versionado que publica na Stream `payment-events` no formato que a suíte deste app trava (`eventId`, `orderId`, `amount`, `currency`, `status`, `timestamp`), configurado por ambiente e sem credencial real versionada
- [ ] Orquestrar os serviços da stack com redes separadas por perfil, com consumidor e Redis na rede interna
- [ ] Manter o Redis sem porta publicada no host e com healthcheck, conforme a base declarada na Issue 02
- [ ] Criar gateway de entrega com validação de payload, chave de idempotência e retry com backoff declarado e limite de tentativas
- [ ] Garantir entrega sem perda sob reinício do consumidor
- [ ] Expor o lag de consumer group por mecanismo declarado nesta Issue (saída de comando versionado ou leitura do próprio serviço) — legível antes e depois de uma queda
- [ ] Preservar o contrato que o consumidor já cumpre: Stream `payment-events`, group `webhook-dispatcher-group`, criação com `MKSTREAM`, tolerância a `BUSYGROUP`, `XACK` por evento e assinatura HMAC-SHA256 com `crypto.timingSafeEqual` — a suíte `npm test` (9 testes) é o que trava esse contrato
- [ ] Configurar os serviços por ambiente (`REDIS_URL`, `STREAM_KEY`, `GROUP_NAME`, `CONSUMER_NAME`, `WEBHOOK_SECRET`) sem valor real versionado

## Critérios de aceitação

- [ ] A stack sobe inteira a partir de `webhook-gateway/`, sem arquivo, serviço ou Issue de outro app
- [ ] Parar o consumidor com eventos pendentes e reiniciá-lo: todos os eventos são entregues e nenhum efeito se repete
- [ ] Payload repetido não gera segunda entrega — a chave de idempotência segura o duplicado
- [ ] Destino de entrega fora do ar: o retry com backoff aparece no log e nenhum evento é descartado silenciosamente
- [ ] O lag do consumer group é lido antes da queda e depois da volta, com a diferença registrada
- [ ] A porta `6379` não aceita conexão a partir de fora do host
- [ ] `npm test` continua verde (9 testes) — o contrato de Stream, group e assinatura não regrediu

## Validação

- Subir a stack a partir do diretório deste app e inspecionar `docker compose ps`
- **Build to break:** parar o consumidor, publicar eventos pelo produtor sintético, reiniciar e observar a retomada sem duplicação
- Publicar o mesmo payload duas vezes e conferir uma única entrega
- Derrubar o destino de entrega e observar o retry com backoff no log
- Ler o lag do consumer group antes da queda e depois da volta
- Inspeção do Compose confirmando ausência de `ports:` no Redis, healthcheck e redes por perfil
- `npm test` em `webhook-gateway/app/` antes e depois da integração

## Evidências

- `docker compose ps` com serviços da stack própria saudáveis
- Log do reinício do consumidor com a retomada e a ausência de entrega duplicada
- Log do payload repetido entregue uma única vez
- Log do retry com backoff contra destino indisponível, com os eventos retidos
- Leitura do lag do consumer group antes e depois da queda
- Saída do `npm test` do `webhook-gateway/app/`

## Limitações / notas

- **Formato de payload herdado do contrato do sistema, não de outro app:** `eventId`, `orderId`, `amount`, `currency`, `status` e `timestamp` são o formato que a suíte deste app já trava. O produtor sintético fala esse formato aqui; quando a integração de produção existir, os dois lados já se entendem
- **Contrato do consumidor que não pode quebrar:** o group é criado com `MKSTREAM` e tolera `BUSYGROUP` (`src/consumer.ts`) — trocar isso faz o contêiner não subir em restart; e a verificação de assinatura usa `crypto.timingSafeEqual`, que exige buffer de mesmo tamanho — payload malformado é tratado, não lançado
- **Integração de produção é a Issue 12, escrita à parte:** a entrada do gateway no compose do `ledger-service`, a ativação de `REDIS_ENABLED=true` e o Redis na stack real estão fora desta Issue e são a [Issue 12](12-integracao-producao.md). Os contratos Java que aquela Issue honra estão documentados no `AGENTS.md` do `ledger-service` (propriedade `redis.enabled` com default `false`, pares `@ConditionalOnProperty` complementares, `spring.data.redis.*`, e publicação síncrona dentro de `@Transactional`)
- O perfil de teste deste app não pode depender de Redis real, e a suíte `npm test` mocka o `ioredis` por completo: ela prova o contrato de Stream, não a integração — a prova de integração é a stack desta Issue
- Comprovar "sem perda" exige acumular evento pendente de propósito: parar o consumidor antes de publicar é a única forma honesta de provocar o cenário, e limpar a Stream depois da prova faz parte da validação
