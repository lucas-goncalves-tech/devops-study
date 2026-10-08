---
aliases: [trilha7-02, troubleshooting-rede]
tags: [tracker, issue, todo]
status: todo
prioridade: media
---

# Issue 02 — diagnóstico de rede: porta, DNS e conectividade camada a camada

## Contexto

A Trilha 7 começou no drill de sistema (T7-01): CPU, memória, disco, processo — o que se
vê de dentro da máquina. A rede é a metade que falta, e é a que mais confunde, porque o
sintoma é sempre o mesmo ("não abre") e a causa mora em quatro lugares diferentes: o host
não tem rota, o nome não resolve, ninguém escuta na porta, ou o serviço do outro lado
caiu. Sem método, a pessoa reinicia tudo na ordem que lembra e apaga a evidência que
diria quem é o culpado — aí "conserta" por sorte e não sabe o que consertou. O lab
provoca os quatro casos sem risco: a stack da Trilha 1 (Caddy na 443, app atrás, Postgres
fechado) é o alvo real, o ufw da T0-03 é a regra que se **lê** para diagnosticar, e o DNS
vem do libvirt. É o drill que separa "a rede caiu" de "o serviço caiu" e de "o DNS caiu" —
as três respostas que custam minutos quando se sabe a árvore, e meia hora quando não.

## Objetivo

Estado final: os 4 cenários provocados, diagnosticados e **recuperados** na `lab-vm` —
porta inacessível (par `refused` × `timeout` observado lado a lado com `nc -zv`), DNS
quebrado (`dig` falhando ao lado de `getent hosts`, ordem do resolver lida com
`resolvectl status`), conectividade camada a camada (`ip link` → `ip route` → `ping` →
`traceroute`/`mtr` → `nc -zv` → `curl -v`, parando no passo certo) e serviço fora do ar
(`docker compose ps` × `curl -v` nomeando o culpado) — cada um com a prova do culpado
colada, a stack verde no fim e o ufw intocado.

## Dependências

- **Requer Trilha7-01** — é o par da mesma trilha: o método de diagnóstico camada a camada
  se aprende sobre o drill de sistema antes; sem a 01 não há sequência de drill a qual esta
  issue pertence.
- **Requer Trilha1-03** — o proxy Caddy e a stack são os alvos reais que se diagnostica —
  sem serviço escutando não há porta inacessível.
- **pré-condição verificável:** `ssh lab@<ip> 'systemctl is-active docker'` → `active`
  **e** `curl -sk -o /dev/null -w '%{http_code}' https://<ip-da-vm>/` → `200`/`3xx`
  (proxy respondendo) — as duas dependências cobertas por um comando cada.

- **estudo par:** `estudos/trilha7-02-troubleshooting-rede.md` — ler antes de executar (é o currículo desta issue)

## Escopo

- Cenário 1 — porta inacessível: subir um listener (`python3 -m http.server 8000` ou
  `nc -lk 8000`), provar a escuta com `ss -tlnp`, e diagnosticar **de fora** com
  `nc -zv`/`curl -v` os dois veredictos — `refused` (regra deixa passar, ninguém escuta) e
  `timeout` (escuta provada, pacote engolido no caminho) — fechando com recuperação: proxy
  de volta, `nc -zv` verde e `ss -tlnp` sem porta de teste sobrando
- Cenário 2 — DNS quebrado: sabotar a resolução na VM, mostrar `dig`/`nslookup` falhando
  ao lado de `getent hosts` respondendo, ler a ordem real do resolver (`/etc/hosts` →
  `nsswitch.conf` → systemd-resolved via `resolvectl status`), reverter e provar `dig`
  resolvendo de novo
- Cenário 3 — conectividade camada a camada: sequência obrigatória `ip link` → `ip route`
  → `ping` → `traceroute`/`mtr` → `nc -zv` → `curl -v` com sabotagem de rota; anotar o
  primeiro comando que falha (ele é a resposta) e devolver a rota
- Cenário 4 — serviço fora do ar: derrubar um serviço da stack e separar o que o cliente
  vê (`curl -v`) de quem caiu (`docker compose ps`); recuperação com o serviço no ar e
  `curl` voltando a `200`
- Os 4 cenários no formato **provocação → diagnóstico → recuperação**, cada um fechando
  com a prova do culpado nomeado por comando
- Leitura da régua do ufw como insumo do diagnóstico (`sudo ufw status`) — só leitura
- **assume pronto:** `proxy-na-borda` + `tls-terminado` (T1-03), `ufw` ativo com a régua
  `22,80,443` (T0-03), método de drill e sequência de diagnóstico (T7-01)
- **entrega:** `drill-rede-executado`, `refused-vs-timeout-provado`,
  `ordem-do-resolver-lida`, `sequencia-camada-a-camada`, `culpado-por-comando`

## Fora de escopo

- Troubleshooting de CPU/memória/disco e processo travado — Issue 01 desta trilha (é o
  par de drill: sistema primeiro, rede aqui)
- Firewall management e hardening (criar/remover regra, política default, fail2ban) —
  T0-03 e T4-01; aqui só se **lê** a regra existente para diagnosticar
- Captura e inspeção profunda de pacotes (Wireshark/tcpdump, análise de handshake) —
  Área 2 / estágio futuro
- Alertas, runbook e página de status — T3-04; a stack de monitoramento da T3-02 entra
  só como confirmação opcional, nunca como prova principal

## Conhecimentos envolvidos

- refused × timeout × 502: o que cada resposta diz sobre **onde** parou o caminho
- Diagnóstico em camadas: link → rota → host → porta → aplicação, nessa ordem
- Resolver: ordem de consulta em `/etc/hosts`, `nsswitch.conf` e systemd-resolved — e por
  que `dig` ignora as duas primeiras
- Escuta × caminho: `ss` prova quem ouve, `nc -zv` prova quem alcança — os dois são
  necessários
- `docker compose ps` × `curl -v`: separar a saúde do serviço da resposta da porta

## Estado atual

- A stack está no ar e só se viu funcionando: nenhum cenário de falha de rede foi
  provocado de propósito nesta VM
- `ss -tlnp`, `nc -zv`, `dig`/`getent` e a sequência camada a camada nunca foram usados
  aqui — não existe par de saída `refused` × `timeout` colado em lugar nenhum
- A diferença entre "rede caiu", "serviço caiu" e "DNS caiu" nunca foi separada por
  comando — hoje é palpite

## Resultado esperado

- Os 4 cenários com provocação, diagnóstico e recuperação registrados, cada um fechando
  com o culpado nomeado por comando
- Par colado da mesma sessão: `nc -zv` → `Connection refused` (regra deixa passar, ninguém
  escuta) × `nc -zv` → `timeout` (escuta provada, pacote não chega)
- `dig` falhando ao lado de `getent hosts` respondendo, com `nsswitch.conf` e
  `resolvectl status` explicando a ordem
- Sequência camada a camada anotada com o passo exato em que cada sabotagem parou o
  diagnóstico
- Estado final = estado inicial: `docker compose ps` healthy, `curl -sk https://<ip>/` →
  `200`/`3xx`, `sudo ufw status` idêntico ao do começo

## Requisitos

- Os 4 cenários executados na `lab-vm`, cada um no formato provocação → diagnóstico →
  recuperação, com a prova do culpado colada
- O cenário 1 produz as **duas** respostas na mesma sessão (`refused` e `timeout`), com a
  causa de cada uma identificada por comando (`ss -tlnp` e `sudo ufw status`), não por
  suposição
- O cenário 2 separa resolução de nome de alcance de host: `dig`/`nslookup` e `getent
  hosts` discordando na mesma sessão, com a ordem do resolver **lida na máquina**
- O cenário 3 percorre a sequência na ordem, sem pular passo — o primeiro comando que
  falha é a resposta do diagnóstico
- O cenário 4 nomeia o culpado com o par `docker compose ps` × `curl -v`: o que o cliente
  recebe e o que o estado mostra
- Toda sabotagem é revertida: o drill termina com a stack saudável como começou
- Nenhuma regra do ufw é criada, alterada ou removida — apenas lida

## Critérios de aceitação

- [ ] Pré-condição: `ssh lab@<ip> 'systemctl is-active docker'` → `active` (T7-01) **e**
      `curl -sk -o /dev/null -w '%{http_code}' https://<ip-da-vm>/` → `200`/`3xx` (proxy
      T1-03 respondendo) — sem os dois, pare aqui
- [ ] Cenário 1: `ss -tlnp` mostra o listener de teste escutando (ex.: `:8000`) e, do
      **host**, `nc -zvw3 <ip> 443` com o proxy parado → `Connection refused`; com o proxy
      de pé, `nc -zvw3 <ip> 8000` (escuta provada, porta fora da régua) → `timeout` —
      as duas saídas coladas lado a lado
- [ ] Cenário 2: na mesma sessão, `dig +short <nome externo>` (ou `nslookup`) falhando
      **e** `getent hosts <nome-de-/etc/hosts>` respondendo, com `grep ^hosts
      /etc/nsswitch.conf` e `resolvectl status` colados
- [ ] Cenário 3: sequência registrada por inteiro — `ip -br link`, `ip route`,
      `ping -c2`, `traceroute`/`mtr`, `nc -zv`, `curl -v` — com a sabotagem de rota
      parando o diagnóstico em `ip route`/`ping` e a recuperação (`ip route add default
      via <gw>`) devolvendo `ping` e `curl`
- [ ] Cenário 4: com um serviço da stack parado, `curl -v https://<ip>/...` mostra o que
      o cliente recebe e `docker compose ps` mostra o serviço `Exit` — os dois colados
      apontando o mesmo culpado
- [ ] Recuperação comprovada nos 4 cenários: `nc -zvw3 <ip> 443` → `succeeded`, `dig`
      resolvendo, `ping`/`curl` verdes e `docker compose ps` com a stack healthy
- [ ] `sudo ufw status` no fim idêntico ao do começo (`22,80,443`) — o drill lê a regra,
      não mexe nela

## Validação

- Checagem inicial (as duas dependências): `ssh lab@<ip> 'systemctl is-active docker'` →
  `active`; `curl -sk -o /dev/null -w '%{http_code}' https://<ip-da-vm>/` → `200`/`3xx`
- Cenário 1 — na VM: `python3 -m http.server 8000 &` → `ss -tlnp | grep :8000` →
  `LISTEN 0.0.0.0:8000`; `sudo ufw status` → `8000` fora da régua. No **host**:
  `nc -zvw3 <ip> 8000` → `timeout`. Depois: `docker compose stop proxy` → no host
  `nc -zvw3 <ip> 443` → `Connection refused`; `docker compose start proxy` →
  `nc -zvw3 <ip> 443` → `succeeded` e `curl -sk -o /dev/null -w '%{http_code}'
  https://<ip>/` → `200`/`3xx`; encerrar o listener de teste
- Cenário 2 — capturar `resolvectl status` **antes** (saudável) → sabotagem (parar o
  resolved, ou plantar linha errada em `/etc/hosts`) → `dig +short <nome externo>` falha,
  `getent hosts <nome-de-/etc/hosts>` responde, `grep ^hosts /etc/nsswitch.conf` mostra a
  ordem → reverter → `dig +short <nome externo>` volta a resolver
- Cenário 3 — `ip route` anotando o gateway → `sudo ip route del default` → `ip -br link`
  segue `UP`, `ip route` sem default, `ping -c2 -W2 1.1.1.1` → `Network is unreachable`,
  `traceroute -n -m 8 1.1.1.1` não sai do primeiro salto → `sudo ip route add default via
  <gw>` → `ping` volta e `curl -vsk https://<ip>/` segue verde
- Cenário 4 — `docker compose stop app` → `curl -vsk https://<ip>/api/v1/actuator/health`
  (o cliente vê a resposta do proxy) + `docker compose ps` (o app `Exit`) → `docker
  compose start app` → `curl -sk` → `200`
- Fechamento: `docker compose ps` healthy, `curl -sk .../` → `200`/`3xx`, `ss -tlnp` sem
  porta de teste sobrando, `sudo ufw status` idêntico ao do começo

## Evidências

- O par do cenário 1 com as duas saídas coladas: `Connection refused` × `timeout`, na
  mesma sessão que o `ss -tlnp` do listener e o `sudo ufw status` da régua
- `dig`/`nslookup` falhando ao lado de `getent hosts` respondendo, mais
  `nsswitch.conf`/`resolvectl status`
- A sequência camada a camada com o passo que falhou em cada sabotagem e a recuperação da
  rota (antes/depois)
- O par do cenário 4: `curl -v` (resposta do cliente) × `docker compose ps` (`Exit`) e a
  recuperação voltando a `200`
- Fechamento: `docker compose ps` healthy + `curl -sk` → `200`/`3xx` + `sudo ufw status`
  inalterado + `ss -tlnp` limpo

## Limitações / notas

- `timeout` é a resposta mais fraca do diagnóstico: ela diz "não chegou", não **quem**
  engoliu — por isso `sudo ufw status` (leitura) fecha a acusação; sem a régua lida, a
  conclusão fica em hipótese e vira chute
- Os testes de fora vêm do host pela rede NAT do libvirt: é esse caminho que se mede.
  Testar de dentro da VM (`nc localhost`) não passa pela régua e mente sobre o firewall —
  mesma lição da T0-03 (testar de fora)
- O DNS do lab vem do libvirt: se a sabotagem parar o resolved, `resolvectl status` pode
  ficar indisponível **durante** ela — capturar o estado saudável antes (o antes/depois é
  a evidência)
- Sabotagem de rota (`ip route del default`) derruba só o acesso externo da VM; o SSH do
  host segue (rede local direta). Se a sessão cair mesmo assim, o recovery é pelo console
  `virsh console lab-vm` — anotar aqui se acontecer
- Confirmação pelo monitoramento (T3-02) é opcional: o Prometheus confirma o que os
  comandos já provaram, não substitui o diagnóstico nem entra como critério
- Wireshark/tcpdump ficam de fora de propósito: aqui se treina a árvore de decisão com
  saída de comando barato; inspeção de pacote é estágio futuro (Área 2)
