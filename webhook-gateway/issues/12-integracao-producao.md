---
aliases: [issue-12, integracao-producao]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 12 — Entrada de `redis` e do gateway na stack de produção

## Contexto

A Issue 10 provou o comportamento na stack deste app: produtor sintético publica, Redis buffera, o consumidor retoma sem duplicar e o gateway entrega assinado. Nada disso existe em produção. A stack real da VPS roda `ledger + postgres` com o publisher desligado (`REDIS_ENABLED=false`, default): a Stream nunca é criada, nenhum evento é publicado e nenhum webhook é despachado — o sistema em produção continua sendo a chamada síncrona frágil que este app existe para substituir.

Esta é a única Issue do repositório autorizada a tocar arquivo de outro app. A exceção está registrada na doutrina (`AGENTS.md` da raiz, `BOARD.md`): as trilhas são autocontidas, e esta integração é a fronteira única onde elas se encontram.

## Objetivo

Estado final: `redis` e `webhook-gateway` rodam dentro da stack de produção da VPS, o publisher do `ledger-service` publica de verdade, e um pagamento criado na API de produção chega assinado no destino — fluxo ponta a ponta com evidência nas três pontas, sobrevivendo a reinício de consumidor e de máquina.

## Dependências

- Requer Issue 02 deste app — a imagem e o contrato de ambiente que a produção consome
- Requer Issue 10 deste app — composição, consumer group e entrega já provados na stack própria
- Requer [Issue 02 do `ledger-service`](../../ledger-service/issues/02-docker-compose.md) — o `docker-compose.yaml` que esta Issue estende **(dependência cross-app: esta é a única permitida no repo)**
- Requer [Issue 06 do `ledger-service`](../../ledger-service/issues/06-compose-isolation.md) — a topologia de rede isolada onde os serviços entram
- Requer [Issue 07 do `ledger-service`](../../ledger-service/issues/07-cicd-vps-deploy.md) — a via de deploy que publica a stack atualizada na VPS

## Escopo

- Serviço `redis` na stack de produção: healthcheck, sem porta publicada, rede interna
- Serviço `webhook-gateway` na mesma stack, na rede interna, consumindo esse `redis`
- Ativação do publisher real do `ledger-service` (`REDIS_ENABLED=true`) sem quebrar a saúde da aplicação
- Caminho declarado da imagem do gateway até a produção, sem construção manual na máquina
- Configuração por ambiente, sem segredo versionado
- Prova ponta a ponta em produção, com reinício de consumidor e de máquina

## Fora de escopo

- Capacidade do consumidor (redes, idempotência, retry, lag) — Issue 10, provada na stack própria
- TLS, reverse proxy e portas públicas — Issues 03 e 04 do `ledger-service`
- Eventos do `commerce-api` — integração ainda sem card, registrada no `BOARD.md`
- Alteração de código dos dois apps — os contratos já existem nos dois lados
- Build e gates do pipeline — Issues 03 a 09 deste app; deploy da stack — Issue 07 do `ledger-service`

## Conhecimentos envolvidos

- Composição multi-app: serviços de dois repositórios numa stack só, com rede e healthcheck
- Ativação por variável de ambiente e o que muda na saúde do serviço quando a flag liga
- Ordem de subida e `depends_on` com `service_healthy` como garantia de inicialização
- Produção como sistema único: quem é dono de quê na fronteira entre trilhas
- Rollback de stack: reverter o compose é reverter a integração

## Estado atual

- A produção da VPS é `ledger + postgres` atrás do Caddy — sem `redis`, sem `webhook-gateway`
- O `RedisPaymentEventPublisher` existe no código, mas `REDIS_ENABLED=false` o mantém desativado
- A Stream `payment-events` nunca foi criada em produção; nenhum evento foi publicado
- Nenhum webhook foi despachado em produção
- A stack própria deste app (Issue 10) e a stack de produção não se encontram

## Resultado esperado

```text
pagamento na API → ledger publica na Stream → consumidor lê → gateway entrega assinado
                                                              ├─ 2xx  → evento concluído
                                                              └─ falha → retry (Issue 10)
```

- `redis` e `webhook-gateway` sobem com a stack da VPS, sem passo manual
- Reinício de consumidor: entrega retoma sem duplicar; reinício de máquina: stack volta completa
- Com o publisher ligado, `/actuator/health` do `ledger` segue `UP` — o rollback da Issue 07 não dispara

## Requisitos

- [ ] Serviço `redis` na stack do `ledger-service`, com healthcheck, sem porta publicada, em rede interna com a aplicação e autenticação por `senha` injetada via ambiente — `REDIS_URL` do gateway com a credencial e `SPRING_DATA_REDIS_PASSWORD` no ambiente do `ledger`
- [ ] Serviço `webhook-gateway` na mesma stack e rede, apontando para o `redis` por nome de serviço
- [ ] `REDIS_ENABLED=true` no ambiente da aplicação, com o healthcheck do `redis` condicionando a subida da aplicação
- [ ] Caminho da imagem do gateway até a produção declarado nesta Issue — build no deploy a partir do `Dockerfile` do app ou imagem publicada pelo pipeline —, sem construção manual na VPS
- [ ] Configuração do gateway (`REDIS_URL`, `WEBHOOK_SECRET`, destino de entrega) por ambiente, fora do repositório — o destino de entrega com `variável própria` declarada nesta Issue e `https` obrigatório em produção
- [ ] `WEBHOOK_SECRET` compartilhado com o `receptor` do destino pelo mesmo canal de ambiente (fora do repositório), sustentando a verificação de HMAC do critério das três pontas
- [ ] Gateway sem porta publicada: ele só fala como cliente, para o `redis` e para fora
- [ ] Os arquivos alterados fora de `webhook-gateway/` são apenas os declarados em Escopo

## Critérios de aceitação

- [ ] Pagamento criado na API do `ledger` em produção aparece na Stream, é consumido pelo grupo `webhook-dispatcher-group` e despachado com HMAC — evidência nas três pontas
- [ ] Parar o gateway com eventos pendentes e subir: a entrega retoma sem duplicar efeito
- [ ] Reiniciar a máquina inteira: a stack volta completa sem intervenção manual
- [ ] Com o publisher ativo, `/actuator/health` responde `200` `UP` e o rollback da Issue 07 do `ledger` não é acionado
- [ ] A porta `6379` não aceita conexão de fora do host e o gateway não publica porta nenhuma
- [ ] Nenhum segredo em arquivo versionado dos dois apps
- [ ] Esta é a única Issue do repo com `Requer` apontando para outro app — varredura no tracker confirma que nenhuma outra ganhou dependência cross-app

- [ ] Conexão ao `redis` de produção **sem credencial é recusada** (build to break), com a porta `6379` seguindo inalcançável de fora do host

## Validação

- Criar um pagamento pela API de produção e acompanhar as três pontas: publicação, consumo e entrega
- **Build to break:** parar o gateway com eventos pendentes, reiniciar e observar a retomada; depois reiniciar a máquina e observar a stack voltar sozinha
- Desligar o `redis` no meio do tráfego e observar o comportamento do healthcheck da aplicação antes de reestabelecer
- Inspecionar o Compose confirmando ausência de `ports:` em `redis` e no gateway
- Rodar o healthcheck da trilha VPS com o sistema completo de pé (esperado: exit 0)
- Varredura dos dois apps por credencial versionada

## Evidências

- Log das três pontas no mesmo intervalo: publicação pelo `ledger`, leitura pelo grupo e entrega assinada
- `docker compose ps` na VPS com `ledger`, `database`, `redis` e `webhook-gateway` saudáveis
- Log de parada e retomada do gateway sem entrega duplicada
- Saída do healthcheck da trilha VPS com o publisher ativo (exit 0)
- Saída da inspeção do Compose sem `ports:` nos serviços internos
- Saída da varredura de credenciais nos dois apps

## Limitações / notas

- **A exceção é o ponto desta Issue:** se outra Issue precisar de `Requer` cross-app, a doutrina foi violada — decisão é do tracker (revisar `AGENTS.md` raiz e `BOARD.md`), não uma adaptação silenciosa num card
- **Invariante de saúde do `ledger 07`:** ligar `REDIS_ENABLED=true` com Redis inacessível faz `/actuator/health` responder `503` e o rollback entrar em loop — o `redis` entra com `service_healthy` e a subida do `redis` precede a da aplicação
- **Rollback é a saída correta:** se a stack integrada falhar, o deploy da Issue 07 do `ledger` volta ao compose anterior, sem `redis` e sem gateway — a integração regride junto, e isso é o comportamento esperado
- Custo zero: `redis` e gateway rodam na VPS já provisionada pela trilha VPS
- O contrato de payload da Stream é o mesmo provado na Issue 10 (`eventId`, `orderId`, `amount`, `currency`, `status`, `timestamp`) — o publisher Java já existe e fala esse formato; só falta ligá-lo
- O `WEBHOOK_SECRET` precisa existir no ambiente da VPS: mesma semâthica das Issues 01 e 02 deste app, segredo fora do repositório
- **Limite de evidência:** a `terceira ponta` das três pontas é provada contra o `destino de entrega` declarado; o consumidor do e-commerce segue `sem card` no `BOARD.md` (`BOARD.md:19`) e a fronteira de escopo não muda nesta Issue
- **Decisão de autenticação:** a decisão declarada é senha própria em produção — rede interna sozinha `não basta` —, na mesma semântica do staging da [Issue 11](11-staging-inseguro.md)
