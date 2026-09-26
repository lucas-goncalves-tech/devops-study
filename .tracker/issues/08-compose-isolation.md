---
aliases: [issue-08, compose-isolation]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 08 — Compose em redes segmentadas com banco inacessível e limites anti-OOM

## Contexto

Hoje a stack usa a rede padrão do Compose: qualquer contêiner alcança qualquer outro, o banco é alcançável de dentro da rede e nenhum serviço tem limite de recursos. Num host com pouca memória, um estouro derruba tudo; uma rede única não defende o dado.

## Objetivo

Estado final: três redes separando fronteira, aplicação e dados; banco e Redis inacessíveis de fora; limites e reservas de CPU/memória declarados; e a stack voltando sozinha após reboot.

## Dependências

- Requer Issue 05 — a rede pública existe por causa do proxy, que é o único alcançável de fora
- Requer Issue 07 — Redis já está na stack e precisa entrar na rede isolada
- Requer Issue 02 — composição base e healthcheck do banco

## Escopo

- Três redes: pública (proxy), interna (API) e isolada (banco e Redis)
- Sem publicação de porta para banco e Redis
- Limites e reservas de CPU/memória anti-OOM
- Política de restart e persistência de volume

## Fora de escopo

- Alteração do proxy ou do TLS — Issue 05
- Backup do banco — Issue 09
- Monitoramento — Issue 06
- Kubernetes e Cloud — Issues 18 e 12

## Conhecimentos envolvidos

- Redes no Docker Compose e escopo de serviço
- Restrições de recursos e cgroups
- OOM-kill vs limite de memória
- Políticas de restart e healthcheck

## Estado atual

- Rede única, banco alcançável por qualquer contêiner da stack
- Sem limites de recursos: estouro derruba o host
- Sem garantia de volta automática após reboot

## Resultado esperado

- Banco mudo para fora da rede isolada
- Só o proxy é alcançável de fora
- Estouro de memória contido pelo limite declarado
- Reboot da VPS restaura a stack sem intervenção

## Requisitos

- [ ] Separar as redes pública (proxy), interna (API) e isolada (banco e Redis)
- [ ] Declarar explicitamente as redes de cada serviço, sem depender da rede padrão
- [ ] Blindar banco e Redis sem publicar porta para o host
- [ ] Garantir que a API e o banco compartilham pelo menos uma rede, com o DNS do serviço `database` resolvendo
- [ ] Preservar `depends_on` com `condition: service_healthy` e o healthcheck `pg_isready`
- [ ] Preservar o volume nomeado `pg_data` montado em `/var/lib/postgresql/data`
- [ ] Validar que só o proxy é alcançável de fora
- [ ] Declarar limites e reservas de CPU/memória anti-OOM
- [ ] Garantir restart automático saudável

## Critérios de aceitação

- [ ] Banco e Redis não aceitam conexão a partir de fora da rede isolada
- [ ] Do host, apenas o portão do proxy aceita conexão externa
- [ ] A API resolve o DNS `database` e inicia normalmente após a segmentação
- [ ] Estouro de memória é contido pelo limite declarado, sem matar outros serviços
- [ ] Após reboot do host, a stack volta sozinha e `/actuator/health` responde `UP`

## Validação

- Tentativa de conexão com banco e Redis vinda de fora da rede isolada, esperada recusada
- Varredura de portas do host mostrando apenas o proxy acessível
- `docker compose up` completo seguido de requisição a `/actuator/health`
- Teste de consumo de memória acima do limite declarado
- Reboot do host e conferência automática da stack e do healthcheck

## Evidências

- Output do teste de inalcanhabilidade do banco e do Redis
- Lista de portas do host após a segmentação
- Log de subida da stack com a API aguardando o healthcheck do banco
- Comportamento observado durante o estouro de memória
- Estado da stack após reboot

## Limitações / notas

- **Invariantes que a segmentação não pode quebrar:**
  - `securepay_api` e `database` precisam dividir ao menos uma rede; `spring.jpa.hibernate.ddl-auto` é `update`, então um banco inacessível vira **falha de boot**, não degradação
  - `depends_on` com `condition: service_healthy` deve sobreviver à separação — sem isso a API sobe "no ar mas quebrada"
  - Banco não pode ganhar entrada `ports:` em nenhuma hipótese
  - `backend/.env` continua obrigatório (`env_file`)
- Esta Issue é a **última palavra sobre topologia de rede**: tudo que entra na stack antes dela (coletor, dashboard, Redis) já está coberto pelas três redes. Serviços adicionados depois precisam ser declarados nas redes corretas explicitamente
- O Redis não pode ser publicado em `0.0.0.0` — acesso externo à Stream é exposição de evento de pagamento
