# Estudo — CI: o build só é verdade quando roda longe do meu laptop

> Material de estudo da Trilha 2. Acompanha a Issue 01 (CI com GitHub Actions), mas não é o
> contrato daquela Issue: aqui é para entender, lá é para provar.

## Verde no laptop é evidência de quê? O runner começa do zero a cada push

Os 56 testes desta `notes-api` estão verdes — no **meu** laptop, com a **minha** JDK 17,
com o **meu** `~/.m2` já aquecido e com o `.env` que eu mesmo criei na raiz de `VPS/`. O
resultado é real, mas ele responde a pergunta errada: "o código compila onde eu já fiz
tudo funcionar". CI é a máquina que responde outra: "o código compila **do zero**, num
lugar que ninguém preparou, antes de virar merge".

- **Por que importa:** o custo de não ter isso aparece sempre no mesmo lugar — na VPS.
  Código que só compila porque eu esqueci uma env var no `.env`, ou porque o `target/`
  guardava um `.class` velho, passa no laptop e quebra no ambiente limpo. E quebrar no
  ambiente limpo é barato (o job fica vermelho); quebrar na VM é caro (a stack pública é a
  Trilha 1-04 e ninguém está olhando quando ela cai).
- **Mecanismo:** o runner do GitHub é **efêmero** — uma VM descartável criada para cada
  run e destruída no fim. Não é "a minha máquina de novo": é uma máquina que só tem o que
  o workflow declarar ter.

| O runner **tem** (ou ganha se você pedir) | O runner **não tem** (e some no fim do run) |
|---|---|
| `ubuntu-latest` com Docker **ligado** (por isso o `@Testcontainers` + `postgres:15-alpine` de `AuthControllerTest` roda lá) | o `.env` — está no `.gitignore` de `VPS/`, então o `checkout` jamais o traz |
| o que o `git clone` entrega: `pom.xml`, `mvnw`, `.mvn/wrapper/maven-wrapper.properties`, `src/` | o `~/.m2/repository` do primeiro run (só existe a partir do cache do 2º) |
| a JDK que o `actions/setup-java` instalar — não a que está no seu PATH | a JDK "que eu uso": sem `setup-java`, a versão é a do runner, não a sua |
| `/tmp` e disco limpos a cada run | estado herdado: o `postgres:15-alpine` do testcontainers nasce vazio a cada run, sem volume de ontem |
| config do IDE, scripts soltos na raiz, `git status` sujo | o `target/` — cada run começa sem build prévio, sem `.class` herdado |

- **Causa e efeito:** se a suíte depender do `.env`, o run falha com
  `Could not resolve placeholder 'JWT_SECRET'` — porque em `src/main/resources/application.yml`
  está escrito `jwt: secret: ${JWT_SECRET}` **sem valor default**, e o arquivo não está no
  repo. A causa raiz é a dependência escondida do ambiente do autor; o efeito é uma CI que
  só fica verde no seu laptop. A saída (que a Issue 01 exige decidir) é uma de duas: o
  segredo da suíte passa a viver no repo em config de teste (`src/test/resources/application-test.yml`,
  que hoje só traz `bucket4j: enabled: false`, lido via `@ActiveProfiles("test")`) **ou**
  vira secret da Action injetado como variável de ambiente. O requisito é o mesmo nos dois
  caminhos: **a CI não referencia o meu `.env`**.
- **Exemplo no lab:** a prova negativa é um grep que precisa dar vazio:

```bash
# na raiz do repo — se algo apontar pro .env do autor, aparece aqui
grep -rn '\.env' .github/              # → vazio
git ls-files .github/workflows/ci.yml  # → o workflow está versionado
git check-ignore -v VPS/.env           # → .gitignore:...:.env (nunca viaja)
```

- **Fronteira entre Issues:** verde na CI **não** prova que a `notes-api` roda na `lab-vm`
  — o ambiente do runner (ubuntu efêmero) ≠ o da VM (Ubuntu com systemd, `.env` em `600`,
  ufw da Trilha 0-03/1-04). Ligar o verde ao deploy real é a **Issue 03**; esta Issue só
  entrega "o build é confiável fora do meu laptop". E o que fazer com o `JWT_SECRET` da
  suíte (default no teste × secret da Action) é decisão que a **Issue 01** registra, não
  regra fixa deste estudo.

## Anatomia de um workflow: quem dispara, quem roda, quem espera

`ci.yml` é config como código — está no repo, versionado, revisável, e é a primeira coisa
que alguém abre quando o pipeline "não funciona". Ler quatro palavras decodifica o arquivo
inteiro.

- **Por que importa:** sem essa gramática, o workflow vira copiar-e-colar de internet que
  ninguém consegue mudar sem medo. Ela é também o vocabulário da vaga: "trigger", "job",
  "step" são as palavras da pergunta "tem CI/CD?".
- **Mecanismo:** a hierarquia tem causa e efeito em cada nível —

| Nível | Papel | Efeito |
|---|---|---|
| `on:` (trigger) | **quando** o workflow nasce | `push` em `main` → roda no que entrou; `pull_request` em `main` → roda no que **quer** entrar (um PR pode disparar os dois) |
| `jobs.<id>` | **máquina** com passos em sequência; jobs rodam em paralelo entre si | cada job tem seu próprio runner efêmero; um job falho derruba o run inteiro e o commit fica vermelho |
| `steps:` | **comandos em ordem** dentro do job | a ordem é a causa: `checkout` antes de `setup-java`, `setup-java` antes do `mvn` — inverter é rodar com o repositório vazio |
| `needs:` | **dependência entre jobs** | job B com `needs: A` só entra quando A termina `success`; A falhou → B nem começa (porta fechada, não erro tardio) |

O arquivo real deste lab — dois detalhes nascem do repositório: o workflow mora na **raiz**
(`.github/workflows/ci.yml`, porque é só lá que o GitHub procura), e o `pom.xml` morre em
`VPS/` (o repo é o `devops-study` inteiro, não só a app) — por isso o
`working-directory: VPS`:

```yaml
# .github/workflows/ci.yml  (raiz do repo; arquivo entregue pela Issue 01)
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

defaults:
  run:
    working-directory: VPS

jobs:
  ci:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4          # o clone limpo do runner

      - uses: actions/setup-java@v4        # JDK 17 pinada, não "a que o runner tiver"
        with:
          distribution: temurin
          java-version: '17'
          cache: maven                     # cache do .m2 entre runs (tópico seguinte)

      - name: Build e testes
        run: ./mvnw -B verify              # 56 testes — a CI É o lugar dos testes

      - name: Prova do que rodou
        if: always()
        run: grep -h "Tests run" target/surefire-reports/*.txt   # working-directory já é VPS
```

- **Causa e efeito:** `./mvnw -B verify` roda a fase `verify` da cadeia de fases Maven
  (`compile` → `test` → `package` → `verify`); qualquer teste falhando faz o Maven sair
  com código ≠ 0, o step fica vermelho, o job `ci` termina `failure` e o run aparece
  vermelho na aba Actions. O `-B` é *batch mode*: sem ele o Maven imprime barra de
  progresso de download (ruído que muda a cada run e polui o log); em CI o log é evidência,
  então ele é obrigatório na prática. E o `mvn` vem do **wrapper** (`./mvnw` +
  `.mvn/wrapper/maven-wrapper.properties` versionados): mesma versão do Maven no seu
  laptop e no runner — sem wrapper, "funciona na minha versão" vira mais uma variável
  ambiente solta. Note `if: always()`: a prova é coletada mesmo quando o step anterior
  falhou — é o log do vermelho que você vai ler às 2h.
- **Exemplo no lab:** o `needs` não aparece porque este lab cabe num job só — mas ele é a
  peça que a **Issue 03** vai usar (job `deploy` com `needs: ci`: deploy só depois do
  verde). O shape é este, e entender agora custa uma linha:

```yaml
jobs:
  ci:      { runs-on: ubuntu-latest, steps: [...] }
  deploy:
    needs: ci          # porta: só abre se ci terminou success
    runs-on: ubuntu-latest
    steps: [...]       # (Issue 03 — não implementar aqui)
```

- **Fronteira entre Issues:** o `ci.yml` desta Issue entrega `workflow-ci` (checkout → JDK
  → `mvn verify`). **Build e push de imagem Docker** dentro do pipeline é a **Issue 02**
  (`docker build-push` para o GHCR, tag por SHA); **deploy, SSH e secrets de infra** são as
  **Issues 03–04**; **lint, SAST, checagem de qualidade de código** é Trilha 4; multi-branch
  e release automatizado é estágio futuro. `act` (rodar o workflow local, `act -j ci`) é
  atalho opcional de desenvolvimento — se usado, declarar; ele não substitui o run verde no
  GitHub.

## Cache do Maven: acelera o download, não decide se o build está certo

No primeiro run a suíte gasta minutos baixando Spring Boot, Testcontainers, Lombok — tudo
que já está no seu `~/.m2`. No segundo, não deveria baixar de novo. Isso é **cache**, e
cache é a única coisa de CI que otimiza sem mudar resultado.

- **Por que importa:** sem cache, cada push paga o preço completo da primeira execução —
  a CI fica lenta, o hábito de rodar some, e o time volta a "deixa eu testar local antes
  do push". Com cache, o run normal cai para perto do tempo real dos 56 testes.
- **Mecanismo:** o `cache: maven` do `actions/setup-java` congela o diretório
  `~/.m2/repository` do runner e o restaura nos runs seguintes. Três causas e efeitos
  importam:
  - **O que acelera:** downloads de artefatos resolvidos. A árvore de dependências é
    resolvida do zero de qualquer forma — o cache só evita ir na rede buscar bytes que já
    estão lá. Eis o efeito visível comparando o log do step de build nos dois runs:

```text
1º run (sem cache):   [INFO] Downloading from central: https://repo.maven.apache.org/...  (centenas de linhas)
2º run (cache hit):   nenhum "Downloading" — o step pula direto para compile/test
em ambos:             [INFO] Tests run: 56, Failures: 0, Errors: 0, Skipped: 0
                      ↑ ./mvnw -B verify: o cache muda o log de download, nunca a execução da suíte
```

  - **O que invalida:** a chave do cache é um **hash do `pom.xml`** (o `setup-java` usa
    `hashFiles('**/pom.xml')`). Mudou `pom.xml` (bump de versão, dependência nova) → hash
    novo → chave nova → miss e baixa tudo de novo. Não mudou → mesma chave → hit. Ou seja:
    invalidação é automática e casada com o que define as dependências.
  - **O que ele NÃO guarda:** `target/` (classes e relatórios não entram na chave nem no
    artefato de cache), resultados de teste, estado do banco, `.env`. Run seguinte recompila
    e **reroda os 56 testes** — é assim que deve ser.
- **Causa e efeito — porque cache é velocidade, não correção:** um cache nunca faz um build
  errado virar verde: a resolução de dependências e a execução dos testes continuam
  acontecendo, e se algo estiver quebrado o run falha igual. O pior caso de um cache
  problemático é **lento** (miss que obriga a baixar de novo) ou **vazado** (restaurar
  artefato velho que o `pom.xml` novo nem pede — o hash evita isso). Pense nele assim: cache
  é o atalho do caminho, não a revisão do conteúdo.
- **Exemplo no lab:** a evidência é comparar dois runs sem mexer no `pom.xml`:

```bash
# primeiro push: linha de download aparece (deps indo pra rede)
# segundo push, pom.xml intacto: o log do step de build não tem "Downloading" — só o resumo
git status --short VPS/pom.xml            # → limpo: nada invalidou a chave
# e a prova de que o cache não roubou a correção:
grep -h "Tests run" VPS/target/surefire-reports/*.txt   # → roda de novo, 56 testes, em todo run
```

- **Fronteira entre Issues:** o cache do `~/.m2` é desta Issue (`cache-maven`). Cache de
  **camada de imagem Docker** e o **registro por tag** (o que faz o pull da imagem ser
  rápido e rastreável) são da **Issue 02**; cache/estado no deploy da VM é da **Issue 03**.
  E nenhum cache entra como requisito de correção — o que prova correção aqui é o
  `Tests run: 56, Failures: 0`, não o verde do download.

## O verde só vale quando a branch se recusa a aceitar o vermelho

Ter uma Action é ter um **aviso**; ter branch protection com required check é ter uma
**regra**. A diferença aparece no dia em que alguém (inclusive o dono do repo) tenta
mergear com o build vermelho.

- **Por que importa:** sem a proteção, o check é opinião — dá para clicar em "Merge" de
  qualquer jeito, e o código quebrado entra na `main` com o mesmo peso do código verde.
  A CI aí vira decoração: todo mundo sabe que o vermelho não impede nada, então ninguém
  olha. O estado atual descrito na Issue 01 é exatamente isso: "nenhum check automático
  antes de merge; código quebrado entra no main".
- **Mecanismo:** branch protection é configuração **do lado do GitHub**, não do arquivo —
  ela existe mesmo que o workflow suma do repo. O encadeamento:

```text
push / abre PR
   └─ GitHub dispara o workflow "CI" (job ci)
         ├─ success  → o check "ci" fica verde no commit/PR
         └─ failure  → check "ci" fica vermelho
Settings → Branches → main (ou gh api .../branches/main/protection)
   └─ required status check = "ci"  →  o merge é bloqueado no servidor
        enquanto o check estiver ausente OU vermelho
```

- **Causa e efeito:** o bloqueio é **server-side** — não é um script convencendo a pessoa,
  é a API do GitHub recusando o merge. Por isso a ordem importa: primeiro o workflow roda
  e **reporta** o resultado, depois a proteção **exige** aquele nome de check. Se o nome
  exigido não bater com o `name:`/`job id` do workflow (ex.: exigir `ci` e o job se chamar
  `build`), o PR fica com "expected — waiting for status", e o merge trava por burocracia
  em vez de por vermelho — o sintoma clássico de required check mal configurado.

```bash
# a configuração como a Issue pede para evidenciar
gh api repos/:owner/:repo/branches/main/protection \
  --jq '.required_status_checks.contexts'      # → ["ci"]
# enquanto o check não existir, o merge é recusado pela API — é o servidor que diz não
```

- **O que o verde obrigatório garante e o que ele não garante:** o check garante que **os
  testes rodaram e passaram** naquele commit, em ambiente limpo. Ele não garante que o
  teste **testa alguma coisa** — um `@Test` vazio passa igual; e não garante que o código
  é bom, só que não quebrou o que já estava declarado. Por isso required check convive
  com revisão humana: ele automatiza a régua mecânica, não o julgamento.
- **Fronteira entre Issues:** **impor o verde** nesta Issue (`check-obrigatorio`); exigir
  qualidade além de "testes verdes" (cobertura mínima, lint, SAST, aprovação de revisor) é
  Trilha 4 — hardening. E a proteção é da `main` deste repo: proteger branches de release
  ou automação de semver é estágio futuro, fora do escopo da Issue 01.

## Fronteira: CI prova o build, CD move a VM — e esta Issue para na primeira metade

"CI/CD" é uma sigla com dois contratos diferentes. Esta Issue entrega só o primeiro, e
saber exatamente onde a linha cai evita treinar no errado.

- **Por que importa:** misturar os dois produz os dois erros clássicos: ou a pessoa acha
  que "CI é deploy" e não consegue explicar por que o build roda antes do merge, ou acha
  que "verde no GitHub" significa "está no ar" — e a VPS continua rodando a versão de
  ontem sem ninguém perceber.
- **Mecanismo:** são duas automações com gatilho, alvo e falha diferentes.

| | CI (esta Issue) | CD (Issues seguintes) |
|---|---|---|
| Gatilho | push / pull request | merge na `main` |
| O que roda | `./mvnw -B verify` — 56 testes em ambiente limpo | levar a imagem nova até a `lab-vm` e trocar a stack |
| Alvo | runner efêmero do GitHub | a VM da Trilha 1-04 (registro → SSH → `compose pull`) |
| Falha visível como | job vermelho, merge bloqueado | stack não atualizada / health não volta a `200` |
| Entrega | `workflow-ci`, `cache-maven`, `check-obrigatorio` | imagem por tag (Issue 02), deploy (Issue 03), rollback (Issue 04) |

- **Causa e efeito:** sem CI verde confiável, automatizar deploy é empurrar quebra para a
  VM — o pipeline passaria a **entregar** erro em produção, que é o oposto do que ele
  existe para fazer. É por isso que a ordem das Issues é fixa: build confiável (01) →
  imagem rastreável por SHA (02) → o merge vira deploy (03) → volta-se atrás com
  segurança (04). Cada degrau só existe em cima do anterior.
- **Exemplo no lab:** hoje, depois de um push, o que acontece é "aba Actions fica verde" e
  **nada muda na VM** — `ssh lab@<ip-da-vm> 'docker compose ps'` continua mostrando a
  imagem antiga, de propósito. Esse vazio é a fronteira: enquanto o verde só prova
  `Tests run: 56, Failures: 0`, a CI cumpriu o contrato desta Issue.
- **Fronteira entre Issues:** desta Issue sai o verde confiável e obrigatório. A imagem
  nascer no pipeline é **Issue 02**; o merge levar a imagem até a VM com healthcheck é a
  **Issue 03** (é lá que o `needs:` e os secrets de SSH aparecem); voltar atrás é a
  **Issue 04**. Nada de deploy, SSH ou secret de infra entra no `ci.yml` de agora — se
  aparecer, é a Issue errada sendo treinada.

## Como iniciar o modo teach-anything

- "Me ensina o runner efêmero do GitHub Actions: o que ele tem e o que não tem, comparando com meu laptop e com o `.env`/`pom.xml` da `notes-api`"
- "Me ensina anatomia de workflow (trigger, job, step, needs) lendo o `.github/workflows/ci.yml` deste repo e o motivo do `working-directory: VPS`"
- "Me ensina cache do Maven no CI: chave por hash do `pom.xml`, o que invalida e por que o `./mvnw -B verify` continua rodando os 56 testes em todo run"
- "Me ensina branch protection e required check: por que o verde vira obrigatório na `main` e o que esse check não garante sobre a qualidade do teste"
- "Me ensina a fronteira entre CI e CD usando as Issues 01 a 04 desta trilha: por que esta para no `mvn verify` e o deploy é assunto da Issue 03"
