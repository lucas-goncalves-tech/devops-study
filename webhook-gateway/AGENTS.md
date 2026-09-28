# webhook-gateway — trilha DevSecOps

Dispatcher de webhooks orientado a eventos: consome uma **Redis Stream** em laço, assina cada
evento com HMAC e despacha. Carrega a **trilha DevSecOps** do monorepo:
`pipeline → secrets → SAST → SCA → hardening → gates → DAST → mensageria`.

Estado final da trilha: pipeline agnóstica de cloud que barra segredo, erro estático e CVE
alta/crítica, com SCA e DAST exercitados, Redis Streams em produção e staging inseguro de
propósito como prova de que os gates pegam o que importa.

Antes de qualquer coisa neste app, as regras da casa (escopo de escrita, padrão de container,
skills e roteamento) valem: leia o [`AGENTS.md` da raiz](../AGENTS.md). Metodologia e política de
status → [`00-visao-geral.md`](../00-visao-geral.md). Status e ordem de execução →
[`BOARD.md`](../BOARD.md), seção `webhook-gateway · trilha DevSecOps`.

## Trilha DevSecOps — 11 Issues

O checkbox do board espelha o `status:` do frontmatter de cada Issue; a tabela abaixo é a mesma
fonte, então uma das duas está desatualizada se elas divergirem.

| # | Issue | Status |
|---|---|---|
| [01](issues/01-linux-runtime.md) | Consumidor sobe de forma previsível no Linux com restart e shutdown gracioso | `todo` |
| [02](issues/02-docker-compose.md) | Imagem Non-Root com rota de saúde e Compose com Redis não publicado | `todo` |
| [03](issues/03-pipeline-base-agnostica.md) | Pipeline base agnóstica com build e teste em scripts locais que o workflow só chama | `todo` |
| [04](issues/04-secrets-hygiene.md) | Higiene de segredos com detecção bloqueante no fluxo de merge | `todo` |
| [05](issues/05-sast-semgrep.md) | Análise estática com gate bloqueante para severidade ERROR | `todo` |
| [06](issues/06-sca-dependencias-imagem.md) | SCA em duas camadas: `npm audit` nas dependências e Trivy na imagem Docker | `todo` |
| [07](issues/07-pipeline-hardening.md) | Pipeline com permissões mínimas e ações pinadas por SHA | `todo` |
| [08](issues/08-devsecops-gates.md) | Gates de segredos, SAST e SCA consolidados como barreira única da pipeline | `todo` |
| [09](issues/09-dast-zap.md) | DAST com OWASP ZAP em baseline contra o serviço de pé, com achado corrigido ou justificado | `todo` |
| [10](issues/10-containers-redis.md) | Composição multi-serviço com Redis Streams e gateway de webhooks | `todo` |
| [11](issues/11-staging-inseguro.md) | Staging inseguro de propósito, fora da produção, com forense de mensageria Redis | `todo` |

**Próxima a entrar: `01`.** Nada da trilha começou — os cards existem, o trabalho não.

## O app não é um servidor HTTP

Isto é o que mais confunde quem chega: **este app não abre porta nenhuma.** `src/index.ts` sobe um
consumidor de Redis Streams em laço (`XREADGROUP` com `BLOCK 2000`) e não há `listen`, rota nem
`PORT`. Consequência prática: **não existe healthcheck L4/L7 aqui** — verificar "o processo está
vivo e conectado ao Redis" é trabalho da Issue 01, em nível de processo e de socket, e o
`HEALTHCHECK` de imagem e a rota `/health` são a Issue 02.

Por isso este app **não tem script `dev`**: TypeScript só é executado depois de compilado.
`npm start` roda `dist/index.js`.

## Stack e comandos

Todos a partir de `webhook-gateway/app/`.

| Comando | Para quê | Concluído quando |
|---|---|---|
| `npm ci` | instala travado pelo `package-lock.json` | sem erro |
| `npm run build` | `tsc` → `dist/` | sem erro de tipo |
| `npm start` | consome a Stream (`node dist/index.js`) | log de subida com Stream e group |
| `npm test` | suíte `vitest run` | verde |

Rodar exige um Redis acessível e o consumer group criado — `initGroup` usa `MKSTREAM` e tolera
`BUSYGROUP`, então subir duas vezes não falha.

## Variáveis de ambiente

Defaults lidos em `src/index.ts`. Os defaults servem para desenvolvimento: em qualquer ambiente
compartilhado, `REDIS_URL` e `WEBHOOK_SECRET` têm de vir de um arquivo de ambiente fora do
repositório.

| Variável | Default | Nota |
|---|---|---|
| `REDIS_URL` | `redis://localhost:6379` | na stack de contêiner vira o nome do serviço |
| `STREAM_KEY` | `payment-events` | chave da Stream |
| `GROUP_NAME` | `webhook-dispatcher-group` | consumer group |
| `CONSUMER_NAME` | `worker-${process.pid}` | nome do consumidor dentro do group |
| `WEBHOOK_SECRET` | valor de exemplo **versionado** | trocar por chave real fora do repositório |

## O que ainda não existe aqui

São gaps **planejados**, não esquecidos — a Issue 02 os cria, e ela é construção do usuário. Não
procure estes arquivos antes dela:

- **Sem `Dockerfile`.** Nenhuma imagem para Trivy varrer (Issue 06) nem para o DAST subir
  (Issue 09).
- **Sem `docker-compose.yaml`.** Redis roda no host até a Issue 02; o `REDIS_URL` default já
  aponta para `localhost:6379` por causa disso. Issue 02 — Redis sem porta publicada.
- **Sem `.env.example`.** A Issue 01 define o arquivo de ambiente de host; a 02 publica o exemplo.
- **Sem servidor HTTP, sem rota `/health` e sem `HEALTHCHECK`.** Issue 02.
- **Sem pipeline.** Issues 03 a 09 são o que existe de CI aqui: nada em
  [`.github/workflows/`](../.github/workflows/) roda para este app ainda — o único workflow do
  repositório é um stub sem passos.

## Rollout dos gates para os outros apps

Os gates DevSecOps nascem aqui (Issues 04 a 09) e são agnósticos de app. Portá-los para
[`ledger-service`](../ledger-service/AGENTS.md) ou [`commerce-api`](../commerce-api/AGENTS.md) é
**adaptar configuração, não reaprender a ferramenta** — o que muda está nesta tabela:

| Gate | Aqui (Node/TypeScript) | Ao portar |
|---|---|---|
| Segredos (`gitleaks`) | igual | nada muda, salvo a baseline |
| SAST (`semgrep`) | `p/javascript` | `p/java` |
| SCA de dependências | `npm audit` | OWASP Dependency-Check no Maven (`ledger`) |
| SCA de imagem (`trivy`) | imagem deste app | idêntico, só troca a imagem varrida |
| DAST (`zap`) | serviço sem autenticação | vira scan autenticado, porque o ledger usa JWT |

Vale a regra que dá nome à Issue 03: o build e o teste vivem em **scripts locais** e o workflow
só os chama. Um gate que só existe dentro do CI não é reproduzível na máquina.

## Onde o trabalho vive

| Caminho | O que é | Quando abrir |
|---|---|---|
| [`issues/`](issues/) | as 11 Issues da trilha, uma por capacidade, no template fixo | antes de implementar ou fechar qualquer Issue |
| [`estudos/`](estudos/) | material de estudo, um arquivo por Issue | quando precisar do passo a passo; a Issue nunca é a aula |
| [`../BOARD.md`](../BOARD.md) | status e ordem das 3 trilhas | para saber o que está feito e o que entra em seguida |
| [`../00-visao-geral.md`](../00-visao-geral.md) | metodologia, template, política de status | antes de escrever ou fechar uma Issue |

## Escopo de escrita

Gravável aqui: `issues/`, `estudos/`, `AGENTS.md` e `README.md`.

`app/` (código, `package.json`), os scripts de gate, `../.github/workflows/` e qualquer IaC são
**construção do usuário** — proponha o diff e espere o pedido explícito. Fechar uma Issue exige a
saída real do comando de validação colada em `## Evidências`; limitação de ambiente registrada em
`Limitações / notas` não vale como evidência.
