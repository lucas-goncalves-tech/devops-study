# Estudo — Redes: do cabo ao CIDR, a teoria que o lab usa

> Material de estudo da Trilha 5. Acompanha a Issue 01 (redes fundamentos), mas não é o
> contrato daquela Issue: aqui é para entender, lá é para provar.

## Um `curl` embrulha sete camadas — e cada uma tem um comando que a revela

- **Por que importa:** sem o mapa, todo erro de rede vira a mesma sensação difusa.
  `Connection refused`, `No route to host` e `Could not resolve host` são problemas de
  camadas **diferentes** (transporte fechou, rota não existe, nome não resolveu), e quem
  não sabe qual camada olhar reinicia serviço, depois reboota, depois troca de cabo. A
  Trilha 4-04 (incidente) começa exatamente por aí: diagnosticar é descartar camadas, de
  baixo para cima ou de cima para baixo, com um comando por camada.
- **Mecanismo:** o modelo OSI (7 camadas) existe para nomear *onde* um problema mora; o
  TCP/IP (4 camadas) é o que a internet usa de verdade. Eles se empilham da mesma forma:
  cada camada recebe a carga da de cima e **embrulha** com o cabeçalho dela.

```text
  GET /api/v1/actuator/health                 <- camada 7: o que o curl escreveu
   └─ TCP  porta 80, seq/ack, handshake       <- camada 4: "quem fala com quem"
       └─ IP  192.168.122.42 -> 151.101.x.x  <- camada 3: "por onde isso passa"
           └─ ethertype 0x0800 + MAC destino  <- camada 2: "para qual aparelho"
               └─ bits no cabo/virtio         <- camada 1
```

  O cruzamento que interessa é este — e o ponto é: **cada camada tem uma ferramenta
  diferente que a expõe**. Você nunca vê tudo com um comando só.

| OSI | TCP/IP | O que embrulha | Comando que revela |
|---|---|---|---|
| 7 Aplicação (e 6/5) | Aplicação | texto HTTP: método, header, status | `curl -v` |
| 4 Transporte | Transporte | TCP/UDP, porta, conexão | `ss -tlnp`, `ss -tn`, `nc -zv` |
| 3 Rede | Internet | IP de origem/destino, rota | `ip addr`, `ip route` |
| 2 Enlace | — | MAC, ethertype, ARP | `ip neigh`, `ip -br link` |
| 1 Física | — | cabo, placa, virtio | `virsh dumpxml lab-vm \| grep -A3 interface` |

  Duas pegadinhas que a tabela esconde e o lab mostra: **os blocos de aplicação (6 e 5) não
  têm comando próprio** — TLS e sessão aparecem misturados no verbose do `curl`; e no
  **loopback** (`127.0.0.1`) a camada 2 não existe de verdade (não há MAC nem ARP), porque
  o pacote não sai da interface. Para ver ethertype/ARP é preciso um destino fora da VM.
- **Exemplo no lab:** a sequência que revela as quatro camadas em ordem, na `lab-vm`:

```bash
ip -br addr                                   # 3: IP/CIDR da interface (ex.: 192.168.122.42/24)
ip route                                      # 3: default via 192.168.122.1 dev ... proto dhcp
ping -c1 192.168.122.1 >/dev/null; ip neigh   # 2: 192.168.122.1 lladdr 52:54:00:.. REACHABLE
ss -tlnp                                      # 4: o que escuta (Caddy em 80/443)
curl -v http://127.0.0.1/api/v1/actuator/health  # 4+7: "Connected to ... port 80" + "GET" + "301"
```

  O `curl -v` é a aula inteira em uma saída: ele mostra a resolução, a conexão TCP
  (`Connected to 127.0.0.1 (127.0.0.1) port 80 (#0)`), o `GET` enviado e o `HTTP/1.1 301`
  recebido. Com `curl -vk https://127.0.0.1/...` aparece também o handshake TLS por cima do
  TCP — a camada de apresentação que o OSI promete e o TCP/IP engole.
- **Fronteira entre Issues:** esta Issue entrega o **mapa e o comando por camada**
  (`mapa-camadas-com-comando`) lendo a VM; firewall que decide o que passa é T0-03, proxy e
  TLS na borda são T1-03, e diagnóstico como procedimento (playbook) é T3-04. ARP a fundo,
  routing e NAT são Área 2 (AWS) — aqui ARP aparece só como `ip neigh`.

## IPv4 é 32 bits com dono: a máscara é quem separa rede de host

- **Por que importa:** a maior parte dos erros de subnetting ("ping não vai", "não
  conecta, mas a rede é a mesma") nasce de tratar o IP como um número solto. Um endereço
  IPv4 **não é um nome de máquina**, é um par: *onde estou* (a rede) + *quem sou lá dentro*
  (o host). Sem a máscara, os 32 bits não dizem qual é qual — e é por isso que duas
  máquinas "na mesma rede" podem ficar incapazes de se falar.
- **Mecanismo:** 4 octetos de 8 bits (32 bits no total), cada um de 0 a 255. A máscara (ou
  prefixo `/N`) liga os primeiros N bits como **rede** e o resto como **host**:

```text
192.168.122.42/24
11000000.10101000.01111010 . 00101010
└────── rede (24 bits) ────┘└─ host ─┘

/24 = 255.255.255.0   ->  1 rede, 2^8 = 256 endereços, 256 - 2 = 254 hosts
```

  Três endereços sempre pertencem à rede, não ao host: o **primeiro** (rede, `192.168.122.0`),
  o **último** (broadcast, `192.168.122.255`) e por isso a regra dos **-2** (no /24:
  254 hosts utilizáveis). É também a razão de `/31` e `/30` terem regras próprias: quando
  sobram 4 endereços, 2 deles viram host e sobram 2 de verdade.
- **Exemplo no lab:** a VM já é um endereço `/24` com dono, e o `ip route` entrega o
  segundo dono (o gateway):

```bash
ip -br addr
  #   eth0  UP  192.168.122.42/24        <- rede 192.168.122.0, host .42
ip route
  #   default via 192.168.122.1 dev eth0 proto dhcp src 192.168.122.42 metric 100
  #   192.168.122.0/24 dev eth0 proto kernel scope link src 192.168.122.42
```

  Lendo a segunda linha: `192.168.122.0/24` é "esta rede inteira está **diretamente** ligada
  a esta interface" (scope link) — qualquer IP dentro dela é alcançável sem passar por
  ninguém. A primeira linha diz o contrário: tudo que **não** está em `192.168.122.0/24`
  (internet, `github.com`) vai para `192.168.122.1`, o gateway do libvirt. Duas linhas, duas
  perguntas respondidas: *com quem falo direto* e *para quem mando o resto*.
- **Fronteira entre Issues:** ler `ip addr`/`ip route` e explicar cada campo é desta Issue;
  **criar** rede nova (bridge/host-only) é T0-01, firewall por endereço é T0-03, NAT
  (o que o `192.168.122.1` faz com o tráfego saindo) e IPv6 são Área 2 (AWS).

## Subnetting à mão primeiro, `ipcalc` depois — só a ordem inversa ensina

- **Por que importa:** subnetting é a parte de redes que entrevistador cobra no papel e
  que mais se erra por pressa. Fazer no automático com ferramenta não ensina nada — você
  não aprende a *dividir*, aprende a confiar num site. A habilidade real é outra: **estimar
  rápido e justificar** ("cabe 62 hosts nessa subnet porque /26") e saber conferir depois.
- **Mecanismo:** tudo é potência de 2. O prefixo `/N` fixa N bits de rede; os 32-N restantes
  são host. Dividir uma rede em K subnets exige emprestar `ceil(log2(K))` bits — 4 subnets
  = 2 bits = `24 + 2 = /26`. Daí a tabela que resolve 90% dos exercícios:

| Prefixo | Máscara | Endereços | Hosts (-2) | Serve para |
|---|---|---|---|---|
| /24 | 255.255.255.0 | 256 | 254 | rede de escritório/lab |
| /25 | 255.255.255.128 | 128 | 126 | dividir um /24 em 2 |
| /26 | 255.255.255.192 | 64 | 62 | dividir um /24 em 4 |
| /27 | 255.255.255.224 | 32 | 30 | segmento pequeno |
| /28 | 255.255.255.240 | 16 | 14 | VLAN de serviço |
| /30 | 255.255.255.252 | 4 | 2 | link ponto a ponto |
| /32 | 255.255.255.255 | 1 | 0 | um host (loopback, rota de host) |

  Os dois exercícios que a Issue cobra, resolvidos à mão **antes** de qualquer ferramenta:

```text
(1) 192.168.1.0/26 -> quantos hosts?
    32 - 26 = 6 bits de host -> 2^6 = 64 endereços
    -2 (rede 192.168.1.0 e broadcast 192.168.1.63) -> 62 hosts utilizáveis
    faixa utilizável: 192.168.1.1 a 192.168.1.62

(2) dividir 192.168.1.0/24 em 4 subnets iguais
    4 subnets = 2^2 -> emprestar 2 bits -> /26
    bloco de 64 em 64:
      192.168.1.0/26    (.0    - .63)    hosts .1 - .62
      192.168.1.64/26   (.64   - .127)   hosts .65 - .126
      192.168.1.128/26  (.128  - .191)   hosts .129 - .190
      192.168.1.192/26  (.192  - .255)   hosts .193 - .254
```

  Só depois da folha preenchida vem a conferência — e ela tem de **bater**, não substituir:

```bash
  # na VM (instalação aceita: ipcalc é ferramenta de leitura)
ipcalc 192.168.1.0/26
  #   Network:   192.168.1.0/26   Netmask: 255.255.255.192 = 26
  #   HostMin:   192.168.1.1      HostMax: 192.168.1.62
  #   Broadcast: 192.168.1.63     Hosts/Net: 62

python3 -c "import ipaddress as ip; n=ip.ip_network('192.168.1.0/26'); print(n.num_addresses, len(list(n.hosts())), n.broadcast_address)"
  #   64 62 192.168.1.63

python3 -c "import ipaddress as ip; print(*ip.ip_network('192.168.1.0/24').subnets(new_prefix=26), sep='\n')"
  #   192.168.1.0/26 / 192.168.1.64/26 / 192.168.1.128/26 / 192.168.1.192/26
```

  Divergiu? O erro está no seu cálculo (quase sempre nos `-2` ou no tamanho do bloco), e é
  justamente isso que o exercício treina — rodar a ferramenta primeiro mata a divergência e
  com ela o aprendizado.
- **Exemplo no lab:** o exercício ganha chão quando a resposta é uma rede real: a `lab-vm`
  vive em `192.168.122.0/24` (rede do libvirt) — pergunte-se "se eu criasse 4 subnets
  aqui, a VM continuaria reachable pelo gateway?" e responda com o cálculo (`/26` caberia,
  mas o gateway `.1` cairia na primeira sub-rede; a VM em `.42` cairia na segunda, então as
  duas deixariam de estar na mesma rede — é por isso que se redesenha rede, não se
  "ajeita IP").
- **Fronteira entre Issues:** os exercícios e a conferência com `ipcalc`/`python3` são
  critério desta Issue (`exercicios-subnetting-conferidos`); VLSM com múltiplos tamanhos
  sobrepostos, routing entre subnets e qualquer desenho de VPC são Área 2 (AWS).

## TCP entrega em ordem, UDP entrega mesmo — por que SSH é um e DNS é os dois

- **Por que importa:** escolher transporte errado é escolher falha errada. SSH sobre UDP
  viraria sessão truncada com shell bagunçado; DNS sobre TCP puro dobraria o custo de cada
  resolução. Como quase toda porta que você vê no lab é TCP (22, 80, 443), é tentador
  concluir que UDP não é usado — e aí o DHCP que deu IP à sua VM, o DNS que resolveu o
  `apt update` e o NTP que ajustou o relógio viram magia.
- **Mecanismo:** a diferença é a promessa.

| | TCP | UDP |
|---|---|---|
| Modelo | conexão: handshake de 3 vias, stream de bytes | datagrama: um pacote solto, sem sessão |
| Ordem / repetição | garantidas (seq/ack, retransmissão) | nenhuma — pode chegar fora, duplicado ou nunca |
| Custo por mensagem | 1 handshake (3 RTT) antes da 1ª byte | manda e esquece |
| Erro de conexão | `Connection refused` / `timeout` é explícito | só o timeout do aplicativo |
| No lab | SSH 22, HTTP 80/443, Postgres 5432 | DNS 53, DHCP 67/68 |

  **Por que SSH é TCP:** um terminal é um fluxo de bytes onde "digite `rm -rf /`" não pode
  virar "`rm -rf /` perdido" nem "dd" na frente — ordem e integridade são obrigação, então
  a confiabilidade do TCP é o mínimo. **Por que DNS usa os dois:** a pergunta-resposta é
  pequena e cabe num datagrama; abrir uma conexão TCP só para perguntar "qual o IP do
  github.com?" custaria mais idas de ida e volta que a própria resposta — por isso
  `dig`/resolvidor falam **UDP por padrão**. Mas DNS cai para TCP quando a resposta é grande
  (mais de ~512 bytes, ou EDNS0 acima do limite) ou quando precisa de zone transfer
  (`AXFR`), e por isso o protocolo é "os dois". O DHCP é o irmão UDP mais prático de todos:
  o cliente **não tem IP ainda**, então não pode estabelecer conexão — ele manda um
  broadcast (`DHCPDISCOVER`) e quem estiver ouvindo (o `dnsmasq` do libvirt) responde.
- **Exemplo no lab:** ver os dois lados da mesma conversa na `lab-vm`:

```bash
ss -tlnp                                  # TCP: o que escuta (Caddy :80/:443)
curl -v http://127.0.0.1/api/v1/actuator/health >/dev/null 2>&1 &
ss -tn | grep 127.0.0.1                   # TCP: ESTAB durante a conversa (porta + seq)
nc -zv 127.0.0.1 80                       # TCP: "Connection to ... port 80 succeeded!"
dig +short github.com                     # DNS por UDP (padrão)
dig +tcp +short github.com                # mesmo nome, agora por TCP 53 — mesma resposta
```

  E o lado que ninguém vê: `sudo ss -ulnp` mostra os servidores UDP (o resolved escutando
  53), enquanto o lease que deu IP à VM veio por UDP 67/68 — dois protocolos, o mesmo lab.
- **Fronteira entre Issues:** ler `ss`/`nc`/`dig` e justificar a escolha do transporte é
  desta Issue; porta fechada por política é T0-03 (ufw), porta publicada por compose é
  T1-02, e balancear conexões entre múltiplas instâncias (load balancer, TCP vs UDP em LB)
  é trilha própria na lacuna de servidores — nada de routing/NAT aqui.

## O lab já roda DHCP e DNS sem você ter configurado nada — e é isso que você vai ler

- **Por que importa:** DNS e DHCP são os dois serviços que **ninguém lembra de ter
  configurado** e cuja falha derruba tudo sem derrubar nada: se o lease sumir, a VM perde o
  IP e o SSH morre; se a resolução sumir, `apt`, `docker pull` e o `curl` do healthcheck
  falham com mensagens que parecem de rede ("Could not resolve host"). Quem nunca olhou os
  dois por baixo do capô trata sintoma: troca DNS do resolver público, "renova IP" no
  chute, reinicia. Ler o que o lab já faz é o que transforma isso em diagnóstico.
- **Mecanismo:** no lab, os dois papéis já estão distribuídos — você só nunca olhou:

```text
HOST (CachyOS)                                  VM lab-vm (Ubuntu 24.04)
┌────────────────────────────────┐              ┌─────────────────────────────────┐
│ libvirt: rede default          │   DHCP       │ dhclient/systemd-networkd       │
│   virbr0 192.168.122.1/24      │ ───────────> │   recebe 192.168.122.42/24,     │
│   dnsmasq: DHCP + DNS          │   (67/68)    │   gateway 192.168.122.1,        │
│     lease: MAC -> IP -> nome   │              │   DNS 192.168.122.1             │
│                                │   DNS        │ systemd-resolved (resolvectl)   │
│                                │ <──────────> │   consultas e cache; NSS liga   │
└────────────────────────────────┘   (53)       │   getent hosts ao resolved      │
                                                └─────────────────────────────────┘
```

  O `dnsmasq` do libvirt entrega **os três** de uma vez no mesmo ato (IP, máscara, gateway
  e servidor de nomes) — é por isso que a VM nunca precisou de configuração de rede. E no
  Ubuntu 24.04 quem atende `getent hosts`/`apt`/`curl` não é o `/etc/resolv.conf` clássico:
  é o **systemd-resolved**, que fica na frente, cacheia e encaminha para o DNS que o DHCP
  mandou. Dois comandos, dois papéis diferentes: `getent hosts` usa o **NSS** (`hosts: files dns`
  do `/etc/nsswitch.conf`, como qualquer programa usaria); `dig` fala **direto** com o
  servidor de nomes na porta 53, ignorando o resolved — por isso os dois concordarem é
  evidência de que o caminho inteiro está de pé.
- **Exemplo no lab:** os dois lados, lado a lado — lease no host, efeito na VM:

```bash
  # no HOST: o que o dnsmasq emprestou
virsh net-dhcp-leases default
  #   Expiry  MAC               IP            Hostname  SPID
  #   ...     52:54:00:..:..    192.168.122.42  lab-vm   1

  # na VM: bate com o lease?
ip -br addr                                   # eth0 ... 192.168.122.42/24
resolvectl status                             # DNS Servers: 192.168.122.1 (veio do DHCP)
resolvectl query github.com                   # o resolved resolve e mostra a origem
getent hosts github.com                       # mesmo IP via NSS (é assim que curl/ssh resolve)
dig +short github.com                         # mesmo IP falando direto com o servidor
```

  Falha ensinando: se `dig` responder e `getent` não, o problema é NSS/resolved (local); se
  os dois falharem mas `ping 192.168.122.1` funcionar, o problema é o servidor de nomes do
  libvirt; se nem o ping vai, o problema é camada 3 — e você acabou de descartar três
  camadas com quatro comandos.
- **Fronteira entre Issues:** esta Issue **lê** DHCP e DNS do lab como estão
  (`dns-dhcp-lidos-na-vm`); mudar servidor de nomes, criar zona DNS própria ou reserva DHCP
  é T0-01/T0-03, DNS público apontando para a VM é T1-04, e DNS/DNSSEC em provedor de
  cloud (Route 53) é Área 2 (AWS).

## Como iniciar o modo teach-anything

- "Me ensina as camadas do OSI/TCP-IP seguindo um `curl -v` real contra `127.0.0.1/api/v1/actuator/health` da `lab-vm`"
- "Me ensina a ler `ip addr` e `ip route` da `lab-vm` e explicar cada campo, incluindo a rota `default via 192.168.122.1`"
- "Me ensina subnetting com o exercício `192.168.1.0/26` e a divisão de um /24 em 4 subnets, conferindo no fim com `ipcalc` e `python3` na VM"
- "Me ensina a diferença entre TCP e UDP explicando por que o SSH do lab é TCP e por que o DNS do lab usa os dois (`dig` vs `dig +tcp`)"
- "Me ensina DNS e DHCP do lab lendo `virsh net-dhcp-leases default` no host e `resolvectl status`/`getent hosts` na VM"
- "Me ensina a descartar camadas de rede em ordem com `ip neigh`, `ip route`, `ss` e `nc -zv` num diagnóstico do tipo 'curl não conecta'"
