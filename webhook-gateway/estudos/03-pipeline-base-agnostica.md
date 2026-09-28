---
aliases: [estudo-03]
tags: [estudo]
issue: 03
---

# Estudos — Issue 03: Pipeline Base Agnóstica

> Material de apoio da Issue 03. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.

### A — Script como unidade de verdade

- Bash: `set -euo pipefail` e código de saída
  - https://www.gnu.org/software/bash/manual/
  - https://mywiki.wooledge.org/BashFAQ/048
  - https://mywiki.wooledge.org/BashFAQ/105
- `npm ci`, `npm test` e lockfile
  - https://docs.npmjs.com/cli/v10/commands/npm-ci
  - https://docs.npmjs.com/cli/v10/commands/npm-test
- TypeScript e `vitest run` (o `build` e o `test` deste app)
  - https://www.typescriptlang.org/tsconfig
  - https://vitest.dev/guide/

**FIM:** sei escrever um script que falha com código não zero e que dá o mesmo resultado em duas máquinas.

---

### B — Anatomia de um workflow

- GitHub Actions: eventos, jobs e passos
  - https://docs.github.com/en/actions/using-workflows
  - https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax
- Cache de dependências e invalidação
  - https://docs.github.com/en/actions/using-workflows/caching-dependencies-to-speed-up-workflows
- GitLab CI: `.gitlab-ci.yml` como outro consumidor dos mesmos scripts
  - https://docs.gitlab.com/ci/
- CircleCI e Jenkins, como outros consumidores do mesmo script
  - https://circleci.com/docs/
  - https://www.jenkins.io/doc/book/pipeline/

**FIM:** sei mostrar que o YAML só chama o script, e não reimplementa o build.

---

### C — Agnosticidade e por que ela custa

- The Twelve-Factor App, fator de build
  - https://12factor.net/build-release-run
- Por que um script é mais portável que um job
  - https://martinfowler.com/articles/microservices.html
- Executando o mesmo script sem Docker
  - https://docs.docker.com/build/
  - https://docs.docker.com/compose/overview/
- Ambientes reprodutíveis (imagem como build agent)
  - https://docs.docker.com/build/building/multi-stage/

**FIM:** sei dizer o que a pipeline garante de ambiente e o que fica por conta do script.
