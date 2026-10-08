---
aliases: [trilha4-03, trivy]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 03 — Supply chain: a imagem que sobe na VM passa por scan

## Contexto

A Trilha 2-02 constrói e publica imagem no GHCR — e o `Fora de escopo` dela prometeu:
"Scan de vulnerabilidade da imagem (Trivy) — Trilha 4". Toda imagem tem camada: base
(Ubuntu/temurin), `apt` da build, jar com as dependências Maven (Spring, Flyway, Netty,
Jackson...). Nada disso foi verificado **antes** de virar artefato de deploy. Scan de
imagem é o gate de supply chain mínimo que vaga pede (e que o `vulnerability-scanner`
do lab discute): rodar Trivy na imagem **já buildada**, classificar achados por
severidade, e decidir **público** o que bloqueia build vs. o que se aceita com
justificativa — porque scan que "fica vermelho sempre" é desligado, e scan que nunca
fala nada é teatro. A decisão política (threshold) é o coração da issue.

## Objetivo

Estado final: job de scan (Trivy) no pipeline **da imagem** (pós-build, pré-push ou
pós-push com gate), report versionado/artefato no Actions, `trivy.yaml`/ignore com o
limiar decidido (ex.: CRITICAL/HIGH bloqueia, MEDIUM registra), e um achado real
processado: ou a base é atualizada e o achado some, ou entra no ignore **com
justificativa e data de revisão**.

## Dependências

- **Requer Trilha2-02** — não existe imagem publicada sem build/registro; o scan é da
  artefato que aquele job produz.
- **Requer Trilha4-02** — o gate de segredo cobre **texto do repo**; este cobre o
  **conteúdo da imagem** — as duas redes de supply chain, uma complementa a outra.
- **pré-condição verificável:** run com job `build` verde e `docker pull` da imagem
  GHCR funcionando + gate de vazamento verde (T4-02).

## Escopo

- Trivy no CI: `trivy image` na imagem buildada (GHCR ou local antes do push — declarar
  qual ponto do pipeline), saída `table`/`sarif` salva como artefato do run
- `trivy.yaml` (ou flags no workflow) **versionado**: severidade de bloqueio e
  `ignorefile` se houver — a política é código, não CLI decorada no workflow
- Decide o threshold: CRITICAL (e HIGH?) que **falha** o job; o resto em report
- Achado real tratado: se for da camada base (`ubuntu`/`temurin`) → trocar tag/sufixo
  nova, rebuild, achado some **ou** registra no `.trivyignore` com CVE, motivo e data
- Fronteira de jar: vulnerabilidade em dependência Maven aparece no scan da imagem —
  atualizar o `pom.xml` é **fora** (app intocado): a resposta é ignore-documentado ou
  aceite; registrar o que ficou pendente para o dono do app
- **assume pronto:** `registro-ghcr-verde` (T2-02), `gate-vazamento-ci` (T4-02)
- **entrega:** `trivy-no-ci`, `threshold-declarado`, `achado-processado`

## Fora de escopo

- Corrigir CVE no `pom.xml`/código (app intocado — é informação para o dev, não tarefa
  de infra)
- SAST/DAST de código (Semgrep, ZAP) — código é do app; aqui é a **imagem**
- SBOM formal (Syft/cyclonedx) — complemento natural do Trivy (`--format cyclonedx`),
  entra se a questão aparecer, não como pré-requisito
- Scan contínuo diário de imagens já publicadas (re-scan agendado) — estágio de operação
  com repositório vivo

## Conhecimentos envolvidos

- Supply chain: da tag do base ao `jar` — cadeia de confiança do que sobe no `docker run`
- CVE e CVSS: o que é severidade e por que ela **não** é exploitabilidade (HIGH no lab
  sem o serviço atacável ≠ incidente)
- O que Trivy varre: OS packages, camadas, dependências empacotadas — e o que ele não
  vê (lógica do app)
- Threshold como política: vermelho sempre = desligado; nada vermelho = decoração —
  o jogo é o limiar que o time sustenta
- `.trivyignore` como dívida registrada (com expiração) vs. silêncio eterno

## Estado atual

- Imagem buildada e publicada (T2-02) com zero varredura — `docker pull && up` aceita
  qualquer conteúdo
- Dependências do Spring empacotadas sem verificação de CVE conhecida
- Nenhuma política de severidade escrita em lugar nenhum

## Resultado esperado

- Workflow do build (ou job acoplado) → etapa `trivy image` roda e publica report no run
- `trivy.yaml`/`ignorefile` no repo → definindo severidade que falha (grep confirma)
- Teste do gate: configurar para bloquear `MEDIUM` **ou** usar imagem com achado
  conhecido → job vermelho; voltar ao limiar decidido → verde (prova que o gate age)
- ≥ 1 achado processado de verdade (atualização de base **ou** ignore documentado)

## Requisitos

- Scan roda **na imagem** (não só no fs do repo) em todo run do build
- Report salvo como artefato (baixável do Actions) — o scan sem artefato morre com o
  log
- Threshold **versionado** (`trivy.yaml` ou bloco no workflow declarado no repo) com a
  severidade de bloqueio explícita
- Prova de que o gate bloqueia: um run **vermelho** causado pelo scan (e não apenas
  verde com print)
- Achado real: ou mudança que o resolve (tag base nova) ou `.trivyignore` com
  CVE + motivo + data
- Pendências de dependência Maven (se houver) registradas como nota para o app — sem
  tocar no `pom.xml`
- Custo de tempo de CI mantido (scan < ~2min ou cache habilitado — declarar)

## Critérios de aceitação

- [ ] Pré-condição: run do Actions com job `build` verde **e** `docker pull
      ghcr.io/<user>/<repo>:<tag>` ok **e** job de scan da T4-02 verde — sem os três,
      pare aqui
- [ ] Etapa de scan visível no workflow do build (`grep -A2 trivy .github/workflows/*`)
      → `trivy image` com a imagem GHCR/local declarada
- [ ] Report baixável do run (artefato com `table`/`sarif` do achado real)
- [ ] Threshold versionado: arquivo com `CRITICAL` (ou `CRITICAL,HIGH`) definido como
      severidade que falha — grep do repo acha a linha
- [ ] Prova negativa: run com o limiar forçado mais baixo **ou** imagem suja → job
      **vermelho** com falha vinda do Trivy; limiar restaurado → verde
- [ ] ≥ 1 achado processado: linha de atualização da tag base **ou** entrada em
      `.trivyignore` com CVE + motivo + data de revisão
- [ ] Nada de app tocado: `git diff` do processo sem mudança em `pom.xml`/`src/`
      (achado Maven vira nota, não commit)

## Validação

- Local primeiro (mesma versão do CI): `docker pull` da imagem →
  `trivy image --severity HIGH,CRITICAL <img>` → ler o report
- Subir o job: push de branch → Actions → etapa do scan roda → artefato baixado
- Testar o gate: criar branch com limiar `MEDIUM` → run vermelho → descartar branch
- Processar achado: se da base → trocar `FROM temurin:xx-jre` para patch novo →
  rebuild → re-scan → achado sumiu ou restou justificado
- Saúde: stack da VM continua 6×healthy com a imagem nova (se houve rebuild)

## Evidências

- Linha do workflow com `trivy image` + arquivo de threshold no repo
- Print/URL do run **vermelho** provocado pelo scan (gate age) + run verde normal
- Artefato do report com achados reais
- `.trivyignore` (ou diff da tag base) com o achado processado

## Limitações / notas

- Trivy mede **conhecimento de CVE**, não atacabilidade: CRITICAL em biblioteca que o
  caminho de rede do lab não alcança não é incidente — é dívida informada; o ignore com
  motivo é onde isso fica escrito (é por isso que "bloquear tudo" vira scan desligado)
- Achado em dependência Maven sem fix: app intocado → aceite documentado; o repo do
  app é que tem que subir a versão — registrar é a entrega, corrigir é de outro dono
  (fronteira do lab)
- `.trivyignore` envelhece: CVE coberto some sozinho em re-scan depois de update —
  a data de revisão no ignore é o que impede a lista eterna de silêncio
- Scan no build não pega o que **sobe depois** (drift na VM, imagem re-tagged) —
  re-scan agendado da imagem registrada é estágio de operação contínua
