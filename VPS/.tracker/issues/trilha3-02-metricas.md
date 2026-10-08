---
aliases: [trilha3-02, metricas]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 02 — Métricas: Prometheus coleta, Grafana mostra, o Actuator responde

## Contexto

A preparação do lab deixou o backend expondo `/api/v1/actuator/prometheus` com 107 métricas
(JVM, HTTP por URI, health com readiness/liveness) — mas ninguém está **escutando**: sem
coletor, a métrica morre no endpoint e "como está a app?" continua sendo `docker logs` e
olho. Prometheus é o padrão que as vagas citam: ele varre (scrape) alvos declarados em
intervalo, guarda série temporal, e o Grafana desenha. É o elo que falta entre "o app
instrumenta" e "eu vejo" — e a razão de o Actuator ter ficado no código desde o começo
(conversa da preparação: instrumentar na borda + no app, testar, operar aqui).

## Objetivo

Estado final: stack `monitoring` (Prometheus + Grafana) no compose da VM, com Prometheus
fazendo scrape do `app:8080/actuator/prometheus` e do próprio Docker (cAdvisor ou
node exporter — declarar qual), `targets` `UP`, e um dashboard no Grafana com ao menos
`http_server_requests` (taxa de 5xx/latência) e memória/JVM rodando com dados reais
gerados por tráfego do teste.

## Dependências

- **Requer Trilha3-01** — logs e métricas são as duas camadas de observabilidade e a de
  logs já está no lugar (rotação/persistência resolvidas antes de adicionar serviço que
  também gera log/disco).
- **Requer Trilha2-04** — a stack que os `targets` apontam é a que o deploy mantém viva;
  o Prometheus entra como serviço **da mesma** composição (rede interna, porta só local).
- **pré-condição verificável:** `log-rotacao-declarada` na VM + deploy verde + 4×healthy.

- **estudo par:** `estudos/trilha3-02-metricas.md` — ler antes de executar (é o currículo desta issue)

## Escopo

- Serviço `prometheus` no `compose.yaml` da VM: scrape config declarada (arquivo de
  config versionado: `app` + o coletor de host escolhido), `volume` para a série temporal
- Serviço `grafana` no mesmo compose, provisioning de datasource automático (YAML, não
  clique manual) e **1 dashboard** versionado (JSON no repo)
- Porta do Grafana **só** em `127.0.0.1` na host **ou** alcançável por SSH tunnel —
  nunca aberta no ufw (a régua 22/80/443 continua sendo a única regra externa)
- Verificação com dado real: gerar tráfego (erros incluídos) e ver a série reagir
- `depends_on` na cadeia: monitoring espera `app` healthy (não inverte a ordem da T1-02)
- **assume pronto:** `log-rotacao-declarada` (T3-01), `rollback-automatico` (T2-04)
- **entrega:** `prometheus-scraping`, `grafana-dashboard`, `monitoring-fechado`

## Fora de escopo

- Alertas/envio (Alertmanager, e-mail/Telegram) — Issue 03
- Runbook de resposta e uptime formal — Issue 04
- APM (traces distribuídos, Jaeger), long-term storage (Thanos/Mimir) — estágio futuro
- Métricas de negócio do app (contagem de vendas) — app intocado; só o que o Actuator
  já emite + infra

## Conhecimentos envolvidos

- Modelo pull do Prometheus: alvo expõe, coletor varre — por que não é o app que "manda"
- Scrape interval, `up` metric, staleness — o que significa "target para"
- Séries temporais e labels (por `uri`, `status`) — cardinalidade na prática
- Provisioning do Grafana como código (datasources/dashboards YAML/JSON no git)
- Exposição de porta de ferramenta: loopback/tunnel vs. regra nova no ufw

## Estado atual

- 107 métricas existem no endpoint — e **zero** coletor lendo (série não acumula)
- Nenhum dashboard; "como está?" = inspeção manual
- VM: 4 serviços + rotação de log; nenhum sinal numérico de saúde histórica

## Resultado esperado

- `prometheus` UI → `/targets` com `app` (e coletor escolhido) `UP`, last scrape há
  segundos
- `curl prometheus:9090/api/v1/query?query=up` → `1` para o alvo da app
- Grafana → dashboard com painel de requisições/5xx e JVM/memória com **dados** (não vazio)
- Após `curl` com erro: série `http_server_requests_seconds_count{status="500"...}` (ou
  4xx) avança no Grafana/Prometheus
- `ufw status` na VM → **continua** só 22/80/443 (Grafana não virou regra nova)
- `docker compose ps` → 6 serviços (4 + 2 novos) healthy

## Requisitos

- Config do Prometheus **versionada** no repo (scrape configs em arquivo, não clicado na
  UI) — config como código, mesma regra do compose
- Alvos: `app:8080/api/v1/actuator/prometheus` (o endereço do laboratório, com
  context-path) + coletor de infra (cAdvisor **ou** node_exporter — escolher e justificar
  no estudo/limitação)
- Grafana com datasource provisionada por YAML (determinístico em máquina limpa)
- Ao menos 1 dashboard versionado (JSON no repo) com métrica HTTP da app + métrica de
  recurso
- Porta do Grafana em loopback no mapeamento (`127.0.0.1:3000:3000`) **ou** sem
  publicação + SSH tunnel — declarar qual; **nenhuma** regra nova no ufw
- Dados reais: o dashboard é validado com tráfego gerado (incluindo status de erro),
  não com o estado recém-ligado vazio
- Volume nomeado para a série do Prometheus (sobrevive a `compose restart`)

## Critérios de aceitação

- [ ] Pré-condição: `ssh lab@<ip-vm> 'grep -c max-size /etc/docker/daemon.json'` → `1`
      (T3-01) **e** deploy verde no último run **e** `docker compose ps` → 4×healthy
      antes de subir monitoring — sem os três, pare aqui
- [ ] `docker compose ps` → `prometheus` e `grafana` `healthy`/`Up` **além** dos 4
      antigos (6 total)
- [ ] `curl -s http://127.0.0.1:9090/api/v1/targets ...` (ou UI) → alvo `app` com
      `health: up` e `lastScrape` < 15s
- [ ] Query `up == 0` → **vazia** (nenhum alvo caído silencioso)
- [ ] Config no repo: `git ls-files` do `prometheus.yml` (ou equivalente) + provisioning
      do Grafana + dashboard JSON → todos rastreados
- [ ] Tráfego de teste: `curl` em rota que gere erro/latência → em ≤ intervalo de scrape,
      `http_server_requests_seconds_count` **avança** no Prometheus
      (`curl -G .../api/v1/query --data-urlencode 'query=http_server_requests_seconds_count'`)
- [ ] Grafana → dashboard abre com painéis **populados** (screenshot/valor), datasource
      `provisioned` (sem adicionar manualmente)
- [ ] `ufw status` na VM → inalterado: exatamente `22,80,443` (Grafana acessível só por
      loopback/tunnel — declarar qual foi feito)
- [ ] `docker compose restart` → volumes preservados: série do Prometheus continua (query
      com dados anteriores ao restart)

## Validação

- Subir a stack de monitoramento: `docker compose up -d` → `ps` → 6 healthy
- Targets: abrir `/targets` (via SSH tunnel ou curl interno) → `UP`; copiar linha de
  `lastScrape`
- Gerar sinal: sequência de `curl` (alguns `404`/`401` já são grátis no app) →
  `query=rate(http_server_requests_seconds_count[1m])` → série com valor
- Dashboard: abrir o JSON provisionado → painéis com dados; conferir que a query do
  painel bate com o que a query crua do Prometheus devolve
- Segurança da borda: `ufw status` da VM (22/80/443) + `ss -tlnp` mostrando 3000/9090
  em `127.0.0.1` (não `0.0.0.0`)
- Persistência: `docker compose restart prometheus` → query antiga ainda responde

## Evidências

- `/targets` com o alvo `app` UP e timestamp do último scrape
- Resposta de `api/v1/query` para `up` (com valor) e para a métrica HTTP após o tráfego
- Screenshot/valor do dashboard com painéis populados
- `git ls-files` com config/provisioning/dashboard rastreados
- `ufw status` + `ss -tlnp` da VM provando que o monitoring não abriu borda nova

## Limitações / notas

- `api/v1/actuator/prometheus` é **permitido sem auth** pela preparação (a issue da
  Trilha 1 do lab antigo o abriu): dentro da rede interna do compose está aceito; se o
  app voltar a ser publicado direto, isso vira brecha — é por isso que a borda (T1-03) é
  a única porta externa e o Prometheus não precisa de regra nova
- Prometheus guarda em RAM/disco local com retenção default (15d) — grow e apagão de
  disco são futuros; a rotação da T3-01 protege logs, a série do Prometheus tem
  `storage.tsdb.retention` próprio (declarar o valor na execução)
- cAdvisor ≈ visão de containers; node_exporter ≈ visão da VM (CPU/disco host) — as duas
  coisas respondem perguntas diferentes; escolher **um** aqui é escopo, o outro entra
  quando a pergunta existir
- Dashboard pronto de comunidade (ID do Grafana.com) é válido importar, mas **versionar
  o JSON no repo** — o clique manual não sobrevive a máquina limpa
