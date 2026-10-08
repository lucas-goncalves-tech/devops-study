# Estudo — Hardening: medir a VM com régua e registrar o que a régua apontou

> Material de estudo da Trilha 4. Acompanha a Issue 01 (auditar e consertar), mas não é o
> contrato daquela Issue: aqui é para entender, lá é para provar.

## Lynis é termômetro, não certificado — e a entrega é o registro, não o número

Hardening sem medição é opinião: "deixei tudo fechado na Trilha 0" é uma frase que ninguém
consegue provar hoje, porque desde então seis containers e as bridges do Docker entraram na VM
sem ninguém re-olhar. O Lynis existe para transformar aquela frase em número e lista.

- **Por que importa:** o custo de ignorar a medição é que a superfície cresce invisível.
  Cada `docker compose up -d` pode publicar porta, criar interface `docker0`, instalar
  serviço — e o `ufw status` "continua o mesmo", dando falsa sensação de que nada mudou.
  Sem baseline, o "depois" não prova nada: quem não guarda o score de antes não pode
  mostrar que melhorou, nem que não piorou.
- **Mecanismo:** `lynis audit system` roda ~300 checks de configuração (kernel, rede,
  contas, permissões, serviços, log, hardening de binários) e devolve duas coisas:
  **warnings/suggestions** (achados individuais) e o **Hardening index** (0–100, média dos
  checks). A leitura correta é: o índice é a *média* — sobe com correções fáceis, e um 80
  convive com 10 warnings de severidade média. Score alto ≠ invulnerável: ele mede
  configuração conhecida, não ataque real; é termômetro, não certificado.

```bash
# baseline — ANTES de mexer em qualquer coisa (sem ela, o "depois" não vale)
lynis audit system | tee /tmp/lynis-before.log
grep "Hardening index" /tmp/lynis-before.log        # → o número que vira referência

# depois das correções — mesmo comando, mesmo filtro
lynis audit system | tee /tmp/lynis-after.log
grep "Hardening index" /tmp/lynis-after.log
```

- **Remediação × aceitação documentada:** os warnings caem em três destinos —
  **corrigir** (comando aplicado, ex.: sysctl do próximo tópico), **aceitar com
  justificativa** (o achado não cabe no lab: parâmetro de servidor físico, módulo que a VM
  não usa) e **ignorado** (não pode). "Aceito com justificativa" é segurança porque a
  decisão fica escrita e revisável em `docs/hardening.md` (achado → severidade → ação →
  comando → porquê); "ignorado" é o vazio que daqui a três meses ninguém sabe se foi
  decisão ou esquecimento. Por isso a Issue exige o registro e **não** score máximo: boa
  parte dos avisos do Lynis é inaplicável a um host de containers, e o resultado correto
  para elas é a justificativa, não o 100.
- **Exemplo no lab:** baseline da `lab-vm` guardado em `/tmp/lynis-before.log` com o
  `Hardening index` anotado no `docs/hardening.md`; cinco achados tratados — uns com
  comando, outros com "aceito: X porque Y" — e o `lynis-after.log` mostrando o índice ≥
  baseline.
- **Fronteira entre Issues:** CIS Benchmark completo com automação (Ansible) é estágio
  AWS, fora do lab; SELinux/AppArmor em modo enforcing customizado também — aqui basta
  registrar o estado atual. O scan de imagem e dependências é a **T4-03** (supply chain),
  secrets são **T4-02**; esta Issue audita o **host**.

## Escutar não é alcançável: `ss -tlnp` mente sozinho, ufw mente sozinho, o cruzamento é que fala

A pergunta de segurança nunca é "a porta está aberta?" e sim "quem consegue chegar nela?".
São duas respostas diferentes, e cada ferramenta sozinha entrega meia resposta.

- **Por que importa:** o erro clássico é achar que a VM está exposta porque `ss -tlnp`
  listou 10 portas (pânico desnecessário, fechar o que não precisa), ou — pior — achar que
  está fechada porque `ufw status` mostra só 22/80/443 (complacência perigosa, se algo
  escuta em loopback e é alcançável por dentro, ou se uma regra de ufw é bypassada por
  regra do Docker no iptables).
- **Mecanismo:** são três camadas, cada uma respondendo uma pergunta:

  | Camada | Comando | Pergunta que responde |
  |---|---|---|
  | Escuta | `ss -tlnp` | o processo está aceitando conexão? |
  | Endereço de escuta | `ss -tlnp` (coluna Local Address) | aceita de `127.0.0.1` ou de `0.0.0.0`/`::`? |
  | Alcançabilidade | `ufw status` | o pacote vindo de fora é permitido? |

  `127.0.0.1:9090` (Prometheus) escuta, mas só a própria VM chega — é o "exposto no
  loopback" da Trilha 1, usado de propósito. `0.0.0.0:8080` aceita de qualquer interface —
  aí quem decide se o pacote externo chega é o ufw. E o detalhe que faz `ss` "mentir": a
  coluna `Local Address` mostra **onde** escuta, nunca **se o firewall deixa passar** —
  `ss` enxerga o socket, ufw enxerga o pacote; um não consulta o outro.

```bash
sudo ss -tlnp                 # escuta: processo + endereço (127.0.0.1 vs 0.0.0.0/::)
docker ps --format '{{.Names}}\t{{.Ports}}'   # o que o Docker publicou na host
sudo ufw status numbered      # régua externa: deve seguir 22, 80, 443 — nada mais
```

- **Exemplo no lab:** cruzar as três telas da `lab-vm` e classificar cada linha de
  `ss -tlnp` — `127.0.0.1:8080` (app, loopback de propósito), `127.0.0.1:3000` (Grafana),
  `127.0.0.1:9090` (Prometheus), `0.0.0.0:80` e `:443` (proxy, o único serviço de cara
  pública, justificado pelas regras 80/443), `:::22` (SSH), bridges do Docker
  (`docker0`, tráfego interno entre containers). Qualquer `0.0.0.0` fora dessa lista é
  "surpresa" e precisa de justificativa no registro. Nota de leitura: o `ufw status`
  continua **inalterado** em 22/80/443 — auditar não vira "abrir porta", vira evidência de
  que não há outro caminho.
- **Fronteira entre Issues:** abrir 80/443 e publicar portas de serviço é da **T1-03/T1-04**
  (proxy e publicação); a política default deny é da **T0-03**; debugar *por que* uma
  conexão específica falha (DNS, NAT) não é desta Issue. Aqui o trabalho é **classificar o
  que já existe** — cada escuta explicada em `docs/hardening.md`.

## `sysctl -w` some no reboot — persistência é arquivo, não runtime

Os parâmetros de rede/kernel que a auditoria aponta (redirecionamento, rp_filter, cookies
de sessão) vivem na memória do kernel. Trocá-los no runtime é trivial; **sobreviver ao
próximo reboot** é a parte que separa quem corrigiu de quem acha que corrigiu.

- **Por que importa:** o custo de ignorar a persistência é silencioso e atrasado. Você
  roda o `sysctl -w`, re-audita, o Lynis sobe, tudo verde — e no primeiro reboot da VM
  (atualização de kernel, manutenção, corte de energia) o parâmetro volta ao valor
  default e o score cai de novo. Nada no dia a dia denuncia: sem re-auditar depois do
  reboot, o regresso é invisível.
- **Mecanismo:** dois caminhos, destinos diferentes —

  | Comando | Onde vale | Sobrevive a reboot? |
  |---|---|---|
  | `sysctl -w chave=valor` | memória do kernel, agora | **não** — runtime puro |
  | linha em `/etc/sysctl.d/*.conf` | lido na boot pelo `systemctl`/`sysctl --system` | **sim** |

  A ordem de carregamento é: arquivos de `/etc/sysctl.d/` (e `/etc/sysctl.conf`) são
  aplicados na inicialização, em ordem alfabética — por isso o nome `99-lab.conf`
  (depois dos defaults da distro, sobrescrevendo o que vier antes). O `-w` é para
  **testar** a mudança com a stack de pé antes de gravar; o arquivo é a correção de
  verdade.

```bash
# 1. testar em runtime (some no reboot — serve só para validar com a stack no ar)
sudo sysctl -w net.ipv4.conf.all.send_redirects=0
sudo sysctl -w net.ipv4.conf.all.rp_filter=1

# 2. persistir: o mesmo valor em arquivo (a correção que sobrevive)
sudo tee -a /etc/sysctl.d/99-lab.conf <<'EOF'
net.ipv4.conf.all.send_redirects = 0
net.ipv4.conf.all.rp_filter = 1
EOF

# 3. prova: recarregar do zero e conferir (equivale ao que a boot fará)
sudo sysctl --system
sysctl net.ipv4.conf.all.send_redirects net.ipv4.conf.all.rp_filter   # → 0 e 1
```

- **Exemplo no lab:** `docs/hardening.md` guarda **par** — o `sysctl -w` testado com os 6
  serviços healthy e o trecho de `/etc/sysctl.d/99-lab.conf` persistido. Rollback é
  simples justamente porque é arquivo: remover/reverter a linha em `99-lab.conf` +
  `sysctl --system` desfaz; quem só rodou o `-w` nem tem o que reverter — não sobrou nada.
  Teste obrigatório: stack de pé (`docker compose ps` → 6×healthy) depois de cada valor,
  porque rp_filter mal colocado e redirect desligado afetam tráfego, não só a nota do Lynis.
- **Fronteira entre Issues:** **quais** parâmetros corrigir vem da auditoria desta Issue (a
  auditoria justifica, não a lista decorada); timeouts de sessão/sudo são o mesmo formato
  de registro, se o Lynis apontar. Ajuste fino de rede para performance, kernel modules e
  tuning de produção não é desta Issue — e automação disso em IaC é estágio AWS.

## Nunca trocar SSH ou rede sem uma sessão paralela: a porta que se tranca por dentro

Existe um tipo de erro de hardening que não deixa registro nem screenshot: o que te deixa
fora da própria máquina. Toda mudança desta Issue toca (ou pode tocar) o caminho pelo qual
você está conectado.

- **Por que importa:** se a única sessão SSH for derrubada por um `sshd` mal reiniciado,
  um `ufw` editado sem regra de 22, ou um `sysctl` de rede que quebra o roteamento, o
  "desfaça a mudança" só é possível **de dentro** — e você está fora. Sem acesso, resta
  console físico/cloud ou recriar a VM: horas de trabalho e perda da stack. A regra da
  T0-02 (nunca trancar a porta pela qual você está dentro) é a mesma coisa aqui, com o
  agravante de que hardening **é** mexer nessas peças de propósito.
- **Mecanismo:** com duas sessões, o efeito destrutivo vira detectável em segundos —

  ```text
  Sessão A (não mexe): dorme no prompt, só observa
      └─ se a Sessão B derrubar o SSH → A continua viva → reverta por A
  Sessão B (mexe): aplica UMA mudança → testa
      └─ SSH caiu → sem A, você está fora; com A, você está no rollback
  ```

  Uma sessão só não tem observador: o comando que derruba a conexão é o mesmo que você
  não vê terminar. Duas sessões separam **quem aplica** de **quem testa a permanência do
  acesso**.

```bash
# terminal 1 — sessão A: observador, não executa nada além de checar
ssh -o ServerAliveInterval=15 lab@<ip-da-vm>     # fica aberta o tempo todo
# (dentro dela, só comandos de verificação:)
docker compose ps                                # → 6×healthy

# terminal 2 — sessão B: a que aplica a mudança de hardening
ssh lab@<ip-da-vm>
sudo sysctl -w net.ipv4.conf.all.send_redirects=0    # exemplo: mudança candidata a risco
```

- **Exemplo no lab:** o roteiro da Issue é executar qualquer ajuste de sysctl/serviço com
  a sessão de manutenção aberta e, ao final, **provar que a porta não trancou**: login
  novo por chave em janela separada (`ssh lab@<ip>`) + stack `6×healthy`. Se o SSH novo
  não entrar, a Sessão A é o que permite reverter (`sudo ufw reload` de origem, reverter
  `99-lab.conf`, `systemctl restart ssh`) em vez de ir ao console.
- **Fronteira entre Issues:** o **acesso** em si (chave-only, sem senha) é da **T0-02**, e
  a régua 22/80/443 é da **T0-03** — esta Issue não muda nenhuma das duas, apenas exige
  que as mudanças de hardening não as quebrem. O procedimento formal de emergência (o que
  fazer quando algo cai) é o runbook da **T3-04**; aqui a sessão paralela é o seatbelt do
  experimento, não um incidente declarado.

## Drift: hardening é estado, não evento — o `compose up` que não passa por auditoria

"Configurei seguro no começo" deixa de ser verdade a cada serviço novo. O estado de
segurança da VM tem data de validade e a data é a última auditoria.

- **Por que importa:** o drift é o modo normal de reabrir portas sem ninguém decidir abrir.
  Um `docker compose up -d` novo publica porta, um pacote instala serviço de rede, alguém
  testa um `sysctl` e esquece de persistir/reverter — e em nenhum desses casos alguém pensou
  "vou relaxar a segurança". O resultado prático é o mesmo de uma decisão consciente: mais
  superfície, medida há meses atrás.
- **Mecanismo:** o ciclo é estado → evento → estado novo:

  ```text
  baseline (Lynis + ss + ufw, registrados)
      → alguém muda a VM (compose up, apt install, sysctl manual)
          → estado real diverge do estado auditado   ← drift
              → só a re-auditoria re-sincroniza registro e realidade
  ```

  Por isso a entrega da Issue é o `docs/hardening.md` **versionado**: ele é a memória do
  estado auditado, e é a comparação dele com a auditoria nova que revela o drift. Score sem
  registro não detecta nada; registro sem re-auditoria envelhece sozinho.

```bash
# a pergunta de drift, antes de assumir que "continua tudo certo":
sudo ss -tlnp | grep -E '0\.0\.0\.0|:::'      # alguma escuta nova desde a última auditoria?
sudo ufw status numbered                       # alguma regra nova? (padrão: 22, 80, 443)
docker ps --format '{{.Names}}\t{{.Ports}}'   # algum container publicando porta nova?
lynis audit system | tee /tmp/lynis-recheck.log  # índice bate com o registrado?
```

- **Exemplo no lab:** a própria Issue documenta o drift já existente — 6+ containers e
  bridges Docker entraram **sem** nova auditoria desde a T0. A re-auditoria desta Issue é
  o primeiro re-sincronismo; o registro deve citar como dívida conhecida o que ficou sem
  cobrir (ex.: serviço novo adicionado fora de ciclo).
- **Fronteira entre Issues:** o **hábito** de re-auditar depois de mudança de stack é
  consolidado no exercício final da **T4-04**, que põe a cadeia inteira sob pressão; o que
  muda quando um **produto novo** entra (imagem escaneada, secrets) é **T4-03** e **T4-02**
  respectivamente. Esta Issue entrega a linha de base, o registro e o entendimento de que
  o ciclo existe — não o cron nem a automação do ciclo (automação de compliance é IaC,
  estágio AWS).

## Como iniciar o modo teach-anything

- "Me ensina a ler a saída do Lynis nesta VM: o que é warning, o que é suggestion, por que
  o Hardening index de `/tmp/lynis-before.log` não é nota de segurança — e como transformar
  um achado em linha de `docs/hardening.md`"
- "Me ensina superfície de ataque cruzando `ss -tlnp`, `docker ps` e `ufw status numbered`
  desta VM: classificar cada escuta (loopback × 0.0.0.0 × bridge) e provar por que só
  22/80/443 são alcançáveis de fora"
- "Me ensina sysctl na prática: por que `sysctl -w` some no reboot, como persistir em
  `/etc/sysctl.d/99-lab.conf` e validar com `sysctl --system` sem derrubar a stack"
- "Me ensina a regra da sessão paralela: montar comigo o padrão de duas sessões SSH
  (observador × aplicador) para testar uma mudança de hardening e o rollback por A quando
  o SSH cai"
- "Me ensina drift: por que hardening é estado e não evento, usando o histórico desta VM
  (hardening na T0, 6 containers depois) e os comandos que re-sincronizam registro com
  realidade"
