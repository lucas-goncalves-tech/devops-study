# Estudo — Backup que só existe quando o restore passa

> Material de estudo da Trilha 0. Acompanha a Issue 05 (backup diário + restore drill), mas
> não é o contrato daquela Issue: aqui é para entender, lá é para provar.

## Um backup nunca testado é só um arquivo ocupando disco

Todo mundo que monta um lab diz "eu tenho backup". Pouca gente responde a pergunta que
importa: **de onde você puxaria esse backup às 3 da manhã, e quem já puxou?** A diferença
entre os dois é a única coisa que separa backup de boa consciência.

- **Por que importa:** um `.tar.gz` que ninguém nunca extraiu pode estar vazio, truncado,
  cifrado com uma senha esquecida, ou apontando pra um caminho que o `tar` nem alcançou
  (ele sai com exit 0 mesmo quando o diretório de dados não existia — `tar` avisa no stderr
  e segue feliz). Nada disso aparece no `ls -lh`.
- **Mecanismo:** a única evidência de que um backup existe é **restaurar em lugar limpo e
  comparar**. O `tar -czf` grava; ele não lê de volta. Quem restaura descobre, na hora,
  que faltou `--xattrs`/`--acls` e as permissões vieram todas como root, ou que o caminho
  absoluto dentro do archive não batia com o que achou que era.
- **Exemplo no lab:** o drill da Issue 05 é exatamente esse passo, medido com `diff -r`:

```bash
mkdir /tmp/restore-drill
tar -xzf ~/backups/lab-backup-2026-10-08.tar.gz -C /tmp/restore-drill
diff -r /etc /tmp/restore-drill/etc      # saída vazia = o backup bate com a realidade
```

  Saída vazia = o arquivo existe de verdade. Qualquer linha de diff é um problema **hoje**,
  não na hora da perda — e é por isso que o drill roda agora, com tudo funcionando.
- **Fronteira entre Issues:** a Issue 05 entrega o drill e a *prova* (`restore-drill-aprovado`);
  ela não entrega restore do Postgres (`pg_dump`) nem do backend — isso é Trilha 1, quando
  houver banco e aplicação. Treinar restore de `.tar.gz` de `/etc` aqui, não de um
  serviço que ainda não existe.

## Snapshot protege contra erro de configuração, não contra o disco morrer

A Issue 01 criou o snapshot `base` e ele resolve um problema real: qualquer experimento
errado no ufw ou no systemd vira `virsh snapshot-revert lab-vm base` e a máquina volta ao
estado limpo. Mas o snapshot tem um segredo incômodo — ele mora **no mesmo disco** que ele
diz proteger.

- **Por que importa:** a pessoa que só tem snapshot descobre o problema no pior momento
  possível. O cenário não é raro: disco do host morre, notebook some, `rm -rf` no diretório
  de imagens, ou um `snapshot-revert` mal feito em cima de dados que só existiam na VM.
  Nos quatro casos, snapshot e dado morrem juntos.
- **Mecanismo:** a pergunta que separa os três conceitos é sempre a mesma — **a cópia
  sobrevive se a máquina de origem desaparecer por completo?**

| Conceito | O que é | Sobrevive à perda do host? | Serve para |
|---|---|---|---|
| Snapshot (Issue 01) | estado do disco qcow2, mesmo volume, copy-on-write | **Não** — mesmo disco | desfazer erro de config, na mesma máquina |
| Mirror (`rsync`/`cp` em espelho) | cópia fiel e sempre atual, sobrescreve o destino | Só se estiver em **outro** disco/máquina | manter uma cópia quente; mas apaga junto se o original for corrompido |
| Backup (Issue 05) | cópia **fora** da origem, com data no nome, retida por um tempo | Sim, se o destino for outro host | voltar no tempo e reconstruir |

  O detalhe que quase todo mundo erra: mirror e snapshot também *propagam* a corrupção —
  se o `rm` ou o disco ruim aconteceu antes da cópia, o espelho espelha o estrago. Backup
  datado e retido é o único que te deixa voltar a ontem.
- **Exemplo no lab:** a simulação de perda da Issue 05 é a prova disso, feita de propósito:

```bash
virsh snapshot-list lab-vm                 # vê o snapshot base
virsh snapshot-revert lab-vm base          # dados de exemplo SOMEM da VM
# ...eles voltam de onde? Do host:
scp host:~/backups/lab-backup-*.tar.gz /tmp/
tar -xzf /tmp/lab-backup-*.tar.gz -C /tmp/restore-drill
diff -r /home/lab/dados /tmp/restore-drill/home/lab/dados   # bate de novo
```

- **Fronteira entre Issues:** o revert do `base` continua sendo ferramenta da Issue 01 —
  aqui ele só é usado como **simulação de perda**, e sempre por último, depois do backup
  externo já estar provado (o revert apaga tudo que a VM tinha).

## A régua 3-2-1: esta Issue entrega a primeira cópia e o hábito

A régua mais citada de backup é **3-2-1**: 3 cópias dos dados, em 2 mídias diferentes,
com 1 cópia fora do local (off-site). Ela é citada tanto que vira desculpa para não fazer
nada — "até ter 3-2-1 completo eu não começo".

- **Por que importa:** o custo de ignorar a régua por completo é ter **zero** cópias. O
  custo de começar com uma cópia só é... ter uma cópia. São problemas de ordens de grandeza
  diferentes, e a ordem de quem recupera é sempre a mesma: primeiro existe algum backup,
  depois ele é confiável, depois ele sobrevive a um incêndio.
- **Mecanismo:** a régua é sobre *falhas independentes*. Cada camada cobre um tipo de
  perda:

```text
3 cópias     → original + 2 backups (se um falhar, os outros respondem)
2 mídias     → disco local + outra tecnologia (fita, outro provedor, SSD vs HDD)
1 fora       → outro datacenter/nuvem (roubo, incêndio, flood, o próprio host)
```

- **Exemplo no lab:** a Issue 05 é honesta sobre onde ela fica — e é bom que seja:

| Camada da régua | A Issue 05 entrega? |
|---|---|
| 1ª cópia (fora da VM) | **Sim** — destino no host, `scp`/`rsync` do tarball |
| 2 mídias | Não — é o mesmo disco do host |
| 1 fora do local | Não — se o host (notebook) sumir, a cópia some junto |
| Hábito de testar | **Sim** — o restore drill com `diff -r` |

  Ou seja: ela entrega o `backup-diario-externo`, o `timer-backup` e o
  `restore-drill-aprovado` — cópia + rotina. Off-site, cifra, `borg`/`restic`/`rsnapshot`
  ficam declaradamente fora de escopo e reaparecem no estágio AWS.
- **Fronteira entre Issues:** não tente satisfazer 3-2-1 completo nesta Issue — está fora
  do escopo e vai te fazer treinar no errado (configurar S3 agora distrai do drill, que é
  o que a Issue cobra).

## systemd timer vs cron: mesma ideia, semântica diferente

O agendamento é reaproveitado da Issue 04 — a mecânica de unit já foi aprendida lá com o
`lab-heartbeat`. Aqui ela vira `lab-backup.service` + `lab-backup.timer`, e a escolha do
timer em vez do cron tem uma razão concreta, não é moda.

- **Por que importa:** cron é "executa às 3h, se a máquina estiver ligada; se não estiver,
  azar o seu". Servidor doméstico/lab que fica desligado ou o notebook que dorme simplesmente
  **perde** a execução, sem aviso. E o pior: você só descobre quando o backup mais recente
  tem 12 dias.
- **Mecanismo:** `Persistent=true` diz ao systemd "se o horário agendado passou enquanto a
  máquina estava desligada, roda assim que ela subir". É a diferença entre perder a janela
  e recuperá-la.

```ini
# /etc/systemd/system/lab-backup.service
[Service]
Type=oneshot
ExecStart=/usr/local/bin/lab-backup.sh

# /etc/systemd/system/lab-backup.timer
[Timer]
OnCalendar=daily
Persistent=true

[Install]
WantedBy=timers.target
```

  Comparação direta:

| | cron | systemd timer |
|---|---|---|
| Agendamento | linha em crontab | `OnCalendar=daily` (aceita `Mon..Fri 03:00`) |
| Máquina desligada na hora | execução **perdida** | `Persistent=true` → roda no próximo boot |
| Estado visível | nenhuma (precisa de log próprio) | `systemctl list-timers` mostra `NEXT` e `last` |
| O que aconteceu | grep no syslog | `journalctl -u lab-backup` com exit code |
| Depende de | daemon à parte | mesma infra da Issue 04 |

- **Exemplo no lab:** a prova de que o agendamento existe é ler o par "próxima execução +
  última execução" — não lembrar de cor:

```bash
systemctl list-timers lab-backup.timer --no-pager
# NEXT                        LEFT     LAST                        PASSED  UNIT
# Wed 2026-10-09 03:00:00 BRT 5h ...  Tue 2026-10-08 03:00:00 BRT  18h...  lab-backup.timer

sudo systemctl start lab-backup.service      # prévia manual antes de confiar no automático
journalctl -u lab-backup -n 20 --no-pager    # exit 0 em cada execução
```

  Note o par de leitura: `list-timers` diz **o que vai acontecer e o que já aconteceu**;
  o journal diz **se deu certo**. Só o primeiro pode estar bonito com o script quebrado.
- **Fronteira entre Issues:** o agendamento em si é conceito da Issue 04 (unit, ciclo de
  vida, journal). Aqui você só aplica a mesma receita a um serviço novo; o que é
  exclusivo desta Issue é o **conteúdo** do agendamento (o tarball saindo da VM) e o
  restore que o valida.

## Disco infinito é lenda: retenção é parte do backup, não enfeite

O script roda todo dia e gera um `.tar.gz` por dia. Sem retenção, em dois anos o destino
do backup é maior que o disco que o hospeda — e aí o backup falha, silenciosamente, no
mesmo dia em que você mais precisa dele.

- **Por que importa:** falha de backup é a falha que ninguém vê, porque ela não derruba
  serviço nenhum. O disco cheio também derruba o *host*: `rsync`/`scp` começa a falhar,
  e o log que diria isso pode nem caber mais. E tem o outro extremo — "guardei tudo pra
  sempre" — que transforma o restore num arqueólogo procurando o tarball certo entre
  900 arquivos.
- **Mecanismo:** retenção é uma **decisão declarada no script**, com número, não uma
  intenção. `find -mtime +7 -delete` diz "mais antigo que 7 dias, apaga" — causa e efeito
  direto, todo dia, na mesma execução que cria o arquivo novo:

```bash
#!/bin/bash
# /usr/local/bin/lab-backup.sh (VM) — ideia central
DEST=~/backups                            # no host, via rsync; o destino NÃO é a VM
DATA=$(date +%F)
tar -czf lab-backup-$DATA.tar.gz /etc /home/lab/dados
rsync -av lab-backup-$DATA.tar.gz host:$DEST/
find $DEST -name 'lab-backup-*.tar.gz' -mtime +7 -delete   # retenção: 7 dias
```

  A ordem importa: **cria primeiro, apaga depois** — nunca o contrário, senão existe um
  instante em que não há backup nenhum.
- **Exemplo no lab:** o resultado esperado é ver a rotação acontecida, não prometida:

```bash
ls -lh ~/backups/lab-backup-*.tar.gz     # datas espalhadas, não infinitas
find ~/backups -name 'lab-backup-*.tar.gz' -mtime +7 | wc -l   # 0 = a retenção rodou
```

- **Fronteira entre Issues:** aqui a retenção é `find -mtime +N -delete` — legível, óbvio,
  bom para aprender o conceito. Rodar `find ... -delete` num destino de produção seria
  apavorante; por isso rsnapshot/borg/restic (com dedupe e retenção em hierarquia
  `daily`/`weekly`/`monthly`) estão fora do escopo e entram depois, no estágio AWS. A
  rotação de log do próprio journal (`journald`) também não é desta — é assunto da
  Trilha 1.

## Perguntas para o modo teach-anything

- "Me ensina restore drill com `tar` e `diff -r` usando `/etc` e o diretório de dados deste lab"
- "Me ensina a diferença entre snapshot, mirror e backup com o `virsh snapshot-revert lab-vm base` como exemplo de perda"
- "Me ensina a régua 3-2-1 e o que exatamente a Issue 05 cobre dela, com o diretório de destino no host"
- "Me ensina `lab-backup.timer` com `Persistent=true` lendo a saída de `systemctl list-timers`"
- "Me ensina retenção de backup com `find -mtime +7 -delete` e a ordem certa no script"
