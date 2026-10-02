---
aliases: [issue-04, caddy-reverse-proxy]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 04 — Entrada única via reverse proxy com TLS automático e headers de segurança

## Contexto

A API está exposta direto na porta 8080, sem TLS, sem redirecionamento e sem headers de segurança. Cada serviço publicado direto obriga a gerenciar certificado na mão e mantém porta de aplicação aberta à internet.

## Objetivo

Estado final: o tráfego externo entra por um único reverse proxy na porta 80/443, com certificado TLS emitido e renovado automaticamente, redirecionamento HTTP→HTTPS, headers de segurança e roteamento por domínio para upstreams internos.

## Dependências

- Requer Issue 03 — o firewall precisa liberar 80/443 e o acesso ao servidor é só por chave

## Escopo

- Reverse proxy como porta única de entrada
- Roteamento por domínio para upstreams internos
- Emissão e renovação automática de TLS
- Headers de segurança padrão, compressão e logs estruturados

## Fora de escopo

- Isolamento de redes do Compose e limites de recursos — Issue 06
- Alteração do compose do backend — a API continua em `ledger-service/app/docker-compose.yaml`
- Backup do banco — Issue 05
- Monitoramento e alertas — [Issue 08](08-trafego-sintetico-alertas.md) desta trilha
- Kubernetes, Cloud e Terraform

## Conhecimentos envolvidos

- Reverse proxy na camada 7
- ACME e ciclo de vida de certificados
- Headers de segurança de resposta
- Terminação de TLS vs pass-through

## Estado atual

- API exposta direto, sem TLS e sem redirecionamento
- HTTP puro, sem headers de segurança
- Nenhum serviço de fronteira entre a internet e a aplicação

## Resultado esperado

- Tráfego externo entra somente pelo proxy
- HTTPS válido com renovação sem downtime
- HTTP redireciona para HTTPS
- Headers de segurança presentes nas respostas
- Compressão e logs estruturados ativos

## Requisitos

- [ ] Subir reverse proxy como porta única de entrada
- [ ] Rotejar por domínio para upstreams internos, alcançáveis pelo proxy apenas os `caminhos declarados` de cada upstream — para a aplicação, `/api/v1/**`, e no actuator somente `/actuator/health` e `/actuator/prometheus`
- [ ] Emitir TLS automático com renovação sem downtime
- [ ] Aplicar headers de segurança padrão
- [ ] Ativar compressão e logs estruturados

## Critérios de aceitação

- [ ] `https://` responde com certificado válido e dentro da validade, renovado automaticamente
- [ ] `http://` redireciona para `https://` com status 301 ou 308
- [ ] Resposta inclui os headers de segurança declarados (verificáveis com inspeção de cabeçalhos)
- [ ] A API não é alcançável diretamente de fora — apenas via proxy
- [ ] Endpoints do actuator fora dos dois declarados não passam pelo proxy — recusa antes de chegar na aplicação

## Validação

- Requisição HTTPS com inspeção do certificado emitido
- Requisição HTTP conferindo o código de redirecionamento
- Inspeção dos cabeçalhos de resposta
- Tentativa de alcançar a porta da API diretamente de fora do servidor, esperada recusada
- Requisição a `/actuator/env` pelo caminho público esperada recusada pelo proxy (404 ou 401)

## Evidências

- Output da inspeção do certificado com emissor, validade e renovação
- Resposta do redirecionamento HTTP→HTTPS
- Lista de headers presentes na resposta
- Teste de inalcançabilidade direta da porta da API

## Limitações / notas

- **Não precisa de VPS pública nem de domínio real para começar.** Caddy emite certificado TLS local para um hostname resolvido via `/etc/hosts`, o que já prova emissão, renovação, redirect HTTP→HTTPS e headers. Domínio real e IP público só entram como prova final de TLS de internet.
- Domínio apontado para o servidor e portas 80/443 liberadas são pré-requisito apenas quando a prova for contra internet real
- **Invariante de porta:** `8080` é a única porta publicada pelo Compose do backend (`${PORT:-8080}:${PORT:-8080}`). Ao torná-la interna, o healthcheck L4/L7 da `Issue 01` (`/dev/tcp` + `curl`), o `EXPOSE` do `Dockerfile`, `server.port` e o health check do target group precisam mudar juntos — mudança isolada de um deles quebra o healthcheck
- O upstream do proxy deve apontar para o serviço `securepay_api` na rede interna, publicando a porta apenas para o proxy
- O backend mantém `server.shutdown: graceful` — o proxy não pode encerrar a JVM abruptamente
