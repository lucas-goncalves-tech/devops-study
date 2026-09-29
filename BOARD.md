---
aliases: [board, kanban, status]
tags: [tracker, board]
---

# SecurePay DevOps — Board

> Kanban DevSecOps. Contexto consolidado em [00 Visão Geral](00-visao-geral.md).
> 1 card por capacidade. **A Issue define o trabalho; a `teach-anything` define o aprendizado.**
> Cada linha descreve **o que a Issue resolve**, não a tecnologia que ela usa.
> O monorepo tem **3 apps = 3 trilhas**. Cada trilha é uma sequência completa e autônoma para o seu app — capacidades compartilhadas (CI, gates, observabilidade) nascem numa trilha e as outras as consomem por referência cruzada.
> O checkbox de cada linha espelha o `status:` do frontmatter da Issue.
> Material de estudo de cada Issue vive em `<app>/estudos/` — fora do escopo da Issue.

## Produção e staging

- **Produção:** sistema único e de verdade — stack `ledger + postgres + redis + webhook` numa VPS atrás do Caddy com domínio/TLS (trilha VPS, Issues 03→07; `redis` e `webhook` entram pela `webhook-gateway/10`) e o `commerce` em EC2 (trilha AWS, Issue 07).
- **Staging:** ambiente separado, público e **deliberadamente falho**, que nunca toca a produção — `ledger 08` (tráfego/alerta), `commerce 08` (falha de observabilidade) e `webhook 11` (insegurança proposital + forense de mensageria).

## ledger-service · trilha VPS

> Java/Spring: `linux → hardening → caddy → backups → isolamento → deploy`.
> **Estado final da trilha:** serviço endurecido numa VPS real, com entrada TLS única, rede segmentada, backup off-site provado, deploy por pipeline verde com rollback e tráfego sintético com alerta real.

- [x] [01 Linux Runtime](ledger-service/issues/01-linux-runtime.md) — a app vira serviço do sistema: sobe com o boot, responde healthcheck, morre sem cortar requisição
- [x] [02 Docker Compose](ledger-service/issues/02-docker-compose.md) — tudo sobe com um comando, sem privilegiado, e a API espera o banco estar de pé
- [ ] [03 VPS Hardening](ledger-service/issues/03-vps-hardening.md) — servidor exposto só aceita chave: sem senha, sem porta aberta, sem força bruta
- [ ] [04 Caddy Reverse Proxy](ledger-service/issues/04-caddy-reverse-proxy.md) — uma única porta na frente, HTTPS emitido sozinho, cabeçalhos de segurança
- [ ] [05 DB Backups](ledger-service/issues/05-db-backups-s3.md) — dado sobrevive se o servidor sumir: dump diário fora do servidor, restore provado
- [ ] [06 Compose Isolation](ledger-service/issues/06-compose-isolation.md) — serviço vizinho não alcança o banco nem o Redis; nada estoura a memória
- [ ] [07 CI/CD VPS Deploy](ledger-service/issues/07-cicd-vps-deploy.md) — merge vira produção sozinho, e volta sozinho se doer
- [ ] [08 Tráfego sintético e alertas](ledger-service/issues/08-trafego-sintetico-alertas.md) — tráfego agendado de ponta a ponta, medido e comparado ao baseline, com alerta real disparando

> **Dependências cross-app:** `07` e `08` dependem de Issues de outras trilhas — `07` requer `commerce 04` e `webhook 04`/`05`/`08`; `08` requer a coleta e os painéis de `commerce 05`.

## commerce-api · trilha AWS

> Node/Postgres: `linux → compose → terraform/LocalStack → CI → observabilidade → S3/EC2`.
> **Estado final da trilha:** API leve em computação real na nuvem, com estado Terraform remoto, pipeline que bloqueia merge e staging falho que prova a observabilidade — relatório em bucket privado previsto (Issue 06 `parked`).

- [ ] [01 Linux Runtime](commerce-api/issues/01-linux-runtime.md) — a API sobe como serviço do sistema, com healthcheck e shutdown gracioso
- [ ] [02 Docker Compose](commerce-api/issues/02-docker-compose.md) — um comando sobe API e banco, imagem non-root e banco sem porta publicada
- [ ] [03 Terraform VPC](commerce-api/issues/03-terraform-vpc.md) — rede que se recria do zero, com o banco inacessível de fora
- [ ] [04 GitHub Actions](commerce-api/issues/04-github-actions.md) — teste quebrado, imagem com CVE ou Terraform inválido não passam revidos
- [ ] [05 Observabilidade](commerce-api/issues/05-observability.md) — golden signals coletados, dashboards por cima e SLO medido por carga com k6
- [ ] [07 AWS Production](commerce-api/issues/07-aws-production.md) — ninguém aplica por cima de ninguém; sei o custo antes de subir
- [ ] [08 Staging falho de observabilidade](commerce-api/issues/08-staging-falho-observabilidade.md) — a falha injetada aparece no painel, é diagnosticada por escrito e consertada com prova de antes e depois

### Parked

- [ ] [06 S3 Reports Infra](commerce-api/issues/06-s3-reports-infra.md) — relatório financeiro só sai no bucket certo, com permissão mínima
      _estacionada (revertida): nenhuma Issue da trilha depende dela e ela não é marco da sequência `01 → 08`._

### Sequência complementar — capacidade e performance

> Complementar à trilha AWS, **não parte dela**. Numeração própria `09 → 13`; as dependências reais
> estão declaradas por link em cada Issue, não pela ordem da trilha — que segue fechada em `01 → 08`.
> Dicionário de medição compartilhado: [`docs/performance/`](docs/performance/dicionario-de-medicao.md).
> **Estado final:** vazão-alvo calculada, limite real encontrado por rampa, gargalo nomeado com
> número, correção com delta medido e custo por vazão — sem provisionar nada.

- [ ] [09 Plano de capacidade](commerce-api/issues/09-plano-de-capacidade.md) — quanto o serviço deveria aguentar, com a conta fechada antes de medir
- [ ] [10 Carga em rampa](commerce-api/issues/10-carga-em-rampa.md) — até onde aguenta de fato, e onde a curva quebra
- [ ] [11 Localização do gargalo](commerce-api/issues/11-localizacao-do-gargalo.md) — o que satura primeiro, nomeado com número
- [ ] [12 Correção com prova de efeito](commerce-api/issues/12-correcao-com-prova.md) — a mudança de maior efeito, com o delta do mesmo script lado a lado
- [ ] [13 Custo da vazão](commerce-api/issues/13-custo-da-vazao.md) — quanto custa sustentar o pico medido, e se a instância é do tamanho certo

> **Cadeia:** `05 → 09 → 10 → {11 → 12, 13}`. Custo zero: k6 como container contra o Compose da `02`.
> Kubernetes, HPA e FinOps como programa seguem fora de escopo (`archive/18-kubernetes-helm`).

## webhook-gateway · trilha DevSecOps

> Node/Redis: `pipeline base → secrets → SAST → SCA → hardening → gates → DAST → mensageria`.
> **Estado final da trilha:** pipeline agnóstica de cloud que barra segredo, erro estático e CVE alta/crítica, com SCA e DAST exercitados, Redis Streams em produção e staging inseguro de propósito como prova de que os gates pegam o que importa.

- [ ] [01 Linux Runtime](webhook-gateway/issues/01-linux-runtime.md) — o consumidor sobe como serviço do sistema, com restart e shutdown gracioso
- [ ] [02 Docker Compose](webhook-gateway/issues/02-docker-compose.md) — imagem non-root, rota de saúde e Redis sem porta publicada
- [ ] [03 Pipeline base agnóstica](webhook-gateway/issues/03-pipeline-base-agnostica.md) — build e teste rodam em script local, e o workflow só chama
- [ ] [04 Secrets Hygiene](webhook-gateway/issues/04-secrets-hygiene.md) — credencial não entra no repositório
- [ ] [05 SAST Semgrep](webhook-gateway/issues/05-sast-semgrep.md) — padrão inseguro não chega no merge
- [ ] [06 SCA dependências e imagem](webhook-gateway/issues/06-sca-dependencias-imagem.md) — biblioteca vulnerável não entra, nem por dependência nem por camada da imagem
- [ ] [07 Pipeline Hardening](webhook-gateway/issues/07-pipeline-hardening.md) — a pipeline não vira o caminho mais curto até o repositório
- [ ] [08 DevSecOps Gates](webhook-gateway/issues/08-devsecops-gates.md) — verde significa: sem segredo, sem erro estático, sem CVE alta/crítica
- [ ] [09 DAST OWASP ZAP](webhook-gateway/issues/09-dast-zap.md) — o serviço rodando é examinado, e o achado é corrigido ou justificado por escrito
- [ ] [10 Containers e Redis](webhook-gateway/issues/10-containers-redis.md) — evento não se perde quando o consumidor cai
- [ ] [11 Staging inseguro](webhook-gateway/issues/11-staging-inseguro.md) — gates verdes que nunca enfrentaram um ambiente inteiro montado errado

> **Dependências cross-app:** `10` depende da composição base já concluída em `ledger 02` e da coleta e dos painéis de `commerce 05`.

## Fora de escopo

- [`archive/18-kubernetes-helm/`](archive/18-kubernetes-helm/issue.md) — Issue 18 (cluster multi-node + chart Helm) arquivada por decisão: Kubernetes não é uma das 3 trilhas e segue como candidata a 4ª trilha futura.
- `interview-prep-finops` e `ansible` — trilhas do `devops-study` que não foram escolhidas para este monorepo; continuam lá, fora daqui.
