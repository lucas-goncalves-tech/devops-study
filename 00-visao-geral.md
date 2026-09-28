---
aliases: [visao-geral, overview, pilares]
tags: [tracker, overview]
---

# Visão Geral — 3 apps, 3 trilhas

> Fonte consolidada de contexto. Define metodologia, pilares, as trilhas e política de status.

## As 3 trilhas

O monorepo tem **3 apps, um por trilha**. Cada trilha é **independente** — segue do serviço local até o
estágio final sem depender de Issue de outro app — e é **completa ponta a ponta**: entrar no app
`X` é fazer uma jornada inteira, não um pedaço de uma sequência global de 18 números.

| App | Trilha | Estágio atual |
|---|---|---|
| [`ledger-service`](ledger-service/) | **VPS** — `linux → hardening → caddy → isolamento → backups → deploy` | `01`, `02` `done`; `03` é a próxima a entrar |
| [`commerce-api`](commerce-api/) | **AWS** — `linux → compose → terraform/LocalStack → CI → observabilidade → S3/EC2` | `01` a entrar; `06` `parked` |
| [`webhook-gateway`](webhook-gateway/) | **DevSecOps** — `pipeline base → secrets → SAST → SCA → hardening → gates → DAST → mensageria` | `01` a entrar |

Os números reiniciam em `01` em cada app: `ledger 05` e `commerce 05` são Issues diferentes, de
apps diferentes. A numeração é identificador estável **dentro do app**, e as dependências reais
estão declaradas em `## Dependências` de cada Issue.

- **Jornada:** da JVM no Linux local à plataforma com Docker, VPS, Terraform, CI/CD e observabilidade.
- **Público:** desenvolvedor em transição para Junior DevOps / Cloud Platform Engineer e backend cloud-native.
- **Objetivo final:** demonstrar a evolução operacional de **sistemas reais** até a plataforma orquestrada, uma capacidade por app.

## Separação de responsabilidades

```text
ISSUE      → problema, escopo, resultado esperado, validação, evidências
teach-anything → diagnostica, ensina e verifica o entendimento necessário
repositório → a infraestrutura real construída
validator  → se a capacidade realmente foi entregue
```

- A Issue **não** é aula, apostila, tutorial nem roadmap de estudos. Todo material de estudo fica em `<app>/estudos/`, fora do escopo das Issues.
- Toda Issue segue o template: `Contexto`, `Objetivo`, `Dependências`, `Escopo`, `Fora de escopo`, `Conhecimentos envolvidos`, `Estado atual`, `Resultado esperado`, `Requisitos`, `Critérios de aceitação`, `Validação`, `Evidências`, `Limitações / notas`.
- Critérios de aceitação são observáveis e verificáveis por um terceiro, sem depender da opinião do autor.

## Metodologia

Cada Issue é uma mudança concreta no sistema, redigida como RFC de problema corporativo:

1. **Problema real do negócio:** a necessidade que motiva a mudança — a tecnologia aparece como consequência, nunca como objetivo.
2. **Escopo e fora de escopo:** o que muda e o que deliberadamente não muda.
3. **Critérios de aceitação:** comportamento observável, verificável por terceiro.
4. **Validação e evidências:** como provar tecnicamente, com build to break / build to defend quando fizer sentido.
5. **Limitações:** restrições do ambiente registradas — nunca transformar limitação conhecida em falsa evidência.

## Os 3 pilares do engenheiro maduro

1. **Fundamentos físicos:** redes L4 (socket TCP, handshake) ao L7 (HTTP, TLS, reverse proxy); gargalos de I/O e memória (IOPS, fsync, heap vs RSS, page cache, OOM em cgroups v2).
2. **Negócio e confiabilidade preventiva:** prevenção acima de reação — shift-left, healthcheck real, tolerância a falhas.
3. **Automação intencional e KISS:** IaC declarativo idempotente e pipelines versionadas em vez de scripts ad-hoc; simplicidade, sem overengineering.

## Política de status

| Status | Significado |
|---|---|
| `done` | Critérios de aceitação verificados e evidências registradas |
| `doing` | Em execução agora |
| `parked` | Revertida ou adiada deliberadamente; nenhuma Issue depende dela |
| `todo` | Não iniciada |

- Nenhuma Issue pode entrar em `done` sem que a `Validação` tenha sido executada e as `Evidências` registradas.
- Limitação de ambiente registrada em `Limitações / notas` nunca conta como evidência de validação.
- O checkbox do [Board](BOARD.md) espelha o `status:` do frontmatter da Issue.

## Restrições de escopo

- **FinOps, entrevistas e Ansible estão fora de escopo por decisão.** As trilhas `interview-prep-finops` e `ansible` do `devops-study` não migraram para este monorepo e continuam lá. A estimativa de custo dentro de `commerce 07` não é um programa de FinOps: é uma trava contra cobrança involuntária, pré-requisito de segurança antes de provisionar recurso pago.
- **Custo zero por regra:** nenhuma Issue exige servidor pago para ser concluída. As trilhas rodam inteiras em VM local, LocalStack ou `terraform plan`; VPS pública e conta AWS só entram como prova final, quando for preciso validar TLS público, DNS, tráfego real de internet ou deploy em nuvem real.
- **Kubernetes fora de escopo por decisão.** A Issue 18 (cluster multi-node + chart Helm) foi arquivada em [`archive/18-kubernetes-helm/`](archive/18-kubernetes-helm/issue.md) e registrada como candidata a 4ª trilha futura — não é marco de nenhuma das 3.
- **A infraestrutura é construção do usuário.** CI, Terraform, Dockerfile, `docker-compose.yaml`, `healthcheck.sh` e código dos apps **nunca** são criados nem alterados pelo agente: o agente escreve Issues, estudos, tracker e docs.
- Nenhuma Issue pode antecipar tecnologia cujo problema ela não resolve.

## Fora de escopo

- [`archive/18-kubernetes-helm/`](archive/18-kubernetes-helm/issue.md) — a Issue 18 e seu estudo, arquivados: Kubernetes não entra neste momento.
- `interview-prep-finops` e `ansible` — trilhas do `devops-study` fora das 3 escolhidas.
- Incorporar os apps num único serviço: cada app é um sistema, com sua trilha.

---

**Board:** [BOARD](BOARD.md)
