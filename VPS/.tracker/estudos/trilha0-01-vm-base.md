# Estudo — KVM, qcow2 e as camadas por trás da `lab-vm`

> Material de estudo da Trilha 0. Acompanha a Issue 01 (VM Ubuntu Server em KVM com snapshot
> de base), mas não é o contrato daquela Issue: aqui é para entender, lá é para provar.

## Hipervisor "tipo 1" ou "tipo 2"? A pergunta que o KVM deixa furada

A classificação clássica existe para responder uma coisa: **entre o seu programa e o hardware
tem um sistema operacional inteiro ou não**.

- **Tipo 1 (bare-metal):** o hipervisor roda direto no ferro — ESXi, Hyper-V, Xen. Sem SO
  host no meio, menos camada para errar e para travar.
- **Tipo 2 (hosted):** o hipervisor é um programa dentro de um SO — VirtualBox, VMware
  Workstation. Confortável de instalar, paga uma camada de desempenho e de falha.

O KVM não cabe direto nos dois rótulos, e é justamente isso que importa entender: o KVM **não
é um hipervisor que você abre**. É um módulo do kernel Linux (`kvm_intel`/`kvm_amd`) que
transforma o próprio Linux num hipervisor — ele expõe `/dev/kvm` e passa a executar as
instruções da VM direto na CPU, com virtualização de hardware (VT-x/AMD-V). O que você chama
de "VM no KVM" é, na prática, **KVM (núcleo, aceleração) + QEMU (user space, emulação de
periféricos)** rodando juntos.

Causa e efeito, no dia a dia do lab:

- Quando o host tem `/dev/kvm` acessível, o QEMU abre esse arquivo e a VM executa quase no
  desempenho nativo; **quando não tem** (BIOS com virtualização desligada, ou o usuário fora
  dos grupos certos), o QEMU cai em emulação pura e a VM arrasta — e a instalação do Ubuntu
  vira castigo.
- Quando `ls /dev/kvm` não existe, nada do lab começa: é a pré-condição que a Issue 01 assume
  como pronta, não como entrega.

No host do lab, verificando isso de verdade:

```bash
ls -l /dev/kvm                          # existe? o kernel expõe o KVM
lsmod | grep kvm                        # kvm_intel ou kvm_amd carregado
groups | grep libvirt                   # usuário do host pode falar com o daemon
virsh capabilities | grep -i kvm          # o libvirt confirma que pode usar o KVM
```

**Fronteira:** instalar KVM no host é pré-condição desta Issue 01 (faz-se uma vez, fora da
VM); tudo que se aplica *dentro* da máquina — SSH, firewall, serviços — é das Issues 02, 03
e 04.

## qcow2 e copy-on-write: por que o disco não ocupa 20 GB nem o snapshot leva 20 GB

`raw` é um arquivo de disco cru: os bytes estão exatamente onde a VM escreveu. `qcow2`
(QEMU copy-on-write) é um arquivo com **cabeçalho próprio, tabela de offsets e — o ponto
chave — suporte a backing file**: um disco que pode dizer "o setor que você quer ler ainda
não foi escrito por mim, olhe lá embaixo".

O mecanismo de escrita é a causa de tudo que vem depois:

1. A VM escreve um setor pela primeira vez;
2. o QEMU lê esse setor do backing file (imagem base, estado anterior) e grava a cópia
   atualizada no qcow2 de cima;
3. daí em diante, a leitura desse setor pega o valor de cima.

Ou seja: **escrever agora significa copiar só o que mudou, nunca o disco inteiro**. Consequências
diretas, todas visíveis no lab:

- Um qcow2 recém-criado ocupa quase nada, mesmo com "virtual size" de 20 GB — o tamanho cheio
  só acontece se a VM escrever 20 GB de verdade.
- Um snapshot de VM desligada custa milissegundos e poucos KB, porque não se copia disco:
  cria-se um **novo qcow2 por cima, com backing file apontando para o anterior** (o próprio
  formato do snapshot é esse overlay).
- `raw` não tem tabela nem backing file — trocar o disco do lab por raw seria comprar
  desempenho de disco fino e pagar perdendo snapshot barato.

Conferindo no host:

```bash
virsh domblklist lab-vm                          # qual arquivo é o disco da VM
qemu-img info ~/.cache/libvirt/images/lab-vm.qcow2
#   > virtual size: 20 GiB        ← o que a VM acha que tem
#   > disk size: 6.2 MiB          ← o que o host realmente ocupa agora
qemu-img info -U /caminho/snapshot.qcow2 | grep backing
#   > backing file: ...lab-vm.qcow2   ← a prova do copy-on-write
du -h ~/.cache/libvirt/images/lab-vm.qcow2       # quanto de disco isso custa de fato
```

**Fronteira:** a Issue 01 escolhe qcow2 e discarta ≥ 20 GB *virtuais* — formatar disco,
LVM e particionamento dentro da VM são assunto dela só até o padrão do instalador; mexer em
`qemu-img convert` (reconverter o formato de um disco vivo) é experimento para depois, quando
houver snapshot para não temer.

## Snapshot, backup e clone: três palavras que se confundem caro

As três parecem "guardar o estado da VM", e cada uma falha em um lugar diferente:

| | Snapshot | Backup | Clone |
|---|---|---|---|
| O que é | ponto de retorno **dentro** da mesma VM | cópia dos dados **para fora** da origem | VM nova e independente a partir de uma imagem |
| Onde mora | mesmo disco do host, colado ao qcow2 | outro disco/mídia, fora da VM | outro arquivo + outro domínio libvirt |
| Custo | instantâneo (overlay de KB) | tempo de cópia + espaço real | o disco da nova VM, crescendo a partir do zero |
| Morre quando | o qcow2/overlay morre | só com a perda de todas as cópias | nunca afeta a original |
| No lab | Issue 01 | Issue 05 | não é destaque de nenhuma Trilha 0 |

O revert é o momento em que a diferença vira concreto. Quando você roda
`virsh snapshot-revert lab-vm base`, o libvirt manda o QEMU trocar a referência do disco para
o estado gravado no snapshot e reiniciar a VM — por isso o arquivo criado em `/tmp/marca`
**some**: ele nunca esteve no snapshot, só nos deltas escritos depois dele. E é a mesma
mecânica que explica o limite: qcow2 e snapshots moram no mesmo filesystem do host, então
**quando o disco do host morre, snapshot e original morrem juntos** — snapshot protege contra
erro de configuração na mesma máquina, não contra perda física.

O ciclo completo, como a Issue 01 usa:

```bash
# com a VM limpa e desligada (ou em quiesce), pelo virt-manager ou pela CLI:
virsh snapshot-create-as lab-vm base "estado limpo pos-instalacao"
virsh snapshot-list lab-vm                    # base aparece com data e tamanho
# ...experimento que dá errado...
virsh snapshot-revert lab-vm base             # volta ao estado marcado
virsh snapshot-info lab-vm                    # qual snapshot está ativo agora
```

A tentação clássica é tratar snapshot como "backup grátis" porque é rápido e sai de graça — e
descobrir, só no dia do disco morto, que o backup nunca existiu. É por isso que a Issue 05
cobra cópia para fora da VM **com restore comprovado**: cópia que ninguém restaurou é só
ocupação de disco.

**Fronteira:** snapshot de base é entrega da Issue 01 (e é a rede de segurança de todas as
experiências seguintes); backup com timer e restore drill é a Issue 05; clone de VM não é
critério de nenhuma das duas — quem quer "uma segunda máquina" está em território de múltiplas
VMs, fora do escopo desta trilha inicial.

## KVM → QEMU → libvirt → virt-manager/virsh: quem faz o quê quando você clica em Start

Quem está começando instala o virt-manager e conclui que "o virt-manager é o hipervisor". Não
é — são quatro camadas, e saber qual é qual é o que evita achar que a GUI quebrou quando o
problema está no daemon:

| Camada | Papel | Se sumir |
|---|---|---|
| **KVM** (`/dev/kvm`) | executa as instruções da VM na CPU (aceleração) | QEMU cai em emulação e a VM engasga |
| **QEMU** (`qemu-system-x86_64`) | monta a máquina: placa-mãe, disk controller, NIC, vídeo; usa o KVM quando pode | não existe VM sem ele — é quem fala com o hardware |
| **libvirt** (`libvirtd`) | daemon que guarda a definição da VM em XML e traduz comandos em linha de comando QEMU | você escreveria a linha do qemu na mão, sem estado, sem padrão |
| **virsh / virt-manager** | dois **clientes** do mesmo daemon: CLI e GUI | nada muda no mundo; é só uma forma diferente de pedir |

O fluxo de um "Start" no virt-manager: a GUI manda o pedido ao `libvirtd` → o daemon monta a
linha do `qemu-system-x86_64` a partir do XML da VM → o QEMU abre `/dev/kvm` e a máquina sobe.
Daí dois efeitos que explicam o comportamento do lab:

- **`virsh` e virt-manager sempre mostram o mesmo estado** — são dois clientes do mesmo
  daemon; se divergem, o cliente está desatualizado, não a VM.
- **A verdade da VM é o XML**, não a tela: o que a GUI sabe exibir é subconjunto do que o
  libvirt sabe fazer.

Enxergando as camadas no host:

```bash
systemctl status libvirtd                    # a camada do meio está de pé?
virsh list --all                             # pedido via CLI...
virsh dominfo lab-vm                         # ...mesmo domínio que a GUI lista
virsh dumpxml lab-vm | head -30              # a definição que o libvirt guarda e reescreve
ps aux | grep 'qemu-system' | head -3        # o QEMU de verdade, com -enable-kvm na linha
```

**Fronteira:** a Issue 01 cria a VM **no virt-manager** (é a GUI que orienta a instalação) e
usa **virsh** para ler estado e snapshots — as duas ferramentas, mesmo motor. Editar XML na
mão ou automatizar por `virt-install` é poder do libvirt que se conquista depois; as Issues
02–04 mexem no sistema dentro da VM, não na definição da máquina.

## A rede NAT do libvirt: por que o IP da `lab-vm` não é um endereço fixo

Ao criar a VM, o libvirt conecta a NIC a `network=default` — e essa rede é **NAT**: o host
cria um bridge (`virbr0`, tipicamente `192.168.122.1/24`), roda um `dnsmasq` que faz DHCP para
as VMs e encaminha os pacotes delas para fora com o IP do host como máscara. A VM fala com o
mundo, mas o mundo não fala com ela diretamente — e o IP dela é **emprestado por DHCP**, não
gravado em lugar nenhum.

Causa e efeito que derrubam o iniciante:

- Quando a VM sobe, ela pede um endereço ao `dnsmasq` e recebe um lease; o IP que você anotou
  é aquele. Enquanto a rede `default` e o lease existem, ele se mantém.
- Quando a rede é destruída e recriada (`virsh net-destroy default` + recriar), a tabela de
  leases some junto com o `dnsmasq` — a VM, ao renovar ou reiniciar, recebe **outro IP**,
  porque endereço DHCP é concessão, não identidade.
- Quando outro appliance do host entrar na mesma rede antes da `lab-vm`, ele pode levar o IP
  que você tinha anotado — DHCP não sabe que aquele era "da VM do lab".

Lendo a rede no host:

```bash
virsh net-list --all                      # a rede default está ativa?
ip -br addr show virbr0                    # 192.168.122.1/24 → este é o HOST
virsh net-dhcp-leases default              # quem tem qual IP agora (MAC + lease)
virsh dumpxml lab-vm | grep -A3 interface  # a NIC da VM aponta para network=default
```

Esse IP é a porta de entrada de toda a Trilha 0: `ssh lab@<ip>` da Issue 02 em diante. Daí a
regra prática da Issue 01 — **anotar o IP** (e reconhecer que ele pode mudar); quem quiser
estabilidade usa reserva DHCP por MAC na rede `default`. Trocar para bridge ou rede host-only,
onde a VM ganha IP "de verdade" da rede física, é outro desenho de rede — fora desta trilha.

**Fronteira:** a Issue 01 entrega a VM na NAT padrão com o IP anotado; a Issue 02 fixa o
acesso por **chave**, que sobrevive a IP mudando; firewall (o que entra e o que não entra,
independente de NAT) é a Issue 03; bridge/múltiplas VMs é Trilha 1+ e estágio AWS.

## Perguntas para o modo teach-anything

- "Me ensina hipervisor tipo 1 vs 2 e onde o KVM se encaixa, usando `ls /dev/kvm` e
  `virsh capabilities` do host do lab"
- "Me ensina qcow2 e copy-on-write com `qemu-img info` e o backing file do disco da `lab-vm`"
- "Me ensina a diferença entre snapshot, backup e clone com `virsh snapshot-list` e
  `virsh snapshot-revert` da `lab-vm`"
- "Me ensina as camadas KVM → QEMU → libvirt → virt-manager/virsh seguindo o que acontece
  quando o Start é clicado no virt-manager"
- "Me ensina a rede NAT do libvirt com `virsh net-dhcp-leases default` e por que o IP da
  `lab-vm` pode mudar"
