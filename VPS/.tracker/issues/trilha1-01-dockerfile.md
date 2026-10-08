---
aliases: [trilha1-01, dockerfile]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 01 — Imagem: build reproduzível com usuário sem privilégio

## Contexto

A app compila no meu Maven e roda no meu terminal — isso é ambiente, não entrega. A imagem
Docker é o contrato que diz "nesta máquina, com estes bytes, sobe assim" — é ela que o CI
da Trilha 2 vai buildar e que a VPS vai rodar; sem ela, "funciona na minha máquina" é a
única evidência. E como a VPS é pública depois da Trilha 0 com hardening, o processo dentro
do container **não pode herdar root**: root dentro do container vira root na prática
quando há falha de escape ou config errada de volume — privilégio mínimo é o mesmo
princípio da Issue 02 da Trilha 0 (quem autentica), aplicado a quem executa.

## Objetivo

Estado final: `docker build` de máquina limpa gera a imagem `notes-api` em tempo
reproduzível (cache válido), o container sobe com `uid ≠ 0` confirmável por `docker exec`,
e a app responde `200` no `/api/v1/actuator/health` — tudo sem tocar em `src/` nem `pom.xml`.

## Dependências

- **Requer Trilha0-01..05** — a base (VM, SSH, firewall, systemd, backup) está confirmada;
  esta Issue acontece no **host** de build, mas o artefato construído aqui é o que a
  Issue 04 levará para a VM.
- Pré-requisito do **host**: Docker Engine + plugin Compose v2 instalados
  (`docker --version` **e** `docker compose version` respondem) — mesma classe do KVM
  na Trilha 0-01: é da máquina de desenvolvimento, não do lab.
- **pré-condição verificável:** `kvm-ok`/`/dev/kvm` ok, `docker compose version` no host,
  `ufw status` ativo na VM e `lab-backup.timer` listado (estado Trilha 0 inteira).

## Escopo

- Dockerfile multi-stage: estágio `build` com Maven (só para compilar), estágio `runtime`
  com JRE 17 enxuto
- Usuário não-root criado **no Dockerfile** (`useradd`/`adduser` + `USER`) — não depender
  de imagem base já ter
- `.dockerignore` para não copiar `.git`, `.tracker`, `target/` no contexto
- Healthcheck do container apontando para `GET /api/v1/actuator/health` (o Actuator da
  preparação é o alvo — é para isso que ele ficou)
- **assume pronto:** `src/` com Actuator+Prometheus instrumentado — da preparação do lab
- **entrega:** `imagem-notes-api`, `uid-diferente-de-zero`, `healthcheck-no-container`

## Fora de escopo

- Compose: ordenação de subida, PG/Redis, portas — Issue 02
- Reverse proxy, TLS, exposição — Issue 03
- Push de imagem para registro, build no CI — Trilha 2
- Imagem "distroless"/UBI/multi-arch — aprofundamento futuro

## Conhecimentos envolvidos

- Multi-stage: por que a ferramenta de build não vai para produção
- Camadas, cache de build e `.dockerignore` — o que invalida cache
- USER, uid/gid e por que root dentro do container é risco, não conveniência
- HEALTHCHECK: semântica (o orquestrador enxerga) vs. liveness do app
- Diferença entre `docker run` que funciona e imagem reproduzível em máquina limpa

## Estado atual

- Dockerfile e compose **não existem** no repo (foram removidos de propósito na preparação)
- Nenhuma imagem construída; a app só roda via `mvnw spring-boot:run` na mão
- Nada prova que a app sobe fora do ambiente de desenvolvimento

## Resultado esperado

- `docker build -t notes-api .` → sucesso, duas stages visíveis no log
- `docker run ... notes-api` + `docker exec ... id -u` → uid ≠ 0
- `curl` no healthcheck do container → `200 {"status":"UP"}`
- Segundo build sem mudar código → "Using cache" na stage de deps (reprodutibilidade +
  velocidade)

## Requisitos

- Multi-stage: `maven:*-jdk17` só na build, `eclipse-temurin:17-jre-*` (ou alpine) no
  runtime
- `USER` não-root no estágio final, com uid fixo conhecido
- `HEALTHCHECK` com `CMD` usando ferramenta existente na imagem (curl/wget — se a base não
  tiver, é parte do trabalho instalá-lo na build ou escolher base com ele)
- `.dockerignore` cobrindo `target/`, `.git/`, `.tracker/`, `.env`
- Porta exposta documentada (`EXPOSE 8080` — anotação, não publicação)
- Nenhuma alteração em `src/`, `pom.xml` ou `application.yml`

## Critérios de aceitação

- [ ] Pré-condição: Trilha 0 completa — `ssh -o BatchMode=yes lab@<ip> 'sudo ufw status |
      head -1'` → `Status: active` **e** `virsh snapshot-list lab-vm` contém `base` **e**
      `docker compose version` no host responde — sem os três, pare aqui
- [ ] `docker build -t notes-api .` → `EXIT 0` a partir de um diretório **limpo**
      (`git status` sem sujeira de build)
- [ ] `docker run -d --name probe notes-api ...` → `docker exec probe id -u` → valor ≠ `0`
- [ ] `docker inspect --format '{{.Config.Healthcheck.Test}}' notes-api` → comando apontando
      para `/api/v1/actuator/health`
- [ ] `curl -fsS` no health do container → HTTP 200 com `"status":"UP"` (o container
      **depende** de PG/Redis para UP — se a Issue 02 ainda não existe, suba os deps
      avulsos para este teste e anote; o compose definitivo é da Issue 02)
- [ ] Segundo `docker build` sem mudança → stages de dependências com `Using cache`
- [ ] `docker history notes-api` → nenhuma camada com `USER root` no fim; estágio final
      com `USER` declarado
- [ ] `git diff --stat` → **vazio** em `src/` e `pom.xml` (a app não foi tocada)

## Validação

- `docker build -t notes-api . 2>&1 | grep -E 'DONE|naming'` → build completo
- `docker run -d --name probe -e ... notes-api` (envs do `.env.example`) →
  `docker exec probe id -u` → `1000` (ou outro ≠ 0)
- `docker inspect --format '{{.State.Health.Status}}' probe` → `starting` → `healthy`
  (esperar o intervalo do healthcheck)
- `docker exec probe wget -qO- http://localhost:8080/api/v1/actuator/health` → `UP`
- Cache: `docker build -t notes-api .` imediatamente de novo → `CACHED` nas camadas de
  `pom.xml`
- Limpeza: `docker rm -f probe` ao final

## Evidências

- Log do primeiro `docker build` (stages visíveis) e do segundo (cache)
- `docker exec probe id -u` com uid ≠ 0
- `docker inspect` do healthcheck + estado `healthy`
- `git diff --stat` vazio em `src/` e `pom.xml`

## Limitações / notas

- O healthcheck dentro do container testa **a app**, não o banco — com PG fora, o estado
  vira `unhealthy` mesmo com o processo vivo. Essa semântica (processo vivo ≠ sistema
  saudável) é exatamente o conceito da Issue 02 e do estudo da Trilha 0-04
- `HEALTHCHECK` do Docker não é lido por `docker run` simples — só por orquestradores
  (compose/swarm/k8s). Aqui ele é declarado e provado via `docker inspect`; o efeito
  prático na ordem de subida vem na Issue 02
- UID fixo sem `USER` na imagem base conhecida: se a base mudar de uid default, o
  `useradd -u 10001` fixo evita surpresa — por isso o uid é declarado, não herdado
