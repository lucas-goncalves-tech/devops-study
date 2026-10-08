---
aliases: [trilha0-05, backup-restore]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 05 — Backup que só existe quando o restore passa

## Contexto

Snapshot (Issue 01) protege contra erro de configuração na **mesma** máquina; não protege
contra o disco do host morrer, contra `rm` errado com snapshot já consolidado, nem contra
perda do notebook. Todo mundo tem "backup"; quase ninguém tem **restore** — e a única
evidência de que um backup existe é restaurar em lugar limpo e bater o diff. É o item mais
barato e mais negligenciado do lab, e um dos primeiros que entrevistador pergunta
("e se o disco morrer amanhã?").

## Objetivo

Estado final: `tar` incremental não — **snapshot completo diário** dos dados que importam
(`/etc`, dados de exemplo, configs da VM) para um diretório **fora da VM** (host), agendado
por **systemd timer** (reaproveitando a Issue 04), com `last-run` visível — e um **restore
comprovado**: restaurar num diretório limpo no host e `diff -r` bater com o original.

## Dependências

- **Requer Trilha0-04** — o agendamento é um `lab-backup.timer`, a mesma mecânica de unit
  que acabou de ser aprendida; sem `servico-lab-heartbeat` confirmado, o timer aqui é chute.
- Pré-requisito do **host**: o host aceita `scp`/`rsync` vindos da VM (sshd ativo e chave
  da VM autorizada) — o destino é fora da VM e o fluxo declarado no Escopo é push da VM
  para cá. Alternativa válida (declarar na execução): inverter e deixar o host **puxar**
  via `rsync` — aí nenhum prereq novo no host, mas o agendamento teria que morar no host.
- **pré-condição verificável:** `systemctl is-enabled lab-heartbeat` → `enabled` e
  `ssh lab@<ip> 'ssh -o BatchMode=yes <host> true'` → exit 0 (o push tem para onde ir).

- **estudo par:** `estudos/trilha0-05-backup-restore.md` — ler antes de executar (é o currículo desta issue)

## Escopo

- Script `lab-backup.sh` na VM: `tar` de `/etc` + diretório de dados de exemplo, com data
  no nome, enviado ao host por `scp`/`rsync` (o destino é **fora** da VM)
- `lab-backup.service` + `lab-backup.timer` (`OnCalendar=daily`, `Persistent=true`)
- Diretório de retenção no host com `find -mtime +N -delete` (retenção declarada, não eterna)
- **Restore drill**: copiar o mais recente para diretório limpo, extrair, `diff -r` contra
  o original → idêntico
- Simulação de perda: `reboot` com rollback do snapshot `base` (dados "perdidos") → restore
  a partir do backup do host → dados de volta
- **assume pronto:** `servico-lab-heartbeat` (unit+timer) — da Issue 04
- **entrega:** `backup-diario-externo`, `timer-backup`, `restore-drill-aprovado`

## Fora de escopo

- Backup do banco Postgres (pg_dump + WAL) — Trilha 1, quando houver banco
- Backup cifrado/off-site/3-2-1 completo, rsnapshot/borg/restic — estudo + estágio AWS
- Restore do backend aplicação — deploy da Trilha 1

## Conhecimentos envolvidos

- O que torna um backup "existir": cópia **fora** da máquina de origem
- Backup vs. snapshot vs. mirror: só o backup sobrevive à perda do host
- systemd timer vs. cron (mesma ideia, semântica `Persistent=true`)
- Restore drill: `diff`, extração em diretório limpo, o hábito de testar
- Rotação/retenção: porque disco infinito é lenda

## Estado atual

- Dados e configs só existem na VM (e no mesmo disco do host que os snapshots)
- Nenhum agendamento de cópia
- Nenhum restore já foi feito — portanto nenhum backup comprovado existe

## Resultado esperado

- `systemctl list-timers lab-backup.timer` → `NEXT` em até 24h e `last` com data
- Diretório no host com ao menos um `.tar.gz` datado, com data mais antiga dentro da
  retenção
- Restore em diretório limpo → `diff -r` sem diferenças
- Após rollback ao snapshot `base` (dados "mortos"), restore devolve os arquivos

## Requisitos

- Destino do backup **fora da VM** — cópia dentro da própria VM não conta (mesma falha
  mata as duas)
- Timer (não execução manual) — o valor do backup é ser automático
- Nome de arquivo com data + retenção declarada no script
- `last-run` conferível: `systemctl list-timers` **ou** marcador de sucesso no destino
- Restore **feito e medido** (`diff -r`), não presumido pelo `tar` ter saído com 0
- Leitura do estado sempre em par: o que existe no destino **e** o que o timer diz

## Critérios de aceitação

- [ ] Pré-condição: `systemctl is-enabled lab-heartbeat` → `enabled` (Issue 04) **e** o
      host aceita push da VM (`ssh lab@<ip> 'ssh -o BatchMode=yes <host> true'` → 0) —
      sem os dois, pare aqui
- [ ] `systemctl list-timers lab-backup.timer` → linha com `NEXT` ≤ 24h e `last` datado
      (ou `lab-backup.service` com `ExecStart` comprovado se ainda não executou)
- [ ] No **host**: existe `lab-backup-<data>.tar.gz` com `tar -tzf` listando `/etc` +
      dados de exemplo
- [ ] Rotação: script contém retenção (ex.: `find ... -mtime +7 -delete`) e há evidência
      de que ela roda (comando no script, não "depois eu faço")
- [ ] **Restore drill:** extrair o `.tar.gz` mais recente em diretório limpo →
      `diff -r <origem> <restaurado>` → sem diferenças
- [ ] **Perda simulada:** `virsh snapshot-revert lab-vm base` (dados de exemplo somem) →
      restore do backup do host → arquivos de volta e `diff -r` idêntico ao pré-perda
- [ ] O backup é agendado por timer, não por cron nem por comando manual —
      `systemctl list-timers | grep lab-backup` → linha presente

## Validação

- Na VM: `systemctl list-timers lab-backup.timer --no-pager` → NEXT/last
- Rodar na mão primeiro (prévia do automático): `sudo systemctl start lab-backup.service` →
  `systemctl status lab-backup.service` → `inactive (dead)` com `status=0/SUCCESS` — depois
  deixar o timer agendar
- No host: `ls -lh lab-backup-*` → arquivo datado; `tar -tzf <arquivo> | head` → conteúdo
- Restore: `mkdir /tmp/restore-drill && tar -xzf <arquivo> -C /tmp/restore-drill && diff -r
  /etc /tmp/restore-drill/etc` → vazio (ou só diferenças justificadas anotadas)
- Perda: `virsh snapshot-revert lab-vm base` → verificar que os arquivos de exemplo sumiram
  → refazer o restore → `diff -r` bate de novo
- `journalctl -u lab-backup -n 20` → execuções com exit 0

## Evidências

- `systemctl list-timers` com a linha `lab-backup.timer`
- `ls -lh` do diretório de destino no host + `tar -tzf` de um backup
- Saída do `diff -r` do restore drill (vazia = sucesso)
- O par antes/depois do `snapshot-revert`: arquivos presentes → sumiram → restore →
  presentes de novo

## Limitações / notas

- O destino "fora da VM" é o **host** — se o host for o notebook e o notebook sumir, não
  há cópia. A régua real é 3-2-1 (3 cópias, 2 mídias, 1 fora); esta Issue entrega a 1ª
  cópia + o **hábito de restore**, o resto (off-site, cifra) é estágio AWS
- `tar` de `/etc` com `--xattrs`/`--acls` pode ser necessário para restaurar permissões
  exatas — detalhe que só aparece quando o restore é de verdade (mais um motivo do drill)
- `snapshot-revert` como "simulação de perda" apaga **tudo** que a VM tinha — feito por
  último nesta Issue, e sempre com o backup externo já provado antes do revert
