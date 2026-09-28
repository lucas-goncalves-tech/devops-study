---
aliases: [issue-02, docker-compose]
tags: [tracker, issue, done]
status: done
prioridade: alta
---

# Issue 02 — Imagem enxuta non-root e Compose com banco isolado atrás de healthcheck

## Contexto

O build atual inclui ferramentas de compilação, roda como root e não garante ordem de subida. Sem imagem reproduzível e sem orquestração, "funciona na minha máquina" é a única evidência disponível e a API pode subir antes do banco aceitar conexões.

## Objetivo

Estado final: imagem multi-stage abaixo de 220 MB executando como usuário sem privilégios, e um Compose onde a API só recebe tráfego depois que o banco está saudável, com dados que sobrevivem a restart.

## Dependências

- Requer Issue 01 — o contrato de porta (`PORT=8080`), o arquivo de ambiente e o fechamento sob `SIGTERM` vêm de lá; o healthcheck do Compose herda o contrato L4/L7 que aquela Issue provou

## Escopo

- Build multi-stage separando compilação (JDK 21) de runtime (JRE Alpine)
- Usuário sem privilégios, contexto de build enxuto, flags de memória para container
- Orquestração de API + `postgres:16-alpine` com volume persistente e healthcheck
- Dependência condicional: API aguarda saúde do banco

## Fora de escopo

- Terraform, Kubernetes/Helm, LocalStack, ECS/EKS, CI/CD remoto
- Foco exclusivo: multi-stage enxuto Alpine, usuário sem privilégios, ordenação por saúde e persistência de volume

## Conhecimentos envolvidos

- Multi-stage builds e boas práticas de Dockerfile
- JVM em containers (`MaxRAMPercentage`, RSS vs heap)
- Ordem de subida e healthcheck no Compose
- PostgreSQL em container e persistência de volume

## Estado atual

- O build inclui ferramentas e roda como root
- A imagem pronta, mas a API pode subir antes do banco
- Não há garantia de persistência nem de ordem de subida

## Resultado esperado

- Imagem abaixo de 220 MB, non-root, contendo apenas o artefato final
- Banco persiste após restart
- API aguarda a saúde do banco antes de aceitar tráfego
- Parada com `SIGTERM` fecha ordenadamente o pool HikariCP

## Requisitos

- [x] Criar build em múltiplos estágios separando compilação (JDK 21) e runtime (JRE mínimo, base Alpine)
- [x] Reduzir imagem final para menos de 220 MB
- [x] Rodar como usuário sem privilégios, nunca root
- [x] Configurar flags de memória ciente de container para a JVM
- [x] Enxugar contexto excluindo `target/`, `.git/`, `.env`
- [x] Orquestrar API + banco (`postgres:16-alpine`) com volume persistente em `/var/lib/postgresql/data`
- [x] Healthcheck do banco via `pg_isready` e URL interna `jdbc:postgresql://<serviço>:5432/securepay_db`
- [x] Garantir que a API só sobe quando o banco está saudável (dependência condicional)
- [x] Confirmar parada com `SIGTERM` e fechamento ordenado do pool HikariCP

## Critérios de aceitação

- [x] Imagem com menos de 220 MB e executada por usuário não-root
- [x] Dados do banco sobrevivem a `docker compose down` seguido de `up`
- [x] A API não inicia antes de o banco responder ao healthcheck
- [x] `SIGTERM` no contêiner da API encerra sem erro de pool aberto

## Validação

- `docker image inspect` confirmando usuário não-root e tamanho abaixo de 220 MB
- `docker compose up` observando a API só iniciar depois de `service_healthy`
- Inserir registro, reiniciar a stack e confirmar que o registro persiste
- Enviar `SIGTERM` ao contêiner da API e conferir o log de desligamento

## Evidências

- Tamanho da imagem e UID do usuário de execução
- Output do `docker compose up` mostrando a condição `service_healthy`
- Teste de persistência entre restarts
- Log de desligamento gracioso

## Limitações / notas

- `ledger-service/app/docker-compose.yaml` publica apenas a porta da API (`${PORT:-8080}:${PORT:-8080}`) e **não** publica a porta do banco — manter assim
- O serviço do banco não pode ganhar entrada `ports:` em nenhuma Issue futura
- `ledger-service/app/.env` é obrigatório para o Compose (`env_file`) e nunca deve ser commitado nem embutido na imagem
- O serviço se chama `database` — o nome é nome DNS dentro da rede do Compose
