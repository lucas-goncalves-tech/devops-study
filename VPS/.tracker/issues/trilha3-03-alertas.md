---
aliases: [trilha3-03, alertas]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 03 — Alertas: alguém é avisado antes do usuário reclamar

## Contexto

Dashboards só funcionam para quem está olhando — e ninguém fica olhando. O valor de
observabilidade só se paga quando o sistema **chama**: regra no Prometheus dispara
Alertmanager, que entrega a mensagem para um canal que um humano realmente lê. É o salto
de "eu vejo quando abro o Grafana" para "eu fico sabendo no celular" — e é o item que
transforma a Trilha 3 de vitrine em operação. A regra de alerta também é pensamento
disciplinado: alerta bom tem condição objetiva, escala de severidade e **dono** — alerta
que avisa tudo e não muda comportamento é spam, e spam é o que faz gente desligar
notificação.

## Objetivo

Estado final: regras de alerta versionadas no repo (alvo caído, app `unhealthy`/5xx
acima do limiar, disco alto) carregadas pelo Prometheus; Alertmanager entregando num
canal real (e-mail SMTP de laboratório ou webhook — declarar qual); **um alerta disparado
de propósito** (derrubar o alvo `app`) chega ao canal e volta para `resolved` quando a
app sobe de novo.

## Dependências

- **Requer Trilha3-02** — alerta é regra sobre série: sem `prometheus-scraping` coletando,
  não há condição a avaliar.
- **pré-condição verificável:** targets `UP` e dashboard populado (dados fluindo) antes
  de qualquer regra.

## Escopo

- `alert_rules.yml` versionado: ao menos 3 regras — `up == 0` (alvo caído, `for: 1m`),
  erro 5xx em taxa (limiar declarado), disco ≥ 90% (`node_filesystem` **se** o coletor da
  T3-02 for node_exporter — senão trocar por métrica equivalente e declarar)
- `for:` com duração em todas as regras (evita alertar em oscilação de 1 scrape)
- Alertmanager no compose: receiver configurado por **arquivo** (não UI), rota simples
  (severidade `critical` → canal)
- Canal de entrega real e testável: SMTP de lab (conta de teste) **ou** webhook
  (ex.: webhook.site/ntfy/Telegram bot) — escolher, declarar e justificar no estudo
- Teste completo de ciclo: `firing` → entrega → `resolved`
- **assume pronto:** `prometheus-scraping`, `grafana-dashboard` — da Issue 02
- **entrega:** `regras-versionadas`, `alertmanager-entregando`, `ciclo-firing-resolved`

## Fora de escopo

- Escalação/pagamento (rotação de plantão, PagerDuty) — estágio de operação real
- Alertas de **negócio** (vendas caíram) — app intocado, só infra/saúde
- SLO/erro budget formal — Trilha 4 discute; aqui são limiares operacionais
- Alerta em dashboard do Grafana (annotations) — complemento, não substituto

## Conhecimentos envolvidos

- Anatomia de alerta: expressão PromQL + `for` + labels + annotations (`summary`/`description`)
- Por que `for:` existe (ambiguidade de um único scrape ruim)
- Severity como roteamento: `critical` vs `warning` — o que cada uma merece incomodar
- Alertmanager: dedup, group by, silences — o mínimo operacional
- Spam de alerta: por que alerta sem dono/ação é pior que não ter alerta

## Estado atual

- Prometheus coleta (T3-02) e Grafana mostra — para quem abrir
- Zero regras: alvo caído, 5xx em rajada e disco cheio passam despercebidos
- Nenhum canal de notificação configurado; nada chega a humano

## Resultado esperado

- `prometheus` → `/rules` com as regras carregadas e sem erro de syntax
- Disparo real: derrubar `app` → alerta `firing` em ≤ `for:` + slack/SMTP recebido com
  nome do alerta e target
- Voltar `app` → mesmo alerta `resolved` entregue
- Regras, config do Alertmanager e receiver no **repo** (git)
- Silencing explicado: `amtool silence` (ou UI) documentado no runbook da 04

## Requisitos

- Ao menos 3 regras com `for:` declarado, exprime PromQL e annotations preenchidas
  (nome de alerta legível + descrição com o que fazer/onde olhar)
- Regras e config do Alertmanager versionadas (mount em arquivo, não editadas na UI)
- Receiver com credencial em env/secret do `.env` (600, fora do git) — mesmo padrão da
  Trilha 1-04
- Teste de ciclo completo obrigatório: firing **e** resolved observados no canal
- Todas as regras avaliáveis com o que a T3-02 coleta (se uma regra precisa de métrica
  que não existe, trocar a métrica ou declarar a dependência — não deixar regra morta)
- Gate: alerta não depende de dashboard aberto (é servidor que chama, não tela)

## Critérios de aceitação

- [ ] Pré-condição: `curl -s <prom>/api/v1/targets` → `app` `health: up` **e**
      `api/v1/query?query=up` com dados (T3-02) — sem série fluindo, pare aqui
- [ ] `prometheus → /rules` (ou `api/v1/rules`) → as 3+ regras `health: ok`, sem erro de
      carregamento
- [ ] `grep -c 'for:' alert_rules.yml` → ≥ 3 (toda regra com janela declarada)
- [ ] Disparo de propósito: `docker compose stop app` → em ≤ `for:` + intervalo,
      `/alerts` mostra `firing` **e** a mensagem chega ao canal (print/saída do
      recebimento com o nome do alerta)
- [ ] Resolução: `docker compose start app` + health `200` → `resolved` entregue no
      mesmo canal (ciclo completo com os dois estados)
- [ ] Regras + config do Alertmanager rastreadas no repo (`git ls-files`) e montadas no
      compose (arquivos, não UI)
- [ ] Credencial do receiver ausente do git: `grep -riE 'password|token' *.yml` no repo
      → só `${...}`/sem valor literal; `.env` com a credencial e permissão 600 na VM
- [ ] Recovery da stack: após o teste, `docker compose ps` → 6 healthy (o teste de alerta
      não deixou serviço caído)

## Validação

- Carregar: `docker compose up -d` (alertmanager) → `curl <prom>/api/v1/rules` → `ok`
- Teste de syntax intencional: quebrar a regra (typo), `docker compose restart prometheus`
  → `/rules` com erro → corrigir → `ok` (prova que o carregamento é real)
- Ciclo feliz do alerta: `docker compose stop app` → observar:
  - `/alerts`: `pending` (dentro de `for:`) → `firing`
  - canal: mensagem recebida com `summary`
  - `docker compose start app` → wait health → `resolved` no canal
- `amtool`/UI: `silence` de 5 min no alerta → mesmo estado firing **não** reentrega
  (silence documentado para a 04)
- Restaurar tudo: `ps` → 6 healthy

## Evidências

- `/api/v1/rules` com as regras e `health: ok`
- Par de mensagens no canal: `firing` (com nome/data/target) e `resolved`
- Linhas do `/alerts` nos dois estados (ou screenshots)
- `git ls-files` com `alert_rules.yml` + config do Alertmanager
- `ls -l .env` na VM → `600` (credencial do receiver fora do git)

## Limitações / notas

- Canal de laboratório (webhook.site/SMTP de teste) **não** sobrevive a operação real —
  o valor desta issue é o **ciclo** (regra→entrega→resolução); trocar de canal depois é
  mudar receiver, não refazer observabilidade
- `up == 0` com `for: 1m` alerta quando o scrape falha — se o próprio Prometheus cair,
  **não** dispara nada (alerta não avisa sobre o ausente). Monitorar o monitor é
  blackbox/heartbeat externo (estágio futuro) — limitação clássica e honesta
- Limiar de 5xx/disco é chute inicial calibrado com dados da T3-02 — sem histórico, o
  primeiro valor é palpite; a issue exige **declarar** o número, a calibragem fina vem
  depois de uma semana de série (é para isso que o volume da T3-01/02 existe)
- Resposta ao alerta (o que **fazer**) não é desta issue — é o runbook da Issue 04; alerta
  sem runbook vira "recebi e cliquei dismiss"
