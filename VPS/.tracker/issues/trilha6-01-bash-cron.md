---
aliases: [trilha6-01, bash-cron]
tags: [tracker, issue, todo]
status: todo
prioridade: media
---

# Issue 01 — bash: script que falha alto e cron que executa sozinho

## Contexto

O lab já é feito de scripts: a T0-04 entregou `lab-heartbeat.sh`, a T0-05 `lab-backup.sh`,
e as Trilhas 1–3 dependem deles. O que nenhuma trilha fez foi **ensinar bash** — nenhum
script do tracker foi verificado com `bash -n`, nenhum tem `set -euo pipefail` declarado,
nenhum teve exit code conferido com `echo $?`. O único "teste" de script que existe hoje é
ele não cuspir erro no terminal no caminho feliz. O custo aparece justamente quando o
script roda **sozinho**: sem `set -e`, um comando que falha no meio é engolido e o script
sai com o exit code do último comando — o `tar` da T0-05 pode não achar o caminho, avisar
no stderr, e o backup seguir "deu certo". Quem agendou só descobre dias depois, olhando um
arquivo que nunca existiu.

O cron foi adiado na T0-05 em favor do systemd timer; o estudo daquela issue compara os
dois numa tabela, mas o lado do cron ficou **só na tabela**. É uma lacuna cara: o mundo
inteiro roda nos dois — o `unattended-upgrades` da T0-03 já é agendado por job de
cron/timer nesta VM sem ninguém ter olhado o arquivo, e `/etc/cron.d/` vem cheio de job
escrito por pacote — e `crontab -l` é o que a maior parte dos servidores, tutoriais e jobs
legados assumem. Sem cron e sem bash de verdade, quando a Trilha 7 chegar com disco cheio
não existe nenhum relatório datado que diga se o disco encheu ontem ou há três semanas, e
todo script novo continua sendo "funcionou quando eu rodei".

## Objetivo

Estado final: `lab-disk-report.sh` na `lab-vm` — script bash real com shebang,
`set -euo pipefail`, variáveis aspasadas e exit code **≠ 0** quando algum filesystem passa
do limite — diagnosticado com `bash -n` (e `shellcheck`, quando presente) e agendado no
**crontab do user `lab`**, com a execução automática provada por
`grep CRON /var/log/syslog`, enquanto o `lab-backup.timer` da T0-05 segue intacto.

## Dependências

- **Requer Trilha0-05** — o `lab-backup.sh` é o script real que se estuda aqui (exit code,
  quoting, o que `set -euo` mudaria nele) e o cron entra como **alternativa** ao timer que
  você já sabe instalar; sem os dois executados não há contraste e não há objeto de estudo.
- **pré-condição verificável:** `ssh lab@<ip> 'systemctl list-timers --no-pager | grep lab-backup'` →
  linha do `lab-backup.timer` **e** `ssh lab@<ip> 'test -f ~/lab-backup.sh'` → exit 0
  (se a T0-05 gravou o script em outro caminho, troque pelo caminho real usado lá) — sem
  os dois, pare aqui.

- **estudo par:** `estudos/trilha6-01-bash-cron.md` — ler antes de executar (é o currículo desta issue)

## Escopo

- Script `lab-disk-report.sh` na VM: relatório de uso de disco por mountpoint com
  **limiar em argumento** (`${1:-80}`), mountpoints declarados em array, linha datada em
  log próprio e exit ≠ 0 quando algum ponto passa do limite
- Cabeçalho e disciplina de script: `#!/bin/bash`, `set -euo pipefail` e o que cada um
  muda contra um script que "só funciona no caminho feliz"
- Quoting: variáveis sempre aspasadas; `"$1"` (um argumento) vs `"$@"` (todos), provado
  com um valor que contém espaço
- Exit codes como contrato: `echo $?` na mão, o caso do `tar` da T0-05 (exit 2 sozinho,
  exit 0 dentro de pipe, exit 0 como código final do script que não aborta) e o relatório
  que **falha de propósito**
- Diagnóstico antes de agendar: `bash -n` sempre; `shellcheck` se `command -v shellcheck`
  retornar 0
- Agendamento **cron** de verdade: entrada no `crontab` do user `lab` (sintaxe de 5
  campos), caminho absoluto e redirecionamento `>> ... 2>&1`
- `/etc/cron.d/` como o outro lugar de onde o cron lê (7 campos = 5 + usuário + comando,
  dono root, nome sem ponto) — exercitado de verdade e removido ao final, para deixar um
  só agendamento ativo
- Onde o cron conta o que fez: `grep CRON /var/log/syslog` (com fallback
  `journalctl -t cron`)
- **assume pronto:** `lab-backup.sh` + `lab-backup.timer` (T0-05), `lab-heartbeat`
  (T0-04), VM `lab-vm` com user `lab` e sudo (T0-01/02)
- **entrega:** `script-bash-rigoroso`, `exit-code-comprovado`, `cron-rodando-e-observado`

## Fora de escopo

- Shell avançado: `expect`, PTY, traps/exit traps, subshells, metaprogramação — autonomia
  (busca própria, não é material de trilha)
- Rotação/compressão do log gerado pelo relatório — T3-01 (rotação de log)
- Agentes de monitoramento, alerta de disco e dashboards — Trilha 3 (Prometheus/Grafana)
- Shell script dentro do app Java ou qualquer mudança em `VPS/src`/`VPS/pom.xml` — o app é
  a carga fixa do lab, intocado de propósito
- Sincronização de hora (NTP/chrony) e cron em mais de uma máquina — estágio AWS

## Conhecimentos envolvidos

- Shebang: quem escolhe o intérprete e o que acontece quando ele não existe
- `set -e`, `set -u`, `set -o pipefail`: o que cada um aborta (e o que não aborta)
- Word splitting e quoting; `"$@"`, `"$1"`, `"$*"` e por que aspas não são enfeite
- Exit code como contrato: 0/≠0, `echo $?`, e quem lê esse número
- Redirecionamento: `>`, `>>`, `2>&1` e a ordem que muda o resultado
- cron: 5 campos, `crontab -l/-e`, `/etc/cron.d`, ambiente mínimo, facility `cron` no
  syslog

## Estado atual

- Nenhum script do tracker foi verificado com `bash -n`/`shellcheck` nem teve exit code
  conferido — não há `echo $?` em nenhuma `Evidências` do tracker
- `crontab -l` do user `lab` não tem job nenhum (spool de cron da VM vazio para este user)
- Nenhum arquivo do lab em `/etc/cron.d/`; ninguém nunca olhou `grep CRON /var/log/syslog`
- Agendamento no lab existe só como systemd timer (T0-04/T0-05); o lado do cron virou uma
  linha da tabela comparativa do estudo da T0-05
- Disco da VM só é olhado na mão (`df -h`) — não há relatório datado de uso nenhum

## Resultado esperado

- `head -1 /home/lab/lab-disk-report.sh` → `#!/bin/bash`; `grep -c 'set -euo pipefail'`
  no script → 1; `test -x` → exit 0
- `bash -n /home/lab/lab-disk-report.sh; echo $?` → `0` (e `shellcheck` sem erro, quando
  instalado)
- `./lab-disk-report.sh 1; echo $?` → `1` e `./lab-disk-report.sh 99; echo $?` → `0` —
  mesmo script, exit code diferente por decisão
- `crontab -l` → exatamente **uma** linha do job, primeiros 5 campos `*/5 * * * *`,
  caminho absoluto e `>> ... 2>&1`
- `grep CRON /var/log/syslog | grep lab-disk-report` → linha `(lab) CMD (...)` com
  timestamp posterior ao `date` marcado, sem ninguém ter logado para disparar
- `~/disk-report.log` com linha datada de cada execução — inclusive a disparada pelo
  `/etc/cron.d` (linha com `limiar=90`)
- `/etc/cron.d/lab-disk-report` não existe mais ao final; `lab-backup.timer` segue
  presente em `systemctl list-timers`

## Requisitos

- Script em `/home/lab/lab-disk-report.sh`, com `#!/bin/bash`, `set -euo pipefail` e
  permissão de execução
- Limiar vindo de argumento com default seguro (`${1:-80}`), que convive com `set -u`;
  mountpoints declarados em array (o script depende de bash, não de sh)
- Toda variável expandida entre aspas; `"$@"`/`"$1"` usados onde há passagem de argumento
- Exit 0 quando todos os mountpoints estão abaixo do limiar; exit ≠ 0 quando algum passa —
  comprovado com limiar artificialmente baixo e artificialmente alto
- Log próprio e datado escrito pelo próprio script (o cron não lê exit code e, sem MTA, a
  saída do job se perde; a linha no log é o rastro da execução automática)
- Agendamento no `crontab` do user `lab`: 5 campos, `*/5` durante a prova, comando com
  caminho absoluto e redirecionamento `>> ... 2>&1`
- `/etc/cron.d/lab-disk-report` exercitado com campo de usuário e removido ao final —
  **um** agendamento ativo por vez, não dois
- `bash -n` antes de agendar (sempre); `shellcheck` executado quando `command -v shellcheck`
  → 0, com o que ele acusou tratado ou justificado
- Saída do cron redirecionada para arquivo — sem redirect, Ubuntu sem MTA descarta stdout
  e stderr do job
- Nenhuma mudança no `lab-backup.timer` (T0-05) e nenhuma mudança no app (`VPS/src`,
  `VPS/pom.xml` intocados)

## Critérios de aceitação

- [ ] Pré-condição: `ssh lab@<ip> 'systemctl list-timers --no-pager | grep lab-backup'` →
      linha do `lab-backup.timer` **e** `ssh lab@<ip> 'test -f ~/lab-backup.sh'` → exit 0
      (T0-05 executada) — sem os dois, pare aqui
- [ ] `ssh lab@<ip> 'head -1 ~/lab-disk-report.sh'` → `#!/bin/bash` **e**
      `ssh lab@<ip> 'grep -c "set -euo pipefail" ~/lab-disk-report.sh'` → `1` **e**
      `ssh lab@<ip> 'test -x ~/lab-disk-report.sh && echo executavel'` → `executavel`
- [ ] `bash -n /home/lab/lab-disk-report.sh; echo $?` → `0` **sempre**; e se
      `command -v shellcheck` → exit 0, `shellcheck /home/lab/lab-disk-report.sh` sem
      erro (SC2086 ausente) ou com o que sobrou justificado em `Limitações / notas`
- [ ] Exit codes comprovados por comando: `./lab-disk-report.sh 1; echo $?` → `1` **e**
      `./lab-disk-report.sh 99; echo $?` → `0` (o relatório "falha" de propósito)
- [ ] Quoting demonstrado: `set -- "arquivo com espaço.txt"` seguido de
      `printf '<%s>' $1; echo` → 3 pedaços e de `printf '<%s>' "$1"; echo` → 1 pedaço —
      saída colhida em `Evidências`
- [ ] O caso do `tar` da T0-05 em duas saídas, colhidas: `tar -czf /tmp/x.tgz /nao-existe >/dev/null 2>&1; echo $?` →
      `2` **e** `tar -tzf /tmp/nao-existe.tgz | head; echo $?` → `0` (mesma falha, dois
      exit codes — é por isso que `pipefail` existe)
- [ ] `crontab -l | grep lab-disk-report` → exatamente 1 linha;
      `crontab -l | grep lab-disk-report | cut -d' ' -f1-5` → `*/5 * * * *`; e a linha
      contém caminho absoluto + `>> ... 2>&1`
- [ ] Execução **automática** provada: `grep CRON /var/log/syslog | grep lab-disk-report` →
      ≥ 1 linha `(lab) CMD (...)` com timestamp posterior ao `date` marcado antes do
      espera, **e** `tail -5 ~/disk-report.log` mostra linha nova sem você ter executado o
      script (se `/var/log/syslog` não existir, `journalctl -t cron | grep lab-disk-report`
      dá a mesma prova e o fallback fica anotado)
- [ ] `/etc/cron.d/lab-disk-report` exercitado: conteúdo colhido (5 campos + usuário +
      comando; `stat -c '%U %a'` → `root 644`) **e** prova de disparo
      `grep 'limiar=90' ~/disk-report.log` → linha existente; depois removido:
      `ls /etc/cron.d/ | grep -c lab-disk-report` → `0`
- [ ] Contraste intacto: `systemctl list-timers --no-pager | grep lab-backup` → linha
      presente (o timer da T0-05 não foi mexido) e `git diff --stat VPS/src VPS/pom.xml`
      → vazio (app intocado)

## Validação

- Conferir a dependência antes de tudo: `ssh lab@<ip> 'systemctl list-timers --no-pager | grep lab-backup'` →
  linha do timer; `ssh lab@<ip> 'test -f ~/lab-backup.sh && echo ok'` → `ok`
- Escrever o script na VM (editor ou heredoc) e `chmod +x /home/lab/lab-disk-report.sh`
- Sintaxe: `ssh lab@<ip> 'bash -n ~/lab-disk-report.sh; echo $?'` → `0`; depois
  `command -v shellcheck` na VM — se existir, `shellcheck ~/lab-disk-report.sh`
- Caminho feliz e caminho de falha: `./lab-disk-report.sh 99; echo $?` → `0`;
  `./lab-disk-report.sh 1; echo $?` → `1`; `tail -5 ~/disk-report.log` com as duas
  execuções datadas
- Quoting: `set -- "arquivo com espaço.txt"`; `printf '<%s>' $1; echo` → 3 pedaços;
  `printf '<%s>' "$1"; echo` → 1 pedaço
- O tar da T0-05: `tar -czf /tmp/x.tgz /nao-existe >/dev/null 2>&1; echo $?` → `2`;
  `tar -tzf /tmp/nao-existe.tgz | head; echo $?` → `0` (sem `pipefail`, o pipe devolve o
  exit do `head`)
- Agendar sem abrir editor, com a linha inteira em um comando só:
  `( crontab -l 2>/dev/null; echo '*/5 * * * * /home/lab/lab-disk-report.sh >> /home/lab/disk-report.cron.log 2>&1' ) | crontab -`
  → depois `crontab -l` mostra a linha única
- Prova do automático: `date` (marca), esperar até 5 min — cron não tem "executar agora",
  a prévia é rodar o script na mão — e então `grep CRON /var/log/syslog | grep lab-disk-report`
  → linha com hora nova e `tail -5 ~/disk-report.log` crescido sozinho
- `/etc/cron.d`:
  `sudo sh -c "echo '*/5 * * * * lab /home/lab/lab-disk-report.sh 90 >> /home/lab/disk-report.cron.log 2>&1' > /etc/cron.d/lab-disk-report"`
  → `stat -c '%U %a %n' /etc/cron.d/lab-disk-report` → `root 644` → esperar ≤ 5 min →
  `grep 'limiar=90' ~/disk-report.log` → linha (o disparo veio do cron.d, não do crontab)
- Fechar o ciclo: `sudo rm /etc/cron.d/lab-disk-report` →
  `ls /etc/cron.d/ | grep -c lab-disk-report` → `0` **e**
  `crontab -l | grep -c lab-disk-report` → `1`
- Integridade: `systemctl list-timers --no-pager | grep lab-backup` → presente; no host,
  `git diff --stat VPS/src VPS/pom.xml` → vazio

## Evidências

- O script inteiro (`cat /home/lab/lab-disk-report.sh`): shebang, `set -euo pipefail`,
  quoting, log e `exit`
- `bash -n ...; echo $?` → `0` mais a saída do `shellcheck` (ou `command -v shellcheck`
  → 127, com a ausência declarada)
- Os dois `echo $?` do relatório (`1` com limiar artificialmente baixo, `0` com limiar
  artificialmente alto) + o `tail` do log com as linhas datadas
- Quoting: as duas saídas (`3` pedaços vs `1` pedaço)
- O tar: `2` sozinho vs `0` dentro de pipe
- `crontab -l` inteiro (a linha única do job, com redirect)
- `grep CRON /var/log/syslog | grep lab-disk-report` com timestamps (ou
  `journalctl -t cron` no fallback) mais o `date` marcado antes do espera
- Conteúdo de `/etc/cron.d/lab-disk-report` com o `stat` (`root 644`) e a linha
  `limiar=90` do log que prova que aquele job disparou
- Estado final: `ls /etc/cron.d/` sem o arquivo, `systemctl list-timers | grep lab-backup`
  presente e `git diff --stat VPS/src VPS/pom.xml` vazio

## Limitações / notas

- Cron não tem `NEXT`, nem `systemctl start` equivalente, nem `Persistent=true`: horário
  perdido com a máquina desligada **não** roda depois — é por isso que o backup continua
  em `lab-backup.timer` (decisão da T0-05 mantida) e o relatório, que pode pular um dia
  sem drama, vai para o cron
- Cron **não reage** ao exit code: o relatório saindo 1 não derruba nada, não notifica e,
  sem MTA na VM, a stdout do job some. O alerta de verdade é da Trilha 3
  (Prometheus/Grafana + T3-03); aqui o valor é o log datado mais o exit code legível por
  quem chamar o script
- `/var/log/syslog` depende do rsyslog estar instalado — se a imagem vier sem ele, o
  `grep CRON` falha e o fallback é `journalctl -t cron`; registrar qual dos dois foi
  usado na evidência, nunca "funcionou em algum lugar"
- `shellcheck` é diagnóstico, não gate: o repo não tem CI nem linter (a única verificação
  executável é a suíte de testes) — tratar SC2086/SC2046 aqui é treino, não build quebrado
- Limiar e mountpoints ficam no topo do script como valores declarados — mudar "disco
  cheio" em alerta é editar uma linha; transformar isso em alerta com estado, histórico e
  dono é Trilha 3, e o drill de "disco encheu" é a Trilha 7
- Dois agendamentos do mesmo job seria duplicação; por isso o `/etc/cron.d` é exercitado e
  removido. Se o job um dia virar sistema (dono root, vale para todos os usuários), ele
  migra para `/etc/cron.d` e sai do `crontab` do user `lab`
