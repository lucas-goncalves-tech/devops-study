---
aliases: [trilha6-02, filesystems-mounts]
tags: [tracker, issue, todo]
status: todo
prioridade: media
---

# Issue 02 — filesystems: o que existe, o que monta e o que é LVM

## Contexto

A T0-01 instalou a `lab-vm` com o particionamento padrão do instalador e declarou na hora
que LVM não era assunto daquela issue; mounts nunca foram matéria de nenhuma trilha do
tracker. A dívida fica aberta até alguém precisar responder "onde está o espaço?",
"por que o `df` mostra 70% e o sistema diz disco cheio?" ou "por que aquele arquivo de
config aparece dentro do container sem ninguém ter copiado?". Com a stack das Trilhas 1–3
rodando, a VM tem Docker, série temporal do Prometheus e logs girando no disco; sem
`df`/`lsblk`/`findmnt`, cada diagnóstico de armazenamento vira adivinhação — e o único
erro de lab que **impede o boot** (um `/etc/fstab` com UUID errado) não tem nem leitura
nem ensaio hoje. Como este repo é o lab inteiro, entender bind mount aqui é também
entender por que os volumes do `compose.yaml` e o scrape config montado no Prometheus da
T3-02 funcionam.

## Objetivo

Estado final: na `lab-vm`, `df -h`, `df -i`, `lsblk` e `findmnt` explicam cada linha da
árvore de montagem (incluindo `proc`/`tmpfs`/`devtmpfs`); uma entrada `UUID=` em
`/etc/fstab` sobrevive a um `reboot` de verdade; um bind mount e um filesystem em arquivo
loop foram montados, observados em `findmnt` e desmontados; e o LVM da instalação está
**identificado** (`lsblk`/`pvs`, sem criar nada).

## Dependências

- **Requer Trilha0-01** — o particionamento real da `lab-vm` é o objeto de estudo desta
  issue; sem a VM no ar não existe `df`/`lsblk` para ler.
- **pré-condição verificável:** `virsh list --all` → `lab-vm` `running` **e**
  `ssh lab@<ip> 'df -hT /'` → linha com o filesystem raiz.

- **estudo par:** `estudos/trilha6-02-filesystems-mounts.md` — ler antes de executar (é o currículo desta issue)

## Escopo

- Leitura de disco em par: `df -h` (bytes) e `df -i` (inodes), com o caso "espaço livre,
  inode esgotado" ensaiado em filesystem descartável
- Árvore de montagem: `lsblk -f`, `findmnt` e `/proc/mounts` — e o porquê de `proc`,
  `tmpfs` e `devtmpfs` não ocuparem disco da VM
- `fstab` com `UUID=`: montagem declarada (sobrevive ao reboot) vs. montagem em runtime
  (`mount`, morre no reboot), `noauto`, e o boot travado por fstab errado com a saída
  (`mount -o remount,rw /` no shell de emergência e live ISO montando a raiz em `/mnt`)
- bind mount com `mount --bind` — a mesma técnica que sustenta os volumes do
  `compose.yaml` (T1-02) e o arquivo de scrape config montado no Prometheus (T3-02)
- filesystem em arquivo: `truncate -s 100M fs.img` → `mkfs.ext4` → `mount -o loop`,
  montado e desmontado (partição de ensaio sem tocar no disco real, seguro na VM)
- LVM como **leitura e conhecimento**: PV/VG/LV, por que a T0-01 adiou e quando vale
  (volume cresce sem reparticionar) — identificado com `lsblk`/`pvs`, sem setup completo
- **assume pronto:** VM `lab-vm`, snapshot `base` e usuário `lab` com sudo — da T0-01;
  stack das Trilhas 1–3 (`compose.yaml`, Prometheus) como exemplo vivo de bind mount
- **entrega:** `disco-lido-df-lsblk-findmnt`, `fstab-uuid-provado-apos-reboot`,
  `bind-mount-e-loop-ensaiados`, `lvm-identificado`

## Fora de escopo

- LVM em produção com resize (`lvextend`/`resize2fs` em volume em uso) — Área 2 / estágio
  futuro, sobre EBS volume
- RAID, ZFS e btrfs — dívida declarada: nenhuma issue do tracker os cobre, destino é o
  backlog de trilha nova de storage do futuro
- Rotação de logs — T3-01 (o disco enche, mas a política de retenção é dela)
- Backup e restore — T0-05 (o que fazer quando o filesystem perde dado não é aqui)
- Reparticionar, formatar ou migrar o disco real da `lab-vm` — nenhum `fdisk`/`parted`
  na VM; `mkfs` só em arquivo

## Conhecimentos envolvidos

- `df -h` vs `df -i`: bytes e inodes são dois tetos independentes do mesmo filesystem
- Árvore de montagem: `findmnt`, `/proc/mounts` e o que são `proc`, `tmpfs`, `devtmpfs`
- fstab: campos, `UUID=`, `noauto`, `pass`/fsck — declarado vs. montado agora
- Boot do systemd e emergency mode quando uma mount obrigatória falha; `remount,rw`
- bind mount e loop device (`losetup`) como montagens que não pedem disco novo
- LVM: PV → VG → LV, a camada entre disco e filesystem

## Estado atual

- Ninguém no lab leu o armazenamento da VM: não há `df`/`lsblk`/`findmnt` em nenhuma
  Evidências do tracker
- O `/etc/fstab` da VM é o que o instalador escreveu: nunca foi testado com `reboot`
  nem explicado
- Nenhum bind mount ensaiado à mão — os mounts do `compose.yaml` e do Prometheus
  funcionam sem ninguém ter aberto um `findmnt` para ver de onde vêm
- LVM segue declarado fora de escopo na T0-01 sem nenhuma leitura posterior (dívida aberta)

## Resultado esperado

- `df -hT /` e `df -i /` na VM respondem e as duas leituras (bytes e inodes) estão
  coladas como Evidências
- `lsblk -f` + `findmnt` explicam cada linha: o que é partição, o que é
  `proc`/`tmpfs`/`devtmpfs`, o que é loop
- `grep UUID /etc/fstab` → entrada da raiz, e `findmnt -o UUID /` depois de `reboot`
  bate com ela
- `mount --bind` visível em `findmnt /mnt/lab-bind`, escrita refletida na origem,
  desmontado sem resto
- `fs.img` de 100 MB montado via `mount -o loop` (SOURCE `/dev/loop0` em `findmnt`) e
  desmontado (`findmnt` do ponto falha)
- Identificação LVM: `lsblk -o NAME,TYPE,FSTYPE,MOUNTPOINT` classificado (e `sudo vgs`
  responder se houver VG), sem nenhum comando de criação

## Requisitos

- Leitura sempre em par: bytes (`df -h`) **e** inodes (`df -i`) — só um não fecha
  diagnóstico
- Toda montagem lida por dois pontos: o que está montado **agora** (`findmnt`/`/proc/mounts`)
  e o que está **declarado** para o boot (`/etc/fstab`)
- fstab usa `UUID=`, nunca nome de dispositivo (`/dev/vda2`); entrada cuja ausência
  derruba o boot passa por `sudo findmnt --verify` antes de qualquer `reboot`
- Ensaio de fstab sempre com backup (`/etc/fstab.bak`) antes de editar
- bind mount e loop montados, observados em `findmnt` e desmontados — nenhum sobra
  montado ao fim da issue
- Nenhum comando de escrita no disco real: zero `fdisk`/`parted`/`mkfs` fora de arquivo
  em `/tmp`
- LVM tratado somente como leitura (`lsblk`, `pvs`, `vgs`, `lvs`) — nenhum `pvcreate`,
  `vgcreate`, `lvcreate` ou `lvextend`

## Critérios de aceitação

- [ ] Pré-condição: `virsh list --all` → `lab-vm` `running` **e**
      `ssh lab@<ip> 'df -hT /'` → linha com filesystem raiz — sem os dois, pare aqui
- [ ] `ssh lab@<ip> 'df -hT /; df -i /'` → as duas saídas e o `IUse%` numérico da raiz
      colado em Evidências (inode lido, não só byte)
- [ ] `lsblk -f` classificado: cada linha explicada por `FSTYPE`/`MOUNTPOINT` (partição,
      boot, `lvm` ou não) — saída colada em Evidências
- [ ] `findmnt -o TARGET,SOURCE,FSTYPE` mostra `/proc` → `proc`, `/dev` → `devtmpfs` e
      `/dev/shm` (ou `/run`) → `tmpfs`; `grep -E ' /proc ' /proc/mounts` → `proc`, e
      `df -h /proc` não consome disco da VM
- [ ] `grep UUID /etc/fstab` → ao menos uma linha `UUID=` da raiz **e**
      `sudo findmnt --verify` sem erro para a linha da raiz
- [ ] Depois de `sudo reboot`: `findmnt -o UUID /` devolve o mesmo UUID do fstab — a
      montagem veio da declaração, não de `mount` manual
- [ ] bind mount: `sudo mount --bind /home/lab /mnt/lab-bind` → `findmnt /mnt/lab-bind`
      com linha; `touch /mnt/lab-bind/marca` aparece como `/home/lab/marca`;
      `sudo umount /mnt/lab-bind` → `findmnt /mnt/lab-bind` falha (exit ≠ 0)
- [ ] loop: `truncate -s 100M /tmp/fs.img && sudo mkfs.ext4 -F /tmp/fs.img && sudo
      mount -o loop /tmp/fs.img /mnt/lab-loop` → `findmnt /mnt/lab-loop` com SOURCE
      `/dev/loop0` **e** `losetup -a` mostrando `/tmp/fs.img`; `sudo umount /mnt/lab-loop`
      → `findmnt /mnt/lab-loop` falha (exit ≠ 0)
- [ ] LVM identificado, não instalado: `lsblk -o NAME,TYPE,FSTYPE,MOUNTPOINT` explica se
      a T0-01 saiu com `lvm` ou sem; se existir VG, `sudo vgs` responde — sem nenhum
      `pvcreate`/`lvcreate` nas Evidências
- [ ] Nada montado sobra no fim: `findmnt | grep -E 'lab-bind|lab-loop'` → vazio

## Validação

- Leitura base na VM: `df -hT; df -i` → duas tabelas; `lsblk -f` → árvore com FSTYPE;
  `findmnt` → `/` em disco real, `/proc` em `proc`, `/dev/shm` em `tmpfs`
- Ensaio de inode esgotado (filesystem descartável, não a raiz): `truncate -s 100M
  /tmp/fs.img && sudo mkfs.ext4 -N 2000 -F /tmp/fs.img && sudo mount -o loop /tmp/fs.img
  /mnt/lab-loop` → `sudo sh -c 'for i in $(seq 1 3000); do touch
  /mnt/lab-loop/arq$i; done'` falha com `No space left on device` → `df -h /mnt/lab-loop`
  com espaço livre **e** `df -i /mnt/lab-loop` com `IUse% 100` → `sudo umount`
- fstab: `sudo cp /etc/fstab /etc/fstab.bak` → conferir/criar a linha `UUID=` da raiz →
  `sudo findmnt --verify` → `sudo reboot` → reconectar → `findmnt -o UUID /` bate com
  `grep UUID /etc/fstab`
- bind: comandos do critério (`mount --bind`, `findmnt`, teste do arquivo refletido,
  `umount`)
- loop: comandos do critério (`truncate`, `mkfs.ext4`, `mount -o loop`, `findmnt`,
  `losetup -a`, `umount`)
- Limpeza final: `findmnt | grep -E 'lab-bind|lab-loop'` → vazio; `rm /tmp/fs.img`

## Evidências

- `df -hT /` + `df -i /` da VM (as duas leituras, em par)
- `lsblk -f` e `findmnt -o TARGET,SOURCE,FSTYPE,OPTIONS` completos
- `grep -E ' /proc | /dev/shm ' /proc/mounts` + `df -h /proc` mostrando que não é disco
- O par fstab: `grep UUID /etc/fstab` (declaração) e `findmnt -o UUID /` pós-reboot
  (realidade)
- `findmnt /mnt/lab-bind` com o bind montado + o teste do arquivo refletido na origem
- `findmnt /mnt/lab-loop` com SOURCE `/dev/loop0`, `losetup -a` e o `df -i` com `IUse%
  100` no filesystem de 100 MB (bytes livres, inodes no teto)
- `lsblk -o NAME,TYPE` (e `sudo vgs` se houver VG) — sem nenhum comando de escrita no
  disco
- Erro real é evidência válida, não defeito a esconder: `No space left on device` com
  `df -h` livre, e `findmnt` falhando **depois** do `umount` (é o resultado esperado)

## Limitações / notas

- Os ensaios rodam em filesystem descartável (`/tmp`, arquivo de 100 MB) com o snapshot
  `base` da T0-01 por perto; o disco raiz não é formatado nem reparticionado — mudança
  de partição real só no estágio AWS
- `df`/`findmnt` numa VM com escritor ativo (Docker, Prometheus) mudam entre uma leitura
  e outra: ler sempre em par e anotar a hora; medir crescimento ao longo do tempo é
  assunto da T3-01
- tmpfs some no reboot (é a prova de que vive em RAM): não é lugar para dado — quem põe
  estado ou log em tmpfs perde tudo no reboot
- fstab é o erro de maior custo desta issue: toda linha nova passa por backup e
  `findmnt --verify` antes do reboot; se o boot travar, a saída é `mount -o remount,rw /`
  no shell de emergência ou live ISO montando a raiz em `/mnt` — ensaiar a leitura, nunca
  a destruição
- LVM fica na fronteira de propósito: esta issue entrega **identificação e vocabulário**
  (PV/VG/LV, crescer sem reparticionar); executar resize é Área 2 / estágio futuro sobre
  EBS, e RAID/ZFS/btrfs seguem declarados como dívida do backlog
