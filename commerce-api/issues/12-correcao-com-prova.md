---
aliases: [issue-12, correcao, otimizacao, prova-de-efeito]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 12 — Correção do gargalo com o mesmo script re-executado e delta lado a lado

## Contexto

A [Issue 11](11-localizacao-do-gargalo.md) devolve um diagnóstico, não um conserto. E a optimize
sem re-medir é a forma mais comum de performance não acontecer: a mudança entra, o número melhora
um pouco, ninguém sabe se melhorou por causa dela ou porque a máquina estava mais livre naquele dia,
e a próxima alteração passa a ser um chute em cima de um chute. Sem o mesmo script re-executado e o
delta colado lado a lado, qualquer ganho é intestimável.

## Objetivo

Estado final: **uma** mudança de maior efeito, aplicada sobre o veredito da Issue 11, com o mesmo
script da [Issue 10](10-carga-em-rampa.md) re-executado sem parâmetro alterado e as duas saídas
comparadas lado a lado.

## Dependências

- Requer [Issue 11 — Localização do gargalo](11-localizacao-do-gargalo.md): a mudança a ser aplicada
  é escolhida pelo veredito, não por preferência. Sem o recurso nomeado, qualquer alteração é chute.
- Requer [Issue 10 — Carga em rampa](10-carga-em-rampa.md): a re-execução usa o **mesmo** script
  daquela Issue, com o mesmo perfil de estágios e o mesmo critério de falha.

## Escopo

- **Uma** mudança de efeito máximo sobre o recurso nomeado no veredito da Issue 11
- Re-execução do script da Issue 10, sem nenhum parâmetro alterado
- Comparação das duas execuções lado a lado: p95, vazão e event loop lag
- A afirmação de qual mudança causou o delta

Para o veredito que este schema produz, os candidatos mais prováveis — nomeados aqui como
possibilidade, **não** como escopo decidido — são a troca dos N `SELECT products` em `await`
sequencial por uma única consulta com `id = ANY(...)`
(`src/modules/orders/orders.service.ts:40-61`), o tratamento da contenção de estoque no
`UPDATE products` que pega lock de linha (`:80`), e a **guarda de estoque** que hoje não existe: um
`UPDATE` condicional com `WHERE stock_quantity >= ${quantity}`, ou `SELECT ... FOR UPDATE` na
validação, ou `CHECK (stock_quantity >= 0)` em `schema.ts:36`. As três resolvem o mesmo defeito e
escolher entre elas é o que o veredito da Issue 11 decide.

O vocabulário de medição está em
[`docs/performance/dicionario-de-medicao.md`](../../docs/performance/dicionario-de-medicao.md).

## Fora de escopo

- **Mais de uma mudança na mesma rodada.** Duas mudanças simultâneas produzem um número melhor que
  não atribui causa a nada, e invalidam a medição.
- Otimização de segunda ordem, mesmo que pareça óbvia.
- Refactor sem medição: reorganizar código por legibilidade é outra Issue, com outro critério.
- Escolher o conserto — isso foi a Issue 11.
- Medir de novo parafraseando o critério: o script é o da Issue 10, não um script novo.

## Conhecimentos envolvidos

- Melhoria de ponto de inflexão: a métrica que importa é onde a curva se move, não o p95 de um
  patamar solto
- Comparação de execuções: o que precisa estar igual para o delta ser atribuível
- Idempotência transacional: por que otimizar o caminho de escrita pode duplicar pedido
- Contenção de lock e as duas correções possíveis: evitar a contenção ou tolerá-la com retry
- Guarda de concorrência: `UPDATE` condicional, `SELECT ... FOR UPDATE` e `CHECK`, e o que cada uma
  resolve e o que deixa de resolver

## Estado atual

Nenhuma otimização foi aplicada, nenhuma mudança foi medida e não existe um único exemplo de
antes e depois neste repositório. O `checkout` faz um `SELECT products` por item em `await`
sequencial dentro de uma transação (`:40-61`) e faz a baixa de estoque com `UPDATE` que pega lock
de linha (`:80`) — duas hipóteses com a mesma assinatura, e nenhuma delas medidas. O pool está
configurado em `max: 10` com `connect_timeout: 10` em `src/db/connection.ts:6-10`.

## Resultado esperado

- Uma mudança aplicada e nomeada
- As duas execuções do mesmo script, lado a lado
- O delta em número, nos três eixos: p95, vazão e event loop lag
- A afirmação de que a mudança causou o delta

## Requisitos

- [ ] Nomear e aplicar **exatamente uma** mudança, registrando qual é
- [ ] Reexecutar o script da Issue 10 sem alterar nenhum parâmetro, e registrar isso
- [ ] Colocar as duas saídas lado a lado na evidência
- [ ] Registrar o delta de p95, de vazão e de event loop lag, com sinal
- [ ] Afirmar qual mudança causou o delta, em número e não em adjetivo
- [ ] Conferir que `checkout` continua idempotente por `idempotencyKey` depois da mudança
- [ ] Conferir que não apareceu pedido duplicado nem estoque negativo no banco

## Critérios de aceitação

- [ ] As duas execuções estão na Issue, lado a lado, vindas do **mesmo** script
- [ ] Exatamente **uma** mudança foi aplicada, e ela está nomeada
- [ ] O delta está em número nos três eixos, com sinal
- [ ] **`checkout` continua idempotente por `idempotencyKey`**, e nenhum pedido duplicado apareceu
      depois da rodada
- [ ] Se a rodada anterior achou estoque negativo, **ele não aparece mais** depois da mudança — a
      correção de desempenho não pode conviver com o defeito de correção que a rampa revelou
- [ ] O delta move o **ponto de inflexão** registrado pela Issue 10, e não só o p95 de um patamar
      solto — é isso que separa uma correção real de um ganho de 1%
- [ ] `/health` continua respondendo `200` com `"UP"` e `503` com `"DEGRADED"`, e `/metrics`
      continua fora do `preHandler` de autenticação
- [ ] Nenhum erro 5xx de aplicação durante a re-execução

## Validação

- Rodar o script da Issue 10 na configuração original, depois na configuração corrigida, sem mudar
  nenhum parâmetro entre as execuções
- Conferir no banco, depois da rodada, que não há pedido duplicado nem estoque negativo
- Consultar `/health` e o log procurando `5xx` na janela da re-execução
- Um terceiro que compare as duas saídas chega à mesma conclusão sobre qual mudança causou o delta

## Evidências

- A saída do k6 antes da mudança
- A saída do k6 depois da mudança, do mesmo script
- As duas lado a lado, com o delta de p95, de vazão e de event loop lag
- A consulta ao banco confirmando ausência de duplicidade e de estoque negativo
- A saída de `/health` e o trecho de log sem 5xx

## Limitações / notas

- **"O p95 melhorou" não é evidência.** A política de `00-visao-geral.md` proíbe fechar Issue com
  limitação de ambiente no lugar de prova, e o mesmo vale para adjetivo: só fecha com as duas
  saídas coladas e o delta calculado.
- **Regra de uma variável por rodada.** Mudar duas coisas simultaneamente produz um número melhor
  que não atribui causa a nada. Se a primeira mudança for insuficiente, isso é uma segunda rodada
  com a primeira já medida — não uma rodada maior.
- **Otimização que derruba estoque ou duplica pedido está errada, mesmo com p95 melhor.** Os dois
  critérios são independentes e ambos obrigatórios: desempenho que quebra correção não é
  otimização, é troca de incidente.
- A alteração de código é construção do usuário — o agente propõe o diff e o usuário implementa. Os
  limites de escrita do monorepo não mudam por causa desta Issue.
- **A magnitude do delta é critério.** A spec de risco desta sequência é explícita: transformar o
  ajuste de bottleneck em tweak infinito, e **melhorar 1% não fecha a Issue**. Um ganho pequeno e não
  atribuído não é resultado — é ruído. O delta precisa mover o **ponto de inflexão** registrado pela
  [Issue 10](10-carga-em-rampa.md), e o critério de aceitação exige que isso apareça em número.
