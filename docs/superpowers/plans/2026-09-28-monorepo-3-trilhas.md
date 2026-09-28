# Monorepo de 3 trilhas — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transformar `securepay-devops` num monorepo com 3 apps autocontidos (`ledger-service`, `commerce-api`, `webhook-gateway`), cada um com `app/`, `issues/`, `estudos/`, `AGENTS.md`, `README.md`, e um board único na raiz — sem criar nenhuma infra.

**Architecture:** Migração estrutural (moves + renumeração + reescrita de referências) seguida de conteúdo de tracker derivado do spec. O tracker deixa de ser um `.tracker/` único e passa a viver dentro de cada pasta de app; a raiz guarda só board, visão geral e routing.

**Tech Stack:** git (moves preservam histórico em `git mv`), Markdown/Obsidian (frontmatter YAML, links relativos), Java 21/Maven (ledger), Node 20/vitest (commerce, webhook).

**Spec:** `docs/superpowers/specs/2026-09-28-monorepo-3-trilhas-design.md`

## Global Constraints

- **Escopo do agente:** só estrutura + tracker + `AGENTS.md`/`README.md`. **Nunca criar:** `.github/workflows/*`, Terraform, `Dockerfile`, `docker-compose.yaml`, `healthcheck.sh`, código de app. (`.github/workflows/CI.yml` e `healthcheck.sh` ficam byte-a-byte idênticos.)
- **Template fixo de Issue** (seções, nesta ordem): frontmatter (`aliases`, `tags`, `status`, `prioridade`) → `# Issue NN — título` → `Contexto`, `Objetivo`, `Dependências`, `Escopo`, `Fora de escopo`, `Conhecimentos envolvidos`, `Estado atual`, `Resultado esperado`, `Requisitos`, `Critérios de aceitação`, `Validação`, `Evidências`, `Limitações / notas`.
- **Template fixo de Estudo:** frontmatter (`aliases: [estudo-NN]`, `tags: [estudo]`, `issue: NN`) → `# Estudos — Issue NN: ...` → nota "não é escopo da Issue; a `teach-anything` lê este arquivo".
- **Status:** preservar da origem, exceto `commerce-api/03-terraform-vpc.md` → `todo` (infra antiga apagada, refaz do zero). Só `ledger 01`, `ledger 02`, `commerce 04` ficam `done`; `commerce 06` fica `parked`; todo o resto `todo`.
- Proibido em Issue: tutorial, FAQ, links de navegação (`Prev`/`Next`), sub-etapas (`1A`, `2B`). Estudo nunca vira escopo da Issue.
- Linguagem: manter a do material existente (PT-BR).
- Commits pequenos e frequentes — um por Task, mensagem no formato do histórico existente (`chore:`, `docs(spec):`).

## Checadores reutilizáveis

**A — Numeração interna consistente** (título, `aliases` e campo `issue` batem com o prefixo do arquivo):

```bash
python3 - <<'EOF'
import glob, re, sys
bad = []
for p in sorted(glob.glob('*/issues/*.md') + glob.glob('*/estudos/*.md')):
    n = re.match(r'.*/(\d+)-', p).group(1)
    t = open(p, encoding='utf-8').read()
    if '/issues/' in p:
        if not re.search(r'^# Issue ' + n + r' —', t, re.M): bad.append(p + ': título')
        m = re.search(r'aliases: \[issue-(\d+)', t)
        if m and m.group(1) != n: bad.append(p + ': alias')
    else:
        if not re.search(r'^# Estudos — Issue ' + n + r':', t, re.M): bad.append(p + ': título')
        m = re.search(r'issue: (\d+)', t)
        if m and m.group(1) != n: bad.append(p + ': campo issue')
print('\n'.join(bad) or 'NUMERAÇÃO OK'); sys.exit(1 if bad else 0)
EOF
```

**B — Links relativos Markdown** (usar nas Tasks 9, 10 e 13):

```bash
python3 - <<'EOF'
import os, re, sys
bad = []
for root, dirs, files in os.walk('.'):
    dirs[:] = [d for d in dirs if d not in ('.git', 'node_modules', 'dist', '.learning')]
    for f in files:
        if not f.endswith('.md'): continue
        p = os.path.join(root, f)
        for m in re.finditer(r'\]\(([^)#]+?)(?:#[^)]*)?\)', open(p, encoding='utf-8').read()):
            t = m.group(1)
            if t.startswith(('http://', 'https://', 'mailto:')): continue
            if not os.path.exists(os.path.normpath(os.path.join(root, t))): bad.append(f'{p} -> {t}')
print('\n'.join(bad) or 'LINKS OK'); sys.exit(1 if bad else 0)
EOF
```

## Review Focus

1. **Referência numérica órfã** — cada Issue cita outras por número ("Requer Issue 10"); após mover/renumerar, números apontam para Issues erradas ou de outro app. *Teste por Task:* `grep -n "Issue 0\|Issue 1" <app>/issues/*.md` e conferir cada linha contra as tabelas de mapeamento da Task.
2. **Link Markdown quebrado** — `BOARD.md` (18 links) e links cruzados entre apps. *Teste:* script Python de checagem de links relativos (Task 13) deve reportar 0 quebrados.
3. **Caminho físico antigo sobrevivendo** — `backend/`, `infra/`, `.tracker/` citados em Issues, skills ou AGENTS. *Teste:* `grep -rn "backend/\|\.tracker/" AGENTS.md .agents <pasta-de-apps>` = 0 ocorrências.
4. **Testes dos apps quebrados pelo move** — `./mvnw` e `package-lock.json` mudam de path. *Teste:* Maven e npm×2 rodam verdes após cada move (Tasks 1 e 3).
5. **Escopo vazando para infra** — Task nenhuma pode gerar YAML/Dockerfile/Terraform. *Teste:* `git show --stat <commit>` de cada Task não contém esses caminhos; `git diff <base> -- .github/workflows/CI.yml healthcheck.sh` vazio no fim.

---

### Task 1: Baseline e snapshot

**Files:** nenhum criado; só verificação.

- [ ] **Step 1: Confirmar que só há mudança pendente esperada**

Run: `git status --porcelain`
Expected: saída contém apenas `?? .learning/` (nada mais). Se houver outra linha, parar e perguntar ao usuário.

- [ ] **Step 2: Registrar o ponto de partida**

Run: `git log --oneline -1`
Expected: `9b2e4b3 docs(spec): checklist de rollout dos gates DevSecOps...` (ou posterior). Anotar o SHA — é a base de revert.

- [ ] **Step 3: Verificar baseline verde do ledger**

Run: `cd backend && ./mvnw -q test`
Expected: `BUILD SUCCESS`, zero falhas. (Testa antes de mover para poder culpar o move se algo quebrar depois.)

- [ ] **Step 4: Commit** — nenhum (nada mudou).

---

### Task 2: Estrutura do ledger — `backend/` → `ledger-service/app/`, apagar `infra/`

**Files:**
- Move: `backend/*` → `ledger-service/app/`
- Delete: `infra/` inteiro
- Nota: `.gitignore` **não** é alterado aqui (a linha `.tracker/.obsidian/` só muda na Task 12, junto com o move da pasta — assim nunca existe arquivo tracked apontado para caminho ignorado)

**Interfaces:**
- Produces: path `ledger-service/app/` com `mvnw`, `pom.xml`, `src/`, `Dockerfile`, `docker-compose.yaml` — Tasks 4, 7, 13 dependem dele.

- [ ] **Step 1: Criar estrutura e mover com histórico**

```bash
mkdir -p ledger-service/app
git mv backend/* ledger-service/app/
git mv backend/.[!.]* ledger-service/app/ 2>/dev/null || true
rmdir backend
git rm -r infra
```

- [ ] **Step 2: Verificar estrutura**

Run: `ls ledger-service/app`
Expected: `Dockerfile docker-compose.yaml mvnw mvnw.cmd pom.xml src` (arquivos ocultos, se houver, vão junto).
Run: `test ! -d backend && test ! -d infra && echo OK`
Expected: `OK`

- [ ] **Step 3: Verificar build após o move**

Run: `cd ledger-service/app && ./mvnw -q test`
Expected: `BUILD SUCCESS`. Falha aqui = path interno do build quebrou → corrigir antes de seguir (proibido prosseguir com build vermelho).

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore(monorepo): mova backend para ledger-service/app e remova infra/ legada"
```

---

### Task 3: Importar `commerce-api` e `webhook-gateway` (cópia limpa)

**Files:**
- Create: `commerce-api/app/**` (cópia de `/home/drummonds/Projects/devops-study/app/commerce-api`)
- Create: `webhook-gateway/app/**` (cópia de `/home/drummonds/Projects/devops-study/app/webhook-gateway`)
- Create: `commerce-api/{issues,estudos}/`, `webhook-gateway/{issues,estudos}/` (vazios; Tasks 5–8 preenchem)

**Interfaces:**
- Produces: `commerce-api/app/` e `webhook-gateway/app/` prontos para `npm ci && npm test` — Tasks 13 usa.

- [ ] **Step 1: Copiar sem lixo**

```bash
mkdir -p commerce-api/app webhook-gateway/app
rsync -a --exclude node_modules --exclude dist --exclude .env \
  /home/drummonds/Projects/devops-study/app/commerce-api/ commerce-api/app/
rsync -a --exclude node_modules --exclude dist --exclude .env \
  /home/drummonds/Projects/devops-study/app/webhook-gateway/ webhook-gateway/app/
mkdir -p commerce-api/issues commerce-api/estudos webhook-gateway/issues webhook-gateway/estudos
```

- [ ] **Step 2: Verificar que nada proibido entrou**

Run: `test ! -d commerce-api/app/node_modules && test ! -d webhook-gateway/app/node_modules && test ! -d commerce-api/app/dist && echo OK`
Expected: `OK`
Run: `git status --porcelain | grep -c "^??"` (apenas os 2 apps + dirs novos esperados)

- [ ] **Step 3: Testes verdes no destino**

Run: `cd commerce-api/app && npm ci --silent && npm test && npm run lint`
Expected: `13 passed`, `tsc --noEmit` sem erro.
Run: `cd webhook-gateway/app && npm ci --silent && npm test && npm run build`
Expected: `9 passed`, build OK.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore(monorepo): importa commerce-api e webhook-gateway (cópia limpa, sem histórico)"
```

---

### Task 4: Migrar tracker do ledger com renumeração

**Files:**
- Move (via `git mv`): 7 issues + 7 estudos de `.tracker/` para `ledger-service/`
- Modify: frontmatter, títulos, referências de cada arquivo movido

**Mapeamento (origem → destino):**

| origem `.tracker/issues/` | destino `ledger-service/issues/` | status |
|---|---|---|
| `01-linux-runtime.md` | `01-linux-runtime.md` | done |
| `02-docker-compose.md` | `02-docker-compose.md` | done |
| `04-vps-hardening.md` | `03-vps-hardening.md` | todo |
| `05-caddy-reverse-proxy.md` | `04-caddy-reverse-proxy.md` | todo |
| `06-db-backups-s3.md` | `05-db-backups-s3.md` | todo |
| `09-compose-isolation.md` | `06-compose-isolation.md` | todo |
| `17-cicd-vps-deploy.md` | `07-cicd-vps-deploy.md` | todo |

Mesmo slugging para `estudos/`. **Mapa de números na escrita** (antigo → novo, só este app): 01→01, 02→02, 04→03, 05→04, 06→05, 09→06, 17→07. Números 07, 08, 10–16, 18 **não existem neste app** — qualquer citação a eles é cross-app (tabela abaixo).

- [ ] **Step 1: Mover arquivos**

```bash
cd .tracker
# issues 01 e 02 mantêm o nome
git mv issues/01-linux-runtime.md issues/02-docker-compose.md ../ledger-service/issues/
git mv estudos/01-linux-runtime.md estudos/02-docker-compose.md ../ledger-service/estudos/
# os demais mudam de número
git mv issues/04-vps-hardening.md        ../ledger-service/issues/03-vps-hardening.md
git mv issues/05-caddy-reverse-proxy.md  ../ledger-service/issues/04-caddy-reverse-proxy.md
git mv issues/06-db-backups-s3.md        ../ledger-service/issues/05-db-backups-s3.md
git mv issues/09-compose-isolation.md    ../ledger-service/issues/06-compose-isolation.md
git mv issues/17-cicd-vps-deploy.md      ../ledger-service/issues/07-cicd-vps-deploy.md
git mv estudos/04-vps-hardening.md       ../ledger-service/estudos/03-vps-hardening.md
git mv estudos/05-caddy-reverse-proxy.md ../ledger-service/estudos/04-caddy-reverse-proxy.md
git mv estudos/06-db-backups-s3.md       ../ledger-service/estudos/05-db-backups-s3.md
git mv estudos/09-compose-isolation.md   ../ledger-service/estudos/06-compose-isolation.md
git mv estudos/17-cicd-vps-deploy.md     ../ledger-service/estudos/07-cicd-vps-deploy.md
cd ..
```

- [ ] **Step 2: Corrigir frontmatter, títulos e status**

Em cada issue movida: `aliases: [issue-NN, ...]` → número novo; título `# Issue NN —` → número novo.
Em cada estudo: `aliases: [estudo-NN]`, `issue: NN`, `# Estudos — Issue NN:` → número novo.
`03-vps-hardening` e `04-caddy-reverse-proxy` seguem com `status: todo` (eram `todo`).

Run: **checador A** (Checadores reutilizáveis)
Expected: `NUMERAÇÃO OK`

- [ ] **Step 3: Reescrever caminhos físicos**

Regra: `backend/` → `ledger-service/app/`. Arquivos afetados: `02-docker-compose.md` (2 refs), `04-caddy-reverse-proxy.md` (1), `06-compose-isolation.md` (1).
Regra: qualquer `infra/` citado → **remover a frase ou marcar como histórico** (este app não tem infra).

Run: `grep -rn "backend/\|infra/" ledger-service/`
Expected: 0 ocorrências.

- [ ] **Step 4: Reescrever referências cruzadas de Issue**

Aplicar o mapa da Task 4 (mesmo app) linha a linha, e a tabela de cross-app:

| arquivo | trecho | vira |
|---|---|---|
| `03-vps-hardening` | "Issue 05" (proxy), "Issue 09" (isolamento) | "Issue 04", "Issue 06" |
| `04-caddy-reverse-proxy` | "Requer Issue 04", "Issue 09" | "Requer Issue 03", "Issue 06" |
| `05-db-backups-s3` | "Requer Issue 02" ✓; "Monitoramento e alertas — Issue 07" | link cross-app `../../commerce-api/issues/05-observability.md` |
| `06-compose-isolation` | "Requer Issue 05"→04; "Requer Issue 08 — Redis já está na stack" | mover de `## Dependências` para `## Limitações / notas`: "quando o Redis entrar na stack (Issue 10 do `webhook-gateway`), ele deve cair na rede interna" |
| `06-compose-isolation` | "Backup — Issue 06"→05; "Monitoramento — Issue 07" | 05 ✓ / cross-app commerce 05 |
| `07-cicd-vps-deploy` | conferir ocorrências | mapear pelo mesmo critério |

Run: `grep -rn "Issue 08\|Issue 07\|Issue 09\|Issue 1[0-8]" ledger-service/issues/*.md`
Expected: toda linha restante contém `../../commerce-api/` ou `../../webhook-gateway/` ou foi reescrita para número válido (01–07).

- [ ] **Step 5: Commit**

```bash
git add ledger-service
git commit -m "chore(tracker): migra issues e estudos do ledger com renumeração 01-07"
```

---

### Task 5: Migrar tracker do commerce-api

**Files:**
- Move: 5 issues + 5 estudos de `.tracker/` para `commerce-api/`

**Mapeamento:**

| origem | destino `commerce-api/issues/` | status |
|---|---|---|
| `03-terraform-vpc.md` | `03-terraform-vpc.md` | **todo** (reset — infra apagada, refaz do zero) |
| `10-github-actions.md` | `04-github-actions.md` | todo |
| `07-observability.md` | `05-observability.md` | todo |
| `11-s3-reports-infra.md` | `06-s3-reports-infra.md` | parked |
| `12-aws-production.md` | `07-aws-production.md` | todo |

**Mapa de números:** 03→03, 10→04, 07→05, 11→06, 12→07. Números 01, 02 existirão como clone (Task 6); 08, 09, 13–18 não existem aqui.

- [ ] **Step 1: Mover com `git mv` e renumerar** (mesmo procedimento da Task 4, Steps 1–2)

- [ ] **Step 2: Corrigir frontmatter/título/status** — `03-terraform-vpc` nasce com `status: todo` e uma nota em `## Limitações / notas`: "a infraestrutura anterior foi apagada (histórico no git); esta Issue começa do zero em `commerce-api/infra/`." `06-s3-reports-infra` preserva `parked`.

Run: **checador A** → Expected: `NUMERAÇÃO OK`

Run: **checador A** → Expected: `NUMERAÇÃO OK`

- [ ] **Step 3: Reescrever caminhos físicos**

Regra: `infra/` → `commerce-api/infra/`; `backend/` → `commerce-api/app/`.
Atenção: `03-terraform-vpc` tem uma tabela descrevendo `infra/provider.tf`, `infra/vpc.tf`, `infra/security.tf`, `infra/s3.tf` e `infra/platform/compose-localstack.yaml` como "estado atual" — os arquivos não existem mais. Reescrever a seção `## Estado atual` para "estado inicial vazio" e converter a tabela em `## Requisitos` (o que a Issue espera que exista), mantendo os critérios de aceitação originais (VPC multi-tier, SGs encadeados, bucket com bloqueios).
Ocorrências em: `03` (5+ refs), `04-github-actions` (5), `06-s3-reports` (4), `07-aws-production` (1).

Run: `grep -rn "backend/" commerce-api/ ; grep -rn "infra/" commerce-api/ | grep -v "commerce-api/infra/"`
Expected: ambas sem saída (0 ocorrências de caminho antigo).

- [ ] **Step 4: Reescrever referências de Issue**

| arquivo | trecho | vira |
|---|---|---|
| `04-github-actions` | "Requer Issue 03" ✓ (03), "Requer Issue 02" | "Requer Issue 02" (clone, Task 6) |
| `05-observability` | "Requer Issue 02" ✓; "por isso esta Issue vem antes da Issue 09" | reescrever sem número: "antes de qualquer segmentação de rede da stack (ver `ledger-service/06`)" |
| `06-s3-reports` | "Requer Issue 03" ✓; "ALB — Issue 03" ✓; nota histórica do "tracker antigo" | manter 03; reescrever a nota sem citar "tracker antigo" |
| `07-aws-production` | "Requer Issue 03" ✓ | ✓ |

Run: `grep -rn "Issue 0[89]\|Issue 1[0-8]" commerce-api/issues/*.md`
Expected: 0 ocorrências.

- [ ] **Step 5: Commit**

```bash
git add commerce-api
git commit -m "chore(tracker): migra issues e estudos do commerce-api com renumeração 03-07"
```

---

### Task 6: Migrar tracker do webhook-gateway

**Files:**
- Move: 5 issues + 5 estudos de `.tracker/` para `webhook-gateway/`

**Mapeamento:**

| origem | destino `webhook-gateway/issues/` | status |
|---|---|---|
| `13-secrets-hygiene.md` | `04-secrets-hygiene.md` | todo |
| `14-sast-semgrep.md` | `05-sast-semgrep.md` | todo |
| `15-pipeline-hardening.md` | `07-pipeline-hardening.md` | todo |
| `16-devsecops-gates.md` | `08-devsecops-gates.md` | todo |
| `08-containers-redis.md` | `10-containers-redis.md` | todo |

**Mapa de números:** 13→04, 14→05, 15→07, 16→08, 08→10. 01, 02 = clones (Task 6/7); 03, 06, 09, 11 = novas (Task 7). Números 07, 09, 12, 17 aqui são **de outro app** (cross-app).

- [ ] **Step 1: Mover com `git mv` e renumerar** (procedimento das Tasks 4/5)

- [ ] **Step 2: Corrigir frontmatter/título/status** (todo o lote era `todo`)

Run: **checador A** → Expected: `NUMERAÇÃO OK`

- [ ] **Step 3: Reescrever caminhos físicos**

| arquivo | trecho | decisão |
|---|---|---|
| `10-containers-redis` | `backend/docker-compose.yaml` (3 refs) | → `ledger-service/app/docker-compose.yaml` — **esta Issue adiciona `redis` + serviço do webhook ao compose da stack de produção (VPS), que é o do ledger**; deixar isso explícito em `## Contexto` |
| demais | `infra/` citado | remover/frasear como fora de escopo desta trilha |

- [ ] **Step 4: Reescrever referências de Issue**

| arquivo | trecho | vira |
|---|---|---|
| `04-secrets-hygiene` | "Requer Issue 10 — pipeline" | "Requer Issue 03" |
| `04-secrets-hygiene` | "Issue 15" (hardening) / "Issue 16" | "Issue 07" / "Issue 08" |
| `04-secrets-hygiene` | "Issue 17 exige como pré-requisito de deploy" | cross-app: "`ledger-service/07` (deploy na VPS)" |
| `05-sast-semgrep` | "Requer Issue 10" | "Requer Issue 03" |
| `05-sast-semgrep` | "SCA (dependências e CVEs) — Issue 15" | "SCA — Issue 06" (a Issue 06 é nova, Task 7) |
| `05-sast-semgrep` | "segredos — Issue 13" / "Issue 16" / "Issue 17" | "Issue 04" / "Issue 08" / cross-app ledger 07 |
| `07-pipeline-hardening` | "Requer Issue 10" | "Requer Issue 03" |
| `07-pipeline-hardening` | "Deploy e proteção de ambiente — Issue 17" | cross-app ledger 07 |
| `07-pipeline-hardening` | "Segredos de infra e credenciais de nuvem — Issue 12" | cross-app `../../commerce-api/issues/07-aws-production.md` |
| `08-devsecops-gates` | "Requer 13, 14, 15, 10" | "Requer Issue 04, 05, 07, 03" |
| `08-devsecops-gates` | "gate de SCA por scan de imagem — Issue 10" | "SCA agora é a Issue 06" |
| `08-devsecops-gates` | "Requer Issue 08 — pipeline cobre múltiplos serviços" | **remover de `## Dependências`** (criaria dependência circular com a Issue 10) e mover para `## Limitações / notas`: "a pipeline cobre os serviços existentes; a stack multi-serviço chega na Issue 10" |
| `08-devsecops-gates` | "Deploy contínuo — Issue 17" / "Monitoramento — Issue 07" | cross-app ledger 07 / commerce 05 |
| `10-containers-redis` | "Requer Issue 07 — painéis" | cross-app `../../commerce-api/issues/05-observability.md` |
| `10-containers-redis` | "Segmentação final de redes — Issue 09" | cross-app `../../ledger-service/issues/06-compose-isolation.md` |

Run: `grep -rn "Issue 0[1-9]\|Issue 1[0-8]" webhook-gateway/issues/*.md`
Expected: toda linha com número fora de 01–11 do próprio app contém `../../ledger-service/` ou `../../commerce-api/`.

- [ ] **Step 5: Commit**

```bash
git add webhook-gateway
git commit -m "chore(tracker): migra issues e estudos do webhook com renumeração 04,05,07,08,10"
```

---

### Task 7: Clonar Issues 01 e 02 para commerce e webhook

**Files:**
- Create: `commerce-api/issues/{01-linux-runtime,02-docker-compose}.md` + estudos
- Create: `webhook-gateway/issues/{01-linux-runtime,02-docker-compose}.md` + estudos

**Interfaces:**
- Consumes: `ledger-service/issues/01-*.md`, `02-*.md` (fonte do clone)
- Produces: 4 issues + 4 estudos; `commerce 04/05` e `webhook 03` já referenciam "Requer Issue 02/03" — com estas Issues existentes, essas dependências passam a resolver.

- [ ] **Step 1: Clonar estrutura de `ledger-service/issues/01-linux-runtime.md` para os 4 destinos**

Manter as seções do template; mudar o conteúdo para a stack de destino:

| destino | adaprações obrigatórias |
|---|---|
| `commerce-api/01` | serviço systemd do `node dist/index.js`; env `PORT`, `DATABASE_URL`, `JWT_SECRET`; healthcheck L7 contra `/health` (já existe em `src/app.ts`); `SIGTERM` → `app.close()` do Fastify; comandos `npm` no lugar de `./mvnw` |
| `webhook-gateway/01` | serviço systemd do `node dist/index.js`; env `REDIS_URL`; **sem rota HTTP hoje** → healthcheck L7 fica adiado para a `02` (o critério aqui é L4/ processo + graceful shutdown); comandos `npm` |
| `commerce-api/02` | usa o `Dockerfile` **já existente** (multi-stage, `USER node`, `HEALTHCHECK /health`) — Issue é sobre orquestrar com Postgres via `docker-compose.yaml` (não existe) e `.env` |
| `webhook-gateway/02` | **cria o `Dockerfile`** (o app não tem): multi-stage, non-root, `HEALTHCHECK` — exige antes rota `/health` no `src/`; cria `docker-compose.yaml` com Redis sem publicar `6379`; cria `.env.example` |

Critérios de aceitação e seções `Validação`/`Evidências` idênticos em forma aos do ledger (evidência = comando no terminal + saída).

- [ ] **Step 2: Clonar e adaptar os estudos correspondentes (4 arquivos)**

Trocar referências Java/Maven por Node/npm; manter a nota de abertura do estudo.

- [ ] **Step 3: Verificar frontmatter e template**

Run: `grep -L "status:" commerce-api/issues/01-*.md commerce-api/issues/02-*.md webhook-gateway/issues/01-*.md webhook-gateway/issues/02-*.md`
Expected: vazio (todos têm frontmatter).
Run: `grep -c "^## " commerce-api/issues/01-linux-runtime.md`
Expected: `13` (as 13 seções do template).

- [ ] **Step 4: Verificar que as dependências abertas passaram a resolver**

Run:
```bash
test -f commerce-api/issues/02-docker-compose.md && test -f webhook-gateway/issues/02-docker-compose.md && echo OK
```
Expected: `OK` — os dois clones existem, então "Requer Issue 02" em `04-github-actions` (commerce) e em `10-containers-redis` (webhook) tem alvo.
Run: **checador A** → Expected: `NUMERAÇÃO OK` (as 4 Issues novas e os 4 estudos batem com o prefixo de arquivo).

- [ ] **Step 5: Commit**

```bash
git add commerce-api webhook-gateway
git commit -m "feat(tracker): clona Issues 01-02 adaptadas para commerce e webhook"
```

---

### Task 8: Criar as 6 Issues novas + seus estudos

**Files:**
- Create: `ledger-service/issues/08-trafego-sintetico-alertas.md` + `ledger-service/estudos/08-trafego-sintetico-alertas.md`
- Create: `commerce-api/issues/08-staging-falho-observabilidade.md` + estudo
- Create: `webhook-gateway/issues/03-pipeline-base-agnostica.md` + estudo
- Create: `webhook-gateway/issues/06-sca-dependencias-imagem.md` + estudo
- Create: `webhook-gateway/issues/09-dast-zap.md` + estudo
- Create: `webhook-gateway/issues/11-staging-inseguro.md` + estudo

**Interfaces:**
- Consumes: template das 13 seções (Global Constraints); texto de origem das trilhas em `/home/drummonds/Projects/devops-study/stages-labs/{vps-devsecops-production,spring-cloud-platform,devsecops-gates}/` **somente para calibrar os critérios** — proibido copiar arena/`verify.py`.

Conteúdo mínimo de cada nova Issue (as outras seções seguem o template):

| Issue | Critérios de aceitação que DEVEM aparecer |
|---|---|
| `ledger 08` | carga k6 agendada (cron/GitHub Action) batendo no endpoint de produção; pelo menos 1 alerta disparando de verdade com evidência (screenshot/log do canal); dashboard/saída mostrando latência p95; nada de falha em produção durante o teste |
| `commerce 08` | ambiente de staging separado da produção; ≥3 falhas injetadas de observabilidade (ex.: latência artificial, pool de conexões esgotado, fuga de memória); para cada uma: detecção pelo painel → diagnóstico registrado → conserto com evidência antes/depois |
| `webhook 03` | lógica de build/teste em scripts locais (`scripts/test.sh` style) que rodam sem a CI; workflow só "apita" os scripts; roda igual em runner GitHub e num runner genérico Linux (prova = mesmo script, sem reescrita) |
| `webhook 06` | `npm audit` (bibliotecas) com piso de severidade definido; Trivy na imagem Docker com falha em CVE alta/crítica; ambos rodam localmente E no gate; baseline documentada |
| `webhook 09` | OWASP ZAP (baseline scan) contra o serviço de pé; ≥1 finding real do ZAP registrado; cada finding tem ou correção ou justificativa; roda depois de `08` e alimenta o `11` |
| `webhook 11` | staging com fraquezas propositalmente deixadas (credencial fraca, porta exposta, segredo no artefato) **fora da produção**; prova de que os gates das `04`–`08` pegam essas fraquezas; forense de mensageria: Redis fora do ar, stream atrasada e assinatura HMAC inválida, cada uma com diagnóstico registrado |

(frontmatter: `status: todo`, `prioridade` coerente com as vizinhas; `aliases: [issue-NN, slug]`.)

- [ ] **Step 1: Escrever as 6 Issues** seguindo o template e a tabela acima
- [ ] **Step 2: Escrever os 6 estudos** (frontmatter `aliases: [estudo-NN]`, `issue: NN`)
- [ ] **Step 3: Verificar contagem e template**

Run: `ls ledger-service/issues | wc -l` → `8`; `ls commerce-api/issues | wc -l` → `8`; `ls webhook-gateway/issues | wc -l` → `11` (estudos espelham: 8/8/11).
Run: `grep -rL "## Evidências" ledger-service/issues/*.md commerce-api/issues/*.md webhook-gateway/issues/*.md` → vazio.
Run: **checador A** → `NUMERAÇÃO OK`.

- [ ] **Step 4: Nenhum `verify.py` ou YAML criado**

Run: `git status --porcelain | grep -E "\.py$|\.ya?ml$|Dockerfile" ; test $? -ne 0 && echo OK`
Expected: `OK`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(tracker): issues novas — tráfego/alerta, staging falho, pipeline base, SCA, DAST, staging inseguro"
```

---

### Task 9: Arquivar a Issue 18 e fechar o `.tracker/`

**Files:**
- Create: `archive/18-kubernetes-helm/{issue,estudo}.md`
- Move: `.tracker/BOARD.md` → `BOARD.md`; `.tracker/00-visao-geral.md` → `00-visao-geral.md`; `.tracker/.obsidian/` → `.obsidian/`
- Delete: `.tracker/` (sobra)

- [ ] **Step 1: Arquivar 18**

```bash
mkdir -p archive/18-kubernetes-helm
git mv .tracker/issues/18-kubernetes-helm.md archive/18-kubernetes-helm/issue.md
git mv .tracker/estudos/18-kubernetes-helm.md archive/18-kubernetes-helm/estudo.md
```

- [ ] **Step 2: Mover board, visão geral e vault**

```bash
git mv .tracker/BOARD.md BOARD.md
git mv .tracker/00-visao-geral.md 00-visao-geral.md
git mv .tracker/.obsidian .obsidian
rmdir .tracker/issues .tracker/estudos .tracker
```

- [ ] **Step 3: Verificar**

Run: `test ! -d .tracker && ls -a | grep -x ".obsidian" && echo OK`
Expected: `OK`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore(tracker): board e visão geral para a raiz, Issue 18 arquivada, .tracker/ encerrado"
```

---

### Task 10: Reescrever `BOARD.md` e `00-visao-geral.md`

**Files:**
- Modify: `BOARD.md`, `00-visao-geral.md`

**Interfaces:**
- Consumes: nomes finais de arquivo das Tasks 4–8 (25 Issues) e `archive/18-kubernetes-helm/`.

- [ ] **Step 1: `BOARD.md` — nova estrutura**

Manter frontmatter e tom. Seções novas:
1. `## Produção e staging` — 2 linhas: estado do sistema em produção (VPS stack + AWS) e do staging falho.
2. `## ledger-service · trilha VPS` — checkboxes `01…08` com link relativo `ledger-service/issues/NN-slug.md`
3. `## commerce-api · trilha AWS` — `01…08` → `commerce-api/issues/...`
4. `## webhook-gateway · trilha DevSecOps` — `01…11` → `webhook-gateway/issues/...`
5. `## Fora de escopo` — `archive/18-kubernetes-helm/` + menção a `interview-prep-finops` e `ansible` (no devops-study).

Preservar o estado. **Regra: o checkbox do Board espelha o frontmatter `status:` da Issue.** Estados finais conhecidos (medidos na origem): `ledger 01` e `ledger 02` = `done` → `[x]`; `commerce 06` (era `11-s3-reports`) = `parked` → seção Parked; `commerce 03` = **`todo`** (reset deliberado, infra apagada) → seção To Do; **todo o resto é `todo`** → To Do. Conferir com `grep -h "^status:" */issues/*.md` antes de fechar.
Apagar a seção `## Mapa de estado do app` (18 linhas do tracker antigo) e substituir por uma linha por app descrevendo o estado final daquela trilha.

- [ ] **Step 2: `00-visao-geral.md` — nova estrutura**

Manter `Separação de responsabilidades`, `Metodologia`, `3 pilares`, `Política de status` (idênticas). Substituir:
- `Jornada e público`, `A narrativa`, `Ordem de execução` → `## As 3 trilhas` (tabela app ↔ trilha ↔ estágio atual) e uma nota de que cada trilha é independente e completa.
- `Restrições de escopo` → reescrever: FinOps/entrevistas/Ansible fora; custo zero por regra permanece; **Kubernetes fora de escopo por decisão, arquivado em `archive/`**; infra (CI/Terraform/Docker) é construção do usuário, não do agente.
- `## Fora de escopo` nova seção listando `archive/18-kubernetes-helm/`.
- O rodapé `**Board:** [BOARD](BOARD.md)` continua válido.

- [ ] **Step 3: Verificar links do Board**

Run: `grep -o "](.*issues/[^)]*)" BOARD.md | wc -l`
Expected: `25` (uma por Issue) — mais links de apoio, se houver.
Run: **checador B** → Expected: `LINKS OK`.

- [ ] **Step 4: Commit**

```bash
git add BOARD.md 00-visao-geral.md
git commit -m "docs(tracker): board de 3 colunas e visão geral das 3 trilhas"
```

---

### Task 11: `AGENTS.md` e `README.md` da raiz

**Files:**
- Modify: `AGENTS.md` (reescrever)
- Create: `README.md`

- [ ] **Step 1: Reescrever `AGENTS.md` (raiz = routing, ~60 linhas)**

Seções exatas:
1. **Identidade** — monorepo de 3 apps, cada app = 1 trilha completa até produção; produção real + staging falho.
2. **Filetree** — o novo layout (sem `backend/`, sem `infra/`, sem `.tracker/`).
3. **Tabela app → trilha → estágio atual** (25 Issues, contagem 8/8/11).
4. **Convenções do tracker** — template fixo, política de status, proibição de tutorial/FAQ/`Prev`-`Next`/sub-etapas; estudo só em `estudos/`; fechar Issue exige evidência no terminal (sem `verify.py`).
5. **Escopo de escrita** — graváveis: `ledger-service/`, `commerce-api/`, `webhook-gateway/`, `BOARD.md`, `00-visao-geral.md`, `archive/`; **somente leitura: infra, CI, Dockerfile, compose, código de app** (o usuário constrói). Enquanto ensinando (`teach-anything`): zero escrita.
6. **Skills/routing** — `using-superpowers` no startup, `ask-matt`, `teach-anything` (read-only → `EXPLICAÇÃO` no chat), grilling via `question`.
7. **Trilha ≠ tecnologia** — o que é comum (Linux, Docker, CI/CD) se repete em cada app; uma trilha nunca antecipa a outra.

Remover: toda menção a `.tracker/`, `backend/`, `infra/` (linhas 26, 36, 98, 102 do arquivo atual) e a tabela "Backend Architecture"/"Infra → App Wiring" que descreve `backend/` — essa informação migra para `ledger-service/AGENTS.md` (Task 12).

- [ ] **Step 2: Criar `README.md` da raiz**

Índice público: o que é o monorepo, tabela dos 3 apps (stack, trilha, como rodar em 1 linha cada), links para `BOARD.md` e para cada `README.md` de app. Sem tutorial.

- [ ] **Step 3: Verificar**

Run: `grep -n "backend/\|\.tracker/\|^| \`infra/\`" AGENTS.md`
Expected: 0 ocorrências.
Run: `test -s README.md && echo OK`

- [ ] **Step 4: Commit**

```bash
git add AGENTS.md README.md
git commit -m "docs(monorepo): AGENTS.md de routing e README de índice na raiz"
```

---

### Task 12: `AGENTS.md` e `README.md` dos 3 apps + higiene

**Files:**
- Create: `ledger-service/AGENTS.md`, `ledger-service/README.md`
- Create: `commerce-api/AGENTS.md`, `commerce-api/README.md`
- Create: `webhook-gateway/AGENTS.md`, `webhook-gateway/README.md`
- Modify: `.gitignore`, `.agents/skills/teach-anything/SKILL.md`

- [ ] **Step 1: `ledger-service/AGENTS.md`**

Conteúdo: tabela das 8 Issues da trilha VPS (número, título, status); arquitetura de portas (`PaymentEventPublisher` → `RedisPaymentEventPublisher`/`NoOp`, `ReportRepository` → `S3`/`NoOp`) e env vars (`SPRING_DATASOURCE_URL`, `REDIS_ENABLED`, `S3_ENABLED`, `JWT_SECRET`…); comandos (`./mvnw test`, `docker compose`, `healthcheck.sh`); healthcheck L4/L7; nota de que `infra/` não existe (trilha VPS não usa Terraform).

- [ ] **Step 2: `commerce-api/AGENTS.md`**

8 Issues da trilha AWS; stack Fastify/Drizzle/Postgres; comandos `npm test`/`npm run lint`/`npm run build`; env (`DATABASE_URL`, `JWT_SECRET`, `PORT`); rota `/health`; **gaps**: sem `docker-compose.yaml`, sem CI, sem `infra/` (Issues 02 e 03 os criam — e são do usuário).

- [ ] **Step 3: `webhook-gateway/AGENTS.md`**

11 Issues da trilha DevSecOps; stack Node + `ioredis` + Redis Streams; comandos `npm test`/`npm run build`; **gaps**: sem Dockerfile/`.env.example`/rota de health até a Issue 02; **nota de rollout dos gates** (do spec): `gitleaks` agnóstico, Semgrep `p/javascript`→`p/java`, `npm audit`→OWASP Dependency-Check Maven, Trivy troca a imagem, ZAP vira scan autenticado (JWT) — portar para `ledger-service`/`commerce-api` é adaptar config, não reaprender.

- [ ] **Step 4: `README.md` de cada app** — o que é, como rodar (1 bloco), status do tracker, links para `issues/` e `estudos/`.

- [ ] **Step 5: Higiene**

`.gitignore`: trocar `.tracker/.obsidian/` por `.obsidian/`.
`.agents/skills/teach-anything/SKILL.md`: reescrever menções a `backend/` e `infra/` para `ledger-service/app/` e `commerce-api/infra/` (onde o contexto for "infra do app").

- [ ] **Step 6: Verificar**

Run: `grep -rn "backend/" .agents/ AGENTS.md ledger-service/ commerce-api/ webhook-gateway/ | grep -v "app/"` → 0.
Run: `grep -n "tracker/.obsidian" .gitignore` → 0; `grep -n "^.obsidian/" .gitignore` → 1.
Run: `ls */AGENTS.md */README.md | wc -l` → `6`.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "docs(monorepo): AGENTS e README por app, rollout de gates, higiene de caminhos"
```

---

### Task 13: Verificação final

**Files:** nenhum (só auditoria; correções pontuais permitidas dentro do commit da auditoria).

- [ ] **Step 1: Estrutura exata**

Run:
```bash
test ! -d .tracker && test ! -d backend && test ! -d infra && \
ls -d ledger-service/app ledger-service/issues ledger-service/estudos \
       commerce-api/app commerce-api/issues commerce-api/estudos \
       webhook-gateway/app webhook-gateway/issues webhook-gateway/estudos && echo OK
```
Expected: `OK`

- [ ] **Step 2: Contagens e numeração**

Run: `ls ledger-service/issues | wc -l` → 8; `commerce-api/issues | wc -l` → 8; `webhook-gateway/issues | wc -l` → 11; estudos idem. `ls */issues/01-*.md */issues/02-*.md | wc -l` → 6 (clones existem nos 3 apps).
Run: **checador A** → `NUMERAÇÃO OK`.

- [ ] **Step 3: Links relativos (0 quebrados)**

Run: **checador B** (Checadores reutilizáveis)
Expected: `LINKS OK` (exit 0).

- [ ] **Step 4: Caminhos fantasma**

Run: `grep -rn "backend/\|\.tracker/" AGENTS.md README.md BOARD.md 00-visao-geral.md .agents/ */AGENTS.md */README.md */issues/ */estudos/ 2>/dev/null`
Expected: 0 ocorrências (exceto trecho histórico citado como "apagado", se houver — deve dizer explicitamente que foi removido).

- [ ] **Step 5: Testes dos 3 apps**

Run: `cd ledger-service/app && ./mvnw -q test` → `BUILD SUCCESS`
Run: `cd commerce-api/app && npm ci --silent && npm test && npm run lint` → `13 passed`
Run: `cd webhook-gateway/app && npm ci --silent && npm test && npm run build` → `9 passed`

- [ ] **Step 6: Nada de infra criada**

Substituir `<base>` pelo SHA anotado na Task 1, Step 2.

Run: `git diff <base>..HEAD --name-only | grep -E "workflows/|Dockerfile|docker-compose|\.tf$"`
Expected: vazio — nenhum YAML de CI, Dockerfile, compose ou Terraform criado/alterado.
Run: `git diff <base>..HEAD -- .github/workflows/CI.yml healthcheck.sh`
Expected: vazio (byte-a-byte idênticos ao início).

- [ ] **Step 7: Estado final**

Run: `git status --porcelain` → só `?? .learning/`
Run: `git log --oneline <sha-da-task1>..HEAD` → ~11 commits na ordem das Tasks.

- [ ] **Step 8: Commit** — só se houver correção; caso contrário, reportar resultados ao usuário.
