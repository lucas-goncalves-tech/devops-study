# Correções auditoria-30-Issues — ledger-service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corrigir no tracker da trilha VPS os achados Crítico/Importante da auditoria de 30 Issues de 02/10/2026 atribuídos a este app — healthcheck inline, evidências reais nas Issues `done`, topologia declarada do proxy, fim do Redis fantasma, dono para o coletor e dono para a migração local→VPS — sem tocar em status, template ou código.

**Architecture:** Edits em markdown do tracker: `ledger-service/issues/` (01, 03, 04, 06, 07, 08) + os contratos de comando em `ledger-service/AGENTS.md` e `ledger-service/README.md`, que hoje publicam `scripts/healthcheck.sh` como se existisse. A Task 2 é a única que executa comandos reais (validação das Issues `01`/`02`) para colar saída de verdade; nenhum arquivo versionado fora dos listados muda.

**Tech Stack:** Tracker markdown (template RFC de 13 seções), gates `bash`/`grep` que imprimem linha `OK`, bateria final com `awk`/`git`.

**Spec — achados desta auditoria destinados a este plano (C/I; Menores em `Diferidos`):**

- **C1** — `01:72` valida com `scripts/healthcheck.sh` inexistente (`ledger-service/scripts/` não existe no repo); a mesma âncora aparece em `04:93`, `07:116`, `08:81/92/102`, `AGENTS.md:75/76/86` e `README.md:23/26/50`. *Correção escolhida (aprovada): comandos inline — nenhum arquivo é criado.*
- **I2** — `01:76-81` e `02:80-85` (`done`) têm `Evidências` descritivas ("output de…"), não saída colada — política da casa exige saída real.
- **I3** — `04:32` proíbe mexer no compose do backend enquanto `04:93/94` exigem mexer; a topologia do Caddy (serviço do compose × serviço do host) nunca é declarada.
- **I4** — `06` promete Redis em Objetivo/Escopo/Requisitos/Critérios/Validação/Evidências (`16/25/26/59/61/71/79/87`) mas `06:102` admite que ele não está na stack (só chega na `webhook 12`).
- **I5** — divergência de rede `ledger 06` × `webhook 12` ("isolada" × "interna") **não é corrigida aqui**: o texto do `12` pertence ao plano `webhook-gateway` (Task 1 dele).
- **I6** — `08` usa coletor sem dono de rota/rede/firewall: `08:64` promete série visível sem declarar rede, e o caminho curto (abrir porta) quebraria o critério do firewall da `03`.
- **I7** — ninguém é dono de levar a stack local ao servidor: `03:90` chama servidor de pré-requisito, mas nenhuma Issue instala runtime nem copia a stack antes de `04`.

**Ordem:** este é 1 de 3 planos da rodada (`ledger`, `commerce`, `webhook`) executados no **mesmo branch** criado de `main@481a2c5` (via `superpowers:using-git-worktrees` na execução). Ordem entre os planos é indiferente; cada um tem sua bateria final com escopo em união dos três apps.

## Global Constraints

- Arquivos editáveis: apenas `ledger-service/issues/*.md`, `ledger-service/AGENTS.md`, `ledger-service/README.md` e este plano em `docs/`. Intocáveis: `BOARD.md`, frontmatter (`status:`, `prioridade:`, `tags:`), `estudos/`, `app/`, `scripts/`, `.github/`, IaC. **Zero mudança de `status:`; nenhum checkbox vira `[x]`.**
- Cada Issue mantém as 13 seções `## ` e o par requisito↔critério do template; só acrescentamos/removemos bullets dentro de seções existentes.
- PT-BR, RFC da casa: sem tutorial, FAQ, navegação (`Prev`/`Next`) nem sub-etapas (`1A`, `2B`).
- Um commit por task; cada gate roda com `set -euo pipefail` e imprime `Task N gate: OK`; gate vermelho = corrigir antes do próximo commit.
- `B0=$(git merge-base main HEAD)` (esperado `481a2c5`).
- Task 2 pode subir/remover containers e imagens (validação de runtime), mas **nenhum arquivo versionado** além dos do escopo pode mudar; `.env` é gitignored e nunca entra em commit.
- **Ruling pré-autorizado (Task 2):** se um comando de validação não puder rodar neste ambiente, **não fabricar saída** — registrar o erro real na seção `## Limitações / notas` da Issue com o marcador `**Ambiente indisponível:**` e sinalizar no relatório de execução. O gate aceita esse caminho.
- Entradas retroativas em `.agents/memory/progress.md` para as Issues `01`/`02` **não são fabricáveis** por uma sessão que não viu aquele fechamento — fora do escopo (ver `Diferidos`).

## Review Focus

- A Task 1 elimina **toda** referência a `scripts/healthcheck.sh` no diretório do app (gate `! grep -rq`).
- Textos novos contêm exatamente os tokens pinados nos gates; âncoras de substituição são únicas no arquivo.
- Task 2: saídas coladas vêm de comandos executados (linhas `$ ` em blocos), nunca reescritas de cabeça; caminho `Ambiente indisponível` só com erro real.
- `06` mantém a cláusula futura do Redis (`quando ele entrar`, linhas `102/103`) — só as promessas presentes saem.
- Nenhum `- [x]` novo, nenhum `status:` alterado, 13 seções por Issue.

---

### Step 0: Commit do plano

- [ ] Copiar este arquivo para o worktree e commitá-lo antes da Task 1:

```bash
git add docs/plano-correcoes-auditoria2-ledger-service.md
git commit -m "docs(tracker): plano de correção auditoria2 ledger-service"
```

### Task 1: Healthcheck L4/L7 inline em todo o contrato (C1)

**Files:**
- Modify: `ledger-service/issues/01-linux-runtime.md:72` — item da `## Validação`.
- Modify: `ledger-service/issues/04-caddy-reverse-proxy.md:93` — invariante de porta.
- Modify: `ledger-service/issues/07-cicd-vps-deploy.md:116` — invariante de saúde/rollback.
- Modify: `ledger-service/issues/08-trafego-sintetico-alertas.md:81`, `:92`, `:102` — critério, validação e evidência.
- Modify: `ledger-service/AGENTS.md:75`, `:76`, `:86` — tabela de comandos e seção de healthcheck.
- Modify: `ledger-service/README.md:22-27`, `:50` — bloco de exemplo e item da lista.

**Interfaces:**
- Consume: evidência da auditoria (C1: arquivo inexistente publicado como se existisse).
- Produz: contrato L4/L7 auto-contido em comando inline, herdado pelas Issues `04/07/08` e documentado em AGENTS/README sem arquivo auxiliar.

- [ ] **Step 1: Validação da `01` inline**

Substituir o item `- Rodar \`scripts/healthcheck.sh\` da trilha …` por item contendo obrigatoriamente: `sem arquivo auxiliar`, o comando L4 `bash -c '</dev/tcp/$HOST/$PORT'`, o comando L7 `curl … /actuator/health | grep -q '"status":"UP"'`, e `exit 0 com o serviço no ar` / `exit 1 com o serviço parado`.

- [ ] **Step 2: Invariante de porta do `04:93`**

Trocar a única ocorrência de `scripts/healthcheck.sh` por `o healthcheck L4/L7 da \`Issue 01\` (\`/dev/tcp\` + \`curl\`)`, mantendo o restante da linha (inclusive `health check do target group` — item diferido, não mexer).

- [ ] **Step 3: Rollback do `07:116`**

Trocar `o rollback usa \`scripts/healthcheck.sh\` desta trilha, que depende de \`curl\`` por `o rollback executa o healthcheck L4/L7 desta trilha (\`curl\` em \`/actuator/health\`, contrato da \`Issue 01\`)`, mantendo `503`, `sai com 1`, `loop` e `**isso é esperado**`.

- [ ] **Step 4: Três referências do `08`**

- `:81` → critério contendo `o healthcheck L4/L7 da \`Issue 01\` sai com 0`.
- `:92` → validação contendo `rodar o healthcheck L4/L7 da \`Issue 01\` durante a janela do teste`.
- `:102` → evidência contendo `e do healthcheck L4/L7 da \`Issue 01\` durante a janela`.

- [ ] **Step 5: Tabela e seção do `AGENTS.md`**

- `:75` → linha `| healthcheck L4/L7 (comandos da \`Issue 01\`) | prova L4+L7 | exit 0 com o serviço no ar, exit 1 com ele parado |`.
- `:76` → linha `| \`HOST\` e \`PORT\` nos mesmos comandos | healthcheck de host não local | mesmo contrato |`.
- `:86` → parágrafo iniciando `Os comandos do healthcheck L4/L7 — \`bash -c '</dev/tcp/$HOST/$PORT'\` e \`curl /actuator/health\` — provam duas coisas em ordem` (remover o link para o arquivo).

- [ ] **Step 6: README**

- Linhas `22-27`: parágrafo passa a se chamar contrato em `comando direto` e o bloco `bash` passa a conter exatamente `bash -c '</dev/tcp/${HOST:-localhost}/${PORT:-8080}'` e `curl -fsS "http://${HOST:-localhost}/${PORT:-8080}/actuator/health" | grep -q '"status":"UP"'` (remover a linha de execução `../scripts/healthcheck.sh`).
- `:50` → `- [\`scripts/\`](scripts/) — demais checks de troubleshooting desta trilha (construídos aqui)`.

- [ ] **Step 7: Gate da Task 1**

Run:
```bash
set -euo pipefail
! grep -rq 'scripts/healthcheck.sh' ledger-service/ \
  && grep -qF 'sem arquivo auxiliar' ledger-service/issues/01-linux-runtime.md \
  && grep -qF 'healthcheck L4/L7 da `Issue 01`' ledger-service/issues/04-caddy-reverse-proxy.md \
  && grep -qF 'healthcheck L4/L7 desta trilha' ledger-service/issues/07-cicd-vps-deploy.md \
  && grep -qF 'healthcheck L4/L7 da `Issue 01`' ledger-service/issues/08-trafego-sintetico-alertas.md \
  && grep -qF 'healthcheck L4/L7 (comandos da `Issue 01`)' ledger-service/AGENTS.md \
  && grep -qF 'Os comandos do healthcheck L4/L7' ledger-service/AGENTS.md \
  && grep -qF 'comando direto' ledger-service/README.md \
  && grep -qF 'dev/tcp' ledger-service/README.md \
  && echo "Task 1 gate: OK"
```
Expected: `Task 1 gate: OK`

- [ ] **Step 8: Commit**

```bash
git add ledger-service/issues/01-linux-runtime.md ledger-service/issues/04-caddy-reverse-proxy.md ledger-service/issues/07-cicd-vps-deploy.md ledger-service/issues/08-trafego-sintetico-alertas.md ledger-service/AGENTS.md ledger-service/README.md
git commit -m "docs(tracker): healthcheck L4/L7 inline — fim da referência ao script inexistente"
```

### Task 2: Evidências reais nas Issues done 01/02 (I2)

**Files:**
- Modify: `ledger-service/issues/01-linux-runtime.md` — seção `## Evidências` (4 bullets descritivos → legenda + bloco com saída real).
- Modify: `ledger-service/issues/02-docker-compose.md` — seção `## Evidências` (idem).

**Interfaces:**
- Consume: `## Validação` da `01` (inline, pós-Task 1) e da `02`; política da casa (saída real colada).
- Produz: evidência auditável do fechamento das duas Issues `done`.

⚠️ Dependência: rodar **depois** da Task 1 (a validação da `01` já não aponta para arquivo). ⚠️ Única task com execução de runtime — nada disso vai para os commits.

- [ ] **Step 1: Ambiente mínimo**

Criar Postgres efêmero para a `01` e `.env` da `02` se ausente (gitignored, deixar no lugar):

```bash
docker run -d --name pg-ev -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=securepay_db -p 127.0.0.1:5432:5432 postgres:16-alpine
[ -f ledger-service/app/.env ] || cp ledger-service/app/.env.example ledger-service/app/.env
```

- [ ] **Step 2: Executar a Validação da `01` e colar4 saídas**

Subir API no host (`./mvnw spring-boot:run` em `ledger-service/app/`, com o Postgres do contêiner em `localhost:5432`) e coletar, **nesta ordem**, quatro saídas — cada uma vira a legenda já existente + bloco cercado cujas linhas de comando começam com `$ `:

1. `ss -tulpn` filtrando a porta da API (precisa conter `:8080`) — sob a legenda da linha 78.
2. `curl -i http://localhost:8080/actuator/health` (precisa conter `HTTP/1.1 200` e `"status":"UP"`) — sob a legenda da linha 79.
3. Healthcheck L4/L7 da Task 1 com serviço **no ar** (esperado `exit 0`) e **parado** (esperado `exit 1`) — dois blocos ou um bloco com os dois cenários — sob a legenda da linha 80.
4. `kill -TERM` no PID da API + tail do log (desligamento sem exceção de pool) — sob a legenda da linha 81.

- [ ] **Step 3: Encerrar a execução da `01`**

Parar a API e remover o Postgres efêmero: `docker rm -f pg-ev`.

- [ ] **Step 4: Executar a Validação da `02` e colar4 saídas**

Em `ledger-service/app/`: `docker compose up --build -d` e coletar:

1. `docker image inspect` da imagem da API formatado (`size=` e `user=`) — imagem < 220 MB, usuário não-root.
2. `docker compose up`/`ps` mostrando o `database` `healthy` **antes** da API iniciar.
3. Registro inserido no banco → `docker compose restart` → mesmo registro consultado (persistência do volume).
4. `docker compose stop securepay_api` + logs (desligamento gracioso).

- [ ] **Step 5: Teardown**

`docker compose down` em `ledger-service/app/` (remover containers; volume/imagens podem ficar). Conferir `git status --porcelain` vazio além dos arquivos deste plano/tasks.

- [ ] **Step 6: Ruling de ambiente (só se aplicar)**

Comando indisponível → **não fabricar**: registrar em `## Limitações / notas` da Issue um bullet `**Ambiente indisponível:** <comando>` + o erro real. Sem esse marcador, o gate exige as saídas.

- [ ] **Step 7: Gate da Task 2**

Run:
```bash
set -euo pipefail
a=ledger-service/issues/01-linux-runtime.md
b=ledger-service/issues/02-docker-compose.md
for f in "$a" "$b"; do
  if grep -q 'Ambiente indisponível' "$f"; then
    grep -q '^## Limitações / notas' "$f" || exit 1
  else
    n=$(grep -c '^\$ ' "$f" || true)
    [ "$n" -ge 4 ] || { echo "SEM SAÍDAS COLETADAS: $f ($n < 4 linhas '\$ ')"; exit 1; }
  fi
done
if ! grep -q 'Ambiente indisponível' "$a"; then
  grep -q 'HTTP/1.1 200' "$a" || { echo "FALTA curl UP na 01"; exit 1; }
fi
if ! grep -q 'Ambiente indisponível' "$b"; then
  grep -q 'size=' "$b" || { echo "FALTA inspect na 02"; exit 1; }
fi
echo "Task 2 gate: OK"
```
Expected: `Task 2 gate: OK`

- [ ] **Step 8: Commit**

```bash
git add ledger-service/issues/01-linux-runtime.md ledger-service/issues/02-docker-compose.md
git commit -m "docs(tracker): evidências reais coladas nas Issues 01 e 02"
```

### Task 3: Topologia declarada do proxy (I3)

**Files:**
- Modify: `ledger-service/issues/04-caddy-reverse-proxy.md` — `## Escopo` (novo bullet) e `## Fora de escopo` (linha 32).

**Interfaces:**
- Consume: contradição `04:32` (proíbe mexer no compose) × `04:93/94` (exige mexer) e a topologia de três redes da `06:20` (rede pública existe por causa do proxy — ele é contêiner).
- Produz: decisão única de topologia: Caddy como serviço do mesmo Compose.

- [ ] **Step 1: Escopo**

Novo bullet após `- Headers de segurança padrão, compressão e logs estruturados` contendo obrigatoriamente: `Caddy como serviço do mesmo Compose` e `única fronteira que publica 80/443`.

- [ ] **Step 2: Fora de escopo**

Substituir `- Alteração do compose do backend — a API continua em \`ledger-service/app/docker-compose.yaml\`` por bullet contendo: `Reescrita da aplicação (código, Dockerfile, server.port)`, `quem mexe no \`docker-compose.yaml\` é esta Issue`, `adiciona o serviço do proxy` e `mantendo os serviços da Issue 02`.

- [ ] **Step 3: Gate da Task 3**

Run:
```bash
set -euo pipefail
f=ledger-service/issues/04-caddy-reverse-proxy.md
! grep -qF 'Alteração do compose do backend' "$f" \
  && grep -qF 'Caddy como serviço do mesmo Compose' "$f" \
  && grep -qF 'adiciona o serviço do proxy' "$f" \
  && echo "Task 3 gate: OK"
```
Expected: `Task 3 gate: OK`

- [ ] **Step 4: Commit**

```bash
git add ledger-service/issues/04-caddy-reverse-proxy.md
git commit -m "docs(tracker): topologia declarada — Caddy como serviço do Compose"
```

### Task 4: Fim do Redis fantasma na 06 (I4)

**Files:**
- Modify: `ledger-service/issues/06-compose-isolation.md` — `16`, `25`, `26`, `59`, `61`, `71`, `79`, `87` (remover `e Redis` das promessas presentes).

**Interfaces:**
- Consume: `06:102/103` (cláusula futura: só quando o Redis entrar, pela `webhook 12`, cai na rede isolada).
- Produz: Issue que promete apenas o que existe; o futuro do Redis fica só na cláusula.

⚠️ Não mexer nas linhas `101/102/103` (`101` é Menor diferido; `102/103` são a cláusula que permanece).

- [ ] **Step 1: Remover `e Redis` das 8 promessas**

Substituir **a linha inteira** de cada, usando esta tabela (coluna "De" = verbatim atual; prefixos `- [ ]`/`- ` e o restante da frase preservados):

| Linha | De (verbatim) | Para |
|---|---|---|
| `16` | `Estado final: três redes separando fronteira, aplicação e dados; banco e Redis inacessíveis de fora; limites e reservas de CPU/memória declarados; e a stack voltando sozinha após reboot.` | `Estado final: três redes separando fronteira, aplicação e dados; banco inacessível de fora; limites e reservas de CPU/memória declarados; e a stack voltando sozinha após reboot.` |
| `25` | `- Três redes: pública (proxy), interna (API) e isolada (banco e Redis)` | `- Três redes: pública (proxy), interna (API) e isolada (banco)` |
| `26` | `- Sem publicação de porta para banco e Redis` | `- Sem publicação de porta para o banco` |
| `59` | `- [ ] Separar as redes pública (proxy), interna (API) e isolada (banco e Redis)` | `- [ ] Separar as redes pública (proxy), interna (API) e isolada (banco)` |
| `61` | `- [ ] Blindar banco e Redis sem publicar porta para o host` | `- [ ] Blindar o banco sem publicar porta para o host` |
| `71` | `- [ ] Banco e Redis não aceitam conexão a partir de fora da rede isolada` | `- [ ] O banco não aceita conexão a partir de fora da rede isolada` |
| `79` | `- Tentativa de conexão com banco e Redis vinda de fora da rede isolada, esperada recusada` | `- Tentativa de conexão com o banco vinda de fora da rede isolada, esperada recusada` |
| `87` | `- Output do teste de inalcanhabilidade do banco e do Redis` | `- Output do teste de inalcanhabilidade do banco` |

- [ ] **Step 2: Gate da Task 4**

Run:
```bash
set -euo pipefail
f=ledger-service/issues/06-compose-isolation.md
! grep -q 'e Redis' "$f" \
  && grep -qF 'isolada (banco)' "$f" \
  && grep -qF 'banco inacessível de fora' "$f" \
  && grep -qF 'quando ele entrar' "$f" \
  && echo "Task 4 gate: OK"
```
Expected: `Task 4 gate: OK`

- [ ] **Step 3: Commit**

```bash
git add ledger-service/issues/06-compose-isolation.md
git commit -m "docs(tracker): Issue 06 promete só o banco; Redis futuro fica na cláusula da 12"
```

### Task 5: Coletor da 08 com dono de rede, rota e firewall (I6)

**Files:**
- Modify: `ledger-service/issues/08-trafego-sintetico-alertas.md` — `## Dependências` (novo item), `## Requisitos` (novo após `:72`), `## Critérios de aceitação` (novo após `:84`), `## Validação` (novo após `:94`).

**Interfaces:**
- Consume: `08:64` (série visível sem caminho declarado), invariante do firewall da `03`, topologia da `06`, porta única do `04`.
- Produz: caminho declarado — rede pela `06`, firewall intocado da `03`, exposição externa só pelo Caddy com autenticação.

- [ ] **Step 1: Dependência**

Novo item após `- Requer Issue 04 — …`: `- Requer Issue 06 — o coletor entra nas redes já declaradas da stack segmentada; nenhuma porta nova fora do firewall da Issue 03`.

- [ ] **Step 2: Requisito**

Novo `- [ ]` após o requisito do impacto da carga (`:72`) contendo: `Declarar a exposição do coletor`, `redes da Issue 06`, `nenhuma porta nova fora do firewall da Issue 03` e `somente pelo Caddy da Issue 04 com autenticação`.

- [ ] **Step 3: Critério**

Novo `- [ ]` após o critério da limpeza (`:84`) contendo: `redes declaradas da Issue 06`, `o firewall da Issue 03 não ganha regra nova` e `acesso externo ao painel, se existir, passa pelo proxy com autenticação`.

- [ ] **Step 4: Validação**

Novo item após `Conferir no banco os registros…` (`:94`): `- Conferir que a porta do coletor não responde fora do proxy e que o firewall da Issue 03 não mudou`.

- [ ] **Step 5: Gate da Task 5**

Run:
```bash
set -euo pipefail
f=ledger-service/issues/08-trafego-sintetico-alertas.md
grep -qF 'Requer Issue 06 — o coletor entra nas redes' "$f" \
  && grep -qF 'Declarar a exposição do coletor' "$f" \
  && grep -qF 'não ganha regra nova' "$f" \
  && grep -qF 'não responde fora do proxy' "$f" \
  && echo "Task 5 gate: OK"
```
Expected: `Task 5 gate: OK`

- [ ] **Step 6: Commit**

```bash
git add ledger-service/issues/08-trafego-sintetico-alertas.md
git commit -m "docs(tracker): coletor da 08 com dono de rede, rota e firewall"
```

### Task 6: Levar a stack ao servidor é requisito da 03 (I7)

**Files:**
- Modify: `ledger-service/issues/03-vps-hardening.md` — `## Escopo` (novo bullet), `## Requisitos` (novo após `:64`), `## Critérios de aceitação` (novo após `:71`), `## Validação` (novo após `:78`), `## Evidências` (novo após `:85`).

**Interfaces:**
- Consume: `03:90` (servidor é pré-requisito de ambiente) e o vazio entre `03` e `04` (ninguém instala runtime nem copia a stack).
- Produz: junta local→VPS fechada — servidor endurecido **e** apto a hospedar a stack.

- [ ] **Step 1: Escopo**

Novo bullet após `- Swap anti-OOM e auditoria de portas abertas`: `- Preparo do servidor para a stack: runtime de contêiner instalado, stack copiada e primeira subida saudável`.

- [ ] **Step 2: Requisito**

Novo `- [ ]` após `- [ ] Auditar portas abertas…` (`:64`) contendo: `Preparar o servidor para receber a stack`, `instalar o Docker`, `copiar a stack da Issue 02` e `healthcheck verde`.

- [ ] **Step 3: Critério**

Novo `- [ ]` após `- [ ] \`free -h\` mostra swap ativo…` (`:71`) contendo: `A stack copiada sobe no servidor preparado` e `/actuator/health responde 200 UP a partir dele`.

- [ ] **Step 4: Validação e Evidência**

- Validação (após `:78`): `- Subir a stack copiada no servidor e consultar \`/actuator/health\``.
- Evidências (após `:85`): `- Saída de \`docker compose ps\` com serviços \`healthy\` e do \`curl\` de \`/actuator/health\` no servidor`.

- [ ] **Step 5: Gate da Task 6**

Run:
```bash
set -euo pipefail
f=ledger-service/issues/03-vps-hardening.md
grep -qF 'Preparar o servidor para receber a stack' "$f" \
  && grep -qF 'instalar o Docker' "$f" \
  && grep -qF 'healthcheck verde' "$f" \
  && grep -qF 'docker compose ps' "$f" \
  && echo "Task 6 gate: OK"
```
Expected: `Task 6 gate: OK`

- [ ] **Step 6: Commit**

```bash
git add ledger-service/issues/03-vps-hardening.md
git commit -m "docs(tracker): levar a stack ao servidor é requisito da 03"
```

- [ ] **Step 7: Bateria final de validação**

Executar **depois** dos commits (árvore limpa). `B0` é o merge-base com `main` (ponto de branch). O escopo é a **união dos três apps + `docs/plano-correcoes-`**: os três planos da rodada compartilham branch, então nenhum range separa por app — os gates de task fixam os tokens de cada task no arquivo dela. Estilo cobre `*/issues/` (AGENTS/README têm tabela e blocos `bash`, que não são texto de Issue) e usa `awk` para ignorar conteúdo **dentro** de blocos cercados (as evidências da Task 2 são saída crua). O `[x]` de cada Issue é comparado contra `B0`. `BOARD.md` não muda nesta rodada. O `grep -cv` do escopo leva `|| true`: contagem zero sai com exit 1 e derrubaria o `set -e`.

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
diff=$(git diff "$B0" HEAD -- ledger-service/issues commerce-api/issues webhook-gateway/issues)
rmv=$(printf '%s\n' "$diff" | grep '^-' | grep -v '^---' | cut -c2- | cut -c1-20 | sort -u)
printf '%s\n' "$diff" | grep '^+' \
  | awk -v rem="$rmv" '/^\+\+\+/{next} /^\+ *\x60\x60\x60/{inb=!inb; next} inb{next} /^\+ *- /{next} /^\+$/{next} {k=substr($0,2,20); if (index(rem,k)>0) next; print}' \
  | grep . && { echo "ESTILO ERRADO"; exit 1; } || true
echo "VALIDAÇÃO FINAL: OK — 30 issues × 13 seções, status 2/6+9/1+12 preservados, escopo união dos 3 apps + docs, BOARD intocado, checkboxes B0, links e estilo (blocos cercados isentos) OK"
```
Expected: `VALIDAÇÃO FINAL: OK — …` e nenhuma linha de erro antes dela. Se falhar: corrigir, commitar o ajuste e reexecutar a bateria antes de considerar o plano concluído.

---

## Dependências entre planos

- O achado **I5** (divergência de rede `ledger 06` × `webhook 12`) é corrigido no plano `webhook-gateway` (Task 1 dele, texto do `12`) — este plano não mexe nela.
- As baterias cobrem os três apps; se este plano rodar sozinho, as tasks dos planos irmãos simplesmente ainda não apareceram no diff (nada a fazer).

## Diferidos (apêndice — sem task)

Escopo aprovado = 3 Críticos + 20 Importantes. Estes **Menores** da auditoria ficam registrados, não corrigidos:

- **`04:64`** — "compressão e logs estruturados" é Requisito sem Critério/Validação (critérios `66-72`, validação `76-80` não mencionam).
- **`04:93`** — "health check do **target group**" é termo de ELB/AWS; não existe target group numa VPS com Caddy (a Task 1 preserva a linha; só o script sai).
- **`05:86` × `:63/:88`** — LocalStack aceito como destino "off-site" sem declarar onde roda.
- **`06:101`** — parêntese "(coletor, dashboard, Redis)" contradiz `06:102` e a ordem da trilha.
- **`07:39`** — "a 12 é quem mexe no compose desta trilha" é falso no literal (`06:59/101` também mexem).
- **`07:12` e `:56`** — "não existe pipeline nenhuma deste app" convive com o stub `.github/workflows/CI.yml` (job `build` sem steps que sai verde).
- **`03:59`** — requisito de usuário operacional com sudo sem Critério observável.
- **`01:56-57`** — requisitos de contrato de ambiente/ordem de subida sem Critérios correspondentes.
- **`estudos/07:12-27`** — estudo cobre só SSH/blue-green; falta seção de gates (proteção de branch, secret scanning).
- **`02:89`** — invariante "manter `${PORT:-8080}:${PORT:-8080}`" fixa porta em todas as interfaces sem prazo (a correção só aparece implícita em `04:93`).
- **`AGENTS.md:55` × `app/docker-compose.yaml:6-7`** — defaults de env divergem (tabela `postgres` × compose `securepay`): alinhar a tabela é edição de documento, mas toca o contrato de env de produção — registrar aqui.
- **Entradas retroativas em `.agents/memory/progress.md` para `01`/`02`** — a regra de Memória vale no ato do fechamento; escrita retroativa por quem não viu o fechamento seria fabricação. Se quiser registro, grave via `/remember` com o que **você** aprendeu ao fechar.
- Não re-reportados da rodada anterior: `06:100`, `07:116` (aspectos de lógica, não o script), `BOARD:20` — severidade não maior.
