---
aliases: [estudo-10]
tags: [estudo]
issue: 10
---

# Estudos — Issue 10: Esteira CI/CD com Testes, Trivy e Gate IaC

> Material de apoio da Issue 10. Não é escopo da Issue — a `teach-devops` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Actions base

- Workflows, triggers e filtros
  - https://docs.github.com/en/actions/writing-workflows
  - https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run

**FIM:** sei explicar quando cada job dispara.

---

### B — Java e Trivy

- Build Maven com cache
  - https://github.com/actions/setup-java
  - https://maven.apache.org/surefire/maven-surefire-plugin/
- Scan bloqueante
  - https://aquasecurity.github.io/trivy/
  - https://github.com/aquasecurity/trivy-action

**FIM:** sei dizer o custo em minutos de cada gate.

---

### C — IaC no CI

- Automação Terraform
  - https://developer.hashicorp.com/terraform/tutorials/github-actions

**FIM:** sei ler um `plan` no log do CI.

