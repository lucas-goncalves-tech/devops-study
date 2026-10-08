---
# Preencha com identificadores curtos e estáveis: o primeiro é o id legível da issue,
# os demais são palavras-chave do assunto (usadas para filtrar/buscar).
aliases: [issue-01, runtime]
tags: [tracker, issue, todo]
# status: todo | andamento | bloqueada | review | done
status: todo
# prioridade: alta | media | baixa
prioridade: alta
---

# Issue NN — Área: título descritivo da entrega

<!--
Formato do título: "# Issue NN — Área: título descritivo da entrega"
- NN: número sequencial da issue no tracker
- Área: domínio afetado (ex.: runtime, api, dados, pipeline, operação)
- Título: o estado final buscado, em uma frase — não o esforço, e sim o resultado
-->

## Contexto

*O que vai aqui:* por que esta issue precisa existir **agora**. Descreva a situação atual
do sistema, o risco ou o custo de continuar como está, e as consequências concretas de não
fazer nada. Foque em fatos do repositório/operção, não em opiniões. Uma pessoa de fora do
projeto deve entender o problema lendo só esta seção.

**Exemplo:**

> Flash sale só começa quando qualquer pessoa do time consegue subir o sistema e obter a
> mesma resposta — hoje o repositório não tem caminho de subida: não existe imagem,
> composição nem contrato de configuração por ambiente, então "funciona na minha máquina"
> é a única evidência disponível. Sem ordenação de subida, a API pode iniciar antes do
> banco aceitar conexão; sem execução sem privilégio, o processo herda root que não usa;
> sem a porta do banco fechada, o dado de venda fica acessível na rede local.

## Objetivo

*O que vai aqui:* o estado final em uma ou duas frases, no formato "Estado final:
<comando/ação> → <resultado observável>". Use linguagem verificável: alguém deve conseguir
dizer, ao final da issue, se o objetivo foi atingido sem perguntar para ninguém.

**Exemplo:**

> Estado final: `docker compose up` em máquina limpa sobe a stack completa na ordem
> certa — banco saudável primeiro, API depois — com a API em container executado por
> usuário sem privilégios, healthcheck do container visível ao orquestrador e nenhuma
> porta do banco publicada na host.

## Dependências

*O que vai aqui:* outras issues, módulos ou entregas que **precisam estar prontas** antes
desta. Liste pelo id/nome e explique em uma frase o que cada uma entrega e o que esta
issue assume dela. Se não houver dependência, escreva "Nenhuma — issue de partida".

**Regra dura:** toda dependência listada aqui DEVE virar uma linha de pré-condição no
início de `## Critérios de aceitação`, com um comando que **comprove** que o item anterior
existe (ex.: "Pré-condição: `virsh snapshot-list` contém `base` — sem ele, pare aqui").
Assim é impossível marcar o checklist X sem A ter existido: o checklist nunca começa no
meio. Issue de partida (sem dependências) usa a pré-condição de que o ambiente base existe.

**Sempre, no fim desta seção:** a linha do estudo par, amarrando issue↔estudo por nome:

> - **estudo par:** `estudos/<mesmo-nome-da-issue>.md` — ler antes de executar (é o
>   currículo desta issue)

**Exemplo:**

> - Requer `01-nucleo/15-projeto-base` — o projeto multi-módulo e o perfil por ambiente
>   são entregues lá; esta Issue assume os dois e entrega como o container sobe sobre eles

## Escopo

*O que vai aqui:* tudo que **entra** nesta issue, em bullets curtos e no formato
`<artefato/função>: o que ele faz>`. Marque explicitamente o que é **assumido pronto**
(vem de outra issue) e o que é **entregue aqui**. Divergência de contrato externo
(documento, padrão do time) deve ser citada aqui como referência, não redefinida.

**Exemplo:**

> - Imagem de runtime enxuta executando como usuário sem privilégios, nunca root
> - Healthcheck declarado no container da API apontando para `GET /ready`
> - Composição inicial restrita a API + Postgres, onde o Postgres não publica porta
> - Configuração por variáveis de ambiente (porta, URL, credenciais), sem recompilar
> - **assume pronto:** `projeto-multimodulo`, `config-por-ambiente` — da `15-projeto-base`
> - **entrega:** `container-nao-root`, `compose-nao-publicado`, `conectividade-minima`

## Fora de escopo

*O que vai aqui:* o que **não** entra aqui e para **qualquer issue/trilha** aquilo
pertence. Sempre pareie cada item com o destino ("— Issue 02", "— trilha 3"), para o
leitor nunca precisar adivinhar onde aquele assunto será tratado.

**Exemplo:**

> - Domínio, API pública e contrato versionado — Issue 02
> - Migração, invariante por constraint e pool — Issue 03; testes e pipeline — Issue 04
> - Deploy, TLS, DNS e observabilidade de produção — trilha 3

## Conhecimentos envolvidos

*O que vai aqui:* os conceitos técnicos que quem implementar precisa dominar — em
tópicos curtos (uma linha cada), sem explicação. É o mapa de estudo da issue e o aviso
de que a entrega exige entendimento, não só digitação.

**Exemplo:**

> - Containers: usuário de execução, privilégio mínimo e inspeção do processo
> - Healthcheck e semântica de orquestração (saudável versus processo de pé)
> - Variáveis de ambiente, camadas de configuração e perfil ativo

## Estado atual

*O que vai aqui:* o sistema **hoje**, em bullets factuais e observáveis ("não existe X",
"Y só muda recompilando"). Nada de previsão ou desejo — só o que alguém consegue
verificar no repositório agora. É o retrato que o "Resultado esperado" vai contrapor.

**Exemplo:**

> - O repositório não tem caminho de subida: sem imagem, sem composição, sem contrato de ambiente
> - Nada prova que o processo está saudável nem que ele não roda como root
> - Comportamento só muda recompilando

## Resultado esperado

*O que vai aqui:* o "depois" em bullets observáveis, cada um espelhando um item do
"Estado atual": mesma ordem, mesmo formato, mas com o comportamento novo. Cada bullet
deve ser algo que um comando ou inspeção confirme.

**Exemplo:**

> - `docker compose up` de máquina limpa termina com banco e API `healthy`
> - Processo da API com uid distinto de zero, confirmável por `docker exec`
> - Nenhuma escuta do Postgres na host (`ss -tlnp` sem `:5432`)
> - Variável de ambiente alterada muda comportamento observável na mesma imagem

## Requisitos

*O que vai aqui:* as regras que o implementador **deve** satisfazer, uma por linha, no
modo indicativo ou imperativo ("A API responde X", "Nenhuma porta fica em Y"). Cada
requisito tem de ser único, checável e sem "se possível"/"talvez" — se é opcional, não é
requisito. Restrições herdadas de contrato externo entram aqui citando a referência.

**Exemplo:**

> - Imagem de runtime executando como usuário sem privilégios, nunca root
> - Healthcheck do container chama `GET /ready` e cai com o banco; `GET /` (liveness)
>   continua `200` sem consultar o banco
> - O Postgres não publica porta na host e nenhuma porta de serviço fica em `0.0.0.0/0`
> - Toda resposta traz `X-Request-Id` (aceito do cliente se válido, gerado quando ausente)

## Critérios de aceitação

*O que vai aqui:* checkboxes `- [ ]` traduzindo os requisitos em **afirmações testáveis**:
cada um diz o comando/inspeção, a entrada e o resultado esperado ("`docker exec ... id -u`
retorna uid ≠ 0"). Se o critério não pode ser verificado por um comando, observação ou
teste, reescreva. Inclua também o caso de falha (dependência fora do ar, dado inválido).

**Exemplo:**

> - [ ] Em máquina limpa, `docker compose up` sobe a stack e a API responde somente após o
>       banco saudável
> - [ ] `docker exec ... id -u` na API retorna uid diferente de zero
> - [ ] `ss -tlnp` na host não mostra escuta do Postgres; nenhuma porta em `0.0.0.0/0`
> - [ ] Banco parado: `GET /` continua `200` e `GET /ready` vira `503`
> - [ ] Requisição com `X-Request-Id` válido volta o mesmo id na resposta; sem ele, a API gera

## Validação

*O que vai aqui:* o roteiro passo a passo, na ordem, para **provar** os critérios: comando
→ o que deve aparecer. Comece limpando o estado anterior, cubra o caminho feliz e pelo
menos um caso de falha. Quem nunca viu a issue deve conseguir executar copiando e colando.

**Exemplo:**

> - `docker compose down -v --remove-orphans` e depois `docker compose up -d --build` →
>   log mostra o banco `healthy` antes da API iniciar; `curl -fsS http://127.0.0.1:<porta>/ready`
>   → HTTP 200
> - `docker compose exec <serviço da api> id -u` → saída com uid ≠ 0
> - Com o banco parado: `curl -i http://127.0.0.1:<porta>/` → `200`;
>   `curl -i http://127.0.0.1:<porta>/ready` → `503` sem causa interna no corpo

## Evidências

*O que vai aqui:* a lista do que será **colado** em "Evidências" ao concluir: saídas de
comando, screenshots ou trechos de log, um item por prova. Registre também o que **não**
serve de evidência (leitura contaminada por processo concorrente, por exemplo) e onde
isso deve ir — nunca como prova.

**Exemplo:**

> - Saída do `docker compose up` com a ordem de subida e de `docker compose ps`
> - Saída de `docker compose exec ... id -u` mostrando uid ≠ 0
> - Saída de `ss -tlnp` sem `:5432` e com a API em loopback
> - Leitura prejudicada por processo concorrente na host vai para `Limitações / notas`,
>   nunca como evidência

## Limitações / notas

*O que vai aqui:* o que esta issue **deliberadamente não cobre**, ressalvas de medição
(condições para um comando ser conclusivo) e acordos de handoff (o que será substituído
ou assumido por uma issue futura, qual a fronteira). Nada de dívida escondida: se há
limitação, declare aqui.

**Exemplo:**

> - `ss -tlnp` só é conclusivo se nenhum Postgres de host estiver escutando em 5432 —
>   desligá-lo antes da leitura e registrar o fato aqui
> - Healthcheck prova que o processo responde e enxerga a dependência; não prova regra
>   de negócio — a prova funcional é das Issues 02 e 04
> - Credenciais são defaults de laboratório; o contrato de segredos é da `06-pipeline-segura`
