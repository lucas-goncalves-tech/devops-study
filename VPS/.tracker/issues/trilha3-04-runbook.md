---
aliases: [trilha3-04, runbook]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 04 — Runbook e uptime: o que fazer quando o alerta toca

## Contexto

A Trilha 3 deu logs (T3-01), métricas (T3-02) e alertas (T3-03) — falta a parte que
transforma sinal em ação: **quando o alerta chega, o que eu faço?** Sem resposta escrita,
o alarme vira ansiedade: a pessoa abre os 6 dashboards, tenta coisa na ordem que lembra,
e descobre o procedimento durante a crise. Runbook é o procedimento **antes** da crise;
e como quase todo "incidente" de lab é provocado ou recorrente, dá para exercitar: a issue
termina com um **drill** — alerta disparado, seguido do runbook à risca, com o tempo
medido. Uptime visível (uma página simples) fecha o pacote: estado da stack respondendo
pergunta de cima, sem logar em servidor.

## Objetivo

Estado final: `RUNBOOK.md` no repo com ao menos 3 procedimentos (alvo caído, disco cheio,
5xx em rajada — os mesmos alertas da T3-03), cada um com sintoma → causa provável →
comandos → escalação; um drill executado seguindo o runbook (tempo até diagnóstico
registrado); e página de status que responde `ok/degraded` consultando o Prometheus,
acessível por loopback/tunnel.

## Dependências

- **Requer Trilha3-03** — o runbook nasce **dos** alertas que existem: sem
  `ciclo-firing-resolved` funcionando, não há gatilho para o procedimento.
- **pré-condição verificável:** as 3 regras da T3-03 `health: ok` e um ciclo firing/
  resolved já entregue (evidência da issue anterior).

- **estudo par:** `estudos/trilha3-04-runbook.md` — ler antes de executar (é o currículo desta issue)

## Escopo

- `RUNBOOK.md` na raiz do repo (ou `docs/runbooks/`): um procedimento por alerta da
  T3-03, formato fixo: **sintoma** (o que chega) → **diagnóstico** (3–5 comandos na ordem,
  começando pelo mais barato) → **mitigação** (o que restaura) → **escalação** (quando
  desistir/é além do lab)
- Drill: executar 1 procedimento de ponta a ponta com cronômetro (alerta → diagnóstico →
  mitigação → resolved), registrar o tempo e o que o runbook não cobriu
- Página de status simples: endpoint/script que consulta `up`/health do Prometheus e
  devolve `ok|degraded` — servida em loopback (sem nova regra ufw)
- Registro de "o que faltou no runbook" como melhoria versionada (o drill é teste do
  runbook, não teatro)
- **assume pronto:** `regras-versionadas`, `alertmanager-entregando` (T3-03)
- **entrega:** `runbook-3-procedimentos`, `drill-executado`, `status-page`

## Fora de escopo

- Post-mortem formal / timeline de incidente real — não há produção; o drill gera o
  registro próximo disso
- Status page pública (statuspage.io, domínio) — estágio futuro com VPS real
- Plantão/escalação humana de verdade — não existe time; a seção "escalação" é o limite
  do que se tenta sozinho
- Auto-remediação (heal automático do alvo) — o rollback da T2-04 já deu o exemplo em
  deploy; aqui é procedimento humano

## Conhecimentos envolvidos

- Runbook vs. documentação: procedimento executável, não explicação
- Diagnóstico em ordem de custo: sintoma → estado geral → componente → detalhe
  (`ps` antes de `logs` antes de `tcpdump`)
- Drill como teste: medir tempo até diagnóstico, anotar o que faltou (runbook é produto,
  drill é review)
- Uptime "de cima": o que uma página de status pode e não pode provar (ela só sabe o
  que consulta)
- Escalação: saber o ponto onde insistir é pior que reportar

## Estado atual

- 3 alertas entregando (T3-03) e nenhum procedimento escrito — quem recebe, improvisa
- Nenhum exercício feito: o caminho alerta→resolução nunca foi percorrido de propósito
- Estado da stack só visível logando na VM ou abrindo Grafana

## Resultado esperado

- `RUNBOOK.md` rastreado no repo com 3 procedimentos no formato fixo, comandos copiáveis
- Drill registrado: qual alerta, quantos minutos até diagnóstico, o que faltou no
  runbook (issue de melhoria ou nota)
- Página de status respondendo `ok` com stack no ar e `degraded` com alvo caído
- Cadeia completa exercitada: alerta → runbook → mitigação → resolved, com tempo

## Requisitos

- 1 procedimento por alerta da T3-03 (mesma nomenclatura — o alerta cita o runbook, o
  runbook parte do alerta)
- Cada procedimento com: sintoma exato (texto que chega no canal), comandos **reais** da
  VM (`ssh`, `docker compose`, `curl` no Prometheus), e critério de "resolvido"
- Comandos testados no drill (nada teórico: se o comando não funcionou, corrigir o
  runbook, não o drill)
- Drill com cronômetro e registro (tempo até diagnóstico e até resolved)
- Status page consultando Prometheus (mesma rede interna), servida em loopback/tunnel
  — **nenhuma** regra nova no ufw (22/80/443 continua a régua)
- Loop de melhoria: lacuna encontrada no drill vira nota/issue — o runbook sai do drill
  melhor do que entrou

## Critérios de aceitação

- [ ] Pré-condição: `curl <prom>/api/v1/rules` → 3+ regras `health: ok` **e** evidência
      do ciclo firing/resolved da T3-03 (mensagem no canal) — sem alerta funcionando,
      runbook é papel, pare aqui
- [ ] `git ls-files RUNBOOK.md` (ou `docs/runbooks/`) → rastreado; `grep -c '## '`
      → ≥ 3 procedimentos
- [ ] Cada procedimento tem os 4 campos (sintoma/diagnóstico/mitigação/escalação) —
      `grep -cE 'Sintoma|Diagnóstico|Mitigação|Escalação'` ≥ 12 (4×3)
- [ ] Nomes batem: os alertas da T3-03 citam o runbook (ou seção equivalente) e o
      runbook trata os mesmos 3 alertas
- [ ] Drill executado e registrado: data, alerta usado, **minutos até diagnóstico**,
      minutos até resolved, e ao menos 1 lacuna anotada
- [ ] O drill seguiu o runbook à risca: comandos do runbook copiados no log do drill
      funcionaram (diferenças corrigidas **no runbook**, com diff)
- [ ] Status page: `curl` em loopback → `ok` com stack no ar; `docker compose stop app`
      → `degraded`; start → volta `ok` (a página reflete a realidade)
- [ ] `ufw status` → inalterado (`22,80,443`) — status page e Grafana sem borda nova

## Validação

- Ler o runbook "frio": seguir um procedimento sem ter feito nada hoje (o drill é exatamente
  isso) com cronômetro
- Exercício completo: provocar o alerta `up == 0` (stop do alvo) → seguir o runbook →
  registrar tempo → mitigar → `resolved` → anotar lacuna
- Status page: `curl -s 127.0.0.1:<porta-status>` nos 3 estados (ok / degraded / voltou)
- Conferência de borda: `ufw status numbered` + `ss -tlnp` (nada novo em 0.0.0.0)
- Commit do drill: `RUNBOOK.md` com as correções + registro do exercício

## Evidências

- `RUNBOOK.md` no repo (conteúdo dos 3 procedimentos)
- Registro do drill: cronômetro, sequência de comandos usados, tempo até diagnóstico e
  resolved, lacunas anotadas
- Par da status page: `ok` no ar × `degraded` com alvo caído
- `ufw status` inalterado

## Limitações / notas

- Drill em ambiente de lab é **exercício**, não incidente: a pressão real (usuário
  esperando) não existe — o valor é treinar o **procedimento**, não simular estresse
- Status page consulta o Prometheus **do mesmo host**: se a VM inteira cair, a página
  também cai (ela não prova disponibilidade, prova "o stack local responde"). Monitorar
  de fora é blackbox externo — estágio com VPS real
- "Escalação" no lab é **parar e anotar** (não há time) — mas a seção existe porque o
  hábito de saber o ponto de desistir é o que separa operar de insistir
- Runbook que ninguém rele depois do primeiro drill envelhece em semanas — a cadência de
  reteste (a cada mudança de stack) fica como nota, não como issue (não há mudanças
  frequentes ainda)
