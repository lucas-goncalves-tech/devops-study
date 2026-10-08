# Estudo — Compose: ordenar por saúde, não por pressa

> Material de estudo da Trilha 1. Acompanha a Issue 02 (a stack sobe na ordem certa e o
> banco fica fechado), mas não é o contrato daquela Issue: aqui é para entender, lá é para
> provar.

## "Subiu" não é "está pronto": o Flyway é quem cobra a diferença

Três terminais com `docker run` funcionam porque **você** sabe a ordem: espera o Postgres
aceitar conexão, aí sobe a API. O `docker compose up -d` não tem essa memória — ele sobe os
containers e acabou. A consequência não é "algum warning no log": a API morre.

- **Por que importa:** o Flyway roda na subida do Spring (`spring.flyway.enabled: true` no
  `application.yml`) e a primeira coisa que ele faz é abrir conexão com
  `jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}`. O Postgres oficial demora um pouco
  depois de `Running initial setup` — ele gera o cluster antes de escutar o socket. Nesse
  intervalo a API recebe `Connection refused`, o Flyway falha, o Spring aborta e o container
  da app termina. Pior: como a falha é na subida, ela parece instabilidade da app — e a
  pessoa que está aprendendo passa a achar que Spring Boot "é assim mesmo".
- **Mecanismo:** `depends_on` (forma curta) só ordena **criação e start** dos containers.
  Ele responde a "o processo do db começou?", não a "o db já aceita conexão?". A forma longa
  troca a pergunta por uma condição consultada no **healthcheck** do serviço de origem:

  ```text
  depends_on: db                depends_on: db + condition: service_healthy
        │                                   │
        ▼                                   ▼
  container db iniciado            healthcheck do db respondeu "healthy"
        │                                   │
        ▼                                   ▼
  app sobe AGORA (cedeira          app só sobe DEPOIS do db aceitar conexão
  ocupada, banco vazio)            (Flyway encontra o banco vivo)
  ```

  Causa e efeito: sem `condition`, a ordem é *processo criado*; com `condition:
  service_healthy`, a ordem é *capacidade confirmada*. O compose espera, com timeout — se o
  serviço nunca ficar `healthy`, ele desiste com `dependency failed to start` em vez de
  deixar a app quebrar em loop.

- **Exemplo no app:** o compose desta Issue tem a `app` dependendo dos dois deps de uma vez
  — porque a app usa banco (Flyway/JPA) **e** Redis (rate limit do Bucket4J,
  `cache-to-use: redis-jedis`):

  ```yaml
  services:
    app:
      image: notes-api          # imagem construída na Issue 01
      env_file: .env
      depends_on:
        db:
          condition: service_healthy
        redis:
          condition: service_healthy
      ports:
        - "127.0.0.1:${APP_PORT:-8080}:8080"
      restart: unless-stopped
  ```

  A prova de que a ordenação aconteceu é cronológica, não opinativa:
  `docker compose logs -t db | tail` com timestamps **anteriores** aos de
  `docker compose logs -t app | head` (o `db` em `healthy` antes do `Started` da app).

- **Fronteira entre Issues:** `condition: service_healthy` protege **a subida**, não a
  queda. Se o banco cair no meio do expediente, ninguém reinicia a app — ela só fica
  `unhealthy` (evidência). Quem reanima processo é o `restart: unless-stopped`, o análogo do
  `Restart=on-failure` da **Trilha 0-04**: ele olha o *processo* (saiu ou não), não o
  *healthcheck*. Auto-recuperação de falha em runtime, alerta e ação são **Trilha 3**;
  orquestração de verdade (k8s, swarm `deploy:`) é explicitamente fora do escopo desta
  Issue.

## O healthcheck é readiness aplicado a container — e o lab já tem os dois sinais

A **Trilha 0-04** ensinou que o supervisor só decide alguma coisa se tiver um sinal para
ler (`Restart=on-failure` lê o código de saída). Aqui o supervisor é o Docker, e o sinal é
o `HEALTHCHECK`: sem ele, `docker compose ps` mostra só `Up` — "o processo existe" — e
`condition: service_healthy` não tem nada para consultar.

- **Por que importa:** sem healthcheck, todo mundo fala a língua errada. "Está no ar"
  (`Up`) convive com a API respondendo `Connection refused` no Flyway; e o atalho óbvio —
  usar o `HEALTHCHECK` como liveness, reiniciando tudo que "está ruim" — reinicia a app
  quando quem quebrou foi o banco, em loop eterno.
- **Mecanismo:** três perguntas diferentes, três respostas diferentes. É a mesma distinção
  de readiness/liveness da Trilha 0-04, agora aplicada a container:

  | Pergunta | Quem responde | No lab |
  |---|---|---|
  | O db **aceita conexão agora**? | `healthcheck` do container (`pg_isready`) | ordena a subida da app |
  | Posso **mandar tráfego** para a app? | readiness do Actuator | inclui `db` — cai quando o banco cai |
  | O **processo** da app está vivo ou em loop morto? | liveness do Actuator | não inclui `db` — não cai por causa do banco |

  O `application.yml` real já traz essa separação escrita:

  ```yaml
  management:
    endpoint:
      health:
        probes:
          enabled: true        # expõe /actuator/health/liveness e /readiness
      group:
        readiness:
          include: readinessState,ping,db   # readiness SENTE o banco
  ```

  Causa e efeito: `docker compose stop db` → o `db` do grupo readiness passa a `DOWN` →
  `/api/v1/actuator/health/readiness` responde `503` e o healthcheck do container da app
  vira `unhealthy` (é a evidência da Issue) — mas a **liveness** continua `UP`, ou seja,
  ninguém "reinicia a app quebrada" por um problema que é do banco. Reiniciar a app não
  conserta o Postgres; só perde o estado em memória.

- **Exemplo no app:** os três healthchecks, um por serviço. Repare no `$$` do banco — o
  compose interpola `${...}`/`$...` **no host**; `$$` escapa para que quem expanda
  `$POSTGRES_USER` seja o shell **dentro** do container, onde essa variável existe:

  ```yaml
    db:
      image: postgres:15
      environment:
        POSTGRES_DB: ${DB_NAME}
        POSTGRES_USER: ${DB_USERNAME}
        POSTGRES_PASSWORD: ${DB_PASSWORD}   # valor vem do .env, nunca escrito aqui
      healthcheck:
        test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER"]
        interval: 5s
        timeout: 3s
        retries: 10
    redis:
      image: redis:7
      healthcheck:
        test: ["CMD", "redis-cli", "ping"]   # PONG → exit 0 → healthy
    app:
      image: notes-api
      # sem healthcheck aqui: a imagem já nasceu com o HEALTHCHECK
      # da Issue 01 apontando para /api/v1/actuator/health
  ```

  Verificação no lab: `docker compose exec db pg_isready` → `accepting connections`;
  `curl 127.0.0.1:8080/api/v1/actuator/health` → `200` com `"db":{"status":"UP"}` (o
  context-path é `/api/v1`, do `server.servlet.context-path`). Parar o banco e esperar o
  `interval` é o que faz o estado da app virar `unhealthy`; `start db` devolve `healthy`.

- **Fronteira entre Issues:** o `HEALTHCHECK` **da imagem** (o que ele mede, por que aponta
  para o Actuator, semântica de exit code) é **Issue 01** — aqui ele é só herdado. O
  conteúdo do Actuator (o que é `prometheus`, o que é readiness de verdade) é da
  instrumentação que já veio pronta no lab; alertar sobre `unhealthy` é **Trilha 3**.

## Por que `db` e `redis` não têm `ports:` — e por que a app é `127.0.0.1:`

A regra prática "só a API tem seção `ports:`" não é estilo: é a fronteira entre *falar
dentro da stack* e *aceitar conexão do mundo*.

- **Por que importa:** `ports:` é uma publicação na **host** — ela grava regra de
  encaminhamento no netfilter da máquina e faz a porta escutar fora do container. Sem ela,
  quem fala com o Postgres é só quem está na rede do compose. Numa VM atrás do NAT de
  desenvolvimento isso não dói; na VPS pública (Trilha 2+) `5432` publicada é banco de
  venda exposto — e o pior é que **dói tarde**: nada quebra hoje, a VM fica exposta por
  semanas, e o dia ruim é o dia em que alguém varre a internet inteira.
- **Mecanismo:** os dois caminhos são independentes:

  | | Rede interna do compose | Publicação (`ports:`) |
  |---|---|---|
  | Alcance | containers da mesma rede | interfaces da host (`0.0.0.0` por padrão) |
  | Precisa de `ports:`? | não | sim |
  | Quem usa | `app` falando com `db:5432` | `psql`/`redis-cli` de **fora**, você, cliente externo |
  | Sem ela, de fora | inacessível | inacessível |

  Detalhe que decide o nível de exposição: **`ports: - "8080:8080"` escuta em `0.0.0.0`** —
  todas as interfaces, inclusive a placa que fala com a rede/Internet. Por isso o requisito
  da Issue é o prefixo explícito `127.0.0.1:`: a mesma porta, porém só no loopback, como se
  a app estivesse atrás da mesma política "só o explícito passa" da **Trilha 0-03**.

- **Exemplo no app:** o compose inteiro, mostrando a assimetria (repare: zero `ports` em
  `db` e `redis`, e a forma `${APP_PORT:-8080}` que troca de porta sem rebuild):

  ```yaml
  services:
    db:      { ... }            # NÃO tem seção ports:
    redis:   { ... }            # NÃO tem seção ports:
    app:
      ports:
        - "127.0.0.1:${APP_PORT:-8080}:8080"   # host:container, host travada no loopback
  ```

  Prova no host: `ss -tlnp | grep -E ':(5432|6379)\b'` → **vazio**; `6379` também. A app
  continua falando com o banco normalmente (a conexão é interna, não passa pela host) —
  teste com `docker compose exec db pg_isready`. E trocar `APP_PORT=8090` no `.env` +
  `docker compose up -d` move a porta **sem nenhum `docker build`**: imagem igual, comportamento
  diferente, porque config não é imagem.

- **Fronteira entre Issues:** o `127.0.0.1:` é o default **desta** Issue e muda de propósito
  na **Issue 03**: o reverse proxy passa a publicar `80/443`, fala com a app pela rede interna
  (`app:8080`) e a publicação direta da app some do YAML. Na **Issue 04** (VM pública) o `ufw`
  da Trilha 0-03 é quem recebe as regras 80/443 — mas não conte só com ele para portas
  publicadas pelo Docker: o Docker escreve as regras dele na cadeia `DOCKER` antes das regras
  do ufw, por isso a regra de segurança aqui é **não publicar** o banco, não "publicar e
  filtrar". Escutar `127.0.0.1` e não publicar são duas camadas, não uma.

## Credencial no YAML é commit para sempre: `env_file`, `${VAR:-default}` e o `JWT_SECRET`

`grep password compose.yaml` devolvendo `postgres` é o erro mais barato de cometer e o mais
caro de limpar — e o lab já ensinou o porquê na leitura do README.

- **Por que importa:** o YAML do compose vai para o git, e git é **histórico**, não
  arquivo. Mesmo que você corrija depois, o valor continua em cada `git log -p` e em todo
  clone existente; na Trilha 2 o repositório passa a ser acessível por token de CI, e
  segredo vazado não se revoga com `git commit --amend` — se revoca trocando a credencial.
  Por isso o critério da Issue é literal: `grep -iE 'password|secret' compose.yaml` → só
  `${...}` referenciado, nenhum valor literal.
- **Mecanismo:** dois papéis diferentes, que o compose separa — e confundir os dois é a
  origem do "coloquei no `.env` e não pegou":

  | | `env_file: .env` | Interpolação `${VAR:-default}` |
  |---|---|---|
  | Quem lê | o compose, **antes** de criar o container | o compose, **ao interpretar o YAML** |
  | Para onde vai | vira variável **dentro do container** | substitui o texto **no YAML** |
  | No lab | app lê `DB_HOST`, `JWT_SECRET` em `application.yml` | `127.0.0.1:${APP_PORT:-8080}:8080` |

  `:-` significa "usa o default se a variável estiver **ausente ou vazia**" — por isso a
  porta da app funciona mesmo sem `APP_PORT` no `.env`. Já `JWT_SECRET` **não pode ter
  default**: no `application.yml` ele está como `jwt.secret: ${JWT_SECRET}` (sem `:` de
  fallback, sintaxe do Spring) e o README é direto — "a app não tem default para
  `JWT_SECRET` e a suíte falha sem ele". Segredo com default confortável é segredo que
  ninguém percebe ausente: o comportamento certo é **falhar alto** na subida, não subir com
  `secret=changeme`. Configuração de conforto ganha default; credencial, nunca.

- **Exemplo no app:** o `.env.example` é o modelo que o repositório carrega, e o `.env`
  real fica de fora (`.gitignore` tem a linha `.env`):

  ```bash
  DB_NAME=notes
  DB_USERNAME=postgres
  DB_PASSWORD=postgres      # valor de LAB, mas mesmo assim não entra no YAML
  DB_HOST=localhost         # localhost muda dentro do compose → vira "db"
  JWT_SECRET=much-longer-secret-with-32-characteres-for-secure   # sem default: é obrigatório
  APP_PORT=8080             # opcional: sem ele, ${APP_PORT:-8080} resolve sozinho
  ```

  Causa e efeito de trocar comportamento sem rebuild: a porta acima, o profile
  (`SPRING_PROFILES_ACTIVE`) e as credenciais são lidos em runtime — por isso `up -d` após
  editar `.env` já basta, e o digest da imagem não muda (`docker image inspect` para
  provar). O que **não** muda sem rebuild é código compilado: isso é imagem, e é a Issue 01.

- **Fronteira entre Issues:** `docker compose down -v` é a limpeza do **volume** `pg_data`
  (estado do banco) — sem `-v` o volume sobrevive e o próximo `up` reencontra o estado
  antigo, a mesma armadilha de restore que a **Trilha 0-05** ensinou com backups. Vault,
  Docker secrets, rotação e contrato formal de segredos são **Trilha 4**; aqui o entregável é
  só o hábito: valor no `.env`, referência no YAML, `.env.example` versionado.

## A stack tem uma rede própria: `app` fala com `db:5432`, nunca com `localhost`

- **Por que importa:** a configuração que funciona no `mvnw spring-boot:run` (o
  `.env.example` vem com `DB_HOST=localhost` e `REDIS_HOST=localhost`) **quebra dentro do
  container** — e quebra do jeito enganoso: `Connection refused` numa porta que "está
  aberta", porque quem está ouvindo em `localhost` é outro processo. `localhost` dentro de
  um container é **o próprio container**: a app não tem Postgres nela, logo nada escuta em
  `localhost:5432`.
- **Mecanismo:** ao subir, o compose cria uma rede bridge padrão (chamada
  `<projeto>_default`, onde `<projeto>` é o nome do diretório) e conecta os três containers
  a ela. Cada container recebe o resolver interno do Docker (`127.0.0.11`) no
  `/etc/resolv.conf`, que traduz **nome de serviço** em IP do container:

  ```text
  app  --("db")--> 127.0.0.11  -->  IP do container db na bridge  -->  :5432
   │
   └──("localhost")--> ele mesmo --> ninguém escuta --> Connection refused
  ```

  Publicar `ports` do db não entra nessa conta: publicação é caminho **host → container**;
  o tráfego container → container usa a bridge e acontece mesmo com zero portas publicadas.
  Ou seja: `db` sem `ports:` não é app "sem acesso ao banco" — é **mundo** sem acesso ao
  banco.

- **Exemplo no app:** o mesmo `.env` muda de valor, não de forma — nada no
  `application.yml` muda, só o que o ambiente entrega:

  ```bash
  DB_HOST=db            # era localhost (dev local); no compose é o nome do serviço
  REDIS_HOST=redis      # rate limit do Bucket4J fala com o serviço, não com a host
  ```

  Conferir a resolução na prática (é ver, não é acreditar):

  ```bash
  docker compose exec app getent hosts db      # devolve o IP do container db na bridge
  docker compose logs -t db | tail             # aceitando conexão
  docker compose exec app sh -c 'echo $DB_HOST'   # db — o env_file chegou
  ```

  E o teste fim-a-fim é o health da própria app: `curl 127.0.0.1:8080/api/v1/actuator/health`
  → `200` com `"db":{"status":"UP"}`. Se ele responder `DOWN`, o caminho completo
  (`app` → `db:5432` → Flyway/JPA) é que está quebrado, não a porta da host.

- **Fronteira entre Issues:** quem alcança quem **de fora** é o oposto: a host só alcança o
  que foi publicado (tópico das portas) — e a partir da **Issue 03** nem isso: o proxy
  entra na mesma rede, fala com `app:8080` pelo nome e vira o único caminho público, o que
  é exatamente o payoff de ter a rede interna existindo desde aqui. Redes customizadas
  (`networks:` nomeadas, isolamento entre grupos de serviços) e `container_name`/`links`
  (depreciado) estão fora desta Issue — modo simples apenas.

## Como iniciar o modo teach-anything

- "Me ensina a diferença entre `depends_on` simples e `condition: service_healthy`
  usando o `compose.yaml` desta stack e o Flyway da `notes-api` quebrando na subida"
- "Me ensina readiness vs. liveness aplicados a container com o `pg_isready` do `db` e os
  grupos `readiness`/liveness do `actuator/health` desta app"
- "Me ensina port mapping explicando por que `db` e `redis` não têm `ports:` e por que a
  app usa `127.0.0.1:${APP_PORT:-8080}:8080`, verificando com `ss -tlnp` nesta máquina"
- "Me ensina `env_file` e `${VAR:-default}` usando o `.env.example` real e o `JWT_SECRET`
  que não tem default no `application.yml`"
- "Me ensina a rede padrão do compose mostrando por que `DB_HOST=db` funciona e
  `DB_HOST=localhost` dá `Connection refused`, com `docker compose exec app getent hosts db`"
