# AGENTS.md

Laboratório de estudos DevOps — não é um app de produto. O documento principal é
`VPS/README.md`; leia-o antes de qualquer tarefa não trivial no tracker ou no app.

## A regra que muda tudo

`VPS/src/` e `VPS/pom.xml` são **intocados de propósito**: o app (Notes API, Spring Boot)
é a carga fixa do laboratório — existe para ser operado, não desenvolvido. Todo o trabalho
do tracker é infra (VM, containers, CI, observabilidade, hardening). Mexer no código Java
só para desbloquear a suíte de testes.

## Layout

- `VPS/` — app Maven único (Spring Boot 3.5, Java 17) + `.tracker/` (sistema de estudo)
- `VPS/.tracker/issues/` — o que fazer, em ordem de trilha; formato em `issues/issue-example.md`
- `VPS/.tracker/estudos/` — material de estudo 1:1 por issue (mesmo nome de arquivo);
  formato em `estudos/estudos.example.md`
- `.agents/skills/` — skills locais (inclui `teach-anything`, usada pelo fluxo de estudo),
  integridade via `skills-lock.json` na raiz
- Não existem CI, linter nem formatter — a única verificação executável é a suíte de testes

## Comandos (sempre a partir de `VPS/`)

As variáveis de ambiente são obrigatórias: `JWT_SECRET` não tem default e a suíte falha
sem ele. Se `VPS/.env` não existir, copie de `.env.example`.

```bash
cd VPS
set -a; . ./.env; set +a
./mvnw test                              # completo: 56 testes, exige Docker (Testcontainers)
./mvnw test -DexcludedGroups=integration # só unit: 18 testes, sem Docker
./mvnw test -Dtest=NoteServiceTest       # uma classe
```

Testes `@Tag("integration")` sobem Postgres 15 via Testcontainers (`@ServiceConnection`);
o profile `test` desliga o bucket4j, então a suíte não precisa de Redis nem de Postgres local.

## Convenções do tracker

- Arquivos `trilhaN-NN-<slug>.md`; frontmatter com `status` (todo | andamento | bloqueada |
  review | done) e `prioridade`
- Dependências entre issues se escrevem **`Requer TrilhaN-NN`** e cada uma exige uma
  pré-condição com comando comprovável no início de `## Critérios de aceitação`
  (regra documentada no `issue-example.md`)
- Ao executar uma issue: ler antes o estudo par em `estudos/` (mesmo nome) e colar as
  provas em `## Evidências` — só então passar para a próxima (fluxo do `VPS/README.md`)
- Estudo termina sempre com a seção `## Como iniciar o modo teach-anything` (gatilhos
  `"Me ensina <tópico> ..."` apontando para código real)
- `Dockerfile` e `compose.yaml` não existem no repo de propósito — criá-los é a entrega
  da Trilha 1, não uma melhoria espontânea

## Fluxo de ensino (teach-anything)

Quando a sessão for ensinar no contexto do tracker (gatilho `teach-anything` /
"me ensina"):

- Achar a próxima issue com `status: todo` — ignorar `issue-example.md` (é template,
  não issue; são **21** issues, não 22)
- O currículo são os tópicos do estudo par (`estudos/<mesmo nome>.md`): primeiro bloco =
  primeiro tópico do estudo, avançar tópico a tópico, e cada bloco desemboca no
  comando/critério correspondente da issue (porquê = estudo, prova = evidência)
- O ciclo do tracker (ler issue → estudo → executar → colar evidências) **não é matéria
  de aula**: preâmbulo de uma frase, no máximo — a sessão começa ensinando a matéria

## Convenções de git

- Conteúdo e commits em português, estilo conventional commits com escopo
  (`feat(trilha): ...`, `docs(trilha): ...`, `fix(trilha): ...`)
