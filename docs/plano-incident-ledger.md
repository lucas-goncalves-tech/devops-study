# Incident Practice na trilha VPS — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fechar a lacuna de *incident practice* na trilha `ledger-service` — alerta da Issue 08 passa a apontar para um procedimento escrito, e uma Issue nova (`09`) exige procedimento executado às cegas, escalonamento declarado e exercitado, e um post-incident review sem culpa com ação, dono e prazo.

**Architecture:** Uma extensão pequena na Issue `08` (o vínculo alerta → runbook, 4 inserções, porque o alerta é dela) e uma Issue nova `09-incidente-runbook-postmortem.md` que carrega a prática: runbook por alerta, escalonamento, exercício cego e post-incident review. Depois, três housekeeping de documentação (BOARD, AGENTS raiz, `00-visao-geral.md`, AGENTS do app) porque a trilha passa a ter 9 Issues e um passo novo. Por fim, a bateria de validação do repo, com o censo atualizado (31 Issues; ledger 2 `done` / 7 `todo`).

**Tech Stack:** markdown (tracker), `grep`/`awk`/`bash` para os gates e a bateria. Nenhuma ferramenta de infraestrutura é instalada — o trabalho de execução (VPS, alerta, runbook, postmortem) é do dono da trilha e está na Task 5.

**Spec:** a justificativa de mercado está em [`docs/requisitos-mercado-devops.md`](requisitos-mercado-devops.md) §3 (divisor júnior↔pleno, item 3 — incidente é o bloqueio nº1: 8 de 27 threads) e §5 (lacuna C, priorizada 3ª por sinal÷esforço). O texto da Issue nova é o bloco exato da Task 2 e não pode ser reescrito pelo implementador.

## Global Constraints

- Toda Issue tem **exatamente 13 seções `## `**, nesta ordem: `Contexto`, `Objetivo`, `Dependências`, `Escopo`, `Fora de escopo`, `Conhecimentos envolvidos`, `Estado atual`, `Resultado esperado`, `Requisitos`, `Critérios de aceitação`, `Validação`, `Evidências`, `Limitações / notas`.
- Frontmatter da Issue nova: `aliases: [issue-09, incidente-runbook-postmortem]`, `tags: [tracker, issue, todo, study-needed]`, `status: todo`, `prioridade: alta`.
- PT-BR, RFC de problema corporativo: problema real → escopo → critérios observáveis por terceiro → validação → evidências. Sem tutorial, sem FAQ, sem navegação `Prev`/`Next`, sem sub-etapas `1A`/`2B`. Material de estudo vive em `ledger-service/estudos/`, fora da Issue.
- **Custo zero por regra**: nenhum recurso pago. Canal de paging pago, cluster e conta paga estão fora de escopo.
- **Nenhum `Requer` para Issue de outro app.** As Issues `commerce 08` e `webhook 11` (que já fazem RCA escrito) podem ser lidas, nunca exigidas.
- Não alterar `status:` de Issue existente, não virar nenhum `[x]` existente, não reescrever evidência das Issues `01`/`02` (`done`).
- Checkbox do `BOARD.md` espelha o `status:` da Issue.
- Escopo gravável: `ledger-service/`, `BOARD.md`, `AGENTS.md`, `00-visao-geral.md`, `docs/plano-*`. Código de app, IaC, workflows e `scripts/` são somente leitura.
- Um commit por task. Cada gate é `set -euo pipefail` e imprime `Task N gate: OK` só quando tudo passa; se falhar, exit ≠ 0 e nada é commitado.
- Estilo: linhas novas são bullets (`- `) ou prosa **dentro** de seção de Issue; nada de parágrafo solto fora de bullet, exceto texto modificado cujo prefixo de 20 caracteres exista entre as linhas removidas do diff.
- **Arquivo novo tem regra própria** (descoberta na execução): em Issue recém-criada, prosa é permitida apenas em `## Contexto` e `## Objetivo`; frontmatter, título e headings são isentos. O check de estilo por prefixo de 20 caracteres só vale para Issue **modificada** — num arquivo novo ele flagraria título, headings e frontmatter, que são obrigatórios.

## Review Focus

Cinco modos de falha que a spec implica e que nenhum teste desta bateria exercita — o implementador deve tratar cada um:

1. **Alerta apontando para runbook inexistente** — o alerta chega sem próximo passo e a Issue `08` parece cumprida. Comportamento esperado: o caminho declarado existe no repositório. Teste: Task 1, gateconfere que o arquivo de runbook é referenciado por nome, e a Task 5 confere que o arquivo existe antes de dizer que a `08` está cumprida.
2. **Runbook que só funciona para quem já sabe a causa** — "rode o comando que eu rodei" é o modo de falha mais caro: passa a inspeção visual e quebra às 3h. Comportamento esperado: executável por alguém sem contexto. Teste: Task 5, exercício cego com executor que não viu a indução da falha.
3. **Post-incident review terminando em "erro humano"** — gerapalma e nenhuma ação. Comportamento esperado: causa raiz mecânica + ações com dono e data. Teste: Task 2, gate exige os dois literalmente; Task 5, na hora de escrever o review.
4. **Escalonamento só no canal primário** — canal caído = ninguém avisado, que é o pior dia possível. Comportamento esperado: secundário chega com primário indisponível. Teste: Task 5, com primário fora e horário registrado.
5. **Tratamento que resolve e corrompe** — devolver o serviço com transação duplicada ou perdida é pior que a queda. Comportamento esperado: sem 5xx nem falha de transação no log da janela. Teste: Task 5, log da Issue `07` + consulta ao banco.

---

### Task 1: Alerta da Issue 08 passa a apontar para o procedimento

**Files:**
- Modify: `ledger-service/issues/08-trafego-sintetico-alertas.md` — 1 requisito (após o requisito de canal), 1 critério (após o critério de PromQL/`for`), 1 validação (após a reversão da degradação), 1 evidência (após a evidência da regra em arquivo)

**Interfaces:**
- Consumes: o requisito `- [ ] Ligar o alerta a um canal observável e capturar a evidência do disparo (log do canal ou captura de tela)` e o critério `- [ ] A regra de alerta tem expressão de PromQL, condição e \`for\` visíveis na configuração, não apenas descrita em texto` (textos exatos abaixo); a Issue `09` criada na Task 2.
- Produces: o identificador `Issue 09` como dono do procedimento de incidente, citado dentro da `08` — a Task 3 usa essa referência para a linha do BOARD.

- [ ] **Step 1: escrever o gate da task**

```bash
# Os scripts de gate ficam FORA do repositório: `.superpowers/` não é ignorado na raiz
echo "main" && git status --porcelain | wc -l   # tem de dar 1 (só este plano, ainda não commitado)
WS="${TMPDIR:-/tmp}/incident-plano"
mkdir -p "$WS"
cat > "$WS/task-1-gate.sh" <<'EOF'
set -euo pipefail
f=ledger-service/issues/08-trafego-sintetico-alertas.md
grep -qF 'runbook' "$f" \
  && grep -qF 'anotação' "$f" \
  && grep -qF 'Issue 09' "$f" \
  && ! grep -qF 'alerta que dispara sem procedimento' "$f" \
  && echo "Task 1 gate: OK"
EOF
bash "$WS/task-1-gate.sh"
```

Esperado nesta altura: **vermelho**, exit ≠ 0 (a Issue `08` ainda não fala de runbook).

- [ ] **Step 2: inserção do requisito**

Âncora exata (substituir por `âncora + "\n" + novo`):

```
- [ ] Ligar o alerta a um canal observável e capturar a evidência do disparo (log do canal ou captura de tela)
```

Novo requisito, imediatamente depois:

```
- [ ] Cada regra de alerta declara o procedimento que o operador segue ao receber o disparo — o caminho do runbook e o primeiro comando a rodar — e a notificação entrega esse caminho junto da mensagem
```

- [ ] **Step 3: inserção do critério**

Âncora exata:

```
- [ ] A regra de alerta tem expressão de PromQL, condição e `for` visíveis na configuração, não apenas descrita em texto
```

Novo critério:

```
- [ ] Cada regra de alerta aponta, por anotação na configuração, para um arquivo de runbook que existe no repositório, e a regra não é considerada cumprida enquanto esse arquivo não existir
```

- [ ] **Step 4: inserção da validação**

Âncora exata:

```
- Reverter a degradação e confirmar que o alerta volta ao estado normal
```

Nova validação:

```
- Abrir o alerta recebido sem saber por que disparou e conferir que o caminho do runbook chega na própria notificação
```

- [ ] **Step 5: inserção da evidência**

Âncora exata:

```
- Regra de alerta em arquivo, com PromQL e `for`
```

Nova evidência:

```
- Trecho da configuração da regra mostrando a anotação do runbook e o arquivo referenciado
```

- [ ] **Step 6: rodar o gate e commitar**

```bash
bash "$WS/task-1-gate.sh"
git add ledger-service/issues/08-trafego-sintetico-alertas.md
git commit -m "docs(tracker): alerta da 08 passa a entregar o procedimento que o operador segue"
```

Esperado: `Task 1 gate: OK` antes do commit; commit só com `08` no stage.

---

### Task 2: Issue nova 09 — incidente com procedimento, escalonamento e análise sem culpa

**Files:**
- Create: `ledger-service/issues/09-incidente-runbook-postmortem.md`

**Interfaces:**
- Consumes: o precedente de runbook de uma página da Issue `05` (requisito `Runbook de emergência de uma página, executável`, critério `O runbook cabe em 1 página e foi executado seguindo apenas do que está escrito`); a regra de alerta com anotação de runbook da Task 1; o healthcheck L4/L7 da Issue `01`; a execução de tráfego da Issue `08`.
- Produces: o arquivo que o BOARD (Task 3) e o AGENTS do app (Task 3) referenciam; a lista de comandos que a Task 5 executa.

- [ ] **Step 1: escrever o gate da task**

```bash
cat > "$WS/task-2-gate.sh" <<'EOF'
set -euo pipefail
f=ledger-service/issues/09-incidente-runbook-postmortem.md
[ -f "$f" ]
[ "$(grep -c '^## ' "$f")" -eq 13 ] || { echo "SEÇÕES: $(grep -c '^## ' "$f") (esperado 13)"; exit 1; }
grep -qF 'status: todo' "$f"
grep -qF 'aliases: [issue-09, incidente-runbook-postmortem]' "$f"
grep -qF 'Requer Issue 08' "$f"
grep -qF 'Requer Issue 05' "$f"
grep -qF 'Requer Issue 07' "$f"
grep -qF 'exercício cego' "$f"
grep -qF 'canal secundário' "$f"
grep -qF 'sem culpa' "$f"
grep -qF 'X-Idempotency-Key\|idempotente\|duplicada' "$f"
grep -c '^- \[ \] ' "$f" | grep -qv '^0$'
! grep -rqE '\]\(\.\./\.\./(commerce-api|webhook-gateway)/' "$f"
! grep -qF 'Ambiente indisponível' "$f"
echo "Task 2 gate: OK"
EOF
bash "$WS/task-2-gate.sh"
```

Esperado nesta altura: **vermelho**, exit ≠ 0 (o arquivo não existe).

- [ ] **Step 2: criar o arquivo com o conteúdo abaixo, sem alterar uma linha**

````markdown
---
aliases: [issue-09, incidente-runbook-postmortem]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 09 — Incidente tem procedimento executável, escalonamento declarado e análise sem culpa

## Contexto

A Issue 08 entrega um alerta que dispara para um canal e a Issue 05 entrega um procedimento escrito para restaurar backup. O que não existe entre os dois: o que a pessoa faz nos primeiros minutos depois de receber a notificação, para quem ela fala quando ninguém responde, e o que fica escrito depois que o serviço volta. Alerta sem procedimento é ruído que treina a ignorar; procedimento sem análise repete a mesma causa no mês seguinte. Incidente de sistema não se resolve na memória de quem estava de plantão às três da manhã — e hoje a stack da trilha VPS é exatamente isso: uma VPS, um banco, um proxy e uma pessoa.

## Objetivo

Estado final: um incidente real ou induzido nesta stack, detectado pelo alerta da Issue 08, tratado por outra pessoa seguindo apenas o que está escrito, com escalonamento declarado e exercitado, e encerrado em um post-incident review sem culpa versionado com causa raiz mecânica e ações com dono e prazo — sem que o tratamento deixe o serviço degradado nem dado de transação duplicado ou perdido.

## Dependências

- Requer Issue 08 — o incidente é detectado pelo alerta desta trilha, e cada regra de alerta precisa declarar o runbook que o operador segue
- Requer Issue 05 — o runbook de restauração de backup já é o precedente do formato (uma página, executável) que esta Issue estende para indisponibilidade do serviço
- Requer Issue 07 — a stack implantada com versão auditável é o que se estraga; sem ela não há incidente de produção para tratar

## Escopo

- Runbook de uma página por regra de alerta da Issue 08, em `ledger-service/runbooks/<alerta>.md`, referenciado pela regra e entregue na própria notificação
- Conteúdo do runbook: como confirmar o sintoma, como chegar ao log, o que restaura o serviço e como confirmar que voltou
- Escalonamento declarado em arquivo: quem é notificado, em que ordem, por qual canal, e em quanto tempo sem resposta o canal secundário é acionado
- Exercício cego: pessoa sem contexto da causa executa o runbook e devolve o serviço ao estado saudável
- Post-incident review sem culpa versionado, com linha do tempo em UTC, impacto observado, causa raiz mecânica e ações com dono e prazo
- Verificação de retorno ao saudável depois do tratamento, incluindo o tráfego sintético da Issue 08
- Registro do incidente e do review em arquivo versionado no repositório

## Fora de escopo

- Ferramenta paga de paging ou de incidente (PagerDuty, Opsgenie, Statuspage): custo zero por regra — o escalonamento usa o canal da Issue 08 mais um canal secundário gratuito
- Equipe e rodízio real de plantão: a stack tem um dono; o que se prova é o procedimento e a chegada no canal secundário, não escala humana
- Comunicação a clientes, status page e SLA externo com contrato
- Chaos engineering como método: a degradação induzida já é da Issue 08; aqui o incidente é o que se documenta e se analisa
- Runbook de restauração de backup — esse procedimento é da Issue 05 e não é reescrito aqui
- Relatório da execução de tráfego sintético — é da Issue 08

## Conhecimentos envolvidos

- Post-incident review sem culpa: linha do tempo em UTC, impacto observado e causa raiz que termina em mecanismo, não em pessoa
- Runbook operacional: uma página, um comando por linha, sem depender de contexto que só quem sofre tem
- Escalonamento: canal primário e secundário, tempo de espera declarado e o critério de quando escalar
- Operação de serviço systemd na Debian: `systemctl status`, `journalctl -u`, unidade do serviço e log do container
- Sinais de saúde da própria stack: `/actuator/health`, healthcheck L4/L7 da Issue 01 e `docker compose ps`

## Estado atual

- O alerta da Issue 08 aponta para um canal, mas nenhuma regra declara o procedimento a seguir: quem recebe "p95 alto" não tem o próximo passo escrito
- O único procedimento existente é o de restauração de backup da Issue 05, que não cobre indisponibilidade do serviço nem degradação de latência
- Não existe canal secundário: se o canal primário estiver indisponível, o alerta chega a ninguém e o silêncio é indistinguível de "tudo bem"
- Nenhum incidente desta trilha foi registrado por escrito; o histórico de falha é o da memória de quem estava no console
- Não há escalonamento declarado: não existe a quem falar quando o primeiro notificado não responde

## Resultado esperado

- Um alerta real da Issue 08 tratado por pessoa que não sabia a causa, seguindo somente o runbook
- Escalonamento exercitado uma vez: a notificação chega ao canal secundário com o primário indisponível, com horário registrado
- Um post-incident review versionado, sem atribuição de culpa, com causa raiz mecânica e ações com dono e data
- Serviço de volta ao estado saudável depois do tratamento, com tráfego sintético dentro dos thresholds e sem transação duplicada ou perdida

## Requisitos

- [ ] Runbook de uma página por regra de alerta da Issue 08, em `ledger-service/runbooks/<alerta>.md`, referenciado pela regra e começando pelo primeiro comando a executar
- [ ] Conteúdo do runbook cobrindo: confirmar o sintoma, chegar ao log relevante, restaurar o serviço e confirmar que voltou
- [ ] Escalonamento declarado em arquivo: quem é notificado, em qual ordem, por qual canal, e em quanto tempo sem resposta o canal secundário é acionado
- [ ] Canal secundário declarado e exercitado pelo menos uma vez com o canal primário indisponível
- [ ] Exercício cego executado por pessoa sem contexto da causa, com o runbook como única fonte de informação
- [ ] Post-incident review sem culpa versionado, com linha do tempo em UTC, impacto observado e causa raiz que não termina em erro humano
- [ ] Cada ação do review com dono, prazo e o Issue que vai conferir a ação
- [ ] Verificação de retorno ao saudável depois do tratamento: `/actuator/health`, healthcheck L4/L7 da Issue 01 e execução do tráfego da Issue 08 sem reprovar threshold

## Critérios de aceitação

- [ ] O exercício cego devolveu o serviço ao estado saudável sem nenhum passo improvisado fora do runbook — o que improvisou, se houve, está anotado e entrou no post-incident review
- [ ] Cada regra de alerta da Issue 08 referencia, por anotação na configuração, um arquivo de runbook que existe em `ledger-service/runbooks/`
- [ ] A notificação de escalonamento chegou ao canal secundário com o primário indisponível, com data e hora registradas
- [ ] O post-incident review está versionado no repositório, tem linha do tempo em UTC e nenhuma frase atribuindo a falha a uma pessoa
- [ ] A causa raiz registrada descreve o mecanismo que falhou, e não a ação de alguém
- [ ] As ações do review têm dono e data, e o Issue responsável por conferir cada uma está nomeado
- [ ] Depois do tratamento, `/actuator/health` responde `UP`, o healthcheck L4/L7 da Issue 01 sai com 0 e o tráfego da Issue 08 fica dentro dos thresholds
- [ ] Nenhuma transação duplicada ou perdida: o log da Issue 07 não mostra erro 5xx nem falha de transação na janela do incidente

## Validação

- Abrir o runbook de uma regra sem saber por que o alerta disparou e seguir a sequência até o serviço voltar, sem consultar histórico da falha
- Conferir na configuração da regra que a anotação do runbook existe e que o arquivo referenciado está no caminho declarado
- Deixar o canal primário indisponível, disparar um alerta de teste e confirmar a chegada no canal secundário com horário
- Inspecionar o log do serviço no intervalo do incidente e confrontar a linha do tempo do review com o horário do alerta
- Consultar `/actuator/health` e rodar o healthcheck L4/L7 da Issue 01 depois do tratamento
- Executar o tráfego sintético da Issue 08 logo após o tratamento e conferir o p95 dentro do threshold
- Conferir no banco que a transação do incidente não ficou duplicada nem perdida

## Evidências

- Arquivo de runbook de uma regra, com a marcação de que foi executado por alguém sem contexto da causa
- Notificação recebida no canal secundário com data e hora, junto do registro do canal primário indisponível
- Arquivo do post-incident review, com a linha do tempo em UTC e as ações com dono e data
- Saída de `/actuator/health`, do healthcheck L4/L7 e do tráfego da Issue 08 depois do tratamento
- Trecho do log do serviço no intervalo do incidente, sem erro 5xx e sem falha de transação

## Limitações / notas

- **Não há equipe, há dono.** O plantão nesta stack é a pessoa responsável e um contato secundário declarado; o que esta Issue prova é procedimento e chegada no canal secundário, não escala humana
- **O incidente pode ser induzido** com a mesma técnica da Issue 08 (limiar artificialmente baixo ou latência adicionada no caminho) ou ser real. Induzido é mais seguro e reprodutível; real é mais honesto sobre o impacto — registre qual dos dois foi usado
- **Canal pago não entra** no desenho: o escalonamento tem de funcionar com o canal da Issue 08 e um secundário gratuito
- **Sem culpa não é sem responsabilidade**: o review descreve o mecanismo da falha e as ações decorrentes; ele não substitui a decisão de não repetir a causa
- Se o exercício cego falhar porque o runbook não bastou, esse é o resultado mais valioso desta Issue — registre a falha em vez de refazer o exercício até passar
````

- [ ] **Step 3: rodar o gate e commitar**

```bash
bash "$WS/task-2-gate.sh"
git add ledger-service/issues/09-incidente-runbook-postmortem.md
git commit -m "docs(tracker): Issue 09 — incidente com procedimento, escalonamento e análise sem culpa"
```

Esperado: `Task 2 gate: OK` antes do commit; o commit contém **um** arquivo novo.

---

### Task 3: Housekeeping — a trilha passa a ter 9 Issues e um passo novo

**Files:**
- Modify: `BOARD.md` — sequência da trilha, texto de "Estado final da trilha", e a linha nova `09` depois da `08`
- Modify: `AGENTS.md` — contagem `8 Issues` → `9 Issues` e a sequência da trilha VPS
- Modify: `00-visao-geral.md` — sequência da trilha VPS na tabela de apps
- Modify: `ledger-service/AGENTS.md` — sequência da linha 4, o texto de "Estado final da trilha", e a linha `09` na tabela de Issues

**Interfaces:**
- Consumes: o arquivo criado na Task 2 (caminho e título exatos: `09-incidente-runbook-postmortem.md`, "Incidente com procedimento, escalonamento e análise sem culpa").
- Produces: os quatro pontos de navegação que uma sessão nova lê antes de escolher a próxima Issue.

- [ ] **Step 1: escrever o gate da task**

Duas armadilhas já corrigidas aqui, ambas da execução real: a numeração de Issue **reinicia por app**, então contar `^- \[ \] \[09 ` no board dá 3 (ledger, commerce e webhook) e não 1 — o gate escopa pelo título; e as linhas do board são item de lista com link, não linha de tabela, então o checkbox se extrai por regex.

```bash
cat > "$WS/task-3-gate.sh" <<'EOF'
set -euo pipefail
b=BOARD.md
a=AGENTS.md
g=00-visao-geral.md
l=ledger-service/AGENTS.md
f=ledger-service/issues/09-incidente-runbook-postmortem.md
grep -qF '09-incidente-runbook-postmortem.md' "$b"
grep -qF '09-incidente-runbook-postmortem.md' "$l"
grep -qF '| `ledger-service` · 9 Issues |' "$a"
! grep -qF '| `ledger-service` · 8 Issues |' "$a"
for x in "$b" "$a" "$g" "$l"; do
  [ "$(grep -c '→ incidente' "$x")" -ge 1 ] || { echo "SEM PASSO DE INCIDENTE: $x"; exit 1; }
done
# a numeração reinicia por app: existe exatamente uma Issue 09 em cada app que tem 09
[ "$(grep -cE '^- \[[ x]\] \[09 ' "$b")" -eq 3 ] || { echo "CONTAGEM DE LINHAS 09 NO BOARD"; exit 1; }
line=$(grep -F '[09 Incidente e postmortem]' "$b")
if ! printf '%s' "$line" | grep -qE '^- \[ \] \[09 '; then
  echo "CHECKBOX DO BOARD ERRADO: $line"; exit 1
fi
grep -F '[09](issues/09-incidente-runbook-postmortem.md)' "$l" | grep -qF '| `todo` |' || { echo "LINHA DA TABELA DO APP ERRADA"; exit 1; }
[ "$(grep -m1 '^status:' "$f")" = "status: todo" ] || { echo "STATUS DA 09"; exit 1; }
grep -c 'Próxima a entrar: `03`' "$l" | grep -qx 1 || { echo "PRÓXIMA ISSUE MUDOU"; exit 1; }
echo "Task 3 gate: OK"
EOF
bash "$WS/task-3-gate.sh"
```

Esperado nesta altura: **vermelho**, exit ≠ 0.

- [ ] **Step 2: `BOARD.md` — sequência e estado final**

Substituir, em `BOARD.md`:

| Antes | Depois |
|---|---|
| ``> Java/Spring: `linux → hardening → caddy → backups → isolamento → deploy`.`` | ``> Java/Spring: `linux → hardening → caddy → backups → isolamento → deploy → incidente`.`` |

E acrescentar ao fim da linha de **Estado final da trilha** do `ledger` (que hoje termina em `... tráfego sintético com alerta real.`), o trecho:

```
, e incidente com procedimento executável, escalonamento e análise sem culpa
```

O resultado é uma frase, não duas: `...com tráfego sintético com alerta real, e incidente com procedimento executável, escalonamento e análise sem culpa.`

- [ ] **Step 3: `BOARD.md` — linha nova da Issue 09**

Imediatamente depois da linha da Issue `08` (que termina em `...com coleta e alerta próprios disparando`), inserir:

```
- [ ] [09 Incidente e postmortem](ledger-service/issues/09-incidente-runbook-postmortem.md) — alerta entrega o procedimento a seguir, escalonamento declarado e exercitado, e todo incidente vira review sem culpa com causa raiz e ação com dono e prazo
```

O checkbox é `[ ]` porque a Issue nasce `todo` (política de status do `00-visao-geral.md`).

- [ ] **Step 4: `AGENTS.md` raiz — contagem e sequência**

Na tabela de apps, a linha do `ledger-service`: `8 Issues` → `9 Issues`, e a trilha `VPS — \`linux → hardening → caddy → backups → isolamento → deploy\`` → `VPS — \`linux → hardening → caddy → backups → isolamento → deploy → incidente\``.

- [ ] **Step 5: `00-visao-geral.md` — sequência**

Na tabela "As 3 trilhas", a linha do `ledger-service`: acrescentar ` → incidente` no fim da sequência da trilha VPS. **Não** tocar na coluna de estágio (`\`01\`, \`02\` \`done\`; \`03\` é a próxima a entrar`) — a próxima Issue da trilha continua sendo a `03`.

- [ ] **Step 6: `ledger-service/AGENTS.md` — sequência, estado final e tabela**

Três edições no arquivo:

1. Linha 4: `\`linux → hardening → caddy → backups → isolamento → deploy\`. Java 21, Postgres, Redis.` → acrescentar ` → incidente` antes do ponto final da sequência.
2. O parágrafo "Estado final da trilha:" — acrescentar, no fim, `... e incidente com procedimento executável, escalonamento e análise sem culpa.` (mesma frase do Step 2, para que app e board contem a mesma promessa).
3. Na tabela de Issues, depois da linha do `08`, inserir:

```
| [09](issues/09-incidente-runbook-postmortem.md) | Incidente com runbook por alerta, escalonamento exercitado e post-incident review sem culpa | `todo` |
```

**Não** tocar em `**Próxima a entrar: \`03\`.**`.

- [ ] **Step 7: rodar o gate e commitar**

```bash
bash "$WS/task-3-gate.sh"
git add BOARD.md AGENTS.md 00-visao-geral.md ledger-service/AGENTS.md
git commit -m "docs: trilha VPS ganha o passo de incidente e a nona Issue"
```

Esperado: `Task 3 gate: OK` antes do commit; nenhum arquivo de Issue no stage.

---

### Task 4: Bateria do repo com o censo novo

**Files:**
- Verify only (nenhum arquivo editado)

**Interfaces:**
- Consumes: as Tasks 1–3.
- Produces: a prova de que a trilha continua íntegra — 31 Issues × 13 seções na ordem canônica, `status:` e checkbox inalterados contra a base, as 31 linhas do BOARD espelhando status, links e estilo intactos.

- [ ] **Step 1: fixar a base do diff**

Base é o commit anterior à Task 1. Nesta execução: `bc09786`.

```bash
B0=bc09786
git cat-file -e "$B0" && echo "base $B0 ok"
```

- [ ] **Step 2: rodar a bateria**

Duas armadilhas já corrigidas nesta versão, ambas encontradas rodando a primeira: (a) com `pipefail`, o `grep -v '^---'` de um diff só de adição devolve vazio e mata o script em silêncio — precisa de `|| true`; (b) as linhas do BOARD são item de lista com link, não linha de tabela, então o check de sincronismo extrai o checkbox e o caminho por regex, não por `IFS='|'`.

```bash
set -euo pipefail
B0=bc09786
[ -z "$(git status --porcelain --untracked-files=no)" ] || { echo "ÁRVORE SUJA"; exit 1; }
ORDER='Contexto|Objetivo|Dependências|Escopo|Fora de escopo|Conhecimentos envolvidos|Estado atual|Resultado esperado|Requisitos|Critérios de aceitação|Validação|Evidências|Limitações / notas'
n=0
for f in ledger-service/issues/*.md commerce-api/issues/*.md webhook-gateway/issues/*.md; do
  [ "$(grep -c '^## ' "$f")" -eq 13 ] || { echo "SEÇÃO ERRADA: $f"; exit 1; }
  [ "$(grep '^## ' "$f" | sed 's/^## //' | paste -sd'|')" = "$ORDER" ] || { echo "ORDEM ERRADA: $f"; exit 1; }
  n=$((n+1))
done
[ "$n" -eq 31 ] || { echo "CONTAGEM: $n (esperado 31)"; exit 1; }
[ "$(grep -h '^status:' ledger-service/issues/*.md | grep -c '^status: done$')" -eq 2 ] || { echo "STATUS ledger done"; exit 1; }
[ "$(grep -h '^status:' ledger-service/issues/*.md | grep -c '^status: todo$')" -eq 7 ] || { echo "STATUS ledger todo"; exit 1; }
[ "$(grep -h '^status:' commerce-api/issues/*.md | grep -c '^status: todo$')" -eq 9 ] || { echo "STATUS commerce todo"; exit 1; }
[ "$(grep -h '^status:' commerce-api/issues/*.md | grep -c '^status: parked$')" -eq 1 ] || { echo "STATUS commerce parked"; exit 1; }
[ "$(grep -h '^status:' webhook-gateway/issues/*.md | grep -c '^status: todo$')" -eq 12 ] || { echo "STATUS webhook"; exit 1; }
for f in ledger-service/issues/*.md commerce-api/issues/*.md webhook-gateway/issues/*.md; do
  if git cat-file -e "$B0:$f" 2>/dev/null; then
    [ "$(git show "$B0:$f" | grep -c '^- \[x\]' || true)" -eq "$(grep -c '^- \[x\]' "$f" || true)" ] || { echo "CHECKBOX MUDOU: $f"; exit 1; }
    [ "$(git show "$B0:$f" | grep -m1 '^status:')" = "$(grep -m1 '^status:' "$f")" ] || { echo "STATUS MUDOU: $f"; exit 1; }
  else
    [ "$(grep -c '^- \[x\]' "$f" || true)" -eq 0 ] || { echo "ISSUE NOVA COM [x]: $f"; exit 1; }
  fi
done
board=0
while IFS= read -r line; do
  board=$((board+1))
  cb=$(printf '%s' "$line" | grep -oE '^- \[[ x]\]' | sed 's/^- \[//; s/\]$//')
  path=$(printf '%s' "$line" | grep -oE '\]\([^)]*\.md\)' | head -1 | sed 's/^](//; s/)$//')
  [ -f "$path" ] || { echo "LINK BOARD QUEBRADO: $path"; exit 1; }
  st=$(grep -m1 '^status:' "$path")
  if [ "$cb" = "x" ]; then
    [ "$st" = "status: done" ] || { echo "BOARD [x] mas $st: $path"; exit 1; }
  else
    [ "$st" != "status: done" ] || { echo "BOARD [ ] mas $st: $path"; exit 1; }
  fi
done < <(grep -E '^- \[[ x]\] \[[0-9]{2} ' BOARD.md)
[ "$board" -eq 31 ] || { echo "LINHAS NO BOARD: $board (esperado 31)"; exit 1; }
for f in ledger-service/issues/*.md commerce-api/issues/*.md webhook-gateway/issues/*.md; do
  d=$(dirname "$f")
  while IFS= read -r t; do
    t=${t%%#*}; [ -z "$t" ] && continue; case "$t" in http*) continue;; esac
    [ -f "$d/$t" ] || { echo "LINK QUEBRADO: $f -> $t"; exit 1; }
  done < <(grep -oE '\]\([^)]*\.md\)' "$f" | sed 's/^](//; s/)$//')
done
for f in $(git diff --name-only --diff-filter=A "$B0" HEAD -- '*/issues/*.md'); do
  awk '/^## /{sec=substr($0,4); next} /^- /{next} /^$/{next} /^---$/{next} /^(aliases|tags|status|prioridade|issue):/{next} /^# /{next} {if (sec!="Contexto" && sec!="Objetivo") {print FILENAME": prosa fora de Contexto/Objetivo na linha "NR; exit 1}}' "$f"
done
rmv=$(git diff "$B0" HEAD -- ledger-service/issues commerce-api/issues webhook-gateway/issues | grep '^-' | grep -v '^---' | cut -c2- | cut -c1-20 | sort -u || true)
for f in $(git diff --name-only --diff-filter=M "$B0" HEAD -- '*/issues/*.md'); do
  out=$(git diff "$B0" HEAD -- "$f" | grep '^+' | awk -v rem="$rmv" '/^\+\+\+/{next} /^\+ *\x60\x60\x60/{inb=!inb; next} inb{next} /^\+ *- /{next} /^\+$/{next} {k=substr($0,2,20); if (index(rem,k)>0) next; print}' || true)
  [ -z "$out" ] || { echo "ESTILO (prosa nova fora de bullet): $f"; printf '%s\n' "$out" | head -3; exit 1; }
done
echo "VALIDAÇÃO FINAL: OK — 31 Issues × 13 seções em ordem canônica, census 2/7 + 9/1 + 12, status e checkboxes intactos vs $B0, 31 linhas no BOARD espelhando status, links OK, estilo OK (arquivo novo: prosa só em Contexto/Objetivo)"
```

- [ ] **Step 3: se algo falhar, corrigir no arquivo dono e reexecutar a bateria inteira**

Nenhum commit de fix nesta task: o commit do ajuste é da task cujo arquivo falhou, e a bateria roda de novo depois.

---

### Task 5: Exercício operacional — dono da trilha (não é tarefa de agente)

Esta task não é executada por agente: exige derrubar e recuperar a stack de produção da trilha VPS. O plano entrega a sequência; a execução e as evidências são de quem opera.

**Files:**
- Modify quando executada: `ledger-service/issues/09-incidente-runbook-postmortem.md` — `## Evidências` recebe a saída real; `## Limitações / notas` recebe o registro de induzido×real

**Interfaces:**
- Consumes: a Issue `09` (Task 2), a regra de alerta com anotação de runbook (`08`, Task 1), o healthcheck L4/L7 da `01`, o tráfego da `08`.
- Produces: as cinco evidências que a Issue `09` lista.

- [ ] **Step 1: escrever o runbook da primeira regra de alerta, com uma página**

Uma regra = um arquivo. Estrutura mínima, nesta ordem: `1) como confirmar o sintoma` · `2) como chegar no log` · `3) o que restaura` · `4) como confirmar que voltou`. Cada linha é um comando copiável. Se a página estourar, o runbook está errado — corte o que não é executável às 3h.

- [ ] **Step 2: declarar o escalonamento em arquivo**

Quem é notificado, em que ordem, por qual canal, e em quanto tempo sem resposta o secundário entra. O secundário tem de ser gratuito eDeclared antes do exercício.

- [ ] **Step 3: exercício cego**

Induza a degradação pela mesma técnica da `08` (limiar artificialmente baixo ou latência adicionada no caminho), **não conte a causa a quem executa**, e entregue só o runbook. Registre o horário do disparo em UTC. Se o executor improvisar algo fora do runbook, isso vai para o review — não corrija o runbook antes de escrever o review, senão a prova perde o objeto.

- [ ] **Step 4: exercitar o canal secundário com o primário indisponível**

Deixe o primário fora, dispare alerta de teste, registre data e hora da chegada no secundário.

- [ ] **Step 5: voltar ao saudável e provar**

`/actuator/health` em `UP` · healthcheck L4/L7 da `01` com exit 0 · tráfego da `08` dentro do threshold · nenhum 5xx nem falha de transação na janela · transação do incidente não duplicada nem perdida no banco.

- [ ] **Step 6: escrever o post-incident review**

Arquivo versionado, com: linha do tempo em UTC (do alerta ao retorno ao verde), impacto observado, causa raiz **mecânica** (para no mecanismo, não em "erro humano"), o que atrasou a detecção, e ações com dono, data e o Issue que confere cada uma. Sem atribuir a falha a uma pessoa — isso é critério de aceitação, não sugestão.

- [ ] **Step 7: colar as evidências e fechar**

Preencher `## Evidências` com saída real (blocos com comando executado e saída), marcar `status:` como `done` **só** depois que os cinco itens da lista de evidências existirem, atualizar `prioridade` se quiser, e virar o checkbox do `BOARD.md` junto.

---

## Fora deste plano

- **Gate de segurança de IaC** (`checkov`/`tflint`) — estender `commerce 04`
- **SBOM/provenance** — estender `webhook 06`/`08` e reescrever o limite de `06:36`
- **Trace do caminho do request** — material de `estudos/`
- **Ferramenta real versionada em TypeScript** — `commerce-api/scripts/` como pacote com testes
- **Trilha 4 cloud-native** — `archive/18-kubernetes-helm` como semente; não agora

Os cinco foram priorizados em [`docs/requisitos-mercado-devops.md`](requisitos-mercado-devops.md) §5 e ficam para planos próprios.