---
aliases: [issue-13, secrets-hygiene]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 13 — Higiene de segredos com detecção bloqueante no fluxo de merge

## Contexto

Não existe detecção de segredo versionado: uma chave colada num commit passa despercebida até virar incidente. `.env` já é ignorado, mas não há nada que impeça alguém de commitar uma credencial em outro arquivo — e o `JWT_SECRET` default está literalmente no repositório.

## Objetivo

Estado final: scanner de segredos no pré-commit e na pipeline, baseline de achados legítimos, e merge bloqueado quando um segredo novo é detectado.

## Dependências

- Requer Issue 10 — o bloqueio de merge exige uma pipeline que rode em pull request

## Escopo

- Scanner de segredos no pré-commit e na pipeline
- Baseline separando falso positivo de vazamento real
- Bloqueio de merge sobre novo segredo
- Higienização dos arquivos de exemplo e documentação do fluxo

## Fora de escopo

- SAST e SCA — Issues 14 e 15
- Endurecimento de permissões e pinagem de ações — Issue 15
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

- O `JWT_SECRET` default em `application.yml` é um literal commitado; tratar como dívida conhecida — substituir por valor de ambiente sem quebrar os testes, que usam perfil próprio
- `.env` é ignorado por `.gitignore` e excluído da imagem por `.dockerignore` — nenhum dos dois pode ser removido
- O bloqueio de merge depende de proteção de branch configurada na Issue 10; sem ela, o gate roda mas não impede nada
- Esta Issue cria o gate de segredos que a Issue 16 reaproveita e que a Issue 17 exige como pré-requisito de deploy
