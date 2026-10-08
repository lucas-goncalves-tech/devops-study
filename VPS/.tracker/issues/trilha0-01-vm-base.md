---
aliases: [trilha0-01, vm-base]
tags: [tracker, issue, todo]
status: todo
prioridade: alta
---

# Issue 01 — Fundação: VM Ubuntu Server em KVM com snapshot de base

## Contexto

O lab inteiro será montado e quebrado nesta máquina — se ela não puder ser recriada ou
revertida sem medo, cada experimento com custo de reinstalação vira experimento adiado.
Hoje não existe a máquina: sem VM não há onde aplicar SSH, firewall ou systemd, e sem
snapshot o primeiro erro de config (ufw mal configurado, por exemplo) derruba o acesso e
obriga a instalação do zero. Snapshot também é o que permite provar rollback — conceito que
volga na Trilha 1 (deploy) e no estágio AWS.

## Objetivo

Estado final: VM `lab-vm` com Ubuntu Server 24.04 rodando em KVM/virt-manager, usuário
`lab` não-root com sudo, hostname `lab-vm`, e um snapshot `base` que `virsh snapshot-revert`
devolve ao estado limpo — provado por revert real, não por confiança.

## Dependências

- Nenhuma — issue de partida do lab.
- Pré-requisito do **host**: KVM + virt-manager instalados e o usuário do host no grupo
  `libvirt` (`groups | grep libvirt`). Sem isso não existe esta Issue.

## Escopo

- VM com qcow2 (descartável, copy-on-write), 2 vCPU, 2–4 GB RAM, disco ≥ 20 GB
- Instalação do Ubuntu Server 24.04 com usuário `lab` (particionamento padrão, LVM não é
  assunto desta Issue)
- Hostname `lab-vm`, rede NAT padrão do libvirt com IP anotado
- Snapshot `base` tirado com a VM limpa e desligada (ou em quiesce)
- **assume pronto:** host com KVM/virt-manager — da sua máquina, não do lab
- **entrega:** `vm-lab-vm`, `snapshot-base`, `usuario-lab-sudo`

## Fora de escopo

- SSH key-only, firewall, atualizações — Issue 02 e 03 (o acesso inicial é por senha, é o
  que a Issue 02 endurece)
- systemd, backups, qualquer serviço — Issues 04 e 05
- Rede bridge/host-only, máquinas múltiplas, cloud — Trilha 1+ e estágio AWS

## Conhecimentos envolvidos

- Hipervisor: KVM, qemu, libvirt e a diferença entre virt-manager (GUI) e virsh (CLI)
- qcow2 versus raw; o que é copy-on-write e por que snapshot não é backup
- Partes de uma VM: vCPU, RAM, disco, rede NAT do libvirt
- Usuário não-root + sudo no primeiro boot

## Estado atual

- Não existe `lab-vm`; não há VM nem snapshot no host
- Não há onde aplicar o resto da Trilha 0

## Resultado esperado

- `virsh list --all` mostra `lab-vm` `running` após boot
- `virsh snapshot-list lab-vm` contém `base`
- Login como `lab` com sudo funcionando (`sudo -n id` não pede senha quando já autenticado)
- Revert de snapshot devolve a VM ao estado marcado — arquivo de teste some após o revert

## Requisitos

- Ubuntu Server 24.04 em VM KVM, nome `lab-vm`, criada por virt-manager
- Usuário `lab` com senha e entrada no grupo `sudo`; root não é usado no dia a dia
- Snapshot `base` criado com a VM em estado conhecido (pós-instalação, sem sujeira)
- Prova de que o snapshot funciona: marcar um arquivo, revertir, confirmar que sumiu
- IP da VM anotado — ele é a porta de entrada de todas as Issues seguintes

## Critérios de aceitação

- [ ] Pré-condição de partida: host tem KVM — `kvm-ok` ou `ls /dev/kvm` existe e o usuário
      está no grupo `libvirt`
- [ ] `virsh list --all` → `lab-vm` presente; `virsh dominfo lab-vm` → 2 vCPU, disco qcow2
- [ ] Login na VM como `lab` e `sudo -v` aceita a senha do usuário
- [ ] `hostnamectl hostname` → `lab-vm`
- [ ] `virsh snapshot-list lab-vm` → snapshot `base` na lista
- [ ] Prova de revert: `touch /tmp/marca` na VM → `virsh snapshot-revert lab-vm base` →
      `ls /tmp/marca` não existe mais (boot fresh do snapshot)
- [ ] Após o revert, a VM sobe e o login `lab` continua funcionando

## Validação

- `virsh list --all` → `lab-vm` `running`
- `virsh dominfo lab-vm` → `ID`, `Estado: running`, `Discos: ...qcow2`
- Dentro da VM: `hostnamectl hostname` → `lab-vm`; `id lab` → `sudo` no grupo
- `touch /tmp/marca` e depois `virsh snapshot-revert lab-vm base` → VM reinicia do snapshot;
  `ls /tmp/marca` → vazio
- `virsh snapshot-list lab-vm` → `base` aparece com data e tamanho

## Evidências

- Saída de `virsh list --all` e `virsh snapshot-list lab-vm`
- Saída de `id lab` dentro da VM mostrando o grupo `sudo`
- Saída do teste de revert: `ls /tmp/marca` antes (existe) e depois (não existe)
- Print do virt-manager com a VM no ar (opcional)

## Limitações / notas

- **Snapshot não é backup**: o qcow2 e os snapshots moram no mesmo disco do host — se o
  disco do host morre, tudo morre junto. Backup de verdade é a Issue 05
- NAT do libvirt atribui IP por DHCP da rede `default` — anotar o IP; se a rede for
  recriada, o IP pode mudar ( ISSUE 02 fixa o acesso por chave, não por IP)
- O snapshot `base` deve ser tirado **antes** de qualquer endurecimento: ele é o estado
  que as Issues 02–05 revertem quando um experimento fecha a porta errada
