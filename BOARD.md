---
aliases: [board, kanban, status]
tags: [tracker, board]
---

# SecurePay DevOps — Board

> Kanban DevSecOps. Contexto consolidado em [00 Visão Geral](00-visao-geral.md).
> 1 card por capacidade. **A Issue define o trabalho; a `teach-anything` define o aprendizado.**
> Cada linha descreve **o que a Issue resolve**, não a tecnologia que ela usa.
> O monorepo tem **3 apps = 3 trilhas**. Cada trilha é uma sequência completa e autônoma para o seu app — CI/CD, gates e observabilidade são construídos do zero em cada uma; o que outra trilha já fez vale como leitura, nunca como pré-requisito. **A única exceção é a integração de produção, `webhook-gateway/12`.**
> O checkbox de cada linha espelha o `status:` do frontmatter da Issue.
> Material de estudo de cada Issue vive em `<app>/estudos/` — fora do escopo da Issue.
> Scripts de check e troubleshooting moram em `<app>/scripts/` — um diretório por trilha, nunca compartilhado.

## Produção e staging

- **Produção:** sistema único e de verdade — stack `ledger + postgres` numa VPS atrás do Caddy com domínio/TLS (trilha VPS, Issues 03→07) e o `commerce` em EC2 com deploy por pipeline (trilha AWS, Issues 07 e 10).
- **Integração de produção:** a entrada de `redis` e do `webhook-gateway` na stack da VPS é a [`webhook-gateway/12`](webhook-gateway/issues/12-integracao-producao.md) — a **única** Issue do repo autorizada a depender de outro app. O consumo dos eventos do e-commerce pelo gateway continua sem card.
- **Staging:** ambiente separado, público e **deliberadamente falho**, que nunca toca a produção — `commerce 08` (falha de observabilidade) e `webhook 11` (insegurança proposital + forense de mensageria). No `ledger`, o que separa produção de staging é `aprovação ou filtro de branch` (Issue 07), sem ambiente paralelo.

## ledger-service · trilha VPS

> Java/Spring: `linux → hardening → caddy → backups → isolamento → deploy`.
> **Estado final da trilha:** serviço endurecido numa VPS real, com entrada TLS única, rede segmentada, backup off-site provado, pipeline própria que barra teste quebrado e credencial, deploy por pipeline verde com rollback e tráfego sintético com alerta real.

- [x] [01 Linux Runtime](ledger-service/issues/01-linux-runtime.md) — a app vira serviço do sistema: sobe com o boot, responde healthcheck, morre sem cortar requisição
- [x] [02 Docker Compose](ledger-service/issues/02-docker-compose.md) — tudo sobe com um comando, sem privilegiado, e a API espera o banco estar de pé
- [ ] [03 VPS Hardening](ledger-service/issues/03-vps-hardening.md) — servidor exposto só aceita chave: sem senha, sem porta aberta, sem força bruta
- [ ] [04 Caddy Reverse Proxy](ledger-service/issues/04-caddy-reverse-proxy.md) — uma única porta na frente, HTTPS emitido sozinho, cabeçalhos de segurança
- [ ] [05 DB Backups](ledger-service/issues/05-db-backups-s3.md) — dado sobrevive se o servidor sumir: dump diário fora do servidor, restore provado
- [ ] [06 Compose Isolation](ledger-service/issues/06-compose-isolation.md) — serviço vizinho não alcança o banco; nada estoura a memória
- [ ] [07 CI/CD VPS Deploy](ledger-service/issues/07-cicd-vps-deploy.md) — pipeline própria barra teste quebrado e credencial, merge vira produção sozinho e volta sozinho se doer
- [ ] [08 Tráfego sintético e alertas](ledger-service/issues/08-trafego-sintetico-alertas.md) — tráfego agendado de ponta a ponta, medido e comparado ao baseline, com coleta e alerta próprios disparando

> **Dependências:** todas internas — cada Issue desta trilha requer apenas Issues da mesma trilha.

## commerce-api · trilha AWS

> Node/Postgres: `linux → compose → terraform/LocalStack → CI → observabilidade → S3/EC2 → apply → deploy`.
> **Estado final da trilha:** API leve em computação real na nuvem, com estado Terraform remoto e lock, pipeline que bloqueia merge, infraestrutura aplicada só por pipeline identificada, deploy com rollback e staging falho que prova a observabilidade — relatório em bucket privado previsto (Issue 06 `parked`).

- [ ] [01 Linux Runtime](commerce-api/issues/01-linux-runtime.md) — a API sobe como serviço do sistema, com healthcheck e shutdown gracioso
- [ ] [02 Docker Compose](commerce-api/issues/02-docker-compose.md) — um comando sobe API e banco, imagem non-root e banco sem porta publicada
- [ ] [03 Terraform VPC](commerce-api/issues/03-terraform-vpc.md) — rede que se recria do zero, com o banco inacessível de fora
- [ ] [04 GitHub Actions](commerce-api/issues/04-github-actions.md) — teste quebrado, imagem com CVE ou Terraform inválido não passam revidos
- [ ] [05 Observabilidade](commerce-api/issues/05-observability.md) — golden signals coletados, dashboards por cima e SLO medido por carga com k6
- [ ] [07 AWS Production](commerce-api/issues/07-aws-production.md) — ninguém aplica por cima de ninguém; sei o custo antes de subir
- [ ] [08 Staging falho de observabilidade](commerce-api/issues/08-staging-falho-observabilidade.md) — a falha injetada aparece no painel, é diagnosticada por escrito e consertada com prova de antes e depois
- [ ] [09 Pipeline infra apply](commerce-api/issues/09-pipeline-infra-apply.md) — infraestrutura só muda por pipeline identificada, atrás de aprovação e sem chave no repositório
- [ ] [10 Deploy EC2](commerce-api/issues/10-deploy-ec2-pipeline.md) — merge publica imagem imutável e faz subir sozinho; healthcheck falho reverte ao digest anterior

### Parked

- [ ] [06 S3 Reports Infra](commerce-api/issues/06-s3-reports-infra.md) — relatório financeiro só sai no bucket certo, com permissão mínima
      _estacionada (revertida): nenhuma Issue da trilha depende dela e ela não é marco da sequência `01 → 10`._

## webhook-gateway · trilha DevSecOps

> Node/Redis: `pipeline base → secrets → SAST → SCA → hardening → gates → DAST → mensageria → integração`.
> **Estado final da trilha:** pipeline agnóstica de cloud que barra segredo, erro estático e CVE alta/crítica, com SCA e DAST exercitados, Redis Streams provado na stack própria, staging inseguro de propósito e o gateway dentro da stack de produção real.

- [ ] [01 Linux Runtime](webhook-gateway/issues/01-linux-runtime.md) — o consumidor sobe como serviço do sistema, com restart e shutdown gracioso
- [ ] [02 Docker Compose](webhook-gateway/issues/02-docker-compose.md) — imagem non-root, rota de saúde e Redis sem porta publicada
- [ ] [03 Pipeline base agnóstica](webhook-gateway/issues/03-pipeline-base-agnostica.md) — build e teste rodam em script local, e o workflow só chama
- [ ] [04 Secrets Hygiene](webhook-gateway/issues/04-secrets-hygiene.md) — credencial não entra no repositório
- [ ] [05 SAST Semgrep](webhook-gateway/issues/05-sast-semgrep.md) — padrão inseguro não chega no merge
- [ ] [06 SCA dependências e imagem](webhook-gateway/issues/06-sca-dependencias-imagem.md) — biblioteca vulnerável não entra, nem por dependência nem por camada da imagem
- [ ] [07 Pipeline Hardening](webhook-gateway/issues/07-pipeline-hardening.md) — a pipeline não vira o caminho mais curto até o repositório
- [ ] [08 DevSecOps Gates](webhook-gateway/issues/08-devsecops-gates.md) — verde significa: sem segredo, sem erro estático, sem CVE alta/crítica
- [ ] [09 DAST OWASP ZAP](webhook-gateway/issues/09-dast-zap.md) — o serviço rodando é examinado, e o achado é corrigido ou justificado por escrito
- [ ] [10 Containers e Redis](webhook-gateway/issues/10-containers-redis.md) — evento não se perde quando o consumidor cai, provado na stack própria
- [ ] [11 Staging inseguro](webhook-gateway/issues/11-staging-inseguro.md) — gates verdes que nunca enfrentaram um ambiente inteiro montado errado
- [ ] [12 Integração de produção](webhook-gateway/issues/12-integracao-producao.md) — `redis` e gateway entram na stack da VPS e um pagamento vira webhook assinado ponta a ponta

> **Dependências:** internas, com a **única exceção do repo**: a **12** toca a stack do `ledger` (Issues 02, 06 e 07 dele) — nenhuma outra Issue de qualquer trilha pode depender de outro app.

## Fora de escopo

- [`archive/18-kubernetes-helm/`](archive/18-kubernetes-helm/issue.md) — Issue 18 (cluster multi-node + chart Helm) arquivada por decisão: Kubernetes não é uma das 3 trilhas e segue como candidata a 4ª trilha futura.
- `interview-prep-finops` e `ansible` — trilhas do `devops-study` que não foram escolhidas para este monorepo; continuam lá, fora daqui.
