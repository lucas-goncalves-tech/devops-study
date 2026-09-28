---
aliases: [estudo-06]
tags: [estudo]
issue: 06
---

# Estudos — Issue 06: SCA em Dependências e Imagem

> Material de apoio da Issue 06. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.

### A — SCA e CVEs

- What is software composition analysis
  - https://owasp.org/www-project-dependency-check/
  - https://owasp.org/www-project-devsecops-guideline/
- Banco de avisos do GitHub
  - https://github.com/advisories
- CVSS: o que a severidade representa
  - https://www.first.org/cvss/
- Supply chain em dependências
  - https://slsa.dev/spec/v1.0/about
  - https://owasp.org/Top10/A06_2021-Vulnerable_and_Outdated_Components/

**FIM:** sei dizer por que CVE alta no lockfile e CVE alta na imagem são dois portões diferentes.

---

### B — `npm audit`

- Comando `npm audit` e `--audit-level`
  - https://docs.npmjs.com/cli/v10/commands/npm-audit
  - https://docs.npmjs.com/cli/v10/using-npm/config#audit-level
- Formatos de saída (incluindo JSON para consumo por máquina)
  - https://docs.npmjs.com/cli/v10/using-npm/config#json
- Lockfile e resolução de versões
  - https://docs.npmjs.com/cli/v10/configuring-npm/package-lock-json

**FIM:** sei rodar o audit com o piso declarado e explicar o código de saída.

---

### C — Trivy na imagem

- Trivy: introdução e instalação
  - https://aquasecurity.github.io/trivy/latest/docs/getting-started/
- Scan de vulnerabilidade em imagem Docker
  - https://aquasecurity.github.io/trivy/latest/docs/scanner/vulnerability/
  - https://aquasecurity.github.io/trivy/latest/docs/configuration/
- Severidade e `--exit-code`
  - https://aquasecurity.github.io/trivy/latest/docs/scanner/vulnerability/
- Base de vulnerabilidades e CVEs do sistema base
  - https://avd.aquasec.com/
- Relatório em JSON para comparar duas execuções
  - https://aquasecurity.github.io/trivy/latest/docs/configuration/reporting/

**FIM:** sei varrer a imagem deste app, filtrar por severidade e arquivar o relatório.

---

### D — Baseline de SCA e ruído

- Como se lida com ruído em SCA
  - https://owasp.org/www-project-top-ten/
- Ignorar achados com justificativa
  - https://docs.npmjs.com/cli/v10/commands/npm-audit#audit-fix
- Verificação de lockfile e o que foge dela (dep nativa)
  - https://docs.npmjs.com/cli/v10/using-npm/config#ignore-scripts

**FIM:** sei escrever uma baseline em que toda exceção tem justificativa e data de revisão.
