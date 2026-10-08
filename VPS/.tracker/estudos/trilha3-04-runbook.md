# Estudo — Runbook, drill e resposta a incidente: transformar alerta em ação

> Material de estudo da Trilha 3. Acompanha a Issue 04 (runbook + drill + status page), mas
> não é o contrato daquela Issue: aqui é para entender, lá é para provar.

## Runbook é procedimento executável, não explicação

- **Por que importa:** a Trilha 3-03 terminou com 3 alertas chegando ao canal e **ninguém com
  escrito o que fazer em seguida**. Sem resposta pronta, o alarme vira ansiedade: quem recebe
  abre os 6 dashboards na ordem que lembra, tenta `restart` porque "sempre resolve", e descobre
  o procedimento *durante* a crise — pagando o custo de erro com o sistema caído. Documentação
  de arquitetura explica *como o sistema é*; em crise ninguém tem tempo de ler explicação. O
  que serve às 3h da manhã é a receita: isto chega → faça isto → meça isto → desista aqui.
- **Mecanismo:** o formato fixo da Issue 04 tem quatro campos, e cada um existe porque mata uma
  forma específica de travar:

  | Campo | Pergunta que responde | O que acontece sem ele |
  |---|---|---|
  | **Sintoma** | o que exatamente chega no canal? | quem lê não sabe se é *este* alerta que o procedimento trata |
  | **Diagnóstico** | 3–5 comandos **na ordem**, do mais barato ao mais caro | improviso: a pessoa pula direto pro comando favorito (logs de 10 GB, `tcpdump`) |
  | **Mitigação** | o que restaura o serviço | diagnóstico bonito e ninguém sabe o botão que volta o `200` |
  | **Escalação** | quando parar de insistir | o caso difícil vira 40 minutos de teimosia sem registro |

  A causa do formato é uma: **crise degrada cognição**. Sob pressão, a memória de trabalho
  encolhe — por isso o procedimento é escrito *antes*, com calma, e na crise é só copiado. Um
  runbook que precisa ser interpretado falha exatamente no momento de uso; um runbook com
  comandos copiáveis falha menos.
- **Exemplo no lab:** um procedimento inteiro do `RUNBOOK.md`, nascido do alerta `up == 0`
  (alvo caído) da T3-03 — repare que os nomes batem, o alerta aponta pro runbook e o runbook
  parte do alerta:

  ```markdown
  ## Alvo caído (`up == 0`)

  ### Sintoma
  Mensagem no canal: "[FIRING] alvo caído: app (up == 0 por 1m)"

  ### Diagnóstico (nesta ordem)
  1. ssh -o BatchMode=yes lab@<ip-da-vm>                       # estado geral, o mais barato
  2. docker compose -f ~/lab/compose.yaml ps                   # quem está morto de verdade
  3. docker compose -f ~/lab/compose.yaml logs app --tail 100  # só se o ps mostrou Restarting
  4. curl -fsS -m 3 http://127.0.0.1:9090/api/v1/query?query=up # o Prometheus concorda?

  ### Mitigação
  docker compose -f ~/lab/compose.yaml start app
  curl -fsS -m 3 http://app:8080/api/v1/actuator/health         # critério de resolvido: 200
  # esperar `resolved` chegar no canal (for: 1m + intervalo de evaluate)

  ### Escalação
  3 tentativas de start sem health 200 → parar, anotar e abrir issue com o que foi visto.
  ```

  Note o critério de "resolvido" no fim: sem ele, o drill termina quando a pessoa *acredita*
  que resolveu, não quando o sistema prova.
- **Fronteira entre Issues:** o **alerta** que chega (regra `for: 1m`, severidade, entrega no
  canal) é da **T3-03** — o runbook nasce *dos* alertas que já existem, não inventa os seus; os
  **comandos de observabilidade** que o diagnóstico usa (`logs`, queries do Prometheus) vêm das
  **T3-01** e **T3-02**; **restaurar automático** (rollback que decide sozinho) foi assunto da
  **T2-04** — aqui a mitigação é humana, de propósito; **post-mortem formal com timeline** está
  fora desta Issue (é Trilha 4 / estágio com operação real); **backup de dado perdido** é a
  **Trilha 0-05** — runbook não recupera dado, ele evita downtime.

## Diagnóstico em ordem de custo: o barato primeiro, o detalhe depois

- **Por que importa:** o erro clássico de quem está aprendendo é abrir o `logs` da app primeiro
  — ou pior, mergulhar em `tcpdump`/heap dump porque "é mais técnico". Cada comando tem custo:
  tempo até resposta, volume de saída, e o risco de levar a uma conclusão errada. Em lab o
  custo é minutos perdidos; em operação é downtime. Pior: o comando caro frequentemente
  *esconde* a resposta — 500 linhas de stack trace de warning fazem a pessoa caçar o erro no
  lugar errado, enquanto o `ps` mostraria que o problema é outro serviço.
- **Mecanismo:** a ordem é um filtro decrescente — cada passo só acontece se o anterior não
  respondeu, e cada passo descarta uma camada inteira de causa:

  | Ordem | Comando | Custo | O que ele responde |
  |---|---|---|---|
  | 1 | `ssh lab@<ip>` | ~2s | a VM está viva? (rede/DNS/SSH) |
  | 2 | `docker compose ps` | ~1s | qual serviço está `Exited`/`Restarting`? |
  | 3 | `docker compose logs <svc> --tail 100` | ~1s | *por que* aquele serviço morreu |
  | 4 | `curl` no health/Prometheus | ~1s | o sistema inteiro concorda com o diagnóstico |
  | 5 | `df -h`, `docker stats`, `tcpdump` | caro | só quando 1–4 apontam pra esse direção |

  A regra causal: **estado geral antes do detalhe**. Se 4 de 6 containers estão `Up` e 1 está
  `Exited (137)`, os logs daquele 1 têm 90% de chance de conter a resposta; se você abriu os
  logs de um serviço `Up`, está lendo o arquivo errado. O `ps` custa um segundo e decide qual
  dos 6 logs vale a pena abrir — começar por ele é comprar a informação mais cara (qual é a
  camada caída) pelo preço mais barato.
- **Exemplo no lab:** a ordem rodando de verdade durante o drill (alerta provocado com
  `docker compose stop app`):

  ```bash
  # 1) estado geral — 2 segundos, define a camada
  ssh -o BatchMode=yes lab@<ip-da-vm> 'docker compose -f ~/lab/compose.yaml ps'
  # → app        Exited (137) 30s ago          ← a camada é o container app
  #   postgres   Up 2 hours (healthy)
  #   redis      Up 2 hours (healthy)
  #   prometheus Up 2 hours
  #   grafana    Up 2 hours

  # 2) detalhe só do suspeito — tail, não histórico inteiro
  docker compose -f ~/lab/compose.yaml logs app --tail 100

  # 3) confirmação de fora: o sistema (Prometheus) vê o mesmo?
  curl -s 'http://127.0.0.1:9090/api/v1/query?query=up{job="app"}'
  # → {"status":"success",...,"value":[...,"0"]}   ← confirma; não é rede, é o alvo

  # 4) mitigar e provar com o critério de resolvido
  docker compose -f ~/lab/compose.yaml start app
  curl -fsS -m 3 http://app:8080/api/v1/actuator/health   # → {"status":"UP"}
  ```

  O `df -h` (disco ≥ 90% é outro alerta da T3-03) não aparece na ordem dele porque o `ps`
  descartou disco como causa — ele sobe pro passo 1 **se** o alerta for o de disco. Ordem de
  custo é por procedimento, não absoluta.
- **Fronteira entre Issues:** **analisar logs como investigação** é a **T3-01** (aqui o log é
  só passo 3 de um roteiro); **as queries que confirmam o estado** são da **T3-02**; o que esta
  Issue entrega é a **ordem fixa no runbook** — o hábito de nunca começar pelo comando caro.
  Disco cheio por *log sem rotação* é assunto de infraestrutura (Trilha 0/4), não do runbook:
  o runbook só trata o sintoma.

## Drill como teste do runbook: o cronômetro aprova o documento, não a pessoa

- **Por que importa:** um runbook nunca escrito é como teste que nunca roda — parece pronto e
  está cheio de erro. Comandos mudam de nome (`compose.yaml` vira `docker-compose.yml`),
  porta some, caminho quebra, e **nobody descobre até a crise real**. O drill é o que converte
  "escrito" em "testado": a Issue 04 exige um alerta disparado de propósito, o runbook seguido
  **à risca**, cronômetro ligado e o tempo até diagnóstico registrado. Sem drill, o runbook é
  papel — e papel não resolve alerta.
- **Mecanismo:** o drill mede duas coisas e produz uma terceira:
  1. **Tempo até diagnóstico** — quantos minutos entre o alerta e "sei qual serviço e por quê".
     É a métrica do runbook, não da pessoa: se demorou, o roteiro não guiou.
  2. **Tempo até `resolved`** — a cadeia completa alerta → runbook → mitigação → resolved.
  3. **Lacunas** — todo drill encontra pelo menos uma coisa que o runbook não cobriu (comando
     que falhou, campo faltando, ordem errada). Aí vem a regra mais importante: **corrigir o
     runbook, não o drill**. Se o comando do runbook não funcionou, a culpa é do documento — a
     correção entra no `RUNBOOK.md` com diff versionado. "Eu sabia pular essa linha" é trapacear
     no próprio teste: a próxima pessoa vai seguir a linha quebrada.

  ```text
  drill = teste de aceitação do runbook
  lacuna encontrada ──► corrigir RUNBOOK.md (diff) ──► retestar no próximo drill
                     └─► NÃO registrar "drill falhou por X" e seguir   (isso é esconder)
  ```
- **Exemplo no lab:** registro do drill, o formato que a Issue 04 pede (data, alerta, tempos,
  lacuna):

  ```markdown
  ## Drill 1 — 2026-10-08 — alerta `up == 0`
  - Gatilho: `docker compose -f ~/lab/compose.yaml stop app` às 14:02
  - Alerta no canal: 14:03 (for: 1m)
  - Diagnóstico ("app Exited (137)"): 14:05 — **3 min** desde o alerta
  - Mitigação + health 200: 14:07 — resolved no canal 14:08 — **5 min** total
  - Lacunas: (1) passo 2 do runbook dizia `docker-compose` (com hífen) e a VM usa
    `docker compose`; (2) não cobria como confirmar o `resolved` — corrigir no RUNBOOK.md
  ```

  As duas lacunas viram diff no `RUNBOOK.md` no mesmo commit do registro — o runbook sai do
  drill melhor do que entrou, que é o ponto da Issue.
- **Fronteira entre Issues:** **disparar e entregar o alerta** (ciclo firing/resolved no canal)
  é a **T3-03** — o drill *reaproveita* esse ciclo, não o cria; **medição de métrica de
  sistema** (CPU, latência) é a **T3-02** — aqui quem é medido é o **procedimento**; drill de
  **rollback** (deploy revertendo sozinho) é da **T2-04**; post-mortem formal e timeline de
  incidente real estão **fora do escopo** desta Issue — o registro do drill é o análogo em
  ambiente sem usuário esperando. Drill em lab é exercício: o que se treina é o procedimento,
  não o estresse (a pressão humana só existe com produção de verdade).

## Status page: "ok" prova o que ela consulta, e nada além disso

- **Por que importa:** todo mundo quer dizer "está tudo no ar" sem logar na VM. Mas uma página
  de status é o item mais fácil de **autoengano** do lab: ela mostra verde, a pessoa relaxa, e
  o verde pode significar desde "stack inteira saudável" até "o script de checagem quebrou e
  devolve `ok` por default". A Issue 04 fecha o pacote com uma página simples justamente para
  ensinar o que ela **não** prova — porque acreditar em mais do que ela dá é dormir com risco
  invisível.
- **Mecanismo:** a página é um agregador: ela responde a **uma** pergunta, com **uma** fonte.
  Neste lab a fonte é o Prometheus da mesma VM, consultando `up` dos alvos:

  | Situação real | O que a página mostra | Por quê |
  |---|---|---|
  | stack no ar, alvos `up == 1` | `ok` | a consulta retorna tudo 1 |
  | `docker compose stop app` | `degraded` | `up{job="app"} == 0` vira 0 |
  | **VM inteira caiu** | **nada** (timeout) | quem responde a página morreu junto |
  | Prometheus caiu, app ok | erro/`degraded` de mentira | a fonte sumiu — ela só sabe o que consulta |

  Duas consequências de causa e efeito: (1) ela só sabe o que **consulta** — se o script checa
  `up` mas não checa o health da app, app devolvendo `500` com scrape vivo pode aparecer `ok`;
  (2) ela roda **na mesma VM** que ela observa — se a VM cair, a página cai junto (aí o
  "degraded" verdadeiro vira timeout, que é um sinal, mas não um status). Por isso a limitação
  está declarada na Issue: esta página prova "o stack local responde", **não** disponibilidade
  de fora. Monitorar de fora (blackbox externo, status page pública) é estágio com VPS real.
- **Exemplo no lab:** o script servido em loopback — mesma ideia do resto da trilha, borda
  **zero** no ufw (22/80/443 continua a régua):

  ```bash
  #!/usr/bin/env bash
  # status.sh — devolve ok|degraded consultando o Prometheus da mesma rede
  set -euo pipefail
  PROM="http://127.0.0.1:9090/api/v1/query"
  DOWN=$(curl -fsS -m 3 --data-urlencode 'query=sum(up == 0)' "$PROM" \
         | grep -o '"value":\[[^,]*,"[0-9]*"' | grep -o '[0-9]*"$' | tr -d '"')
  if [ "${DOWN:-1}" = "0" ]; then echo "ok"; else echo "degraded"; fi
  ```

  Prova dos três estados, na ordem que a Issue exige:

  ```bash
  curl -s 127.0.0.1:<porta-status>                      # → ok
  docker compose -f ~/lab/compose.yaml stop app
  curl -s 127.0.0.1:<porta-status>                      # → degraded (reflete a realidade)
  docker compose -f ~/lab/compose.yaml start app
  curl -s 127.0.0.1:<porta-status>                      # → ok de novo
  ufw status                                            # → inalterado: 22,80,443 — nenhuma borda nova
  ```

  Se a página mostrasse `ok` com o app parado, ela seria pior que inútil: seria mentira
  verde — o mesmo erro que a **T2-04** baniu do deploy (`|| true`), agora no(relatório de)
  uptime.
- **Fronteira entre Issues:** **as métricas** que a página lê são da **T3-02**, **os alertas**
  que avisam antes de alguém olhar a página são da **T3-03** — status page não substitui
  alerta, é a resposta passiva para "posso dar uma olhada?"; **exposição pública com domínio**
  (statuspage.io etc.) está **fora do escopo** desta Issue; abrir porta nova no ufw continua
  sendo regra da **Trilha 0-03** — por isso aqui é loopback/tunnel.

## Escalação: o ponto onde insistir é pior que reportar

- **Por que importa:** o campo **escalação** é o que separa operar de teimar. Sem ele
  escrito, o comportamento humano na crise é o pior possível: a pessoa continua tentando o
  mesmo comando com variações, porque parar parece admitir derrota — e cada minuto insistindo é
  minuto sem registrar o que já foi tentado, sem avisar quem precisa saber, com a janela de
  decisão fechando. No lab não existe time para chamar, mas **o hábito** é exatamente o que se
  treina aqui: a Issue 04 diz que escalação no lab é *parar e anotar* — e a seção existe porque
  saber o ponto de desistir é o que separa quem opera de quem só insiste.
- **Mecanismo:** escalação é um limiar decidido **antes**, não um estado de ânimo:

  ```text
  tentativa 1 → tentativa 2 → tentativa 3 (ou: X minutos sem health 200)
                                    │
                ┌───────────────────┴────────────────────┐
                ▼                                        ▼
        ainda dentro do runbook                 escalação: PARAR
        (seguir o próximo passo)                - registrar o que já foi feito
                                                - registrar o que foi descartado
                                                - abrir issue / avisar (no lab: nota)
                                                - NÃO inventar passo novo no meio da crise
  ```

  A causa e efeito: passos **fora** do runbook não são cobertos por ninguém (você os inventou
  sob pressão, sem terceiro olho) e não ficam registrados — se derem errado, o próximo a
  seguir o procedimento parte de um estado sujo sem saber. Já o *parar e anotar* produz
  exatamente o insumo do loop de melhoria da Issue: o que você anotou na escalação vira a
  lacuna do drill, que vira o diff no `RUNBOOK.md`. Escalação não é fracasso, é o canal pelo
  qual a operação aprende.
- **Exemplo no lab:** a seção de escalação do procedimento "5xx em rajada", com limite
  explícito e registro:

  ```markdown
  ### Escalação
  - Limite: 3 mitigações sem `200` estável, ou 10 min sem resolved → parar.
  - Registrar em issue: o alerta, os comandos executados (saída), o que já foi descartado
    (ex.: "disco ok, DB healthy, proxy 200"), a hipótese atual.
  - O que NÃO fazer no meio: mudar regra de alerta, `docker compose down` da stack inteira,
    mexer no ufw — cada um desses apaga evidência ou derruba os outros 5 serviços.
  ```

  Repare nos "não fazer": escalação também protege a **coleta de evidência**. Quem derruba a
  stack inteira para "testar" destrói o estado que o diagnóstico ia mostrar — é o equivalente
  operacional a colar printer de suporte no console.
- **Fronteira entre Issues:** **plantão, escalação humana de verdade e PagerDuty** estão fora
  desta Issue (não existe time — declarado em `Fora de escopo`); **silenciar alerta** durante
  manutenção (`amtool silence`, documentado na T3-03 para vir aqui) é ferramenta do
  Alertmanager, não escalação; **auto-remediação** (heal que decide sozinho) é estágio futuro —
  a T2-04 já deu o exemplo em deploy com rollback automático; **resposta a incidente formal**
  (post-mortem, comunicação a usuário) é tema da **Trilha 4**. Esta Issue entrega o limite
  escrito e o hábito de usá-lo.

## Como iniciar o modo teach-anything

- "Me ensina a diferença entre runbook e documentação, montando o procedimento de `alvo caído`
  no formato sintoma → diagnóstico → mitigação → escalação com os comandos desta VM"
- "Me ensina diagnóstico em ordem de custo usando a stack do lab: por que `docker compose ps`
  antes de `logs`, e como a ordem muda entre os 3 alertas da T3-03"
- "Me ensina a usar o drill como teste do runbook: cronômetro, tempo até diagnóstico, o que
  anotar como lacuna e por que a correção vai no `RUNBOOK.md` e não no registro do drill"
- "Me ensina o que uma status page `ok|degraded` consulta no Prometheus e por que ela cai
  junto quando a VM cai — com os três estados testados no loopback"
- "Me ensina escalação: como escrever o limiar de parar num procedimento e transformar o 'pare
  e anote' em melhoria versionada do runbook"
