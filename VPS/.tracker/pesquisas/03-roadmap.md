---
title: "Caminho de aprendizado DevOps: ordem dos nós dos roadmaps e o que dá para praticar num lab de VPS"
tags: [pesquisa, roadmap]
data: 2026-10-07
---

# 03 — Roadmap / caminho (path) de aprendizado

> **Como esta pesquisa foi feita.** O roadmap do roadmap.sh é um grafo interativo (React Flow) e o
> texto visível na página HTML não traz os nós. Eu extraí o **JSON do grafo embutido no HTML**
> (mesmo endpoint/serialização que a página usa para renderizar) e ordenei os nós pela posição
> vertical (y) do layout — que é a ordem de leitura de cima para baixo do grafo. Todo o que está
> marcado como **[roadmap.sh]** abaixo vem do grafo/guia oficiais; tudo que está marcado como
> **[minha inferência]** é minha classificação/opinião, não do roadmap.

---

## 1. Estado atual do roadmap.sh/devops (fonte primária)

Fontes:

- https://roadmap.sh/devops
- https://roadmap.sh/devops-beginner (versão iniciante; botão no grafo "Visit the Beginner Version" → `/devops?r=devops-beginner`)
- Repositório: https://github.com/kamranahmedse/developer-roadmap (369K stars; a estrutura nova fica em `roadmaps/<slug>/content/*.md`)
- Changelog: https://roadmap.sh/changelog

**Recência / versão [roadmap.sh]:**

| Roadmap | `updatedAt` (do dado oficial) |
|---|---|
| devops (role, "Step by step guide ... in 2026") | **2026-07-29** |
| devops-beginner | 2026-03-04 |
| linux | 2026-05-12 |
| docker | 2026-02-26 |
| network-engineer | 2026-08-18 |
| kubernetes | 2026-09-23 |
| devsecops | 2026-06-10 |

O site está na edição **2026** e o changelog tem entrada de **17 Sep 2026** ("New Roadmaps, Lesson
Packs and UX/UI updates"); DevSecOps foi atualizado em 31 Dez 2025. Ou seja: **o roadmap está
ativo e foi editado há ~2 meses** (set/2026 = última release grande; jul/2026 = última edição do
grafo devops).

**Tamanho do grafo devops [roadmap.sh]:** 171 nós (22 tópicos, 117 subtópicos, 6 seções/caixas,
6 rótulos de grupo, 8 botões, 1 legenda) e 60 arestas; dimensões 1107×3379 px.

**Como o próprio roadmap trata "ordem" e "fundamental vs avançado" [roadmap.sh] — ponto
crítico:** ele **não tem marcador "fundamental/avançado" por nó**. Ele usa três mecanismos:

1. **Legenda de cores** no próprio grafo:
   - 🟣 `#874efe` **"Personal Recommendation / Opinion"** → ferramenta preferida do autor (= o "essencial" na prática);
   - 🟢 `#667113` **"Alternative Option - Pick this or Purple"** → alternativa, escolha uma;
   - ⚪ `#949494` **"Order in Roadmap not Strict - Learn anytime"** → **"ordem não estrita, aprenda quando quiser"**.
2. **Roadmap separado para iniciantes** (`/devops-beginner`, 11 tópicos) vs **roadmap detalhado**
   (`/devops`, 171 nós). O beginner é, portanto, a lista oficial de "o que é essencial primeiro".
3. **Guias em texto** com passos numerados (`/devops/how-to-become-devops-engineer`).

Itens marcados como **"ordem não estrita"** no grafo detalhado [roadmap.sh]: OSI Model, FTP/SFTP e
todo o bloco de **e-mail** (SMTP, IMAP, POP3S, SPF, DMARC, Domain Keys, White/Grey Listing) —
ou seja, o roadmap diz explicitamente que **protocolo de e-mail não precisa seguir a ordem**.

---

## 2. Ordem dos nós do roadmap.sh/devops (ordem visual de cima para baixo)

**[roadmap.sh]** Espinha principal (arestas sólidas do grafo) + agrupamento por linha, com a marca
de recomendação da legenda (`*` = roxo/recomendado, `alt` = alternativa, `livre` = ordem não estrita):

```
1.  Learn a Programming Language ... Python* | Go* | JavaScript/Node.js(alt)   [grupo lateral: "Scripting" → Bash*]
2.  Operating System .............. Linux: Ubuntu/Debian*, RHEL/Derivatives*, SUSE(alt)
                                   Unix: FreeBSD*, OpenBSD(alt), NetBSD(alt) | Windows(alt)
3.  Terminal Knowledge ............ Bash*, Vim/Nano/Emacs*, PowerShell(alt)
                                   Process Monitoring*, Performance Monitoring*, Networking Tools*, Text Manipulation*
                                   [grupo "Editors"]
4.  Version Control Systems ....... Git*
5.  (linha paralela)
    ├ VCS Hosting ................. GitHub*, GitLab(alt), Bitbucket(alt)
    ├ Containers .................. Docker*, LXC(alt)
    └ What is and how to setup X? . Forward Proxy*, Reverse Proxy*, Caching Server*, Firewall*, Load Balancer*
                                   [caixa "Web Server"]: Nginx*, Caddy/Tomcat/Apache/IIS (alt)
6.  Networking & Protocols ........ HTTP*, HTTPS*, DNS*, SSH*, SSL/TLS*, OSI Model(livre), FTP/SFTP(livre)
                                   [caixa "Email Protocols" — tudo livre]: SMTP, IMAP, POP3S, SPF, DMARC, Domain Keys, White/Grey Listing
7.  Cloud Providers ............... AWS*, Azure*, Google Cloud*, DigitalOcean/Hetzner/Alibaba/Heroku/Render (alt)
    Serverless .................... AWS Lambda*, Cloudflare*, Vercel/Netlify/Azure Functions/GCP Functions (alt)
8.  Configuration Management ...... Ansible*, Puppet/Chef/Salt (alt)
    Provisioning .................. Terraform*, CloudFormation/Pulumi/AWS CDK (alt)
9.  CI / CD Tools ................. GitHub Actions*, CircleCI*, GitLab CI*, Jenkins/Buildkite/TeamCity/Railway/Octopus (alt)
    Logs Management ............... Elastic Stack*, Loki*, Graylog/Splunk/Papertrail (alt)
10. Infrastructure Monitoring ..... Prometheus*, Grafana*, Datadog*, Zabbix(alt)
    Secret Management ............. Vault*, Sealed Secrets/ESO/SOPS/Cloud Specific Tools (alt)
11. Container Orchestration ....... Kubernetes*, Docker Swarm/OpenShift/GKE-EKS-AKS/AWS ECS-Fargate (alt)
    Observability ................. Jaeger/OpenTelemetry/New Relic/Datadog/Dynatrace (alt)
    Artifact Management ........... Artifactory*, Nexus, Cloud Smith (alt)
    GitOps ....................... ArgoCD*, FluxCD(alt)
12. Service Mesh .................. Istio*, Consul*, Linkerd/Envoy (alt)
13. Cloud Design Patterns ......... Availability | Data Management | Design and Implementation | Management and Monitoring
14. (botões de saída) Backend | Docker | Kubernetes | Linux | Network Engineer
```

**Comparação: o roadmap diz "Linux/redes antes de Kubernetes"?**
Sim, em todas as variantes do próprio roadmap.sh (seção 6), e a ordem acima coloca
OS/Terminal/VCS/Redes nos ~40% iniciais do grafo e orquestração/service mesh no final.

---

## 3. CRÍTICO — Classificação de cada nó para o nosso lab de VPS/Ubuntu

Legenda da classificação (**[minha inferência]** — o roadmap.sh não faz essa classificação):

- **(a)** alcançável num lab de VPS/Ubuntu Server **sem cloud** (uma VM ou uma VPS);
- **(b)** parcialmente alcançável (precisa de **mais de uma máquina/VM** ou de **conta grátis**);
- **(c)** precisa de **cloud/pago**.

| # | Nó do roadmap [roadmap.sh] | Recomendação | Lab (inferência) | Observação prática [minha inferência] |
|---|---|---|---|---|
| 1 | Learn a Programming Language (Python/Go/JS) | Python*, Go* | **(a)** | Roda inteiro local; Python/Go no Ubuntu da VM. |
| 2 | Operating System (Ubuntu/Debian, RHEL, BSD, Windows) | Ubuntu/Debian*, RHEL*, FreeBSD* | **(a)** | É exatamente o alvo: Ubuntu Server na VM/VPS. RHEL/Rocky → precisa de outra VM (b). |
| 3 | Terminal Knowledge (+ Bash, editores, monitoring de processos) | Bash*, Vim*, ps/top/awk/grep* | **(a)** | Núcleo do lab: sem custo nenhum. |
| 4 | Version Control Systems (Git) | Git* | **(a)** | Repo local já basta. |
| 5 | VCS Hosting (GitHub/GitLab/Bitbucket) | GitHub* | **(b)** | Conta grátis; GitLab self-hosted na VPS seria (a) mas é pesado. |
| 6 | Containers (Docker, LXC) | Docker* | **(a)** | Docker Engine roda bem numa VPS; cuidado com RAM (<2 GB é apertado). |
| 7 | "What is and how to setup X" (proxies, cache, firewall, LB, web servers) | Nginx*, Forward/Reverse Proxy*, LB*, Firewall* | **(a)** / LB **(b)** | Nginx como reverse proxy do Spring Boot = (a). Balancear de verdade exige 2+ instâncias → (b). |
| 8 | Networking & Protocols (HTTP/HTTPS/DNS/SSH/SSL/OSI) | HTTP/HTTPS/DNS/SSH/SSL* | **(a)** | DNS próprio (BIND/CoreDNS) e TLS (Let's Encrypt) na VM; domínio público custa → (b). |
| 9 | Email Protocols (SMTP/SPF/DMARC/...) | `livre` (ordem não estrita) | **(b)/(c)** | Exige domínio próprio + IP dedicado/reputação; fora do escopo mínimo do lab. |
| 10 | Cloud Providers (AWS/Azure/GCP/DO/Hetzner/...) | AWS*, Azure*, GCP* | **(c)** | É o nó mais caro. Free tier (b) é frágil. **Fica para o estágio futuro**, como planejado. |
| 11 | Serverless (Lambda, Cloudflare, Vercel, Netlify...) | Lambda*, Cloudflare* | **(c)** | Sem sentido sem cloud. |
| 12 | Configuration Management (Ansible/Puppet/Chef/Salt) | Ansible* | **(b)** (básico **(a)**) | Playbook real precisa de ≥2 hosts: local + VPS, ou 2 VMs no hypervisor local. Contra `localhost` dá para estudar → (a). |
| 13 | Provisioning / IaC (Terraform, CloudFormation, Pulumi, AWS CDK) | Terraform* | **(b)/(c)** | Terraform contra cloud = (c). Terraform com provider `docker`/`libvirt`/`local` no lab = (b) e é uma ótima ponte. CloudFormation/CDK = (c). |
| 14 | CI/CD Tools (GitHub Actions, GitLab CI, Jenkins, CircleCI...) | GitHub Actions*, CircleCI*, GitLab CI* | **(b)** / **(a)** | GitHub Actions free minutes = conta grátis (b). Jenkins self-hosted na própria VPS = (a). |
| 15 | Logs Management (Elastic, Loki, Graylog, Splunk, Papertrail) | Elastic*, Loki* | **(a)** | ELK/Loki/Graylog self-hosted na VPS couber (ELK é pesado → use Loki/OpenSearch Lite). Splunk/Papertrail = (c). |
| 16 | Infrastructure Monitoring (Prometheus, Grafana, Zabbix, Datadog) | Prometheus*, Grafana*, Datadog* | **(a)** (Datadog **(c)**) | Prometheus+Grafana é o combo clássico de VPS único. |
| 17 | Secret Management (Vault, Sealed Secrets, ESO, SOPS) | Vault* | **(a)** (Sealed/ESO **(b)**) | Vault standalone na VPS = (a). Sealed Secrets/ESO exigem cluster K8s. |
| 18 | Artifact Management (Artifactory, Nexus, Cloud Smith) | Artifactory* | **(a)** (Cloud Smith **(c)**) | Nexus/Artifactory OSS self-hosted — útil para publicar o jar do Spring Boot. |
| 19 | Container Orchestration (Kubernetes, Swarm, GKE/EKS/AKS, ECS, OpenShift) | Kubernetes* | **(b)** (single-node **(a)**; managed **(c)**) | k3s/kubeadm numa VPS só = (a) porém limitado; cluster de verdade ≥2-3 nodes = (b). EKS/GKE/AKS = (c). |
| 20 | Observability (Jaeger, OpenTelemetry, Datadog, New Relic, Dynatrace) | alternativas todas | **(a)** self-hosted / **(c)** SaaS | Jaeger+OTel na VPS = (a); Datadog/New Relic/Dynatrace = (c). |
| 21 | GitOps (ArgoCD, FluxCD) | ArgoCD* | **(b)** | Precisa de cluster K8s + repositório Git remoto (conta grátis). |
| 22 | Service Mesh (Istio, Consul, Linkerd, Envoy) | Istio*, Consul* | **(b)** | Roda em single-node, mas o valor só aparece com múltiplos nós/serviços. |
| 23 | Cloud Design Patterns (Availability, Data Management, ...) | sem marca | **(a)** estudo / **(c)** exercícios | Conceitual; aplicar de verdade costuma envolver cloud. |

**Leitura agregada [minha inferência]:** dos 23 grupos, **11 são 100% (a)**, **7 são (b)** e **5 são
majoritariamente (c)**. Todo o bloco "cloud/pago" é exatamente o bloco que o nosso plano já deixou
para um estágio futuro (AWS + DevSecOps). **O caminho inteiro do nó 1 ao nó 9 + 14 + 15 + 16 + 18
(isto é, Linux → terminal → git → redes/SSH/TLS → Docker → Nginx → CI/CD → logs → métricas →
artefatos) cabe num lab de VPS/Ubuntu sem gastar nada além da própria VPS.**

---

## 4. roadmap.sh/devops-beginner — a ordem explícita passo a passo

Fonte: https://roadmap.sh/devops-beginner (e o botão `/devops?r=devops-beginner`)
**[roadmap.sh]** — 11 tópicos, com texto de instrução ao lado de cada um (extraído literalmente):

1. **What is DevOps?** — "DevOps is all about bringing developers and operations teams together to improve software delivery. The key focus areas are automation, infrastructure and monitoring."
2. **Programming Language** — "Pick any Programming Language... The goal is to learn the programming skills that you can use to write automation scripts." (Python*, Go*)
3. **Operating System → Linux** — "Learn about the file system, package managers, managing services, checking logs, bash scripting, permissions, pipes, output redirection, text manipulation tools, process monitoring, networking tools, CLI editors etc." + **"Pick Ubuntu if you have little to no experience."**
4. **Networking & Protocols** — "Learn about DNS, TCP/IP Protocols, SSH, Ports, Gateways, Routing, IP Addressing, and subnetting." + "Handy with deployments & troubleshooting"
5. **Docker** — "Learn about containerization. Be comfortable writing dockerfiles. Learn to troubleshoot. Get familiar with Alpine Linux. Learn about security, network and storage."
6. **Git / GitHub** — "Learn about git, create your GitHub profile." + "DevOps teams usually practice 'git ops' i.e. making changes to any operations related activity require a pull request against a git repository."
7. **AWS** — "Pick one of the cloud providers AWS, GCP or Azure. Start with core services e.g. AWS VPC, EC2, S3, IAM, and then RDS, Route53, Cloudwatch and ECS." → "Create and deploy some application to AWS."
8. **Terraform** — "Learn what IaC means... If you deployed an application to AWS in previous step, destroy the infrastructure and recreate it using terraform."
9. **Ansible** — "Learn what is configuration management. Understand roles, playbooks, inventory management and automation." → "Write some automation scripts e.g. db backup"
10. **GitHub Actions (CI/CD)** — "Learn about the concepts of CI/CD and how to implement in your projects using some CI/CD tool." → "Integrate CI/CD into your app."
11. **Nginx** — "Nginx is commonly used for web serving, reverse proxying, caching, load balancing..." → "Learn the basic config options, TLS setup, rate limiting, caching etc."

Frase de fechamento **[roadmap.sh]** (literal): *"At this point, you should have enough knowledge to
find a junior to mid-level (maybe even senior) DevOps position at any company depending on the
depth of your knowledge. Keep learning and building projects till you find a job."*

**Observação importante para o nosso lab [minha inferência]:** o único nó obrigatório (b)→(c) do
caminho iniciante é o **AWS (7º passo)**. Os passos 1-6 e 8-11 podem ser feitos 100% no lab
(Terraform/Ansible com provider local ou contra a própria VPS), deixando AWS para depois — exatamente
a estratégia que já está no plano.

---

## 5. Roadmaps irmãos: /linux, /docker, /network-engineer

### 5.1 https://roadmap.sh/linux — atualizado 2026-05-12 [roadmap.sh]
Ordem dos tópicos (por y): **Navigation Basics → Editing Files (Vim/Nano) → Shell and Other Basics
(stdout/stdin, env vars, redirects, super user, file permissions) → Working with Files + Text
Processing (cut/paste/sort/tr/head/tail/grep/awk/sed-like tools) → Process Management → Server
Review (uptime/load, logs de autenticação, serviços, memória/disco) → User Management → Service
Management (systemd: criar serviços, logs, start/stop/status) → Disks & Filesystems (inodes,
mounts, LVM, discos, swap) → Package Management (APT/repos/snap/logs) → Booting Linux → Networking
(TCP/IP, subnetting, Ethernet/arp, DHCP, IP routing, DNS resolution, netfilter, SSH, file transfer,
ICMP, ping, traceroute, netstat, análise de pacotes) → Shell Programming (literais, variáveis,
loops, conditionals, debugging) → Troubleshooting (ulimits, cgroups) → Containerization (container
runtime, Docker)**.
No fim, botões para: **DevOps, Backend, Docker, Network Engineer** — ou seja, o próprio roadmap linux
termina encaminhando ao DevOps. **Não há nenhuma certificação citada no grafo linux** (busca por
RHCSA/LPIC não achou nada) — [minha inferência: RHCSA não é prescrito pelo roadmap].

### 5.2 https://roadmap.sh/docker — atualizado 2026-02-26 [roadmap.sh]
Feito "in partnership with Sid Palas". Três colunas:
- **Pré-requisitos (coluna esquerda, rótulo literal "Linux Fundamentals")**: Package Managers,
  Users/Groups Permissions, Shell Commands, Shell Scripting, Programming Languages, Application
  Architecture; depois rótulo "Web Development": Command Line Utilities, Databases, Hot Reloading,
  Debuggers, Tests, **Continuous Integration**.
  → **[roadmap.sh] diz explicitamente: Linux fundamental antes de Docker.**
- **Núcleo**: Introduction (o que são containers, bare metal vs VM vs containers, Docker & OCI,
  namespaces/cgroups/union fs) → Installation/Setup (Docker Desktop vs Docker Engine Linux) → Basics
  of Docker → Using 3rd Party Images → Data Persistence (volumes/bind mounts) → Running Containers
  (docker run, docker compose) → Container Registries (DockerHub, ghcr/ecr/gcr/acr) → Building
  Images (Dockerfiles, layer caching, tamanho/segurança) → Docker CLI (images/containers/volumes/
  networks) → Container Security (image/runtime security) → Networking → Developer Experience →
  **Deploying Containers: Nomad, Docker Swarm, Kubernetes, PaaS**.
- Botões de saída: **Kubernetes Roadmap**, **DevOps Roadmap**.

### 5.3 https://roadmap.sh/network-engineer — atualizado 2026-08-18 [roadmap.sh]
Existe um bloco chamado **"Certifications"** no topo com: **CompTIA Network+, CCNA, CompTIA
Security+, Cloud Certifications** — i.e., o roadmap.sh lista CCNA como *opção de certificação* do
perfil de rede, não como etapa obrigatória de DevOps.

---

## 6. Guias de carreira do roadmap.sh (ordem em texto)

### 6.1 https://roadmap.sh/devops/how-to-become-devops-engineer [roadmap.sh] — "TL;DR" literal:
1. Learn a programming language → 2. **Get comfortable with Linux and terminal** → 3. Version
control and code hosting platforms → 4. **Networking fundamentals** → 5. Containerization →
6. Cloud services → 7. Continuous integration and delivery.

Trechos úteis [roadmap.sh]:
- "92.4% of the world's top 1 million servers run on Linux" (usa Gitnux) — justifica Linux cedo.
- Redes: "At a minimum, you should clearly understand standard protocols (TCP/IP, UDP), routing,
  IP addressing, subnetting, and ports" + "sound knowledge of web servers like Nginx".
- Passo 6 (cloud): "If you are a beginner, start with AWS... Once comfortable... learn Infrastructure
  as Code (IaC). I recommend Terraform because it is cloud-agnostic."
- Ordem dele é **containerização (5) ANTES de cloud (6) e CI/CD (7)**.

### 6.2 https://roadmap.sh/devops/career-path [roadmap.sh]
- Diz que o entrada normal é **junior DevOps engineer / release manager**; especializações:
  automation expert, systems engineer, DevOps architect, DevSecOps engineer, DevOps test engineer.
- Skills pedidas: scripting, containers/orquestração, logging + config management, administração de
  sistemas, version control, CI/CD e soft skills.
- Sobre começar a carreira: "obtain a bachelor's degree... You can also obtain DevOps certification...
  **One of the popular DevOps certifications is the AWS Certified DevOps Engineer**."

---

## 7. Fontes complementares de "path"

| Fonte | URL | Ordem que propõe | Acessível? |
|---|---|---|---|
| **honeypot.io "The DevOps Roadmap"** | https://honeypot.io/ (buscas por `honeypot.io devops roadmap`) | — | ❌ **Inacessível**: a página não respondeu em 2 tentativas (timeout) e nenhuma busca indexou um post "The DevOps Roadmap" no domínio. **Não pude usar.** |
| **milanm/DevOps-Roadmap (2026)** | https://github.com/milanm/DevOps-Roadmap | **1 Git → 2 linguagem → 3 Linux & Scripting → 4 Networking & Security → 5 Server Management → 6 Containers → 7 Orchestration → 8 IaC ("X as Code") → 9 CI/CD → 10 Monitoring & Observability → 11 um provedor cloud → 12 Software Engineering Practices → bônus DevSecOps** | ✅ |
| **AWS Builder Center – "DevOps for Beginners: A Roadmap for 2026"** (artigo de comunidade, pub. 15 Jul 2026) | https://builder.aws.com/content/3GX0vypgof1IMrmHEAgV461MIWG/devops-for-beginners-a-roadmap-for-2026 | **1 Linux → 2 Networking → 3 Git/GitHub → 4 AWS → 5 Docker → 6 CI/CD → 7 Kubernetes → 8 IaC → 9 Monitoring → 10 DevSecOps → 11 projetos** | ✅ |
| **roadmap.sh/devops-beginner** | https://roadmap.sh/devops-beginner | ver seção 4 | ✅ |

**Observação [minha inferência]:** honeypot.io não serviu como fonte; substituí por milanm
(muito citado) + artigo da AWS, que cobrem o mesmo papel de "path com ordem".

---

## 8. O que os roadmaps dizem sobre ORDEM (Linux/redes antes de Kubernetes?)

**Consenso entre as fontes [roadmap.sh + milanm + AWS]** — nenhuma fonte manda aprender Kubernetes
antes de Linux/redes:

| Fonte | Linux | Redes | Git | Docker | Cloud | IaC | CI/CD | K8s | Monitoring |
|---|---|---|---|---|---|---|---|---|---|
| roadmap.sh **beginner** | 3º | 4º | 6º | 5º | 7º | 8º (Terraform) | 9º (Actions) | — (não aparece) | — |
| roadmap.sh **how-to-become** | 2º | 4º | 3º | 5º | 6º | (recomenda após cloud) | 7º | — | — |
| roadmap.sh **detalhado (grafo)** | 2º-3º (topo) | 6º | 4º-5º | 6º (Containers) | 7º | 8º (Provisioning) | 9º | 11º (Container Orchestration) | 10º |
| milanm | 3º | 4º | 1º | 6º | **11º** | 8º | 9º | 7º | 10º |
| AWS Builder 2026 | **1º** | 2º | 3º | 5º | 4º | 8º | 6º | 7º | 9º |

Pontos fortes [roadmap.sh]:
- O grafo detalhado traz a legenda **"Order in Roadmap not Strict - Learn anytime"** — ou seja,
  **o autor avisa que a ordem do grafo não é rígida**; o que é rígido de fato é a sequência da
  versão iniciante.
- O grafo começa em **linguagem → SO/Linux → terminal → VCS** e só chega em **orquestração,
  service mesh e cloud design patterns no terço final** (y 2475-2965 de 3379).
- O roadmap Docker declara **"Linux Fundamentals" como pré-requisito**; o roadmap Linux termina em
  **Containerization → Docker/DevOps**. É a "regra" explícita: **Linux → Docker → Kubernetes**.
- [minha inferência] A única discordância entre as fontes é a posição do **cloud**: roadmap.sh
  iniciante e AWS colocam cloud antes de CI/CD/K8s; milanm deixa cloud quase por último (11º).
  Para quem **não quer gastar com cloud agora**, a ordem do milanm é a que melhor encaixa no lab.

---

## 9. Certificações (CCNA, RHCSA, AWS) para quem é júnior

**O que os próprios roadmaps dizem [roadmap.sh]:**
- `/devops/how-to-become-devops-engineer`: "certifications like **Docker Certified Associate** and
  **Certified Kubernetes Administrator** can help you prove your knowledge" (passo containerização) e
  "Becoming an **AWS certified** DevOps engineer helps you gain credibility for your DevOps skills"
  (passo cloud).
- `/devops/career-path`: "One of the popular DevOps certifications is the **AWS Certified DevOps
  Engineer**"; menciona também graduação.
- `/network-engineer` tem caixa **"Certifications": CompTIA Network+, CCNA, CompTIA Security+,
  Cloud Certifications** — CCNA aparece no roadmap de **rede**, não no de DevOps.
- `/linux`: **nenhuma certificação citada** (RHCSA/LPIC não aparecem) — [minha inferência:
  RHCSA não é exigência dos roadmaps].
- Nenhum dos 4 grafos que li marca nó algum como "certificação obrigatória" — [minha inferência:
  roadmap.sh trata cert como *prova de conhecimento*, não como etapa do caminho].

**Opinião da comunidade (busca web; não é posição oficial de roadmap) — usar como termômetro:**
- RHCSA: "RHCSA is an incredible base for devops that has served me very well. I wouldn't waste my
  time going for RHCE though." — https://www.reddit.com/r/devops/comments/1ofalho/is_rhce_enough_for_jr_devops/
- RHCSA júnior: "IMO yes. The knowledge is really useful. **The cert itself, less so**, but what you
  learn during it will serve you throughout your career." — https://www.reddit.com/r/redhat/comments/1qlwf0g/is_it_worth_it_to_pass_the_rhcsa_exam_as_a_junior/
- CCNA vs DevOps: "I did CCNA. If you are into DevOps it's going to be more or less useless...
  As long as you understand the basics of subnetting, static routing, DNS, firewalls, and ports,
  you'll be fine for cloud work." — https://www.reddit.com/r/devops/comments/11rqi7a/ccna_or_network_for_devopscloud/
  e https://www.reddit.com/r/devops/comments/1raogv8/
- Nuvem: "maybe try and get **AWS Practitioner or AZ-104**" — https://www.reddit.com/r/devops/comments/1hwkifz/
  e "cloud certs tend to have the most staying power, especially AWS, GCP, or Azure" —
  https://www.reddit.com/r/devops/comments/1q7hz9b/

**Síntese [minha inferência]:** para o nosso perfil (júnior, lab de VPS, backend Java/Spring), a
ordem de preferência é **(1)** projeto/portfólio > **(2)** cert de nuvem barato (AWS Cloud Practitioner
ou AZ-104) quando o estágio de cloud chegar > **(3)** RHCSA só se quiser aprofundar Linux de forma
estruturada > **(4)** CCNA não compensa para DevOps (conteúdo de redes sim, cert não). Nenhum roadmap
oficial exige cert para a primeira vaga.

---

## 10. O que isso vira no nosso lab (base do plano) [minha inferência]

| Etapa do nosso lab | Nós do roadmap cobertos | Classe |
|---|---|---|
| VM/VPS Ubuntu + SSH + users + systemd + APT + discos | Operating System, Terminal Knowledge, Service Management, Disks, Package Management | (a) |
| Backend Java 17 + Spring Boot servido por trás do Nginx (reverse proxy, TLS, rate limit) | Networking & Protocols, "What is and how to setup X" (Nginx, reverse proxy, SSL/TLS) | (a) |
| Dockerizar o Spring Boot + compose + volume | Containers / Docker | (a) |
| Git + GitHub (repo do lab) | Version Control Systems, VCS Hosting | (a)/(b) |
| CI/CD (GitHub Actions free, ou Jenkins na VPS) → deploy na VPS via SSH/Ansible | CI/CD Tools, Configuration Management | (b) |
| Logs (Loki/OpenSearch) + métricas (Prometheus/Grafana) + healthchecks | Logs Management, Infrastructure Monitoring, Observability | (a) |
| Registry local (Nexus/Artifactory OSS) | Artifact Management | (a) |
| Depois: k3s/Kubernetes multi-node, ArgoCD, Vault | Container Orchestration, GitOps, Secret Management | (b) |
| Estágio futuro: AWS + Terraform real + DevSecOps (ex.: OWASP NodeGoat) | Cloud Providers, Serverless, Provisioning, DevSecOps roadmap (https://roadmap.sh/devsecops) | (c) |

---

## Resumo (10 linhas)

1. O **roadmap.sh/devops está atualizado** (grafo editado em **2026-07-29**, página "in 2026", changelog
   de 17 Set 2026) e tem **171 nós / 60 arestas**; não existe marcador "fundamental/avançado": a
   distinção vem da **versão iniciante** (`/devops-beginner`) e da **legenda de cores** (roxo =
   recomendação pessoal, verde = alternativa, cinza = "ordem não estrita, aprenda quando quiser").
2. **Ordem dos nós do grafo detalhado:** Learn a Programming Language → Operating System → Terminal
   Knowledge → Version Control Systems → (VCS Hosting ∥ Containers ∥ What is/how to setup X: proxies,
   LB, firewall, web servers) → Networking & Protocols (+ e-mail) → Cloud Providers ∥ Serverless →
   Configuration Management ∥ Provisioning → CI/CD Tools ∥ Logs Management → Infrastructure
   Monitoring ∥ Secret Management → Container Orchestration ∥ Observability ∥ Artifact Management ∥
   GitOps → Service Mesh → Cloud Design Patterns.
3. **Ordem do `/devops-beginner` (literal):** Linguagem → **Linux/Ubuntu** → **Redes (DNS, TCP/IP,
   SSH, ports, subnetting)** → **Docker** → **Git/GitHub** → **AWS** → **Terraform** → **Ansible** →
   **GitHub Actions** → **Nginx** → "já dá para vagas junior/mid".
4. **Classificação do lab (minha inferência):** 11 dos 23 grupos são **(a)** 100% sem cloud (linguagem,
   SO, terminal, git, Docker, Nginx/proxies/redes, logs, monitoramento, artefatos, Vault); 7 são
   **(b)** (GitHub, CI grátis, Ansible multi-host, Terraform local, cluster K8s, GitOps, service mesh);
   5 são **(c)** (Cloud Providers, Serverless, SaaS de logs/monitoring, IaC em nuvem, managed K8s).
5. **Concluso-chave para o plano:** **toda a coluna crítica (Linux → redes → Docker → Nginx → CI/CD →
   observabilidade) cabe numa VPS/Ubuntu sem cloud nenhuma**; só o nó "AWS/Cloud Providers" exige
   pagar — e ele já está reservado para o estágio futuro.
6. **Sobre ordem:** todas as fontes colocam **Linux e redes antes de Kubernetes**; o roadmap Docker
   declara "Linux Fundamentals" como pré-requisito e o roadmap Linux termina em Containerization →
   Docker/DevOps. Único ponto de divergência: **milanm deixa cloud em 11º** (último) — o mais
   compatível com o nosso lab sem cloud.
7. **Roadmaps irmãos:** `/linux` (16 tópicos, systemd/disks/Networking/Shell Programming/Troubleshooting/Containerization),
   `/docker` (Introduction → tecnologias subjacentes → CLI → segurança → networking → Deploying:
   Swarm/K8s), `/network-engineer` (traz caixa "Certifications": Network+, **CCNA**, Security+, cloud).
8. **Fontes complementares:** **honeypot.io inacessível** (timeout, não indexado) — usados no lugar
   **github.com/milanm/DevOps-Roadmap** (Git → linguagem → Linux → redes → servers → containers →
   orquestração → IaC → CI/CD → monitoring → cloud → DevSecOps) e o artigo **AWS Builder 2026**
   (Linux → redes → git → AWS → Docker → CI/CD → K8s → IaC → monitoring → DevSecOps).
9. **Certificações:** roadmap.sh só cita **DCA/CKA e AWS** como "prova de conhecimento"; **CCNA não
   aparece no roadmap de DevOps** e a comunidade a considera pouco útil para DevOps; **RHCSA é
   elogiada como base mas o certificado pesa menos que o conhecimento**; para júnior, projeto/prática
   > cert de nuvem barato.
10. **Próximo passo sugerido:** tratar a sequência do `/devops-beginner` (passos 1-6 e 8-11) como o
    **trilho obrigatório do lab**, usando o grafo detalhado como **catálogo de nós "roxo"** para o
    que vem depois (Vault, ArgoCD, Prometheus/Grafana), e o `/devsecops` + cloud só no estágio futuro.
