---
aliases: [issue-11, gargalo, profiling, pg-stat-statements]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 11 — Localização do gargalo com veredito sustentado por número

## Contexto

A [Issue 10](10-carga-em-rampa.md) diz até onde a API aguenta, mas não diz por que ela para ali.
Latência alta pode ser o Node bloqueado, o banco saturado, contenção de lock numa linha quente ou
contenção de disco — e a otimização certa para uma dessas causas não faz nada pelas outras.
Três Serviços de medição já coletam latência, taxa de falha e fila do pool, e nenhum deles distingue
essas causas. O resultado hoje é otimizar no escuro, em cima de palpite.

## Objetivo

Estado final: um **veredito escrito nomeando o recurso que satura primeiro**, sustentado por
`pg_stat_statements`, `EXPLAIN (ANALYZE, BUFFERS)`, waits de lock e a disjunção do event loop lag.

## Dependências

- Requer [Issue 10 — Carga em rampa](10-carga-em-rampa.md): o ponto de inflexão registrado por ela
  é o cenário onde o perfil é feito. Sem o limite conhecido, qualquer profiling mede o sistema em
  um patamar arbitrário e não diz nada.

## Escopo

- `pg_stat_statements` nomeando a query com maior **tempo total acumulado** e a query com maior
  **número de chamadas**, com os dois números
- `EXPLAIN (ANALYZE, BUFFERS)` na query do caminho crítico, com buffers, rows e tempo real
- Contagem de waits de lock em `products` durante a janela do ponto de inflexão, via
  `pg_stat_activity` e `pg_locks`
- Aplicação da disjunção do event loop lag: se subiu com o p95, o Node bloqueia; se ficou baixo
  enquanto o p95 subiu, é o banco
- **O veredito**, nomeando um recurso

O vocabulário de medição está em
[`docs/performance/dicionario-de-medicao.md`](../../docs/performance/dicionario-de-medicao.md).

## Fora de escopo

- **Alterar código, schema ou índice.** Esta Issue diagnostica; a mudança é da
  [Issue 12](12-correcao-com-prova.md).
- **Escolher a correção.** Nomear a causa não é escolher o conserto.
- APM, tracing distribuído, Jaeger e OpenTelemetry Collector.
- Chaos engineering e injeção de falha.
- Coleta e dashboards em si — [Issue 05](05-observability.md).

## Conhecimentos envolvidos

- `pg_stat_statements`: o que são `calls`, `total_exec_time` e `rows`, e por que os dois números
  juntos revelam N+1
- Leitura de plano de execução: `EXPLAIN (ANALYZE, BUFFERS)`, o que é `Seq Scan` contra índice, e
  por que `rows` previsto muito diferente de `rows` real é o sinal de estatística desatualizada
- Lock no Postgres: `pg_locks`, `pg_stat_activity` e o que distingue espera por lock de espera por
  I/O
- A disjunção entre event loop do Node e latência do banco

## Estado atual

Nenhuma das três ferramentas está habilitada. Não há `pg_stat_statements` no Postgres do Compose,
não há `EXPLAIN` registrado em lugar nenhum, e ninguém olhou para waits de lock. O `/metrics` criado
pela Issue 05 ganhou event loop lag com a Issue 10, mas ele ainda não foi aplicado contra nada. O
`checkout` emite 14 round-trips serializados por carrinho de 3 itens
(`src/modules/orders/orders.service.ts:22-114`) e faz um `SELECT products` por item em `await`
sequencial — hipótese forte, ainda não um número.

## Resultado esperado

- A query mais cara nomeada em número, não em adjetivo
- O plano de execução da query do caminho crítico
- A contagem de waits de lock da janela
- A disjunção aplicada e escrita
- **O veredito, nomeando um recurso**

## Requisitos

- [ ] Habilitar a coleta de estatística de consulta no Postgres do Compose
- [ ] Nomear a query com maior **tempo total** e, separadamente, a query com maior **número de
      chamadas**, com os dois números registrados
- [ ] Registrar o `EXPLAIN (ANALYZE, BUFFERS)` da query do caminho crítico
- [ ] Registrar a contagem de waits de lock em `products` durante a janela do ponto de inflexão
- [ ] Aplicar e escrever a disjunção do event loop lag, escolhendo entre Node e banco
- [ ] **Escrever o veredito nomeando um recurso** — o que satura primeiro
- [ ] Se a hipótese da Issue 10 for contrariada pelos dados, registrar o erro de hipótese
      explicitamente

## Critérios de aceitação

- [ ] O veredito nomeia **um** recurso, com o número que sustenta a nomeação
- [ ] Os dois números de `pg_stat_statements` estão registrados: tempo total e contagem de chamadas
- [ ] A disjunção do event loop lag está aplicada e escrita, não apenas mencionada
- [ ] Se a hipótese da Issue 10 (pool esgotado) for contrariada, o erro de hipótese está registrado
      na Issue, e não omitido
- [ ] Nenhum código, schema ou índice foi alterado

## Validação

- Rodar a carga no ponto de inflexão da Issue 10 com a coleta habilitada e conferir que a janela
  consultada corresponde à janela da carga
- Reexecutar as consultas de `pg_stat_statements` e de lock fora da janela, para confirmar que o que
  foi registrado pertence à carga e não ao tráfego de fundo
- Um terceiro que lê o veredito e os números chega à mesma conclusão sobre qual recurso satura

## Evidências

- A saída de `pg_stat_statements` com tempo total e contagem de chamadas
- O `EXPLAIN (ANALYZE, BUFFERS)` da query do caminho crítico
- A contagem de waits de lock na janela
- A série de event loop lag confrontada com a de p95
- O veredito, escrito, nomeando o recurso

## Limitações / notas

- **Habilitar `pg_stat_statements` exige `shared_preload_libraries` e reinício do Postgres.** Isso
  é configuração do Compose, que é construção do usuário. Se a extensão não estiver disponível no
  ambiente, registre a dependência aqui e **não** a converta em evidência: a validação só fecha com
  os números reais, conforme a política de `00-visao-geral.md`.
- `EXPLAIN ANALYZE` executa a query de verdade. Rodar no caminho crítico em produção altera estado
  em queries de escrita, então o perfil é feito no ambiente de laboratório da Issue 02, no ponto
  de inflexão da Issue 10.
- Estatísticas do Postgres envelhecem. Se `rows` previsto e `rows` real divergirem muito, o plano
  ótimo para os dados pode não ser o ótimo para a massa de dados atual — isso é um achado da Issue,
  não uma falha dela.
- Lock wait não aparece em nenhuma métrica de latência. Uma carga pode ter p95 aceitável e perder
  horas em espera de lock, e só a contagem de `pg_locks` mostra isso.
- Se o veredito sair "o banco" de forma genérica, a Issue não cumpriu o objetivo: o recurso tem nome.
