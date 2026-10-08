# Estudo — git: merge, rebase e tag que sobrevivem ao histórico

> Material de estudo da Trilha 2. Acompanha a Issue 05 (merge, rebase, conflito e tag no
> repo real), mas não é o contrato daquela Issue: aqui é para entender, lá é para provar.

## Fast-forward move um ponteiro; --no-ff grava a decisão

Todo mundo já "fez merge"; quase ninguém sabe **o que o git deixou para trás**. O formato
do histórico não é estética: é o que sobrevive para quem vier depois ler, e é o que
determina se voltar atrás num conjunto de commits é um comando ou um arqueólogo.

- **Por que importa:** no fast-forward o git nem cria commit — ele só desloca o ponteiro
  da `main` até o fim da branch. O histórico fica uma reta e some a informação "isso foi
  feito em separado e entrou junto". Na prática isso dói no dia do arrependimento: com um
  commit de merge, reverter a feature inteira é `git revert -m 1 <sha-do-merge>` (um
  comando, um alvo); sem ele, os commits estão soltos na reta e você tem que descobrir o
  intervalo — e `git revert` cego num commit de meio caminho deixa o sistema num estado
  que ninguém sabe nomear.
- **Mecanismo:** merge só precisa inventar um commit quando os dois lados avançaram desde
  a base comum. A tabela inteira do assunto:

| Situação desde a base comum | O que o git faz | O grafo mostra |
|---|---|---|
| Só a branch avançou | fast-forward: ponteiro anda, nenhum commit novo | reta, sem sinal de branch |
| Os dois avançaram, sem overlap | commit de merge com dois pais | bolha com nó `*` |
| Os dois avançaram na **mesma linha** | conflito → resolução humana → commit de merge | bolha cujo nó guarda a resolução |

  `--no-ff` força a bolha mesmo quando o fast-forward seria possível: é dizer "quero que o
  histórico conte que houve branch". É exatamente o que o GitHub faz quando você mescla um
  PR com "Create a merge commit". Repare que conflito e `--no-ff` são independentes: todo
  conflito vira merge commit (não há fast-forward para quem divergiu), mas um merge commit
  pode nascer sem conflito nenhum.
- **Exemplo no lab:** dois alvos partindo do mesmo ponto deixam os dois formatos na mesma
  saída — comparação que não depende de memória:

```bash
git switch -c demo/feat main            # 1 commit de docs
git switch -c demo/alvo-ff main
git merge demo/feat                     # saída: "Fast-forward"
git switch -c demo/alvo-noff main
git merge --no-ff demo/feat -m "merge: integra demo/feat preservando o no"
git log --graph --oneline --decorate --all
```

  O `demo/alvo-ff` aparece como continuação reta de `demo/feat`; o `demo/alvo-noff` tem o
  nó `*` com dois pais e a bolha por baixo. Mesmo conteúdo nos dois alvos — só o histórico
  difere, e é a única coisa que muda.
- **Fronteira entre Issues:** branch, PR e o required check que decide se o merge passa são
  da **Trilha 2-01**; o merge que leva a imagem até a VM (deploy) é a **Trilha 2-03** e o
  voltar atrás é a **Trilha 2-04**. Esta Issue só entrega a **forma do histórico** — qual
  dos dois formatos foi escolhido e por que ele está no grafo.

## Rebase troca histórico por linearidade, e o troco é pago em SHA

Rebase é a ferramenta mais útil e mais perigosa do git ao mesmo tempo, porque ela faz uma
coisa que quase nenhuma outra faz de propósito: **reescrever o passado**.

- **Por que importa:** sem rebase, a branch que nasceu ontem se bifurca do ponto em que a
  `main` estava ontem — o histórico da integração mostra a bolha com commits de duas
  épocas misturados e a revisão "o que essa branch mudou" fica barulhenta. Com rebase, ela
  é uma coluna limpa em cima da `main` atual. O lado caro: cada commit ganha SHA novo.
  Quem já tinha aquela branch pushada (a CI rodou naquele SHA, alguém pode ter clonado
  aquela ponta) acorda com referências mortas — e o `push --force` comum, no lugar errado,
  apaga trabalho de outra pessoa.
- **Mecanismo:** `git rebase main` guarda os seus commits (por patch, não por SHA), aponta
  a branch para a `main` e reaplica um a um, de cima para baixo. Consequências diretas:

  | Antes | Depois |
  |---|---|
  | branch com pai antigo, bifurcação em `main` | branch com pai = ponteiro atual da `main` |
  | SHAs originais | SHAs novos, mensagens iguais |
  | `main..branch` com ruído de época | `main..branch` linear, só o que falta integrar |

  Regra de ouro: **rebase só o que é seu e ainda não foi publicado**. Reaplicar commits que
  já estão no `main` duplica o conteúdo no grafo (dois SHAs com o mesmo patch) e é a forma
  clássica de envenenar o histórico do time. Se a branch é só sua mas já foi pushada, a
  única força aceitável é `git push --force-with-lease` (recusa o push se alguém
  pushou depois de você) — nunca em `main`, que é justamente o que a branch protection da
  **Trilha 2-01** impede.
- **Exemplo no lab:** a prova é que o conteúdo é o mesmo e o passado é outro:

```bash
git switch -c experimento/git-avancado main
  # 2 commits de docs em linhas diferentes do ROADMAP.md
git rev-parse HEAD                      # anota o SHA da ponta
git switch main
  # 1 commit de docs em OUTRA linha (para o rebase não conflitar)
git switch experimento/git-avancado
git rebase main
git log --oneline main..HEAD            # os mesmos 2 commits, lineares, SHAs novos
git diff <sha-antigo> <sha-novo>        # vazio: árvore idêntica, histórico reescrito
git reflog | head                       # entrada "rebase (start)" — a rede de segurança
```

  Se o rebase der errado, `git rebase --abort` volta ao estado de antes; se você já saiu
  do abort, o `reflog` guarda o ponteiro antigo por dias — `git reset --hard
  HEAD@{n}` devolve a branch.
- **Fronteira entre Issues:** cherry-pick e stash são autonomia declarada **fora** desta
  Issue; force-push em `main` é proibido aqui (proteção é da **Trilha 2-01**). O que esta
  Issue entrega é o rebase **antes do merge**, na branch de prática — não um fluxo de
  `pull --rebase` obrigatório para o time, que é decisão de time, não de lab.

## Conflito é o git dizendo "isso eu não sei decidir"

Conflito tem fama de acidente. Ele é o contrário: é o git **funcionando** e se recusando
a escolher por você.

- **Por que importa:** quando dois commits mudam a mesma linha desde a base comum, existe
  mais de uma resposta correta e só um ser humano sabe a intenção. Quem resolve no
  automático (aceitar `ours` ou `theirs` sem ler) leva uma versão silenciosamente errada —
  e o pior tipo de bug é o que entra no repositório com o build verde. E há o pânico: a
  primeira vez que aparece `<<<<<<< HEAD`, muita gente abandona a branch inteira com
  `reset --hard` — quando, na verdade, o estado do merge está guardado e dá para desistir
  com um comando.
- **Mecanismo:** o merge é de **três vias** — git encontra a base comum (ancestral dos
  dois lados), olha o que mudou de cada lado e só pergunta onde os dois mudam nos **mesmos
  bytes**. Aí o arquivo sai com os marcadores:

```text
linha original
<<<<<<< HEAD
redação do lado em que você está
=======
redação do outro lado
>>>>>>> experimento/git-avancado
```

  Resolver é decidir o conteúdo final (às vezes é uma terceira redação que combina os
  dois), apagar **todos** os marcadores, `git add` no arquivo e `git commit` — a mensagem
  padrão já vem com o resumo do merge e a lista `Conflicts:`. Desistir é
  `git merge --abort` (ou `git rebase --abort`), que restaura o estado pré-operação.
  Detalhe que derruba muita gente: `--ours`/`--theirs` **inverte** num rebase — durante o
  replay, "ours" é a base nova (a `main`), não o seu branch, porque você está
  temporariamente nela.
- **Exemplo no lab:** o conflito é provocado de propósito, na mesma linha do `ROADMAP.md`
  (a de Git da cobertura — a que diz "**faltam** merge/rebase/tags"):

```bash
git switch experimento/git-avancado
  # editar a linha de Git do ROADMAP.md (redação 2) e commitar
git switch main
  # editar a MESMA linha (redação 3) e commitar
git switch experimento/git-avancado
git merge main
  # CONFLICT (content): Merge conflict in ROADMAP.md
git status                               # both modified: ROADMAP.md
git merge --abort                        # caso de falha: desiste e volta ao estado limpo
git merge main                           # refaz, agora para resolver
  # abrir ROADMAP.md, decidir a redação final, apagar os marcadores
git add ROADMAP.md && git commit
grep -c '<<<<<<<' ROADMAP.md             # 0 = marcadores saíram
git log --graph --oneline -n 5           # o nó de merge com os dois pais
```

  A resolução fica registrada no commit de merge — é o único tipo de decisão de conteúdo
  que o histórico guarda para sempre.
- **Fronteira entre Issues:** a CI (**Trilha 2-01**) decide se o código **pode** entrar
  (verde obrigatório); ela nunca decide **o que** entra — conflito é resolução humana, e
  é por isso que o check vem depois do merge, não no lugar dele. Resolver conflito de
  `pom.xml`/`VPS/src` não é o treino aqui: a regra do AGENTS.md mantém esses arquivos
  intocados, o campo de prática é documentação.

## Tag anotada é um objeto, não um apelido

Git tem duas formas de marcar um commit, e a diferença só aparece quando você pergunta
"o que era mesmo essa marca?".

- **Por que importa:** tag leve (`git tag v0.1.0`) é só um nome apontando para um SHA —
  sem autor, sem data, sem mensagem. Seis meses depois, `git show v0.1.0` não responde
  nada sobre o porquê: nem autor, nem data, nem intenção. Tag anotada (`git tag -a`) cria um
  **objeto tag** com tagger, data, mensagem e até assinatura possível: vira registro, não
  apelido. E ela é o único "ponto
  fixo" de um histórico mutável — depois de um rebase, o commit antigo continua existindo
  e alcançável exatamente porque a tag continua apontando para ele (git não coleta objeto
  referenciado).
- **Mecanismo:** a cadeia é `tag anotada → objeto tag → commit → árvore`. O que muda na
  prática:

| | Tag leve | Tag anotada |
|---|---|---|
| O que é | só um ref (arquivo com SHA) | objeto com tagger, data, mensagem |
| `git show` | mostra o commit direto | mostra a mensagem e o `Tagger:` |
| Assinável | não | sim (`-s`) |
| Serve de registro de release | não | sim |

  Dois detalhes operacionais: tags **não** sobem num `git push` normal — é
  `git push origin <tag>` ou `git push origin --follow-tags`; e `git describe --tags`
  traduz o `HEAD` no nome mais próximo (`v0.1.0-3-g2f1c4aa` = 3 commits após a tag), que é
  como ferramenta nenhuma perde a noção de "onde estamos".
- **Exemplo no lab:** a tag é criada no commit de integração, depois do merge na `main`,
  com mensagem que diz o que ela marca:

```bash
git tag -a v0.1.0 -m "marco: merge, rebase e tag exercitados no lab (T2-05)"
git tag -n                               # nome + mensagem na 1ª linha
git show v0.1.0 | head                   # Tagger:, Date:, a mensagem
git push origin v0.1.0
git ls-remote --tags origin              # a tag está no remoto
git log --graph --oneline --decorate -n 12   # tag: v0.1.0 decorando o commit
```

- **Fronteira entre Issues:** a **política** de versionamento (semver, tag por release,
  automation que gera a tag no pipeline) é estágio futuro declarado pela **Trilha 2-01**
  (backlog do ROADMAP) — aqui a tag é marco de prática, o mecanismo sem a política. O
  rollback da **Trilha 2-04** volta por imagem/SHA; a tag serve para rastrear *o que*
  estava rodando, não para executar o rollback.

## O grafo é a prova — e o merge é o gatilho do deploy

Nada do que ficou acima é verificável sem olhar o histórico. Ler rápido o grafo é a
habilidade que fecha a Issue.

- **Por que importa:** sem `--graph`, todas as situações acima parecem a mesma lista de
  commits — é impossível distinguir fast-forward de merge, linha reta de histórico
  reescrito, marca de release de apelido. E no dia a dia de operação é o grafo que responde
  "de onde veio o código que está na produção": merge na `main`, SHA no deploy, tag no
  marco.
- **Mecanismo:** `git log --graph --oneline --decorate --all` combina quatro lentes —
  `--graph` desenha a árvore, `--oneline` cabe no terminal, `--decorate` põe os nomes de
  branch e tag ao lado do SHA (sem ele, a tag some da leitura), `--all` traz o que existe
  mesmo sem estar no `HEAD`. Filtros essenciais: `main..branch` = o que a branch tem e a
  `main` não (o que vai entrar no PR); `branch..main` = o que a `main` ganhou e a branch
  ainda não tem (é daí que vem o rebase); `main~3..main` = os últimos 3 commits da `main`.
  Leitura guiada: `*` sozinho = commit; `*` com dois traços vindo de baixo = merge commit;
  coluna que se junta a outra = branch integrada; sufixo `(tag: v0.1.0)` = marco.
- **Exemplo no lab:** ao final da Issue 05, a mesma leitura conta a história inteira —
  coluna reta onde só havia fast-forward, bolha do `--no-ff` nos demos descartáveis, junção
  do conflito resolvido e a tag no topo:

```bash
git log --graph --oneline --decorate --all | head -40
git log --oneline origin/main..experimento/git-avancado   # o que o PR levou
```

  Com o `gh` na mão, o grafo e a CI se amarram: `gh run list` mostra o run do commit de
  merge — o mesmo SHA que aparece no topo da `git log` da `main`.
- **Fronteira entre Issues:** esta Issue entrega o histórico legível e a tag como marca
  duradoura. O merge que **dispara deploy** (e o `needs:` que depende do verde) é a
  **Trilha 2-03**; o rollback a partir do que está marcado é a **Trilha 2-04**. Legenda
  prática: se a pergunta for "o que entrou" → esta Issue; "o que subiu" → 2-03;
  "como volto" → 2-04.

## Como iniciar o modo teach-anything

- "Me ensina fast-forward vs `--no-ff` usando os branches `demo/alvo-ff` e `demo/alvo-noff` deste repo e o `git log --graph` dos dois"
- "Me ensina rebase antes do merge com a branch `experimento/git-avancado`, mostrando o SHA antigo, o novo e o `git diff` vazio entre eles"
- "Me ensina resolver conflito à mão na linha de Git do `ROADMAP.md`: marcadores, `git add`, `git commit` e o `git merge --abort` como caminho de saída"
- "Me ensina a regra de ouro de não rebasear o que já foi publicado, usando o `git reflog` deste repo como rede de segurança"
- "Me ensina tag anotada com `git tag -a` neste repo: `git tag -n`, `git show`, `git describe --tags` e o push da tag ao origin"
- "Me ensina ler `git log --graph --oneline --decorate --all` neste repo e amarrar o SHA do merge com o run da CI no `gh run list`"
