# Estudo — Resposta a incidente: medir, conter e aprender sob o cronômetro

> Material de estudo da Trilha 4. Acompanha a Issue 04 (exercício de incidente), mas não é
> o contrato daquela Issue: aqui é para entender, lá é para provar.

## Três horários, não dois indicadores: o que MTTD e MTTR significam quando viram números de VM

- **Por que importa:** "MTTD/MTTR" é jargão que aparece em vaga e em slide, e o erro
  comum é tratar como dois indicadores bonitos de painel. Sem traduzir para o lab, a coisa
  vira inútil das duas formas: quem ignora não consegue dizer *onde* o tempo foi perdido, e
  quem decora as siglas responde entrevista sem ter **um** registro com horário real. O custo
  concreto de medir mal: um drill que "deu tudo certo em 10 minutos" não ensina nada — 10
  minutos até **descobrir** é um problema; 10 minutos até **conter** é outro; 10 minutos até
  **verde** é um terceiro. Só o marcador separado mostra qual dos três dói.
- **Mecanismo:** a cadeia de um incidente tem quatro instantes e três intervalos, e cada
  intervalo é governado por uma peça **diferente** do lab — por isso eles se medem separados:

  | Instante | O que acontece | Quem/what marca | Governado por |
  |---|---|---|---|
  | `t0` | o gatilho é plantado (quem planta sabe) | commit do cenário / cronômetro | o exercício |
  | `t1` **detect** | o **sistema** grita primeiro | linha do alerta/gate/status page | **T3-03 / T4-02 / T4-03** |
  | `t2` **conter** | o estrago parou de crescer | sua ação + `date -Is` | o runbook (**T3-04**) |
  | `t3` **resolvido** | causa removida e estado verde provado | `ps` 6×healthy + gates | esta **T4-04** |

  ```text
  t0 ──(tempo cego: ninguém sabe que começou)──► t1 detect
  t1 ──(diagnóstico + contenção)──► t2 conter      =  MTTD  e  "até conter"
  t2 ──(correção + verificação)──► t3 resolvido    =  "conter → resolvido" (MTTR real)
  ```

  Cause e efeito de cada intervalo:

  - **`t0 → t1` (MTTD)** é medido em **sistemas**: alerta com `for: 1m`, gate que falha o
    job, status page `degraded`. Nenhum comando seu encurta ele — só configuração de sinal.
  - **`t1 → t2` (detect → conter)** é medido em **procedimento**: quantos minutos até o
    estrago parar. Enquanto ele corre, a causa pode nem ser conhecida — e **não precisa
    ser** (tópico seguinte).
  - **`t2 → t3` (conter → resolvido)** é medido em **correção**: entender a causa, agir,
    provar verde. Aqui sim entra diagnóstico profundo.

  A regra que vem disso: se você só anotou "começo" e "fim", os três problemas se misturam e
  a conclusão vira opinião ("demorei porque fui devagar"), quando na verdade demorou porque
  **o sinal demorou** (um problema da T3-03) ou porque **faltou passo no runbook** (um
  problema da T3-04). Sem marcador, nada disso aparece.
- **Exemplo no lab:** os três marcadores de um drill real, com os comandos que marcam cada
  um — repare que cada horário vem de uma **evidência**, não da memória de quem executou:

  ```bash
  # t0 — o cenário está declarado no repo ANTES de executar (evidence: commit datado)
  git add docs/cenario-drill.md && git commit -m "drill: cenário secret vazado (T4-04)"

  # t1 — detect: o PRIMEIRO sinal, lido do sistema (não do executor olhando)
  date -Is   # → 2026-10-08T14:03:12-03:00  (timestamp da linha do gate/alerta)
  curl -s http://127.0.0.1:9090/api/v1/alerts | jq '.data.alerts[] | {name: .labels.alertname, state: .state}'

  # t2 — conter: ação registrada com horário próprio
  date -Is   # → 2026-10-08T14:06:40-03:00  (ex.: rotação/revogação aplicada, serviço parado)

  # t3 — resolvido: verde PROVADO, não acreditado
  date -Is   # → 2026-10-08T14:19:05-03:00
  ssh -o BatchMode=yes lab@<ip-da-vm> 'docker compose -f ~/lab/compose.yaml ps'   # → 6×healthy
  ssh -o BatchMode=yes lab@<ip-da-vm> 'sudo ufw status numbered'                  # → 22,80,443
  ```

  E o registro resultante, o formato que a Issue pede em `docs/incidentes.md`:

  ```markdown
  ## 2026-10-08 — secret no repo (cenário T4-04)
  - t0 plantado: 13:55 (commit do cenário)
  - detect: 14:03:12 — **gate de vazamento vermelho no job** (sinal, não o executor)  → MTTD 8 min
  - conter: 14:06:40 — valor revogado/rotacionado (passo X do RUNBOOK §rotação)        → detect→conter 3,5 min
  - resolvido: 14:19:05 — gates verdes + ps 6×healthy + ufw 22/80/443                 → conter→resolvido 12,5 min
  - total t0→t3: 84 min
  ```
- **Fronteira entre Issues:** **fazer o sinal existir** (alerta `for: 1m` entregando no
  canal, status page `ok|degraded`) é da **T3-03/T3-04**; **os gates que podem ser o t1**
  (varredura de secret, Trivy) são da **T4-02/T4-03**; **a borda auditável que o fecho
  confere** (`ss`/`ufw`) é da **T4-01**. O que esta **T4-04** entrega é o **cronômetro nos
  3 marcadores e o registro da timeline** — medir é desta Issue, fazer o que é medido já
  existir é das outras. Métrica de **sistema** (CPU, latência) continua sendo **T3-02**:
  aqui quem é cronometrado é a **resposta**, não a máquina.

## Conter antes de corrigir: a ordem que a pressão humana inverte

- **Por que importa:** o instinto de quem está aprendendo — e o pior instinto de quem está
  sob pressão — é **entender primeiro**: abrir logs, caçar a causa, montar teoria. Enquanto
  isso, o estrago continua crescendo: o secret continua valendo, o disco continua enchendo,
  a porta continua aberta. A ordem correta é a oposta do conforto intelectual: **parar o
  estrago com a ação mais barata e reversível**, e só então investigar com calma. Quem inverte
  paga com dano acumulado; em lab o dano é pequeno e por isso é o lugar barato de aprender
  a inverter a ordem **antes** de produção.
- **Mecanismo:** contenção e correção são duas ações diferentes, com riscos diferentes e
  critérios de "pronto" diferentes:

  | | **Contenção** | **Correção** |
  |---|---|---|
  | pergunta | como faço o estrago **parar de crescer**? | por que aconteceu e como **não repete**? |
  | exige entender a causa? | **não** — age no sintoma | sim |
  | ação típica no lab | revogar/rotacionar, parar serviço, bloquear, desligar o gerador | limpar histórico, corrigir regra, rotação de log, ajustar gate |
  | critério de pronto | o dano parou (horário anotado) | estado verde **provado** + causa removida |
  | risco de errar | baixo e reversível | alto se feito às pressas no meio do estrago |

  ```text
        sintoma aparece (t1)
              │
      ┌───────┴────────┐
      ▼                ▼
  CONTER            CORRIGIR-DIRETO
  ação barata,      investigar a causa
  reversível,       enquanto o estrago
  sem diagnosticar  ainda cresce
      │                 │
      ▼                 ▼
  estrago parou     dano acumulado +
  (t2)              diagnóstico feito
      │             sob pressão (pior
      ▼             qualidade, mais
  diagnosticar      tempo = maior MTTR)
  com calma
      │
      ▼
  corrigir → verde (t3)
  ```

  Duas armadilhas clássicas da ordem invertida:

  - **conter "de propósito" a mais:** derrubar a stack inteira (`docker compose down`) para
    "testar" apaga a evidência que o diagnóstico ia mostrar e derruba os 5 serviços
    saudáveis junto — contenção é **mínima e cirúrgica**, não é apagar tudo.
  - **corrigir sem conter:** em secret, "limpar o arquivo do repo" **não** conteria nada —
    o valor continua válido no histórico e em quem já copiou. A contenção é **revogar o
    valor** (rotacionar); a correção é limpar o passado. Ordem importa: quem só limpou
    fingiu resolver.
- **Exemplo no lab:** a sequência de um dos cenários da Issue, com o passo do runbook
  citado — a contenção vem **antes** de qualquer `logs`:

  ```bash
  # t1 chegou (gate vermelho / alerta no canal)

  # 1) CONTER — parar o estrago, sem saber ainda "quem plantou e por quê"
  ssh -o BatchMode=yes lab@<ip-da-vm> 'cd ~/lab && docker compose stop app'   # isola o que está causando dano
  date -Is                                # marca t2 — o horário da contenção, não do entendimento
  # cenário secret: conter = rotacionar (T4-02), gerar e aplicar o valor novo ANTES de investigar:
  #   openssl rand -base64 48  →  aplicar via fluxo da T2-03 → health 200 → token antigo 401

  # 2) CORRIGIR — agora, com o estrago parado, investiga-se sem pressão
  ssh -o BatchMode=yes lab@<ip-da-vm> 'docker compose -f ~/lab/compose.yaml logs app --tail 100'

  # 3) VERIFICAR — verde provado, marca t3
  ssh -o BatchMode=yes lab@<ip-da-vm> 'docker compose -f ~/lab/compose.yaml ps'   # → 6×healthy
  ssh -o BatchMode=yes lab@<ip-da-vm> 'sudo ufw status numbered'                  # → 22,80,443
  ```

  O runbook da **T3-04** já ensinou a ordem de **diagnóstico** (barato → caro); esta Issue
  acrescenta a ordem de **ação** (conter → corrigir). As duas se encaixam: conter é um passo
  de mitigação, e o diagnóstico profundo é o que vem depois dele.
- **Fronteira entre Issues:** **o procedimento de mitigação escrito** (seção "Mitigação" do
  formato da **T3-04**) é o que é copiado aqui — a T4-04 **usa** o runbook, não o reescreve;
  **a rotação que serve de contenção em secret** é a capacidade entregue pela **T4-02**;
  **rollback de deploy** (conter um deploy ruim) é da **T2-04**; **limpeza de disco/rotação
  de log** é da **T3-01**. Esta Issue entrega o **costurar na ordem certa + cronômetro**:
  conter está no caminho entre `detect` e `resolvido`, e a ordem é registrada na timeline.

## O sinal primeiro: se o humano viu antes do sistema, o sistema falhou

- **Por que importa:** a linha mais valiosa de um `docs/incidentes.md` é a primeira — e ela
  tem que ser uma **linha do sistema** (alerta, gate, status page) com timestamp **anterior**
  a qualquer comando seu. Se a primeira linha é o seu `docker compose ps`, o que falhou não
  foi a resposta: foi a **detecção**, e você acabou de exercitar "eu sou o sensor", que não
  escala, não dorme em plantão e não existe em produção. O drill só separa "tenho ferramenta"
  de "sei operar" se o gatilho for descoberto **pelo lab**, não pela pessoa que plantou.
- **Mecanismo:** existem três primeiras linhas possíveis, e elas dizem quem descobriu o
  incidente — só a primeira é o objetivo:

  | Quem gritou primeiro | Como a timeline fica | O que isso significa |
  |---|---|---|
  | **sistema** (alerta `firing` / gate vermelho / page `degraded`) | `14:03 sinal → 14:05 primeiro diagnóstico` | ✅ o lab inteiro funcionou: T3-03 avisou, T3-04/T4-0x acusaram |
  | **o executor olhando** ("rodei `ps` e viu") | `14:05 executor olhou → 14:05 sem sinal` | ❌ a resposta até pode ser boa, mas a **detecção não foi testada** |
  | **o próprio plantador** (lembrou do que plantou) | `t0 = t1` | ❌ o exercício mediu memória, não detecção |

  A causa e efeito do atraso: o tempo `t0 → t1` é **ruído puro** — nenhum serviço precisa
  existir para encurtá-lo, exceto o sinal. Por isso a latência detect→reação é o número
  mais honesto do registro: ela é a soma de (a) quanto tempo o sistema levou para falar
  (`for:` + intervalo de avaliação, decisão da **T3-03**) e (b) quanto tempo **você** levou
  para reagir (procedimento da **T3-04**). Misturar os dois num "10 minutos" esconde qual
  dos dois conserta.

  Os três tipos de sinal que o lab tem para ser a primeira linha:

  - **alerta** (Prometheus → Alertmanager → canal): grita para `up == 0`, 5xx, disco —
    latência declarada por `for:` (T3-03);
  - **gate** (CI/job que **falha**): grita no momento exato do commit/push — é o sinal mais
    rápido possível para secret (T4-02) e CVE na imagem (T4-03);
  - **status page** (`ok|degraded`): grita para quem **olha** — é a linha de quem passou por
    perto (T3-04), mais fraca que as outras duas, e honesta sobre isso.
- **Exemplo no lab:** o teste de "quem falou primeiro" aplicado ao cenário — a evidência é o
  **par de timestamps** (sinal × primeira ação), que é o que a Issue exige como resultado
  esperado:

  ```bash
  # o sinal (exemplo: gate da T4-02 no job, ou alerta da T3-03) — lido DO SISTEMA
  curl -s http://127.0.0.1:9090/api/v1/alerts \
    | jq -r '.data.alerts[] | "\(.labels.alertname) \(.state)"'
  # → AlvoCaido firing            (ou: job de scan terminou vermelho com o achado)

  # a PRIMEIRA ação de diagnóstico do responder — precisa vir DEPOIS
  date -Is; ssh -o BatchMode=yes lab@<ip-da-vm> 'docker compose -f ~/lab/compose.yaml ps'
  # → 14:05:xx  (se este horário for anterior ao do sinal, a timeline está invertida:
  #              o humano viu antes do sistema = a detecção NÃO foi testada)
  ```

  E o critério que separa os dois casos é de registro, não de opinião: na timeline, a linha
  do alerta/gate tem que existir **com horário anterior** ao primeiro comando. Se não
  existir, a honestidade manda escrever na entrada: "detecção pelo executor — sinal não
  acusou", e isso vira **lacuna** (ver o tópico seguinte), não se passa por drill bom.
- **Fronteira entre Issues:** **fazer o alerta disparar e entregar** é da **T3-03**;
  **os gates que podem ser o t1** (varredura de vazamento, Trivy) são da **T4-02/T4-03** —
  aqui eles só são **exercitados** com um gatilho plantado; **a status page** é da
  **T3-04**; **observador externo** (blackbox/heartbeat que avisaria se a VM inteira cair)
  continua **fora** — declarado como limitação na T3-03 e aqui: um cenário plantado
  **dentro** do perímetro testa a detecção interna, não a da borda (T0/T1).

## Exercício adversarial: quem planta já sabe a resposta — e a surpresa controlada tem limite

- **Por que importa:** o estado atual da trilha é "4 alertas e 1 drill básico — sempre com
  o solver sabendo o que ia acontecer". Sabendo, a pessoa **finge detecção**: olha o canal
  com um olho, sabe qual serviço vai cair e já tem a mão no mouse para o `start`. Isso
  treina a resposta (utiliza) e **não** treina nada da descoberta — e é a descoberta que
  custa caro em produção, onde ninguém sabe qual serviço caiu. A separação
  `executor ≠ solver` existe porque detecção só se testa em quem **não** está esperando.
- **Mecanismo:** a surpresa controlada é um desenho de papéis, não uma brincadeira:

  ```text
  quem PLANTA (t0)          quem RESOLVE (t1 → t3)
  - escolhe o cenário        - não sabe qual é (ideal)
  - declara NO REPO          - recebe só o sintoma
    (data + porquê),         - segue o runbook com
    sem dizer o gatilho        cronômetro
  - não interfere            - detecção tem que vir
                               do sinal, não dele
  ```

  Duas variáveis se combinam, e **cada uma mede uma coisa diferente**:

  | | solver ≠ executor | solver = executor (repo solo) |
  |---|---|---|
  | o que é testado | **detecção de verdade** (sintoma → "o que é isso?") + resposta | só o **sinal** (o gate/alerta acusou?) + resposta |
  | o que **não** é testado | nada de grave — mas o executor ainda tem a vantagem de conhecer o lab | a sensação de "não sei o que aconteceu" |
  | como declarar | ideal, com outras pessoas ou sessão futura | obrigatório escrever a limitação no registro |

  O que a surpresa controlada **ainda não testa**, mesmo no caso ideal — anotar isso é
  parte do exercício:

  - **o perímetro externo:** o gatilho é plantado **dentro** do lab já defendido — port scan
    real, brute force são teste ofensivo com VPS (fora, declarado);
  - **a pressão de verdade:** não há usuário esperando nem status page pública reclamando —
    ver o tópico seguinte;
  - **a falha do monitorador:** se o Prometheus (ou o CI) estiver caído **junto** com o
    cenário, ninguém grita — a limitação "o alerta não avisa de si mesmo" (**T3-03**) está
    viva aqui também;
  - **adivinhação:** em repo solo é fácil deduzir o cenário (só existem 4 clássicos: secret,
    porta, CVE, disco) — por isso a declaração `quem plantou × quem resolveu` é obrigatória:
    sem ela, o resultado parece melhor do que é.
- **Exemplo no lab:** o formato da provocação escondida — o gatilho (um dos 4 clássicos)
  declarado **antes**, mas o **sintoma** é a única coisa que o solver recebe:

  ```markdown
  <!-- docs/cenario-drill.md — commitado ANTES da execução -->
  ## Cenário 2026-10-08 — porta fora da régua
  - Por que representa risco real do lab: a T4-01 auditou `ss -tlnp`/`ufw` como régua;
    uma porta nova sem justificativa é exatamente o drift que a auditoria existe para pegar.
  - Gatilho: plantado pelo executor em data marcada; solver recebe só o sintoma.
  - Detectado por: gate/auditoria (T4-01) — não pelo executor olhar.
  ```

  ```bash
  # executor (t0, escondido do solver) planta o gatilho na VM ...
  ssh -o BatchMode=yes lab@<ip-da-vm> 'sudo ss -tlnp | grep -E ":(3000|9090)\b"'   # estado que vai mudar

  # solver (t1 em diante) recebe só o sintoma e procura o primeiro sinal:
  ssh -o BatchMode=yes lab@<ip-da-vm> 'sudo ufw status numbered'                   # → 22,80,443 (ou a surpresa)
  ssh -o BatchMode=yes lab@<ip-da-vm> 'sudo ss -tlnp'                              # → a escuta nova aparece aqui
  ```

  Se o `ss`/`ufw` acusarem **antes** de o executor apontar, a régua da **T4-01** foi
  exercitada como detecção; se o solver foi direto no comando certo porque **lembrou do
  que plantou**, isso se escreve na entrada como limitação — não se conta como detecção.
- **Fronteira entre Issues:** o **drill básico** (alerta disparado de propósito, com o solver
  sabendo) é da **T3-04** — aqui é o mesmo método com gatilho **escondido**; **as defesas
  que podem acusar** (auditoria de porta, gate de secret, Trivy) são das **T4-01/T4-02/
  T4-03**; **ataque real de fora** está declarado fora do escopo desta Issue (seria teste
  ofensivo, estágio com VPS); **caos engenharia / múltiplos cenários simultâneos** também
  está fora — 1 cenário profundo é o bastante.

## A lacuna anotada é a entrega: o drill que "deu tudo certo" é o que menos ensinou

- **Por que importa:** a tentação de todo exercício é o cenário bonito — nada falhou, tudo
  saiu no cronômetro, e o registro vira uma linha de vitória. Mas o que a ferramenta, o
  runbook e o alerta **não cobriram** só aparece quando algo escapa; e é justamente essa
  informação que some, porque anotar "faltou X" parece fracasso. O valor do drill não está
  no cenário que deu certo, está no **registro do que faltou** — é ele que transforma o
  exercício em melhoria versionada e em história de entrevista com substância.
- **Mecanismo:** o loop é o mesmo da T3-04 ("corrigir o runbook, não o drill"), agora com
  o campo mais amplo de um incidente — a lacuna pode nascer de **qualquer** elo da cadeia:

  ```text
  drill executado ──► algo escapou (passo inexistente, sinal lento, comando quebrado)
                          │
                          ▼
              registrar em docs/incidentes.md  (>= 1 lacuna OBRIGATÓRIA)
                          │
            ┌─────────────┴──────────────┐
            ▼                            ▼
     corrigir aqui, no elo certo    virar ISSUE da fila
     (runbook → diff; alerta        (trabalho grande demais
     → alert_rules.yml; gate        para o drill — a T4-04
     → workflow)                    GERA trabalho, não o conclui)
            │                            │
            └────────────► próximo drill começa de pé ◄──────┘
  ```

  Cada elo tem um **lugar certo** para a correção — anotar "faltou" sem destino é anotação
  morta:

  | A lacuna foi em... | Correção vai para... |
  |---|---|
  | passo do procedimento (faltou, estava errado, ordem errada) | `RUNBOOK.md` (diff, mesma trilha da T3-04) |
  | sinal demorou ou não existiu (humano viu primeiro) | `alert_rules.yml`/gate da T3-03/T4-02/T4-03 |
  | ferramenta que devia acusar e não acusou | defesa da T4-01/T4-02/T4-03 → nota/issue |
  | coisa grande demais para concluir no drill | **issue na fila** (fora de escopo declarado) |

  E o efeito na **entrevista**: o que se pergunta sobre incidente é sempre a mesma sequência
  — *o que aconteceu, como vocês ficaram sabendo, o que fizeram primeiro, quanto tempo, e o
  que você mudou depois*. Repare que 4 das 5 perguntas respondem exatamente com os marcadores
  e a linha do sinal; a última só tem resposta se existir **lacuna registrada e corrigida**.
  Contar "fiz um exercício e deu certo" responde zero; contar "o gate gritou em 8 minutos,
  conteni em 3,5, faltou o passo X e virou PR no runbook" é a resposta completa — porque
  vem de evidência versionada, não de memória.
- **Exemplo no lab:** o registro com lacuna e o seu destino, mais o fecho que prova que o
  exercício **não deixou lixo**:

  ```markdown
  ## 2026-10-08 — imagem com CRITICAL rodando (cenário T4-04)
  - detect 14:03:12 (gate Trivy vermelho no job) → conter 14:06:40 → resolvido 14:19:05
  - Runbook usado: RUNBOOK §rotação (passos 1–3 copiados)
  - Lacuna 1: nenhum procedimento cobria "gate vermelho no CI" — o RUNBOOK só parte de
    alerta no canal → destino: diff no RUNBOOK.md (novo §gate-scan)
  - Lacuna 2: re-scan manual não estava em passo nenhum → destino: issue na fila
    (re-scan agendado é escopo da T4-03, não do drill)
  ```

  ```bash
  # fecho: o exercício deixa tudo verde (nada de lixo para o próximo)
  ssh -o BatchMode=yes lab@<ip-da-vm> 'docker compose -f ~/lab/compose.yaml ps'   # → 6×healthy
  ssh -o BatchMode=yes lab@<ip-da-vm> 'sudo ufw status numbered'                  # → 22,80,443
  # gates T4-02/T4-03 de volta a verde no último run
  git add docs/incidentes.md docs/cenario-drill.md RUNBOOK.md && git commit -m "drill T4-04: timeline + 2 lacunas"
  git diff --stat -- src/ pom.xml    # → vazio: app intocado (o drill não mexe no app)
  ```

- **Fronteira entre Issues:** o **formato de registro de drill com cronômetro e lacuna** nasce
  na **T3-04** — aqui ele cresce para `docs/incidentes.md` com a **linha do sinal primeiro**
  e os **3 marcadores**; **corrigir a lacuna de defesa** (gate/alerta) é das Issues que a
  entregaram (T4-01..03), por isso a lacuna grande vira **issue da fila** e não escopo deste
  exercício; **post-mortem blameless formal com time** está fora do escopo declarado (o
  "time" é 1 pessoa) — o formato é treinado, a dinâmica de time não. E a fronteira mais
  honesta de todas: **exercício ≠ incidente real** — aqui não há usuário esperando nem
  pressão de negócio; o que se treina é o **procedimento e a confiança nele**, e a pressão
  de verdade só existe com sistema em produção. Dito isso em voz alta na entrevista é o que
  separa honestidade de enrolação.

## Como iniciar o modo teach-anything

- "Me ensina MTTD/MTTR na prática com os 3 marcadores (detect/conter/resolvido), montando
  uma timeline real de drill com `date -Is`, `docker compose ps` e `ufw status` desta VM"
- "Me ensina contenção vs. correção: a ordem parar-estrago-depois-entender, aplicada a um
  secret vazado (rotacionar da T4-02) e a um serviço problemático no `compose.yaml` do lab"
- "Me ensina o sinal primeiro: como provar na timeline que o alerta/gate/status page falou
  antes do meu primeiro comando, usando o `/api/v1/alerts` do Prometheus e o job de scan"
- "Me ensina exercício adversarial: por que quem planta não testa detecção, como desenhar
  `executor ≠ solver` num repo solo e o que a surpresa controlada ainda não cobre"
- "Me ensina a lição de drill: escrever o `docs/incidentes.md` com timeline e ≥1 lacuna,
  decidir o destino de cada lacuna (diff no `RUNBOOK.md` × issue na fila) e contar o
  incidente na entrevista sem jargão vazio"
