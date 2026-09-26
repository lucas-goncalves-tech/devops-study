---
aliases: [issue-05, caddy-reverse-proxy]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 05 — Entrada única via reverse proxy com TLS automático e headers de segurança

## Contexto

A API está exposta direto na porta 8080, sem TLS, sem redirecionamento e sem headers de segurança. Cada serviço publicado direto obriga a gerenciar certificado na mão e mantém porta de aplicação aberta à internet.

## Objetivo

Estado final: o tráfego externo entra por um único reverse proxy na porta 80/443, com certificado TLS emitido e renovado automaticamente, redirecionamento HTTP→HTTPS, headers de segurança e roteamento por domínio para upstreams internos.

## Dependências

- Requer Issue 04 — o firewall precisa liberar 80/443 e o acesso ao servidor é só por chave

## Escopo

- Reverse proxy como porta única de entrada
- Roteamento por domínio para upstreams internos
- Emissão e renovação automática de TLS
- Headers de segurança padrão, compressão e logs estruturados

## Fora de escopo

- Isolamento de redes do Compose e limites de recursos — Issue 08
- Alteração do compose do backend — a API continua em `backend/docker-compose.yaml`
- Backup e monitoramento — Issues 09 e 06
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
- [ ] Rotejar por domínio para upstreams internos
- [ ] Emitir TLS automático com renovação sem downtime
- [ ] Aplicar headers de segurança padrão
- [ ] Ativar compressão e logs estruturados

## Critérios de aceitação

- [ ] `https://` responde com certificado válido e dentro da validade, renovado automaticamente
- [ ] `http://` redireciona para `https://` com status 301 ou 308
- [ ] Resposta inclui os headers de segurança declarados (verificáveis com inspeção de cabeçalhos)
- [ ] A API não é alcançável diretamente de fora — apenas via proxy

## Validação

- Requisição HTTPS com inspeção do certificado emitido
- Requisição HTTP conferindo o código de redirecionamento
- Inspeção dos cabeçalhos de resposta
- Tentativa de alcançar a porta da API diretamente de fora do servidor, esperada recusada

## Evidências

- Output da inspeção do certificado com emissor, validade e renovação
- Resposta do redirecionamento HTTP→HTTPS
- Lista de headers presentes na resposta
- Teste de inalcançabilidade direta da porta da API

## Limitações / notas

- Requer domínio apontado para o servidor e porta 80/443 liberadas — pré-requisito de ambiente
- **Invariante de porta:** `8080` é a única porta publicada pelo Compose do backend (`${PORT:-8080}:${PORT:-8080}`). Ao torná-la interna, `healthcheck.sh`, o `EXPOSE` do `Dockerfile`, `server.port` e o health check do target group precisam mudar juntos — mudança isolada de um deles quebra o healthcheck
- O upstream do proxy deve apontar para o serviço `securepay_api` na rede interna, publicando a porta apenas para o proxy
- O backend mantém `server.shutdown: graceful` — o proxy não pode encerrar a JVM abruptamente
