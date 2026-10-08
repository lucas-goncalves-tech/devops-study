---
aliases: [trilha5-01, redes-fundamentos]
tags: [tracker, issue, todo]
status: todo
prioridade: media
---

# Issue 01 — redes: do cabo ao CIDR, a teoria que o lab usa

## Contexto

As Trilhas 0–4 operaram a VM inteira sem nunca abrir a teoria que a sustenta: a T0-03
digitou regra de ufw sem nomear camada, a T1-03 pôs o Caddy na frente da API sem ler um
handshake, o CI e o Prometheus falaram por porta e rota sem ninguém dizer de onde vem o IP
que eles usam. O lab **funciona** e a pessoa não sabe explicar por que funciona — lacuna que
aparece na primeira entrevista ("o `curl` caiu com `connection refused`, me explica") e no
primeiro problema fora do happy path (rota sumiu, DNS não resolveu, subnet colidiu). O
ROADMAP já declara: quase toda a categoria Redes é trilha nova. Sem esta issue, todo
diagnóstico das trilhas seguintes continua sendo tentativa e erro, e o subnetting que a
vaga cobra nunca foi feito uma vez sequer.

## Objetivo

Estado final: lendo a `lab-vm` que já existe, cada camada do OSI/TCP-IP aparece com o
comando que a revela — `ip neigh` (ethertype/ARP), `ip addr`/`ip route` (IP), `ss`/`nc`
(TCP), `curl -v` (HTTP) — e mais: subnetting/CIDR resolvido **à mão** e conferido na VM com
`ipcalc` ou `python3`, e DNS/DHCP do próprio lab observados (`getent hosts`/`dig`,
`resolvectl status`, lease do libvirt). Nenhuma VM nova, nenhuma regra nova.

## Dependências

- **Requer Trilha0-01** — a VM é onde a teoria é lida na prática: sem `lab-vm` rodando não
  há `ip addr`, `ip route`, lease nem resolução para observar. A rede NAT do libvirt e o IP
  anotado lá são o material desta issue.
- **pré-condição verificável:** `virsh list --all` mostra `lab-vm` `running` **e**
  `ssh lab@<ip-da-vm> ip addr` responde.

- **estudo par:** `estudos/trilha5-01-redes-fundamentos.md` — ler antes de executar (é o currículo desta issue)

## Escopo

- Modelo OSI (7 camadas) × TCP/IP mapeado ao que acontece num `curl` real — tabela
  camada → comando → saída, cada camada com o comando que a revela: ethertype/ARP com
  `ip neigh`, IP com `ip addr`/`ip route`, TCP com `ss`/`nc`, HTTP com `curl -v`
- IPv4: octetos, máscara/prefixo, rede vs host vs broadcast e a regra dos hosts utilizáveis
- Subnetting e CIDR: exercícios calculados à mão (192.168.1.0/26 → quantos hosts, dividir
  um /24 em 4 subnets, ...) e **conferidos na VM** com `ipcalc` ou `python3` (`ipaddress`)
- TCP vs UDP: por que SSH é TCP, por que DNS usa os dois, DHCP ser UDP — lido na VM com
  `ss`, `nc` e `dig +tcp`
- DNS e DHCP pela prática do lab: a VM recebe IP por DHCP do libvirt (lease em
  `virsh net-dhcp-leases default`), `resolvectl status` mostra o resolved, `getent hosts` e
  `dig` mostram a resolução acontecendo
- Ferramentas de leitura que faltarem na VM: instalar só `ipcalc`/`dnsutils` (leitura, não
  infra) e registrar nas notas
- **assume pronto:** `vm-lab-vm` + rede NAT do libvirt com IP anotado — da Trilha0-01;
  app Notes API atrás do Caddy com TLS autoassinado (T1-03) como alvo do `curl`
- **entrega:** `mapa-camadas-com-comando`, `exercicios-subnetting-conferidos`,
  `dns-dhcp-lidos-na-vm`

## Fora de escopo

- Routing estático e dinâmico (rotas manuais, OSPF/BGP) — Área 2 (AWS)
- ARP profundo (proxy ARP, gratuitous ARP, spoofing) — só a superfície: `ip neigh`
- NAT na prática (MASQUERADE detalhado) e NAT Gateway — Área 2 (AWS)
- IPv6 (endereçamento, SLAAC, dual-stack) — Área 2 (AWS)
- Infra nova de rede: VM nova, bridge/host-only, regra nova de ufw — ufw é T0-03 e
  múltiplas VMs/redes é T0-01 + Área 2 (AWS); esta issue só **lê**
- TLS a fundo (CA, handshake, renovação) — T1-03 já o entregou na borda; aqui o `curl -v`
  só o observa

## Conhecimentos envolvidos

- Modelo OSI (7) × TCP/IP (4) e o que cada camada embrulha no pacote
- Frame × packet × segment: ethertype, MTU e cabeçalho de cada nível
- ARP: a MAC é resolvida antes de qualquer IP falar na rede local
- IPv4: 32 bits em 4 octetos, máscara/prefixo, rede vs host vs broadcast
- Subnetting e CIDR: potências de 2, VLSM básico, conferência com `ipcalc`
- TCP (handshake, porta, stream confiável) × UDP (datagrama, sem ordem)
- DNS: resolução, `getent` vs `dig`, systemd-resolved
- DHCP: lease, gateway e DNS entregues pelo `dnsmasq` do libvirt

## Estado atual

- Trilhas 0–4 usaram rede sem nomear camada: ufw (T0-03) e Caddy/TLS (T1-03) foram
  configuração, não teoria — ninguém no lab leu `ip route`, `ip neigh` ou `ss` explicando
  o que aquilo é
- Nenhum exercício de subnetting/CIDR foi feito e conferido no laboratório
- DNS e DHCP do lab nunca foram observados: ninguém olhou o lease do libvirt junto com o
  `ip addr` da VM nem o `resolvectl status` explicando de quem veio o servidor de nomes

## Resultado esperado

- Tabela camada → comando preenchida com saída **real** da `lab-vm` (`ip neigh`, `ip addr`,
  `ip route`, `ss`, `curl -v`), cada camada com seu trecho de saída
- `ip route` lido na VM: rota `default via <gateway>` explicada (quem é gateway, dev, proto)
- ≥ 4 exercícios de subnetting à mão, cada um com a resposta batendo no `ipcalc` ou no
  `python3`
- `getent hosts` e `dig` devolvendo o mesmo IP na VM, coerente com o IP que o `curl`
  conectou
- `resolvectl status` (VM) + `virsh net-dhcp-leases default` (host) mostrando o DHCP do
  libvirt e o resolved, lado a lado

## Requisitos

- Cada camada do OSI/TCP-IP com pelo menos um comando que a revele, rodando **dentro** da
  `lab-vm` (mapa colado com saída real, não decoreba de slide)
- Subnetting escrito à mão **antes** de qualquer conferência automática — `ipcalc`/python
  só depois, para comparar (senão o exercício não é exercício)
- Ao menos 4 exercícios, incluindo obrigatoriamente `192.168.1.0/26` e a divisão de um /24
  em 4 subnets
- Leitura de rede sempre em par: `ip addr` **e** `ip route` (endereço sozinho não explica
  como o pacote sai)
- DNS demonstrado por dois caminhos (`getent hosts` e `dig`) e DHCP por dois lados (lease
  no host, endereço dentro da VM, batendo entre si)
- Nenhuma VM nova, nenhum serviço novo, nenhuma regra de firewall nova — `ufw status` segue
  `22,80,443`
- Pacote instalado, se for o caso, só se for de leitura (`ipcalc`, `dnsutils`), e isso
  declarado em `Limitações / notas`

## Critérios de aceitação

- [ ] Pré-condição: `virsh list --all` → `lab-vm` `running` **e** `ssh lab@<ip-da-vm> ip addr`
      → saída com IP/máscara — sem os dois, pare aqui
- [ ] `ip addr` + `ip route` lidos na VM: saída colada com o IP/CIDR da interface e a rota
      `default via <gateway>` — rede, host e gateway identificados (não só copiados)
- [ ] Tabela de camadas completa, cada uma com comando e trecho de saída real: ethertype/ARP
      (`ip neigh`), IP (`ip addr`/`ip route`), TCP (`ss -tlnp` + `ss -tn` durante um `curl`
      ou `nc -zv`), HTTP (`curl -v`)
- [ ] `curl -v http://127.0.0.1/api/v1/actuator/health` → verbose com a conexão TCP e a
      resposta HTTP (`301` para `https://`); `curl -vk https://127.0.0.1/api/v1/actuator/health`
      → `200` com o handshake TLS/TCP visível no verbose
- [ ] Subnetting: ≥ 4 exercícios à mão, incluindo `192.168.1.0/26` (resposta: 64 endereços,
      62 hosts utilizáveis, broadcast `192.168.1.63`) e um /24 dividido em 4 subnets de /26;
      cada resposta **conferida** por `ipcalc` ou `python3` e batendo com o cálculo manual
- [ ] `getent hosts <nome>` e `dig +short <nome>` na VM devolvem **o mesmo IP** (resolução
      visível pelos dois caminhos)
- [ ] DHCP em dois lados: `virsh net-dhcp-leases default` no host com o lease da `lab-vm`
      batendo com o `ip addr` dela **e** `resolvectl status` na VM com o resolved ativo e
      um servidor de nomes listado
- [ ] Sem infra nova: `virsh list --all` com a mesma contagem de VMs de antes **e**
      `ufw status` → `22,80,443` (nenhuma regra nova)

## Validação

- Pré: `virsh list --all` → `lab-vm` `running`; `ssh lab@<ip> ip addr` → resposta com IP
- Camada 2/3 na VM: `ping -c1 <gateway>` e depois `ip neigh` → linha do gateway com MAC e
  estado `REACHABLE`; `ip -br addr` e `ip route` → IP/CIDR + `default via <gateway> dev ...`
- Camada 4/7 na VM: `ss -tlnp` (o que escuta) + `ss -tn` durante um `curl` (a conexão
  `ESTAB`); `nc -zv 127.0.0.1 80` → `succeeded!`; `curl -v` → conexão + `GET` + resposta
- Subnetting: entregar a folha com os 4 exercícios calculados à mão → rodar na VM
  `ipcalc 192.168.1.0/26` e `python3 -c "import ipaddress as ip; n=ip.ip_network('192.168.1.0/26'); print(n.num_addresses, len(list(n.hosts())), n.broadcast_address)"`
  → `64 62 192.168.1.63` batendo com o cálculo; repetir para os outros exercícios
- DNS na VM: `getent hosts github.com` e `dig +short github.com` → mesmo IP;
  `resolvectl status` → servidor de nomes ativo; `resolvectl query <nome>` → resolve
- DHCP: no host `virsh net-dhcp-leases default` → linha da `lab-vm`; dentro da VM `ip addr`
  → o mesmo endereço; `ufw status` → `22,80,443` no fim (nada mudou na borda)
- Registrar tudo em `## Evidências` (issue sem mudar infra: só leitura e commits)

## Evidências

- Saída de `ip -br addr`, `ip route` e `ip neigh` da VM com cada campo explicado
- Tabela camada → comando → saída preenchida, com os trechos reais (`ip neigh`, `ip addr`,
  `ip route`, `ss`, `curl -v`)
- Verbose dos dois curls: `http://` (301) e `https:// -k` (200) com handshake visível
- Os ≥ 4 exercícios de subnetting com o cálculo à mão **e** a saída de `ipcalc`/`python3`
  lado a lado (batendo)
- `getent hosts` + `dig +short` com o mesmo IP, `resolvectl status` e
  `virsh net-dhcp-leases default`
- `ufw status` final inalterado (`22,80,443`) e `virsh list --all` com a mesma contagem de VMs
- **não serve de evidência:** resposta do `ipcalc` colada sem o cálculo à mão; saída de
  `dig` rodado no host (a resolução tem de ser da VM); tabela de camadas copiada de slide
  sem saída do lab — tudo isso vai para `Limitações / notas`, nunca como prova

## Limitações / notas

- Issue de **teoria + leitura**: nada aqui muda comportamento da VM; se algum comando
  exigir mudança de config, é sinal de que saiu do escopo
- `ipcalc` e `dig` podem não estar na VM (Ubuntu Server 24.04 não traz `dnsutils` de
  fábrica) — instalar `ipcalc`/`dnsutils` é aceito por ser ferramenta de leitura, e deve
  ser registrado aqui; `ip`, `ss`, `getent` e `resolvectl` já existem
- No **loopback** (`127.0.0.1`) não existe ethertype nem ARP — para ver a camada 2 é
  preciso um destino fora da VM (o gateway `192.168.122.1`, por exemplo); declarar qual
  destino foi usado na evidência
- ARP fica só na superfície (`ip neigh`); profiling/spoofing de ARP — Área 2 (AWS)
- IPv4 apenas; IPv6, routing estático/dinâmico, NAT e NAT Gateway — Área 2 (AWS)
- Esta issue **não substitui** o firewall (T0-03) nem o proxy/TLS (T1-03): ela explica as
  camadas que aqueles dois já usam
