---
aliases: [visao-geral, overview, pilares]
tags: [tracker, overview]
---

# Visão Geral — Jornada do Local à Nuvem

> Fonte consolidada de contexto. Define metodologia, pilares, ordem de execução e política de status.

## Jornada e público

- **Jornada:** da JVM no Linux local à plataforma com Docker, VPS, Terraform, CI/CD e observabilidade.
- **Público:** desenvolvedor em transição para Junior DevOps / Cloud Platform Engineer e backend cloud-native.
- **Objetivo final:** transformar o backend em plataforma profissional DevSecOps ao longo das 18 Issues.

## Separação de responsabilidades

```text
ISSUE      → problema, escopo, resultado esperado, validação, evidências
teach-devops → diagnostica, ensina e verifica o entendimento necessário
repositório → a infraestrutura real construída
validator  → se a capacidade realmente foi entregue
```

- A Issue **não** é aula, apostila, tutorial nem roadmap de estudos. Todo material de estudo fica em `estudos/`, fora do escopo das Issues.
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

## Ordem de execução

A ordem `01 → 18` segue a evolução da infraestrutura do sistema, não uma sequência de aulas:

```text
LOCAL (01)
  ↓
CONTAINERS (02)
  ↓
VPS / LINUX PURO (04–05)
  ↓
MONITORAMENTO E MENSAGERIA (06–07)
  ↓
OPERAÇÃO E HARDENING (08–09)
  ↓
AUTOMAÇÃO (10)
  ↓
IaC E CLOUD (11–12)
  ↓
CI/CD + DEVSECOPS (13–17)
  ↓
KUBERNETES (18)
```

- `01, 02, 03` Done; `11` Parked; `04` é a próxima a entrar em execução.
- A numeração é identificador estável. As dependências reais são declaradas em `## Dependências` de cada Issue.
- `03-terraform-vpc` foi concluída cedo, como laboratório LocalStack sem custo; o restante da IaC e da Cloud aparece depois que a necessidade existe.

## Política de status

| Status | Significado |
|---|---|
| `done` | Critérios de aceitação verificados e evidências registradas |
| `doing` | Em execução agora |
| `parked` | Revertida ou adiada deliberadamente; nenhuma Issue depende dela |
| `todo` | Não iniciada |

- Nenhuma Issue pode entrar em `done` sem que a `Validação` tenha sido executada e as `Evidências` registradas.
- Limitação de ambiente registrada em `Limitações / notas` nunca conta como evidência de validação.

## Restrições de escopo

- **FinOps, entrevistas e Ansible estão fora de escopo por decisão.** A estimativa de custo dentro de `12-aws-production` não é um programa de FinOps: é uma trava para impedir cobrança involuntária, pré-requisito de segurança antes de provisionar recurso pago.
- Kubernetes, Cloud e Terraform entram apenas quando existe necessidade concreta que os justifique — nunca porque fazem parte do objetivo final.
- Nenhuma Issue pode antecipar tecnologia cujo problema ela não resolve.

---

**Board:** [[BOARD]]
