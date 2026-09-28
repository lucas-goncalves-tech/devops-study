---
aliases: [issue-04, secrets-hygiene]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 04 — Higiene de segredos com detecção bloqueante no fluxo de merge

## Contexto

Não existe detecção de segredo versionado: uma chave colada num commit passa despercebida até virar incidente. `.env` já é ignorado, mas não há nada que impeça alguém de commitar uma credencial em outro arquivo — e o `WEBHOOK_SECRET` default está literalmente em `webhook-gateway/app/src/index.ts`. O app também lê a configuração de `.env` via `dotenv.config()`, então o arquivo de exemplo precisa nascer com placeholders, não com valor.

## Objetivo

Estado final: scanner de segredos no pré-commit e na pipeline, baseline de achados legítimos, e merge bloqueado quando um segredo novo é detectado.

## Dependências

- Requer Issue 03 — o bloqueio de merge exige uma pipeline que rode em pull request

## Escopo

- Scanner de segredos no pré-commit e na pipeline
- Baseline separando falso positivo de vazamento real
- Bloqueio de merge sobre novo segredo
- Higienização dos arquivos de exemplo e documentação do fluxo

## Fora de escopo

- SAST e SCA — Issues 05 e 06
- Endurecimento de permissões e pinagem de ações — Issue 07
- Rotação de credenciais já vazadas — operação, não esta Issue
- Kubernetes, Cloud e Terraform

## Conhecimentos envolvidos

- Gestão e higiene de segredos em repositórios
- Segredos do GitHub Actions
- Baselines de scanner e triagem de falso positivo

## Estado atual

- Nenhum scanner de segredo no repositório nem na pipeline
- Sem baseline: não há como distinguir achado legítimo de vazamento
- `.env` é ignorado, mas nada barra outro arquivo com credencial

## Resultado esperado

- Commit com chave mock é barrado automaticamente
- Baseline existe e separa falso positivo de vazamento real
- Pull request com segredo novo não pode ser mergeado
- Arquivos de exemplo não contêm valor real

## Requisitos

- [ ] Adotar scanner de segredos no pré-commit e na pipeline
- [ ] Criar baseline de achados legítimos vs vazamento real
- [ ] Bloquear merge se novo segredo for detectado
- [ ] Higienizar variáveis sensíveis nos arquivos de exemplo
- [ ] Documentar o fluxo: exemplo versionado, valor real só via ambiente ou secret

## Critérios de aceitação

- [ ] Commit contendo uma chave de teste formato credencial é recusado pelo scanner
- [ ] A baseline existe no repositório e o pipeline passa sobre o código atual sem falso positivo pendente
- [ ] Pull request com segredo novo é reprovado e o merge fica bloqueado
- [ ] Nenhum arquivo versionado contém valor de segredo real — apenas placeholders

## Validação

- Inserir uma chave sintética num commit e confirmar a recusa
- Rodar o scanner sobre o repositório limpo e confirmar zero achado fora da baseline
- Abrir pull request com segredo introduzido e confirmar o bloqueio de merge
- Varredura dos arquivos de exemplo confirmando apenas placeholders

## Evidências

- Log do scanner recusando o commit com chave sintética
- Arquivo de baseline versionado
- URL do pull request reprovado pelo gate
- Varredura dos arquivos de exemplo

## Limitações / notas

- O `WEBHOOK_SECRET` default em `webhook-gateway/app/src/index.ts` (`default-webhook-secret-key-32chars`) é um literal commitado; tratar como dívida conhecida — substituir por valor de ambiente sem quebrar a suíte, que injeta o segredo pelo construtor de `StreamConsumer`
- O `JWT_SECRET` default do `ledger-service` (`application.yml`) é a mesma dívida no app Java — o escopo desta Issue é o app Node; o equivalente do ledger é trabalho de lá
- `.env` é ignorado por `.gitignore`; o `.dockerignore` que o exclui da imagem só existe quando a imagem existir (Issue 02) — nenhum dos dois pode ser removido
- Não há `.env.example` neste app ainda: ele nasce junto com a imagem (Issue 02) — este é o momento barato de não vazar nada
- O bloqueio de merge depende de proteção de branch configurada na Issue 03; sem ela, o gate roda mas não impede nada
- Esta Issue cria o gate de segredos que a Issue 08 reaproveita e que a [Issue 07 do `ledger-service`](../../ledger-service/issues/07-cicd-vps-deploy.md) exige como pré-requisito de deploy
