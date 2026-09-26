---
aliases: [issue-09, db-backups-s3]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 09 — Backup off-site com retenção e restore provado

## Contexto

O banco roda com volume local numa VPS econômica. Perda total do servidor significa perda total dos dados — e um backup que nunca foi restaurado não é backup, é esperança.

## Objetivo

Estado final: dump diário comprimido, criptografado e enviado para storage fora do servidor com retenção definida, e um restore completo executado do zero medindo o tempo de recuperação.

## Dependências

- Requer Issue 08 — o banco isolado é o alvo do dump

## Escopo

- Rotina agendada de dump comprimido em horário de baixo tráfego
- Envio para storage off-site com retenção definida
- Criptografia do backup em repouso
- Teste de restore em ambiente limpo e definição de RPO/RTO

## Fora de escopo

- Provisionamento da infraestrutura de nuvem e do Terraform — Issues 11 e 12
- Bucket e IAM da Issue 11 — são para relatórios financeiros, com outro ciclo de retenção e outra política de custo
- Monitoramento e alertas — Issue 06
- Kubernetes — Issue 18

## Conhecimentos envolvidos

- `pg_dump` e `pg_restore`
- Storage com retenção e criptografia em repouso
- RPO e RTO

## Estado atual

- Sem backup: dado único na VPS
- Backup eventualmente existe, mas restore nunca foi testado

## Resultado esperado

- Backup diário off-site com retenção
- 100% dos dados críticos restaurados dentro do RTO medido
- Runbook de emergência de uma página, executável

## Requisitos

- [ ] Agendar dump comprimido em horário de baixo tráfego
- [ ] Enviar para storage off-site com retenção definida
- [ ] Criptografar o backup em repouso
- [ ] Testar restore em ambiente limpo e medir o tempo
- [ ] Definir RPO e documentar a emergência em 1 página

## Critérios de aceitação

- [ ] Um dump é gerado, comprimido, criptografado e entregue fora do servidor a cada dia
- [ ] A retenção configurada é observável: objetos antigos expiram conforme definido
- [ ] Restore completo em ambiente limpo recupera 100% dos dados críticos dentro do RTO medido
- [ ] O runbook cabe em 1 página e foi executado seguindo apenas o que está escrito

## Validação

- Inspecionar o job agendado e o último artefato gerado
- Confirmar a presença do artefato no storage off-site e a retenção configurada
- Restaurar em ambiente limpo cronometrando o tempo total e contando os registros
- **Build to break:** apagar o volume local e restaurar apenas a partir do storage off-site
- **Build to defend:** a aplicação sobe sobre o banco restaurado e responde `/actuator/health` com `UP`

## Evidências

- Último dump gerado com data, tamanho e destino
- Lista do storage off-site mostrando a retenção
- Tempo medido de restore e contagem de registros recuperados
- Runbook de 1 página
- Confirmação de que a aplicação subiu sobre o banco restaurado

## Limitações / notas

- O destino é "storage off-site" — qualquer S3-compatible (LocalStack, AWS, Backblaze, Wasabi, R2) ou repositório remoto satisfaz. Provisionar o destino é parte desta Issue
- O bucket da Issue 11 não pode ser reusado: a política de IAM é escopada para `s3:PutObject`/`GetObject`/`DeleteObject` em `securepay-financial-reports` e o ciclo de vida expira versões antigas de relatório — misturar os dois workloads corrompe o controle de custo que a Issue 11 existe para estabelecer
- Backup em disco local **na mesma VPS** não satisfaz "off-site": não cobre perda total do servidor
- RPO e RTO são definição de negócio, não de ferramenta — precisam ser fixados antes de configurar a retenção
- O banco alvo é o serviço `database` do Compose, acessível apenas da rede isolada
