# Estudo — bash: script que falha alto e cron que executa sozinho

> Material de estudo da Trilha 6. Acompanha a Issue 01 (script bash rigoroso + cron), mas
> não é o contrato daquela Issue: aqui é para entender, lá é para provar.

## Por que `set -euo pipefail` — e o que muda contra um script que só funciona no caminho feliz

Todo script do lab até agora foi escrito para **rodar**: alguém abriu o terminal, digitou,
funcionou. Esse é o caminho feliz, e ele é o único que a maioria dos scripts já viu. O
problema é que script de servidor tem uma segunda vida, muito pior: ele roda sozinho, sem
nobody olhando, e aí os erros do meio do caminho deixam de ser mensagens e viram silêncio.

- **Por que importa:** bash, por padrão, **não aborta** quando um comando falha. Ele
  segue para a próxima linha e o exit code final do script é o do **último** comando. No
  formato do `lab-backup.sh` da T0-05 isso quer dizer: o `tar` não achou o diretório de
  dados, imprimiu no stderr, continuou; o `rsync` ou o `find` do fim rodaram; o script
  saiu `0`; a agenda comemorou. O backup não existe e **nada** falhou em lugar nenhum. É
  o tipo de falha que só aparece no dia da perda — que é justamente o dia em que errar é
  caro.
- **Mecanismo:** as três flags fecham três brechas diferentes, e vale saber exatamente
  qual é a de cada uma:

  | Flag | O que fecha | Exemplo do que passaria sem ela |
  |---|---|---|
  | `set -e` | aborta no primeiro comando que sair ≠ 0 | `tar` falha e o script segue para o `rsync` |
  | `set -u` | aborta em variável não definida | `$DATA` vazia vira string vazia e apaga arquivo (`lab-backup-.tar.gz`) |
  | `set -o pipefail` | o **pipe** passa a falhar se qualquer pedaço falhar | `tar ... \| gzip` com `tar` morto e `gzip` feliz → pipe devolve 0 |

  O shebang vem antes de tudo: `#!/bin/bash` diz ao kernel qual intérprete executar. Sem
  ele, o cron (e o `sh -c` de qualquer lugar) cai no `/bin/sh` da caixa — que no Ubuntu é
  `dash`, e aí um array como `MOUNTS=("/" "/home" "/tmp")` do relatório desta issue morre
  com erro de sintaxe. Repare que as flags não são religiosas: `set -e` **não** vale
  dentro da condição de um `if`, nem no comando à esquerda de `&&`/`||` — ali a falha é
  esperada; e sem `pipefail` ele enxerga só o último pedaço do pipe. Por isso as três
  andam juntas: cada uma cobre o buraco que a outra não pega.
- **Exemplo no lab:** o jeito de ver a diferença é rodar o mesmo trecho com e sem as
  flags, medindo com `echo $?`:

```bash
printf '#!/bin/bash\ntar -czf /tmp/demo.tgz /nao-existe 2>/dev/null\ncp /etc/hostname /tmp/\n' > /tmp/feliz.sh
chmod +x /tmp/feliz.sh
/tmp/feliz.sh; echo $?                       # 0: o tar falhou e o script "deu certo"
sed -i '2i set -euo pipefail' /tmp/feliz.sh
/tmp/feliz.sh; echo $?                       # 2: abortou exatamente onde o tar falhou
```

  O primeiro `0` é a prova do Contexto da Issue 01; o segundo `2` é o mesmo script com
  as três flags — o erro que era invisível virou exit code. Depois disso, olhar o
  `lab-backup.sh` da T0-05 com esse olho muda a leitura: a linha que falta não é o `tar`,
  é o `set -euo pipefail` no topo.
- **Fronteira entre Issues:** esta Issue só exige que o **seu** script tenha as flags e
  que `bash -n`/`shellcheck` passem antes de agendar. Provar que a falha é pega
  (teste que derruba build) é da Trilha 2 (CI), e o `expect`/PTY/traps ficam declarados
  fora de escopo — autonomia.

## `$1` não é o argumento todo: quoting, word splitting e o bug do espaço

A armadilha mais cara de bash não é sintaxe exoticamente errada — é uma aspa que faltou.
Ela é cara porque o script **funciona** com os nomes de arquivo bonitinhos que você usou
no teste e quebra no primeiro nome com espaço, que é exatamente o nome que alguém vai criar
depois.

- **Por que importa:** sem aspas, o bash faz duas coisas invisíveis antes de executar:
  **word splitting** (quebra a expansão nos espaços de `$IFS`) e **glob** (interpreta `*`
  e `?` como padrão). `echo $MSG` com `MSG="um dois"` já são dois argumentos; um caminho
  `"/home/lab/meus dados"` vira dois caminhos inexistentes e o comando falha num jeito que
  não tem nada a ver com o problema real. O pior caso não é o erro — é o comando que
  aceita os dois pedaços e faz alguma coisa errada com eles.
- **Mecanismo:** a régua é curta e é sempre a mesma:

  | Escrita | Expande para | Serve para |
  |---|---|---|
  | `$1` | o 1º argumento **quebrado em palavras** | quase nunca é o que você quer |
  | `"$1"` | o 1º argumento como **uma** palavra | o caso normal de valor |
  | `$@` | todos os argumentos, quebrados | pior dos dois mundos |
  | `"$@"` | todos os argumentos, **cada um** como palavra | repassar argumentos (`for`/função) |
  | `"$*"` | todos os argumentos **colados** num valor | virar uma string só |

  `"$@"` existe por um motivo específico: é a única forma de repassar N argumentos sem
  perder a fronteira entre eles e sem importar se algum tem espaço. É o padrão em toda
  função que recebe "os argumentos que eu recebi".
- **Exemplo no lab:** o teste cabe em duas linhas e é a prova que a Issue 01 cobra:

```bash
set -- "arquivo com espaço.txt"
printf '<%s>' $1; echo        # 3 pedaços: <arquivo><com><espaço.txt>
printf '<%s>' "$1"; echo      # 1 pedaço: <arquivo com espaço.txt>
```

  E no `lab-disk-report.sh` a mesma disciplina aparece onde o valor é caminho ou número:
  `df -P "$mp"`, `printf '...' >> "$LOG"`, `[ "$USO" -ge "$LIMIAR" ]`. Note o detalhe do
  `set -u`: ele faria `$1` abortar quando o script roda sem argumento, e é por isso que o
  limiar é escrito como `"${1:-80}"` — default que convive com `-u` em vez de brigar com
  ele.
- **Fronteira entre Issues:** quem escreve `shellcheck` na pipeline descobre esses casos
  sozinho (SC2086 é "variável não aspasada", o achado mais comum da ferramenta) — e é
  exatamente o diagnóstico que esta Issue manda rodar. Corrigir o app Java ou qualquer
  coisa em `VPS/src` continua proibido: o app é carga do lab.

## Exit code é contrato: o relatório que precisa falhar de propósito

`echo $?` é a pergunta mais barata e mais ignorada de bash: "o que acabou de acontecer?".
Quem nunca pergunta sai com `0` na mão e trata como bênção; quem pergunta descobre que `0`
só quer dizer "o último comando terminou dizendo que sim" — nada sobre o trabalho estar
certo.

- **Por que importa:** exit code é a **única** interface que o agendamento tem com o seu
  script. O cron não lê stdout, o systemd lê o número, a CI lê o número. Se o script
  devolve `0` em falha, todos esses leitores concluem "deu certo" e a mentira vira
  estado. E o inverso também: um script que sai ≠ 0 no caso normal é ruim, porque quem
  agenda passa a ignorar — é o alarme que toca sempre, que ninguém mais escuta.
- **Mecanismo:** os números não são aleatórios e valem a pena decorar os cinco primeiros:

  | Exit | Significado |
  |---|---|
  | `0` | sucesso (o único código que diz "ok") |
  | `1` | falha genérica — é o que o relatório desta issue usa |
  | `2` | erro de uso ou do programa (`tar` com caminho inexistente sai `2`) |
  | `126` | encontrado, mas sem permissão de executar |
  | `127` | comando não encontrado |
  | `128+N` | morto pelo sinal N (`Ctrl+C` = 130) |

  O caso do `tar` da T0-05 merece ser visto em duas leituras, porque é onde nasce a frase
  "o tar sai 0 com caminho inexistente" — ela é verdadeira e falsa ao mesmo tempo,
  dependendo de **onde** você lê o número:

```bash
tar -czf /tmp/x.tgz /nao-existe >/dev/null 2>&1; echo $?   # 2: Cannot stat + failure status
tar -tzf /tmp/nao-existe.tgz >/dev/null 2>&1; echo $?       # 2: arquivo nem abre
tar -tzf /tmp/nao-existe.tgz | head; echo $?                # 0: manda o head, sem pipefail
```

  Sozinho, o `tar` é honesto: avisa e sai `2`. Dentro de um pipe, quem responde é o
  último comando da fila — e é por isso que `set -o pipefail` é a terceira flag. E no
  script sem `set -e` o `0` aparece de novo, no nível acima: o exit final é o do último
  comando, então o `tar` falhando no meio e o `cp`/`find`/`rsync` do fim bem-sucedido
  produzem um script com exit `0`.
- **Exemplo no lab:** o `lab-disk-report.sh` entrega exatamente esse contrato, escrito de
  propósito para **falhar** quando o limite passa:

```bash
#!/bin/bash
set -euo pipefail
LIMIAR="${1:-80}"
LOG="${HOME}/disk-report.log"
AGORA="$(date -Is)"
MOUNTS=("/" "/home" "/tmp")
RESULTADO=0
for mp in "${MOUNTS[@]}"; do
  USO="$(df -P "$mp" | awk 'NR==2 {gsub("%","",$5); print $5}')"
  if [ "$USO" -ge "$LIMIAR" ]; then
    ESTADO="ALERT"; RESULTADO=1
  else
    ESTADO="ok"
  fi
  printf '%s limiar=%s %s %s=%s%%\n' "$AGORA" "$LIMIAR" "$ESTADO" "$mp" "$USO" >> "$LOG"
done
exit "$RESULTADO"
```

  A prova é o par de comandos da Issue 01, porque o limiar controla a realidade: com
  `./lab-disk-report.sh 1` todo mountpoint passa e o script sai `1`; com
  `./lab-disk-report.sh 99` nenhum passa e ele sai `0`. Mesmo script, dois exit codes —
  quem agendar vai ler exatamente esse número. Repare também que a falha é **registrada
  duas vezes**: no exit code (para quem chamar) e numa linha datada no `LOG` (para quem
  não estiver por perto — detalhe que o próximo tópico explica porque é necessário).
- **Fronteira entre Issues:** no cron, esse exit `1` não dispara nada: o agendador não
  reage a código de saída e, sem MTA na VM, a stdout do job some. O alerta de verdade
  (estado, histórico, dono) é da Trilha 3 — Prometheus/Grafana da T3-02 e alertas da
  T3-03; o drill de "o disco encheu" é a Trilha 7. Aqui o que se entrega é o contrato
  **escrito e medido** no script.

## cron e systemd timer: mesma ideia, ferramenta diferente — e o lab usa as duas

A T0-05 já instalou um agendamento e já mostrou a tabela comparativa; esta Issue aprofunda
o lado que ficou de fora. Não é escolher "o moderno": as duas ferramentas convivem na
mesma máquina e no mesmo currículo porque elas respondem a perguntas ligeiramente
diferentes — e porque o mundo real que você vai encontrar é feito das duas.

- **Por que importa:** systemd timer é o que você já sabe instalar (unit, `systemctl`,
  journal), e é a escolha certa para o `lab-backup` — a T0-05 justificou com
  `Persistent=true`, que recupera o horário perdido. Só que a maioria dos tutoriais, jobs
  legados, hospedagens compartilhadas e receitas de recepção de vaga assume `crontab -l`.
  E na própria VM já existe cron trabalhando sem ninguém ter olhado: o `unattended-upgrades`
  da T0-03 é agendado por job de cron/timer, e `/etc/cron.d/` vem com arquivo escrito por
  pacote. Não saber ler cron é não saber o que a própria máquina está fazendo.
- **Mecanismo:** as duas resolvem "rodar comando em horário", mas com filosofias
  diferentes — cron é um daemon que consulta lista, systemd é estado declarado em unit:

  | | cron | systemd timer (T0-05) |
  |---|---|---|
  | Onde se declara | `crontab` do user, `/etc/cron.d/`, `/etc/crontab` | `lab-backup.timer` com `OnCalendar=` |
  | Sintaxe | 5 campos (`*/5 * * * *`) | `OnCalendar=daily` (aceita `Mon..Fri 03:00`) |
  | Próxima execução | **não existe** — `crontab -l` só mostra a regra | `systemctl list-timers` → `NEXT`/`last` |
  | Máquina desligada na hora | execução **perdida** | `Persistent=true` → roda no boot |
  | O que aconteceu | `grep CRON /var/log/syslog` | `journalctl -u lab-backup` com exit code |
  | Reage ao exit code | **não** | sim: `OnFailure=`, `Restart=` |
  | "Executar agora" | não (só rodar o script na mão) | `systemctl start lab-backup.service` |
  | Ambiente do processo | PATH curto, sem `.bashrc`, sem herança do seu login | `Environment=`, `WorkingDirectory=` |

  A coluna que muda tudo é a da linha "perdida": por isso o backup **fica** no timer
  (pular dia de backup é inaceitável) e o relatório de disco pode ir para o cron (pular
  um relatório é ruim, não é catástrofe). Ferramenta errada não é a mais velha — é a que
  não entrega a semântica que o job precisa.
- **Exemplo no lab:** as duas convivendo na mesma VM, e a leitura em par é o hábito que
  essa Issue treina:

```bash
systemctl list-timers --no-pager | grep lab-backup    # o que JÁ roda (T0-05)
crontab -l                                            # o que VAI rodar por cron
grep CRON /var/log/syslog | grep lab-disk-report | tail -3   # o que o cron JÁ rodou
journalctl -t cron -n 10 --no-pager                   # a mesma coisa no journal
```

  Note a assimetria de leitura: com o timer, uma tela (`list-timers`) responde "quando
  vem e quando foi"; com cron, são **duas** — `crontab -l` para a intenção e o syslog
  para o fato. Só a intenção pode estar linda com o script quebrado, por isso nenhum dos
  dois sozinho é evidência.
- **Fronteira entre Issues:** o timer da T0-05 continua sendo o agendamento do backup —
  esta Issue **não** migra nada para o cron, só cria o segundo agendamento (o relatório)
  para o contraste existir de verdade. Rotina de log gerado por esse relatório é T3-01
  (logrotate), e qualquer "agendar mais coisa" sem decidir timer vs cron é chute.

## Anatomia do crontab: 5 campos, 7 em `/etc/cron.d`, e onde o cron conta o que fez

A sintaxe de cron é pequena o suficiente para decorar, e é por isso que ela parece simples
— a maioria dos erros não é de sintaxe, é de **lugar** (em qual arquivo a linha mora, com
qual usuário, em qual ambiente) e de **prova** (achar onde a execução deixou rastro).

- **Por que importa:** uma linha de cron errada não dá erro na hora — ela é aceita
  silenciosamente e só se revela depois, quando o job não roda (horário inválido), roda
  como usuário errado (`/etc/cron.d` sem o campo de usuário) ou roda e **some** (saída sem
  redirect numa caixa sem MTA). E no fim, ninguém olha: sem saber onde grepar, "está
  agendado" e "rodou" parecem a mesma coisa.
- **Mecanismo:** os 5 campos, na ordem em que o cron lê:

  | Campo | Valores | Exemplo |
  |---|---|---|
  | minuto | 0–59 | `*/5` = a cada 5 min; `10` = no minuto 10 |
  | hora | 0–23 | `7` = 7h |
  | dia do mês | 1–31 | `*` = qualquer |
  | mês | 1–12 | `*` = qualquer |
  | dia da semana | 0–7 (0 e 7 = domingo) | `1-5` = segunda a sexta; `1,15` também vale |

  Todos aceitam `*` (qualquer), faixa `1-5`, lista `1,15` e passo `*/n`. Existem ainda os
  atalhos `@daily`, `@hourly`, `@reboot` (só em `crontab`/`/etc/cron.d`, não nos 5 campos).
  O detalhe que separa os dois lugares principais é o **usuário**: no `crontab` de
  `lab`, a linha é só `5 campos + comando` (o dono é quem está no spool); em
  `/etc/cron.d/` e no `/etc/crontab`, são **7 campos** — `5 campos + usuário + comando`,
  porque ali qualquer um pode ser o alvo:

```text
*/5 * * * * lab /home/lab/lab-disk-report.sh >> /home/lab/disk-report.cron.log 2>&1
15 7 * * * root test -x /usr/sbin/anacron || run-parts --report /etc/cron.hourly
```

  Onde cada um mora: `crontab -e/-l/-r` mexe em `/var/spool/cron/crontabs/<user>` (só
  root lê o arquivo direto); `/etc/cron.d/` precisa de dono `root`, modo `644`, não
  executável, e nome sem ponto (letras, dígitos, `_` e `-` só — `lab-disk-report.bak` é
  **ignorado** em silêncio); `/etc/cron.daily|hourly|weekly|monthly` são pastas de
  `run-parts`, onde "agendar" é jogar arquivo executável na pasta. E o ambiente é curto:
  `PATH=/usr/bin:/bin`, `HOME` vem do passwd, não há `.bashrc` nem herança da sua sessão —
  por isso caminho absoluto no comando e shebang no script, e por isso o redirect
  `>> arquivo 2>&1`: sem MTA na VM, stdout e stderr do job simplesmente se perdem.

  E a última peça: **onde o cron conta o que fez**. O daemon loga na facility `cron` do
  syslog, e no Ubuntu isso vai parar em `/var/log/syslog` assim:

```text
out  8 07:15:01 lab-vm CRON[4123]: (lab) CMD (/home/lab/lab-disk-report.sh >> /home/lab/disk-report.cron.log 2>&1)
```

  A linha `(lab) CMD (...)` é o registro da execução automática: user, comando exato,
  timestamp. Falta ela = o job não rodou (daemon parado, linha inválida, arquivo
  ignorado); existe mas o seu log não cresceu = rodou e o script falhou em silêncio, o
  que é justamente o caso que o `set -euo pipefail` e o `exit "$RESULTADO"` transformam
  em algo legível.
- **Exemplo no lab:** a sequência completa da Issue 01, do arquivo à prova:

```bash
crontab -l                                              # a regra (intenção)
crontab -l | grep lab-disk-report | cut -d' ' -f1-5     # os 5 campos: */5 * * * *
sudo stat -c '%U %a %n' /etc/cron.d/lab-disk-report     # root 644, nome sem ponto
grep 'limiar=90' ~/disk-report.log                       # a prova de qual agenda disparou
grep CRON /var/log/syslog | grep lab-disk-report | tail -3   # o cron contando o que fez
```

  O truque do limiar é proposital: como `crontab` e `/etc/cron.d` disparam o **mesmo**
  comando e escrevem na **mesma** linha de log, o argumento diferente (`90` no cron.d) é
  a única forma de provar qual dos dois agendamentos rodou — sem isso, "rodou" não diz
  de quem é o mérito. É o mesmo princípio do resto do tracker: evidência tem que
  distinguir as hipóteses.
- **Fronteira entre Issues:** o relatório com limiar fixo aqui é o prelúdio, não o fim:
  virar alerta com estado e dono é T3-03, dashboard é T3-02, e rotação do
  `disk-report.log` (que cresce todo dia) é T3-01. E evitar que dois jobs do mesmo tipo
  se sobreponham (`flock`) fica fora — assim como `expect`/PTY e qualquer shell
  avançado, que são autonomia.

## Como iniciar o modo teach-anything

- "Me ensina `set -euo pipefail` usando o `lab-disk-report.sh` e o `lab-backup.sh` da T0-05 como contraste"
- "Me ensina quoting e `"$@"` com o teste do arquivo com espaço e o `${1:-80}` do relatório"
- "Me ensina exit code como contrato, mostrando o `tar` saindo `2` sozinho e `0` dentro de pipe"
- "Me ensina cron vs systemd timer comparando a linha do `crontab -l` com o `lab-backup.timer` da T0-05"
- "Me ensina a sintaxe de 5 campos do crontab e o campo de usuário do `/etc/cron.d/lab-disk-report`"
- "Me ensina onde o cron conta o que fez, lendo `grep CRON /var/log/syslog` da VM"
