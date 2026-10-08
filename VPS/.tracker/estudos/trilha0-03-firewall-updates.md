# Estudo — Firewall e atualizações: tudo fechado, tudo corrigido

> Material de estudo da Trilha 0. Acompanha a Issue 03 (firewall + atualizações
> automáticas), mas não é o contrato daquela Issue: aqui é para entender, lá é para provar.

## Default deny: só o que foi decidido passa

Firewall não é proteção contra "hackers" em abstrato — é a garantia de que **só o que alguém
decidiu** fala com a sua máquina. A maioria das distros nasce sem política nenhuma: tudo que
sobe, expõe. Na Trilha 1 o Postgres e o Redis vão ligar portas sozinhos, e sem política
"default deny" essas portas já nascem abertas para qualquer IP — ninguém lembrou de abrir,
ninguém precisa lembrar de fechar.

- **Por que importa:** o custo de ignorar isso é silencioso. Nada quebra hoje; a VM fica
  exposta por semanas, e o dia ruim é o dia em que alguém varre a internet inteira e acha a
  sua 5432 com senha fraca. Fechar depois é dor; abrir antes é decisão.
- **Mecanismo:** a política default é o *fallback* — o que acontece quando nenhuma regra
  específica casa com o pacote. `deny incoming` diz: pacote que chega sem regra correspondente
  é descartado. `allow outgoing` diz: o que sai de dentro (apt, ssh para fora) segue sem
  atrito. A ordem de avaliação é: regra explícita primeiro, fallback por último.

```bash
sudo ufw default deny incoming      # fallback: pacote novo que chega = descarta
sudo ufw default allow outgoing     # fallback de saída: apt, DNS, ssh de saída funcionam
sudo ufw allow 22/tcp               # a ÚNICA exceção explícita, decidida por alguém
```

- **Exemplo no lab:** em `lab-vm`, depois da Issue 02 a porta 22 aceita só chave — mas ainda
  abre para qualquer IP, e qualquer outra porta futura também. Com a política acima, o estado
  final é `default deny (incoming)` + uma linha `22/tcp ALLOW`; tudo o mais que a VM liga
  (hoje nada, na Trilha 1 o Postgres) continua invisível de fora **sem** ninguém escrever
  regra para isso — é o deny que trabalha, não o allow.
- **Fronteira entre Issues:** abrir 80/443 (reverse proxy) e 5432/6379 (banco) é da Trilha 1,
  e será feito **por regra explícita sobre esta base** — o trabalho dela é deixar a base
  `deny` pronta. fail2ban, IDS e rate limit são Trilha 1+/DevSecOps (ver `Fora de escopo` da
  Issue 03). E a ordem importa: firewall entra só **depois** da Issue 02 (chave no SSH) —
  trancar a porta com login por senha e sem snapshot é o jeito clássico de se trancar fora.

## ufw é um atalho sobre iptables/nftables — e o que ele faz por você

`iptables` (e o sucessor `nftables`) é a máquina de baixo nível: cadeias, alvos, sintaxe que
não perdoa um erro. `ufw` é a camada de atalho por cima: uma linha legível que **gera** as
regras de firewall do kernel por você.

- **Por que importa:** sem o atalho, o erro mais comum é digitar a regra no lugar errado da
  cadeia (ou esquecer o `-A`/`-I`) e a regra existir, mas não ser avaliada — a VM parece
  protegida e não está. Com `ufw`, o estado é legível em uma tela.
- **Mecanismo:** `ufw allow 22/tcp` → ufw traduz para uma regra inserida na cadeia `INPUT` do
  netfilter com alvo `ACCEPT`; pacote que chega → netfilter avalia → regra casa? aceita :
  política default (drop). `ufw` cuida também do que o atalho esconde: ativar (`--force`),
  persistir após reboot (guarda as regras em disco), e manter a regra de loopback aceita para
  o próprio sistema não se travar.

```bash
sudo ufw allow 22/tcp          # traduz em regra INPUT/ACCEPT no netfilter
sudo ufw status verbose        # política + regras: a tela que se cola como evidência
sudo ufw enable --force         # ativa sem perguntar; grava para sobreviver ao reboot
sudo ufw allow 5432/tcp        # (Trilha 1) abrir = adicionar regra explícita à base deny
```

- **Exemplo no lab:** `sudo ufw status verbose` em `lab-vm` é a tela que responde "o que está
  decidido": `Status: active`, `default deny (incoming)`, `default allow (outgoing)`, e a
  única `ALLOW` é `22/tcp`. Note que o `enable` do ufw insere sozinho uma regra de 22 para
  você não ser trancado fora — mas isso **não** substitui o teste de fora feito logo adiante.
- **Fronteira entre Issues:** desmontar ufw para inspecionar `iptables -L`/`nft list ruleset`
  é leitura desta Issue (entender a camada); gerenciar regras direto no nftables, com
  tabelas próprias, não é — aqui se usa o atalho. E as regras das portas de serviço são da
  Trilha 1, não treinem a abri-las agora.

## A evidência mentirosa: por que testar de fora, nunca de dentro

O teste mais tentador é `nc -zv localhost 80` dentro da própria VM. Ele **mente** — e pior:
mente dizendo "passou".

- **Por que importa:** a prova do firewall é o teste negativo. Quem cola `nc localhost 80` →
  recusado como evidência está medindo o erro: ou o serviço nem está escutando (a recusa vem
  do kernel, não do firewall), ou, se está escutando, o teste **passa** e não prova nada.
- **Mecanismo:** o tráfego de loopback (`127.0.0.1`, interface `lo`) não atravessa a mesma
  trilha do tráfego que chega de fora — é o próprio sistema falando consigo, aceito por
  padrão. Já o pacote vindo do host perde a interface `eth0` e passa pelas regras `INPUT`:
  regra casa → aceita; não casa → política `deny` → descartado (o `nc` do outro lado vê
  timeout/`refused`).

```bash
# DE DENTRO da VM — não prova nada sobre o firewall:
nc -zv localhost 5432        # loopback não passa pelas regras: evidência mentirosa

# DE FORA (no host), mesma origem, duas portas:
nc -zv <ip-da-vm> 22         # deve dar succeeded  → a regra ALLOW existe
nc -zv <ip-da-vm> 80         # deve dar falha/timeout → o deny default está de pé
```

- **Exemplo no lab:** a comparação correta é par à par **do host**: 22 → `succeeded`,
  5432 → `refused`/timeout. Mesma origem, portas diferentes, resultados diferentes — é essa
  diferença que prova o firewall. Se os dois derem sucesso, falta regra; se os dois falharem,
  você se trancou fora (aí a salvação é o snapshot `base` da pré-condição, via `virsh`).
- **Fronteira entre Issues:** a **persistência** (reboot → `ufw status` idêntico) é o resto
  desta mesma Issue; debugar por que um serviço específico não conecta (DNS, NAT do libvirt)
  é outra coisa — a NAT da rede local da Trilha 0 já esconde a VM, e mesmo assim se configura
  deny: quando a VM virar VPS pública na Trilha 1, a regra já estará certa.

## unattended-upgrades: rotina automática, não evento manual

Atualizar "quando der" é atualizar quando alguém lembrar — e ninguém lembra. O
`unattended-upgrades` transforma "eu deveria atualizar" em "o sistema atualiza sozinho e
registra".

- **Por que importa:** atualização manual depende de um ser humano e de um lembrete; o
  sistema fica desatualizado por meses e a evidência de execução não existe (ninguém pode
  provar que atualizou).
- **Mecanismo:** o pacote instala um job no `cron.d`/timer do systemd que roda `apt-get` com
  as seeds de origem permitidas (no Ubuntu, os repos principais com atualizações de
  segurança), baixa, instala e **escreve em log** — a ordem é: agenda → executa → registra.
  Depois, `apt list --upgradable` volta vazio, porque não sobrou nada pendente.

```bash
sudo apt install unattended-upgrades
sudo dpkg-reconfigure unattended-upgrades    # liga/ajusta as origens permitidas
systemctl is-enabled unattended-upgrades     # → enabled
sudo cat /var/log/unattended-upgrades/unattended-upgrades.log | tail
apt list --upgradable 2>/dev/null | wc -l    # → 1 (só o cabeçalho = zero pendentes)
```

- **Exemplo no lab:** em `lab-vm`, a cadeia de evidência é `systemctl is-enabled` →
  `enabled`, o trecho datado do log em `/var/log/unattended-upgrades/` mostrando pelo menos
  uma execução, e `apt update && apt upgrade` deixando `apt list --upgradable` limpo como
  ponto de partida.
- **Fronteira entre Issues:** o limite é de design e está no escopo desta Issue: as seeds
  padrão do Ubuntu **não** incluem repos de terceiros — o repo do Docker, que entra na
  Trilha 1, ficará de fora da automação e será da Issue de atualizações daquela trilha.
  Falhar upgrade automático por pacote de terceiro não é o caso aqui.

## Superfície de ataque: pacote desatualizado é vulnerabilidade conhecida

"Me atualizarem" parece manutenção de rotina; na prática é **segurança**. Um pacote
desatualizado não é risco teórico — é uma vulnerabilidade com número, com data e, muitas
vezes, com PoC público.

- **Por que importa:** a assimetria é o ponto. Do lado do atacante: um scan encontra a
  versão e o exploit correspondente é conhecido (CVE publicado, correção já existe). Do lado
  seu: a correção existe, está no repo, e ninguém instalou. Quem é lento está correndo contra
  quem já sabe.
- **Mecanismo:** a janela é assimétrica — a falha é descoberta, publicada e corrigida nessa
  ordem; entre a publicação e o seu `apt upgrade` fica a janela de exposição. Atualização
  automática encurta essa janela para o ciclo do job (horas, não meses) e, por registrar em
  log, transforma "eu acho que está atualizado" em prova datada.

```bash
apt list --upgradable 2>/dev/null | wc -l    # quantos ainda estão na janela aberta
sudo apt update && sudo apt upgrade -y        # fecha a janela; o log mostra quando
```

- **Exemplo no lab:** a dupla da Issue 03 fecha os dois lados da superfície: o firewall
  reduz **o que é alcançável** (só 22/tcp), o unattended-upgrades reduz **por quanto tempo**
  o que é alcançável fica vulnerável. Um sem o outro é meia proteção: porta aberta com
  serviço corrigido aguenta; porta fechada com serviço velho, quando a Trilha 1 abrir a
  porta, expõe a falha já conhecida.
- **Fronteira entre Issues:** livepatch/kernel e renovação contínua de segurança ficam fora
  do lab (ver `Fora de escopo` da Issue 03); imagens base imutáveis e pipeline de patches da
  Trilha 1+ também não são daqui. Esta Issue entrega o piso: deny por padrão + rotina de
  atualização com evidência.

## Como iniciar o modo teach-anything

- "Me ensina política default deny e regras explícitas usando o `ufw` do lab"
- "Me ensina como o ufw se traduz em regras de iptables/nftables, com `ufw status verbose` e `nft list ruleset` lado a lado"
- "Me ensina por que o teste de firewall tem que vir de fora, comparando `nc localhost` com `nc <ip>` no host"
- "Me ensina o ciclo do unattended-upgrades: cron/systemd, seeds permitidas e o log em `/var/log/unattended-upgrades/`"
- "Me ensina superfície de ataque com `apt list --upgradable` e o que fica de fora nos repos de terceiros"
