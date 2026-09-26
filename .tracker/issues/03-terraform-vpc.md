---
aliases: [issue-03, terraform-vpc]
tags: [tracker, issue, done, study-needed]
status: done
prioridade: alta
---

# Issue 03 — Rede multi-tier declarada em Terraform com banco isolado e bucket privado

## Contexto

A infraestrutura existe só como estado manual: não é reproduzível, não é versionada e não há como provar que o desenho não mudou por acidente. O laboratório LocalStack permite validar arquitetura de nuvem sem custo.

## Objetivo

Estado final: rede, roteamento, segurança de grupo e storage declarados em HCL idempotente contra um emulador local, com o banco isolado por rota e por security group, e um bucket privado. O que é validado no laboratório fica separado do que só existe em AWS real.

## Escopo

- Provider `hashicorp/aws` com endpoints locais e credenciais mock
- VPC `10.0.0.0/16` com três subnets: pública, privada e isolada
- Internet Gateway e route tables com comportamento de rota explícito
- Security groups encadeados (API → banco) e bucket S3 com bloqueio de acesso público
- ALB, target group e listener declarados e desativados no laboratório

## Fora de escopo

- Kubernetes, Helm, CloudWatch Logs avançado, EKS, esteiras de CI/CD
- Foco exclusivo: HCL, provider local em `localhost:4566`, VPC multi-tier, roteamento explícito, SGs sem vazamento em `5432`, bucket privado, drift via `plan` e ALB com target group e health check
- NAT Gateway, TLS/HTTPS no ALB, WAF, Multi-AZ e backup: fora desta Issue

## Conhecimentos envolvidos

- HCL, estado do Terraform, idempotência e drift
- VPC, subnets, route tables, security groups
- Internet Gateway e diferença entre subnet pública e privada
- S3 e bloqueio de acesso público
- Load balancers: ALB vs NLB, target groups e health checks
- Integração Terraform + LocalStack

## Estado atual

| Arquivo | Conteúdo atual | Observação |
|---|---|---|
| `infra/provider.tf` | Provider AWS + LocalStack, endpoints `s3`/`ec2` | Endpoint `elbv2` comentado |
| `infra/vpc.tf` | VPC, três subnets, IGW e route tables | ALB comentado; IGW e route tables já implementados |
| `infra/security.tf` | SG da API e SG do banco | SG do ALB comentado; API exposta temporariamente para o laboratório |
| `infra/s3.tf` | Bucket e quatro bloqueios de acesso público | Adequado ao escopo desta Issue |

## Resultado esperado

```text
subnet pública → Internet Gateway
subnet API     → sem rota internet direta
subnet banco   → sem rota internet direta
```

- `init`, `validate` e `plan` passam sem drift
- Porta `5432` jamais aceita `0.0.0.0/0`
- Bucket 100% privado com os quatro bloqueios
- Recursos de ALB preservados no código e documentados como desativados

## Requisitos

- [x] Declarar provider `hashicorp/aws` `~> 5.0`, endpoints `ec2` e `s3` em `http://localhost:4566`, região `sa-east-1`, credenciais mock com `skip_credentials_validation` e `skip_requesting_account_id`
- [x] Subir emulador local com a lista de serviços do `infra/platform/compose-localstack.yaml` e endpoint respondendo
- [x] Criar VPC `10.0.0.0/16`
- [x] Criar subnet pública `10.0.1.0/24` (load balancers), privada `10.0.2.0/24` (API) e isolada `10.0.3.0/24` (destinada ao banco)
- [x] Declarar `aws_internet_gateway` associado à VPC
- [x] Criar route table pública com rota padrão `0.0.0.0/0` para o Internet Gateway e associar a subnet pública
- [x] Criar route table privada para a subnet da API, sem rota padrão direta para o Internet Gateway
- [x] Criar ou reutilizar route table sem rota internet para a subnet do banco
- [x] Associar explicitamente cada subnet à sua route table e garantir que a subnet do banco não tenha rota direta para a internet
- [x] Criar SG da API e SG do banco
- [x] Criar entrada `5432` no SG do banco exclusivamente a partir do SG da API, nunca `0.0.0.0/0`
- [x] Criar bucket `securepay-financial-reports` com os 4 bloqueios (`block_public_acls`, `block_public_policy`, `ignore_public_acls`, `restrict_public_buckets`)
- [ ] Quando o ALB estiver ativo, restringir a entrada da API na porta `8080` exclusivamente pelo SG do ALB
- [x] Executar `init` → `validate` → `apply -auto-approve` → `plan -detailed-exitcode` com exit 0
- [x] Declarar ALB na subnet pública, target group para a API na porta `8080`, listener na porta `80` e health check em `/actuator/health`, mantendo-os comentados enquanto o emulador não suportar o serviço
- [ ] Reativar os recursos em um ambiente com suporte a `elbv2` e validar DNS, listener e health check

## Critérios de aceitação

**Laboratório**

- [x] `terraform plan -detailed-exitcode` retorna exit 0
- [x] Nenhuma regra de segurança expõe `5432` em `0.0.0.0/0`
- [x] Bucket privado com os 4 bloqueios; VPC e subnets com os CIDRs exatos
- [x] Subnet pública associada a route table com rota para o Internet Gateway
- [x] Subnets da API e do banco sem rota internet direta
- [x] ALB implementado no código e desativado/documentado no laboratório

**Arquitetura final (pendente de ambiente com `elbv2`)**

- [ ] SG da API aceita `8080` somente do SG do ALB
- [ ] ALB acessível via DNS público
- [ ] Health check do Target Group em `target healthy`

## Validação

- `terraform init`, `terraform validate` e `terraform plan -detailed-exitcode` com exit 0
- Inspeção das route tables confirmando a topologia de rotas descrita
- `awslocal ec2` / inspeção de security groups confirmando que `5432` só aceita o SG da API
- Conferência dos 4 bloqueios de acesso público no bucket
- Segundo `plan -detailed-exitcode` após qualquer mudança: exit 0 (sem drift)

## Evidências

- Output de `terraform plan -detailed-exitcode` (exit 0)
- Dump das route tables com as rotas de cada subnet
- Regras dos security groups mostrando a origem de `5432`
- Saída da inspeção dos bloqueios do bucket

## Limitações / notas

- **Lab ≠ real:** sucesso de `init`, `validate`, `plan` e `apply` prova que o emulador aceitou os recursos; **não prova** que a semântica de rede, roteamento e segurança da AWS real foi reproduzida
- `infra/platform/compose-localstack.yaml` declara `SERVICES=s3,ec2,elbv2` e exige `LOCALSTACK_AUTH_TOKEN` — qualquer automação que suba o emulador precisa respeitar essa configuração
- O LocalStack pode não validar toda a semântica de rota; as rotas são declaradas para o desenho ser fiel à arquitetura pretendida
- **Carry-over condicionado a ambiente com `elbv2`:** reativar `aws_alb`, `aws_alb_target_group` e `aws_alb_listener`; restringir a entrada da API ao SG do ALB (hoje a regra temporária `0.0.0.0/0` em `80`/`443` permanece documentada como lab-only). Nenhum item bloqueia a Issue 11
- Enquanto o ALB está desativado, a entrada temporária da API é de laboratório e não deve ser confundida com o desenho final de produção
