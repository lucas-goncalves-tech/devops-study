---
aliases: [issue-10, github-actions]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 10 — Pipeline de CI com testes, scan de imagem e gate de IaC

## Contexto

O workflow `.github/workflows/CI.yml` existe mas o job `build` não tem nenhum `steps` — não executa nada. Hoje o gate real é `./mvnw test` rodando localmente: teste quebrado, CVE em imagem ou Terraform inválido passam batido até alguém lembrar de olhar.

## Objetivo

Estado final: a cada push e pull request, a pipeline executa a suíte de testes, constrói a imagem e faz scan de vulnerabilidades, valida o HCL, e qualquer falha dessas barreiras impede o merge.

## Dependências

- Requer Issue 03 — o gate de IaC valida o HCL declarado em `infra/`
- Requer Issue 02 — o scan de imagem precisa do `Dockerfile` e do contexto de build

## Escopo

- Substituir o workflow existente por um pipeline com passos reais
- Job de backend: Java 21, cache do Maven e execução da suíte de testes
- Job de segurança de imagem com scan bloqueante
- Gate de IaC com `fmt`, `validate` e `plan`
- Proteção de merge: pipeline verde é pré-requisito

## Fora de escopo

- GitOps (ArgoCD/Flux), deploy ECS Fargate, Ansible
- Foco exclusivo: workflow YAML, build e testes Maven, build Docker, scan de imagem bloqueante, `fmt` e `validate`
- Deploy contínuo — Issue 17
- Gates de segredos, SAST e SCA — Issues 13, 14 e 15
- **FinOps de staging (auto-stop de ambiente)** — fora de escopo por decisão registrada em `00-visao-geral.md`; além disso não existe ambiente de staging para desligar antes da Issue 12
- Foco exclusivo do gate de IaC: consistência entre `infra/provider.tf` e o serviço que o emulador realmente inicializa

## Conhecimentos envolvidos

- Workflows, triggers e filtros de caminho
- Build Maven com cache em CI
- Scan de vulnerabilidade de imagem em pipeline
- `fmt`, `validate` e `plan` do Terraform no CI
- Proteção de branch e pré-requisitos de merge

## Estado atual

- `.github/workflows/CI.yml` declara um job `build` sem `steps`
- Não há execução automática de testes em push ou pull request
- Não há scan de imagem nem validação de HCL automatizados

## Resultado esperado

- Pull request com teste quebrado falha na pipeline e não pode ser mergeado
- CVE `HIGH` ou `CRITICAL` na imagem falha a pipeline
- Erro de `fmt` ou `validate` no HCL falha a pipeline
- A pipeline verde é exigida como pré-requisito de merge

## Requisitos

- [ ] Substituir `.github/workflows/CI.yml` por um workflow com `push` e `pull_request` em `main`/`master`
- [ ] Separar backend e IaC por filtro de caminho alterado
- [ ] Job de backend: Java 21 com `cache: maven` e execução de `./mvnw verify`
- [ ] Job de segurança de imagem: build e scan bloqueante para `HIGH` e `CRITICAL`
- [ ] Job de gate de IaC: `fmt -check` → `init` → `validate` → `plan`
- [ ] Fazer o job de IaC subir o emulador com a configuração real de `infra/platform/compose-localstack.yaml`
- [ ] Exigir YAML íntegro e pipeline verde como pré-requisito de merge
- [ ] Manter `./mvnw test` como verificação local equivalente ao job de backend

## Critérios de aceitação

- [ ] Um commit com teste quebrado derruba a pipeline e o merge é bloqueado
- [ ] Um `HIGH` ou `CRITICAL` detectado no scan da imagem derruba a pipeline
- [ ] `terraform fmt -check` ou `terraform validate` com erro derruba a pipeline
- [ ] O job de IaC sobe o emulador sem erro de inicialização de variável de ambiente
- [ ] Pull request limpo passa em todos os jobs

## Validação

- Introduzir deliberadamente um teste quebrado e confirmar o falha no job de backend; reverter em seguida
- **Build to break:** forçar um erro de sintaxe no HCL e confirmar falha no gate; reverter
- Conferir a saída do job de IaC mostrando `fmt`, `validate` e `plan` executando
- Abrir pull request limpo e confirmar pipeline verde

## Evidências

- URL da execução da pipeline verde no commit atual
- Log da execução com teste quebrado (falso) e depois revertido
- Output do job de IaC com os três comandos
- Configuração de proteção de branch exigindo a pipeline

## Limitações / notas

- **Achado — divergência entre a especificação do gate e a configuração real do emulador.** Antes desta Issue, o gate de IaC foi especificado como `localstack/localstack:4.4.0` com `SERVICES=s3,ec2` e sem token. O arquivo real `infra/platform/compose-localstack.yaml` hoje declara:
  - `image: localstack/localstack` — sem pin de versão
  - `LOCALSTACK_AUTH_TOKEN=${LOCALSTACK_AUTH_TOKEN:?}` — obrigatório; sem a variável o Compose aborta a inicialização
  - `SERVICES=s3,ec2,elbv2`
  - `infra/provider.tf` declara endpoints apenas para `s3` e `ec2` (`elbv2` comentado)

  O job de IaC precisa partir da configuração real: ou reutiliza o próprio arquivo de Compose (com `LOCALSTACK_AUTH_TOKEN` fornecido como secret), ou define seu próprio serviço com os serviços que `provider.tf` declara. Especificar `SERVICES=s3,ec2` num job isolado não reproduz o ambiente do repositório.
- **Contrato de portas:** `8080` é a única porta publicada pelo Compose do backend. A suíte de testes usa perfil próprio com porta efêmera e H2, então não colide com o serviço rodando localmente
- O job de testes precisa de Docker disponível no runner — a suíte inclui um teste de contêiner baseado em Testcontainers
- Não assumir que a pipeline anterior captura regressões: o workflow atual não executa nada
