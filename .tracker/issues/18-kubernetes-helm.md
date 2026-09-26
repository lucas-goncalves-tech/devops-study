---
aliases: [issue-18, kubernetes-helm]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: media
---

# Issue 18 — Plataforma em cluster multi-node com chart versionado, probes e limites

## Contexto

A plataforma roda em Compose: não há simulação de falha de nó, nem definição declarada de saúde por serviço, nem estratégia de rollout. O que funciona num host só não prova comportamento em orquestrador.

## Objetivo

Estado final: cluster local multi-node, plataforma empacotada como chart com valores por ambiente, probes e recursos declarados por serviço, e rollout/rollback sem downtime visível — com falha de nó não derrubando a plataforma.

## Dependências

- Requer Issue 07 — o chart empacota a plataforma multi-serviço, não o monolito
- Requer Issue 06 — as métricas e o comportamento sob carga definem os limiares de probe

## Escopo

- Cluster local multi-node capaz de simular falha de nó
- Chart único com valores por ambiente
- Probes de startup, liveness e readiness por serviço
- Requests e limits de CPU e memória
- Estratégia de rollout e rollback

## Fora de escopo

- Nuvem gerenciada e estado remoto — Issue 12
- Pipeline de CI/CD e gates — Issues 10 e 16
- Gerenciamento de segredos dentro do cluster
- Service mesh, autoscaling e ingress avançado

## Conhecimentos envolvidos

- Pod, Deployment, Service e probes do Kubernetes
- Requests, limits e OOM em contêineres
- Helm: charts e values por ambiente
- Cluster local multi-node
- Estratégias de rollout e rollback

## Estado atual

- Tudo fora de orquestrador
- Pods sem probe nem limite declarado
- Deploy sem estratégia; falha de nó não é exercitada

## Resultado esperado

- Chart instala em qualquer namespace de teste
- Probe mal configurada detectada em teste
- OOM contido pelo limite declarado
- Falha de um nó não derruba a plataforma
- Rollback volta sozinho sem downtime visível

## Requisitos

- [ ] Subir cluster local multi-node capaz de simular falha de nó
- [ ] Empacotar a plataforma como chart único com valores por ambiente
- [ ] Declarar probes de startup, liveness e readiness por serviço
- [ ] Declarar requests e limits de CPU e memória por serviço
- [ ] Validar rollout, rollback e restart sem downtime visível
- [ ] Simular falha de nó e confirmar recuperação da plataforma

## Critérios de aceitação

- [ ] O chart instala limpo em namespace de teste e remove sem resíduo
- [ ] Uma probe configurada incorretamente é detectada e reinicia o serviço correspondente
- [ ] Consumo acima do limite declarado derruba apenas o serviço atingido
- [ ] Falha de um nó não interrompe o serviço para o cliente
- [ ] Rollback retorna à versão anterior sem downtime observável na requisição

## Validação

- Instalação e remoção do chart em namespace limpo
- **Build to break:** instalar com probe apontando para caminho inexistente e observar o reinício do serviço
- **Build to break:** consumir memória além do limite e observar o reinício isolado
- **Build to break:** derrubar um nó do cluster e observar a plataforma continuar respondendo
- Executar rollout para versão nova, provocar falha e observar o rollback automático
- Requisição contínua durante o rollout confirmando ausência de downtime

## Evidências

- Log de instalação e remoção do chart
- Eventos do cluster mostrando o reinício causado pela probe
- Comportamento isolado durante o estouro de memória
- Eventos de recuperação após falha de nó
- Log do rollback e a medição de continuidade da requisição

## Limitações / notas

- Requer recursos locais suficientes para cluster multi-node — pré-requisito de hardware
- Os limiares de probe precisam partir do comportamento observado na Issue 06; probe sem lastro em métrica vira chute e reinicia serviço saudável
- O contrato de saúde é o mesmo do Compose: `/actuator/health` com HTTP 200 e `"status":"UP"` — probes de HTTP devem seguir esse contrato
- `server.shutdown: graceful` precisa ser preservado: o probe de prestop depende da JVM encerrar sem cortar requisição
- Esta é a última Issue da sequência — nenhuma outra depende dela
