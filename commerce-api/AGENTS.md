# commerce-api — trilha AWS

API de e-commerce e inventário (Node 20 · Fastify · Drizzle ORM · Postgres). Carrega a **trilha
AWS** do monorepo: `linux → compose → terraform → CI → observabilidade → S3/EC2 → apply → deploy`.

Estado final da trilha: API leve em computação real na nuvem, com estado Terraform remoto e lock,
pipeline que bloqueia merge, infraestrutura aplicada só por pipeline identificada, deploy com
rollback e staging falho que prova a observabilidade — relatório em bucket privado previsto
(Issue 06, `parked`).

Antes de qualquer coisa neste app, as regras da casa (escopo de escrita, padrão de container,
skills e roteamento) valem: leia o [`AGENTS.md` da raiz](../AGENTS.md). Metodologia e política de
status → [`00-visao-geral.md`](../00-visao-geral.md). Status e ordem de execução →
[`BOARD.md`](../BOARD.md), seção `commerce-api · trilha AWS`.

## Trilha AWS — 10 Issues

O checkbox do board espelha o `status:` do frontmatter de cada Issue; a tabela abaixo é a mesma
fonte, então uma das duas está desatualizada se elas divergirem.

| # | Issue | Status |
|---|---|---|
| [01](issues/01-linux-runtime.md) | API sobe de forma previsível no Linux com healthcheck e shutdown gracioso | `todo` |
| [02](issues/02-docker-compose.md) | Compose com banco isolado e API Non-Root, orquestrando o Dockerfile existente | `todo` |
| [03](issues/03-terraform-vpc.md) | Rede multi-tier declarada em Terraform com banco isolado e bucket privado | `todo` |
| [04](issues/04-github-actions.md) | Pipeline de CI com testes, scan de imagem e gate de IaC | `todo` |
| [05](issues/05-observability.md) | Coleta de golden signals, dashboards e prova de carga com SLO | `todo` |
| [06](issues/06-s3-reports-infra.md) | Storage S3 para relatórios com IAM least privilege e endpoint privado | `parked` |
| [07](issues/07-aws-production.md) | Estado Terraform remoto com lock e computação em nuvem real | `todo` |
| [08](issues/08-staging-falho-observabilidade.md) | Staging separado com três falhas de observabilidade injetadas, diagnosticadas e corrigidas | `todo` |
| [09](issues/09-pipeline-infra-apply.md) | Identidade da pipeline e `apply` de infraestrutura com credencial federada | `todo` |
| [10](issues/10-deploy-ec2-pipeline.md) | Deploy da aplicação em computação real por pipeline, com rollback por healthcheck | `todo` |

**Próxima a entrar: `01`.** `06` está `parked` de propósito (revertida): nenhuma Issue da trilha
depende dela e ela não é marco da sequência. Não a puxe de volta para fechar o gap. `09` e `10`
entram depois da `07`: estado remoto e computação real existem antes de aplicar e publicar.

## Stack e comandos

Todos a partir de `commerce-api/app/`.

| Comando | Para quê | Concluído quando |
|---|---|---|
| `npm ci` | instala travado pelo `package-lock.json` | sem erro |
| `npm run dev` | sobe com `tsx watch` (recarrega ao salvar) | log de `listening on http://0.0.0.0:3000` |
| `npm run build` | `tsc` → `dist/` | sem erro de tipo |
| `npm start` | roda `dist/index.js` | idem `dev`, sem watch |
| `npm test` | suíte `vitest run` | verde |
| `npm run lint` | `tsc --noEmit` — o lint deste app é o compilador | zero erro de tipo |

`npm run lint` não é ESLint: é o `tsc` sem emitir. Erro de tipo é falha de lint aqui.

## Variáveis de ambiente

Validadas por `zod` em `src/config/env.ts`: valor inválido **quebra a subida** em vez de cair no
default silenciosamente. Leia esse arquivo quando uma variável nova entrar.

| Variável | Default | Consumida por |
|---|---|---|
| `NODE_ENV` | `development` | `development` \| `test` \| `production` |
| `PORT` | `3000` | Fastify e o `HEALTHCHECK` da imagem |
| `HOST` | `0.0.0.0` | bind do Fastify |
| `DATABASE_URL` | `postgresql://commerce_user:commerce_pass@localhost:5432/commerce_db` | Postgres (Drizzle e `drizzle.config.ts`) |
| `JWT_SECRET` | valor de desenvolvimento de 32+ chars | assinatura de token (`@fastify/jwt`) |
| `CORS_ORIGIN` | `*` | `@fastify/cors` |

`DATABASE_URL` é lida de dois lugares: `src/config/env.ts` e `drizzle.config.ts` (para os comandos
do `drizzle-kit`). Ao apontar para a rede do Compose ou para LocalStack, ajuste as duas — o schema
não migra sozinho.

## Rota de saúde

`GET /health` em `src/app.ts` — **`200` com `status: "UP"`** quando o Postgres responde,
**`503` com `status: "DEGRADED"`** quando não. O `HEALTHCHECK` da imagem em
[`app/Dockerfile`](app/Dockerfile) (`wget --spider`) usa esse 200; um serviço vivo com o banco
caído é exatamente o caso que o 503 expõe, e é por isso que a rota checa banco em vez de só
devolver `ok`.

## O que ainda não existe aqui

Os quatro gaps abaixo são **conhecidos e planejados**, não esquecidos: são as Issues 02, 03, 04,
09 e 10 que os criam, e elas são construção do usuário.

- **Sem `docker-compose.yaml`.** Existe [`app/Dockerfile`](app/Dockerfile) multi-stage non-root, mas
  nada o orquestra: enquanto a Issue 02 não vier, o Postgres roda no host e o `DATABASE_URL`
  default já aponta para ele. Issue 02 — banco isolado, sem porta publicada.
- **Sem `infra/` (Terraform).** Issue 03 cria a VPC multi-tier, os subnets e o bucket privado,
  com endpoints apontando para LocalStack na maior parte da trilha. Não espere o diretório: ele
  nasce com a Issue 03.
- **Sem CI.** O único workflow do repositório é um stub em
  [`.github/workflows/CI.yml`](../.github/workflows/CI.yml) (nome, gatilhos e um job sem passos).
  Issue 04 é o que dá testes, scan de imagem e gate de IaC — com gate de IaC, o `terraform validate`
  nasce junto com a pasta `infra/`, então `04` depende de `03`.
- **Sem identidade de pipeline e sem deploy.** Nada aplica infraestrutura pela pipeline e nada
  publica a API em computação: a Issue 09 cria a identidade federada e o `apply` com aprovação, e
  a Issue 10 o deploy com rollback por healthcheck.

## Onde o trabalho vive

| Caminho | O que é | Quando abrir |
|---|---|---|
| [`issues/`](issues/) | as 10 Issues da trilha, uma por capacidade, no template fixo | antes de implementar ou fechar qualquer Issue |
| [`estudos/`](estudos/) | material de estudo, um arquivo por Issue | quando precisar do passo a passo; a Issue nunca é a aula |
| [`scripts/`](scripts/) | scripts de check e troubleshooting desta trilha (construídos aqui) | para validar ou depurar sem depender de outra trilha |
| [`../BOARD.md`](../BOARD.md) | status e ordem das 3 trilhas | para saber o que está feito e o que entra em seguida |
| [`../00-visao-geral.md`](../00-visao-geral.md) | metodologia, template, política de status | antes de escrever ou fechar uma Issue |

## Escopo de escrita

Gravável aqui: `issues/`, `estudos/`, `AGENTS.md` e `README.md`.

`app/` (código, `package.json`, `Dockerfile`), `infra/` (Terraform), `scripts/`,
`../.github/workflows/` e qualquer IaC são **construção do usuário** — proponha o diff e espere o pedido explícito. Fechar
uma Issue exige a saída real do comando de validação colada em `## Evidências`; limitação de
ambiente registrada em `Limitações / notas` não vale como evidência.
