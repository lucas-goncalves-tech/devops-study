---
aliases: [issue-06, s3-reports-infra]
tags: [tracker, issue, parked, study-needed]
status: parked
prioridade: alta
---

# Issue 06 — Storage S3 para relatórios com IAM least privilege e endpoint privado

## Contexto

O bucket de relatórios financeiros já tem dono do lado do código: a porta `ReportRepository` e suas implementações (`S3ReportRepository` / `NoOpReportRepository`, controladas por `S3_ENABLED`) vivem no `ledger-service/app/`, e estão desativadas por padrão (`S3_ENABLED=false`). Falta o lado de infraestrutura desta trilha: sem identidade IAM, sem contrato de variáveis publicado e sem proteções no bucket, relatório financeiro não tem onde ser gravado com segurança.

## Objetivo

Estado final: bucket versionado, criptografado e com retenção; identidade IAM com política restrita ao bucket; endpoint de rede privada para o S3; e o contrato das quatro variáveis de ambiente publicado — tudo por IaC, sem tocar em código de aplicação.

## Dependências

- Requer Issue 03 — o bucket, o route table privado e a VPC foram declarados lá

## Escopo

- Identidade IAM (Role, política escopada no bucket) e prova de least privilege
- Versionamento, criptografia em repouso e regra de ciclo de vida do bucket
- Publicação do contrato das variáveis de ambiente que o app lê
- Endpoint Gateway do S3 associado à route table privada
- Validação determinística via `awslocal`

## Fora de escopo

- Modificar a porta `ReportRepository` e suas implementações (`S3ReportRepository`, `NoOpReportRepository`, configuração S3) — esse código é do `ledger-service/app/`, não desta app
- Gerar conteúdo de relatório
- CI/CD — Issue 04
- ALB — Issue 03 (carry-over condicionado a `elbv2`)
- Backup de banco — [Issue 05 do `ledger-service`](../../ledger-service/issues/05-db-backups-s3.md)
- Criação de lanes de bugs no tracker

## Conhecimentos envolvidos

- IAM: principal, Role vs User, política escopada
- S3: versionamento, criptografia server-side, regras de ciclo de vida
- Endpoints Gateway de VPC e prefix lists
- Cobertura de IAM no emulador local

## Estado atual

- No `ledger-service`, `S3_ENABLED=false` por padrão, então a implementação NoOp da porta de relatórios está ativa
- O bucket `securepay-financial-reports` existe, mas ninguém tem permissão para usá-lo
- O emulador não expõe o serviço IAM
- Não existe nenhum `aws_vpc_endpoint` em código, e a route table privada que o endpoint vai precisar só virá a existir com a Issue 03, por desenho e não por esquecimento

## Resultado esperado

- `Put` e `Get` funcionam apenas no bucket alvo; fora dele a operação é negada
- Bucket versionado, criptografado e com expiração de versões antigas
- O app consumidor (`ledger-service`) configurável 100% pelas quatro variáveis de ambiente, sem hardcode
- `aws_vpc_endpoint` declarado e associado à route table privada

## Requisitos

- [ ] Em `commerce-api/infra/provider.tf`, adicionar `iam = "http://localhost:4566"` ao bloco `endpoints { }`
- [ ] Em `commerce-api/infra/platform/compose-localstack.yaml`, adicionar `iam` à variável `SERVICES` e `ENFORCE_IAM=1` ao bloco `environment`
- [ ] Reiniciar o emulador — `SERVICES` só é lido na inicialização
- [ ] Conferir que `endpoints { }` do Terraform lista exatamente os serviços que `SERVICES=` inicializou
- [ ] Declarar IAM **Role** (não User) com `assume_role_policy` para o principal que roda o app,
  anotando no comentário do arquivo que no laboratório a Role não é assumida por nenhuma
  instância real (não há instância) — ela é declarada para o desenho ser fiel
- [ ] Declarar `aws_iam_policy` com `Effect: Allow` apenas para `s3:PutObject`, `s3:GetObject` e `s3:DeleteObject` no ARN de `securepay-financial-reports`, nunca `Resource: "*"`
- [ ] Associar a policy à Role via `aws_iam_role_policy_attachment`
- [ ] Criar um User descartável **só** para a prova, com a mesma policy — artefato de laboratório, não o entregável
- [ ] Exportar as access keys desse User e fazer `put-object` no bucket alvo (esperado: sucesso)
- [ ] Repetir o `put` contra um bucket que não é o alvo (esperado: `AccessDenied`)
- [ ] Repetir com `get-object` no bucket alvo (esperado: sucesso)
- [ ] Reusar o bucket `securepay-financial-reports` já declarado em `commerce-api/infra/s3.tf`
- [ ] Declarar `aws_s3_bucket_versioning` como recurso próprio do provider `aws` v5
- [ ] Declarar `aws_s3_bucket_server_side_encryption_configuration` com `sse_algorithm = "AES256"`
- [ ] Declarar `aws_s3_bucket_lifecycle_rule` expirando versões antigas
- [ ] Publicar o contrato das quatro variáveis que o app consumidor lê: `S3_ENABLED`, `S3_BUCKET_NAME`, `AWS_REGION`, `S3_ENDPOINT_URL` (o contrato do `ledger-service`, que é quem implementa a porta de relatórios)
- [ ] Declarar `aws_vpc_endpoint` com `vpc_endpoint_type = "Gateway"` e o `service_name` do S3 da região
- [ ] Associar o endpoint à route table privada da subnet da API
- [ ] Criar security group permitindo tráfego S3 vindo da subnet privada onde roda o app

## Critérios de aceitação

- [ ] `terraform validate` passa e `terraform plan -detailed-exitcode` retorna exit 0
- [ ] `Put` e `Get` funcionam no bucket alvo e `Put` fora dele retorna `AccessDenied` — com a ressalva do `ENFORCE_IAM` registrada se a negação não ocorrer
- [ ] `get-bucket-acl` e `get-bucket-public-access-block` confirmam zero acesso público
- [ ] `get-bucket-versioning` e `get-bucket-encryption` confirmam versionamento e AES256
- [ ] As **4** variáveis de ambiente estão documentadas e o app consumidor sobe sem nenhuma delas hardcoded
- [ ] `aws_vpc_endpoint` declarado **e associado** à route table privada

## Validação

- `terraform init`, `validate` e `plan -detailed-exitcode` com exit 0
- `awslocal s3 ls` acessando o bucket
- `put`/`get` com as credenciais do User descartável no bucket alvo e `AccessDenied` fora dele
- `awslocal s3api get-bucket-acl`, `get-bucket-public-access-block`, `get-bucket-versioning` e `get-bucket-encryption`
- Inspecionar a route table privada confirmando a associação do endpoint

## Evidências

- Output de `terraform plan -detailed-exitcode` (exit 0)
- Resultado dos comandos de `put`/`get` dentro e fora do alvo
- Saída das inspeções do bucket (ACL, bloqueios, versionamento, criptografia)
- Route table com o endpoint associado
- Contrato das variáveis publicado

## Limitações / notas

- **Status: parked.** O trabalho de provisionar a identidade IAM foi desfeito e depois apagado junto com a infraestrutura inteira (`commerce-api/infra/provider.tf` e `commerce-api/infra/platform/compose-localstack.yaml` não existem mais; histórico no git) — este card voltou ao estado inicial e a entrega foi adiada para depois das trilhas de VPS, backup e CI. Nenhuma outra Issue depende dela — `S3_ENABLED` continua `false` e a implementação NoOp da porta de relatórios responde
- **Lab ≠ real:** no LocalStack o app fala com `localhost:4566` na máquina host. As subnets são objetos declarados sem núcleo de rede real e **não há como observar tráfego**. A prova do endpoint é `plan` limpo com o recurso aceito pelo emulador — declarado ≠ funcionando
- **`ENFORCE_IAM` é feature Pro e está desabilitada por padrão.** Sem ela, nenhuma API do emulador nega nada. Se a prova de negação não produzir `AccessDenied`, o cenário correto é rebaixar essa parte para "declarar e inspecionar a política" e marcar a prova de negação como **bloqueada por ambiente**, não como falha
- **S3 com versionamento sem expiração faz o bucket crescer para sempre:** `deleteObject` vira *delete marker* e o objeto anterior continua faturando — por isso a regra de ciclo de vida é obrigatória
- **`delete-object` fora do alvo não prova nada:** o LocalStack tem `s3:DeleteObject` sem cobertura de negação testada, então esse resultado não serve de evidência — quem rodar deve registrá-lo como inspeção, nunca como prova de least privilege
- **Variáveis que o app não lê:** `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` não são lidas em lugar nenhum do `ledger-service`. A configuração S3 dele injeta credenciais fixas quando há endpoint. Servem para a prova manual, não para o app
- **Dívida conhecida (código do `ledger-service`, não desta trilha):** a implementação S3 da porta de relatórios monta a chave em `tipo + "/" + id` enquanto a leitura usa apenas `id` — um `save` seguido de leitura pelo id não encontra o objeto. Corrigir quando uma Issue de código for liberada lá. Não bloqueia nenhuma etapa: tudo é validado por `awslocal`
