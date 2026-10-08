# Estudo — Deploy: o merge vira o deploy na `lab-vm`

> Material de estudo da Trilha 2. Acompanha a Issue 03 (deploy via SSH), mas não é o
> contrato daquela Issue: aqui é para entender, lá é para provar.

## CI constrói, CD toca: o que muda quando o pipeline ganha credencial e efeito no mundo

Até a Issue 02, o pipeline só **lia** e **produzia**: clonava o repo, rodava `mvn -B verify`
e empurrava uma imagem para o GHCR. Nada disso mexia em máquina que alguém usa. O job
`deploy` é a primeira coisa neste lab que **autentica numa VM real e troca o que está
rodando lá** — e é exatamente aí que a brincadeira deixa de ser barata.

- **Por que importa:** um bug num step de CI custa um run vermelho e cinco minutos. Um bug
  num step de deploy custa downtime, versão errada no ar ou chave vazada — e, pior, custa
  *depois* de o job terminar verde, quando ninguém mais está olhando. É por isso que
  "tem CI/CD?" na entrevista é duas perguntas encorridas: a segunda metade é a que tem
  permissão para quebrar produção.
- **Mecanismo:** a diferença não é o YAML, é a soma de três mudanças de causa e efeito:

| | CI (Issues 01–02) | CD (Issue 03) |
|---|---|---|
| O que o job toca | runner efêmero, repo, registry | a `lab-vm`, stack viva em `~/lab` |
| Credencial na mão | `GITHUB_TOKEN` efêmero (`packages: write`) | chave SSH duradoura na VM (`SSH_PRIVATE_KEY`) |
| Estado entre execuções | nenhum — máquina nova a cada run | a **mesma VM**, com o estado da execução anterior dentro |
| Sintoma de erro | run vermelho, na hora, na UI | às vezes só na VM, depois do verde |

O terceiro ponto é o que força o tópico "idempotente" lá embaixo: CI cria o estado do zero
toda vez; CD opera **em cima do estado que ele mesmo deixou** na run anterior.

O gatilho é o que transforma merge em deploy — e é por isso que ele só pode existir de um
lado só:

```yaml
# .github/workflows/ci.yml — a cadeia dos três jobs
jobs:
  test:            # Issue 01: mvn -B verify num runner limpo
    ...
  build:
    needs: test    # Issue 02: imagem nasce só do código que passou no teste
    ...
  deploy:
    needs: [test, build]   # sem os dois verdes, este job nem aparece
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: [self-hosted]   # só o runner do host alcança a VM (NAT do libvirt)
    steps:
      - uses: actions/checkout@v4
      - name: deploy
        run: ./scripts/deploy.sh "$GITHUB_SHA"
```

Cadeia de causa: `needs:` diz "só roda depois" (a UI do Actions desenha a seta); `if:` na
`main` diz "branch de PR nunca toca a VM" — deployar código que ainda vai mudar é trocar a
produção por um rascunho — e `event_name == 'push'` fecha a segunda porta: repo é público,
e o runner da LAN só aceita push da main própria. Somando com a *branch protection* da
Issue 01 (check obrigatório), quem decide o que chega à VM passa a ser a revisão do PR:
**o merge é o deploy**, e revisar um PR é revisar um deploy.

- **Exemplo no lab:** o cabamento novo é `runner → lab-vm`, e ele é físico: `lab-vm` mora
  atrás do NAT do libvirt (`192.168.122.0/24`, lease em `virsh net-dhcp-leases default` —
  Trilha 0-01), então o `runs-on: ubuntu-latest` do GitHub **não alcança** a VM por rede.
  A decisão da Issue 03 é o self-hosted no **host**: registrado no repo, instalado como
  serviço systemd, na mesma LAN que a VM — enquanto build/teste continuam no runner
  hospedado. O runner do host só faz conexão **outbound** para o GitHub (o NAT de casa
  não atrapalha quem puxa, só quem empurra) e, como o repo é público, agenda apenas push
  da main própria. Com VPS pública (estágio futuro) o hosted alcança direto e o
  `ubuntu-latest` volta a ser a escolha óbvia — é uma linha no workflow.
- **Fronteira entre Issues:** **CI verde** continua sendo Issue 01 e **imagem com tag SHA**
  Issue 02 — esta Issue só monta o cabo entre as duas (pré-condição: `ghcr.io/...:<sha>`
  publicada e stack 4×healthy, Trilha 1-04). Notificação de deploy é Trilha 3; ambientes
  múltiplos, blue-green e canary são estágio AWS; OIDC (credencial efêmera GitHub → cloud)
  também — aqui é chave SSH clássica de propósito, para aprender o modelo duradouro.

## Segredo no CI tem prazo de validade: `GITHUB_TOKEN` morre com o run, a chave SSH não

O runner é uma máquina que você **não** controla, que nasce e morre a cada execução. Toda
credencial que entra nele herda uma dessas duas naturezas — e escolher a errada é decidir
por acidente quanto tempo um vazamento fica valendo.

- **Por que importa:** com segredo duradouro em jogo, "o run falhou" deixa de ser a pior
  notícia possível. Se uma action de terceiro (`uses: alguma-coisa@v1`) ler os env do step,
  ela leva junto a chave que autentica na sua VM — e essa chave continua valendo amanhã,
  quando o run já sumiu e ninguém mais está olhando.
- **Mecanismo:**

| Segredo | Onde morre | Vida útil | Alcance se vazar | Onde entra aqui |
|---|---|---|---|---|
| `GITHUB_TOKEN` | automático, novo a cada run | minutos | só o que `permissions:` concedeu (`packages: write` — Issue 02) | push no GHCR |
| `SSH_PRIVATE_KEY` (secret do repo) | você o cria e o revoga | **até você revogar** | a VM inteira, como `lab` | `ssh` da Issue 03 |
| `.env` (Trilha 1-04) | arquivo em `600` na VM | até trocar a senha | `DB_PASSWORD`, `JWT_SECRET` | nunca sai da VM |

`GITHUB_TOKEN` não é "um segredo bom": é um token **efêmero por construção** — o GitHub o
emite, entrega ao step, mata no fim. A chave SSH é o oposto: duradouro **por construção** — o
custo de duradouro é que a única coisa que limita o dano é quem revoga. Daí a economia do
lab: chave dedicada de deploy (não a sua `~/.ssh` pessoal da Trilha 0-02), **sem senha**,
só para `lab@<ip-da-vm>` — se vazar, você revoga uma chave de lab e gera outra em minutos,
não troca a identidade de ninguém.

Dois detalhes de mecanismo que evitam o vazamento "sem querer":

```yaml
    - name: deploy
      env:
        SSH_PRIVATE_KEY: ${{ secrets.SSH_PRIVATE_KEY }}   # passado como VARIÁVEL
      run: |
        mkdir -p "$HOME/.ssh"
        printf '%s\n' "$SSH_PRIVATE_KEY" > "$HOME/.ssh/id_ed25519"   # como DADO, não como texto do script
        chmod 700 "$HOME/.ssh" && chmod 600 "$HOME/.ssh/id_ed25519"
        cp .github/known_hosts "$HOME/.ssh/known_hosts"
```

Causa e efeito: `${{ secrets.X }}` colocado **dentro** do `run:` é interpolado no texto do
script *antes* do bash ver — a chave com quebras de linha quebraria o script, e o segredo
passa a viver dentro de um comando que alguém pode `echo` por engano. Via `env`, o segredo
chega como valor de variável: o bash trata como dado, o log mostra só `***`, e a chave não
aparece no texto do passo.

```bash
# prova de que a chave nunca entrou no repo (a evidência é o log vazio)
git log --all -S 'BEGIN OPENSSH' --oneline     # → vazio
grep -rE 'BEGIN OPENSSH|sshpass|Password' .github scripts  # → vazio
```

- **Exemplo no lab:** a chave existe **só** em `Settings → Secrets and variables →
  Actions → SSH_PRIVATE_KEY` (o print é do *nome*, nunca do valor). O `ssh-keygen -t
  ed25519 -N "" -C "deploy-key-trilha2-03"` que a gerou roda uma vez, no laptop, e o arquivo
  privado é descartado do disco depois de subir para o secret — o par que sobrevive é o
  `authorized_keys` da VM. Nada de `sshpass` nem senha: a Trilha 0-02 já baniu senha do SSH
  antes de existir CI.
- **Fronteira entre Issues:** rotacionar chave automaticamente, vault/`docker secrets` e
  escopo mínimo por ambiente são Trilha 4; **OIDC** (credencial efêmera de verdade, o
  correspondente moderno do `GITHUB_TOKEN` para cloud) é estágio AWS — esta Issue é o
  modelo duradouro clássico, com um host, de propósito, para você sentir o custo. O
  `docker login` no GHCR se o pacote for privado é da Issue 02 (ver *Limitações* dela) —
  aqui só importa que essa credencial, se existir, também entra por secret, nunca no repo.

## Sem `known_hosts` pinado, o ssh não sabe com quem falou — e `ssh-keyscan` a cada execução é pedir MITM

Quando você digita `ssh lab@<ip>` pela primeira vez, o ssh pergunta "a autenticidade deste
host não pode ser estabelecida, aceita?" — aquela pergunta **é** a segurança de conexão
inteira. Respondida com "yes" uma vez, a chave da VM vira o padrão em `known_hosts`, e toda
conexão seguinte compara.

- **Por que importa:** o runner não tem mão nem ouvido — ninguém está lá para digitar
  "yes". Se a checagem de host key for desligada (`StrictHostKeyChecking no`) ou contornada
  (`ssh-keyscan` a cada execução), **qualquer coisa que responda na porta 22 passa a ser
  "a VM"**, e o deploy entrega a sessão para essa coisa: os comandos, o caminho da tag, a
  credencial do `docker login` se houver, e a saída falsa que deixa o job verde.
- **Mecanismo:** MITM (man-in-the-middle) é alguém entre runner e VM — mesma rede, spoof
  de ARP/DNS, rota desviada — que responde no lugar da VM. O que o pinning faz:

| Postura do cliente | O que acontece se um atacante se apresentar | Resultado |
|---|---|---|
| `known_hosts` pinado (correto) | chave apresentada ≠ chave guardada | `REMOTE HOST IDENTIFICATION HAS CHANGED!` → conexão **abortada** |
| `StrictHostKeyChecking no` | aceita qualquer chave, em silêncio | sessão com o atacante; ele vê e responde tudo |
| `ssh-keyscan <ip> > known_hosts` **no job, toda execução** | a chave "confiável" é justamente a que o atacante acabou de mandar | pinning de fachada: formalismo sem garantia |

A ironia da terceira linha: ela *parece* correta (gera um `known_hosts`, a checagem roda),
mas o que é fixado é a resposta do atacante. Uma pinagem só vale se a chave vier de um canal
que você confia — no lab, de uma execução única, feita à mão, de um lugar onde você sabe
que está falando com a VM de verdade:

```bash
# UMA vez, do laptop, na rede em que você confia — e vai para o repo
ssh-keyscan -t ed25519 <ip-da-vm> > .github/known_hosts
ssh-keygen -lf .github/known_hosts        # fingerprint legível para revisão no PR

# no runner: a cópia fixa, nunca um keyscan novo
cp .github/known_hosts "$HOME/.ssh/known_hosts"

# e o resto do comando não-interativo
ssh -i ~/.ssh/id_ed25519 -o BatchMode=yes -o IdentitiesOnly=yes \
    -o UserKnownHostsFile="$HOME/.ssh/known_hosts" \
    lab@<ip-da-vm> 'cd ~/lab && docker compose ps'
```

Causa e efeito de cada flag: `-i` escolhe **qual** chave usar (sem ela o ssh tenta todas as
do `~/.ssh` e o servidor rejeita um monte antes de acertar); `IdentitiesOnly=yes` impede
que outras identidades sejam oferecidas; `BatchMode=yes` transforma qualquer pedido de
senha/promoção em falha imediata — sem ele, um run sem chave ficaria **pendurado por 10
minutos** esperando um "yes" que ninguém vai digitar, e só depois daria timeout.

Dois desdobramentos honestos deste lab: a `lab-vm` tem IP por lease DHCP (Trilha 0-01), então
**trocar o IP da VM invalida o pin** — a resposta certa é repinar conscientemente
(`ssh-keygen -R <ip-novo>` e `ssh-keyscan` de novo do lugar confiável), nunca desligar a
checagem; e uma VM recriada (ou um `virsh snapshot-revert` para estado sem host key nova)
também muda a chave — o alarme `HOST IDENTIFICATION HAS CHANGED` é ele **funcionando**, e a
regra é investigar antes de limpar.

- **Exemplo no lab:** a evidência é o `.github/known_hosts` versionado com a chave ed25519
  da VM, e o log do step de deploy começando sem nenhuma linha de aviso de host key. O
  negativo também se testa: trocar um byte na linha do `known_hosts` e rodar o `ssh` → o
  erro de identidade aparece, provando que a checagem está viva.
- **Fronteira entre Issues:** **só-chave, sem senha, na porta 22** é Trilha 0-02 (o `ufw allow
  22` da Trilha 0-03 é quem mantém a porta aberta); a checagem de host key em si é conceito
  de SSH, cobrado aqui só porque o runner a obriga a ser declarada. Certificado de host
  gerenciado (CA de host keys) e `known_hosts` com faixa de IP do GitHub para restringir
  origem do `authorized_keys` (`from=`) são hardening de Trilha 4.

## Deploy idempotente: rodar duas vezes é o caso normal, não o acidente

CI cria o mundo do zero a cada execução (máquina nova — Issue 01). CD sempre pisa **em cima
do que já foi feito**: o `Re-run jobs` é o botão mais clicado do Actions, e toda execução
depois da primeira encontra a VM já naquela versão. Script que só funciona na primeira vez
não é script de deploy, é script de sorte.

- **Por que importa:** o inimigo clássico é o `docker compose down && up -d` "por segurança"
  ou o `sed -i` trocando a tag no `compose.yaml`: rodados uma vez funcionam; rodados duas,
  derrubam a stack sem necessidade, acumulam substituição em cima de substituição, ou deixam
  o arquivo do meio da última interrupção. Nesse mundo, **re-rodar um deploy vira decisão
  assustadora** — e aí ninguém roda, e o pipeline vira decoração.
- **Mecanismo:** idempotência = "segunda execução com a mesma entrada leva ao mesmo estado,
  sem efeito colateral". Neste lab ela vem de três decisões encadeadas:

| Passo | O que faz na 1ª vez | O que faz na 2ª vez (mesmo SHA) | Por quê |
|---|---|---|---|
| `docker compose pull app` | baixa `ghcr.io/<owner>/notes-api:<sha>` | `Image is up to date` — nada baixa | tag SHA é **imutável** (Issue 02): mesmo conteúdo = mesmo digest |
| `docker compose up -d` | cria/recria o container | `Container app Running` — não recria | spec do container já é a desejada; sem mudança, sem recreate |
| poll do health | espera o `200` | `200` na hora | stack já está na versão certa |

O motor é a tag por SHA: com `latest`, "a tag" é uma aposta sobre o que alguém publicou e
pull de novo resolve referência nova; com `<sha>`, a entrada do deploy é um valor fixo, e
pull de valor fixo que já está no disco é literalmente um no-op. O `up -d` completa a
história porque compara o desejado com o existente — só recria se algo mudou.

O lado oposto, para o contraste ficar visível no script:

```bash
# NÃO idempotente — cada rodada paga downtime à toa
docker compose down                 # derruba db, redis, app e proxy
docker compose up -d                # sobe tudo de novo, healthcheck do zero

# idempotente — o caso feliz e o repetido têm o mesmo desfecho
IMAGE_TAG="${SHA:0:7}" docker compose pull app
IMAGE_TAG="${SHA:0:7}" docker compose up -d
```

(No `compose.yaml`, o serviço `app` recebe `image: ghcr.io/<owner>/notes-api:${IMAGE_TAG:-latest}`
— a tag precisa chegar ao compose de alguma forma, e variável de substituição é a única que
**não escreve no arquivo**: `sed -i` na tag é exatamente o acumulativo que a idempotência
proíbe.)

- **Exemplo no lab:** o caminho de prova é o re-run: Actions → `Re-run all jobs` → o step
  `deploy` termina verde de novo com `Image is up to date` e `Container app Running` no log,
  e na VM `docker compose ps` segue 4×healthy sem restart visível. Sobre "push só de docs":
  cuidado com a simplificação — docs mudam o `GITHUB_SHA`, logo a tag muda, logo a imagem é
  reconstruída e recriada. Para o no-op ser literal, ou o deploy é filtrado por caminho
  (job `changes` com `dorny/paths-filter` + `if:` no `deploy`), ou se aceita o restart de
  segundos e se **declara** — a Issue 03 diz explicitamente que o requisito é *não falhar e
  não derrubar*, e declarar qual das duas você provou é parte do estudo.
- **Fronteira entre Issues:** **idempotência (não quebrar ao repetir) é desta Issue**;
  **reverter quando não fica saudável** é Issue 04 — lá o script passa a ler o SHA anterior
  *antes* da troca e voltar sozinho. Rollback manual declarado (`docker compose pull
  <sha-anterior> && up -d`) já entra aqui como saída de emergência. Zero-downtime real
  (blue-green, tráfego dividido) é estágio AWS: `up -d` troca a app com queda de segundos
  (pull + recreate), que o lab aceita.

## Health gate: o job só pode dizer verde depois do `200`

`docker compose up -d` retorna em **milissegundos** — ele garante que o container foi
*criado*, não que a app responde. Em Spring Boot com Flyway, dá tempo de sobra do processo
estar "rodando" enquanto a migração ainda corre, o datasource ainda não existe ou o Flyway
morre no meio: a stack aparece no ar e não serve tráfego.

- **Por que importa:** um deploy sem verificação é cópia com sorte. O job fica verde, todo
  mundo volta pro trabalho, e a falha só aparece quando um usuário (ou a Issue 04, quando
  existir) reclamar — o pior momento de descobrir, porque agora é incidente e não mais
  deploy. E o pior dos mundos é o "meio sucedido": exit 0 com a versão nova quebrada, um
  verde que **mentiu**.
- **Mecanismo:** o gate é um poll com timeout que só devolve sucesso no `200` — e o `200`
  precisa vir de fora da app, pelo caminho que o usuário usa:

| Sinal | O que ele prova | O que ele **não** prova |
|---|---|---|
| `docker compose ps` → `Running` | processo existe | que ele responde HTTP |
| healthcheck do container (HEALTHCHECK da imagem — Issue 01) | `/api/v1/actuator/health` de dentro da app | o caminho externo pelo proxy |
| `curl` no proxy → `502`/`503` | proxy de pé, app atrás caída | sucesso nenhum (Trilha 1-03: 502 do proxy não é health) |
| `curl` no proxy → **`200`** | cadeia completa: borda 443 → rede interna `app:8080` → app → `db`/`redis` | — este é o sinal do gate |

Causa e efeito do timeout: sem ele, um app que nunca sobe deixa o job **pendurado para
sempre** (GitHub corta em 6h e o erro vira "canceled", sem explicação); com timeout
declarado, o run termina em minutos, **vermelho**, com a causa na tela — falhar feio é o
requisito, não efeito colateral.

```bash
#!/usr/bin/env bash
# scripts/deploy.sh — versão enxuta; o rollback automático é a Issue 04
set -euo pipefail

SHA="${1:-${GITHUB_SHA:?precisa de um SHA}}"
IMAGE_TAG="${SHA:0:7}"
HEALTH_URL="https://<ip-da-vm>/api/v1/actuator/health"
TIMEOUT=60          # declarado, não "espera eterna"
INTERVAL=3
ssh_run() { ssh -i "$HOME/.ssh/id_ed25519" -o BatchMode=yes -o IdentitiesOnly=yes \
                 lab@<ip-da-vm> "$@"; }

# 1) troca de versão na VM (idempotente: mesma tag = no-op)
ssh_run "cd ~/lab && IMAGE_TAG=$IMAGE_TAG docker compose pull app && \
                 IMAGE_TAG=$IMAGE_TAG docker compose up -d"

# 2) health gate: só verde depois do 200, pelo proxy (Trilha 1-03)
deadline=$((SECONDS + TIMEOUT)); code=000
while (( SECONDS < deadline )); do
  code=$(curl -ks -o /dev/null -w '%{http_code}' --max-time 5 "$HEALTH_URL" || echo 000)
  [[ "$code" == "200" ]] && break
  sleep "$INTERVAL"
done

if [[ "$code" != "200" ]]; then
  echo "::error::health não respondeu 200 em ${TIMEOUT}s (último: $code)"
  exit 1                          # exit ≠ 0 → job vermelho, nunca "meio verde"
fi

echo "deploy ok: $(ssh_run 'docker compose ps --format "{{.Image}}"')"
echo "implantado: ghcr.io/<owner>/notes-api:${IMAGE_TAG}"
```

Detalhes que são mecanismo, não enfeite: `set -euo pipefail` faz o script morrer no primeiro
comando que falhar (sem ele, um `ssh` recusado seria engolido e o script seguiria até o
`200` antigo da versão anterior — verde **com a tag errada**); `curl -k` porque o proxy usa
certificado autoassinado (Trilha 1-03) — a alternativa mais dura é `-CAcert` com a CA do
proxy; e o `200` é checado de **fora**, no mesmo `https://<ip>` que o mundo usa.

- **Exemplo no lab:** o log do step de deploy precisa mostrar a linha `200` **antes** do
  exit 0, e a última linha imprime a tag implantada — é a ponte entre "pipeline" e "VM"
  (`docker compose ps --format '{{.Image}}'` na VM devolve `ghcr.io/...:<sha do merge>`).
  O negativo se prova com a simulação de falha: subir a app com env quebrada e ver o
  `::error:: ... (último: 503)` seguido de job `failure` — é a mesma provocação que a
  Issue 04 vai herdar.
- **Fronteira entre Issues:** aqui o health gate **espera e falha** (exit ≠ 0, VM fica com a
  versão nova — limitação declarada); **voltar sozinho** para o SHA anterior é Issue 04,
  que inclusive decide onde mora a lógica de rollback (no script ou em `if: failure()` no
  workflow). Notificar quando o gate falhar é Trilha 3; métrica/latência além do `200` da
  app (Prometheus decidindo deploy) é Trilha 3 + estágio AWS. E o healthcheck **do
  container** que ordena a subida do `db` é da Trilha 1-02 — não confunda: o do compose
  cuida da dependência interna, o do gate cuida da resposta ao mundo.

## Self-hosted runner: a máquina que o GitHub manda rodar na sua LAN

O runner hospedado é uma máquina efêmera do GitHub: nasce quando o job agenda e some
quando termina. O self-hosted é o contrário — um programa **na sua máquina**, registrado
num repo com um token, que fica em loop perguntando "tem trabalho pra mim?". É essa
direção do fio que resolve o NAT da Trilha 1-04.

- **Por que importa:** o hospedado *recebe* trabalho (o GitHub agenda máquina dele); o
  self-hosted **puxa** por conexão outbound — sai da sua rede como todo tráfego normal,
  zero ingress, zero regra de ufw, zero túnel. A VM atrás do NAT do libvirt fica
  alcançável porque quem executa o job está *do lado de dentro* (o host), e o
  `ssh lab@<ip-da-vm>` dele é tráfego de LAN. Quem pergunta "como o GitHub chega na minha
  VM?" está perguntando a direção errada: ninguém chega, quem sai é o runner.
- **Mecanismo:** registro e instalação são dois comandos, e o serviço é o que faz o
  runner sobreviver ao reboot — mesma semântica de `WantedBy`/`Restart` que a Trilha 0-04
  ensinou, agora para uma unit chamada `actions.runner.*`:

```bash
# no host — Actions → Settings → Runners → New self-hosted runner (copiar o token)
./config.sh --url https://github.com/<owner>/<repo> --token <TOKEN>
sudo ./svc.sh install && sudo ./svc.sh start     # vira serviço do systemd
systemctl is-enabled actions.runner.<owner>-<repo>-lab  # enabled = sobe no boot
```

Com o runner `online`, o job só muda de endereço: `runs-on: [self-hosted]`. E é aí que
aparece o preço da escolha — tudo que mora na sua máquina cobra na sua máquina:

- **Repo público exige restrição:** qualquer pessoa abre PR no seu repo, e um job com
  `runs-on: [self-hosted]` disparado por `pull_request` de fork executa o código do
  autor **na sua LAN**, com as permissões do runner (grupo `docker` incluso). A regra
  está no `if:` desta Issue: self-hosted só em `push` da main própria — nunca mais um
  job com esse label fora dessa condição.
- **Host desligado = job `pending`:** o merge acontece, o job espera a máquina ligar.
  Em lab é aceito e declarado; em produção isso se chama "fila de deploy" e é feature.
- **Fronteira:** com VPS pública (estágio futuro) o hosted alcança a VM por IP, o
  self-hosted desliga e `runs-on: ubuntu-latest` volta — o custo de saída da decisão de
  hoje é uma linha no workflow.

## Como iniciar o modo teach-anything

- "Me ensina CI vs CD com o `.github/workflows/ci.yml` deste lab: por que `needs:` e `if:
  github.ref == 'refs/heads/main'` fazem do merge um deploy"
- "Me ensina segredos em CI usando o `SSH_PRIVATE_KEY` desta Issue: por que `env:` em vez de
  `${{ secrets }}` dentro do `run:`, e o que difere do `GITHUB_TOKEN` do build da Issue 02"
- "Me ensina SSH não-interativo com o `.github/known_hosts` e o `ssh -o BatchMode=yes` do
  runner: o que é MITM e por que `ssh-keyscan` a cada execução não protege nada"
- "Me ensina idempotência de deploy lendo o `scripts/deploy.sh` deste repo e o serviço
  `app` do `compose.yaml`: por que `pull` da tag SHA é `Image is up to date` e `down && up`
  não é"
- "Me ensina health gate usando o poll do `scripts/deploy.sh` contra
  `https://<ip-da-vm>/api/v1/actuator/health`: por que `200` pelo proxy é a única prova de
  deploy bem-sucedido e o que é 'falhar feio' com `exit 1`"
- "Me ensina self-hosted runner com o `if:` e o `runs-on: [self-hosted]` deste `ci.yml`:
  por que o NAT do libvirt obriga o deploy a rodar no host, o que o `svc.sh install` faz
  no systemd e por que um repo público proíbe `pull_request` nesse runner"
