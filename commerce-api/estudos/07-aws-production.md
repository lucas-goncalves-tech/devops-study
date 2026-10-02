---
aliases: [estudo-07]
tags: [estudo]
issue: 07
---

# Estudos — Issue 07: Nuvem Real com Estado Remoto e Lock

> Material de apoio da Issue 07. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Backend remoto

- Backends e locking
  - https://developer.hashicorp.com/terraform/language/backend
  - https://docs.aws.amazon.com/s3/
  - https://docs.aws.amazon.com/dynamodb/

**FIM:** sei explicar state + lock.

---

### B — FinOps

- Calculadora e right-sizing
  - https://calculator.aws.amazon.com/

**FIM:** sei estimar antes de aplicar.

---

### C — Valores por ambiente

- Valores por ambiente: bloco `variable`, `*.tfvars`, `-var` e `TF_VAR_`
  - https://developer.hashicorp.com/terraform/language/values/variables

**FIM:** sei declarar `variable`, sobrepor valor com `tfvars`/`-var`/`TF_VAR_` e explicar por que laboratório e produção não compartilham o mesmo valor.

