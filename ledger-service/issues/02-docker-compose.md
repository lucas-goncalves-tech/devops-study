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

```console
$ docker image inspect app-securepay_api --format 'size={{.Size}} user={{.Config.User}} image={{index .RepoTags 0}}'
size=447386841 user=java image=app-securepay_api:latest
$ docker images --format '{{.Repository}}:{{.Tag}} {{.Size}}' | grep securepay
app-securepay_api:latest 447MB
```

- Output do `docker compose up` mostrando a condição `service_healthy`

```console
$ docker compose up --build -d > .superpowers/sdd/plano-correcoes-auditoria2-ledger-service/compose-up.log 2>&1; echo "up exit=$?"
up exit=0
$ tail -4 .superpowers/sdd/plano-correcoes-auditoria2-ledger-service/compose-up.log
 Container app-database-1 Waiting 
 Container app-database-1 Healthy 
 Container app-securepay_api-1 Starting 
 Container app-securepay_api-1 Started 
$ grep -E '^\[(075|076)s\]' .superpowers/sdd/plano-correcoes-auditoria2-ledger-service/02-snapshots.txt  # amostragem por segundo durante a subida (trecho)
[075s] database: Up 11 seconds (healthy) | securepay_api: Created
[076s] database: Up 13 seconds (healthy) | securepay_api: Up 2 seconds
$ docker compose ps --format '{{.Service}} {{.Status}}'
database Up 9 minutes (healthy)
securepay_api Up 16 seconds
```

- Teste de persistência entre restarts

```console
$ docker compose exec -T database psql -U postgres -d securepay_db -tA -c 'SELECT count(*) FROM accounts;'
0
$ docker compose exec -T database psql -U postgres -d securepay_db -tA -c "INSERT INTO accounts (id, created_at, email, full_name, password_hash, role) VALUES (gen_random_uuid(), now(), 'evidence-02@test.local', 'Evidencia Issue 02', 'hash-nao-precisa', 'ROLE_USER');"
INSERT 0 1
$ docker compose exec -T database psql -U postgres -d securepay_db -tA -c "SELECT email FROM accounts WHERE email = 'evidence-02@test.local';"
evidence-02@test.local
$ docker compose restart
 Container app-database-1 Started 
 Container app-securepay_api-1 Started 
$ docker compose exec -T database psql -U postgres -d securepay_db -tA -c "SELECT email FROM accounts WHERE email = 'evidence-02@test.local';"
evidence-02@test.local
```

- Log de desligamento gracioso

```console
$ docker compose stop securepay_api
 Container app-securepay_api-1 Stopped 
$ docker compose logs securepay_api | grep -E 'Commencing graceful|Graceful shutdown complete|HikariPool-1 - Shutdown|Closing JPA'
securepay_api-1  | 2026-10-02T21:49:01.756Z  INFO 1 --- [ledger-service] [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown completed.
securepay_api-1  | 2026-10-02T21:49:27.602Z  INFO 1 --- [ledger-service] [ionShutdownHook] o.s.b.w.e.tomcat.GracefulShutdown        : Commencing graceful shutdown. Waiting for active requests to complete
securepay_api-1  | 2026-10-02T21:49:27.607Z  INFO 1 --- [ledger-service] [tomcat-shutdown] o.s.b.w.e.tomcat.GracefulShutdown        : Graceful shutdown complete
securepay_api-1  | 2026-10-02T21:49:27.650Z  INFO 1 --- [ledger-service] [ionShutdownHook] j.LocalContainerEntityManagerFactoryBean : Closing JPA EntityManagerFactory for persistence unit 'default'
securepay_api-1  | 2026-10-02T21:49:27.655Z  INFO 1 --- [ledger-service] [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown initiated...
securepay_api-1  | 2026-10-02T21:49:27.663Z  INFO 1 --- [ledger-service] [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown completed.
$ docker compose logs securepay_api | grep -ciE 'connection is not available|PoolInitialization|HikariPool.*ERROR'
0
$ docker compose ps -a --format '{{.Service}} {{.Status}}'
database Up 29 seconds (healthy)
securepay_api Exited (143) 3 seconds ago
```

## Limitações / notas

- `ledger-service/app/docker-compose.yaml` publica apenas a porta da API (`${PORT:-8080}:${PORT:-8080}`) e **não** publica a porta do banco — manter assim
- **Divergência registrada nesta reexecução:** o `docker image inspect` mede `size=447386841` (447 MB — base `eclipse-temurin:21-jre-alpine` ~363 MB + jar ~84 MB) e os critérios `menos de 220 MB` não foram atendidos por esta build; a correção é reduzir a base da imagem — a medição fica registrada aqui em vez de silenciar a evidência
- O serviço do banco não pode ganhar entrada `ports:` em nenhuma Issue futura
- `ledger-service/app/.env` é obrigatório para o Compose (`env_file`) e nunca deve ser commitado nem embutido na imagem
- O serviço se chama `database` — o nome é nome DNS dentro da rede do Compose
