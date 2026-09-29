---
aliases: [issue-10, rampa, carga-em-rampa, k6]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 10 — Carga em rampa até degradação, com o ponto de inflexão registrado

## Contexto

A [Issue 05](05-observability.md) roda carga com 50–100 VUs e um threshold fixo de p95 abaixo de
500ms. Isso responde "o serviço aguenta um patamar?", que é a pergunta certa para um smoke test de
p95, mas é a pergunta errada para descobrir quanto o serviço aguenta. Um threshold fixo só diz que
passou, nunca onde a curva quebra, e ninguém sabe hoje em que vazão a API começa a degradar — que é
justamente o número que dimensiona produção, alerta e justifica custo.

## Objetivo

Estado final: um script k6 que cresce em estágios até o critério de falha, com o ponto de inflexão
registrado — o estágio exato onde a curva quebra, a vazão atingida nele e a série de event loop lag
colada na evidência.

## Dependências

- Requer [Issue 09 — Plano de capacidade](09-plano-de-capacidade.md): a rampa é dimensionada pela
  vazão-alvo que aquela Issue calcula, e não por um número escolhido no chute.
- Requer [Issue 05 — Observabilidade](05-observability.md): é ela que publica `/metrics` com
  `prom-client` e que traz a carga base a ser transformada em rampa.

## Escopo

- Script k6 com estágios `ramping-vus` que **crescem até o critério falhar e param ali**, em vez de
  rodar um número fixo de VUs
- Curva de RPS × p95 × erros ao longo dos estágios, preservada como saída da execução
- **Event loop lag entrando no `/metrics`**, publicado pela mesma rota de métricas que a Issue 05
  criou
- Persistência da saída de cada execução, com o estágio da degradação legível no resumo
- Identificação e registro do **ponto de inflexão**: o último estágio que passou e o primeiro que
  quebrou, com a vazão de cada um

O vocabulário de medição está em
[`docs/performance/dicionario-de-medicao.md`](../../docs/performance/dicionario-de-medicao.md).

## Fora de escopo

- **Corrigir qualquer coisa.** Esta Issue mede e nomeia o limite; a correção é da
  [Issue 12](12-correcao-com-prova.md).
- **Procurar a causa.** Dizer que degrada é o produto desta Issue; dizer *por que* degrada é da
  [Issue 11](11-localizacao-do-gargalo.md).
- Carga destrutiva em massa, teste de penetração, chaos engineering.
- Tráfego sintético agendado em produção — isso é
  [Issue 08 do `ledger-service`](../../ledger-service/issues/08-trafego-sintetico-alertas.md), com
  vazão baixa e escrita em banco real, e é outra coisa.
- Kubernetes, HPA, autoscaling e orquestração — fora de escopo por decisão do monorepo.

## Conhecimentos envolvidos

- k6: `ramping-vus`, estágios, thresholds e leitura do resumo de execução
- Curvas de saturação: o que é um ponto de inflexão e como reconhecê-lo
- Event loop lag do Node: o que mede, de onde vem e por que é a métrica que separa "Node bloqueia"
  de "banco é lento"
- Relação entre vazão, latência e erro sob carga crescente

## Estado atual

A Issue 05 fixa o volume em 50–100 VUs e o critério em p95 de 500ms. Não existe rampa, não existe
registro de onde a curva quebra, e `/metrics` não publica event loop lag. A vazão de pico nunca foi
medida em lugar nenhum do repositório.

## Resultado esperado

- Uma execução que termina por degradação, não por fim de tempo
- O ponto de inflexão registrado com vazão e p95
- A série de event loop lag da janela, anexada

## Requisitos

- [ ] Escrever o script k6 com a vazão-alvo da [Issue 09](09-plano-de-capacidade.md) declarada em
      comentário, e estágios que crescem a partir dela
- [ ] Fazer a rampa **parar no primeiro estágio em que o threshold falha**, e registrar esse estágio
- [ ] Declarar em comentário qual recurso se espera que sature primeiro, para que a
      [Issue 11](11-localizacao-do-gargalo.md) tenha uma hipótese declarada a confrontar
- [ ] Registrar o último estágio que passou e o primeiro que quebrou, com a vazão de cada um
- [ ] Publicar o event loop lag na rota `/metrics` que a Issue 05 já expõe
- [ ] Manter a carga com `idempotencyKey` no corpo do checkout, como a Issue 05 já faz
- [ ] Persistir a saída da execução em arquivo, com p95 legível no resumo
- [ ] Registrar, na mesma janela, a saúde da aplicação e a ausência de erro 5xx
- [ ] Identificar e limpar o que a carga criou no banco

## Critérios de aceitação

- [ ] Existe um estágio identificado onde o critério de falha quebrou, e ele é nomeado na Issue
- [ ] O ponto de inflexão está registrado com a vazão atingida e o p95 naquele ponto
- [ ] A série de event loop lag da janela está anexada como evidência
- [ ] Durante a janela **até o último estágio que passou**, `/health` responde `200` com
      `"status":"UP"` com o banco de pé, e `503` com `"DEGRADED"` sem ele
- [ ] Nenhum erro 5xx de aplicação aparece no log **na mesma janela**, até o último estágio que
      passou — a janela de encerramento é a de degradação esperada, e exigir 5xx zero nela tornaria
      a Issue impossível de fechar
- [ ] Nenhum pedido duplicado resultante da carga
- [ ] `/metrics` e `/health` continuam registrados **fora** do `preHandler` de autenticação
- [ ] Se houve estoque negativo, ele está registrado como observação da execução, com a consulta
      que o mostra

## Validação

- Executar a rampa inteira e conferir que ela termina por falha de threshold, e não por fim de tempo
- Inspecionar a curva de RPS × p95 × erros e confirmar que o ponto de inflexão é visível
- Conferir a série de event loop lag na janela do ponto de inflexão
- Consultar `/health` e consultar o log procurando `5xx` no mesmo intervalo
- Conferir no banco o que a carga criou e a limpeza

## Evidências

- Saída do k6 com o estágio da degradação, a vazão e o p95 naquele ponto
- A curva preservada, com os dois lados do ponto de inflexão
- A série de event loop lag da janela
- Saída de `/health` e trecho do log sem 5xx no mesmo intervalo
- Consulta ao banco com o que a carga criou e o registro da limpeza

## Limitações / notas

- **`/metrics` e `/health` fora do `preHandler` de JWT é invariante herdada da Issue 05.** Se
  qualquer uma das duas cair atrás da autenticação, o scraping e o `HEALTHCHECK` do `Dockerfile`
  param de funcionar. O event loop lag novo também é publicado nessa rota, então o mesmo cuidado
  vale para ele.
- `/health` precisa continuar respondendo `200` com `"status":"UP"` quando o banco está de pé e
  `503` com `"DEGRADED"` quando não — o `HEALTHCHECK` da imagem só falha por código de saída e não
  distingue os dois casos.
- Custo zero: a rampa roda com k6 como container contra o Compose da
  [Issue 02](02-docker-compose.md), com Postgres local. Nada aqui exige VPS ou conta AWS.
- O p95 desta rampa não é o p95 de produção. É um patamar de laboratório, em hardware conhecido, e
  serve para localizar o limite relativo — não para prometer latência de campo.
- A carga escreve no banco. Ela usa `idempotencyKey` para não duplicar pedido, mas cria linhas de
  verdade: o que ela cria precisa ser identificado e removido ao final, como a Issue 08 já exige da
  carga sintética dela.
- **Estoque negativo é um resultado esperado desta Issue, não uma falha dela.** A validação de
  estoque em `orders.service.ts:45-47` não é bloqueante (sem `FOR UPDATE`), a transação roda em
  READ COMMITTED, o `UPDATE` subtrai sem guarda e `schema.ts:36` não tem `CHECK`. O lock de linha
  serializa as escritas, mas não impede que a segunda passe a validação com o saldo já gasto por
  outro. Uma rampa com muitos VUs sobre o mesmo produto encontra isso, e encontrar é o que a Issue
  promete. Exigir saldo limpo na rampa tornaria a Issue impossível de fechar e esconderia um defeito
  real de correção — que é da [Issue 12](12-correcao-com-prova.md) consertar, com a guarda nomeada
  lá como candidata.
