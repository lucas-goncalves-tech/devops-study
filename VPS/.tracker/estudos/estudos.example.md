# Estudo — <tema da Issue NN>

<!--
Este arquivo é o modelo de como escrever um estudo em VPS/.tracker/estudos/.
O arquivo gerado NÃO é para uma pessoa humana fixa: é material que a skill
teach-anything usa para ensinar 1:1, bloco a bloco, quem estuda a Issue.
As instruções abaixo valem para quem escreve o estudo; o "Exemplo completo"
no fim mostra um arquivo real, com tópicos nascidos do assunto.
-->

## Como escrever este arquivo

**O que é:** material de estudo que acompanha **uma** Issue (`VPS/.tracker/issues/issue-XX.md`).
Acompanha, mas não é o contrato da Issue: **aqui é para entender, lá é para provar**. Nada
aqui substitui os Critérios de aceitação — e nenhum deles é copiado pra cá.

**Como achar o assunto (um estudo por Issue, 1:1):**

- Leia a Issue inteira antes de escrever. Os tópicos vêm do que ela **exige que a pessoa
  domine**: o que aparece em `Conhecimentos envolvidos`, mais o que o `Contexto` e o
  `Escopo` pressupõem sem explicar. Se a Issue não exige nada novo, não escreva estudo.
- São 3–5 tópicos. Se a lista passar disso, a Issue está grande ou o estudo está tentando
  ensinar demais — corte até o que esta Issue realmente precisa.

**Como escrever (sem seções engessadas):**

- **Os títulos nascem do assunto**, em forma de afirmação curta ou pergunta de verdade
  ("Por que a estrutura é decisão de código, não enfeite"). É proibido usar seção fixa
  (Introdução / Desenvolvimento / Conclusão / Referências) e é proibido copiar os títulos
  das seções da Issue — o estudo tem estrutura própria, a da explicação.
- **Receita mínima de cada tópico**, na ordem em que a explicação pede (não obrigatoriamente
  todas, mas nada de teoria solta):
  - *Por que importa* — o custo concreto de ignorar aquilo (o que quebra, o que dói);
  - *Mecanismo* — causa e efeito: quando faz X, acontece Y, porque Z; diagrama, tabela ou
    árvore quando ajudar a ver a relação;
  - *Exemplo no app* — ancorado no código real do repositório, com nomes do domínio do
    projeto, nunca exemplo genérico de internet;
  - *Fronteira entre Issues* — o que esta Issue entrega, o que é de outra (cite pelo id),
    para ninguém treinar no errado.
- Proibido: atalho de "best practice" sem mecanismo, jargão sem apresentação, lista enorme
  de conceitos sem conexão entre eles.
- O tom é de quem explica de verdade — o exemplo abaixo não é formalidade, é o padrão.

**Fecho obrigatório — o gancho com a teach-anything:**

- Última seção sempre `## Como iniciar o modo teach-anything`, com um gatilho de entrada
  por tópico, no formato `"Me ensina <tópico> ..."` apontando para o código/app reais.
- **O que essa seção É:** os gatilhos para *começar* a sessão de ensino — o mapa do que o
  estudo cobre, em forma de pedido. Basta falar um deles (ou equivalente com suas palavras)
  para a sessão começar por este material.
- **O que essa seção NÃO é:** as perguntas da sessão. A teach-anything gera as próprias
  perguntas ao vivo, em cada bloco, a partir do que acabou de ensinar e da sua resposta —
  se você disser "não sei", ela retrocede e ensina um nível abaixo com uma pergunta nova.
  Nada disso é pré-escrito aqui; só a porta de entrada é.
- Uma pergunta de entrada que não pode ser respondida com um bloco demonstrado no chat
  (só com arquivo alterado) não entra — a teach-anything é somente leitura.

## Exemplo completo

# Estudo — Estrutura de pacotes e clean code

> Material de estudo da Trilha 1. Acompanha a Issue 02 (estrutura declarada), mas não é o
> contrato daquela Issue: aqui é para entender, lá é para provar.

## Por que a estrutura é decisão de código, não enfeite

A estrutura de pacotes decide duas coisas caras: onde um programador procura quando precisa mudar
uma regra, e o que ele pode importar sem medo. Ela não é "organizar pastas bonitinha" — é a
resposta a "quem depende de quem", escrita no diretório. Sem ela, cada arquivo importa o que
quiser e uma mudança em "só o controller" quebra o domínio de outra feature.

- **Package-by-layer** (`controllers/`, `services/`, `repositories/`): dói quando a feature
  cresce — a regra de compra fica espalhada em 4 pastas e ninguém sabe o conjunto completo da
  capacidade; adicionar uma coluna obriga a abrir 4 diretórios.
- **Package-by-feature** (`compra/`, `catalogo/`, `sessao/`): dói quando a camada muda — trocar o
  banco obriga a tocar todas as features, a menos que a camada dentro da feature tenha nome e
  fronteira claros.

O formato da casa combina os dois: **feature no topo, camada dentro da feature**.

## O formato deste app

```text
com/ticketflow/
├── compra/          # capacidade: compra
│   ├── api/         # controller, DTO de entrada/saída
│   ├── application/ # casos de uso, orquestração, transação
│   ├── domain/      # regras, invariantes, tipos do agregado
│   └── infra/       # adapter de persistência desta feature
├── catalogo/        # evento, seção, ingresso (o "estoque")
├── sessao/          # emissão, refresh, revogação (Issue 10)
├── admin/           # rotas de operador (Issue 08)
├── security/        # transversal dentro de comum/: emissão/validação de token, filtros
└── core/            # (ou common) transversal dentro de comum/: tipos e utilitários compartilhados
```

A árvore acima é o **interior de `comum`**; os artefatos da §9.1 são `api`, `consumidor` e
`comum`, e cada `security`/`core` é um transversal **dentro de `comum`**, não um artefato paralelo.

O que cada camada pode importar (dependências apontam para dentro):

| Camada | Pode importar | Não pode importar |
|---|---|---|
| `api` | `application`, `domain` | `infra` direto |
| `application` | `domain`, portas de `infra` definidas em `domain` | controllers, framework de web |
| `domain` | nada de fora da própria feature | `api`, `infra`, `security` |
| `infra` | `domain` | `api`, `application` |

Regra prática: **`domain` não conhece `api` nem `infra`** — ele é o centro, e todos apontam
para ele, nunca o contrário.

## Módulos transversais

- **`security`**: o que é emitir e validar token, o filtro que lê o header, o módulo que aplica
  401/403 (Issue 05). Cruzar features é o trabalho dele — por isso fica fora delas, dentro de
  `comum`.
- **`core`/`common`**: só o que **duas ou mais** features realmente compartilham (um tipo de
  erro, um formato de problema+json, um utilitário de tempo). Se só uma feature usa, o lugar é
  dentro dela.
- **Erro comum:** `common` vira lixeira — "não sei onde põe, jogo aqui". Quando isso acontece, a
  estrutura parou de dizer a verdade: olhe de qual feature aquele tipo nasceu e mova para lá.

## Regra de dependência escrita

A Issue 02 cobra que a estrutura declarada bata com a árvore real **e** que a regra esteja
escrita em `docs/arquitetura.md`: uma frase por aresta proibida, sem espaço para interpretação.

> "O pacote `domain` de uma feature não importa `api`, `infra` nem `security`." — uma frase,
> testável lendo o código.

Enquanto a regra só existe na cabeça de quem escreveu, ela sobrevive uma sprint. Escrita, ela
vira conversa de revisão; depois (Issue 02 é o passo manual) vira teste automatizado.

## Clean code aplicado ao domínio

- **Nomes de negócio:** `disponibilidadeRestante`, não `qtd2`; `esgotarLote`, não `processar3`.
  O vocabulário é o mesmo do Contexto das Issues (evento, seção, ingresso, compra).
- **Funções curtas com propósito único:** um método que valida, grava e formata resposta não tem
  nome verdadeiro — ele tem três.
- **Sem lógica no controller:** o `api` traduz HTTP; decidir se a compra cabe no lote é
  `application` + `domain`. Controller gordo é o sintoma mais comum de arquitetura esquecida.
- **Estado onde ele pertence:** a disponibilidade vive no banco (Issue 03), não num campo
  estático da classe — memória de processo não sobrevive nem à próxima thread.
- **Anêmica vs. rica:** a modelo anêmica é data class com getters e a regra espalhada num
  service (`if (ingresso.getQtd() > 0)` em três lugares); a modelo rica põe a invariante no
  objeto (`ingresso.reservar()` recusa quando esgotado). A **Issue 02** modela os quatro
  recursos e a **Issue 03** leva a invariante para o banco: objeto rico não substitui
  constraint, e constraint não substitui a regra no código.

## Evolução: ArchUnit e por que não é critério da Issue 02

Teste de arquitetura (ex.: ArchUnit) transforma a frase do `docs/arquitetura.md` em assert:
quebra a build quando alguém importa `domain` → `infra`. É o próximo passo natural — mas **não
é critério da Issue 02** de propósito: a Issue 02 cobra a declaração e a correspondência
árvore/documento, que um ser humano verifica com `find`. O teste automatizado entra como
profundização da trilha (ver `Fora de escopo` da Issue 02), depois que a estrutura existe e a
suíte (Issue 04) já roda: primeiro o comportamento observável, depois a regra que o defende.

## Mapa: quais Issues tocam qual parte da estrutura

| Issue | O que ela declara/entrega na estrutura |
|---|---|
| 02 | árvore de pacotes package-by-feature + `docs/arquitetura.md` com a regra de dependência |
| 03 | camada `infra` de persistência com a interface vinda do contrato |
| 05 | transversal `security` (dentro de `comum`): emissão, validação de token, filtros 401/403 |
| 08 | feature `admin` (rotas de operador) |
| 10 | feature `sessão`: sessões, refresh, revogação |

## Como iniciar o modo teach-anything

- "Me ensina package-by-feature usando o `src/` deste app"
- "Me ensina a regra de dependência entre camadas com exemplos do domínio de compra"
- "Me ensina clean code aplicado a este domínio: nomes, funções e modelo rico vs. anêmica"
- "Me ensina ArchUnit e como transformar o `docs/arquitetura.md` em teste"
