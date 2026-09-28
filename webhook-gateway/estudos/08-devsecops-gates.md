---
aliases: [estudo-08]
tags: [estudo]
issue: 08
---

# Estudos — Issue 08: Quality Gates DevSecOps no CI

> Material de apoio da Issue 08. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Gates

- DevSecOps
  - https://owasp.org/www-project-devsecops-guideline/

**FIM:** sei posicionar cada gate no fluxo.

---

### B — SCA

- CVEs e dependências
  - https://aquasecurity.github.io/trivy/
  - https://docs.github.com/en/code-security/dependabot
- Neste app o SCA olha `webhook-gateway/app/package-lock.json` e a imagem construída a partir dele — o mesmo gate roda para `commerce-api` (também npm); o `ledger-service` precisaria do ecosystem Maven

**FIM:** sei triar CVE relevante vs ruído.

