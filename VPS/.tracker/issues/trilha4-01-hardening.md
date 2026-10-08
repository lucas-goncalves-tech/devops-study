---
aliases: [trilha4-01, hardening]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 01 — Hardening: auditar a VM com régua, consertar o que ela apontar

## Contexto

A Trilha 0 fez hardening na mão (chave, ufw, updates) e a Trilha 1–3 encheram a VM de
serviço: 6+ containers, portas de bridge do Docker, processos do host. A superfície
cresceu **sem** nova auditoria — "configurei seguro no começo" deixa de ser verdade a
cada `compose up`. Hardening de verdade aqui não é lista decorada de checklist de
internet: é **rodar uma ferramenta de auditoria** (Lynis), ler o que ela marca, consertar
o que faz sentido para este lab e **registrar o que foi aceito como está** — porque
toda ferramenta grita 40 avisos e a habilidade é separar o relevante do ruído. É o
pensamento que a vaga cobra como "security mindset": medir, priorizar, justificar.

## Objetivo

Estado final: Lynis audit na VM com score registrado (baseline antes/depois); ao menos 5
achados de severidade média/alta **corrigidos ou justificados** em arquivo versionado;
`ufw status` inalterado em regra (22/80/443) mas agora com evidência de que **não há
outro caminho** (portas/serviços auditados fora do Docker); sysctl e serviços
desnecessários ajustados conforme o que a auditoria apontou.

## Dependências

- **Requer Trilha3-04** — o runbook e o drill deram o mapa operacional da VM; hardening
  muda comportamento e precisa do procedimento de resposta por perto. Sem a Trilha 3
  feita, o "arrumar" vira experimento sem rede de segurança.
- **pré-condição verificável:** `RUNBOOK.md` rastreado + drill registrado + 6 serviços
  healthy na VM.

## Escopo

- Baseline: `lynis audit system` na VM **antes** de mexer (score e achados guardados)
- Correções priorizadas (as que a auditoria apontar e caberem no lab), ex.:
  - sysctl: `redirect_accept`/`send_redirects=0`, rp_filter, cookie de sessão — só o
    que a auditoria justificar
  - desligar serviços/portas desnecessários **fora** do escopo Docker (a régua ufw já
    cobre a borda, mas `ss -tlnp` mostra o que escuta em loopback)
  - permissões de arquivos críticos (`/etc/ssh`, `.env` 600 — já feito, agora medido)
  - timeouts de sessão/sudo se a auditoria exigir
- `docs/hardening.md` (ou seção no runbook): **achado → ação → justificativa** (consertou
  ou aceitou, com porquê) — a deliverable é o registro, não o score
- Auditoria final: score depois, delta registrado
- **assume pronto:** `runbook-3-procedimentos`, `drill-executado` (T3-04)
- **entrega:** `lynis-baseline`, `hardening-registro`, `score-antes-depois`

## Fora de escopo

- CIS Benchmark completo com automação (Ansible/OS hardening tools) — IaC é estágio AWS
- SELinux/AppArmor em modo enforcing customizado — distribuição traz default, mudar
  política é fora do escopo do lab (registrar o estado atual basta)
- WAF, IDS/IPS (fail2ban já discutido na T0-03) — borda é Trilha 4-03 se entrar
- Pen test/ataque real — a simulação de incidente é a Issue 04, aqui é defesa medida

## Conhecimentos envolvidos

- Lynis: o que ele audita, como ler score/warnings (score alto ≠ invulnerável)
- Superfície de ataque: porta escuta × porta alcançável (loopback × 0.0.0.0 × ufw)
- sysctl: parâmetros de rede/kernel e o que cada um muda de verdade
- Remediação vs. aceitação documentada: por que "aceito com justificativa" é
  segurança, e "ignorado" não é
- Drift: hardening não é evento único — o que muda quando novo serviço entra (fronteira
  com o ciclo de re-auditoria)

## Estado atual

- VM com hardening "na mão" na T0, sem medição formal e sem registro
- 6+ containers e bridges Docker novos desde então — superfície não auditada
- Nenhum score, nenhum documento de decisões de segurança do host

## Resultado esperado

- `lynis audit system` → log com `Hardening index` e lista de warnings
- `docs/hardening.md` no repo: ≥ 5 achados com ação/justificativa e o score antes/depois
- `ss -tlnp` interpretado: cada escuta classificada (necessária Docker/loopback/outras)
- `ufw status` → **continua** só 22/80/443 (auditoria não virou regra de abrir porta)
- Stack segue 6×healthy após as mudanças (hardening não quebrou operação)

## Requisitos

- Baseline Lynis **antes** de qualquer mudança (sem baseline, o "depois" não prova nada)
- Ao menos 5 achados tratados: **corrigido** (comando aplicado) **ou aceito**
  (justificativa escrita) — nada de "deixei passar"
- Registro versionado (`docs/hardening.md`): achado → severidade → ação → comando →
  porquê
- Toda mudança de sysctl/serviço testada com a stack de pé (não derrubar rede/SSH —
  regra da T0-02: nunca trancar a porta pela qual você está dentro)
- `ss -tlnp` + `ufw status` auditados ao final: cada porta explica sua existência
- Nenhuma regra nova no ufw sem justificativa no registro (padrão 22/80/443 é a base)
- Rollback viável: mudanças reversíveis documentadas (sysctl é arquivo, não runtime só)

## Critérios de aceitação

- [ ] Pré-condição: `ssh lab@<ip-vm> 'test -f ~/lab/RUNBOOK.md || ls docs/runbooks/'` →
      runbook existe **e** registro do drill da T3-04 presente **e**
      `docker compose ps` → 6×healthy — sem os três, pare aqui
- [ ] Baseline salva **antes**: log do primeiro `lynis audit system` com
      `Hardening index` numérico arquivado (na evidência ou no repo)
- [ ] `lynis audit system` final → score **≥** baseline (não piorou) e a diferença
      registrada
- [ ] `docs/hardening.md` rastreado no repo com ≥ 5 achados no formato
      achado/severidade/ação/justificativa (grep conta os campos)
- [ ] Ao menos 3 correções **aplicadas** de verdade (comando no registro bate com o
      estado atual da VM — conferir 2 delas por comando)
- [ ] `ss -tlnp` na VM: toda escuta em `0.0.0.0`/`::` fora de 80/443 e bridges Docker
      **justificada** no registro (nenhuma surpresa)
- [ ] `ufw status` → inalterado: `22,80,443` (auditoria não virou "abrir tudo")
- [ ] Após as mudanças: `docker compose ps` → 6×healthy **e** `ssh` continua entrando
      por chave (não trancou a porta — regra da T0-02 preservada)

## Validação

- Baseline: `lynis audit system | tee /tmp/lynis-before.log` → copiar `Hardening index`
- Corrigir 2–3 achados prioritários (sysctl por exemplo):
  `sysctl -w net.ipv4.conf.all.send_redirects=0` → persistir em
  `/etc/sysctl.d/99-lab.conf` → re-auditar
- Auditoria de superfície: `ss -tlnp` (host) → cruzar com `docker ps` → cada linha
  classificada; `ufw status` para a régua externa
- Re-auditar: `lynis audit system | tee /tmp/lynis-after.log` → comparar índices
- Saúde: `docker compose ps` → 6 healthy; login novo por chave (sessão de manutenção
  aberta durante a mudança — regra T0-02)
- Commit do registro + evidências

## Evidências

- Par de logs do Lynis: baseline (antes) e final (depois) com os `Hardening index`
- `docs/hardening.md` com os ≥ 5 achados e ações/justificativas
- `ss -tlnp` classificado + `ufw status` (inalterado)
- `docker compose ps` 6×healthy e login SSH por chave após as mudanças

## Limitações / notas

- **Score alto não é segurança**: Lynis mede configuração conhecida, não ataque real —
  é termômetro, não certificado; registrar isso no `hardening.md` é parte do entendimento
- Muitos warnings do Lynis são **inaplicáveis** a container host (kernel params de
  servidor físico, módulos que o lab não usa) — "aceito com justificativa" é o resultado
  correto para boa parte, e é por isso que a issue exige registro e não score máximo
- Sysctl persistente é `/etc/sysctl.d/`, não `sysctl -w` (runtime some no reboot) — a
  issue exige persistir; quem só rodou o `-w` perde a correção no próximo reboot
- Re-auditoria depois de mudança de stack: a Trilha 4-04 dá o hábito no exercício
  final; drift contínuo (novo serviço sem passar por auditoria) é a porta de entrada
  clássica — citar no registro como dívida conhecida
