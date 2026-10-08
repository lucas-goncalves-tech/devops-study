# Estudo — Registro e tag: onde a imagem passa a existir e como ela se chama

> Material de estudo da Trilha 2. Acompanha a Issue 02 (build-push no GHCR com tag por
> commit), mas não é o contrato daquela Issue: aqui é para entender, lá é para provar.

## A imagem só existe onde alguém consegue puxá-la

Hoje a `notes-api` foi construída **na mão** no host (`docker build -t notes-api .`, da
Trilha 1-01) e a VM roda uma cópia que foi levada até lá. O nome `notes-api` não tem
endereço: ele resolve só naquele disco. É como ter um binário no `~/Downloads` do seu
notebook e dizer que ele "está em produção" — ele está em **um** lugar, e esse lugar não é
compartilhado com ninguém (nem com o runner da CI, que é uma máquina efêmera que nasce e
morre a cada run).

- **Por que importa:** build no CI **sem** registro é fabricar artefato e jogar fora: o
  runner termina o job, a imagem some com ele. E a transferência manual para a VM tem um
  custo pior que o trabalho — é *divergência silenciosa*: a VM tem uma cópia que ninguém
  consegue provar de qual commit veio, e "qual versão está rodando?" vira conversa, não
  consulta. O registro é o que transforma a imagem em **coisa pública do projeto**: o CI
  coloca, a VM puxa, o seu host puxa — mesma imagem em qualquer máquina.
- **Mecanismo:** registro é um servidor HTTP que fala o protocolo *Registry v2*: guarda
  **camadas** (os blobs do sistema de arquivos) e **manifests** (a lista de camadas + os
  nomes que apontam para ela). O cliente só precisa de três coisas — endereço, nome do
  repositório e credencial:

  | | GHCR | Docker Hub | ECR (AWS) |
  |---|---|---|---|
  | endereço | `ghcr.io/<owner>/notes-api` | `docker.io/<user>/notes-api` | `<conta>.dkr.ecr.<regiao>.amazonaws.com/notes-api` |
  | credencial | `GITHUB_TOKEN` do próprio run | usuário + PAT (conta pessoal) | chave/credencial IAM |
  | dói onde | pacote herda permissão do repo | rate limit no pull anônimo, conta pessoal no push | só existe dentro da AWS, `aws ecr login` antes |
  | custo de trocar | trocar o prefixo e o login | idem | idem |

  A ideia é **uma**: os três guardam a mesma coisa e falam o mesmo protocolo — muda o
  endereço e quem emite a credencial. Repare que o `Dockerfile` não aparece em nenhuma
  coluna: ele termina nas camadas, sem saber onde vão morar. O destino da imagem é
  decisão do **pipeline**, não do build.

- **Exemplo no app:** o login e o nome de imagem na forma em que entram no workflow desta
  Issue (o `ci.yml` da Issue 01 ganha um job ao lado do `mvn -B verify`):

  ```yaml
    build-push:
      needs: test
      if: github.event_name == 'push' && github.ref == 'refs/heads/main'
      runs-on: ubuntu-latest
      permissions:
        contents: read
        packages: write          # mínimo para publicar no GHCR
      steps:
        - uses: actions/checkout@v4
        - uses: docker/login-action@v3
          with:
            registry: ghcr.io
            username: ${{ github.actor }}
            password: ${{ secrets.GITHUB_TOKEN }}   # token do run, sem secret novo
  ```

  E o efeito no compose da VM — o serviço `app` deixa de ter endereço local:

  ```yaml
    app:
      image: ghcr.io/<owner>/notes-api:<sha>   # era: image: notes-api
      # nenhum bloco build: — a VM não compila, só puxa
  ```

  Prova de que o registro passou a ser o lugar da imagem (roda em qualquer máquina, não
  só no runner):

  ```bash
  docker pull ghcr.io/<owner>/notes-api:<sha>          # do host, fora do CI
  ssh lab@<ip-vm> 'docker compose pull'                 # a VM baixa em vez de receber cópia
  ssh lab@<ip-vm> 'docker compose ps --format "{{.Image}}"'
  ```

- **Fronteira entre Issues:** **esta** Issue entrega o build-push e a tag no GHCR, e o
  compose apontando para `image:` (o `pull` já acontece aqui, como pré-condição do
  deploy). O que acontece **depois** do pull — trocar a stack, esperar o health `200`,
  SSH, `scripts/deploy.sh` — é a **Issue 03**. Registro próprio (Harbor), multi-arch,
  scan de vulnerabilidade (Trivy) e assinatura (cosign/SBOM) estão explicitamente fora
  (Trilha 4 e estágio AWS). Detalhe que a Issue deixa na sua nota: se o pacote for
  **privado**, o `docker login` da VM precisa de credencial de leitura — e essa credencial
  é secret da **Issue 03**, não daqui.

## `latest` é apelido, SHA é matrícula

- **Por que importa:** `docker pull ghcr.io/<owner>/notes-api:latest` parece seguro porque
  "é a versão nova" — mas é uma **aposta**: você está apostando que o conteúdo daquele
  nome hoje é o mesmo que você testou ontem, e não tem como conferir, porque o nome não
  guarda nada além de "o último a chegar". Quando dá errado, a pergunta "qual commit está
  na VM?" não tem resposta — e sem resposta não há como investigar, corrigir nem voltar.
- **Mecanismo:** tag é **ponteiro**, não conteúdo. O conteúdo tem identidade própria (o
  *digest*, um `sha256:` calculado sobre o manifest); a tag é o rótulo que hoje aponta
  para ele e amanhã pode apontar para outro. As duas tags desta Issue são funções
  diferentes:

  | | `:latest` | `:<sha7>` |
  |---|---|---|
  | o que diz | "o mais novo que alguém publicou" | "o binário do commit `abc1234`" |
  | quem define | o último `push` — qualquer um | `git`, determinístico |
  | muda? | sim, sem aviso e sem registro | só se o próprio commit mudar |
  | serve para deploy? | não — é aposta | sim — é identidade |
  | serve para | humano perguntar "o que está no ar?" | pipeline, rollback, auditoria |

  A cadeia de rastreabilidade é o que a tag SHA compra:

  ```text
  VM (docker inspect → imagem)  -->  tag  -->  commit (git show <sha>)
                                                    │
                                                    └--> run da CI que testou AQUELE código
  ```

- **Exemplo no app:** a tag nasce do SHA do commit que disparou o run — `${GITHUB_SHA}`
  é lido direto do contexto do Actions (nada digitado à mão):

  ```yaml
        - id: sha
          run: echo "curto=${GITHUB_SHA::7}" >> "$GITHUB_OUTPUT"
        - uses: docker/build-push-action@v6
          with:
            context: .
            push: true
            tags: |
              ghcr.io/${{ github.repository_owner }}/notes-api:${{ steps.sha.outputs.curto }}
              ghcr.io/${{ github.repository_owner }}/notes-api:latest
  ```

  Conferir a correspondência é comandos, não confiança:

  ```bash
  git rev-parse --short HEAD                                  # o SHA do commit local
  docker pull ghcr.io/<owner>/notes-api:<sha7>                # mesmo nome, em outra máquina
  ssh lab@<ip-vm> 'docker compose ps --format "{{.Image}}"'   # o que a VM roda
  ```

  O `latest` é publicado junto (o requisito da Issue é ter as duas), mas **quem lê tag é
  o humano**; o compose e qualquer script usam o SHA.

- **Fronteira entre Issues:** como a tag é construída (7 chars, `type=sha` do
  `docker/metadata-action`, `format=long`) é escolha desta Issue — o requisito é "tag com
  SHA do commit", a forma é detalhe. **Usar** essa tag no deploy (`docker compose pull`
  da tag do `GITHUB_SHA`) é a **Issue 03**; **voltar** para a tag anterior é a
  **Issue 04**. Versionamento com semver/release automation está fora (declarado na
  Issue 01).

## Publicar é entregar: o push vem depois do verde

- **Por que importa:** no registro, chegar **é** ficar disponível — tudo que é publicado
  vira puxável por qualquer um com acesso, inclusive pelo deploy da VM (a Issue 03 faz
  `pull`). Publicar antes de testar não é "um build a menos no histórico": é colocar no
  único lugar em que o deploy confia um artefato que ninguém sabe se funciona. E a imagem
  ruim **fica** lá — depois do run vermelho, ela continua no registro, à espera de um
  pull.
- **Mecanismo:** no Actions, job com `needs:` só executa se as dependências terminaram em
  `success`. É a ordem escrita em YAML que protege, não a esperança de que alguém rode as
  coisas na ordem certa:

  ```text
  push na main
      │
      ▼
  job test (mvn -B verify, 56 testes)  ── verde ──► job build-push ──► ghcr.io/...:<sha>
      │
      └── vermelho ──► build-push NÃO executa (skipped) ──► nada publicado
  ```

  A segunda guarda é o `if:`: só `push` (não `pull_request`) e só `refs/heads/main`
  publicam. PR roda o teste e valida o código, mas não escreve no registro — fork nem
  teria permissão para isso.

  Tem ainda a leitura que a Issue 01 já ensinou sobre cache, agora com camadas de imagem:
  `cache-from: type=gha` / `cache-to: type=gha,mode=max` guarda camadas no cache do
  GitHub entre runs. **Cache é aceleração, não correção**: `COPY pom.xml` + `dependency`
  igual → pula download (o runner não tem o seu `~/.m2`); mudou o `pom.xml` → a camada
  seguinte invalida e baixa de novo. O teste continua rodando e o push continua sendo a
  **etapa final**.

- **Exemplo no app:** o job inteiro, na forma em que entra no `.github/workflows/ci.yml`
  (o `ci` da Issue 01 continua disparando em `push` **e** `pull_request`; o build-push é
  um job irmão que depende dele):

  ```yaml
  jobs:
    test:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v4
        - uses: actions/setup-java@v4
          with: { distribution: temurin, java-version: '17', cache: maven }
        - run: mvn -B verify

    build-push:
      needs: test                                   # <-- a ordem mora aqui
      if: github.event_name == 'push' && github.ref == 'refs/heads/main'
      runs-on: ubuntu-latest
      permissions:
        contents: read
        packages: write
      steps:
        - uses: actions/checkout@v4
        - uses: docker/login-action@v3
          with: { registry: ghcr.io, username: "${{ github.actor }}", password: "${{ secrets.GITHUB_TOKEN }}" }
        - id: sha
          run: echo "curto=${GITHUB_SHA::7}" >> "$GITHUB_OUTPUT"
        - uses: docker/build-push-action@v6
          with:
            context: .                              # o .dockerignore da Trilha 1-01 vale aqui
            push: true
            tags: |
              ghcr.io/${{ github.repository_owner }}/notes-api:${{ steps.sha.outputs.curto }}
              ghcr.io/${{ github.repository_owner }}/notes-api:latest
            cache-from: type=gha
            cache-to: type=gha,mode=max
  ```

  A prova da proteção é o par vermelho→verde: quebrar um teste e dar push → run
  `failure` e o step de imagem **não aparece** (não "falhou o push", *não rodou*);
  `git revert` → run verde e imagem publicada. Vale a mesma disciplina da Trilha 0-04 e
  da Issue 01: a falha precisa ser provocada de propósito, uma vez, para valer.

- **Fronteira entre Issues:** exigir o check verde antes do merge (branch protection,
  required check `ci`) é da **Issue 01** — aqui a ordem é interna ao workflow. O job que
  **usa** a imagem publicada (`needs: [test, build]` no deploy) é a **Issue 03**. Scan de
  vulnerabilidade da imagem (Trivy) bloqueando o push é Trilha 4: hoje a ordem protege
  contra "imagem de código que não passou no teste", não contra "imagem com CVE
  conhecido".

## O token que morre com o run

- **Por que importa:** o caminho errado óbvio funciona perfeitamente: criar um token de
  acesso pessoal de longa duração, colar em `Settings → Secrets` e nunca mais pensar
  nisso. O preço aparece depois: é uma credencial **eterna** que circula em texto, ninguém
  lembra de revogar, e o dia do vazamento (um workflow de terceiro lendo seus secrets, um
  fork com permissão) entrega acesso permanente ao registro. Credencial duradoura só é
  aceitável quando ela precisa durar — e um push de imagem não precisa.
- **Mecanismo:** o `GITHUB_TOKEN` não é um secret que você cria: o GitHub **emite um novo
  para cada run**, com a identidade daquele job, escopo daquele repositório, e o
  `permissions:` diz o que ele pode fazer. Ele expira sozinho com o run — não serve nem
  de fora (`gh` com aquele token em outra máquina é credencial inválida). Contrastando os
  tipos que este lab já usou:

  | | `GITHUB_TOKEN` (aqui) | `.env` 600 da VM (Trilha 1-04) | `SSH_PRIVATE_KEY` (Issue 03) |
  |---|---|---|---|
  | quem emite | GitHub, por run | você, na mão | você, chave dedicada |
  | validade | dura o run e morre | enquanto o arquivo existir | enquanto o secret existir |
  | blast radius | este repo + só o que `permissions:` declara | tudo que o `.env` guarda | shell na VM |
  | revogação | automática, não tem o que fazer | trocar arquivo + valor | gerar outra chave |
  | serve para | publicar imagem | app ler `JWT_SECRET`, `DB_*` | runner entrar por SSH |

  O `permissions: packages: write` é o **mínimo** para a tarefa: `contents: read` para ver
  o código, `packages: write` para escrever no GHCR. Declarado **no job**, ele restringe
  em vez de herdar a permissão padrão do repositório (que pode ser bem mais larga — o
  limite real depende da configuração de `Settings → Actions → Workflow permissions`).

- **Exemplo no app:** no workflow, o token aparece como `${{ secrets.GITHUB_TOKEN }}`,
  que o Actions preenche sozinho — nenhum secret é criado em Settings, e nada de
  credencial no YAML:

  ```bash
  grep -rn 'GITHUB_TOKEN\|password\|ghcr' .github/workflows/
  # esperado: só ${{ secrets.GITHUB_TOKEN }} no login e o prefixo ghcr.io nas tags
  gh secret list            # nenhum secret novo criado para o registro
  ```

  Compare com o hábito da Trilha 1-04, que continua certo **no lugar errado**: valor no
  `.env`, permissão 600, referência por `${VAR}` no YAML — na VM, para segredo de
  **host**. No CI, o equivalente saudável é exatamente este: segredo emitido para um run,
  que some com ele.

- **Fronteira entre Issues:** a credencial de **leitura** para a VM puxar pacote privado
  (se o lab usar pacote privado) é secret da **Issue 03**, junto com a chave SSH — e a
  Issue 03 já avisa o trade-off: chave SSH em secret é duradoura, é o modelo clássico, e
  a alternativa efêmera de verdade (OIDC) só brilha com cloud (estágio AWS). Secret
  manager, rotação automatizada e escaneamento de segredos são **Trilha 4**; aqui a
  entrega é o hábito de emitir por run e declarar mínimo.

## Por que a tag não pode mudar — e o que isso compra para a Issue 04

- **Por que importa:** rollback é responder "volta para a versão que estava antes". Se
  "antes" é um nome cujo conteúdo pode ter mudado, voltar pode significar ir **para
  frente** sem perceber — ou voltar para algo que ninguém nunca testou. A garantia de que
  *o binário de hoje é o de ontem* é o que transforma rollback de "torcer" em
  procedimento; e é o que permite dizer em incidente, com evidência: "a VM roda
  exatamente o que o CI construiu no commit `abc1234`".
- **Mecanismo:** três peças, e só uma delas é automática:

  1. **O nome nasce do conteúdo** — `<sha7>` é derivado do commit; publicar outro código
     sob aquele nome significaria mentir sobre qual commit produziu a imagem. O fluxo
     nunca reescreve a tag SHA: cada commit publica o **seu** nome, uma vez.
  2. **A identidade real é o digest** — `sha256:` do manifest, calculado sobre as
     camadas. Duas tags apontando para o mesmo digest são a mesma imagem, provável de
     ver; digest diferente é imagem diferente, provável de ver também.
  3. **O registro não é o guarda** — honestamente, GHCR não *impede* re-push de uma tag;
     a imutabilidade aqui é do **processo** (nome = commit) + **verificação** (digest).
     Se alguém re-pushar `latest`, o `latest` muda de digest e o SHA antigo continua lá,
     intacto — por isso a regra do deploy é nunca ler `latest`.

  ```bash
  # as duas tags, mesmo digest (mesma imagem com dois nomes)
  docker manifest inspect ghcr.io/<owner>/notes-api:latest  | jq -r .config.digest
  docker manifest inspect ghcr.io/<owner>/notes-api:<sha7>  | jq -r .config.digest

  # o binário que roda = o binário que o CI construiu
  docker image inspect ghcr.io/<owner>/notes-api:<sha7> --format '{{.Id}}'          # no host
  ssh lab@<ip-vm> "docker image inspect \$(docker compose images -q app) --format '{{.Id}}'"  # na VM
  ```

  Os dois `Id` iguais é a frase da Issue tornada comando: a VM não buildou, não recebeu
  cópia, **puxou** — e puxou o mesmo bit.

- **Exemplo no app:** o elo com o rollback é literal, está escrito na Issue 04: o script
  lê o `Image` atual **antes** da troca, e o caminho de volta é o mesmo `pull`/`up -d`
  com a tag anterior:

  ```bash
  # scripts/deploy.sh — a forma que a Issue 04 vai exigir
  PREV=$(docker compose images -q app)                 # ou o nome da tag lida antes
  docker compose pull "ghcr.io/<owner>/notes-api:${SHA_NOVO}"
  docker compose up -d
  # health não veio em 60s →
  docker compose pull "${TAG_ANTERIOR}" && docker compose up -d   # volta ao SHA anterior
  ```

  O `grep` da Issue 04 — "rollback usa tag SHA, nada de `latest` no revert" — só pode
  ser atendido porque a **Issue 02** publicou tag SHA e não sobrescreveu. Sem imutável,
  "voltar para o SHA anterior" é voltar para um nome; com imutável, é voltar para um
  **binário**.

- **Fronteira entre Issues:** **esta** Issue entrega a tag SHA publicada e não reescrita —
  o que existe **é** isso. Ler o SHA da VM antes da troca, poll de health com timeout
  declarado, `if: failure()`, exit ≠ 0 mesmo com sistema de pé (nunca verde escondendo
  rollback) e a simulação de falha são integralmente a **Issue 04**. O ato de trocar a
  versão na VM é a **Issue 03**. Política de retenção (limpar tags antigas sem apagar as
  que rollback pode usar), cosign/SBOM e Trivy estão fora — Trilha 4/futuro.

## Como iniciar o modo teach-anything

- "Me ensina o que é um registro de imagens comparando GHCR, Docker Hub e ECR com o
  `ghcr.io/<owner>/notes-api` deste lab e o login do `ci.yml`"
- "Me ensina a diferença entre tag `latest` e tag SHA usando as tags do
  `build-push-action` e o `docker compose ps --format "{{.Image}}"` da VM"
- "Me ensina por que o `build-push` tem `needs: test` no workflow, olhando o
  `mvn -B verify` da Issue 01 e o run que fica vermelho quando o teste falha"
- "Me ensina GITHUB_TOKEN e permissões mínimas pelo `permissions: packages: write` do
  job, comparando com o `.env` 600 da VM e a chave SSH da Issue 03"
- "Me ensina imutabilidade de artefato pelo digest do `docker manifest inspect` da tag SHA
  e ligando isso ao `docker compose pull <sha anterior>` do rollback da Issue 04"
