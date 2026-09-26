---
aliases: [visao-geral, overview, pilares]
tags: [tracker, overview]
---

# Visão Geral — Jornada do Local à Nuvem

> Fonte consolidada de contexto. Define metodologia, pilares, ordem de execução e política de status.

## Jornada e público

- **Jornada:** da JVM no Linux local à plataforma com Docker, VPS, Terraform, CI/CD e observabilidade.
- **Público:** desenvolvedor em transição para Junior DevOps / Cloud Platform Engineer e backend cloud-native.
- **Objetivo final:** demonstrar a evolução operacional do **mesmo sistema** — o SecurePay — do ambiente local até a plataforma orquestrada, uma capacidade por vez.

## A narrativa

Uma frase só, do começo ao fim:

> Comecei executando a aplicação localmente, depois operei a mesma aplicação em um servidor Linux manualmente, automatizei as partes repetitivas, migrei a arquitetura para serviços de Cloud e finalmente passei a gerenciar essa infraestrutura com IaC e Kubernetes.

Ela se desdobra em cinco transições. Cada transição é uma **necessidade**, não uma tecnologia:

| Transição | A dor que a provoca | Issues |
|---|---|---|
| manual → repetitivo | o mesmo comando roda toda vez e alguém esquece de um deles | 04–06 |
| repetitivo → automatizado | teste quebrado passa revido porque ninguém olhou | 07, 10 |
| único → multi-serviço | o consumidor cai e o evento some com ele | 08–09 |
| local → cloud | o estado do Terraform vive no disco de quem aplicou | 11–12 |
| declarado → orquestrado | um nó morre e ninguém percebe | 13–18 |

Os números são de `01 → 18`, mas a ordem narrativa é esta — nem toda Issue é um marco, e `03` é o caso explícito (ver [Ordem de execução](#ordem-de-execução)).

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
SERVIDOR (04–05)
  ↓
RECUPERAÇÃO (06)
  ↓
OBSERVABILIDADE E MENSAGERIA (07–08)
  ↓
ISOLAMENTO E LIMITES (09)
  ↓
AUTOMAÇÃO (10)
  ↓
IaC E CLOUD (11–12)
  ↓
CI/CD + DEVSECOPS (13–17)
  ↓
KUBERNETES (18)
```

- `01, 02` Done; `11` Parked; `04` é a próxima a entrar em execução.
- A numeração é identificador estável. As dependências reais são declaradas em `## Dependências` de cada Issue.
- `03-terraform-vpc` é **laboratório anexado**, não etapa da narrativa: foi concluída antes de existir a dor que Terraform resolve, para não custar dinheiro. **Não a use como marco da evolução.** Ela permanece como pré-requisito de `10`, `11` e `12` apenas porque o gate de IaC valida o HCL que ela criou.

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
- **Custo zero por regra:** nenhuma Issue exige servidor pago para ser concluída. `04–09` rodam inteiros numa VM local (VirtualBox/UTM/libvirt com Ubuntu ou Debian); `03`, `11` e `12` usam LocalStack ou `terraform plan`. VPS pública só entra como prova final opcional, quando for preciso validar TLS público, DNS e tráfego real de internet.
- Kubernetes, Cloud e Terraform entram apenas quando existe necessidade concreta que os justifique — nunca porque fazem parte do objetivo final.
- Nenhuma Issue pode antecipar tecnologia cujo problema ela não resolve.

---

**Board:** [BOARD](BOARD.md)
