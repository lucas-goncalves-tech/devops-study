---
aliases: [dicionario, medicao, performance, metricas]
tags: [reference, performance, observability]
---

# Dicionário de medição — capacidade e performance

> **Contrato, não aula.** Este arquivo diz *qual* métrica responde *qual* pergunta, e de onde
> ela vem. O passo a passo de cada ferramenta vive em `commerce-api/estudos/`, nunca aqui.

A sequência complementar `09 → 13` do `commerce-api` mede performance com este vocabulário. As
cinco Issues o referenciam; a coluna `Origem` é o que separa o que já existe do que a sequência
cria, e por isso nenhuma Issue anterior precisa ser reescrita para acomodá-la.

## Camadas de medição

| Métrica | Pergunta que responde | Fonte | Origem |
|---|---|---|---|
| `http_req_duration` p50/p95/p99 | quanto tempo o usuário espera | k6 | `commerce 05` |
| `http_req_failed` | quanto da carga quebra | k6 | `commerce 05` |
| pool active / idle / waiting | o banco está afogado? | `prom-client` sobre `postgres` | `commerce 05` |
| event loop lag p99 | o Node bloqueia ou o banco é lento? | `node:perf_hooks` → `prom-client` | novo |
| calls por query | existe N+1? | `pg_stat_statements` | novo |
| tempo total por query | qual query custa o quê? | `pg_stat_statements` | novo |
| buffers / rows / tempo por query | por que a query é cara? | `EXPLAIN (ANALYZE, BUFFERS)` | novo |
| lock wait em `products` | há contenção de escrita? | `pg_stat_activity` + `pg_locks` | novo |

As três primeiras linhas nascem na [Issue 05](../../commerce-api/issues/05-observability.md), que
publica `/metrics` com `prom-client` e instala a carga k6 com thresholds. As cinco seguintes são o
que a sequência `09 → 13` cria, e cada uma responde a uma pergunta que nenhuma métrica existente
responde.

## Invariante de disjunção

**Se o event loop lag sobe junto com o p95, o Node bloqueia; se fica baixo enquanto o p95 sobe, o
problema é do banco.**

Essa disjunção é o que separa um relatório de performance de um palpite, e é o que a
[Issue 11](../../commerce-api/issues/11-localizacao-do-gargalo.md) tem de aplicar e registrar. Sem
ela, "rodei k6 e o p95 subiu" não diz se Increasinga o servidor ou Increasing banco — e otimizar a
camada errada custa tempo e não devolve nada.

Três fatos do código que as medições vão encontrar, registrados aqui para que a leitura não precise
abrir o repositório:

- O pool é curto: `max: 10` e `connect_timeout: 10` saem de
  `commerce-api/app/src/db/connection.ts:6-10`. Com carga alta, a fila de pendentes cresce e o
  tempo de espera vira erro.
- O `checkout` emite **14 round-trips serializados** por carrinho de 3 itens
  (`commerce-api/app/src/modules/orders/orders.service.ts:22-114`): 1 `SELECT` de idempotência,
  3 `SELECT products` um a um em `await` sequencial, 1 `INSERT orders` e 9 nas baixas, itens e
  auditoria. A latência cresce linearmente com o tamanho do carrinho.
- A baixa de estoque (`orders.service.ts:80`) pega lock de linha em `products`, retido até o commit
  da transação inteira. Duas compras simultâneas do mesmo produto travam uma na outra — e lock wait
  não aparece em nenhuma das métricas de latência, só na de contenção.
