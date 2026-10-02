---
aliases: [issue-09, pipeline-infra-apply]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: media
---

# Issue 09 — Identidade da pipeline e `apply` de infraestrutura com credencial federada

## Contexto

A Issue 07 tirou o estado do disco de quem aplicou: agora ele é remoto, versionado e travado. Mas quem pode mudar a infraestrutura continua indefinido — o `apply` sai da máquina de quem roda o comando, sem trilha de auditoria e sem separação entre quem propõe a mudança (pull request) e quem executa (máquina pessoal). Se a pipeline fosse aplicar hoje, o caminho mais curto seria colar uma access key da AWS num arquivo versionado: a credencial vira parte do repositório, sobrevive a qualquer revoke que alguém faça depois, e nenhum gate desta trilha a detecta.

## Objetivo

Estado final: a única forma de aplicar infraestrutura é a pipeline, identificada por credencial federada efêmera, só a partir de `main` e atrás de aprovação de ambiente; nenhuma chave AWS longa existe no repositório; cada aplicação tem autor, horário e commit recuperáveis pelo histórico da execução.

## Dependências

- Requer Issue 04 — o `apply` é um job do pipeline que a Issue 04 cria, e o `plan` verde dela é o que autoriza executá-lo
- Requer Issue 07 — sem estado remoto e lock, dois `apply` concorrentes da pipeline corrompem o estado que esta Issue passa a escrever

## Escopo

- Identidade federada da pipeline: `provider` e IAM Role com política de confiança escopada no repositório e na branch
- Obtenção de credencial efêmera por `id-token` no job, sem access key estática
- Job de `apply` restrito a `main`, atrás de environment com aprovação
- `plan` verde do mesmo commit como pré-requisito do `apply`
- Verificação de drift logo após a aplicação

## Fora de escopo

- Provisionamento, seleção de recursos e estimativa de custo — Issue 07
- Deploy da aplicação em computação — [Issue 10](10-deploy-ec2-pipeline.md)
- Recursos novos de infraestrutura além dos declarados na Issue 03 e provisionados na Issue 07
- Gates de conteúdo da pipeline (segredos, SAST, SCA) — não são pré-requisito desta Issue
- Rollback de infraestrutura por revert de HCL — esta Issue entrega aplicação controlada e drift provado

## Conhecimentos envolvidos

- Identidade federada: `provider` OIDC, `AssumeRoleWithWebIdentity` e a cadeia de confiança
- Política de confiança com escopo de repositório e `sub` de branch — quem pode assumir o quê
- Permissão mínima do job: `id-token: write` só onde há troca de credencial
- Environments do repositório e aprovação como barreira de execução
- Drift: `plan` logo após `apply` como prova de que o estado reflete o repositório

## Estado atual

- `terraform apply` roda de quem executa o comando, com credencial dessa pessoa
- Nenhuma identidade de pipeline existe: o CI não tem como assumir papel nenhum
- Se a pipeline fosse aplicar, precisaria de access key versionada ou de credencial em log
- Não há registro de quem aplicou qual mudança, nem em que commit

## Resultado esperado

```text
merge em main → plan verde → aprovação → apply identificado → plan sem drift
```

- `apply` só acontece pela pipeline, nunca por credencial de pessoa
- Nenhuma chave AWS longa em arquivo versionado
- Execução fora de `main` ou sem aprovação não aplica nada
- Log da execução sem credencial
- `plan` imediatamente após o `apply` não encontra mudança

## Requisitos

- [ ] Declarar IAM Role com política de confiança cujo principal é a identidade federada do provedor de CI, restringida ao repositório e à branch `main`
- [ ] Configurar o job para obter credencial efêmera via `id-token`, sem access key estática em secret, arquivo ou variável
- [ ] Restringir o job de `apply` a `main` e a um environment com aprovação declarada
- [ ] Exigir o job de `plan` verde do mesmo commit antes do `apply`
- [ ] Passar os valores de ambiente ao `plan` e ao `apply` por `-var`/`TF_VAR_` vindos do environment do repositório (segredo ou `vars.*`), sem valor literal colado no YAML da pipeline
- [ ] Aplicar somente a infraestrutura já declarada na Issue 03 e estimada na Issue 07 — nenhum recurso novo
- [ ] Rodar `terraform plan -detailed-exitcode` logo após o `apply`, com exit 0
- [ ] Manter o caminho manual de `apply` da Issue 07 documentado e funcional — a pipeline não pode ser a única via de recuperação

## Critérios de aceitação

- [ ] Executar o workflow de `apply` a partir de uma branch diferente de `main` não aplica nada — build to break: disparar em branch e observar a recusa
- [ ] Um workflow disparado sem aprovação do environment não executa o `apply`
- [ ] Varredura do repositório não encontra `aws_access_key_id` nem `aws_secret_access_key` em nenhum arquivo versionado
- [ ] O log da execução do `apply` não contém credencial
- [ ] O `terraform plan -detailed-exitcode` logo após o `apply` retorna exit 0
- [ ] Nenhum valor de ambiente (CIDR, tipo de instância, credencial) aparece literal no workflow: o YAML declara a origem do valor, não o valor
- [ ] O histórico da execução recupera commit, autor e horário da última mudança de infraestrutura

## Validação

- Disparar o workflow fora de `main` e confirmar que nenhum `apply` acontece; reverter
- Disparar sem aprovação e confirmar que o job espera ou falha antes de aplicar
- Varredura por padrões de access key em todos os arquivos versionados
- Conferir a política de confiança confirmando o escopo de repositório e branch
- Rodar `plan` após o `apply` e capturar o exit 0
- Inspecionar o log da execução procurando credencial

## Evidências

- Log da tentativa fora de `main` sem aplicação
- Log da execução aguardando aprovação do environment
- Saída da varredura de access keys sem achado
- Trecho do log do `apply` sem credencial
- Output do `plan` pós-apply com exit 0
- Página do histórico da execução com commit, autor e horário

## Limitações / notas

- Requer conta de nuvem real: a obtenção de credencial federada não é reproduzível no emulador — a prova do federado é o único critério desta Issue que depende de ambiente real, e isso registra aqui em vez de virar evidência pela metade. Custa `0`: `STS` e o `provider` de federado não geram cobrança, e os recursos aplicados são os já estimados na Issue 07
- Contra o emulador local a credencial é mockada; sucesso no laboratório prova que o job roda, não que a identidade federada existe
- **Higiene de segredo é barreira desta Issue, não pré-requisito de outra trilha:** a varredura por access key é critério de aceitação aqui. Se o scanner de segredos da trilha DevSecOps existir quando esta Issue rodar, ele reforça a mesma barreira — referência, nunca dependência
- Uma Role nova não pode apagar o caminho manual: se a pipeline cair, o `apply` da Issue 07 continua sendo a via de recuperação e precisa continuar documentado
- A política de confiança ampla demais (`*` em repositório ou branch) anula o controle desta Issue — escopar no repositório e em `main` é o critério, não um detalhe de implementação
