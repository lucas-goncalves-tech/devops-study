---
title: "Pesquisa 01 — Vagas: o que o mercado exige para DevOps/Infra júnior"
tags: [pesquisa, vagas]
data: 2026-10-07
---

# Pesquisa 01 — Vagas de DevOps / Infraestrutura Júnior

**Objetivo:** levantar o que vagas reais (Brasil + remoto internacional) exigem de candidatos júnior, para desenhar o caminho de estudos do laboratório VPS (`VPS/` — Ubuntu Server + NoteMaster Java 17/Spring Boot em Docker/compose).

**Como ler este documento:** todo item está marcado como
`[FATO]` = lido diretamente na fonte citada (URL), ou
`[INFERÊNCIA]` = minha conclusão a partir dos fatos (não é texto de vaga).

---

## 0. Metodologia e limitações (ler antes de usar os números)

- **Acessos bloqueados (paywall/login/bot):** Indeed (`403`), startup.jobs (`403`), Glassdoor (não aberto diretamente), Catho (não acessado), Reddit (sem retorno). Onde usei esses sites, o dado veio de **trecho de resultado de busca** e está marcado como tal.
- **Acessos OK:** páginas públicas de vaga do LinkedIn (`br.linkedin.com/jobs/view/...` sem login), Vagas.com, ProgramaThor, Dexian, DevVagas, Robert Half, RemoteRocketship, artigo de análise de mercado (Sebastian Marines).
- **Amostra pequena:** 6 vagas júnior brasileiras lidas na íntegra + 1 internacional (Emerging Travel) + agregadores. Vaga por vaga serve para entender *formato de requisito*; para frequência estatística eu uso o dataset de 8.579 vagas do Sebastian Marines.
- **Não inventei vagas nem salários.** Onde não havia número publicado, escrevo **"não encontrado"**. As vagas do LinkedIn têm data de publicação relativa ("Há 1 semana"), ou seja, todas da amostra são de set/out 2026.

---

## 1. Amostra — vagas JÚNIOR no Brasil (texto integral lido)

### 1.1 Dexian — "Analista de Infraestrutura Linux Junior" (na prática: Engenheiro DevOps/SRE Júnior)
- **URL:** https://brasil.dexian.com/en/jobs/analista-de-infraestrutura-linux-junior
- **Modalidade:** Remoto / híbrido em SP (2x/mês no capital) · **Salário:** não encontrado
- `[FATO]` O próprio texto diz: *"Estamos em busca de um(a) Engenheiro(a) DevOps/SRE Júnior"*.
- `[FATO]` **Dia a dia:** Kubernetes/AKS, deploy em containers, Linux (SO, redes, VPN, segurança), monitoramento com Prometheus/Grafana, pipelines GitLab/GitHub Actions/Jenkins, GitOps com ArgoCD, documentação técnica.
- `[FATO]` **Requisitos:** "experiência prática em atividades relacionadas a DevOps, SRE, infraestrutura ou cloud"; orquestradores (Kubernetes, Docker Swarm); **"vivência em pelo menos uma nuvem pública (AWS, GCP ou, preferencialmente, Azure)"**; princípios DevOps/SRE; monitoramento (Prometheus/Datadog/New Relic); CI/CD; noções de GitOps/ArgoCD; Linux (máquinas virtuais **e** containers); Git; comunicação.
- `[FATO]` **Diferenciais:** Azure, Kubernetes, Grafana, Prometheus, Docker, doc técnica.
- `[FATO]` "atue com acompanhamento de profissionais mais experientes" = júnior com mentoria, não autônomo.

### 1.2 BSN — "🚀 Vaga: DevOps Júnior / Infraestrutura e CI/CD"
- **URL:** https://br.linkedin.com/jobs/view/%F0%9F%9A%80-vaga-devops-j%C3%BAnior-infraestrutura-e-ci-cd-at-bsn-4355285029
- **Contrato:** CLT · **100% remoto** · **Salário:** "A combinar" (não encontrado)
- `[FATO]` **Requisitos exigidos:** "Experiência prática inicial (**1 a 2 anos**) com servidores Linux (Ubuntu/Debian/CentOS)"; noções de Windows Server; "conhecimento básico em Git e pipelines de CI/CD"; "experiência inicial com Docker e containers"; **"noções de cloud computing (AWS, Azure ou GCP)"**; "conhecimentos básicos em redes (DNS, HTTP/HTTPS, firewall)"; "inglês técnico para leitura de documentação".
- `[FATO]` **Diferenciais:** GitLab CI/GitHub Actions, **noções de Kubernetes**, monitoramento (Zabbix, Grafana, Prometheus), bancos em produção, segurança/SRE.
- `[FATO]` **Atribuições:** administração/monitoramento de servidores (Linux **e** Windows), CI/CD, deploys em homologação/produção, verificações de segurança/backup/controle de acessos, automação, documentação.

### 1.3 XP Inc. — "Analista Júnior em Infraestrutura | SRE"
- **URL:** https://br.linkedin.com/jobs/view/analista-j%C3%BAnior-em-infraestrutura-sre-at-xp-inc-4471691207
- **Local:** São Paulo (modelo com mais frequência presencial) · **+200 candidaturas** · **Salário:** não encontrado
- `[FATO]` **Como descrevem "nível júnior":** *"não esperamos autonomia total — buscamos alguém com disciplina operacional, vontade de aprender e capacidade de seguir procedimentos com rigor… monitorar dashboards, atender incidentes N1, executar mudanças padronizadas"*.
- `[FATO]` **Pré-requisitos:** ensino superior cursando/completo (CC/EngComp/SI); **"1 a 3 anos de experiência em infraestrutura de TI, operação de servidores, suporte técnico ou administração de sistemas"**; administração básica de **Linux (RHEL/CentOS)** — arquivos, processos, permissões, systemd, logs, pacotes; **Windows Server** (AD, GPO, IIS, Event Viewer, RDP); **conceitos de Kubernetes** (pods, services, deployments, namespaces, kubectl); **redes TCP/IP** (OSI, sub-redes, roteamento, NAT, portas, sockets); Git (commits, branches, PRs); **shell script Bash**; leitura de dashboards (CPU, memória, latência).
- `[FATO]` **Diferenciais:** Kafka/RabbitMQ teóricos, **Docker**, **noções de cloud (AWS, Azure ou GCP)**, firewall (iptables, security groups), logging (ELK, Loki), **certificações LPIC-1, RHCSA, AWS Cloud Practitioner ou CKAD**, plantão em escala.
- `[FATO]` **Stack do dia a dia:** Kubernetes, Kafka, RabbitMQ, Linux RHEL, dashboards **Dynatrace, Grafana, Prometheus**; automatizar ao menos 1 tarefa repetitiva/trimestre.

### 1.4 Semantix — "Analista de Infraestrutura Júnior | Windows/Linux/Cloud"
- **URL:** https://br.linkedin.com/jobs/view/analista-de-infraestrutura-j%C3%BAnior-windows-linux-cloud-at-semantix-4468042006
- **Local:** Londrina/PR, **híbrido**, turno **noturno 23h–07h (ter–sáb)** · 98 candidaturas · **Salário:** não encontrado
- `[FATO]` **Requisitos:** "conhecimentos básicos em **Windows Server e Linux**"; "conhecimentos básicos em ambientes virtualizados, especialmente **VMware**"; **"noções de ambientes Cloud (Azure, AWS ou GCP)"**; AD e GPO; WSUS; "serviços de rede como **DNS, DHCP e DFS**"; File/Print Server; segurança da informação; gestão de ativos de TI; **"ensino superior cursando ou completo"**.
- `[FATO]` **Diferenciais:** certificações/cursos em Windows, Linux ou Cloud; **PowerShell ou Shell Script**; ferramentas de monitoramento.
- `[FATO]` Atribuições: patches, mudanças em produção sob supervisão, monitoramento, SLA, atendimento a clientes. **Não aparecem:** Docker, CI/CD, Kubernetes, IaC.

### 1.5 Jobbol (para Tailorit) — "Analista DevOps Júnior"
- **URL:** https://br.linkedin.com/jobs/view/analista-devops-j%C3%BAnior-at-jobbol-4475860585
- **Local:** São Paulo (Zona Sul), **presencial**, efetivo · 108 candidaturas · **Salário:** "faixa será informada na entrevista" (não encontrado)
- `[FATO]` **Requisitos:** "Ensino superior completo"; **"Experiência: Necessário experiência"** (sem número de anos).
- `[FATO]` Atividades: "automação de rotinas de infraestrutura e implantação de aplicações", acompanhar ambientes dev/prod, tratar falhas operacionais, melhoria contínua de processos de entrega.
- `[FATO]` Afirma que afinidade vale com: Engenheiro de DevOps Júnior, Analista de Infraestrutura Júnior, Desenvolvedor de Sistemas Júnior ou **Administrador de Sistemas Júnior**.

### 1.6 Vagas.com — "Analista de Infraestrutura de TI" (página de cargo + listagem)
- **URL cargo:** https://www.vagas.com.br/vagas-de-analista-de-infraestrutura-de-ti
- `[FATO]` **Salário mediano no Brasil: R$ 3.770,00**; formação mais frequente: graduação em Informática; próximo cargo mais comum: Analista de Redes.
- `[FATO]` Na listagem (15 vagas): nível **Pleno 8, Júnior/Trainee 4, Auxiliar/Operacional 2, Técnico 1**; regime **CLT 13 / PJ 2**; apenas 1 é 100% home office.
- `[FATO]` Exemplo júnior da listagem — InterCement, "Analista Infraestrutura TI Jr" (SP): filas de atendimento, ITSM, controles e inventário de ativos, "noções gerais de infraestrutura e suporte ao usuário" (https://www.vagas.com.br/vagas/v2827356/analista-infraestrutura-ti-jr-sao-paulo-sp). Sem Docker/CI/CD no trecho visível.
- `[FATO]` Página **"7 vagas de emprego para devops"**: níveis **Sênior 4 e Pleno 3 — nenhuma júnior** (https://www.vagas.com.br/vagas-de-devops).

### 1.7 ProgramaThor — vagas DevOps
- **URL:** https://programathor.com.br/jobs-devops (página 1 de 29)
- `[FATO]` Nas 15 vagas da primeira página, **nenhuma é DevOps júnior** — predominam Pleno/Sênior (ex.: "Senior DevOps Engineer", "SRE Team Lead", "Analista de Cloud Sênior", "SRE Sênior | Datadog"). Há filtro "Nível de experiência → Júnior".
- `[FATO]` Vagas DevOps do Brasil ali citam tags como: AWS EC2/RDS, Azure, GCP, Kubernetes, Docker, Linux, Jenkins, Bash, Python, Terraform.
- `[FATO]` Faixas exibidas (exemplos): SRE Sênior até R$ 12.000; Analista de Cloud Sênior até R$ 15.000; Júnior com tag "Acima de R$ 18.000" num cargo mal etiquetado (Sr Developer BI) — **rótulo inconsistente, não usar como referência**.

---

## 2. Amostra — vagas remotas internacionais / remote-first

### 2.1 Emerging Travel Group — "Junior DevOps Engineer" (empresa internacional, contratando no Brasil)
- **URL:** https://br.linkedin.com/jobs/view/junior-devops-engineer-at-emerging-travel-group-4456334641
- **Local:** Montenegro/RS · Nível: Júnior · 28 candidaturas · **Salário:** não encontrado
- `[FATO]` **Requirements:** "**1.5+ years of commercial DevOps experience**"; "Experience working with Linux"; "Knowledge of **Ansible**"; "Knowledge of **Docker**"; "Experience with **Kubernetes (6+ months)**"; "**Conversational English level B1 (Intermediate) or higher**".
- `[FATO]` **Nice to have:** PostgreSQL operacional, "bare-metal servers", **cloud platforms (AWS, OVH, Cloudflare)**.
- `[FATO]` Benefits incluem "flexible schedules and opportunity to work remotely" e escola corporativa de inglês.

### 2.2 Experian — "Junior Site Reliability Engineer (Remote)" (EUA)
- **URL:** https://jobs.experian.com/job/junior-site-reliability-engineer-remote-in-united-states-jid-1883
- `[FATO]` Faixa salarial exibida: **$80.237 – $139.077/ano** (moeda do cartão = USD, localização "United States"). **A vaga consta como expirada** na própria página ("This vacancy has now expired").
- `[FATO]` "Bonus experience": **AWS e/ou GCP**, linguagens JVM (Java/Scala), **IaC (Terraform, CloudFormation, Ansible ou Chef)**, **Kubernetes**, Go/Python.
- ⚠️ A mesma página mistura outros cartões (um deles Serasa Experian híbrido em São Paulo); não confundir os requisitos entre cartões.

### 2.3 Glassdoor — vagas remotas "Junior DevOps Engineer" (EUA)
- **URL:** https://www.glassdoor.com/Job/remote-junior-devops-engineer-jobs-SRCH_IL.0,6_IS11047_KO7,29.htm
- `[FATO — trecho de busca, página não aberta diretamente]` **503 vagas abertas** "Junior devops engineer in Remote", com faixa **$91K – $118K** e outra de **$40,00 – $60,00/hora**.
- `[FATO — trecho de busca]` Página Brasil: **130 vagas "devops junior"** (https://www.glassdoor.com.br/Vaga/devops-junior-vagas-SRCH_KO0,13.htm) e **12 vagas "Junior devops engineer"** (https://www.glassdoor.com.br/Vaga/junior-devops-engineer-vagas-SRCH_KO0,22.htm).

### 2.4 Indeed (remoto, EUA) — "remote jr devops"
- **URL:** https://www.indeed.com/q-remote-jr-devops-jobs.html (acesso direto: **403**; trecho de busca)
- `[FATO — trecho]` "Relevant Exp: **0 - 2 Years**"; requisitos de um anúncio: *"Strong expertise in **Linux-based systems and shell scripting**"*, *"Proficiency with **Docker, Kubernetes, and Helm** for container orchestration"*.
- `[FATO — trecho]` Outro anúncio (Think Future Technologies, work-from-anywhere): "DevOps Experienced - course/competitions/internships/job (**<2 anos**)", MUST HAVE inclui "Implemented major managed services in **AWS or Azure or GCP**", "containerization Python/JavaScript", "Implemented **CI/CD pipelines at scale**", "container orchestration".
- `[FATO — trecho]` Vaga de infra ("0-2 Years") exige Linux + shell + Docker/K8s/Helm.

### 2.5 Leidos — "Junior DevOps Engineer" (não remoto, EUA)
- **URL:** https://careers.leidos.com/jobs/17806504-junior-devops-engineer
- `[FATO — trecho]` "Minimum Requirements: **US Citizenship** … **1–3 years of experience** in DevOps, related field; **Familiarity with Linux system administration**; **Experience with Docker**". Híbrido presencial (Columbia, MD).

### 2.6 LATAMHire (staffing LATAM para empresas dos EUA) — perfil "Junior DevOps Engineer"
- **URL:** https://lathire.com/hire-latam-devops-engineers
- `[FATO]` Requisitos publicados: "**1-3 years of DevOps experience**", "Familiar with **Jenkins and GitLab CI/CD** (or similar)", "**Proficient in AWS Azure and GCP**", "Proficient in **Git and GitHub**"; e "**2 or more years in a broader tech background**" (contrata-se júnior em DevOps, mas com base técnica anterior). **Starting at $3.0k** (unidade não especificada na página; presumivelmente USD/mês — `[INFERÊNCIA]`).

### 2.7 RemoteRocketship — agregador de vagas remotas
- **URL (EUA, junior):** https://www.remoterocketship.com/us/jobs/junior-devops
- `[FATO]` Tabela salarial própria: **Entry-level DevOps (0 anos): $94.326** (15 vagas); **Junior DevOps (1-2 anos): $111.334** (172 vagas); Mid (2-4): $136.610; Senior (5-9): $160.941; Lead (10+): $189.580.
- **URL (LATAM junior devops):** https://www.remoterocketship.com/country/latin-america/jobs/junior-devops
- `[FATO]` Apenas **8 vagas** "junior devops" em toda a América Latina; várias marcadas "No degree required", "Portuguese Required" ou "French Required", com "ghost score" (risco de vaga falsa) de 11% a 51%.
- **URL (LATAM SRE):** https://www.remoterocketship.com/country/latin-america/jobs/site-reliability-engineer
- `[FATO]` Junior SRE LATAM: **5 vagas amostradas, salário médio $0** (sem dados publicados). Mid-level LATAM: $55.000/ano.

### 2.8 Dataset de mercado LATAM (maior amostra desta pesquisa)
- **URL:** https://sebastianmarines.com/post/state-of-cloud-devops-hiring-latam-2026 (publicado 14/04/2026; coleta nov/2025–abr/2026; fontes: Glassdoor, Workable, LinkedIn, Greenhouse)
- `[FATO]` **8.579 vagas** cloud/DevOps em 14 países; Brasil = **3.296 vagas (38,4%)**.
- `[FATO]` **Júnior = só 2,2% do mercado** (191 de 8.579). Pleno 61,0%, sênior 26,3%.
- `[FATO]` Experiência mínima média (3.050 vagas que informam): **4,6 anos**; apenas **12,5%** pedem 2 anos ou menos.
- `[FATO]` Formação: de 2.299 vagas que citam educação, **67,8% pedem graduação**, 15,2% dizem "não precisa de diploma/experiência equivalente"; **júnior pede diploma com mais frequência (33,5%) que pleno (20,8%) e sênior (19,3%)**.
- `[FATO]` Idioma: **70,6% das vagas em inglês**, 21,3% em português; de 1.643 que citam nível, **79,1% exigem inglês avançado/fluente**; só 0,9% aceitam básico.
- `[FATO]` Trabalho: **52,9% remoto, 27,2% híbrido, 19,9% presencial**; mas **só 39,6% das vagas júnior são remotas** vs. 61,0% das sênior.
- `[FATO]` Cloud: AWS 4.640 menções, Azure 3.812, GCP 2.380; **em vagas júnior Azure lidera (45,5%) sobre AWS (39,3%)**; no Brasil há liderança clara da AWS.
- `[FATO]` Ferramentas: **Terraform 2.222** (3x Ansible 706, 4x CloudFormation 516); CI/CD: **GitHub Actions 945 / Jenkins 918 / GitLab CI 410 / ArgoCD 203**; monitoramento: **Grafana 816 / Prometheus 714** (618 citam as duas) / Datadog 519 / CloudWatch 384; containers: **Kubernetes 2.813 (32,8%)** e Docker 2.497 (1.757 citam os dois); **Kubernetes aparece em 19,4% das vagas júnior** (vs. 49% no nível staff).
- `[FATO]` Linguagens: **Python 3.707**, JS/TS 1.404, **Java 1.309**, Go 908, **Bash/Shell 333**.
- `[FATO]` Certificações citadas em apenas **6,6%** das vagas; top: Terraform Associate (68), AZ-104 (65), AWS SAA (60), CKA (60).

---

## 3. Salários (o que foi encontrado)

| Fonte | Escopo | Valor | URL |
|---|---|---|---|
| Robert Half, Guia Salarial | **Analista de Infraestrutura Júnior (BR)** | R$ 4.400 (25º) / **R$ 5.600 (mediana)** / R$ 7.450 (75º) | https://www.roberthalf.com/br/pt/vagas-detalhes/analista-de-infraestrutura-junior |
| Robert Half | Analista de Suporte Júnior | R$ 3.350 / 4.400 / 5.650 | mesma URL (tabela de cargos relacionados) |
| Robert Half | Analista de Infraestrutura Pleno / Sênior / Especialista de Cloud | 6.400–10.600 / 9.400–15.750 / 11.850–19.100 | mesma URL |
| Vagas.com | Analista de Infraestrutura de TI (mediana nacional) | **R$ 3.770** | https://www.vagas.com.br/vagas-de-analista-de-infraestrutura-de-ti |
| Glassdoor (via BeTrybe) | Analista de Infraestrutura **júnior** (BR) | mediana **R$ 3.647** (sênior R$ 8.343) | https://www.betrybe.com/guia-salarios-profissoes/analista-de-infraestrutura |
| DevVagas (texto editorial do site) | DevOps/SRE Júnior | **R$ 5.000 – 10.000/mês** | https://www.devvagas.com.br/vagas/devops |
| Glassdoor (trecho de busca) | "Analista de Infraestrutura DevOps" SP | R$ 57.033/ano | https://www.glassdoor.com/Salaries/sao-paulo-brazil-analista-de-infraestrutura-junior-devops-salary-SRCH_IL.0,16_IM1009_KO17,57.htm |
| Glassdoor (trecho de busca) | Vaga "Analista de Cloud, Infraestrutura ou DevOps Júnior" (BR) | **R$ 4K – R$ 6K/mês** | https://www.glassdoor.com/Salaries/brazil-engenheiro-de-devops-j%C3%BAnior-salary-SRCH_IL.0,6_IN36_KO7,34.htm |
| Experian (vaga expirada) | Junior SRE remoto (EUA) | **$80.237 – $139.077/ano** | https://jobs.experian.com/job/junior-site-reliability-engineer-remote-in-united-states-jid-1883 |
| Glassdoor (trecho de busca) | Junior DevOps remoto (EUA) | $91K – $118K (e $40–60/h) | https://www.glassdoor.com/Job/remote-junior-devops-engineer-jobs-SRCH_IL.0,6_IS11047_KO7,29.htm |
| RemoteRocketship (agregador) | Junior DevOps remoto (EUA) | $111.334 média (172 vagas) | https://www.remoterocketship.com/us/jobs/junior-devops |
| LATAM dataset (166 vagas com salário) | LATAM júnior | **$16K–$28K/ano** (mid $27–40K; sênior $62–87K) | https://sebastianmarines.com/post/state-of-cloud-devops-hiring-latam-2026 |

`[FATO]` Nas 6 vagas brasileiras lidas na íntegra: **nenhuma publicou salário** (4 × "não encontrado/a combinar", 1 × "informado na entrevista", 1 × sem menção).
`[FATO]` Guia Salarial 2026 Robert Half (tecnologia): https://www.roberthalf.com/br/pt/insights/guia-salarial/tecnologia — a tabela exposta no site traz cargos de gestão (ex.: Gerente de Infraestrutura mediana R$ 21.700); o valor de "DevOps mediana R$ 18.500" circula em postagem de Instagram citando o guia (https://www.instagram.com/reel/DPU9AnBDkws/) e **não foi confirmado na página oficial — tratar como não verificado**.

---

## 4. Síntese

### (a) Requisitos que aparecem em TODAS ou quase todas as vagas

`[FATO — presente em todas as 7 vagas júnior lidas na íntegra]`
1. **Linux** — sempre, com nível variado: "básico" (Semantix), "administração básica: arquivos, processos, permissões, systemd, logs, pacotes" (XP), Ubuntu/Debian/CentOS (BSN), "experience working with Linux" (Emerging Travel).
2. **Experiência prática prévia exigida mesmo para júnior** — BSN "1 a 2 anos", XP "1 a 3 anos", Emerging Travel "1.5+ anos comerciais", Jobbol "Necessário experiência", Dexian "experiência prática em DevOps/SRE/infra/cloud". Nenhuma das 7 diz "zero experiência".
3. **Contêineres/Docker** — BSN, XP (diferencial), Dexian, Emerging Travel, Leidos, Indeed. Única exceção: Semantix (vaga de infraestrutura tradicional, sem containers).
4. **Git + algum CI/CD** — BSN, Dexian, XP (Git/PRs), Jobbol (implantação/entrega), Emerging Travel (build/delivery), LATAMHire (Jenkins/GitLab), Indeed (pipelines).
5. **Redes de base** — DNS, TCP/IP, HTTP/HTTPS, firewall (BSN, XP, Semantix, latentes em Dexian).
6. **Noções de cloud** — em todas: "noções" (BSN, XP, Semantix), "vivência em pelo menos uma nuvem" (Dexian), "nice to have: cloud" (Emerging Travel), "proficient in AWS/Azure/GCP" (LATAMHire).
7. **Monitoramento/leitura de métricas** — XP (Dynatrace/Grafana/Prometheus), Dexian (Prometheus/Grafana/Datadog), BSN (diferencial: Zabbix/Grafana/Prometheus), Semantix (diferencial).
8. **Documentação e runbooks** — XP, Dexian, BSN.
9. **Soft skills** — "vontade de aprender", comunicação, organização, trabalho em equipe, "disciplina operacional" (todas as BR).

`[INFERÊNCIA]` O "pacote mínimo" que abre porta júnior é: **Linux + Docker + Git/CI/CD básico + redes básicas + noções de cloud + monitoramento básico + documentação**. É o que sustenta o lab de VPS — mas ele sozinho ainda não fecha os itens 2, 7 e 9.

### (b) Requisitos que SEPARAM vaga BR de vaga remoto internacional

| Dimensão | Vaga BR (amostra) | Vaga remoto internacional (amostra) |
|---|---|---|
| `[FATO]` Cargo | "Analista…" (ver item d) | "…Engineer" |
| `[FATO]` Windows/AD/GPO/VMware | Presente em **4 de 5** (BSN, XP, Semantix; ausente em Dexian e Jobbol) | Praticamente ausente (Linux-only) |
| `[FATO]` IaC (Terraform/Ansible) | Raramente como requisito de júnior (Dexian cita ArgoCD/GitOps; BSN/XP/Semantix não citam) | **Emerging Travel exige Ansible**; Experian lista Terraform/CloudFormation/Ansible; dataset LATAM: Terraform é a ferramenta #1 (2.222 menções) |
| `[FATO]` Kubernetes | Diferencial no Brasil júnior (BSN "noções", XP "conceitos", Dexian "experiência prática") | **Exigido**: Emerging Travel "6+ meses"; dataset: 19,4% das vagas júnior já citam K8s |
| `[FATO]` Inglês | "Inglês técnico **para leitura** de documentação" (BSN); não citado nas demais BR | **B1 conversacional obrigatório** (Emerging Travel); 79,1% das vagas LATAM que citam nível exigem avançado; 70,6% das vagas em inglês |
| `[FATO]` Graduação | Exigida em **todas as BR** que citam (XP, Semantix, Jobbol) | Emerging Travel não cita; dataset: 15,2% das vagas dizem "não precisa de diploma" |
| `[FATO]` Trabalho | Predomínio híbrido/presencial; vagas "100% remoto" existem mas são minoria (1 de 15 no filtro de Vagas.com) | Remoto é padrão; porém **só 39,6% das vagas júnior são remotas** |
| `[FATO]` Salário | Raramente publicado (4/6 vagas sem valor) | Frequente em USD e publicado |
| `[FATO]` Operação | Plantão, escala 12x36/noturno, atendimento N1, ITIL/suporte ao usuário (XP, Semantix) | Menos enfatizado no texto júnior, mas "participate in real incidents" (Emerging Travel) |
| `[FATO]` Certificações | Diferencial BR: LPIC-1, RHCSA, AWS Cloud Practitioner, CKAD (XP), certificações Windows/Linux/Cloud (Semantix) | Diferencial também (Terraform Associate, AZ-104, AWS SAA, CKA), citadas em só 6,6% das vagas |

`[INFERÊNCIA]` Existe um eixo duplo no Brasil: **(1) trilha "Analista de Infraestrutura/Cloud"** — Windows, VMware, AD/GPO, atendimento, presencial, graduação; e **(2) trilha "DevOps/SRE"** — Linux, containers, CI/CD, GitOps, monitoramento, remoto. O lab de VPS alimenta a (2); a (1) é onde está a maioria das vagas júnior hoje.

### (c) O que exigem de cloud até para júnior

- `[FATO]` **BR:** em vagas de DevOps/SRE júnior, cloud está quase sempre como **"noções" / "diferencial"** (BSN, XP, Semantix), **exceto** a Dexian, que já exige "**vivência em pelo menos uma nuvem pública (AWS, GCP ou, preferencialmente, Azure)**" e "experiência prática em atividades relacionadas a cloud".
- `[FATO]` **BR:** vagas "Analista de Infra Júnior" de empresa tradicional citam cloud apenas como noções (Semantix) ou nem citam (InterCement, no trecho visível).
- `[FATO]` **Internacional:** cloud é praticamente obrigatório: Emerging Travel põe cloud em "nice to have", mas Experian coloca AWS/GCP em "bonus", o anúncio remoto da Indeed exige "Implemented major managed services in AWS/Azure/GCP", LATAMHire exige "proficient in AWS Azure and GCP" desde o júnior.
- `[FATO]` **Qual nuvem:** dataset LATAM mostra **Azure à frente da AWS em vagas júnior (45,5% vs 39,3%)**, enquanto no Brasil a AWS lidera; Docker (2.497) é mais citado que qualquer IaC.
- `[INFERÊNCIA]` Para "Analista de Infra Júnior" clássico, **noções de cloud bastam**; para "DevOps/SRE Júnior", espera-se **já ter mexido em cloud de verdade** (mínimo: criar VM, rede, storage, IAM e subir um serviço). Um lab 100% on-prem (VPS/VM) deixa essa lacuna aberta — mitigável com free tier + simulação, mas precisa ser planejado como etapa posterior.

### (d) Nomes exatos de cargos

**Brasil `[FATO — títulos lidos nas vagas/listagens]`:**
- Analista de Infraestrutura Júnior / Analista de Infraestrutura TI Jr
- Analista de Infraestrutura Júnior | Windows/Linux/Cloud
- Analista DevOps Júnior · DevOps Júnior · "DevOps (Cloud) | Trainee/Junior"
- Analista Júnior em Infraestrutura | SRE · Analista SRE JR
- Analista de Cloud Ops Júnior · Analista de Observabilidade Júnior
- Analista de Monitoramento Júnior (NOC) · Analista de NOC Junior
- Analista de Suporte de TI Jr · Analista de Suporte e Infraestrutura
- Engenheiro de DevOps Júnior (usado por Jobbol como cargo anterior equivalente)
- DevOps Engineer I (Legrand, Blumenau) · Engenheiro(a) DevOps

**Internacional `[FATO — títulos lidos]`:**
- Junior DevOps Engineer
- Junior Site Reliability Engineer / SRE (junior)
- Site Reliability Engineer (entry-level)
- Junior Linux DevOps / Linux Administrator
- Infrastructure Engineer / Platform Engineer (níveis I/júnior)
- DevOps Engineer (remote, "0-2 years" / "entry level")

`[INFERÊNCIA]` No Brasil, "Analista [de Infraestrutura/DeviOps/SRE] Júnior" é o padrão e é assim que se deve buscar (muda o resultado vs. "DevOps Engineer"); a palavra **"Trainee"** e os níveis **"Assistente"/"Auxiliar"** aparecem como sinônimos de entrada no LinkedIn/Vagas.com.

### (e) LACUNAS — o que as vagas cobrem e um lab de VPS (Ubuntu + NoteMaster em Docker/compose) **não** cobre

| # | Lacuna | Evidência `[FATO]` | Impacto |
|---|---|---|---|
| 1 | **Kubernetes / orquestração** | 19,4% das vagas júnior LATAM citam K8s; Dexian exige; Emerging Travel exige 6 meses; XP pede conceitos (pods/services/deployments/kubectl) | Alto — compose.yaml não ensina orquestração |
| 2 | **Infra como Código (Terraform/Ansible)** | Terraform = 2.222 menções (nº 1); Ansible exigido pela Emerging Travel; vagas BR júnior citam pouco, mas plenas citam sempre | Alto para trilha DevOps; médio para trilha "analista" |
| 3 | **Pipeline de CI/CD real (GitHub Actions/GitLab CI/Jenkins)** | BSN, Dexian, Emerging Travel, Indeed, LATAMHire; GitHub Actions 945 / Jenkins 918 | Alto — lab tem Dockerfile, mas não build/test/deploy automatizado em repositório |
| 4 | **Cloud (AWS/Azure/GCP)** | Dexian exige "vivência em nuvem"; Azure lidera em júnior no LATAM; dataset: AWS 4.640 menções | Alto — VPS é on-prem |
| 5 | **Observabilidade: Prometheus + Grafana + logs + alertas** | 618 vagas citam Prometheus **e** Grafana juntas; XP monitora dashboards; Dexian/BNS exigem | Alto — lab provavelmente não instrumenta nada |
| 6 | **Windows Server / AD / GPO / VMware / WSUS** | Presente em 4 das 5 vagas BR de infra/desenvolvimento… (Semantix, BSN, XP) | Alto para a trilha "Analista de Infraestrutura", que é a maioria das vagas júnior |
| 7 | **Rotina operacional: plantão, N1, runbooks, ITIL/atendimento, SLA** | XP: "atender incidentes N1", "plantão em escala", "atualizar runbooks"; Semantix: SLA, atendimento a clientes; Semantix é turno noturno | Médio/Alto — não é técnico, é comportamental; lab não gera isso |
| 8 | **Redes em profundidade + segurança** | XP: OSI, sub-redes, NAT, iptables/security groups; BSN: DNS/HTTP/firewall; Semantix: DNS/DHCP/DFS | Médio — parcialmente coberto se o lab exigir proxy TLS, firewall, DNS interno |
| 9 | **Git em fluxo colaborativo (branches, PRs, code review)** | XP lista commits/branches/PRs como pré-requisito | Médio — vira GitOps depois |
| 10 | **Versionamento da infra e GitOps (ArgoCD)** | Dexian exige noções de ArgoCD; dataset: ArgoCD 203 menções e crescendo | Médio |
| 11 | **Python/Bash para automação** | Python = 3.707 menções; Bash = 333 e "baseline expectation" | Médio — Bash sim; Python raramente vem de lab de deploy |
| 12 | **Certificações (LPIC-1, RHCSA, AZ-104, AWS SAA, Terraform Associate, CKA)** | XP lista LPIC-1/RHCSA/AWS CP/CKAD como diferencial; Terraform Associate é a nº 1 do dataset | Médio — sinal que passa por filtro de currículo |
| 13 | **Inglês técnico (leitura e conversação)** | BSN: leitura; Emerging Travel: B1 conversacional; 79,1% das vagas LATAM exigem avançado | Alto para vaga internacional |
| 14 | **Experiência prévia comprovável (1–3 anos)** | Presente em 100% das amostras com experiência declarada; média LATAM de 4,6 anos de mínimo | **Crítico** — lab não vira "anos de experiência" sozinho |
| 15 | **Graduação/cursando** | Exigida nas vagas BR que citam formação; 33,5% das vagas júnior LATAM pedem diploma | Crítico se não cursar |
| 16 | **Bancos de dados em produção (PostgreSQL etc.)** | BSN (diferencial), Dexian (diferencial), Emerging Travel (nice to have); PostgreSQL = 951 menções | Baixo/médio |

`[INFERÊNCIA]` Prioridade de preenchimento para o lab, considerando frequência × esforço:
**CI/CD pipeline (GitHub Actions) → Prometheus/Grafana + logs → k3s/Kubernetes → Terraform/Ansible → nginx/TLS/firewall/DNS (redes) → noções de cloud em free tier → GitOps**. Windows/AD/VMware e plantão **não** entram no lab: são cobrindo por estágio/primeira vaga em suporte/NOC/infra.
`[INFERÊNCIA]` Como **97,8% das vagas cloud/DevOps LATAM não são júnior** e o mínimo médio é 4,6 anos, a porta de entrada mais realista é: (i) vaga de **Analista de Suporte/Infra Júnior** ou **NOC/monitoramento** (exige Linux, redes, atendimento — menos stack moderna), ou (ii) vaga de **DevOps júnior em consultoria/staffing** (exige mais stack e inglês). O lab de VPS cobre o segundo caminho; o primeiro precisa de argumento de "atendimento/runbook/documentação".

---

## 5. Fatos vs. inferências — resumo de segurança

- **Fatos:** todos os textos de vaga, números de vagas, faixas salariais e percentuais do dataset, com URL citada ao lado.
- **Inferências minhas:** item 4(a) "pacote mínimo", item 4(c) conclusão sobre cloud, item 4(e) tabela de prioridades e a estratégia de porta de entrada.
- **Não verificado:** salário mediano "DevOps R$ 18.500" (apenas em post de Instagram citando guia da Robert Half); a unidade "US$3.0k" do LATAMHire; a consistência dos dados salariais do Glassdoor (a plataforma devolve valores anuais que parecem mensais em alguns cartões — ex.: "R$3.473 **per year**" em https://www.glassdoor.com/Salaries/brazil-analista-infraestrutura-junior-salary-SRCH_IL.0,6_IN36_KO7,37.htm, o que é incoerente; **não usar esses valores**).
- **Não encontrado:** salário publicado em qualquer das 6 vagas brasileiras lidas na íntegra; vagas de "Engenheiro de Plataforma júnior" com texto completo (a maioria é pleno/sênior); vagas júnior na ProgramaThor DevOps (página 1); vagas no Catho.

---

## 6. Resumo — 10 linhas

1. Vaga júnior de DevOps/Infra no Brasil **existe, mas é escassa**: só 2,2% das 8.579 vagas cloud/DevOps da LATAM são júnior e o mínimo médio pedido é 4,6 anos — "júnior" na prática é 1 a 3 anos de experiência.
2. O núcleo que aparece em quase toda vaga: **Linux + Docker/containers + Git/CI/CD básico + redes (DNS/TCP-IP/firewall) + noções de cloud + monitoramento + documentação**.
3. **Windows Server/AD/GPO/VMware** e atendimento/ITIL aparecem em metade das vagas BR de infra — é o divisor entre trilha "Analista de Infraestrutura" (maioria) e trilha "DevOps/SRE".
4. **Kubernetes e IaC** (Terraform nº 1 com 2.222 menções, depois Ansible) ainda entram como *diferencial* no júnior brasileiro, mas já são **requisito** em vagas remotas internacionais.
5. **Cloud**: nas vagas BR júnior costuma ser "noções", mas há vaga (Dexian) exigindo vivência real em AWS/GCP/Azure; no internacional é praticamente obrigatório, com **Azure liderando entre júnior** e AWS liderando no Brasil.
6. **Inglês**: BR pede "leitura técnica"; internacional exige B1 conversacional e 79% das vagas LATAM que citam nível pedem avançado — 70,6% das vagas são publicadas em inglês.
7. **Formação**: graduação é exigida em todas as vagas BR que citam escolaridade; 67,8% das vagas LATAM pedem diploma (júnior pede mais: 33,5%).
8. **Salários**: BR júnior entre **R$ 3.647 (Glassdoor) e R$ 5.600 (mediana Robert Half)**, com DevOps júnior citado entre R$ 5–10 mil; remoto júnior dos EUA ~**US$ 94–111 mil/ano**; nenhuma das 6 vagas BR lidas publicou salário.
9. **Nomes de cargo**: Brasil = "Analista de Infraestrutura/DevOps/SRE Júnior", "Analista de Monitoramento (NOC)", "Trainee"; internacional = "Junior DevOps Engineer", "Junior Site Reliability Engineer", "Linux Administrator".
10. **Lacunas do lab de VPS**: Kubernetes, Terraform/Ansible, pipeline de CI/CD real, Prometheus/Grafana+logs, cloud, Windows/VMware, plantão/runbooks/ITIL, inglês e — o mais difícil — **experiência prévia comprovável**; prioridade sugerida: CI/CD → observabilidade → k3s → IaC → redes/segurança → cloud free tier.
