# Estudo — Rollback: o deploy que se desfaz sozinho e ainda conta que desfez

> Material de estudo da Trilha 2. Acompanha a Issue 04 (rollback automático com health gate),
> mas não é o contrato daquela Issue: aqui é para entender, lá é para provar.

## Rollback é só um deploy para trás — e a tag imutável é o que existe para voltar

- **Por que importa:** o deploy da Issue 03 troca a imagem e espera o `200`; quando o health
  não vem, o job fica vermelho **e a VM fica com a versão ruim rodando**. Nesse momento a
  pergunta "o que eu volto?" vira arqueologia: `git reflog`, `docker inspect`, "qual era o SHA
  do último merge bom mesmo?". Enquanto alguém escava, a `notes-api` continua respondendo
  `Connection refused` ou `500` — o downtime é a escavação, não a falha.
- **Mecanismo:** o rollback não é um procedimento especial, é o **mesmo** caminho do deploy
  rodando na direção oposta — `pull` de uma tag, `up -d`, poll de health. O que muda é a tag.
  E só é possível porque a **Issue 02** publica `ghcr.io/<owner>/notes-api:<sha>` com tag
  **imutável**: o binário de ontem ainda está lá, com o mesmo nome, com o mesmo digest. Tag
  `latest` não serve para voltar — ela é reescrevível, e "voltar para o `latest`" pode ser
  voltar para exatamente a versão que acabou de quebrar.

  | Passo | Deploy (Issue 03) | Rollback (Issue 04) |
  |---|---|---|
  | de onde venho | — (não pergunta) | lê o `Image` da VM **antes** da troca |
  | o que faz | `pull <sha-novo>` → `up -d` | `pull <sha-anterior>` → `up -d` |
  | quem decide se deu certo | poll do health | poll do health (outra vez) |
  | exit | `0` se `200` | `1` **sempre**, mesmo com `200` no fim |

  A causa é única: o "para onde voltar" precisa ser **gravado antes** de qualquer coisa mudar.
  A Issue 04 chama isso de "todo deploy tem um `de onde eu venho`" — sem essa linha no log,
  não há rollback automático, há tentativa.
- **Exemplo no lab:** o `scripts/deploy.sh` estendido. Repare que o compose da VM referencia
  `image: ghcr.io/<owner>/notes-api:${APP_TAG}` (padrão da Issue 02), então trocar de versão
  é trocar uma variável — por isso rollback é literalmente "deploy com outra tag":

  ```bash
  #!/usr/bin/env bash
  # scripts/deploy.sh — Issue 03 (caminho feliz) + Issue 04 (caminho infeliz)
  set -euo pipefail

  HEALTH="https://<ip-da-vm>/api/v1/actuator/health"
  TIMEOUT=60 INTERVAL=2
  TARGET="${GITHUB_SHA}"
  VM="lab@<ip-da-vm>"
  COMP="docker compose -f ~/lab/compose.yaml"

  run_vm() { ssh -o BatchMode=yes "$VM" "$@"; }

  # 1) "de onde eu venho" — ANTES de qualquer troca
  PREVIOUS=$(run_vm "$COMP ps -q app | xargs docker inspect -f '{{.Config.Image}}'")
  echo "previous: ${PREVIOUS}"
  echo "previous=${PREVIOUS}" >> "$GITHUB_OUTPUT"   # se a lógica for no workflow

  # 2) a troca — mesmo caminho da Issue 03
  run_vm "APP_TAG=${TARGET} $COMP pull app && APP_TAG=${TARGET} $COMP up -d app"

  # 3) health gate (tópico seguinte) e 4) rollback = deploy com a tag anterior
  if ! wait_healthy; then
    echo "rolling back to ${PREVIOUS}"
    run_vm "APP_TAG=${PREVIOUS} $COMP pull app && APP_TAG=${PREVIOUS} $COMP up -d app"
    wait_healthy || true
    echo "rolled back to ${PREVIOUS}"
    exit 1
  fi
  echo "deployed ${TARGET}"
  ```

  Sequência que a evidência da Issue 04 exige no log:
  `previous: ghcr.io/...:abc1234` → `pull nova` → `gave up after 60s` → `rolled back to
  ghcr.io/...:abc1234` → `200` → `exit 1`.
- **Fronteira entre Issues:** **ler e gravar o SHA anterior + reverter** é desta Issue 04; o
  `pull`/`up -d` que ele reaproveita, o `scripts/deploy.sh` e o `SSH_PRIVATE_KEY` são da
  **Issue 03** (rollback **manual**, declarado em documento); a tag SHA imutável que torna o
  revert possível é da **Issue 02**. Blue-green e canary — trocar versão sem downtime nenhum —
  são estágio AWS, fora do lab.

## O health gate precisa de um relógio: timeout declarado vs. espera infinita

- **Por que importa:** "esperar o `200`" sem teto é esperar para sempre. O job do Actions é
  morto por timeout global (horas) e, pior, **ninguém sabe quando desistir** — o sistema
  ficou ruim, mas o pipeline ainda está "tentando", o time não foi chamado, e a janela de
  decisão virou indefinida. Um timeout com número no log (`gave up after 60s`) é o que
  transforma "não respondeu" em **fato datado**.
- **Mecanismo:** o poll tem três constantes declaradas — total, intervalo e tempo limite de
  **cada** requisição. Causa e efeito:

  | Decisão | Se der errado |
  |---|---|
  | `TIMEOUT=60`, `INTERVAL=2` | ~30 tentativas, falha conhecida em ≤ 60s |
  | sem timeout (loop `while true`) | job pendurado até o runner matar; log sem causa |
  | `curl` sem `-m` | uma conexão que não fecha trava a tentativa inteira |
  | `\|\| true` / `curl ... \|\| echo falhou` no fim da cadeia | o passo **sempre** sai `0` → mentir verde |

  O `set -euo pipefail` do script merece cuidado: ele mata o script no primeiro `curl`
  mal-sucedido, **antes** do rollback rodar — por isso a falha do poll entra num `if !` e não
  num comando solto. E "falhar feio" é a escolha oposta de "mentir verde": o exit do script é
  a única coisa que o Actions lê, então quem transforma uma exceção em `exit 0` está
  escolhendo esconder a crise.
- **Exemplo no lab:** o poll do `deploy.sh`, com o `curl -m` limitando cada tentativa e o
  total contado em segundos:

  ```bash
  wait_healthy() {
    local deadline=$(( $(date +%s) + TIMEOUT ))
    while [ "$(date +%s)" -lt "$deadline" ]; do
      if curl -fsS -m 3 "$HEALTH" > /dev/null; then
        echo "health 200 before timeout"
        return 0
      fi
      sleep "$INTERVAL"
    done
    echo "gave up after ${TIMEOUT}s"     # a linha que a evidência procura
    return 1
  }
  ```

  Teste de fogo: publicar tag boa com env inválida (`SPRING_PROFILES_ACTIVE=inexistente` no
  run) para o app subir e nunca responder `200` — aí o log mostra o `gave up after 60s` com
  valor, não um loop.
- **Fronteira entre Issues:** o `HEALTHCHECK` **do container** (`interval`/`retries` no
  compose, `pg_isready`, `/api/v1/actuator/health` herdado da **Issue 01**) é o Docker na VM
  perguntando de dentro; o health gate é o **runner** perguntando de fora, via proxy da
  Trilha 1-03 — são dois relógios, e só o segundo decide o exit do job. Auto-heal por métrica
  (Prometheus ordenando o rollback) é **Trilha 3**; aqui quem decide é o script.

## Consertar e reportar: por que o job termina vermelho mesmo com a VM healthy

- **Por que importa:** se o pipeline saísse verde depois de um rollback, ele estaria dizendo
  "esta versão foi implantada" enquanto a VM roda a versão **anterior**. A branch protection
  da **Issue 01** deixaria seguir, o próximo merge partiu de um "sucesso" que não houve, e o
  time dorme tranquilo com um release que nunca chegou. Verde com rollback é a mentira mais
  cara do CI porque ela **acalma**.
- **Mecanismo:** o critério de sucesso do job não é "o sistema está de pé" (está — o rollback
  garantiu), é "**a versão nova serviu**". São duas perguntas diferentes, e a segunda é a que
  o pipeline responde:

  | Situação | Sistema no ar | Exit do `deploy.sh` | Cor do job |
  |---|---|---|---|
  | deploy novo, `200` | versão nova | `0` | verde |
  | health falhou → rollback ok → `200` | versão **anterior** | `1` | **vermelho** |
  | health falhou → rollback também falhou | qualquer um | `1` | vermelho |
  | script "resolve" com `\|\| true` | versão anterior | `0` | verde (mentira) |

  A consequência prática: um run vermelho **com sistema de pé** é um sinal legível — "o
  deploy tentou e desistiu, a VM está na versão boa". Um run verde é ambíguo. Por isso a
  Issue 04 exige que a falha seja **reprodutível**: `Re-run jobs` no mesmo run tem que repetir
  a sequência e o mesmo vermelho, senão vira fantasma.
- **Exemplo no lab:** o workflow, com o exit do script sendo a única voz do job:

  ```yaml
  jobs:
    deploy:
      needs: [test, build-push]
      if: github.ref == 'refs/heads/main' && github.event_name == 'push'
      runs-on: [self-hosted]      # runner do host (T2-03) — só ele alcança a VM
      steps:
        - uses: actions/checkout@v4
        - name: Deploy com health gate
          run: bash scripts/deploy.sh        # exit 0 = implantou; exit 1 = rollback feito
          env:
            SSH_PRIVATE_KEY: ${{ secrets.SSH_PRIVATE_KEY }}
  ```

  E a prova no estado da VM, feita **depois** do run vermelho — o contraste é o argumento
  inteiro deste tópico:

  ```bash
  # run vermelho no Actions, mas a máquina não ficou pendurada
  ssh -o BatchMode=yes lab@<ip-da-vm> 'docker compose -f ~/lab/compose.yaml ps'
  # → ghcr.io/<owner>/notes-api:<sha-anterior>   Running (healthy)
  ```
- **Fronteira entre Issues:** **exit ≠ 0 após rollback** é desta Issue; o `exit ≠ 0` do
  caminho feliz (deploy sem `200`) já é da **Issue 03**, que declarou "falha feia, não meio
  sucedido". Avisar gente (Slack/e-mail) quando isso acontece é **Trilha 3** — aqui o
  relatório é a cor do job e a linha `rolled back to <sha>` no log. Exigir o verde antes do
  merge continua sendo a **Issue 01** (branch protection).

## O código volta, o dado não: a fronteira entre deploy de app e deploy de schema

- **Por que importa:** rollback automático conserta **indisponibilidade**, não **dados**.
  Se a versão nova subiu, rodou o Flyway e gravou algo errado antes de o health falhar, o
  `pull` da tag anterior devolve o binário — e nada mais. O pior caso não é a coluna extra
  esquecida: é a versão nova ter **removido** coluna que a antiga ainda usa — aí o rollback
  "deu certo" (app antiga de pé, healthy) e mesmo assim toda query explode com `500`, porque
  o código voltou para um banco que já não é o dele.
- **Mecanismo:** deploy de app é uma troca de endereço (reversível: a tag antiga ainda está
  no GHCR); deploy de dado é uma **escrita** (sem desfazer — a linha foi inserida, a coluna
  foi criada, o `flyway_schema_history` registrou `V3` como executada). É a fronteira clássica:

  | | Deploy de app (Issues 02–04) | Deploy de schema (Flyway) |
  |---|---|---|
  | operação | trocar imagem | rodar `V<n>__*.sql` no Postgres |
  | reversível? | sim, por tag imutável | não, por padrão (sem undo) |
  | quem executa | `deploy.sh` no runner | a própria app, no boot |
  | se der errado | rollback automático | forward-only: só para frente |

  O que vem **depois** é o desenho que torna o rollback de código inofensivo — **expand/
  contract**: primeiro *expand* (adicionar coluna nullable, sem tocar nas antigas; código novo
  lê as duas formas), trocar a versão, e só então *contract* (remover a coluna velha), nunca
  na mesma release. A regra prática: **a versão nova tem que rodar com o banco da versão
  antiga**. Neste lab as migrações são aditivas e não causam dor — mas é exatamente por isso
  que a Issue 04 registra a limitação em vez de ignorá-la.
- **Exemplo no lab:** as migrações reais, vistas com o olho do rollback:

  ```sql
  -- src/main/resources/db/migration/V2__create_notes_table.sql
  CREATE TABLE "notes" (
      "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      ...
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
  );
  ```

  Rodou uma vez na VM da Trilha 1-02 (Flyway no boot da `notes-api`). Se uma futura `V3`
  fizesse `ALTER TABLE notes DROP COLUMN content`, o fluxo seria:

  ```text
  deploy da V3  →  app nova de pé, health 200  →  (nada a rolar)
  deploy quebrado → rollback de CÓDIGO       →  app velha de pé
                     ...mas "content" já não existe  →  500 em toda nota
  ```
- **Fronteira entre Issues:** **rollback de migração (Flyway undo) está fora do escopo desta
  Issue** — ela cobre só a imagem. Restaurar dado é o hábito da **Trilha 0-05** (backup é o
  único caminho para dado perdido) e o assunto do backup de banco é **Trilha 3**; a evolução
  de schema pensada para compatibilidade (expand/contract) entra junto, depois que a app já
  tem deploy automático. Auto-heal que decide sozinho é **Trilha 3**.

## Onde mora a lógica do rollback: `if: failure()` no workflow ou dentro do `deploy.sh`

- **Por que importa:** as duas posições funcionam, e a Issue 04 exige **decidir e declarar
  uma** — ter as duas duplica o caminho: o script já revertu e o passo do workflow reverte
  de novo (ou pior, reverte para o alvo errado, já que o `PREVIOUS` lido de novo agora é a
  versão nova que está de pé). Duplicar não é ter dupla segurança, é ter duas verdades.
- **Mecanismo:** a diferença é **quem sabe o que aconteceu**. O script viu a sequência inteira
  (SHA anterior, `gave up`, exit); o workflow só enxerga o exit code do passo — menos um
  detalhe, mais visibilidade na UI.

  | | Dentro do `deploy.sh` | `if: failure()` no workflow |
  |---|---|---|
  | visibilidade | tudo numa linha de log do mesmo passo | passo próprio, visível na UI do Actions |
  | conhecimento | o script já tem o `PREVIOUS` em memória | precisa de `outputs:` do passo anterior |
  | teste | `bash scripts/deploy.sh` na mão, sem CI | só existe dentro do Actions |
  | armadilha | quem lê o YAML não vê que existe rollback | `failure()` pega **qualquer** falha (ssh caiu, checkout falhou) — e aí não há o que reverter |
  | porta | roda em outra CI ou no SSH manual | específico do GitHub Actions |

  A armadilha do `if: failure()` tem uma segunda metade: se a falha acontecer **antes** de o
  SHA anterior ser capturado, o passo de rollback roda sem alvo — por isso a guarda precisa
  ser dupla (`failure()` **e** alvo preenchido). Já no script, o `if ! wait_healthy; then ...`
  é uma cadeia única: ou a sequência inteira acontece, ou o script nem chegou lá.
- **Exemplo no lab:** as duas posições, para comparar lado a lado — escolha **uma**:

  ```yaml
  # Posição A — o script é o dono do rollback (um único caminho, recomendado aqui)
        - name: Deploy com health gate e rollback
          run: bash scripts/deploy.sh          # sai com 1 depois de reverter

  # Posição B — o workflow é o dono (passo separado, precisa do alvo)
        - name: Deploy
          id: deploy
          run: bash scripts/deploy.sh          # só troca + health; exit 1 se não 200
        - name: Rollback
          if: failure() && steps.deploy.outputs.previous != ''
          run: bash scripts/rollback.sh "${{ steps.deploy.outputs.previous }}"
  ```

  Em B, a linha `echo "previous=$PREVIOUS" >> "$GITHUB_OUTPUT"` **antes** da troca é o que
  permite ao passo seguinte saber o alvo — é o mesmo "de onde eu venho" do tópico 1, mudando
  de memória de shell para output do job.
- **Fronteira entre Issues:** o **conteúdo** do rollback (captura do SHA, caminho `pull`/`up`,
  exit ≠ 0) é da **Issue 04** em qualquer posição; o job e o script que ele chama são da
  **Issue 03**. O que é **fora** do lab: CD de verdade com orquestrador (ArgoCD, Flux — ele
  é que declara "estado desejado × estado real" e converge sozinho) é estágio futuro; alerta
  de rollback é **Trilha 3**.

## Como iniciar o modo teach-anything

- "Me ensina rollback automático na `notes-api`: ler o SHA anterior da VM, reverter com a tag imutável do GHCR e provar com o log `previous` → `gave up` → `rolled back to`"
- "Me ensina health gate com timeout no `scripts/deploy.sh`: poll declarado (`TIMEOUT`/`INTERVAL`), `curl -m` por tentativa e por que `|| true` é mentir verde"
- "Me ensina por que o job `deploy` termina vermelho mesmo com a VM healthy, olhando o exit do `deploy.sh` e o `docker compose ps` depois da crise"
- "Me ensina rollback de app × rollback de schema com as migrações `V1__`/`V2__` deste repo e o que seria expand/contract nelas"
- "Me ensina `if: failure()` no workflow vs. lógica dentro do `deploy.sh` — o trade-off de ter os dois caminhos"
