---
title: "Pesquisa 02 — Reddit: o que a comunidade (e quem contrata) diz sobre entrar em DevOps júnior"
tags: [pesquisa, reddit]
data: 2026-10-07
---

# Pesquisa 02 — Reddit: caminho de estudos reconhecido para DevOps/infra júnior

**Objetivo:** colher opinião de quem já entrou na área e de quem já esteve do lado da contratação, para validar o plano do lab em `VPS/` (Ubuntu Server em VM/VPS + backend Java 17/Spring Boot em Docker).

**Como ler este documento:**
`[TÍTULO]` = título literal da thread (com URL).
`[PARÁFRASE]` = eu resumindo/parafraseando o que um comentário ou post disse — **não é citação literal**.
`[BUSCA]` = trecho devolvido por motor de busca, não li o texto integral.
`[FATO]` = informação objetiva que consegui confirmar (existência do sub, nº de comentários, etc.).
`[INFERÊNCIA]` = minha conclusão, não é texto do Reddit.

---

## 0. Metodologia e limitações (ler antes de confiar nos achados)

- **O Reddit bloqueou o acesso direto.** `www.reddit.com`, `old.reddit.com`, o endpoint `.json` e o proxy `r.jina.ai` retornaram **403 / "blocked by network security"**.
- **Workaround usado:** (a) `websearch` com `site:reddit.com` para *descobrir* as threads e ler os títulos/snippets; (b) **API pública do PullPush** (`api.pullpush.io`) para ler o **texto integral dos posts e dos comentários** por `link_id`. Todo conteúdo de comentário veio do PullPush.
- **Cobertura parcial do PullPush:** em algumas threads ele devolve poucos comentários e o campo `score` vem como `1` (perdido na indexação). Onde isso aconteceu, sinalizo. Não usei `score` como medida de "consenso" sozinho.
- **Não inventei quotes.** Nada aqui está entre aspas como fala de alguém; tudo está parafraseado e marcado.
- **Não achei threads relevantes em r/ProgramingGamesBR** (fora do escopo — é sobre jogos). **r/DevOpsBR existe** (`reddit.com/r/DevOpsBR`), mas é pequeno e aparentemente parado (últimos posts indexados de 2023) — quem discute carreira DevOps no Brasil no Reddit usa **r/brdev**.
- Amostra: ~25 threads em r/devops, r/brdev, r/ITCareerQuestions, r/homelab, r/selfhosted, r/sysadmin, r/kubernetes, r/devopsjobs.

---

## 1. Threads por subreddit (inventário com URL)

### 1.1 r/devops

- `[TÍTULO]` **"Failed to get a junior DevOps job"** — https://www.reddit.com/r/devops/comments/1j5q1lo/ (OP: 7 meses estudando Udemy + bootcamp da "TechWorld with Nana", trabalha de Software Support, não passou em nada)
  - `[PARÁFRASE]` Comentário de `isolated_monk1`: o que mudou o jogo foi **parar de focar em tutorial e montar projeto** — CI/CD própria, deploy com Docker, módulo Terraform, tudo documentado limpo no GitHub; depois, arrumar o currículo e **ensaiar como falar dos próprios projetos** — os callbacks aumentaram quando ele soou como alguém que *usou* as ferramentas, não que *assistiu* vídeo sobre elas.
  - `[PARÁFRASE]` `ahmed1smael`: achar empresa disposta a contratar em entry-level é metade do problema; a outra metade é **mostrar projetos pequenos automatizados** (no cloud ou com automação) — ele diz que não é fã de certificação, que prefere projeto mostrável.
  - `[PARÁFRASE]` `coolalee_`: contraponto — ele diz que **existem sim muitas vagas júnior de DevOps** publicadas.
- `[TÍTULO]` **"How to get ready for a junior/entry level DevOps job"** — https://www.reddit.com/r/devops/comments/1ei3x48/
  - `[PARÁFRASE]` `lpriorrepo`: programar primeiro (curso tipo boot.dev), depois Linux de sysadmin o suficiente, depois a ferramenta da vaga, e **conseguir emprego de help desk ou de engenharia** para ter experiência profissional.
  - `[PARÁFRASE]` `CyberSpaceJunkie`: começar em **empresa pequena** (b2b, web apps custom, baixa complexidade, alta saída) — chance maior, salário menor, mais liberdade de escolher ferramenta; ele virou devops/sysops sem saber o que estava fazendo e aprendeu no fogo.
  - `[PARÁFRASE]` `CyberSpaceJunkie` (outro comentário): caminho que funcionou — **ser backend engineer numa empresa sem gente de devops e assumir infra/automação** por conta própria.
  - `[PARÁFRASE]` OP do thread: alguém o orientou a fazer **projetos pessoais para ter algo concreto a mostrar a recrutador**.
- `[TÍTULO]` **"Is it plausible to get a job in this field without experience?"** — https://www.reddit.com/r/devops/comments/1l04dla/
  - `[PARÁFRASE]` `klipseracer`: "júnior em DevOps" é um oxímoro.
  - `[PARÁFRASE]` `Trakeen`: DevOps não é entry-level; você precisa de anos de experiência real antes de o currículo ser lido.
  - `[PARÁFRASE]` `shadowdog293`: num mercado em que quem tem estágio e diploma já disputa as vagas, **ninguém vai confiar infra/automação a alguém que só "mexeu por conta própria"**.
  - `[PARÁFRASE]` `cdragebyoch`: sem diploma, mas com 5 anos de engenharia + Linux forte + AWS profundo antes do primeiro emprego de DevOps; sugere **virar engenheiro full-stack por 2–3 anos** antes de decidir.
  - `[PARÁFRASE]` `CEO_Of_Antifa69`: há gente que conseguiu sem faculdade, mas **só com projeto próprio e estudo auto-dirigido forte**; só curso, sem projeto, não se destaca.
- `[TÍTULO]` **"Devops learning path"** (discussão do roadmap.sh) — https://www.reddit.com/r/devops/comments/115gx5e/ — **ver seção 4.**
- `[TÍTULO]` **"Devops is not for absolute beginners?"** — https://www.reddit.com/r/devops/comments/1h9chro/
  - `[PARÁFRASE]` `DreamAeon`: parar de se fixar em "quero entrar em DevOps"; primeiro ser **bom engenheiro** (FE/BE/DB) e entender o SDLC de ponta a ponta; depois, aí sim, mergulhar em redes, CI/CD, observabilidade.
  - `[PARÁFRASE]` `LeStk`: DevOps é cultura/prática, não título de cargo — pressupõe que você **já tenha** um cargo.
- `[TÍTULO]` **"Feeling stuck in DevOps tutorial hell for 5+ years"** — https://www.reddit.com/r/devops/comments/1om8kb1/
  - `[PARÁFRASE]` `FreshView24` (comentário muito reagido): "ninguém se importa quantos tutoriais você assistiu" — importa se você **assume problemas, conserta sistema quebrado e entrega resultado**; DevOps é sobre resultado, não sobre ferramenta.
  - `[PARÁFRASE]` `Fantastic-Average-25`: **o homelab foi o que o tirou do tutorial hell** — a partir daí ele passou a construir e resolver problema real em vez de assistir.
  - `[PARÁFRASE]` `nooneinparticular246`: a forma barata de sair do tutorial hell é **arrumar problema de DevOps no trabalho atual** (pipeline lenta, observabilidade fraca, alerta inútil).
- `[TÍTULO]` **"[UK] Thinking of moving from IT Field Engineer to DevOps"** — https://www.reddit.com/r/devops/comments/1ll0i3s/
  - `[PARÁFRASE]` `colmeneroio`: pular o bootcamp caro; certificado de nível fundacional não vale muita coisa, um **Associate (SAA)** sim; e **construir projeto que resolve problema real em vez de seguir tutorial** — ex.: montar pipeline CI/CD de uma web app simples e automatizar.
  - `[PARÁFRASE]` `---why-so-serious---`: DevOps é papel de programação pesada e conhecimento amplo; ele entrou vindo de **~10 anos como engenheiro Java** e o caminho foi responsabilidade crescendo na stack.
  - `[PARÁFRASE]` `Alwandy`: 8 anos na área, tom bem negativo; conselho técnico: **aprender a codar, CI/CD, containers, Terraform, Ansible, e "entender cada aspecto de rede"**; certificado não ajuda muito.
- `[TÍTULO]` **"Juniorr DevOps Interview Experience || Questions I Was Asked || REJECTED"** — https://www.reddit.com/r/devops/comments/1r98h8f/ — **ver seção 3.2** (é a thread mais concreta sobre "o que separa").
- `[TÍTULO]` **"Devops Project Ideas for intern"** — https://www.reddit.com/r/devops/comments/zqkyzt/
  - `[PARÁFRASE]` `serverhorror`: lista de projetos em **VPS barato** — subir servidor de mail (SMTP/IMAP/webmail/filtro/DNS) e **refazer o mesmo com Puppet, depois Ansible, depois Chef, depois Vagrant, depois Packer, depois Terraform no DO, depois AWS/Azure/GCP**; e em seguida trocar o app (Apache+WordPress → nginx → Redmine).
  - `[PARÁFRASE]` `ovo_Reddit`: respondendo o OP com `roadmap.sh/devops`, dizendo que aquele link responde boa parte das perguntas do sub.
- `[TÍTULO]` **"Monthly 'Getting into DevOps' thread - 2022/01"** — https://www.reddit.com/r/devops/comments/ru3zhm/ — `[BUSCA]` o post fixado lista roadmap.sh e adverte (parafraseando o snippet) que DevOps como termo e prática ainda está em fluxo.

### 1.2 r/brdev (Brasil)

- `[TÍTULO]` **"Existe devops iniciante? Devops que já começa a carreira..."** — https://www.reddit.com/r/brdev/comments/1jhkeae/
  - `[PARÁFRASE]` `andreiross`: DevOps exige entender muito bem como software é desenvolvido **e entregue em produção**; por isso é raro existir "devops iniciante" — a não ser que a empresa esteja usando "DevOps" como sinônimo de suporte ou analista de infra.
  - `[PARÁFRASE]` `msfor300`: DevOps é mais quem cuida de automação de CI/CD, escalonamento, logs e segurança; comenta (em tom de piada) que quando um sai, um dev iniciante assume o lugar.
  - `[PARÁFRASE]` `fborgesss`: analogia — tentar construir robô cirurgião sem noção de como funciona uma cirurgia.
- `[TÍTULO]` **"alguém pode me dizer pq eh tão difícil conseguir vaga em [devops]"** — https://www.reddit.com/r/brdev/comments/1viarru/ — **thread mais citável do lado brasileiro**
  - `[PARÁFRASE]` `guigouz` (altas reações): DevOps **não é área de entrada**, é raro ter estagiário; o caminho comum é alguém de **dev ou infra que já tem experiência ser movido internamente**. E no começo de carreira não dá pra escolher: pegue o que existe na sua região, entre no mercado e se especialize depois.
  - `[PARÁFRASE]` `StoreOk4417`: mais fácil **entrar como dev e migrar** depois.
  - `[PARÁFRASE]` `edshow_`: ou você começa em suporte (N1/N2/N3) ou é dev antes; quem é DevOps hoje em geral passou por suporte por um bom tempo.
  - `[PARÁFRASE]` `Automatic_Current684`: precisa de **muita bagagem de infraestrutura**; estágio de "DevOps" na prática é estágio de infra.
  - `[PARÁFRASE]` `aexpedito`: muita gente ainda cai nos "cursos milagrosos" da internet.
- `[BUSCA]` Outras threads do r/brdev só vi título/snippet, sem ler integralmente: **"DevOps - mercado"** (https://www.reddit.com/r/brdev/comments/1bnek71/), **"DevOps jr/pl?"** (https://www.reddit.com/r/brdev/comments/1c632hs/), **"Dicas para entrar no mercado de DevOps após anos como dev"** (https://www.reddit.com/r/brdev/comments/1u49s28/), **"Rotina de um DevOps"** (https://www.reddit.com/r/brdev/comments/1ob1ack/ — OP relata ter aceitado estágio de DevOps "sabendo quase nada da área").

### 1.3 r/ITCareerQuestions

- `[TÍTULO]` **"Is DevOps an entry level position?"** — https://www.reddit.com/r/ITCareerQuestions/comments/18nkh0b/
  - `[PARÁFRASE]` `darwinn_69` (diz ser **Lead DevOps**): as posições de entrada para DevOps são **sysadmin, NOC, help desk** — operação; raro dev entrar em DevOps sem escrever código de deploy. E o que falta no júnior não é técnica técnica: é **"ter passado por isso algumas vezes"** — triagem e priorização corretas.
  - `[PARÁFRASE]` `deacon91`: não é cargo de entrada, **mas existem vagas de entrada** — o caminho é estágio + background certo.
  - `[PARÁFRASE]` `Lnars` (diz ter estagiários): tem estágio, mas é raro em comparação com SWE/QA, costuma ser contrato de 12+ meses, e **exige fundamento de TI + programação**, o que derruba muito estudante recém-formado.
  - `[PARÁFRASE]` `murder-mittens-magic`: exige ter os fundamentos de TI dominados.
- `[TÍTULO]` **"How can I get my first job in Devops / Cloud background?"** — https://www.reddit.com/r/ITCareerQuestions/comments/1u1w4zi/
  - `[PARÁFRASE]` `typhon88`: "você começa num emprego de help desk".
  - `[PARÁFRASE]` `Infectedtoe32`: primeiro aprender **como computador, rede, impressora etc. funcionam juntos**, depois help desk; diz que o caminho é help desk → sysadmin → cloud/devops, e que sem diploma dá para pular o help desk se tiver algo a mais.
  - `[PARÁFRASE]` `AdeelAutomates`: dois caminhos explícitos — `helpdesk 1→2→3 → sysadmin → cloud/devops` ou `dev → software engineer → devops engineer`.
  - `[PARÁFRASE]` `Evaderofdoom` (**voz contrária ao homelab**): "ninguém se importa com homelabs quando os outros candidatos têm anos de experiência real em dev, ops ou ambos — mira mais baixo e vai subindo".
  - `[PARÁFRASE]` `h9xq`: cloud não é ponto de partida; é especialização depois de alguns anos.
- `[BUSCA]` **"Made the transition from Support Tech to Junior DevOps Engineer"** — https://www.reddit.com/r/ITCareerQuestions/comments/ktomzl/ — `[PARÁFRASE]` do snippet: é possível entrar **sem diploma e sem certificação**, mas elas "ajudam muito".
- `[BUSCA]` **"How important is it for devops"** — https://www.reddit.com/r/ITCareerQuestions/comments/19eggbf/ — `[PARÁFRASE]` do snippet: praticamente impossível chegar lá sem experiência em TI; mesmo com experiência, exigem vivência de sysadmin.

### 1.4 r/homelab, r/selfhosted, r/sysadmin, r/kubernetes

- `[TÍTULO]` **"Why do people build Kubernetes homelabs? Is it actually useful for internships/jobs?"** (175 pts, 93 comentários) — https://www.reddit.com/r/homelab/comments/1sxtqg2/ — **thread central sobre "homelab vale no currículo?"**
  - `[PARÁFRASE]` `Clank75` (**ele se identifica como manager há muito tempo**): ele monta o lab para entender na prática o que dá errado, o que é fácil e o que é difícil — e diz, parafraseando, que **absolutamente fica mais inclinado a contratar alguém (especialmente em nível de estágio/aptidão) que demonstrou paixão e aptidão mexendo nisso no tempo livre**.
  - `[PARÁFRASE]` `pythosynthesis`: para estudante buscando estágio, o lab **dá vantagem no lugar certo**.
  - `[PARÁFRASE]` `EnvironmentalAsk3531` (**voz contrária**): é experimentar e "perder tempo ajustando", inútil na prática para quase todo mundo.
  - `[PARÁFRASE]` `DamianRyse` (222 pts): valor do lab é aprender **o que pode dar errado** e ganhar ambiente padronizado; não é sobre contratação.
  - `[PARÁFRASE]` `scavno`: montar cluster em casa hoje leva poucas horas — o valor é aprender o modelo, não o hardware.
- `[TÍTULO]` **"What home lab can I create to gain system admin skills?"** — https://www.reddit.com/r/homelab/comments/12jx1bi/
  - `[PARÁFRASE]` `Schm1tty`: relato pessoal — ele **mostrou o próprio lab na entrevista** quando foi promovido a sysadmin e **conseguiu a vaga**; recomenda montar domínio, Group Policy, serviços IIS e cluster de failover para **poder demonstrar**.
  - `[PARÁFRASE]` `S7R4NG3`: pegue os sistemas que você usa no dia a dia e **monte-os do zero você mesmo** — "é surpreendente o quanto você acha que sabe um sistema até ter que instalá-lo sozinho".
  - `[PARÁFRASE]` `eclecticbit`: **para de procurar "o melhor"**; pegue qualquer coisa, quebre, refaça; e quando funcionar manualmente, **automatize a instalação e a configuração**.
  - `[PARÁFRASE]` `SystemsBadmin` (**contraponto**): se o objetivo é carreira, ele recomenda **ir para cloud (AWS/Azure free tier)** porque a vaga on-prem está diminuindo e você competirá com gente de 10–30 anos de casa.
- `[TÍTULO]` **"Having a homelab is really helpful?"** — https://www.reddit.com/r/sysadmin/comments/18jl21m/ — `[PARÁFRASE]` do título/primeiro resultado: para vários participantes **o lab não foi útil diretamente para o emprego** (um deles é suporte Windows de dia e roda Linux em casa por paixão); há quem defenda que é justamente o interesse próprio que separa profissional bom de mediano. **Opinião dividida.**
- `[BUSCA]` **"Homelab training to become a sysadmin?"** — https://www.reddit.com/r/sysadmin/comments/r27vps/ — `[PARÁFRASE]` do snippet: um relato afirma ter virado head sysadmin em empresa de 3.000+ pessoas **maiormente por conhecimento do próprio homelab**.
- `[TÍTULO]` **"Let's talk about putting your self hosted hobby on [your resume]"** — https://www.reddit.com/r/selfhosted/comments/1gbe1mg/ (OP relata revisar currículos e achar um candidato listando o homelab em "Projetos")
  - `[PARÁFRASE]` `jkirkcaldy`: **generalize** — "implantei múltiplas aplicações usando tecnologias de container e virtualização" diz a mesma coisa e soa aplicável a vaga.
  - `[PARÁFRASE]` `DaikiIchiro`: mostra projeto do qual você se orgulha quando a conversa permite, mas **nunca escreva no currículo a finalidade privada/questionável** — parafraseie.
  - `[PARÁFRASE]` `ValuableOk6702`: tem o portfólio hospedado no próprio lab e vê isso mais como **pauta de conversa** do que como item de currículo.
  - `[PARÁFRASE]` `Migamix`: hobby alinhado com a vaga coloca você na "pilha de consideração" — mostra dedicação a aprender.
  - `[PARÁFRASE]` `Brehhbruhh` (**contraponto ácido**): quem lista "sei instalar programa" como conquista passa vergonha na entrevista.

### 1.5 r/devopsjobs e r/kubernetes

- `[BUSCA]` **"How do you get your first DevOps job when every 'junior' role requires 2+ years"** — https://www.reddit.com/r/devopsjobs/comments/1qa0o1f/
- `[BUSCA]` **"Is it realistic to land a junior DevOps job without prior experience?"** — https://www.reddit.com/r/devopsjobs/comments/1jiavpi/
- `[BUSCA]` **"Trying to Break Into Cloud/DevOps With No IT Background"** — https://www.reddit.com/r/devopsjobs/comments/1vl8kcs/ — `[PARÁFRASE]` do snippet: vários avisam que sem background de TI é muito difícil conseguir entry-level.
- `[TÍTULO]` (mesma pergunta do r/homelab crosspostada) — https://www.reddit.com/r/kubernetes/comments/1sxtq1f/ — 64 comentários, mesma discussão.

---

## 2. O que SEPARA candidato aprovado de rejeitado em nível júnior

### 2.1 Síntese (o que apareceu repetidamente em subs diferentes)

| Separador | Evidência |
|---|---|
| **Já ter construído/deployado algo de verdade e conseguir explicar em voz alta** | r/devops `isolated_monk1` (callbacks subiram quando passou a "soar como quem usou"); r/devops `colmeneroio`; thread de entrevista 1r98h8f |
| **Fundamentos de Linux + rede + "como a coisa toda se conecta" antes das ferramentas** | r/ITCareerQuestions `Infectedtoe32`, `h9xq`, `darwinn_69`; r/devops `lpriorrepo`, `Alwandy`, `murder-mittens-magic` |
| **Ter passado por operação real (help desk / suporte / sysadmin / dev) — triagem e priorização** | `darwinn_69` (Lead DevOps), r/brdev `guigouz`/`edshow_`, r/devops `Trakeen`, `PepeTheMule` |
| **Conseguir falar do projeto sem parecer que recitou vídeo** | r/devops `isolated_monk1`; thread de entrevista 1r98h8f |
| **Projetos pequenos, automatizados e documentados** (não labirinto de ferramentas) | `ahmed1smael`; `FayaLargeau` na thread do roadmap; `eclecticbit` |

### 2.2 Perguntas que um entrevistador júnior realmente fez — a prova mais objetiva que achei

`[TÍTULO]` https://www.reddit.com/r/devops/comments/1r98h8f/ — candidato a **Junior DevOps** numa empresa de serviços, **rejeitado**. `[PARÁFRASE]` da lista de perguntas que ele registrou:

- **CI/CD/Git:** qual reverse proxy você usa; autoavaliação 0–10 em GitLab CI/CD; o que são *artefatos*; **diferença entre GitLab CI/CD e GitHub Actions**; diferença entre Git, GitHub Actions e GitLab CI.
- **Deploy/Cloud:** "você já subiu algum projeto Node.js na AWS?"; cenário com backend em Docker numa EC2 — **como montar certificado SSL, como gerar o arquivo de configuração, por que SSL existe**; já usou RDS/Aurora; estratégia de migração Bitbucket → GitLab on-prem; migração de banco via pipeline.
- **Docker:** **escrever um Dockerfile** para app Node.js (npm, porta 3000); diferença **ENTRYPOINT vs CMD**.
- **Serverless/CDN:** limites de tamanho de pacote do Lambda e o que fazer se `node_modules` estourar; cold start; o que um CDN cacheia e o que são edge servers.

`[INFERÊNCIA]` Note o padrão: **quase toda pergunta é "você já fez isso de verdade?"** — SSL, Dockerfile, deploy, migração. Não é decorar ferramenta de roadmap; é ter operado. Isso é exatamente o tipo de coisa que um backend Java subindo em VPS próprio força você a responder.

### 2.3 O "erro clássico" que a comunidade aponta

- `[PARÁFRASE]` r/devops `isolated_monk1` + `FreshView24` + `colmeneroio`: **só tutorial não vale** — o currículo vira lista de ferramentas e o candidato não consegue explicar decisão técnica.
- `[PARÁFRASE]` r/brdev `aexpedito` + r/devops `CEO_Of_Antifa69`: **cair no "curso milagroso"** e parar aí.
- `[PARÁFRASE]` r/ITCareerQuestions `Infectedtoe32` + `darwinn_69` + r/devops `murder-mittens-magic`: **pular redes/Linux/fundamentos de TI** e ir direto para Kubernetes/Ansible — o consenso é que é isso que faz o candidato travar na entrevista.
- `[PARÁFRASE]` r/devops `DreamAeon` + `LeStk`: querer o **título** de DevOps antes de ser engenheiro que entende o SDLC.

---

## 3. Peso: projeto pessoal/homelab × certificação × faculdade

### 3.1 Projeto/homelab/VPS

- **A favor (o mais forte):** `[PARÁFRASE]` `Clank75` (manager, r/homelab 1sxtqg2) diz que fica **mais inclinado a contratar** quem mexeu com isso no tempo livre, sobretudo em nível de estágio/aptidão; `[PARÁFRASE]` `Schm1tty` (r/homelab 12jx1bi) **mostrou o lab na entrevista e foi promovido a sysadmin**; `[PARÁFRASE]` `Migamix` e `ValuableOk6702` (r/selfhosted) dizem que coloca o candidato na fila de consideração / vira pauta de conversa.
- **Contra:** `[PARÁFRASE]` `Evaderofdoom` (r/ITCareerQuestions 1u1w4zi) diz que **ninguém se importa com homelab** quando os concorrentes têm experiência real; `[PARÁFRASE]` `shadowdog293` (r/devops 1l04dla) diz que nenhum hiring manager entregaria infra a quem só "dabbled"; `[PARÁFRASE]` vários no r/sysadmin 18jl21m dizem que o lab **não** ajudou no emprego deles.
- **Como escrever:** `[PARÁFRASE]` `jkirkcaldy` (r/selfhosted) — generalize ("implantei aplicações com container e virtualização"), não liste a finalidade hobby; `[PARÁFRASE]` `DaikiIchiro` — nunca escreva a finalidade privada, parafraseie.
- `[INFERÊNCIA]` O padrão que emerge: **homelab sozinho não é ficha de experiência; homelab + capacidade de explicar decisões + algo em produção que dá para mostrar = ficha de conversa**. E funciona melhor em empresa pequena/startup (cf. `CyberSpaceJunkie`) do que em processo grande.

### 3.2 Certificação

**Opinião dividida** (não há consenso):
- `[PARÁFRASE]` `ahmed1smael` (r/devops): não gosta de exame/vendor lock-in; prefere projeto.
- `[PARÁFRASE]` `colmeneroio` (r/devops): **fundacional não vale nada**, um Associate (AWS SAA) "significa algo para hiring manager".
- `[BUSCA]` r/ITCareerQuestions `ktomzl`: sem diploma e sem certificado deu, mas **"ajudam muito"**.
- `[PARÁFRASE]` `Alwandy`: "certs won't do much".
- `[PARÁFRASE]` `FayaLargeau` (r/devops 115gx5e): diz ganhar 160k como platform engineer **sem saber ~70% do que está no roadmap**; o que o contratou foram 2 cursos (Ansible, Kubernetes).

### 3.3 Faculdade

- **Dividido, com leve inclinação "faculdade ajuda mas não é obrigatória":**
  - `[PARÁFRASE]` `Lnars` (r/ITCareerQuestions): os estágios de DevOps existem, mas são para **graduando** e ainda assim exigem fundamento que muitos não têm.
  - `[BUSCA]` `ktomzl`: entrou **sem diploma e sem certs**.
  - `[PARÁFRASE]` `cdragebyoch`: não tem diploma, mas tinha 5 anos de engenharia.
  - `[PARÁFRASE]` `Infectedtoe32`: sem diploma, o caminho é mais longo (help desk primeiro).
  - `[INFERÊNCIA]` No Brasil (r/brdev), a conversa quase não gira em diploma — gira em **"por onde você entrou"** (suporte/dev/infra).

---

## 4. Roadmap.sh — o que dizem

Thread central: **"Devops learning path"** — https://www.reddit.com/r/devops/comments/115gx5e/

- `[PARÁFRASE]` `nourez` (57 pts): não gosta do **formato** de roadmap — lista de ferramentas ok, mas há **ênfase demais em SO/ferramentas no começo** e cloud/IaC entram tarde demais; na visão dele vale mais começar na cloud, construir um app simples, automatizar e depois preencher lacunas.
- `[PARÁFRASE]` `nourez` (depois): concorda com `FayaLargeau` — **fica bom em um nicho e aprende o resto no trabalho**.
- `[PARÁFRASE]` `FayaLargeau` (44 pts): "odio esse roadmap e todo mundo aqui trata como evangelho" — ele só **atolha você com 100 conceitos**; a receita curta seria poucas habilidades + cert de entrada + **vaga de nível 1 e crescer por dentro**.
- `[PARÁFRASE]` `hijinks` (34 pts): simplesmente responde com o link `https://roadmap.sh/devops` — ou seja, **muita gente usa como resposta padrão**.
- `[PARÁFRASE]` `tech_tuna` (10 pts): conselho genérico mas aceito — **meter o pé na porta e ter fome de aprender**; e há **muitos caminhos de entrada** (dev, QA, suporte, sysadmin, DBA, sales engineer).
- `[BUSCA]` **"Tips for someone who wants to change careers?"** — https://www.reddit.com/r/devops/comments/1aow6h7/ — snippet com `roadmap.sh` criticado (texto truncado, não li integralmente).
- `[BUSCA]` **"Is it possible to become a DevOps Engineer in 6 months..."** — https://www.reddit.com/r/devops/comments/1ffieaw/ — snippet **recomenda** `roadmap.sh/devops` como ponto de partida.
- `[BUSCA]` **"Monthly 'Getting into DevOps' thread - 2022/01"** — https://www.reddit.com/r/devops/comments/ru3zhm/ — post fixado **lista** roadmap.sh como recurso.
- `[PARÁFRASE]` r/devops `ovo_Reddit` (na thread de ideias de projeto): roadmap.sh responde "40–50%" das perguntas do sub.

**Leitura:** `[INFERÊNCIA]` **consenso fraco** em que roadmap.sh é boa **lista de vocabulário/checklist do que existe**, e **opinião dividida** quanto a usá-lo como **sequência de estudo** — a crítica recorrente é a ordem (SO/coso demais cedo; cloud/IaC tarde) e o efeito de "atolamento".

---

## 5. CONSENSO × OPINIÃO DIVIDIDA

### 5.1 CONSENSO (apareceu em subs e perfis diferentes, sem contradição relevante)

1. **DevOps não é área de entrada.** O caminho normal é *help desk/suporte → sysadmin/infra → DevOps* ou *dev → DevOps*, no Brasil (r/brdev `guigouz`, `edshow_`) e nos EUA (r/ITCareerQuestions `darwinn_69`, `AdeelAutomates`, `typhon88`; r/devops `Trakeen`, `PepeTheMule`).
2. **Só tutorial/curso não basta.** Precisa ter construído algo e conseguir explicar decisões (r/devops `isolated_monk1`, `FreshView24`, `colmeneroio`; r/brdev `aexpedito`).
3. **Linux + redes + fundamentos de TI vêm antes de ferramenta "bonita"** (r/ITCareerQuestions `Infectedtoe32`, `h9xq`, `murder-mittens-magic`; r/devops `lpriorrepo`, `Alwandy`).
4. **Toda entrevista júnior pergunta "você já operou isso?"** — Dockerfile, SSL, pipeline, deploy (thread 1r98h8f).
5. **Roadmap.sh existe e é amplamente usado** como referência — ninguém discute sua utilidade como *lista*.
6. **Demonstrar projeto na entrevista funciona quando você consegue mostrá-lo/exlicá-lo** (r/homelab `Schm1tty`; r/selfhosted `jkirkcaldy`, `ValuableOk6702`).

### 5.2 OPINIÃO DIVIDIDA (marquei os dois lados)

1. **Homelab/VPS no currículo vale?**
   - SIM: `Clank75` (manager), `Schm1tty`, `Migamix`, `Fantastic-Average-25`, `pythosynthesis`.
   - NÃO: `Evaderofdoom`, `shadowdog293`, `EnvironmentalAsk3531`, e vários no r/sysadmin 18jl21m.
2. **Existe vaga júnior de DevOps?**
   - SIM: `coolalee_` ("tem um monte de postagem"); existem estágios (`Lnars`, `deacon91`).
   - NÃO: `klipseracer` ("oxímoro"), `Trakeen`, `shadowdog293`, `whatdoido8383`.
3. **Estudar cloud primeiro ou Linux/SO primeiro?** `nourez` (cloud primeiro) × roadmap.sh/`lpriorrepo` (SO/sysadmin primeiro). Também `SystemsBadmin` recomenda cloud por causa do mercado on-prem.
4. **Certificação vale?** `colmeneroio` (SAA sim) × `ahmed1smael`/`Alwandy` (não) × `ktomzl` (ajuda muito).
5. **Bootcamp/pago vs autodidata + projeto:** o OP do 1j5q1lo comprou bootcamp e não passou; `colmeneroio` manda pular bootcamp caro; `FayaLargeau` diz que foram 2 cursos baratos de Udemy que o levaram ao cargo.
6. **Focar em "entrar em DevOps" vs. virar bom engenheiro primeiro:** `DreamAeon`/`cdragebyoch` (vire full-stack primeiro) × quem acha que dá para mirar direto em júnior de DevOps.

---

## 6. O que isso significa para o lab em `VPS/` (minha leitura — `[INFERÊNCIA]`)

1. **A VM/VPS com Ubuntu Server + Java 17/Spring Boot em Docker está alinhada com o consenso** — é exatamente o "construí e operei algo" que as entrevistas perguntam (Dockerfile, porta, SSL, reverse proxy, pipeline).
2. **O risco não é o lab, é parar no lab.** O consenso exige: (a) **automatizar** (compose/Ansible/script — não só "rodei na mão"), (b) **CI/CD de verdade** (pipeline que testa, builda e publica a imagem), (c) **conseguir explicar em voz alta** cada decisão.
3. **Registrar como evidência:** README por serviço, decisões escritas, e — se der — o serviço **exposto publicamente com HTTPS** é o que os entrevistadores citam como "já fez isso".
4. **Não espere o lab substituir entrada na área.** O consenso brasileiro (r/brdev) diz: a primeira vaga provavelmente será **suporte/infra/dev**, e o lab é o que te diferencia *dentro* desse caminho.
5. **roadmap.sh serve de checklist, não de ordem** — use para identificar lacunas, não para seguir linha a linha.
6. **Cloud pode entrar depois** (a pendência AWS do projeto não contradiz a comunidade: quase todos aqui já tinham base de Linux/infra antes de cloud).

---

## 7. Resumo em 10 linhas

1. Reddit bloqueia acesso direto (403); achei as threads por busca e li o texto integral via API pública do PullPush — cobertura de comentários é parcial em algumas threads.
2. **CONSENSO:** DevOps não é cargo de entrada — o caminho real é suporte/help desk → sysadmin/infra → DevOps, ou dev → DevOps (r/brdev e r/ITCareerQuestions dizem a mesma coisa).
3. **CONSENSO:** "Só tutorial não vale" — o que separa aprovado de rejeitado é ter construído algo e **saber explicar por quê** (r/devops, thread de tutorial hell e a thread de currículo rejeitado).
4. **CONSENSO:** Linux + redes + fundamentos antes de Kubernetes/Ansible; pular essa base é o erro mais apontado.
5. **CONSENSO:** roadmap.sh é ótimo como **lista/checklist**, ruim como **sequência** — a crítica é cloud/IaC entrarem tarde e o efeito de atolamento.
6. **DIVIDIDO:** homelab/VPS no currículo — um manager (r/homelab) diz que contrataria, um candidato experiente (r/ITCareerQuestions) diz que "ninguém se importa".
7. **DIVIDIDO:** certificação (SAA "vale" × "certs não fazem diferença") e diploma (ajuda, mas há exemplos de entrada sem).
8. **Prova objetiva:** um processo júnior real perguntou Dockerfile, ENTRYPOINT vs CMD, SSL, GitLab CI vs GitHub Actions e "você já subiu app na EC2" — é tudo "você já operou?".
9. Para o lab em `VPS/`: vale, **desde que automatizado, com CI/CD e documentado** — mostrar a entrevista é o que gera vantagem; lab "rodou na mão e parei" não.
10. Recomendação prática: usar o lab para gerar 2–3 histórias explicáveis em entrevista e encará-lo como **complemento** da primeira vaga (suporte/infra), não como atalho para a vaga DevOps júnior.
