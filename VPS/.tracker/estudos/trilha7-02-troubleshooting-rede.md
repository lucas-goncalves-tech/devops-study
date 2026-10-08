# Estudo — diagnóstico de rede: porta, DNS e conectividade camada a camada

> Material de estudo da Trilha 7. Acompanha a Issue 02 (drill de rede), mas não é o
> contrato daquela Issue: aqui é para entender, lá é para provar.

## Recusado ou sem resposta: as duas respostas do nc acusam suspeitos diferentes

"Não conecta" é o sintoma mais mentiroso do troubleshooting: ele descreve o que a pessoa
viu, não o que aconteceu. O `nc -zv` devolve pelo menos quatro respostas diferentes, e
cada uma acusa um suspeito diferente — quem lê todas como "deu erro" fica reiniciando
serviço que está saudável.

- **Por que importa:** a resposta é o único dado que separa três culpas caras. `refused`
  com serviço parado leva 30 segundos de diagnóstico; `timeout` sem saber ler régua leva
  20 minutos de `docker compose restart`, `ufw reload` e palpite — e no fim a pessoa
  "consertou" destruindo a evidência (o estado que mostrava quem era o culpado).
- **Mecanismo:** o que muda é **onde o pacote parou** e **se alguém respondeu**:

| Resposta do cliente | O que aconteceu no caminho | Culpado provável | Comando que fecha a acusação |
|---|---|---|---|
| `Connection refused` | o SYN chegou ao destino e o kernel respondeu RST: **ninguém escuta** naquela porta | serviço parado / porta errada | `ss -tlnp` (sem a escuta), `docker compose ps` |
| `timeout` (silêncio) | o pacote foi **engolido** no caminho: régua com drop, rota ausente, host fora | caminho: régua ou rota | `sudo ufw status`, `ip route` |
| `No route to host` | nem saiu: a própria máquina disse que não tem para onde ir | rota local | `ip route` |
| TCP ok + erro HTTP (`502`/`503`) | a conexão **funcionou**; quem respondeu contou uma falha do outro lado | aplicação/serviço atrás da porta | `curl -v`, `docker compose ps` |

  A régua do ufw explica por que `refused` e `timeout` aparecem em portas diferentes do
  mesmo lab: a política default é **deny incoming com drop** (pacote some → `timeout`),
  e uma porta na régua (`22,80,443`) alcança o kernel, que responde RST quando não há
  escuta (`refused`). Ou seja: `refused` = "cheguei e me mandaram embora"; `timeout` =
  "ninguém nem viu chegar".
- **Exemplo no lab:** o cenário 1 da Issue 02 monta os dois veredictos lado a lado, na
  mesma sessão, sem tocar no ufw:

```bash
python3 -m http.server 8000 &        # na VM: escuta provada (alternativa: nc -lk 8000)
ss -tlnp | grep :8000                # LISTEN 0.0.0.0:8000
sudo ufw status                      # 8000 fora da régua → drop

nc -zvw3 <ip> 8000                   # no host: → timeout (escuta existe, régua engole)
docker compose stop proxy            # provocação: porta da régua sem escuta
nc -zvw3 <ip> 443                    # no host: → Connection refused
docker compose start proxy           # recuperação
nc -zvw3 <ip> 443                    # no host: → succeeded
```

  E o `curl -v` é quem dá a quarta resposta, porque ele conta a camada de aplicação:

```text
* connect to <ip> port 443: Connection refused      ← recusado: nada escuta
* Connected to <ip> (<ip>) port 443                 ← TCP e TLS ok
< HTTP/1.1 502 Bad Gateway                          ← a porta está fina; o outro lado caiu
```

- **Fronteira entre Issues:** **ler a régua e decidir o que fazer com ela** (criar regra,
  política default, hardening) é T0-03 e T4-01 — aqui a regra só é **lida** como prova;
  **furar a camada de captura** (ver os SYN no wireshark, por que o RST tem aquele TTL) é
  Área 2, estágio futuro; **alertar** quando isso vira sintoma recorrente é T3-03/T3-04.
  O que esta Issue entrega é o par de saídas e a leitura delas.

## A sequência camada a camada: o passo que para é a resposta

Diagnóstico de rede não é listar comandos, é uma **árvore de decisão**: a cada pergunta,
o "não" já é a resposta e corta metade da árvore. Pular passo não deixa mais rápido —
deixa caro, porque a pessoa passa a testar o fim da cadeia (curl) sem saber se o começo
(ip route) existe, e cada teste negativo vira uma nova hipótese.

- **Por que importa:** quem começa pelo `curl` e vê `timeout` não sabe se é DNS, rota,
  régua ou serviço — o sintoma é idêntico nos quatro casos. A sequência resolve isso
  invertendo o custo: cada passo custa menos de um segundo e **só** o primeiro que falha
  importa. Pular direto ao `tcpdump` ou ao restart é pagar o preço do passo 6 para
  responder a pergunta do passo 2.
- **Mecanismo:** a ordem é da camada mais barata para a mais cara, do cabo para a
  aplicação — e o par "comando → o que ele prova" é fixo:

| # | Comando | O que prova | Se falhar, o culpado é |
|---|---|---|---|
| 1 | `ip -br link` | a placa está `UP` e com link | enlace / VM / container |
| 2 | `ip route` | existe rota para o destino (o `default`) | rota |
| 3 | `ping -c2 -W2` | o host responde no caminho (vizinho e externo) | enlace ou rota no meio |
| 4 | `traceroute -n` / `mtr -rw` | **onde** para (último hop antes do silêncio) | o salto seguinte |
| 5 | `nc -zv` | a porta TCP aceita conexão | régua (timeout) ou escuta (refused) |
| 6 | `curl -v` | a aplicação responde HTTP | serviço do outro lado, ou DNS antes dele |

  Duas armadilhas da ordem: (a) **DNS entra antes do HTTP, não dentro dele** — se `ping
  1.1.1.1` funciona e `curl https://nome` não, o caminho está de pé e o problema é o nome
  (é o cenário 2); (b) `ping` e `curl` **não** são intercambiáveis — ICMP pode ser
  bloqueado no meio do caminho enquanto o TCP passa, então `ping` falhando não fecha o
  caso sozinho, ele só manda a investigação para `ip route`.
- **Exemplo no lab:** a sabotagem do cenário 3 é tirar a rota padrão e ver a sequência
  parar exatamente onde deve:

```bash
ip route                                  # ANOTAR o gateway (192.168.122.1) antes
sudo ip route del default                 # provocação

ip -br link                               # UP → passo 1 ok
ip route                                  # sem default → PAROU AQUI (passo 2)
ping -c2 -W2 1.1.1.1                      # Network is unreachable
traceroute -n -m 8 1.1.1.1                # não sai do primeiro salto
getent hosts ubuntu.com                   # AINDA resolve: DNS do libvirt é rede local
nc -zvw3 <ip> 443                         # porta da VM segue respondendo (rede local)

sudo ip route add default via 192.168.122.1   # recuperação
ping -c2 -W2 1.1.1.1                      # volta
curl -vsk https://<ip>/                   # verde de novo
```

  Repare no detalhe que ensina mais que o resto: com a rota morta, **o SSH e o DNS
  continuaram** (tudo é rede local do libvirt) e só o externo caiu — quem testa só `curl`
  concluiria "rede caiu", e quem testa só o SSH nem perceberia. O passo 2 é que separa.
- **Fronteira entre Issues:** **o método de drill e a disciplina de diagnóstico** (custo
  do comando, ordem, registrar o que descartou) são T7-01 — esta Issue aplica a mesma
  disciplina na camada de rede; **diagnóstico de sistema** (CPU, disco, OOM) é a Issue 01
  da trilha; **o que fazer depois de achar** (mitigação escrita, escalação) é T3-04. Aqui
  se entrega a sequência executada e o passo que parou.

## DNS: quem responde primeiro (e por que o dig pula a fila)

"Não abre" quase sempre vira "DNS caiu" como palpite — e o palpite esconde que existem
**dois caminhos de resolução** na mesma máquina, consultados em ordem fixa, e que as
ferramentas não concordam entre si porque cada uma usa um caminho diferente.

- **Por que importa:** sem entender a ordem, o diagnóstico de DNS vira chutar
  `/etc/resolv.conf` (a correção mais comum e mais errada) e culpar o provedor. Com a
  ordem na mão, duas saídas que se contradizem (`dig` falha, `getent` responde) viram a
  prova exata de **qual caminho** quebrou.
- **Mecanismo:** o `nsswitch.conf` diz **quem** o libc pergunta, e em que ordem:

```text
/etc/nsswitch.conf →  hosts: files systemd
                        │        │
                        │        └─ systemd-resolved (stub 127.0.0.53) → upstream
                        │           do lab: 192.168.122.1 (dnsmasq do libvirt)
                        └─ /etc/hosts  (responde na hora, sem rede)
```

  E a parte que quase ninguém sabe: **`dig` e `nslookup` ignoram o `nsswitch` e o
  `/etc/hosts`** — eles falam direto com o servidor listado no `/etc/resolv.conf`. Já
  `getent hosts`, `curl`, `ssh` e `ping` passam pelo libc e **honram** os dois. Por isso
  o par de ferramentas discorda, e a discordância é o diagnóstico:

| Situação | `dig nome` | `getent hosts nome` | Leitura |
|---|---|---|---|
| tudo saudável | resolve | resolve | nada a fazer |
| resolved parado | falha (`127.0.0.53` recusa) | só responde o que está em `files` | o resolver morreu; `/etc/hosts` segue vivo |
| linha errada em `/etc/hosts` | resolve certo | devolve o IP **errado** | o `files` engoliu o nome |
| upstream do libvirt fora | falha | resolve só `files` | o problema é do lado do host/lab, não da VM |

- **Exemplo no lab:** provocar e diagnosticar o DNS na `lab-vm`, sempre capturando o
  estado saudável **antes** (ele é metade da prova):

```bash
grep ^hosts /etc/nsswitch.conf        # ANTES: hosts: files systemd
resolvectl status | grep -iE 'dns|server'   # ANTES: upstream 192.168.122.1 (libvirt)
dig +short ubuntu.com                 # ANTES: resolve

sudo systemctl stop systemd-resolved  # provocação: matar só o resolved

dig +short ubuntu.com                 # falha: connection refused a 127.0.0.53
getent hosts localhost                # AINDA 127.0.0.1 → veio do files, não do DNS
resolvectl status                     # erro → confirma quem parou

sudo systemctl start systemd-resolved # recuperação
dig +short ubuntu.com                 # volta a resolver
```

  Se `dig` não existir na VM, `sudo apt install -y dnsutils` (ou usar `nslookup`/
  `resolvectl query`) — instalar ferramenta de leitura não muda o estado do lab.
- **Fronteira entre Issues:** **gerenciar** DNS (criar zona, mexer no dnsmasq do libvirt,
  gravar hosts permanente) não é desta issue; **DNS interno do compose** (o nome `app`
  entre serviços) é T1-02; **nome/upstream do proxy** é T1-03. O que aqui se entrega é a
  ordem lida na máquina e o par `dig` × `getent` como prova.

## Porta aberta não é porta alcançável: ss prova a escuta, nc prova o caminho

A confusão mais cara do cenário de porta é tratar `ss -tlnp` como "a porta está aberta".
Ele é uma leitura **de dentro** da máquina: prova que um processo chamou `bind` e
`listen`. Não prova ninguém conseguir chegar — e o teste de dentro, por ser loopback, nem
passa pela régua.

- **Por que importa:** as duas metades têm donos diferentes. Uma escuta ótima com régua
  errada é "ninguém alcança" (o cenário 1); um caminho perfeito sem escuta é "recusado".
  Só lendo uma das duas se chega a conclusão errada — e a conclusão errada vira restart à
  toa no serviço inocente.
- **Mecanismo:** cada leitura nasce em um lugar e prova uma metade:

| Leitura | Feita onde | Prova | Não prova |
|---|---|---|---|
| `ss -tlnp` | dentro da VM | quem escuta, em qual IP:porta e qual processo | que alguém alcança |
| `nc -zv` | de fora (host) | o caminho inteiro até a porta responde | **quem** escuta (pode ser o `docker-proxy`!) |
| `curl -v` | de fora (host) | handshake TLS + resposta da aplicação | saúde interna do serviço |

  O terceiro detalhe é o que separa sênior de júnior: com porta publicada, quem aceita o
  TCP pode ser o `docker-proxy` da host — `nc -zv` diz `succeeded` mesmo com o container
  morto. Por isso `succeeded` **não** é "funciona": é só o passo 5. O `curl -v` (passo 6)
  é que mostra se o serviço respondeu.
- **Exemplo no lab:** o drill monta o par explícito, com os dois lados obrigatórios:

```bash
ss -tlnp | grep :8000                 # NA VM: LISTEN 0.0.0.0:8000 ← escuta existe
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8000/   # NA VM: 200
sudo ufw status                       # NA VM: 8000 fora da régua

nc -zvw3 <ip> 8000                    # NO HOST: timeout ← a régua corta, a escuta é inocente
nc -zvw3 <ip> 443                     # NO HOST: succeeded ← caminho e escuta existem
curl -vsk https://<ip>/               # NO HOST: e aí sim 200/3xx, ou 502, ou refused
```

  O trio acima é a prova completa: escuta OK + caminho bloqueado = o culpado é a régua
  (lida, não mexida). E o teste `nc localhost` dentro da VM, que sempre "funciona", não
  conta: loopback não passa por regra nenhuma — mesma lição da T0-03 de testar de fora.
- **Fronteira entre Issues:** **escrever/alterar a régua** é T0-03 e T4-01; **publicar
  porta no compose** é T1-02/T1-03; **medir** o resultado (latência, up) é T3-02. Esta
  Issue entrega o par de leituras obrigatório e a recusa em aceitar uma sozinha.

## Quando o culpado é o serviço: 502, refused e a leitura do docker compose ps

A última fronteira do drill: a porta responde, a rede está fina, e mesmo assim o usuário
não vê nada. Nesse caso **ninguém de rede caiu** — caiu o serviço do outro lado da porta,
e o diagnóstico certo é provar isso com o par "o que o cliente recebe" × "o que o estado
mostra".

- **Por que importa:** é o erro que faz a pessoa desconfiar da própria infraestrutura
  ("reinicia o proxy", "reinicia o firewall") enquanto o culpado está parado em `Exit (137)`
  há dez minutos. Também é o oposto do erro anterior: aqui a culpa é da **aplicação**, e
  chamar o time de rede é o atraso mais comum de atendimento.
- **Mecanismo:** o `curl -v` entrega a pista em duas linhas — se o TCP e o TLS passaram,
  a rede fez o trabalho dela; o que veio depois (502/503) é o proxy contando que o
  upstream não respondeu. E o contraste entre os dois erros fecha a árvore:

| O cliente vê | Porta | O que isso acusa |
|---|---|---|
| `Connection refused` | nem abre | ninguém escuta: serviço **deste** lado parado |
| `502`/`503` com TLS ok | abre e responde | serviço **do outro lado** da porta caiu |
| `timeout` | silêncio | ninguém chega: caminho (régua/rota), nem serviço |
| `200` mas conteúdo errado | ok | serviço vivo — assunto de aplicação, não de rede |

  Quem só olha `docker compose ps` vê a stack inteira e não sabe qual importa; quem
  só olha o `curl` vê "502" e não sabe quem caiu. O par é obrigatório justamente porque
  cada um responde uma pergunta diferente.
- **Exemplo no lab:** provocação, acusação e recuperação do cenário 4:

```bash
docker compose stop app                # provocação: o serviço do outro lado da porta

curl -vsk https://<ip>/api/v1/actuator/health
* Connected to <ip> (<ip>) port 443 (#0)          ← rede e TLS OK
* successfully set certificate verify locations...
< HTTP/1.1 502 Bad Gateway                         ← o proxy respondeu contando a falha

docker compose ps                      # app  Exited (137)  ← o culpado, nomeado

docker compose start app               # recuperação
curl -sk -o /dev/null -w '%{http_code}\n' https://<ip>/api/v1/actuator/health   # 200
docker compose ps                      # todos healthy
```

  Repare que no cenário 4 a porta **nunca** parou de responder — é exatamente isso que
  distingue "serviço caiu" de "porta inacessível" (cenário 1, onde o `nc` dava `refused`
  ou `timeout`). Mesmo sintoma para o usuário, dois culpados, dois comandos.
- **Fronteira entre Issues:** **saudabilidade interna da app** (healthcheck, o `200` do
  actuator) é T1-02/T1-03; **log do serviço** para achar por que ele morreu é T3-01;
  **alertar** quando ele cai é T3-03 e **o procedimento escrito** é T3-04; CPU/disco que
  derrubam serviço é Issue 01 desta trilha. Esta Issue entrega a acusação pelo par
  `docker compose ps` × `curl -v` e a recuperação comprovada. Confirmação opcional pelo
  monitoramento (T3-02): se o Prometheus mostrar `up == 0` na mesma janela, ele **confirma**
  o diagnóstico — nunca substitui os comandos.

## Como iniciar o modo teach-anything

- "Me ensina refused vs timeout com os dois `nc -zv` desta VM: o `443` sem proxy e o
  `8000` com `ss -tlnp` provando a escuta fora da régua"
- "Me ensina a sequência camada a camada `ip link` → `ip route` → `ping` → `traceroute` →
  `nc -zv` → `curl -v` usando a rota do libvirt e a sabotagem `ip route del default`"
- "Me ensina a ordem do resolver lendo `grep ^hosts /etc/nsswitch.conf` e
  `resolvectl status` desta VM, e por que o `dig` ignora o `/etc/hosts`"
- "Me ensina por que `ss -tlnp` sozinho não prova alcançabilidade, com o listener
  `python3 -m http.server 8000` e o teste de fora vindo do host"
- "Me ensina a separar `502` de `Connection refused` com `curl -v` e `docker compose ps`
  na stack do Caddy, e o que cada resposta acusa"
- "Me ensina a árvore de decisão que separa 'rede caiu', 'serviço caiu' e 'DNS caiu' com
  os quatro cenários desta issue"
