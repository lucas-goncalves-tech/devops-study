---
aliases: [board, kanban, status]
tags: [tracker, board]
---

# SecurePay DevOps — Board

> Kanban DevSecOps. Contexto consolidado em [00 Visão Geral](00-visao-geral.md).
> 1 card por capacidade. **A Issue define o trabalho; a `teach-anything` define o aprendizado.**
> Cada linha descreve **o que a Issue resolve**, não a tecnologia que ela usa.
> Ordem de execução: `01 → 18`, seguindo a evolução operacional:
> `LOCAL → CONTAINERS → SERVIDOR → RECUPERAÇÃO → OBSERVABILIDADE → MENSAGERIA → ISOLAMENTO → AUTOMAÇÃO → IaC/CLOUD → CI/CD + DEVSECOPS → KUBERNETES`
> Material de estudo de cada Issue vive em `estudos/` — fora do escopo da Issue.

## Mapa de estado do app

> O que **existe no app** depois que a Issue está `done`. Leia de cima para baixo: cada linha assume as anteriores.

| # | O app depois dela | Etapa |
|---|---|---|
| 01 | Serviço Linux: env vars, healthcheck L4/L7 com código de saída distinto, `SIGTERM` gracioso | Local |
| 02 | Imagem < 220 MB non-root; API só recebe tráfego com o banco saudável; dados sobrevivem a restart | Local |
| 03 | Rede multi-tier em HCL idempotente; banco isolado por rota e por SG; bucket privado | *Laboratório* |
| 04 | Servidor só com login por chave, firewall mínimo, ban de brute-force e swap | **Servidor** |
| 05 | Entrada única em 80/443 com TLS automático, headers de segurança e roteamento por domínio | **Servidor** |
| 06 | Dump diário criptografado e off-site com retenção; restore testado com tempo medido | Recuperação |
| 07 | Prometheus no `/actuator/prometheus`, 4 painéis Grafana, carga k6 com thresholds | Observabilidade |
| 08 | Redis na stack como buffer via Streams, serviços isolados por perfil, gateway de webhooks | Mensageria |
| 09 | 3 redes separando fronteira/app/dados; banco e Redis inacessíveis de fora; limites anti-OOM | Isolamento |
| 10 | Pipeline que **impede o merge**: testes, scan de imagem e validação de HCL | Automação |
| 11 | Bucket versionado, IAM least privilege e endpoint privado — sem tocar código Java | *Laboratório — parked* |
| 12 | Estado remoto com lock, ambientes separados, compute mínimo com custo conhecido | **Cloud real** |
| 13 | Scanner de segredos no pré-commit e na pipeline; baseline; merge bloqueado | DevSecOps |
| 14 | SAST obrigatório, bloqueando severidade `ERROR` | DevSecOps |
| 15 | Jobs com permissão mínima, ações pinadas por SHA, auditoria anti-tag mutável | DevSecOps |
| 16 | 3 gates consolidados — verde = sem segredo, sem `ERROR`, sem CVE alta/crítica | DevSecOps |
| 17 | **Produção:** merge com gates → deploy automático na VPS → healthcheck → rollback → auditoria | **Deploy** |
| 18 | Cluster local multi-node, chart Helm, probes, limits, rollout sem downtime | Kubernetes |

**Fronteiras:** `Servidor` = `04–05` (e volta em `17`) · `Cloud real` = `12` só (`03` e `11` são laboratório) ·
`DevSecOps` = `10 → 13–16` · **produção acende em `17`**, não em `04` (ali só existe um servidor público com deploy manual) ·
`03` é **laboratório anexado**: fica no tracker, mas não é marco da narrativa (ver [Visão Geral](00-visao-geral.md)).

## Done

- [x] [01 Linux Runtime](issues/01-linux-runtime.md) — a app vira serviço do sistema: sobe com o boot, responde healthcheck, morre sem cortar requisição
- [x] [02 Docker Compose](issues/02-docker-compose.md) — tudo sobe com um comando, sem privilegiado, e a API espera o banco estar de pé

### Laboratório concluído (fora da narrativa principal)

- [x] [03 Terraform VPC](issues/03-terraform-vpc.md) — rede que se recria do zero, com o banco inacessível de fora
      _concluída cedo, como laboratório LocalStack sem custo. **NÃO** é marco da cadeia
      manual → automatizado → cloud: ela pré-existe a dor que resolve. Permanece como
      pré-requisito de `10`, `11` e `12` porque o gate de IaC valida o HCL que ela criou.
      _carry-over condicionado a `elbv2`: reativar ALB e entrada da API pelo SG do ALB_

## Parked

- [ ] [11 S3 Reports Infra](issues/11-s3-reports-infra.md) — relatório financeiro só sai no bucket certo, com permissão mínima
      _estacionada (revertida); `S3_ENABLED=false` mantém o `NoOpReportRepository` ativo e nenhuma Issue depende dela_

## To Do

### Servidor (04–05)

- [ ] [04 VPS Hardening](issues/04-vps-hardening.md) — servidor exposto só aceita chave: sem senha, sem porta aberta, sem força bruta
- [ ] [05 Reverse Proxy](issues/05-caddy-reverse-proxy.md) — uma única porta na frente, HTTPS emitido sozinho, cabeçalhos de segurança

### Recuperação (06)

- [ ] [06 DB Backups](issues/06-db-backups-s3.md) — dado sobrevive se o servidor sumir: dump diário fora do servidor, restore provado

### Observabilidade e mensageria (07–08)

- [ ] [07 Observability](issues/07-observability.md) — sei que está lento ou quebrado antes do usuário perceber
- [ ] [08 Containers e Redis](issues/08-containers-redis.md) — evento não se perde quando o consumidor cai

### Isolamento e limites (09)

- [ ] [09 Compose Isolation](issues/09-compose-isolation.md) — serviço vizinho não alcança o banco nem o Redis; nada estoura a memória

### Automação (10)

- [ ] [10 GitHub Actions](issues/10-github-actions.md) — teste quebrado, imagem com CVE ou Terraform inválido não passam revidos

### IaC e Cloud (12)

- [ ] [12 AWS Production](issues/12-aws-production.md) — ninguém aplica por cima de ninguém; sei o custo antes de subir

### CI/CD + DevSecOps (13–17)

- [ ] [13 Secrets Hygiene](issues/13-secrets-hygiene.md) — credencial não entra no repositório
- [ ] [14 SAST Semgrep](issues/14-sast-semgrep.md) — padrão inseguro não chega no merge
- [ ] [15 Pipeline Hardening](issues/15-pipeline-hardening.md) — a pipeline não vira o caminho mais curto até o repositório
- [ ] [16 DevSecOps Gates](issues/16-devsecops-gates.md) — verde significa: sem segredo, sem erro, sem CVE crítica
- [ ] [17 CI/CD VPS Deploy](issues/17-cicd-vps-deploy.md) — merge vira produção sozinho, e volta sozinho se doer

### Kubernetes (18)

- [ ] [18 Kubernetes Helm](issues/18-kubernetes-helm.md) — nó morre e o usuário não percebe
