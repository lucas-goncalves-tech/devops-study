---
aliases: [estudo-11]
tags: [estudo]
issue: 11
---

# Estudos — Issue 11: Staging Inseguro de Propósito e Forense de Mensageria

> Material de apoio da Issue 11. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.

### A — Ambiente de teste como alvo conhecido

- Staging e pré-produção em OWASP
  - https://owasp.org/www-project-web-security-testing-guide/
  - https://owasp.org/www-project-application-security-verification-standard/
- Weak, default e exposed credentials
  - https://cwe.mitre.org/data/definitions/798.html
  - https://cwe.mitre.org/data/definitions/521.html
- Segredos em artefato e em build
  - https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html
  - https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html

**FIM:** sei declarar, por escrito, que um ambiente é falho de propósito e por quê.

---

### B — Gates como prova (o que cada um pega)

- Gate de segredos
  - https://github.com/gitleaks/gitleaks
  - https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html
- SAST
  - https://semgrep.dev/docs/getting-started/
  - https://semgrep.dev/docs/writing-rules/rule-syntax/
- SCA e imagem
  - https://aquasecurity.github.io/trivy/
  - https://docs.npmjs.com/cli/v10/commands/npm-audit
- Hardening de pipeline
  - https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions
  - https://docs.github.com/en/actions/security-guides/automatic-token-authentication
- Supply chain
  - https://slsa.dev/spec/v1.0/about

**FIM:** sei montar a matriz fraqueza → gate → saída que reprovou, e marcar o que nenhum gate pega.

---

### C — Modos de falha de Redis Streams

- Redis Streams e consumer groups
  - https://redis.io/docs/latest/develop/data-types/streams/
- Atraso do grupo: `XINFO GROUPS` e `XPENDING`
  - https://redis.io/docs/latest/commands/xinfo-groups/
  - https://redis.io/docs/latest/commands/xpending/
- Leitura do grupo e confirmação
  - https://redis.io/docs/latest/commands/xreadgroup/
  - https://redis.io/docs/latest/commands/xack/
- Recuperação de conexão e resiliência do cliente
  - https://redis.io/docs/latest/develop/clients/
  - https://github.com/redis/ioredis
- Monitoramento de memória e evictions no servidor
  - https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/memory-optimization/

**FIM:** sei medir o atraso do consumer group e dizer quem segura a mensagem.

---

### D — HMAC e assinatura de payload

- `crypto.timingSafeEqual` no Node
  - https://nodejs.org/api/crypto.html
- HMAC: construção, verificação e uso
  - https://en.wikipedia.org/wiki/HMAC
- Comparação em tempo constante e o que evita
  - https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html
  - https://owasp.org/www-project-web-security-testing-guide/

**FIM:** sei explicar por que assinatura inválida precisa de rejeição sem retry infinito.
