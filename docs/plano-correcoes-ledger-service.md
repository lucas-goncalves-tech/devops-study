# Correções dev≠prod — ledger-service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corrigir no tracker da trilha VPS os quatro achados Crítico/Importante da auditoria dev≠prod de 02/10/2026 — contrato de segredos de produção, método de schema × rollback, escopo do actuator e definição de staging — sem tocar em status, template nem código.

**Architecture:** Edits só em markdown do tracker: novos requisitos, critérios e notas **aditivas** dentro das 13 seções existentes, mais uma substituição de linha em `BOARD.md:20` (aprovação explícita do usuário). Nenhum `status:`, nenhum checkbox de seção fechada e nenhum arquivo de `estudos/` mudam — os achados não adicionam itens novos em `Conhecimentos envolvidos`, então o pareamento com estudos não é acionado.

**Tech Stack:** Tracker markdown (template RFC de 13 seções), gates `bash`/`grep`/`git` que imprimem linha `OK`.

**Spec:** A auditoria que motiva este plano está embutida na seção **Contexto da auditoria** abaixo (não há arquivo de spec separado — pedido do usuário foi um plano por app em `docs/`).

---

## Contexto da auditoria (spec)

Classes julgadas nesta trilha (evidência antes de corrigir):

- **A — valores de lab fixos: COBERTO.** Domínio, portas e destino de backup são parametrizados/flaggados (`04:89`, `04:91`, `07:112`, `02:89`, `05:86-89`).
- **B — config de lab vazando: MENOR.** `05:86` aceita LocalStack como destino "off-site" válido → **Diferido**.
- **C — método de schema órfão: PROBLEMA (Importante).** `06:96` trata `ddl-auto=update` como fato; zero menções a migração versionada em `issues/`+`estudos/`; `07:73` exige rollback automático sem estratégia de schema compatível.
- **D — contrato de segredos de produção: PROBLEMA (Crítico).** `01:49` fixa valores de lab (`postgres`/`postgres`, JWT 256 bits), `AGENTS.md` do app diz `JWT_SECRET` **embutido**, e nenhuma das 8 Issues exige geração/substituição em produção. O `webhook-gateway/04:87` aponta a dívida para cá ("o equivalente do ledger é trabalho de lá") e `grep JWT_SECRET` em `ledger-service/issues/` retorna 0.
- **E — superfície exposta sem auth: PROBLEMA (Importante).** `07:114` manda manter `/actuator/**` inteiro em `permitAll`; `04:61` não reserva caminho no proxy; `08:64` expõe a série sem requisito de auth no coletor. (Rebaixado de Crítico: conjunto exposto pequeno, firewall 22/80/443.)
- **F — decisões sem dono: PROBLEMA (Importante).** `07:71/82` exige separação de produção/staging mas nada define o que é staging; `BOARD.md:20` aponta `ledger 08` (que é produção: `08:8`, `08:16`) como card de staging. Ghost `target group` (`04:91`) e drift de endpoints (`08:50`) → **Diferidos**.

## Global Constraints

- Escopo de escrita deste plano: `ledger-service/issues/**` + **uma linha** de `BOARD.md` (Task 4). Qualquer outro arquivo = falha.
- Zero mudança em `status:`/frontmatter. Status esperado ao final: `done` ×2 (Issues 01, 02), `todo` ×6 (03–08). Os checkboxes `[x]` das Issues 01/02 não mudam.
- Template intacto: 13 seções `## ` por Issue, na ordem de `00-visao-geral.md`. Edits só adicionam/substituem bullets dentro de seções já existentes.
- Texto novo em PT-BR, estilo da casa: bullet `- `, notas em `- **Título:** ...`, sem tutorial/FAQ/sub-etapas dentro de Issue.
- Não adicionar itens novos em `Conhecimentos envolvidos` (pareamento com `estudos/` só seria acionado por isso; nenhum achado exige).
- Todo gate de verificação imprime uma linha (`echo "...: OK"`) — comando silencioso morre sob `pipefail`.
- Links `.md` novos resolvem relativos ao arquivo que os contém.
- Um commit por task, mensagem `docs(tracker): ...`.

## Review Focus

1. **Status intocado** — `01/02` seguem `done`, `03–08` seguem `todo`; nenhum `[x]` vira `[ ]` ou vice-versa. Gate: bateria final da Task 4.
2. **Template** — as 8 Issues seguem com exatamente 13 seções `## `. Gate: bateria final da Task 4.
3. **Escopo de diff** — só `ledger-service/issues/` + `BOARD.md`. Gate: bateria final da Task 4.
4. **`BOARD.md` minimalista** — a única linha alterada é a da seção `Staging`; nenhum checkbox/status do board muda. Gate: bateria final da Task 4.
5. **Estilo e links** — toda linha nova começa com bullet; links relativos resolvem. Gate: bateria final da Task 4.

---

### Task 1: Contrato de segredos de produção (D)

**Files:**
- Modify: `ledger-service/issues/07-cicd-vps-deploy.md` — seção `## Requisitos` (novo bullet após o requisito do deploy com chave efêmera) e seção `## Evidências` (novo bullet ao final).
- Modify: `ledger-service/issues/01-linux-runtime.md` — seção `## Limitações / notas` (novo bullet ao final, após a linha do contrato de portas).

**Interfaces:**
- Consume: evidência da auditoria `01:49` (valores de lab) e `AGENTS.md` do app (`JWT_SECRET` embutido).
- Produz: o contrato que torna verdadeiro o ponteiro `webhook-gateway/issues/04-secrets-hygiene.md:87` ("o equivalente do ledger é trabalho de lá") — ver Dependências entre planos.

- [ ] **Step 1: Novo requisito de contrato em `07-cicd-vps-deploy.md`**

Bullet único em `## Requisitos`, contendo obrigatoriamente os tokens: `contrato de segredos de produção`, o inventário `JWT_SECRET`, `SPRING_DATASOURCE_PASSWORD`, `credencial do backup` (Issue 05) e `token do canal de alerta` (Issue 08), o piso `256 bits`, os meios de vida `permissão restrita` (arquivo de ambiente da VPS) e `environment` (CI), e os verbos `geração` e `rotação`. Nenhum valor real no texto.

- [ ] **Step 2: Evidência do contrato em `07-cicd-vps-deploy.md`**

Bullet ao final de `## Evidências`, contendo: conferência do ambiente de produção com cada segredo do contrato presente e nenhum default de lab ativo.

- [ ] **Step 3: Nota de lab em `01-linux-runtime.md`**

Bullet ao final de `## Limitações / notas`, contendo obrigatoriamente: `laboratório` (declarando que os valores de `## Resultado esperado` são de lab) e referência à `Issue 07` como dona do contrato de produção. Não alterar nenhum `[x]`.

- [ ] **Step 4: Gate da Task 1**

Run:
```bash
set -euo pipefail
grep -q 'contrato de segredos de produção' ledger-service/issues/07-cicd-vps-deploy.md \
  && grep -q '256 bits' ledger-service/issues/07-cicd-vps-deploy.md \
  && grep -q 'JWT_SECRET' ledger-service/issues/07-cicd-vps-deploy.md \
  && grep -q 'rotação' ledger-service/issues/07-cicd-vps-deploy.md \
  && grep -q 'laboratório' ledger-service/issues/01-linux-runtime.md \
  && grep -q 'Issue 07' ledger-service/issues/01-linux-runtime.md \
  && echo "Task 1 gate: OK"
```
Expected: `Task 1 gate: OK`

- [ ] **Step 5: Commit**

```bash
git add ledger-service/issues/07-cicd-vps-deploy.md ledger-service/issues/01-linux-runtime.md
git commit -m "docs(tracker): contrato de segredos de produção nas Issues 01 e 07"
```

### Task 2: Método de schema × rollback (C)

**Files:**
- Modify: `ledger-service/issues/06-compose-isolation.md` — seção `## Limitações / notas` (novo bullet de nível raiz entre o sub-bloco de invariantes e o bullet "última palavra sobre topologia").
- Modify: `ledger-service/issues/07-cicd-vps-deploy.md:73` — requisito de rollback.

**Interfaces:**
- Consume: evidência da auditoria `06:96` (`ddl-auto=update` como fato) e `07:73` (rollback sem estratégia de schema).
- Produz: declaração de que o método de produção são migrações versionadas, com a decisão sobre o `app/` registrada como dívida (mesma natureza do fix `drizzle push` no commerce).

- [ ] **Step 1: Nota de método de schema em `06-compose-isolation.md`**

Bullet de nível raiz em `## Limitações / notas`, contendo obrigatoriamente: `ddl-auto=update` como método de laboratório, `migrações versionadas` como método de produção, e que a troca mexe no `app/` (construção do usuário) e fica registrada como dívida.

- [ ] **Step 2: Rollback condicionado em `07-cicd-vps-deploy.md:73`**

Estender o bullet `- [ ] Healthcheck pós-deploy com rollback automático se falhar` com cláusula contendo `compatibilidade de schema` entre a versão nova e a revertida, como pré-condição declarada antes do primeiro rollback.

- [ ] **Step 3: Gate da Task 2**

Run:
```bash
set -euo pipefail
grep -q 'migrações versionadas' ledger-service/issues/06-compose-isolation.md \
  && grep -q 'ddl-auto=update' ledger-service/issues/06-compose-isolation.md \
  && grep -q 'compatibilidade de schema' ledger-service/issues/07-cicd-vps-deploy.md \
  && echo "Task 2 gate: OK"
```
Expected: `Task 2 gate: OK`

- [ ] **Step 4: Commit**

```bash
git add ledger-service/issues/06-compose-isolation.md ledger-service/issues/07-cicd-vps-deploy.md
git commit -m "docs(tracker): método de schema declarado e rollback condicionado a compatibilidade"
```

### Task 3: Escopo do actuator e auth do coletor (E)

**Files:**
- Modify: `ledger-service/issues/07-cicd-vps-deploy.md:114` — nota de `permitAll`.
- Modify: `ledger-service/issues/04-caddy-reverse-proxy.md:61` — requisito de roteamento.
- Modify: `ledger-service/issues/08-trafego-sintetico-alertas.md:64` — requisito do coletor.

**Interfaces:**
- Consume: evidência da auditoria (`07:114` blanket `permitAll`; `04:61` sem scoping; `08:64` série visível sem auth) e o contexto de que só `health`, `info`, `metrics` e `prometheus` estão no conjunto de exposição.
- Produz: escopo de caminho usado pela validação de proxy de qualquer Issue futura desta trilha.

- [ ] **Step 1: Nota de `permitAll` escopada em `07-cicd-vps-deploy.md:114`**

Substituir o bullet `- /actuator/** precisa continuar permitAll ...` por nota contendo: `permitAll` restrito a `/actuator/health` e `/actuator/prometheus` (healthcheck e scraping), e os demais endpoints do actuator inalcançáveis pelo proxy público. O literal `/actuator/**` não pode mais existir no arquivo.

- [ ] **Step 2: Scoping de caminho em `04-caddy-reverse-proxy.md:61`**

Estender o bullet `- [ ] Rotejar por domínio para upstreams internos` com cláusula contendo: apenas os `caminhos declarados` de cada upstream alcançam o proxy, com os caminhos do actuator citados (`/actuator/health`, `/actuator/prometheus`).

- [ ] **Step 3: Auth do coletor em `08-trafego-sintetico-alertas.md:64`**

Estender o bullet do coletor com cláusula contendo obrigatoriamente o texto `painel/API do coletor` + `autenticação` — acesso restrito declarado junto com a série visível. (Token distinto de propósito: `autenticação` já existe em `08:46` e não discrimina a edição.)

- [ ] **Step 4: Gate da Task 3**

Run:
```bash
set -euo pipefail
! grep -qF '/actuator/**' ledger-service/issues/07-cicd-vps-deploy.md \
  && grep -q 'permitAll' ledger-service/issues/07-cicd-vps-deploy.md \
  && grep -q 'caminhos declarados' ledger-service/issues/04-caddy-reverse-proxy.md \
  && grep -q 'painel/API do coletor' ledger-service/issues/08-trafego-sintetico-alertas.md \
  && echo "Task 3 gate: OK"
```
Expected: `Task 3 gate: OK`

- [ ] **Step 5: Commit**

```bash
git add ledger-service/issues/07-cicd-vps-deploy.md ledger-service/issues/04-caddy-reverse-proxy.md ledger-service/issues/08-trafego-sintetico-alertas.md
git commit -m "docs(tracker): actuator escopado no proxy e coletor com autenticação declarada"
```

### Task 4: Staging definido na trilha + correção do BOARD (F)

**Files:**
- Modify: `ledger-service/issues/07-cicd-vps-deploy.md` — seção `## Limitações / notas` (novo bullet ao final).
- Modify: `BOARD.md:20` — linha da seção `## Produção e staging`.

**Interfaces:**
- Consume: evidência da auditoria (`07:71/82` exige staging indefinido; `BOARD.md:20` aponta `ledger 08`, que é produção `08:8/16`).
- Produz: definição única de staging desta trilha, consumida pelo BOARD e pela validação da Issue 07.

⚠️ Esta é a única edição fora de `ledger-service/issues/`. A linha alterada do `BOARD.md` é a do `Staging`; nenhum checkbox, `status:` ou outra linha do board muda.

- [ ] **Step 1: Nota "Staging nesta trilha" em `07-cicd-vps-deploy.md`**

Bullet ao final de `## Limitações / notas`, contendo obrigatoriamente: `Staging nesta trilha`, a definição `aprovação ou filtro de branch`, e que não existe ambiente paralelo nesta trilha.

- [ ] **Step 2: Corrigir `BOARD.md:20`**

Substituir a linha `- **Staging:** ambiente separado, público e **deliberadamente falho**, que nunca toca a produção — \`ledger 08\` (tráfego/alerta), \`commerce 08\` (falha de observabilidade) e \`webhook 11\` (insegurança proposital + forense de mensageria).` por linha que: mantém o prefixo `- **Staging:**`; cita `commerce 08` e `webhook 11` como os cards de staging; **não** contém `ledger 08`; e declara que no `ledger` a separação é aprovação/filtro de branch pela `Issue 07`, sem ambiente paralelo.

- [ ] **Step 3: Gate da Task 4**

Run:
```bash
set -euo pipefail
grep -q 'Staging nesta trilha' ledger-service/issues/07-cicd-vps-deploy.md \
  && ! grep -qF 'ledger 08' BOARD.md \
  && grep -qF 'webhook 11' BOARD.md \
  && echo "Task 4 gate: OK"
```
Expected: `Task 4 gate: OK`

- [ ] **Step 4: Commit**

```bash
git add ledger-service/issues/07-cicd-vps-deploy.md BOARD.md
git commit -m "docs(tracker): staging definido na trilha VPS e correção do card de staging no BOARD"
```

- [ ] **Step 5: Bateria final de validação**

Executar **depois** dos 4 commits (árvore limpa): os ranges são `HEAD~4` (as 4 tasks) e `HEAD~1` (só a Task 4, dona da linha do `BOARD.md`) — âncoras relativas, independentes do base do branch.

Run:
```bash
set -euo pipefail
[ -z "$(git status --porcelain)" ] || { echo "ÁRVORE SUJA: execute após os 4 commits"; exit 1; }
for f in ledger-service/issues/*.md; do
  [ "$(grep -c '^## ' "$f")" -eq 13 ] || { echo "SEÇÃO ERRADA: $f"; exit 1; }
done
[ "$(grep -h '^status:' ledger-service/issues/*.md | grep -c '^status: done$')" -eq 2 ] \
  || { echo "STATUS ERRADO (esperado 2 done)"; exit 1; }
[ "$(grep -h '^status:' ledger-service/issues/*.md | grep -c '^status: todo$')" -eq 6 ] \
  || { echo "STATUS ERRADO (esperado 6 todo)"; exit 1; }
out=$(git diff --name-only HEAD~4 HEAD -- | grep -cv '^ledger-service/issues/\|^BOARD.md$\|^docs/plano-correcoes-')
[ "$out" -eq 0 ] || { echo "ESCOPO ERRADO: $out arquivos fora"; exit 1; }
[ "$(git diff --unified=0 HEAD~1 HEAD -- BOARD.md | grep -E '^[+-][^+-]' | grep -vc 'Staging')" -eq 0 ] \
  || { echo "BOARD ERRADO: linha fora da seção Staging alterada"; exit 1; }
broken=0
for f in ledger-service/issues/*.md; do
  d=$(dirname "$f")
  while IFS= read -r t; do
    t=${t%%#*}; [ -z "$t" ] && continue; case "$t" in http*) continue;; esac
    [ -f "$d/$t" ] || { echo "LINK QUEBRADO: $f -> $t"; broken=1; }
  done < <(grep -oE '\]\([^)]*\.md\)' "$f" | sed 's/^](//; s/)$//')
done
[ "$broken" -eq 0 ] || exit 1
git diff HEAD~4 HEAD -- ledger-service BOARD.md \
  | grep '^+' | grep -vE '^\+\+\+|^\+ *- ' && { echo "ESTILO ERRADO"; exit 1; } || true
echo "VALIDAÇÃO FINAL: OK — 8 issues × 13 seções, status 2 done/6 todo, diff só em issues+BOARD+planos desta branch, BOARD só na linha Staging, links e estilo OK"
```
Expected: `VALIDAÇÃO FINAL: OK — ...` e nenhuma linha de erro antes dela. Se falhar: corrigir, commitar o ajuste e reexecutar a bateria antes de considerar a planilha concluída.

---

## Dependências entre planos

- A Task 1 deste plano é o que faz o ponteiro `webhook-gateway/issues/04-secrets-hygiene.md:87` ("o equivalente do ledger é trabalho de lá") ser verdadeiro. Se o plano do `webhook-gateway` executar antes deste, aquele ponteiro segue órfão temporariamente (consta nos Diferidos dele).

## Diferidos (apêndice — sem task)

- **`05:86` — LocalStack como destino de backup válido:** flagar que a prova de produção usa provedor real durável, emulador só como atalho de lab.
- **`04:91` — "health check do target group" fantasma:** não existe target group nesta trilha (sem `infra/`); remover ou referenciar o que realmente faz health check atrás do Caddy.
- **`08:50` — drift de endpoints:** a Issue diz `/actuator/health` e `/actuator/prometheus`, a config real do app expõe `health,info,metrics,prometheus`; declarar a lista exata como contrato.
