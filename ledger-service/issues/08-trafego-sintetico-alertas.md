---
aliases: [issue-08, trafego-sintetico-alertas]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 08 — Tráfego sintético com k6 agendado e alerta real disparando na stack de produção

## Contexto

A stack de produção responde, mas ninguém sabe se ela responde em pé: nenhum dado prova a latência que um cliente real sente, e nenhum alerta existe para avisar que a latência subiu. Um dashboard bonito alimentado por scrape mostra o que já está acontecendo — quem descobre o problema primeiro é o usuário. O app já expõe o que um teste sintético precisa (`/actuator/health` e `/actuator/prometheus` em `application.yml`) e é a Issue 07 quem publica essa stack atrás do Caddy, mas não existe rotina que gere tráfego, não existe latência p95 registrada e nenhum alerta disparou até hoje.

## Objetivo

Estado final: um script k6 agendado que autentica e transaciona contra o endpoint de produção com limite de vazão declarado, uma execução por janela de tempo cujo resumo registra p95, e pelo menos um alerta real disparado com evidência do canal que o recebeu — sem que a produção tenha falhado durante o teste.

## Dependências

- Requer Issue 07 — o tráfego só existe depois que há versão implantada e auditável na VPS
- Requer Issue 04 — o Caddy é a porta única de entrada; o k6 fala com o domínio público, nunca com a porta `8080` do container
- Requer a coleta e os painéis que nascem no `commerce-api` — [Issue 05 do `commerce-api`](../../commerce-api/issues/05-observability.md)

## Escopo

- Script k6 versionado: login em `/api/v1/auth/login`, transação em `/api/v1/payments/transfer` com `X-Idempotency-Key` e sondagem de `/actuator/health`
- Agendamento fora do pedido (cron na VPS ou `schedule` de GitHub Actions) com horário e vazão declarada
- Thresholds de p95 e de taxa de erro que reprovam a execução, não só imprimam
- Saída persistida da execução, com p95 legível no resumo
- Um alerta disparando de verdade, com evidência do canal que o recebeu
- Verificação de ausência de dano: saúde da aplicação e ausência de erro 5xx durante a janela

## Fora de escopo

- Coleta, dashboards e Alertmanager em si — [Issue 05 do `commerce-api`](../../commerce-api/issues/05-observability.md); esta Issue consome o que existe e prova que ele avisa
- **Tráfego sintético agendado não é teste de capacidade.** Aqui a vazão é baixa de propósito, o tráfego escreve em produção e o objetivo é provar que a stack responde e que o alerta avisa. Descobrir até onde a API aguenta é a [Issue 10 do `commerce-api`](../../commerce-api/issues/10-carga-em-rampa.md), que roda em laboratório e não toca a produção
- Chaos engineering, teste de penetração e carga destrutiva em massa
- Endpoint de métricas novo: `/actuator/prometheus` já é exposto pela configuração atual
- Kubernetes e orquestração — fora de escopo desta trilha

## Conhecimentos envolvidos

- k6: VUs, ritmo, thresholds e resumo de execução
- Coleta de métricas HTTP: histograma de latência e cálculo de p95
- Prometheus e Alertmanager: regra, `for`, severidade e roteamento por canal
- Agendamento: crontab em servidor e `schedule` de GitHub Actions
- Contrato da API: autenticação JWT, idempotência por `X-Idempotency-Key` e degradação de saúde por `/actuator/health`

## Estado atual

- A aplicação expõe `/actuator/health` e `/actuator/prometheus` (exposição em `management.endpoints.web.exposure`), mas a stack da VPS não tem rotina que consuma isso periodicamente
- Nenhum script de tráfego sintético existe no repositório
- Nenhum alerta foi configurado, portanto nenhum alerta pode ter disparado
- A latência p95 da stack de produção nunca foi medida nem registrada

## Resultado esperado

- Uma execução agendada por janela de tempo, com resumo persistido e p95 visível
- Alerta que dispara diante de uma degradação induzida e chega a um canal observável
- Produção íntegra durante e depois da janela de teste

## Requisitos

- [ ] Escrever o script k6 versionado, com login, transação idempotente e sondagem de `/actuator/health`, e limite de vazão declarado em comentário no código
- [ ] Agendar a execução fora do pedido (cron na VPS ou `schedule` de GitHub Actions), com horário e destino da saída declarados
- [ ] Definir thresholds de p95 e de taxa de erro que reprovam a execução quando a degradação passar do limite
- [ ] Persistir a saída da execução em arquivo ou artefato, com o p95 legível no resumo do k6
- [ ] Configurar ao menos um alerta sobre métrica desta stack (por exemplo p95 de requisição ou taxa de erro) com condição e tempo de espera
- [ ] Ligar o alerta a um canal observável e capturar a evidência do disparo (log do canal ou captura de tela)
- [ ] Registrar, na mesma janela do teste, a saúde da aplicação e a ausência de erro 5xx no log do serviço
- [ ] Declarar o impacto da carga sintética no banco de produção (o que ela cria) e registrar a limpeza do que foi criado

## Critérios de aceitação

- [ ] A execução agendada acontece sem intervenção manual e o resumo da execução fica persistido com o p95 exibido
- [ ] Uma degradação induzida faz o alerta disparar de verdade e o canal configurado mostra o disparo, com a evidência arquivada nesta Issue
- [ ] A regra de alerta tem expressão de PromQL, condição e `for` visíveis na configuração, não apenas descrita em texto
- [ ] Durante toda a janela do teste, `/actuator/health` responde `UP` e o `healthcheck.sh` da raiz do repo sai com 0
- [ ] Nenhum erro 5xx de aplicação aparece no log do serviço durante a janela do teste
- [ ] O script k6 roda contra o domínio público atrás do Caddy, e não contra a porta do container
- [ ] O que a carga sintética criou no banco de produção está identificado e limpo ao final da execução

## Validação

- Executar o agendamento manualmente uma vez e conferir o horário e a saída persistida
- Induzir a degradação que o alerta observa (limiar artificialmente baixo ou latência adicionada no caminho) e esperar o disparo no canal
- Reverter a degradação e confirmar que o alerta volta ao estado normal
- Consultar `/actuator/health` e rodar `healthcheck.sh` durante a janela do teste
- Inspecionar o log do serviço procurando `5xx` no mesmo intervalo
- Conferir no banco os registros criados pela carga e removê-los

## Evidências

- Saída do k6 com o p95 da janela e o nome do thresholds avaliado
- Evidência do disparo no canal (linha de log do Alertmanager ou captura de tela com horário)
- Regra de alerta em arquivo, com PromQL e `for`
- Saída de `/actuator/health` e do `healthcheck.sh` durante a janela
- Trecho do log do serviço sem erro 5xx no mesmo intervalo
- Consulta ao banco com o que a carga criou e o registro da limpeza

## Limitações / notas

- **A carga escreve em produção.** `/api/v1/payments/transfer` debita carteira de verdade: a vazão tem de ser baixa, o script usa usuário sintético e o que ele cria é removido no fim. Carga de desempenho de verdade é a [Issue 10 do `commerce-api`](../../commerce-api/issues/10-carga-em-rampa.md), que roda contra o ambiente de laboratório e encontra o ponto de inflexão por rampa — a [Issue 05 do `commerce-api`](../../commerce-api/issues/05-observability.md) é o smoke test de patamar que ela consome, e não um teste de capacidade
- "Alerta que dispara de verdade" não significa alerta sempre vermelho: a prova é um disparo registrado com horário e canal, depois o alerta em estado normal
- Se a degradação induzida exigir mexer no limite da regra e não no tráfego, registre isso no relatório — induzir pelo lado do alerta e pelo lado da aplicação são provas diferentes, e a segunda é mais forte
- O alerta depende de alguém coletar `/actuator/prometheus` nessa stack; se a coleta ainda não existir quando esta Issue começar, registre a dependência e não declare o critério de disparo como cumprido sem o canal
- Onde o alerta notifica (e-mail, chat, webhook) é escolha de operação: o critério exige o canal observável e a evidência, não um produto específico
