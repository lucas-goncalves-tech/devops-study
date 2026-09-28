---
aliases: [estudo-07]
tags: [estudo]
issue: 07
---

# Estudos — Issue 07: Endurecimento da Pipeline (Least-Privilege + SHA Pin)

> Material de apoio da Issue 07. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Hardening

- Runners e permissões
  - https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions
  - https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflows-do/controlling-permissions-for-github_token

**FIM:** sei mapear permissão mínima por job.

---

### B — Supply chain

- Ações de terceiros
  - https://docs.github.com/en/actions/security-for-github-actions/security-guides/using-third-party-actions

**FIM:** sei explicar o ataque que cada medida previne.

---

### C — Neste app

- O job deste app roda Node 20 com `actions/setup-node` e cache de npm; `npm ci` e `npm test` são o gate de build
  - https://docs.github.com/en/actions/using-workflows/caching-dependencies-to-speed-up-workflows

**FIM:** sei dizer o que a pinagem por SHA protege num job Node.

