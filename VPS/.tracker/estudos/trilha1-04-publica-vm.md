# Estudo — Publicação: a stack sai do laptop e passa a viver na `lab-vm`

> Material de estudo da Trilha 1. Acompanha a Issue 04 (publicação na VM), mas não é o
> contrato daquela Issue: aqui é para entender, lá é para provar.

## No servidor o Docker se instala pelo repo oficial — o snap muda os caminhos e o dono do daemon

Até aqui o Docker só existia no laptop, instalado "de um jeito" e nunca mais questionado.
Na VM o cálculo muda: lá o daemon é um **serviço de servidor** — ele precisa voltar sozinho
depois do reboot, com caminhos que os guias, os scripts e os seus próprios comandos futuros
assumem. Instalar pelo caminho errado não falha hoje: a stack até sobe. O problema aparece
depois, quando `du -h /var/lib/docker` vem vazio, `/etc/docker/daemon.json` é ignorado ou o
daemon reinicia no meio de um `compose up`.

- **Por que importa:** existem quatro jeitos de dizer "docker instalado" nesta VM e três
  deles quebram a operação sem nunca dar erro na instalação. O snap é o mais traiçoeiro
  porque funciona — até o primeiro guia que assume os caminhos padrão.
- **Mecanismo:** cada forma de instalar decide **quem cuida do daemon** e **onde ele mora**:

| Instalação | Quem cuida do daemon | Onde ficam dados/config | O que dói no servidor |
|---|---|---|---|
| Docker Engine (repo oficial) | systemd (`systemctl enable docker`) | `/var/lib/docker`, `/etc/docker/daemon.json` | nenhum — é o que os guias assumem |
| `docker.io` (universe do Ubuntu) | systemd | idem | versão presa ao ciclo do Ubuntu; `apt policy docker-ce` nem existe |
| snap `docker` | snapd, com auto-refresh | `/var/snap/docker/common/var/lib/docker`, config em `/var/snap/docker/current/config/daemon.json` | scripts e guias apontam pro lugar errado; o refresh do snap reinicia o daemon por conta própria |
| Docker Desktop | aplicativo de laptop (com VM própria embutida; licença corporativa) | dentro da VM do Desktop | num servidor não há GUI logada — o engine não é um serviço que o systemd traz de volta no boot |

A sequência oficial (repo `download.docker.com`) é a única que entrega os pacotes que os
próximos comandos e o CI da Trilha 2 vão citar: `docker-ce`, `docker-ce-cli`,
`containerd.io`, `docker-buildx-plugin` e `docker-compose-plugin` (o `docker compose` v2,
sem hífen — a Issue 04 exige ≥ 2).

```bash
# na lab-vm — repo oficial do Docker para Ubuntu
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update && sudo apt-get install -y docker-ce docker-ce-cli containerd.io \
  docker-buildx-plugin docker-compose-plugin
```

```bash
# prova de que o caminho foi o oficial
apt policy docker-ce                     # origin: download.docker.com, não o universe
command -v docker                        # /usr/bin/docker (o snap deixaria /snap/bin/docker)
snap list docker                         # → erro: não instalado
docker compose version                   # Docker Compose version v2.x
systemctl is-enabled docker              # enabled → volta sozinho no reboot (Trilha 0-04)
sudo usermod -aG docker lab              # operar sem sudo no dia a dia; vale no próximo login
```

- **Exemplo no lab:** em `lab-vm`, o estado procurado é `docker-ce` vindo do repo oficial,
  `lab` no grupo `docker` (mesmo princípio do usuário não-root da Trilha 0: quem opera não
  é root, mas note que **grupo `docker` é root na prática** — quem tem o socket manda em tudo;
  a Issue 04 aceita isso para não ter `sudo` em cada comando) e `systemctl is-active docker`
  → `active`. O snap jamais deve aparecer em `snap list` nesta VM.
- **Fronteira entre Issues:** **instalar** o Engine nesta Issue; **atualizar** o Docker e
  renovar esse repo de terceiro é outra coisa — o `unattended-upgrades` da Trilha 0-03 cobre
  só as seeds padrão do Ubuntu, e a Issue 03 deixou isso anotado justamente por causa do
  repo do Docker. Docker Desktop e Kubernetes/swarm não entram em lugar nenhum do lab.

## Contexto remoto ou build dentro: viaja código ou viaja bytes

A stack existe só no laptop; a VM está ociosa (`ss` sem nenhuma porta de serviço). Levar a
stack é decidir **o que cruza o cabo** — e a Issue 04 exige que o caminho seja declarado e
justificado, porque cada um entrega uma prova diferente.

- **Por que importa:** escolher no automático produz evidência fraca. Build no laptop e
  `docker load` na VM passam nos testes de superfície, mas a imagem nasceu em outro lugar,
  com outro cache e — se as arquiteturas divergirem — nem roda: o `docker load` aceita
  qualquer binário e o container explode depois com `exec format error`.
- **Mecanismo:** o `docker context` não move seu terminal: ele aponta o **cliente** para a
  API do daemon de outra máquina, através de um túnel SSH. Tudo que se digita com
  `--context labvm` executa lá — inclusive o build, que sempre roda no daemon. O que viaja
  pelo cabo é o **contexto de build** (o repo, como tar) e o `compose.yaml`, lido no laptop
  e enviado ao daemon remoto.

| Caminho | O que viaja | Quem builda | O que se ganha / perde |
|---|---|---|---|
| A — `docker context` remoto | repo (tar) + `compose.yaml` pelo SSH | daemon da VM | reproduzível no alvo; depende do cliente rodando no laptop |
| B — repo copiado (`rsync`) + build dentro | repo para `~/lab` | daemon da VM | reproduzível **e** sobrevive à sessão SSH; paga a RAM da VM |
| C — `docker save` \| `docker load` | imagem binária | daemon do laptop | rápido e cabe em 2 GB; o build não aconteceu no alvo |

Os dois primeiros são o mesmo efeito com dependência diferente: em A o laptop precisa estar
ligado e com o contexto certo; em B a VM tem o código e qualquer SSH continua de onde parou
— é o fluxo que o CI da Trilha 2 vai imitar (clone + build no alvo). O C só se aceita como
**limitação declarada** (Issue 04: VM com 2 GB e Maven reclamando), nunca em silêncio.

```bash
# Caminho A — orquestrar de fora (build roda na VM, código viaja como tar)
docker context create labvm --docker host=ssh://lab@<ip-da-vm>
docker context ls                        # labvm aparece; use `docker context rm` pra desfazer
docker --context labvm compose up -d --build
docker --context labvm compose ps        # os 4 serviços, listados de dentro da VM

# Caminho B — repo dentro da VM, build na VM (escolha declarada na evidência)
rsync -a --exclude target --exclude .git ./ lab@<ip-da-vm>:~/lab/   # o .env vai junto (ver tópico 4)
ssh lab@<ip-da-vm>
cd ~/lab && docker compose up -d --build
docker compose ps                        # db, redis, app, proxy — 4 healthy na VM

# Caminho C — só como limitação declarada (VM sem RAM pro build)
docker save notes-api | ssh lab@<ip-da-vm> 'docker load'   # imagem viaja, assinatura de build não
```

- **Exemplo no lab:** o que a evidência precisa mostrar é `docker compose ps` com os 4
  serviços healthy **na VM** (o `docker --context labvm compose ps` ou o mesmo comando
  rodando dentro do SSH — os dois provam o mesmo), mais uma linha dizendo qual dos caminhos
  foi usado e por quê. Se a VM não aguentar o Maven, o C entra escrito como limitação, com
  a troca "imagem binária viaja × build reproduzível" explícita.
- **Fronteira entre Issues:** registry próprio, push de imagem e deploy automático são a
  **Trilha 2** — lá a imagem deixa de viajar por cabo e passa a ter dono e digest. Migrar
  volumes/dados de host para VM está fora do escopo (lab novo, volume novo). E `docker
  context` é configuração do cliente no seu laptop (`ls`, `use`, `rm`) — não é infra da VM.

## Abrir 80/443 é literalmente a mesma ação da Trilha 0-03, com motivo novo

O proxy da Trilha 1-03 publica `80:80` e `443:443` — e a primeira coisa que dá vontade de
fazer quando o `curl` de fora falha é `ufw disable` "só pra testar". Esse comando desfaz em
uma linha todo o trabalho da Trilha 0-03, inclusive a regra da 22, e não deixa rastro de que
foi desligado. Firewall se muda por **regra**, sempre: `allow`, `deny`, `delete` — `disable`
não é um passo deste lab.

- **Por que importa:** o custo do atalho é invisível no teste e total no incidente: com o
  ufw fora, a 5432 do banco (se um dia for publicada por engano) responde direto, e ninguém
  lembra de religar a régua depois do `curl` verde.
- **Mecanismo:** mesma cadeia da Trilha 0-03 — default deny, exceção explícita, e o estado
  é legível numa tela que vira evidência. O motivo novo é o proxy: 80/443 passam a ser a
  única superfície de serviço, e 5432/6379 continuam fechadas.

```bash
ssh lab@<ip-da-vm>
sudo ufw allow 80/tcp           # mesma ação da Trilha 0-03 (naquela vez: 22)
sudo ufw allow 443/tcp
sudo ufw status numbered        # → 22, 80, 443 — exatamente 3 regras ALLOW
```

```bash
# prova de fora, no host — mesma origem, portas diferentes
curl -k -o /dev/null -w '%{http_code}\n' https://<ip-da-vm>/api/v1/actuator/health  # 200
nc -zv <ip-da-vm> 80            # succeeded — a regra existe
nc -zv <ip-da-vm> 5432          # recusado — o db continua atrás do deny
nc -zv <ip-da-vm> 6379          # recusado — idem
```

Honestidade de mecanismo, para ninguém treinar no errado: o que o **Docker** publica é
encaminhado pela cadeia `DOCKER` que o próprio daemon cria no netfilter — esse tráfego mal
passa pela cadeia `INPUT` que o ufw regra. Ou seja: quem faz a 80/443 **existir** é o
`ports:` do serviço `proxy` (Trilha 1-03); quem faz a 5432 **não existir** é a ausência de
`ports:` no serviço `db` (Trilha 1-02). O ufw continua mandando em tudo o mais (o ssh na 22,
qualquer serviço fora do Docker) e a regra explícita 80/443 **declara a intenção** — é ela,
e não um `disable`, que a VPS pública vai herdar.

- **Exemplo no lab:** `ufw status verbose` na `lab-vm` com `22,80,443 ALLOW Anywhere` e
  `grep -c ALLOW` → `3` é a régua; o par `nc` (80 ok, 5432 recusado) é a prova de fora.
  Repetir o padrão inteiro da Trilha 0: regra explícita → teste de fora → negativo também
  testado.
- **Fronteira entre Issues:** filtrar o que o Docker encaminha (cadeia `DOCKER-USER`),
  rate limit na borda e hardening de borda (HSTS, entre outros) são Trilha 4; domínio +
  porta 80 para o Let's Encrypt é estágio futuro com VPS real; abrir 5432 para o mundo
  jamais é destas Issues.

## O `.env` chega à VM como arquivo protegido, nunca como histórico do git

O `.env` deste repo tem `DB_PASSWORD` e `JWT_SECRET` — e ele é a única cópia com valores
reais (o `.env.example` só tem os nomes). No laptop, single-user, o arquivo está hoje em
`644` (`-rw-r--r--`) e ninguém notou. Numa VM com mais gente, mais processos e backups
agendados (Trilha 0-05), o mesmo 644 vira "qualquer conta lê o segredo do app".

- **Por que importa:** segredo que entrou no git **não sai mais** — dá para remover do
  HEAD, mas o histórico continua com ele para sempre, e este repo vai ganhar remote público
  na Trilha 2 (GitHub Actions). E na VM, quem lê o `.env` é o processo do `docker compose up`
  rodando como `lab`: dono errado quebra a subida, permissão larga vaza o segredo.
- **Mecanismo:** três decisões independentes, todas causais:
  - **No git:** tracked vs. untracked. `.env` está no `.gitignore` (última linha do arquivo)
    → nunca virou commit, então `git clone` na VM **não o traz** — por design. A stack sem
    ele nem sobe (`env_file` da Issue 02), então ele viaja por cópia explícita, à parte.
  - **Na cópia:** `rsync -a` preserva modo e copia tudo que não foi excluído; `scp` traz o
    arquivo cru. Nos dois casos o modo chega como o de lá de cima — por isso o `chmod` é
    depois, na ponta.
  - **No disco:** `600` = escrita/leitura só do dono, nada para grupo e outros. Dono `lab`
    porque o leitor é o compose de `lab`; se ficar dono `root` com 600, `docker compose up`
    como `lab` morre com `permission denied`.

```bash
# host: o segredo nunca entrou no histórico
git check-ignore -v .env                # → .gitignore:...:.env   (regra que o protege)
git log --all --oneline -- .env         # → vazio: nunca houve commit com o segredo

# cópia explícita para a VM + permissão na ponta
scp .env lab@<ip-da-vm>:~/lab/.env
ssh lab@<ip-da-vm> 'chmod 600 ~/lab/.env && stat -c "%a %U" ~/lab/.env'   # → 600 lab
```

- **Exemplo no lab:** a dupla de comandos acima é a evidência da Issue 04
  (`stat` → `600 lab`) e o `git log` vazio é a prova de que o método de exclusão seguiu
  valendo. Repare no detalhe operacional: quem sobe a stack é `docker compose up` como `lab`
  lendo `~/lab/.env` — por isso `env_file` e `600 lab` têm que ser a mesma história.
- **Fronteira entre Issues:** o contrato `.env.example` com nomes e nenhum valor literal é
  da **Issue 02**; injeção de segredo (vault, `docker secrets`, nada em disco) é Trilha 4 —
  a Issue 04 é só o hábito, não a solução final (senha de laboratório fraca de propósito).
  E se um dia o backup da Trilha 0-05 guardar a VM inteira, o `.env` vai junto dentro do
  backup: segredo dentro de cópia de segurança é assunto da Trilha 4, não desta Issue.

## O terminal agora é remoto: o comando mora na VM, e o do laptop não prova nada

Durante as Issues 01–03, "abrir um terminal" significava abrir o laptop — e o Docker, o
`curl` e o `compose ps` estavam todos na mesma máquina. A partir daqui não: o terminal é
remoto, e a maior parte dos erros de publicação deixa de ser erro de Docker para virar
erro de **máquina errada**.

- **Por que importa:** com as duas stacks no ar (a do host e a da `lab-vm`, portas
  diferentes, sem conflito), o `docker compose ps` do laptop continua mostrando 4 healthy —
  **do laptop**. Um `curl localhost` igualmente. Colar essas telas como evidência da Issue 04
  é provar a máquina errada com um comando certo.
- **Mecanismo:** SSH é shell de verdade em outro filesystem e outro kernel. Cada efeito
  depende de onde o comando caiu:
  - `cd ~/lab` só existe na VM — rodou no laptop, não há o que operar;
  - sessão de um comando (`ssh host '...'`) executa e sai, ideal para evidência e scripts;
    sessão interativa é para operar; build em foreground que cai junto com a sessão morre —
    por isso `compose up -d` e o daemon (serviço do systemd **da VM**) sobrevivem a tudo;
  - o servidor não precisa de ninguém logado: `docker` habilitado no systemd +
    `restart: unless-stopped` (Trilha 0-04/Issue 02) é o que faz o reboot voltar sozinho.

```bash
# host: evidência sem abrir shell (uma frase, uma resposta)
ssh -o BatchMode=yes lab@<ip-da-vm> 'cd ~/lab && docker compose ps'
ssh -o BatchMode=yes lab@<ip-da-vm> 'sudo ufw status | head -1'     # pré-condição: Status: active

# host: operar (sessão interativa) — tudo a partir daqui é "lá"
ssh lab@<ip-da-vm>
cd ~/lab && docker compose up -d

# host: a prova de que a máquina testada é a certa — mesmo caminho, dois alvos
curl -k -o /dev/null -w '%{http_code}\n' https://127.0.0.1/api/v1/actuator/health   # stack do host (ERRADA como evidência)
curl -k -o /dev/null -w '%{http_code}\n' https://<ip-da-vm>/api/v1/actuator/health  # a VM (a evidência da Issue)

# host: reboot e volta sem mão nenhum
ssh lab@<ip-da-vm> 'sudo reboot'
until ssh -o BatchMode=yes lab@<ip-da-vm> 'docker compose ps'; do sleep 3; done      # 4 healthy
```

- **Exemplo no lab:** o IP da `lab-vm` vem do lease DHCP da rede NAT do libvirt e **pode
  mudar** (Trilha 0-01) — antes de operar, `virsh net-dhcp-leases default` para saber qual
  `<ip-da-vm>` está valendo hoje; é o endereço que todo `ssh`, `curl` e `nc` deste estudo
  usa. E o teste de exposição tem fronteira geográfica: o `curl` do host para `<ip-da-vm>`
  só prova a rede local (`virbr0`, `192.168.122.1/24`, NAT — o mundo não alcança a VM de
  fora). Isso **não** é defeito do teste: é o lab. O que importa é que a configuração já
  está certa para quando houver IP roteável — 3 regras explícitas é exatamente o que a VPS
  vai herdar. Se a subida quebrar a VM, a saída de emergência continua sendo a da Trilha
  0-01: `virsh snapshot-revert lab-vm base`.
- **Fronteira entre Issues:** keepalive/reconexão de SSH e a porta só-com-chave são da
  **Trilha 0-02**; colocar a VM na bridge da rede física (IP de LAN) ou expor para a
  internet é outro desenho de rede, fora do lab; o teste com IP público de verdade chega no
  estágio com VPS real, e o deploy via SSH automatizado é a **Trilha 2** — aqui o SSH é
  operação humana, um comando por vez.

## Como iniciar o modo teach-anything

- "Me ensina Docker Engine vs Docker Desktop vs snap instalando o repo oficial do Docker na `lab-vm` e conferindo `apt policy docker-ce`"
- "Me ensina `docker context` remoto vs build dentro da VM com o `compose.yaml` e a `notes-api` deste repo"
- "Me ensina abrir 80/443 no ufw da `lab-vm` por regra explícita e provar de fora com `curl` e o par `nc` 80/5432"
- "Me ensina o `.env` deste app em servidor: `chmod 600`, dono `lab` e por que o segredo nunca entra no histórico do git"
- "Me ensina operar esta stack via SSH (`ssh lab@<ip>`) e por que o `curl` no localhost não prova que a `lab-vm` responde"
