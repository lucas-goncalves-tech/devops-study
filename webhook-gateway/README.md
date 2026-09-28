# webhook-gateway

Dispatcher de webhooks orientado a eventos em **Node 20 · TypeScript · ioredis · Redis Streams**:
consome uma Stream em laço, assina cada evento com HMAC e despacha. É o app da **trilha
DevSecOps** do monorepo: a jornada vai do consumidor na máquina até uma pipeline agnóstica de
cloud que barra segredo, erro estático e CVE alta/crítica.

**Este app não abre porta HTTP.** Ele é um consumidor de Redis Streams em laço — não há `listen`,
nem rota, nem `PORT`.

## Como rodar

Precisa de Node 20 e de um Redis acessível:

```bash
cd webhook-gateway/app
npm ci
npm run build
npm start
```

O consumer group é criado com `MKSTREAM` e `BUSYGROUP` é tratado como sucesso, então subir duas
vezes não falha. O log de subida identifica a Stream e o group em uso.

Injete um evento na Stream `payment-events` para ver o processamento e o `XACK`. Note que
`XREADGROUP` usa `BLOCK 2000`: a primeira leitura pode demorar até 2 s, então um evento disparado
bem na hora da subida pode não aparecer imediatamente.

Testes e build:

```bash
npm test        # vitest run
npm run build   # tsc -> dist/
```

Não há script `dev`: TypeScript só roda depois de compilado, então `npm start` sempre executa
`dist/index.js`.

## Tracker

| # | Issue | Status |
|---|---|---|
| [01](issues/01-linux-runtime.md) | Consumidor como serviço, com restart e shutdown gracioso | `todo` |
| [02](issues/02-docker-compose.md) | Imagem non-root, rota de saúde, Redis sem porta publicada | `todo` |
| [03](issues/03-pipeline-base-agnostica.md) | Pipeline base agnóstica chamando scripts locais | `todo` |
| [04](issues/04-secrets-hygiene.md) | Segredos bloqueados no fluxo de merge | `todo` |
| [05](issues/05-sast-semgrep.md) | Semgrep bloqueante em severidade ERROR | `todo` |
| [06](issues/06-sca-dependencias-imagem.md) | `npm audit` nas libs e Trivy na imagem | `todo` |
| [07](issues/07-pipeline-hardening.md) | Permissões mínimas e ações pinadas por SHA | `todo` |
| [08](issues/08-devsecops-gates.md) | Segredos + SAST + SCA como barreira única | `todo` |
| [09](issues/09-dast-zap.md) | OWASP ZAP contra o serviço de pé | `todo` |
| [10](issues/10-containers-redis.md) | Redis Streams e gateway de webhooks em contêineres | `todo` |
| [11](issues/11-staging-inseguro.md) | Staging inseguro de propósito, com forense de mensageria | `todo` |

A trilha começa pela `01`: enquanto ela não vier, não há Dockerfile, `docker-compose.yaml`,
`.env.example` nem rota de saúde — a `02` os cria.

Issue = uma capacidade, escrita como RFC. Não é tutorial: o passo a passo de cada uma está em
[`estudos/`](estudos/) e o status de todas as trilhas no [`BOARD.md`](../BOARD.md).

- [`issues/`](issues/) — as 11 Issues da trilha DevSecOps
- [`estudos/`](estudos/) — material de estudo, um arquivo por Issue
- [`AGENTS.md`](AGENTS.md) — stack, variáveis de ambiente e o mapa de rollout dos gates para os
  outros apps, para quem vai mexer
