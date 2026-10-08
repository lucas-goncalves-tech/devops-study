# Estudo — Diagnóstico de sistema: CPU, memória, disco e processo sob comando

> Material de estudo da Trilha 7. Acompanha a Issue 01 (4 drills de diagnóstico provocado na
> VM), mas não é o contrato daquela Issue: aqui é para entender, lá é para provar.

## Diagnóstico é método, não intuição: sintoma → hipótese → comando → prova

- **Por que importa:** o reflexo de quem nunca treinou diagnóstico é `sudo reboot`. Reiniciar
  não é diagnóstico por três motivos caros: (1) **apaga a prova** — o `top`, o `ps -eo stat` e o
  `df` mostrariam a causa, e o estado morre junto com o processo; (2) dá **alívio temporário**
  (cache fria, fila zerada) sem mudar nada da causa, então o sintoma volta e agora com a
  confiança de que "sempre resolveu"; (3) não deixa **aprendizado** — a próxima vez o custo é o
  mesmo, porque ninguém registrou o que o sistema estava dizendo. Em lab o prejuízo é minutos; em
  produção é downtime que se repete.
- **Mecanismo:** o método é uma sequência em que cada passo tem obrigação diferente. A hipótese
  é *chute com formato*; o comando não serve para confirmar o que você já acredita — ele precisa
  ser capaz de **desmentir**; a prova é a saída colada, com o culpado escrito nela:

  ```text
  sintoma ("VM lenta", "df cheio", "processo sumiu")
    └─ hipótese (no máximo 3, a mais barata primeiro): CPU? memória? disco? I/O? processo?
         └─ comando barato que DESCARTA uma camada inteira (uptime / free -h / df -h)
              ├─ descartou → próxima hipótese — NÃO reiniciar, NÃO mudar de comando caro
              └─ apontou? → comando que NOMEIA o culpado (ps / du)
                     └─ prova = saída com PID, uid ou caminho — colada, não resumida
  ```

  A ordem importa porque cada comando barato descarta uma causa **inteira** por menos de um
  segundo, enquanto um comando caro (log de 10 GB, `tcpdump`) só examina a camada que você já
  suspeitava — e frequentemente esconde a resposta.
- **Exemplo no lab:** os 4 drills começam todos pelo mesmo triagem de 3 comandos na `lab-vm`,
  que decide entre os 4 cenários da Issue 01 antes de qualquer investigação:

  ```bash
  ssh lab@<ip>
  uptime         # load subiu? então CPU/I/O/fila entrou na lista
  free -h        # "available" apertado? então memória entrou na lista
  df -h -x       # algum filesystem >= 90%? então disco entrou na lista
  ps -eo pid,ppid,pcpu,pmem,stat,comm --sort=-pcpu | head
  ```

  Com essa tela na frente, a hipótese deixa de ser "reiniciar" e vira "o PID 1234 com 98% de
  CPU" ou "o `/var/tmp` em 94%". A prova do drill é exatamente essa saída — a Issue 01 só
  considera diagnóstico feito quando o culpado aparece **nomeado pelo comando**.
- **Fronteira entre Issues:** a **ordem de custo** (estado geral antes do detalhe) e o formato
  do procedimento são da **T3-04** — lá ela é o passo "diagnóstico" dentro do runbook; aqui ela
  é o músculo que se treina sozinho, sem alerta. O **alerta que chega antes de alguém olhar** é
  da **T3-03**; a **confirmação pelo sistema** (a mesma coisa vista no Prometheus/Grafana) é da
  **T3-02** e aqui é bônus, não prova. Troubleshooting de **rede, porta e DNS** é da Issue 02
  desta mesma trilha — os 4 drills daqui são de **recursos da máquina**.

## load average, %CPU e iowait: três números, três histórias

- **Por que importa:** "CPU alta" é o palpite mais popular e frequentemente errado. A VM pode
  estar lenta com **5%** de CPU (o gargalo é disco: o processo espera I/O), ou com **100%** de
  CPU em 1 processo e o resto da máquina perfeitamente saudável (nem é problema). Tratar o
  número errado custa o drill inteiro: você mata o processo que estava só *esperando*, enquanto
  o culpado continuava em outro lugar.
- **Mecanismo:** são três medições que respondem perguntas diferentes — e só uma delas é
  "quanto de CPU está sendo usado":

  | Número | Pergunta que responde | Onde se lê | Cuidado |
  |---|---|---|---|
  | **load average** (1/5/15 min) | quantos processos estão **na fila** (rodando + esperando I/O) | `uptime`, `top` linha 1 | conta CPU **e** disco: load alto com `%CPU` baixo = espera, não computação |
  | **%CPU** | quanto tempo de CPU **este** processo consumiu na janela | `top`, `ps -o pcpu` | janela curta; 100% em 1 core de 2 não é saturação |
  | **iowait** | % de tempo ocioso **esperando disco** | `top` 3ª linha (`wa`), `vmstat` (`wa`) | não é CPU: é o disco sendo gargalo — tratar como CPU é o erro clássico |

  A leitura combinada é o diagnóstico, não qualquer número sozinho:

  ```text
  load alto + %CPU alto nos processos      → CPU de verdade: ache o PID com mais %CPU
  load alto + %CPU baixo + iowait alto     → gargalo é DISCO/I/O: o culpado espera, não calcula
  load alto + todos baixos                 → fila de algo (I/O, lock, swap): olhar estado (D/S)
  %CPU alto + load normal                  → 1 processo frenético; com N cores, talvez nem importe
  ```

  Regra de bolso: compare o load com `nproc` (núcleos). Load `0,15` numa VM de 2 vCPUs é folga;
  load `3,0` é fila 1,5× a capacidade — degradando, mesmo que o `top` mostre 40% de uso.
- **Exemplo no lab:** provocar, ler os três números, nomear o culpado, recuperar (cronômetro
  ligado, como na Issue 01):

  ```bash
  nproc
  uptime                                   # baseline: load average: 0.15, 0.10, 0.08
  yes > /dev/null &
  yes > /dev/null &
  yes > /dev/null &
  sleep 60 && uptime                       # load sobe ~1 por job na janela de 1 min
  ps -eo pid,pcpu,comm --sort=-pcpu | head -4   # os PIDs dos yes em ~100% — a prova
  vmstat 1 5                               # coluna wa: se ~0, subiu CPU mesmo, não I/O
  kill $(jobs -p)                          # recuperação
  sleep 60 && uptime                       # load volta ao baseline
  ```

  Se `stress-ng` estiver instalável na VM, `stress-ng --cpu $(nproc) --timeout 60s` faz a mesma
  provocação com fim marcado — sem depender de achar os jobs depois.
- **Fronteira entre Issues:** **alertar** quando CPU/disco passam do limiar é da **T3-03**;
  **graficar** a série histórica de CPU é da **T3-02** (lá o valor vira tendência, aqui é leitura
  pontual); **o que fazer depois de saber** (procedimento, escalação) é da **T3-04**. Esta Issue
  entrega só a **leitura**: sair do "está lento" para o "PID x com 98% / iowait 40%".

## free -h mostra cache, não vazamento — e o OOM killer é o fim do filme

- **Por que importa:** o susto mais comum de quem olha `free` pela primeira vez é ver a coluna
  `used` quase cheia e concluir "tem vazamento, vou matar serviço". Dois erros em um: (1) no
  Linux, RAM **parada é RAM desperdiçada** — o kernel a usa para page cache de arquivos, e
  devolve na hora que alguém precisar; (2) a corrida contra o OOM killer tem final pior: quando a
  memória acaba, **o kernel escolhe sozinho** qual processo morrer pelo `oom_score` — e no lab
  pode ser o `postgres` do container, não o processo culpado. Aí o "diagnóstico" vira incidente.
- **Mecanismo:** a coluna que importa é `available` (quanto dá para usar **sem** swapper), não
  `free` nem `used`. E há dois tipos de consumo bem diferentes:

  ```text
  arquivo grande escrito (dd)      → buff/cache sobe, available quase igual → cache, é normal
                                     recuperação: rm do arquivo (e drop_caches se quiser ver zerado)
  processo anônimo (malloc/python) → available desce de verdade             → consumo real
                                     aqui sim: achar o PID e agir antes de available = 0
        available ~ 0  →  kernel sem saída  →  OOM killer mata por oom_score (o que o kernel achar)
                                               /proc/<pid>/oom_score diz quem provavelmente morre
  ```

  A sequência de defesa **antes** do OOM, nessa ordem: (1) `ps aux --sort=-%mem | head` para
  **nomear** o PID; (2) `kill -TERM` nele (graceful, dá chance de fechar arquivo/lock); (3) se o
  consumo for cache, `rm` + `drop_caches`; (4) se for um serviço, **não** é `kill -9` de reflexo —
  é parar o serviço do jeito dele. Esperar `available` chegar a 0 é entregar a decisão ao kernel.
- **Exemplo no lab:** os dois passos da Issue 01 — primeiro provar que cache não é vazamento,
  depois provocar consumo real **com folga declarada** (nunca até 0):

  ```bash
  free -h                                   # baseline
  dd if=/dev/zero of=/var/tmp/cache-filler.img bs=1M count=1024
  free -h                                   # buff/cache subiu, available folgado → NÃO é vazamento
  ps aux --sort=-%mem | head -4             # quem tem RSS de verdade nessa máquina
  sudo sh -c 'sync; echo 3 > /proc/sys/vm/drop_caches'
  rm /var/tmp/cache-filler.img              # recuperação: free -h de volta ao baseline

  stress-ng --vm 1 --vm-bytes 400M --timeout 60s &    # consumo anômimo delimitado (python3 se não houver)
  sleep 5 && free -h                                  # available caiu — e continua > 0
  ps aux --sort=-%mem | head -4                       # o PID que segura os 400M — a prova
  wait                                                 # deixa terminar sozinho; baseline volta
  ```

  O limite do drill está **declarado antes** de começar (`400M` numa VM de 2 GiB, `timeout`
  marcado): provocar memória até o OOM não é treino, é derrubar a stack — e a Issue 01 proíbe
  isso (`Mem available` nunca a 0, nenhum container `Exited (137)`).
- **Fronteira entre Issues:** **alertar** memória/OOM no Prometheus é da **T3-03**; **limitar
  recurso por container** (`mem_limit` no `compose.yaml`) é assunto de containers (**Trilha 1**) —
  aqui se diagnostica, não se dimensiona; **restaurar dado** se o OOM matar o banco é da
  **Trilha 0-05** (backup), não deste drill. O que esta Issue entrega é a leitura honesta de
  `free -h` e a ordem de ação **antes** do killer.

## df diz que o disco encheu, du diz quem encheu (e df -i conta inodes)

- **Por que importa:** "disco cheio" é o sintoma com pior custo colateral: com o filesystem em
  100%, o log não grava, o `apt` falha, o container pode parar de escrever — e a causa do estrago
  posterior é o disco, não o serviço que parou. E o `df` sozinho não resolve: ele aponta **um
  filesystem**, não o arquivo. Quem só sabe `df` chega ao culpado no escuro (ou pior, apaga algo
  aleatório até caber).
- **Mecanismo:** cada comando responde uma pergunta com alcance diferente — e a ordem é de
  menor para maior granularidade:

  | Comando | Pergunta | Alcance |
  |---|---|---|
  | `df -h` | **quanto** de espaço os filesystems usam? | a montagem inteira |
  | `du -xh --max-depth=1 <dir> \| sort -h` | **qual árvore** está grande? | dentro de um fs (o `-x` não cruza montagem) |
  | `ls -lhS <dir> \| head` | **qual arquivo** é o maior? | um diretório |
  | `df -i` | quantos **inodes** (arquivos) sobraram? | o fs — pode chegar a 100% com o disco pela metade |

  ```text
  df -h >= 90%  →  du -xh --max-depth=1 / | sort -h   →  du do suspeito / ls -lhS  →  rm
                    (em qual diretório?)                 (qual arquivo, quanto pesa)
  df -h ok mas erro "No space left on device"  →  df -i 100%: esgotaram inodes
                                                    (milhões de arquivos pequenos — típico de log)
  df -h 100% e du somando MENOS que o total  →  arquivo apagado ainda aberto por um processo
                                                    (rm só libera quando o último fd fecha)
  ```

  Três pegadinhas que o drill cobre: (1) o `-x` — sem ele, `du -xh` de `/` cruza para outros
  filesystems montados (bind mounts do Docker, `/boot`) e o número não bate com o `df`; (2) inode
  é um recurso **à parte** do byte, pré-alocado na criação do fs; (3) `df` 100% com processo
  segurando arquivo apagado só "resolve" achando o processo (`lsof +L1` se instalável, senão
  `ls -l /proc/*/fd/* | grep deleted`).
- **Exemplo no lab:** encher de propósito até ≥ 90%, achar o arquivo pelo comando, conferir
  inodes, limpar:

  ```bash
  df -h /var/tmp                                  # baseline — e confirma que o alvo não é tmpfs
  dd if=/dev/zero of=/var/tmp/disco-cheio.img bs=1M count=2048
  df -h /                                         # subiu: "que encheu?" — só diz o tamanho
  du -xh --max-depth=1 /var/tmp | sort -h         # disco-cheio.img aparece no topo — o culpado
  ls -lhS /var/tmp | head
  df -i /                                         # IUse% baixo: 1 arquivo grande não gasta inode
  rm /var/tmp/disco-cheio.img                     # recuperação
  df -h /                                         # volta ao baseline — drill fecha limpo
  ```

  Para ver o caso de inode de verdade (opcional e com limite), criar N arquivos pequenos num
  diretório de teste e repetir o `df -i` — `IUse%` sobe enquanto o `df -h` mal se mexe.
- **Fronteira entre Issues:** **o que é um filesystem, mount, `fstab` e por que `-x` existe** é
  da **Trilha 6-02** (filesystems/mounts) — aqui só se lê o que está montado; **disco cheio por
  log sem rotação** é causa tratada na **T3-01** (rotação de log) e alertado na **T3-03**
  (limiar ≥ 90%); o **procedimento** de responder ao alerta de disco é da **T3-04**. Esta Issue
  entrega o par de comandos `df` → `du` e o hábito de conferir `df -i`.

## Processo não morre do jeito que você quer: TERM, KILL, Z e D

- **Por que importa:** `kill -9` virou reflexo — e é reflexo ruim em três frentes: (1) ele
  **não deixa fechamento limpo** (nenhum handler roda: arquivo aberto pode ficar corrompido,
  lock não liberado, transação não finalizada); (2) ele **não resolve** processos em estado `D`
  (o kernel nem atende o sinal até a I/O terminar) nem `defunct` (que já está morto); (3) com ele
  você mata o **processo errado com certeza absoluta**, porque vem depois de adivinhar. Saber
  qual sinal usar e qual estado exige o diagnóstico — é exatamente o que a Issue 01 treina.
- **Mecanismo:** o estado em `ps` (`STAT`) diz o que o processo está fazendo; o sinal diz o que
  você pede:

  | Estado | Significado | Matar resolve? |
  |---|---|---|
  | `R` | rodando (ou na fila da CPU) | sim, se ele é a causa |
  | `S` | dormindo **interruptible** — a maioria: esperando conexão, sinal, sleep | sim; ele acorda e trata o sinal |
  | `D` | dormindo **uninterruptible** — esperando I/O do kernel | **não**: `kill -9` não tem efeito até a I/O acabar; a causa (disco ruim, fs pendurado) é que resolve |
  | `Z` (`defunct`) | **já morreu**; fica na tabela até o pai chamar `wait()` | matar o `Z` não faz sentido — o culpado é o **pai** que não faz `wait` |

  ```text
  kill -TERM (15)  →  "por favor, encerre": o processo fecha graceful (handler, flush, exit 0)
  kill -HUP  (1)   →  "recarregue a configuração" (não mata)
  kill -9   (KILL) →  o kernel mata na hora, sem handler, sem limpeza — último recurso
                       e ainda assim inútil enquanto o processo estiver em D
  ```

  O que **não** se resolve matando: estado `D` (resolve-se removendo a causa de I/O); `Z` morto
  sozinho (resolve-se matando ou fazendo o **pai** reapar — `kill -TERM <ppid>`); processo que
  você matou mas a **causa** continua (matou o `yes` e o disco segue cheio — o drill de disco é
  outro); e PID dentro de container (o processo pode morrer, mas o container é gerenciado pelo
  `docker` — o jeito certo é o comando do serviço, não `kill` solto).
- **Exemplo no lab:** provocar um `defunct` de verdade (fork sem `wait`), ver o pai pela saída,
  matar o **pai** e confirmar que o estado sumiu:

  ```bash
  ps -eo pid,ppid,stat,cmd | head -20                       # lendo os estados do sistema
  python3 -c 'import os,time
  if os.fork() == 0: os._exit(0)
  time.sleep(300)' &
  sleep 1
  ps -eo pid,ppid,stat,cmd | awk '$3 == "Z"'                # linha Z com PPID = nosso python — a prova
  kill -TERM $(ps -eo pid,ppid,stat,cmd | awk '$3 == "Z" {print $2}')   # mata o PAI
  ps -eo pid,ppid,stat,cmd | awk '$3 == "Z"' | wc -l        # 0 — o Z foi colhido junto
  ```

  Para `D`: amostrar durante uma escrita síncrona pesada e registrar o que aparecer
  (`dd if=/dev/zero of=/var/tmp/io.img bs=1M count=1024 oflag=dsync &` com
  `while kill -0 $! 2>/dev/null; do ps -o stat= -p $!; done | sort -u`). Se `D` não aparecer na
  janela, a limitação entra no registro — **nunca** se rotula `S` como `D` para o critério passar
  (a Issue 01 aceita `Z` como prova do drill e exige `D` explicado, não forjado).
- **Fronteira entre Issues:** **o que fazer com o alerta** de serviço morto e o formato de
  resposta são da **T3-04**; **parar/ reiniciar serviços do jeito certo** (`docker compose stop`,
  `systemctl restart`) é das **Trilhas 1 e 0** (serviços/containers) — este drill **não** mexe
  na stack; **matéria de processo em profundidade** (pid namespaces, cgroups, `systemd` como
  gerenciador) começa na **Trilha 0-04**. O que esta Issue entrega é a leitura de `STAT`, a
  escolha do sinal e a honestidade de dizer "aqui matar não resolve".

## Como iniciar o modo teach-anything

- "Me ensina o método de diagnóstico (sintoma → hipótese → comando → prova) usando os 4 drills
  desta issue na `lab-vm`, e por que `sudo reboot` não é diagnóstico"
- "Me ensina a ler load average, %CPU e iowait juntos, provocando carga com `yes > /dev/null &`
  e nomeando o PID culpado com `ps -eo pid,pcpu,comm --sort=-pcpu`"
- "Me ensina por que `free -h` mostra cache e não vazamento, e o que fazer **antes** do OOM
  killer, com `free -h`/`ps aux --sort=-%mem` rodando nesta VM"
- "Me ensina a achar o arquivo que encheu o disco com `df -h` → `du -xh --max-depth=1 | sort -h`,
  e o caso de `df -i` em que o disco está folgado e o erro é de inode"
- "Me ensina estados de processo (`R`, `S`, `D`, `Z`) e a diferença entre `kill -TERM` e
  `kill -9`, plantando um `defunct` com `python3` na `lab-vm`"
- "Me ensina o que **não** se resolve matando processo — estado `D`, pai que não faz `wait`,
  causa de I/O — com as saídas de `ps` desta máquina"
