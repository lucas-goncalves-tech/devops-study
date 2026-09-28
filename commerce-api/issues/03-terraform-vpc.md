---
aliases: [issue-03, terraform-vpc]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 03 — Rede multi-tier declarada em Terraform com banco isolado e bucket privado

## Contexto

A infraestrutura existe só como estado manual: não é reproduzível, não é versionada e não há como provar que o desenho não mudou por acidente. O laboratório LocalStack permite validar arquitetura de nuvem sem custo.

## Objetivo

Estado final: rede, roteamento, segurança de grupo e storage declarados em HCL idempotente contra um emulador local, com o banco isolado por rota e por security group, e um bucket privado. O que é validado no laboratório fica separado do que só existe em AWS real.

## Dependências

- Nenhuma dependência de outra Issue — ponto de entrada da trilha de cloud; a Issue 04 (pipeline) e a Issue 06 (bucket com IAM) consomem o que é declarado aqui

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

Estado inicial vazio: `commerce-api/infra/` não existe. Não há nenhum arquivo Terraform neste app — a rede, o bucket e o ALB precisam ser declarados do zero.

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

Arquivos que esta Issue cria em `commerce-api/infra/` — nenhum deles existe hoje. A tabela é o
**contrato de saída**: cada arquivo e cada observação abaixo é um requisito que a Issue precisa
satisfazer quando criar o arquivo, não um inventário do que já está lá.

| Arquivo | Conteúdo esperado | Observação (o arquivo criado deverá…) |
|---|---|---|
| `commerce-api/infra/platform/compose-localstack.yaml` | Emulador local com `SERVICES=s3,ec2,elbv2` e `LOCALSTACK_AUTH_TOKEN` obrigatório | Sem a variável de token o Compose aborta: o job de IaC da Issue 04 precisa fornecê-la |
| `commerce-api/infra/provider.tf` | Provider AWS + LocalStack, endpoints `s3`/`ec2` | Manter `elbv2` comentado até o emulador suportar o serviço |
| `commerce-api/infra/vpc.tf` | VPC, três subnets, IGW e route tables | IGW e route tables implementados; ALB comentado até o carry-over |
| `commerce-api/infra/security.tf` | SG da API e SG do banco | SG do ALB comentado; entrada da API temporária e de laboratório até o ALB existir |
| `commerce-api/infra/s3.tf` | Bucket e quatro bloqueios de acesso público | Atende ao escopo desta Issue |

A infraestrutura que existia antes desta Issue foi apagada junto com a migração (histórico no git).
A forma dela serviu de referência para o desenho acima, mas **nada dela é reaproveitado como está**:
se um arquivo recriado divergir do que esta tabela exige, vale a tabela.

- [ ] Declarar provider `hashicorp/aws` `~> 5.0`, endpoints `ec2` e `s3` em `http://localhost:4566`, região `sa-east-1`, credenciais mock com `skip_credentials_validation` e `skip_requesting_account_id`
- [ ] Subir emulador local com o `commerce-api/infra/platform/compose-localstack.yaml` que esta Issue cria, expondo a lista de serviços que ele declara, e confirmar o endpoint respondendo
- [ ] Criar VPC `10.0.0.0/16`
- [ ] Criar subnet pública `10.0.1.0/24` (load balancers), privada `10.0.2.0/24` (API) e isolada `10.0.3.0/24` (destinada ao banco)
- [ ] Declarar `aws_internet_gateway` associado à VPC
- [ ] Criar route table pública com rota padrão `0.0.0.0/0` para o Internet Gateway e associar a subnet pública
- [ ] Criar route table privada para a subnet da API, sem rota padrão direta para o Internet Gateway
- [ ] Criar ou reutilizar route table sem rota internet para a subnet do banco
- [ ] Associar explicitamente cada subnet à sua route table e garantir que a subnet do banco não tenha rota direta para a internet
- [ ] Criar SG da API e SG do banco
- [ ] Criar entrada `5432` no SG do banco exclusivamente a partir do SG da API, nunca `0.0.0.0/0`
- [ ] Criar bucket `securepay-financial-reports` com os 4 bloqueios (`block_public_acls`, `block_public_policy`, `ignore_public_acls`, `restrict_public_buckets`)
- [ ] Quando o ALB estiver ativo, restringir a entrada da API na porta `3000` (`PORT` da `commerce-api`) exclusivamente pelo SG do ALB
- [ ] Executar `init` → `validate` → `apply -auto-approve` → `plan -detailed-exitcode` com exit 0
- [ ] Declarar ALB na subnet pública, target group para a API na porta `3000`, listener na porta `80` e health check em `/health` (rota real da `commerce-api`, que já responde `200` com `"status":"UP"` e `503` com `"DEGRADED"`), mantendo-os comentados enquanto o emulador não suportar o serviço
- [ ] Reativar os recursos em um ambiente com suporte a `elbv2` e validar DNS, listener e health check

## Critérios de aceitação

**Laboratório**

- [ ] `terraform plan -detailed-exitcode` retorna exit 0
- [ ] Nenhuma regra de segurança expõe `5432` em `0.0.0.0/0`
- [ ] Bucket privado com os 4 bloqueios; VPC e subnets com os CIDRs exatos
- [ ] Subnet pública associada a route table com rota para o Internet Gateway
- [ ] Subnets da API e do banco sem rota internet direta
- [ ] ALB implementado no código e desativado/documentado no laboratório

**Arquitetura final (pendente de ambiente com `elbv2`)**

- [ ] SG da API aceita `3000` somente do SG do ALB
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

- **Recomeço do zero:** a infraestrutura anterior foi apagada (histórico no git); esta Issue começa do zero em `commerce-api/infra/`
- **Lab ≠ real:** sucesso de `init`, `validate`, `plan` e `apply` prova que o emulador aceitou os recursos; **não prova** que a semântica de rede, roteamento e segurança da AWS real foi reproduzida
- O `commerce-api/infra/platform/compose-localstack.yaml` criado por esta Issue deverá declarar `SERVICES=s3,ec2,elbv2` e exigir `LOCALSTACK_AUTH_TOKEN` — qualquer automação que suba o emulador (inclusive o job de IaC da Issue 04) precisa respeitar essa configuração
- O LocalStack pode não validar toda a semântica de rota; as rotas são declaradas para o desenho ser fiel à arquitetura pretendida
- **Carry-over condicionado a ambiente com `elbv2`:** reativar `aws_alb`, `aws_alb_target_group` e `aws_alb_listener`; restringir a entrada da API ao SG do ALB. Enquanto o ALB não existe no laboratório, a entrada da API é temporária e **deve** ficar declarada como lab-only — a porta do serviço é a `3000` (`PORT` da `commerce-api`), nunca `80`/`443`, e nenhuma delas pode ficar aberta em `0.0.0.0/0` como estado final. Nenhum item bloqueia a Issue 06
- Enquanto o ALB está desativado, a entrada temporária da API é de laboratório e não deve ser confundida com o desenho final de produção
