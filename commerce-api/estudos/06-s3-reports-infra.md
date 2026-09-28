---
aliases: [estudo-06]
tags: [estudo]
issue: 06
---

# Estudos — Issue 06: Infraestrutura S3 para Relatórios Financeiros

> Material de apoio da Issue 06. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — IAM: identidade, Role vs User, least privilege

- O que é um principal e por que toda requisição à AWS precisa responder "quem sou eu"
  - https://docs.aws.amazon.com/IAM/latest/UserGuide/id_users.html
  - https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html
- **Role vs User:** Role é assumida temporariamente por quem já está dentro da AWS; User gera access key fixa de longa duração
  - https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles.html
- Policy escopada num bucket vs `*`
  - https://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies.html

**FIM:** sei justificar por que a política atinge só o bucket; sei dizer por que uma Role é a resposta certa para o app na VPC e um access key não é.

---

### B — S3: versão, criptografia e retenção

- Versioning
  - https://docs.aws.amazon.com/AmazonS3/latest/userguide/versioning.html
- Server-side encryption
  - https://docs.aws.amazon.com/AmazonS3/latest/userguide/serv-side-encryption.html
- Lifecycle rules: por que versioning sem expiração faz o bucket crescer para sempre
  - https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lifecycle-mgmt.html

**FIM:** sei explicar o que o versioning recupera, o que a SSE protege, e por que um `deleteObject` com versioning ligado **não** diminui o storage.

---

### C — Emulador local

- S3 e IAM emulados, endpoint local vs real
  - https://docs.localstack.cloud/user-guide/aws/s3/
  - https://docs.localstack.cloud/user-guide/aws/iam/
- `ENFORCE_IAM=1` é Pro e desligado por padrão: sem ele nada é negado
  - https://docs.localstack.cloud/aws/capabilities/security-testing/iam-policy-enforcement
- Cobertura de IAM no LocalStack: quais operações S3 já foram testadas para negação
  - https://docs.localstack.cloud/aws/capabilities/security-testing/iam-coverage

**FIM:** sei apontar endpoint local vs AWS real e o custo de cada um ($0 vs $/GB); sei dizer quais provas desta issue o emulador **não** consegue entregar.

---

