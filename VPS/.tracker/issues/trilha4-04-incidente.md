---
aliases: [trilha4-04, incidente]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 04 — Exercício de incidente: a cadeia inteira sob fogo

## Contexto

A Trilha 4 até aqui mediu (T4-01), protegiu (T4-02, T4-03) — mas **nunca foi
treinado** sob pressão de um cenário que começa com "algo errado aconteceu e eu não sei
o que". O lab tem tudo para exercitar: alerta que dispara (T3-03), runbook escrito
(T3-04), rotação de secret (T4-02), rollback de deploy (T2-04), borda auditável
(T4-01). A issue é costurar a corrente inteira num **scenario drill**: um dos cenários
clássicos (secret vazado / porta esquecida aberta / imagem com CVE rodando / disco
enchendo) é provocado **antes** de alguém ver, e a resposta segue o caminho que o lab
construiu — detectar → conter → corrigir → registrar. É o exercício que separa "tenho
ferramenta" de "sei operar", e é o item de entrevista mais forte do tracker: contar um
incidente **exercitado** com tempo e evidência.

## Objetivo

Estado final: 1 cenário provocado em condições de surpresa controlada (outras pessoas ou
o próprio sessão futura podem disparar), detectado **pelo sinal do lab** (alerta/log/
scan — não pelo executor olhando), respondido seguindo o runbook com cronômetro,
fechado com estado verde (6×healthy + borda 22/80/443), e registrado num
`docs/incidentes.md` no formato: timeline, o que detectou, o que o runbook cobriu, o que
faltou (vira nota/issue).

## Dependências

- **Requer Trilha4-01..03** — o cenário usa a defesa que aquelas issues entregaram
  (audit, gate de secret, scan de imagem). Sem a Trilha 4 feita, não há o que exercitar.
- **Requer Trilha3-04** — a resposta segue o `RUNBOOK.md` e usa o formato de drill já
  praticado; aqui é o mesmo método com cenário **adversarial** (executor ≠ solver quando
  possível).
- **pré-condição verificável:** runbook com 3+ procedimentos + ciclo de alerta entregue
  + todos os gates (vazamento, Trivy) verdes.

## Escopo

- Escolha de **1 cenário** (justificar no estudo), ex.:
  - secret no repo (gate da T4-02 deve pescar — se pescou, a resposta é revogar+rotação)
  - porta/serviço exposto fora da régua (T4-01 `ss`/`ufw` devem acusar)
  - imagem com CRITICAL rodando (Trivy do T4-03 no re-scan)
  - disco enchendo por log (T3-01 rotação + alerta da T3-03)
- Provocação **escondida** do solver (ou em data marcada sem aviso de qual): o gatilho
  é dado só pelo sintoma
- Detecção pelo **sinal**: o que grita primeiro? (alerta chega / gate falha / status
  page `degraded`) — registrar a latência detect→reação
- Resposta via runbook com cronômetro: conter → corrigir → verificar → `resolved`
- Fechamento: stack verde, borda 22/80/443, gates voltam a verde
- Registro: `docs/incidentes.md` (ou seção do runbook) com timeline e lacunas
- **assume pronto:** todos os gates T4-01..03 + `runbook-3-procedimentos` (T3-04)
- **entrega:** `cenario-provocado`, `resposta-runbook-cronometrada`, `registro-incidente`

## Fora de escopo

- Múltiplos cenários simultâneos / caos engenharia (Chaos Monkey) — 1 cenário profundo
  basta para o exercício
- Ataque real de fora (port scan externo, brute force) — seria teste da borda da T0/T1;
  o cenário é provocado **dentro** do perímetro já defendido
- Post-mortem blameless formal com time — o registro tem o formato, o "time" é 1 pessoa
- Corrigir a lacuna encontrada aqui e agora se for grande — pode virar issue da fila
  (o drill **gera** trabalho, não precisa **concluir** tudo)

## Conhecimentos envolvidos

- MTTD/MTTR na prática: tempo de detecção × tempo de resposta — medir os dois sem
  jargão vazio
- Contenção vs. correção: parar o estrago primeiro (conter), arrumar a causa depois
- Exercício adversarial: por que "executor sabe o que plantou" não ensina detecção
- Lição de drill: o valor está nas lacunas anotadas, não no cenário bonito que deu certo
- Registro de incidente: timeline objetiva, sem autoacusação — artefato de carreira

## Estado atual

- 4 alertas e 1 drill básico (T3-04) já praticados — mas **sempre** com o solver sabendo
  o que ia acontecer
- Gates da Trilha 4 configurados e nunca testados contra um caso plantado de propósito
- Nenhum incidente registrado no repo — nada para mostrar em entrevista

## Resultado esperado

- Cenário escolhido e provocado **antes** de o solver saber qual era (ou na data marcada
  sem briefing do gatilho)
- Evidência de que o **sinal** falou primeiro: linha do alerta/gate/status page com
  timestamp **antes** da primeira ação de diagnóstico
- Resposta cronometrada: detect → conter → corrigido → verde (minutos registrados)
- Fechamento verificado: `docker compose ps` 6×healthy, `ufw status` 22/80/443, gates
  T4-02/03 verdes
- `docs/incidentes.md` rastreado com a timeline e ≥ 1 lacuna anotada

## Requisitos

- 1 cenário declarado com **por que** ele representa o risco real do lab
- Provocação e solução em momentos separados quando possível (o quem planta ≠ quem
  resolve; se for a mesma pessoa, declarar a limitação)
- Detecção obrigatoriamente pelo **sinal do sistema** (alerta/gate/page), não pelo
  executor olhar — a primeira linha da timeline é o sinal, não o comando
- Cronômetro nos 3 marcos: detect, conter, resolvido
- Resposta cita o passo do runbook usado (se usou passo que **não** está no runbook, a
  lacuna é obrigatória no registro)
- Fecho: gates e stack verdes de novo (o exercício não deixa lixo)
- Registro versionado — mesmo que o cenário tenha "dado errado", registra-se

## Critérios de aceitação

- [ ] Pré-condição: `git ls-files RUNBOOK.md docs/incidentes.md` +
      `curl <prom>/api/v1/rules` → 3+ `ok` (T3-04/T3-03) **e** gates T4-02/03 verdes
      no último run — sem os quatro, pare aqui
- [ ] Cenário registrado no repo **antes** da execução (o que ia ser provocado e por
      quê) — comitado ou evidência datada
- [ ] Timeline em `docs/incidentes.md` com os 3 marcadores (detect/conter/resolvido) e
      **detecção pelo sinal** (timestamp do alerta/gate anterior ao 1º diagnóstico)
- [ ] Resposta com cronômetro: minutos detect→conter e conter→resolvido registrados
- [ ] Runbook acionado: pelo menos 1 passo copiado do `RUNBOOK.md` aparece na
      sequência da resposta (citado na timeline)
- [ ] Lacuna: ≥ 1 anotada no registro (o que faltou no runbook/gate/alerta)
- [ ] Fechamento verde: `docker compose ps` 6×healthy **e** `ufw status` `22,80,443` **e**
      gates (scan/secret) voltaram a verde
- [ ] `docs/incidentes.md` rastreado no repo; app intocado (`git diff` sem `src/`)

## Validação

- Preparar (sem alertar o solver): plantar o gatilho do cenário escolhido na VM
- Observar: registrar o **primeiro sinal** recebido (alerta no canal / job vermelho /
  status page) com horário
- Responder seguindo o runbook com cronômetro → conter → corrigir → verificar verde
- Fechar: `ps`, `ufw status`, re-rodar os gates → tudo verde
- Escrever a timeline com os horários reais e as lacunas → commit
- Repetir o gate de pré-condição (a Trilha 4 começa de pé para a próxima rodada)

## Evidências

- O commit do cenário (antes) × o `docs/incidentes.md` (depois) com a timeline
- A linha do sinal primeiro (alerta/gate) com timestamp anterior ao diagnóstico
- Os tempos: detect, conter, resolvido
- Fechamento: `ps` 6×healthy + `ufw status` + gates verdes
- ≥ 1 lacuna anotada (com destino: corrigir aqui ou virar issue)

## Limitações / notas

- **Exercício não é incidente real**: não há usuário esperando nem pressão de negócio —
  o que se treina é o **procedimento** e a confiança nele; a pressão real só vem com
  sistema em produção (fronteira honesta do lab)
- Surpresa controlada tem limite: em repo solo é fácil adivinhar o cenário — declarar
  quem plantou × quem resolveu; se forem os mesmos, a detecção testada foi a do **sinal**,
  não a de "não saber o que aconteceu"
- Cenário plantado dentro do perímetro **não** testa a borda (T0-02/T1-03) — ataque
  externo de verdade é teste ofensivo, estágio futuro com VPS real
- Lacuna grande encontrada vira **issue**, não escopo desta: o drill fecha com registro,
  a melhoria entra na fila — é o loop que mantém runbook e gates vivos
