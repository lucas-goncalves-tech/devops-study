# Estudo — SSH só por chave: por que a senha sai de cena

> Material de estudo da Trilha 0. Acompanha a Issue 02 (ssh-key-only), mas não é o contrato
> daquela Issue: aqui é para entender, lá é para provar.

## O que o par de chaves guarda de verdade

Quem só usou senha acha que chave SSH é "senha comprida guardada em arquivo". Não é: a
chave privada não é um segredo cifrado esperando ser lido — é uma **semente de 32 bytes da
qual tudo o mais é derivado**, e o que o protocolo faz com ela é **assinar**, nunca
transmitir. Essa distinção decide por que dá pra confiar.

- **Pública (`id_ed25519.pub`)**: um ponto numa curva elíptica, 32 bytes, sem nenhum
  segredo. Você pode colar em `~/.ssh/authorized_keys` da VM, mandar por e-mail, commitar —
  ela só serve para **verificar** assinaturas. Quem a tem não consegue produzir uma.
- **Privada (`id_ed25519`)**: 32 bytes de semente, de onde saem a chave e o nonce de
  assinatura. Quem a tem, autentica. Por isso ela nunca sai do seu host.

O login funciona assim, causa → efeito:

1. o servidor manda um desafio aleatório (32 bytes) para o cliente;
2. o cliente assina o desafio com a privada — `ed25519_assinatura(desafio, chave_privada)`;
3. o servidor verifica com a pública que está no `authorized_keys`:
   `verificar(desafio, assinatura, chave_publica)` → true/false.

A "matemática só o suficiente": a assinatura só pode ser gerada por quem conhece a privada,
e verificar é barato (uma multiplicação de ponto na curva). Assimétrico significa exatamente
isso: **verificar é fácil, produzir é impossível sem a privada** — o oposto de uma senha,
onde o servidor precisa guardar o próprio segredo (o hash) pra poder conferir.

No lab, a evidência de que isso aconteceu é verbosa:

```bash
ssh -v lab@<ip>          # "Offering public key" → "Accepted publickey"
cat ~/.ssh/id_ed25519.pub | ssh lab@<ip> 'cat >> ~/.ssh/authorized_keys'
```

**Fronteira:** o que esta Issue instala é o par e a linha em `authorized_keys` do usuário
`lab`. Descobrir o algoritmo a fundo (curvas, ECDSA vs EdDSA), gerar chaves de produção,
bastion e certificados são do estágio AWS; firewall que fecha a porta 22 é a Issue 03.

## A senha é o gargalo porque o espaço dela é humano; o da chave não

Toda força bruta é a mesma aposta: "eu consigo testar mais entradas do que o espaço de
possibilidades permite". A diferença entre senha e chave é o tamanho do espaço — e ele não
é "um pouco maior", é de outra ordem de grandeza.

| | Senha | Chave ed25519 |
|---|---|---|
| Espaço | humano: dicionário + teclado (~2^30–2^40) | 2^256 (o tamanho da semente) |
| Quem escolhe | a pessoa, sob pressão, reutiliza | o `ssh-keygen`, aleatório |
| Tentativa por pacote | 1 senha correta/errada, servidor faz PAM + hash | 1 verificação de assinatura |
| Roubo do servidor | vaza o hash → cracker **offline** sem limite | vaza a pública → inútil |

Causa e efeito no ataque real: um botnet varre a porta 22 e manda `root:123456`. Com
`PasswordAuthentication yes`, cada tentativa é uma chance — 1000/s por IP, milhares de IPs,
e uma senha fraca cai em horas. Com chave, **a mesma varredura é matemática perdida**: pra
adivinhar uma semente de 256 bits a 1000 tentativas por segundo levariam mais anos do que
o universo tem. E como o desafio é aleatório a cada conexão, não existe lista pré-computada
de "assinaturas válidas" — atacante que roube o `authorized_keys` da VM ganha nada.

Outro efeito que quase ninguém vê: recusar senha não apaga a senha da conta. O `lab` ainda
tem senha local e o `sudo` continua pedindo — você desligou o **método de autenticação do
sshd**, não o usuário. Confira:

```bash
grep -E '^(PasswordAuthentication|PermitRootLogin)' /etc/ssh/sshd_config   # no / no
ssh -o PreferredAuthentications=password -o PubkeyAuthentication=no lab@<ip> # Permission denied (publickey)
journalctl -u ssh -n 20                                                     # as tentativas recusadas
```

**Fronteira:** a Issue 02 muda **quem** autentica (chave, não senha), não **de onde** se
pode falar — porta 22 segue aberta pra rede. `fail2ban`, TOTP e porta não-padrão (que
atacam a repetição da tentativa) são da Issue 03/04.

## Cada diretiva do sshd_config e o que ela deixa sem porta

O `sshd_config` é uma lista de alavancas; hardening é decidir quais derrubar e saber o que
cada uma protege (e o que não protege). Três diretivas fazem o trabalho desta Issue:

| Diretiva | Causa → efeito | Protege contra | Não protege contra |
|---|---|---|---|
| `PubkeyAuthentication yes` | liga o caminho da assinatura | — é a porta de entrada boa, explicitada pra não depender do default | chave roubada do seu host |
| `PasswordAuthentication no` | o sshd nem oferece senha como método | toda força bruta de senha, de qualquer conta, por qualquer IP | quem já tem chave válida |
| `PermitRootLogin no` | root é recusado **mesmo com chave** | root logado direto: audit trail (tudo passa por `lab` + `sudo`), comprometimento com raiz imediato | root dentro da VM via `sudo` |

A sutileza de `PermitRootLogin no`: ela não é o teste de senha — é o fim do "entra como
raiz e acabou". E há uma diferença de natureza que a Issue cobra distinguir: **recusar
senha é política do sshd** (uma linha de config), **não ter conta root** seria outra coisa
(`passwd -l root`, alteração da conta) — e aqui o root continua existindo, só é vetado no
SSH.

O caminho operacional no lab:

```bash
sudo cp /etc/ssh/sshd_config /etc/ssh/sshd_config.bak   # backup antes de mexer
sudoedit /etc/ssh/sshd_config                           # editar as três linhas
sudo sshd -t                                             # silêncio = config válida
sudo systemctl reload ssh                                # aplica sem derrubar sessões
systemctl is-active ssh                                 # active
```

`sshd -t` é só validação: lê o arquivo, confere sintaxe e semântica, **não toca no daemon
rodando**. `reload` (SIGHUP) faz o daemon reler e passa a valer para **novas** conexões — a
sessão autenticada antes continua viva, que é justamente o que queremos.

**Fronteira:** `sshd_config` muda autenticação, não rede. `ufw`, portas, `MaxAuthTries`,
`fail2ban` são Issue 03; `PubkeyAuthentication yes` é desta Issue, `Match`/contas
dedicadas e forced commands são assunto de infraestrutura mais adiante.

## Testar antes de trancar: a ordem que separa lab de apagão

O erro clássico é uma linha só: editar a config, reiniciar o sshd e descobrir que digitou
errado — sem sessão, sem porta, sem como voltar. A ordem operacional existe porque o
hardening tem uma propriedade perigosa: **quase todo erro dele te deixa fora, e o
sintoma só aparece na próxima conexão.**

A sequência segura, causa → efeito:

1. **Sessão de manutenção fica aberta** (o terminal onde você está logado na `lab-vm`).
   Ela sobrevive a `reload`, então qualquer erro de config ainda deixa uma porta de entrada.
2. **Backup + edição** (`sshd_config.bak` acima). Sem backup, o revert vira "adivinhar o que
   eu mudei".
3. **`sshd -t` antes de aplicar.** Erro de digitação morre aqui, com o daemon intacto:
   `sudo sshd -t` imprime nada quando a config é válida; se aparecer qualquer linha de
   erro, não aplique — corrija e rode de novo até o silêncio.
4. **`reload`, nunca `restart`, na primeira aplicação.** `restart` mata as sessões ativas
   (inclusive a de manutenção); `reload` preserva.
5. **Prova em sessão nova:** `ssh -o BatchMode=yes lab@<ip> 'echo ok'` → `ok`. Enquanto não
   houver `ok` numa sessão recém-criada, o hardening não está feito — sessão velha não
   prova nada.
6. **Só então** `sudo systemctl reboot` pra confirmar que a config persiste (arquivo, não
   runtime). Se o reboot não voltar, aí sim o `snapshot-revert` da Issue 01 é acionado —
   e ele é a **última** rede, não a primeira.

Outro modo de trancar a si mesmo, mais discreto: mexer no `authorized_keys`. Ele é uma
lista de identidade, e apagar a linha errada derruba o acesso tão eficientemente quanto
uma config errada — por isso o teste negativo é sempre feito **com a sessão de manutenção
viva**:

```bash
wc -l ~/.ssh/authorized_keys                    # a linha certa continua lá?
ssh -o BatchMode=yes lab@<ip> 'echo ok'         # recém-criada, sem prompt → ok
ssh root@<ip>                                   # Permission denied
ssh -o PreferredAuthentications=password -o PubkeyAuthentication=no lab@<ip>  # Permission denied
```

**Fronteira:** a rede de segurança **estrutural** (revert) é da Issue 01; o que esta Issue
entrega é o **procedimento** que a usa. E "sobrevive a reboot" é daqui — o `reload` vale
próximas conexões, o reboot prova que está no arquivo.

## ed25519 vs RSA, e a passphrase que protege o arquivo, não a máquina

A escolha da curta lista de opções do `ssh-keygen -t` tem motivo técnico, não modismo:

- **ed25519**: 32 bytes de pública, um único conjunto de parâmetros (nada de "qual curva
  escolher"), assinatura rápida e segurança bem entendida. É o que a Issue pede, e é o
  default moderno.
- **RSA**: precisa de 3072–4096 bits pra equivaler ed25519 (`ssh-keygen -t rsa -b 4096`),
  arquivo maior, e um dia foi escolha obrigatória por compatibilidade com ferramentas
  velhas — hoje só importa se o outro lado for legado.

Gerar no lab:

```bash
ssh-keygen -t ed25519 -C "lab@$(whoami)@host"   # ~/.ssh/id_ed25519 (+ .pub)
ssh-add -l                                      # o que o agent segura agora
```

Aí vem a segunda decisão, que é sobre **propriedade, não sobre algoritmo**: a privada sem
passphrase é um arquivo que vale quanto a VM vale. Alguém que copie `~/.ssh/id_ed25519`
(copia de backup, home vazada, laptop roubado) autentica por você — não existe "número
errado" pra digitar, porque não existe número nenhum. Com passphrase, o arquivo passa a ser
cifrado: `ssh-keygen -p` pra adicionar depois, e a privada só sai do disco a cada uso
desbloqueada.

O **agent** é o que evita digitar a passphrase dez vezes por dia:

```bash
eval "$(ssh-agent bash)"        # sobe o agent no SEU host — nunca no servidor
ssh-add ~/.ssh/id_ed25519       # pergunta a passphrase uma vez, guarda em memória
ssh-add -l                      # listado; ssh-add -D apaga tudo
ssh lab@<ip>                    # usa a chave guardada, sem prompt
```

Efeito colateral que precisa de nome: **`ForwardAgent yes` em servidor de terceiros
entrega ao dono da máquina a capacidade de assinar com a sua chave** — é a forma clássica
de transformar uma proteção em furo. Limite o tempo (`ssh-add -t 8h`) e nunca encaminhe o
agent pra máquina que você não administra.

**Fronteira:** a Issue 02 exige o par `ed25519` instalado e funcionando — passphrase +
agent é explicitamente o **próximo degrau** (nota da Issue), e a chave sem passphrase já
resolve a força bruta desta entrega. Passphrase protege contra roubo de arquivo; proteger
a máquina inteira (keystroke logger, host comprometido) não é escopo de SSH nenhum.

## Como iniciar o modo teach-anything

- "Me ensina o par de chaves SSH (pública vs privada, assinatura com desafio) usando os
  arquivos `~/.ssh/id_ed25519` e `authorized_keys` deste lab"
- "Me ensina por que força bruta de senha morre e de chave não, com os comandos de teste
  negativo da VM `lab-vm`"
- "Me ensina cada diretiva do `/etc/ssh/sshd_config` desta Issue e o que ela deixa de
  proteger ao ser desligada"
- "Me ensina a ordem 'testa antes de trancar' — `sshd -t`, `reload` e sessão de manutenção
  — rodando na `lab-vm` com o snapshot `base` como rede de segurança"
- "Me ensina ed25519 vs RSA e passphrase + `ssh-add` no host, sem usar `ForwardAgent`"
