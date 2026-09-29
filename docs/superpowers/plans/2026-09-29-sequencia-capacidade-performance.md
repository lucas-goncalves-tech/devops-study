# Sequência de Capacidade e Performance — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar a sequência complementar `09 → 13` de capacidade e performance no `commerce-api`, mais o dicionário de medição que elas compartilham, e sincronizar o tracker.

**Architecture:** Sete artefatos de documentação novos (1 dicionário + 5 Issues) e cinco atualizações de tracker existentes. Cada Issue é uma RFC no template fixo de 13 seções de `00-visao-geral.md`, com dependências declaradas por link relativo — não por numeração. O dicionário de medição nasce primeiro porque as cinco Issues o referenciam.

**Tech Stack:** Markdown, git. Nenhum código é escrito por este plano: `commerce-api/app/` é construção do usuário e nenhuma task o toca.

**Spec:** `docs/superpowers/specs/2026-09-29-complementar-capacidade-performance-design.md`

## Global Constraints

- **Escopo de escrita** (`AGENTS.md:52-61`): gravável é `ledger-service/`, `commerce-api/`, `webhook-gateway/`, `BOARD.md`, `00-visao-geral.md`, `README.md`, `AGENTS.md`, `archive/`, `docs/`, `.superpowers/`. **Leitura apenas:** `<app>/app/` (código, Dockerfile, compose), IaC/Terraform, `.github/workflows/`, `healthcheck.sh`, `.agents/`. Nenhuma task deste plano escreve fora da lista gravável.
- **Template fixo de Issue**, 13 seções H2 em exatamente esta ordem (`00-visao-geral.md:42`): `Contexto`, `Objetivo`, `Dependências`, `Escopo`, `Fora de escopo`, `Conhecimentos envolvidos`, `Estado atual`, `Resultado esperado`, `Requisitos`, `Critérios de aceitação`, `Validação`, `Evidências`, `Limitações / notas`. Nenhuma a mais, nenhuma a menos, nenhuma fora de ordem.
- **Issue não é aula** (`00-visao-geral.md:41`, `AGENTS.md:44`): sem tutorial, FAQ, `Prev`/`Next`, numeração de sub-etapa (`1A`, `2B`) nem passos numerados de "como fazer". Passos de execução vão para `commerce-api/estudos/`, que este plano **não** cria.
- **Nenhuma Issue antecipa tecnologia** (`00-visao-geral.md:80`) cuja questão ela não resolve.
- **Custo zero** (`00-visao-geral.md:77`): nenhuma das 5 Issues pode exigir VPS ou conta AWS. k6 roda como container contra o Compose da `commerce 02`.
- **Checkbox do board espelha o `status:` do frontmatter** (`00-visao-geral.md:72`). `09` a `13` nascem todas `todo` / `[ ]`.
- **Kubernetes, HPA e autoscaling seguem fora de escopo** — `archive/18-kubernetes-helm` continua candidata a 4ª trilha.
- **FinOps como programa segue fora de escopo** (`00-visao-geral.md:76`).
- **Nenhuma Issue existente é reescrita.** `commerce 05` e `ledger 08` recebem apenas uma linha de desambiguação.
- **As 5 Issues referenciam o dicionário.** Toda Issue da sequência liga para `../../docs/performance/dicionario-de-medicao.md` a partir da seção `Escopo`. Um dicionário que só o Task 1 cita é um arquivo órfão, e a spec o define como contrato compartilhado.
- **A alteração de escopo de escrita do `AGENTS.md` já foi feita** na conversa que originou a spec (`docs/` e `.superpowers/` movidos para gravável). Este plano não a repete; o `AGENTS.md` só aparece em `Global Constraints` como a regra vigente.

## Review Focus

Cinco modos de falha que a spec implica e que nenhuma verificação de conteúdo pega sozinha. Cada um ganha um check no task que é dono do código afetado.

1. **Issue que vira aula.** O pull natural é abrir `Requisitos` com "criar o script k6" e colar sintaxe. Comportamento esperado: a Issue diz o que o script tem que provar; o `como` é de `estudos/`. Verificado no Task 2 e replicado nos demais.
2. **Dependência circular com `commerce 05`.** A `05` já diz "SLO medido por carga com k6" e a `10` faz a rampa. Se alguém "atualizar" a `05` para descrever a rampa, ela passa a depender de `10` e a dependência invertida quebra a sequência. Verificado no Task 8.
3. **Board dessincronizado do frontmatter.** Falha já nomeada em `00-visao-geral.md:72`, e a chance real é abrir a Issue e esquecer a linha do board. Verificado no Task 7.
4. **Evidência que não é evidência.** O risco próprio de performance é o critério "o p95 melhora", que não é verificável por terceiro. `00-visao-geral.md:71` já proíbe isso; a `12` é onde a tentação é máxima. Verificado no Task 5.
5. **Tecnologia antecipada.** O pull é mencionar Prometheus, instâncias AWS ou HPA dentro de `09`, `10` ou `13` como se fossem o assunto. Verificado no Task 7.

## File Structure

**Criados:**

| Caminho | Responsabilidade |
|---|---|
| `docs/performance/dicionario-de-medicao.md` | contrato de medição compartilhado pelas 5 Issues: 8 métricas, origem, e a invariante de disjunção |
| `commerce-api/issues/09-plano-de-capacidade.md` | RFC: a conta de capacidade antes do experimento |
| `commerce-api/issues/10-carga-em-rampa.md` | RFC: rampa até degradação e ponto de inflexão |
| `commerce-api/issues/11-localizacao-do-gargalo.md` | RFC: o veredito sobre qual recurso satura primeiro |
| `commerce-api/issues/12-correcao-com-prova.md` | RFC: uma mudança de maior efeito com delta medido |
| `commerce-api/issues/13-custo-da-vazao.md` | RFC: custo por RPS e right-sizing |

**Modificados:**

| Caminho | Mudança |
|---|---|
| `BOARD.md` | subseção nova em `commerce-api` com as 5 linhas e a cadeia de dependências |
| `commerce-api/AGENTS.md` | tabela de Issues ganha bloco `09–13`; "próxima a entrar" continua `01` |
| `commerce-api/README.md` | índice dos arquivos novos |
| `00-visao-geral.md` | 3 linhas: a sequência complementar existe e `archive/18` segue candidata a 4ª trilha |
| `commerce-api/issues/05-observability.md` | **uma** linha em `## Fora de escopo` separando smoke test de rampa |
| `ledger-service/issues/08-trafego-sintetico-alertas.md` | **uma** linha em `## Fora de escopo` separando tráfego sintético de teste de capacidade |

**Não tocados:** qualquer arquivo sob `*/app/`, `infra/`, `.github/`, `healthcheck.sh`, `.agents/`.

---

## Task 1: Dicionário de medição

**Files:**
- Create: `docs/performance/dicionario-de-medicao.md`
- Modify: nenhum

**Interfaces:**
- Consumes: nada.
- Produces: o arquivo `docs/performance/dicionario-de-medicao.md`, linkado por link relativo a partir das 5 Issues como `../../docs/performance/dicionario-de-medicao.md`. As seções `## Camadas de medição` (a tabela de 8 métricas) e `## Invariante de disjunção` são os trechos citados.

- [ ] **Step 1: Criar o arquivo com frontmatter e as duas seções**

Frontmatter:

```yaml
---
aliases: [dicionario, medicao, performance, metricas]
tags: [reference, performance, observability]
---
```

Seções H2, exatamente duas: `Camadas de medição` e `Invariante de disjunção`. Mais um parágrafo de abertura de 2 linhas dizendo que o arquivo é **contrato, não aula** e que o passo a passo de cada ferramenta vive em `commerce-api/estudos/`.

- [ ] **Step 2: Escrever a tabela de 8 métricas, com os valores exatos da spec**

Colunas: `Métrica | Pergunta que responde | Fonte | Origem`. Linhas, com a coluna `Origem` escrita exatamente assim:

| Métrica | Fonte | Origem |
|---|---|---|
| `http_req_duration` p50/p95/p99 | k6 | `commerce 05` |
| `http_req_failed` | k6 | `commerce 05` |
| pool active / idle / waiting | `prom-client` sobre `postgres` | `commerce 05` |
| event loop lag p99 | `node:perf_hooks` → `prom-client` | novo |
| calls por query | `pg_stat_statements` | novo |
| tempo total por query | `pg_stat_statements` | novo |
| buffers / rows / tempo por query | `EXPLAIN (ANALYZE, BUFFERS)` | novo |
| lock wait em `products` | `pg_stat_activity` + `pg_locks` | novo |

A coluna `Origem` é o que impede a Issue `05` de ser reescrita: é ela que marca o que já existe e o que a sequência cria.

- [ ] **Step 3: Escrever a invariante de disjunção**

Texto a fixar: **se o event loop lag sobe junto com o p95, o Node bloqueia; se fica baixo enquanto o p95 sobe, o problema é do banco.** Registrar também que pool `max: 10` e `connect_timeout: 10` saem de `commerce-api/app/src/db/connection.ts:6-10`, e que o `checkout` emite 14 round-trips serializados por carrinho de 3 itens (`commerce-api/app/src/modules/orders/orders.service.ts:22-114`).

- [ ] **Step 4: Verificar estrutura e links**

```bash
grep '^## ' docs/performance/dicionario-de-medicao.md
```

Expected: exatamente `## Camadas de medição` e `## Invariante de disjunção`, nessa ordem.

```bash
grep -c '| novo |' docs/performance/dicionario-de-medicao.md
```

Expected: `5`. São cinco linhas novas — event loop lag, as duas de `pg_stat_statements`, `EXPLAIN` e lock wait. Um `4` aqui significaria que uma delas foi perdida.

- [ ] **Step 5: Commit**

```bash
git add docs/performance/dicionario-de-medicao.md
git commit -m "docs(perf): dicionario de medicao compartilhado pela sequencia 09-13"
```

---

## Task 2: Issue 09 — Plano de capacidade

**Files:**
- Create: `commerce-api/issues/09-plano-de-capacidade.md`
- Modify: nenhum

**Interfaces:**
- Consumes: `docs/performance/dicionario-de-medicao.md` (Task 1).
- Produces: link para `../../docs/performance/dicionario-de-medicao.md`. Define a **vazão-alvo** que a `10` usa como ponto de partida da rampa, e a **vazão de pico** que a `13` converte em custo. A `10` referencia o slug deste arquivo; a `13` idem.

- [ ] **Step 1: Criar o arquivo com frontmatter e H1**

```yaml
---
aliases: [issue-09, capacidade, plano-de-capacidade]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: media
---
```

H1: `# Issue 09 — Plano de capacidade com a conta que precede o experimento`.

- [ ] **Step 2: Escrever as 13 seções**

Conteúdo fixado pela spec, sem自由度 de decisão:

- `Contexto` — dimensionamento hoje é chute; ninguém sabe quantos RPS o serviço aguenta.
- `Objetivo` — estado final é a conta fechada com a origem de cada parcela, não um número solto.
- `Dependências` — link para `05-observability.md`: a conta é conferida contra a latência observada.
- `Escopo` — usuários concorrentes → requisições por usuário → RPS de pico → Little's law (concorrência = vazão × latência) → vCPU e memória → tipo de instância.
- `Fora de escopo` — provisionar qualquer coisa; medir; provisionar em nuvem. **Proibir menção a HPA, Prometheus e Auto Scaling aqui** (Review Focus 5).
- `Conhecimentos envolvidos` — Little's law; dimensionamento por vazão; leitura de consumo de recurso por tipo de instância.
- `Estado atual` — não existe vazão-alvo documentada em lugar nenhum do repo.
- `Resultado esperado` — um documento com a conta, a premissa de cada parcela e a vazão-alvo que a `10` vai testar.
- `Requisitos` — um item por parcela da cadeia, cada um exigindo a origem do número; um item exigindo que a vazão-alvo e a vazão de pico estejam nomeadas separadamente.
- `Critérios de aceitação` — a conta fecha aritmeticamente; toda parcela tem origem escrita; a vazão-alvo é um número, não uma faixa; um terceiro recalcula e chega ao mesmo resultado.
- `Validação` — refazer a conta a partir das premissas escritas e conferir que o resultado bate com o registrado.
- `Evidências` — o documento de capacidade com as parcelas e o cálculo final.
- `Limitações / notas` — esta Issue não mede: a medição é da `10`; se a conta e a medição divergirem, quem está errado é o `11` que descobre, não esta Issue.

- [ ] **Step 3: Check de template contra a Issue de referência**

```bash
diff <(grep '^## ' commerce-api/issues/05-observability.md) <(grep '^## ' commerce-api/issues/09-plano-de-capacidade.md) && echo TEMPLATE_OK
```

Expected: `TEMPLATE_OK` e nenhuma outra saída. Divergência aqui significa seção faltando, sobrando ou fora de ordem.

- [ ] **Step 4: Check de que a Issue não virou aula, nem antecipou tecnologia (Review Focus 1 e 5)**

```bash
grep -nE '```|npm |docker run|k6 run |^1\.|^2\.|passo a passo|como fazer' commerce-api/issues/09-plano-de-capacidade.md
grep -nE 'HPA|horizontal pod|autoscal|helm|kubernetes|prometheus|grafana' commerce-api/issues/09-plano-de-capacidade.md
```

Expected: sem saída nos dois. Qualquer ocorrência no primeiro é tutorial invadindo a Issue; no segundo é tecnologia antecipada — a `09` é aritmética de capacidade, e `Prometheus` e `Grafana` pertencem à `05`.

- [ ] **Step 5: Commit**

```bash
git add commerce-api/issues/09-plano-de-capacidade.md
git commit -m "feat(perf): issue 09 plano de capacidade"
```

---

## Task 3: Issue 10 — Carga em rampa até degradação

**Files:**
- Create: `commerce-api/issues/10-carga-em-rampa.md`

**Interfaces:**
- Consumes: Task 1 (dicionário), Task 2 (`09-plano-de-capacidade.md`).
- Produces: o slug `commerce-api/issues/10-carga-em-rampa.md` e o **ponto de inflexão** registrado, que a `11` usa como cenário de profiling e a `12` usa como cenário de re-execução. A `11` e a `13` referenciam este arquivo.

- [ ] **Step 1: Criar o arquivo com frontmatter e H1**

```yaml
---
aliases: [issue-10, rampa, carga-em-rampa, k6]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---
```

H1: `# Issue 10 — Carga em rampa até degradação, com o ponto de inflexão registrado`.

- [ ] **Step 2: Escrever as 13 seções**

Diferenças em relação à `09`, todas fixadas:

- `Contexto` — a `05` fixa 50–100 VUs e p95 de 500ms; isso prova que passou, não onde quebra. Link para a `05` aqui.
- `Dependências` — `09-plano-de-capacidade.md` (a rampa é dimensionada pela vazão-alvo) e `05-observability.md`.
- `Escopo` — estágios `ramping-vus` subindo até o critério falhar; curva RPS × p95 × erros; **`event loop lag` entrando no `/metrics`**; persistência da saída.
- `Fora de escopo` — corrigir qualquer coisa (é a `12`);Prometheus, HPA e Auto Scaling; qualquer menção a provisionamento.
- `Estado atual` — `commerce 05` trava em 50–100 VUs; nunca houve registro de onde a curva quebra.
- `Requisitos` — item exigindo que a rampa **cresça até o threshold falhar** e pare, não que rode um número fixo; item exigindo o estágio exato da quebra e a vazão atingida naquele ponto; item exigindo a série de `event loop lag` colada; item exigindo que o `/metrics` continue sem `preHandler` de autenticação.
- `Critérios de aceitação` — existe estágio identificado onde o critério quebrou; o ponto de inflexão está registrado com vazão e p95; a série de event loop lag está anexada; `/health` segue `200` `"UP"` com banco de pé e `503` `"DEGRADED"` sem ele durante toda a janela.
- `Limitações / notas` — **`/metrics` e `/health` fora do `preHandler` de JWT é invariante herdada de `commerce 05`**; o event loop lag novo também é publicado ali. Registrado aqui porque é a primeira vez que a sequência toca no `/metrics`.

- [ ] **Step 3: Check de template**

```bash
diff <(grep '^## ' commerce-api/issues/05-observability.md) <(grep '^## ' commerce-api/issues/10-carga-em-rampa.md) && echo TEMPLATE_OK
```

Expected: `TEMPLATE_OK`.

- [ ] **Step 4: Check de aula e de tecnologia antecipada (Review Focus 1 e 5)**

```bash
grep -nE '```|passo a passo|como fazer' commerce-api/issues/10-carga-em-rampa.md
grep -nE 'HPA|horizontal pod|autoscal|Helm|kubernetes' commerce-api/issues/10-carga-em-rampa.md
```

Expected: sem saída nos dois.

- [ ] **Step 5: Commit**

```bash
git add commerce-api/issues/10-carga-em-rampa.md
git commit -m "feat(perf): issue 10 carga em rampa ate degradacao"
```

---

## Task 4: Issue 11 — Localização do gargalo

**Files:**
- Create: `commerce-api/issues/11-localizacao-do-gargalo.md`

**Interfaces:**
- Consumes: Tasks 1–3. Recebe o **ponto de inflexão** produzido pela `10`.
- Produces: o slug `commerce-api/issues/11-localizacao-do-gargalo.md` e o **veredito escrito** — o recurso que satura primeiro — que a `12` usa para escolher a mudança, e que a `13` pode usar para checar se a instância dimensionada era o limite.

- [ ] **Step 1: Criar o arquivo com frontmatter e H1**

```yaml
---
aliases: [issue-11, gargalo, profiling, pg-stat-statements]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---
```

H1: `# Issue 11 — Localização do gargalo com veredito sustentado por número`.

- [ ] **Step 2: Escrever as 13 seções**

- `Contexto` — saber que degrada não diz se é app, banco, lock ou I/O. Nenhuma Issue hoje produz essa resposta.
- `Dependências` — `10-carga-em-rampa.md`.
- `Escopo` — `pg_stat_statements` por número de chamadas **e** tempo total; `EXPLAIN (ANALYZE, BUFFERS)` no caminho crítico; waits de lock em `products` via `pg_stat_activity` + `pg_locks`; a disjunção do event loop lag aplicada.
- `Fora de escopo` — alterar código, schema ou índice. Registrar mudança é da `12`.
- `Conhecimentos envolvidos` — `pg_stat_statements`; leitura de plano de execução; waits de lock; a disjunção event loop vs banco.
- `Estado atual` — nenhuma dessas três ferramentas está habilitada; o `/metrics` da `05` não tem event loop lag antes da `10`.
- `Requisitos` — um item exigindo a query nomeada com maior tempo total **e** com maior contagem de chamadas, com os dois números; um item exigindo o `EXPLAIN (ANALYZE, BUFFERS)` da query do caminho crítico; um item exigindo a contagem de waits de lock em `products` durante a janela; um item exigindo a disjunção aplicada explicitamente; um item exigindo **o veredito escrito nomeando um recurso**.
- `Critérios de aceitação` — o veredito nomeia um recurso, não diz "o sistema está lento"; se a hipótese da `10` (pool esgotado) for contrariada pelo dado, a Issue **registra o erro de hipótese** em vez de omitir. Este último item é o que impede a Issue de virar narrativa.
- `Limitações / notas` — `pg_stat_statements` exige `shared_preload_libraries` e restart do Postgres. Isso é configuração do Compose, que é **construção do usuário**: se não estiver disponível, a dependência vai registrada aqui e **não** conta como evidência. Aplicar `00-visao-geral.md:71`.

- [ ] **Step 3: Check de template**

```bash
diff <(grep '^## ' commerce-api/issues/05-observability.md) <(grep '^## ' commerce-api/issues/11-localizacao-do-gargalo.md) && echo TEMPLATE_OK
```

Expected: `TEMPLATE_OK`.

- [ ] **Step 4: Check de aula e de violação do limite de escrita**

```bash
grep -nE '```|passo a passo|como fazer' commerce-api/issues/11-localizacao-do-gargalo.md
grep -n 'shared_preload_libraries' commerce-api/issues/11-localizacao-do-gargalo.md
```

Expected: primeira sem saída; segunda com exatamente 1 ocorrência, dentro de `## Limitações / notas` e acompanhada das palavras "construção do usuário" ou "não conta como evidência".

- [ ] **Step 5: Commit**

```bash
git add commerce-api/issues/11-localizacao-do-gargalo.md
git commit -m "feat(perf): issue 11 localizacao do gargalo"
```

---

## Task 5: Issue 12 — Correção com prova de efeito

**Files:**
- Create: `commerce-api/issues/12-correcao-com-prova.md`

**Interfaces:**
- Consumes: Tasks 1–4. Recebe o **veredito** da `11` e o **script da `10`**.
- Produces: o slug `commerce-api/issues/12-correcao-com-prova.md`. Nenhuma Issue posterior depende desta — é o fim da cadeia `05 → 09 → 10 → 11 → 12`.

- [ ] **Step 1: Criar o arquivo com frontmatter e H1**

```yaml
---
aliases: [issue-12, correcao, otimizacao, prova-de-efeito]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---
```

H1: `# Issue 12 — Correção do gargalo com o mesmo script re-executado e delta lado a lado`.

- [ ] **Step 2: Escrever as 13 seções**

- `Contexto` — a `11` devolve diagnóstico, não conserto. Sem re-medição, uma mudança de performance é opinião.
- `Dependências` — `11-localizacao-do-gargalo.md`; a re-execução usa o script de `10-carga-em-rampa.md`.
- `Escopo` — **uma** mudança de maior efeito indicada pelo veredito; re-execução do mesmo script; comparação lado a lado. Candidatos a nomear em `Conhecimentos envolvidos`, sem implementar nenhum: `SELECT products WHERE id = ANY(...)` no lugar dos N selects sequenciais (`orders.service.ts:40-61`) e tratamento da contendão de estoque no `UPDATE products` (`:80`).
- `Fora de escopo` — otimização de segunda ordem; refactor sem medição; mais de uma mudança na mesma rodada.
- `Requisitos` — item exigindo **exatamente uma** mudança aplicada, nomeada; item exigindo re-execução do **mesmo** script, sem parâmetro alterado; item exigindo as duas saídas lado a lado; item exigindo o delta de p95, de vazão e de event loop lag; item exigindo a afirmação de **qual** mudança causou o delta.
- `Critérios de aceitação` — as duas execuções estão no Issue, lado a lado, com o mesmo script; o delta está em número, não em adjetivo; **`checkout` continua idempotente por `idempotencyKey`** e nenhum pedido duplicado nem estoque negativo aparece no banco depois da rodada; `/health` e `/metrics` seguem invariantes.
- `Limitações / notas` — **"o p95 melhorou" não é evidência** (`00-visao-geral.md:71`): só fecha com as duas saídas coladas. Regra de uma variável por rodada: duas mudanças simultâneas produzem um número melhor que não atribui causa a nada e invalidam a rodada. Otimização que derruba estoque ou duplica pedido está errada mesmo com p95 melhor — os dois critérios de aceitação são independentes e ambos obrigatórios.

- [ ] **Step 3: Check de template**

```bash
diff <(grep '^## ' commerce-api/issues/05-observability.md) <(grep '^## ' commerce-api/issues/12-correcao-com-prova.md) && echo TEMPLATE_OK
```

Expected: `TEMPLATE_OK`.

- [ ] **Step 4: Check de que a evidência exigida é verificável (Review Focus 4)**

```bash
grep -n 'não é evidência' commerce-api/issues/12-correcao-com-prova.md
grep -c 'idempot' commerce-api/issues/12-correcao-com-prova.md
```

Expected: primeira com ≥1 ocorrência; segunda com ≥2 (uma em `Critérios de aceitação`, uma em `Limitações / notas`).

- [ ] **Step 5: Check de aula**

```bash
grep -nE '```|passo a passo|como fazer' commerce-api/issues/12-correcao-com-prova.md
```

Expected: sem saída.

- [ ] **Step 6: Commit**

```bash
git add commerce-api/issues/12-correcao-com-prova.md
git commit -m "feat(perf): issue 12 correcao com prova de efeito"
```

---

## Task 6: Issue 13 — Custo da vazão

**Files:**
- Create: `commerce-api/issues/13-custo-da-vazao.md`

**Interfaces:**
- Consumes: Tasks 1–3. Recebe a **vazão de pico** da `09` e o **ponto de inflexão** da `10`.
- Produces: o slug `commerce-api/issues/13-custo-da-vazao.md`. Fecha a sequência `09 → 13`.

- [ ] **Step 1: Criar o arquivo com frontmatter e H1**

```yaml
---
aliases: [issue-13, custo, finops, right-sizing]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: media
---
```

H1: `# Issue 13 — Custo por vazão sustentada e checagem de right-sizing`.

- [ ] **Step 2: Escrever as 13 seções**

- `Contexto` — a `07` estima custo de infra **provisionada**; ninguém calculou o custo da **vazão** que o serviço mediu.
- `Dependências` — `10-carga-em-rampa.md` (o ponto de inflexão é a vazão que precisa ser sustentada) e `09-plano-de-capacidade.md` (a instância que ela dimensionou é a que entra na conta).
- `Escopo` — custo por RPS sustentado por mês a partir do ponto de inflexão; checagem de right-sizing: a instância da `09` aguenta o pico medido com folga ou é over-provision; se for, o valor do overprovision em reais.
- `Fora de escopo` — **provisionar qualquer coisa**; FinOps como programa (`00-visao-geral.md:76`); Savings Plans, instâncias reservadas e spot como estratégia de compras; tags e alocação de custo.
- `Conhecimentos envolvidos` — custo por unidade de vazão; right-sizing a partir de consumo medido; a trava de custo de recurso pago já existente na `07`.
- `Estado atual` — a `07` tem a trava de custo antes de provisionar; não existe nenhuma conta que ligue vazão medida a custo.
- `Requisitos` — um item exigindo o custo por RPS/mês com a origem de cada preço; um item exigindo a conclusão explícita de right-sizing, em uma das duas direções (aguenta com folga / é over-provision); item exigindo a conta do overprovision quando for o caso; item exigindo link para a trava de custo da `07`.
- `Critérios de aceitação` — a conta fecha e um terceiro refaz; a conclusão de right-sizing está escrita e é uma das duas direções, não "depende"; **nenhum recurso foi provisionado** — a Issue entrega número, não cobrança.
- `Limitações / notas` — a `07` segue sendo a única Issue que pode chegar a recurso pago, e só como prova final. FinOps como programa permanece fora de escopo por decisão registrada em `00-visao-geral.md:76`.

- [ ] **Step 3: Check de template**

```bash
diff <(grep '^## ' commerce-api/issues/05-observability.md) <(grep '^## ' commerce-api/issues/13-custo-da-vazao.md) && echo TEMPLATE_OK
```

Expected: `TEMPLATE_OK`.

- [ ] **Step 4: Check de aula e deFinOps escorregando para dentro (Review Focus 5)**

```bash
grep -nE '```|passo a passo|como fazer' commerce-api/issues/13-custo-da-vazao.md
grep -nE 'Savings Plan|instância reservada|spot|tags de custo|showback|chargeback' commerce-api/issues/13-custo-da-vazao.md
```

Expected: sem saída no primeiro. No segundo, ocorrências são **aceitas** se estiverem dentro de `Fora de escopo` listando o que fica de fora; **rejeitadas** em qualquer outra seção.

- [ ] **Step 5: Commit**

```bash
git add commerce-api/issues/13-custo-da-vazao.md
git commit -m "feat(perf): issue 13 custo da vazao e right-sizing"
```

---

## Task 7: Sincronizar o tracker do commerce-api

**Files:**
- Modify: `BOARD.md` (seção `commerce-api · trilha AWS`)
- Modify: `commerce-api/AGENTS.md` (tabela de Issues)
- Modify: `commerce-api/README.md` (índice)

**Interfaces:**
- Consumes: os 5 arquivos de Issue dos Tasks 2–6.
- Produces: os slugs exatos `09-plano-de-capacidade.md`, `10-carga-em-rampa.md`, `11-localizacao-do-gargalo.md`, `12-correcao-com-prova.md`, `13-custo-da-vazao.md`, usados pelos checks de consistência do Task 9.

- [ ] **Step 1: Adicionar a subseção ao `BOARD.md`**

Logo após o bloco `### Parked` da seção `commerce-api`, criar a subseção com este cabeçalho e esta forma:

```markdown
### Sequência complementar — capacidade e performance

> Complementar à trilha AWS, não parte dela. `09 → 13`, numeração própria, dependências declaradas
> por link — não pela sequência `01 → 08`, que fica fechada e intocada. Dicionário de medição
> compartilhado: [`docs/performance/`](../docs/performance/dicionario-de-medicao.md).
> **Estado final:** vazão-alvo calculada, limite real encontrado por rampa, gargalo nomeado com
> número, correção com delta medido e custo por vazão — sem provisionar nada.

- [ ] [09 Plano de capacidade](commerce-api/issues/09-plano-de-capacidade.md) — quanto o serviço deveria aguentar, com a conta fechada antes de medir
- [ ] [10 Carga em rampa](commerce-api/issues/10-carga-em-rampa.md) — até onde aguenta de fato, e onde a curva quebra
- [ ] [11 Localização do gargalo](commerce-api/issues/11-localizacao-do-gargalo.md) — o que satura primeiro, nomeado com número
- [ ] [12 Correção com prova de efeito](commerce-api/issues/12-correcao-com-prova.md) — a mudança de maior efeito, com o delta do mesmo script lado a lado
- [ ] [13 Custo da vazão](commerce-api/issues/13-custo-da-vazao.md) — quanto custa sustentar o pico medido, e se a instância é do tamanho certo

> **Cadeia:** `05 → 09 → 10 → {11 → 12, 13}`. Custo zero: k6 como container contra o Compose da `02`.
> Kubernetes, HPA e FinOps como programa seguem fora de escopo (`archive/18-kubernetes-helm`).
```

- [ ] **Step 2: Adicionar o bloco `09–13` na tabela de `commerce-api/AGENTS.md`**

Logo após a linha da Issue `08` da tabela, inserir uma linha de cabeçalho de bloco e as 5 linhas, espelhando a forma usada pelas outras trilhas. A linha "Próxima a entrar" **não muda**: continua `01`, porque a trilha AWS não foi antecipada.

- [ ] **Step 3: Adicionar os arquivos ao índice de `commerce-api/README.md`**

Entrada para o dicionário de medição, referenciado com caminho relativo a partir de `commerce-api/`.

- [ ] **Step 4: Check de board vs frontmatter (Review Focus 3)**

```bash
for n in 09-plano-de-capacidade 10-carga-em-rampa 11-localizacao-do-gargalo 12-correcao-com-prova 13-custo-da-vazao; do
  # O board escreve o TITULO no colchete e o SLUG no parentese: casar o slug direto
  # da falso negativo nas cinco.
  board=$(grep -c "\[ \] \[[^]]*\](commerce-api/issues/$n.md)" BOARD.md)
  fm=$(grep -c '^status: todo$' commerce-api/issues/$n.md)
  echo "$n board=$board status=$fm"
done
```

Expected: cinco linhas, todas `board=1 status=1`. Qualquer `board=0` é linha de board faltando; qualquer `status=0` é frontmatter fora de `todo`.

- [ ] **Step 5: Check de que nenhum link do board aponta para arquivo inexistente (Review Focus 5)**

```bash
grep -oE '\((commerce-api/issues/(09|10|11|12|13)-[a-z-]+\.md)\)' BOARD.md | tr -d '()' | sort -u | while read p; do [ -f "$p" ] || echo "QUEBRADO: $p"; done
```

Expected: sem saída.

- [ ] **Step 6: Commit**

```bash
git add BOARD.md commerce-api/AGENTS.md commerce-api/README.md
git commit -m "docs(tracker): registra a sequencia complementar 09-13"
```

---

## Task 8: Enquadramento no monorepo e desambiguação das Issues existentes

**Files:**
- Modify: `00-visao-geral.md` (3 linhas)
- Modify: `commerce-api/issues/05-observability.md` (1 linha em `## Fora de escopo`)
- Modify: `ledger-service/issues/08-trafego-sintetico-alertas.md` (1 linha em `## Fora de escopo`)

**Interfaces:**
- Consumes: Tasks 2–7.
- Produces: nada consumido adiante. É o passo que impede a leitura errada da sequência.

- [ ] **Step 1: As 3 linhas do `00-visao-geral.md`**

Uma na tabela de apps, uma na seção `## Restrições de escopo` e uma em `## Fora de escopo`. As três, com este conteúdo:

- Tabela de apps: a linha do `commerce-api` passa a ler `AWS` + `09 → 13` como sequência complementar declarada, deixando explícito que **continua sendo uma trilha só**.
- Restrições: registrar que a performance entrou como sequência complementar do `commerce-api` e **não** como 4ª trilha, e que `archive/18-kubernetes-helm` continua candidata a 4ª trilha.
- Fora de escopo: repetir que HPA e autoscaling continuam fora, agora nomeando `archive/18` como o lugar onde isso vive.

- [ ] **Step 2: A linha de desambiguação na `commerce 05`**

Em `## Fora de escopo`, acrescentar que o smoke test de 50–100 VUs com threshold fixo **não** é o teste de capacidade, e linkar `10-carga-em-rampa.md` como quem faz a rampa. **Uma linha só** — a `05` não é reescrita, porque ela continua válida e continua sendo o que a `10` consome.

- [ ] **Step 3: A linha de desambiguação no `ledger 08`**

Em `## Fora de escopo`, acrescentar que tráfego sintético agendado com vazão baixa em produção **não** é teste de capacidade, e linkar `10-carga-em-rampa.md` — que a própria `08` já referencia na seção `Limitações / notas` ao dizer que "carga de desempenho de verdade é a da `Issue 05` do `commerce-api`". A linha nova corrige essa referência para `10`.

- [ ] **Step 4: Check de não-circularidade (Review Focus 2)**

```bash
grep -n 'Dependências' -A6 commerce-api/issues/05-observability.md | grep -c '10-carga-em-rampa'
grep -c '10-carga-em-rampa' commerce-api/issues/05-observability.md
```

Expected: `0` e `1`. O primeiro garante que a menção **não** está dentro de `## Dependências` — se estivesse, `05` passaria a depender de `10` e inverteria a cadeia. O segundo garante que a menção existe em algum lugar, para o leitor não confundir as duas coisas.

- [ ] **Step 5: Check de que a `05` e a `08` continuam estruturalmente intactas**

```bash
diff <(grep '^## ' commerce-api/issues/05-observability.md | wc -l) <(grep '^## ' commerce-api/issues/05-observability.md | wc -l) && grep -c '^## ' commerce-api/issues/05-observability.md
```

Expected: `13` na segunda parte, e o diff sem saída. As duas Issues não podem ganhar nem perder seção — a mudança é de uma linha dentro de `Fora de escopo`.

- [ ] **Step 6: Commit**

```bash
git add 00-visao-geral.md commerce-api/issues/05-observability.md ledger-service/issues/08-trafego-sintetico-alertas.md
git commit -m "docs: enquadra a sequenca de performance como complementar, nao 4a trilha"
```

---

## Task 9: Verificação final da sequência

**Files:**
- Modify: nenhum
- Test: verificação de consistência do conjunto

**Interfaces:**
- Consumes: tudo dos Tasks 1–8.
- Produces: nada. É o gate de fechamento.

- [ ] **Step 1: Os 5 Issues respondem ao template**

```bash
for n in commerce-api/issues/0[9]*.md commerce-api/issues/1[0-3]*.md; do
  diff -q <(grep '^## ' commerce-api/issues/05-observability.md) <(grep '^## ' "$n") >/dev/null && echo "OK $n" || echo "FORA DO TEMPLATE $n"
done
```

Expected: cinco `OK`, nenhum `FORA DO TEMPLATE`.

- [ ] **Step 2: A cadeia de dependências resolve e não tem ciclo**

```bash
for n in 09-plano-de-capacidade 10-carga-em-rampa 11-localizacao-do-gargalo 12-correcao-com-prova 13-custo-da-vazao; do
  f=commerce-api/issues/$n.md
  # Dependencia se declara como link IRMAO ("05-observability.md"), nao como caminho
  # completo: as 5 Issues estao no mesmo diretorio. A forma "../../app/issues/..."
  # e cross-app. E so a secao Dependencias conta — link em Fora de escopo e referencia.
  d=$(awk '/^## Dependências$/{s=1;next} /^## /{s=0} s' "$f" | grep -oE '[0-9]{2}-[a-z0-9-]+\.md' | sort -u)
  [ -z "$d" ] && echo "SEM DEPENDENCIA: $n"
  for l in $d; do [ -f "commerce-api/issues/$l" ] || echo "LINK QUEBRADO: $n -> $l"; done
done
```

Expected: sem `SEM DEPENDENCIA` e sem `LINK QUEBRADO`. A cadeia que deve aparecer, conferida
seção a seção: `09 → [05]` · `10 → [05, 09]` · `11 → [10]` · `12 → [10, 11]` · `13 → [09, 10]`,
que é a mesma que o `BOARD.md` declara.

Expected: sem `SEM DEPENDENCIA` e sem `LINK QUEBRADO`. Os quatro links que devem existir: `05` (em `09` e em `10`), `09` (em `10`, `13`), `10` (em `11`, `13`), `11` (em `12`).

O `cd` por arquivo importa: as dependências resolvem a partir do diretório **do arquivo que as contém**, e a sequência só usa links `commerce-api/`. Resolver sempre contra `commerce-api/issues/` acusaria de quebrado qualquer link cross-app legítimo.

Dicionário referenciado pelas 5, e nenhum órfão:

```bash
grep -c '../../docs/performance/dicionario-de-medicao.md' commerce-api/issues/0[9]*.md commerce-api/issues/1[0-3]*.md
```

Expected: cinco linhas, todas com contagem ≥ 1. E a origem do link tem que existir:

```bash
[ -f docs/performance/dicionario-de-medicao.md ] && echo DICIONARIO_OK
```

Expected: `DICIONARIO_OK`. O caminho relativo `../../docs/` resolve a partir de `commerce-api/issues/`, que é onde as Issues vivem.

- [ ] **Step 3: Nenhuma Issue virou aula**

```bash
grep -lE 'passo a passo|como fazer|Prev\]\(|Next\]\(' commerce-api/issues/0[9]*.md commerce-api/issues/1[0-3]*.md
```

Expected: sem saída.

- [ ] **Step 4: Nenhuma Issue de performance provisiona recurso pago**

```bash
grep -lniE 'provisionar uma inst|aws ec2|criar uma vps|provisionar a conta' commerce-api/issues/1[0-3]*.md
```

Expected: sem saída. A regra de custo zero vale para a sequência inteira.

- [ ] **Step 5: Nada fora do escopo gravável foi tocado**

```bash
# MERGE_BASE, nao HEAD~N: a branch tem o commit de setup mais os 9 tasks, entao um
# HEAD~8 cortaria o primeiro task e o falso-verde deixaria de ver infra tocada.
MB=$(git merge-base main HEAD)
git diff --name-only "$MB"..HEAD | grep -vE '^(docs/|commerce-api/issues/|commerce-api/AGENTS\.md$|commerce-api/README\.md$|ledger-service/issues/|BOARD\.md$|00-visao-geral\.md$|AGENTS\.md$)'
```

Expected: sem saída. Qualquer coisa sob `app/`, `infra/`, `.github/`, `.agents/`, Terraform ou
`healthcheck.sh` é violação de escopo. A alternação precisa ancorar cada arquivo com `$`: sem o
âncora, o padrão de diretório `commerce-api/issues/` nunca casa uma Issue e a verificação acusa
falso positivo em todos os arquivos legítimos.

- [ ] **Step 6: Reportar ao usuário sem commit**

Relatar: os 11 arquivos tocados, a cadeia de dependências como ficou, e a lista das 5 Issues com o `status:` de cada uma. **Não** marcar nenhuma como `done` — a política de `00-visao-geral.md:65` exige evidência de validação registrada, e aqui a entrega é o tracker, não a capacidade.
