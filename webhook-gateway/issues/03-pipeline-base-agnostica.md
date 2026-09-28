---
aliases: [issue-03, pipeline-base-agnostica]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 03 — Pipeline base agnóstica com build e teste em scripts locais que o workflow só chama

## Contexto

Pipeline de conteúdo costuma virar um script Bash gigante dentro do YAML: `npm ci` colado no passo, `npm test` colado no passo seguinte, e a mesma sequência reescrita à mão quando alguém troca de provedor de CI ou roda o processo numa máquina limpa. Aí a prova de que o build funciona é a pipeline verde, e ninguém consegue reproduzir localmente o que ela fez. Este app tem `package.json` com `build` (`tsc`), `test` (`vitest run`) e 9 testes em `tests/`, e um Dockerfile que só existe depois da Issue 02 — mas nada disso está escrito como comando reproduzível fora do CI. Esta Issue é a base de todas as gates (04 a 08): sem ela, cada gate precisa reescrever o próprio "como construir e testar este app".

## Objetivo

Estado final: a sequência de build e teste mora em scripts versionados que rodam em qualquer shell Linux, com código de saída confiável; o workflow da pipeline não tem passo de build ou teste próprio, apenas chama os scripts; e o mesmo script roda sem reescrita no runner da GitHub e num runner Linux genérico.

## Dependências

- Requer Issue 02 — o `Dockerfile` deste app nasce ali, e o script de build de imagem que a pipeline chama depende dele

## Escopo

- Scripts versionados de dependência, build, teste e (opcionalmente) build de imagem
- Código de saída não zero quando build ou teste falha, propagado pelo script
- Workflow da pipeline contendo apenas a preparação de ambiente e a chamada dos scripts
- Prova de execução do mesmo script em runner da GitHub e em runner Linux genérico
- Instrução de uso local no próprio script ou no README do app

## Fora de escopo

- Gates de conteúdo (segredos, SAST, SCA) — Issues 04, 05, 06 e 08
- Endurecimento de permissões e pinagem de ação por SHA — Issue 07
- Deploy e ambientes — [Issue 07 do `ledger-service`](../../ledger-service/issues/07-cicd-vps-deploy.md)
- Multiplicar provedores de CI: a agnosticidade é demonstrada, não é obrigação de manter três integrações
- Kubernetes, Terraform e cloud

## Conhecimentos envolvidos

- Scripts de shell com `set -euo pipefail`, código de saída e uso de `exec`
- `npm ci` contra `package-lock.json`, `tsc` e `vitest run`
- Anatomia de um workflow: gatilho, checkout, cache, permissões e passos
- Cache de dependências e o que invalidar quando o lockfile muda
- O contrato de "o script é a verdade": pipeline chama, não reimplementa

## Estado atual

- A suíte existe (`vitest run` com 9 testes em `tests/`, `ioredis` mockado) e roda com `npm test` a partir de `webhook-gateway/app/`
- A lógica de build e teste está no `package.json`; nada disso está em script executável fora do CI
- Não há workflow da GitHub Action deste app — a pipeline deste repositório hoje é a do `ledger-service`, com um job `build` sem passos
- Não existe `Dockerfile` neste app ainda — a imagem é trabalho da Issue 02

## Resultado esperado

- Um comando local reproduz o que a pipeline faz, com o mesmo código de saída
- O workflow da pipeline não repete nenhum comando de build ou teste
- O mesmo script roda em dois runners diferentes sem edição

## Requisitos

- [ ] Criar script de dependência + teste (no estilo `scripts/test.sh`) que rode a partir de `webhook-gateway/app/` sem GitHub, sem Docker e sem rede além do registro npm
- [ ] Criar script de build (`scripts/build.sh`) que produza `dist/` a partir do mesmo lockfile
- [ ] Garantir código de saída não zero em falha de dependência, build ou teste, propagado pelo próprio script
- [ ] Tornar os scripts idempotentes e repetíveis: duas execuções seguidas dão o mesmo resultado
- [ ] Escrever o workflow da pipeline do app contendo apenas checkout, versão do Node, cache e chamada aos scripts, sem `npm ci`/`npm test`/`tsc` em linha
- [ ] Executar o mesmo script de teste no runner da GitHub e num runner Linux genérico, colando as duas saídas lado a lado
- [ ] Registrar no README do app (ou no próprio script) o comando local equivalente à pipeline

## Critérios de aceitação

- [ ] `scripts/test.sh` roda numa máquina Linux limpa com Node 20 e `npm` disponíveis, sem nenhum serviço de CI, e termina com 0
- [ ] Quebrar um teste propositalmente faz o script sair com código não zero, e a pipeline reprova no mesmo passo que chamou o script
- [ ] Nenhum passo do workflow contém comando de build ou teste digitado diretamente: só a chamada ao script
- [ ] A saída do mesmo `scripts/test.sh` no runner da GitHub e no runner Linux genérico é idêntica no resultado (mesmo número de testes passando, mesmo código de saída) e o script não foi editado entre as execuções
- [ ] O script falha de forma não silenciosa quando uma dependência não está instalada ou o lockfile está fora de sincronia com o `package.json`
- [ ] O README do app (ou o cabeçalho do script) traz o comando local que reproduz a pipeline

## Validação

- Executar o script de teste na máquina local e conferir o código de saída
- **Build to break:** introduzir um teste que falha (ou renomear um arquivo importado) e confirmar reprovação local e reprovação na pipeline, com o passo responsável identificado
- Ler o workflow procurando comando de build ou teste em linha e confirmar que não existe
- Executar o mesmo script em runner Linux genérico (container, VM ou `act`) e colar as duas saídas
- Alterar o `package.json` sem atualizar o `package-lock.json` e confirmar que o script falha com mensagem clara
- Seguir as instruções do README e reproduzir a pipeline localmente

## Evidências

- Saída local de `scripts/test.sh` com o resumo do `vitest run` e o código de saída
- Execução da pipeline com o passo que chamou o script e o resultado
- Execução quebrada com o teste forçado a falhar
- Saídas lado a lado do mesmo script nos dois runners
- Conteúdo do workflow mostrando apenas a chamada ao script
- Trecho do README com o comando local

## Limitações / notas

- "Idêntica" no critério de aceitação significa mesmo resultado e mesmo código de saída, não string por string: runner pode diferir em versão de glibc ou na cor da saída de uma ferramenta
- O cache de dependências da pipeline é a principal fonte de divergência entre o runner e a máquina local: cache velho com lockfile novo reprova de forma enganosa e o script tem de detectar isso
- Agnosticidade não é portabilidade absoluta: os scripts assumem Linux com Bash, o que cobre GitHub, GitLab, uma VPS e a maior parte dos runners genéricos; Windows nativo fica fora e isso precisa estar escrito
- Se o workflow precisar de um passo que não cabe em script (publicar artefato, comentar em PR), esse passo continua no YAML — a regra é que build e teste não sejam reimplementados lá
- Esta Issue não prova que os gates funcionam; ela prova que existe um lugar único onde eles se encaixam, o que é o que as Issues 04 a 08 assumem
