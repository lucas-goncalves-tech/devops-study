---
aliases: [issue-09, incidente-runbook-postmortem]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 09 — Incidente tem procedimento executável, escalonamento declarado e análise sem culpa

## Contexto

A Issue 08 entrega um alerta que dispara para um canal e a Issue 05 entrega um procedimento escrito para restaurar backup. O que não existe entre os dois: o que a pessoa faz nos primeiros minutos depois de receber a notificação, para quem ela fala quando ninguém responde, e o que fica escrito depois que o serviço volta. Alerta sem procedimento é ruído que treina a ignorar; procedimento sem análise repete a mesma causa no mês seguinte. Incidente de sistema não se resolve na memória de quem estava de plantão às três da manhã — e hoje a stack da trilha VPS é exatamente isso: uma VPS, um banco, um proxy e uma pessoa.

## Objetivo

Estado final: um incidente real ou induzido nesta stack, detectado pelo alerta da Issue 08, tratado por outra pessoa seguindo apenas o que está escrito, com escalonamento declarado e exercitado, e encerrado em um post-incident review sem culpa versionado com causa raiz mecânica e ações com dono e prazo — sem que o tratamento deixe o serviço degradado nem dado de transação duplicado ou perdido.

## Dependências

- Requer Issue 08 — o incidente é detectado pelo alerta desta trilha, e cada regra de alerta precisa declarar o runbook que o operador segue
- Requer Issue 05 — o runbook de restauração de backup já é o precedente do formato (uma página, executável) que esta Issue estende para indisponibilidade do serviço
- Requer Issue 07 — a stack implantada com versão auditável é o que se estraga; sem ela não há incidente de produção para tratar

## Escopo

- Runbook de uma página por regra de alerta da Issue 08, referenciado pela regra e entregue na própria notificação
- Conteúdo do runbook: como confirmar o sintoma, como chegar ao log, o que restaura o serviço e como confirmar que voltou
- Escalonamento declarado em arquivo: quem é notificado, em que ordem, por qual canal, e em quanto tempo sem resposta o canal secundário é acionado
- Exercício cego: pessoa sem contexto da causa executa o runbook e devolve o serviço ao estado saudável
- Post-incident review sem culpa versionado, com linha do tempo em UTC, impacto observado, causa raiz mecânica e ações com dono e prazo
- Verificação de retorno ao saudável depois do tratamento, incluindo o tráfego sintético da Issue 08
- Registro do incidente e do review em arquivo versionado no repositório

## Fora de escopo

- Ferramenta paga de paging ou de incidente (PagerDuty, Opsgenie, Statuspage): custo zero por regra — o escalonamento usa o canal da Issue 08 mais um canal secundário gratuito
- Equipe e rodízio real de plantão: a stack tem um dono; o que se prova é o procedimento e a chegada no canal secundário, não escala humana
- Comunicação a clientes, status page e SLA externo com contrato
- Chaos engineering como método: a degradação induzida já é da Issue 08; aqui o incidente é o que se documenta e se analisa
- Runbook de restauração de backup — esse procedimento é da Issue 05 e não é reescrito aqui
- Relatório da execução de tráfego sintético — é da Issue 08

## Conhecimentos envolvidos

- Post-incident review sem culpa: linha do tempo em UTC, impacto observado e causa raiz que termina em mecanismo, não em pessoa
- Runbook operacional: uma página, um comando por linha, sem depender de contexto que só quem sofre tem
- Escalonamento: canal primário e secundário, tempo de espera declarado e o critério de quando escalar
- Operação de serviço systemd na Debian: `systemctl status`, `journalctl -u`, unidade do serviço e log do container
- Sinais de saúde da própria stack: `/actuator/health`, healthcheck L4/L7 da Issue 01 e `docker compose ps`

## Estado atual

- O alerta da Issue 08 aponta para um canal, mas nenhuma regra declara o procedimento a seguir: quem recebe "p95 alto" não tem o próximo passo escrito
- O único procedimento existente é o de restauração de backup da Issue 05, que não cobre indisponibilidade do serviço nem degradação de latência
- Não existe canal secundário: se o canal primário estiver indisponível, o alerta chega a ninguém e o silêncio é indistinguível de "tudo bem"
- Nenhum incidente desta trilha foi registrado por escrito; o histórico de falha é o da memória de quem estava no console
- Não há escalonamento declarado: não existe a quem falar quando o primeiro notificado não responde

## Resultado esperado

- Um alerta real da Issue 08 tratado por pessoa que não sabia a causa, seguindo somente o runbook
- Escalonamento exercitado uma vez: a notificação chega ao canal secundário com o primário indisponível, com horário registrado
- Um post-incident review versionado, sem atribuição de culpa, com causa raiz mecânica e ações com dono e data
- Serviço de volta ao estado saudável depois do tratamento, com tráfego sintético dentro dos thresholds e sem transação duplicada ou perdida

## Requisitos

- [ ] Runbook de uma página por regra de alerta da Issue 08, referenciado pela regra, começando pelo primeiro comando a executar
- [ ] Conteúdo do runbook cobrindo: confirmar o sintoma, chegar ao log relevante, restaurar o serviço e confirmar que voltou
- [ ] Escalonamento declarado em arquivo: quem é notificado, em qual ordem, por qual canal, e em quanto tempo sem resposta o canal secundário é acionado
- [ ] Canal secundário declarado e exercitado pelo menos uma vez com o canal primário indisponível
- [ ] Exercício cego executado por pessoa sem contexto da causa, com o runbook como única fonte de informação
- [ ] Post-incident review sem culpa versionado, com linha do tempo em UTC, impacto observado e causa raiz que não termina em erro humano
- [ ] Cada ação do review com dono, prazo e o Issue que vai conferir a ação
- [ ] Verificação de retorno ao saudável depois do tratamento: `/actuator/health`, healthcheck L4/L7 da Issue 01 e execução do tráfego da Issue 08 sem reprovar threshold

## Critérios de aceitação

- [ ] O exercício cego devolveu o serviço ao estado saudável sem nenhum passo improvisado fora do runbook — o que improvisou, se houve, está anotado e entrou no post-incident review
- [ ] Cada regra de alerta da Issue 08 referencia, por anotação na configuração, um arquivo de runbook que existe no repositório
- [ ] A notificação de escalonamento chegou ao canal secundário com o primário indisponível, com data e hora registradas
- [ ] O post-incident review está versionado no repositório, tem linha do tempo em UTC e nenhuma frase atribuindo a falha a uma pessoa
- [ ] A causa raiz registrada descreve o mecanismo que falhou, e não a ação de alguém
- [ ] As ações do review têm dono e data, e o Issue responsável por conferir cada uma está nomeado
- [ ] Depois do tratamento, `/actuator/health` responde `UP`, o healthcheck L4/L7 da Issue 01 sai com 0 e o tráfego da Issue 08 fica dentro dos thresholds
- [ ] Nenhuma transação duplicada ou perdida: o log da Issue 07 não mostra erro 5xx nem falha de transação na janela do incidente

## Validação

- Abrir o runbook de uma regra sem saber por que o alerta disparou e seguir a sequência até o serviço voltar, sem consultar histórico da falha
- Conferir na configuração da regra que a anotação do runbook existe e que o arquivo referenciado está no caminho declarado
- Deixar o canal primário indisponível, disparar um alerta de teste e confirmar a chegada no canal secundário com horário
- Inspecionar o log do serviço no intervalo do incidente e confrontar a linha do tempo do review com o horário do alerta
- Consultar `/actuator/health` e rodar o healthcheck L4/L7 da Issue 01 depois do tratamento
- Executar o tráfego sintético da Issue 08 logo após o tratamento e conferir o p95 dentro do threshold
- Conferir no banco que a transação do incidente não ficou duplicada nem perdida

## Evidências

- Arquivo de runbook de uma regra, com a marcação de que foi executado por alguém sem contexto da causa
- Notificação recebida no canal secundário com data e hora, junto do registro do canal primário indisponível
- Arquivo do post-incident review, com a linha do tempo em UTC e as ações com dono e data
- Saída de `/actuator/health`, do healthcheck L4/L7 e do tráfego da Issue 08 depois do tratamento
- Trecho do log do serviço no intervalo do incidente, sem erro 5xx e sem falha de transação

## Limitações / notas

- **Não há equipe, há dono.** O plantão nesta stack é a pessoa responsável e um contato secundário declarado; o que esta Issue prova é procedimento e chegada no canal secundário, não escala humana
- **O incidente pode ser induzido** com a mesma técnica da Issue 08 (limiar artificialmente baixo ou latência adicionada no caminho) ou ser real. Induzido é mais seguro e reprodutível; real é mais honesto sobre o impacto — registre qual dos dois foi usado
- **Canal pago não entra** no desenho: o escalonamento tem de funcionar com o canal da Issue 08 e um secundário gratuito
- **Sem culpa não é sem responsabilidade**: o review descreve o mecanismo da falha e as ações decorrentes; ele não substitui a decisão de não repetir a causa
- Se o exercício cego falhar porque o runbook não bastou, esse é o resultado mais valioso desta Issue — registre a falha em vez de refazer o exercício até passar
