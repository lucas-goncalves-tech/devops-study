---
aliases: [issue-12, aws-production]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: media
---

# Issue 12 — Estado Terraform remoto com lock e computação em nuvem real

## Contexto

O estado do Terraform vive no disco da máquina de quem aplicou. Dois applies simultâneos corrompem o estado, não há histórico compartilhado e nenhuma carga roda em computação real — o laboratório prova o desenho, não a operação.

## Objetivo

Estado final: estado remoto com versionamento e lock contra applies concorrentes, ambientes separados, e computação mínima provisionada com custo conhecido antes de subir e destruição limpa depois de validar.

## Dependências

- Requer Issue 03 — é o estado local de `infra/terraform.tfstate` que precisa ser migrado

## Escopo

- Backend remoto com versionamento
- Lock contra applies concorrentes
- Separação de ambientes por workspace ou prefixo
- Estimativa de custo, computação mínima e destruição sem órfãos

## Fora de escopo

- Kubernetes e orquestração — Issue 18
- Pipeline de CI/CD — Issues 10 e 17
- Backups e monitoramento — Issues 09 e 06
- Serviços gerenciados além da computação mínima

## Conhecimentos envolvidos

- Backends remotos e locking do Terraform
- Workspaces e separação de ambientes
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
- [ ] Separar ambientes por workspace ou prefixo
- [ ] Estimar custo mensal antes de subir qualquer recurso
- [ ] Provisionar computação mínima para API e banco
- [ ] Desligar após validar, sem recursos órfãos

## Critérios de aceitação

- [ ] Dois `terraform apply` disparados em paralelo não corrompem o estado: um espera ou falha indicando o lock
- [ ] O estado não está mais apenas no disco local e mantém histórico de versão
- [ ] Ambientes não escrevem na mesma chave de estado
- [ ] O custo mensal estimado está documentado **antes** do primeiro `apply` de recursos pagos
- [ ] Após `terraform destroy`, não resta cobrança de recurso

## Validação

- Disparar dois `apply` simultâneos e observar o lock agindo
- Inspecionar o backend confirmando versionamento
- Rodar o mesmo comando em dois ambientes e confirmar chaves distintas
- Conferir a estimativa registrada antes do provisionamento
- Rodar `destroy` e verificar a ausência de recursos remanescentes e de cobrança

## Evidências

- Log dos dois `apply` concorrentes mostrando o lock
- Inspeção do backend com histórico de versão
- Chaves de estado por ambiente
- Estimativa de custo documentada antes do apply
- Output do `destroy` sem recursos órfãos

## Limitações / notas

- Requer conta de nuvem real e credenciais — pré-requisito de ambiente. O custo é a razão pela qual a estimativa é critério de aceitação, não etapa opcional
- A migração de estado exige cuidado: `terraform init -migrate-state` com backup antes de qualquer operação
- **FinOps:** o contrato de escopo em `00-visao-geral.md` declara FinOps fora de escopo por decisão. Aqui a estimativa de custo não é um programa de FinOps — é uma trava de segurança para não criar cobrança involuntária. Registrar essa distinção ao executar
- Esta Issue só é atingível depois da Issue 03, que gera o estado local a ser migrado
