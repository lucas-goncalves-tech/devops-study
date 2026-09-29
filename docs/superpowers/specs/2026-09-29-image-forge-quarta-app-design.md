---
aliases: [spec, image-forge, quarta-app, performance, seguranca]
tags: [spec, design, image-forge]
status: pendente
data: 2026-09-29
---

# Spec — `image-forge`, o 4º app: uma API de imagem com fetch por URL, insegura e lenta por construção

> Documento de design. **Não é plano de execução** — o plano é artefato separado e referencia esta
> spec. As Issues do `image-forge` **não existem ainda**: nascem do plano.
>
> **⛔ PENDENTE — não executar antes da conclusão das 3 trilhas.** A regra que levanta a pendência
> está em [`AGENTS.md`](../../../AGENTS.md), seção *Pendente: 4º app*. Enquanto ela valer, este
> documento é apenas leitura.

## Decisões já tomadas (não reabrir sem motivo novo)

| # | Decisão | Efeito |
|---|---|---|
| D-1 | **Nuvem: AWS** | Terraform, consistente com o `commerce-api`; LocalStack cobre S3/EC2/IAM/ALB sem custo |
| D-2 | **Implantação dona do grosso dos defeitos** | O grosso dos problemas vive no `.tf`, não no código. Código limpo e reaproveitável, com poucos caminhos nomeados |
| D-3 | **Exceção nomeada e datada** à regra "nenhuma porta em `0.0.0.0/0`" | Cada recurso inseguro é declarado no inventário com detector e Issue que remove |
| D-4 | **Domínio: API de imagem com fetch por URL** | Densidade máxima de nuvem: S3 + CDN + compute + HTTP externo + Postgres |
| D-5 | **`image-forge` vira o subjects do `archive/18-kubernetes-helm`** | A Issue 18 sai de órfã e ganha alvo: 4 containers, probes, requests, rollout |
| D-6 | **Inventário completo na spec, trilha só o que fecha capacidade** | A spec carrega os ~27 defeitos para nada se perder; a trilha sequencia o que fecha capacidade. O resto fica "fora desta sequência" com motivo |

## Problema

O monorepo tem 3 trilhas que teacham cloud, IaC, Docker/Compose e DevSecOps — cada uma com o
recorte dela. A trilha AWS ensina Terraform sem performance. A sequência `09 → 13` do
`commerce-api` ensina k6 e otimização sem segurança. A trilha DevSecOps ensina gate sem rodar o
sistema. **O que falta é um lugar onde tudo aparece junto, num sistema real, e onde dá para ver o
que se sabe e o que não se sabe.**

Um app com defeito é um **diagnóstico**. Não se estuda "cache" — aponta-se um sintoma e caça-se a
causa. É isso que sobrevive a dois anos sem treinar.

O `image-forge` é esse corpo de prova. Ele **funciona** — é um serviço real, usável — e nasce com as
omissões que toda primeira versão tem, escritas por omissão e não plantadas.

## Princípio de desenho que sustenta a trilha inteira

> **Um defeito que nenhuma ferramenta de terceiro acha é um defeito que você nunca vai lembrar de
> procurar.**

Por isso todo item do inventário tem uma coluna **Detector**. A regra é dura: se não existe uma
ferramenta, um script ou um painel que ache aquilo, o item não entra no inventário. Isso é o que
separa "problemas" de "problemas diagnosticáveis" — e é a razão de D-2 dar a implantação ao grosso:
um SG aberto é achado por scanner, um bucket público por Public Access Block, um p95 ruim por
painel. Defeito em código versionado depende da sua memória de ter escrito aquilo.

## A regra da casa e a exceção nomeada

`AGENTS.md` diz: *"containers non-root e nenhuma porta de serviço publicada em `0.0.0.0/0` — vale
para os 3 apps"*.

O "vale para os 3 apps" deixa o 4º de fora **por omissão**, não por decisão. D-3 fecha isso: a
exceção passa a ser **declarada**, com data e inventário, e o `AGENTS.md` é corrigido na mesma
mudança que cria a spec. A exceção é:

- **Limitada a recursos nomeados no inventário.** Não é um "a 4ª trilha pode fazer o que quiser".
- **Datada.** Uma exceção sem data vira regra em seis meses.
- **Consumível.** Cada linha tem o Issue que a remove. A trilha fechando, a exceção fecha.
- **Auditável.** Um terceiro lê o inventário e sabe exatamente o que está exposto e por quê.

## Escopo funcional (RF)

| ID | Requisito | Descrição |
|---|---|---|
| RF-01 | Autenticação | Login por email e senha, devolve JWT. Único controle de acesso |
| RF-02 | Upload binário | Recebe imagem em `multipart/form-data`, valida, armazena, devolve id |
| RF-03 | Fetch por URL | Recebe `{ url }`, baixa a imagem remota, armazena, devolve id. Compartilha o mesmo caminho de ingestão do RF-02 |
| RF-04 | Normalização e variantes | Gera variantes (thumbnail, largura fixa) a partir de uma especificação nomeada |
| RF-05 | Armazenamento | Original e variantes como objetos em object storage; metadados em Postgres |
| RF-06 | Serving | Conteúdo servido por CDN com URL estável, endereçando o objeto por `storage_key` |
| RF-07 | Consulta e listagem | Metadados de uma imagem; listagem das imagens do usuário autenticado |
| RF-08 | Remoção | Apaga objeto, variantes e registros |
| RF-09 | Saúde | `/v1/health` responde `200` `"UP"` com banco de pé e `503` `"DEGRADED"` sem ele |
| RF-10 | Observabilidade mínima | `/v1/metrics` expõe contadores HTTP e métricas de processo via `prom-client` |

**Fora do escopo funcional, deliberadamente:** transformações encadeadas, recorte/rotate, upload
em lote, alteração de formato (WebP/AVIF), watermark, organização/álbum, compartilhamento público
por token, webhooks de notificação.

## Endpoints

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `POST` | `/v1/auth/login` | não | `email` + `password` → JWT |
| `POST` | `/v1/images` | sim | `multipart/form-data` **ou** `{ url }` → cria imagem |
| `GET` | `/v1/images` | sim | lista as imagens do usuário autenticado |
| `GET` | `/v1/images/:id` | sim | metadados de uma imagem |
| `DELETE` | `/v1/images/:id` | sim | remove objeto, variantes e registros |
| `GET` | `/v1/images/:id/content` | não | original, servido pelo CDN |
| `GET` | `/v1/images/:id/variants/:variant` | não | variante; **computada sob demanda** |
| `POST` | `/v1/images/:id/variants` | sim | materializa a variante e persiste |
| `GET` | `/v1/health` | não | `200`/`503` |
| `GET` | `/v1/metrics` | não | métricas `prom-client` |

`/v1/content`, `/v1/variants` e `/v1/health` e `/v1/metrics` são **fora** do `preHandler` de JWT,
por causa do mesmo motivo que vale no `commerce-api`: `HEALTHCHECK` de imagem e scraping de
coletor não carregam token.

## Modelo de dados

### `users`

| Coluna | Tipo | Nota |
|---|---|---|
| `id` | `uuid` PK | `default random` |
| `email` | `varchar(255)` | `unique`, `not null` |
| `password_hash` | `text` | `not null` |
| `created_at` | `timestamptz` | `not null`, `default now()` |
| `updated_at` | `timestamptz` | `not null`, `default now()` |

Índice: `users_email_idx` em `email`.

### `images`

| Coluna | Tipo | Nota |
|---|---|---|
| `id` | `uuid` PK | `default random` |
| `owner_id` | `uuid` → `users.id` | `onDelete: restrict`, `not null` |
| `source_type` | `varchar(16)` | `'upload'` \| `'url'` |
| `source_url` | `text` | nulo quando `source_type = 'upload'` |
| `storage_key` | `text` | `not null` — chave do objeto original |
| `original_filename` | `text` | nome vindo do cliente, guardado sem sanitizar |
| `mime_type` | `varchar(64)` | vindo do cliente, **não verificado** |
| `byte_size` | `integer` | `not null` |
| `width` | `integer` | `not null` |
| `height` | `integer` | `not null` |
| `status` | `varchar(16)` | `'pending'` \| `'ready'` \| `'failed'` |
| `checksum_sha256` | `text` | |
| `created_at` | `timestamptz` | `not null`, `default now()` |
| `updated_at` | `timestamptz` | `not null`, `default now()` |

Índices: `images_owner_id_idx` em `owner_id`.
**Lacuna deliberada:** **sem índice em `created_at`**, que é a coluna de ordenação da listagem.

### `variants`

| Coluna | Tipo | Nota |
|---|---|---|
| `id` | `uuid` PK | `default random` |
| `image_id` | `uuid` → `images.id` | `onDelete: cascade`, `not null` |
| `variant` | `varchar(32)` | `'thumb'`, `'w200'`, `'w800'`, … `not null` |
| `storage_key` | `text` | `not null` |
| `mime_type` | `varchar(64)` | `not null` |
| `byte_size` | `integer` | `not null` |
| `width` | `integer` | `not null` |
| `height` | `integer` | `not null` |
| `created_at` | `timestamptz` | `not null`, `default now()` |

Constraint: `unique (image_id, variant)`. Esse unique **é** o índice de `image_id` — e é o que
esconde a listagem por variante, que nunca foi consultada.

### `fetch_log`

| Coluna | Tipo | Nota |
|---|---|---|
| `id` | `uuid` PK | |
| `image_id` | `uuid` → `images.id` | `onDelete: cascade` |
| `url` | `text` | URL completa, query string incluída |
| `resolved_ip` | `inet` | preenchido depois da resolução DNS |
| `status` | `varchar(32)` | resultado da busca |
| `byte_size` | `integer` | |
| `created_at` | `timestamptz` | `default now()` |

Existe porque o RF-03 grava toda URL buscada. Não há verificação contra rede privada: a coluna
`resolved_ip` existe **para** registrar um SSRF que ninguém impede.

## Requisitos não funcionais (RNF)

O ponto desta tabela é que **os valores são ruins, e essa é a especificação da v1**. Toda linha com
valor ausente é um defeito do inventário, não uma escolha.

| ID | RNF | Valor na v1 | Por que está assim |
|---|---|---|---|
| RNF-01 | Limite de tamanho de upload | **inexistente** | o primeiro `curl` funcionou |
| RNF-02 | Timeout do fetch externo | **inexistente** | ninguém pensou no request que não volta |
| RNF-03 | Rate limit | **inexistente** | ninguém pensou em custo de saída |
| RNF-04 | SLO | **não definido** | não existe p95 alvo em lugar nenhum |
| RNF-05 | Retenção de objetos | **inexistente** | o bucket só cresce |
| RNF-06 | Allowlist de MIME | **inexistente** | o tipo vem do cliente e é aceito |
| RNF-07 | Verificação de magic bytes | **inexistente** | `mime_type` é declarado, não provado |
| RNF-08 | Timeout de request | default do Node | sem valor explícito |
| RNF-09 | Compressão HTTP | **desligada** | |
| RNF-10 | Cache | **nenhum** | nem CDN, nem headers, nem em memória |
| RNF-11 | Concorrência | 1 processo Node | sem cluster, sem workers, sem fila |
| RNF-12 | Auditoria de acesso | **inexistente** | não há log de quem acessou o quê |

## Gargalos de performance — o mapa

Todos **naturais**, nenhum plantado. A lista é o que a Issue `09` do `image-forge` vai medir e a
`10` vai localizar.

| ID | Gargalo | Como aparece | Detector |
|---|---|---|---|
| G-01 | `GET /variants/:variant` **recomputa** a transformação a cada request | p95 de `/variants` cresce com a repetição da mesma variante | p95 por rota |
| G-02 | A transformação de imagem roda **no event loop** | event loop lag p99 sobe junto com o p95 | event loop lag |
| G-03 | `GET /v1/images` faz **N+1** — variantes por imagem | tempo da listagem cresce linearmente com a quantidade de imagens | p95 da listagem + `pg_stat_statements` |
| G-04 | **Sem índice** em `images.created_at` | `Seq Scan` no plano da listagem | `EXPLAIN (ANALYZE, BUFFERS)` |
| G-05 | **Sem CDN** — todo GET vai à origem | custo de egress e latência de `/content` | conta de custo + p95 |
| G-06 | Fetch externo **sem timeout** | conexão pendurada ocupa worker | event loop lag / conexões |
| G-07 | **Sem pool de conexão** para o object storage | reuso de socket subótimo | latência de I/O |
| G-08 | Instância **sobre-dimensionada** | `cpu.credit` ocioso, custo sem contrapartida | AWS Cost Explorer vs. `commerce-api 09 → 13` |
| G-09 | **Sem compressão HTTP** | payload maior que o necessário | p95 e bytes por resposta |
| G-10 | **Um processo só**, sem cluster nem fila | uma transformação satura a instância inteira | event loop lag |

**Nota sobre G-01 e o RF-04:** a variante é computada sob demanda no GET. Isso é o que torna G-01
tão óbvio, e é o que dá à Issue `10` uma correção de efeito grande e mensurável — a mesma mecânica
de "mudança uma coisa, reexecuta o mesmo script, compara o delta" que a sequência `09 → 13` do
`commerce-api` treina.

## O que é seguro e o que não é

Duas listas explícitas, porque "segurança" sem enumeração não é informação.

### Já seguro na v1 (e a Issue da trilha que prova)

- Autenticação JWT com senha com hash — e o gate SAST que garante que o padrão inseguro não entra
- Containers non-root e healthcheck real em `/v1/health` com `503` quando o banco cai
- `/v1/metrics` e `/v1/health` fora do `preHandler` de autenticação
- Idempotência declarada no `POST` via `X-Idempotency-Key` para não duplicar imagem no retry
- Coluna `checksum_sha256` para dedupe sem reprocessar
- Constraint `unique (image_id, variant)` impede variante duplicada
- IaC declarativa em Terraform com lock de estado — o desenho é reprodutível mesmo sendo inseguro

### Inseguro de propósito — código (a minoria, D-2)

| ID | Insegurança | Onde | Detector |
|---|---|---|---|
| C-01 | **SSRF**: fetch de URL arbitrária, sem bloqueio de rede privada, sem allowlist de esquema/host | `src/modules/fetch/remote-fetcher.ts` | DAST (ZAP) ou teste dedicado |
| C-02 | Segue **redirect** do destino sem reavaliar | idem | teste dedicado |
| C-03 | **Sem allowlist de MIME** e sem verificação de magic bytes: o tipo declarado é aceito | `images.service` | DAST / fuzz |
| C-04 | **Sem limite de tamanho**: nem no upload, nem na resposta do fetch | `images.service` | DAST |
| C-05 | **Segredo em log**: `source_url` completa (com query string) e o corpo do request entram no log em nível `info` | `app.ts` / logger | inspeção de log + DAST |
| C-06 | **Sem rate limit** em `/v1/images` | `app.ts` | custo de saída + teste de carga |
| C-07 | `storage_key` derivada de `original_filename` **sem sanitizar** | `storage.service` | teste dedicado |
| C-08 | Sem rate limit nem cota por usuário: um usuário consome o bucket inteiro | `images.routes` | conta de custo |

### Inseguro de propósito — implantação (o grosso, D-2)

Cada linha é uma exceção nomeada a D-3.

| ID | Recurso | O que está errado | Detector |
|---|---|---|---|
| I-01 | Security group do Postgres | `5432` aberto em `0.0.0.0/0` | scanner de rede / AWS Config |
| I-02 | Security group da API | porta HTTP aberta em `0.0.0.0/0` | scanner de rede |
| I-03 | Bucket S3 | `block_public_access` desligado | S3 Public Access Block / Security Hub |
| I-04 | IAM da task de execução | `Action: "*"` e `Resource: "*"` | IAM Access Analyzer |
| I-05 | ALB | listener só em HTTP, **sem TLS** | `sslscan` / auditoria de listener |
| I-06 | ALB | **sem WAF** associado | ausência — checklist de config |
| I-07 | VPC | **sem VPC endpoint** para o S3; o tráfego de aplicação sai para internet | VPC Flow Logs |
| I-08 | RDS | backup com **retenção 0 dias** | AWS Config rule |
| I-09 | Conta | **sem CloudTrail**, sem GuardDuty, sem Config | ausência — checklist de conta |
| I-10 | Segredos | credencial em **arquivo de ambiente versionado**, sem Secrets Manager | `gitleaks` / `trufflehog` |
| I-11 | Imagem base | tag antiga, com CVE conhecida | `trivy image` |
| I-12 | CDN | **não existe** — origem serve tudo | ausência — arquitetura |
| I-13 | Tagging | recursos sem `CostCenter`/`Project` | AWS Config rule |
| I-14 | Ambiente | **um único** ambiente, sem separação de staging | ausência — arquitetura |

## Inventário de defeitos deliberados

A tabela que serve ao objetivo de **não esquecer**. A spec a carrega completa (D-6); a trilha
sequencia só o que fecha capacidade, e o resto fica marcado com o motivo de ficar de fora.

| Grupo | Itens | Problema | Detector | Issue que remove |
|---|---|---|---|---|
| Exposição de rede | I-01, I-02 | Banco e API alcançáveis da internet aberta | scanner de rede | `04` |
| Dados | I-03, I-08 | Bucket público, backup de retenção zero | Public Access Block, Config | `04` |
| Permissões | I-04, I-07 | IAM de administrador, sem endpoint de S3 | Access Analyzer, Flow Logs | `04` |
| Tr Transit e borda | I-05, I-06 | Sem TLS, sem WAF | `sslscan`, checklist | `04` |
| Conta | I-09, I-13 | Sem trilha de auditoria, sem tagging | checklist, Config | `04` |
| Supply chain | I-10, I-11 | Segredo versionado, imagem com CVE | `gitleaks`, `trivy` | `06` |
| Arquitetura | I-12, I-14 | Sem CDN, um ambiente só | ausência | `11`, `08` |
| Código — SSRF | C-01, C-02 | Fetch de URL arbitrária, segue redirect | DAST, teste | `05` |
| Código — ingestão | C-03, C-04, C-07 | Tipo e tamanho sem validar, chave sem sanitizar | DAST, teste | `05` |
| Código — vazamento | C-05, C-08 | Segredo em log, sem cota por usuário | inspeção de log, custo | `05` |
| Performance | G-01 a G-10 | Recomputa, event loop, N+1, sem índice, sem CDN, sem timeout, sem pool, sobre-dimensionado, sem compressão, um processo | p95, event loop, `EXPLAIN`, custo | `09` → `10` → `11` |
| RNF ausentes | RNF-01 a RNF-12 | Nenhum limite, nenhum SLO, nenhuma retenção | ausência | `05`, `09`, `11` |

**Fora desta sequência, com motivo:** HPA/autoscaling e orquestração — são a Issue 18
(`archive/18-kubernetes-helm`), que passa a rodar este app. Service mesh, multi-região, CDN
próprio. Todos os RNF ausentes não viram Issues individuais: são absorvidos pelo Issue que
corresponde ao problema de negócio que os viola.

## Estrutura de pastas e arquivos

```
image-forge/
├── AGENTS.md                      # arquitetura, portas, env, limites de escrita
├── README.md                      # como rodar
├── app/
│   ├── Dockerfile                 # multi-stage, non-root, HEALTHCHECK em /v1/health
│   ├── package.json
│   ├── package-lock.json
│   ├── tsconfig.json
│   ├── drizzle.config.ts
│   ├── .env.example
│   ├── src/
│   │   ├── index.ts               # bind, SIGTERM/SIGINT, shutdown gracioso
│   │   ├── app.ts                 # Fastify, rotas, preHandler de JWT
│   │   ├── config/env.ts          # zod — falha na subida em vez de default silencioso
│   │   ├── db/
│   │   │   ├── connection.ts      # pool postgres.js
│   │   │   └── schema.ts          # as 4 tabelas acima
│   │   ├── modules/
│   │   │   ├── auth/{auth.routes,auth.service}.ts
│   │   │   ├── images/{images.routes,images.service,transformer}.ts
│   │   │   ├── storage/storage.service.ts
│   │   │   └── fetch/remote-fetcher.ts    # C-01, C-02
│   │   └── plugins/{jwt,metrics}.ts
│   └── tests/                     # a suíte trava o contrato, não a segurança
├── infra/                         # Terraform — trilha AWS
│   ├── main.tf  variables.tf  outputs.tf
│   ├── vpc.tf  s3.tf  rds.tf  iam.tf  alb.tf  ecs.tf
│   └── legacy/                    # ISOLAMENTO DOS DEFEITOS (D-2, D-3)
│       ├── README.md              # o inventário, com detector e Issue de cada linha
│       ├── networking-legacy.tf   # I-01, I-02
│       ├── s3-legacy.tf           # I-03
│       ├── iam-legacy.tf          # I-04
│       ├── alb-legacy.tf          # I-05, I-06
│       └── backup-legacy.tf       # I-08
├── estimates/                     # saída de terraform plan para custo
├── load/                          # scripts k6 (nascem na Issue 09)
├── studies/                       # um arquivo por Issue — a Issue não é a aula
├── issues/                        # as 12 Issues da trilha — NASCEM NO PLANO
└── docs/
    └── inventory.md               # a tabela do inventário, viva, atualizada a cada Issue
```

**`infra/legacy/` é a decisão de D-2 e D-3 tornado física.** Os recursos inseguros não ficam
espalhados pelo Terraform: ficam num diretório próprio, cada arquivo com o ID do inventário e um
comentário dizendo o que está errado, quem acha e qual Issue remove. Isso torna a exceção
auditável com `ls`.

## A trilha — 12 Issues

| # | Issue | O que resolve |
|---|---|---|
| `01` | App local | Sobe em Compose, upload e fetch funcionam, `/v1/health` responde 503 com banco caído |
| `02` | **Estado insano versionado** | Terraform entrega a stack com `infra/legacy/` populado, cada defeito com ID e comentário |
| `03` | **Os acham** | Scan de IaC, imagem, rede e IAM encontra o que a `02` plantou — a prova de que são diagnosticáveis |
| `04` | Rede, exposição e permissões | I-01 a I-09 removidos: SG fechado, bucket privado, IAM mínimo, TLS, WAF, endpoint de S3, backup com retenção |
| `05` | Código corrigido | C-01 a C-08 e os RNF de ingestão: SSRF, redirect, MIME, tamanho, chave, log, cota |
| `06` | Supply chain e secrets | I-10 e I-11: Secrets Manager, imagem base atualizada, `trivy` no gate |
| `07` | CI/CD com gates | O pipeline **reprova** o que a `03` achou, e o SAST barra o padrão inseguro |
| `08` | Observabilidade | Golden signals, dashboards, alerta real — e o ambiente de staging separado que fecha I-14 |
| `09` | Capacidade e rampa | Reusa o método de `commerce-api 09 → 13`: conta, rampa até degradação, ponto de inflexão |
| `10` | Gargalo corrigido com prova | **Uma** mudança de maior efeito, mesmo script re-executado, delta lado a lado |
| `11` | Custo e right-sizing | I-12 e I-13: o CDN que faltava, as tags que faltavam, e a instância que a `09` provar grande demais |
| `12` | A casa fecha | Inventário com cada defeito marcado e **a prova de que sumiu**; exceção D-3 revogada |

**A Issues `02` e `03` são o coração.** Sem elas a trilha é uma lista de boas práticas. Com elas, é
investigação com gabarito: você entrega o problema versionado, e a Issue seguinte é achá-lo com
ferramenta que não foi você quem escreveu.

**A conexão de consolidação** que o pedido original buscava está em `09` → `10` → `11`: a
sequência `09 → 13` do `commerce-api` é o **método**, e aqui ela é aplicada a um sistema com muito
mais superfície de nuvem. A `11` fecha o ciclo: a `09` dimensiona, a `10` conserta, a `11` descobre
que a instância era o dobro do necessário — que é exatamente G-08 medido, não plantado.

## Sobre o Kubernetes (`archive/18`)

D-5: o `image-forge` **não substitui** o slot da 4ª trilha — **ocupa e dá corpo**. O app tem
`api`, `worker` (depois da `10`), `postgres` e `redis`, que é o mínimo para chart com probes e
rollout fazerem sentido. A Issue 18 deixa de ser "cluster multi-node hipotético" e passa a ser "o
cluster que roda o `image-forge`", com `HPA` e autoscaling finalmente justificados por um problema
que a `09` mediu.

**Sequência de habilitação:** `archive/18` só entra depois que a `09` do `image-forge` existir. Sem
número de capacidade medido, o HPA é adivinhação — e é exatamente isso que a Issue 18 já dizia que
precisava da Issue de carga.

## Custo

A regra de custo zero do repo se mantém, com uma ressalva que precisa estar escrita:

| Fase | Onde roda | Custo |
|---|---|---|
| `01` | Docker Compose local | zero |
| `02`–`08` | LocalStack (S3, EC2, IAM, ALB, RDS) | zero |
| `09`–`11` | Compose local com a carga de `commerce-api 09 → 13` | zero |
| Prova final | AWS real, só o necessário | baixado, e **travado** por `terraform destroy` registrado |

**Ressalva:** LocalStack não emula CDN de forma utilizável. A parte de CDN da Issue `11` é a
primeira que **exige conta real** na trilha, e isso precisa estar escrito na Issue, não descoberto
na hora.

**O custo de saída do S3 é o que a Issue `11` existe para revelar.** Um bucket público e sem CDN é
um plano de custo que só aparece na fatura.

## Impacto nos documentos existentes

| Arquivo | Mudança | Quando |
|---|---|---|
| `AGENTS.md` | Escopo de escrita passa a incluir `image-forge/`; a regra de `0.0.0.0/0` ganha a **exceção nomeada e datada** (D-3), em vez do "vale para os 3 apps" que deixava o 4º de fora por omissão | **no levantamento da pendência** — não agora |
| `AGENTS.md` *(já feito)* | Seção `⛔ Pendente: 4º app` com a trava e a regra de levantamento verificável; linha da tabela de estágio marcada `⛔ pendente` | ✅ **feito**, com a entrada da spec |
| `00-visao-geral.md` | Passa a "4 apps, 4 trilhas"; a linha de `archive/18` muda de "candidata a 4ª trilha" para "trilha futura sobre o `image-forge`" | **no levantamento da pendência** — não agora |
| `README.md` | O índice ganha o 4º app como ativo | **no levantamento da pendência** |
| `BOARD.md` | Nova seção `image-forge · trilha AWS hardening` com as 12 linhas; a seção "Fora de escopo" deixa de listar o `archive/18` como 4ª trilha futura e passa a dizer que ele serve ao `image-forge` | ao criar as Issues |
| `commerce-api/AGENTS.md` | Nenhuma mudança — a sequência `09 → 13` é consumida, não alterada | — |
| `docs/performance/dicionario-de-medicao.md` | Referência cruzada: o `image-forge` reusa este dicionário na Issue `09` | ao criar a Issue `09` |

**Por que a exceção de `0.0.0.0/0` ficou para depois:** conceder `0.0.0.0/0` a um app que ainda não
existe é uma permissão que ninguém audita e que não tem recurso algum pendurado nela — exatamente o
formato em que uma exceção datada vira regra permanente. A exceção só tem sentido quando há
recursos reais para nomear, e ela nasce limitada a eles. A pendência registrada no `AGENTS.md`
torna esse "depois" obrigatório em vez de opcional.

## O que esta spec deliberadamente não decide

- **A ordem de `04` a `11` pode ser reordenada** depois que a `03` rodar de verdade e mostrar quais
  defeitos são mais graves. A spec dá a ordem lógica, não a empírica.
- **Se o `transformer` usa `sharp` ou `jimp`** — muda o build da imagem e o consumo de memória, e é
  decisão de implementação do usuário, não de design.
- **A estratégia de credencial do banco** — fica no `commerce-api` e no `ledger-service` já tem
  precedente; a `06` decide.
- **O que fazer com o `fetch_log` quando o SSRF for fechado** — manter como auditoria ou remover é
  decisão da `05`.

## Como isso se lê para quem chega depois

Uma frase: **entrega-se um serviço real, inseguro e lento por construção, e a trilha é o ato de
achar e corrigir cada um desses defeitos com prova.** A spec guarda a lista inteira para que nada
se perca; a trilha percorre só o que fecha capacidade; e o `archive/18` fica esperando o momento em
que houver número medido para justificar um cluster.
