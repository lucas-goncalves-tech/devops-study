# Correções auditoria-30-Issues — commerce-api Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corrigir no tracker da trilha AWS os achados Crítico/Importante da auditoria de 30 Issues de 02/10/2026 atribuídos a este app — caminho de rede do deploy, ciclo de vida `destroy`↔re-provisionamento, dono da fronteira ALB/TLS, mecanismo de reuso lab→prod, backup com dono, alertas e seed da carga, e política mínima do deploy — sem tocar em status, template, código ou IaC.

**Architecture:** Edits em markdown em `commerce-api/issues/` (`03`, `05`, `07`, `09`). Sete tasks, um commit cada. Nenhum arquivo de `infra/`, `.github/`, `app/` ou `scripts/` é editado — as declarações novas descrevem o que o `07`/`05`/`09` **prometem**; executar `terraform`/`apply` continua sendo construção do usuário.

**Tech Stack:** Tracker markdown (template RFC de 13 seções), gates `bash`/`grep` com linha `OK`, bateria final com `awk`/`git`.

**Spec — achados desta auditoria destinados a este plano (C/I; Menores em `Diferidos`):**

- **C2** — `07:64-66` provisiona "computação mínima" sem declarar **placement**: em qual subnet a instância vai, como sai para a internet (docker pull), por onde entra o SSH (`22`) — `03:86` põe a API em subnet privada sem rota padrão (sem NAT: `03:34`) e nada fecha esse vão. A `10` presume rede pronta.
- **I2** — `07:66/74/93` exigem `destroy` sem re-provisionar: o estado final come a premissa das `08:51` e `10:51` (que assumem a máquina viva).
- **I3** — fronteira ALB+TLS sem dona: `03:34` declara TLS/HTTPS no ALB fora de escopo e `10:36` aponta "ALB e security group da Issue 03 e da Issue 07" — a `07` nunca promete reativar o `elbv2`.
- **I4** — `07:63` proíbe editar `infra/provider.tf` (contrato da `03`), mas o mecanismo de reuso lab→prod nunca é declarado.
- **I5** — backup apontado para fora da trilha: `07:33` aponta a `05` do `ledger-service` (outra trilha) e `03:34` só diz "fora desta Issue".
- **I6** — `08:36` aponta alertas para a `05` desta trilha, mas a `05` não entrega alerta.
- **I7** — `05:63` roda k6 contra `/api/v1/orders/checkout` sem seed de estoque; no app real o 409 de estoque esgotado derruba `http_req_failed < 0.01`.
- **I8** — `09` declara least-privilege só no trust policy (`09:68/111`); a política de ações (`Action`/`Resource`) não tem critério.

**Ordem:** este é 2 de 3 planos da rodada (`ledger`, `commerce`, `webhook`) executados no **mesmo branch** criado de `main@481a2c5` (via `superpowers:using-git-worktrees` na execução). Ordem entre os planos é indiferente; cada um tem sua bateria final com escopo em união dos três apps.

## Global Constraints

- Arquivos editáveis: apenas `commerce-api/issues/*.md` e este plano em `docs/`. Intocáveis: `BOARD.md`, frontmatter (`status:`, `prioridade:`, `tags:`), `archive/`, `app/`, `infra/`, `.github/`, `scripts/`. **Zero mudança de `status:`; nenhum checkbox vira `[x]`** (inclusive o `parked` da `06`).
- Cada Issue mantém as 13 seções `## ` e o par requisito↔critério; só acrescentamos/removemos bullets dentro de seções existentes.
- PT-BR, RFC da casa: sem tutorial, FAQ, navegação (`Prev`/`Next`) nem sub-etapas (`1A`, `2B`).
- Um commit por task; cada gate roda com `set -euo pipefail` e imprime `Task N gate: OK`; gate vermelho = corrigir antes do próximo commit.
- `B0=$(git merge-base main HEAD)` (esperado `481a2c5`).
- Nenhuma task executa comando de runtime — este plano é 100% texto de tracker.

## Review Focus

- Tokens pinados exatamente como escritos; âncoras de substituição únicas.
- Rulings desta rodada embutidos no texto: deploy em **subnet pública** com `22` só via `var.admin_cidr` (custo zero, sem NAT); backup **dentro** do `07` (não apontar para outra trilha); alerta **dentro** do `05` (faz `08:36` virar verdade); fronteira ALB/TLS com dono `07`.
- `06` continua `parked`; `03:34` e `07:33` não apontam mais para fora.
- Nenhum `- [x]` novo, nenhum `status:` alterado, 13 seções por Issue.

---

### Step 0: Commit do plano

- [ ] Copiar este arquivo para o worktree e commitá-lo antes da Task 1:

```bash
git add docs/plano-correcoes-auditoria2-commerce-api.md
git commit -m "docs(tracker): plano de correção auditoria2 commerce-api"
```

### Task 1: Placement e caminho de rede do deploy (C2)

**Files:**
- Modify: `commerce-api/issues/07-aws-production.md` — `## Requisitos` (novo após o requisito `Provisionar computação mínima…`), `## Critérios de aceitação` (novo após `:76`), `## Validação` (novo após `:85`), `## Evidências` (novo após `:94`), `## Limitações / notas` (novo ao final).

**Interfaces:**
- Consume: subnet pública com rota via IGW já criada em `03:85`, SG da API em `03:89-92`, e a `10` que presume rede pronta.
- Produz: placement declarado — subnet, rota, bootstrap de Docker e entrada `22` parametrizada; a `10` passa a ter caminho real.

**Ruling registrado:** instância em **subnet pública** (rota via IGW sem NAT = custo zero); fronteira fechada por SG — `22` nunca `0.0.0.0/0`, `3000` só via ALB (Task 3).

- [ ] **Step 1: Requisito de placement**

Novo `- [ ]` após `- [ ] Provisionar computação mínima para API e banco` contendo obrigatoriamente: `subnet pública da VPC da Issue 03`, `rota padrão para o IGW (sem NAT)`, `bootstrap com Docker antes do primeiro deploy` e `entrada \`22\` restrita à variável \`admin_cidr\` — nunca \`0.0.0.0/0\``.

- [ ] **Step 2: Critério**

Novo `- [ ]` após o critério `:76` contendo: `O plano real mostra a instância na subnet pública com rota para o IGW`, `entrada \`22\` limitada a \`admin_cidr\``, `nenhuma porta \`0.0.0.0/0\`` e `o \`docker pull\` na instância nova conclui antes do deploy`.

- [ ] **Step 3: Validação e Evidências**

- Validação (após `:85`): `- Na instância provisionada, executar o bootstrap e confirmar \`docker pull\` e \`ssh\` pelo endereço restrito à CIDR de admin`.
- Evidências (após `:94`): `- Saída do \`docker pull\` na instância nova e regra de ingress \`22\` com \`admin_cidr\``.

- [ ] **Step 4: Nota de custo zero**

Novo bullet ao final de `## Limitações / notas` contendo: `Subnet pública por custo zero`, `a alternativa privada exigiria NAT/endpoint (custo recorrente)` e `a fronteira é fechada por SG`.

- [ ] **Step 5: Gate da Task 1**

Run:
```bash
set -euo pipefail
f=commerce-api/issues/07-aws-production.md
grep -qF 'subnet pública da VPC da Issue 03' "$f" \
  && grep -qF 'admin_cidr' "$f" \
  && grep -qF 'docker pull' "$f" \
  && grep -qF 'Subnet pública por custo zero' "$f" \
  && echo "Task 1 gate: OK"
```
Expected: `Task 1 gate: OK`

- [ ] **Step 6: Commit**

```bash
git add commerce-api/issues/07-aws-production.md
git commit -m "docs(tracker): placement e caminho de rede do deploy declarados na 07"
```

### Task 2: destroy sem comer a premissa de 08/10 (I2)

**Files:**
- Modify: `commerce-api/issues/07-aws-production.md` — requisito `:66`, critério `:74`, evidência `:93`.

**Interfaces:**
- Consume: `08:51` e `10:51` (máquina existe como premissa).
- Produz: ciclo de vida com estado final vivo — `destroy` é prova intermediária, `apply` de re-provisionamento fecha a Issue.

- [ ] **Step 1: Requisito `:66`**

Substituir `- [ ] Desligar após validar, sem recursos órfãos` por `- [ ] Provar o ciclo de vida (\`destroy\` sem cobrança residual) e deixar o ambiente re-provisionado ao final, sem recursos órfãos — a máquina viva é premissa das Issues 08 e 10`.

- [ ] **Step 2: Critério `:74`**

Substituir `- [ ] Após \`terraform destroy\`, não resta cobrança de recurso` por `- [ ] Após \`terraform destroy\`, não resta cobrança de recurso, e um \`apply\` de re-provisionamento devolve a instância \`running\` — destroy é prova de ciclo de vida, não o estado final desta Issue`.

- [ ] **Step 3: Evidência `:93`**

Substituir `- Output do \`destroy\` sem recursos órfãos` por `- Output do \`destroy\` sem recursos órfãos e do \`apply\` de re-provisionamento com a instância \`running\``.

- [ ] **Step 4: Gate da Task 2**

Run:
```bash
set -euo pipefail
f=commerce-api/issues/07-aws-production.md
! grep -qF 'Desligar após validar, sem recursos órfãos' "$f" \
  && grep -qF 're-provisionamento' "$f" \
  && grep -qF 'premissa das Issues 08 e 10' "$f" \
  && echo "Task 2 gate: OK"
```
Expected: `Task 2 gate: OK`

- [ ] **Step 5: Commit**

```bash
git add commerce-api/issues/07-aws-production.md
git commit -m "docs(tracker): destroy vira prova de ciclo de vida, estado final re-provisionado"
```

### Task 3: Fronteira ALB/TLS com dona (I3)

**Files:**
- Modify: `commerce-api/issues/07-aws-production.md` — `## Requisitos` (novo), `## Critérios de aceitação` (novo), `## Validação` (novo), `## Evidências` (novo), `## Limitações / notas` (nota de TLS).
- Modify: `commerce-api/issues/03-terraform-vpc.md:34` — `## Fora de escopo` ganha dona.

**Interfaces:**
- Consume: scaffold comentado do ALB em `03:72-73/94-95/108-112` e o ponteiro `10:36` ("ALB e security group da Issue 03 e da Issue 07").
- Produz: dona declarada — a `07` reativa `elbv2` e entrega a fronteira; `03:34` e `10:36` passam a ser verdadeiros.

- [ ] **Step 1: Requisito da fronteira em `07`**

Novo `- [ ]` após o requisito de placement (Task 1) contendo: `Reativar o \`elbv2\` no provider de produção`, `ALB na subnet pública`, `target group para a API na porta \`3000\``, `listener \`80\`` e `quando houver domínio apontado: certificado ACM e redirect \`80\` para \`443\` (sem domínio, registrar a pendência em Limitações)`.

- [ ] **Step 2: Critério**

Novo `- [ ]` após o critério da Task 1 contendo: `O plano real mostra o ALB ativo`, `health check do target group \`healthy\``, `entrada \`3000\` da API restrita ao SG do ALB` e `nenhuma regra com \`0.0.0.0/0\``.

- [ ] **Step 3: Validação e Evidências**

- Validação: `- Inspecionar o ALB: DNS responde, health check \`healthy\` e a API só é alcançável através dele`.
- Evidências: `- DNS do ALB e estado \`healthy\` do target group`.

- [ ] **Step 4: Nota de TLS pendente**

Novo bullet em `## Limitações / notas` contendo: `sem domínio registrado não há certificado`, `declarar a pendência em vez de deixar a fronteira sem dono` e `o listener \`80\` existe mesmo sem domínio`.

- [ ] **Step 5: `03:34` ganha donas**

Substituir `- NAT Gateway, TLS/HTTPS no ALB, WAF, Multi-AZ e backup: fora desta Issue` por `- NAT Gateway, WAF e Multi-AZ: fora desta Issue — a fronteira ALB/TLS e o backup do banco são da \`Issue 07\``.

- [ ] **Step 6: Gate da Task 3**

Run:
```bash
set -euo pipefail
f=commerce-api/issues/07-aws-production.md
g=commerce-api/issues/03-terraform-vpc.md
grep -qF 'Reativar o `elbv2`' "$f" \
  && grep -qF 'target group' "$f" \
  && grep -qF 'SG do ALB' "$f" \
  && grep -qF 'certificado ACM' "$f" \
  && grep -qF 'a fronteira ALB/TLS e o backup do banco são da `Issue 07`' "$g" \
  && echo "Task 3 gate: OK"
```
Expected: `Task 3 gate: OK`

- [ ] **Step 7: Commit**

```bash
git add commerce-api/issues/07-aws-production.md commerce-api/issues/03-terraform-vpc.md
git commit -m "docs(tracker): fronteira ALB/TLS e backup com dona na 07; 03 com ponteiros locais"
```

### Task 4: Mecanismo de reuso lab→prod (I4)

**Files:**
- Modify: `commerce-api/issues/07-aws-production.md` — `## Requisitos` (novo após o requisito do provider de produção), `## Critérios de aceitação` (novo ao final).

**Interfaces:**
- Consume: `07:63` (provider em diretório próprio, sem editar `infra/provider.tf`) e `03:131` (valores de laboratório são contrato de saída da `03`).
- Produz: mecanismo declarado — instanciação de módulo com aliases; sem cópia de arquivo e sem resíduo de `localhost:4566`.

- [ ] **Step 1: Requisito do mecanismo**

Novo `- [ ]` após `- [ ] Antes do primeiro \`apply\` real, declarar o provider da AWS remota…` contendo: `Declarar o mecanismo de reuso do código da Issue 03`, `a raiz de produção instancia o módulo (source para commerce-api/infra, providers/configuration_aliases resolvidos para a AWS real)`, `os endpoints do laboratório ficam atrás de variable vazia em produção` e `nenhum .tf do lab é editado`.

- [ ] **Step 2: Critério**

Novo `- [ ]` após o critério `:76` contendo: `Reuso por instanciação, não por cópia`, `o apply de produção termina com git diff vazio em commerce-api/infra/` e `o plan da raiz de produção não referencia localhost:4566`.

- [ ] **Step 3: Gate da Task 4**

Run:
```bash
set -euo pipefail
f=commerce-api/issues/07-aws-production.md
grep -qF 'configuration_aliases' "$f" \
  && grep -qF 'instanciação, não por cópia' "$f" \
  && grep -qF 'git diff` vazio' "$f" \
  && echo "Task 4 gate: OK"
```
Expected: `Task 4 gate: OK`

- [ ] **Step 4: Commit**

```bash
git add commerce-api/issues/07-aws-production.md
git commit -m "docs(tracker): mecanismo de reuso lab→prod declarado na 07"
```

### Task 5: Backup do banco com dona nesta trilha (I5)

**Files:**
- Modify: `commerce-api/issues/07-aws-production.md` — `## Fora de escopo` (`:33`), `## Requisitos`, `## Critérios de aceitação`, `## Validação`, `## Evidências` (novos itens).

**Interfaces:**
- Consume: bucket `securepay-financial-reports` com 4 bloqueios da `03:91` e `06` (`parked`, dona do bucket S3).
- Produz: backup→restore declarados dentro do `07`; `07:33` deixa de apontar para a trilha do `ledger`.

- [ ] **Step 1: `07:33` sai da lista de fora de escopo**

Substituir `- Backups e monitoramento — [Issue 05 do \`ledger-service\`](../../ledger-service/issues/05-db-backups-s3.md) e Issue 05` por `- Monitoramento — [Issue 05](05-observability.md) deste app`.

- [ ] **Step 2: Requisito**

Novo `- [ ]` após `- [ ] Estimar custo mensal antes de subir qualquer recurso` contendo: `Backup diário do banco para o bucket securepay-financial-reports da Issue 03`, `retenção declarada` e `restore provado a partir do objeto fora da instância antes de encerrar`.

- [ ] **Step 3: Critério**

Novo `- [ ]` após o critério `:76` contendo: `Um dump do banco existe no bucket (fora da instância)` e `o restore a partir dele devolve os dados — teste registrado nesta Issue`.

- [ ] **Step 4: Validação e Evidências**

- Validação: `- Gerar o dump, restaurar uma cópia de teste a partir do objeto e conferir os dados`.
- Evidências: `- Saída do objeto de backup no bucket e do restore com os dados conferidos`.

- [ ] **Step 5: Gate da Task 5**

Run:
```bash
set -euo pipefail
f=commerce-api/issues/07-aws-production.md
! grep -q 'ledger-service/issues/05-db-backups' "$f" \
  && grep -qF 'restore provado' "$f" \
  && grep -qF 'securepay-financial-reports' "$f" \
  && grep -qF 'o restore a partir dele devolve os dados' "$f" \
  && echo "Task 5 gate: OK"
```
Expected: `Task 5 gate: OK`

- [ ] **Step 6: Commit**

```bash
git add commerce-api/issues/07-aws-production.md
git commit -m "docs(tracker): backup do banco vira requisito do 07 — fim do ponteiro para outra trilha"
```

### Task 6: Alerta na 05 e seed da carga k6 (I6 + I7)

**Files:**
- Modify: `commerce-api/issues/05-observability.md` — `## Requisitos` (estender `:63`, novos após `:64`), `## Critérios de aceitação` (novos após `:71`), `## Validação` (novo após `:78`), `## Evidências` (novos após `:85`).

**Interfaces:**
- Consume: métricas do coletor/`prom-client` já escopados na `05`; ponteiro `08:36` ("a observabilidade que esta trilha entrega está na Issue 05"); app real com controle de estoque (`409` em checkout).
- Produz: `05` entrega alerta (o ponteiro da `08` vira verdade — sem editar a `08`) e carga semeadada (o critério `http_req_failed < 0.01` deixa de ser derrubável por `409`).

- [ ] **Step 1: Estender requisito da carga (`:63`)**

Ao final do requisito `- [ ] Carga k6 de 50–100 VUs autenticados … com \`idempotencyKey\` no corpo`, acrescentar: `, sobre produtos semeados de teste com estoque alto — semeadura e limpeza declaradas nesta Issue`.

- [ ] **Step 2: Requisito de alerta**

Novo `- [ ]` após o último requisito (`:64`) contendo: `Definir ao menos uma regra de alerta sobre as métricas coletadas (p95 ou taxa de erro)`, `com PromQL, condição e \`for\``, `ligada a um canal observável` e `registrar um disparo de teste`.

- [ ] **Step 3: Critérios**

Novos `- [ ]` após o último critério (`:71`):

- Um contendo: `A regra existe em arquivo com PromQL e \`for\` visíveis` e `um disparo real foi observado no canal durante a carga`.
- Um contendo: `A janela de carga não registra \`409\` de estoque esgotado` e `a carga não é reprovada por \`http_req_failed\` por falta de dados`.

- [ ] **Step 4: Validação e Evidências**

- Validação (após `:78`): `- Conferir na saída da carga a ausência de \`409\` e registrar a semeadura e a limpeza dos dados de teste`.
- Evidências (após `:85`): `- Regra de alerta em arquivo e registro do disparo no canal` e `- Saída da carga sem \`409\` e registro da semeadura/limpeza`.

- [ ] **Step 5: Gate da Task 6**

Run:
```bash
set -euo pipefail
f=commerce-api/issues/05-observability.md
grep -qF 'produtos semeados de teste' "$f" \
  && grep -qF 'PromQL' "$f" \
  && grep -qF 'canal observável' "$f" \
  && grep -qF '409' "$f" \
  && echo "Task 6 gate: OK"
```
Expected: `Task 6 gate: OK`

- [ ] **Step 6: Commit**

```bash
git add commerce-api/issues/05-observability.md
git commit -m "docs(tracker): alerta e seed de carga declarados na 05"
```

### Task 7: Política mínima de ações no deploy (I8)

**Files:**
- Modify: `commerce-api/issues/09-pipeline-infra-apply.md` — `## Requisitos` (novo após o requisito da credencial efêmera), `## Critérios de aceitação` (novo ao final), `## Validação` (novo após `:94`), `## Evidências` (novo após `:103`).

**Interfaces:**
- Consume: trust policy já escopada em `09:68` e o anti-padrão registrado em `09:111` (política de confiança ampla).
- Produz: least-privilege completo — trust **e** actions, cada `Action` com comando correspondente.

- [ ] **Step 1: Requisito**

Novo `- [ ]` após `- [ ] Configurar o job para obter credencial efêmera via \`id-token\`…` contendo: `Declarar a política de ações da Role com as ações mínimas que o job executa (só o que plan/apply da trilha usa)` e `sem \`*\` em \`Action\` ou \`Resource\``.

- [ ] **Step 2: Critério**

Novo `- [ ]` após o último critério (`:85`) contendo: `O JSON da política não contém \`Action: *\` nem \`Resource: *\``, `cada ação corresponde a um comando da pipeline` e `evidência: trecho da política`.

- [ ] **Step 3: Validação e Evidências**

- Validação (após `:94`): `- Inspecionar a política da Role e confrontar cada \`Action\` com os comandos que o job executa`.
- Evidências (após `:103`): `- JSON da política da Role sem \`*\` nas ações`.

- [ ] **Step 4: Gate da Task 7**

Run:
```bash
set -euo pipefail
f=commerce-api/issues/09-pipeline-infra-apply.md
grep -qF 'ações mínimas' "$f" \
  && grep -qF 'Resource' "$f" \
  && grep -qF 'confrontar cada `Action`' "$f" \
  && echo "Task 7 gate: OK"
```
Expected: `Task 7 gate: OK`

- [ ] **Step 5: Commit**

```bash
git add commerce-api/issues/09-pipeline-infra-apply.md
git commit -m "docs(tracker): least-privilege estendido à política de ações no 09"
```

- [ ] **Step 6: Bateria final de validação**

Executar **depois** dos commits (árvore limpa). Mesma construção do plano irmão `ledger`: escopo em **união dos três apps + `docs/plano-correcoes-`** (branch compartilhada), estilo em `*/issues/` com blocos cercados isentos via `awk`, `[x]` comparado contra `B0`, `BOARD.md` intocado nesta rodada. Census deste app: **9 `todo` + 1 `parked`**.

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
out=$(git diff --name-only "$B0" HEAD | grep -Ec '^ledger-service/issues/|^ledger-service/AGENTS\.md$|^ledger-service/README\.md$|^commerce-api/issues/|^webhook-gateway/issues/|^docs/plano-correcoes-' || true)
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

- Nenhuma task deste plano depende dos planos `ledger` ou `webhook`; as baterias cobrem os três apps em união (branch compartilhada).
- O achado `I3` de `10:36` é resolvido pelo lado da `07` (a `10` já apontava para ela) — a `10` não é editada.

## Diferidos (apêndice — sem task)

Escopo aprovado = 3 Críticos + 20 Importantes. Estes **Menores** da auditoria ficam registrados, não corrigidos:

- **`01:60,63,68` (+ `07:65`, `09:75`)** — requisitos sem Critério/Evidência correspondente.
- **`01:61-63`** — hardening `systemd` sem `User=`/`Group=` nos critérios.
- **`02:63` × `10:73`** — Compose de laboratório (`build:`) × deploy de produção (`image:`): relação não declarada.
- **`04:110`/:21** — declaração desatualizada (o `app/compose.yaml` foi apagado no refactor).
- **`04:105-106` × `03:70-71`** — contrato do emulador contraditório + `LOCALSTACK_AUTH_TOKEN` sem origem declarada.
- **`06:49` × `:111` / `03:47`** — Estado atual desatualizado (o bucket "existe" mas a infra foi apagada).
- **`05:93` (e `02:116`)** — segmentação de rede de produção sem dona.
- **`07:59,99`** — bootstrap do backend (S3+DynamoDB) sem dono.
- **`08:64` (e `05:24`, `08:111`)** — staging em nuvem sem caminho de observação/deploy.
