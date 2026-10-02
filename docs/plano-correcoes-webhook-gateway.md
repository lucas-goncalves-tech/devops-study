# Correções dev≠prod — webhook-gateway Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corrigir no tracker da trilha DevSecOps os três achados Importante da auditoria dev≠prod de 02/10/2026 — contrato do destino de entrega, ciclo de vida do `WEBHOOK_SECRET` e autenticação do Redis de produção — sem tocar em status, template nem código.

**Architecture:** Edits só em markdown do tracker: novos requisitos, critérios e notas **aditivos** (mais uma substitição de linha em `Fora de escopo`) dentro das 13 seções existentes. Nenhum `status:`, nenhum arquivo de `estudos/` e nenhum arquivo fora de `webhook-gateway/issues/` mudam — os achados não adicionam itens novos em `Conhecimentos envolvidos`, então o pareamento com estudos não é acionado.

**Tech Stack:** Tracker markdown (template RFC de 13 seções), gates `bash`/`grep`/`git` que imprimem linha `OK`.

**Spec:** A auditoria que motiva este plano está embutida na seção **Contexto da auditoria** abaixo (não há arquivo de spec separado — pedido do usuário foi um plano por app em `docs/`).

---

## Contexto da auditoria (spec)

Classes julgadas nesta trilha (evidência antes de corrigir):

- **A — valores de lab fixos: COBERTO.** Defaults flaggados na origem (`01:65`, `02:116`), DAST nunca contra produção (`09:37/78/103`), workflow sem literais por construção (`03:65/73`).
- **B — config de lab vazando: COBERTO.** `12:77` vira `REDIS_ENABLED=true`; staging em quarentena (`11:42/131`); porta do health só se validação exigir (`02:76`).
- **C — método de schema: NÃO SE APLICA** (estado vive em Redis Streams; criação de stream/group é contrato declarado na `10:73`).
- **D — contrato de segredos de produção: PROBLEMA PARCIAL (Importante).** Mecanismo existe (`01:64`, `12:79`), mas: rotação é órfã por decisão explícita (`04:33` "não esta Issue" — única ocorrência de "rotação" no repo inteiro); nenhum piso de entropia para o `WEBHOOK_SECRET` de produção (`01:55`); Redis de produção sem autenticação declarada (`12:75`) enquanto o staging exige credencial (`11:70`) — default invertido e nunca enunciado.
- **E — painel sem auth: NÃO SE APLICA** (única superfície HTTP é `/health`; em produção o gateway não publica porta — `12:80`).
- **F — decisões sem dono: PROBLEMA PARCIAL (Importante).** `12:79` exige configurar "destino de entrega" sem nome de variável, sem exigir `https` e sem declarar quem segura o `WEBHOOK_SECRET` do lado do receptor; `BOARD.md:19` documenta o consumidor como "sem card", e a evidência "nas três pontas" (`12:85`) precisa desse limite declarado. Drift `PORT`/destino fora do `AGENTS.md` do app e o ponteiro `04:87` → **Diferidos**.

## Global Constraints

- Escopo de escrita deste plano: **apenas** `webhook-gateway/issues/**`. Qualquer outro arquivo = falha.
- Zero mudança em `status:`/frontmatter. Status esperado ao final: `todo` ×12.
- Template intacto: 13 seções `## ` por Issue, na ordem de `00-visao-geral.md`. Edits só adicionam/substituem bullets dentro de seções já existentes.
- Texto novo em PT-BR, estilo da casa: bullet `- `, notas em `- **Título:** ...`, sem tutorial/FAQ/sub-etapas dentro de Issue.
- Não adicionar itens novos em `Conhecimentos envolvidos` (pareamento com `estudos/` só seria acionado por isso; nenhum achado exige).
- Todo gate de verificação imprime uma linha (`echo "...: OK"`) — comando silencioso morre sob `pipefail`.
- Links `.md` novos resolvem relativos ao arquivo que os contém (aqui: `../../BOARD.md` para a raiz).
- Um commit por task, mensagem `docs(tracker): ...`.

## Review Focus

1. **Status intocado** — as 12 Issues seguem `todo`. Gate: bateria final da Task 3.
2. **Template** — as 12 Issues seguem com exatamente 13 seções `## `. Gate: bateria final da Task 3.
3. **Escopo de diff** — só `webhook-gateway/issues/`. Gate: bateria final da Task 3.
4. **`04:33` honesto** — a linha velha de rotação "não esta Issue" não pode sobrar junto da política nova (contradição interna). Gate: Task 2.
5. **Estilo e links** — toda linha nova começa com bullet; links relativos resolvem. Gate: bateria final da Task 3.

---

### Task 1: Contrato do destino de entrega (F/D)

**Files:**
- Modify: `webhook-gateway/issues/12-integracao-producao.md` — `## Requisitos` (expandir o bullet de configuração do gateway; novo bullet após ele) e `## Limitações / notas` (novo bullet ao final).

**Interfaces:**
- Consume: evidência da auditoria (`12:79` destino sem nome; `12:85` três pontas; `BOARD.md:19` consumidor sem card).
- Produz: contrato de configuração do destino consumido por quem implementar a integração e pela revisão de evidência da Issue 12.

- [ ] **Step 1: Expandir o requisito de configuração em `12-integracao-producao.md:79`**

Estender o bullet `- [ ] Configuração do gateway (\`REDIS_URL\`, \`WEBHOOK_SECRET\`, destino de entrega) por ambiente, fora do repositório` contendo obrigatoriamente: `destino de entrega` com nome de `variável própria` declarada nesta Issue e exigência de `https` em produção.

- [ ] **Step 2: Novo requisito de compartilhamento do segredo em `12-integracao-producao.md`**

Bullet logo após o do Step 1, contendo obrigatoriamente: `WEBHOOK_SECRET` compartilhado com o `receptor` do destino pelo mesmo canal de ambiente (fora do repositório), sustentando a verificação de HMAC do critério das três pontas.

- [ ] **Step 3: Nota do limite de evidência em `12-integracao-producao.md`**

Bullet ao final de `## Limitações / notas`, contendo obrigatoriamente: a `terceira ponta` das três pontas é provada contra o `destino de entrega` declarado; o consumidor do e-commerce segue `sem card` no `BOARD.md` (`BOARD.md:19`); a fronteira de escopo não muda nesta Issue.

- [ ] **Step 4: Gate da Task 1**

Run:
```bash
set -euo pipefail
grep -q 'https' webhook-gateway/issues/12-integracao-producao.md \
  && grep -q 'variável própria' webhook-gateway/issues/12-integracao-producao.md \
  && grep -q 'receptor' webhook-gateway/issues/12-integracao-producao.md \
  && grep -q 'terceira ponta' webhook-gateway/issues/12-integracao-producao.md \
  && echo "Task 1 gate: OK"
```
Expected: `Task 1 gate: OK`

- [ ] **Step 5: Commit**

```bash
git add webhook-gateway/issues/12-integracao-producao.md
git commit -m "docs(tracker): contrato do destino de entrega declarado na Issue 12"
```

### Task 2: Ciclo de vida do `WEBHOOK_SECRET` (D)

**Files:**
- Modify: `webhook-gateway/issues/04-secrets-hygiene.md` — `## Fora de escopo` (substituir a linha de rotação), `## Requisitos` (novo bullet ao final) e `## Critérios de aceitação` (novo bullet ao final).

**Interfaces:**
- Consume: evidência da auditoria (`04:33` rotação órfã — única ocorrência no repo; `01:55` sem piso de entropia).
- Produz: política de ciclo de vida que a Issue 12 (receptor) e o contrato do `ledger` referenciam; torna o ponteiro `04:87` internamente consistente.

- [ ] **Step 1: Substituir a linha de rotação em `04-secrets-hygiene.md:33`**

Substituir o bullet `- Rotação de credenciais já vazadas — operação, não esta Issue` por bullet que: mantém a rotação **reativa por incidente** como operação (fora de escopo honesto) e declara que a **política de rotação** do `WEBHOOK_SECRET` (gatilho e troca coordenada) é requisito desta Issue. A frase `não esta Issue` não pode mais existir no arquivo.

- [ ] **Step 2: Novo requisito de ciclo de vida em `04-secrets-hygiene.md`**

Bullet ao final de `## Requisitos`, contendo obrigatoriamente: ciclo de vida do `WEBHOOK_SECRET` de produção com geração de no mínimo `32 caracteres`, armazenamento fora do repositório, gatilho de rotação e troca coordenada com o `receptor` do destino.

- [ ] **Step 3: Critério do piso em `04-secrets-hygiene.md`**

Bullet ao final de `## Critérios de aceitação`, contendo obrigatoriamente: o `WEBHOOK_SECRET` declarado no ciclo de vida atende ao piso de `32 caracteres` e distinto do literal versionado de exemplo.

- [ ] **Step 4: Gate da Task 2**

Run:
```bash
set -euo pipefail
! grep -q 'não esta Issue' webhook-gateway/issues/04-secrets-hygiene.md \
  && grep -q 'rotação' webhook-gateway/issues/04-secrets-hygiene.md \
  && grep -q '32 caracteres' webhook-gateway/issues/04-secrets-hygiene.md \
  && grep -q 'receptor' webhook-gateway/issues/04-secrets-hygiene.md \
  && echo "Task 2 gate: OK"
```
Expected: `Task 2 gate: OK`

- [ ] **Step 5: Commit**

```bash
git add webhook-gateway/issues/04-secrets-hygiene.md
git commit -m "docs(tracker): ciclo de vida do WEBHOOK_SECRET declarado na Issue 04"
```

### Task 3: Autenticação do Redis de produção (D)

**Files:**
- Modify: `webhook-gateway/issues/12-integracao-producao.md:75` — requisito do serviço `redis`.
- Modify: `webhook-gateway/issues/12-integracao-producao.md` — `## Critérios de aceitação` (novo bullet ao final) e `## Limitações / notas` (novo bullet ao final).

**Interfaces:**
- Consume: evidência da auditoria (`12:75` prod sem auth vs `11:70` staging com credencial) e o fato de que o publisher do `ledger` consome Redis pelas propriedades padrão `SPRING_DATA_REDIS_*` (contrato do `AGENTS.md` daquele app).
- Produz: decisão de autenticação única da stack de produção, consumida pelos dois lados da integração.

- [ ] **Step 1: Autenticação no requisito do `redis` em `12-integracao-producao.md:75`**

Estender o bullet `- [ ] Serviço \`redis\` na stack do \`ledger-service\`, com healthcheck, sem porta publicada, em rede interna com a aplicação` contendo obrigatoriamente: autenticação por `senha` injetada via ambiente, `REDIS_URL` do gateway com credencial e `SPRING_DATA_REDIS_PASSWORD` no ambiente do `ledger`.

- [ ] **Step 2: Critério de recusa em `12-integracao-producao.md`**

Bullet ao final de `## Critérios de aceitação`, contendo obrigatoriamente: conexão ao `redis` de produção **sem credencial é recusada** (build to break), com a porta `6379` seguindo inalcançável de fora do host.

- [ ] **Step 3: Nota de decisão em `12-integracao-producao.md`**

Bullet ao final de `## Limitações / notas`, contendo obrigatoriamente: a decisão declarada é senha própria em produção (rede interna sozinha `não basta`), na mesma semântica do staging da [Issue 11](11-staging-inseguro.md) — o link precisa resolver.

- [ ] **Step 4: Gate da Task 3**

Run:
```bash
set -euo pipefail
grep -q 'SPRING_DATA_REDIS_PASSWORD' webhook-gateway/issues/12-integracao-producao.md \
  && grep -q 'sem credencial é recusada' webhook-gateway/issues/12-integracao-producao.md \
  && grep -q 'não basta' webhook-gateway/issues/12-integracao-producao.md \
  && echo "Task 3 gate: OK"
```
Expected: `Task 3 gate: OK`

- [ ] **Step 5: Commit**

```bash
git add webhook-gateway/issues/12-integracao-producao.md
git commit -m "docs(tracker): autenticação do redis de produção declarada na Issue 12"
```

- [ ] **Step 6: Bateria final de validação**

Executar **depois** dos commits (árvore limpa): os ranges ancoram em `B=$(git merge-base main HEAD)` — o ponto de branch —, imunes a commits de ajuste entre a primeira e a reexecução. O `grep -cv` do escopo leva `|| true`: contagem zero sai com exit 1 e derrubaria o `set -e` silenciosamente.

Run:
```bash
set -euo pipefail
[ -z "$(git status --porcelain)" ] || { echo "ÁRVORE SUJA: execute após os commits"; exit 1; }
B=$(git merge-base main HEAD)
for f in webhook-gateway/issues/*.md; do
  [ "$(grep -c '^## ' "$f")" -eq 13 ] || { echo "SEÇÃO ERRADA: $f"; exit 1; }
done
[ "$(grep -h '^status:' webhook-gateway/issues/*.md | grep -c '^status: todo$')" -eq 12 ] \
  || { echo "STATUS ERRADO (esperado 12 todo)"; exit 1; }
out=$(git diff --name-only "$B" HEAD -- | grep -cv '^webhook-gateway/issues/' || true)
[ "$out" -eq 0 ] || { echo "ESCOPO ERRADO: $out arquivos fora"; exit 1; }
broken=0
for f in webhook-gateway/issues/*.md; do
  d=$(dirname "$f")
  while IFS= read -r t; do
    t=${t%%#*}; [ -z "$t" ] && continue; case "$t" in http*) continue;; esac
    [ -f "$d/$t" ] || { echo "LINK QUEBRADO: $f -> $t"; broken=1; }
  done < <(grep -oE '\]\([^)]*\.md\)' "$f" | sed 's/^](//; s/)$//')
done
[ "$broken" -eq 0 ] || exit 1
git diff "$B" HEAD -- webhook-gateway \
  | grep '^+' | grep -vE '^\+\+\+|^\+ *- ' && { echo "ESTILO ERRADO"; exit 1; } || true
echo "VALIDAÇÃO FINAL: OK — 12 issues × 13 seções, status 12 todo, diff só em issues, links e estilo OK"
```
Expected: `VALIDAÇÃO FINAL: OK — ...` e nenhuma linha de erro antes dela. Se falhar: corrigir, commitar o ajuste e reexecutar a bateria antes de considerar o plano concluído.

---

## Dependências entre planos

- O ponteiro `04:87` ("o equivalente do ledger é trabalho de lá") só se torna verdadeiro quando o plano do `ledger-service` (Task 1) executar; até lá segue órfão — por isso não é tocado aqui.

## Diferidos (apêndice — sem task)

- **`04:86` — literal `default-webhook-secret-key-32chars`:** dívida conhecida sem Issue dona; o remoção (ou justificativa permanente) precisa de checkbox própria.
- **`02:27/75` e `12:79` fora do `AGENTS.md` do app:** `PORT` e o destino de entrega não constam da tabela de variáveis; sincronismo de doc sem dono.
- **`04:87` — ponteiro para o `ledger`:** órfão até a execução do plano irmão (ver Dependências entre planos).
