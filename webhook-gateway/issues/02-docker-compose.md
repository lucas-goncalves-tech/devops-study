---
aliases: [issue-02, docker-compose]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 02 — Imagem Non-Root com rota de saúde e Compose com Redis não publicado

## Contexto

O `webhook-gateway` não tem imagem: não existe `Dockerfile`, não existe `docker-compose.yaml` e não existe `.env.example`. O código compila para `dist/` e roda com `node dist/index.js`, mas um consumidor que precisa de Redis não sobe sozinho em lugar nenhum, e o `.gitignore` do app já ignora `.env` sem que exista molde de configuração para alguém preencher. Pior: como o app não serve HTTP, um `HEALTHCHECK` de imagem não tem o que consultar — a rota `/health` precisa nascer junto com o `Dockerfile`, não depois.

## Objetivo

Estado final: imagem multi-stage enxuta executando como usuário sem privilégios, com rota `/health` e `HEALTHCHECK` reais, e um `docker compose up` que sobe o consumidor junto do Redis sem publicar a porta `6379` no host, com `.env.example` como molde documentado.

## Dependências

- Requer Issue 01 — o contrato de arquivo de ambiente, política de restart e fechamento sob `SIGTERM` vem de lá; a rota `/health` precisa observar a mesma conexão Redis que aquela Issue verifica

## Escopo

- Criar a rota `/health` no `src/`, expondo o estado do consumidor e da conexão com o Redis
- Criar `Dockerfile` multi-stage com usuário sem privilégios e `HEALTHCHECK`
- Criar `docker-compose.yaml` com o serviço do consumidor e o serviço Redis em rede interna
- Criar `.env.example` com placeholders de `REDIS_URL`, `STREAM_KEY`, `GROUP_NAME`, `CONSUMER_NAME`, `WEBHOOK_SECRET` e `PORT` (default `8081`, porta da rota de saúde)
- Dependência condicional: o consumidor sobe depois que o Redis está saudável

## Fora de escopo

- Terraform, Kubernetes, CI/CD, gateway de webhooks com validação, idempotência e retry
- Foco exclusivo: rota `/health`, multi-stage com usuário sem privilégios, `HEALTHCHECK`, `docker-compose.yaml` com Redis e `.env.example`
- Segmentação de redes e limites de recursos — [Issue 06 do `ledger-service`](../../ledger-service/issues/06-compose-isolation.md)
- Entrada do gateway na stack de produção — [Issue 12](12-integracao-producao.md) desta trilha; TLS e publicação de porta — fora desta Issue
- Métricas de lag do consumer group — [Issue 10](10-containers-redis.md)

## Conhecimentos envolvidos

- Multi-stage builds e usuário não-root em imagem Node
- `EXPOSE`, `HEALTHCHECK` e a dependência entre healthcheck de imagem e rota HTTP real
- Orquestração com Docker Compose, `env_file` e healthcheck de serviço
- Redis em contêiner: `redis-server`, persistência e healthcheck por `redis-cli ping`
- Liveness e readiness em processo que não é servidor

## Estado atual

- Não existe `Dockerfile`, `docker-compose.yaml` nem `.env.example` neste app
- Não existe servidor HTTP nem rota de saúde: `src/index.ts` só instancia o `StreamConsumer` e instala os handlers de sinal
- A configuração existe apenas como defaults em código (`REDIS_URL` em `redis://localhost:6379`, `WEBHOOK_SECRET` com chave de exemplo versionada)
- O `.gitignore` do app já lista `.env`, sem que exista arquivo de exemplo para orientar o preenchimento
- Sem `HEALTHCHECK`, um contêiner com o laço de consumo travado reporta `Up` e ninguém percebe que o consumo parou

## Resultado esperado

- `Dockerfile` multi-stage com a imagem final contendo apenas o artefato compilado e as dependências de produção
- Contêiner executando como usuário sem privilégios, nunca root
- `/health` respondendo HTTP 200 quando o consumidor está ativo e conectado ao Redis
- `HEALTHCHECK` da imagem consultando `/health` e falhando quando o consumidor para de consumir
- Compose com Redis em rede interna, sem porta `6379` publicada no host
- `.env.example` documentando as seis variáveis com placeholders, sem segredo real

## Requisitos

- [ ] Criar `webhook-gateway/app/Dockerfile` multi-stage: estágio de build com `npm ci` e `npm run build`, estágio de runtime com `node:20-alpine`, `npm ci --omit=dev` e cópia apenas de `dist/` e dos módulos de produção
- [ ] Rodar o contêiner com usuário sem privilégios via `USER node`, nunca root
- [ ] Declarar `EXPOSE` da porta `PORT` (default `8081`) da rota de saúde e `HEALTHCHECK` no `Dockerfile` consultando `/health`
- [ ] Criar `.dockerignore` excluindo `node_modules`, `dist`, `.git` e `.env` do contexto de build
- [ ] Criar a rota `/health` no `src/`, com resposta que distingue consumidor ativo de conexão com o Redis perdida
- [ ] Registrar a rota de saúde no mesmo processo que executa o `StreamConsumer`, de modo que o servidor HTTP de saúde suba e desça junto com o consumidor, sem processo separado nem segundo ciclo de vida
- [ ] Criar `webhook-gateway/app/docker-compose.yaml` com o serviço do consumidor construído a partir do `Dockerfile` e o serviço `redis:7-alpine`
- [ ] Não publicar a porta `6379` do Redis no host; o consumidor fala com ele pelo nome do serviço na rede do Compose
- [ ] Dar healthcheck ao Redis via `redis-cli ping` e ligar o consumidor a ele por `depends_on` com `condition: service_healthy`
- [ ] Usar `env_file` no serviço do consumidor, com `REDIS_URL` apontando para o nome do serviço Redis
- [ ] Criar `webhook-gateway/app/.env.example` com `REDIS_URL`, `STREAM_KEY`, `GROUP_NAME`, `CONSUMER_NAME`, `WEBHOOK_SECRET` e `PORT` (default `8081`) em placeholder
- [ ] Publicar no host apenas a porta `PORT` (default `8081`) da rota de saúde, e somente em loopback (`127.0.0.1:${PORT:-8081}:${PORT:-8081}`), se e somente se a validação exigir consultá-la de fora
- [ ] Confirmar que `SIGTERM` no contêiner fecha o `quit()` do `ioredis` e encerra com código de saída 0
- [ ] Manter a suíte `npm test` (9 testes) verde — a rota de saúde não pode regreder o contrato de Stream, group e assinatura

## Critérios de aceitação

- [ ] Imagem construída em multi-stage, executada por usuário não-root e contendo apenas `dist/` e dependências de produção
- [ ] `HEALTHCHECK` da imagem retorna saudável com o consumidor conectado e falho com o consumidor parado
- [ ] `/health` responde HTTP 200 com o consumidor ativo e conexão com o Redis estabelecida
- [ ] A porta `6379` não é publicada no host: conexão vinda de fora da rede do Compose é recusada
- [ ] O consumidor não inicia antes de o Redis responder ao healthcheck
- [ ] `SIGTERM` no contêiner encerra com código de saída 0 e sem conexão Redis pendente
- [ ] `webhook-gateway/app/.env.example` existe e não contém segredo real
- [ ] Nenhum `.env` real é versionado e nenhum `.env` entra no contexto de build

## Validação

- `docker build` seguido de inspeção da imagem confirmando usuário de execução e tamanho
- `docker compose up` observando o consumidor só iniciar depois de `service_healthy` do Redis
- `curl` na rota de saúde com o consumidor no ar (esperado: 200) e com o consumidor parado (esperado: falha)
- Injetar um evento na Stream e observar o log do consumidor processando e fazendo `XACK`
- Tentar conectar na porta `6379` a partir do host, esperando recusa
- Enviar `SIGTERM` ao contêiner e conferir o log de parada
- `npm test` em `webhook-gateway/app/` antes e depois da integração

## Evidências

- Saída de `docker image inspect` com usuário de execução e tamanho da imagem
- Output do `docker compose up` com a condição `service_healthy` do Redis
- Resposta da rota de saúde nos dois cenários (saudável e falho)
- Saída da tentativa de conexão na porta `6379` a partir do host (recusada)
- Log de desligamento gracioso no `SIGTERM` do contêiner
- Conteúdo de `.env.example` com os seis campos em placeholder
- Saída do `npm test` com os 9 testes verdes

## Limitações / notas

- **A rota `/health` nasce nesta Issue porque não há como testar HTTP sem ela:** o app hoje não serve HTTP nenhum, então o `HEALTHCHECK` da imagem e a rota são o mesmo trabalho, não dois
- A rota de saúde responde sobre o estado do consumidor e da conexão com o Redis, e não sobre entrega de webhook: um consumidor que despacha errado e acerta o healthcheck é um bug de entrega, não de disponibilidade
- O `.env` real é ignorado por `.gitignore` e excluído por `.dockerignore`; `.env.example` é o único arquivo de configuração que pode ser versionado e ele só aceita placeholders
- `REDIS_URL` muda de host conforme o ambiente: `localhost:6379` no host, nome do serviço na rede do Compose. O default do código (`redis://localhost:6379`) só é correto fora do contêiner
- O serviço do Redis não pode ganhar entrada `ports:` em nenhuma Issue futura — acesso externo à Stream de eventos é exposição de dado de pedido
- Esta Issue não segmenta redes: consumidor e Redis sobem na rede padrão do Compose. A segmentação por perfil de rede é da [Issue 10](10-containers-redis.md) deste app
- O consumidor cria o group com `MKSTREAM` e tolera `BUSYGROUP` (`src/consumer.ts`): manter isso é o que faz o contêiner subir em restart
- Publicar a porta `PORT` (default `8081`) da rota de saúde no host é opcional e só serve para validação externa; em produção ela não precisa ser alcançável de fora — e o bind é sempre `127.0.0.1`, nunca `0.0.0.0`
- Esta Issue não entrega webhook: validação, idempotência, retry e lag são [Issue 10](10-containers-redis.md)
