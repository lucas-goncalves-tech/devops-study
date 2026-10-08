---
aliases: [trilha2-03, deploy-ssh]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 03 — Deploy: push na main leva a imagem nova para a VM

## Contexto

A CI roda verde (Issue 01) e a imagem mora no registro (Issue 02), mas quem atualiza a
VM sou eu, na mão: `scp`/`ssh` + `compose pull` — esquecer um passo é rodar versão antiga
sem saber. CD (Entrega Contínua) aqui é o fim da frase: o merge **é** o deploy — o runner
autentica na VM por SSH, puxa a tag do commit e troca a stack, com o healthcheck da
Trilha 1 dizendo se a troca deu certo. É o fluxo que toda vaga chama de "CI/CD" e que só
faz sentido nessa ordem: primeiro o build confiável, depois a imagem rastreável, agora o
cabo entre os dois. Só que o cabo tem um obstáculo de rede: a VM está atrás do NAT do
libvirt (a Trilha 1-04 já avisou que "ninguém de fora alcança") e o runner hospedado do
GitHub não faz `ssh` para dentro da sua LAN. A saída sem abrir borda nenhuma é trazer o
runner para perto: um **self-hosted runner no host**, que fala com o GitHub só por conexão
outbound e com a VM pela rede local.

## Objetivo

Estado final: merge na main dispara job `deploy` que (1) autentica na VM com chave em
`SSH_PRIVATE_KEY` secret, (2) roda `docker compose pull && up -d` com a tag SHA do commit,
(3) espera o health `200` na VM, (4) registra qual SHA foi implantado — e um push sem
mudança de app **não** derruba a stack (pull de tag igual é no-op).

## Dependências

- **Requer Trilha2-01 e Trilha2-02** — deploy móvel = teste verde + imagem no registro;
- **Requer Trilha1-04** — a stack na VM com `.env` 600 e ufw explícito é o alvo do cabo.
- **pré-condição verificável:** imagem `ghcr.io/...:<sha>` publicada **e** `docker
  compose ps` 4×healthy na VM.

## Escopo

- Job `deploy` no workflow, `needs: [test, build]`, com
  `if: github.ref == 'refs/heads/main' && github.event_name == 'push'` — o repo é
  **público** e este é o único job que roda no runner de baixo (fork/PR nunca o agenda)
- Self-hosted runner no **host** como serviço systemd: download do runner, token de
  registro do repo e `./svc.sh install` (sobe no boot do host); o job de deploy usa
  `runs-on: [self-hosted]`, `test`/`build-push` seguem no runner hospedado. Motivo: o NAT
  do libvirt (Trilha 1-04) — o runner do host está na mesma LAN que a VM e conecta ao
  GitHub **só por outbound**, sem porta nova e sem regra de ufw
- Chave SSH dedicada **sem senha** (deploy key do lab) em `SSH_PRIVATE_KEY` (secret do
  repo); known_hosts com o host key da VM pinado (não `StrictHostChecking no`)
- `docker login ghcr.io` na VM **se** o pacote for privado (PAT de leitura como credencial
  da VM, fora do git); pacote público → `pull` anônimo. **Declarar qual** o lab usa — a
  T2-02 deixou essa pendência anotada nas notas dela
- Script de deploy no repo (`scripts/deploy.sh`): pull da tag `${GITHUB_SHA}` → `up -d`
  → espera `200` no health via proxy → imprime SHA implantado
- Rollback manual declarado: `docker compose pull <sha anterior> && up -d` (o automatizado
  é a Issue 04)
- **assume pronto:** `tag-por-sha`, `compose-puxa-imagem` — da Issue 02;
  `stack-na-vm` — da Trilha 1-04
- **entrega:** `job-deploy`, `ssh-secret`, `deploy-script`, `runner-self-hosted`

## Fora de escopo

- Rollback automático com health gate — Issue 04
- Ambientes múltiplos (staging/prod), blue-green, canary — estágio AWS
- OIDC/short-lived credentials do GitHub → cloud — estágio AWS (aqui é chave SSH clássica)
- Notificações (Slack/e-mail) do deploy — Trilha 3 (alertas)

## Conhecimentos envolvidos

- CD vs CI: o que muda quando o pipeline **toca** infra
- Segredos em CI: GitHub Secrets, o que é efêmero (GITHUB_TOKEN) vs. duradouro (chave SSH)
- SSH non-interactive: `ssh -i`, `known_hosts` e o que é "host key pinning" (MITM)
- Script de deploy idempotente: rodar 2× não quebra (pull igual = no-op)
- Health gate: esperar o `200` **antes** de declarar sucesso

## Estado atual

- Atualizar a VM é manual (`scp` + `pull` + `up`), com passos sujeitos a esquecimento
- Nenhum segredo no GitHub; nenhuma conexão runner→VM
- O SHA rodando na VM é desconhecido sem `docker inspect`

## Resultado esperado

- Merge na main → job `deploy` verde após `test` e `build-push`
- Na VM: `docker compose ps` → imagem `ghcr.io/...:<sha do merge>` healthy
- Health `200` verificado **pelo runner** antes do job terminar verde
- Push só de docs → deploy vira pull de tag igual → stack sem restart desnecessário
- Chave SSH existe só como secret (nunca no repo)

## Requisitos

- Job `deploy` com `needs:` dos jobs anteriores e
  `if: github.ref == 'refs/heads/main' && github.event_name == 'push'` (só push na main
  agenda o runner — repo público)
- Runner self-hosted registrado no repo e instalado como serviço systemd **no host**
  (`systemctl is-enabled actions.runner.*` → `enabled`); `deploy` com
  `runs-on: [self-hosted]`, `test`/`build-push` permanecem em runner hospedado
- Nenhum job além do `deploy` usa o runner do host (na LAN só passa o que veio de push na
  main própria)
- Chave SSH em secret do repo; **zero** ocorrência de chave/senha no repo
  (`grep -r "BEGIN OPENSSH" .` → vazio)
- `known_hosts` com host key da VM pinado no repo ou gerado no job via
  `ssh-keyscan` **uma** vez e fixado (a decisão e o trade-off vão para o estudo —
  declarar qual foi tomada)
- `scripts/deploy.sh` versionado: pull da tag do `GITHUB_SHA` → `up -d` → poll do health
  com timeout → exit ≠ 0 se não ficar `200` (deploy falha **feio**, não "meio sucedido")
- Idempotência: re-run do mesmo SHA → `Image is up to date` + stack sem downtime
- Log do deploy imprime o SHA implantado (traceabilidade: pipeline → VM)
- Rollback manual documentado no script/README: qual comando volta ao SHA anterior

## Critérios de aceitação

- [ ] Pré-condição: `ghcr.io/...:<sha>` publicada (Issue 02) **e**
      `ssh lab@<ip-vm> 'docker compose ps'` → 4×healthy (Trilha 1-04) — sem as duas, pare aqui
- [ ] Merge na main → jobs `test` → `build-push` → `deploy` verdes **nesse ordem** no
      Actions (o `needs` aparece na UI)
- [ ] Após merge: na VM, `docker compose ps --format '{{.Image}}'` →
      `ghcr.io/...:<sha do commit>` (o deploy implantou exatamente aquele commit)
- [ ] O runner provou o health: log do `deploy.sh` → linha de `200` antes do exit 0
- [ ] `grep -rE "BEGIN OPENSSH|sshpass|Password"` no repo → **vazio**; a chave existe
      apenas em `Settings → Secrets`
- [ ] Push só alterando `.md` → deploy executa e termina verde com `Image is up to date`
      (idempotência — stack não restarta com mudança de docs... ou se restartar por
      tag nova, declarar: o requisito é **não falhar** e não derrubar)
- [ ] Re-run do job `deploy` no mesmo SHA → verde de novo (2ª execução = no-op saudável)
- [ ] Runner do host: `systemctl is-enabled actions.runner.<repo>.*` → `enabled` no host
      **e** o runner `online` no repo; o log do `deploy` mostra o runner do host como
      executor (`runs-on: [self-hosted]`)
- [ ] `grep -A1 'runs-on' .github/workflows/*` → `deploy` com `[self-hosted]`;
      `test`/`build-push` permanecem no runner hospedado
- [ ] PR aberto → o run do CI roda no hospedado e **nenhum** job `deploy` é agendado no
      runner do host (`github.event_name == 'push'` segura — repo público)
- [ ] Rollback manual testado: `scripts/deploy.sh <sha-anterior>` (ou comando documentado)
      → VM volta para o SHA anterior e health `200`

## Validação

- Merge de teste → Actions: sequência dos 3 jobs; abrir `deploy` → cada passo (ssh, pull,
  up, health poll)
- Na VM: `docker compose ps` + `docker inspect` do `Image` → tag SHA do merge
- Idempotência: `Re-run jobs` no run → deploy verde, log com "up to date"
- Rollback: escolher o SHA anterior na UI do Actions → rodar o comando documentado →
  confirmar tag antiga na VM e `200` no health
- Segredos: `git log -p --all -S 'BEGIN OPENSSH'` → nada (a chave nunca entrou no git)

## Evidências

- Run do Actions com os 3 jobs verdes e a seta de `needs`
- `docker compose ps` da VM mostrando a imagem `ghcr.io/...:<sha>`
- Trecho do log do deploy com o `200` do health e o SHA impresso
- `git log -S 'BEGIN OPENSSH'` vazio + print do secret configurado (nome, não valor)
- Rollback: par antes/depois (tag SHA na VM)

## Limitações / notas

- Chave SSH em secret é **duradoura**: se o runner for comprometido (workflow de
  terceiro), a chave vaza — é o modelo clássico e a razão de existir o OIDC do GitHub
  (efêmero), que só brilha de verdade com cloud (estágio AWS). Aqui se aprende o modelo
  duradouro com um lab de um host
- `ssh-keyscan` no job a cada run é conveniente e **fraco** (aceita qualquer chave que se
  apresentar — MITM); pinar o host key no repo é o certo. A issue exige decidir e
  declarar; o estudo explica o porquê
- Deploy sem janela: `up -d` troca a app com downtime de segundos (pull + recreate) —
  aceitável no lab; zero-downtime (blue-green) é estágio futuro
- O deploy **só** atualiza a app; mudança de `.env`/compose (estrutura) continua sendo
  leva manual do repo para a VM — o pipeline carrega binário, não configuração de host
- Host desligado no momento do merge → o job de `deploy` fica `pending` até a máquina
  voltar — aceito no lab e declarado aqui; com VPS pública no futuro, a volta ao runner
  hospedado é trocar `runs-on`, uma linha
- Repo público + self-hosted é a clássica mina: quem abrir PR executaria código na sua
  LAN com o grupo `docker`. Por isso este job é restrito a push da main própria — e
  nenhum outro job pode ganhar `runs-on: [self-hosted]` enquanto o repo for aberto
