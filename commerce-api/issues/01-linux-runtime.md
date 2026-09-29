---
aliases: [issue-01, linux-runtime]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 01 — API sobe de forma previsível no Linux com healthcheck e shutdown gracioso

## Contexto

A `commerce-api` precisa rodar num host Linux sem contêineres. O contrato de ambiente já existe (`commerce-api/app/src/config/env.ts` valida `PORT`, `HOST`, `DATABASE_URL`, `JWT_SECRET` e `CORS_ORIGIN` com `zod`, e `commerce-api/app/.env.example` documenta os mesmos campos), mas ele só vale para quem roda `npm run dev` na própria máquina: não há unidade de serviço, ninguém prova que o processo está saudável, e o desligamento sob `SIGTERM` existe no código (`src/index.ts`) sem nada que o exija. Sem esse contrato, qualquer orquestrador posterior (Docker, VPS, K8s) herda um serviço imprevisível.

## Objetivo

Estado final: a aplicação gerenciada por um serviço `systemd` que sobe sozinha, com configuração lida de arquivo de ambiente, healthcheck em duas camadas com código de saída distinto para saudável/falho, e desligamento gracioso sob `SIGTERM`.

## Dependências

- Nenhuma dependência de outra Issue — ponto de entrada da trilha; a Issue 02 (orquestração com Postgres) e a Issue 03 (rede no Terraform) consomem a porta e o contrato de ambiente definidos aqui

## Escopo

- Configuração de URL do banco, porta, host de escuta e segredo JWT via variáveis de ambiente
- Unidade de serviço para `node dist/index.js` com diretório de trabalho, arquivo de ambiente e política de restart
- Healthcheck L4 por socket TCP e L7 por `GET /health`
- Tratamento de `SIGTERM` para fechamento ordenado do servidor HTTP e do pool

## Fora de escopo

- Docker, Docker Compose, Terraform, Kubernetes, CI/CD
- Foco exclusivo: processos Linux, permissões de arquivo, variáveis de ambiente, porta local, PostgreSQL no host e healthcheck em bash
- Orquestração de API e banco em rede de contêiner — Issue 02

## Conhecimentos envolvidos

- Modelo de processos Linux e sinais POSIX
- Unidades `systemd`: `WorkingDirectory`, `EnvironmentFile`, `ExecStartPre`, `Restart`, `TimeoutStopSec`
- Variáveis de ambiente e validação de schema na inicialização
- Sockets TCP e inspeção de portas
- Liveness e readiness em uma instância Fastify
- Readiness do PostgreSQL (`pg_isready`)

## Estado atual

- O contrato de ambiente existe no código e no `.env.example`, mas só é usado em execução manual
- A rota `/health` já responde `200` com `"status":"UP"` e `503` com `"status":"DEGRADED"` (`src/app.ts`), porém nada no host a consulta
- O tratamento de `SIGTERM`/`SIGINT` existe em `src/index.ts` (fecha o Fastify e encerra o client do Postgres com timeout de 5 s), mas não há prova de que ele roda fora de um `Ctrl+C`
- Nenhuma política de restart: uma queda deixa o serviço fora até alguém agir manualmente

## Resultado esperado

- Variáveis esperadas conhecidas: `PORT=3000`, `HOST=0.0.0.0`, `DATABASE_URL=postgresql://commerce_user:commerce_pass@localhost:5432/commerce_db`, `JWT_SECRET` com no mínimo 16 caracteres
- Healthcheck retorna exit 0 quando saudável e exit 1 quando falho
- `/health` responde 200 com `"status":"UP"` e `checks.database` em `UP`
- `SIGTERM` encerra com código de saída 0, sem conexão de banco aberta no fim do processo

## Requisitos

- [ ] Documentar como subir o PostgreSQL no host e a API na ordem correta, aplicando o schema com `npx drizzle-kit push` a partir de `commerce-api/app/`
- [ ] Criar a unidade de serviço executando `node dist/index.js` com `WorkingDirectory` em `commerce-api/app/`, sem `npm run dev` no `ExecStart`
- [ ] Apontar a unidade para um arquivo de ambiente fora do repositório, com permissão restrita, e nunca versionar o arquivo real
- [ ] Declarar política de restart na unidade e tempo de parada suficiente para o fechamento ordenado
- [ ] Implementar teste L4 via socket TCP (`/dev/tcp` ou `nc -z`) na porta configurada
- [ ] Implementar teste L7 via `GET /health` exigindo HTTP 200 e `"status":"UP"`
- [ ] Validar que o processo escuta a porta configurada em `0.0.0.0`, conferida via `ss -tulpn`
- [ ] Confirmar o desligamento sob `SIGTERM` com log de fechamento do servidor HTTP e do pool de conexões
- [ ] Manter a suíte `npm test` (13 testes) e o `npm run lint` verdes com o contrato de ambiente em uso

## Critérios de aceitação

- [ ] A unidade do serviço sobe a aplicação e o status do serviço reporta `active (running)` sem interação manual
- [ ] `/health` responde HTTP 200 com `"status":"UP"` e `checks.database` em `UP` com o banco no host
- [ ] Healthcheck retorna exit 0 no caso saudável e exit 1 no caso falho
- [ ] `SIGTERM` encerra o processo com código de saída 0, sem erro de pool de conexões aberto
- [ ] `ss -tulpn` mostra o processo escutando em `0.0.0.0:3000`
- [ ] Nenhum arquivo de ambiente real é versionado no repositório

## Validação

- Subrir o banco e a API na ordem documentada e confirmar `curl -i http://localhost:3000/health` retornando HTTP 200 com `"status":"UP"`
- Rodar o healthcheck com o serviço no ar (esperado: exit 0) e com o serviço parado (esperado: exit 1)
- Conferir a porta com `ss -tulpn`
- Enviar `SIGTERM` ao processo e observar desligamento ordenado, sem erro de pool aberto
- Derrubar o processo e observar a política de restart devolvendo o serviço ao ar sem intervenção
- Rodar `npm test` e `npm run lint` em `commerce-api/app/`

## Evidências

- Output de `ss -tulpn` mostrando a porta `3000` e o processo Node
- Output de `curl -i /health` com HTTP 200, `"status":"UP"` e `checks.database` em `UP`
- Códigos de saída do healthcheck nos dois cenários (0 e 1)
- Log do desligamento sob `SIGTERM` com o fechamento do servidor HTTP e do pool
- Output de `npm test` com os 13 testes verdes

## Limitações / notas

- O PostgreSQL roda no host nesta Issue — não há contêiner nem rede de orquestração; a Issue 02 é quem publica `3000` e isola o banco
- O healthcheck L4 prova que a porta aceita conexão; não prova que a aplicação responde — por isso existe o teste L7
- `/health` é liveness **e** readiness ao mesmo tempo: com o banco fora, responde `503` com `"status":"DEGRADED"`, então um healthcheck que aceite só HTTP 200 trata indisponibilidade de banco como queda do processo
- Cada trilha tem seus próprios scripts de check em `scripts/`; o desta trilha é `commerce-api/scripts/healthcheck.sh` e nasce aqui — nenhum script de outra trilha é reaproveitado nem editado por causa desta Issue
- O pool do Postgres é criado pelo driver `postgres` com `max: 10`, `idle_timeout: 20` e `connect_timeout: 10` (`src/db/connection.ts`): `SIGTERM` precisa de folga para `client.end({ timeout: 5 })` concluir
- `HOST=0.0.0.0` é o default do código; a API escuta em todas as interfaces, então a exposição publicamente é decisão de Issue posterior, não desta
- Este contrato de porta, de arquivo de ambiente e de `SIGTERM` é herdado por todas as Issues posteriores que mexem em runtime
