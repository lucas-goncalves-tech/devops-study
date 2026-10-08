---
aliases: [trilha2-02, imagem-registro]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 02 — Imagem: build no CI e registro por tag imutável

## Contexto

A imagem `notes-api` foi construída na mão no host (Trilha 1-01) e vive só ali — a VM tem
a cópia que foi levada, não tem relação com o repositório. CI/CD exige que a imagem **nasça
do pipeline** e guarde num registro (GHCR): aí a tag diz exatamente qual commit produziu
qual binário, a VM puxa a imagem certa sem build, e "qual versão está rodando?" vira uma
pergunta com resposta objetiva (`docker inspect ... Image`). Sem tag por commit, o deploy
da Issue 03 teria que reconstruir no destino — e "rebuild na hora do deploy" é como se
perde a reprodutibilidade que o Dockerfile conquistou.

## Objetivo

Estado final: `docker build-push` no workflow publica `ghcr.io/<owner>/notes-api:<sha>` e
`:latest` a cada merge na main; a tag do SHA é imutável e rastreável até o commit; a VM
**puxa** essa imagem (`docker compose pull`) em vez de buildar — mesmo `ImageID` no host
que construiu e na VM que roda.

## Dependências

- **Requer Trilha2-01** — a imagem é construída **dentro** do pipeline verde: sem
  `workflow-ci` funcionando, não há onde embutir o build de imagem.
- **pré-condição verificável:** run `ci` verde no último push.

- **estudo par:** `estudos/trilha2-02-imagem-registro.md` — ler antes de executar (é o currículo desta issue)

## Escopo

- Step `docker/build-push-action` (ou `docker build` + `push`) no workflow, após os
  testes verdes
- Login no GHCR com `GITHUB_TOKEN` (sem secret extra para o registro)
- Tags: `<ghcr.io owner/repo>:<sha curto>` (imutável, traceabilidade) + `:latest`
  (conveniência — discutida no estudo)
- `.dockerignore` já existe da Trilha 1-01; o build usa o contexto do runner
- Compose da VM alterado para `image: ghcr.io/...` (o build sai do fluxo de deploy)
- **assume pronto:** `workflow-ci` — da Issue 01
- **entrega:** `build-push-ghcr`, `tag-por-sha`, `compose-puxa-imagem`

## Fora de escopo

- Deploy na VM (pull acontece, mas a orquestração do deploy é Issue 03)
- Scan de vulnerabilidade da imagem (Trivy) — Trilha 4
- Assinatura de imagem (cosign), SBOM — estágio futuro/DevSecOps
- Registry próprio (Harbor), multi-arch — estágio AWS

## Conhecimentos envolvidos

- Registro de imagens: GHCR vs Docker Hub vs ECR — a ideia é uma, muda o endereço
- Tags: `latest` é conveniência, SHA é identidade — por que deploy por `latest` é aposta
- `GITHUB_TOKEN`: permissão curta e efêmera do run (não é secret eterno)
- build-push: build e publicação como um passo, cache de camadas do GitHub
- Imutabilidade de artefato: o que garante que o binário de hoje é o de ontem

## Estado atual

- Imagem existe só no host (build manual, tag `notes-api` genérica)
- Nenhum registro; nenhuma tag ligada a commit
- A VM roda cópia transferida, não puxada — divergência silenciosa possível

## Resultado esperado

- Merge na main → job de build roda depois do teste → imagem publicada no GHCR
- `docker pull ghcr.io/<owner>/notes-api:<sha>` → funciona de qualquer máquina
- `docker compose config` da VM → `image: ghcr.io/...:<tag>`, zero `build:` no serviço app
- Duas tags no registry: `:latest` e `:<sha>` apontando para o mesmo `ImageID`

## Requisitos

- Build de imagem **só após** testes verdes no mesmo job/workflow (não publicar imagem
  de código que não passou no teste)
- Tags com SHA do commit (`${{ github.sha }}` truncado) **e** `latest`
- Push via `GITHUB_TOKEN` com `permissions: packages: write` mínimo
- Serviço `app` no compose da VM referencia `image:` (registro), não `build:` — a VM não
  compila mais
- `docker compose pull` documentado como o jeito de atualizar (o insumo do deploy da 03)
- Tag publicada nunca é reescrita (SHA não muda — imutabilidade de artefato)

## Critérios de aceitação

- [ ] Pré-condição: último run do workflow `ci` → `success` (Issue 01) — sem CI verde,
      pare aqui
- [ ] Push/merge na main → step de build-push `success` e imagem listada em
      `ghcr.io/<owner>/notes-api` com tag `:<sha>` do commit que disparou
- [ ] `docker pull ghcr.io/<owner>/notes-api:<sha>` → `Status: Image is up to date` (ou
      baixa) em máquina **outra** que não o runner (no host, por exemplo)
- [ ] Duas tags apontando para o mesmo digest: `docker manifest inspect` (ou UI do GHCR)
      → `latest` e `:<sha>` com digest idêntico
- [ ] Falha de teste **impede** o push: quebrar teste → run vermelho → step de imagem não
      executa (ou workflow aborta antes) — a imagem ruim nunca chega ao registro
- [ ] `ssh lab@<ip-vm> 'grep -A1 "image:" compose.yaml'` → serviço `app` com
      `ghcr.io/...` e **nenhum** `build:` para o app
- [ ] `ssh lab@<ip-vm> 'docker compose pull && docker compose ps'` → pull da tag nova e
      stack volta healthy (o pull funciona na VM)
- [ ] Nenhum secret extra criado para o registro: `grep -c 'GITHUB_TOKEN\|ghcr'` no
      workflow → usa o token do run, sem `password` hardcoded

## Validação

- Merge de qualquer change → Actions → job com steps `test` → `build-push` nessa ordem
- No host: `docker pull ghcr.io/<owner>/notes-api:<sha>` → `docker images` mostra a tag
- Corresponder: `docker image inspect <sha> --format '{{.Id}}'` no host vs. na VM depois
  do pull → mesmo digest (a VM roda o que o CI construiu)
- Quebrar teste → re-run → confirmar que o step de imagem **não** aparece como sucesso
- `docker compose pull` na VM com log → `Image is up to date` ou download completo

## Evidências

- Run do workflow com os steps `test` e `build-push` verdes
- Listagem de tags no GHCR com `latest` + `:<sha>` (digest igual)
- `docker pull` no host da tag SHA
- `grep image/build` do compose na VM mostrando `image: ghcr.io/...` sem `build:`
- Digest idêntico host × VM (`docker image inspect`)

## Limitações / notas

- GHCR gratuito para imagens públicas; repo privado usa o storage da conta — lab não
  estoura, mas é o limite real
- `latest` **não** é imutável: alguém pode re-pushar; é por isso que os critérios cobrem
  a tag SHA — deploy sempre por SHA, `latest` é só para Humanos lerem
- `permissions: packages: write` no workflow é o mínimo: o `GITHUB_TOKEN` morre com o
  run — é segredo efêmero, não credencial (contraste com o `.env` 600 da Trilha 1-04)
- O pull da VM precisa de `docker login ghcr.io` com token que tenha leitura do pacote:
  se o repo/pacote é público, `pull` anônimo resolve; se privado, a credencial vai para a
  VM como secret da 03 — registrar qual situação o lab está usando
