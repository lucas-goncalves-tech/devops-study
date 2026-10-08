# ROADMAP — devops-study

> Painel único: **o que está feito, o que vem, o que foi adiado.** É a fonte de visão
> geral do repo — acima das pastas (`VPS/`, `AWS/`, `DEVOPS/`, `SECURITY/`).

## Legenda e manutenção

- `[x]` executado (prática concluída, evidência no tracker) · `[ ]` não iniciado
- **Manutenção manual:** ao passar uma issue para `status: done` no frontmatter,
  marcar o checkmark correspondente aqui (mesma sessão, antes do commit)
- Conteúdo em português, termos técnicos em inglês

## Sequência das áreas

| # | Área | Pasta | Estado | Próximo marco |
|---|------|-------|--------|---------------|
| 1 | Infraestrutura — Junior | `VPS/` | **em andamento** — trilhas 0–7 com material pronto (27 issues + 27 estudos), execução 0/27 | executar a Trilha 0 |
| 2 | Cloud — Junior → Pleno | `AWS/` (futura) | não iniciado | nasce após a área 1 |
| 3 | DevOps — Pleno | `DEVOPS/` (futura) | não iniciado | refaz a área 2 com IaC |
| 4 | Cloud Security — Pleno/Sênior | `SECURITY/` (futura) | não iniciado | endurece tudo que existir |

## Regra de reprática — cada área refaz as bases do zero

Nada de "já vi, pular". Ao entrar numa área nova, os conceitos-base da área anterior
são **refeitos do zero** no novo contexto:

| Área nova | O que significa "fazer do 0" |
|-----------|------------------------------|
| **AWS** (área 2) | Na primeira EC2 (EC2 = a VM na nuvem, o equivalente à VM KVM da Trilha 0): refazer a Trilha 0 inteira — filesystem, usuários, SSH, firewall, systemd, logs, packages — e as Trilhas 1–2 (Docker, Dockerfile, Compose, git) re-instalados e reconfigurados à mão na instância recém-criada. Diferença didática: **banco vira serviço gerenciado (RDS)** em vez de container, e a rede vira VPC em vez de bridge do KVM. |
| **DEVOPS** (área 3) | Refazer com IaC o que foi feito à mão: Terraform recria VPC/EC2/security group que existiam só na console; Ansible refaz o hardening da Trilha 4; o pipeline recria o CI/CD; Kubernetes reencena o Compose. A mesma infra, agora declarada. |
| **SECURITY** (área 4) | Refazer hardening, supply chain e secrets sobre a arquitetura nova — agora com IAM, KMS, policy as code e CIS sobre a stack que já existe. |

---

# Área 1 — Infraestrutura (Junior) · `VPS/`

## Status por trilha

| Trilha | Escopo | Material | Execução |
|--------|--------|----------|----------|
| 0 — Fundação Linux | VM/KVM, SSH, firewall+updates, systemd, backup/restore | 5/5 issues + estudos | 0/5 |
| 1 — Containers e borda | Dockerfile, Compose, reverse proxy+TLS, publicação | 4/4 | 0/4 |
| 2 — Git e CI/CD | CI build, registro, deploy, rollback, git avançado (merge/rebase/tag) | 5/5 | 0/5 |
| 3 — Observabilidade | logs, métricas, alertas, runbook | 4/4 | 0/4 |
| 4 — Hardening | hardening, secrets, supply chain, incidente | 4/4 | 0/4 |
| 5 — Redes | fundamentos: OSI/TCP-IP, IPv4, subnetting/CIDR, TCP/UDP, DNS/DHCP | 1/1 | 0/1 |
| 6 — Linux profundo | bash scripting + cron, filesystems/mounts/LVM | 2/2 | 0/2 |
| 7 — Troubleshooting | drills de sistema (CPU/mem/disco/processo), de rede (porta/DNS/camadas) | 2/2 | 0/2 |

**Progresso geral: 0/27 issues executadas · 27/27 com material pronto.**

## Cobertura dos tópicos

- Linux: Trilhas 0–4 na prática (SSH, systemd, logs, usuários, packages, env vars) +
  Trilha 6 (bash/cron, filesystems/mounts)
- Redes: fundamentos na Trilha 5 (teoria + exercícios + leitura na VM); **para a Área 2**
  (console AWS dá contexto): routing, ARP, DHCP, NAT, IPv6
- Servidores: VM, VPS, reverse proxy, SSH hardening, backup e monitoramento prontos;
  **para a Área 2**: Nginx/Apache (reprática na EC2), load balancer (com alvo real),
  LVM (vira EBS); bare metal fica como teoria (sem hardware no lab)
- Git: branch, PR, SSH keys e .gitignore na prática (T2) + merge/rebase/tags (T2-05)
- Troubleshooting: trilha dedicada na Trilha 7 (drills provocado → diagnóstico → prova)

### Linux

- [ ] CLI e filesystem
- [ ] Processos e serviços
- [ ] Usuários, grupos e permissões
- [ ] SSH
- [ ] Systemd
- [ ] Logs
- [ ] Package managers
- [ ] Bash scripting
- [ ] Cron
- [ ] Filesystems e mounts
- [ ] Variáveis de ambiente

### Redes

- [ ] OSI/TCP-IP
- [ ] IPv4/IPv6
- [ ] Subnetting
- [ ] CIDR
- [ ] Routing
- [ ] ARP
- [ ] DNS
- [ ] DHCP
- [ ] TCP/UDP
- [ ] HTTP/HTTPS
- [ ] TLS básico
- [ ] NAT
- [ ] Firewall
- [ ] Portas
- [ ] Proxy / Reverse Proxy
- [ ] Load Balancer

### Servidores

- [ ] VM
- [ ] VPS
- [ ] Bare metal
- [ ] Web server
- [ ] Reverse proxy
- [ ] Nginx/Apache
- [ ] SSH hardening
- [ ] Storage
- [ ] Backup
- [ ] Monitoramento básico

### Virtualização

- [ ] Hypervisor
- [ ] VM vs container
- [ ] CPU/RAM/Storage virtualizados
- [ ] Bridge/NAT/networking de VMs

### Containers — fundamentos

- [ ] Docker
- [ ] Images
- [ ] Containers
- [ ] Volumes
- [ ] Networks
- [ ] Dockerfile
- [ ] Docker Compose
- [ ] Registry
- [ ] Container lifecycle
- [ ] Container logs

### Git

- [ ] Repository
- [ ] Branch
- [ ] Merge/Rebase
- [ ] Pull Request
- [ ] Tags
- [ ] SSH keys
- [ ] .gitignore

### Troubleshooting

- [ ] CPU alta
- [ ] Memória alta
- [ ] Disco cheio
- [ ] Processo travado
- [ ] Porta inacessível
- [ ] DNS quebrado
- [ ] Serviço fora do ar
- [ ] Logs
- [ ] Connectivity testing

---

# Área 2 — Cloud / Infra Cloud (Junior → Pleno) · `AWS/` (futura)

> Começa a transformar o conhecimento de infraestrutura em cloud infrastructure.
> Aprender **na mão** (console/CLI) antes de qualquer IaC — é o insumo da área 3.

### Cloud fundamentals

- [ ] Datacenter
- [ ] Regions
- [ ] Availability Zones
- [ ] Edge locations
- [ ] Shared Responsibility Model
- [ ] Elasticidade
- [ ] Scalability
- [ ] High Availability
- [ ] Fault tolerance
- [ ] Disaster Recovery

### Cloud networking

- [ ] VPC/VNet
- [ ] Subnets
- [ ] Route tables
- [ ] Internet Gateway
- [ ] NAT Gateway
- [ ] Security Groups
- [ ] Network ACLs
- [ ] Private/Public subnet
- [ ] ENI/NIC
- [ ] Load Balancer
- [ ] DNS
- [ ] Private DNS
- [ ] VPN
- [ ] Peering
- [ ] Transit Gateway / Hub-and-Spoke

### Compute

- [ ] VM / EC2 / Compute instances
- [ ] Autoscaling
- [ ] Instance types
- [ ] Images
- [ ] Boot disks
- [ ] Ephemeral vs persistent storage

### Storage

- [ ] Block storage
- [ ] Object storage
- [ ] File storage
- [ ] Snapshots
- [ ] Backup
- [ ] Lifecycle policies

### Databases

- [ ] Managed database (RDS/Azure Database/etc.)
- [ ] Replication
- [ ] Backup
- [ ] Failover
- [ ] Read replicas
- [ ] Connection pooling

### Cloud services

- [ ] Queue
- [ ] Pub/Sub
- [ ] Cache
- [ ] Object storage
- [ ] Secrets
- [ ] Parameter/config management
- [ ] Container registry

### IAM

- [ ] Users
- [ ] Groups
- [ ] Roles
- [ ] Policies
- [ ] Permissions
- [ ] Least privilege
- [ ] Service identities
- [ ] Temporary credentials
- [ ] MFA

### Observability

- [ ] Metrics
- [ ] Logs
- [ ] Traces
- [ ] Alerts
- [ ] Dashboards
- [ ] Health checks

### Cloud CLI

- [ ] AWS CLI / Azure CLI / GCP CLI
- [ ] SDK basics
- [ ] Authentication
- [ ] Profiles
- [ ] Credential management

---

# Área 3 — DevOps (Pleno) · `DEVOPS/` (futura)

> De "como mantenho funcionando" para "como automatizo e torno reproduzível".
> Aqui moram Terraform e Ansible — o IaC que **refaz** a área 2.

### CI/CD

- [ ] CI
- [ ] CD
- [ ] Pipelines
- [ ] Build
- [ ] Test
- [ ] Artifact
- [ ] Deployment
- [ ] Environments
- [ ] Promotion
- [ ] Rollback
- [ ] Approval gates
- [ ] GitHub Actions / GitLab CI / Jenkins

### Infrastructure as Code

- [ ] Terraform
- [ ] Providers
- [ ] Resources
- [ ] Variables
- [ ] Outputs
- [ ] Modules
- [ ] State
- [ ] Remote State
- [ ] Plan/Apply
- [ ] Drift
- [ ] Import
- [ ] Dependency management

### Configuration management

- [ ] Ansible
- [ ] Idempotência
- [ ] Inventory
- [ ] Playbooks
- [ ] Roles

### Containers — avançado

- [ ] Multi-stage builds
- [ ] Image optimization
- [ ] Container security
- [ ] Non-root
- [ ] Resource limits
- [ ] Healthchecks
- [ ] Registries
- [ ] Image scanning

### Kubernetes

- [ ] Architecture
- [ ] Nodes
- [ ] Control Plane
- [ ] Pods
- [ ] Deployments
- [ ] ReplicaSets
- [ ] Services
- [ ] Ingress
- [ ] ConfigMaps
- [ ] Secrets
- [ ] Volumes
- [ ] StatefulSets
- [ ] Jobs/CronJobs
- [ ] Probes
- [ ] Resource requests/limits
- [ ] Namespaces
- [ ] RBAC
- [ ] Network Policies
- [ ] Helm

### Cloud architecture

- [ ] Multi-AZ
- [ ] Autoscaling
- [ ] Load balancing
- [ ] Stateless applications
- [ ] HA
- [ ] Failover
- [ ] Disaster Recovery
- [ ] Backup strategies

### Observability

- [ ] Prometheus
- [ ] Grafana
- [ ] ELK/OpenSearch
- [ ] OpenTelemetry
- [ ] Alerting
- [ ] SLI
- [ ] SLO
- [ ] SLA
- [ ] Incident response

### Reliability

- [ ] Incident management
- [ ] Root Cause Analysis
- [ ] Postmortem
- [ ] Error budgets
- [ ] Capacity planning
- [ ] Performance
- [ ] Resilience
- [ ] Chaos testing básico

### Automation

- [ ] Bash
- [ ] Python
- [ ] APIs
- [ ] CLI automation
- [ ] Webhooks
- [ ] Event-driven automation

### FinOps

- [ ] Cloud pricing
- [ ] Cost allocation
- [ ] Tagging
- [ ] Budgets
- [ ] Rightsizing
- [ ] Autoscaling
- [ ] Reserved/committed capacity
- [ ] Cost monitoring

---

# Área 4 — Cloud Security (Pleno/Sênior) · `SECURITY/` (futura)

> Segurança sobre a arquitetura e a operação que já existem nas áreas 1–3.

### Cloud Security Fundamentals

- [ ] Shared Responsibility
- [ ] Threat modeling
- [ ] Attack surface
- [ ] Security boundaries
- [ ] Defense in depth
- [ ] Zero Trust
- [ ] Least privilege

### IAM Security

- [ ] IAM architecture
- [ ] RBAC
- [ ] ABAC
- [ ] Federation
- [ ] SSO
- [ ] MFA
- [ ] Service accounts
- [ ] Workload identities
- [ ] Privilege escalation
- [ ] Permission analysis
- [ ] Just-in-time access

### Network Security

- [ ] Network segmentation
- [ ] Private/Public architecture
- [ ] Security Groups
- [ ] NACLs
- [ ] Firewall
- [ ] WAF
- [ ] IDS/IPS
- [ ] Network Firewall
- [ ] VPN
- [ ] Private endpoints
- [ ] Bastion
- [ ] Zero Trust networking

### Cloud workload security

- [ ] VM hardening
- [ ] Container security
- [ ] Kubernetes security
- [ ] Pod Security
- [ ] Kubernetes RBAC
- [ ] Network Policies
- [ ] Secrets
- [ ] Runtime security
- [ ] Image security

### Cloud IAM attack paths

- [ ] Privilege escalation
- [ ] Credential theft
- [ ] Metadata service attacks
- [ ] Over-permissioned roles
- [ ] Trust relationships
- [ ] Cross-account access
- [ ] Lateral movement

### Data Security

- [ ] Encryption at rest
- [ ] Encryption in transit
- [ ] KMS
- [ ] Key rotation
- [ ] Secrets management
- [ ] Data classification
- [ ] Backup security
- [ ] Data exfiltration prevention

### Cloud Detection & Response

- [ ] CloudTrail / audit logs
- [ ] Cloud security logs
- [ ] SIEM
- [ ] Detection engineering
- [ ] Alerting
- [ ] Incident response
- [ ] Forensics
- [ ] Threat hunting
- [ ] Cloud attack paths

### Security posture

- [ ] CSPM
- [ ] CWPP
- [ ] CNAPP
- [ ] Security benchmarks
- [ ] CIS Benchmarks
- [ ] Security posture management
- [ ] Misconfiguration detection

### DevSecOps

- [ ] SAST
- [ ] DAST
- [ ] SCA
- [ ] Secret scanning
- [ ] IaC scanning
- [ ] Container scanning
- [ ] SBOM
- [ ] Supply-chain security
- [ ] Dependency security
- [ ] Security gates

### Infrastructure Security

- [ ] Secure Terraform
- [ ] IaC security
- [ ] Terraform state security
- [ ] Policy as Code
- [ ] OPA
- [ ] Sentinel
- [ ] Drift detection
- [ ] Secure modules

### Kubernetes Security

- [ ] RBAC
- [ ] Service Accounts
- [ ] NetworkPolicy
- [ ] Pod Security
- [ ] Admission Controllers
- [ ] Secrets
- [ ] Image policies
- [ ] Runtime security
- [ ] Cluster hardening
- [ ] Supply chain

### Cloud Governance

- [ ] Policies
- [ ] Guardrails
- [ ] Landing Zones
- [ ] Account/Subscription organization
- [ ] Compliance
- [ ] Audit
- [ ] Logging strategy
- [ ] Resource governance

### Security Architecture

- [ ] Threat modeling
- [ ] Secure reference architectures
- [ ] Identity boundaries
- [ ] Trust boundaries
- [ ] Segmentation
- [ ] Blast radius
- [ ] Failure isolation
- [ ] Security controls

### Compliance

- [ ] CIS
- [ ] NIST
- [ ] ISO 27001
- [ ] SOC 2
- [ ] LGPD
- [ ] PCI DSS
- [ ] Evidence collection
- [ ] Audit trails

---

# Backlog — o que foi adiado (origem → destino)

Promessas escritas nas issues que **não** pertencem ao estágio atual. Atribuição de
destino provisória — confirmar na hora de executar.

## Estágio VPS real (host + domínio próprios)

| Item | Origem |
|------|--------|
| Domínio real, Let's Encrypt e renovação automática | T1-03 |
| DNS público apontando para a VM | T1-04 |
| WAF, rate limit de borda, HSTS preload | T1-03 |
| Status page pública (statuspage.io, domínio) | T3-04 |
| Teste externo/ofensivo de verdade | T4-04 |

## Área 2 — AWS (cloud)

| Item | Origem |
|------|--------|
| Conta em provedor Cloud/DNS | T1-03 |
| Máquinas múltiplas, cloud (rede bridge → VPC) | T0-01 |
| Bastion, chaves de produção, certificados | T0-02 |
| Múltiplos serviços, depends complexos | T0-04 |
| Backup cifrado/off-site/3-2-1 (rsnapshot/borg/restic) | T0-05 |
| Registro de imagem próprio, multi-host, multi-arch | T1-04, T2-02 |
| Ambientes múltiplos (staging/prod), blue-green, canary | T2-03, T2-04 |
| OIDC/short-lived credentials do GitHub → cloud | T2-03 |
| Deploy com instâncias efêteras (modelo cloud) | T2-03 |
| Redes avançadas: routing, ARP, DHCP, NAT, IPv6 | Trilha 5 (decisão de escopo) |
| Nginx/Apache (reprática do proxy na EC2) | T1-03 (Caddy escolhido no lugar) |
| Load balancer com alvo real (ALB / nginx upstream) | ROADMAP (área 1, sem trilha) |
| LVM com resize (vira EBS volume) | T0-01 (particionamento padrão) |

## Área 3 — DevOps (IaC e observabilidade avançada)

| Item | Origem |
|------|--------|
| CIS Benchmark completo com automação (Ansible) | T4-01 |
| Vault / external secrets manager (SOPS, age) | T4-02 |
| Centralização de logs (ELK/Loki/OpenSearch), coleta multi-host | T3-01 |
| APM/traces distribuídos (Jaeger), long-term storage (Thanos/Mimir) | T3-02 |
| Blackbox/heartbeat externo | T3-03 |
| Multi-branch, release automation, semver | T2-01 |
| Feature flags / kill switch | T2-04 |
| Rollback de migração de banco (Flyway undo) | T2-04 |
| Backup/restore de banco (pg_dump, expand/contract) | T2-04 |
| Contrato de segredos versionado | T1-02 |

## Área 4 — Security (DevSecOps)

| Item | Origem |
|------|--------|
| fail2ban, IDS, rate limit de borda | T0-03 |
| Lint/SAST nos pipelines | T2-01 |
| Assinatura de imagem (cosign), SBOM | T2-02 |
| Alvos ofensivos (OWASP NodeGoat/JuiceShop) | pesquisa do tracker |
