---
aliases: [trilha1-04, publica-vm]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 04 — Publicação: a stack sai do host e vai para a VM endurecida

## Contexto

Até aqui tudo subiu no **host** do desenvolvimento, atrás do NAT do libvirt. A Trilha 0
preparou a `lab-vm` (SSH por chave, ufw deny, auto-updates, systemd, backup) e ela está
ociosa — e é nela que a stack tem que viver: só assim os firewalls, portas e upgrades da
Trilha 0 **passam a proteger alguma coisa real**, e só assim aparecem os problemas que só
existem fora da máquina de desenvolvimento (arquitetura diferente não, mas recursos, IP,
reinício, e o hábito de operar via SSH). É o pulo de "no meu laptop" para "numa máquina
de verdade" — sem cloud, sem custo, com o revert da Trilha 0-01 como saída de emergência.

## Objetivo

Estado final: `docker compose up -d` roda **na `lab-vm`** com a stack inteira (proxy, app,
db, redis) publicando só 80/443; de **fora** da VM (do host), `curl -k https://<ip-da-vm>/`
responde `200` através do proxy; o ufw da Trilha 0-03 mostra **80/443 liberados como
regra explícita** (nada aberto por acidente) e `5432/6379` seguem recusados de fora.

## Dependências

- **Requer Trilha1-01..03** — é a imagem, o compose e o proxy de lá que vão para cá;
- **Requer Trilha0-01..05** — a VM endurecida é o destino, o ufw é a régua, o snapshot é
  a saída de emergência.
- **pré-condição verificável:** `docker compose ps` 4×healthy no host (Trilha 1 feita) +
  `ufw status` ativo na VM com só 22 (Trilha 0 feita). As duas pontas precisam estar
  prontas antes do cabo.

## Escopo

- Docker Engine **na VM** (repo oficial, não o snap — discutido no estudo) e usuário `lab`
  no grupo `docker`
- Transferência da stack: build da imagem **na VM** (build remoto via `docker context` ou
  `scp` do contexto + build local — escolher e justificar; não copiar imagem binária entre
  arquiteturas por genérico)
- `.env` real copiado para a VM (fora do git, permissão 600 — segredo de laboratório)
- Regras **explícitas** no ufw: `allow 80/tcp`, `allow 443/tcp` — a porta nova entra
  pelo mesmo mecanismo da Trilha 0-03, nunca por "desligar o ufw"
- Rollback provado: se a subida quebrar a VM, `virsh snapshot-revert` (ou `down`) resolve
- **assume pronto:** `proxy-na-borda` (Trilha 1-03) + `ufw-deny-default` (Trilha 0-03)
- **entrega:** `stack-na-vm`, `ufw-com-80-443-explicitos`, `https-responde-de-fora`

## Fora de escopo

- CI/CD, push de imagem, deploy automático — Trilha 2
- Domínio apontando para a VM (DNS público) — estágio futuro com VPS real
- Registro de imagem próprio (registry), multi-host — estágio AWS
- Migração de dados/volumes entre host e VM — lab novo, volume novo

## Conhecimentos envolvidos

- Docker Engine vs. Docker Desktop; repo oficial vs. snap (por que o snap confunde paths)
- `docker context` / contexto remoto: build e orquestração de fora vs. dentro
- Firewall por regra explícita: abrir porta = mesma ação da Trilha 0, agora com motivo novo
- `.env` em disco: `chmod 600`, dono, e por que ele nunca viaja por git
- Operar via SSH: o "terminal" da VM é remoto — o que muda no fluxo de comandos

## Estado atual

- Stack completa mas só no host (4 serviços healthy no laptop)
- VM `lab-vm` no ar, endurecida, com só a 22 aberta e **sem Docker**
- `ss` na VM: nenhuma porta de serviço (nada roda lá)
- Host continua sendo o único lugar onde a app existe

## Resultado esperado

- `docker compose ps` **na VM** → 4 serviços healthy
- No host: `curl -k -i https://<ip-vm>/api/v1/actuator/health` → `200` via proxy da VM
- `ufw status` na VM → `22,80,443 ALLOW` (regras explícitas) e `5432`/`6379` de fora →
  recusado
- Reboot da VM → stack volta (`restart: unless-stopped`) e 80/443 seguem respondendo
- `docker exec` na VM → `id -u` da app ≠ 0 (a propriedade da Issue 01 viajou com a imagem)

## Requisitos

- Docker Engine instalado pela **via oficial** na VM, `lab` em `docker` (sem `sudo` no
  dia a dia — alinhado ao usuário não-root da Trilha 0)
- Stack construída/rodando na VM por método declarado (contexto remoto **ou** build
  local no repo copiado — um dos dois, justificado na evidência)
- Regras ufw `80/tcp` e `443/tcp` adicionadas com `ufw allow` (nunca `ufw disable`) e
  testadas de fora
- `.env` na VM com `chmod 600` e dono `lab`
- Boot: `docker restart: unless-stopped` + Docker habilitado no systemd → stack volta sozinha
- Nenhuma porta extra aberta — a régua é a mesma da Trilha 0: só o explícito

## Critérios de aceitação

- [ ] Pré-condição (ponta Trilha 1): no host, `docker compose ps` → 4× healthy;
      pré-condição (ponta Trilha 0): `ssh lab@<ip-vm> 'sudo ufw status | head -1'` →
      `Status: active` com só `22/tcp` ALLOW — **sem as duas pontas, pare aqui**
- [ ] Na VM: `docker --version` → Engine instalado (repo oficial, `apt policy docker-ce`
      ou `docker compose version` ≥ 2) e `id lab | grep -o docker` → grupo presente
- [ ] Na VM: `docker compose ps` → `db`, `redis`, `app`, `proxy` healthy
- [ ] No **host**: `curl -k -o /dev/null -w '%{http_code}' https://<ip-vm>/api/v1/actuator/health`
      → `200` (chegou pelo proxy da VM, não pelo localhost)
- [ ] `ufw status` na VM → exatamente `22,80,443 ALLOW Anywhere` — nenhuma outra porta;
      `grep -c 'ALLOW'` → `3`
- [ ] No host: `nc -zv <ip-vm> 5432` → recusado **e** `nc -zv <ip-vm> 80` → sucesso
      (mesma origem, portas diferentes)
- [ ] `ssh lab@<ip-vm> 'sudo docker exec <container app> id -u'` → ≠ 0
- [ ] `sudo reboot` na VM → após SSH voltar, `docker compose ps` → 4 healthy **sem** ninguém
      subir nada na mão
- [ ] `.env` na VM: `ssh lab@<ip-vm> 'stat -c "%a %U" .env'` → `600 lab`

## Validação

- Instalar Docker na VM: seguir `docker-ce` do repo oficial; `systemctl is-active docker`
  → `active`
- Levar a stack (escolher um caminho e repetir o mesmo nas evidências):
  - `docker context create labvm --docker host=ssh://lab@<ip-vm>` + `docker --context
    labvm compose up -d`, **ou**
  - `rsync`/`scp` do repo para `~/lab/` + `docker compose up -d --build` dentro da VM
- Prova externa (no host): `curl -kv https://<ip-vm>/... 2>&1 | grep -E 'HTTP|subject'`
  → `200` e cert local
- ufw: `ssh lab@<ip-vm> 'sudo ufw allow 80/tcp && sudo ufw allow 443/tcp && sudo ufw
  status numbered'` → as 3 regras explícitas, `22` intacta
- Negativos no host: `nc -zv <ip-vm> 5432` → falha; `nc -zv <ip-vm> 6379` → falha
- Boot: `ssh lab@<ip-vm> 'sudo reboot'` → `until ssh ... 'docker compose ps'; do sleep 3;
  done` → 4 healthy
- Rollback de emergência documentado: `virsh snapshot-list` ainda tem `base` (nunca foi
  apagado nesta Issue)

## Evidências

- `docker compose ps` rodando **na VM** (4 healthy)
- `curl -k -i` do **host** contra `<ip-vm>` → `200` com header do proxy
- `ufw status verbose` da VM com exatamente 22/80/443
- Par `nc` do host: 80 ok, 5432/6379 recusado
- Pós-`reboot`: `docker compose ps` → 4 healthy e `systemctl is-enabled docker` → enabled
- `stat` do `.env` → `600`
- Registro de qual caminho de transferência foi usado (contexto remoto ou repo+build
  local) e por quê

## Limitações / notas

- A VM está atrás do NAT do libvirt: `curl` do host para `<ip-vm>` funciona na rede local,
  mas **ninguém de fora** alcança — o teste real de exposição pública (com IP roteável)
  é o estágio com VPS real. Mesmo assim, a configuração já está certa para quando isso
  acontecer: ufw com 3 regras explícitas é o que vai herdar a VPS pública
- Build na VM exige RAM suficiente (Maven no estágio de build reclama com < 2 GB) —
  se a VM tiver 2 GB, considerar build no host + `docker save/load` **declarado** como
  limitação (imagem binária viaja, assinatura de build não)
- `.env` na VM é segredo de laboratório com senha fraca de propósito — em produção ele
  nem existiria em disco assim (injeção de segredo é estágio futuro). O `600` aqui é o
  hábito, não a solução final
- Se a stack da VM e a do host rodarem **ao mesmo tempo**, as duas respondem em portas
  diferentes — sem conflito, mas o `curl` errado prova a máquina errada; anotar sempre
  qual `<ip>` foi testado
