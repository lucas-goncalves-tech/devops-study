# Estudo — Dockerfile de runtime: o que a notes-api leva (e não leva) para produção

> Material de estudo da Trilha 1. Acompanha a Issue 01 (imagem reproduzível com usuário sem
> privilégio), mas não é o contrato daquela Issue: aqui é para entender, lá é para provar.

## O Maven é ferramenta de build, não parte do produto

Hoje a app só existe como `./mvnw spring-boot:run` no seu terminal: isso é ambiente, não
entrega. O dia em que houver uma imagem `notes-api`, tudo o que estiver dentro dela passa a
ser pago de três formas — alguém baixa, alguém varre, alguém atualiza. A pergunta certa não é
"ela roda?", é "o que sobra dentro depois que o build acabou?".

**Mecanismo.** Cada `FROM` abre um estágio com filesystem próprio; só o que você copiar
explicitamente com `COPY --from=` atravessa a fronteira. O estágio de build não é empacotado na
imagem final — ele fica só no cache do builder (e ajuda no próximo build), invisível para quem
puxar a imagem. Números medidos neste lab:

| | estágio `build` (`maven:3.9-eclipse-temurin-17`) | estágio `runtime` (`eclipse-temurin:17-jre`) |
|---|---|---|
| peso em disco (`docker images`) | 775 MB | 442 MB |
| tem JDK, `javac`, Maven, plugins | sim | não — só JRE |
| tem o repositório de dependências | `/root/.m2` = **275 MB** baixados em **2m42** no `dependency:go-offline` | não existe `.m2` |
| tem o código-fonte | sim (`src/`, 248 KB) | só o jar pronto |
| serve a porta 8080 | não | é exatamente para isso que ele existe |

O Dockerfile desta Issue, na forma em que ele entra no repo:

```dockerfile
# ---- estágio 1: build (nunca vai para produção) ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

# ---- estágio 2: runtime (o único que vira imagem final) ----
FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 --create-home notes
WORKDIR /opt/notes
COPY --from=build /app/target/notes-api-0.0.1-SNAPSHOT.jar app.jar
RUN chown notes:notes /opt/notes app.jar
USER notes
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -fsS http://localhost:8080/api/v1/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Duas leituras de causa e efeito nesse arquivo:

- O **único** objeto que cruza dos estágios é `notes-api-0.0.1-SNAPSHOT.jar` (70 MB, o que o
  `spring-boot-maven-plugin` gera em `target/`). O Maven, o JDK, o `/root/.m2` de 275 MB e o
  `src/` não têm nenhum caminho para a imagem final — não porque foram "limpos", mas porque
  nunca entraram.
- O `-DskipTests` no `package` não é preguiça: o `pom.xml` deste lab traz
  `spring-boot-testcontainers` + `postgresql`, e dentro de `docker build` não existe
  `/var/run/docker.sock` para o Testcontainers abrir um Postgres — a suíte quebraria o build.
  Teste roda no host agora, no CI depois (Trilha 2); a imagem só precisa do artefato.

**Fronteira entre Issues:** imagem distroless/UBI/multi-arch é aprofundamento futuro (está no
"fora de escopo" desta Issue); build e push no CI é **Trilha 2**; construir a imagem na VM é a
**Issue 04**. O que esta Issue entrega é `docker build -t notes-api .` funcionando no host de
build, a partir de diretório limpo.

## A ordem das instruções é o cache: `pom.xml` antes, `src` depois

O `dependency:go-offline` levou 2m42 neste pom. Esses 167 segundos são pagos a cada build —
exceto quando a ordem das instruções protege a camada deles.

**Mecanismo.** O Docker guarda uma camada por instrução, identificada pelo hash dos arquivos
copiados + o texto da instrução + o hash da camada anterior. Num rebuild, hash bate →
`Using cache`; hash manda → a instrução roda de novo **e tudo que vem depois dela também**,
mesmo que aquilo não tenha mudado. A ordem, portanto, decide quem é vítima da mudança:

```text
FROM maven:3.9-eclipse-temurin-17 AS build   ─┐
COPY pom.xml .                               ─┤ pom não mudou → cache vale
RUN mvn -B dependency:go-offline             ─┘ (2m42 protegidos)
COPY src ./src                               ← editei NoteService.java → hash muda
RUN mvn -B package -DskipTests               ← precisa rodar de novo (2m42 de novo,
                                               se as deps tiverem de ser rebaixadas)
```

O erro clássico é `COPY . .` antes do build: aí qualquer arquivo que mude no diretório — o
`target/` que o seu `spring-boot:run` acabou de reescrever, um `.env` editado, uma Issue nova
no `.tracker/` — invalida a camada das dependências e você volta a esperar 2m42 para "mudar um
comentário". A ordem correta (`COPY pom.xml .` → `dependency:go-offline` → `COPY src ./src`)
é só isso: **separe o que muda raramente do que muda sempre**, no sentido da leitura de cima
para baixo.

O inverso também é o cache funcionando, não falhando: trocar a versão do
`bucket4j-spring-boot-starter` ou do `springdoc-openapi` no `pom.xml` invalida daí em diante —
porque as dependências realmente mudaram.

Nota honesta: `dependency:go-offline` não é perfeito. Ele resolve a maior parte dos jars, poms
e plugins, mas há cenários (parent poms de BOM, plugin que só é baixado na hora de executar) que
ainda acontecem no `package`. Ele não elimina a rede; ele empurra a maior parte para uma camada
que só se invalida quando o pom muda — que é justamente quando *deve*.

Para enxergar sem adivinhar:

```bash
docker build -t notes-api . 2>&1 | grep -E 'CACHED|DONE'
docker build --progress=plain -t notes-api .   # mostra cada camada e o que reaproveitou
```

**Fronteira entre Issues:** invalidar o cache quando o código mudou de verdade é o build
refazendo o que precisa — não é defeito para esta Issue corrigir. O que *entra* no contexto de
build é o tópico abaixo; rodar a suíte antes de virar imagem é responsabilidade do host/CI
(**Issue 04**, **Trilha 2**), não do Dockerfile.

## `.dockerignore`: o contexto é um tar que sai da sua máquina antes de qualquer instrução

**Por que importa.** `docker build` não "lê alguns arquivos do diretório": ele empacota o
diretório inteiro — o **contexto** — num tar e manda para o daemon *antes* de interpretar a
primeira linha do Dockerfile. Neste lab a conta é visível:

```bash
du -sh .                                              # 69M  (68M é o target/)
du -sh --exclude=target --exclude=.tracker --exclude=.env .   # 300K
```

**Mecanismo — três custos distintos**, para não tratar `.dockerignore` como enfeite:

1. **Transferência toda build.** O tar é enviado sempre, mesmo com cache quente. 69 MB a cada
   `docker build` na sua máquina; 69 MB a cada build no CI e na VM.
2. **Conteúdo dentro da imagem.** Se houver um `COPY . .`, os 68 MB do `target/` entram na
   camada — incluindo o jar local de 70 MB que a imagem nem vai usar (ela copia o do estágio de
   build), o `classes/` e os relatórios do `surefire-reports`.
3. **Segredo e lixo no contexto.** O `.env` deste lab carrega `JWT_SECRET`, senha de banco e
   `CORS_ALLOWED_ORIGINS`; `.tracker/` carrega as Issues. Não é o Docker que vaza sozinho — é
   um `COPY . .` inocente transformando contexto em imagem.

```dockerignore
# .dockerignore do lab
target/
.git/
.tracker/
.env
*.iml
.idea/
```

Uma particularidade honesta deste repo: hoje **não existe `.git/` dentro do contexto** — a raiz
do repositório é um nível acima (`../.git`), então listar `.git/` aqui parece inútil. Parece,
até o build rodar de um checkout inteiro, que é o que acontece no CI (**Trilha 2**) e quando a
stack é reconstruída na VM (**Issue 04**). Aí entra a distinção que ninguém ensina junto:
`.gitignore` é a regra do **git** (o que vai para o commit) e `.dockerignore` é a regra do
**daemon** (o que vai para o tar). São dois mecanismos independentes — nenhum lê o arquivo do
outro, e o `.gitignore` deste repo já deixa `target/` e `.env` de fora do commit sem fazer nada
pelo contexto de build.

**Fronteira entre Issues:** segredo de verdade não pode entrar na imagem por nenhum caminho
(`ENV`, `ARG`, `COPY`) — como o runtime lê config do ambiente (`env_file`, variáveis do
`.env.example`) é da **Issue 02**. Aqui se garante uma coisa só: ele não entra no contexto nem
na camada `COPY`.

## Root dentro do container é o default que precisa ser desligado

**Por que importa.** Toda imagem base sobe como uid 0 por padrão, e root dentro do container é
mais poderoso do que parece: escreve em volume montado (arquivo com dono root na **host**),
lê tudo o que está dentro da imagem, e numa falha de escape ou montagem errada é o degrau para
a máquina — que depois da Trilha 0 com hardening é justamente o que não pode acontecer. É o mesmo
princípio de menor privilégio da Trilha 0 aplicado a quem *executa*, não a quem autentica.

**Mecanismo — a ordem importa**, porque o `USER` é um interruptor que não volta atrás:

```dockerfile
FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 --create-home notes   # cria usuário com uid FIXO
WORKDIR /opt/notes
COPY --from=build /app/target/notes-api-0.0.1-SNAPSHOT.jar app.jar
RUN chown notes:notes /opt/notes app.jar               # ainda root: precisa de permissão
USER notes                                             # daqui para frente: uid 10001
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- Tudo **antes** do `USER` roda como root — é o build sendo root, não a app. Tudo **depois**
  roda como `10001`, e é esse número que `docker exec ... id -u` devolve.
- O `chown` precisa ficar antes do `USER`: depois que a instrução vira non-root, ela não tem
  permissão para corrigir dono de arquivo — e o Java falha com `Permission denied` na abertura
  do jar, num erro que parece da app e é do Dockerfile.
- **uid declarado, não herdado.** Se a base trocar o usuário padrão, os arquivos de um volume
  escritos pelo usuário antigo viram `permission denied` sem aviso. `--uid 10001` transforma o
  dono numa decisão escrita, reproduzível entre builds e entre máquinas.
- `USER` é o default da imagem, não uma lei: `docker run -u 0` sobrescreve. Quem tem essa mão
  já tem o daemon Docker, que roda como root da máquina — esta proteção é contra escape,
  imagem comprometida e volume mal montado, não contra o administrador.

Por que a Issue insiste em criar o usuário **no Dockerfile** em vez de "a base já tem um":
nas bases comuns o uid default não é garantido nem documentado como contrato. Herdar é aceitar
que a próxima atualização da base mude o dono dos seus arquivos.

Prova, sem interpretar log:

```bash
docker run -d --name probe -e SPRING_PROFILES_ACTIVE=dev notes-api
docker exec probe id -u                 # 10001 (≠ 0)
docker history notes-api --no-trunc | grep -i user
```

**Fronteira entre Issues:** capabilities, `--read-only`, remap de user namespace e
`--security-opt no-new-privileges` são aprofundamento futuro, não desta Issue; firewall, SSH e
updates da VM são **Trilha 0**; publicar porta no host é **Issue 03**.

## HEALTHCHECK: o orquestrador não enxerga o processo, enxerga o probe

**Por que importa.** Sem healthcheck, tudo o que qualquer supervisor sabe é "o container existe
e o PID 1 não morreu". Isso é verdade e não basta: a notes-api pode estar com a JVM viva e o
socket aberto, mas sem conseguir falar com o Postgres — aceitando conexão, respondendo erro ou
pendurada no Flyway. É a mesma lição do estudo da **Trilha 0-04**: lá o systemd observava o
código de saída do processo e via "saiu com 0" onde o terminal via um problema; aqui quem olha é
o Docker/orquestrador, e o que ele vê é a saída de um comando periódico. Nos dois casos,
processo vivo ≠ sistema saudável.

**Mecanismo:**

```dockerfile
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -fsS http://localhost:8080/api/v1/actuator/health || exit 1
```

- a cada `interval` o comando roda **dentro** do container; exit `0` conta como healthy, exit
  `≠ 0` acumula `retries` e vira unhealthy;
- os estados são `starting` → `healthy` / `unhealthy`. O `start-period` existe por causa do
  boot do Spring (Flyway, contexto, segurança): sem ele os primeiros probes falham e a imagem
  nasce marcada como doente;
- o caminho **não** é `/actuator/health`: o `application.yml` deste lab declara
  `server.servlet.context-path: /api/v1`, e o Actuator herda o prefixo. Apontar para `/` ou
  `/actuator/health` devolve `404` e o container fica `unhealthy` eterno com a app perfeita —
  probe errado é pior que probe nenhum, porque ele mente para o supervisor;
- quem responde é a própria app agregando componentes: `management.endpoints...include:
  health,info,prometheus` e o grupo `readiness: readinessState,ping,db`. Sem Postgres, o
  corpo volta `DOWN` mesmo com a JVM feliz — é a app dizendo "não estou pronta".

Ferramenta do probe é parte do trabalho, não detalhe: confira em vez de assumir.

```bash
docker run --rm eclipse-temurin:17-jre sh -c 'command -v curl wget useradd'
# /usr/bin/curl, /usr/bin/wget, /usr/sbin/useradd  (nesta base: presente)
```

Trocou a base (por exemplo, alpine) e a ferramenta sumiu? Aí instalar na build ou escolher
base com ela é decisão sua — o que não pode existir é `HEALTHCHECK` apontando para comando
inexistente: ele falha sempre, e o estado fica `unhealthy` pelo motivo errado.

**Quem enxerga o que:**

| Quem | O que faz com o estado |
|---|---|
| `docker run` simples | nada — só `docker inspect --format '{{.State.Health.Status}}' probe` |
| `docker compose` | `depends_on: condition: service_healthy` (é assim que a API espera o banco) |
| k8s / swarm | tira do balanceamento, reinicia ou substitui o pod |

**Fronteira entre Issues:** a Issue 01 declara o healthcheck e aponta para o caminho certo;
**ordenar a subida** (banco healthy primeiro, API depois, portas fechadas) é da **Issue 02**,
e é lá que essa declaração passa a agir. Reverse proxy/TLS é **Issue 03**. E vale o alerta da
própria Issue: com o banco fora, o estado vira `unhealthy` com o processo vivo — não é defeito
do healthcheck, é ele trabalhando.

## Mapa rápido: quem cuida de qual parte

| Parte | De quem é |
|---|---|
| multi-stage, `.dockerignore`, `USER` não-root, `HEALTHCHECK` | Issue 01 (esta) |
| compose, ordenação por saúde, `env_file`, portas do banco fechadas | Issue 02 |
| proxy, TLS, exposição pública | Issue 03 |
| build e transferência da stack para a VM com hardening | Issue 04 |
| build/push de imagem no CI | Trilha 2 |

## Como iniciar o modo teach-anything

- "Me ensina multi-stage com o Dockerfile da notes-api: o que atravessa do estágio `build` para
  o `runtime` e por que o Maven fica para trás"
- "Me ensina camadas e cache do Docker usando o `pom.xml` e o `src/` deste repo, com a ordem
  `COPY pom.xml` → `dependency:go-offline` → `COPY src`"
- "Me ensina `.dockerignore` olhando os 69 MB deste diretório e mostrando o que um
  `COPY . .` sem ele colocaria na imagem"
- "Me ensina `USER` não-root com uid fixo no Dockerfile da notes-api, provando com
  `docker exec probe id -u` e `docker history notes-api`"
- "Me ensina `HEALTHCHECK` com o `/api/v1/actuator/health` do `application.yml`: como o estado
  do container vira `starting`, `healthy` e `unhealthy`"
