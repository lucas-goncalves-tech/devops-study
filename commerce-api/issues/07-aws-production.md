---
aliases: [issue-07, aws-production]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: media
---

# Issue 07 — Estado Terraform remoto com lock e computação em nuvem real

## Contexto

O estado do Terraform vive no disco da máquina de quem aplicou. Dois applies simultâneos corrompem o estado, não há histórico compartilhado e nenhuma carga roda em computação real — o laboratório prova o desenho, não a operação.

## Objetivo

Estado final: estado remoto com versionamento e lock contra applies concorrentes, ambientes separados, e computação mínima provisionada com custo conhecido antes de subir e destruição limpa depois de validar.

## Dependências

- Requer Issue 03 — é o estado local de `commerce-api/infra/terraform.tfstate` que precisa ser migrado

## Escopo

- Backend remoto com versionamento
- Lock contra applies concorrentes
- Separação de ambientes por workspace, prefixo ou `tfvars` por ambiente
- Estimativa de custo, computação mínima e destruição sem órfãos

## Fora de escopo

- Kubernetes e orquestração — fora de escopo por decisão; a Issue correspondente está arquivada em `archive/18-kubernetes-helm/`
- Pipeline de CI/CD — Issue 04; aplicação de infraestrutura e deploy automatizado — [Issue 09](09-pipeline-infra-apply.md) e [Issue 10](10-deploy-ec2-pipeline.md) desta trilha
- Backups e monitoramento — [Issue 05 do `ledger-service`](../../ledger-service/issues/05-db-backups-s3.md) e Issue 05
- Serviços gerenciados além da computação mínima

## Conhecimentos envolvidos

- Backends remotos e locking do Terraform
- Workspaces e separação de ambientes
- Bloco `variable`, `tfvars` e sobreposição de valor por ambiente
- Cálculo de custo e right-sizing em nuvem

## Estado atual

- Estado local: `apply` concorrente pode corromper
- Sem histórico de versão nem trava
- Custo desconhecido; nada sobe em nuvem real

## Resultado esperado

- Dois `apply` simultâneos não corrompem o estado — o segundo espera ou falha com lock
- Estado com histórico de versão acessível fora da máquina local
- Ambientes separados sem colisão de chave
- Custo mensal documentado antes do provisionamento
- `destroy` sem cobrança residual

## Requisitos

- [ ] Migrar o estado para backend remoto com versionamento
- [ ] Ativar locking contra `apply` concorrentes
- [ ] Separar ambientes por workspace, prefixo ou `tfvars` por ambiente
- [ ] Parametrizar com bloco `variable` tudo que muda entre ambientes: valor de laboratório nunca é o de produção e nenhum valor de ambiente fica hardcoded nos arquivos `.tf`
- [ ] Antes do primeiro `apply` real, declarar o provider da AWS remota em diretório próprio de produção, sem editar `commerce-api/infra/provider.tf` — ele é contrato de saída da Issue 03 e o `plan` do gate da [Issue 04](04-github-actions.md) roda com ele; `endpoints` de `http://localhost:4566`, credenciais mock e `skip_credentials_validation`/`skip_requesting_account_id` ficam fora da declaração de produção
- [ ] Declarar o mecanismo de reuso do código da Issue 03: a raiz de produção instancia o módulo (source para commerce-api/infra, providers/configuration_aliases resolvidos para a AWS real), os endpoints do laboratório ficam atrás de variable vazia em produção e nenhum .tf do lab é editado
- [ ] Estimar custo mensal antes de subir qualquer recurso
- [ ] Provisionar computação mínima para API e banco
- [ ] Provisionar a instância na subnet pública da VPC da Issue 03, com rota padrão para o IGW (sem NAT), bootstrap com Docker antes do primeiro deploy e entrada `22` restrita à variável `admin_cidr` — nunca `0.0.0.0/0`
- [ ] Reativar o `elbv2` no provider de produção, com ALB na subnet pública, target group para a API na porta `3000` e listener `80`; quando houver domínio apontado: certificado ACM e redirect `80` para `443` (sem domínio, registrar a pendência em Limitações)
- [ ] Provar o ciclo de vida (`destroy` sem cobrança residual) e deixar o ambiente re-provisionado ao final, sem recursos órfãos — a máquina viva é premissa das Issues 08 e 10

## Critérios de aceitação

- [ ] Dois `terraform apply` disparados em paralelo não corrompem o estado: um espera ou falha indicando o lock
- [ ] O estado não está mais apenas no disco local e mantém histórico de versão
- [ ] Ambientes não escrevem na mesma chave de estado
- [ ] O custo mensal estimado está documentado **antes** do primeiro `apply` de recursos pagos
- [ ] Após `terraform destroy`, não resta cobrança de recurso, e um `apply` de re-provisionamento devolve a instância `running` — destroy é prova de ciclo de vida, não o estado final desta Issue
- [ ] O provider da execução real não mantém nenhum resíduo do laboratório (`localhost:4566`, credencial mock, `skip_*`) — build to break: sem os `endpoints`, nenhuma chamada vai mais para `localhost:4566`; sem as credenciais mock (ou removido um `skip_*`), o `plan` falha por falta de credencial real
- [ ] Os valores que diferem entre ambientes vêm de `variable`/`tfvars` ou da separação escolhida, nunca de valor fixo reaproveitado do laboratório
- [ ] Reuso por instanciação, não por cópia: o apply de produção termina com `git diff` vazio em commerce-api/infra/ e o plan da raiz de produção não referencia localhost:4566
- [ ] O plano real mostra a instância na subnet pública com rota para o IGW, entrada `22` limitada a `admin_cidr`, nenhuma porta `0.0.0.0/0` e o `docker pull` na instância nova conclui antes do deploy
- [ ] O plano real mostra o ALB ativo, health check do target group `healthy`, entrada `3000` da API restrita ao SG do ALB e nenhuma regra com `0.0.0.0/0`

## Validação

- Disparar dois `apply` simultâneos e observar o lock agindo
- Inspecionar o backend confirmando versionamento
- Rodar o mesmo comando em dois ambientes e confirmar chaves distintas
- Conferir a estimativa registrada antes do provisionamento
- Rodar `destroy` e verificar a ausência de recursos remanescentes e de cobrança
- Inspecionar o `provider.tf` usado no apply real confirmando ausência de endpoints locais e `skip_*`
- Na instância provisionada, executar o bootstrap e confirmar `docker pull` e `ssh` pelo endereço restrito à CIDR de admin
- Inspecionar o ALB: DNS responde, health check `healthy` e a API só é alcançável através dele

## Evidências

- Log dos dois `apply` concorrentes mostrando o lock
- Inspeção do backend com histórico de versão
- Chaves de estado por ambiente
- Estimativa de custo documentada antes do apply
- Output do `destroy` sem recursos órfãos e do `apply` de re-provisionamento com a instância `running`
- Trecho do `provider.tf` de produção sem endpoints locais nem credenciais mock
- Saída do `docker pull` na instância nova e regra de ingress `22` com `admin_cidr`
- DNS do ALB e estado `healthy` do target group

## Limitações / notas

- Requer conta de nuvem real e credenciais — pré-requisito de ambiente. O custo é a razão pela qual a estimativa é critério de aceitação, não etapa opcional
- A migração de estado exige cuidado: `terraform init -migrate-state` com backup antes de qualquer operação
- **FinOps:** o contrato de escopo em `00-visao-geral.md` declara FinOps fora de escopo por decisão. Aqui a estimativa de custo não é um programa de FinOps — é uma trava de segurança para não criar cobrança involuntária. Registrar essa distinção ao executar
- Esta Issue só é atingível depois da Issue 03, que gera o estado local a ser migrado
- Subnet pública por custo zero: a alternativa privada exigiria NAT/endpoint (custo recorrente); a fronteira é fechada por SG
- Fronteira com pendência declarada: sem domínio registrado não há certificado ACM; declarar a pendência em vez de deixar a fronteira sem dono — o listener `80` existe mesmo sem domínio
