# Revisão Cirúrgica do Tracker — Plano

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reordenar `06–09` (Backup antes do Observability), converter o Board para link markdown com títulos por necessidade, demover a `03` para laboratório, e reescrever o objetivo da `00-visao-geral` — sem tocar em `backend/` ou `infra/`.

**Architecture:** A intervenção tem um núcleo arriscado (renomeação **cíclica** de 4 Issues) e uma camada narrativa (Board + visão geral). O ciclo `06→07→08→09→06` exige tokens temporários — qualquer `sed` direto colide arquivo com arquivo.

**Tech Stack:** Markdown, Obsidian wikilinks → markdown relativos, `git mv`.

**Spec:** a conversa deste plan mode (decisões: Backup adiantado · só título do Board · link markdown).

## Global Constraints

- `git diff -- backend infra` deve ter **0 linhas** ao final
- Toda Issue mantém as **11 seções obrigatórias** (`## Contexto` … `## Limitações / notas`)
- Zero `## O que aprender`, `## O que fazer`, `## Critério de pronto`, `**Prev:**`, `**Next:**`, `**Board:**`, sub-etapas `1A`/`2B`
- Contagem: **18** issues, **18** estudos, `status` = 3 `done` + 1 `parked` + 14 `todo`
- **Uma única aresta muda** de significado: `09→08` vira `Backup→02`. Todas as outras são só renomeadas
- Aresta `08→07` do card de Isolamento ("última palavra sobre topologia") **não pode ser quebrada**

## Review Focus

| Risco | Esperado | Teste |
|---|---|---|
| Renumeração cíclica colide arquivo com arquivo | `06-observability` vira `07-observability` sem sobrescrever o antigo | `ls .tracker/issues` = 18 arquivos, sem colisão |
| Renumeração cria auto-referência (`Backup` apontando para si) | aresta do Backup vira `02`, não `09` | grep `Requer Issue` no card de Backup |
| Tokens `06–09` fora do padrão escapam (`issue: 06`, `estudo-06`) | frontmatter dos estudos remapeado | grep `issue-0[6789]\|estudo-0[6789]\|issue: 0[6789]` = 0 |
| Tabela do `AGENTS.md` escapam (formato `\| 06 \|`, sem `Issue`) | 4 linhas trocadas de posição | diff visual |
| Link do Board com path errado | `issues/04-…` a partir de `.tracker/` | abrir link relativo |

---

### Task 1: Relaxar a aresta do Backup

**Files:**
- Modify: `.tracker/issues/09-db-backups-s3.md` (`## Dependências`)

**Por quê primeiro:** é a **única** mudança semântica. Se vier depois da renomeação, o `sed` cíclico a transformaria em auto-referência.

- [ ] **Step 1:** trocar a dependência

De:
```markdown
- Requer Issue 08 — o banco isolado é o alvo do dump
```
Para:
```markdown
- Requer Issue 02 — o banco de dados existe desde a Issue 02; o isolamento de rede do banco não é pré-requisito do dump, só do desenho final
```

- [ ] **Step 2:** verificar

Run: `grep -n "Requer Issue" .tracker/issues/09-db-backups-s3.md`
Expected: `Requer Issue 02 — ...`

---

### Task 2: Renumeração cíclica 06 → 07 → 08 → 09 → 06

**Files:**
- Rename: `.tracker/issues/{06-observability,07-containers-redis,08-compose-isolation,09-db-backups-s3}.md`
- Rename: `.tracker/estudos/{idem}.md`
- Modify: 10 arquivos de issue com referências (04, 05, 06, 07, 08, 09, 11, 16, 17, 18)

**Interfaces:**
- Produces: numeração nova que as Tasks 3–5 consomem

**Mapeamento:** `06→07` (Obs) · `07→08` (Redis) · `08→09` (Isolamento) · `09→06` (Backup)

- [ ] **Step 1: renomear arquivos com token temporário** (ciclo exige dois passes)

```bash
cd .tracker
for d in issues estudos; do
  git mv $d/06-observability.md            $d/ZZ7ZZ-observability.md
  git mv $d/07-containers-redis.md         $d/ZZ8ZZ-containers-redis.md
  git mv $d/08-compose-isolation.md        $d/ZZ9ZZ-compose-isolation.md
  git mv $d/09-db-backups-s3.md            $d/ZZ6ZZ-db-backups-s3.md
done
for d in issues estudos; do
  git mv $d/ZZ7ZZ-observability.md         $d/07-observability.md
  git mv $d/ZZ8ZZ-containers-redis.md      $d/08-containers-redis.md
  git mv $d/ZZ9ZZ-compose-isolation.md     $d/09-compose-isolation.md
  git mv $d/ZZ6ZZ-db-backups-s3.md         $d/06-db-backups-s3.md
done
```

- [ ] **Step 2: renomear referências no conteúdo, 4 passes com token**

```bash
cd .tracker
FILES=$(ls issues/*.md estudos/*.md *.md) 2>/dev/null
# passe 1: prefixos com 06 -> alvo 07 (token ZZ7ZZ)
for p in "Issue" "issue-" "estudo-" "issue:"; do
  sed -i "s/${p} 06\b/${p} ZZ7ZZ/g" $FILES
  sed -i "s/${p} 07\b/${p} ZZ8ZZ/g" $FILES
  sed -i "s/${p} 08\b/${p} ZZ9ZZ/g" $FILES
  sed -i "s/${p} 09\b/${p} ZZ6ZZ/g" $FILES
done
# passe 2: colapsar tokens
sed -i "s/ZZ7ZZ/07/g; s/ZZ8ZZ/08/g; s/ZZ9ZZ/09/g; s/ZZ6ZZ/06/g" $FILES
```

> O `\b` é obrigatório: impede que `Issue 06` vire algo quando o padrão não fecha. Só os 4 prefixos acima podem mudar — `06` solto em porta, CIDR ou data **não** pode ser tocado.

- [ ] **Step 3: verificar que a renomeação foi completa e não gerou auto-referência**

Run:
```bash
grep -rn "ZZ[0-9]ZZ" .tracker && echo "FALHA: token sobrando" || echo "OK: sem token"
grep -rn "issue-0[6789]\|estudo-0[6789]\|issue: 0[6789]" .tracker/issues/0[6789]-*.md
grep -n "Requer Issue" .tracker/issues/06-db-backups-s3.md
ls .tracker/issues | wc -l; ls .tracker/estudos | wc -l
```
Expected: sem token · estudos `07/08/09/06` corretos · `06-db-backups-s3` requer `Issue 02` · 18 e 18

- [ ] **Step 4: conferir a DAG preservada**

Run: `grep -h "Requer Issue" .tracker/issues/*.md | sort`
Expected: todas as arestas antigas **renomeadas** (`Obs 07`, `Redis 08`, `Isolamento 09`), exceto Backup que agora aponta para `02`.

- [ ] **Step 5: commit**

```bash
git add .tracker
git commit -m "chore: move db backups before observability in tracker order"
```

---

### Task 3: Reescrever `BOARD.md`

**Files:**
- Modify: `.tracker/BOARD.md` (arquivo inteiro)

Por que reescrever de uma vez: o Board precisa de 4 mudanças simultâneas (link, título, ordem, `03`) — edição parcial produz estado inconsistente.

- [ ] **Step 1: converter wikilinks para markdown relativo** — o Board está em `.tracker/`, logo `issues/<slug>.md`

`[[04-vps-hardening|04 VPS Hardening]]` → `[04 VPS Hardening](issues/04-vps-hardening.md)`
`[[00-visao-geral|00 Visão Geral]]` → `[00 Visão Geral](00-visao-geral.md)`
`[[BOARD]]` (dentro de `00-visao-geral.md`) → `[BOARD](BOARD.md)`

- [ ] **Step 2: títulos por necessidade** (18 linhas)

| # | Nova descrição da linha do Board |
|---|---|
| 01 | a app vira serviço do sistema: sobe com o boot, responde healthcheck, morre sem cortar requisição |
| 02 | tudo sobe com um comando, sem privilegiado, e a API espera o banco estar de pé |
| 03 | rede que se recria do zero com o banco inacessível de fora — *laboratório* |
| 04 | servidor exposto só aceita chave: sem senha, sem porta aberta, sem força bruta |
| 05 | uma única porta na frente, HTTPS emitido sozinho, cabeçalhos de segurança |
| 06 | dado sobrevive se o servidor sumir: dump diário fora do servidor, restore provado |
| 07 | sei que está lento ou quebrado antes do usuário perceber |
| 08 | evento não se perde quando o consumidor cai |
| 09 | serviço vizinho não alcança o banco nem o Redis; nada estoura a memória |
| 10 | teste quebrado, imagem com CVE ou Terraform inválido não passam revidos |
| 11 | relatório financeiro só sai no bucket certo, com permissão mínima |
| 12 | ninguém aplica por cima de ninguém; sei o custo antes de subir |
| 13 | credencial não entra no repositório |
| 14 | padrão inseguro não chega no merge |
| 15 | a pipeline não vira o caminho mais curto até o repositório |
| 16 | verde significa: sem segredo, sem erro, sem CVE crítica |
| 17 | merge vira produção sozinho, e volta sozinho se doer |
| 18 | nó morde e o usuário não percebe |

- [ ] **Step 3: reordenar** — seções `###` passam a ser: `VPS (04–05)` · `Backup e recuperação (06)` · `Monitoramento e mensageria (07–08)` · `Isolamento e limites (09)` · `Automação (10)` · `IaC e Cloud (12)` · `CI/CD + DevSecOps (13–17)` · `Kubernetes (18)`. Linhas do mapa (tabela) 06–09 trocam de posição junto.

- [ ] **Step 4: tirar a `03` da narrativa principal**

`## Done` vira:
```markdown
## Done

- [x] [01 Linux Runtime](issues/01-linux-runtime.md) — ...
- [x] [02 Docker Compose](issues/02-docker-compose.md) — ...

### Laboratório concluído (fora da narrativa principal)

- [x] [03 Terraform VPC](issues/03-terraform-vpc.md) — ...
      _concluída cedo, como laboratório LocalStack sem custo. NÃO faz parte da cadeia
      manual → automatizado → cloud: ela pré-existe a necessidade que resolve.
      Continua como pré-requisito de 10, 11 e 12 por causa do gate de IaC._
```

- [ ] **Step 5: verificar**

Run:
```bash
grep -c '\[\[' .tracker/BOARD.md                    # esperado: 0
grep -c '\](issues/' .tracker/BOARD.md               # esperado: 17 (01,02,04–18)
grep -n 'Laboratório concluído' .tracker/BOARD.md
```

---

### Task 4: Reescrever `00-visao-geral.md`

**Files:**
- Modify: `.tracker/00-visao-geral.md`

- [ ] **Step 1: objetivo final (linha 14)**

De:
```markdown
- **Objetivo final:** transformar o backend em plataforma profissional DevSecOps ao longo das 18 Issues.
```
Para:
```markdown
- **Objetivo final:** demonstrar a evolução operacional do **mesmo sistema** — o SecurePay —
  do ambiente local até a plataforma orquestrada, uma capacidade por vez.
```

- [ ] **Step 2: inserir a narrativa explícita** — nova seção `## A narrativa` logo após `## Jornada e público`:

```markdown
## A narrativa

Uma frase só, do começo ao fim:

> Comecei executando a aplicação localmente, depois operei a mesma aplicação em um servidor
> Linux manualmente, automatizei as partes repetitivas, migrei a arquitetura para serviços de
> Cloud e finalmente passei a gerenciar essa infraestrutura com IaC e Kubernetes.

Ela se desdobra em cinco transições — cada uma delas é uma **necessidade**, não uma tecnologia:

| Transição | A dor que a provoca | Issues |
|---|---|---|
| manual → repetitivo | o mesmo comando roda toda vez e alguém esquece | 04–06 |
| repetitivo → automatizado | teste quebrado passa revido porque ninguém olhou | 07–10 |
| único → multi-serviço | o consumidor cai e o evento some | 08–09 |
| local → cloud | o estado vive no disco de quem aplicou | 11–12 |
| declarado → orquestrado | nó morre e ninguém percebe | 13–18 |
```

- [ ] **Step 3: diagrama de sequência** — trocar as duas faixas:

```text
BACKUP E RECUPERAÇÃO (06)
  ↓
MONITORAMENTO E MENSAGERIA (07–08)
  ↓
ISOLAMENTO E LIMITES (09)
```

- [ ] **Step 4: reforçar a nota da `03`**

```markdown
- `03-terraform-vpc` é **laboratório anexado**, não etapa da narrativa: foi concluída antes de
  existir a dor que Terraform resolve, para não custar dinheiro. NÃO a use como marco da evolução.
  Ela permanece como pré-requisito apenas porque o gate de IaC da Issue 10 valida o HCL que ela criou.
```

- [ ] **Step 5: converter o link final** `**Board:** [[BOARD]]` → `**Board:** [BOARD](BOARD.md)`

---

### Task 5: `AGENTS.md` — tabela 06–09

**Files:**
- Modify: `AGENTS.md:45-48`

- [ ] **Step 1:** trocar as 4 linhas de posição

```markdown
| 06 | Off-site DB backups with retention + tested restore | To Do |
| 07 | Prometheus, Grafana dashboards, k6 load testing | To Do |
| 08 | Multi-service Compose, Redis Streams, webhook gateway | To Do |
| 09 | Network isolation, DB/Redis lockdown, resource limits | To Do |
```

> Esta tabela usa `| 06 |`, **não** `Issue 06` — a renomeação da Task 2 não pega.

- [ ] **Step 2:** verificar que o filetree do `AGENTS.md` ainda bate (ele já descreve `issues/` + `estudos/`)

---

### Task 6: VPS em VM local — nota de custo zero

**Files:**
- Modify: `.tracker/issues/04-vps-hardening.md`, `.tracker/issues/05-caddy-reverse-proxy.md` (`## Limitações / notas`)
- Modify: `.tracker/00-visao-geral.md` (seção `## Restrições de escopo`)

- [ ] **Step 1:** adicionar em ambas as Issues:

```markdown
- **Não precisa de VPS pública para começar.** Esta Issue roda inteira numa VM local
  (VirtualBox/UTM/libvirt com Ubuntu/Debian): SSH, firewall, Docker, Compose e Caddy não
  exigem IP público. Use VPS pública só quando precisar provar TLS público, DNS e tráfego
  real de internet — e nesse caso uma VPS temporária resolve.
```

- [ ] **Step 2:** adicionar em `00-visao-geral`:

```markdown
- **Custo:** nenhuma Issue exige servidor pago para ser concluída. 04–09 rodam em VM local;
  03, 11 e 12 usam LocalStack ou `terraform plan`. VPS pública só entra como prova final opcional.
```

---

### Task 7: Verificação global

- [ ] **Step 1: escopo intocado**

Run: `git diff --stat -- backend infra`
Expected: vazio

- [ ] **Step 2: estrutura**

Run:
```bash
ls .tracker/issues/*.md | wc -l          # 18
ls .tracker/estudos/*.md | wc -l         # 18
grep -h '^status:' .tracker/issues/*.md | sort | uniq -c   # 3 done, 1 parked, 14 todo
grep -rh '^## ' .tracker/issues/ | sort | uniq -c | wc -l  # 11 seções
grep -rn 'O que aprender\|O que fazer\|Critério de pronto\|\*\*Prev:\|\*\*Next:\|^#### ' .tracker/issues/  # vazio
grep -c '\[\[' .tracker/BOARD.md .tracker/00-visao-geral.md  # 0 e 0
```

- [ ] **Step 3: DAG final**

Run: `for f in .tracker/issues/*.md; do echo "${f##*/}: $(awk '/^## Dependências/{x=1;next} /^## /{x=0} x' $f | grep -o 'Issue [0-9]*' | tr '\n' ' ')"; done`
Expected: idêntico ao antigo **exceto** `06-db-backups-s3: Issue 02`, e `07/08/09` corretamente deslocados

- [ ] **Step 4: commit**

```bash
git add -A
git commit -m "chore: reframe tracker narrative, need-first board titles and github-safe links"
```

---

## Self-review do plano

**Cobertura da spec:** Backup adiantado → Task 1+2 ✓ · título Board → Task 3 ✓ · links GitHub → Task 3+4 ✓ · 03 como laboratório → Task 3+4 ✓ · objetivo reescrito → Task 4 ✓ · narrativa manual→automatizado → Task 4 ✓ · VPS sem dinheiro → Task 6 ✓. **Não coberto, por decisão sua:** mover o Redis (você escolheu "só o Backup").

**Gaps detectados e corrigidos:** a tabela do `AGENTS.md` escaparia do `sed` (Task 5 separada); `issue: 06` dos estudos escaparia de `Issue 06` (prefixo `issue:` incluído no Task 2).
