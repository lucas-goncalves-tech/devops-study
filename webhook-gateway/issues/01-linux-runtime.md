---
aliases: [issue-01, linux-runtime]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 01 — Consumidor sobe de forma previsível no Linux com restart e shutdown gracioso

## Contexto

O `webhook-gateway` não é um servidor HTTP: é um consumidor de Redis Streams em laço, sem porta nenhuma. Ele lê a configuração do ambiente em `src/index.ts` (`REDIS_URL`, `STREAM_KEY`, `GROUP_NAME`, `CONSUMER_NAME`, `WEBHOOK_SECRET`, todos com default), já trata `SIGTERM`/`SIGINT` chamando `consumer.stop()`, e a suíte (`npm test`, 9 testes) trava o contrato de Stream, group e assinatura. O que não existe é o contrato de host: nenhuma unidade de serviço, nenhuma verificação de que o processo está vivo, nenhuma política de restart. Um consumidor que cai silenciosamente para de despachar webhooks e ninguém percebe até um parceiro reclamar que não recebeu nada.

## Objetivo

Estado final: o consumidor gerenciado por uma unidade de serviço que sobe sozinha, com configuração lida de arquivo de ambiente, verificação em nível de processo (o consumidor está rodando e conectado ao Redis) e desligamento gracioso sob `SIGTERM` que fecha a conexão do `ioredis`.

## Dependências

- Nenhuma dependência de outra Issue — ponto de entrada da trilha; a Issue 02 (imagem e `HEALTHCHECK`) e a Issue 10 (entrada do gateway na stack de produção) consomem o contrato de ambiente definido aqui

## Escopo

- Configuração de `REDIS_URL`, `STREAM_KEY`, `GROUP_NAME`, `CONSUMER_NAME` e `WEBHOOK_SECRET` via variáveis de ambiente
- Unidade de serviço para `node dist/index.js` com diretório de trabalho, arquivo de ambiente e política de restart
- Verificação em nível de processo: unidade ativa, `MainPID` estável e conexão com o Redis estabelecida
- Tratamento de `SIGTERM` para fechamento ordenado do cliente Redis

## Fora de escopo

- Docker, Docker Compose, Terraform, Kubernetes, CI/CD
- Foco exclusivo: processos Linux, permissões de arquivo, variáveis de ambiente, verificação de processo e script de verificação em bash
- Servidor HTTP, rota `/health` e `HEALTHCHECK` de imagem — Issue 02
- Liveness e readiness em HTTP — Issue 02; métricas de lag e entrega de webhook — Issue 10

## Conhecimentos envolvidos

- Modelo de processos Linux e sinais POSIX
- Unidades `systemd`: `WorkingDirectory`, `EnvironmentFile`, `Restart`, `Type=simple` e `TimeoutStopSec`
- Variáveis de ambiente e defaults em código
- Verificação de processo: `systemctl status`, `MainPID` e descrição de uma conexão TCP estabelecida
- Redis: cliente conectado, `PING` e encerramento com `QUIT`
- `ioredis`: `quit()`, `disconnect()` e reconexão automática

## Estado atual

- A configuração por ambiente existe em código com defaults inseguros: sem `REDIS_URL` o consumidor aponta para `redis://localhost:6379` e sem `WEBHOOK_SECRET` assina com uma chave de exemplo versionada no repositório
- O consumidor roda em laço com `XREADGROUP` bloqueante (`BLOCK 2000`) e um `catch` que espera 1 s antes de tentar de novo — falha de Redis não derruba o processo, mas também não gera alerta
- O tratamento de `SIGTERM`/`SIGINT` chama `consumer.stop()`, que faz `quit()` e cai para `disconnect()` se o `quit` falhar
- Nenhuma unidade de serviço: o processo só existe enquanto o terminal existir
- Nenhum sinal de vida observável: o healthcheck L4/L7 do host não se aplica, porque não há porta

## Resultado esperado

- Variáveis esperadas conhecidas: `REDIS_URL=redis://localhost:6379`, `STREAM_KEY=payment-events`, `GROUP_NAME=webhook-dispatcher-group`, `CONSUMER_NAME=worker-<pid>`, `WEBHOOK_SECRET` com chave real fora do repositório
- A unidade reporta `active (running)` e o `MainPID` permanece o mesmo enquanto o consumidor não cai
- O processo mantém uma conexão estabelecida com a porta `6379` do Redis
- `SIGTERM` encerra com código de saída 0, com a conexão do Redis fechada e sem consumer pendente preso

## Requisitos

- [ ] Documentar a ordem de subida: Redis acessível, grupo da Stream criado, consumidor iniciado
- [ ] Criar a unidade de serviço executando `node dist/index.js` com `WorkingDirectory` em `webhook-gateway/app/`, sem nenhum executor de TypeScript no `ExecStart` (este app não tem script `dev`)
- [ ] Apontar a unidade para um arquivo de ambiente fora do repositório, com permissão restrita, e nunca versionar o arquivo real
- [ ] Garantir que `REDIS_URL` e `WEBHOOK_SECRET` venham desse arquivo, sem depender dos defaults do código
- [ ] Declarar política de restart na unidade e tempo de parada suficiente para o `quit()` do `ioredis` concluir
- [ ] Implementar verificação em nível de processo que confirme a unidade ativa e o `MainPID` da unidade
- [ ] Implementar verificação L4 da conexão com o Redis, esperando conexão estabelecida na porta `6379` e nenhuma conexão do consumidor presa em `SYN_SENT`
- [ ] Implementar verificação de que o consumidor entrou no grupo certo, lendo o log de subida com a Stream e o group declarados
- [ ] Confirmar o desligamento sob `SIGTERM` com log de parada e conexão Redis encerrada
- [ ] Manter a suíte `npm test` (9 testes) verde com o contrato de ambiente em uso

## Critérios de aceitação

- [ ] A unidade do serviço sobe o consumidor e o status reporta `active (running)` sem interação manual
- [ ] O `MainPID` da unidade é o processo Node que executa o consumidor e permanece estável enquanto ele roda
- [ ] Existe conexão estabelecida entre o processo e a porta `6379` do Redis, confirmada por inspeção de sockets
- [ ] O log de subida identifica a Stream e o group esperados, sem `BUSYGROUP` tratado como erro
- [ ] `SIGTERM` encerra o processo com código de saída 0 e sem conexão do Redis pendente
- [ ] Derrubar o processo devolve o consumidor ao ar pela política de restart, sem intervenção manual
- [ ] Nenhum arquivo de ambiente real é versionado no repositório

## Validação

- Subir o Redis, iniciar a unidade e conferir `systemctl status` reportando `active (running)` com `MainPID` estável
- Conferir a conexão estabelecida com a porta `6379` na inspeção de sockets do processo
- Injetar um evento na Stream e observar o log do consumidor processando e fazendo `XACK`
- Rodar a verificação de processo com o consumidor no ar (esperado: exit 0) e com ele parado (esperado: exit 1)
- Enviar `SIGTERM` e observar desligamento ordenado, sem erro de conexão aberta
- Matar o processo e observar a política de restart devolvendo o consumidor ao ar
- Rodar `npm test` em `webhook-gateway/app/`

## Evidências

- Saída do status da unidade com `active (running)` e `MainPID`
- Saída da inspeção de sockets mostrando a conexão estabelecida com a porta `6379`
- Log de subida com Stream, group e nome do consumidor
- Log do processamento de um evento injetado, com a assinatura gerada
- Códigos de saída da verificação de processo nos dois cenários (0 e 1)
- Log do desligamento sob `SIGTERM` e saída 0
- Saída do `npm test` com os 9 testes verdes

## Limitações / notas

- **Este app não abre porta HTTP:** a verificação desta Issue é em nível de processo e de conexão com o Redis, não L7. Um healthcheck de verdade sobre HTTP é trabalho da Issue 02, que cria a rota `/health` e o `HEALTHCHECK` da imagem
- Por isso a verificação L4 aqui significa "o processo está vivo e conectado ao Redis", e não "a porta X responde": as duas coisas não são equivalentes e a segunda ainda não existe
- O `REDIS_URL` do `.env` de host aponta para `localhost:6379` porque o Redis roda no host nesta Issue; na stack de contêiner o mesmo valor muda para o nome do serviço
- O `WEBHOOK_SECRET` tem default versionado em `src/index.ts` — ele serve para desenvolvimento e precisa ser substituído por arquivo de ambiente em qualquer ambiente compartilhado
- O `initGroup` cria o group com `MKSTREAM` e tolera `BUSYGROUP` (`src/consumer.ts`): em restart o consumidor não pode falhar por o group já existir
- O laço de consumo trata erro de leitura com espera de 1 s e continua, então indisponibilidade do Redis não derruba o processo nem o reinicia — só aparece no log
- `XREADGROUP` com `BLOCK 2000` significa que a primeira leitura pode demorar até 2 s: uma verificação que exija evento imediato não é confiável
- A verificação de processo não prova entrega de webhook; prova que o consumidor está consumindo. A entrega real é escopo da [Issue 10](10-containers-redis.md)
- Este contrato de arquivo de ambiente, política de restart e `SIGTERM` é herdado por todas as Issues posteriores deste app que mexem em runtime
