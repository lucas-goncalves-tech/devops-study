---
aliases: [trilha2-01, ci-build]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 01 — CI: o build que roda longe da minha máquina

## Contexto

Hoje os 56 testes verdes significam "passou **no meu** laptop, com **minhas** versões e
**meu** `.env`" — exatamente a evidência que a Trilha 1-01 já desmontou para a imagem.
CI (Integração Contínua) é a máquina que roda o build em ambiente limpo a **cada** push:
se o código só compila com a JDK local ou com uma env var esquecida, é a CI que descobre
— antes do merge, não na VPS. É o item mais cobrado em pipeline de vaga júnior ("tem
CI/CD?" é a pergunta de sempre) e o primeiro degrau: sem build verde confiável, automatizar
deploy é empurrar quebra para aVM.

## Objetivo

Estado final: push no GitHub dispara workflow que em ambiente limpo roda `mvn verify`
(56 testes), com Maven cacheado entre execuções; build verde exigido para seguir (branch
protegida com check obrigatório); falha de teste derruba o job com vermelho visível —
reproduzido também por `act` ou re-run local, não só uma vez.

## Dependências

- **Requer Trilha1-01..04** — a mesma app que a CI builda é a que está publicada na VM;
  a pré-condição é o estado final da Trilha 1.
- **pré-condição verificável:** `ssh lab@<ip-vm> 'docker compose ps'` → 4 healthy (stack
  viva) e repositório GitHub com a main pushada — se o repo ainda não existir no GitHub,
  criá-lo e pushar é o **primeiro passo desta Issue** (pré-condição declarada aqui, não
  produto da Trilha 1).

## Escopo

- Workflow `.github/workflows/ci.yml`: trigger `push` + `pull_request` na main
- Steps: checkout → setup-java (17) → cache Maven (`.m2` via `actions/cache` ou cache
  nativo do setup-java) → `mvn -B verify` → reportar falha
- Segredo mínimo ainda **não** — os envs de teste vêm do `application-test.yml` do repo
  (o `JWT_SECRET` da suíte: ou vira default no teste, ou secret da action — decidir na
  issue; aqui o requisito é "CI não depende do meu laptop")
- Branch protection: check do workflow obrigatório para merge
- **assume pronto:** `stack-na-vm`, `https-responde-de-fora` — da Trilha 1-04
- **entrega:** `workflow-ci`, `cache-maven`, `check-obrigatorio`

## Fora de escopo

- Build/push de imagem Docker — Issue 02
- Deploy, SSH, secrets de infra — Issues 03–04
- Qualidade de código (lint, SAST) — estágio futuro (DevSecOps); a Trilha 4 cobre
  segredos e imagem (T4-02/T4-03), não análise do código
- Multi-branch, release automation, semver — estágio futuro

## Conhecimentos envolvidos

- O que CI é (e o que CD é) — fronteira do termo
- Runner: a máquina efêmera do GitHub no que ela tem e no que ela não tem (sem `.env`, sem
  Docker state, sem cache até a primeira vez) — build/teste só; o deploy da Issue 03 já
  usa outro modelo (self-hosted no host, por causa do NAT da VM)
- Jobs, steps, triggers — anatomia de um workflow
- Cache: o que acelera, o que invalida, e por que cache é aceleração não correção
- Branch protection + required check: onde o "verde obrigatório" é imposto

## Estado atual

- Testes só rodam onde o autor roda, com o `.env` do autor
- Nenhum check automático antes de merge; código quebrado entra no main
- A VPS/VM não tem relação nenhuma com o repositório

## Resultado esperado

- Push → aba Actions com job verde em minutos; `mvn -B verify` com 56 testes
- Segundo push sem mudar pom → log mostra Maven cacheado (skip de download)
- Push com teste quebrado → job vermelho com a falha visível no log
- PR sem check verde → merge bloqueado (branch protection)

## Requisitos

- Workflow disparando em `push` e `pull_request`
- `mvn -B verify` (não `-DskipTests` — a CI **é** o lugar dos testes)
- Cache do Maven entre runs (`.m2` ou cache do `setup-java`)
- Setup de JDK 17 explícito e pinado (`actions/setup-java` com versão fixa)
- A CI roda **sem** `.env` do autor: tudo que a suíte precisa está no repo (config de
  teste) ou em secret da action — decidir e declarar qual
- Branch protection na `main` com o check do workflow obrigatório
- Workflow versionado no repo (`.github/workflows/` — config como código)

## Critérios de aceitação

- [ ] Pré-condição: `ssh lab@<ip-vm> 'docker compose ps'` → 4× healthy (Trilha 1-04) e
      `git push` da branch main funciona no GitHub — sem as duas pontas, pare aqui
- [ ] Push qualquer → job `ci` executando e terminando `success` no GitHub Actions
- [ ] Log do step de teste → `Tests run: 56, Failures: 0, Errors: 0` gravado na CI
- [ ] Segundo run sem mudar `pom.xml` → log mostra cache hit (download de deps pulado)
- [ ] Run com teste forçado a falhar (commit temporário) → job `failure` com o teste
      visível no log; revert do commit → verde de novo (**prova do vermelho**)
- [ ] `settings → branches → main` → required status check `ci` ativo; tentar merge sem
      check → bloqueado
- [ ] A CI não referencia caminho local: `grep -r '\.env' .github/` → vazio (o run não
      depende do meu `.env`)
- [ ] Workflow inteiro no repo: `git ls-files .github/workflows/ci.yml` → rastreado

## Validação

- `git push` de um commit qualquer → Actions → run verde; abrir o step → 56 testes
- Re-run (`Re-run all jobs`) → mesmo resultado e cache aquecido no segundo run
- Quebrar de propósito: adicionar `@Test` que falha, push → run vermelho com o stack
  trace; `git revert` → verde
- `act` (local, opcional): `act -j ci` para ver o workflow sem push — declarar se usado
- Branch protection: `gh api repos/:owner/:repo/branches/main/protection` ou UI →
  required check presente

## Evidências

- Print/saída do run verde com `Tests run: 56` no step
- Log do segundo run com cache hit (trecho com "Downloading"/"Using cache")
- Par vermelho→verde: run falho (com o teste no log) e run de revert verde
- Config da branch protection com o required check

## Limitações / notas

- Runner do GitHub é máquina efêmera e gratuita para repo público; privado tem limite de
  minutos — o lab não estoura, mas é o limite que existe
- `mvn -B verify` na CI roda os testcontainers? Se a suíte usar Testcontainers, o runner
  **tem** Docker; se usar o `application-test.yml` puro, não precisa — conferir a suíte e
  declarar aqui qual caminho foi usado (a suíte atual usa mocks/testcontainers? A issue
  exige: CI verde com os 56, seja como for — o detalhe vai para Limitações)
- Verde na CI **não** prova que roda na VM (o ambiente da CI ≠ da VM) — a Trilha 2-03
  liga o verde ao deploy real; aqui é só "o build é confiável fora do meu laptop"
- Required check impõe o **verde**, não a qualidade do teste — teste que não testa nada
  também passa (a revisão humana continua necessária)
