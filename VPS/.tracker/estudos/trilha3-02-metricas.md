# Estudo — Métricas: o Actuator já fala, falta quem escute

> Material de estudo da Trilha 3. Acompanha a Issue 02 (Prometheus coleta, Grafana mostra,
> o Actuator responde), mas não é o contrato daquela Issue: aqui é para entender, lá é para
> provar.

## O alvo expõe, o coletor varre: por que métrica não é o app que manda

Hoje o `notes-api` entrega 107 métricas em `/api/v1/actuator/prometheus` e **ninguém lê**.
O texto existe por alguns segundos a cada `curl` e morre ali: sem coletor, não há série, não
há histórico, não há "como estava ontem". É por isso que a pergunta "como está a app?" ainda
responde com `docker logs`.

- **Por que importa:** instrumentar é metade do trabalho; a outra metade é ter alguém
  consultando o endpoint em intervalo regular e guardando a resposta. Sem o coletor, o
  Actuator é um console escondido — e a armadilha é agradável: `curl` funciona, os 107
  números aparecem, dá a sensação de que "já temos observabilidade". A dor só aparece
  depois: pergunta que precisa de **histórico** ("a latência piorou depois do deploy da
  T2-03?") não tem resposta, porque ninguém gravou o antes.
- **Mecanismo:** o modelo do Prometheus é **pull** — o alvo expõe, o coletor varre. Causa e
  efeito, em quatro passos:

  ```text
  request chega em /api/v1/notes
        │
        ▼
  filtro HTTP do Spring incrementa um Timer (Micrometer, dentro da JVM)
        │
        ▼
  micrometer-registry-prometheus renderiza a registry em texto exposition 0.0.4
  (# HELP / # TYPE / nome{labels} valor) em GET /actuator/prometheus
        │
        ▼
  o Prometheus faz GET nesse texto a cada 15s, parseia e grava uma amostra
  em disco (volume da TSDB) → a série existe a partir daí
  ```

  Por que **não** é o app que "manda" a métrica para um servidor:

  | | Pull (Prometheus) | Push (app manda) |
  |---|---|---|
  | Quem sabe o endereço do quê | o coletor, num único `scrape_configs` | cada app, com endpoint de destino |
  | App nova na stack | 1 linha de YAML | deploy de config em N apps |
  | Caminho de rede | coletor → alvo (cabe atrás de firewall/NAT, como a rede interna do compose) | alvo → coletor (o app precisa alcançar o coletor) |
  | App morreu | **o próprio scrape falha** → `up` vira `0`, descoberto pelo ausente | o que morreu não manda nada; detectar é achar a *ausência* de push |

  A exceção clássica é o Pushgateway, para job curto (batch) que vive menos que o
  `scrape_interval` — não é o caso de uma API que fica no ar o tempo todo.

- **Exemplo no app:** a instrumentação veio pronta — `pom.xml` com
  `spring-boot-starter-actuator` + `micrometer-registry-prometheus`, e o `application.yml`
  abrindo o endpoint (sem `prometheus` no `include`, o GET responde `404` e o alvo cai por
  *caminho errado*, não por app estar fora do ar):

  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: health,info,prometheus
    metrics:
      tags:
        application: ${spring.application.name}   # → notes-api em toda série
  ```

  O que o endpoint devolve (formato texto que o coletor lê):

  ```text
  # HELP http_server_requests_seconds
  # TYPE http_server_requests_seconds summary
  http_server_requests_seconds_count{application="notes-api",exception="None",method="GET",uri="/notes",status="200"} 12.0
  http_server_requests_seconds_sum{application="notes-api",exception="None",method="GET",uri="/notes",status="200"} 0.418
  # HELP jvm_memory_used_bytes JVM memory used bytes
  # TYPE jvm_memory_used_bytes gauge
  jvm_memory_used_bytes{application="notes-api",area="heap",id="G1 Old Gen"} 1.18e8
  ```

  Conferir de dentro da stack (mesmo caminho que o coletor vai usar):

  ```bash
  curl -s http://127.0.0.1:8080/api/v1/actuator/prometheus | head -20   # texto exposition
  curl -s http://127.0.0.1:8080/api/v1/actuator/prometheus | wc -l      # ~107 métricas
  ```

- **Fronteira entre Issues:** o app é **intocado** nesta Issue — só o que o Actuator já
  emite (métrica de negócio, tipo "notas criadas por hora", exigiria mudar código: fora de
  escopo). O `health`/readiness/liveness do mesmo Actuator é da **Trilha 1-02** (é o que o
  healthcheck do container lê); sondar a *borda* de fora (blackbox exporter batendo no
  HTTPS do proxy) é estágio futuro. Métrica derivada de **log** ("erros por segundo" a
  partir dos arquivos da **Trilha 3-01**) é o elo que a **Issue 03** usa, mas a base numérica
  nasce aqui.

## Scrape interval, a métrica `up` e o que significa um target "estar para"

"Target UP" é a primeira tela de quem opera Prometheus — e é a única que responde
"está coletando?" antes de qualquer dashboard bonito.

- **Por que importa:** o alvo pode estar morto, o endpoint pode estar errado, a rede interna
  pode estar estranha — nos três casos o Grafana abre lindo e **vazio**, e a pessoa conclui
  erradamente que "o Grafana não funciona". O `/targets` separa os três casos em uma tela,
  e ele é a pré-condição de tudo (sem série fluindo, dashboard nenhum tem o que desenhar).
- **Mecanismo:** o Prometheus varre cada alvo no `scrape_interval` (default `15s`) e grava,
  para **cada alvo**, a métrica sintética `up`:

  | Situação no lab | `up` | Consequência |
  |---|---|---|
  | scrape respondeu `200` com texto | `1` | série da app acumulando normalmente |
  | conexão recusada / timeout (app parada) | `0` | série da app **para de avançar** |
  | resposta `404` (endpoint fora do `include`) | `0` | alvo "para" mesmo com app viva |
  | alvo nem está no `scrape_configs` | *não existe* | nem erro: a pergunta sequer é feita |

  Duas consequências que quase ninguém percebe de primeira:

  - **`up == 0` é descoberto pelo coletor**, não pelo app — é o ponto do modelo pull: o
    ausente se denuncia. Daí o critério `query=up == 0 → vazia` da Issue: nenhum alvo caiu
    *silencioso*.
  - **A série envelhece.** Depois de ~3 intervalos sem scrape, o Prometheus marca os pontos
    como *stale*: `up` pode até voltar a `1`, mas o buraco no meio fica registrado. Por isso
    `lastScrape < 15s` no `/targets` é a evidência pedida — "UP" sozinho pode ser resquício.

- **Exemplo no app:** a scrape config desta Issue, versionada no repo e montada por `volume`
  (nunca editada na UI — a UI do Prometheus é somente leitura de config):

  ```yaml
  # monitoring/prometheus.yml
  global:
    scrape_interval: 15s
    evaluation_interval: 15s

  scrape_configs:
    - job_name: app
      metrics_path: /api/v1/actuator/prometheus   # context-path do notes-api
      static_configs:
        - targets: ['app:8080']                   # nome do serviço na rede do compose

    - job_name: node                              # coletor da host escolhido (ver nota)
      static_configs:
        - targets: ['node-exporter:9100']
  ```

  Três detalhes de causa e efeito nesse bloco: `targets` usa o **nome do serviço**, não
  `localhost` (mesma regra da rede do compose — `localhost` dentro do container é ele
  mesmo); `metrics_path` carrega o `/api/v1` do `server.servlet.context-path`, e é o erro
  mais comum de scrape `up == 0` com app de pé; e `job_name` vira label de toda série
  daquele alvo, é como depois se separa app de infra.

  **Coletor de infra — escolha e justificação:** a Issue permite cAdvisor **ou**
  node_exporter, um só. Este estudo escolhe **node_exporter**: ele responde "a VM está
  cheia/CPU lá em cima?", que é pergunta de *host* — e a **Issue 03** já prevê a regra de
  disco com `node_filesystem`, que só existe com ele. cAdvisor responde "qual container está
  comendo?", pergunta de contenção entre serviços; ela entra quando existir a dúvida, não
  por antecipação (escopo de uma Issue por vez).

  Prova de que está varrendo, em três comandos:

  ```bash
  curl -s http://127.0.0.1:9090/api/v1/targets \
    | jq '.data.activeTargets[] | {job, health, lastScrape}'   # health: up, lastScrape: 3s
  curl -sG http://127.0.0.1:9090/api/v1/query --data-urlencode 'query=up'
  # → {"result":[{"metric":{"job":"app",...},"value":[...,"1"]}]}

  docker compose stop app                    # provocar a queda
  # em ≤ 15s: health vira "down" e o query acima devolve "0" para job=app
  docker compose start app                   # voltar → health "up", série respiro
  ```

- **Fronteira entre Issues:** **aviso** de `up == 0` (regra com `for: 1m` entregue em
  canal) é a **Issue 03** — aqui a métrica existe e é consultável, ninguém é incomodado;
  o que **fazer** quando o alvo cai é o runbook da **Issue 04**. Alertar que o próprio
  Prometheus parou (monitorar o monitor) é blackbox/heartbeat, estágio futuro — se o
  coletor cai, `up` de todo mundo some junto, e regra sobre série morta não dispara.

## Série temporal é nome + labels: a cardinalidade que ninguém vê chegar

Prometheus não guarda "a métrica" — guarda **uma linha por combinação de labels**, e cada
linha é uma série distinta com amostra a cada 15s. Escolher o que entra como label é decisão
de armazenamento, não de estilo.

- **Por que importa:** cardinalidade é a forma de uma stack pequena consumir a VM sem
  ninguém ter escrito uma linha pesada. Cada série nova é um índice + uma linha na TSDB +
  4 amostras por minuto; label com valor sem teto (id de usuário, caminho cru da URL, request
  id) multiplica tudo — e o sintoma aparece tarde: `disk full` no volume do Prometheus, query
  lenta, e a rotação de log da **Trilha 3-01** resolveu o log, não a série.
- **Mecanismo:** a conta é multiplicativa. O `http_server_requests_seconds` desta app tem
  `application`, `method`, `uri`, `status`, `exception`:

  ```text
  http_server_requests_seconds_count{uri="/notes/{id}", status="200", method="GET", ...}
  http_server_requests_seconds_count{uri="/notes/{id}", status="404", method="GET", ...}
                                  │
                                  └─ cada valor de label que muda = outra linha na TSDB
  ```

  | O que vira label | Séries criadas | Custo |
  |---|---|---|
  | `uri="/notes/{id}"` (padrão da rota) | 1 por rota | trivial — é o que o Spring entrega |
  | `uri="/notes/17", "/notes/18"...` (caminho cru) | 1 **por nota existente** | explode a cada INSERT do usuário |
  | `status="200"/"404"/"401"` | ×3 (faixa finita, sempre) | barato — é o eixo do painel de erro |
  | `exception="None"/"NotFound"...` | ×poucos | barato |

  O Micrometer protege por padrão: ele usa o **padrão** da rota (`/notes/{id}`), não o ID —
  é por isso que a cardinalidade aqui é benigna. O perigo é quem instrumenta na mão e cola
  `userId`/`noteId` "para filtrar depois": 10 mil notas × 3 status = 30 mil séries de uma
  rota só, e aí `uri` deixou de ser dimensão e virou dado. Regra prática: **label é para
  filtrar e agrupar (dimensão), nunca para identificar um registro**.

  O mesmo formato serve para virar número útil — a taxa só existe porque contador não é
  "quantos por segundo" sozinho (e `rate` tolera o *reset* de contador quando o container
  reinicia):

  ```promql
  # taxa de requisições por status nos últimos 5s (soma dos scrapes de 1m)
  sum by (status) (rate(http_server_requests_seconds_count[5m]))

  # erro 5xx por minuto — o número que a Issue 03 vai alertar
  sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) * 60

  # latência p95 (histograma) e uso de heap
  histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[5m])))
  rate(jvm_memory_used_bytes{area="heap"}[5m])
  ```

- **Exemplo no app:** gerar o sinal de verdade (o dashboard tem que reagir a tráfego
  real, não ao estado recém-ligado) e ver a série andar em ≤ `scrape_interval`:

  ```bash
  for i in $(seq 1 30); do
    curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1/api/v1/notes/nao-existe
  done   # 404 de graça: o GlobalExceptionHandler responde e o filtro HTTP conta

  curl -sG http://127.0.0.1:9090/api/v1/query \
    --data-urlencode 'query=sum by (status) (rate(http_server_requests_seconds_count[1m]))'
  # → status="404" aparece com valor > 0; antes do tráfego, a série nem existia
  ```

- **Fronteira entre Issues:** **reduzir** cardinalidade dentro do app (remover tag,
  trocar `uri` cru por padrão) é mexer em código — app intocado, fora do escopo daqui;
  o lab aceita o padrão do Micrometer. Retenção longa e indexação externa (Thanos/Mimir),
  exemplars e link métrica→trace são estágio futuro (APM é explicitamente fora); o que
  esta Issue faz é só escolher labels que **não explodam** e versionar a config.

## Datasource e dashboard no git: clique manual não sobrevive a máquina limpa

O Grafana vem vazio e "intuitivo": clique em *Add data source*, aponte para o Prometheus,
salve, monte o painel, salve. Tudo isso grava no banco do próprio Grafana — e é exatamente
lá que a informação morre.

- **Por que importa:** a promessa da Trilha 2 é `deploy` sem mão e rollback por
  (`T2-04`) — um Grafana configurado a mão é o único componente da stack que **não**
  reconstrói. Teste brutal e barato: `docker compose down -v` num laboratório novo, subir
  de novo e tentar abrir o dashboard. Sem provisionamento, o que aparece é "datasource
  not found" e painéis `No data`, e o conhecimento sobre *qualquer coisa* da stack virou
  memória de uma pessoa. Além disso: config que está no git tem diff, review e histórico —
  config só na UI não tem nem backup.
- **Mecanismo:** o Grafana lê, na subida, diretórios de **provisioning** — YAML que descreve
  datasource e provedor de dashboard — e carrega os JSON de painel de uma pasta. Causa e
  efeito: subir o container = montar tudo; a UI vira *sessão de visualização*, não de
  configuração. O caminho é fixo (`/etc/grafana/provisioning`), por isso o `volume` monta
  arquivo a partir do repo, `:ro`:

  ```yaml
  # monitoring/grafana/provisioning/datasources/datasource.yml
  apiVersion: 1
  datasources:
    - name: Prometheus
      type: prometheus
      access: proxy                     # o Grafana fala com prometheus:9090 na rede interna
      url: http://prometheus:9090
      isDefault: true
  ```

  ```yaml
  # monitoring/grafana/provisioning/dashboards/dashboards.yml
  apiVersion: 1
  providers:
    - name: lab
      folder: Trilha3
      type: file
      options:
      path: /var/lib/grafana/dashboards   # onde o JSON versionado é montado
  ```

  ```yaml
  # trecho do compose.yaml — os três volumes que fazem a mágica
    grafana:
      image: grafana/grafana
      environment:
        GF_SECURITY_ADMIN_USER: admin
        GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD}   # valor no .env (fora do git)
      volumes:
        - ./monitoring/grafana/provisioning:/etc/grafana/provisioning:ro
        - ./monitoring/grafana/dashboards:/var/lib/grafana/dashboards:ro
        - grafana-data:/var/lib/grafana                   # usuários/painéis *criados a mão* ainda sobrevivem
      ports:
        - "127.0.0.1:3000:3000"
      depends_on:
        prometheus:
          condition: service_healthy
  ```

  O JSON do painel entra no repo (dashboard da comunidade do grafana.com é import válido —
  **o JSON é que precisa estar rastreado**, `git ls-files` é a evidência), e o teste é o
  dashboard abrir **populado** depois de tráfego, não o `No data` do recém-ligado.

- **Exemplo no app:** os painéis desta Issue saem do que a app já emite — taxa de
  requisições 5xx (`http_server_requests_seconds_count{status=~"5.."}`) e recurso
  (`jvm_memory_used_bytes` / `process_cpu_usage`), mais o `up` do alvo. A prova de que
  datasource e painel batem é bater a query crua do Prometheus com a do painel:

  ```bash
  # mesmas duas queries, um lado no Prometheus e o outro no Grafana
  curl -sG http://127.0.0.1:9090/api/v1/query \
    --data-urlencode 'query=sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))'
  git ls-files monitoring/    # prometheus.yml + provisioning/*.yml + dashboards/*.json
  ```

- **Fronteira entre Issues:** **versionar** a infra (compose, CI, deploy) é das
  **Trilhas 1-02/2-01/2-03** — aqui o que entra no mesmo hábito é a config da ferramenta de
  observabilidade. Regras de alerta versionadas seguem o mesmo padrão, mas o conteúdo é da
  **Issue 03**. Gerenciar Dashboard-as-Code com `grafonnet`/`jsonnet` (gerar o JSON em vez
  de escrevê-lo) é evolução possível, não requisito — o entregável é o JSON rastreado.

## Ver o Grafana sem abrir borda: loopback e tunnel mantêm o ufw em 22/80/443

Ferramenta de observabilidade tem UI, e UI dá vontade de publicar porta. É aqui que a
Stack ganha uma regra nova no firewall "só pra conferir o dashboard" — e a régua de
**Trilha 0-03**/`T1-04` (22, 80, 443) é o que a Issue manda preservar.

- **Por que importa:** Grafana e Prometheus expostos em `0.0.0.0` são painel de status
  público (mapa da stack, versões, URLs internas) e, no caso do Prometheus, API aberta
  (`/api/v1/admin/tsdb/snapshot`, leitura de toda a série). Não há login obrigatório por
  padrão em `:9090`. E a dor é silenciosa: nada quebra, `ufw status` "está liberado", e a
  exposição dura semanas — o mesmo padrão de atraso que a Trilha 1-02 já desenhou para o
  banco. Pior: como o Docker escreve as regras dele antes das do ufw, "publicar e filtrar"
  não é defesa, é confiança na ordem das cadeias.
- **Mecanismo:** são **duas** chaves independentes, e a Issue exige que você declare
  qual usou (ou as duas):

  | | Loopback na publicação | SSH tunnel |
  |---|---|---|
  | No compose | `ports: ["127.0.0.1:3000:3000"]` | nem publica (`ports` ausente) |
  | Quem alcança | processos **da própria VM** | só quem tem chave SSH (`-L`) |
  | Acesso de fora | via outra máquina? não | `ssh -L 3000:127.0.0.1:3000` e abre `localhost:3000` |
  | Custo | preciso de um salto (o próprio SSH) para usar | sempre um terminal aberto |

  Nenhum dos dois altera o netfilter: a porta **existe** na VM, mas atrás do `127.0.0.1`,
  igual à `app` da Trilha 1-02 — o pacote vindo da Internet bate no `INPUT` do ufw e cai na
  política `deny`. Quem fala com o Prometheus dentro da stack é o Grafana, pela rede interna
  (`prometheus:9090`), sem nenhuma publicação envolvida.

- **Exemplo no app:** o compose de monitoring com as duas portas presas no loopback, e a
  conferência da borda:

  ```yaml
    prometheus:
      ports:
        - "127.0.0.1:9090:9090"     # UI/query para inspeção local
      # rede interna: grafana → http://prometheus:9090 (sem passar por aqui)
    grafana:
      ports:
        - "127.0.0.1:3000:3000"     # alternativa declarada: sem esta linha, use só tunnel
  ```

  ```bash
  ssh -N -L 3000:127.0.0.1:3000 lab@<ip-da-vm>    # em um terminal; browser em localhost:3000

  ss -tlnp | grep -E ':(3000|9090)'
  # → 127.0.0.1:3000 e 127.0.0.1:9090  (nunca 0.0.0.0:3000)
  sudo ufw status numbered
  # → 22, 80, 443 — inalterado: o monitoring NÃO virou regra nova
  ```

  A régua mental completa: `ports` com `127.0.0.1` (quem alcança na host), rede interna do
  compose (quem alcança entre containers) e ufw (quem alcança da rede externa) são **três
  camadas**, não uma opção entre outras.

- **Fronteira entre Issues:** as regras 80/443 são da **Trilha 1-03/1-04** (proxy na
  borda) e o `ufw` em si é da **Trilha 0-03** — esta Issue só se compromete a **não tocar**
  nele. A nota da própria Issue: o endpoint `/api/v1/actuator/prometheus` é permitido sem
  autenticação e isso só é aceito *porque* ele está atrás da rede interna e da borda única;
  se a app voltar a ser publicada direto, vira brecha (é a borda que sustenta a
  permissão). Autenticação/SSO do Grafana, TLS do dashboard e rate limit da UI são
  hardening da **Trilha 4**; alertar sobre exposição de porta não é assunto desta Issue.

## Como iniciar o modo teach-anything

- "Me ensina o modelo pull do Prometheus usando o `/api/v1/actuator/prometheus` do
  `notes-api` e o `scrape_configs` que aponta para `app:8080`"
- "Me ensina o que `up` e `scrape_interval` significam de verdade, testando com
  `docker compose stop app` e o `/targets` do Prometheus desta stack"
- "Me ensina cardinalidade de labels com o `http_server_requests_seconds` desta app —
  `uri` × `status` — e o que aconteceria se o `uri` viesse com o id cru da nota"
- "Me ensina provisioning do Grafana em YAML/JSON versionado, comparando com o clique
  manual e o teste da máquina limpa (`down -v` e subir de novo)"
- "Me ensina a expor ferramenta de observabilidade sem regra nova no ufw, com
  `127.0.0.1:3000:3000`, SSH tunnel e `ufw status` do lab"
