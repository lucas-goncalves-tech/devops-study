---
aliases: [issue-08, staging-falho-observabilidade]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 08 — Staging separado com três falhas de observabilidade injetadas, diagnosticadas e corrigidas

## Contexto

O ambiente real vai subir na nuvem e nada foi feito para quebrar de propósito. Painel bonito sobre sistema que nunca falhou não é prova de observabilidade: é prova de que a observabilidade nunca teve o que mostrar. Enquanto as falhas forem hipotéticas, a resposta a elas é chute — e o chute em incidente custa mais caro do que a falha. A Issue 05 entrega `/metrics` com `prom-client` e os quatro painéis, e a Issue 07 põe o serviço em computação real; falta o ambiente errado, separado do certo, onde a falha é provocada, vista no painel, diagnosticada por escrito e consertada com prova de antes e depois.

## Objetivo

Estado final: um ambiente de staging claramente separado da produção, três falhas injetadas — latência artificial, pool de conexões esgotado e retenção de memória — cada uma detectada pelos painéis existentes, com diagnóstico escrito e conserto com evidência de antes e depois.

## Dependências

- Requer Issue 05 — a detecção de cada falha depende dos quatro painéis e das métricas de pool e memória que a Issue 05 entrega
- Requer Issue 07 — é a computação real que define o que é "produção" e, portanto, o que precisa ficar separado
- Requer Issue 02 — a stack (API + Postgres) que recebe a injeção é a orquestrada aqui

## Escopo

- Ambiente de staging separado da produção: credencial, banco, volume e URL próprios
- Três falhas injetadas por mecanismo declarado: latência artificial, pool de conexões esgotado, retenção de memória
- Detecção de cada falha pelo painel correspondente, com série que mostra a mudança
- Diagnóstico escrito por falha: sintoma, métrica que disparou, causa raiz
- Conserto com evidência antes/depois, incluindo a repetição da injeção depois do conserto, quando o painel já não degrada
- Registro do que o ambiente de staging custou e quando é descartável

## Fora de escopo

- Chaos engineering distribuído, biblioteca de experimentos e automação de falha em produção
- Alertas, SLO e escalonamento — fora desta Issue; a observabilidade que esta trilha entrega está na Issue 05
- Terraform novo para o staging: o ambiente nasce do que a Issue 07 já provisiona, com os recursos separados e os valores por ambiente parametrizados
- Staging inseguro de propósito — [Issue 11 do `webhook-gateway`](../../webhook-gateway/issues/11-staging-inseguro.md)
- Kubernetes e orquestração — fora de escopo por decisão, a Issue correspondente está arquivada em `archive/18-kubernetes-helm/`

## Conhecimentos envolvidos

- Separação de ambientes: credencial, dados e URL próprios, e o que isso impede
- Injeção de falha controlada por variável de ambiente e por limite de recurso
- Métricas de pool do driver `postgres` (postgres.js): ativas, ociosas e pendentes de espera contra o limite real de `max: 10`
- Métricas de processo Node: `process_resident_memory_bytes` e `process_heap_bytes` do `prom-client`
- Diagnóstico por timeline: sintoma, métrica, causa — e por que a correção funciona

## Estado atual

- A Issue 07 põe o serviço em computação real e nada foi preparado ao lado dela: uma falha aplicada na produção seria indistinguível de um defeito real
- Os quatro painéis da Issue 05 nunca viram uma curva de degradação: só viram tráfego saudável
- Latência, pool e memória são suspeitas clássicas de incidente que ninguém exercitou
- Nenhum registro de diagnóstico de falha existe para este app

## Resultado esperado

- Staging separado por credencial, dados e URL, identificável como staging em qualquer requisição
- Cada uma das três falhas vista no painel antes de qualquer intervenção
- Diagnóstico escrito e conserto com antes/depois para cada falha

## Requisitos

- [ ] Provisionar staging com URL, credencial de banco, volume e nome de ambiente próprios, a partir dos recursos da Issue 07
- [ ] Provisionar o staging pela mesma declaração da produção, com os valores do ambiente vindo de `variable`/`tfvars` ou da separação da Issue 07 — nenhum arquivo `.tf` duplicado para criar o staging
- [ ] Tornar o staging identificável de fora: nome de serviço, cabeçalho ou URL distintos, e nenhum recurso compartilhado com a produção
- [ ] Injetar latência artificial por mecanismo declarado (variável de ambiente lida pelo app), com valor e duração registrados
- [ ] Injetar esgotamento de pool de conexões por mecanismo declarado (limite de pool reduzido ou consulta lenta segurando conexão), com valor registrado
- [ ] Injetar retenção de memória por mecanismo declarado (intervalo que acumula sem liberar, ou cache sem limite), com taxa de crescimento registrada
- [ ] Aplicar a carga k6 de referência da Issue 05 como linha de base do painel antes de qualquer injeção
- [ ] Para cada falha: captura do painel no momento da detecção, com a série que mostra a mudança e o horário
- [ ] Para cada falha: diagnóstico escrito com sintoma observado, métrica que disparou e causa raiz
- [ ] Para cada falha: conserto com evidência de antes e depois (mesma injeção, mesmo painel, comportamento oposto) e registro do por que funciona
- [ ] Registrar o custo do staging e a regra de descarte do ambiente

## Critérios de aceitação

- [ ] A linha de base dos painéis com a carga de referência está registrada antes de qualquer injeção, para que a mudança seja comparável
- [ ] O staging responde em URL própria e nenhuma requisição nele toca banco, volume ou credencial da produção
- [ ] As três falhas são detectadas pelos painéis da Issue 05 — p95 para a latência, pendentes do pool para o esgotamento, RSS/heap para a memória — com série e horário arquivados
- [ ] Cada uma das três falhas tem diagnóstico escrito com sintoma, métrica e causa raiz
- [ ] Cada uma das três falhas tem evidência antes/depois: a mesma injeção deixa de degradar o painel depois do conserto
- [ ] O diagnóstico de cada falha cita a métrica do painel que a revelou, e não apenas uma linha de log da aplicação
- [ ] O conserto não é desligar a injeção: a repetição da injeção após o conserto é registrada e o comportamento é o esperado
- [ ] A regra de descarte do ambiente e o custo estimado do staging estão registrados
- [ ] Nenhum arquivo `.tf` é copiado entre produção e staging: o que difere é valor de ambiente, não declaração; quando o fallback local da nota de limitações for usado, a separação vale por compose e nome próprios

## Validação

- Consultar a URL do staging e a URL de produção, confirmando que dados de um não aparecem no outro
- Reaplicar cada injeção individualmente, observando o painel sem tocar em log de aplicação
- Ler o diagnóstico registrado e conferir se a métrica citada é a mesma que o painel mostrou
- Reaplicar a injeção depois do conserto e comparar as duas capturas do mesmo painel
- Conferir a lista de recursos do staging procurando qualquer recurso compartilhado com a produção

## Evidências

- URL do staging e da produção lado a lado, com requisição a cada uma
- Captura (ou exportação) de painel da linha de base antes da injeção
- Três capturas (ou exportações) de painel, uma por falha, com horário
- Três diagnósticos escritos, com sintoma, métrica e causa
- Três pares de captura antes/depois da mesma injeção
- Saída de `docker stats` ou equivalente com o crescimento de memória registrado
- Registro de custo e de descarte do ambiente

## Limitações / notas

- **Injeção por variável de ambiente é o mecanismo honesto:** uma chave `if (env.SLOW_MODE)` no app é feia para produção e é a forma mais rápida de tornar a falha reproduzível e reversível — o que importa é que ela exista como faca explícita, não escondida na lógica
- **O esgotamento de pool é a falha mais cara de detectar em log:** ela aparece como timeout do driver, não como erro de pool. O painel de pendentes de espera é o que fecha o diagnóstico
- **Consertar injeção não é desligar a injeção:** repetir a injeção depois do conserto é o que separa correção de cancelamento de sintoma
- Se o staging não couber no custo da Issue 07, um ambiente local com o mesmo compose e outro nome satisfaz os critérios de separação; o que não pode é ser o mesmo ambiente
- Cada falha injetada tem de ter um parceiro em produção: se a mesma mudança entrar em produção e o painel não mostra, a observabilidade falhou mesmo tendo passado pelos critérios
