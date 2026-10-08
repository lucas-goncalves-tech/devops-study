# Estudo — systemd: o processo que pertence ao sistema

> Material de estudo da Trilha 0. Acompanha a Issue 04 (serviço lab-heartbeat), mas não é o
> contrato daquela Issue: aqui é para entender, lá é para provar.

## Quando o processo deixa de ser "seu" e passa a ser do sistema

Quem só rodou programa no terminal tem um modelo mental incompleto: `./meu.sh` roda enquanto
o terminal está aberto, e o que acontece depois é "some". Numa VM de servidor esse modelo
cobra preço em três dores concretas:

- **Sobrevivência:** o processo morre no `logout`, no `reboot`, numa queda de SSH. Na Trilha 1
  o backend vai cair do mesmo jeito — e o primeiro `reboot` da VM apaga tudo "silenciosamente".
- **Memória:** log no terminal é volátil. Ninguém responde "o que aconteceu ontem às 3h" sem
  arquivo com data.
- **Ordem:** um processo que tenta falar com a rede ou com o banco antes deles existirem falha
  e ninguém reorganiza a fila.

O que muda não é o script — é **quem é o pai dele**. No terminal, o pai é o seu shell: quando
o shell some, o filho é órfão (e normalmente morto). Com systemd, o pai é o init (PID 1), que
não faz logout nem morre com a sessão. Por isso o processo "pertence ao sistema":

| | Processo no terminal | Serviço no systemd |
|---|---|---|
| Pai | seu shell | init (PID 1) |
| Sobrevive a logout/reboot | não | sim, se estiver habilitado |
| Quem reanima quando cai | ninguém | o systemd, conforme `Restart=` |
| Onde está a história | stdout perdido | journal, com timestamp |
| Ordem de subida | quem rodou primeiro | declarada em `[Unit]` |

Causa e efeito: você entrega a unit ao systemd (`systemctl start`), e a partir daí o ciclo de
vida — subir, parar, reiniciar, esperar dependências — deixa de ser responsabilidade sua e
passa a ser dele. A Issue 04 só exige que você veja isso acontecer com um script minúsculo,
porque o mecanismo é idêntico ao de um backend Java.

**Fronteira entre Issues:** o backend Java como serviço é Trilha 1 — aqui se aprende com o
`lab-heartbeat` de propósito. Rodar/desligar a VM e ter por onde entrar é Trilha0-01 e
Trilha0-02; estar com firewall ativo é Trilha0-03. Esta Issue assume tudo isso pronto e só
traz o ciclo de vida de processo.

## A unit file: três blocos que decidem coisas diferentes

O erro clássico é ler um `.service` como "configuração" — é um contrato com três seções, e
cada uma responde a uma pergunta que, se ficar em branco, o systemd decide por você (às vezes
do jeito que você não quer).

Cada bloco decide:

- **`[Unit]` — *por que e junto com o quê*.** Descrição, ordem e dependência. É aqui que se
  escreve "não comece antes da rede" (`After=network-online.target`) — sem isso, um serviço
  de rede tenta falar no boot antes de existir rede, falha, e o erro aparece horas depois,
  num lugar que ninguém suspeita.
- **`[Service]` — *o que roda e o que fazer com ele*.** `ExecStart` (o comando), o tipo de
  processo, e a política de reincidência: `Restart=`, `RestartSec`. É a seção que transforma
  "programa" em "comportamento supervisionado".
- **`[Install]` — *o que acontece quando alguém habilita*.** `WantedBy=multi-user.target` é
  a âncora de boot: diz em qual alvo da cadeia de boot esta unit entra. Esse bloco **só é
  consultado no `enable`/`disable`** — mudar ele e esquecer de `daemon-reload` + `enable` é
  outra fonte clássica de "por que não subiu no reboot".

A unit real do lab, na forma em que ela vive na VM:

```ini
# /etc/systemd/system/lab-heartbeat.service
[Unit]
Description=Lab heartbeat - exercita ciclo de vida do systemd
After=network-online.target

[Service]
Type=simple
ExecStart=/usr/local/bin/lab-heartbeat.sh
Restart=on-failure
RestartSec=2

[Install]
WantedBy=multi-user.target
```

Leitura causa-efeito dessa unit: o `ExecStart` é o único comando que o systemd conhece (ele
não adivinha o que seu script faz); `Restart=on-failure` + `RestartSec=2` diz "se o processo
morrer com falha, espere 2 s e volte sozinho"; `WantedBy=multi-user.target` diz "quando o
sistema entrar no alvo multi-user (o estado normal de servidor, sem interface gráfica),
ative-me". Os três blocos juntos respondem, nessa ordem: *quando posso rodar → o que rodar →
quando me pedir para existir*.

Duas alavancas de dor, para não decorar à toa: editar a unit sem `systemctl daemon-reload` →
o systemd continua com a versão antiga em memória, e você jura que a edição não "pegou";
declarar `ExecStart` apontando para caminho inexistente → a unit entra em loop de falha
visível só no journal.

**Fronteira entre Issues:** `WantedBy=timer` e agenda de execução são da **Issue 05**
(timer/backup) — a Issue 04 é só serviço, um conceito por vez. `depends` complexos entre
serviços (o backend esperar o Postgres de verdade) é estágio AWS, fora desta Trilha.

## Restart=on-failure: exit 0 não reinicia, e isso é o critério inteiro

Aqui mora a distinção que a Issue 04 chama de "coração do requisito". A política `on-failure`
não olha se "o processo sumiu" — ela olha **o código de saída** (e o sinal) com que ele sumiu.

Por que importa: se o systemd reiniciasse tudo que terminasse, um script que faz seu trabalho,
grava o resultado e sai com `exit 0` (o caminho normal de todo programa Unix) entraria em
loop infinito de subir/morrer. E se nunca reiniciasse, um crash de verdade ficaria morto até
alguém perceber. O código de saída é o canal com que o processo diz "acabei" versus
"quebrei" — `on-failure` é a escuta desse canal.

Mecanismo, em árvore:

```
processo terminou
├── exit 0                    → "terminei"      → NÃO reinicia (nem em always seria falha)
├── exit ≠ 0 (1, 2, 127...)   → "quebrei"       → reinicia em RestartSec  ✓
├── killed por SIGTERM/SIGKILL
│     pedido pelo systemd (stop/restart)         → NÃO reinicia (foi ordem, não falha)
│     morreu por sinal (ex.: SIGSEGV)            → reinicia  ✓
└── outros sinais (ex.: SIGINT/SIGTERM externo)  → em geral NÃO conta como falha
```

Repare na consequência prática: o `lab-heartbeat.sh` tem dois modos. No modo normal ele
escreve heartbeat e segue no loop — se você o matar com `exit 0` de propósito, o systemd fica
parado, `is-active` → `inactive`, e **ninguém alerta**: para ele, isso foi um fim legítimo.
No modo "crash" o script sai com código 1; aí, em até `RestartSec=2` s, o journal grava
`Scheduled restart job` sozinho, sem ninguém digitar nada. Duas mortes iguais para quem olha
o terminal, decisões opostas para o sistema — por isso a Issue exige que o crash seja
provocado por código ≠ 0: é a única forma de exercitar a semântica que `on-failure` observa.

Corolário que evita frustração: `Restart=on-failure` **não** protege contra `exit 0` nem
contra `systemctl stop`. Se o objetivo um dia for "sempre voltar", a política é outra
(`Restart=always`) — mas escolha a política olhando o significado do código de saída do
*seu* processo, não por reflexo de tutorial.

**Fronteira entre Issues:** o *conteúdo* dos logs (rotação, `storage=persistent`, quanto
tempo guardar) é Trilha 1, quando houver serviços reais de verdade. Aqui o journal é apenas
a evidência do restart.

## enable ≠ start: dois eixos que não se tocam

Quem vem do terminal assume que "ligar" é uma chave só. O systemd tem **dois estados
independentes**, cada um com seu comando e sua pergunta de verificação:

| Eixo | Comando | Pergunta | Verificação |
|---|---|---|---|
| Estado **atual** | `start` / `stop` / `restart` | está rodando **agora**? | `systemctl is-active lab-heartbeat` → `active` / `inactive` |
| Estado de **boot** | `enable` / `disable` | vai subir **no próximo boot**? | `systemctl is-enabled lab-heartbeat` → `enabled` / `disabled` |

Causa e efeito — as quatro combinações que importam:

- **enabled + active** (estado final da Issue 04): roda agora **e** volta sozinho no reboot.
- **disabled + active**: roda agora, mas morre no próximo reboot e ninguém ressuscita — o
  estado mais traiçoeiro, porque o teste de hoje passa e o servidor "some" na primeira
  manutenção. É exatamente o que acontece quando você faz `start` e esquece o `enable`.
- **enabled + inactive**: no próximo boot ele sobe, mas agora está parado — `stop` não mexe
  em enable.
- **disabled + inactive**: nada roda, nada sobe.

Dois detalhes de mecânica que causam 90% dos "não funcionou": (1) `enable` **só** lê o grupo
`[Install]` — se ele estiver vazio ou apontando para outro target, o `enable` pode até
aceitar e o boot não trazer nada; (2) editar o arquivo e chamar `enable` de novo não basta —
é `systemctl daemon-reload` antes, senão o systemd opera sobre a versão antiga. E a ordem do
teste importa: `enable` **antes** do `reboot`, senão o reboot não prova nada (só prova que
você não habilitou).

**Fronteira entre Issues:** sobrevivência do próprio arquivo de log depois de vários boots
é Trilha 1; a Issue 05 vem em cima deste eixo exigindo `is-enabled lab-heartbeat` →
`enabled` como pré-condição — o timer só faz sentido sobre um serviço já habilitado.

## journalctl: ler a história em vez de adivinhar com echo

Sem systemd, a prova de algo é "eu vi no terminal" — e o terminal não guarda. O journal é a
mesma saída, porém gravada com timestamp, prioridade e unidade de origem. Por que importa:
toda pergunta de operação vira consulta, não reexecução. Em vez de rodar de novo para ver o
que acontece, você pergunta ao que já aconteceu.

O trio que resolve quase tudo, com a unit do lab:

```bash
# o que esta unit fez, com histórico (a timeline com >= 3 eventos de Started/Stopping)
journalctl -u lab-heartbeat -b --no-pager | head -20

# ao vivo: ver a falha e o "Scheduled restart job" chegando dentro de RestartSec
journalctl -u lab-heartbeat -f

# depois do reboot: só a história deste boot (o reboot zera a contagem)
journalctl -u lab-heartbeat -b
```

Causa e efeito de cada flag:

- **`-u lab-heartbeat`** filtra pela unidade — sem ele você lê o journal inteiro da VM e se
  perde; é o equivalente ao "echo do meu script", só que do sistema inteiro.
- **`-f`** é o `tail -f` do journal: é como se observa o auto-restart *acontecendo*, sem
  reexecutar o crash cem vezes para "caçar" a mensagem.
- **`-b`** delimita ao boot atual — a razão de o critério da Issue contar
  `Started`/`Stopping` em `-b`: start inicial + parada deliberada + restart do crash são três
  eventos distintos da mesma sessão de máquina, e um reboot começa uma nova contagem.

O erro de método, e a Issue o proíbe de propósito: `echo` no terminal prova que *você*
executou alguma coisa agora; `journalctl` prova que **o systemd** agiu sozinho. A frase
`Scheduled restart job` não vem do seu script — vem do supervisor. Só o journal distingue
"eu reiniciei" de "ele reiniciou sozinho", que é o fato que a Issue 04 existe para provar.

**Fronteira entre Issues:** onde e por quanto tempo o journal é gravado em disco
(`storage=persistent`, rotação) é Trilha 1; prioridades de log do aplicativo e estruturação
de eventos entram junto com o backend real. Aqui se usa o journal como instrumento de
observação do ciclo de vida, nada mais.

## Como iniciar o modo teach-anything

- "Me ensina a diferença entre processo de terminal e serviço systemd usando o
  lab-heartbeat.service e o `systemctl status lab-heartbeat` desta VM"
- "Me ensina a anatomia de um unit file lendo o lab-heartbeat.service real e explicando o
  que cada bloco `[Unit]`, `[Service]` e `[Install]` decide"
- "Me ensina `Restart=on-failure` e a semântica do código de saída fazendo o
  lab-heartbeat.sh sair com 0 e depois com 1 e comparando o journal nos dois casos"
- "Me ensina a diferença entre `systemctl enable` e `systemctl start` com
  `is-enabled`/`is-active` no lab-heartbeat"
- "Me ensina a ler a história de um serviço com `journalctl -u`, `-f` e `-b` no
  lab-heartbeat desta VM"
