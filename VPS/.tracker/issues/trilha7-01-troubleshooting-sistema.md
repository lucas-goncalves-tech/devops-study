---
aliases: [trilha7-01, troubleshooting-sistema]
tags: [tracker, issue, todo]
status: todo
prioridade: media
---

# Issue 01 — diagnóstico de sistema: CPU, memória, disco e processo sob comando

## Contexto

Troubleshooting no lab só existe **reagindo**: a T3-04 começa com o alerta já disparado e a
T4-04 com o cenário já plantado — nas duas, o diagnóstico é o passo que "simplesmente acontece"
entre o sintoma e a mitigação, porque pressupõe alguém que já sabe qual comando olhar. O
ROADMAP registra exatamente essa lacuna (*troubleshooting: diagnóstico exercitado dentro das
Trilhas 3–4; falta trilha dedicada*). O custo de continuar assim é concreto: quando a VM
aparecer lenta ou cheia às 3h, sem alerta e sem runbook que sirva, a única técnica disponível é
adivinhar e `sudo reboot` — e reiniciar **apaga a prova**: o estado que o diagnóstico ia ler
(top, `ps`, `df`, `free`) morre junto com o processo. Esta issue cria a Trilha 7 com o treino
deliberado que hoje ninguém tem: provocar na VM 4 pane de recurso **de propósito**, diagnosticar
cada uma com o comando certo (culpado nomeado, não chutado), e **recuperar** — o sistema volta
ao baseline, sem sujeira deixada para quem vier depois.

## Objetivo

Estado final: 4 drills de diagnóstico executados na `lab-vm` (CPU alta, memória alta, disco
cheio, processo travado), cada um no formato cronometrado **provocar → diagnosticar → provar →
recuperar**, com o culpado nomeado por saída de comando (PID, uid ou caminho — nunca "acho que é
o app"), `uptime`/`free -h`/`df -h` de volta ao baseline medido antes, a stack docker 6×`Up`
intocada e as saídas coladas em `## Evidências`.

## Dependências

- **Requer Trilha3-04** — o runbook com procedimento escrito e o hábito de drill com cronômetro
  já existem; esta issue é o treino de **diagnóstico** que alimenta aquele processo (sem a
  T3-04, diagnóstico é palpite sem procedimento: não há formato, nem cronômetro, nem para onde
  levar a lacuna encontrada).
- **pré-condição verificável:** `ssh lab@<ip> 'systemctl is-active docker'` → `active` **e**
  `git ls-files RUNBOOK.md docs/runbooks/` → artefato da T3-04 rastreado (o procedimento no
  formato `sintoma → diagnóstico → mitigação → escalação`) — sem os dois, pare aqui.

- **estudo par:** `estudos/trilha7-01-troubleshooting-sistema.md` — ler antes de executar (é o currículo desta issue)

## Escopo

- 4 cenários provocados **de propósito** na `lab-vm`, com o limite declarado **antes** de
  começar em cada um (nunca 100% sustentado de CPU/RAM/disco, nunca `kill` em PID da stack):
  - **CPU alta:** `yes > /dev/null &` em N jobs (ou `stress-ng --cpu` se instalável na VM) →
    culpado nomeado por `uptime` (load average) + `top`/`ps -eo pid,pcpu,comm --sort=-pcpu`
    (%CPU) + `iowait` descartado ou comprovado (`vmstat`)
  - **Memória alta:** preenchimento delimitado em dois passos — arquivo grande (page cache
    subindo sem consumo real) e alocação anônima limitada (`available` caindo) → `free -h` +
    `ps aux --sort=-%mem` → o que fazer **antes** do OOM killer agir
  - **Disco cheio:** `dd` de um arquivo até o filesystem chegar a ≥ 90% → `df -h` (que fs
    encheu) → `du -xh --max-depth=1 <dir> | sort -h` (quem encheu) → `df -i` (inodes)
  - **Processo travado:** processo em `defunct`/`Z` plantado de propósito e estado `D` lido →
    `ps -eo pid,ppid,stat,cmd` → `kill -TERM` × `kill -9`, e o que **não** se resolve matando
- Cada cenário fecha com a **prova** (culpado nomeado pelo comando certo, saída colada) e a
  **recuperação** (baseline de volta, zero arquivo/processo de teste sobrando)
- Confirmação no Prometheus/Grafana (T3-02) é bem-vinda — "o sistema viu o mesmo" — mas a prova
  é a leitura direta na máquina, não o dashboard
- **assume pronto:** `runbook-3-procedimentos`, `drill-executado` (T3-04)
- **entrega:** `4-drills-diagnosticados`, `culpado-por-comando`, `vm-limpa-ao-final`

## Fora de escopo

- Troubleshooting de rede, porta e DNS — Issue 02 desta mesma trilha (`trilha7-02`)
- Alertas, runbook e o formato de drill em si — T3-03/T3-04; aqui se treina o **diagnóstico**,
  lá o **processo** de reagir ao alerta
- APM (tracing, OpenTelemetry, métrica de aplicação) — estágio futuro; o alvo aqui é a máquina
- Anatomia de filesystem, `mount`/`fstab`, tipos de fs — Trilha 6-02; aqui só se **lê** `df`/`du`
  de um filesystem já montado
- Provocar a stack (parar container, `docker compose down`, matar PID de serviço) — T3-04/T4-04;
  esta issue provoca **recursos da VM**, nunca os serviços

## Conhecimentos envolvidos

- Método de diagnóstico: sintoma → hipótese → comando que confirma/desmente → prova
- load average × %CPU × iowait: três números, três histórias diferentes
- page cache vs. memória em uso, `available` vs. `free`, OOM killer e o que fazer antes dele
- `df` (filesystem) × `du` (árvore, com `-x`) × `df -i` (inodes esgotados com disco folgado)
- Estados de processo (R/S/D/Z) e a diferença real entre `kill -TERM` e `kill -9`
- Limite da provocação: drill que derruba a stack é drill que perdeu a VM de teste

## Estado atual

- Troubleshooting só aparece reagindo: T3-04 parte do alerta já disparado, T4-04 do cenário já
  plantado — o diagnóstico nunca foi isolado e treinado com calma
- Nenhum recurso da VM já foi provocado de propósito para ser diagnosticado; não existe registro
  de drill de diagnóstico no tracker
- Sem técnica registrada para "VM lenta / memória no talo / disco cheio": o procedimento de
  facto é reiniciar
- Nenhuma evidência no tracker de culpado nomeado por comando (PID, uid ou caminho)

## Resultado esperado

- 4 drills registrados em `## Evidências`, cada um com cronômetro (provocar → culpado →
  recuperado), comando usado e saída da prova
- Culpado nomeado por comando nos 4 cenários: `ps` para CPU e memória, `du` para disco,
  `ps -eo stat` mostrando a linha `Z`/`D` para processo
- Baseline de volta medido contra o "antes": `uptime` load ≈ inicial, `free -h` com `available`
  folgado, `df -h` abaixo de 70%, arquivos de teste ausentes
- Stack intacta: `docker compose ps` → 6×`Up` (healthy) e `git diff --stat` sem tocar `VPS/src/`
- ≥ 1 lacuna anotada com destino declarado (corrigir `RUNBOOK.md` ou virar issue)

## Requisitos

- Provocação sempre **limitada e declarada antes de começar**: folga de RAM (`Mem available`
  nunca a 0), disco enchido até ~90% e não até 100%, CPU por janela com cronômetro — a stack
  docker nunca é alvo nem meio de provocação
- Ordem fixa em cada cenário: sintoma → hipótese → comando que confirma/desmente → prova →
  recuperação (o mesmo formato de drill da T3-04, com cronômetro nos 3 marcos)
- O culpado é **nomeado pelo comando** (PID, uid ou caminho aparecendo na saída), nunca deduzido
  de "acho que"
- Cada cenário distingue causa de sintoma: `iowait` alto não é "CPU alta"; `buff/cache` alto com
  `available` folgado não é vazamento; load alto não é `%CPU` alto
- Recuperação **verificada por comando** em cada cenário — achar a causa não fecha o drill
- Nenhum processo da stack morto com `kill`, nenhum container parado/reiniciado durante os drills
- Diagnóstico pela leitura direta na máquina (`ssh` + `top`/`ps`/`df`/`du`/`free`); dashboard
  serve como confirmação, nunca como prova
- Lacuna encontrada (comando inexistente na VM, saída que não bateu, tempo que estourou) vira
  registro com destino: `RUNBOOK.md` ou nova issue

## Critérios de aceitação

- [ ] Pré-condição: `ssh lab@<ip> 'systemctl is-active docker'` → `active` **e**
      `git ls-files RUNBOOK.md docs/runbooks/` → artefato da T3-04 rastreado — sem os dois,
      pare aqui
- [ ] **CPU:** carga provocada (`yes > /dev/null &` × N ou `stress-ng --cpu --timeout 60s`) →
      `uptime` acima do baseline **e** `ps -eo pid,pcpu,comm --sort=-pcpu | head -3` nomeando o
      PID culpado; `kill` dos jobs devolve o load ao baseline em ≤ 2 min
- [ ] **CPU:** a leitura separa os três números — o registro cita o load average, o `%CPU` do
      culpado e o `iowait` (`top -bn1`/`vmstat 1 5`), dizendo **qual dos três** subiu
- [ ] **Memória:** preenchimento delimitado → `free -h` mostra o consumo **e**
      `ps aux --sort=-%mem | head -3` nomeia o PID; `Mem available` nunca chegou a 0 e nenhum
      container saiu como `Exited (137)`/`OOMKilled`
- [ ] **Memória:** a prova distingue cache de vazamento — um passo mostra `buff/cache` alto com
      `available` folgado, e a recuperação (`rm` do arquivo / `drop_caches`) devolve o `free -h`
- [ ] **Disco:** `dd`/fill até ≥ 90% → `df -h` acusa **e**
      `du -xh --max-depth=1 <dir> | sort -h` aponta o arquivo culpado; `df -i` lido e o
      `IUse%` registrado
- [ ] **Processo:** `ps -eo pid,ppid,stat,cmd` mostra processo em `Z`/`defunct` **ou** `D` durante
      o drill; no caso `Z`, o **pai** é nomeado pela saída e morto com `kill -TERM`, e o estado
      some (`ps` sem `Z`)
- [ ] **Processo:** o registro explica com a saída na mão por que `kill -9` não resolve um estado
      `D` (e o que resolveria: remover a causa de I/O), mesmo que o `D` tenha sido lido em vez
      de provocado
- [ ] **kill:** diferença `TERM` × `KILL` demonstrada num processo de teste (TERM permite
      fechamento limpo, KILL não) — nenhum PID da stack morto no caminho
- [ ] **Recuperação limpa:** `uptime`, `free -h` e `df -h` de volta ao baseline medido antes,
      `ps` sem processo de teste e sem `Z` sobrando, arquivos de drill removidos (`ls` dos
      caminhos usados → vazio)
- [ ] **Stack intacta:** `docker compose ps` → 6×`Up` no fim e `git diff --stat` sem `VPS/src/`
- [ ] Os 4 drills estão em `## Evidências` com cronômetro e ≥ 1 lacuna com destino declarado

## Validação

- Antes de qualquer provocação: `uptime`, `free -h`, `df -h -x` e
  `ps -eo pid,ppid,stat,comm | head -20` → salvar como baseline do "depois" (cronômetro ligado
  a partir daqui)
- CPU: subir os jobs → `uptime` + `top -bn1 | head -12` + `vmstat 1 5` → nomear o PID com
  `ps -eo pid,pcpu,comm --sort=-pcpu` → `kill $(jobs -p)` → `uptime` de volta ao baseline
- Memória: `dd` de arquivo grande → `free -h` (sobe `buff/cache`, `available` firme) → alocação
  anônima delimitada (`stress-ng --vm 1 --vm-bytes 400M` ou `python3` alocando e tocando páginas)
  → `free -h` (`available` cai) + `ps aux --sort=-%mem` → `rm` + `sudo sh -c 'sync; echo 3 >
  /proc/sys/vm/drop_caches'` → `free -h` de volta
- Disco: conferir o alvo com `df -h /var/tmp` (se o fill for em `tmpfs`, não enche fs de
  verdade) → `dd if=/dev/zero of=/var/tmp/disco-cheio.img bs=1M` controlado por `df -h` até ≥
  90% → `du -xh --max-depth=1 /var/tmp | sort -h` → `df -i` → `rm` → `df -h` no baseline
- Processo: plantar o `defunct` (`python3` com `fork` sem `wait`) → `ps -eo pid,ppid,stat,cmd`
  mostrando `Z` → `kill -TERM <ppid>` → `ps` sem `Z`; `D` lido com amostragem
  (`ps -eo stat,pid,comm`) e explicado, com a limitação registrada se não aparecer
- Fechamento de cada drill: cronômetro parado nos 3 marcos (provocar, culpado, recuperado) e
  saídas coladas em `## Evidências`
- Fecho geral: `docker compose ps` → 6×`Up`; `uptime`/`free -h`/`df -h` ≈ baseline; `ls` dos
  caminhos de teste → vazio; `ufw status` sem mudança

## Evidências

- Baseline inicial (`uptime`, `free -h`, `df -h`) × o mesmo trio depois de cada drill
- CPU: `ps -eo pid,pcpu,comm --sort=-pcpu` com o PID culpado + `vmstat`/`top` mostrando qual
  dos três (load, %CPU, iowait) subiu
- Memória: par de `free -h` (cache × available) + `ps aux --sort=-%mem` com o PID nomeado
- Disco: par `df -h` (≥ 90%) × `du -xh --max-depth=1 | sort -h` apontando o arquivo, mais a
  leitura de `df -i`
- Processo: `ps -eo pid,ppid,stat,cmd` com a linha `Z`/`D` e o `ps` de depois sem ela; o
  `kill -TERM` no pai com o estado sumindo
- Fechamento: `docker compose ps` 6×`Up`, `ls` vazio nos caminhos de teste, cronômetro dos 4
  drills e as lacunas com destino

## Limitações / notas

- Provocação em lab é **exercício**: não há usuário esperando nem pressão de negócio — o que se
  treina é o método e a confiança nele (mesma fronteira declarada na T3-04 e na T4-04)
- Estado `D` depende de I/O verdadeira e pode não aparecer numa janela curta na VM: se não
  nascer, registrar a limitação e usar o `Z` (sempre provocável com `fork` sem `wait`) como
  prova do drill — nunca rotular `S` como `D` para o critério passar
- `drop_caches` é ferramenta de **diagnóstico**, não de tuning: é efêmero e não vira hábito de
  "resolver memória"
- `stress-ng` só entra se instalável na VM sem tocar na stack (`apt`); sem ele, `yes`, `dd` e
  `python3` cobrem os 4 cenários — a issue não depende do pacote
- O baseline é por sessão: com a stack crescendo (T3-02 já adicionou serviços), o "antes" é
  sempre **re-medido** antes do drill, nunca decorado de uma execução anterior
- Lacunas do drill têm destino `RUNBOOK.md` (T3-04) ou nova issue — alterar o runbook em si é
  escopo daquela issue; aqui só se anota o destino
