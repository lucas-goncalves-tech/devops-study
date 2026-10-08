---
aliases: [trilha0-02, ssh-key-only]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 02 — Acesso: SSH só por chave, senha e root fora do jogo

## Contexto

A VM da Issue 01 abre a porta 22 para a rede e aceita login por senha — exatamente o alvo
de todo botnet: força bruta de senha roda 24/7 em qualquer IP público e a VM do lab vai
para um provedo real na Trilha 1. Enquanto senha for válida, o brute force só precisa de
tempo; quando a chave é o único caminho, a mesma varredura vira ruído. E é **agora**, com
snapshot de revert na mão (Issue 01), que vale experimentar errar: fechar o acesso errado
custa um `snapshot-revert`, não uma reinstalação.

## Objetivo

Estado final: de qualquer máquina, `ssh lab@<ip>` autentica **só** com chave SSH; senha,
root e login sem chave são recusados na porta 22 — e o acesso por chave continua funcionando
depois do endurecimento (a ordem importa: testar antes de trancar).

## Dependências

- **Requer Trilha0-01** — a VM `lab-vm` com snapshot `base` é onde isto é aplicado, e o
  revert é a rede de segurança do experimento. Sem `snapshot-base` confirmado, esta Issue
  não começa.
- **pré-condição verificável:** `virsh snapshot-list lab-vm` contém `base`.

## Escopo

- Par de chaves no host (`ed25519`), pública instalada em `~/.ssh/authorized_keys` da VM
- `PasswordAuthentication no`, `PermitRootLogin no`, `PubkeyAuthentication yes`
- Teste de falha: senha e root devem ser **recusados**, chave deve entrar
- **assume pronto:** `vm-lab-vm`, `snapshot-base` — da Issue 01
- **entrega:** `ssh-key-only`, `sem-login-senha`, `sem-root-ssh`

## Fora de escopo

- Firewall/ufw (porta 22 ainda aberta para a rede) — Issue 03
- fail2ban, TOTP/2FA, porta SSH não-padrão — discussão da Issue 03/04
- Chaves de produção, bastion, certificados — estágio AWS

## Conhecimentos envolvidos

- Par de chaves: pública vs privada, ed25519 vs rsa, por que a privada nunca sai do host
- sshd_config: cada diretiva e o que ela desliga
- A ordem operacional "testa antes de trancar" (uma sessão de manutenção sempre aberta)
- Diferença entre recusar senha e não ter conta root

## Estado atual

- SSH aceita senha (e aceitaria senha fraca)
- Root acessível por SSH se alguém advinhar a senha
- Nenhuma chave instalada — o acesso inteiro depende de digitar senha

## Resultado esperado

- `ssh lab@<ip>` entra sem pedir senha (chave)
- `ssh -o PreferredAuthentications=password lab@<ip>` → recusado
- `ssh root@<ip>` → recusado
- Sobrevive a reboot da VM (sshd configurado, não em runtime)

## Requisitos

- Par `ed25519` gerado no host, pública em `authorized_keys` do usuário `lab`
- `sshd_config` com `PasswordAuthentication no` e `PermitRootLogin no`
- Sessão de manutenção mantida aberta **antes** de reiniciar o sshd (regra: nunca trancar
  a porta pela qual você está dentro sem outra forma de entrar — o revert da Issue 01 é a
  última rede, não a primeira)
- Validação de config `sshd -t` antes de `systemctl reload ssh`
- Config sobrevive a reboot

## Critérios de aceitação

- [ ] Pré-condição: `virsh snapshot-list lab-vm` contém `base` (Issue 01) — sem ele, pare aqui
- [ ] `ssh -o BatchMode=yes lab@<ip> 'echo ok'` → `ok` sem prompt de senha
- [ ] `ssh -o PreferredAuthentications=password -o PubkeyAuthentication=no lab@<ip>` →
      autenticação recusada ("Permission denied (publickey)")
- [ ] `ssh root@<ip>` → recusado
- [ ] `sshd -t` → silêncio (config válida) antes do reload; `systemctl is-active ssh` → active
- [ ] Reboot da VM → `ssh lab@<ip>` continua entrando por chave
- [ ] `grep -E '^(PasswordAuthentication|PermitRootLogin)' /etc/ssh/sshd_config` → `no` nos dois

## Validação

- Com a VM no ar: `ssh -o BatchMode=yes lab@<ip> 'echo ok; id -un'` → `ok` e `lab`
- Teste negativo de senha (esperado: recusa): `ssh -o PreferredAuthentications=password -o
  PubkeyAuthentication=no lab@<ip>` → `Permission denied`
- Teste negativo de root: `ssh root@<ip>` → `Permission denied`
- `sudo systemctl reboot` na VM; após subir, repetir o login por chave → funciona
- `journalctl -u ssh -n 20` → mostra as tentativas recusadas (evidência do "não" acontecendo)

## Evidências

- Saída do login por chave com `-v` mostrando `Offering public key` → `Accepted publickey`
- Saída dos dois testes negativos (senha e root) com `Permission denied`
- Saída de `sshd -t` (vazia) e `grep` das diretivas no `sshd_config`
- Saída pós-reboot do login por chave

## Limitações / notas

- A porta 22 continua **aberta para a rede** — a chave torna o brute force inútil, mas o
  firewall (fechar tudo que não é necessário) é a Issue 03; aqui muda-se **quem** autentica,
  não **de onde** é possível falar
- O `authorized_keys` é uma lista de identidade: apagar a chave errada derruba o acesso —
  por isso a sessão de manutenção fica aberta até o teste final
- Chave sem passphrase protege contra roubo de arquivo, não contra roubo da máquina inteira
  — passphrase + agent é o próximo degrau, discutido no estudo desta Issue
