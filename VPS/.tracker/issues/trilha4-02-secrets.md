---
aliases: [trilha4-02, secrets]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 02 — Secrets: inventoriar, nunca vazar, conseguir rotacionar

## Contexto

O lab já trata segredo certo na Trilha 1-04 (`.env` com 600, fora do git) — mas "estar
fora do git hoje" não é gestão de secrets: é sorte. Não existe **inventário** (quais
segredos existem, onde vivem, quem usa), nenhum teste de **vazamento** (o repo já teve
`Dockerfile`/`compose` versionados — um `grep` acidental de token entra sem barulho), e
`JWT_SECRET` nunca foi **rotacionado** — se ele vazar (histórico de git, backup, tela
compartilhada), a resposta hoje seria improvisar no meio do estrago. Em vaga, "como você
gerencia secrets?" é pergunta de screen; a resposta esperada não é "tenho um `.env`", é
"tenho inventário, gate anti-vazamento e procedimento de rotação testado".

## Objetivo

Estado final: `secrets/INVENTARIO.md` (ou equivalente) listando cada segredo do lab
(chave SSH, JWT_SECRET, senha PG/Redis, credencial do receiver da T3-03) com dono e
ciclo de rotação; **gate anti-vazamento** que falha o CI se segredo literal aparecer no
repo; e **1 rotação real executada** (o `JWT_SECRET`): aplicada, stack saudável, sessão
antiga invalidada, com o procedimento escrito.

## Dependências

- **Requer Trilha3-03** — a credencial do receiver de alerta é o secret mais novo do
  lab; sem a T3-03 feita, o inventário já nasce incompleto.
- **Requer Trilha2-03** — o caminho declarado para a VM já existe (deploy e comandos via
  SSH); a rotação em si é **transporte manual** do `.env` novo + restart — o pipeline da
  T2-03 carrega binário, não configuração, então a troca de segredo não é "só merge".
- **pré-condição verificável:** `deploy-ssh-verde` + `.env` com 600 na VM + T3-03 com
  receiver configurado (secret do receiver vivo).

- **estudo par:** `estudos/trilha4-02-secrets.md` — ler antes de executar (é o currículo desta issue)

## Escopo

- Inventário versionado (sem valores!): nome, onde é usado (arquivo/serviço), onde está
  guardado (`.env` da VM, chave SSH, GHCR), rotina de troca e "quem avisa quem"
- Gate de vazamento no CI: `grep`/trufflehog/gitleaks (escolher e declarar) varrendo
  histórico ou diff — **falhando** o job em achado (não warning)
- Rotação real do `JWT_SECRET`: gerar novo, aplicar via transporte declarado (scp do
  `.env` na VM + restart — não é o fluxo do pipeline, que carrega binário), verificar que
  liveness/health ficam verdes e token antigo é recusado (401)
- Procedimento de rotação escrito (passos copiáveis) — a rotação é o "backup testado"
  dos secrets: só existe quando já fez uma vez
- Varredura manual pós-escopo: `git log -p | grep` por padrões de segredo nos arquivos
  que **já** passaram pelo histórico
- **assume pronto:** `deploy-ssh-verde` (T2-03), `alertmanager-entregando` (T3-03)
- **entrega:** `inventario-secrets`, `gate-vazamento-ci`, `rotacao-jwt-executada`

## Fora de escopo

- Vault/external secrets manager (HashiCorp, SOPS, age) — estágio AWS com IaC; o
  inventário define **o que** o vault substituiria depois
- Secrets do app no código (o `application.yml` lê de env — app intocado, sem mudar)
- Troca de toda credencial a cada N dias com automação — sem org/time, o ciclo é
  declarado e a execução é manual
- Chave SSH da VM + GitHub Actions secrets já existem: aqui é inventariá-las, não criá-las

## Conhecimentos envolvidos

- O que é secret vs. config vs. credential: por que `JWT_SECRET` é segredo e
  `PORT` não é
- Rotação: invalidar o antigo **antes** de distribuir o novo (janela vs. quebra)
- Por que `.env` com 600 não basta (histórico de git é eterno; backup leva junto)
- Pre-commit/CI como gate: o segredo que entra, entra no primeiro commit — o gate é
  rede, não revisão humana
- Efeito da rotação em sistema com estado: sessões JWT quebram de propósito — o
  "usuário desloga" é comportamento esperado, não incidente

## Estado atual

- `.env` (600) na VM com `JWT_SECRET`, `POSTGRES_PASSWORD`, `REDIS_PASSWORD` — nenhum
  inventariado em documento
- Credencial do receiver (T3-03) fora do git por costume, não por processo
- Zero gate: um token em `application.yml`/`compose.yaml` commitado passaria reto no CI
- `JWT_SECRET` nunca trocado desde a criação — rotação é teoria

## Resultado esperado

- `git ls-files` → inventário com ≥ 5 secrets documentados (sem valores literais)
- CI com job de scan rodando em todo push/PR → verde sem achado; teste com um dummy
  `AWS_SECRET_ACCESS_KEY=...` **no branch** → job vermelho (e removido antes do merge)
- Rotação: `JWT_SECRET` novo aplicado → health `200`, token antigo → `401`
- Procedimento de rotação no repo (mesmo documento ou runbook)

## Requisitos

- Inventário com campos: nome, valor **não presente**, onde vive, quem consome, como
  rotacionar, a partir de quando (data)
- Scan de vazamento **no CI** (job que bloqueia, com evidência de falha em teste) — não
  só comando manual local
- Varredura do histórico git feita e resultado registrado (achado → revogação/aceite)
- Rotação do `JWT_SECRET` de ponta a ponta: novo valor → `.env` novo na VM (scp) +
  restart da stack → app saudável
  → **prova** de quebra do token antigo e funcionamento do novo
- Sem segredo literal em nenhum arquivo rastreado (`grep` final do repo)
- App intocado: rotação usa `set -a; . ./.env`/restart — zero mudança em `src/`

## Critérios de aceitação

- [ ] Pré-condição: `ssh lab@<ip-vm> 'stat -c %a ~/.env'` → `600` **e** deploy verde
      (T2-03) **e** evidência de receiver da T3-03 configurado — sem os três, pare aqui
- [ ] Inventário rastreado no repo com ≥ 5 entradas e **nenhum** valor literal
      (`grep -iE 'secret|password|token' <inventario>` → só nomes/procedimentos)
- [ ] Job de scan no workflow CI/CD → roda em push/PR (linha no Actions) e termina
      verde
- [ ] Prova negativa do gate: commit de teste com dummy secret **em branch** → job
      **vermelho**; branch de teste removida/branch sem o dummy (print do falhou)
- [ ] `git log -p | grep -cE '(JWT_SECRET=[A-Za-z0-9]{20,}|password: [^$])'` → varredura
      do histórico executada; resultado registrado (0 achado ou achado revogado)
- [ ] Rotação executada: hash/`compare` provando `JWT_SECRET` da VM **diferente** do
      inicial (`.env.example` não conta — é o `.env` real)
- [ ] Pós-rotação: health/liveness `200` **e** `curl` com token emitido **antes** →
      `401` **e** login novo → `200` (ciclo completo da rotação)
- [ ] Procedimento de rotação versionado (passos reais usados, não teóricos)

## Validação

- Inventário: ler contra a realidade — `ssh ... 'grep -o "^[A-Z_]*" .env'` × o
  documento (cada varável do `.env` aparece no inventário ou tem justificativa)
- Gate: criar branch `test-secret-leak`, commitar dummy (`API_KEY=abc123...`), push →
  Actions vermelho → apagar branch
- Histórico: `git log -p --all | grep -nE 'SECRET|PASSWORD|TOKEN' | head` → revisar
  cada hit
- Rotação: gerar `openssl rand -base64 48` → editar `.env` na VM pelo fluxo →
  `docker compose restart app` → health → token antigo `401` → login novo `200`
- Saúde final: `docker compose ps` → 6×healthy

## Evidências

- `secrets/INVENTARIO.md` no repo (≥ 5 entradas, sem valores)
- Print/linhas do job de scan no Actions (verde normal + vermelho no teste)
- Linha da varredura de histórico com resultado
- Par do pós-rotação: token antigo `401` × novo `200` + health verde
- Procedimento de rotação versionado

## Limitações / notas

- O gate de vazamento só pega padrão **literal** — segredo dividido por string
  concatenação ou codificado passa; é rede, não garantia (por isso o inventário tem
  "como rotacionar": o plano B quando o gate falhar é trocar)
- Rotação do `JWT_SECRET` desloga todo mundo — em lab sem usuário, o "usuário" é o
  próprio teste; em produção seria janela de comunicação (fronteira do estágio real)
- Histórico git **não se apaga** sem reescrever branches (force-push, coordenação) —
  acha-se vazamento antigo → revoga-se **o valor** (rotação), não o passado; nunca
  tentar "limpar" histórico como primeiro reflexo
- Inventário em git é metal: quem lê o repo sabe o **nome** de cada segredo (não o
  valor) — isso é aceito e documentado; valores num vault é a próxima fronteira (AWS)
