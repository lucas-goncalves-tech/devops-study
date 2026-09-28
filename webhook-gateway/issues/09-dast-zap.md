---
aliases: [issue-09, dast-zap]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 09 — DAST com OWASP ZAP em baseline contra o serviço de pé, com achado corrigido ou justificado

## Contexto

Segredos, SAST e SCA olham o repositório antes de a imagem existir. Nenhum deles olha o serviço em execução: cabeçalhos que não são enviados, versão de servidor revelada, rota sem autenticação, SSL mal configurado, host com DNS reverso — nada disso mora em arquivo. O `webhook-gateway` ainda tem superfície HTTP pequena (a rota `/health` que a Issue 02 cria junto do `Dockerfile`), e é exatamente por isso que o DAST agora é barato: sem alvo rico, o primeiro baseline levanta a lista de cabeçalhos e de informações expostas e ensina o que o ZAP sabe ver. O alvo que justifica varredura repetida é o ambiente falho da Issue 11.

## Objetivo

Estado final: um baseline scan do OWASP ZAP executado contra o serviço de pé, com pelo menos um achado real registrado, cada achado ou corrigido ou justificado por escrito, e a varredura rodando depois dos gates consolidados e alimentando a Issue 11.

## Dependências

- Requer Issue 08 — os gates estáticos (segredos, SAST, SCA) rodam antes; o DAST completa a cobertura, não a substitui
- Requer Issue 02 — a rota `/health` e o `Dockerfile` deste app são a superfície HTTP que a primeira varredura tem como alvo

## Escopo

- Serviço do app de pé e alcançável pelo scanner (compose local ou ambiente de teste)
- Baseline scan do ZAP em modo não autenticado, com alvo e timeout declarados
- Registro do relatório bruto (HTML ou JSON) versionado como evidência
- Triagem de cada alerta: corrigido no app ou aceito com justificativa escrita
- Repetição do scan após a correção, provando que o alerta desapareceu
- Execução da varredura depois dos gates da Issue 08, e passagem do resultado como entrada da Issue 11

## Fora de escopo

- Gates de segredos, SAST e SCA — Issues 04, 05, 06 e 08
- Scan autenticado com JWT e navegação autenticada: o alvo atual é público e não há login para assumir nesta fase
- Pentest manual e testes de negócio
- Alvo em produção: a varredura roda contra ambiente de teste, nunca contra a stack de produção
- Alertas de runtime sobre vulnerabilidade da imagem em produção — [Issue 06](06-sca-dependencias-imagem.md)

## Conhecimentos envolvidos

- DAST: o que o scanner vê de fato (a resposta HTTP, não o código)
- OWASP ZAP: baseline scan, spider, regras de alerta, relatório e exit code
- Alertas típicos em serviço Node: cabeçalhos de segurança ausentes, banner de servidor, cookies sem `HttpOnly`/`Secure`, ausência de `X-Content-Type-Options`
- Falso positivo versus baixa severidade aceita: o filtro que decide o que corrige
- Reprodução: como repetir o mesmo scan e comparar relatório com relatório

## Estado atual

- Não existe varredura dinâmica neste app nem em nenhum outro: cobertura é só de repositório
- A única superfície HTTP deste app é a rota `/health` criada pela Issue 02, então hoje não há autenticação a assumir
- Nenhum relatório de scanner existe como evidência de nada
- A Issue 11 (staging inseguro de propósito) ainda não tem o alvo que este DAST precisa

## Resultado esperado

- Relatório de baseline existe, com timestamp, alvo e lista de alertas
- Ao menos um alerta real registrado, com correção verificada por segunda execução
- Cada alerta remanescente tem justificativa escrita e nomeada

## Requisitos

- [ ] Subir o serviço de pé em ambiente de teste (compose local ou equivalente), com a rota alvo respondendo
- [ ] Executar o baseline scan do ZAP contra o alvo, com URL alvo, timeout e política de alerta declarados
- [ ] Salvar o relatório bruto da primeira execução (HTML ou JSON) como evidência versionada
- [ ] Classificar cada alerta do relatório em corrigido, aceito com justificativa ou falso positivo
- [ ] Corrigir no app ao menos um alerta real (por exemplo cabeçalhos de segurança ausentes ou banner de servidor)
- [ ] Repetir o scan após a correção e arquivar o segundo relatório, com o alerta corrigido ausente
- [ ] Registrar em um arquivo a justificativa de cada alerta aceito, com o motivo e a condição de reavaliação
- [ ] Declarar no repositório que a varredura roda depois dos gates da Issue 08 e que a Issue 11 é o alvo de reexecução

## Critérios de aceitação

- [ ] Existe relatório de baseline do ZAP contra o serviço de pé, com alvo, timestamp e pelo menos um alerta real listado
- [ ] Cada alerta do relatório tem ou a correção verificada na segunda execução (alerta ausente) ou a justificativa escrita em arquivo
- [ ] O relatório da segunda execução não lista o alerta que foi corrigido, e os dois relatórios estão arquivados lado a lado
- [ ] Um alerta aceito com justificativa aparece escrito no arquivo de justificativas, com motivo e condição de reavaliação
- [ ] A varredura é executada em ambiente de teste: nenhum relatório aponta para a stack de produção
- [ ] A sequência registrada mostra DAST rodando depois dos gates da Issue 08, e não no lugar deles

## Validação

- Confirmar que a rota alvo responde antes de rodar o scan
- Rodar o scan e abrir o relatório, listando os alertas
- Aplicar a correção e rodar o scan de novo, comparando os dois relatórios
- Rodar com o serviço fora do ar e confirmar que o scanner falha ruidosamente em vez de "passar" sem alvo
- Conferir o arquivo de justificativas contra a lista de alertas remanescentes

## Evidências

- Relatório HTML/JSON da primeira execução com a lista de alertas
- Relatório da execução após a correção, com o alerta ausente
- Arquivo de justificativas dos alertas aceitos
- Trecho de log mostrando a ordem: gates da Issue 08 verdes, depois DAST
- Saída do scanner com o serviço fora do ar, provando que ele falha em vez de fingir verde

## Limitações / notas

- **A superfície é pequena de propósito:** com só a rota `/health`, os primeiros achados são cabeçalhos e banner; achado de rota autenticada só é possível quando o gateway de entrega de webhooks entrar na stack ([Issue 10](10-containers-redis.md))
- Baseline scan é assíncrono e mais rápido que o scan completo, mas é o que produz relatório; scan completo (active scan) é mais barulhento e não é o escopo desta fase
- "Falso positivo" é uma afirmação que precisa de prova: a justificativa tem de dizer o que o ZAP viu e por que isso não se aplica ao serviço
- Scan em staging falho (Issue 11) vai produzir mais achado que aqui, porque o alvo tem porta exposta, credencial fraca e segredo no artefato — a varredura é a ferramenta que enxerga parte dessas fraquezas, não todas
- Nunca aponte o scanner para produção: o spider do ZAP gera tráfego que pode disparar alerta e trigger de deploy (ver Issue 07 do `ledger-service`)
- O resultado desta Issue é insumo da Issue 11, não substituto dela: a matriz de quais gates pegam quais fraquezas continua sendo a entrega da Issue 11
