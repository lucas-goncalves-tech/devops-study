---
aliases: [issue-01, linux-runtime]
tags: [tracker, issue, done]
status: done
prioridade: alta
---

# Issue 01 — Backend sobe de forma previsível no Linux com healthcheck e shutdown gracioso

## Contexto

O backend Spring Boot precisa rodar num host Linux sem contêineres. Hoje não existe contrato de configuração por ambiente, ninguém prova que o processo está saudável e um `kill` pode cortar requisições em voo. Sem esse contrato, qualquer orquestrador posterior (Docker, VPS, K8s) herda um serviço imprevisível.

## Objetivo

Estado final: configuração lida do ambiente, healthcheck em duas camadas com código de saída distinto para saudável/falho, e desligamento gracioso sob `SIGTERM`.

## Dependências

- Nenhuma dependência de outra Issue — ponto de entrada da trilha; a Issue 02 (imagem e composição com Postgres) consome a porta, o arquivo de ambiente e o contrato de `SIGTERM` definidos aqui

## Escopo

- Configuração de URL do banco, usuário, senha, porta e segredo JWT via variáveis de ambiente
- Healthcheck L4 por socket TCP e L7 por `/actuator/health`
- Tratamento de `SIGTERM` para fechamento ordenado

## Fora de escopo

- Docker, Docker Compose, Terraform, Kubernetes, LocalStack, CI/CD
- Foco exclusivo: processos Linux, permissões de arquivo, variáveis de ambiente, portas locais, PostgreSQL no host e healthcheck em bash

## Conhecimentos envolvidos

- Modelo de processos Linux e sinais POSIX
- Variáveis de ambiente e camadas de configuração
- Sockets TCP e inspeção de portas
- Health indicators do Spring Boot Actuator
- Readiness do PostgreSQL (`pg_isready`)

## Estado atual

- Nenhuma variável de ambiente documentada
- O processo sobe, mas ninguém prova que está saudável
- O healthcheck passa, mas um `kill` abrupto pode corromper

## Resultado esperado

- Variáveis esperadas conhecidas: URL `jdbc:postgresql://localhost:5432/securepay_db`, usuário `postgres`, senha `postgres`, porta `8080`, JWT de 256 bits em hex ou base64
- Healthcheck retorna exit 0 quando saudável e exit 1 quando falho
- API responde 200 com `status UP`
- `SIGTERM` encerra sem conexões cortadas abruptamente

## Requisitos

- [x] Definir URL do banco, usuário, senha, porta e segredo JWT via ambiente
- [x] Documentar como subir banco local e API na ordem correta
- [x] Implementar teste L4 via socket TCP (`/dev/tcp` ou `nc -z`)
- [x] Implementar teste L7 via `/actuator/health` exigindo `"UP"`
- [x] Validar que o processo responde UP na porta configurada, conferida via `ss -tulpn`
- [x] Tratar `SIGTERM` para fechar conexões sem derrubar requisições em voo

## Critérios de aceitação

- [x] API responde 200 com `status UP` em `/actuator/health`
- [x] Healthcheck retorna exit 0 no caso saudável e exit 1 no caso falho
- [x] `SIGTERM` encerra o processo com código de saída limpo e sem requisição em voo abortada

## Validação

- Subir o banco e a API na ordem documentada e confirmar `curl` em `/actuator/health` retornando HTTP 200 com `"status":"UP"`
- Rodar o healthcheck L4/L7 em comando direto — sem arquivo auxiliar: L4 `bash -c '</dev/tcp/$HOST/$PORT'` e L7 `curl -fsS http://$HOST:$PORT/actuator/health | grep -q '"status":"UP"'` — exit 0 com o serviço no ar, exit 1 com o serviço parado
- Conferir a porta com `ss -tulpn`
- Enviar `SIGTERM` ao processo e observar desligamento ordenado, sem erro de pool aberto

## Evidências

- Output de `ss -tulpn` mostrando a porta configurada

```console
$ ss -tulpn | grep -E ':8080\b'
tcp   LISTEN 0      100                                     *:8080             *:*    users:(("java",pid=114020,fd=149))         
```

- Output de `curl -i /actuator/health` com HTTP 200 e `status UP`

```console
$ curl -i -s http://localhost:8080/actuator/health
HTTP/1.1 200 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/vnd.spring-boot.actuator.v3+json
Transfer-Encoding: chunked
Date: Fri, 02 Oct 2026 21:32:25 GMT

{"status":"UP","components":{"db":{"status":"UP","details":{"database":"PostgreSQL","validationQuery":"isValid()"}},"diskSpace":{"status":"UP","details":{"total":117255962624,"free":36626010112,"threshold":10485760,"path":"/var/home/drummonds/Projects/securepay-devops/.worktrees/correcoes-auditoria2/ledger-service/app/.","exists":true}},"livenessState":{"status":"UP"},"ping":{"status":"UP"},"readinessState":{"status":"UP"}},"groups":["liveness","readiness"]}
```

- Códigos de saída do healthcheck nos dois cenários (0 e 1)

```console
$ bash -c '</dev/tcp/localhost/8080'; echo "exit=$?"
exit=0
$ curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"'; echo "exit=$?"
exit=0
$ # cenário serviço parado, após kill -TERM:
$ bash -c '</dev/tcp/localhost/8080'; echo "exit=$?"
bash: connect: Connection refused
bash: line 1: /dev/tcp/localhost/8080: Connection refused
exit=1
$ curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"'; echo "exit=$?"
curl: (7) Failed to connect to localhost port 8080 after 0 ms: Could not connect to server
exit=1
```

- Log do desligamento sob `SIGTERM` sem exceção de fechamento de conexão

```console
$ kill -TERM 114020
$ grep -E 'Graceful shutdown|HikariPool-1 - Shutdown|Closing JPA' .superpowers/sdd/plano-correcoes-auditoria2-ledger-service/api-run.log
2026-10-02T18:32:40.251-03:00  INFO 114020 --- [ledger-service] [ionShutdownHook] o.s.b.w.e.tomcat.GracefulShutdown        : Commencing graceful shutdown. Waiting for active requests to complete
2026-10-02T18:32:40.256-03:00  INFO 114020 --- [ledger-service] [tomcat-shutdown] o.s.b.w.e.tomcat.GracefulShutdown        : Graceful shutdown complete
2026-10-02T18:32:40.292-03:00  INFO 114020 --- [ledger-service] [ionShutdownHook] j.LocalContainerEntityManagerFactoryBean : Closing JPA EntityManagerFactory for persistence unit 'default'
2026-10-02T18:32:40.309-03:00  INFO 114020 --- [ledger-service] [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown initiated...
2026-10-02T18:32:40.318-03:00  INFO 114020 --- [ledger-service] [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown completed.
$ grep -ciE 'connection is not available|PoolInitialization|connection reset|HikariPool.*ERROR' .superpowers/sdd/plano-correcoes-auditoria2-ledger-service/api-run.log
0
```

## Limitações / notas

- O PostgreSQL roda no host nesta Issue — não há contêiner nem rede de orquestração
- O healthcheck L4 prova que a porta aceita conexão; não prova que a aplicação responde — por isso existe o teste L7
- Este contrato de portas e de `SIGTERM` é herdado por todas as Issues posteriores que mexem em runtime
- Os valores do `## Resultado esperado` desta Issue são de `laboratório` (`postgres`/`postgres`, JWT de exemplo): o `contrato de segredos de produção` — geração, guarda e rotação — é requisito da `Issue 07`, e nenhum default embutido sobrevive ao primeiro deploy
