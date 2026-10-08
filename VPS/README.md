# Lab DevOps — NoteMaster na VPS

Laboratório de estudos de **infra/DevOps júnior**: colocar este backend Java dentro de uma
VPS (Ubuntu Server — VM primeiro, provedo real depois), aplicando os conceitos que o mercado
cobra. O app é a **carga fixa do laboratório**: ele existe para ser operado, não para ser
desenvolvido. A partir daqui, `src/` e `pom.xml` são intocados — tudo que muda é infra.

> Por que este recorte? Ver a pesquisa que embasa o lab em
> [`.tracker/pesquisas/`](.tracker/pesquisas/) — vagas (01), Reddit (02), roadmap.sh (03).

## O que este repositório contém

- **`src/` + `pom.xml`** — API Spring Boot 3.5 (Java 17) com PostgreSQL, Redis (rate limit),
  JWT, Flyway e **observabilidade já instrumentada** (Actuator + Micrometer →
  `/api/v1/actuator/prometheus`, readiness cai com o banco, liveness não). O que sobe na VPS
  é isto.
- **`.tracker/`** — o sistema de estudo:
  - `issues/` — o que fazer, em ordem ([formato](.tracker/issues/issue-example.md));
  - `estudos/` — o que entender antes de fazer ([formato](.tracker/estudos/estudos.example.md)),
    com o fecho de gatilhos de entrada para a skill `teach-anything`;
  - `pesquisas/` — a pesquisa de mercado que define as trilhas.
- **Ausente de propósito:** `Dockerfile` e `compose.yaml`. Construí-los é entrega das issues,
  não cópia pronta.

## Como usar

1. Leia a issue em `issues/` (a ordem é o número).
2. Antes de executar, leia o estudo correspondente em `estudos/`.
3. Quando algo não fizer sentido, chame a `teach-anything` com um dos gatilhos do fim do
   estudo (a skill gera as próprias perguntas durante a sessão, adaptando ao seu nível).
4. Execute a issue; cole as evidências exigidas; só então passe para a próxima.

## Trilhas

| Trilha | Tema | Entrega visível |
|---|---|---|
| **0 — Fundação da máquina** | Ubuntu Server na VM, SSH key-only, sudo, ufw, systemd, backup | máquina endurecida e reprodutível |
| **1 — Containerizar e expor** | Dockerfile (non-root), compose (app+PG+Redis, healthcheck), reverse proxy + TLS | app no ar, porta fechada, HTTPS |
| **2 — Entrega contínua** | GitHub Actions: build, imagem, deploy via SSH, rollback | push → deploy sem mão |
| **3 — Operar** | logs, Prometheus + Grafana (scrape no que já está instrumentado), alertas, runbook | dashboards e alertas funcionando |
| **4 — Endurecer** | hardening, secrets, resposta a incidentes | checklist de segurança aplicado |

**Fora do lab (estágio futuro):** AWS + IaC (Terraform/Ansible) + DevSecOps com alvos tipo
OWASP NodeGoat/JuiceShop — estes, instrumentados só na borda, nunca no código deles.

## Requisitos locais

Docker, JDK 17+, e as variáveis de ambiente do `.env.example` exportadas (`set -a; . ./.env; set +a`)
— a app não tem default para `JWT_SECRET` e a suíte falha sem ele.
