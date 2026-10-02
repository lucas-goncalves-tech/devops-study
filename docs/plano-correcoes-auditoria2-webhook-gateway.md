# Correções auditoria-30-Issues — webhook-gateway Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corrigir no tracker da trilha DevSecOps os achados Crítico/Importante da auditoria de 30 Issues de 02/10/2026 atribuídos a este app — contrato do Redis da `12` (variável de host + rede coerente com o `ledger 06`), dona para proteção de branch, dois defeitos de honestidade na pipeline (`07` invertido, `06` prometendo SBOM), bind de porta da `02`, e as três promessas inviáveis do staging `11` (host, evidências de "produção", escopo de semanas) — sem tocar em status, template ou código.

**Architecture:** Edits em markdown em `webhook-gateway/issues/` (`02`, `03`, `06`, `07`, `11`, `12`). Cinco tasks, um commit cada. Nenhum arquivo de `app/`, `.github/`, `scripts/` ou IaC é editado.

**Tech Stack:** Tracker markdown (template RFC de 13 seções), gates `bash`/`grep` com linha `OK`, bateria final com `awk`/`git`.

**Spec — achados desta auditoria destinados a este plano (C/I; Menores em `Diferidos`):**

- **C3** — `12:75/77` exigem `REDIS_ENABLED=true` mas nunca declaram `SPRING_DATA_REDIS_HOST`; o default do código é `localhost` (`ledger-service` `application.yml:23`), então em produção o host resolve para o próprio container e `/actuator/health` cai para `503`.
- **I-rede** (achado contábil do `ledger 06`, corrigido aqui) — `12:75` diz rede "interna com a aplicação"; `ledger 06:102` obriga o Redis na rede **isolada** da topologia daquela trilha.
- **I-branch** — `04:94`, `05:65` e `08:99` pressupõem proteção de branch configurada na `03`, que não a declara (requisito, critério, validação, evidência).
- **I-flip** — `07:67` ("nenhum job com permissão de escrita publica artefato") **inverte** `07:59` ("restringir escrita a apenas os jobs que publicam").
- **I-SBOM** — `06:36` promete SBOM/assinatura para a `07`, que não os entrega em nenhuma linha.
- **I-bind** — `02:76/120` publicam a porta da rota de saúde sem bind em `127.0.0.1` (default do Docker = `0.0.0.0`).
- **I-11a** — `11:72` exige "endereço público" sem declarar quem fornece (custo zero = túnel gratuito).
- **I-11b** — `11:83/87/98/102/109/113/122` exigem evidências da "stack de produção", que só existe após a `12` (posterior a esta Issue).
- **I-11c** — `11` é a maior Issue da trilha sem marcos; `11:70/72` descrevem semanas de trabalho como um único passo.

**Ordem:** este é 3 de 3 planos da rodada (`ledger`, `commerce`, `webhook`) executados no **mesmo branch** criado de `main@481a2c5` (via `superpowers:using-git-worktrees` na execução). Ordem entre os planos é indiferente; cada um tem sua bateria final com escopo em união dos três apps.

## Global Constraints

- Arquivos editáveis: apenas `webhook-gateway/issues/*.md` e este plano em `docs/`. Intocáveis: `BOARD.md`, frontmatter (`status:`, `prioridade:`, `tags:`), `archive/`, `app/`, `.github/`, `scripts/`. **Zero mudança de `status:`; nenhum checkbox vira `[x]`** (as 3 Issues `done` — `03`, `08`, `09` — mantêm o par requisito↔critério e os `[x]` originais; a Task 2 acrescenta itens **desmarcados** à `03`).
- Cada Issue mantém as 13 seções `## `; só acrescentamos/removemos bullets dentro de seções existentes.
- PT-BR, RFC da casa: sem tutorial, FAQ, navegação (`Prev`/`Next`) nem sub-etapas (`1A`, `2B`).
- Um commit por task; cada gate roda com `set -euo pipefail` e imprime `Task N gate: OK`; gate vermelho = corrigir antes do próximo commit.
- `B0=$(git merge-base main HEAD)` (esperado `481a2c5`).
- Nenhuma task executa comando de runtime — este plano é 100% texto de tracker.

## Review Focus

- Tokens pinados exatamente como escritos; âncoras de substituição únicas.
- Rulings embutidos no texto: `SPRING_DATA_REDIS_HOST=redis` exigido com build-to-break; `12` herda a topologia do `ledger 06` (rede isolada + rede compartilhada com a app); proteção de branch é **da `03`** (as ponteiras da `04`/`05`/`08` passam a ser verdadeiras sem edição); SBOM vira limite declarado, não promessa; staging usa **túnel gratuito**; "produção" da `11` = a stack da `Issue 10` (o que existe antes da `12`).
- `04`, `05`, `08` **não são editados** — o achado de branch protection se resolve declarando a capacidade na `03`.
- Nenhum `- [x]` novo, nenhum `status:` alterado, 13 seções por Issue.

---

### Step 0: Commit do plano

- [ ] Copiar este arquivo para o worktree e commitá-lo antes da Task 1:

```bash
git add docs/plano-correcoes-auditoria2-webhook-gateway.md
git commit -m "docs(tracker): plano de correção auditoria2 webhook-gateway"
```

### Task 1: Contrato do Redis na 12 — host e rede (C3 + I-rede)

**Files:**
- Modify: `webhook-gateway/issues/12-integracao-producao.md` — requisito `:75`, `## Critérios de aceitação` (novo após `:95`), `## Validação` (novo após `:105`), `## Evidências` (novo após `:114`).

**Interfaces:**
- Consume: default `localhost` do `ledger-service` `application.yml:23` e a topologia de três redes da [`Issue 06` do `ledger-service`](../../ledger-service/issues/06-compose-isolation.md) (`06:59/102` — rede isolada + invariante de rede compartilhada).
- Produz: variável de host exigida (não opcional) e rede coerente com a trilha do `ledger`; fecha também o achado de divergência `06×12` contábil do app `ledger`.

- [ ] **Step 1: Requisito `:75`**

Substituir o requisito inteiro por (mantendo `healthcheck`, `sem porta publicada`, `senha` e `REDIS_URL` do gateway):

`- [ ] Serviço \`redis\` na stack do \`ledger-service\`, com healthcheck, sem porta publicada, na rede isolada da topologia da [\`Issue 06\`](../../ledger-service/issues/06-compose-isolation.md) do \`ledger-service\` (o \`redis\` e a aplicação dividem rede como aquele invariante exige) e autenticação por \`senha\` injetada via ambiente — \`REDIS_URL\` do gateway com a credencial, e \`SPRING_DATA_REDIS_HOST=redis\` (nome do serviço) + \`SPRING_DATA_REDIS_PASSWORD\` no ambiente do \`ledger\``

- [ ] **Step 2: Critério (novo)**

Novo `- [ ]` após o último critério (`:95`) contendo: `\`SPRING_DATA_REDIS_HOST=redis\` está declarada no ambiente do \`ledger\``, \`/actuator/health\` responde \`UP\` com o publisher ligado` e `build to break: sem a variável a saúde cai para \`503\``.

- [ ] **Step 3: Validação (nova)**

Novo item após o último (`:105`) contendo: `Remover \`SPRING_DATA_REDIS_HOST\` do ambiente`, `observar \`503\``, `restaurar e confirmar \`UP\`` e `conferir na stack que o \`redis\` está na rede isolada da \`Issue 06\``.

- [ ] **Step 4: Evidência (nova)**

Novo item após o último (`:114`): `- Saída do \`UP\` com a variável e do \`503\` sem ela`.

- [ ] **Step 5: Gate da Task 1**

Run:
```bash
set -euo pipefail
f=webhook-gateway/issues/12-integracao-producao.md
grep -qF 'SPRING_DATA_REDIS_HOST=redis' "$f" \
  && grep -qF 'rede isolada' "$f" \
  && ! grep -qF 'em rede interna com a aplicação' "$f" \
  && grep -qF '503' "$f" \
  && echo "Task 1 gate: OK"
```
Expected: `Task 1 gate: OK`

- [ ] **Step 6: Commit**

```bash
git add webhook-gateway/issues/12-integracao-producao.md
git commit -m "docs(tracker): host e rede do Redis exigidos na 12 — contrato coerente com a 06 do ledger"
```

### Task 2: Proteção de branch declarada na 03 (I-branch)

**Files:**
- Modify: `webhook-gateway/issues/03-pipeline-base-agnostica.md` — `## Requisitos` (novo após `:67`), `## Critérios de aceitação` (novo após `:76`), `## Validação` (novo após `:85`), `## Evidências` (novo após `:94`).

**Interfaces:**
- Consume: check `build` que a própria `03` cria (requisitos `:65-66`) e as ponteiras existentes `04:94` ("proteção de branch configurada na Issue 03"), `05:65` ("obrigatório"), `08:99` ("barreira única do merge").
- Produz: a capacidade que todas pressupõem — daqui em diante os três ponteiros são verdadeiros **sem editar** `04`, `05` ou `08`.

⚠️ A `03` é `done`: os itens novos entram **desmarcados** (`- [ ]`), como promessa de reexecução; nenhum `[x]` existente muda.

- [ ] **Step 1: Requisito**

Novo `- [ ]` após `- [ ] Registrar no README do app (ou no próprio script) o comando local equivalente à pipeline` contendo: `Configurar proteção de branch no branch padrão do repositório (Rulesets/branch protection)`, `exigindo o check \`build\` verde antes do merge`, `vale para pull request e para push direto`.

- [ ] **Step 2: Critério**

Novo `- [ ]` após o último critério (`:76`) contendo: `Um pull request com o check \`build\` pendente ou vermelho não merge` e `um push direto ao branch padrão é recusado`.

- [ ] **Step 3: Validação e Evidências**

- Validação (após `:85`): `- Abrir um pull request e tentar merge com o check pendente, esperando a recusa`.
- Evidências (após `:94`): `- Captura ou log da tentativa de merge recusada`.

- [ ] **Step 4: Gate da Task 2**

Run:
```bash
set -euo pipefail
grep -qF 'proteção de branch no branch padrão do repositório' webhook-gateway/issues/03-pipeline-base-agnostica.md \
  && grep -qF 'não merge' webhook-gateway/issues/03-pipeline-base-agnostica.md \
  && grep -qF 'merge recusada' webhook-gateway/issues/03-pipeline-base-agnostica.md \
  && grep -qF 'proteção de branch configurada na Issue 03' webhook-gateway/issues/04-secrets-hygiene.md \
  && grep -qF 'barreira única do merge' webhook-gateway/issues/08-devsecops-gates.md \
  && echo "Task 2 gate: OK"
```
Expected: `Task 2 gate: OK`

- [ ] **Step 5: Commit**

```bash
git add webhook-gateway/issues/03-pipeline-base-agnostica.md
git commit -m "docs(tracker): proteção de branch declarada na 03 — ponteiros 04/05/08 resolvem"
```

### Task 3: Pipeline honesta — critério corrigido e SBOM como limite (I-flip + I-SBOM)

**Files:**
- Modify: `webhook-gateway/issues/07-pipeline-hardening.md:67` — critério invertido.
- Modify: `webhook-gateway/issues/06-sca-dependencias-imagem.md:36` — promessa de SBOM/assinatura para a `07`.

**Interfaces:**
- Consume: requisito `07:59` ("restringir escrita a apenas os jobs que publicam") e o fato de que `07:58-62` não entregam SBOM nem assinatura.
- Produz: critério que espelha o requisito e limite de trilha declarado (nada prometido para fora).

- [ ] **Step 1: Corrigir `07:67`**

Substituir `- [ ] Nenhum job com permissão de escrita publica artefato` por `- [ ] Os únicos jobs com permissão de escrita são os que publicam artefato — nenhum job sem publicação tem \`write\``.

- [ ] **Step 2: `06:36` vira limite**

Substituir `- SBOM e assinatura de artefato: a imagem assinada é trabalho do pipeline endurecido, [Issue 07](07-pipeline-hardening.md)` por `- SBOM e assinatura de artefato: não são entregues por nenhuma Issue desta trilha — limite registrado aqui; a imagem publicada é validada pelos gates de conteúdo, não assinada`.

- [ ] **Step 3: Verificação de ponteiros**

Conferir que nenhum outro arquivo desta trilha promete SBOM para alguém:

```bash
grep -rn 'SBOM' webhook-gateway/issues/
```
Esperado: apenas a linha nova da `06`. Se aparecer outro ponto, registrar no relatório de execução (não editar fora do escopo desta task).

- [ ] **Step 4: Gate da Task 3**

Run:
```bash
set -euo pipefail
grep -qF 'Os únicos jobs com permissão de escrita são os que publicam' webhook-gateway/issues/07-pipeline-hardening.md \
  && ! grep -qF 'Nenhum job com permissão de escrita publica artefato' webhook-gateway/issues/07-pipeline-hardening.md \
  && grep -qF 'não são entregues por nenhuma Issue desta trilha' webhook-gateway/issues/06-sca-dependencias-imagem.md \
  && [ "$(grep -rln 'SBOM' webhook-gateway/issues/ | wc -l)" -eq 1 ] \
  && echo "Task 3 gate: OK"
```
Expected: `Task 3 gate: OK`

- [ ] **Step 5: Commit**

```bash
git add webhook-gateway/issues/07-pipeline-hardening.md webhook-gateway/issues/06-sca-dependencias-imagem.md
git commit -m "docs(tracker): critério de escrita corrigido e SBOM declarado como limite da trilha"
```

### Task 4: Bind de loopback na porta da rota de saúde (I-bind)

**Files:**
- Modify: `webhook-gateway/issues/02-docker-compose.md` — requisito `:76`, nota `:120`.

**Interfaces:**
- Consume: padrão da casa (nenhuma porta de serviço em `0.0.0.0/0`) e o critério da própria `02` (`:89`, `.env` real nunca versionado).
- Produz: publicação opcional, mas sempre em `127.0.0.1`.

- [ ] **Step 1: Requisito `:76`**

Substituir `- [ ] Publicar no host apenas a porta \`PORT\` (default \`8081\`) da rota de saúde, se e somente se a validação exigir consultá-la de fora` por `- [ ] Publicar no host apenas a porta \`PORT\` (default \`8081\`) da rota de saúde, e somente em loopback (\`127.0.0.1:\${PORT:-8081}:\${PORT:-8081}\`), se e somente se a validação exigir consultá-la de fora`.

- [ ] **Step 2: Nota `:120`**

Substituir a nota `- Publicar a porta \`PORT\` (default \`8081\`) da rota de saúde no host é opcional e só serve para validação externa; em produção ela não precisa ser alcançável de fora` por versão contendo: `é opcional`, `só serve para validação externa`, `em produção ela não precisa ser alcançável de fora` e `— e o bind é sempre \`127.0.0.1\`, nunca \`0.0.0.0\``.

- [ ] **Step 3: Gate da Task 4**

Run:
```bash
set -euo pipefail
f=webhook-gateway/issues/02-docker-compose.md
grep -qF '127.0.0.1:${PORT:-8081}:${PORT:-8081}' "$f" \
  && grep -qF 'nunca `0.0.0.0`' "$f" \
  && echo "Task 4 gate: OK"
```
Expected: `Task 4 gate: OK`

- [ ] **Step 4: Commit**

```bash
git add webhook-gateway/issues/02-docker-compose.md
git commit -m "docs(tracker): porta da rota de saúde com bind de loopback na 02"
```

### Task 5: Staging 11 honesto — host, evidências e marcos (I-11a/b/c)

**Files:**
- Modify: `webhook-gateway/issues/11-staging-inseguro.md` — escopo `:30`, requisitos `:70`, `:72`, `:83`, critérios `:87`, `:98`, validação `:102`, `:109`, evidências `:113`, `:122`, nota nova em `## Limitações / notas`.

**Interfaces:**
- Consume: dependências da `11` (`:20-26` — só gates + `09` + `10`) e o fato de que a integração de produção é a `12` (posterior).
- Produz: host com fornecedor declarado (túnel gratuito, custo zero), evidências contra a stack que existe (`Issue 10`) e marcos de execução — a `11` passa a ser verificável.

⚠️ A `11` é `done`: as substituições trocam texto de critérios/validações/evidências sem tocar em `[x]` (seções sem checkbox ou com pares intactos — os itens alterados são listas simples de `## Validação`/`## Evidências` e checkboxes já `[x]` mantêm o `- [x]`).

- [ ] **Step 1: Host via túnel (`:72`)**

Substituir `- [ ] Injetar e documentar porta exposta do serviço de staging em endereço público, com a exposição visível por varredura externa` por `- [ ] Injetar e documentar porta exposta do serviço de staging em endereço público fornecido por túnel gratuito (cloudflared ou ngrok — custo zero, URL registrada nesta Issue), com a exposição visível por varredura externa`.

- [ ] **Step 2: "Produção" → stack da `Issue 10`**

Substituir nas linhas indicadas (uma a uma, mantendo o restante da frase):

| Linha | De (trecho) | Para (trecho) |
|---|---|---|
| `:30` (escopo) | `sem recurso compartilhado com a produção` | `sem recurso compartilhado com a stack da \`Issue 10\`` |
| `:70` | `sem recurso compartilhado com a produção` | `sem recurso compartilhado com a stack da \`Issue 10\`` |
| `:83` | `que a stack de produção permaneceu intacta … na produção` | `que a stack da \`Issue 10\` permaneceu intacta … nela` |
| `:87` | `namespace da produção` | `namespace da stack da \`Issue 10\`` |
| `:98` | `A stack de produção permaneceu intacta` | `A stack da \`Issue 10\` permaneceu intacta` |
| `:102` | `do staging e da produção` | `do staging e da stack da \`Issue 10\`` |
| `:109` | `o log da produção` | `o log da stack da \`Issue 10\`` |
| `:113` | `do staging e da produção lado a lado` | `do staging e da stack da \`Issue 10\` lado a lado` |
| `:122` | `Log da produção` | `Log da stack da \`Issue 10\`` |

Manter `:16` (Objetivo, "separado da produção" = intenção de desenho) e `:42` (Fora de escopo, "escalar as fraquezas para produção") — são conceituais.

- [ ] **Step 3: Nota de marcos (nova ao final de `## Limitações / notas`)**

Bullet contendo obrigatoriamente: `Esta é a Issue longa da trilha`, os quatro blocos `(I) staging + fraquezas + matriz`, `(II) varredura DAST`, `(III) forense dos três incidentes de mensageria`, `(IV) evidências finais`, `executáveis em etapas com validação própria`, `registrar o avanço por bloco`, e a regra de leitura: \`Produção\` nesta Issue é a stack da \`Issue 10\` — o que existe antes da integração real da \`Issue 12\`.

- [ ] **Step 4: Gate da Task 5**

Run:
```bash
set -euo pipefail
f=webhook-gateway/issues/11-staging-inseguro.md
grep -qF 'túnel gratuito' "$f" \
  && grep -qF 'stack da `Issue 10`' "$f" \
  && ! grep -qF 'stack de produção' "$f" \
  && grep -qF 'quatro blocos' "$f" \
  && grep -qF 'Issue 12' "$f" \
  && echo "Task 5 gate: OK"
```
Expected: `Task 5 gate: OK`

- [ ] **Step 5: Commit**

```bash
git add webhook-gateway/issues/11-staging-inseguro.md
git commit -m "docs(tracker): staging 11 com host, evidências e marcos declarados"
```

- [ ] **Step 6: Bateria final de validação**

Executar **depois** dos commits (árvore limpa). Mesma construção dos planos irmãos: escopo em **união dos três apps + `docs/plano-correcoes-`** (branch compartilhada), estilo em `*/issues/` com blocos cercados isentos via `awk`, `[x]` comparado contra `B0`, `BOARD.md` intocado nesta rodada. Census deste app: **12 `todo`** (as três `done` continuam `done`).

Run:
```bash
set -euo pipefail
[ -z "$(git status --porcelain)" ] || { echo "ÁRVORE SUJA: execute após os commits"; exit 1; }
B0=$(git merge-base main HEAD)
for f in ledger-service/issues/*.md commerce-api/issues/*.md webhook-gateway/issues/*.md; do
  [ "$(grep -c '^## ' "$f")" -eq 13 ] || { echo "SEÇÃO ERRADA: $f"; exit 1; }
done
[ "$(grep -h '^status:' ledger-service/issues/*.md | grep -c '^status: done$')" -eq 2 ] || { echo "STATUS ERRADO ledger (esperado 2 done)"; exit 1; }
[ "$(grep -h '^status:' ledger-service/issues/*.md | grep -c '^status: todo$')" -eq 6 ] || { echo "STATUS ERRADO ledger (esperado 6 todo)"; exit 1; }
[ "$(grep -h '^status:' commerce-api/issues/*.md | grep -c '^status: todo$')" -eq 9 ] || { echo "STATUS ERRADO commerce (esperado 9 todo)"; exit 1; }
[ "$(grep -h '^status:' commerce-api/issues/*.md | grep -c '^status: parked$')" -eq 1 ] || { echo "STATUS ERRADO commerce (esperado 1 parked)"; exit 1; }
[ "$(grep -h '^status:' webhook-gateway/issues/*.md | grep -c '^status: todo$')" -eq 12 ] || { echo "STATUS ERRADO webhook (esperado 12 todo)"; exit 1; }
for f in ledger-service/issues/*.md commerce-api/issues/*.md webhook-gateway/issues/*.md; do
  a=$(git show "$B0:$f" | grep -c '^- \[x\]' || true)
  b=$(grep -c '^- \[x\]' "$f" || true)
  [ "$a" -eq "$b" ] || { echo "CHECKBOX ERRADO: $f mudou de estado ($a → $b)"; exit 1; }
done
out=$(git diff --name-only "$B0" HEAD | grep -Evc '^ledger-service/issues/|^ledger-service/AGENTS\.md$|^ledger-service/README\.md$|^commerce-api/issues/|^webhook-gateway/issues/|^docs/plano-correcoes-' || true)
[ "$out" -eq 0 ] || { echo "ESCOPO ERRADO: $out arquivos fora"; exit 1; }
[ -z "$(git diff --name-only "$B0" HEAD -- BOARD.md)" ] || { echo "BOARD MUDOU: nesta rodada o board não é editado"; exit 1; }
broken=0
for f in ledger-service/issues/*.md commerce-api/issues/*.md webhook-gateway/issues/*.md; do
  d=$(dirname "$f")
  while IFS= read -r t; do
    t=${t%%#*}; [ -z "$t" ] && continue; case "$t" in http*) continue;; esac
    [ -f "$d/$t" ] || { echo "LINK QUEBRADO: $f -> $t"; broken=1; }
  done < <(grep -oE '\]\([^)]*\.md\)' "$f" | sed 's/^](//; s/)$//')
done
[ "$broken" -eq 0 ] || exit 1
git diff "$B0" HEAD -- ledger-service/issues commerce-api/issues webhook-gateway/issues \
  | grep '^+' \
  | awk '/^\+\+\+/{next} /^\+ *\x60\x60\x60/{inb=!inb; next} inb{next} /^\+ *- /{next} /^\+$/{next} {print}' \
  | grep . && { echo "ESTILO ERRADO"; exit 1; } || true
echo "VALIDAÇÃO FINAL: OK — 30 issues × 13 seções, status 2/6+9/1+12 preservados, escopo união dos 3 apps + docs, BOARD intocado, checkboxes B0, links e estilo (blocos cercados isentos) OK"
```
Expected: `VALIDAÇÃO FINAL: OK — …` e nenhuma linha de erro antes dela. Se falhar: corrigir, commitar o ajuste e reexecutar a bateria antes de considerar o plano concluído.

---

## Dependências entre planos

- A **Task 1** fecha também o achado **I-rede** (`ledger 06` × `webhook 12`) contábil no app `ledger` — o plano `ledger-service` não o edita.
- A **Task 2** resolve o achado de branch protection das `04`/`05`/`08` sem editar nenhuma delas (só a `03` declara a capacidade).
- As baterias cobrem os três apps em união (branch compartilhada).

## Diferidos (apêndice — sem task)

Escopo aprovado = 3 Críticos + 20 Importantes. Estes **Menores** da auditoria ficam registrados, não corrigidos:

- **`02:34` × `:118`** — `Fora de escopo` manda a segmentação de rede para a `ledger 06`, as Limitações dizem segmentação é da `10` deste app.
- **`10:35`** — lista de gates esquece a `06` (SCA).
- **`01:20`** — ponteiro "Issue 10 (entrada do gateway em produção)" deveria ser a `12`.
- **`12:78`, `:79`, `:82`** — requisitos que mandam declarar sem declarar (caminho da imagem, variável de destino de entrega, arquivos alterados fora do app).
- **`04:93`** — "não há `.env.example` neste app" falso na ordem do `BOARD` (a `02` o cria antes).
- **`04:63`** — ciclo de vida do `WEBHOOK_SECRET` sem dono/cadência de rotação.
- **`01:62`/`:71`, `05:61`, `10:74`** — requisitos sem Critério correspondente.
- **`12:88`** — "a stack volta completa" sem requisito de política de restart (`restart: unless-stopped`).
- **`01:48`/`:110`, `10:72`** — alerta de produção sem dono (limite da trilha não declarado).
