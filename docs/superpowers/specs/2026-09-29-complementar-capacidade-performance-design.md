---
aliases: [spec, performance, capacidade, design]
tags: [spec, design, performance]
status: proposta
data: 2026-09-29
---

# Spec — Sequência complementar de capacidade e performance no `commerce-api`

> Decisão de design, não plano de execução. As Issues nascem da sequência `09 → 13` descrita aqui.
> Execução e Construction Steps ficam no plano de implementação, que é artefato separado.

## Problema

O vídeo que originou esta conversa mostra o cenário que falta no repositório: **mais de mil
usuários dentro, requisições batendo num servidor com reverse proxy, Node e Postgres, medindo
com k6**. O monorepo tem o instrumento, não tem o método.

- `commerce-api/issues/05-observability.md` roda k6 com **50–100 VUs e threshold fixo de 500ms**.
  Isso prova que *passou*, não *onde quebra*.
- `ledger-service/issues/08-trafego-sintetico-alertas.md` roda tráfego sintético com vazão baixa
  em produção e grava p95. Também não procura o limite.
- `commerce-api/issues/07-aws-production.md` cita "custo e right-sizing" numa linha.
- **Não existe nenhuma Issue de otimização.** Não há `EXPLAIN`, índice sob carga, event loop lag,
  `pg_stat_statements`, HPA nem ciclo baseline → mudança → comparação. Confirmado por busca em
  todo o repo.

Resultado: quem termina as 3 trilhas sabe *medir* e não sabe *melhorar*.

## Decisão de estrutura

**Sequência complementar `09 → 13` dentro do `commerce-api`.** Não é uma 4ª trilha e não é um app
novo.

Por que dentro do `commerce-api`:

- O gargalo que a sequência estuda **está no código desse app** (`src/modules/orders/orders.service.ts`),
  na mesma stack do vídeo: Node/Fastify/Postgres.
- A regra da casa é "cada app é um sistema, com sua trilha"
  ([`00-visao-geral.md`](../../00-visao-geral.md)). Criar `perf-lab/` para exercitar a
  `commerce-api` duplicaria Compose, Dockerfile, README e AGENTS sem ganhar uma única métrica.
- A trilha AWS `01 → 08` continua fechada e intocada. A numeração `09 → 13` é bloco próprio, com
  dependências declaradas em `## Dependências` de cada Issue, exatamente como o repo já faz.

Rejeitado:

| Alternativa | Por que não |
|---|---|
| `perf-lab/` como 4º app | Duplica infra para medir app alheio; contradiz "app = trilha" sem ganho |
| Engrossar `commerce 05` e `07` | Dilui duas Issues já grandes e mistura "medir" com "melhorar" |
| Pasta transversal `performance/` | Foge de "cada app é um sistema"; ninguém sabe onde marcar `[x]` |
| App novo, à la `archive/18` | O precedente existe mas é para orquestração, não para um gargalo que já está codado |

`archive/18-kubernetes-helm` continua sendo candidata a 4ª trilha e segue **fora de escopo**:
autoscaling e orquestração não entram aqui.

## Estado atual que fundamenta o desenho

`commerce-api/app/src/modules/orders/orders.service.ts:22-114` — `checkout` abre
`this.db.transaction()` e, para um carrinho de **3 itens**, emite:

| Passo | Queries | Local |
|---|---|---|
| idempotência | 1 `SELECT orders` | `:25-27` |
| validação de estoque | 3 `SELECT products`, um por item, **`await` sequencial** | `:40-61` |
| criação do pedido | 1 `INSERT orders` | `:65-72` |
| baixa + itens + auditoria | 3 × (`UPDATE products` + `INSERT order_items` + `INSERT audit_logs`) = 9 | `:77-107` |

**14 round-trips, todos serializados, dentro de uma transação aberta.** Três consequências, todas
verificáveis:

1. **N+1 sequencial.** O `for` da linha 40 não paraleliza — é `await` dentro de `await`. A latência
   do checkout cresce linearmente com o tamanho do carrinho.
2. **Contenção de escrita.** `UPDATE products SET stock_quantity = stock_quantity - X` (`:80`) pega
   lock de linha, retido até o commit da transação inteira. Duas compras simultâneas do mesmo
   produto travam uma na outra.
3. **Pool curto.** `src/db/connection.ts:6-10` declara `max: 10`, `connect_timeout: 10`. Com carga
   alta, a fila de pendentes cresce e o tempo de espera vira erro.

Nenhum desses três é hipótese. Todos produzem número, e é isso que a sequência existe para medir.

## Regra de medição (o dicionário que as 5 Issues compartilham)

Este é o artefato que responde "como se mede performance aqui". Fica versionado em
`docs/performance/dicionario-de-medicao.md` e é referenciado por todas as Issues da sequência —
é contrato, não aula.

| Métrica | Pergunta que responde | Fonte | Origem |
|---|---|---|---|
| `http_req_duration` p50/p95/p99 | quanto tempo o usuário espera | k6 | `commerce 05` |
| `http_req_failed` | quanto da carga quebra | k6 | `commerce 05` |
| pool active / idle / waiting | o banco está afogado? | `prom-client` sobre `postgres` | `commerce 05` |
| **event loop lag p99** | o Node bloqueia ou o banco é lento? | `node:perf_hooks` → `prom-client` | **novo** |
| **calls por query** | existe N+1? | `pg_stat_statements` | **novo** |
| **tempo total por query** | qual query custa o quê? | `pg_stat_statements` | **novo** |
| **buffers / rows / tempo por query** | por que a query é cara? | `EXPLAIN (ANALYZE, BUFFERS)` | **novo** |
| **lock wait em `products`** | há contenção de escrita? | `pg_stat_activity` + `pg_locks` | **novo** |

Invariante do par das duas últimas: **se o event loop lag sobe junto com o p95, o Node bloqueia; se
fica baixo enquanto o p95 sobe, o problema é do banco.** Essa é a disjunção que separa um relatório
de performance de um palpite, e é o que a sequência treina a produzir.

## As 5 Issues

Todas no template fixo ([`00-visao-geral.md`](../../00-visao-geral.md)): RFC de problema, sem aula,
sem tutorial, sem sub-etapas. Material de estudo vai para `commerce-api/estudos/09..13-*.md`.

Cadeia de dependências: `05 → 09 → 10 → {11 → 12, 13}`.

### `09` Plano de capacidade

Problema: ninguém sabe quantos RPS o serviço aguenta, então dimensionamento é chute.

Entrega a conta antes do experimento: usuários concurrentes → requisições por usuário → RPS de pico
→ Little's law (concorrência = vazão × latência) → vCPU e memória → tipo de instância. Cada parcela
com a origem escrita, e o resultado como número que fecha.

Artefato: um documento com a conta e a premissa de cada parcela, mais a vazão-alvo que as Issues
seguintes vão testar. Sem medição aqui — esta Issue é aritmética, e sua validação é a conta fechar.

Depende de `05` (é contra a latência observada que a conta é conferida). Fora de escopo: provisionar
qualquer coisa.

### `10` Carga em rampa até degradação

Problema: threshold fixo de 500ms em 100 VUs prova aprovação, não localiza o limite.

Entrega: script k6 com `ramping-vus` subindo até o critério de falha, e a curva RPS × p95 × erros ×
**ponto de inflexão** registrado. É aqui que o `event loop lag` entra no `/metrics`.

Artefato: saída do k6 com o estágio em que o threshold quebrou, a vazão atingida nesse ponto, e a
série de event loop lag colada.

Depende de `09` (a rampa é dimensionada pela vazão-alvo) e de `05`. Fora de escopo: corrigir
qualquer coisa — esta Issue mede e nomeia o limite, não o conserta.

### `11` Localização do gargalo

Problema: saber que degrada não diz se é app, banco, lock ou I/O.

Entrega: no ponto de inflexão da `10`, `pg_stat_statements` nomeando a query mais cara em número de
chamadas **e** tempo total, `EXPLAIN (ANALYZE, BUFFERS)` no caminho crítico, correlação com waits de
lock em `products`, e a disjunção do event loop lag aplicada.

Artefato: **veredito escrito nomeando o recurso que satura primeiro**, sustentado pelos números.
Contra a hipótese da Issue anterior: se `10` previu pool e a causa for lock de linha, isso está
registrado como erro de hipótese, não escondido.

Depende de `10`. Fora de escopo: alterar código ou schema.

### `12` Correção com prova de efeito

Problema: `11` devolve diagnóstico, não conserto.

Entrega: a mudança de maior efeito identificada na `11` — para este schema, o candidato principal é
`SELECT products WHERE id = ANY(...)` em vez de N selects sequenciais, e o tratamento da contention
de estoque — aplicada, com **o mesmo script da `10` re-executado** e comparação lado a lado.

Regra dura: **uma variável por rodada.** Duas mudanças simultâneas produzem um número melhor que
não atribui causa a nada, e a rodada é inválida.

Artefato: saída da `10` e da re-execução lado a lado, com o delta de p95, de vazão e de event loop
lag, e a afirmação de qual mudança causou o delta.

Depende de `11`. Fora de escopo: otimização de segunda ordem, refactor sem medição.

### `13` Custo de sustentar o pico medido

Problema: `07` estima custo de infra provisionada; ninguém calculou o custo da vazão.

Entrega: a partir do ponto de inflexão da `10`, custo por RPS-sustentado por mês, e a checagem de
right-sizing — a instância que a `09` dimensionou aguenta o pico medido com folga, ou é
over-provision? Se for, a conta do overprovision em reais.

Artefato: a conta com a origem de cada preço e a conclusão de right-sizing, referenciando a trava de
custo de recurso pago que já vive em `07`.

Depende de `10`. Fora de escopo: provisionar, e FinOps como programa — segue a restrição de
[`00-visao-geral.md`](../../00-visao-geral.md) que mantém FinOps fora do escopo.

## Invariantes

O que nenhuma Issue da sequência pode quebrar:

- `/health` segue `200` `"UP"` com banco de pé e `503` `"DEGRADED"` sem ele. O `HEALTHCHECK` do
  `Dockerfile` só falha por código de saída, então sem `503` a orquestração não percebe queda.
- `/metrics` e `/health` seguem **fora** do `preHandler` de autenticação — a invariante já
  registrada em `commerce 05` continua valendo, e o event loop lag novo também é publicado ali.
- O `checkout` continua **idempotente** por `idempotencyKey`. Otimização que duplica pedido ou
  derruba estoque está errada, mesmo que melhore o p95.
- Toda Issue fecha com a saída real do comando de validação em `## Evidências`. "Deve melhorar" não
  é evidência; o delta colado lado a lado é.
- Custo zero: k6 roda como container contra o Compose da `02`, Postgres local. Nenhuma Issue da
  sequência exige VPS ou conta AWS.

## Impacto nos documentos existentes

Nenhum arquivo existente muda de identidade — a sequência é aditiva.

| Arquivo | Mudança |
|---|---|
| `BOARD.md` | nova subseção em `commerce-api`, com as 5 linhas e a cadeia de dependências |
| `00-visao-geral.md` | 3 linhas: a sequência complementar existe, é `09 → 13`, e `archive/18` segue candidata a 4ª trilha |
| `commerce-api/AGENTS.md` | tabela de Issues ganha o bloco `09–13`; "próxima a entrar" continua `01` |
| `commerce-api/README.md` | índice dos arquivos novos |
| `AGENTS.md` | `docs/` e `.superpowers/` movidos para gravável (feito) |
| `docs/performance/dicionario-de-medicao.md` | novo — o contrato de medição da tabela acima |

`ledger-service/08` e `commerce-api/05` **não são reescritas**. A `05` continua sendo o smoke test de
50–100 VUs com threshold; a `10` é a rampa que ele não faz. A distinção fica escrita nas duas, para
que a `05` não pareça redundante.

## Fora de escopo

- **Kubernetes, HPA, autoscaling** — `archive/18-kubernetes-helm` segue candidata a 4ª trilha.
- **CDN, cache distribuído, multi-região.**
- **APM / Jaeger / tracing distribuído** — fora de escopo também em `commerce 05`.
- **FinOps como programa** — a restrição de `00-visao-geral.md` permanece.
- **Teste de penetração, carga destrutiva, chaos engineering.**

## Riscos

| Risco | Mitigação |
|---|---|
| `pg_stat_statements` exige `shared_preload_libraries` e restart do Postgres | é configuração do Compose, que é construção do usuário; registrado como dependência de ambiente na Issue `11`, não como evidência |
| A sequência ficar longa demais antes de produzir valor | `09` e `10` já entregam número útil; `11`–`13` só adicionam aprofundamento |
| Otimização quebrar correção | a invariante de idempotência e o `/health` são critério de aceitação de `12`, não nota |
| Tração de bottleneck virar tweak infinito | `12` exige **uma** mudança de maior efeito com delta medido; melhorar 1% não fecha a Issue |

## Como isso se lê para quem chega depois

Cinco Issues que, lidas em ordem, contam uma história: **quanto deveria aguentar → até onde aguentou
de fato → o que quebrou primeiro → consertar e provar que consertou → quanto custa sustentar**.
É a diferença entre medir e melhorar, que era o gap.
