---
aliases: [trilha0-03, firewall-updates]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 03 — Firewall e atualizações: tudo fechado, tudo corrigido

## Contexto

Depois da Issue 02 a porta 22 aceita só chave, mas continua **abrindo para qualquer IP** —
e qualquer outra porta que um serviço venha a ligar (o que acontece na Trilha 1, com
Postgres e Redis) nasce aberta por padrão em muitas distros. Firewalls não são para
"proteger de hackers" em abstrato: são para garantir que **só o que foi decidido** fala.
Ao lado, a outra metade da superfície: pacote desatualizado é vulnerabilidade conhecida —
`unattended-upgrades` é o que transforma "eu deveria atualizar" em "o sistema atualiza
sozinho e registra". Juntos formam o piso mínimo que qualquer vaga de infra cobra.

## Objetivo

Estado final: `ufw` ativo com política default **deny incoming / allow outgoing**, só a
porta 22/tcp liberada, atacando de fora a porta 80/5432 → recusado (não-escutado/filtered);
e `unattended-upgrades` habilitado com log de execução — depois de `apt update && apt
upgrade`, zero pacotes pendentes.

## Dependências

- **Requer Trilha0-02** — o firewall entra **depois** do SSH por chave: fechar a porta
  errada com login por senha e sem revert testado é o jeito clássico de se trancar fora.
- **pré-condição verificável:** `ssh -o BatchMode=yes lab@<ip> 'echo ok'` → `ok` (chave
  funcionando) **e** snapshot `base` presente (rede de segurança).

## Escopo

- `ufw default deny incoming`, `default allow outgoing`, allow `22/tcp` (e `22/tcp` só)
- Teste de recusa vindo de **fora da VM** (host), não de dentro — de dentro o pacote nunca
  é bloqueado e o teste mente
- `ufw` habilitado com `--force` e estado persistente após reboot
- `unattended-upgrades` habilitado e com evidência de execução no log
- `apt update && apt upgrade` deixando o sistema limpo como ponto de partida
- **assume pronto:** `ssh-key-only` — da Issue 02
- **entrega:** `ufw-deny-default`, `só-22-aberto`, `auto-updates-ativos`

## Fora de escopo

- Portas 80/443 (reverse proxy) e 5432/6379 (banco) — quem as abrirá é a Trilha 1, e elas
  entrarão **por regra explícita** nesta base
- fail2ban, IDS, rate limit de borda — Trilha 1+ e estágio DevSecOps
- Renovação de segurança de kernel/livepatch — fora do lab

## Conhecimentos envolvidos

- Firewall: política default, regras explícitas, a diferença entre incoming/outgoing
- Por que testar de fora (rede/loopback) e não de dentro
- ufw por cima de iptables/nftables — o que o atalho faz
- `unattended-upgrades`: apt, cron.d, log em `/var/log/unattended-upgrades/`
- Atualização como rotina vs. como evento manual

## Estado atual

- Nenhum firewall configurado (padrão da imagem: tudo que subir fica exposto)
- Atualizações manuais — dependem de alguém lembrar
- Snapshot existe mas a VM está com a config da instalação, sem padrão de exposição

## Resultado esperado

- `ufw status verbose` → `Status: active`, `default deny (incoming)`, `22/tcp ALLOW`
- De fora: `nc -zv <ip> 5432` (ou porta fechada) → recusado/timeout; porta 22 → aberta
- Reboot → ufw continua ativo com as mesmas regras
- `unattended-upgrades` enabled e ao menos uma execução registrada no log
- `apt list --upgradable` → vazio

## Requisitos

- Política default deny incoming **antes** de liberar 22 (ordem: nunca deny-total sem a
  regra de acesso já testada)
- Liberação de 22 feita e **testada de fora** antes de `ufw enable` (ufw ativa com regra de
  ssh incluída automaticamente, mas o teste de fora é obrigatório)
- `unattended-upgrades` configurado e habilitado no systemd (`systemctl is-enabled`)
- Upgrade completo aplicado e logado
- Leitura do estado feita com o teste negativo correspondente — "está ativo" não é prova,
  "recusou de fora" é

## Critérios de aceitação

- [ ] Pré-condição: `ssh -o BatchMode=yes lab@<ip> 'echo ok'` → `ok` **e**
      `virsh snapshot-list lab-vm` contém `base` — sem os dois, pare aqui
- [ ] `sudo ufw status verbose` → `Status: active`, `default deny (incoming)`, única
      ALLOW é `22/tcp`
- [ ] Teste de fora (no **host**): `nc -zv <ip> 80` → falha/recusa, `nc -zv <ip> 22` → sucesso
- [ ] Reboot da VM → `sudo ufw status` idêntico ao de antes (persistência)
- [ ] `systemctl is-enabled unattended-upgrades` → `enabled`
- [ ] `grep -c 'Unattended upgrade' /var/log/unattended-upgrades/*.log*` (ou log do dia) →
      evidência de ao menos uma execução
- [ ] `apt list --upgradable 2>/dev/null | wc -l` → `1` (só o cabeçalho = zero pendentes)

## Validação

- Dentro da VM: `sudo ufw status verbose | head -5` → política e regra 22
- No **host**: `nc -zv <ip> 22` → `succeeded`; `nc -zv <ip> 5432` → `refused`/timeout —
  mesma origem, portas diferentes, resultados diferentes (é a prova do firewall, não da
  falta de serviço)
- `sudo systemctl reboot`; repetir os dois `nc` do host → mesmos resultados (persistência)
- `sudo systemctl status unattended-upgrades` → enabled/active; `sudo cat
  /var/log/unattended-upgrades/unattended-upgrades.log | tail` → execução registrada
- `sudo apt update && sudo apt list --upgradable` → vazio após o upgrade

## Evidências

- `ufw status verbose` colado (política + regras)
- Os dois `nc` do host lado a lado: 22 succeeded, 80/5432 falhou — **feito do host**,
  anotando da qual origem veio o teste
- `systemctl is-enabled unattended-upgrades` + trecho do log com data
- `apt list --upgradable` vazio

## Limitações / notas

- `ufw` protege a própria VM; a VM atrás de NAT do libvirt já não é exposta na sua rede
  local — **mesmo assim** se configura deny: a NAT cai quando a VM virar VPS pública na
  Trilha 1, e a regra já estará certa
- Testar de dentro da VM (`nc localhost 80`) não prova nada sobre o firewall — o tráfego de
  loopback não passa pelas regras; quem fizer isso e colar como evidência está medindo o
  erro
- `unattended-upgrades` padrão do Ubuntu só cobre as seeds principais; repos de terceiros
  (como o do Docker, Trilha 1) **não** entram nessa automação — fica para a Issue de
  atualizações da Trilha 1
