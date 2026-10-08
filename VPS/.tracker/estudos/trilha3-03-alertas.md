# Estudo — Alertas: o sistema chama antes do usuário reclamar

> Material de estudo da Trilha 3. Acompanha a Issue 03 (alertas com Prometheus + Alertmanager),
> mas não é o contrato daquela Issue: aqui é para entender, lá é para provar.

## Anatomia de um alerta: quem decide, quem confirma, quem explica

- **Por que importa:** a T3-02 deixou `notes-api` expondo 107 métricas e o Prometheus varrendo
  `app:8080/api/v1/actuator/prometheus` — mas **nada decide nada**. Alvo caído, rajada de 5xx e
  disco enchendo passam despercebidos até alguém abrir o Grafana, e "ninguém abriu" é o caso
  normal. Pior: escrever a regra errada (métrica que não existe, YAML com typo) não gera erro
  visível — o Prometheus simplesmente **não carrega** a regra e você segue achando que está
  protegido. Anatomia certa é o que separa "tem alerta" de "existe um alerta que dispara".
- **Mecanismo:** um alerta tem cinco peças, e cada uma responde uma pergunta diferente:

  | Peça | Pergunta que responde | No lab |
  |---|---|---|
  | `expr` (PromQL) | **quando** isso é verdade? | `up{job="app"} == 0` |
  | `for` | há quanto tempo ininterruptamente? | `1m` |
  | `labels` | como o Alertmanager **roteia** e agrupa? | `severity: critical` |
  | `annotations.summary` | em uma linha, **o que** aconteceu? | nome + instance |
  | `annotations.description` | **o que fazer / onde olhar** | comandos + runbook |

  Labels e annotations parecem texto, mas são dois papéis que não se trocam:

  | | `labels` | `annotations` |
  |---|---|---|
  | quem lê | Prometheus e Alertmanager (`group_by`, `matchers`, chave de dedup) | o **humano** que recebe a mensagem |
  | formato | valor identificador, sem espaço | texto livre com template Go (`{{ $labels.instance }}`) |
  | efeito de mudar | muda a **identidade** do alerta (vira outro alerta) | muda só o texto entregue |

  Consequência prática: `severity` precisa ser **label** (é o que o roteamento lê), enquanto
  `summary`/`description` são annotations (o roteamento não olha). Trocar os papéis é o erro
  clássico: escrever `severity: critical` em annotation e a rota `matchers: severity="critical"`
  nunca casa — o alerta existe, chega no canal errado, e ninguém entende por quê.

  O ciclo de vida da regra também é máquina: `inactive` (condição falsa) → `pending`
  (verdadeira, cronômetro correndo) → `firing` (entregue ao Alertmanager). E como a regra é
  YAML carregado na subida, um typo não derruba o Prometheus: ele recusa **aquela** regra e
  segue vivo — por isso a checagem é `health: ok` em `/api/v1/rules`, não "o container subiu".
- **Exemplo no lab:** o `alert_rules.yml` versionado no repo e montado no container do
  Prometheus — as três regras exigidas pela Issue, com `for:` em todas e annotations preenchidas:

  ```yaml
  # alert_rules.yml — montado em /etc/prometheus/alert_rules.yml (ro)
  groups:
    - name: notes-api
      rules:
        - alert: AlvoCaido
          expr: up{job="app"} == 0
          for: 1m
          labels:
            severity: critical
          annotations:
            summary: "Alvo {{ $labels.instance }} da notes-api parou de responder ao scrape"
            description: >-
              up == 0 por mais de 1m. Próximo passo: docker compose ps app
              e docker compose logs app --tail 50 (RUNBOOK §alvo-caido).

        - alert: Taxa5xxAlta
          expr: |
            sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
              / sum(rate(http_server_requests_seconds_count[5m]))
              > 0.05
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "Mais de 5% das respostas são 5xx ({{ $value | humanizePercentage }})"
            description: >-
              Janela de 5m em http_server_requests_seconds_count por status.
              Sem tráfego o denominador é 0 → NaN → não alerta (por desenho).
              Onde olhar: /api/v1/actuator/health e logs da app (RUNBOOK §5xx).

        - alert: DiscoQuaseCheio
          expr: |
            100 * (1 - node_filesystem_avail_bytes{fstype!~"tmpfs|overlay|squashfs"}
                       / node_filesystem_size_bytes{fstype!~"tmpfs|overlay|squashfs"}) >= 90
          for: 10m
          labels:
            severity: warning
          annotations:
            summary: "Mountpoint {{ $labels.mountpoint }} em {{ $value | humanize }}% da VM"
            description: >-
              Acima de 90% por 10m: logs da T3-01 e a série do Prometheus disputam
              o mesmo disco (RUNBOOK §disco).
  ```

  E o elo que liga essas regras ao Alertmanager — trecho do `prometheus.yml` da T3-02, com o
  que a T3-03 acrescenta:

  ```yaml
  global:
    scrape_interval: 15s
    evaluation_interval: 15s   # de onde sai o relógio que conta o "for:"

  rule_files:
    - /etc/prometheus/alert_rules.yml

  alerting:
    alertmanagers:
      - static_configs:
          - targets: ["alertmanager:9093"]
  ```

  Prova de que o carregamento é real (a mesma da validação da Issue):

  ```bash
  curl -s http://127.0.0.1:9090/api/v1/rules \
    | jq '.data.groups[].rules[] | {name: .name, health: .health, state: .state}'
  # → 3 regras, health: "ok", state: "inactive"
  # quebre de propósito (um typo no expr), restart → health: "failure" → corrija → ok
  ```

  Se a T3-02 escolheu **cAdvisor** em vez de `node_exporter`, a terceira regra troca de métrica
  (`container_fs_usage_bytes / container_fs_limit_bytes` de containers) e a troca se **declara**
  na Issue — regra apontando para métrica que ninguém coleta é regra morta: silenciosa, e ainda
  dá a sensação de cobertura.
- **Fronteira entre Issues:** as **métricas e labels** que a expressão pode usar vêm da
  **T3-02** (sem série fluindo não há o que avaliar — é a pré-condição verificável da Issue);
  **carregar regra versionada + entregar no canal + ciclo firing→resolved** é o desta
  **T3-03**; **o que fazer quando ela toca** (sintoma → diagnóstico → comandos) é a **T3-04** —
  por isso o `description` aponta para o runbook em vez de ensinar o procedimento aqui;
  alerta como annotation de painel do Grafana está **fora de escopo** declarado desta Issue
  (complemento, não substituto de canal).

## Por que `for:` existe: um scrape ruim não é tendência

- **Por que importa:** sem `for:`, a regra grita com **uma** amostra. E uma amostra ruim não é
  tendência: um `docker compose up -d` na stack, um restart de container, um timeout de rede
  interno bastam para um scrape falhar — o alerta dispara, a pessoa levanta do celular, abre a
  VM e está tudo verde. Três alarmes assim e a notificação vira badge muta: o próximo alerta
  **verdadeiro** entra na mesma fila e espera o mesmo desprezo. Falso positivo não é incômodo
  pontual, é treino para ignorar.
- **Mecanismo:** `for:` é um cronômetro que só corre enquanto a expressão continua verdadeira
  em **cada** avaliação (`evaluation_interval`). Ele transforma a pergunta de "é verdade agora?"
  para "vem sendo verdade tempo suficiente para ser tendência?":

  | instante | scrape de `up{job="app"}` | estado da regra `for: 1m` | entrega |
  |---|---|---|---|
  | 00:00 | `docker compose stop app` → scrape falha → `0` | `inactive` → **`pending`** | nada |
  | 00:15…00:59 | `0` em toda avaliação | `pending` (cronômetro correndo) | nada |
  | ~01:00 | `0` contínuo ≥ 1m | **`firing`** | Alertmanager → canal |
  | 00:40 (exemplo) | um scrape volta a dar `1` | `pending` → `inactive`, contador **zera** | nada — nunca chegou a existir alerta |

  Duas consequências que pagam a conta:

  - **atraso declarado:** tempo até incomodar ≈ `for:` + até 1 `evaluation_interval`. É o preço
    da confiança, e ele é **decidido por regra** — `1m` para alvo caído (queremos saber rápido),
    `10m` para disco (disco não enche em 15 segundos; é curva, não pico).
  - **contador zerável:** uma amostra falsa não é "meio caminho", é volta ao zero. Só a falha
    **sustentada** chega a `firing`.

  O erro oposto também existe: `for:` longo com janela de `rate` longa detecta devagar —
  `for: 30m` numa rajada de 5xx entrega o recado quando o incidente já acabou. `for:` não é
  "quanto maior melhor", é a declaração de **quanto atraso eu tolero** por regra.
- **Exemplo no lab:** o teste de disparo de propósito da Issue é exatamente a leitura da
  máquina de estados acima:

  ```bash
  docker compose stop app
  # /alerts (ou api/v1/alerts): "pending" durante ~1m  →  "firing" depois
  curl -s http://127.0.0.1:9090/api/v1/alerts \
    | jq '.data.alerts[] | {name: .labels.alertname, state: .state}'
  # mensagem chega no canal com o summary, não antes do "for:"
  docker compose start app   # health 200 → "resolved" entregue no mesmo canal
  ```

  E a régua da Issue é literal porque é mecanismo, não formalidade: `grep -c 'for:' alert_rules.yml`
  → `3`. Toda regra com janela declarada — se uma ficar sem `for:`, é ela que vai acordar o
  celular por um restart.
- **Fronteira entre Issues:** o `for:` (e o hábito de declarar duração em toda regra) é do
  **desta Issue**; calibrar **limiar e janela com histórico de série** (o `0.05` de 5xx é chute
  inicial) só fica possível depois de uma semana de dados da **T3-02** — a Issue exige
  **declarar o número**, a calibragem fina vem depois. Filtro de alerta por **manutenção
  programada** (silence) é do tópico seguinte; **inhibition** (suprimir 5xx enquanto o alvo
  caído já alertou) e escalação por plantão são estágio de operação real, fora do lab.

## `severity` é roteamento, não enfeite: o que merece incomodar o celular

- **Por que importa:** um alerta sem severidade — ou com severidade que ninguém definiu o
  significado — cai num canal único junto com tudo. Aí há só dois estados possíveis: o canal
  vibra para qualquer coisa (e é mutado em uma semana) ou nunca vibra (e o celular não serve
  para nada). `severity` só resolve isso quando **cada valor tem um contrato escrito**: o que
  fazer, quando, e quem é acordado. Sem contrato, `critical` é opinião.
- **Mecanismo:** `severity` é apenas mais um label na regra; quem lhe dá poder é o `matchers`
  de uma rota no Alertmanager. O caminho da decisão:

  ```text
  regra marca severity: critical
      → Alertmanager casa com a rota routes[0].matchers: severity="critical"
      → receiver lab-critical (tópico que vibra no celular)
      → sem match → receiver raiz (lab-aviso, informativo)
  ```

  A régua para escolher o valor não é gravidade abstrata, é **consequência prática**:
  "alguém faria algo diferente às 3h da manhã?". Se sim, `critical`; se a ação cabe no
  horário de trabalho, `warning`; se **ninguém mudaria de comportamento**, não é alerta —
  é painel na T3-02.

  | | `critical` | `warning` |
  |---|---|---|
  | regras do lab | `AlvoCaido`, `Taxa5xxAlta` | `DiscoQuaseCheio` |
  | ação esperada | agora, tira alguém do sério | agenda, monitora a curva |
  | canal | tópico que notifica com som | tópico informativo, sem urgência |
  | custo de errar | acordar à toa → mute geral | ficar para depois → encher o disco |
  | ciclo testado no lab | `stop app` → firing → resolved | mesma rota, severidade diferente |

  Repare que disco é `warning` **e** tem `for: 10m`: a severidade e a janela são as duas
  metades da mesma decisão ("não incomoda por nada, mas incomoda se persistir").
- **Exemplo no lab:** a rota simples que a Issue pede — severidade `critical` para o canal,
  o resto para o receiver raiz — dentro do `alertmanager.yml` versionado (detalhe do receiver
  no tópico seguinte):

  ```yaml
  route:
    receiver: lab-aviso                 # todo alerta cai aqui por padrão
    group_by: [alertname, severity]
    routes:
      - matchers: ['severity="critical"']
        receiver: lab-critical          # só o que acorda alguém
        # sem "continue:": critical NÃO desce para o receiver raiz
  ```

  Convenições de valor ficam escritas no repo (no próprio `alert_rules.yml` e no README da
  stack): a pessoa que acrescentar a quarta regra copia um `severity` existente em vez de
  inventar um — é assim que `critical` mantém significado depois do autor sair.
- **Fronteira entre Issues:** **roteamento por severidade (critical → canal)** é da **T3-03**;
  **escalação/humana de verdade** (rotação de plantão, PagerDuty, "quem está de sobreaviso")
  está declarada **fora de escopo** aqui; alerta de **negócio** ("vendas caíram") também —
  esta Issue é só infra/saúde; **SLO e erro budget** (o 5% aqui é limiar operacional, não
  orçamento de erro) são assunto da **Trilha 4**. O que fazer quando o `critical` chega é a
  **T3-04**.

## Alertmanager não é boletim: dedup, agrupamento, silence — e o alerta sem dono

- **Por que importa:** o Alertmanager é o elo entre "a regra ficou `firing`" e "um celular
  vibrou", e é onde alerta morre de duas formas. Entregando demais, vira spam: alerta que
  repete toda hora e não muda comportamento ensina a pessoa a desligar notificação — e quando
  o `critical` verdadeiro chega, ele entra no mesmo silêncio. Alerta **sem dono e sem ação
  escrita** é pior que não ter alerta: sem alerta você sabe que não sabe; spam **finge** que
  alguém está olhando. Por isso a entrega é configurada por **arquivo** (versionado, revisável,
  igual a tudo o resto do repo) e não clicada na UI.
- **Mecanismo:** a cadeia é `regra → Prometheus avalia → POST /api/v1/alerts → rota → receiver →
  canal`. Duas famílias de decisão moram no Alertmanager:

  **1. Agrupamento e dedup (quando reentregar):** o Alertmanager não manda cada alerta assim
  que chega — ele mantém grupos e só reentrega quando algo **muda**:

  | campo | valor no lab | efeito |
  |---|---|---|
  | `group_by: [alertname, severity]` | iguais viram **um** bloco | 3 alvos caídos = 1 mensagem |
  | `group_wait: 30s` | espera inicial | junta o que estourou junto |
  | `group_interval: 5m` | entre mudanças do grupo | um 2º alvo caído espera 5m, não spamma |
  | `repeat_interval: 12h` | enquanto nada mudar | lembrete longíssimo, não telemarketing |
  | `send_resolved: true` | entrega `resolved` | é o que fecha o ciclo firing→resolved |

  **2. Silence (parar de entregar sem mentir):** manutenção programada na `app` não deve virar
  alerta no celular, mas o estado `firing` **continua verdadeiro** — silence suprime a
  **entrega**, não o fato. E por ser temporário por definição, expira sozinho: o alerta volta
  a ser entregue sozinho depois, sem ninguém precisar "lembrar de desligar o alarme".
- **Exemplo no lab:** o receiver por arquivo, montado no container (o mesmo segredo do `.env`
  da T1-04 — fora do git, `600`, e só o **caminho** do arquivo aparece no YAML):

  ```yaml
  # alertmanager.yml — ./alertmanager.yml montado em /etc/alertmanager/alertmanager.yml (ro)
  global:
    resolve_timeout: 5m          # sem novo firing por 5m → entrega "resolved"

  route:
    receiver: lab-aviso
    group_by: [alertname, severity]
    group_wait: 30s
    group_interval: 5m
    repeat_interval: 12h
    routes:
      - matchers: ['severity="critical"']
        receiver: lab-critical

  receivers:
    - name: lab-critical
      webhook_configs:
        - url: https://ntfy.sh/devops-lab-critical   # canal escolhido: webhook ntfy
          send_resolved: true
          http_config:
            authorization:
              type: Bearer                           # default; declarado por clareza
              credentials_file: /etc/alertmanager/secrets/ntfy_token

    - name: lab-aviso
      webhook_configs:
        - url: https://ntfy.sh/devops-lab-aviso
          send_resolved: true
          http_config:
            authorization:
              credentials_file: /etc/alertmanager/secrets/ntfy_token
  ```

  Por que webhook (ntfy) e não SMTP: o canal precisa provar **ciclo** (`firing` e `resolved`
  observáveis) num laboratório sem conta de e-mail de verdade — o ntfy entrega no celular em
  segundos e dispensa infra de SMTP. A alternativa SMTP de lab (`smtp_smarthost` +
  `smtp_auth_username` + `smtp_auth_password_file`) usa exatamente o **mesmo mecanismo de
  segredo**: arquivo, não literal.

  O segredo nasce do `.env` (600, fora do git) e vira arquivo montado — o Alertmanager **não**
  expande `${VAR}` no YAML (é pedido aberto há anos), então "só referenciar o arquivo" não é
  preferência, é o caminho que existe:

  ```bash
  cd ~/lab
  set -a; . ./.env; set +a          # NTFY_TOKEN vive aqui, chmod 600, fora do git
  umask 077
  printf '%s' "$NTFY_TOKEN" > secrets/ntfy_token
  ls -l secrets/ntfy_token          # -rw-------
  grep -riE 'password|token' *.yml  # → só credentials_file/..._file, nenhum valor literal
  docker compose up -d alertmanager
  ```

  Silencing (documentado no runbook da T3-04, exercitado aqui):

  ```bash
  # suprime a entrega por 15m; /alerts continua "firing" — é só o celular que cala
  docker compose exec alertmanager amtool silence add alertname='AlvoCaido' \
    --expires=15m --comment="manutenção proposital da app" \
    --alertmanager.url=http://127.0.0.1:9093
  docker compose exec alertmanager amtool silence query --alertmanager.url=http://127.0.0.1:9093
  docker compose exec alertmanager amtool silence expire <id>   # cancela antes do prazo
  ```

  E a régua de "não é spam": toda regra do `alert_rules.yml` termina com `description`
  apontando **o que fazer** (comando + `RUNBOOK §...`). Se ninguém faria nada diferente ao
  ler, a regra vira painel — o alerta existe para mudar comportamento, não para gerar
  estatística de notificação.
- **Fronteira entre Issues:** **receiver/rota por arquivo, dedup e o canal entregando** são do
  **desta Issue** (é o que entrega `alertmanager-entregando`); **silence documentado no
  `RUNBOOK.md`** é da **T3-04** (aqui ele é exercitado, lá ele vira procedimento); **a resposta
  ao alerta** (diagnóstico, mitigação, escalação) é integralmente **T3-04** — alerta sem
  runbook vira "recebi e cliquei dismiss"; inhibition rules, plantão e canais pagos são
  estágio de operação real, **fora do escopo** da Issue.

## O alerta não avisa de si mesmo: a fronteira honesta do laboratório

- **Por que importa:** a confiança "não chegou nada, então está tudo bem" só vale enquanto
  **quem avalia está vivo**. Se o Prometheus cair, as regras param de rodar — inclusive a que
  diria "o Prometheus caiu". Nesse momento o sistema mais silencioso da stack é exatamente o
  responsável por quebrar o silêncio, e o lab fica com a pior sensação possível: falsa
  tranquilidade. Declarar essa fronteira é o que separa operar de acreditar.
- **Mecanismo:** todo alerta desta Issue é uma cadeia de dependentes — e qualquer elo morto
  produce o mesmo resultado: **silêncio**:

  | elo que caiu | quem percebe | resultado observável |
  |---|---|---|
  | `app` caída | Prometheus (`up == 0`) | ✅ alerta `firing` — o caso de sucesso |
  | scrape falhando 1 amostra | cronômetro do `for:` | ✅ nada (é para isso que `for:` existe) |
  | **Prometheus** caído | ninguém — as regras vivem **nele** | ❌ nada chega; `/alerts` nem responde |
  | **Alertmanager** caído | ninguém entrega | ❌ Prometheus tenta notificar, canal calado |
  | canal (ntfy/SMTP) fora | ninguém | ❌ `firing` registrado, mensagem perdida |
  | VM inteira fora | ninguém na VM | ❌ silêncio total (inclusive da status page) |

  A circularidade é de princípio, não de implementação: escrever
  `up{job="prometheus"} == 0` não ajuda, porque **a regra só é avaliada se o Prometheus estiver
  avaliando**. O mesmo vale para `absent(...)`: quem calcula a ausência é ele. "Monitorar o
  monitor" exige um observador **fora** da cadeia — o que vem depois.
- **Exemplo no lab:** o contraste que prova a fronteira com as próprias mãos (faça o teste do
  `stop app` primeiro, depois este):

  ```bash
  # 1) caso de sucesso: o ALVO cai, quem avalia está vivo
  docker compose stop app
  # ~1m depois: /alerts → firing → mensagem no ntfy → start app → resolved

  # 2) o MONITORADOR cai: ninguém avalia nada
  docker compose stop prometheus
  sleep 180                          # tempo bem maior que o "for:" das três regras
  # nada chega no canal — não é que o alerta demorou, não há quem o gere
  docker compose start prometheus    # volume preservado: a série histórica continua
  docker compose ps                  # stack de volta a 6 healthy (o teste não deixa resto)
  ```

  O que vem depois (declarado como limitação da Issue, **não** como entrega): um observador
  externo — **blackbox exporter** sondando a stack de fora, ou um **heartbeat** de cron noutra
  máquina/mundo (padrão healthchecks.io: "se ninguém mandou sinal em 5min, avise"), ou um
  Uptime Kuma. Todos têm a mesma forma: alguém **de fora** da cadeia pergunta "e se o
  vigilante dormir?" — daí o nome da classe: monitorar o monitor.
- **Fronteira entre Issues:** esta Issue entrega o alerta **dentro** da stack e **declara** a
  limitação em `Limitações / notas` — não entrega blackbox/heartbeat (estágio futuro, com VPS
  real); a **status page da T3-04** consulta o Prometheus **do mesmo host**, portanto sofre da
  mesma fronteira (ela prova "o stack local responde", não "a VM está no ar") — é honesta,
  mas não é o observador externo; resposta ao alerta e drill continuam sendo **T3-04**.
  Enquanto não existir o heartbeat externo, a frase certa não é "está tudo bem", é "não recebi
  alerta **e sei quem estaria em posição de mandar um**".

## Como iniciar o modo teach-anything

- "Me ensina a anatomia de um alerta abrindo o `alert_rules.yml` deste lab: `expr` PromQL,
  `for:`, label `severity` e as annotations `summary`/`description` — e por que severity não
  pode ir em annotation"
- "Me ensina por que `for:` existe percorrendo a máquina de estados `inactive → pending →
  firing` da regra `up{job=\"app\"} == 0`, com o atraso de detecção que ele impõe"
- "Me ensina severity como roteamento olhando o `route:` do `alertmanager.yml`: o matcher
  `severity=\"critical\"`, o receiver raiz e o teste das 3h da manhã para decidir o valor"
- "Me ensina dedup, agrupamento e silence no Alertmanager com `group_by`/`group_wait`/
  `repeat_interval` e um `amtool silence add --expires=15m` — e por que alerta sem dono é
  pior que alerta nenhum"
- "Me ensina a fronteira do alerta: o que acontece quando o próprio Prometheus cai, percorrendo
  a cadeia regra → Prometheus → Alertmanager → canal e o que seria blackbox/heartbeat externo"
