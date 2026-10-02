---
aliases: [issue-07, cicd-vps-deploy]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 07 — Deploy contínuo auditável na VPS só com pipeline verde

## Contexto

O deploy é manual: alguém conecta por SSH, roda comandos e não deixa rastro. Não há barreira entre um merge e produção, não há rollback automático e não há como dizer em 30 segundos quem implantou o quê. E antes de existir deploy, não existe pipeline nenhuma deste app: build, teste e varredura de credencial não rodam em pull request, então a primeira barreira a ser construída — a pipeline — é trabalho desta Issue, não herança de outra trilha.

## Objetivo

Estado final: uma pipeline própria deste app, com build, testes e varredura de credencial em pull request, proteção de branch exigindo o verde, e cada merge aprovado virando deploy automático na VPS via chave efêmera, com healthcheck pós-deploy e rollback automático em falha, e trilha de auditoria de versão, autor e horário.

## Dependências

- Requer Issue 02 — a imagem e a composição que este deploy publica
- Requer Issue 03 — acesso ao servidor somente por chave SSH
- Requer Issue 04 — o proxy é a porta única de entrada
- Requer Issue 06 — topologia de rede alvo do deploy

## Escopo

- Pipeline própria deste app no CI: build, suíte de testes e proteção de branch
- Gate de varredura de credencial versionada como barreira mínima de merge
- Gates como pré-requisito obrigatório do deploy
- Separação de produção e staging por aprovação ou filtro de branch
- Deploy via SSH com chave efêmera, sem credencial em log
- Healthcheck pós-deploy com rollback automático
- Registro auditável de versão, autor e timestamp

## Fora de escopo

- SAST, SCA e DAST aprofundados — não são pré-requisito desta Issue; a trilha DevSecOps do `webhook-gateway` é onde essas ferramentas são estudadas a fundo, e existir lá não cria dependência aqui
- Provisionamento de servidor e firewall — Issue 03; Redis e gateway de webhooks entram na stack de produção pela [Issue 12 do `webhook-gateway`](../../webhook-gateway/issues/12-integracao-producao.md) — essa Issue é quem mexe no compose desta trilha
- Kubernetes e orquestração — fora de escopo desta trilha
- Rollout blue-green com duas versões simultâneas — esta Issue entrega deploy com rollback, não dual-run

## Conhecimentos envolvidos

- Pipeline de CI: jobs, triggers, cache e proteção de branch como barreira de merge
- Varredura de credencial versionada como gate
- SSH e acesso por chave em automação
- Environments e proteção de credencial de deploy
- Estratégias de deploy e rollback
- Healthcheck como gate de imediatismo

## Estado atual

- Deploy manual por SSH, sem rastro
- Nenhuma barreira entre merge e produção
- Não existe pipeline deste app: build, teste e varredura de credencial não rodam em pull request
- Sem healthcheck pós-deploy nem rollback

## Resultado esperado

- Só pipeline verde dispara deploy
- Pull request com teste quebrado ou credencial versionada é barrado antes do merge
- Produção e staging separados por aprovação ou filtro de branch
- Falha de healthcheck reverte automaticamente para a versão anterior
- Versão, autor e horário auditáveis em 30 segundos

## Requisitos

- [ ] Criar a pipeline deste app no CI: build e suíte de testes em pull request, com proteção de branch exigindo o resultado verde
- [ ] Adicionar gate de varredura de credencial versionada, reprovando o pull request quando encontrar chave
- [ ] Exigir os gates verdes como pré-requisito do deploy
- [ ] Separar produção de staging por aprovação ou filtro de branch
- [ ] Deploy via SSH com chave efêmera, sem senha ou chave em log
- [ ] Declarar o `contrato de segredos de produção` antes do primeiro deploy: inventário com `JWT_SECRET` (geração de `256 bits`), `SPRING_DATASOURCE_PASSWORD`, a credencial do backup (Issue 05) e o token do canal de alerta (Issue 08); cada segredo vive fora do repositório — arquivo de ambiente da VPS com `permissão restrita` ou `environment` do CI —, com `geração` documentada e `rotação` definida; nenhum default de laboratório segue para produção
- [ ] Healthcheck pós-deploy com rollback automático se falhar — com a `compatibilidade de schema` entre a versão nova e a revertida declarada como pré-condição antes do primeiro rollback
- [ ] Registrar versão, autor e timestamp de forma auditável

## Critérios de aceitação

- [ ] Pull request com teste quebrado não passa pela proteção de branch deste app — build to break
- [ ] Commit contendo credencial de teste é recusado pelo gate de varredura — build to break, revertido em seguida
- [ ] Nenhum requisito desta Issue depende de Issue de outro app: pipeline, gate e deploy nascem todos nesta trilha
- [ ] Pull request sem gates verdes não dispara deploy
- [ ] Produção e staging não aceitam deploy pelo mesmo caminho sem a separação declarada
- [ ] Nenhum log de execução contém credencial de acesso
- [ ] Deploy com healthcheck falhando reverte sozinho para a versão anterior
- [ ] A versão implantada, o autor e o horário são recuperáveis em até 30 segundos

## Validação

- Abrir pull request com um gate falhando e confirmar que o deploy não inicia
- Introduzir um teste quebrado e confirmar o bloqueio na proteção de branch; reverter
- Commitar uma chave de teste formato credencial e confirmar a reprovação do gate; reverter
- Conferir a separação entre os caminhos de produção e de staging
- Varredura dos logs de execução procurando credencial
- **Build to break:** implantar uma versão com healthcheck falhando e observar o rollback automático
- **Build to defend:** implantar versão saudável e confirmar que ela permanece
- Consultar o registro de auditoria cronometrando a recuperação

## Evidências

- Execução do pipeline sem deploy quando um gate falha
- Log da pipeline verde no commit atual
- Log do gate de varredura reprovando a credencial de teste, com o commit corrigido
- Configuração de separação de ambientes
- Trecho de log sem credencial
- Log do rollback automático com a versão revertida
- Registro de auditoria com versão, autor e timestamp
- Conferência do ambiente de produção: cada segredo do contrato presente e nenhum default de laboratório ativo

## Limitações / notas

- **Os gates deste app nascem aqui:** a pipeline e o gate de varredura de credencial são escopo desta Issue, construídos do zero para este app — esta trilha não consome pipeline, gate nem proteção de branch de outra trilha. SAST, SCA e DAST aprofundados ficam deliberadamente fora: eles são o conteúdo da trilha DevSecOps do [`webhook-gateway`](../../webhook-gateway/AGENTS.md), e existir ali não cria pré-requisito aqui
- **Invariante de saúde:** o rollback usa `scripts/healthcheck.sh` desta trilha, que depende de `curl` em `/actuator/health` retornando HTTP 200 e do literal `"status":"UP"`. Se qualquer Issue ligar `REDIS_ENABLED=true` sem Redis alcançável, `/actuator/health` responde 503 e o rollback entra em loop — manter o healthcheck do Redis acoplado a `service_healthy`
- **Invariante de porta:** se a Issue 04 tornou `8080` interna, o healthcheck precisa apontar para o upstream correto; `PORT`, o `EXPOSE` do `Dockerfile` e `server.port` devem continuar coerentes entre si
- A chave efêmera depende do acesso por chave estabelecido na Issue 03
- `permitAll` restrito a `/actuator/health` e `/actuator/prometheus` (healthcheck e scraping); os demais endpoints do actuator ficam inalcançáveis pelo proxy público
