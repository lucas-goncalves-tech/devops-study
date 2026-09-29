---
aliases: [issue-02, docker-compose]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 02 — Compose com banco isolado e API Non-Root, orquestrando o Dockerfile existente

## Contexto

O `Dockerfile` da `commerce-api` já existe e é bom: multi-stage em `node:20-alpine`, `USER node`, `EXPOSE 3000` e `HEALTHCHECK` em `/health`. O que não existe é a orquestração — não há `docker-compose.yaml` neste app, então ninguém sobe API e banco juntos, a API pode subir antes do Postgres aceitar conexões, e `.env.example` aponta para um host (`postgres`) que só existe se alguém declarar esse serviço. Sem um `docker compose up` reproduzível, a orquestração de produção da plataforma segue dependendo da stack do `ledger-service`, que é outro app com outro banco e outro esquema.

## Objetivo

Estado final: um `docker compose up` que sobe a API e o Postgres na mesma rede, com o banco inalcançável a partir do host, dados que sobrevivem a `down`/`up`, e a API só recebendo tráfego depois que o banco está saudável.

## Dependências

- Requer Issue 01 — o contrato de porta (`PORT=3000`), arquivo de ambiente e fechamento sob `SIGTERM` vem de lá

## Escopo

- Orquestração da API (build a partir do `Dockerfile` existente) com `postgres` em rede interna e volume persistente
- Configuração do Postgres por ambiente: database, usuário e senha da `commerce-api`
- Dependência condicional: a API aguarda a saúde do banco
- `.env` obrigatório para o Compose, derivado de `.env.example` com placeholders

## Fora de escopo

- Alterar o `Dockerfile` já existente, Terraform, Kubernetes, CI/CD
- Foco exclusivo: `docker-compose.yaml` com `postgres:16-alpine`, `env_file`, volume nomeado, healthcheck e `depends_on` por saúde
- Segmentação de redes e limites de recursos — [Issue 06 do `ledger-service`](../../ledger-service/issues/06-compose-isolation.md)
- Endpoint HTTP de métricas e dashboards — [Issue 05](05-observability.md)
- Gateway de webhooks consumindo os eventos deste e-commerce — integração de produção, fora desta Issue (card ainda não criado, registrado no `BOARD.md`)

## Conhecimentos envolvidos

- Orquestração de serviços com Docker Compose
- Ordem de subida e healthcheck no Compose (`depends_on` com condição)
- `env_file`, interpolação de variáveis e precedência entre arquivo e ambiente
- PostgreSQL em contêiner e persistência de volume nomeado
- DNS de serviço dentro da rede do Compose

## Estado atual

- O `Dockerfile` está pronto e já declara `HEALTHCHECK` em `/health`; o que falta é o Compose que o consome
- `commerce-api/app/.env.example` já traz `DATABASE_URL=postgresql://commerce_user:commerce_pass@postgres:5432/commerce_db` — o host `postgres` é o nome de serviço que esta Issue precisa declarar
- Sem `docker-compose.yaml`, o banco e a API sobem em máquinas diferentes e o schema precisa ser aplicado à mão
- Sem volume nomeado, `docker compose down` com remoção de volume apaga os dados de catálogo, produtos e pedidos
- Nenhuma garantia de ordem de subida: a API sobe, o pool tenta conectar e a primeira requisição falha

## Resultado esperado

- `docker compose up` sobe banco e API em um comando, com o schema aplicado e a API servindo
- O Postgres não aceita conexão publicada no host — só o contêiner da API o alcança, pelo DNS do serviço
- Os dados sobrevivem a `docker compose down` seguido de `up`
- A API inicia somente depois de o healthcheck do banco reportar saudável
- `SIGTERM` no contêiner da API fecha o servidor HTTP e o pool sem erro

## Requisitos

- [ ] Declarar `docker-compose.yaml` em `commerce-api/app/` com o serviço da API a partir do `Dockerfile` existente, sem reescrever o build
- [ ] Declarar o serviço de banco com `postgres:16-alpine`, configurando `POSTGRES_DB`, `POSTGRES_USER` e `POSTGRES_PASSWORD` a partir do ambiente, com valores coerentes com `DATABASE_URL` do `.env`
- [ ] Montar um volume nomeado em `/var/lib/postgresql/data` e declará-lo em `volumes:`
- [ ] Dar healthcheck ao banco via `pg_isready` apontando para o usuário e o database daquele serviço
- [ ] Garantir que a API só sobe quando o banco estiver saudável (`depends_on` com `condition: service_healthy`)
- [ ] Usar `env_file` no serviço da API, com `.env` derivado de `.env.example` e nunca versionado
- [ ] Publicar no host apenas a porta da API, `${PORT:-3000}:${PORT:-3000}`, e nenhuma porta do banco
- [ ] Garantir que o nome do serviço do banco resolve dentro da rede do Compose, porque `DATABASE_URL` do `.env.example` já aponta para o host `postgres`
- [ ] Documentar a aplicação do schema com `npx drizzle-kit push` contra o `DATABASE_URL` da rede do Compose
- [ ] Confirmar que `SIGTERM` no contêiner da API encerra o servidor HTTP e o pool de conexões do driver `postgres` sem erro
- [ ] Manter a suíte `npm test` (13 testes) e o `npm run lint` verdes — o Compose não altera o contrato do código

## Critérios de aceitação

- [ ] `docker compose up` deixa banco e API saudáveis, com `/health` respondendo HTTP 200 e `"status":"UP"`
- [ ] A porta do Postgres não é publicada no host: conexão vinda de fora da rede do Compose é recusada
- [ ] Dados do banco sobrevivem a `docker compose down` seguido de `up`
- [ ] A API não inicia antes de o banco responder ao healthcheck
- [ ] O contêiner da API executa como usuário não-root, com o usuário `node` do estágio `runner`
- [ ] `SIGTERM` no contêiner da API encerra sem erro de pool aberto
- [ ] Nenhum arquivo `.env` real é versionado

## Validação

- `docker compose up` observando a API só iniciar depois de `service_healthy`
- `docker compose ps` mostrando banco e API saudáveis
- Inserir um produto, executar `docker compose down`, `docker compose up` e confirmar que o registro persiste
- Tentar conectar na porta do Postgres a partir do host, esperando recusa
- `docker inspect` no contêiner da API confirmando o usuário de execução e o tamanho da imagem
- Enviar `SIGTERM` ao contêiner da API e conferir o log de desligamento
- `curl -i http://localhost:3000/health` com o stack no ar (esperado: 200, `UP`)

## Evidências

- Output do `docker compose up` com a condição `service_healthy` do banco
- `docker compose ps` com os dois serviços saudáveis
- Saída da tentativa de conexão na porta do banco a partir do host (recusada)
- Teste de persistência entre `down` e `up`
- Usuário de execução e tamanho da imagem via `docker inspect`/`docker image inspect`
- Log de desligamento gracioso no `SIGTERM` do contêiner da API
- Saída do `npm test` da `commerce-api`

## Limitações / notas

- O `Dockerfile` desta app já é multi-stage, `USER node` e com `HEALTHCHECK` em `/health` — esta Issue **não** o reescreve; se algo precisar mudar nele, é revisão do contrato de imagem, não orquestração
- `commerce-api/app/.dockerignore` já exclui `.env`, `node_modules` e `dist` do contexto de build — ele não pode ser removido
- `commerce-api/app/docker-compose.yaml` publica apenas a porta da API (`${PORT:-3000}:${PORT:-3000}`) e **não** publica a porta do banco — manter assim
- O serviço do banco não pode ganhar entrada `ports:` em nenhuma Issue futura
- `commerce-api/app/.env` é obrigatório para o Compose (`env_file`) e nunca deve ser commitado nem embutido na imagem; `.env.example` é o molde e só pode conter placeholders
- O serviço do banco se chama `postgres` porque `DATABASE_URL` do `.env.example` já aponta para esse host — renomear o serviço sem renomear a variável quebra a conexão
- O schema é aplicado por `drizzle-kit push` contra o banco do Compose, e não por inicialização automática da aplicação: subir a stack não cria tabelas sozinho
- A porta `5432` e o schema `commerce_db` são os desta app; o `ledger-service` tem banco, credenciais e volume próprios — as duas stacks não compartilham serviço
- Esta Issue não isola redes: tudo sobe na rede padrão do Compose. A segmentação que impede qualquer contêiner de alcançar o banco é [Issue 06 do `ledger-service`](../../ledger-service/issues/06-compose-isolation.md)
