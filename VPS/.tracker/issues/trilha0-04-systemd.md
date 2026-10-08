---
aliases: [trilha0-04, systemd]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 04 — systemd: o serviço que morre sozinho e volta sozinho

## Contexto

Na Trilha 1 o backend vai rodar como processo na VM — e a diferença entre "programa que
alguém rodou num terminal" e "serviço de servidor" é o systemd: sobe no boot, reinicia
sozinho quando cai, registra em journal com data, e obedece a ordem (espera a rede, espera
o banco). Sem isso, o primeiro `reboot` da VM apaga o sistema "silenciosamente" e a
pessoa descobre na hora do cliente reclamar. É também o primeiro contato com a unidade de
gerenciamento que **toda** vaga júnior cobra: `systemctl`, `journalctl`, unit files.

## Objetivo

Estado final: um serviço de demonstração `lab-heartbeat` (script que escreve heartbeat e
"crasha" sob pedido) gerenciado pelo systemd — `systemctl start/stop/restart` funciona,
falha deliberada gera **auto-restart** registrado no journal, `WantedBy=multi-user.target`
sobe no boot, e `journalctl -u` conta a história com timestamps.

## Dependências

- **Requer Trilha0-01 (VM), Trilha0-02 (SSH) e Trilha0-03 (firewall)** — o serviço é
  instalado via SSH numa VM com acesso por chave e base endurecida; o journal que prova o
  restart é lido depois de `systemctl` operar.
- **pré-condição verificável:** login por chave ok + `ufw status` ativo (Issues 02–03).

## Escopo

- Script `/usr/local/bin/lab-heartbeat.sh` (loop que loga e sai com código ≠ 0 a cada N
  segundos quando em modo "crash")
- Unit `lab-heartbeat.service`: `ExecStart`, `Restart=on-failure`, `RestartSec`,
  `WantedBy=multi-user.target`
- Grupo `lab-heartbeat.timer` **não** — timer é assunto de outra issue; aqui é só serviço
  (escopo fechado: um conceito por issue)
- Boot: habilitado (`systemctl enable`) e comprovado após `reboot`
- Falha forçada → journal mostra `Scheduled restart job`
- **assume pronto:** `ssh-key-only`, `ufw-deny-default` — Issues 02 e 03
- **entrega:** `servico-lab-heartbeat`, `auto-restart-provado`, `boot-persistente`

## Fora de escopo

- O backend Java como serviço (é a Trilha 1 — aqui se aprende com um script minúsculo)
- Timer/cron de backup — Issue 05
- journald persistente/rotação de log — Trilha 1 (quando houver serviços reais)
- Gerenciamento de múltiplos serviços/depends complexos — estágio AWS

## Conhecimentos envolvidos

- Ciclo de vida de unit: start/stop/restart/enable/disable vs. ativo/habilitado
- `Restart=on-failure` e códigos de saída: o que conta como "falha"
- Journal: `journalctl -u`, `-f`, `-b`, prioridades
- Diferença entre "está rodando agora" e "sobe no boot" (dois estados independentes)
- Anatomy de unit file: `[Unit]` (dependência) / `[Service]` / `[Install]`

## Estado atual

- Nenhum serviço gerenciado: qualquer processo morre com o logout/reboot
- Nenhuma evidência do que aconteceu ontem (logs só no terminal, voláteis)
- A VM não sobrevive a um reboot com serviços de aplicação (não existem)

## Resultado esperado

- `systemctl is-active lab-heartbeat` → `active`; `is-enabled` → `enabled`
- `systemctl restart lab-heartbeat` → volta em < 2s
- Crash forçado → em ~`RestartSec` o journal registra o restart sozinho
- Reboot → serviço volta sem ninguém logar
- `journalctl -u lab-heartbeat -b` → timeline com eventos das 3 situações acima

## Requisitos

- Script mínimo escrito por você (o valor é a unit, não o script)
- Unit file com `Restart=on-failure` e `RestartSec` declarado (não default silencioso)
- `enable` feito **antes** do teste de boot (senão o reboot não prova nada)
- Crash provocado por código de saída ≠ 0 (a semântica que `on-failure` observa)
- Todo o ciclo observado via `journalctl`, não via echo

## Critérios de aceitação

- [ ] Pré-condição: `ssh -o BatchMode=yes lab@<ip> 'echo ok'` → `ok` **e**
      `sudo ufw status | head -1` → `Status: active` (Issues 02–03) — sem os dois, pare aqui
- [ ] `systemctl is-active lab-heartbeat` → `active` e `is-enabled` → `enabled`
- [ ] `systemctl stop lab-heartbeat` → `inactive`; `start` → `active` (controle manual)
- [ ] Crash forçado (ex.: variável que faz o script sair com 1) →
      `journalctl -u lab-heartbeat -n 5` **sem** `-e` mostra `Scheduled restart job` —
      o systemd reiniciou **sem** comando humano
- [ ] `sudo reboot` → após subir, `systemctl is-active lab-heartbeat` → `active` sem login
      ter feito nada
- [ ] `journalctl -u lab-heartbeat -b --no-pager | grep -c 'Started\|Stopping'` → ≥ 3
      (start inicial, parada deliberada, restart do crash) — a história está registrada
- [ ] `systemctl status lab-heartbeat --no-pager` → exit code do último crash visível em
      `Main PID`/`Status`

## Validação

- `systemctl status lab-heartbeat` → `active (running)`
- Parar e subir: `sudo systemctl stop/start lab-heartbeat` + `is-active` em cada passo
- Provocar crash (editar `ExecStart` para comando que retorna 1, ou flag no script) →
  `journalctl -u lab-heartbeat -f` ao vivo: mensagens de falha seguidas de
  `Scheduled restart job` dentro de `RestartSec`
- `sudo reboot`; esperar SSH voltar (`until ssh ...; do sleep 2; done`) → `is-active` → `active`
- `journalctl -u lab-heartbeat -b --no-pager | head -20` → a sequência completa do boot

## Evidências

- Conteúdo do `lab-heartbeat.service` (unit inteira)
- Saída do `journalctl` mostrando o auto-restart com o intervalo de `RestartSec`
- Saída pós-reboot: `is-enabled` + `is-active`
- Timeline `-b` com os ≥ 3 eventos de start/stop

## Limitações / notas

- O crash é **provocado de propósito** — em produção isso seria um bug; aqui é o jeito de
  ver a proteção funcionando (é o mesmo princípio dos testes da Issue 01: só se prova o
  que se faz acontecer)
- `Restart=on-failure` não reinicia em `exit 0` nem em sinal de `stop` — se o script morrer
  "limpo", o systemd fica parado. Essa distinção é o coração do requisito e aparece no estudo
- Journal é volátil por padrão neste Ubuntu (storage=persistent varia) — sobrevive a reboot
  no mesmo boot; retenção/rotação é assunto da Trilha 1 quando houver serviços reais
