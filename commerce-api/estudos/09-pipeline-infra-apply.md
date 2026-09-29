---
aliases: [estudo-09]
tags: [estudo]
issue: 09
---

# Estudos — Issue 09: Identidade da Pipeline e Apply com Credencial Federada

> Material de apoio da Issue 09. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Identidade federada

- Quem é o principal federado
  - https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_terms_and-concepts.html
  - https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRoleWithWebIdentity.html
- `provider` de OIDC e a cadeia de confiança
  - https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_providers_create_oidc-provider.html
  - https://docs.github.com/en/actions/security-for-github-actions/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services

**FIM:** sei explicar por que não existe chave estática nesse fluxo e quem pode assumir a Role.

---

### B — Escopo da confiança e permissão mínima

- Política de confiança com escopo de repositório e branch
  - https://docs.aws.amazon.com/IAM/latest/UserGuide/access_policy_lang_evaluate-decision-conditions.html
  - https://docs.github.com/en/actions/security-for-github-actions/security-hardening-your-deployments/about-security-hardening-with-openid-connect
- `id-token: write` só no job que troca credencial
  - https://docs.github.com/en/actions/security-for-github-actions/security-hardening-your-deployments/about-security-hardening-with-openid-connect

**FIM:** sei dizer o que um `*` na política de confiança quebra.

---

### C — Ambiente, aprovação e drift

- Environments e aprovação de execução
  - https://docs.github.com/en/actions/deployment/managing-environments/about-environments
- Drift como verificação pós-apply
  - https://developer.hashicorp.com/terraform/cli/commands/plan
  - https://developer.hashicorp.com/terraform/language/state

**FIM:** sei ler a diferença entre "apply executou" e "estado está em sincronia".
