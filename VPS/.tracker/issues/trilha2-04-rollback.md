---
aliases: [trilha2-04, rollback]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 04 — Rollback: o deploy que desiste sozinho quando não fica saudável

## Contexto

O deploy da Issue 03 troca a imagem e **espera** o `200` — mas se o health não vier, o
job fica vermelho e a VM fica com a versão ruim rodando. Verde/vermelho no CI não conserta
o sistema no ar: alguém precisa voltar. Rollback automático é fechar o ciclo: mesma tag
anterior (já no registro, imutável) volta a rodar e o pipeline **reporta a crise** em vez
de só dela. É a diferença entre "meu deploy falhou" e "meu deploy falhou e o sistema
continuou de pé" — a pergunta que separa quem aperta botão de quem opera.

## Objetivo

Estado final: deploy que não alcança `200` no timeout **reverte automaticamente** para o
SHA implantado anteriormente (lido da VM antes da troca), a stack volta `healthy` com a
versão boa, e o job termina **vermelho** com o rollback registrado — o sistema nunca fica
pendurado em versão que não responde, e o pipeline nunca mente (falha, mesmo tendo
consertado).

## Dependências

- **Requer Trilha2-03** — rollback é o caminho de volta do deploy: sem `job-deploy`,
  `deploy-script` e `ssh-secret`, não há o que reverter nem de onde ler o SHA anterior.
- **pré-condição verificável:** último deploy vermelho→verde na main com health `200`
  registrado no log (o fluxo feliz da 03 existe antes de tratar o infeliz).

## Escopo

- `scripts/deploy.sh` estendido: **antes** da troca, ler e gravar o `Image` atual da VM
  (SHA anterior); após o poll de health falhar → `compose pull <anterior> && up -d` →
  novo poll → sair com código ≠ 0
- Timeout declarado no poll (ex.: 60s com intervalo) — declarado, não "espera eterna"
- Passo de rollback no workflow com `if: failure()` **após** o deploy (ou dentro do
  script — decidir e declarar onde mora a lógica)
- Simulação controlada de falha: tag boa publicada mas app quebrada (ex.: env inválida
  injetada no run de teste do rollback) para **provar** o caminho infeliz
- **assume pronto:** `job-deploy`, `deploy-script`, `ssh-secret` — da Issue 03
- **entrega:** `rollback-automatico`, `timeout-declarado`, `falha-reportada`

## Fora de escopo

- Blue-green/canary com tráfego dividido (trocar versão sem downtime) — estágio AWS
- Auto-heal por métrica (Prometheus decide o rollback) — Trilha 3 dá a base, automação
  por sinal é futuro
- Feature flags / kill switch — estágio futuro
- Rollback de migração de banco (Flyway undo) — estágio futuro, discutido no estudo

## Conhecimentos envolvidos

- Rollback: para **onde** voltar (tag imutável da Issue 02 é o que torna possível)
- Health gate com timeout: poll declarado vs. espera infinita; o que é "falhar feio"
- `if: failure()` no Actions — o passo que roda quando o caminho deu errado
- O pipeline que conserta **e** reporta: exit ≠ 0 mesmo com sistema de pé (nunca verde
  com rollback)
- Rollback de app vs. rollback de schema: por que banco quebrado não volta sozinho
  (fronteira clara do que esta issue **não** faz)

## Estado atual

- Deploy da 03: se o health não vier, job vermelho e **VM com versão ruim rodando**
- O SHA anterior não é registrado antes da troca (não há "onde voltar" declarado)
- Nenhum teste do caminho de falha — só o feliz foi exercitado

## Resultado esperado

- Deploy com health que não responde → após timeout, log mostra o **revert para o SHA
  anterior** e o `200` da versão boa → job `failure` com "rolled back to <sha>"
- A VM, ao final da crise, roda a versão anterior e `docker compose ps` → healthy
- Timeout aparece no log com valor (ex.: `gave up after 60s`), não indefinido
- Re-run do job → de novo vermelho (a falha é reprodutível, não fantasma)

## Requisitos

- Captura do SHA atual **antes** da troca (todo deploy tem um "de onde eu venho")
- Poll de health com `timeout` e `intervalo` declarados no script (variáveis, não
  hardcoded em silêncio)
- Caminho de rollback: mesmo `compose pull`/`up -d` do deploy, com a tag anterior —
  rollback **é** deploy para trás, não procedimento especial
- Exit ≠ 0 obrigatório após rollback: job falha **sempre** que a versão nova não serviu,
  mesmo com o sistema de pé (verde com rollback seria mentira)
- Rollback registrada no log com SHA de origem e SHA implantado
- Teste do caminho infeliz é **parte da issue** (a prova do rollback é provocar falha)
- Schema (Flyway): declarado como não coberto — se a versão nova migrar banco, o revert
  de código não desfaz dados (limitação honesta no estudo e nas notas)

## Critérios de aceitação

- [ ] Pré-condição: deploy feliz da Issue 03 verificado — `docker compose ps` na VM com
      imagem `ghcr.io/...:<sha>` e log do deploy com `200`; `scripts/deploy.sh` no repo —
      sem isso, pare aqui
- [ ] O script lê e grava o SHA anterior antes da troca (log do deploy mostra
      `previous: <sha>` antes de `pull`)
- [ ] Simulação: deploy de tag com app que não fica `200` → log mostra `gave up after
      <timeout>` + `rolled back to <sha anterior>` + o novo `200` da versão boa
- [ ] O job termina **`failure`** mesmo com a VM saudável no final (exit ≠ 0 visível na
      UI do Actions) — verde nunca esconde rollback
- [ ] Após a simulação: na VM, `docker compose ps` → a **versão anterior** rodando,
      healthy (o sistema não ficou pendurado)
- [ ] Timeout com valor declarado no script (`grep -E 'TIMEOUT|timeout=' scripts/deploy.sh`
      → constante com número)
- [ ] Re-run do mesmo run → mesma sequência e mesmo vermelho (reprodutível)
- [ ] `grep` no script: rollback usa a mesma tag SHA imutável (nada de `latest` no revert)

## Validação

- Caminho feliz (controle): merge normal → deploy verde, `previous` e `current` iguais
  ou troca limpa com `200`
- Caminho infeliz: publicar tag quebrada (ex.: `SPRING_PROFILES_ACTIVE=inexistente` via
  env do run, ou imagem com health errado) → forçar o `deploy` → observar no log a
  sequência: `previous: X` → `pull nova` → poll → `gave up after 60s` → `pull X` →
  `up -d` → `200` → `exit 1`
- Estado pós-crise: `ssh ... 'docker compose ps'` → imagem X, healthy
- UI do Actions: run com `deploy` vermelho e nota de rollback; artifact/log preservado
- Limpar a simulação (revert do commit de teste) → deploy verde de volta

## Evidências

- Log completo da simulação: `previous`, timeout com valor, `rolled back to`, `200` final,
  exit ≠ 0
- `docker compose ps` da VM logo após a crise (versão anterior, healthy)
- Dois screenshots/linhas da UI: run vermelho do rollback × run verde do deploy normal
- Trecho do script mostrando o timeout declarado e o rollback usando tag SHA

## Limitações / notas

- O rollback **não** desfaz migração de Flyway: se a versão ruim criou coluna/tabela e a
  boa não conhece, voltar o código não volta o schema — é a fronteira clássica entre
  "deploy de app" e "deploy de dado". Neste lab as migrações da app são aditivas, mas a
  regra vale para o futuro: evolução de schema pensada para compatibilidade (expand/
  contract) é estágio futuro — fora do tracker
- Simular falha exige uma tag publicada que não sobe — a forma escolhida vai para
  Limitações da execução (env inválida × imagem propositalmente errada); o que importa é
  que a falha seja **real**, não mockada no script
- Rollback automático conserta indisponibilidade, não **dados** corrompidos — app que
  gravou errado antes de falhar deixou rastro que só backup resolve (Trilha 0-05 já deu o
  hábito; o backup de banco é estágio futuro — fora do tracker)
- `if: failure()` vs. lógica dentro do script: ter os dois (passo no workflow **e**
  tratamento no script) duplica caminho — a issue pede **decidir e declarar** um; o
  estudo compara as duas posições
