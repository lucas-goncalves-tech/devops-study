---
aliases: [issue-13, custo, right-sizing, custo-por-vazao]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: media
---

# Issue 13 — Custo por vazão sustentada e checagem de right-sizing

## Contexto

A [Issue 07 — AWS Production](07-aws-production.md) estima o custo de **provisionar** um recurso
antes de subir, e essa trava de custo involuntário continua válida e não muda aqui. O que não existe
é a conta do outro lado: quanto custa **sustentar** uma vazão. Se um relatório de performance dissesse que a
API quebra em 340 RPS, ninguém converteria 340 RPS em reais por mês, e sem essa conta o
right-sizing seria opinião — não haveria como dizer se a instância dimensionada é do tamanho certo
ou se está pagando por capacidade que a aplicação nunca usa.

## Objetivo

Estado final: o custo por vazão sustentada, calculado a partir do ponto de inflexão medido pela
[Issue 10](10-carga-em-rampa.md), e uma conclusão escrita de right-sizing — a instância que a
[Issue 09](09-plano-de-capacidade.md) dimensionou aguenta o pico medido com folga, ou é
over-provision.

## Dependências

- Requer [Issue 10 — Carga em rampa](10-carga-em-rampa.md): o ponto de inflexão é a vazão que
  precisa ser sustentada, e ela é medida, não estimada.
- Requer [Issue 09 — Plano de capacidade](09-plano-de-capacidade.md): a instância que aquela Issue
  dimensionou é a que entra na conta, e é contra ela que a checagem de right-sizing é feita.

## Escopo

- Conversão do ponto de inflexão da Issue 10 em custo por RPS sustentado por mês
- A conta com a origem de cada preço escrita, do preço da instância à conta final
- Checagem de right-sizing: a instância da Issue 09 aguenta o pico medido com folga declarada, ou
  é over-provision
- Quando for over-provision, a conta do valor pago por capacidade não usada
- Link para a trava de custo de recurso pago que já vive na Issue 07

O vocabulário de medição está em
[`docs/performance/dicionario-de-medicao.md`](../../docs/performance/dicionario-de-medicao.md).

## Fora de escopo

- **Provisionar qualquer recurso.** Esta Issue entrega número, não cobrança — a trava de custo de
  recurso pago continua sendo da Issue 07 e nenhuma Issue desta sequência chega a abrir conta.
- **FinOps como programa.** A restrição de `00-visao-geral.md` mantém FinOps fora do escopo do
  monorepo: o que vive aqui é dimensionamento por vazão, não gestão de portfólio de custo.
- Savings Plans, instâncias reservadas, spot e estratégias de compra.
- Tags, showback, chargeback e alocação de custo por time ou produto.
- Dimensionamento de banco gerenciado, cache distribuída, CDN e tráfego de saída.
- Otimização de custo: esta Issue **mede** a conta, não a reduz.

## Conhecimentos envolvidos

- Custo por unidade de vazão: de RPS a reais por mês, e o que divide esse cálculo
- Right-sizing a partir de consumo medido, e a diferença entre capacidade necessária e capacidade
  contratada
- Origem de preço: por que o preço de uma instância muda com região e com família, e por que a
  origem de cada valor precisa estar escrita
- Margem de folga: quanto provisionar acima do pico medido, e por que zero margem medido é perigoso

## Estado atual

A Issue 07 tem a trava de custo antes de provisionar, e ela está de pé. Não existe nenhuma conta
que ligue a vazão medida a custo: o ponto de inflexão da Issue 10 será o primeiro número real de
capacidade do repositório, e ele ainda não foi convertido em nada. Nenhuma conclusão de
right-sizing foi registrada para nenhuma configuração.

## Resultado esperado

- O custo por RPS sustentado por mês, com a origem de cada preço
- Uma conclusão de right-sizing em uma das duas direções, escrita
- O valor do overprovision, quando houver
- Nenhum recurso provisionado

## Requisitos

- [ ] Registrar a origem de cada preço usado: a fonte, a região e a data de referência
- [ ] Converter o ponto de inflexão da Issue 10 em custo por RPS sustentado por mês
- [ ] Declarar a margem de folga aplicada sobre o pico medido, e o porquê dela
- [ ] Escrever a conclusão de right-sizing em **uma das duas direções**: aguenta o pico com folga,
      ou é over-provision
- [ ] Se for over-provision, registrar o valor pago por capacidade não usada
- [ ] Referenciar a trava de custo de recurso pago da Issue 07
- [ ] **Não provisionar nada** — a Issue entrega número, não recurso

## Critérios de aceitação

- [ ] A conta fecha e um terceiro que refaça a partir das origens chega ao mesmo número
- [ ] Toda origem de preço está escrita, com fonte, região e data
- [ ] A conclusão de right-sizing está escrita e é uma das duas direções, não "depende" nem
      "a avaliar"
- [ ] A margem de folga está declarada com o motivo
- [ ] **Nenhum recurso foi provisionado e nenhuma conta foi aberta**

## Validação

- Refazer a conta a partir das origens de preço escritas e conferir que o resultado bate
- Confrontar a vazão-alvo da Issue 09 com o ponto de inflexão medido pela Issue 10 e conferir que
  a conclusão de right-sizing decorre dos dois números
- Confirmar que nenhuma conta foi criada e que nada está cobrado

## Evidências

- A conta de custo por RPS, com a origem de cada preço
- A conclusão de right-sizing, escrita
- A conta do overprovision, quando houver
- A referência à trava de custo da Issue 07

## Limitações / notas

- **A Issue 07 continua sendo a única Issue que pode chegar a recurso pago**, e só como prova final
  quando for preciso validar TLS público, DNS, tráfego real de internet ou deploy em nuvem real. Esta
  Issue não muda isso e não abre nenhuma conta.
- FinOps como programa permanece fora de escopo por decisão registrada em `00-visao-geral.md`. O que
  esta Issue entrega é dimensionamento por vazão, que é outra coisa.
- O pico medido pela Issue 10 é um patamar de laboratório, em hardware conhecido. Convertê-lo em
  custo assume que o hardware de produção se parece com o laboratório — e a conta precisa declarar
  essa suposição em vez de escondê-la atrás do número.
- Preço muda. Sem data de referência, a conta envelhece e deixa de ser comparável. Por isso a data
  é requisito, e não observação de rodapé.
- Sem folga, um pico um pouco acima do medido derruba o serviço. A margem é escolha operacional, e
  a escolha precisa estar escrita junto com o número.
