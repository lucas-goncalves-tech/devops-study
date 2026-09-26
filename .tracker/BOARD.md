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
