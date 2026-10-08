---
aliases: [trilha2-05, git-avancado]
tags: [tracker, issue, todo]
status: todo
prioridade: media
---

# Issue 05 — git: merge, rebase e tag que sobrevivem ao histórico

## Contexto

A Trilha 2 já deu branch, PR e CI, mas todo commit entrou pela `main`, um atrás do outro:
`git log --graph --oneline` deste repo é uma coluna única — não há bifurcação, não há
junção, não há o que o grafo contar. Falta o trio que separa "sei criar branch" de "sei
integrar": **merge** (duas linhas de trabalho viram uma, e o formato do histórico decide o
que dá para reverter depois), **rebase** (reescrever o histórico de propósito, com o preço
disso) e **tag** (o marco que continua apontando para o mesmo código quando o histórico
muda). O próprio ROADMAP declara a dívida na linha de Git da cobertura ("**faltam**
merge/rebase/tags") — e é ali que a primeira pessoa trava: o primeiro conflito real é a
situação em que mais gente joga `reset --hard` e perde trabalho sem saber que `git status`
já tinha entregue a resposta.

## Objetivo

Estado final: neste repo, a branch `experimento/git-avancado` integrada pelo caminho
completo — rebase antes do merge, um conflito provocado (dois commits mudando a mesma
linha do `ROADMAP.md`) e resolvido à mão, fast-forward e `--no-ff` comparados no **mesmo**
`git log --graph --oneline`, e tag anotada com mensagem pushada ao origin — com o grafo
provando cada passo e nenhuma linha de `VPS/src/` tocada.

## Dependências

- **Requer Trilha2-01** — o campo de prática é o repo com histórico real e CI: sem commits
  empilhados, sem branch de integração e sem check verde para travar o merge, merge/rebase/tag
  viram exercício de brinquedo — não há grafo para ler nem PR para integrar.
- **pré-condição verificável:** `git log --oneline | head` → histórico real na `main`
  (commits em português) **e** `gh run list --limit 3` → ao menos um run da CI da
  Trilha 2-01 — sem as duas pontas, a Trilha 2-01 vem primeiro.
- **estudo par:** `estudos/trilha2-05-git-avancado.md` — ler antes de executar (é o currículo desta issue)

## Escopo

- Branch de experimento `experimento/git-avancado` neste repo: campo dos commits de
  prática, sempre em docs — nunca em `VPS/src/`
- Demo de integração em branches descartáveis (`demo/feat`, `demo/alvo-ff`,
  `demo/alvo-noff`): mesmo ponto de partida, um merge fast-forward e um `--no-ff`,
  comparáveis lado a lado numa única saída de `git log --graph`
- Rebase da branch sobre a `main` avançada: replay dos commits com SHA novo e conteúdo
  idêntico, provado por `git diff` vazio entre o SHA antigo e o novo
- Conflito real provocado: experimento e `main` mudam a **mesma linha** do `ROADMAP.md`
  (a linha de Git da seção de cobertura) com redações diferentes → resolução à mão,
  marcadores removidos, `git add` + `git commit`; a tentativa de desistir
  (`git merge --abort`) também é exercitada
- Tag anotada `git tag -a v0.1.0 -m "..."`: marco com mensagem no commit de integração,
  pushada ao origin (tags não sobem num `git push` normal)
- Integração por PR com CI verde (required check da Trilha 2-01) e remoção das branches
  locais depois do merge
- **assume pronto:** `workflow-ci`, `check-obrigatorio` — da Trilha 2-01
- **entrega:** `merge-ff-e-noff`, `rebase-antes-do-merge`, `conflito-resolvido-na-mao`,
  `tag-anotada`

## Fora de escopo

- cherry-pick e stash — autonomia de git; não são critério desta issue
- Política semver, release automation e tag gerada por pipeline — estágio futuro declarado
  na Trilha 2-01 (backlog do ROADMAP: "Multi-branch, release automation, semver"); a tag
  aqui é marco de prática, não versionamento de release
- Force-push em `main` e em qualquer branch compartilhada — proibido; `--force-with-lease`
  só aparece como teoria (material de estudo), não como prática
- Mudanças em `VPS/src/` e `VPS/pom.xml` — intocados de propósito (regra do AGENTS.md)
- Branch protection e required check — Trilha 2-01; merge que dispara deploy — Trilha 2-03;
  rollback — Trilha 2-04

## Conhecimentos envolvidos

- Fast-forward vs `--no-ff`: o que cada um deixa (e apaga) no histórico — e por que um
  `git revert` de feature só é trivial num deles
- Merge de três vias (base comum, ours, theirs) e leitura dos marcadores de conflito
- Rebase: replay commit a commit, reescrita de SHA, regra de ouro do histórico publicado
- Conflito durante rebase vs durante merge (`--continue`, `--abort`, quem é "ours")
- Tag anotada vs tag leve: objeto tag, tagger, mensagem, push de tag, `git describe`
- Leitura de estado: `git status`, `git log --graph/--oneline/--decorate`, `git reflog`
  como rede de segurança
- Conventional commits com escopo em português (convenção do repo, AGENTS.md)

## Estado atual

- `git tag` → nenhuma tag; `git branch -a` → só `main`; nenhuma branch além da `main` no
  origin
- Todo commit entrou pela `main` direto: nunca houve duas linhas de trabalho para integrar
- `git log --graph --oneline --all` é uma coluna única — o grafo não tem o que mostrar
- O ROADMAP já escreve a dívida: "**faltam** merge/rebase/tags" na linha de Git da cobertura

## Resultado esperado

- `git log --graph --oneline --decorate --all` → coluna reta do fast-forward e bolha com
  nó de merge do `--no-ff` na mesma saída, com a tag decorando um commit
- Merge em conflito registrado (`CONFLICT (content): Merge conflict in ROADMAP.md`) e
  depois resolvido: `grep -c '<<<<<<<' ROADMAP.md` → 0
- `git diff <sha-antigo> <sha-novo>` pós-rebase → vazio (conteúdo igual, histórico novo)
- `git tag -n` → tag anotada com mensagem; `git ls-remote --tags origin` → tag no remoto
- PR mergeado com CI verde; `git ls-remote --heads origin` → só `main` (branches de prática
  não sobem)

## Requisitos

- Dois merges comparáveis a partir do mesmo ponto de partida: um fast-forward sem commit de
  merge e um `--no-ff` com mensagem de merge escrita
- Pelo menos um rebase da branch de experimento sobre a `main`, com histórico linear e
  conteúdo idêntico ao de antes do rebase
- Um conflito real provocado (dois commits, mesma linha) e resolvido à mão: marcadores
  saem do arquivo, `git add` + `git commit` fecham o merge com dois pais
- Tag criada com `git tag -a` e mensagem (não tag leve), no commit de integração, e
  pushada ao origin
- Prova visual com `git log --graph --oneline --decorate` em cada etapa (grafo é a evidência)
- Commits em português, no formato conventional commits com escopo (`docs(...)`, `chore(...)`)
- Nenhum commit da prática toca `VPS/src/` nem `VPS/pom.xml`; nenhum force-push; branches
  `demo/*` e `experimento/*` nunca são pushadas — só `main`, PR e tag vão ao origin

## Critérios de aceitação

- [ ] Pré-condição: `git log --oneline | head` → histórico real na `main` **e**
      `gh run list --limit 3` → run da CI da Trilha 2-01 — sem as duas, pare aqui
- [ ] Fast-forward: `git merge demo/feat` em `demo/alvo-ff` sem argumentos → `Fast-forward`
      na saída e `git log --graph --oneline demo/alvo-ff` **sem** nó de merge (só o
      ponteiro andou)
- [ ] `--no-ff`: `git log --graph --oneline demo/alvo-noff` → nó de merge com dois pais
      (bolha da branch) e `git show --stat <sha-do-merge>` com a mensagem de merge escrita
- [ ] Rebase: `git log --oneline main..experimento/git-avancado` → commits lineares acima
      da `main`, sem commit de merge; `git diff <sha-antes> <sha-depois>` → vazio e os dois
      SHAs diferentes
- [ ] Conflito provocado: o merge reporta `CONFLICT (content): Merge conflict in
      ROADMAP.md` e `git status` → `both modified`; `git merge --abort` desiste e devolve
      o estado pré-merge (caminho de falha comprovado)
- [ ] Conflito resolvido à mão: `grep -c '<<<<<<<\|>>>>>>>' ROADMAP.md` → 0,
      `git status --short` → vazio, e `git log --graph --oneline` mostra o commit de merge
      com dois pais (fork e junção visíveis)
- [ ] Tag: `git tag -n` → a tag com a mensagem na primeira linha; `git show v0.1.0 | head`
      → `Tagger:` e `Date:` (objeto tag, não tag leve); `git ls-remote --tags origin` →
      tag presente no remoto
- [ ] Integração: `gh pr view` → PR mergeado com o check da CI verde; `git log --oneline
      origin/main -n 3` → o commit de merge na `main`
- [ ] Limpeza: `git ls-remote --heads origin` → só `main`; `git branch --merged main` →
      `experimento/git-avancado` não aparece (branch apagada após o merge)
- [ ] Repo intacto: `git show --stat` de cada commit da prática → só `ROADMAP.md`
      (`VPS/src/` e `VPS/pom.xml` intocados) e nenhum push com `--force` no fluxo

## Validação

- Base limpa: `git fetch origin && git switch main && git pull --ff-only` → `main` igual
  ao `origin/main`; `git status --short` → vazio
- Fast-forward vs `--no-ff` (branches locais descartáveis, nunca pushadas):
  `git switch -c demo/feat main` + 1 commit de docs → `git switch -c demo/alvo-ff main` →
  `git merge demo/feat` → `Fast-forward`; depois `git switch -c demo/alvo-noff main` →
  `git merge --no-ff demo/feat -m "merge: integra demo/feat preservando o no"` →
  `git log --graph --oneline --decorate --all` → coluna reta de um lado, bolha com nó do
  outro
- Rebase: `git switch -c experimento/git-avancado main` + 2 commits de docs (linhas
  diferentes do `ROADMAP.md`) → `git switch main` + 1 commit de docs em **outra** linha
  (local, sem push) → `git switch experimento/git-avancado` → anotar os SHAs
  (`git rev-parse HEAD`) → `git rebase main` → `git log --oneline main..HEAD` → os mesmos
  2 commits, lineares, com SHAs novos; `git diff <sha-antigo> <sha-novo>` → vazio
- Conflito: no experimento, editar a linha de Git do `ROADMAP.md` (redação 2) e commitar →
  `git switch main`, editar a **mesma** linha (redação 3) e commitar →
  `git switch experimento/git-avancado && git merge main` → `CONFLICT (content): Merge
  conflict in ROADMAP.md` e `git status` → `both modified` → `git merge --abort` (prova
  que dá para desistir) → `git merge main` de novo → abrir o `ROADMAP.md`, decidir a
  redação final, apagar os marcadores → `git add ROADMAP.md && git commit` →
  `grep -c '<<<<<<<' ROADMAP.md` → 0
- Integração: conferir antes do reset que nada se perde:
  `git merge-base --is-ancestor main experimento/git-avancado` → exit 0 (os commits
  locais da `main` são ancestrais da branch) → `git switch main && git reset --hard
  origin/main` → `git push -u origin experimento/git-avancado` → `gh pr create` → CI verde
  → merge na UI → `git switch main && git pull --ff-only` → `git log --graph --oneline -n
  12` com os commits de merge
- Tag: `git tag -a v0.1.0 -m "marco: merge, rebase e tag exercitados no lab (T2-05)"` no
  commit da `main` → `git tag -n` → `git push origin v0.1.0` →
  `git ls-remote --tags origin` → tag listada
- Limpeza: `git branch -d experimento/git-avancado` e `git branch -D demo/feat demo/alvo-ff
  demo/alvo-noff` (`-D` só nas descartáveis locais) → `git ls-remote --heads origin` → só
  `main`
- Caso de falha extra: `git status` durante o conflito e a saída do `git merge --abort`
  (voltou ao estado pré-merge, nada perdido) — registrar ambas

## Evidências

- `git log --graph --oneline --decorate --all` no momento em que `demo/alvo-ff` e
  `demo/alvo-noff` existem: reta × bolha lado a lado
- Saída do merge em conflito (`CONFLICT ...`, `git status` com `both modified`) e depois
  `grep -c '<<<<<<<' ROADMAP.md` → 0 com `git status --short` vazio
- `git diff <sha-antigo> <sha-novo>` vazio após o rebase (os dois SHAs colados)
- `git log --graph --oneline` do commit de merge final, mostrando os dois pais
- `git tag -n` e `git show v0.1.0 | head` (Tagger/Date/mensagem) + `git ls-remote --tags
  origin`
- `gh pr view` com CI verde e merge concluído; `git ls-remote --heads origin` só com `main`

## Limitações / notas

- Os commits de prática vivem só em docs (`ROADMAP.md`): conteúdo de demonstração que
  não interessa manter pode ser a própria atualização honesta da linha de Git (ela diz
  "**faltam**" justamente por esta issue) — nunca lixo em `VPS/src/`
- A tag `v0.1.0` é marco de prática com nome livre; política semver e release automation
  continuam no backlog declarado pela Trilha 2-01, e nada aqui versiona o app
- Os commits locais na `main` existem **só** para provocar divergência: se a branch
  protection da Trilha 2-01 bloquear push direto, o caminho é o PR descrito na Validação —
  o `reset --hard origin/main` local não perde nada, porque o rebase e o merge carregaram
  aqueles commits para o histórico do experimento (prova:
  `git merge-base --is-ancestor main experimento/git-avancado` → exit 0)
- O merge na `main` dispara a CI da Trilha 2-01 (e, se o deploy da Trilha 2-03 estiver
  ligado a push na `main`, um novo deploy): o diff é de docs, a imagem não muda — é o
  fluxo normal do repo
- Repositório é o lab: branches `demo/*` são locais e descartáveis, deletadas ao final;
  o único artefato que permanece no origin é o histórico da integração + a tag
- `git reflog` é a rede de segurança de um rebase malfeito (`git reset --hard
  HEAD@{...}` volta ao estado pré-rebase) — mencionado no estudo, não é critério aqui
