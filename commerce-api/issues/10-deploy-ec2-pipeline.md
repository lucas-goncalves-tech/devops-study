---
aliases: [issue-10, deploy-ec2-pipeline]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 10 — Deploy da aplicação em computação real por pipeline, com rollback por healthcheck

## Contexto

A Issue 07 põe a API e o banco em computação real, mas nenhum caminho define como o código chega lá. Sem esse caminho, publicar é conectar na máquina e rodar comando na mão: não há versão registrada, não há rollback e ninguém responde em 30 segundos por o que está servindo tráfego agora. A imagem do `Dockerfile` já existe desde a Issue 02 e a pipeline desde a Issue 04 — falta o trecho que transforma merge em versão em produção.

## Objetivo

Estado final: cada merge com pipeline verde publica uma imagem imutável e a faz subir na computação da Issue 07 sozinha, com healthcheck pós-deploy e rollback automático para a versão anterior quando a saúde falha, e com versão, autor e horário recuperáveis.

## Dependências

- Requer Issue 07 — a computação real é o destino do deploy
- Requer Issue 04 — pipeline, gates de merge e proteção de branch
- Requer Issue 02 — o `Dockerfile` e o `docker-compose.yaml` que a imagem publicada e o serviço da EC2 consomem

## Escopo

- Build da imagem versionada na pipeline e publicação em registro com digest imutável
- Deploy via SSH com chave efêmera, sem credencial em log
- Pull do digest declarado na máquina de destino, sem build na produção
- Healthcheck pós-deploy com rollback automático
- Registro auditável de versão, autor e horário

## Fora de escopo

- Provisionamento da máquina, rede e custo — Issue 07
- `terraform apply` e identidade da pipeline de infraestrutura — [Issue 09](09-pipeline-infra-apply.md)
- TLS e balanceamento — ALB e security group da Issue 03 e da Issue 07
- Gates de conteúdo da pipeline (segredos, SAST, SCA) — não são pré-requisito desta Issue
- Rollout blue-green com duas versões simultâneas — esta Issue entrega deploy com rollback, não dual-run
- Migração de schema de banco pelo deploy — o schema é tratado pela aplicação, não por passo de pipeline

## Conhecimentos envolvidos

- Registros de imagem, tags e digest imutável como versão de deploy
- SSH com chave efêmera em automação, com segredo fora do repositório
- Healthcheck como gate de imediatismo: a versão só conta se responde
- Rollback por digest anterior e por que ele é mais barato que rebuild
- Deploy e build como jobs separados: produção nunca constrói

## Estado atual

- A computação da Issue 07 existe sem caminho de publicação declarado
- Publicar depende de conectar na máquina e executar comando manual
- Não há versão registrada da aplicação em execução
- Rollback é refazer na mão o que foi feito na mão

## Resultado esperado

```text
merge verde → imagem publicada com digest → pull na EC2 → healthcheck
                                                     ├─ UP     → versão implantada
                                                     └─ falhou → rollback ao digest anterior
```

- Só pipeline verde publica e deploya
- A produção não compila nada: todo código chega por pull da imagem
- Healthcheck falhando reverte sozinho para o digest anterior
- Versão, autor e horário recuperáveis em 30 segundos

## Requisitos

- [ ] Job da pipeline publica a imagem com tag derivada do commit e registra o digest
- [ ] Deploy roda só em `main` com pipeline verde, usando chave SSH efêmera guardada em secret de environment
- [ ] O deploy na máquina de destino é `pull` do digest declarado e subida pelo Compose — nenhum `build` na produção
- [ ] Entregar os segredos de produção fora do repositório e fora de log: `JWT_SECRET` novo (32+ caracteres, distinto do valor de desenvolvimento), `DATABASE_URL` do banco da Issue 07 e `CORS_ORIGIN` restrito ao domínio real — os defaults do `.env.example` nunca chegam à máquina de destino
- [ ] Healthcheck pós-deploy em `/health` exigindo HTTP 200 e `"status":"UP"`
- [ ] Rollback automático para o digest anterior quando o healthcheck falha
- [ ] Registro de auditoria com digest implantado, autor do merge e horário
- [ ] A chave SSH não aparece em nenhum log de execução

## Critérios de aceitação

- [ ] Pull request com pipeline vermelha não publica imagem nem deploya
- [ ] Um deploy disparado manualmente fora de `main` não acontece pelo caminho de produção
- [ ] Nenhum log de execução contém chave SSH ou senha
- [ ] A máquina de destino não roda com valor de desenvolvimento: `JWT_SECRET` distinto do `.env.example` e `CORS_ORIGIN` sem `*` — build to break: apontar para o valor de dev e observar a diferença
- [ ] Deploy de uma versão com `/health` falhando reverte sozinho para o digest anterior e a API volta a responder `200` `UP` — build to break
- [ ] Deploy de uma versão saudável permanece em execução — build to defend
- [ ] A versão implantada, o autor e o horário são recuperáveis em até 30 segundos
- [ ] A máquina de destino não contém código construído localmente: o que roda corresponde ao digest publicado

## Validação

- Abrir pull request com a suíte falhando e confirmar que nada é publicado
- Implantar deliberadamente uma versão com `/health` quebrado e observar o rollback automático; reverter
- Implantar a versão saudável e confirmar que ela permanece
- Varredura dos logs de execução procurando chave
- Conferir na máquina de destino o `JWT_SECRET` e o `CORS_ORIGIN` em uso, sem valores de desenvolvimento
- Consultar o registro de auditoria cronometrando a recuperação da versão, autor e horário
- Conferir na máquina de destino que o código em execução é o da imagem puxada, não um build local

## Evidências

- Log da pipeline publicando a imagem com o digest
- Log do deploy com o digest implantado
- Log do rollback automático com a versão revertida
- Trecho de log de execução sem credencial
- Configuração de ambiente da máquina de destino com segredos de produção e sem valores de dev
- Registro de auditoria com digest, autor e horário
- Comparação entre digest publicado e imagem em execução no destino

## Limitações / notas

- A máquina de destino vem da Issue 07, cuja trava de custo vale aqui: deploy não provisiona recurso novo, só publica o que já existe
- **Healthcheck é o critério de imediatismo, não de perfeição:** `/health` responde `503` com `"status":"DEGRADED"` quando o banco cai, então um deploy durante indisponibilidade do banco reverte. Isso é o comportamento correto — registrar o caso em vez de afrouxar o critério
- A porta é a `3000` (`PORT` da `commerce-api`); o `EXPOSE` do `Dockerfile` e o healthcheck do Compose precisam continuar coerentes com ela
- A chave SSH é secret de environment do repositório — fora do repositório versionado, com revogação simples; se algum dia existir identidade federada para acesso à máquina, migrar não altera esta Issue
- O registro de imagem é gratuito para esta carga; trocar de registro muda só a configuração do job, nunca o contrato desta Issue
- Rollout e canary não existem aqui: rollback por digest é a estratégia declarada, e ela precisa ser provada com build to break para valer
