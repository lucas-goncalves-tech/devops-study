---
aliases: [issue-09, capacidade, plano-de-capacidade]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: media
---

# Issue 09 — Plano de capacidade com a conta que precede o experimento

## Contexto

Ninguém sabe quantos RPS a API aguenta. O dimensionamento de uma instância nova é escolhido no
palpite, a planilha não existe, e o resultado é um servidor dimensionado por intuição — que ou
custa caro demais para a vazão que ele recebe, ou mal segura o primeiro pico real e vira herói de
incidente. A carga só vai ser medida pela [Issue 10](10-carga-em-rampa.md); antes dela existe um
passo mais barato: a conta.

## Objetivo

Estado final: uma conta fechada que vai dos usuários concorrentes até o tipo de instância, com a
origem de cada parcela escrita e um número único de vazão-alvo que a Issue 10 usa como ponto de
partida da rampa.

## Dependências

- Requer [Issue 05 — Observabilidade](05-observability.md): a conta é conferida contra a latência
  observada, não inventada. Sem p95 medido não há termo para a Lei de Little.

## Escopo

A cadeia completa de dimensionamento, com a origem de cada parcela escrita:

- Usuários concorrentes no pico
- Requisições por usuário durante a janela de pico
- Vazão-alvo em requisições por segundo
- Lei de Little: concorrência = vazão × latência
- vCPU e memória exigidos por essa vazão
- Tipo de instância correspondente

O vocabulário de medição usado por esta Issue e pelas outras quatro da sequência está em
[`docs/performance/dicionario-de-medicao.md`](../../docs/performance/dicionario-de-medicao.md).

## Fora de escopo

- Provisionar qualquer recurso. Esta Issue entrega número, não recurso.
- Medir. A medição é da [Issue 10](10-carga-em-rampa.md).
- Coleta de métricas, dashboards e alertas — [Issue 05](05-observability.md).
- Dimensionamento em nuvem gerenciada ou Multi-AZ.
- Previsão de demanda por sazonalidade, crescimento orgânico ou projeção de receita.

## Conhecimentos envolvidos

- Lei de Little e por que ela liga vazão, latência e concorrência
- Dimensionamento por vazão: de RPS a vCPU e memória
- Consumo de recurso por tipo de instância e o que muda entre famílias
- Leitura de consumo medido como entrada de dimensionamento, e não como resultado

## Estado atual

Não existe vazão-alvo, nem registrada em qualquer documento do repositório. A Issue 05 mede 50–100
VUs, o que é suficiente para provar ausência de starvation do pool, mas não diz nada sobre qual
vazão o serviço deveria atender. O único número de latência existente é o threshold de 500ms que a
Issue 05 usa como critério de aprovação.

## Resultado esperado

- Um documento com a conta, a premissa de cada parcela e a vazão-alvo resultante
- Uma vazão de pico nomeada separadamente da vazão-alvo
- Um número que um terceiro recalcula e chega ao mesmo resultado

## Requisitos

- [ ] Registrar a origem de cada parcela da cadeia: de onde vem o número de usuários, de onde vem o
      número de requisições por usuário, de onde vem a latência
- [ ] Nomear a vazão-alvo e a vazão de pico como dois números distintos, sem confundi-los
- [ ] Aplicar a Lei de Little explicitamente, mostrando a conta e não só o resultado
- [ ] Traduzir a vazão-alvo em vCPU e memória, e daí em um tipo de instância
- [ ] Marcar cada premissa como medida ou estimada, sem apresentar estimativa como fato
- [ ] Deixar a vazão-alvo referenciável pela [Issue 10](10-carga-em-rampa.md), que vai usá-la como
      ponto de partida da rampa

## Critérios de aceitação

- [ ] A conta fecha aritmeticamente: um terceiro que refaça a soma a partir das premissas escritas
      chega ao mesmo número
- [ ] Toda parcela tem a origem declarada, e nenhuma origem é circular
- [ ] A vazão-alvo é um número, não uma faixa nem um "de X a Y"
- [ ] Toda premissa estimada está marcada como estimada
- [ ] Nenhum recurso foi provisionado e nenhuma conta foi aberta

## Validação

- Refazer a conta do início a partir das premissas escritas e conferir que o resultado bate com o
  registrado
- Conferir que a origem de cada parcela aponta para algo real e verificável

## Evidências

- O documento de capacidade, com as parcelas, as origens e o cálculo final
- A conta da Lei de Little, mostrando os dois lados da igualdade

## Limitações / notas

- Esta Issue não mede: ela produz a expectativa contra a qual a [Issue 10](10-carga-em-rampa.md)
  vai medir. Se a conta e a medição divergirem, a divergência é descoberta pela
  [Issue 11](11-localizacao-do-gargalo.md), e não corrigida aqui.
- O valor da latência usado na Lei de Little vem do que a Issue 05 mediu sob carga sintética, que
  é um patamar controlado, não o p95 do pico real. A conta herda essa limitação e precisa declará-la.
- Dimensionar por conta não substitui medir. Uma conta com premissas erradas produz um número
  errado com muita confiança — por isso toda premissa tem origem e o tipo (medida ou estimada) é
  declarado.
