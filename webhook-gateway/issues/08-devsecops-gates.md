---
aliases: [issue-08, devsecops-gates]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 08 — Gates de segredos, SAST e SCA consolidados como barreira única da pipeline

## Contexto

Os gates de conteúdo (segredos, SAST, SCA) nascem isolados, cada um com seu próprio arquivo e sua própria severidade. Consolidá-los numa barreira única evita a dúvida de "qual foi o problema?" no merge: cada gate precisa falhar de forma isolada, com mensagem acionável, e o verde da pipeline precisa significar as três coisas ao mesmo tempo.

O app auditado aqui é o `webhook-gateway` (Node 20 + TypeScript, `ioredis`): o SAST roda com a família de regras `p/javascript` (Issue 05) e o SCA sobre `package-lock.json` mais a imagem (Issue 06).

## Objetivo

Estado final: os três gates (segredos, SAST, SCA) rodam em toda mudança, falham isoladamente com mensagem acionável, e o verde significa ausência de segredo, ausência de violação `ERROR` e ausência de CVE alta ou crítica — com o custo em minutos de cada gate medido.

## Dependências

- Requer Issue 04 — gate de segredos
- Requer Issue 05 — gate de SAST
- Requer Issue 07 — permissões mínimas e pinagem da pipeline que os hospeda
- Requer Issue 03 — pipeline base
- Requer Issue 06 — gate de SCA por scan de imagem

## Escopo

- Três gates obrigatórios: segredos, SAST e SCA
- Falha isolada por gate com mensagem acionável
- Separação entre severidade que falha e severidade que só alerta
- Medição de tempo adicionado por gate e otimização com cache

## Fora de escopo

- Criação dos gates individuais — Issues 04, 05, 06 e 07
- Deploy contínuo — [Issue 07 do `ledger-service`](../../ledger-service/issues/07-cicd-vps-deploy.md)
- Monitoramento da aplicação — [Issue 05 do `commerce-api`](../../commerce-api/issues/05-observability.md)
- Kubernetes — fora de escopo por decisão (arquivado no repositório)
- Cloud — [Issue 07 do `commerce-api`](../../commerce-api/issues/07-aws-production.md)

## Conhecimentos envolvidos

- DevSecOps e posicionamento de gates no fluxo
- SCA: CVEs, dependências e triagem de ruído
- Severidade vs política de bloqueio
- Custo de pipeline e cache

## Estado atual

- `.github/workflows/CI.yml` tem um job `build` sem `steps` — não há gate de conteúdo rodando
- Os gates de segredos, SAST e SCA ainda não existem; cada um vem da sua Issue (04, 05, 06) e esta Issue os consolida depois
- Ninguém sabe quanto tempo cada gate adiciona

## Resultado esperado

- Cada gate falha isolado com mensagem acionável
- Verde significa: sem segredo, sem violação `ERROR`, sem CVE alta ou crítica
- Tempo de execução por gate conhecido e cache aplicado onde couber

## Requisitos

- [ ] Garantir o gate de segredos bloqueando vazamento em toda mudança
- [ ] Garantir o gate de SAST bloqueando padrões inseguros
- [ ] Garantir o gate de SCA bloqueando CVEs altas e críticas
- [ ] Garantir que cada gate falha isolado, com mensagem acionável
- [ ] Diferenciar severidade que falha de severidade que só alerta
- [ ] Medir o tempo adicionado por gate e otimizar com cache

## Critérios de aceitação

- [ ] Uma mudança com segredo, uma com violação `ERROR` e uma com CVE crítica reprovam a pipeline em jobs distintos e identificáveis
- [ ] Um problema em um gate não mascara a execução dos outros
- [ ] Pipeline verde implica ausência de segredo, de violação `ERROR` e de CVE alta ou crítica
- [ ] O tempo de execução de cada gate é medido e registrado

## Validação

- **Build to break:** introduzir separadamente segredo, padrão `ERROR` e dependência com CVE crítica, confirmando falha em cada caso; reverter
- Verificar que os jobs permanecem independentes na saída
- Conferir a saída da pipeline verde com os três gates ativos
- Ler a medição de tempo por gate

## Evidências

- Três execuções falhando, uma por gate, com o job responsável identificado
- Saída da pipeline verde com os três gates
- Registro dos tempos de execução por gate

## Limitações / notas

- Os gates individuais vêm prontos das Issues 04, 05, 06 e 07 — esta Issue consolida e sintoniza, não recria
- Sintonizar para baixo a severidade bloqueante sem registrá-lo transforma o verde em ilusão: qualquer afrouxamento deve aparecer como decisão registrada
- Os critérios de "mensagem acionável" e "severidade que alerta" precisam de artefato observável na saída do gate, não de descrição
- A pipeline cobre os serviços já existentes no momento em que ela é construída; a stack multi-serviço chega na Issue 10, e os gates continuam valendo sem mudança de desenho
- Esta Issue é pré-requisito da [Issue 07 do `ledger-service`](../../ledger-service/issues/07-cicd-vps-deploy.md), que exige os gates como barreira antes do deploy — gate verde aqui é o que autoriza aquele deploy, mas o job é deste app
