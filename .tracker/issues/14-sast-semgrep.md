---
aliases: [issue-14, sast-semgrep]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 14 — Análise estática com gate bloqueante para severidade ERROR

## Contexto

Padrão inseguro no código Java só é pego em revisão humana — ou nunca. Injeção, hardcode de credencial e uso inseguro de criptografia passam até alguém atento olhar, e a revisão não escala.

## Objetivo

Estado final: scanner SAST rodando como gate obrigatório na pipeline, bloqueando apenas severidade `ERROR`, com zero violação pendente no código atual.

## Dependências

- Requer Issue 10 — o gate precisa de uma pipeline onde se integrar

## Escopo

- Scanner SAST com regras focadas em Java
- Modo bloqueante restrito a severidade `ERROR`
- Integração como gate obrigatório de pull request
- Zeramento das violações existentes

## Fora de escopo

- SCA (dependências e CVEs) — Issue 15
- Detecção de segredos — Issue 13
- Correção de arquitetura do código além das violações apontadas
- Kubernetes, Cloud e Terraform

## Conhecimentos envolvidos

- Análise estática e shift-left
- Diferença entre SAST, SCA e scan de segredos
- Regras, severidades e exceções de scanner

## Estado atual

- Padrão inseguro só aparece em revisão ou nunca aparece
- Nenhum gate de análise estática na pipeline
- Backlog de violação desconhecido

## Resultado esperado

- Pull request com padrão mapeado falha sozinho, sem intervenção humana
- Nenhuma violação `ERROR` aberta sem justificativa registrada
- Regras bloqueantes ativas e limitadas a `ERROR`

## Requisitos

- [ ] Adotar scanner SAST com regras focadas em Java
- [ ] Ativar modo bloqueante apenas para severidade `ERROR`
- [ ] Integrar à pipeline como gate obrigatório
- [ ] Zerar violações `ERROR` ou registrar cada uma como exceção justificada
- [ ] Documentar o procedimento para adicionar regra sem quebrar a pipeline sem motivo

## Critérios de aceitação

- [ ] O gate de análise estática roda em todo pull request e é obrigatório
- [ ] Um padrão conhecido como `ERROR` introduzido no código derruba a pipeline
- [ ] Nenhuma violação `ERROR` existe sem exceção registrada e justificada
- [ ] Somente severidade `ERROR` bloqueia — `WARNING` não impede o merge

## Validação

- Introduzir deliberadamente um padrão mapeado como `ERROR` e confirmar a falha do gate; reverter em seguida
- Rodar o scanner sobre o código atual e revisar a lista de violações pendentes
- Confirmar que nenhuma severidade abaixo de `ERROR` reprova a pipeline

## Evidências

- URL da pipeline falhando sobre o padrão introduzido
- Relatório do scanner sobre o código atual sem violação `ERROR` pendente
- Configuração do gate mostrando o nível bloqueante
- Registro das exceções justificadas, se houver

## Limitações / notas

- Começar bloqueando só `ERROR` é deliberado: um gate barulhento vira ruído e é desativado
- Dívida de código detectada aqui pode exigir alteração em classes de domínio — se o escopo da Issue não cobrir, registrar como dívida em vez de afrouxar o gate
- Esta Issue cria o gate de SAST que a Issue 16 reaproveita e que a Issue 17 exige como pré-requisito de deploy
