# Estudo — Supply chain da imagem: da tag do base ao jar que sobe no `docker run`

> Material de estudo da Trilha 4. Acompanha a Issue 03 (scan da imagem com Trivy), mas não é
> o contrato daquela Issue: aqui é para entender, lá é para provar.

## O que o `docker run` aceita sem perguntar

**Por que importa.** A Trilha 4-02 varre o **texto do repo**: um token colado em `compose.yaml`
ou no histórico de git. Ela não varre um byte da imagem. E a imagem é metade do que existe:
a VM recebe `ghcr.io/<owner>/notes-api:<sha7>` por `docker compose pull` e executa
`java -jar app.jar` — sem perguntar de onde vieram os 106 jars dentro dele, nem quem
construiu a base `eclipse-temurin`, nem se aquele `apt-get` da build pegou pacote com CVE.
O repo pode estar impecável e a imagem continuar sendo uma caixa que ninguém abriu.

- **Por que importa (custo):** uma imagem é artefato que **circula** — quem tem acesso ao
  GHCR puxa o mesmo bit que a VM roda. Dá para meramente *declarar* que "o código está
  revisado" enquanto o que de fato roda carrega uma dependência comprometida; e, diferente
  de commit, imagem não tem diff legível: só um scan enxerga para dentro.

- **Mecanismo — a cadeia tem quatro origens diferentes**, e cada uma tem um dono de
  correção distinto. Esse é o ponto inteiro do tópico:

  | Etapa | Quem monta | O que entra | Quem corrige se estiver sujo |
  |---|---|---|---|
  | `FROM eclipse-temurin:17-jre` | upstream (Eclipse Adoptium / Ubuntu) | userspace da base + JRE + libs do sistema | trocar a tag/sufixo → **esta Issue** |
  | camadas de `RUN` da build | seu Dockerfile | pacote instalado na build (se houver) | seu Dockerfile → **esta Issue** |
  | `COPY --from=build .../app.jar` | seu build Maven | 106 jars em `BOOT-INF/lib` | quem mantém o `pom.xml` → **fora daqui** (tópico 5) |
  | manifest e digest no GHCR | registry + `build-push` | identidade do conjunto (T2-02) | rebuild + nova tag |

  Lida de cima para baixo, é o caminho real do que executa:

  ```text
  FROM eclipse-temurin:17-jre          ─┐ camadas da base (OS + JRE)
  RUN useradd ... ; COPY --from=build  ─┤ camadas suas (usuário, jar)
  COPY --from=build /app/target/notes-api-0.0.1-SNAPSHOT.jar app.jar
        │
        └── jar = fat jar com 106 arquivos em BOOT-INF/lib
                (spring-web, tomcat-embed-core-10.1.52, jackson-databind-2.19.4,
                 hibernate-core-6.6.42.Final, flyway-core-11.7.2, jedis-6.0.0,
                 snakeyaml-2.4, postgresql-42.7.10, ...)
        │
        ▼
  push → ghcr.io/<owner>/notes-api:<sha7> → VM: docker compose pull → java -jar app.jar
  ```

  Duas consequências que explicam o shape da Issue:

  1. **O estágio `build` não existe na imagem final.** O JDK, o Maven e os 275 MB de
     `/root/.m2` ficaram no cache do builder (T1-01). Scan da imagem final não enxerga
     nem precisa enxergar o JDK da build — ele está fora do artefato.
  2. **Scan de `fs` do repo e scan de imagem são varreduras de coisas diferentes.**
     `trivy fs .` olha `pom.xml` e arquivos soltos; `trivy image` olha o que a VM vai
     executar, inclusive os jars **dentro** do fat jar. A Issue exige `trivy image` justamente
     porque é o artefato, não a intenção, que sobe no `docker run`.

  ```bash
  # a prova de que a imagem é mais que o repo: o jar dentro dela
  unzip -l target/notes-api-0.0.1-SNAPSHOT.jar | grep -c 'BOOT-INF/lib'   # 106
  unzip -l target/notes-api-0.0.1-SNAPSHOT.jar | grep -E 'scalar|devtools|testcontainer'
  # scalar-0.1.0.jar           ← presente (dependência de build declarada sem <scope>)
  # (devtools e testcontainers não aparecem — exclusos/test scope, nunca chegam aqui)
  ```

- **Fronteira entre Issues:** a **T4-02** entrega o gate que falha o CI por segredo em
  **texto** (repo e histórico); **esta** entrega o gate que falha por CVE no **conteúdo da
  imagem** — as duas redes de supply chain, uma não cobre a outra. A imagem existir e ser
  publicada é **T2-02**; a VM puxar e subir é **T2-03**; o que se faz quando um achado
  vira incidente de verdade é **T4-04**.

| Pergunta | Rede que responde |
|---|---|
| "tem token no meu repositório?" | T4-02 (texto, git) |
| "tem CVE conhecido no que a VM executa?" | T4-03 (imagem, esta) |
| "tem CVE no código que eu escrevi?" | SAST — fora daqui |
| "exploraram alguma coisa?" | T4-04 (incidente) |

## CRITICAL no lab não é incidente: severidade mede o CVE, não o seu caminho até ele

**Por que importa.** É o erro que mata o scan para sempre: rodar o Trivy a primeira vez,
ver `CRITICAL 9.8` em vermelho, e concluir que o gate não pode ser bloqueante porque
"sempre vai estar vermelho". Daí se desliga o scan — e aí o gate vira decoração. O oposto
também dói: tratar tudo como urgência transforma cada release numa caça a CVE que ninguém
consegue terminar. O que sustenta o limiar é separar duas pergunta que o número só responde
uma.

- **Mecanismo — CVE, CVSS e severidade são três coisas:**
  - **CVE** é um *identificador*: `CVE-YYYY-NNNNN` diz "esta fraqueza tem este nome
    registrado", nada mais — não diz se está na sua imagem nem se é explorável.
  - **CVSS** é uma nota de 0 a 10 calculada de vetores (`AV` quem chega, `AC`Complexidade,
    `PR`privilégio prévio, `UI`interação do usuário, `C/I/A`impacto). É da nota que sai a
    severidade:

    | nota CVSS | severidade |
    |---|---|
    | 0.0 | NONE |
    | 0.1–3.9 | LOW |
    | 4.0–6.9 | MEDIUM |
    | 7.0–8.9 | HIGH |
    | 9.0–10.0 | CRITICAL |

  - **A nota descreve o pior caso do CVE, não o seu caso.** `AV:N/AC:L/PR:N/UI:N` (chega de
    rede, sem credencial) é o que faz 9.8; `AV:L/AC:H/PR:L/UI:W` com a mesma grauidade de
    impacto pontua muito menos. O score nunca pergunta: *esta biblioteca está no meu
    classpath? a função afetada é chamada? de onde um atacante chega até ela aqui?*

  Então a leitura correta do report é uma árvore, e cada "não" desce o nível de urgência:

  ```text
  achado no report
  ├─ existe versão corrigida?  ── não ──► não dá para agir hoje: ignore documentado
  ├─ o pacote está na imagem final? ── não ──► irrelevante aqui (ficou na build/teste)
  ├─ o código que o usa está no caminho da app? ── não ──► dívida baixa, anotar
  └─ um atacante de fora alcança esse caminho?
       ├─ portas da VM (ufw, só 443 via proxy T1-03) ── fechado? autenticado? ── sim
       └─ então sim: vira incidente → T4-04
  ```

- **Exemplo no app:** o `BOOT-INF/lib` traz `tomcat-embed-core-10.1.52` e
  `jackson-databind-2.19.4` — servidor HTTP embutido e serialização, os alvos mais
  comuns de CVE de rede. Mas a notes-api só escuta atrás do reverse proxy com TLS
  (**T1-03**), o ufw não publica 8080 na VM, e as rotas passam pelo Spring Security
  (`/api/v1/...`). Um CRITICAL numa rota que exige JWT válido, com porta fechada na borda
  e em biblioteca cuja função afetada a app nem chama, **não é incidente**: é dívida
  informada — e é exatamente para dizer isso por escrito que existe o `.trivyignore`
  com motivo e data (próximo tópico). Já um CRITICAL em pacote de sistema exposto por
  serviço rodando na borda é outra conversa, e ela é da **T4-04**.

- **Fronteira entre Issues:** classificar achado e declarar o limiar é desta Issue;
  **conduzir** o incidente (contenção, comunicação, lições) é **T4-04**; hardening da borda
  que reduz o "atacante alcança?" é **T4-01** e **T1-03**. Esta Issue **não** decide que
  CRITICAL é inofensivo — ela decide que severidade sozinha não fecha decisão, e que a
  decisão fica escrita em arquivo versionado.

## O que o Trivy enxerga dentro da imagem — e os três pontos cegos de propósito

**Por que importa.** Um scan só vale o que ele mede. Se você acha que `trivy image` "aprova
o app", vai dormir tranquilo com um SQL injection no `NoteController`; se você acha que ele
"aprova o código", vai exigir dele o que nenhum scanner de imagem entrega. Saber a lista do
que ele varre e a lista do que ele não varre é o que evita as duas decepções.

- **Mecanismo — como ele monta a lista de pacotes:**
  1. baixa o **manifest** da imagem e as **camadas** (do daemon local, de um tar ou do
     registro);
  2. para o sistema operacional, lê o banco do gerador de pacotes da base —
     `/var/lib/dpkg/status` no Ubuntu da `eclipse-temurin` — e vira a lista de pacotes OS
     com versão;
  3. para aplicações, abre arquivos dentro das camadas: jars/wars/eggs — é aqui que ele
     entra no fat jar e identifica `jackson-databind-2.19.4.jar`, `hibernate-core-6.6.42.Final.jar`,
     `snakeyaml-2.4.jar` (por nome e conteúdo, não por `pom.xml`);
  4. compara cada versão com a **Trivy DB**, a base de advisories baixada em cache
     (padrão: atualiza a cada 24h). Sem DB atualizado não há achado — e DB velho é report
     antigo, o que também é motivo de tempo de CI: primeira execução paga o download;
  5. devolve achados e — detalhe decisivo para a política — **código de saída**:
     `--exit-code 1` faz qualquer achado do limiar terminar o processo com 1. É essa linha
     que um job de CI enxerga como falha.

  ```bash
  # local primeiro, mesma versão que o CI vai ver (roda em qualquer máquina)
  docker pull ghcr.io/<owner>/notes-api:<sha7>
  trivy image --severity HIGH,CRITICAL ghcr.io/<owner>/notes-api:<sha7>
  trivy image --format sarif -o trivy-results.sarif --exit-code 1 --severity CRITICAL,HIGH \
      ghcr.io/<owner>/notes-api:<sha7>
  ```

- **Os pontos cegos (o que ele não vê, e por quê):**
  - **Lógica do app.** Ele não executa nada: não vê IDOR, JWT mal validado, SQL montado
    com string em `src/main/java`. Varrer isso é SAST/DAST (Semgrep, ZAP) — **fora de
    escopo** desta Issue.
  - **Configuração e ambiente.** Porta aberta no ufw, `JWT_SECRET` fraco no `.env` da VM,
    `management.endpoints` exposto: não estão nas camadas da imagem. Ele tem um modo
    próprio (`trivy config`) mirando IaC/Dockerfile, que é outro alvo — e mesmo assim só
    se você apontar para o arquivo.
  - **O que ficou de fora do artefato.** Dependências de teste (`testcontainers`, `junit`)
    e `spring-boot-devtools` não entram no fat jar: não aparecem, e **bem** — reportar o
    que não roda é ruído. O inverso também é verdade e surpreende:
    `com.scalar.maven:scalar:0.1.0` está declarada no `pom.xml` sem `<scope>` e por isso
    **está** nos 106 jars — ele vai aparecer no scan, e não é o scan que está errado.
  - **Explorabilidade e "é usado?"** — o limite do tópico anterior: o Trivy compara versão
    com advisory, ponto. Ele diz *existe*, nunca *dá para explorar aqui*.
  - **CVE sem versão corrigida.** Sem `Fixed Version`, não há o que update bloquear;
    `--ignore-unfixed true` tira esses do report — decisão legítima **se declarada** no
    `trivy.yaml`, porque escondê-los sem escrever é política escondida.

- **Fronteira entre Issues:** SAST/DAST, SBOM formal (`--format cyclonedx` é complemento,
  entra se aparecer a questão) e re-scan agendado da imagem já publicada estão **fora de
  escopo** da Issue — este último é estágio de operação contínua, não de build. O scan
  contínuo de segredo no texto do repo é **T4-02**.

## Vermelho sempre é scan desligado: o limiar que o time sustenta

**Por que importa.** O threshold é o coração da Issue porque é a única parte que pode
morrer sem ninguém perceber. Três mortes clássicas: (1) limiar tão baixo que **todo** run
fica vermelho → alguém remove a etapa ou passa a ignorar vermelho, e o gate está desligado;
(2) limiar tão alto (ou `--exit-code 0`) que **nunca** falha → decoração, linda no print da
evidência; (3) report que só existe no log do run → some quando o log expira, e amanhã
ninguém prova o que foi achado. O jogo não é "qual severidade é perigosa", é **o limiar que
este time consegue sustentar**: bloquear o que dá para corrigir rápido, registrar o resto.

- **Mecanismo — a política precisa morar no repo, não na linha digitada no workflow.**
  Duas peças, ambas versionadas:

  ```yaml
  # trivy.yaml — a política de scan, lida por: trivy image --config trivy.yaml <img>
  severity:            # severidade que CONTA para o exit code
    - CRITICAL
    - HIGH
  exit-code: 1         # achado no limiar ⇒ processo termina com 1 ⇒ job vermelho
  format: table
  scanners:
    - vuln             # padrão; secret/license são opt-in
  ignorefile: .trivyignore
  ```

  ```yaml
  # .github/workflows/ci.yml — etapa dentro do job build-push (continua o ci da T2-02)
        - name: Scan da imagem (gate de supply chain)
          uses: aquasecurity/trivy-action@0.28.0      # piner em tag, conferir a atual
          with:
            image-ref: ghcr.io/${{ github.repository_owner }}/notes-api:${{ steps.sha.outputs.curto }}
            trivy-config: trivy.yaml                   # a política vem do repo, não do YAML do job
            format: table
            exit-code: '1'
            ignore-unfixed: 'false'                    # decisão declarada, não implícita
        - name: Report como artefato do run
          if: always()                                 # o report sobrevive ao run vermelho
          uses: actions/upload-artifact@v4
          with:
            name: trivy-report
            path: trivy-report.*
  ```

  Duas armadilhas de mecanismo que a Issue force a resolver, e não são detalhe:

  - **Onde o scan roda:** o `build-push-action` usa buildx, que por padrão **não** deixa a
    imagem no daemon local do runner — `trivy image notes-api:<sha7>` falharia com "image
    not found". Ou você aponta para a referência do GHCR (o `docker/login-action` já
    autentica o pull), ou usa `load: true` para trazê-la ao daemon antes de escanear. O
    requisito é **declarar** qual ponto do pipeline é o seu: pós-push com gate (imagem já
    publicada, mas run vermelho bloqueia o ciclo) ou pós-build pré-push.
  - **Prova negativa obrigatória:** gate só é gate se já foi visto bloqueando. Baixar o
    limiar para `MEDIUM` num branch de teste e observar o run **vermelho**, depois
    restaurar o limiar decidido e ver verde, é a diferença entre "está configurado" e
    "age". Sem isso, o `exit-code: '1'` pode estar com aspas erradas há meses.

- **`.trivyignore` é dívida registrada, não perdão.** Um CVE entra na lista só com três
  coisas: o ID, o **motivo** e a **data de revisão**:

  ```bash
  # .trivyignore — lido automaticamente pelo Trivy no diretório de trabalho
  # CVE-2025-XXXXXX — jackson-databind 2.19.4, sem release corrigida no BOM do
  # spring-boot-starter-parent 3.5.11; atacabilidade: rota serialização exige JWT válido,
  # porta fechada na VM (ufw/443). Registrado em 2026-10-08, revisar em 2026-11-08.
  CVE-2025-XXXXXX
  ```

  Por que a data: o ignore **envelhece sozinho para o lado errado**. Depois que a base
  é atualizada, o achado some do report — e a linha morta continua no arquivo, ensinando a
  próxima pessoa que aquilo é "coisa conhecida e ok". A data de revisão é o que transforma
  a lista eterna em fila com prazo; quem lê consegue achar silêncio velho
  (`grep '^#' .trivyignore | grep 2026`). O Trivy não valida a data — quem valida é o
  processo, e é por isso que ela é requisito da Issue, não conveniência.

  Os anti-padrões que entregam o scan teatro, em ordem de frequência: só `CRITICAL` no
  limiar (vira irrelevante em base suja), `exit-code: 0` "para não quebrar o pipeline",
  `ignore-unfixed: true` sem ninguém ter escrito que é decisão, `.trivyignore` colado de
  outra pessoa sem motivo, e report que nunca vira artefato.

- **Fronteira entre Issues:** **esta** Issue entrega o threshold versionado, a prova de
  que o gate bloqueia e o achado processado; **exigir** check verde antes do merge é
  branch protection (**T2-01**); o gate por segredo de texto é **T4-02**; re-scan diário
  da imagem já publicada e cache/orçamento de scan em escala de time estão fora — aqui o
  custo a declarar é só "scan < ~2min ou cache habilitado".

## O achado no jar que a infra não pode corrigir: registrar é a entrega

**Por que importa.** É a fronteira onde a entrega costuma virar atrito: o scan aponta
`tomcat-embed-core` ou `spring-security` dentro do jar, e alguém "resolve" abrindo
`pom.xml` no repo de infra. Isso é fork silencioso do app — infra passa a manter uma versão
de dependência que não tem teste para validar, e o próximo `mvn verify` verde não prova
nada sobre a mudança. O oposto também falha: achar, não anotar em lugar nenhum, e o achado
virar ruído do próximo run.

- **Mecanismo — quem decide a versão daquela lib.** No `pom.xml` deste lab quase nada
  declara versão explicitamente: `spring-boot-starter-parent:3.5.11` traz a **BOM**, e é
  ela que fixa `jackson-databind:2.19.4`, `tomcat-embed-core:10.1.52`, `hibernate-core:6.6.42.Final`.
  Corrigir um CVE desses significa subir o Spring Boot (ou pinnar versão à mão, o que
  briga com a BOM) → muda comportamento de framework → **56 testes** são quem tem que
  provar que nada quebrou. Quem tem os testes é quem tem o pom:

  ```text
  achado no BOOT-INF/lib do notes-api-0.0.1-SNAPSHOT.jar
  ├─ é pacote OS da base (ubuntu/temurin)?  ── sim ──► trocar tag da base, rebuild
  │                                                    → achado some → ações daqui
  └─ é lib dentro do jar (Maven)?           ── sim ──► dono = repo do app
                                                       → NÃO tocar pom.xml aqui
                                                       → registrar e passar adiante
  ```

- **Exemplo no app — a nota que fecha o ciclo.** A entrega não é commit, é informação
  rastreável, no formato que o próximo run consegue confrontar com o report:

  ```markdown
  ### Pendência de dependência — scan de 2026-10-08 (run #<n>)
  - CVE-2025-XXXXXX — `tomcat-embed-core:10.1.52` → versão corrigida: 10.1.xx
  - Caminho: `BOOT-INF/lib` do jar da imagem `ghcr.io/<owner>/notes-api:<sha7>`
  - Atacabilidade no lab: rota exige JWT válido; VM só publica 443 (T1-03/T4-01)
  - Tratamento: no `.trivyignore` com motivo + data (revisar em 2026-11-08)
  - Dono da correção: repo do app (bump do `spring-boot-starter-parent` + `mvn verify`)
  - Fora de escopo de infra: alterar `pom.xml`/`src/` nesta trilha
  ```

  A prova de que a fronteira foi respeitada é um `git diff` do processo **sem** mudança em
  `pom.xml` nem `src/` — o achado virou linha no ignore com prazo e nota para o dono, e o
  build continuou idêntico.

- **Fronteira entre Issues:** atualizar a **base** (`FROM eclipse-temurin:17-jre` para sufixo
  com patch novo) é ato desta Issue; subir versão de **dependência do app** é de quem mantém
  o código (aqui: informação, não tarefa); corrigir vulnerabilidade **no código** (SAST,
  DAST) é outra frente, assim como SBOM formal e scan de imagem re-publicada — todos
  declarados fora de escopo. Se um achado chegar a ponto de exploração real, a conversa
  muda de dono: vira **T4-04**.

## Como iniciar o modo teach-anything

- "Me ensina a supply chain da imagem da notes-api: da `FROM eclipse-temurin:17-jre` até
  os 106 jars do `BOOT-INF/lib`, e por que o gate da T4-02 não pega nada disso"
- "Me ensina CVE × CVSS com um achado CRITICAL hipotético em `tomcat-embed-core-10.1.52`
  nesta app: que preciso responder para dizer se ele é atacável aqui"
- "Me ensina o que o `trivy image` varre de verdade em `ghcr.io/<owner>/notes-api:<sha7>` —
  e os pontos cegos dele, da lógica em `src/main/java` ao `scalar-0.1.0.jar` que está no jar"
- "Me ensina threshold como política usando o `trivy.yaml` e a etapa de scan do `ci.yml`,
  incluindo a prova negativa do run vermelho e o `.trivyignore` com CVE + motivo + data"
- "Me ensina a fronteira do achado Maven: por que a resposta certa é registrar para o dono
  do app e provar com `git diff` que o `pom.xml` não foi tocado"
