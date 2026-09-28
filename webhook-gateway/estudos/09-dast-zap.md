---
aliases: [estudo-09]
tags: [estudo]
issue: 09
---

# Estudos — Issue 09: DAST com OWASP ZAP

> Material de apoio da Issue 09. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.

### A — DAST

- O que é DAST e como difere de SAST
  - https://owasp.org/www-project-devsecops-guideline/
  - https://owasp.org/www-project-web-security-testing-guide/
- Testes dinâmicos na pipeline
  - https://owasp.org/www-project-top-ten/
  - https://owasp.org/www-project-application-security-verification-standard/

**FIM:** sei dizer o que o scanner enxerga que o SAST não enxerga.

---

### B — OWASP ZAP

- ZAP: início rápido
  - https://www.zaproxy.org/getting-started/
  - https://www.zaproxy.org/docs/desktop/
- Baseline scan (o modo desta Issue)
  - https://www.zaproxy.org/docs/docker/baseline-scan/
  - https://www.zaproxy.org/docs/desktop/addons/
- Regras de alerta e tabela de severidade
  - https://www.zaproxy.org/docs/alerts/
  - https://www.zaproxy.org/docs/desktop/addons/alert-filters/
- Relatórios
  - https://www.zaproxy.org/docs/desktop/ui/
- Scan autenticado (fase seguinte)
  - https://www.zaproxy.org/docs/desktop/addons/authentication-helper/

**FIM:** sei rodar um baseline contra o serviço de pé e abrir o relatório.

---

### C — Achado comum em serviço Node

- Cabeçalhos de segurança que o ZAP procura
  - https://owasp.org/www-project-secure-headers/
- Cookies e atributos `HttpOnly`/`Secure`
  - https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html
- TLS e banner de servidor
  - https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Security_Cheat_Sheet.html

**FIM:** sei diferenciar alerta que exige correção de alerta que exige justificativa.

---

### D — Repetibilidade e segurança da varredura

- Executando o ZAP com alvo controlado
  - https://www.zaproxy.org/docs/docker/
- Spider e carga gerada pelo scanner
  - https://www.zaproxy.org/docs/desktop/start/features/spider/
- Múltiplas execuções e comparação de relatórios
  - https://www.zaproxy.org/docs/desktop/ui/

**FIM:** sei repetir o mesmo scan e comparar relatório com relatório.
