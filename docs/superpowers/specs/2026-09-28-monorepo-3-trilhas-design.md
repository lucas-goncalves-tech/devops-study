# Spec — Monorepo de 3 apps, cada app = 1 trilha completa

- **Data:** 2026-09-28
- **Status:** aguardando revisão do usuário (não implementar antes da aprovação)
- **Caminho do processo:** architectural (brainstorming → spec → writing-plans → implementação)

## Contexto

O repositório `securepay-devops` concentra hoje código (`backend/`), infra (`infra/`) e tracker
(`.tracker/` com 18 Issues + estudos) de um único app. Fica confuso porque mistura três coisas
com naturezas diferentes: código, material de estudo e trabalho pendente.

O usuário quer transformar o repo em **monorepo com 3 apps**, onde **cada app é uma trilha de
aprendizado completa (do zero até produção)** — não fatias de trilha distribuídas entre apps.

Existem 3 trilhas já construídas em `/home/drummonds/Projects/devops-study/stages-labs/`
(cada uma com `README.md` de arena + oráculos `verify.py`):

| Trilha | Estágios | Conteúdo |
|---|---|---|
| `spring-cloud-platform` | 5 | linux-runtime, docker-compose, terraform-vpc, github-actions, observability |
| `microservices-kubernetes` | 4 | containers-e-redis, devsecops-gates, kubernetes-helm, aws-production |
| `vps-devsecops-production` | 5 | vps-hardening, caddy-reverse-proxy, compose-isolation, db-backups, cicd-vps-deploy |

O `.tracker/` atual do `securepay-devops` (Issues 01–18) é, na prática, uma **mistura dessas
trilhas** aplicada a um app só — origem da confusão.

## Decisões tomadas

| # | Decisão | Resposta do usuário |
|---|---|---|
| 1 | Repo que sobrevive | `securepay-devops` **absorve** os 2 outros apps |
| 2 | Estrutura | **3 camadas dentro de cada pasta de app**: `app/`, `issues/`, `estudos/` |
| 3 | `app/ledger-service` do devops-study | é o **mesmo código** do `backend/` atual (não migra de lá) |
| 4 | Terraform | vive **dentro de cada app**; a infra do ledger é **apagada** (histórico no git preserva) |
| 5 | Origem dos 2 apps | só os apps migram do `devops-study`; `stages-labs/`, `docs/`, `ROADMAP.md`, `.agents/` de lá **não migram** (repo fica onde está) |
| 6 | Board | **único na raiz** (`BOARD.md` + `00-visao-geral.md`) |
| 7 | `AGENTS.md` | raiz = routing/convenções; cada app = domínio |
| 8 | Histórico git | **cópia limpa** (commit único, sem `filter-repo`/`subtree`); repo no GitHub continua `securepay-devops` |
| 9 | Distribuição das trilhas | uma trilha inteira por app (ver tabela abaixo) |
| 10 | Conteúdo das Issues | **só Issues, sem `verify.py`** — escopo + critérios de aceite + evidência obrigatória |
| 11 | Issues 08 e 18 | `08` (Redis/Streams/gateway) → `webhook-gateway` como extra; `18` (k8s/Helm) → **fora de escopo**, registrado como 4ª trilha futura |
| 12 | Produção | **sistema em produção de verdade + staging falho** (não incorporar os apps, não deixar produção insegura) |
| 13 | CI / infra | **o agente nunca cria CI, Terraform, Dockerfile, compose ou código de infra** — é só do usuário |

## Trilhas × Apps

| App | Trilha | Justificativa |
|---|---|---|
| `ledger-service` (Java/Spring) | **VPS** — linux → hardening → caddy → isolamento → backups → deploy | RAM da JVM não cabe em free tier; Redis de serviço à parte é quase sempre pago |
| `commerce-api` (Node+Postgres) | **AWS local → produção** — linux → docker → terraform/LocalStack → CI → observabilidade → S3/EC2 | leve, cabe em free tier |
| `webhook-gateway` (Node+Redis) | **DevSecOps** — secrets → SAST → hardening → gates (agnóstico de cloud) | trilha é de CI/análise estática, não exige cloud pada; coerente com Redis pago |

O que é comum a todas (Linux, Docker, CI/CD) **se repete em cada app** — cada trilha vai do zero
ao fim, mantendo o escopo final dela.

## Estrutura alvo

```text
securepay-devops/
├── AGENTS.md               # routing: mapa, convenções, escopo de escrita, skills, política do tracker
├── README.md               # índice público do monorepo
├── BOARD.md                # board único, seção por app
├── 00-visao-geral.md       # metodologia, narrativa das 3 trilhas, "fora de escopo"
├── archive/                # 18-kubernetes-helm (issue + estudo)
├── .obsidian/              # vault config (era .tracker/.obsidian/)
├── .agents/skills/         # inalterado
├── .learning/              # inalterado
├── .github/workflows/      # INTOCADO (CI existente do ledger)
├── docs/, skills-lock.json, .gitignore, healthcheck.sh   # inalterados
│
├── ledger-service/         # trilha VPS
│   ├── AGENTS.md · README.md
│   ├── app/                # era backend/  (sem infra/ — apagada)
│   ├── issues/             # 01..08
│   └── estudos/            # 01..08
├── commerce-api/           # trilha AWS
│   ├── AGENTS.md · README.md
│   ├── app/                # importado de devops-study/app/commerce-api
│   ├── issues/             # 01..08
│   └── estudos/            # 01..08
└── webhook-gateway/        # trilha DevSecOps
    ├── AGENTS.md · README.md
    ├── app/                # importado de devops-study/app/webhook-gateway
    ├── issues/             # 01..09
    └── estudos/            # 01..09
```

Não existe `infra/` na estrutura: infra é do usuário.

## Migração (de → para)

| Origem | Destino | Nota |
|---|---|---|
| `backend/` | `ledger-service/app/` | rename |
| `infra/` | **apagado** | histórico no git; Issue 03 do commerce nasce do zero |
| `.tracker/issues/*` | distribuído (tabela abaixo) | 18 de 18 têm destino: 17 reutilizadas, `18` arquivada |
| `.tracker/estudos/*` | distribuído junto com a sua Issue | nenhum estudo se perde |
| `.tracker/BOARD.md` | `BOARD.md` (raiz) | 17 links `issues/NN.md` → `ledger-service/issues/NN.md` |
| `.tracker/00-visao-geral.md` | `00-visao-geral.md` (raiz) | reescrito para as 3 trilhas |
| `.tracker/.obsidian/` | `.obsidian/` (raiz) | `.gitignore` ajustado |
| `devops-study/app/commerce-api` | `commerce-api/app/` | cópia limpa, sem histórico |
| `devops-study/app/webhook-gateway` | `webhook-gateway/app/` | cópia limpa, sem histórico |
| `healthcheck.sh` | inalterado na raiz | específico do ledger, mas é infra → não move |

## Issues por app

### `ledger-service` — trilha VPS (8)

| # | Issue | Origem |
|---|---|---|
| 01 | Linux Runtime (systemd, env, healthcheck L4/L7, SIGTERM) | reuse `01` |
| 02 | Docker Compose multi-stage non-root | reuse `02` |
| 03 | VPS Hardening (UFW, chave-only, fail2ban, swap) | reuse `04` |
| 04 | Caddy reverse proxy + TLS | reuse `05` |
| 05 | Compose production isolation (redes, anti-OOM, DB cego) | reuse `09` |
| 06 | Backups off-site + restore testado | reuse `06` |
| 07 | CI/CD deploy via SSH + rollback | reuse `17` |
| 08 | **NOVA** — Tráfego sintético & alertas (k6 em cron, alerta real) | nova |

### `commerce-api` — trilha AWS (8)

| # | Issue | Origem |
|---|---|---|
| 01 | Linux Runtime (adaptado a Node/npm) | clone de `01` |
| 02 | Docker Compose (aproveita o Dockerfile já existente) | clone de `02` |
| 03 | Terraform VPC multi-tier no LocalStack (**do zero**) | reuse `03` |
| 04 | GitHub Actions: testes + scan + gate IaC | reuse `10` |
| 05 | Observabilidade (métricas, painéis, carga) | reuse `07` |
| 06 | Bucket S3 + IAM least-privilege | reuse `11` |
| 07 | AWS real: state remoto com lock, EC2, custo conhecido | reuse `12` |
| 08 | **NOVA** — Staging falho de observabilidade (falhas injetadas, detectar/diagnosticar/consertar) | nova |

### `webhook-gateway` — trilha DevSecOps (9)

| # | Issue | Origem |
|---|---|---|
| 01 | Linux Runtime | clone de `01` |
| 02 | Docker Compose — **nasce o Dockerfile que falta** + healthcheck | clone de `02` |
| 03 | Secrets hygiene (gitleaks + baseline) | reuse `13` |
| 04 | SAST Semgrep bloqueante | reuse `14` |
| 05 | Pipeline hardening (least-privilege, SHA pin) | reuse `15` |
| 06 | Gates consolidados (secrets + SAST + CVE) | reuse `16` |
| 07 | Pipeline agnóstica (roda igual em GitHub/GitLab/VPS) | nova, sintetizada de `10`/`17` |
| 08 | Redis Streams em produção (compose + Redis + consumer) | reuse `08` |
| 09 | **NOVA** — Staging inseguro de propósito (falha que os gates têm que pegar + forense de mensageria) | nova |

**Total: 25 Issues (eram 18).**

### Fora de escopo (registrado no `00-visao-geral.md`)

- `18` Kubernetes/Helm → `archive/` (issue + estudo), candidato a 4ª trilha
- `interview-prep-finops`, `ansible` (trilhas do devops-study) — não são das 3 trilhas escolhidas

## Estudos

Regra: **toda Issue tem um estudo irmão com o mesmo número.** Os estudos existentes migram junto
da Issue que servem, **renumerados**; as Issues novas (clonadas ou criadas) ganham estudo novo.

| App | estudos reutilizados | Renumeração | estudos novos | Total |
|---|---|---|---|---|
| `ledger-service` | 01, 02, 04, 05, 06, 09, 17 | → `01..07` | `08` | 8 |
| `commerce-api` | 03, 07, 10, 11, 12 | → `03..07` | `01`, `02`, `08` | 8 |
| `webhook-gateway` | 13, 14, 15, 16, 08 | → `03..06` e `08` | `01`, `02`, `07`, `09` | 9 |

Estudos clonados (`01` Linux, `02` Docker) são **reescritos** para a stack do app alvo
(Maven vs npm) — cada app é autocontido, sem apontar para pasta de outro app.

## AGENTS.md / README.md

**Raiz `AGENTS.md` (~60 linhas, só routing):**
1. Identidade do monorepo (3 apps, 3 trilhas, produção real + staging falho)
2. Filetree novo + tabela app → trilha → estágio atual
3. Convenções do tracker: template fixo (Contexto → Limitações/notas), política de status,
   proibição de tutorial/FAQ/`Prev`-`Next`/sub-etapas; estudo só em `estudos/`
4. Escopo de escrita: graváveis = `ledger-service/`, `commerce-api/`, `webhook-gateway/`,
   `BOARD.md`, `00-visao-geral.md`, `archive/`; **infra/CI/código são do usuário**
5. Skills: `using-superpowers` no startup, `ask-matt`, `teach-anything` (read-only), grilling via `question`
6. Fechar Issue exige evidência no terminal (o `verify.py` saiu; evidência virou obrigatória)

**Raiz `README.md`:** índice público — o que é o monorepo, os 3 apps, as 3 trilhas, como rodar.

**`AGENTS.md` por app (só domínio):** tabela de Issues da trilha, stack e comandos, env vars,
healthcheck, gaps conhecidos, `infra` local se houver. Ex.: `webhook-gateway/AGENTS.md` já nasce
documentando que não tem Dockerfile/healthcheck até fechar a Issue 02.

**`README.md` por app:** público e curto — o que é, como rodar, links para `issues/` e `estudos/`.

**Reescrever menções quebradas:** `AGENTS.md` linhas com `.tracker/`, `backend/`, write scope;
skill `.agents/skills/teach-anything/SKILL.md` (menções a `backend/`/`infra/`).

## Produção + staging

- **Produção = sistema único:** VPS com `ledger + postgres + redis + webhook` num compose só,
  atrás do Caddy com domínio/TLS (Issues 03→07 do ledger); `webhook` entra na stack pela Issue 08;
  `commerce` vai para EC2 pela Issue 07.
- **Staging falho = ambiente separado, nunca a produção** (repo público): as Issues novas
  `ledger 08` (tráfego/alerta), `commerce 08` (falha de observabilidade), `webhook 09`
  (insegurança proposital + forense de mensageria).
- O staging, o k6 e as injeções de falha são **infra/código do usuário**; o agente escreve
  só o texto da Issue e do estudo.

## Escopo de execução

**O agente cria:** pastas do monorepo, moves/renames, `issues/`, `estudos/`, `archive/`,
`BOARD.md`, `00-visao-geral.md`, `AGENTS.md`/`README.md` (raiz + 3 apps), `.obsidian/`,
`.gitignore`, rewrite de links quebrados, import limpo dos 2 apps.

**O agente NUNCA cria nem altera:** `.github/workflows/`, Terraform, Dockerfile,
`docker-compose.yaml`, `healthcheck.sh`, código dos apps, `.agents/skills/*` (exceto o rewrite
pontual de caminho na `teach-anything`).

## Riscos e verificações pendentes

1. **Links relativos:** as Issues se referem entre si por arquivo irmão — continuam válidas dentro
   da mesma pasta; `BOARD.md` e `AGENTS.md` precisam de rewrite (medido por grep).
2. **Refs a `backend/`/`infra/` em 7 Issues + 1 skill** — rewrite obrigatório.
3. **webhook-gateway nasce incompleto** (sem Dockerfile/health/.env.example) — documentado na
   Issue 02 e no `AGENTS.md` do app, não consertado pelo agente.
4. **CI do ledger** fica como está; se o rename quebrar paths dela, o agente aponta o diff e o
   usuário decide.
5. **Cópia limpa** = histórico dos 2 apps só no `devops-study` (que continua no GitHub).

## Etapas seguintes

1. Usuário revisa este spec
2. `writing-plans` → plano de implementação passo a passo
3. Aprovação do plano → execução
