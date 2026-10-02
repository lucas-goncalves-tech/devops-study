---
aliases: [issue-08, trafego-sintetico-alertas]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 08 — Tráfego sintético com k6 agendado e alerta real disparando na stack de produção

## Contexto

A stack de produção responde, mas ninguém sabe se ela responde em pé: nenhum dado prova a latência que um cliente real sente, e nenhum alerta existe para avisar que a latência subiu. Um dashboard bonito alimentado por scrape mostra o que já está acontecendo — quem descobre o problema primeiro é o usuário. O app já expõe o que um teste sintético precisa (`/actuator/health` e `/actuator/prometheus` em `application.yml`) e é a Issue 07 quem publica essa stack atrás do Caddy, mas não existe rotina que gere tráfego, não existe coleta que consuma a métrica exposta, não existe latência p95 registrada e nenhum alerta disparou até hoje.

## Objetivo

Estado final: um script k6 agendado que autentica e transaciona contra o endpoint de produção com limite de vazão declarado, uma execução por janela de tempo cujo resumo registra p95, e um alerta real — avaliado sobre métrica coletada desta própria stack — disparando com evidência do canal que o recebeu, sem que a produção tenha falhado durante o teste.

## Dependências

- Requer Issue 07 — o tráfego só existe depois que há versão implantada e auditável na VPS
- Requer Issue 04 — o Caddy é a porta única de entrada; o k6 fala com o domínio público, nunca com a porta `8080` do container
- Requer Issue 06 — o coletor entra nas redes já declaradas da stack segmentada; nenhuma porta nova fora do firewall da Issue 03

## Escopo

- Coleta da métrica que o alerta observa, nesta stack: `scrape` de `/actuator/prometheus` pelo coletor desta VPS
- Script k6 versionado: login em `/api/v1/auth/login`, transação em `/api/v1/payments/transfer` com `X-Idempotency-Key` e sondagem de `/actuator/health`
- Agendamento fora do pedido (cron na VPS ou `schedule` de GitHub Actions) com horário e vazão declarada
- Thresholds de p95 e de taxa de erro que reprovam a execução, não só imprimam
- Saída persistida da execução, com p95 legível no resumo
- Regra de alerta sobre a métrica desta stack, com condição e tempo de espera, ligada a um canal observável
- Verificação de ausência de dano: saúde da aplicação e ausência de erro 5xx durante a janela

## Fora de escopo

- Painel de serviço completo e carga de laboratório: a medição desta Issue é a do k6 contra produção, e a carga pesada em ambiente descartável é assunto de outro app e outra trilha
- Chaos engineering, teste de penetração e carga destrutiva em massa
- Endpoint de métricas novo: `/actuator/prometheus` já é exposto pela configuração atual
- Kubernetes e orquestração — fora de escopo desta trilha

## Conhecimentos envolvidos

- k6: VUs, ritmo, thresholds e resumo de execução
- Coleta de métricas HTTP: scrape, histograma de latência e cálculo de p95
- Prometheus e Alertmanager: regra, `for`, severidade e roteamento por canal
- Agendamento: crontab em servidor e `schedule` de GitHub Actions
- Contrato da API: autenticação JWT, idempotência por `X-Idempotency-Key` e degradação de saúde por `/actuator/health`

## Estado atual

- A aplicação expõe `/actuator/health` e `/actuator/prometheus` (exposição em `management.endpoints.web.exposure`), mas nada nesta stack consome essas métricas: não há coletor, não há série e não há o que a regra de alerta observar
- Nenhum script de tráfego sintético existe no repositório
- Nenhum alerta foi configurado, portanto nenhum alerta pode ter disparado
- A latência p95 da stack de produção nunca foi medida nem registrada

## Resultado esperado

- A métrica da stack de produção é coletada por um coletor desta própria VPS
- Uma execução agendada por janela de tempo, com resumo persistido e p95 visível
- Alerta que dispara diante de uma degradação induzida e chega a um canal observável
- Produção íntegra durante e depois da janela de teste

## Requisitos

- [ ] Subir o coletor de métricas desta stack apontando para `/actuator/prometheus` da aplicação em produção, com a série visível e, se o coletor expuser `painel/API do coletor`, o acesso restrito por `autenticação`
- [ ] Escrever o script k6 versionado, com login, transação idempotente e sondagem de `/actuator/health`, e limite de vazão declarado em comentário no código
- [ ] Agendar a execução fora do pedido (cron na VPS ou `schedule` de GitHub Actions), com horário e destino da saída declarados
- [ ] Definir thresholds de p95 e de taxa de erro que reprovam a execução quando a degradação passar do limite
- [ ] Persistir a saída da execução em arquivo ou artefato, com o p95 legível no resumo do k6
- [ ] Configurar ao menos um alerta sobre métrica desta stack (por exemplo p95 de requisição ou taxa de erro) com condição e tempo de espera
- [ ] Ligar o alerta a um canal observável e capturar a evidência do disparo (log do canal ou captura de tela)
- [ ] Cada regra de alerta declara o procedimento que o operador segue ao receber o disparo — o caminho do runbook e o primeiro comando a rodar — e a notificação entrega esse caminho junto da mensagem
- [ ] Registrar, na mesma janela do teste, a saúde da aplicação e a ausência de erro 5xx no log do serviço
- [ ] Declarar o impacto da carga sintética no banco de produção (o que ela cria) e registrar a limpeza do que foi criado
- [ ] Declarar a exposição do coletor: ele entra nas redes da Issue 06, nenhuma porta nova fora do firewall da Issue 03, e o acesso externo ao painel é somente pelo Caddy da Issue 04 com autenticação

## Critérios de aceitação

- [ ] A execução agendada acontece sem intervenção manual e o resumo da execução fica persistido com o p95 exibido
- [ ] A regra de alerta é avaliada sobre métrica coletada desta stack: o scrape do `/actuator/prometheus` está ativo e a série é consultável
- [ ] Uma degradação induzida faz o alerta disparar de verdade e o canal configurado mostra o disparo, com a evidência arquivada nesta Issue
- [ ] A regra de alerta tem expressão de PromQL, condição e `for` visíveis na configuração, não apenas descrita em texto
- [ ] Cada regra de alerta aponta, por anotação na configuração, para um arquivo de runbook que existe no repositório, e a regra não é considerada cumprida enquanto esse arquivo não existir
- [ ] Nenhum requisito desta Issue depende de Issue de outro app: coleta, regra e canal nascem nesta trilha
- [ ] Durante toda a janela do teste, `/actuator/health` responde `UP` e o healthcheck L4/L7 da `Issue 01` sai com 0
- [ ] Nenhum erro 5xx de aplicação aparece no log do serviço durante a janela do teste
- [ ] O script k6 roda contra o domínio público atrás do Caddy, e não contra a porta do container
- [ ] O que a carga sintética criou no banco de produção está identificado e limpo ao final da execução
- [ ] O coletor está nas redes declaradas da Issue 06, o firewall da Issue 03 não ganha regra nova e qualquer acesso externo ao painel, se existir, passa pelo proxy com autenticação

## Validação

- Conferir o scrape ativo do `/actuator/prometheus` no coletor desta stack e a série resultante
- Executar o agendamento manualmente uma vez e conferir o horário e a saída persistida
- Induzir a degradação que o alerta observa (limiar artificialmente baixo ou latência adicionada no caminho) e esperar o disparo no canal
- Reverter a degradação e confirmar que o alerta volta ao estado normal
- Abrir o alerta recebido sem saber por que disparou e conferir que o caminho do runbook chega na própria notificação
- Consultar `/actuator/health` e rodar o healthcheck L4/L7 da `Issue 01` durante a janela do teste
- Inspecionar o log do serviço procurando `5xx` no mesmo intervalo
- Conferir no banco os registros criados pela carga e removê-los
- Conferir que a porta do coletor não responde fora do proxy e que o firewall da Issue 03 não mudou

## Evidências

- Saída do coletor mostrando a série coletada desta stack
- Tentativa de acesso direto à porta do coletor recusada fora do proxy e firewall da Issue 03 sem mudança
- Saída do k6 com o p95 da janela e o nome do thresholds avaliado
- Evidência do disparo no canal (linha de log do canal de notificação ou captura de tela com horário)
- Regra de alerta em arquivo, com PromQL e `for`
- Trecho da configuração da regra mostrando a anotação do runbook e o arquivo referenciado
- Saída de `/actuator/health` e do healthcheck L4/L7 da `Issue 01` durante a janela
- Trecho do log do serviço sem erro 5xx no mesmo intervalo
- Consulta ao banco com o que a carga criou e o registro da limpeza

## Limitações / notas

- **A carga escreve em produção.** `/api/v1/payments/transfer` debita carteira de verdade: a vazão tem de ser baixa, o script usa usuário sintético e o que ele cria é removido no fim. Carga pesada em ambiente descartável não é escopo desta Issue — aqui o que se prova é o comportamento sob tráfego real, não capacidade de aguentar carga
- **A coleta é escopo desta Issue, não pré-requisito vindo de fora:** sem scrape ativo nesta stack, não existe série e o critério de disparo não pode ser declarado cumprido. Se a coleta falhar no meio do trabalho, registre a limitação em vez de afrouxar o critério
- "Alerta que dispara de verdade" não significa alerta sempre vermelho: a prova é um disparo registrado com horário e canal, depois o alerta em estado normal
- Se a degradação induzida exigir mexer no limite da regra e não no tráfego, registre isso no relatório — induzir pelo lado do alerta e pelo lado da aplicação são provas diferentes, e a segunda é mais forte
- Onde o alerta notifica (e-mail, chat, webhook) é escolha de operação: o critério exige o canal observável e a evidência, não um produto específico
