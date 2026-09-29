---
aliases: [plan, image-forge, quarta-app]
tags: [plan, image-forge]
status: pendente
data: 2026-09-29
---

# `image-forge` — 4º app Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development` ou
> `superpowers:executing-plans`, task por task. Passos em checkbox (`- [ ]`).
>
> **⛔ PENDENTE — não executar antes da conclusão das 3 trilhas.** A regra que levanta a pendência
> está em [`AGENTS.md`](../../../AGENTS.md), seção *Pendente: 4º app*. Enquanto ela valer, este
> documento é apenas leitura. O **Task 1** deste plano já está cumprido: os dois arquivos estão no
> repo, sem o bloco de movimento e sem o frontmatter `destino:`.

**Goal:** Criar o 4º app do monorepo — uma API de imagem com fetch por URL que funciona e nasce
insegura e lenta por construção — com a trilha de 12 Issues, a spec e a atualização dos documentos
que hoje dizem "3 apps, 3 trilhas".

**Architecture:** Três entregas independentes, nesta ordem. (1) **A spec entra no repo** e os
documentos que hoje dizem "3 apps, 3 trilhas" passam a dizer a verdade, incluindo a exceção
nomeada e datada à regra de `0.0.0.0/0`. (2) As **12 Issues** nascem no template fixo, com o
inventário como referência compartilhada. (3) O **corpo do app e a infraestrutura são construção do
usuário** — este plano nunca escreve em `app/`, `infra/`, Dockerfile ou compose.

**Tech Stack:** Node 20 · Fastify · Drizzle ORM · Postgres · Redis (a partir de `10`) · `sharp` ·
Terraform (`hashicorp/aws`) · LocalStack · k6 · Prometheus · Grafana · Alertmanager.

**Spec:** `docs/superpowers/specs/2026-09-29-image-forge-quarta-app-design.md`

## Global Constraints

- **Escopo de escrita** (`AGENTS.md`): gravável inclui `image-forge/` a partir do momento em que
  esta spec entra no repo. Leitura apenas: `<app>/app/`, IaC/Terraform, `.github/workflows/`,
  `healthcheck.sh`, `.agents/`. **Este plano escreve Issues, estudos, tracker e docs — nunca
  código de app nem Terraform.**
- **Template fixo de Issue**, 13 seções H2 na ordem de `00-visao-geral.md`: `Contexto`, `Objetivo`,
  `Dependências`, `Escopo`, `Fora de escopo`, `Conhecimentos envolvidos`, `Estado atual`,
  `Resultado esperado`, `Requisitos`, `Critérios de aceitação`, `Validação`, `Evidências`,
  `Limitações / notas`.
- **Issue não é aula** (`00-visao-geral.md:41`): sem tutorial, FAQ, `Prev`/`Next`, sub-etapas
  (`1A`, `2B`) nem passos de "como fazer". Passo a passo vai para `image-forge/estudos/`, que este
  plano **não** cria.
- **A exceção nomeada e datada** (D-3) vale **apenas** para os recursos listados em
  `image-forge/docs/inventory.md`, e cada um tem Issue que o remove. Nenhum recurso fora da lista
  pode ser publicado em `0.0.0.0/0`.
- **Custo zero** (`00-visao-geral.md:77`): nenhuma Issue exige conta AWS. A **única** exceção
  declarada é a CDN da Issue `11`, que LocalStack não emula de forma utilizável — e essa
  ressalva precisa estar escrita **naquela Issue**, não descoberta durante a execução.
- **Nenhuma Issue antecipa tecnologia** cuja questão ela não resolve.
- **Checkbox do board espelha o `status:` do frontmatter** (`00-visao-geral.md:72`). As 12 nascem
  `todo` / `[ ]`.
- **O inventário é a fonte da verdade.** Um defeito que não tem coluna `Detector` não entra. Um
  defeito sem Issue que o remove fica marcado "fora desta sequência" **com motivo** (D-6).
- **`archive/18-kubernetes-helm` não é marco desta trilha.** É a Issue que roda este app depois
  que a `09` existir.

## Review Focus

Cinco modos de falha que os checks deste plano não exercitam. Cada um ganha um check no task que é
dono do arquivo afetado.

1. **Exceção que cresce.** A regra de `0.0.0.0/0` começa com 4 entradas e termina com 12, cada
   nova "só para testar". Comportamento esperado: a lista do inventário **só encolhe** a partir da
   Issue `04`. Verificado no Task 3.
2. **Defeito sem detector.** Um item entra no inventário porque "parece um problema", sem nada que
   o ache. Verificado no Task 2.
3. **App que não funciona.** A falha mais grave é o app não subir nem com upload local. Uma Issue de
   segurança construída sobre serviço quebrado não é trilha, é fantasia. Verificado no Task 4.
4. **Issue de hardening que reescreve a Issue de origem.** A `03` "acha" defeitos, mas não os
   conserta; se ela começar a consertar, a `04` fica sem trabalho e o mapa de responsabilidades
   quebra. Verificado nos Tasks 5 e 6.
5. **A trilha virar wish list.** 12 Issues que ninguém fecha é pior que 6 que fecha. O sinal é a
   Issue `01` e a `02` custando mais que a `04`. Verificado no Task 8.

---

## Fase 1 — A spec entra no repo

Nenhuma Issue existe ainda. Esta fase muda o que o repo **afirma sobre si mesmo**, e é a única
alteração que não pode ser desfeita por reverter um branch: ela muda a premissa.

## Task 1: Mover a spec e o plano para o repo

**Files:**
- Create: `docs/superpowers/specs/2026-09-29-image-forge-quarta-app-design.md`
- Create: `docs/superpowers/plans/2026-09-29-image-forge-quarta-app.md`
- Delete: os arquivos correspondentes fora do repo

**Interfaces:**
- Consumes: os dois documentos já redigidos.
- Produces: os caminhos canônicos que todas as Tasks seguintes e o `AGENTS.md` referenciam.

- [x] **Step 1: `git mv` não se aplica — os arquivos nunca foram versionados. Copiar:** ✅ cumprido

```bash
mkdir -p docs/superpowers/specs docs/superpowers/plans
cp "$PLANDIR/2026-09-29-image-forge-quarta-app-design.md" \
   docs/superpowers/specs/2026-09-29-image-forge-quarta-app-design.md
cp "$PLANDIR/2026-09-29-image-forge-quarta-app.md" \
   docs/superpowers/plans/2026-09-29-image-forge-quarta-app.md
```

- [x] **Step 2: Remover o bloco `⚠️ Ao mover para o repo` das duas cópias** ✅ cumprido

A instrução de movimento deixa de valer assim que o arquivo está no lugar. Deixar é deixar o
documento mentindo sobre si mesmo.

- [x] **Step 3: Remover o frontmatter `destino:` das duas cópias** ✅ cumprido — idem.

- [ ] **Step 4: Conferir que os links relativos entre os dois resolvem**

```bash
grep -oE 'docs/superpowers/[a-z]+/[a-z0-9.-]+\.md' docs/superpowers/specs/2026-09-29-image-forge-quarta-app-design.md docs/superpowers/plans/2026-09-29-image-forge-quarta-app.md | sed 's/^[^:]*://' | sort -u | while read p; do [ -f "$p" ] || echo "QUEBRADO: $p"; done
```

Expected: sem `QUEBRADO`.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/
git commit -m "docs(spec): spec do image-forge, 4o app do monorepo"
```

---

## Task 2: O inventário como contrato

**Files:**
- Create: `image-forge/docs/inventory.md`

**Interfaces:**
- Consumes: Task 1 (a spec, seção "Inventário de defeitos deliberados").
- Produces: `image-forge/docs/inventory.md`, linkado por todas as 12 Issues. É a fonte da verdade
  sobre o que está exposto: `ID | grupo | o que está errado | Detector | Issue que remove |
  status`.

- [ ] **Step 1: Criar com frontmatter e uma tabela**

```yaml
---
aliases: [inventario, inventory, image-forge]
tags: [reference, image-forge, security]
---
```

Colunas: `ID | Grupo | O que está errado | Detector | Issue que remove | Status`.
Preencher os 22 IDs da spec: `I-01` a `I-14`, `C-01` a `C-08`. **Os `G-01` a `G-10` e os `RNF` não
entram como linhas** — são performance e ausência de requisito, tratados pelas Issues `09`, `10` e
`11` em bloco, e a spec explica por quê.

- [ ] **Step 2: Escrever a regra do inventário no topo do arquivo**

Texto a fixar: **um defeito que nenhuma ferramenta de terceiro acha é um defeito que você nunca vai
lembrar de procurar.** Todo item novo precisa de detector antes de entrar.

- [ ] **Step 3: `Status` de todo item começa como `plantado` ou `ausente`**

Nenhum item pode nascer `resolvido` e nenhum pode nascer `desconhecido` — a existência do item **é**
a conhecimento. `I-12` e `I-14` (sem CDN, um ambiente só) começam como `ausente`, que significa
"não plantado de propósito, e mesmo assim é um defeito".

- [ ] **Step 4: Check de que nenhum item entrou sem detector**

```bash
awk -F'|' '/^\| *[IC]-[0-9]/ && $0 !~ /—|-{3,}/ {print $5}' image-forge/docs/inventory.md | grep -cvE '\S'
```

Expected: `0` linhas. A coluna 5 é a do `Detector`; zero significa que todo item tem detector.

- [ ] **Step 5: Check de contagem**

```bash
grep -cE '^\| *[IC]-[0-9]{2}' image-forge/docs/inventory.md
```

Expected: `22` (14 de infra + 8 de código).

- [ ] **Step 6: Commit**

```bash
git add image-forge/docs/inventory.md
git commit -m "docs(image-forge): inventario de defeitos deliberados como contrato"
```

---

## Task 3: Corrigir o que o repo afirma sobre si mesmo

**Files:**
- Modify: `AGENTS.md`
- Modify: `00-visao-geral.md`
- Modify: `README.md`

**Interfaces:**
- Consumes: Tasks 1 e 2.
- Produces: a premissa "4 apps, 4 trilhas", a exceção nomeada e datada de `0.0.0.0/0`, e o
  `image-forge/` na lista de gravável. As Tasks 4+ assume que isso já vale.

> **⛔ Pré-condição: a pendência do `AGENTS.md` já foi levantada.** Este plano só roda depois do
> levantamento, e o levantamento **já cumpre os Steps 1, 2 e 3** abaixo. Antes de começar este Task,
> confirme com um único comando que a trava saiu:
>
> ```bash
> grep -c 'Pendente: 4º app' AGENTS.md   # esperado: 0
> ```
>
> Se retornar ≠ 0, **pare**: o repo ainda está em modo pendente e nenhum destes Tasks deve rodar.

- [x] **Step 1: `AGENTS.md` — escopo de escrita** ✅ cumprido **no levantamento da pendência**

Adicionar `image-forge/` à lista de gravável, na mesma linha dos outros três.

- [x] **Step 2: `AGENTS.md` — a exceção nomeada e datada (D-3)** ✅ cumprido **no levantamento da
  pendência**

Substituir *"nenhuma porta de serviço publicada em `0.0.0.0/0` — vale para os 3 apps"* por texto
que: mantém a regra para `ledger-service`, `commerce-api` e `webhook-gateway` **sem ressalva**;
declara que o `image-forge` é **exceção temporária, datada do dia do levantamento** (e não de
`2026-09-29`, que é a data da spec — a exceção corre a partir de quando há recursos reais), limitada
às linhas do [`image-forge/docs/inventory.md`](image-forge/docs/inventory.md) com ID `I-01`–`I-14`;
e diz que a exceção é revogada quando a Issue `04` e a `11` fecharem. **A data é obrigatória** —
exceção sem data vira regra em seis meses.

- [x] **Step 3: `AGENTS.md` — tabela de roteamento** ✅ cumprido **no levantamento da pendência**

Linha já existe, nascida marcada `⛔ pendente`; o levantamento troca a célula de `Estágio` para
`01 a entrar` e apaga a nota de que é rastro de planejamento. A coluna "Trilha" precisa dizer que é
AWS-hardening, **não** repetir a trilha AWS do `commerce-api`.

- [ ] **Step 4: `00-visao-geral.md` — "3 apps, 3 trilhas" vira "4 apps, 4 trilhas"**

Atualizar o título, o parágrafo de abertura, a tabela de apps e a seção "Separação de
responsabilidades". A entrada do `image-forge` na tabela declara que o app **nasce inseguro por
construção** e que o inventário é o contrato.

- [ ] **Step 5: `00-visao-geral.md` — a linha do `archive/18`**

Hoje diz "candidata a 4ª trilha futura". Passa a dizer que o `archive/18` **serve ao `image-forge`**
como trilha futura, habilitada só depois que a Issue `09` produzir número de capacidade — porque
HPA sem número medido é adivinhação, que é a própria Issue 18 já dizendo.

- [ ] **Step 6: `00-visao-geral.md` — o bloco de restrições**

Adicionar a restrição de que o `image-forge` é a **única** exceção à regra de `0.0.0.0/0`, com
link para o inventário.

- [ ] **Step 7: `README.md` da raiz — o índice**

Adicionar o `image-forge` como 4º app na tabela de índice, com a nota de que ele está em
construção e que a spec está em `docs/superpowers/specs/`.

- [ ] **Step 8: Check de Review Focus 1 — a lista do inventário é o que autoriza a exceção**

```bash
grep -c 'inventory.md' AGENTS.md
grep -q '0.0.0.0/0' AGENTS.md && echo "regra ainda presente" 
grep -q 'exceção' AGENTS.md && echo "excecao declarada" || echo "FALTA excecao"
```

Expected: `inventory.md` citado ao menos 2 vezes (escopo e regra), e "excecao declarada".

- [ ] **Step 9: Check de que o app é o único app nomeado na exceção**

```bash
grep -n 'exceção' AGENTS.md | grep -c 'image-forge'
grep -n 'exceção' AGENTS.md | grep -vc 'image-forge'
```

Expected: primeiro ≥ 1, segundo `0`. Exceção que nomeia outro app é a regra original quebrada.

- [ ] **Step 10: Check da data obrigatória**

```bash
grep -A3 'exceção' AGENTS.md | grep -cE '202[0-9]-[0-9]{2}-[0-9]{2}'
```

Expected: ≥ 1. Sem data, a Task falha.

- [ ] **Step 11: Commit**

```bash
git add AGENTS.md 00-visao-geral.md README.md
git commit -m "docs(repo): 4o app, com excecao nomeada e datada a 0.0.0.0/0"
```

---

## Fase 2 — As 12 Issues

## Task 4: Issue 01 — o app local

**Files:**
- Create: `image-forge/issues/01-app-local.md`
- Modify: `image-forge/AGENTS.md` (criado aqui, com o bloco de rastreio e a tabe de 12 Issues)

**Interfaces:**
- Consumes: Tasks 1 e 2.
- Produces: o slug `image-forge/issues/01-app-local.md`. A Issue `02` depende desta.

- [ ] **Step 1: Criar `image-forge/AGENTS.md`**

Seções mínimas: arquitetura e portas de infraestrutura; variáveis de ambiente com default e
consumidor; comandos (`npm ci`, `npm test`, `npm run dev`, `docker compose up`); rota de saúde com
o contrato 200/503; o que ainda não existe (Compose, Terraform, CI — cada uma com a Issue que cria);
onde o trabalho vive; escopo de escrita, espelhando a estrutura dos outros três apps.

- [ ] **Step 2: Escrever a Issue no template**

- `Contexto` — o serviço não existe. RF-01 a RF-10 e as 4 tabelas estão na spec; aqui o problema é
  que upload e fetch por URL não rodam em lugar nenhum.
- `Dependências` — nenhuma. É a entrada da trilha.
- `Escopo` — unidade de serviço e Compose com a stack; `/v1/health` com 200/503; upload binário
  funcionando; **fetch por URL funcionando**; `preHandler` de JWT aplicado às rotas de imagem e
  **não** a `/v1/health` e `/v1/metrics`; shutdown gracioso sob `SIGTERM`; `idempotencyKey` no POST
  de imagem.
- `Fora de escopo` — **qualquer validação de segurança.** Esta Issue entrega o serviço **inseguro**
  de propósito, e isso é o estado inicial da trilha. Nomear: sem allowlist de MIME, sem limite de
  tamanho, sem rate limit, sem proteção de SSRF.
- `Conhecimentos envolvidos` — unidade de serviço e healthcheck; bind e shutdown gracioso em Node;
  `multipart/form-data`; transação idempotente.
- `Estado atual` — só existe a spec. Nenhuma pasta, nenhum código.
- `Resultado esperado` — serviço no ar com upload e fetch por URL respondendo.
- `Requisitos` — um por item do `Escopo`, incluindo o `X-Idempotency-Key` e a exclusão explícita de
  `/v1/health` e `/v1/metrics` do `preHandler`.
- `Critérios de aceitação` — o serviço sobe por um comando; upload binário devolve id e o objeto
  gravado; fetch por URL devolve id; `/v1/health` responde 200 `"UP"` com banco de pé e 503
  `"DEGRADED"` sem ele; dois POSTs com o mesmo `idempotencyKey` não criam duas imagens; nenhuma
  rota de imagem responde sem token; `/health` e `/metrics` respondem sem token.
- `Validação` — subir, exercitar upload e fetch, parar o banco e conferir o 503, repetir o POST com
  a mesma chave, e `curl` sem token em rota protegida e em `/v1/health`.
- `Evidências` — saída de `docker compose up`, saída do upload e do fetch, saída do `/health` nos
  dois estados, saída do POST repetido, saída dos dois `curl` sem token.
- `Limitações / notas` — a segurança deste serviço é **inexistente de propósito** e está
  registrada como `C-01` a `C-08` em
  [`docs/inventory.md`](../docs/inventory.md). A [Issue 05](05-codigo-corrigido.md) é quem fecha
  isso; esta Issue não promete nada além de funcionar.

- [ ] **Step 3: Check de Review Focus 3 — o app funciona**

O único check que importa nesta Issue: os critérios de aceitação exigem upload e fetch **funcionando**,
não "implementado". Check por presença das duas rotas e do par 200/503:

```bash
for k in '/v1/images' 'multipart' 'idempotencyKey' '200' '503' 'SIGTERM' 'preHandler'; do
  grep -q "$k" image-forge/issues/01-app-local.md || echo "FALTA: $k"
done
```

Expected: sem `FALTA`.

- [ ] **Step 4: Check de template contra o gold standard**

```bash
diff <(grep '^## ' commerce-api/issues/05-observability.md) <(grep '^## ' image-forge/issues/01-app-local.md) && echo TEMPLATE_OK
```

Expected: `TEMPLATE_OK`.

- [ ] **Step 5: Check de que a Issue não virou aula nem promete segurança**

```bash
grep -q '```' image-forge/issues/01-app-local.md && echo "FALHA: cerca de codigo"
grep -A4 '^## Fora de escopo' image-forge/issues/01-app-local.md | grep -qi 'segurança' && echo "OK: inseguranca declarada fora de escopo" || echo "FALHA: precisa declarar a insegurancia como fora de escopo"
```

- [ ] **Step 6: Commit**

```bash
git add image-forge/AGENTS.md image-forge/issues/01-app-local.md
git commit -m "feat(image-forge): issue 01 app local"
```

---

## Task 5: Issue 02 — o estado insano versionado

**Files:**
- Create: `image-forge/issues/02-infra-legacy-versionada.md`

**Interfaces:**
- Consumes: Task 4 (a Issue 01), Task 2 (o inventário).
- Produces: o slug e o diretório `infra/legacy/` que a Issue 03 varre.

- [ ] **Step 1: Frontmatter e H1**

```yaml
---
aliases: [issue-02, infra-legacy, terraform, estado-insano]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---
```

H1: `# Issue 02 — Estado insano versionado: a stack com os defeitos do inventário plantados e nomeados`

- [ ] **Step 2: Escrever as 13 seções**

- `Contexto` — um serviço que só existe localmente não tem superfície de nuvem para diagnosticar. E
  um problema de segurança que não está em IaC versionada é um problema que ninguém revisa.
- `Dependências` — [Issue 01](01-app-local.md).
- `Escopo` — rede multi-tier declarada (VPC, subnets pública/privada/isolada, IGW, route tables,
  SGs); RDS Postgres isolado; bucket S3; ALB com target group e health check; IAM da task; a
  **população de `infra/legacy/`** com `I-01` a `I-08`; endpoints apontando para LocalStack na maior
  parte da trilha; `terraform plan` limpo e idempotente.
- `Fora de escopo` — **consertar qualquer defeito.** Esta Issue entrega o problema. `I-09` a
  `I-14` **não** entram aqui: são ausência (CloudTrail, tagging, CDN, segundo ambiente) e a Issue
  que fecha cada uma é `04`, `08` e `11` — declaradas no inventário, não plantadas.
- `Conhecimentos envolvidos` — HCL, estado, idempotência, VPC multi-tier, SGs, ALB, IAM; e a prática
  de **isolar recurso inseguro em arquivo próprio com o motivo escrito**.
- `Estado atual` — não existe `infra/`. LocalStack não está configurado.
- `Resultado esperado` — um `terraform apply` que sobe a stack **com os defeitos**, cada um
  identificável por `ls infra/legacy/` e rastreável ao inventário.
- `Requisitos` — um item por arquivo de `infra/legacy/`, cada um exigindo: o ID do inventário no
  **nome ou no cabeçalho do arquivo**, um comentário dizendo o que está errado, e o link para a
  Issue que remove; um item exigindo que a `terraform validate` e o `plan` passem mesmo com os
  defeitos; um item exigindo que nenhum recurso **fora** do inventário esteja publicado em
  `0.0.0.0/0`; um item exigindo endpoints de LocalStack.
- `Critérios de aceitação` — a stack sobe; os 8 arquivos de `infra/legacy/` existem e cada um cita
  o ID e a Issue; `terraform plan` é limpo na segunda aplicação; o SG do Postgres está em
  `0.0.0.0/0` e isso está escrito no arquivo; o bucket aceita leitura anônima e isso está escrito;
  a IAM é `Action: "*"` e isso está escrito.
- `Validação` — `terraform validate`; `apply` contra LocalStack; `plan` duas vezes para idempotência;
  `nmap` ou equivalente contra o Postgres confirmando a porta aberta; leitura anônima do bucket
  confirmando o acesso público; chamadaSTS assumindo a IAM.
- `Evidências` — saída do `apply`; saída do `plan` limpo; saída do `nmap`; saída da leitura anônima;
  saída do `sts` assumindo a IAM; o `ls infra/legacy/`.
- `Limitações / notas` — **a exceção de `0.0.0.0/0` vem do inventário e só do inventário.** Um
  recurso aberto fora dos `I-01`–`I-14` é violação de `AGENTS.md`, não método de aprendizagem. `infra/legacy/` é a
  decisão da spec em que a implantação é dona do grosso dos defeitos: eles ficam isolados e
  nomeados, não espalhados.

- [ ] **Step 3: Check de Review Focus 1 e 4 — o inventário autoriza, e a Issue não conserta**

```bash
for id in I-01 I-02 I-03 I-04 I-05 I-06 I-07 I-08; do
  grep -q "$id" image-forge/issues/02-infra-legacy-versionada.md || echo "FALTA: $id"
done
grep -A4 '^## Fora de escopo' image-forge/issues/02-infra-legacy-versionada.md | grep -qi 'consertar' \
  && echo "OK: nao conserta" || echo "FALHA: precisa dizer que nao conserta"
```

Expected: 8 IDs presentes e "OK: nao conserta".

- [ ] **Step 4: Check de template**

```bash
diff <(grep '^## ' commerce-api/issues/05-observability.md) <(grep '^## ' image-forge/issues/02-infra-legacy-versionada.md) && echo TEMPLATE_OK
```

- [ ] **Step 5: Commit**

```bash
git add image-forge/issues/02-infra-legacy-versionada.md
git commit -m "feat(image-forge): issue 02 estado insano versionado"
```

---

## Task 6: Issue 03 — os acham

**Files:**
- Create: `image-forge/issues/03-deteccao-dos-defeitos.md`

**Interfaces:**
- Consumes: Tasks 2 e 5. A Issue 02 planta; esta acha.
- Produces: a lista de achados, que é a matéria-prima das Issues `04` a `07`.

- [ ] **Step 1: Frontmatter e H1**

```yaml
---
aliases: [issue-03, deteccao, scan, sast,achados]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---
```

H1: `# Issue 03 — Detecção: as ferramentas de terceiros encontram o que a Issue 02 plantou`

- [ ] **Step 2: Escrever as 13 seções**

- `Contexto` — a Issue 02 entregou uma stack insegura e **versionada**, o que é o meio de ela ser
  diagnosticável. Mas um defeito que ninguém procura é um defeito que permanece: alguém precisa
  apontar a ferramenta e produzir o achado.
- `Dependências` — [Issue 02](02-infra-legacy-versionada.md).
- `Escopo` — scan de IaC (Checkov, tfsec ou equivalente) contra o Terraform; scan de imagem
  (`trivy`) contra a imagem construída; auditoria de configuração de segurança em nuvem (AWS
  Security Hub, Config ou equivalente) apontando `I-01` a `I-08`; scanner de rede confirmando o
  Postgres exposto; scanner TLS confirmando a ausência de TLS; detector de segredo no repositório
  apontando `I-10`; e a **lista de achados**, cada linha com o ID do inventário e a ferramenta que o
  achou.
- `Fora de escopo` — **corrigir qualquer coisa.** Esta Issue produz achado, não conserto. Correção é
  `04` (rede, exposição, permissões), `05` (código) e `06` (secrets, imagem).
- `Conhecimentos envolvidos` — o que cada scanner sabe e o que ele **não** sabe; sair alvo e o que
  isso significa; severidade reportada vs. severidade real.
- `Estado atual` — a stack da Issue 02 existe; nenhuma ferramenta foi rodada contra ela.
- `Resultado esperado` — a lista de achados, com cada ID do inventário marcado como
  `detectado` ou `não detectado por ferramenta`, e a explicação quando um não foi.
- `Requisitos` — um item por classe de scan; um item exigindo a **lista de achados** com ID e
  ferramenta; um item exigindo que um ID que **nenhuma** ferramenta achou seja registrado como
  `não detectado` **com a razão** — porque um defeito invisível é o achado mais importante do
  exercício; um item exigindo que o detector do inventário seja confrontado com o que a ferramenta
  realmente achou.
- `Critérios de aceitação` — cada `I-01` a `I-08` aparece na lista de achados; cada linha tem o ID
  e o nome da ferramenta; os IDs do inventário cujo Detector prometeu achar e não achou estão
  registrados como `não detectado` com razão; nenhum conserto foi aplicado.
- `Validação` — rodar cada scanner e conferir que a saída menciona o recurso esperado; para um ID
  `não detectado`, conferir que o motivo é real (ferramenta não cobre aquele recurso) e não
  preguiça.
- `Evidências` — a saída de cada scanner; a lista de achados; a tabela `ID × Detector ×
  Encontrado`.
- `Limitações / notas` — **um ID que nenhuma ferramenta acha invalida o Detector dele**, e o
  inventário precisa ser corrigido na mesma Issue, não depois. É por isso que a regra do inventário
  exige detector antes de o item entrar. Scanner que não cobre um recurso não é motivo para
  apagar o defeito: é motivo para o detector ser humano ou por painel.

- [ ] **Step 3: Check de Review Focus 4 — acha, não conserta**

```bash
for k in Checkov tfsec trivy Security\ Hub nmap gitleaks; do
  grep -q "$k" image-forge/issues/03-deteccao-dos-defeitos.md || echo "FALTA scanner: $k"
done
grep -A4 '^## Fora de escopo' image-forge/issues/03-deteccao-dos-defeitos.md | grep -qi 'corrigir' \
  && echo "OK: nao conserta" || echo "FALHA: precisa dizer que nao conserta"
```

- [ ] **Step 4: Check de template**

```bash
diff <(grep '^## ' commerce-api/issues/05-observability.md) <(grep '^## ' image-forge/issues/03-deteccao-dos-defeitos.md) && echo TEMPLATE_OK
```

- [ ] **Step 5: Commit**

```bash
git add image-forge/issues/03-deteccao-dos-defeitos.md
git commit -m "feat(image-forge): issue 03 deteccao dos defeitos"
```

---

## Task 7: Issues 04 a 08 — fechar o que foi achado

**Files:**
- Create: `image-forge/issues/04-rede-exposicao-permissoes.md`
- Create: `image-forge/issues/05-codigo-corrigido.md`
- Create: `image-forge/issues/06-supply-chain-secrets.md`
- Create: `image-forge/issues/07-ci-cd-com-gates.md`
- Create: `image-forge/issues/08-observabilidade-e-staging.md`

**Interfaces:**
- Consumes: Tasks 2, 5 e 6. A lista de achados da `03` é a entrada.
- Produces: os cinco slugs exatos acima, referenciados pelo `BOARD.md` na Task 9. Depois da `04` e
  da `11`, a exceção de `0.0.0.0/0` revoga os IDs correspondentes no inventário.

- [ ] **Step 1: `04` — Rede, exposição e permissões**

Fecha `I-01` a `I-09`. Escopo: SG do Postgres fechado à subnet da API; bucket privado; IAM de
princípio do menor privilégio; TLS no ALB com certificado; WAF associado; VPC endpoint para o S3;
backup com retenção declarada; CloudTrail, GuardDuty e Config ligados; `infra/legacy/` esvaziado
item a item. Critério de aceitação decisivo: **cada arquivo de `infra/legacy/` que desapareceu
tinha o ID e a Issue nele** — a exceção não some por acaso.

- [ ] **Step 2: `05` — Código corrigido**

Fecha `C-01` a `C-08` e os RNF de ingestão. Escopo: SSRF com allowlist de esquema e bloqueio de
rede privada **depois** da resolução DNS; redirect reavaliado a cada salto; allowlist de MIME com
verificação de magic bytes; limite de tamanho no upload e na resposta do fetch; `storage_key`
derivada de valor controlado, nunca de `original_filename`; log sem query string e sem corpo;
rate limit e cota por usuário. Critério decisivo: DAST passa a não achar nenhum achado, e um teste
dedicado prova que `http://169.254.169.254` é recusado.

- [ ] **Step 3: `06` — Supply chain e secrets**

Fecha `I-10` e `I-11`. Escopo: Secrets Manager com rotação; credencial fora do versionado; imagem
base atualizada; `trivy` no gate; SBOM. Critério decisivo: `gitleaks` limpo, `trivy image` sem CVE
alta ou crítica, e o pipeline **reprova** quando o segredo volta.

- [ ] **Step 4: `07` — CI/CD com gates**

Escopo: build e teste em script local e o workflow só chama; gate de teste, de imagem, de IaC, de
segredo e de SAST; um job que **reintroduz** o defeito e prova que o gate barra. Critério
decisivo: cada um dos 4 IDs que a `03` achou **não** passa pelo pipeline. Esta é a Issue que prova
que a `03` não foi decorativa.

- [ ] **Step 5: `08` — Observabilidade e staging**

Fecha `I-14` (segundo ambiente). Escopo: `/v1/metrics` com `prom-client`; Prometheus e Grafana com
golden signals; alerta com `for` e canal; ambiente de staging separado que **nunca toca produção**.
Critério decisivo: uma falha injetada aparece no painel e um alerta **dispara de verdade**, com
evidência do canal. Fecha o que a `03` não podia achar, porque ausência não é acha.

- [ ] **Step 6: Check de template nas cinco**

```bash
for f in image-forge/issues/0[4-8]-*.md; do
  diff <(grep '^## ' commerce-api/issues/05-observability.md) <(grep '^## ' "$f") >/dev/null \
    && echo "OK   $f" || echo "FORA $f"
done
```

Expected: cinco `OK`.

- [ ] **Step 7: Check de Review Focus 4 — nenhuma delas conserta o que não é dela**

```bash
for f in image-forge/issues/0[4-8]-*.md; do
  echo "--- $f"
  awk '/^## Dependências$/{s=1;next} /^## /{s=0} s' "$f" | grep -oE '[0-9]{2}-[a-z-]+\.md' | sort -u | tr '\n' ' '
  echo
done
```

Expected: `04 → 03`; `05 → 01`; `06 → 02 03`; `07 → 03 04 05 06`; `08 → 07`. Nenhuma declara a Issue
que a destrói.

- [ ] **Step 8: Commit**

```bash
git add image-forge/issues/
git commit -m "feat(image-forge): issues 04-08 rede, codigo, supply chain, gates e observabilidade"
```

---

## Task 8: Issues 09 a 12 — performance e custo

**Files:**
- Create: `image-forge/issues/09-capacidade-e-rampa.md`
- Create: `image-forge/issues/10-gargalo-corrigido.md`
- Create: `image-forge/issues/11-custo-e-right-sizing.md`
- Create: `image-forge/issues/12-inventario-fechado.md`

**Interfaces:**
- Consumes: Tasks 2, 7. A `09` **reusa** o método de `commerce-api 09 → 13` e referencia
  [`docs/performance/dicionario-de-medicao.md`](../../docs/performance/dicionario-de-medicao.md).
- Produces: os quatro slugs exatos, e a `12` revoga a exceção de `AGENTS.md`.

- [ ] **Step 1: `09` — Capacidade e rampa**

Escopo: a conta de capacidade reaproveitando o método de `commerce-api 09`; script k6 com
`ramping-vus` contra `/v1/images/:id/variants/:variant` e `/v1/images`; event loop lag no
`/v1/metrics`; curva RPS × p95 × erros; **ponto de inflexão registrado**. Fecha `G-01` a `G-07` e
`G-09` a `G-10` como **medidos**. Fora de escopo: corrigir, e HPA — que é a Issue 18.

- [ ] **Step 2: `10` — Gargalo corrigido com prova**

Escopo: **uma** mudança de maior efeito, o mesmo script da `09` re-executado, delta lado a lado. O
candidato natural é o `G-01`: materializar a variante e servir do CDN em vez de recomputar — que é
justamente o `I-12` de CDN ausente, então a Issue toca infraestrutura e código. Fora de escopo: mais de
uma mudança, e otimização de segunda ordem.

- [ ] **Step 3: `11` — Custo e right-sizing**

Fecha `G-08`, `I-12` e `I-13`. Escopo: o CDN que faltava; tags de custo; custo por vazão
sustentada a partir do ponto de inflexão da `09`; checagem de right-sizing contra a instância que a
`09` dimensionou. **Ressalva obrigatória na Issue:** o CDN é a primeira parte da trilha que
**exige conta AWS real**, porque LocalStack não emula CDN de forma utilizável — e o
`terraform destroy` da prova final é registrado.

- [ ] **Step 4: `12` — A casa fecha**

Escopo: o inventário com as 22 linhas em `resolvido`, cada uma com a **evidência** que prova que
sumiu; a exceção de `AGENTS.md` revogada; a regra de `0.0.0.0/0` valendo para os 4 apps; e o
`archive/18` reabilitado como trilha futura. Critério decisivo: **o inventário é o documento que
permanece depois que a pessoa esquecer tudo** — e é ele que sobrevive como reimplementação.

- [ ] **Step 5: Check de template nas quatro**

```bash
for f in image-forge/issues/09-*.md image-forge/issues/1[0-2]-*.md; do
  diff <(grep '^## ' commerce-api/issues/05-observability.md) <(grep '^## ' "$f") >/dev/null \
    && echo "OK   $f" || echo "FORA $f"
done
```

Expected: quatro `OK`.

- [ ] **Step 6: Check de Review Focus 5 — o custo e a exceção de nuvem estão escritos antes da hora**

```bash
grep -q 'conta AWS real' image-forge/issues/11-custo-e-right-sizing.md \
  && echo "OK: ressalva de custo escrita na Issue" || echo "FALHA: ressalva de custo ausente"
grep -q 'archive/18' image-forge/issues/12-inventario-fechado.md \
  && echo "OK: k8s reabilitado na 12" || echo "FALHA: k8s ausente"
```

- [ ] **Step 7: Check de que a `09` referencia o dicionário e o método reusado**

```bash
grep -q 'dicionario-de-medicao.md' image-forge/issues/09-capacidade-e-rampa.md \
  && echo "OK: reusa o dicionario" || echo "FALHA: dicionario ausente"
grep -q 'commerce-api/issues/09-plano-de-capacidade.md' image-forge/issues/09-capacidade-e-rampa.md \
  && echo "OK: reusa o metodo" || echo "FALHA: metodo ausente"
```

- [ ] **Step 8: Commit**

```bash
git add image-forge/issues/
git commit -m "feat(image-forge): issues 09-12 capacidade, gargalo, custo e fechamento"
```

---

## Fase 3 — O tracker

## Task 9: BOARD, app AGENTS e README

**Files:**
- Modify: `BOARD.md`
- Modify: `README.md` (raiz, se a Task 3 não o cobriu)
- Create: `image-forge/README.md`

**Interfaces:**
- Consumes: os 12 slugs dos Tasks 4, 5, 6, 7 e 8.
- Produces: a seção do board, e a chain `01 → 02 → 03 → {04..08} → 09 → 10 → 11 → 12`.

- [ ] **Step 1: Nova seção no `BOARD.md`**

Cabeçalho `## image-forge · trilha AWS hardening`, com o parágrafo de estado final, as 12 linhas e a
cadeia. A linha de estado final precisa dizer, sem eufemismo: **serviço real e utilizável, inseguro
e lento por construção, com cada defeito rastreado a uma Issue e a uma ferramenta que o acha.**

- [ ] **Step 2: `image-forge/README.md`**

Índice das 12 Issues, do `docs/inventory.md`, dos comandos de `image-forge/AGENTS.md`, e o aviso de
que `app/` e `infra/` são construção do usuário.

- [ ] **Step 3: Mover `archive/18` para fora de "Fora de escopo"**

Ele deixa de ser "candidata a 4ª trilha" e passa a ser "trilha futura sobre o `image-forge`",
habilitada só depois da Issue `09`.

- [ ] **Step 4: Check de Review Focus 3 do plano anterior — board espelha frontmatter**

```bash
for n in 01 02 03 04 05 06 07 08 09 10 11 12; do
  slug=$(ls image-forge/issues/$n-*.md 2>/dev/null | head -1)
  base=$(basename "$slug" .md)
  board=$(grep -c "\[ \] \[[^]]*\](image-forge/issues/$base.md)" BOARD.md)
  fm=$(grep -c '^status: todo$' "$slug")
  echo "$base board=$board status=$fm"
done
```

Expected: doze linhas, todas `board=1 status=1`.

- [ ] **Step 5: Check de que nenhum app virou `done`**

```bash
grep -l '^status: done$' image-forge/issues/*.md || echo "OK: nenhuma nasce done"
```

Expected: `OK`. A trilha é entregue **aberta**. Fechar Issue exige evidência de validação, e este
plano não produz nenhuma.

- [ ] **Step 6: Commit**

```bash
git add BOARD.md README.md image-forge/README.md
git commit -m "docs(tracker): registra a trilha do image-forge"
```

---

## Task 10: Verificação final

**Files:** nenhum. Modify: nada.
**Test:** consistência do conjunto.

- [ ] **Step 1: As 12 respondem ao template**

```bash
for f in image-forge/issues/*.md; do
  diff <(grep '^## ' commerce-api/issues/05-observability.md) <(grep '^## ' "$f") >/dev/null \
    && echo "OK   $f" || echo "FORA $f"
done
```

Expected: doze `OK`.

- [ ] **Step 2: Todo item do inventário tem Issue que o remove**

```bash
awk -F'|' '/^\| *[IC]-[0-9]/ {gsub(/ /,"",$2); gsub(/ /,"",$6); print $2, $6}' \
  image-forge/docs/inventory.md | while read id issue; do
  [ "$issue" = "—" ] && echo "SEM ISSUE: $id"
done
```

Expected: sem saída. Item de inventário sem Issue é defeito sem dono.

- [ ] **Step 3: Todo ID citado nas Issues existe no inventário**

```bash
grep -ohE '\b[IC]-[0-9]{2}\b' image-forge/issues/*.md | sort -u | while read id; do
  grep -q "$id" image-forge/docs/inventory.md || echo "ID FANTASMA: $id"
done
```

Expected: sem saída.

- [ ] **Step 4: Nenhuma Issue virou aula**

```bash
grep -lniE 'passo a passo|como fazer|\[Prev\]|\[Next\]' image-forge/issues/*.md || echo "OK"
```

- [ ] **Step 5: Nenhuma Issue exige recurso pago sem declarar**

```bash
grep -lniE 'conta aws|instância real|criar uma vps' image-forge/issues/*.md
```

Expected: **apenas** `11-custo-e-right-sizing.md`, que declara a ressalva. Qualquer outra é
violação da regra de custo zero.

- [ ] **Step 6: Nada fora do escopo gravável foi tocado**

```bash
MB=$(git merge-base main HEAD)
git diff --name-only "$MB"..HEAD | grep -vE '^(docs/|image-forge/(issues/|docs/|AGENTS\.md|README\.md)|BOARD\.md|00-visao-geral\.md|AGENTS\.md|README\.md)$'
```

Expected: sem saída. Nada sob `app/`, `infra/`, `.github/`, `.tf`, `.agents/` ou `healthcheck.sh`.

- [ ] **Step 7: A premissa do repo é consistente nos dois sentidos**

```bash
grep -rq '4 apps, 4 trilhas' AGENTS.md 00-visao-geral.md && echo "OK: premissa 4 apps nos dois"
grep -rq '3 apps, 3 trilhas' AGENTS.md 00-visao-geral.md && echo "FALHA: sobrou '3 apps'"
```

Expected: a primeira, e **não** a segunda.

- [ ] **Step 8: A exceção está datada e nomeada**

```bash
grep -A5 'exceção' AGENTS.md | grep -cE '202[0-9]-[0-9]{2}-[0-9]{2}|inventory\.md'
```

Expected: ≥ 2. Data **e** link para o inventário, porque sem os dois a exceção é uma autorização
vazia.

- [ ] **Step 9: Reportar sem fechar nada**

Relatar os arquivos tocados, a cadeia, e as 12 Issues com `status:`. **Nenhuma** marcada como
`done`: a política de `00-visao-geral.md` exige evidência de validação registrada, e o produto
deste plano é o tracker, não a capacidade.
