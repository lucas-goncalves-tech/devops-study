---
aliases: [trilha3-01, logs]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 01 — Logs: sobrevivem ao restart, rotacionam sozinhos, respondem pergunta

## Contexto

O journald da Trilha 0-04 registra o `lab-heartbeat`, mas os logs dos **containers** seguem
o driver do Docker (json-file por padrão) — e três coisas quebram sem ninguém ver: o disco
enche (sem rotação, o `*.json.log` cresce até `ENOSPC` derrubar o daemon), o restartsome
logs (se o driver não for persistente/rotacionado, a investigação perde o começo), e "olha
o log" vira `docker logs` manual em cada container. Operar é responder "o que aconteceu
ontem 3h?" sem adivinhar — e sem quebrar a máquina no processo. É o item mais barato da
Trilha 3 e o que mais falta em ambiente que "nunca teve problema" (até ter).

## Objetivo

Estado final: driver de log do Docker com **rotação declarada** (`max-size`/`max-file`)
em daemon.json, logs dos containers persistindo entre restarts, journal do host
persistente e com retenção; um incidente provocado (log de erro gerado) é **encontrado**
depois de restart do container via `docker logs --since` — e o disco da VM não muda de
nível significativamente durante o teste.

## Dependências

- **Requer Trilha2-01..04** — a stack é atualizada por deploy (os logs são os das versões
  que passam) e a VM é o alvo operacional; sem a Trilha 2 feita, não há fluxo de versão
  cujo histórico valha a pena preservar.
- **pré-condição verificável:** último deploy verde na main (`run` com job `deploy`
  success) e `docker compose ps` 4×healthy na VM.

## Escopo

- `daemon.json` na VM: `max-size` + `max-file` para o driver `json-file` (default do
  Docker) — configurado **no host**, vale para todos os containers
- Renovação/limitação do journal: `journalctl --vacuum-size=` (ou TimeSpan) + confirmação
  de storage persistente no host da VM
- Rotina de inspeção: `docker system df` antes/depois, `docker logs --since` como busca
- Incidente de teste: gerar linha de erro na app (ex.: chamada que loga erro real do
  GlobalExceptionHandler), restartar o container, **achar** a linha com `--since`
- **assume pronto:** `rollback-automatico` (T2-04) + journald do `lab-heartbeat` (T0-04)
- **entrega:** `log-rotacao-declarada`, `logs-sobrevivem-ao-restart`, `journal-limitado`

## Fora de escopo

- Centralização (ELK/Loki/OpenSearch) e coleta multi-host — estágio futuro
- Métricas de log (erros/segundo como série) — Issue 02 dá a base, Issue 03 alerta
- Log estruturado/JSON no app (mudança de código) — app é intocado (regra do lab)
- Retenção por compliance/GDPR — não há dado sensível de produção aqui

## Conhecimentos envolvidos

- Driver de log do Docker: onde o stdout do container é gravado, `max-size`/`max-file`
- Por que disco cheio derruba o daemon (ENOSPC em `/var/lib/docker` = tudo para)
- `docker logs --since/--until` como linha do tempo (o que sobrevive a restart)
- journald persistente vs volátil: `Storage=persistent` e `--vacuum-size`
- "Log que não rotaciona não é log, é mina de disco"

## Estado atual

- Containers com driver `json-file` **sem** rotação (padrão: `max-size` ilimitado)
- Logs dos containers somem com `docker rm` (e alguns com restart, dependendo do driver)
- Journal da VM com limite padrão (ou sem vacuum feito)
- Nenhuma rotina de "olhar/limpar" — o disco só é olhado quando enche

## Resultado esperado

- `docker info | grep -A3 'Logging Driver'`/`cat /etc/docker/daemon.json` → rotação com
  valores declarados
- `ls -lh /var/lib/docker/containers/*/*.log` → tamanho ≤ `max-size`
- `docker compose restart app` → `docker logs --since 10m app` encontra linha anterior
  ao restart
- `journalctl --disk-usage` → abaixo do limite declarado
- Teste de carga de log não empurra o disco da VM além do esperado (`df -h` antes/depois)

## Requisitos

- `/etc/docker/daemon.json` com `log-driver` (ou default) + `max-size` + `max-file`
  declarados com **valores** (ex.: 50m × 3 — o número escolhido e justificado vai para o
  estudo/limitação)
- Docker reconfigurado com reload (`systemctl reload docker` ou restart — declarar qual
  e o impacto: containers em execução **não** herdam config nova sem recreate)
- Journal persistente confirmado (`journalctl -o json | head` funciona após reboot **ou**
  `Storage=persistent` verificado)
- `--vacuum-size` (ou TimeSpan) aplicado e limite documentado
- Rotina de inspeção escrita no runbook mínimo da Issue 04 (aqui: os comandos funcionam;
  o runbook organizado é a 04)
- Prova de sobrevivência: erro gerado **antes** do restart é encontrado **depois**
- Nenhuma mudança no app (logs são do Docker/host, código intocado)

## Critérios de aceitação

- [ ] Pré-condição: `git log --oneline -1` corresponde a run com `deploy` verde no
      Actions **e** `ssh lab@<ip-vm> 'docker compose ps'` → 4×healthy (T2-04) — sem
      isso, pare aqui
- [ ] `ssh lab@<ip-vm> 'cat /etc/docker/daemon.json'` → `max-size` e `max-file` com
      valores numéricos (não default implícito)
- [ ] Arquivo de log de um container: `docker exec`/stat mostrando tamanho ≤ `max-size`
      **ou** `docker inspect --format '{{.HostConfig.LogConfig}}'` → config com os limites
- [ ] Restart: `docker compose restart app` → log de **antes** do restart recuperado com
      `docker logs --since <horário>` (linha encontrada e citada)
- [ ] `journalctl --disk-usage` → valor abaixo do limite declarado;
      `journalctl -u lab-heartbeat -b -n 5` → ainda funciona (journal intacto)
- [ ] `df -h /` na VM antes e depois do teste de geração de log → crescimento dentro do
      esperado (rotação segurou), sem `USE%` crítico
- [ ] Reload/restart do Docker feito com impacto declarado: containers recriados se
      necessário (`docker compose up -d` após a troca de config) e stack healthy
- [ ] Nada de app alterado: `git diff` da execução sem tocar em `src/`/`pom.xml`

## Validação

- Antes: `ssh ... 'df -h /; docker system df; journalctl --disk-usage'` → baseline
- Config: editar `daemon.json`, `sudo systemctl reload docker` (ou restart + `up -d`),
  `docker info | grep Logging` → confirmar
- Gerar erro real: `curl` que dispare o handler de erro da app (ex.: rota inexistente com
  corpo que o `GlobalExceptionHandler` registra) ou log proposital via endpoint — usar o
  que o app já emite, sem mexer no código
- Restart: `docker compose restart app` → `docker logs --since 2m app | grep <erro>` →
  linha encontrada
- Limites: `docker inspect --format '{{.HostConfig.LogConfig}}' <container>` →
  `max-size/max-file` presentes
- Disco: `df -h /` de novo → delta aceitável; `journalctl --disk-usage` ≤ vacuum aplicado
- Limpeza: containers de teste removidos, stack healthy ao final

## Evidências

- `cat /etc/docker/daemon.json` da VM (valores reais)
- `docker inspect` do LogConfig + stat do arquivo de log rotacionado
- O par de `docker logs --since` (linha anterior ao restart recuperada)
- `journalctl --disk-usage` antes/depois do vacuum e `df -h` do teste de geração
- `docker compose ps` healthy ao final

## Limitações / notas

- `max-size`/`max-file` aplicam-se a containers **criados depois** da mudança de
  `daemon.json` — os que já rodam herdam só após recreate (`up -d --force-recreate`);
  rodar `reload` e achar que "ficou pronto" sem recriar é a armadilha clássica aqui
- Rotação é por arquivo, não por "importância": linha de ERROR de 3h atrás pode já ter
  sido rotacionada se o volume de log for alto — é por isso que a centralização
  (Loki/ELK, estágio futuro) existe; aqui se aceita o limite do disco local
- `journalctl --vacuum-size` apaga o **mais antigo** de uma vez — rodar em host com
  incidente antigo em investigação perde a prova; por isso o vacuum é rotina agendada
  (Issue 04), não executor avulso de madrugada
- Logs de app em texto (não JSON estruturado) — grep resolve no lab; em escala, parse de
  texto é o que a Trilha 3-02 evita nas **métricas** (daí o Actuator que já está no app)
