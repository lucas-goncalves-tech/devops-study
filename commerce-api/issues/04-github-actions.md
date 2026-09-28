---
aliases: [issue-04, github-actions]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 04 — Pipeline de CI com testes, scan de imagem e gate de IaC

## Contexto

O workflow `.github/workflows/CI.yml` deste repo compartilhado existe mas o job `build` não tem nenhum `steps` — não executa nada. Hoje o gate real da `commerce-api` é `npm test` rodando localmente: teste quebrado, CVE em imagem ou Terraform inválido passam batido até alguém lembrar de olhar. Os arquivos de CI são construção do usuário: esta Issue descreve o que o workflow precisa conter, não o escreve.

## Objetivo

Estado final: a cada push e pull request, a pipeline executa a suíte de testes, constrói a imagem e faz scan de vulnerabilidades, valida o HCL, e qualquer falha dessas barreiras impede o merge.

## Dependências

- Requer Issue 03 — o gate de IaC valida o HCL declarado em `commerce-api/infra/`
- Requer Issue 02 — o scan de imagem precisa do `commerce-api/app/Dockerfile` e do contexto de build (`commerce-api/app/`, com `package-lock.json`)

## Escopo

- Substituir o workflow existente por um pipeline com passos reais
- Job da `commerce-api`: Node 20, cache do npm e execução da suíte de testes
- Job de segurança de imagem com scan bloqueante
- Gate de IaC com `fmt`, `validate` e `plan`
- Proteção de merge: pipeline verde é pré-requisito

## Fora de escopo

- GitOps (ArgoCD/Flux), deploy ECS Fargate, Ansible
- Foco exclusivo: workflow YAML, `npm ci` e `npm test`, build Docker, scan de imagem bloqueante, `fmt` e `validate`
- Deploy contínuo — [Issue 07 do `ledger-service`](../../ledger-service/issues/07-cicd-vps-deploy.md)
- Gates de segredos, SAST, SCA e hardening do pipeline — [Issue 04](../../webhook-gateway/issues/04-secrets-hygiene.md), [Issue 05](../../webhook-gateway/issues/05-sast-semgrep.md), [Issue 06](../../webhook-gateway/issues/06-sca-dependencias-imagem.md) e [Issue 07](../../webhook-gateway/issues/07-pipeline-hardening.md) do `webhook-gateway`
- **FinOps de staging (auto-stop de ambiente)** — fora de escopo por decisão registrada em `00-visao-geral.md`; além disso não existe ambiente de staging para desligar antes da Issue 07
- Foco exclusivo do gate de IaC: consistência entre `commerce-api/infra/provider.tf` e o serviço que o emulador realmente inicializa

## Conhecimentos envolvidos

- Workflows, triggers e filtros de caminho
- Build Node com cache em CI (`actions/setup-node` com `cache: npm` e `npm ci`)
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
- [ ] Separar app e IaC por filtro de caminho alterado
- [ ] Job da `commerce-api`: Node 20 com `cache: npm` e execução de `npm ci && npm test && npm run lint`
- [ ] Job de segurança de imagem: build e scan bloqueante para `HIGH` e `CRITICAL`
- [ ] Job de gate de IaC: `fmt -check` → `init` → `validate` → `plan`
- [ ] Fazer o job de IaC subir o emulador com a configuração real de `commerce-api/infra/platform/compose-localstack.yaml`
- [ ] Exigir YAML íntegro e pipeline verde como pré-requisito de merge
- [ ] Manter `npm test` e `npm run lint` como verificação local equivalente ao job da `commerce-api`

## Critérios de aceitação

- [ ] Um commit com teste quebrado derruba a pipeline e o merge é bloqueado
- [ ] Um `HIGH` ou `CRITICAL` detectado no scan da imagem derruba a pipeline
- [ ] `terraform fmt -check` ou `terraform validate` com erro derruba a pipeline
- [ ] O job de IaC sobe o emulador sem erro de inicialização de variável de ambiente
- [ ] Pull request limpo passa em todos os jobs

## Validação

- Introduzir deliberadamente um teste quebrado em `commerce-api/app/tests/` e confirmar a falha no job da API; reverter em seguida
- **Build to break:** forçar um erro de sintaxe no HCL e confirmar falha no gate; reverter
- Conferir a saída do job de IaC mostrando `fmt`, `validate` e `plan` executando
- Abrir pull request limpo e confirmar pipeline verde

## Evidências

- URL da execução da pipeline verde no commit atual
- Log da execução com teste quebrado (falso) e depois revertido
- Output do job de IaC com os três comandos
- Configuração de proteção de branch exigindo a pipeline

## Limitações / notas

- **Achado — divergência entre a especificação do gate e a configuração real do emulador.** Antes desta Issue, o gate de IaC foi especificado como `localstack/localstack:4.4.0` com `SERVICES=s3,ec2` e sem token. O arquivo real `commerce-api/infra/platform/compose-localstack.yaml` hoje declara:
  - `image: localstack/localstack` — sem pin de versão
  - `LOCALSTACK_AUTH_TOKEN=${LOCALSTACK_AUTH_TOKEN:?}` — obrigatório; sem a variável o Compose aborta a inicialização
  - `SERVICES=s3,ec2,elbv2`
  - `commerce-api/infra/provider.tf` declara endpoints apenas para `s3` e `ec2` (`elbv2` comentado)

  O job de IaC precisa partir da configuração real: ou reutiliza o próprio arquivo de Compose (com `LOCALSTACK_AUTH_TOKEN` fornecido como secret), ou define seu próprio serviço com os serviços que `provider.tf` declara. Especificar `SERVICES=s3,ec2` num job isolado não reproduz o ambiente do repositório.
- **Contrato de portas:** a `commerce-api` escuta em `PORT=3000` (host `0.0.0.0`) e o `Dockerfile` expõe `3000` com `HEALTHCHECK` em `/health`. Não existe `docker-compose.yaml` neste app — a Issue 02 orquestra API + Postgres; até lá o job de testes não depende de serviço no ar
- A suíte `npm test` (vitest) usa `app.inject()` e mocka o serviço, então não abre porta nem precisa de Docker no runner — apenas o job de segurança de imagem precisa de daemon Docker para o build
- Não assumir que a pipeline anterior captura regressões: o workflow atual não executa nada
