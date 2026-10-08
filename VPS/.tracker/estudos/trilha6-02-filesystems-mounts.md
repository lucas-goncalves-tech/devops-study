# Estudo — filesystems: o que existe, o que monta e o que é LVM

> Material de estudo da Trilha 6. Acompanha a Issue 02 (leitura de disco, mounts e noções
> de LVM), mas não é o contrato daquela Issue: aqui é para entender, lá é para provar.

## O disco está "cheio" com metade do espaço livre: `df -h` e `df -i` medem coisas diferentes

Quando alguém diz "o servidor caiu porque o disco encheu", quase nunca é o espaço em
bytes que acabou. O filesystem guarda **duas** contagens separadas — blocos de dados e
**inodes** — e o sistema morre por qualquer uma das duas, com o mesmo erro
(`No space left on device`) e diagnósticos completamente diferentes.

- **Por que importa:** esgotar inodes com 60% de espaço livre é o clássico de ambiente
  com muitos arquivos pequenos — mail spool, caches, `node_modules`, camadas de build,
  e aqui no lab o próprio Docker e a série do Prometheus. Quem só sabe `df -h` vê
  "espaço sobrando" e fica sem resposta; o problema não é apagar um arquivo grande, é
  que **cada arquivo novo consome 1 inode** e a tabela de inodes do filesystem acabou.
- **Mecanismo:** o `mkfs` reserva os inodes **na hora da formatação**, em percentual
  fixo do tamanho (padrão ext4: 1 inode a cada 16 KB). Depois de formatado, não existe
  `mkfs` de novo sem reformatar. `df -h` conta blocos livres; `df -i` conta inodes
  livres — e os dois têm colunas `Use%` independentes:

  | Comando | O que mede | Acabou quando | O que fazer |
  |---|---|---|---|
  | `df -h` | bytes usados/livres | `Use%` ~100% | apagar/grow do filesystem |
  | `df -i` | inodes usados/livres | `IUse%` ~100% | apagar **arquivos pequenos**; o `Use%` está mentindo |

  O inverso também existe: filesystem enorme com poucos arquivos tem inodes ociosos —
  `df -i` mostra 1% e tudo bem, não é desperdício.
- **Exemplo no lab:** as duas leituras na `lab-vm`, sempre em par (é o primeiro critério
  da Issue 02):

  ```bash
  ssh lab@<ip> 'df -hT /; df -i /'
  # Filesystem     Type  Size  Used Avail Use% Mounted on   ← bytes (ext4, /dev/vda...)
  # Filesystem     Inodes IFree IUse% Use% Mounted on      ← inodes do MESMO filesystem

  # achando quem come inodes (arquivos pequenos por diretório):
  sudo du -i --max-depth=1 -x /var 2>/dev/null | sort -n | tail
  # fallback que não depende de coreutils novo:
  find /var/lib -xdev -type f 2>/dev/null | wc -l
  ```

  A prova didática de esgotamento vem do loop device do tópico 4: um `mkfs.ext4` com
  `-N 5000` (5 mil inodes para um filesystem de 100 MB) enche de arquivos de 0 byte e
  `df -i` vai a 100% com o `df -h` quase vazio — só que no seu filesystem descartável,
  nunca no disco real da VM.
- **Fronteira entre Issues:** aqui entra **diagnóstico** (`df`/`du`/`find`). O que fazer
  com o disco cheio é de outro lugar: rotação/centralização de log é a T3-01, backup é a
  T0-05, e **crescer** o disco de verdade (partição/LVM/EBS) é o tópico 5 daqui e o
  estágio AWS.

## A árvore de montagem é convenção, não geografia do disco

`/proc` "ocupa" 0 bytes, `/dev/shm` some quando a máquina reinicia, e o mesmo disco
aparece em dois caminhos diferentes. Nada disso é bug: a árvore `/` é uma construção do
kernel, e cada diretório de montagem é só um **encaixe** onde um filesystem foi pendurado.

- **Por que importa:** sem essa ideia, todo `df` vira adivinhação. Gravar estado onde
  não se espera (um diretório que é `tmpfs` = memória RAM) significa perder o dado no
  reboot sem nenhum aviso. E a pergunta "onde esse arquivo mora de verdade?" não se
  responde olhando o caminho — se responde perguntando ao kernel qual filesystem está
  naquele ponto.
- **Mecanismo:** o kernel mantém uma tabela de mounts. `/proc/mounts` (aliás de
  `/proc/self/mounts`) é essa tabela crua, um arquivo **do filesystem `proc`**, que é
  uma API de leitura do kernel — `ps` lê `/proc/<pid>` e "inventa" a lista de processos;
  nada ali é arquivo de disco. `findmnt` é o `ls` dessa tabela: formata em árvore, com
  SOURCE e opções. Os tipos que não vêm de disco:

  | Ponto | FSTYPE | De onde vem | Ocupa disco? | Sobrevive ao reboot? |
  |---|---|---|---|---|
  | `/proc` | `proc` | API do kernel | não (nem RAM) | renasce a cada boot |
  | `/dev` | `devtmpfs` | kernel cria os nodes | não | renasce a cada boot |
  | `/dev/shm`, `/run/...` | `tmpfs` | RAM | não | **não** — some |
  | `/` (nesta VM) | `ext4` | `/dev/vda...` | sim | sim (via fstab) |

- **Exemplo no lab:** ler a árvore da `lab-vm` por três ângulos que batem entre si:

  ```bash
  ssh lab@<ip>
  findmnt                              # árvore com └/├
  findmnt -o TARGET,SOURCE,FSTYPE,OPTIONS /
  findmnt -t tmpfs,proc,devtmpfs       # só os que não ocupam disco
  grep ' /proc ' /proc/mounts          # linha crua: proc /proc proc rw,nosuid,...
  df -hT /proc                         # Size 0: não existe "espaço" a encher
  ```

  Um exercício de trilha de acreditar no olho: `findmnt` num container —
  `docker exec <container> findmnt /` mostra o `overlay` (camadas da imagem) no topo da
  árvore **daquele** namespace, e o mesmo acontece na host com os volumes.
- **Fronteira entre Issues:** este tópico entrega o **mapa** (o que cada ponto da árvore
  é). Duas peças do mapa ficam para os próximos: **onde a árvore é declarada para o
  boot** é o tópico 3 (fstab), e **como encaixar filesystems de propósito** é o tópico 4
  (bind e loop). O `overlay`/union fs de imagem de container é Trilha 1 (T1-01).

## `fstab`: declarar para sobreviver ao reboot

`mount -o remount,rw /dados` funciona até a próxima reiniciada — e só depois dela você
descobre que ninguém declarou aquele ponto. `/etc/fstab` é a lista de mounts que o boot
**promete** recriar; e é também o único lugar deste lab onde um erro de digitação
trava a máquina inteira.

- **Por que importa:** mount manual morre no reboot. Se um serviço (banco, Prometheus,
  proxy) depende de um ponto montado à mão, ele não volta sozinho — e um `fstab` com
  UUID errado ou ponto inexistente faz o `local-fs.target` falhar e o boot parar em
  **emergency mode**. Custa meia hora terreno: snapshot `base` da T0-01 ou live ISO.
- **Mecanismo:** linha de 6 campos — `disco ponto tipo opções dump pass`:

  ```fstab
  UUID=3f2a...-... /mnt/dados ext4 defaults,noatime 0 2
  # 1: QUE filesystem (UUID = identidade do filesystem, não do /dev/sdX)
  # 3: ext4 — o tipo; um erro aqui também derruba o mount
  # 4: options — defaults, noauto (NÃO monta no boot), ro, uid=...
  # 6: pass — 2 = fsck no boot só se o disco for removível; 1/2 para / ; 0 = não checa
  ```

  Por que `UUID=` e não `/dev/vda1`: o nome do device **não é estável** (renumeração de
  disco, disco novo, boot de outro meio); o UUID é gravado no superbloco do filesystem
  e acompanha ele. `noauto` é o oposto útil: existe a linha, mas só monta quando você
  pedir (`mount /mnt/dados`) — para mídia que pode não estar lá, e é o que evita o boot
  esperando um disco ausente.

  O caminho do boot travado: systemd monta o `fstab` → linha falha → target dependente
  falha → shell de emergência com `/` montado **read-only**. A saída, nesse shell:

  ```bash
  mount -o remount,rw /        # libera escrita na raiz já montada
  nano /etc/fstab              # comenta/corrige a linha problemática
  exit                         # o boot continua de onde parou
  ```

  E o caminho de live ISO (sem shell de emergência): boot do ISO → monta a raiz real em
  `/mnt` → edita `/mnt/etc/fstab` → reboot. Mesma ideia: você conserta o **arquivo**, não
  a memória do boot.
- **Exemplo no lab:** declarar um ponto na `lab-vm` e provar que ele volta:

  ```bash
  ssh lab@<ip>
  grep -v '^\s*#' /etc/fstab                 # o que o instalador da T0-01 já declara
  grep UUID /etc/fstab                       # prova de que a raiz já é declarada por UUID
  sudo findmnt --verify                      # valida o fstab SEM arriscar reboot
  # adicionar a linha do /mnt/dados com o UUID de verdade (blkid), depois:
  sudo mount /mnt/dados                      # monta PELA linha do fstab
  findmnt /mnt/dados                         # SOURCE = /dev/... com o UUID declarado
  sudo umount /mnt/dados
  ```

  O teste de falha sem drama: uma linha com UUID inventado + `sudo mount -a` → o mount
  falha ali, na hora, com a VM viva. O mesmo `mount -a` no boot é o que levaria a
  emergency — e se quiser ver o boot travado de verdade, o snapshot `base` da T0-01
  devolve a VM em segundos (é para isso que ele existe).
- **Fronteira entre Issues:** fstab é **desta** Issue — declarar + `findmnt --verify` +
  o par de comandos que prova que o reboot manteve o mount. O `mount -o remount,rw` de
  recuperação é ferramenta, não entrega: o hardening de boot é assunto de Trilha 4
  (T4-01), e fstab de volume de produção junto com grow de disco é estágio AWS.

## Montar sem reparticionar: bind mount e loop device

As duas técnicas que esta Issue pratica com as mãos — e uma delas você já usa sem saber
todo dia: o `volumes:` do `compose.yaml` **é** bind mount.

- **Por que importa:** bind mount é o mecanismo por baixo de "config versionada no repo
  aparece mágica dentro do container" — a T3-02 monta `monitoring/prometheus.yml` no
  Prometheus e o `compose.yaml` da Trilha 1 monta
  `./monitoring/grafana/provisioning:/etc/grafana/provisioning:ro` exatamente assim.
  Saber disso muda a pergunta de "por que o container enxerga o arquivo?" para "qual
  diretório da host está encaixado em qual ponto do container?" — metade dos bugs de
  volume somem aí. O loop device, por sua vez, é a forma segura de treinar
  `mkfs`/`mount` **sem tocar no disco real** da VM.
- **Mecanismo:** `mount --bind origem destino` cria uma **segunda entrada** na árvore
  apontando para o mesmo inode. Não copia, não sincroniza, não tem "espelho": escrever
  por um lado é ver pelo outro, e se a origem for apagada o destino mostra o que restou.
  As duas entradas somem juntas no `umount`. É a mesma syscall que o Docker chama por
  baixo (bind = diretório da host; volume nomeado = diretório em
  `/var/lib/docker/volumes/<nome>` encaixado no container).

  ```text
  árvore do container            árvore da host
  /etc/prometheus/  ──bind──►    <repo>/monitoring/prometheus.yml
  /var/lib/prometheus ──vol──►   /var/lib/docker/volumes/prometheus_data/_data
  ```

  O loop device vai no sentido oposto: **um arquivo fingindo ser disco**. `mount -o loop`
  pede ao kernel um `/dev/loopN` livre (`losetup` por baixo), `mkfs` assina aquele
  arquivo como filesystem e o mount encaixa ele na árvore como se fosse `/dev/sda`. O
  tamanho do "disco" é o tamanho do arquivo — e `truncate` cria um arquivo **sparse**
  (100 MB declarados, poucos KB ocupados até alguém escrever).
- **Exemplo no lab:** bind primeiro, à mão, na `lab-vm`:

  ```bash
  ssh lab@<ip>
  sudo mount --bind /home/lab /mnt/lab-bind
  findmnt /mnt/lab-bind                    # SOURCE = /home/lab, mesmo FSTYPE
  touch /mnt/lab-bind/marca && ls /home/lab/marca   # os dois caminhos, o mesmo arquivo
  sudo umount /mnt/lab-bind && findmnt /mnt/lab-bind   # não encontrado
  ```

  Depois o loop device completo — montar, provar, desmontar:

  ```bash
  truncate -s 100M /tmp/fs.img             # arquivo de 100M (sparse)
  sudo mkfs.ext4 -F /tmp/fs.img            # assina filesystem no arquivo
  sudo mkdir -p /mnt/lab-loop
  sudo mount -o loop /tmp/fs.img /mnt/lab-loop
  losetup -a                               # /dev/loop0 => /tmp/fs.img (o truque, visível)
  findmnt /mnt/lab-loop                    # SOURCE /dev/loop0
  df -hT /mnt/lab-loop                     # "disco" de 100M que não é partição nenhuma
  sudo umount /mnt/lab-loop                # desmonta e LIBERA o loop
  losetup -a                               # sem a linha do fs.img
  rm /tmp/fs.img                           # acabou o experimento, nada no disco real mudou
  ```

  O mesmo bind do outro lado da fronteira, na host com Docker — a prova de que o
  `volumes:` do compose é a técnica que você acabou de praticar:

  ```bash
  docker inspect <container> --format '{{range .Mounts}}{{.Source}} -> {{.Destination}}{{"\n"}}{{end}}'
  # /repo/monitoring/prometheus.yml -> /etc/prometheus/prometheus.yml   ← bind de arquivo
  # /var/lib/docker/volumes/<nome>/_data -> /var/lib/prometheus          ← bind de volume
  ```

- **Fronteira entre Issues:** bind e loop são **desta** Issue: o ensaio com `findmnt`
  como prova. A **política** de volume é de Trilha 1 (T1-02 nomeia `pg_data` e ensina o
  `down -v`; T3-02 monta a série do Prometheus) — aqui você entende o mecanismo, lá se
  decide o que vive em volume. O que bind/loop **não** resolvem: o disco real encheu e
  não dá para `umount` — crescer de verdade é o tópico 5.

## LVM em uma página: PV, VG, LV e por que a T0-01 adiou

LVM é uma **camada de indireção** entre o disco e o filesystem. Em vez do `mkfs` falar
direto com `/dev/vda3`, ele fala com um **logical volume** (LV), que por baixo pode
esticar-se sobre vários discos, crescer, ou migrar sem desmontar. É a resposta natural
para "o disco encheu e eu não quero reparticionar".

- **Por que importa:** com partição fixa (o que a T0-01 instalou), o filesystem tem o
  tamanho do dia da instalação e encher é problema: reparticionar a partição do root é
  operação de risco, e em disco removível nem se faz. Na nuvem a história é pior: o EBS
  cresce, e sem LVM você não aproveita. Entender PV/VG/LV é o que permite ler qualquer
  `lsblk` de servidor sem achar que `ubuntu-vg` é um mistério — e decidir, com
  argumento, quando vale o setup.
- **Mecanismo:** três nomes, uma pirâmide:

  ```text
  /dev/vda3  /dev/vdb        ← discos/partições "de verdade"
      │ pvcreate                  ▼
    [PV]   [PV]               Physical Volumes: marcados, doados
      └─────┴── vgcreate ──►  [VG]  ubuntu-vg   piscina única de espaço
                                  │ lvcreate
                                  ▼
                              [LV]  ubuntu-lv   fatia da piscina = o que o mkfs vê
                                  │ mkfs.ext4 + mount
                                  ▼
                              / (ou /mnt/dados)
  ```

  O truque é a linha `vgcreate`: o espaço dos PVs vira **piscina única**, e o LV é uma
  fatia que pode ser **redefinida** — `lvextend -L +5G /dev/ubuntu-vg/ubuntu-lv` seguido
  de `resize2fs /` (ext4, online) cresce o filesystem **sem mexer em partição, sem
  parar serviço, sem reparticionar**. É o mesmo princípio que o EBS cresce na nuvem:
  anexar volume, esticar, `pvcreate`/`vgextend`/`lvextend` — a parte "cresce sem
  reparticionar" é exatamente o que a T0-01 declarou como não-assunto e esta Issue fecha
  como **conhecimento**.
- **Exemplo no lab:** aqui é **leitura, não setup** (a Issue 02 faz `pvs`/`vgs`/`lvs` no
  máximo; nenhum `pvcreate`/`lvextend`):

  ```bash
  ssh lab@<ip>
  lsblk -o NAME,TYPE,FSTYPE,MOUNTPOINT
  # descobrir o que a T0-01 realmente criou: o instalador do Ubuntu Server costuma
  # entregar LVM por padrão — se aparecer lvm / ubuntu--vg / ubuntu--lv, é ele;
  # se aparecer só part / ext4, o particionamento foi direto
  sudo pvs && sudo vgs && sudo lvs        # só faz sentido se o lsblk mostrar TYPE lvm
  sudo findmnt -o SOURCE /                # da onde o root monta: LV ou partição
  ```

  O cenário que motiva o LVM, discutido sem executar: root em 20 GB cheio aos 18 →
  `lvextend -L +10G` + `resize2fs` resolve em 30 segundos; sem LVM seria desmontar,
  reparticionar, arriscar. E o contraponto honesto: numa VM descartável de lab com
  snapshot `base`, o custo do setup completo (e do rollback de um `pvcreate` errado)
  não pagava — por isso a T0-01 adiou.
- **Fronteira entre Issues:** esta Issue entrega **vocabulário e leitura** (`lsblk`,
  `pvs`/`vgs`/`lvs` no estado atual). O **setup** — criar VG, esticar LV, redimensionar
  filesystem em volume em uso — está declaradamente fora de escopo e pertence ao estágio
  **AWS (Área 2)**, onde o volume que cresce é o EBS. RAID e ZFS/btrfs também não entram
  aqui (dívida declarada na Issue); backup do conteúdo do LV é a T0-05.

## Como iniciar o modo teach-anything

- "Me ensina `df -h` vs `df -i` usando o root da `lab-vm` e o caso do disco cheio com espaço livre"
- "Me ensina a árvore de montagem com `findmnt` e `/proc/mounts` da `lab-vm` — por que `/proc` não ocupa disco"
- "Me ensina `fstab` com UUID: declarar um mount, validar com `findmnt --verify` e sair do boot travado com `mount -o remount,rw /`"
- "Me ensina bind mount com `mount --bind` na VM e mostre o mesmo mecanismo nos volumes do `compose.yaml` e do Prometheus da T3-02"
- "Me ensina loop device montando o `fs.img` de 100 MB com `mkfs.ext4` e `mount -o loop` — e por que isso é seguro na VM"
- "Me ensina LVM (PV/VG/LV) lendo o `lsblk`/`lvs` da `lab-vm` e por que a T0-01 adiou o setup"
