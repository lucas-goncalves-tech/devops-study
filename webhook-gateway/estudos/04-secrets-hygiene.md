---
aliases: [estudo-04]
tags: [estudo]
issue: 04
---

# Estudos — Issue 04: Higiene de Segredos e Anti-Vazamento

> Material de apoio da Issue 04. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Segredos

- Gestão e higiene em repos
  - https://gitleaks.io/
  - https://docs.github.com/en/actions/security-for-github-actions/security-guides/using-secrets-in-github-actions
- Onde o segredo deste app vive: `WEBHOOK_SECRET` e `REDIS_URL` chegam por `dotenv.config()` a partir de `.env` (`webhook-gateway/app/src/index.ts`) — o segredo real nunca vai para o repositório, e sim para a variável de ambiente ou para o secret do GitHub Actions

**FIM:** sei dizer onde o segredo mora em cada ambiente.

