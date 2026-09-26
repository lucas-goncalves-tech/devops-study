---
aliases: [issue-15, pipeline-hardening]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 15 — Pipeline com permissões mínimas e ações pinadas por SHA

## Contexto

A pipeline vai concentrar permissões de escrita e chamar ações de terceiros por tag mutável. Qualquer comprometimento de uma action pública ou de um token sobre-permissivo transforma o CI no caminho mais curto até o repositório e até as credenciais de deploy.

## Objetivo

Estado final: cada job opera com a menor permissão possível, ações de terceiros estão pinadas por SHA imutável, e uma auditoria automatizada impede a reintrodução de tag mutável.

## Dependências

- Requer Issue 10 — não há jobs nem ações para auditar antes da pipeline existir

## Escopo

- Auditoria e aplicação de least-privilege por job
- Restrição de escrita a apenas os jobs que publicam algo
- Pinagem de ações de terceiros por SHA
- Auditoria automatizada contra tag mutável
- Política de atualização de SHA

## Fora de escopo

- Gates de conteúdo (segredos, SAST, SCA) — Issues 13, 14 e 16
- Deploy e proteção de ambiente — Issue 17
- Segredos de infraestrutura e credenciais de nuvem — Issue 12
- Kubernetes e Terraform

## Conhecimentos envolvidos

- Permissões do token de workflow e controle de acesso por job
- Ações de terceiros e supply chain de CI/CD
- Imutabilidade por SHA vs tag mutável

## Estado atual

- Jobs herdam permissão padrão do token, com poder além do necessário
- Ações referenciadas por tag, sujeita a movimento
- Nada impede reintrodução de tag mutável

## Resultado esperado

- Nenhum job com escrita desnecessária
- Toda action de terceiros referenciada por SHA fixa
- Auditoria falha quando uma tag mutável aparece
- Procedimento de atualização de SHA documentado

## Requisitos

- [ ] Auditar permissões de cada job e aplicar least-privilege
- [ ] Restringir escrita a apenas os jobs que publicam algo
- [ ] Pinar ações de terceiros por SHA imutável
- [ ] Criar auditoria que falha se tag mutável for usada
- [ ] Documentar a política de atualização de SHA

## Critérios de aceitação

- [ ] Todo job declara explicitamente suas permissões e nenhuma tem `write` sem necessidade declarada
- [ ] Nenhum job com permissão de escrita publica artefato
- [ ] Toda ação de terceiros no workflow está referenciada por SHA de 40 caracteres
- [ ] A auditoria falha de propósito quando uma tag mutável é introduzida
- [ ] O procedimento de atualização de SHA existe e é executável

## Validação

- Revisar a lista de permissões por job e confirmar a ausência de `write` supérfluo
- Varredura do workflow procurando referências por tag em actions de terceiros
- **Build to break:** trocar uma SHA por uma tag e confirmar que a auditoria falha; reverter
- Seguir o procedimento de atualização de SHA do zero e confirmar pipeline verde

## Evidências

- Tabela ou trecho de workflow com as permissões declaradas por job
- Saída da auditoria passando sobre o código atual
- Falha da auditoria com a tag mutável introduzida e revertida
- Registro da atualização de SHA executado

## Limitações / notas

- Actions oficiais do próprio GitHub também beneficiam de pinagem — avaliar caso a caso em vez de criar exceção ampla
- Pinagem por SHA exige atualização manual; sem a política documentada, a pipeline envelhece e a pinagem vira dívida
- Os critérios de "permissão mínima" são observados pela presença ou ausência de declaração por job, não por juízo sobre o grau de exposição
- Esta Issue endurece a pipeline criada na Issue 10 e é pré-requisito da proteção de ambiente da Issue 17
