---
aliases: [board, kanban, status]
tags: [tracker, board]
---

# SecurePay DevOps — Board

> Kanban DevSecOps. Contexto consolidado em [[00-visao-geral|00 Visão Geral]].
> 1 card por capacidade. **A Issue define o trabalho; a `teach-devops` define o aprendizado.**
> Ordem de execução: `01 → 18`, seguindo a evolução da infraestrutura:
> `LOCAL → CONTAINERS → VPS/LINUX → OPERAÇÃO → HARDENING/BACKUP/MONITORAMENTO → AUTOMAÇÃO → IaC → CLOUD → CI/CD + DEVSECOPS → KUBERNETES`
> Material de estudo de cada Issue vive em `estudos/` — fora do escopo da Issue.

## Mapa de estado do app

> O que **existe no app** depois que a Issue está `done`. Leia de cima para baixo: cada linha assume as anteriores.

| # | O app depois dela | Trilha |
|---|---|---|
| 01 | Serviço Linux: env vars, healthcheck L4/L7 com código de saída distinto, `SIGTERM` gracioso | Local |
| 02 | Imagem < 220 MB non-root; API só recebe tráfego com o banco saudável; dados sobrevivem a restart | Local |
| 03 | Rede multi-tier em HCL idempotente; banco isolado por rota e por SG; bucket privado | Laboratório (LocalStack) |
| 04 | Servidor só com login por chave, firewall mínimo, ban de brute-force e swap | **VPS** |
| 05 | Entrada única em 80/443 com TLS automático, headers de segurança e roteamento por domínio | **VPS** |
| 06 | Prometheus no `/actuator/prometheus`, 4 painéis Grafana, carga k6 com thresholds | Observabilidade |
| 07 | Redis na stack como buffer via Streams, serviços isolados por perfil, gateway de webhooks | Mensageria |
| 08 | 3 redes separando fronteira/app/dados; banco e Redis inacessíveis de fora; limites anti-OOM | Operação |
| 09 | Dump diário criptografado e off-site com retenção; restore testado com tempo medido | Backup |
| 10 | Pipeline que **impede o merge**: testes, scan de imagem e validação de HCL | CI |
| 11 | Bucket versionado, IAM least privilege e endpoint privado — sem tocar código Java | Laboratório — **parked** |
| 12 | Estado remoto com lock, ambientes separados, compute mínimo com custo conhecido | **Cloud real** |
| 13 | Scanner de segredos no pré-commit e na pipeline; baseline; merge bloqueado | DevSecOps |
| 14 | SAST obrigatório, bloqueando severidade `ERROR` | DevSecOps |
| 15 | Jobs com permissão mínima, ações pinadas por SHA, auditoria anti-tag mutável | DevSecOps |
| 16 | 3 gates consolidados — verde = sem segredo, sem `ERROR`, sem CVE alta/crítica | DevSecOps |
| 17 | **Produção:** merge com gates → deploy automático na VPS → healthcheck → rollback → auditoria | **Deploy** |
| 18 | Cluster local multi-node, chart Helm, probes, limits, rollout sem downtime | Kubernetes |

**Fronteiras:** `VPS` = `04–05` (e volta em `17`) · `Cloud real` = `12` só (`03` e `11` são laboratório) ·
`DevSecOps` = `10 → 13–16` · **produção acende em `17`**, não em `04` (ali só existe um servidor público com deploy manual).

## Done

- [x] [[01-linux-runtime|01 Linux Runtime]] — runtime Linux, env, healthcheck L4/L7, SIGTERM
- [x] [[02-docker-compose|02 Docker Compose]] — imagem enxuta non-root, compose com dependência saudável
- [x] [[03-terraform-vpc|03 Terraform VPC]] — rede multi-tier, SGs encadeados, bucket privado
      _carry-over condicionado a `elbv2`: reativar ALB e entrada da API pelo SG do ALB_

## Parked

- [ ] [[11-s3-reports-infra|11 S3 Reports Infra]] — IAM least privilege, bucket versionado, endpoint privado
      _estacionada (revertida); `S3_ENABLED=false` mantém o `NoOpReportRepository` ativo e nenhuma Issue depende dela_

## To Do

### VPS / Linux puro (04–05)

- [ ] [[04-vps-hardening|04 VPS Hardening]] — chave só, firewall mínimo, ban de brute-force, swap
- [ ] [[05-caddy-reverse-proxy|05 Reverse Proxy]] — entrada única, TLS automático, headers de segurança

### Monitoramento e mensageria (06–07)

- [ ] [[06-observability|06 Observability]] — Prometheus, 4 painéis, carga k6 com thresholds
- [ ] [[07-containers-redis|07 Containers e Redis]] — multi-serviço, Streams, gateway de webhooks

### Operação e hardening (08–09)

- [ ] [[08-compose-isolation|08 Compose Isolation]] — 3 redes, banco e Redis blindados, limites anti-OOM
- [ ] [[09-db-backups-s3|09 DB Backups]] — backup off-site com retenção e restore testado

### Automação (10)

- [ ] [[10-github-actions|10 GitHub Actions]] — testes Maven, scan de imagem, gate de IaC

### IaC e Cloud (11–12)

- [ ] [[12-aws-production|12 AWS Production]] — estado remoto com lock e computação mínima

### CI/CD + DevSecOps (13–17)

- [ ] [[13-secrets-hygiene|13 Secrets Hygiene]] — scanner de segredos bloqueando merge
- [ ] [[14-sast-semgrep|14 SAST Semgrep]] — análise estática bloqueando severidade ERROR
- [ ] [[15-pipeline-hardening|15 Pipeline Hardening]] — least-privilege e pinagem por SHA
- [ ] [[16-devsecops-gates|16 DevSecOps Gates]] — gates consolidados e tempo medido por gate
- [ ] [[17-cicd-vps-deploy|17 CI/CD VPS Deploy]] — deploy com gates, rollback e auditoria

### Kubernetes (18)

- [ ] [[18-kubernetes-helm|18 Kubernetes Helm]] — cluster multi-node, chart, probes e limites
