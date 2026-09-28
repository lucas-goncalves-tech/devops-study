---
aliases: [issue-07, cicd-vps-deploy]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 07 — Deploy contínuo auditável na VPS só com pipeline verde

## Contexto

O deploy é manual: alguém conecta por SSH, roda comandos e não deixa rastro. Não há barreira entre um merge e produção, não há rollback automático e não há como dizer em 30 segundos quem implantou o quê.

## Objetivo

Estado final: cada merge com gates verdes vira deploy automático na VPS via chave efêmera, com healthcheck pós-deploy e rollback automático em falha, e trilha de auditoria de versão, autor e horário.

## Dependências

- Requer [Issue 04 do `commerce-api`](../../commerce-api/issues/04-github-actions.md) — pipeline e proteção de branch
- Requer [Issue 04 do `webhook-gateway`](../../webhook-gateway/issues/04-secrets-hygiene.md) — gate de segredos
- Requer [Issue 05 do `webhook-gateway`](../../webhook-gateway/issues/05-sast-semgrep.md) — gate de SAST
- Requer [Issue 08 do `webhook-gateway`](../../webhook-gateway/issues/08-devsecops-gates.md) — gates consolidados como pré-requisito
- Requer Issue 03 — acesso ao servidor somente por chave SSH
- Requer Issue 04 — o proxy é a porta única de entrada
- Requer Issue 06 — topologia de rede alvo do deploy

## Escopo

- Gates como pré-requisito obrigatório do deploy
- Separação de produção e staging por aprovação ou filtro de branch
- Deploy via SSH com chave efêmera, sem credencial em log
- Healthcheck pós-deploy com rollback automático
- Registro auditável de versão, autor e timestamp

## Fora de escopo

- Criação dos gates — [Issue 04](../../webhook-gateway/issues/04-secrets-hygiene.md), [Issue 05](../../webhook-gateway/issues/05-sast-semgrep.md), [Issue 07](../../webhook-gateway/issues/07-pipeline-hardening.md) e [Issue 08](../../webhook-gateway/issues/08-devsecops-gates.md) do `webhook-gateway`
- Provisionamento de servidor e firewall — Issue 03; a stack com Redis e webhook chega na [Issue 10 do `webhook-gateway`](../../webhook-gateway/issues/10-containers-redis.md)
- Kubernetes e orquestração — fora do escopo desta trilha
- Rollout blue-green com duas versões simultâneas — esta Issue entrega deploy com rollback, não dual-run

## Conhecimentos envolvidos

- SSH e acesso por chave em automação
- Environments e proteção de credencial de deploy
- Estratégias de deploy e rollback
- Healthcheck como gate de imediatismo

## Estado atual

- Deploy manual por SSH, sem rastro
- Nenhuma barreira entre merge e produção
- Sem healthcheck pós-deploy nem rollback automático

## Resultado esperado

- Só pipeline verde dispara deploy
- Produção e staging separados por aprovação ou filtro de branch
- Falha de healthcheck reverte automaticamente para a versão anterior
- Versão, autor e horário auditáveis em 30 segundos

## Requisitos

- [ ] Reutilizar os gates (segredos, SAST, SCA) como pré-requisito do deploy
- [ ] Separar produção de staging por aprovação ou filtro de branch
- [ ] Deploy via SSH com chave efêmera, sem senha ou chave em log
- [ ] Healthcheck pós-deploy com rollback automático se falhar
- [ ] Registrar versão, autor e timestamp de forma auditável

## Critérios de aceitação

- [ ] Pull request sem gates verdes não dispara deploy
- [ ] Produção e staging não aceitam deploy pelo mesmo caminho sem a separação declarada
- [ ] Nenhum log de execução contém credencial de acesso
- [ ] Deploy com healthcheck falhando reverte sozinho para a versão anterior
- [ ] A versão implantada, o autor e o horário são recuperáveis em até 30 segundos

## Validação

- Abrir pull request com um gate falhando e confirmar que o deploy não inicia
- Conferir a separação entre os caminhos de produção e de staging
- Varredura dos logs de execução procurando credencial
- **Build to break:** implantar uma versão com healthcheck falhando e observar o rollback automático
- **Build to defend:** implantar versão saudável e confirmar que ela permanece
- Consultar o registro de auditoria cronometrando a recuperação

## Evidências

- Execução do pipeline sem deploy quando um gate falha
- Configuração de separação de ambientes
- Trecho de log sem credencial
- Log do rollback automático com a versão revertida
- Registro de auditoria com versão, autor e timestamp

## Limitações / notas

- **Invariante de saúde:** o rollback usa `healthcheck.sh`, que depende de `curl` em `/actuator/health` retornando HTTP 200 e do literal `"status":"UP"`. Se qualquer Issue ligar `REDIS_ENABLED=true` sem Redis alcançável, `/actuator/health` responde 503 e o rollback entra em loop — manter o healthcheck do Redis acoplado a `service_healthy`
- **Invariante de porta:** se a Issue 04 tornou `8080` interna, o healthcheck precisa apontar para o upstream correto; `PORT`, o `EXPOSE` do `Dockerfile` e `server.port` devem continuar coerentes entre si
- A chave efêmera depende do acesso por chave estabelecido na Issue 03
- `/actuator/**` precisa continuar `permitAll` — senão o healthcheck e o scraping falham por autenticação
