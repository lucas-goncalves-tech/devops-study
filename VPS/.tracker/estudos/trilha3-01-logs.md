# Estudo — Logs: onde o stdout para, quanto fica e o que sobrevive

> Material de estudo da Trilha 3. Acompanha a Issue 01 (logs: sobrevivem ao restart,
> rotacionam sozinhos, respondem pergunta), mas não é o contrato daquela Issue: aqui é para
> entender, lá é para provar.

## O stdout do container não vai para lugar nenhum — até virar arquivo em /var/lib/docker

Quem vem do terminal tem o hábito de "ver o log" como quem olha uma janela: apareceu,
passou, acabou. Container não é janela. O processo do container continua escrevendo em
stdout/stderr, mas não existe terminal esperando: o Docker coloca um leitor na ponta do
pipe e **despeja tudo num arquivo JSON por container**, dentro da área de dados do próprio
daemon. Se ninguém disser nada, esse arquivo só cresce.

- **Por que importa:** esse arquivo é o único lugar onde a app fala. Sem ele, "o que
  aconteceu ontem 3h?" vira reexecutar o cenário e torcer para o bug aparecer de novo. E
  como ele é gravado sem teto por padrão, o descuido tem duas faces: não ter histórico
  (some) ou ter histórico demais (o disco enche — ver o próximo tópico).
- **Mecanismo:** stdout do processo → pipe → coletor do driver de log → arquivo em
  `/var/lib/docker/containers/<id>/<id>-json.log`. O driver é escolhido **uma vez, na
  criação do container**, e fica gravado na configuração dele:

  ```bash
  # qual driver a VM usa (default: json-file)
  docker info | grep -A3 'Logging Driver'

  # onde o log da app realmente está, no disco da VM
  ls -lh /var/lib/docker/containers/*/*.log

  # o que este container concreto herdou quando foi criado
  docker inspect --format '{{.HostConfig.LogConfig}}' <container>
  ```

  O formato é JSON de uma linha por evento: `{"log":"...","stream":"stdout","time":"..."}`.
  Por isso `docker logs` consegue reconstituir a linha do tempo — ele não "pergunta ao
  processo", ele lê o arquivo e reemite.
- **Exemplo no app:** a stack da VM tem `app`, `db`, `redis` e `proxy` — quatro arquivos
  crescentes independentes, um por container. O `GlobalExceptionHandler` da `notes-api`
  imprime no stderr/stdout o erro que a API devolveu; é essa linha, parada no
  `<id>-json.log` da `app`, que a Issue 01 vai caçar depois do restart. Repare que o
  `docker compose logs -t app` do compose é a mesma leitura, só que pelo nome do serviço
  em vez do id.
- **Fronteira entre Issues:** o **conteúdo** do log (app escrever JSON estruturado, níveis,
  campos de negócio) é código da app e está fora do escopo da Issue 01 — a app não é
  tocada no lab. Aqui se cuida do **cano** (driver, arquivo, limite), não da mensagem.

## A armadilha do reload: quem já existe não herda a config nova

A solução para "o arquivo só cresce" parece trivial: escrever `max-size`/`max-file` no
`/etc/docker/daemon.json`. O problema é onde essa config é lida e **quando ela passa a
valer** — e o descompasso entre os dois é a armadilha clássica desta Issue.

- **Por que importa:** você edita o arquivo, dá reload, olha o container, vê o log de 400 MB
  e conclui "não funcionou" — ou pior, conclui funcionando e descobre no próximo deploy que
  só os containers novos ficaram limitados. Enquanto isso o disco continua subindo.
- **Mecanismo, causa e efeito:**
  1. `daemon.json` é lido pelo `dockerd` **na inicialização** (e parcialmente em `SIGHUP`,
     o que o `systemctl reload docker` envia) — ou seja, ele muda o que o *daemon* passa a
     fazer daqui para frente;
  2. as opções de log são **copiadas para dentro do container no momento da criação** e
     ficam lá, em `HostConfig.LogConfig`. Mudar o pai não reescreve os filhos;
  3. logo: container criado antes da mudança = continua com o comportamento antigo, para
     sempre, até ser **recriado** (não basta reiniciar).

  ```bash
  # 1. declarar o limite para TODO container criado daqui em diante
  sudo tee /etc/docker/daemon.json <<'EOF'
  {
    "log-driver": "json-file",
    "log-opts": {
      "max-size": "50m",
      "max-file": "3"
    }
  }
  EOF

  # 2. dizer ao daemon para reler (declaração de impacto: em execução não herda)
  sudo systemctl reload docker

  # 3. A PROVA: o container que já roda ainda está com a config velha
  docker inspect --format '{{.HostConfig.LogConfig}}' <container>

  # 4. recriar — só aqui o container novo herda max-size/max-file
  docker compose up -d --force-recreate app
  docker inspect --format '{{.HostConfig.LogConfig}}' app-1   # agora com os limites
  ```

  Três erros de semântica que custam caro: **`docker compose restart`** mantém o mesmo
  container (mesmo id, mesma config — só mata e sobe o processo), portanto não herda nada;
  **`docker compose up -d`** só recria se algo no YAML mudou, então com YAML intocado ele
  é no-op; **`reload` sem recreate** deixa a stack "meio configurada", que é o estado mais
  perigoso porque todos os comandos de inspeção superficial parecem OK.
- **Exemplo no app:** o deploy da Trilha 2 (`scripts/deploy.sh` → `pull` + `up -d`) já
  recria containers quando a imagem muda — mas um `daemon.json` editado à mão, sem novo
  deploy, não dispara nada. Por isso o passo da Issue é explícito: trocar a config e
  declarar o impacto (`up -d` após a mudança, stack voltando healthy com os 4×).
- **Fronteira entre Issues:** *recriar com uma versão nova de imagem* é Trilha 2
  (deploy/rollback da T2-04); aqui o recreate é só o veículo da herda de config de log. E
  rodar o passo "config → recreate → verificar" faz parte desta Issue; **organizar** isso
  num runbook com ordem e verificação é a **Issue 04** da Trilha 3.

## docker logs --since: a linha do tempo é janela, não memória

Com limite declarado e container recriado, o próximo erro de mentalidade é achar que
`docker logs` é "o histórico do serviço". Ele é **o arquivo do container atual**, e a
busca no tempo só funciona dentro do que esse arquivo ainda contém.

- **Por que importa:** a pergunta real de operação é "deu erro entre 3h e 4h?" — sem janela
  você faz `docker logs app | tail -200` e olha o fim das coisas, que raramente é onde o
  problema começou. `--since`/`--until` transformam a leitura em pergunta por intervalo.
- **Mecanismo:** o `--since` filtra pelo **timestamp que o daemon registrou no momento em
  que recebeu a linha**, não pelo texto. Aceita tempo relativo (`10m`, `2h`) e absoluto
  (`2026-10-08T03:00:00`); `--until` fecha a janela pelo outro lado; `-t` mostra os
  timestamps para você enxergar a fronteira:

  ```bash
  # a linha do tempo da app, com timestamp em cada registro
  docker logs -t app --since 30m | tail -40

  # janela fechada: o incidente das 3h
  docker logs -t app \
    --since 2026-10-08T03:00:00 --until 2026-10-08T04:00:00 \
    | grep -i error

  # gerar o erro real da app (GlobalExceptionHandler) e marcá-lo no tempo
  curl -s -o /dev/null -w '%{http_code}\n' -X POST \
    http://127.0.0.1:8080/api/v1/rota-que-nao-existe -d '{}'

  # matar e subir de novo o MESMO container (restart preserva o arquivo)
  docker compose restart app

  # achar a linha anterior ao restart — é a prova da Issue
  docker logs -t app --since 2m | grep -i error
  ```

  O que **sobrevive a restart:** `docker compose restart` (e `docker stop`/`start`) não
  apaga o arquivo — o container é o mesmo, o `<id>-json.log` é o mesmo, então a linha de
  antes do restart continua lá e `--since 2m` encontra. O que **não sobrevive:**
  `docker compose rm` + `up` (container novo, id novo, diretório de log novo) e
  `docker compose up -d --force-recreate` — o log antigo morre com o container antigo, que
  é justamente porquê a recriação do tópico anterior acontece **antes** do incidente de
  teste, não durante a investigação.
- **Exemplo no app:** a sequência da Issue 01 é exatamente essa — gerar o erro que o
  `GlobalExceptionHandler` já emite (o app é intocado), restartar a `app`, e **achar** a
  linha com `docker logs --since 2m app | grep <erro>`. Repare no paradoxo que ela
  exercita: o restart é a coisa que "some logs" no modelo errado, e é exatamente o que
  prova que os logs não somem.
- **Fronteira entre Issues:** quanto tempo o arquivo **guarda** (teto de disco) é o tópico
  do driver; quanto tempo o journal **guarda** é o próximo tópico; e busca em vários
  hosts/com janela longa demais é o ponto onde o disco local acaba — isso é a fronteira
  com a centralização (Loki/ELK), estágio futuro, e com as **métricas da Issue 02**, que
  respondem "quantos erros por segundo" sem varrer texto.

## journald do host: persistente, volátil, e o vacuum que apaga a prova

A app fala pelo driver do Docker; o **host** fala pelo journald — o `lab-heartbeat` da
Trilha 0-04, o sshd, o docker daemon, o kernel. São dois sistemas de log com duas
armadilhas de geometria: onde eles moram e quando eles apagam.

- **Por que importa:** em modo volátil, o journal vive em `/run` (tmpfs) e **some no
  reboot** — o incidente das 3h vira investigação de um host que reiniciou e perdeu a
  causa raiz. E mesmo persistente, ele tem limite padrão; sem decidir o teto, o disco decide
  por você (com o agravante de `/var/lib/docker` e `/var/log/journal` costumarem estar no
  mesmo filesystem de `/` — um enche o outro).
- **Mecanismo — a escolha de armazenamento:**

  | | Volátil (`Storage=auto` sem `/var/log/journal`) | Persistente (`Storage=persistent`) |
  |---|---|---|
  | Onde grava | `/run/log/journal` (tmpfs) | `/var/log/journal` (disco) |
  | Sobrevive a reboot | **não** | sim |
  | Custo | histórico perdido a cada queda | precisa de limite |

  ```bash
  # onde o journal está gravando agora
  ls -ld /var/log/journal            # existe → persistente
  grep -E '^#?(Storage)' /etc/systemd/journald.conf

  # prova de persistência: o arquivo existe e tem conteúdo
  journalctl --disk-usage
  journalctl -u lab-heartbeat -b -n 5

  # criar o diretório e reativar para gravar em disco daqui em diante
  sudo install -d -m 2755 /var/log/journal
  sudo systemctl restart systemd-journald
  ```

  E o mecanismo do vacuum — por que ele apaga o **mais antigo primeiro**:

  ```bash
  journalctl --disk-usage                      # quanto o journal ocupa hoje
  sudo journalctl --vacuum-size=200M           # teto: sobra no máximo 200M
  sudo journalctl --vacuum-time=7d             # ou: só últimos 7 dias
  journalctl --disk-usage                      # depois: ≤ 200M
  ```

  `--vacuum-size` é uma limpeza **por antiguidade**: ele descarta os arquivos arquivados
  mais antigos até caber no teto. Não há noção de "importância" — ele não sabe o que é um
  incidente. Detalhe de mecânica: o arquivo corrente (do boot atual) só é arquivado num
  `--rotate`, então rodar vacuum sem rodar `journalctl --rotate` antes costuma liberar
  menos do que o esperado.

- **Exemplo no app — o vacuum de madrugada:** imagine o incidente da 3h da manhã gerando
  rajada de erro no `lab-heartbeat` e no `docker` (mesmo journal). Um cron às 4h roda
  `--vacuum-size=50M` e o journal, que estava grande, é cortado **começando pelo mais
  antigo** — que é exatamente a faixa onde a causa raiz mora. De manhã, quem investiga vê
  o journal "limpo e saudável" (`--disk-usage` dentro do teto) e nenhum indício do que
  houve. A prova não foi analisada: foi otimizada para fora do disco. É por isso que o
  vacuum é **rotina agendada e declarada** (com limite documentado), coisa da
  **Issue 04** (runbook), não executor avulso rodando quando alguém lembra.
- **Fronteira entre Issues:** o journal do `lab-heartbeat` nasceu na **Trilha 0-04** e o
  `Storage=persistent` + `--vacuum-size` são desta Issue 01; **quando** rodar a limpeza e
  o que fazer se o disco estourar entra no `RUNBOOK.md` da **Issue 04**; alerta de "disco
  ≥ 90%" é da **Issue 03**; e a série temporal que responderia "journal cresceu quanto nas
  últimas 24h" é **métrica**, Issue 02.

## Log que não rotaciona é mina de disco — e a fronteira com a centralização

Juntando as peças: todo log é mina enquanto não houver teto. A rotação
(`max-size` × `max-file`) **transforma o custo de log em constante** — e uma constante é
o que dá direito a dizer "aqui o disco não vai mudar de nível".

- **Por que importa:** ENOSPC não é "falta de espaço para salvar arquivo". Quando
  `/var/lib/docker` não tem espaço, o daemon não consegue gravar layers, criar container,
  nem manter o próprio log rodando — o efeito dominó é `docker pull` falhando, container
  não subindo e a stack caindo; num host de serviço, o disco cheio derruba a máquina
  inteira de formas que parecem não ter relação com "um log cresceu".
- **Mecanismo:** teto declarado = aritmética simples.

  | Config | Teto por container | Empilhado na stack da VM (4 containers) |
  |---|---|---|
  | sem rotação (default) | **ilimitado** | infinito, até ENOSPC |
  | `50m` × `3` | 150 MB | ≤ 600 MB, sempre |
  | `10m` × `3` | 30 MB | ≤ 120 MB — barato, mas some mais cedo |

  ```bash
  df -h /                                    # baseline ANTES do teste
  docker system df                           # quanto os logs ocupam hoje
  cat /etc/docker/daemon.json                 # o teto declarado, com números

  # gerar volume de log na app e ver a rotação segurar o crescimento
  for i in $(seq 1 5000); do
    curl -s -o /dev/null -X POST http://127.0.0.1:8080/api/v1/rota-que-nao-existe
  done

  ls -lh /var/lib/docker/containers/*/*.log   # nenhum passa de max-size
  ls -lh /var/lib/docker/containers/*/*-json.log.*   # as rotações (max-file)
  df -h /                                    # DEPOIS: delta dentro do esperado
  ```

- **Exemplo no app — a escolha de 50m × 3:** o número é decisão, não enfeite. 150 MB por
  container × 4 serviços é um teto que cabe folgado na VM e ainda cobre janelas de horas
  de `app` (é o que faz `--since 2m` do incidente achar a linha). Quem escolhe 5 MB × 1
  paga barato e perde o histórico no meio do expediente; quem escolhe 500 MB × 10 "para
  não perder nada" assina um cheque de 20 GB e descobre o preço no `df`. O valor declarado
  e justificado é parte da entrega da Issue.
- **Fronteira entre Issues — o teto é local, a pergunta nem sempre:** rotação por arquivo
  não tem noção de importância: um `ERROR` de 3h atrás pode já ter sido rotacionado se o
  volume for alto. Isso é o limite honesto do disco local e o motivo de **centralização
  (Loki/ELK/OpenSearch) ser estágio futuro, fora desta Issue** — lá a retenção vira
  consulta por dias/semanas em outro sistema. Antes de chegar lá, a ordem da Trilha 3 é
  esta: **01** (cano com teto e histórico que sobrevive) → **02** (métrica: quantos erros,
  sem varrer texto) → **03** (alerta: alguém é avisado) → **04** (runbook: o que fazer).

  | Issue | O que ela responde sobre log/observabilidade |
  |---|---|
  | 01 (esta) | onde o log mora, quanto ocupa, o que sobrevive ao restart |
  | 02 | quantos erros por segundo, série no Grafana (Actuator/Prometheus) |
  | 03 | quem é avisado quando disco sobe ou alvo cai |
  | 04 | qual comando rodar na crise — vacuum agendado, drill cronometrado |
  | futuro | busca centralizada em vários hosts (Loki/ELK) |

## Como iniciar o modo teach-anything

- "Me ensina o driver de log do Docker e onde o stdout da `app` para de fato, partindo do
  `/var/lib/docker/containers/*/*.log` desta VM"
- "Me ensina por que `systemctl reload docker` não limita os containers que já rodam, e
  quando o `max-size`/`max-file` do `daemon.json` passa a valer de verdade"
- "Me ensina `docker logs --since`/`--until` como linha do tempo, usando o erro do
  `GlobalExceptionHandler` e um `docker compose restart app` para mostrar o que sobrevive"
- "Me ensina journald persistente vs volátil e `journalctl --vacuum-size`, com o
  `lab-heartbeat` e o `--disk-usage` desta VM"
- "Me ensina a escolher `max-size` × `max-file` sem encher o disco, medindo o antes/depois
  com `df -h` e `docker system df`"
