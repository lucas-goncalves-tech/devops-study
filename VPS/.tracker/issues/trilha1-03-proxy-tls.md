---
aliases: [trilha1-03, proxy-tls]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 03 — Borda: reverse proxy com TLS e a app fora do alcance direto

## Contexto

Hoje a API aceita conexão direta na porta publicada — em texto puro, e quando houver
domínio/VPS pública, exposta sem HTTPS. Reverse proxy resolve duas coisas de uma vez:
termina TLS (o navegador e o `curl` falam HTTPS, o proxy repassa HTTP para a app na rede
interna) e vira a **única** porta que o mundo alcança — a app sai do caminho. É o mesmo
princípio da Trilha 0-03 (só o explícito passa) aplicado na borda: o único serviço com
porta pública é o proxy; tudo mais fala só na rede interna do compose.

## Objetivo

Estado final: `curl -k https://127.0.0.1:<porta-https>/api/v1/actuator/health` → `200`
via proxy; a porta da app **não** está publicada na host (só `80/443` do proxy);
tentar a app direto na porta interna a partir do host → falha; certificado TLS
autoassinado (ou Caddy local) gerado e visível no `curl -v`.

## Dependências

- **Requer Trilha1-02** — o proxy entra na mesma composição, aponta para o serviço `app`
  pelo nome e depende do `compose-ordenado` para existir a rede interna.
- **pré-condição verificável:** `docker compose ps` com os três serviços healthy e a
  resposta `200` no health direto (estado final da Issue 02).

## Escopo

- Serviço `proxy` (Caddy — escolhido por gerar certificado com menos ceremony; Nginx é a
  alternativa discutida no estudo) no **mesmo** `compose.yaml`
- `ports` da app trocados: **remover** a publicação direta; app só acessível na rede
  interna (`app:8080`)
- Proxy publica `80:80` e `443:443` (host) — única superfície aberta
- TLS com certificado autoassinado/local (sem domínio real no lab — domínio + Let's
  Encrypt é estágio futuro; o mecanismo TLS é o aprendizado)
- Redirect HTTP→HTTPS no proxy
- `upstream` do proxy apontando para `/api/v1` no contexto da app
- **assume pronto:** `compose-ordenado`, `portas-do-banco-fechadas` — da Issue 02
- **entrega:** `proxy-na-borda`, `tls-terminado`, `app-sem-porta-publica`

## Fora de escopo

- Domínio real, Let's Encrypt/renovação automática — estágio futuro (com VPS real)
- WAF, rate limit de borda, HSTS preload — estágio futuro (a Trilha 4 endurece o host,
  não a config do proxy)
- Cache de estáticos, compressão — otimização sem valor de estudo aqui
- Conta em provedor Cloud/DNS — estágio AWS

## Conhecimentos envolvidos

- Reverse proxy: por que terminar TLS na borda e não na app
- Certificado autoassinado: o que o `curl -k` ignora e por que o warning existe
- Redes do compose: quem alcança quem, mapeamento de porta vs. comunicação interna
- HTTP→HTTPS redirect e o que o cliente vê antes/depois
- Caddy vs Nginx: config de 3 linhas vs. manual — trade-off, não "o melhor"

## Estado atual

- A app é publicada direto na host (porta da Issue 02, em loopback)
- Tráfego em texto puro (`http://`); sem camada de proxy
- Duas formas de alcançar a app (direto e futura via proxy) — a que sobra é a errada

## Resultado esperado

- `curl -k -i https://127.0.0.1/health` → `200` com cabeçalho `server: <proxy>`
- `curl -i http://127.0.0.1/...` → `301/308` apontando para `https://`
- `ss -tlnp` → só `80` e `443` (do proxy) + portas internas **não** visíveis na host
- Porta antiga da app (ex.: 8080) → conexão recusada no host
- `docker compose config` → `ports` da `app` ausente

## Requisitos

- Serviço `proxy` no compose com `depends_on: app: service_healthy` (borda só entra com
  a app pronta)
- Publicação da app **removida** — não "também fechada", removida (não existe mais
  `ports` na `app`)
- TLS terminado no proxy: certificado gerado (autoassinado/local) e usado no listener 443
- Redirect `http://` → `https://` respondido pelo proxy
- Healthcheck do proxy declarado (borda também é serviço com estado)
- Cadeia de dependência preservada: proxy espera app, app espera banco — a ordem da Issue
  02 continua valendo com o proxy no topo

## Critérios de aceitação

- [ ] Pré-condição: `docker compose ps --format json` (ou tabela) → `db`, `redis`, `app`
      todos healthy **e** `curl -fsS http://127.0.0.1:<porta>/api/v1/actuator/health`
      → `200` (Issue 02) — sem isso, pare aqui
- [ ] `grep -A3 'ports:' compose.yaml` → nenhuma publicação na `app`; só o `proxy` tem
      `ports` (80/443)
- [ ] `curl -k -i https://127.0.0.1/api/v1/actuator/health` → `200` e
      `server:` do proxy no cabeçalho (TLS terminou na borda)
- [ ] `curl -i http://127.0.0.1/api/v1/actuator/health` → `301`/`308` com `Location:
      https://...`
- [ ] `curl -m 3 http://127.0.0.1:<porta-antiga-da-app>/...` → **conexão recusada**
      (a app não é mais alcançável diretamente)
- [ ] `ss -tlnp | grep -E ':(80|443)\b'` → os dois do proxy; nenhuma outra porta de
      serviço pública
- [ ] `docker compose ps` → os 4 serviços healthy (proxy entrou na cadeia sem quebrar a
      ordem)
- [ ] `docker compose config` → nenhuma credencial literal e nenhuma porta de `db`/
      `redis`/`app` publicada

## Validação

- `docker compose up -d` → `ps` → ordem: db/redis healthy → app started → proxy started
- TLS: `curl -kv https://127.0.0.1/ 2>&1 | grep -E 'SSL|subject|issuer'` → handshake
  visível e issuer local/autoassinado (anotar que a cadeia é local — é a limitação
  declarada)
- Redirect: `curl -i http://127.0.0.1/` → `301` + `Location:` https
- Porta morta: identificar a porta antiga (`docker compose config | grep -A2 ports`) →
  `curl -m 3` → `Connection refused`
- Cadeia: `docker compose stop db` → app `unhealthy` → proxy:
  `curl -k https://.../health` → `502/503` (a borda responde, mas conta a falha — mesma
  semântica de "responde ≠ saudável" da Trilha 0)
- `docker compose down` → `ss` → 80/443 somem

## Evidências

- `docker compose ps` com os 4 serviços
- `curl -k -i https://...` (200, header do proxy) e `curl -i http://...` (301 para https)
- `curl -m 3` na porta antiga → `Connection refused`
- `ss -tlnp` mostrando só 80/443 como superfície de serviço
- `docker inspect` do certificado gerado (subject/issuer) ou `openssl x509 -noout -dates`

## Limitações / notas

- Certificado autoassinado gera warning no navegador — **esperado**; o estudo desta Issue
  explica o que o `-k` pula e por que em produção isso é inaceitável (é a porta de
  entrada para o Let's Encrypt quando houver domínio)
- Sem domínio, o TLS com SNI de nome real não se aplica — `https://<ip>` com cert de IP é
  a forma correta aqui; renovação automática é o que muda depois
- `docker compose stop db` → o proxy devolve `502`: isso é **correto** (a borda conta a
  falha em vez de sumir), mas não é healthcheck da app — o `unhealthy` da Issue 02 continua
  sendo o sinal verdadeiro de dependência
- Remover a porta pública da app significa que **todo** acesso passa pelo proxy — se o
  proxy morrer, a app fica inacessível de fora. É o trade-off aceito de ter uma borda;
  `restart: unless-stopped` no proxy é o amparo
