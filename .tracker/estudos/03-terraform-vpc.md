---
aliases: [estudo-03]
tags: [estudo]
issue: 03
---

# Estudos — Issue 03: Infraestrutura como Código com Terraform, VPC Multi-Tier, Roteamento e ALB

> Material de apoio da Issue 03. Não é escopo da Issue — a `teach-devops` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Terraform base

- HCL, estado e drift
  - https://developer.hashicorp.com/terraform/docs
  - https://developer.hashicorp.com/terraform/cli/commands/plan

**FIM:** sei explicar idempotência e drift.

---

### B — Rede e storage AWS

- VPC, subnets, route tables, SGs
  - https://docs.aws.amazon.com/vpc/latest/userguide/what-is-amazon-vpc.html
  - https://docs.aws.amazon.com/vpc/latest/userguide/vpc-security-groups.html
  - https://docs.aws.amazon.com/vpc/latest/userguide/vpc-route-tables.html
- Internet Gateway e diferença entre subnet pública e privada
- S3 e bloqueio público
  - https://docs.aws.amazon.com/s3/

**FIM:** sei justificar 3 tiers, rota de cada subnet e SG encadeado.

---

### D — Load Balancer

- ALB vs NLB vs GLB, Target Groups e health checks
  - https://docs.aws.amazon.com/elasticloadbalancing/latest/application/introduction.html
  - https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-target-groups.html
- Security Groups encadeados (ALB → API → DB)
  - https://docs.aws.amazon.com/vpc/latest/userguide/vpc-security-groups.html

**FIM:** sei justificar quando usar ALB vs NLB; health check configurado.

---

### C — Emulador local

- Integração com Terraform
  - https://docs.localstack.cloud/user-guide/integrations/terraform/
- O que o LocalStack free cobre e o que ele simplifica em relação à AWS real

**FIM:** sei apontar endpoint e serviços emulados, e separar validação local de prontidão para produção.

