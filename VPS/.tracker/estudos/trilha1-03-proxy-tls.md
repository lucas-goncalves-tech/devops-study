# Estudo — Borda: reverse proxy, TLS e a app fora do alcance

> Material de estudo da Trilha 1. Acompanha a Issue 03 (proxy com TLS na borda), mas não é
> o contrato daquela Issue: aqui é para entender, lá é para provar.

## Por que terminar TLS na borda: uma porta pública, e só ela

TLS existe para que o que sai da sua máquina chegue íntegro e cifrado — mas ele só protege
o ponto onde ele termina. Se a app fala HTTPS sozinha, a app carrega certificado, renovação,
porta 443 e expõe o servlet container ao mundo; se o proxy termina TLS, a app nem sabe que
TLS existe e continua falando HTTP na rede interna. A escolha é onde o mundo encosta.

- **Por que importa:** cada serviço com porta pública é uma superfície separada para
  configurar, atualizar e lembrar. Hoje é a `notes-api` em `127.0.0.1:8080`; amanhã, com VPS
  pública, seriam a app **e** o proxy — duas portas, dois TLS, dois pontos de falha, e a app
  (Spring Boot, Flyway, Actuator) exposta a qualquer scan direto na porta.
- **Mecanismo:** porta publicada (`ports:` no compose) é um encaminhamento `host:container`
  que o Docker abre na host; sem `ports:`, o container só é alcançável pela rede interna do
  compose. O proxy fica no meio: mundo → `443` (proxy) → `app:8080` (rede interna, HTTP).
  É o mesmo princípio da Trilha 0-03 aplicado à rede: **só o explícito passa** — a única
  publicação explícita que sobra é a do proxy.

```bash
docker compose config | grep -A3 'ports:'   # só o proxy tem ports: (80/443)
ss -tlnp | grep -E ':(80|443)\b'            # a única superfície de serviço da host
curl -m 3 http://127.0.0.1:8080/api/v1/actuator/health   # porta antiga → Connection refused
```

- **Exemplo no lab:** na Issue 02 a `app` publica `127.0.0.1:${APP_PORT:-8080}:8080` e o
  `curl` direto nela funciona — é o estado que esta Issue **remove**. Estado final:
  `docker compose config` não mostra `ports:` em nenhuma linha da `app`, `ss -tlnp` mostra
  só 80/443, e o `curl -k -i https://127.0.0.1/api/v1/actuator/health` devolve `200` com
  cabeçalho `server:` do proxy — o mesmo `200` da Issue 02, agora com um caminho diferente.
- **Fronteira entre Issues:** o que é desta Issue é a **borda** (proxy publicado, app sem
  `ports`, redirect HTTP→HTTPS). Firewall da Trilha 0-03 é a regra de host que fecha o que o
  Docker não publica; domínio real + Let's Encrypt, WAF e rate limit de borda são Trilha 4
  e estágio com VPS (ver `Fora de escopo` da Issue 03). E cuidado com a consequência aceita:
  se o proxy morrer, a app fica inacessível de fora — `restart: unless-stopped` no proxy é o
  amparo declarado, não um acidente.

## O handshake TLS em cinco frases, e o que é um certificado autoassinado

O handshake é a conversa inicial em que cliente e servidor concordam em cifrar — e o
certificado é a prova que o cliente exige durante essa conversa.

- **Por que importa:** sem entender a troca, o warning do navegador e o `-k` do `curl` viram
  ruído que se ignora; e ignorar o warning é exatamente como se entra em produção com a
  proteção desligada.
- **Mecanismo:** a ordem causa-efeito do handshake TLS 1.2/1.3, em linhas gerais:
  1. cliente envia *ClientHello* (versões que entende, cipher suites);
  2. servidor responde com o **certificado** — um arquivo público com `subject` (de quem é),
     `issuer` (quem garantiu), validade e a chave pública;
  3. o cliente verifica: a assinatura do `issuer` bate? o nome confere com o endereço que
     ele pediu? está dentro das datas? Se sim, deriva a chave simétrica e manda o conteúdo
     cifrado — a partir daí, texto puro vira registro cifrado.
  Um certificado **autoassinado** é um em que `issuer == subject`: a própria entidade se
  garante. A cadeia de confiança não chega a nenhuma autoridade reconhecida — ela termina
  nela mesma.

```bash
# o que o lab mostra durante o handshake:
curl -kv https://127.0.0.1/api/v1/actuator/health 2>&1 | grep -E 'SSL|subject|issuer'
#   * SSL connection using TLSv1.3 ...
#   * server certificate verification OK
#   * subject: CN=...      issuer: CN=...   ← iguais = autoassinado (cadeia local)
openssl x509 -noout -dates -subject -issuer -in <certificado-do-proxy>
```

- **Exemplo no lab:** sem domínio real, o certificado é gerado localmente (Caddy gera um
  autoassinado sozinho na primeira subida; Nginx exige você criar um com `openssl`). Nos
  logs do `curl -kv` acima, `subject` e `issuer` iguais são a assinatura visual de que a
  cadeia é local — é a limitação declarada da Issue, e anotá-la é parte do estudo.
- **Fronteira entre Issues:** o mecanismo do handshake e do certificado é o aprendizado
  **desta** Issue; emitir certificado **confiável** para um domínio (Let's Encrypt, CA
  pública, renovação automática) é estágio futuro com VPS real — o laboratório usa IP/local
  de propósito, porque o que muda depois é a origem do certificado, não o handshake.

## `curl -k`: o que ele pula, por que o warning existe e por que em produção é inaceitável

`-k` (igual `--insecure`) diz ao `curl`: não verifique o certificado, conecte mesmo assim.

- **Por que importa:** o warning "certificate verify failed" não é um detalhe de UX — é o
  único freio contra ataque man-in-the-middle: alguém na rede se apresenta como
  `127.0.0.1`/seu servidor, entrega **outro** certificado, e o cliente confiaria nele só se
  não estiver verificando. `-k` desliga exatamente essa verificação, e aí o TLS continua
  cifrando... a conversa com o atacante.
- **Mecanismo:** a verificação tem três verificações — cadeia (o `issuer` encacha até uma
  autoridade confiável do sistema), nome (o `subject`/SAN casa com o host pedido) e datas
  (`notBefore`/`notAfter`). Cadeia local autoassinada falha na primeira; sem `-k`, o `curl`
  aborta antes de mandar qualquer requisição. Com `-k`, ele segue e o `200` volta — mas o
  `200` prova só que **alguém** respondeu, não que alguém foi verificado.

```bash
curl -i  https://127.0.0.1/api/v1/actuator/health   # → error: self-signed certificate (falha é o correto)
curl -ki https://127.0.0.1/api/v1/actuator/health   # → 200 (aceitando o risco explícito)
curl -kv https://127.0.0.1/api/v1/actuator/health 2>&1 | grep -E 'SSL|certificate'  # o handshake visível
```

- **Exemplo no lab:** nos dois comandos acima a diferença é a **atitude**, não o servidor:
  o primeiro falha com erro de verificação (a defesa funcionando), o segundo devolve `200`
  e cabeçalho do proxy. No lab isso é aceito e esperado — o certificado é local e o warning
  do navegador é o comportamento correto. Em produção, `-k` em script ou CI vira o hábito
  que silencia o único alarme que existe; a correção é trocar a origem do certificado
  (domínio + Let's Encrypt), nunca manter `-k`.
- **Fronteira entre Issues:** esta Issue entrega o **mecanismo** (handshake visível,
  `-k` consciente, warning entendido); a confiança pública com renovação automática é estágio
  futuro (ver `Limitações` da Issue 03). HSTS, preload e pinning são Trilha 4 (endurecer) —
  não treine esses agora.

## Caddy ou Nginx: o trade-off é ceremony vs. controle, não "o melhor"

Os dois terminam TLS e fazem proxy reverso; a diferença prática é **onde está o trabalho de
configurar** — e esse custo aparece no primeiro dia, não na hora do deploy.

- **Por que importa:** escolher por moda ("Nginx é o padrão") esconde a pergunta real: quem
  gera e renova o certificado, e quanto do TLS está escrito à mão por você. Erro de
  configuração manual de TLS é o tipo de coisa que só se descobre quando o certificado vence.
- **Mecanismo:** causa-efeito lado a lado —

| | Caddy (escolha da Issue) | Nginx (alternativa discutida) |
|---|---|---|
| Config do proxy + TLS | ~3 linhas; certificado automático | `server {}` + `ssl_certificate` apontando para arquivos que **você** gera |
| Certificado | gera sozinho na primeira subida | `openssl req -x509 ...` manual (ou lego/certbot depois) |
| O que você aprende | o desenho (borda, upstream, redirect) | também a maquinaria (caminhos de cert, reload) |
| Quando dói | quando você precisa de controle fino | quando a renovação/ardeiro esquece de alguém |

```yaml
# Caddy — o trade-off em 3 linhas:
:443 {
    tls internal                 # certificado local autoassinado, gerado sozinho
    reverse_proxy /api/v1/* app:8080
}
# Nginx — o mesmo desenho, com a maquinaria à vista:
#   ssl_certificate     /etc/nginx/cert.pem;   # gerado por você com openssl
#   ssl_certificate_key /etc/nginx/key.pem;
#   location /api/v1/ { proxy_pass http://app:8080; }
```

- **Exemplo no lab:** o `compose.yaml` ganha um serviço `proxy` (imagem `caddy`) com
  `depends_on: app: condition: service_healthy` e `ports: ["80:80", "443:443"]`. A evidência
  do trade-off é a mesma nos dois caminhos: `curl -ki https://127.0.0.1/...` → `200` com
  `server:` do proxy e `curl -i http://127.0.0.1/...` → `301`/`308` com `Location: https://...`.
  O que muda é quanto texto você escreveu e assinou para chegar lá.
- **Fronteira entre Issues:** escolher Caddy **é** o escopo desta Issue; migrar para Nginx,
  HAProxy ou Traefik depois é refatoração de borda, não critério. O upstream apontando para
  `app:8080` e o redirect `http→https` valem para qualquer um dos dois — o desenho é o
  aprendizado; a ferramenta é a ferramenta.

## Depois que a porta da app some: quem alcança quem na rede do compose

`ports:` e rede interna são dois mundos com regras diferentes, e a Issue 03 troca a app de
um para o outro.

- **Por que importa:** o erro clássico é achar que "remover a porta" desliga o serviço. Não
  — ele continua de pé, escutando em `app:8080`; só quem alcança muda. Quem não entende isso
  debuga `Connection refused` procurando bug no Spring quando a causa é um `ports:` removido.
- **Mecanismo:** o compose monta uma rede bridge onde os serviços se resolvem pelo **nome de
  serviço** (o DNS é do Docker: `app`, `db`, `redis`). Dentro de um container, `localhost`
  é ele mesmo — por isso a app fala `db:5432`, nunca `localhost:5432` (mesma regra da
  Issue 02). Com `ports:` removido, a host não tem mais encaminhamento para a app; o que
  sobra é `mundo → 80/443 (proxy) → rede interna → app:8080`.

```bash
docker compose exec proxy wget -qO- http://app:8080/api/v1/actuator/health   # rede interna: passa
curl -m 3 http://127.0.0.1:8080/api/v1/actuator/health                       # host, porta antiga: recusado
docker compose config | grep -A2 'ports:'                                    # só 80/443 do proxy
docker compose stop db                                                        # cai o upstream de verdade
curl -ki https://127.0.0.1/api/v1/actuator/health                             # → 502/503 do proxy (correto!)
```

- **Exemplo no lab:** o `exec` acima prova que a app **continua viva e alcançável** — só que
  do lado de dentro, onde só os serviços do compose falam. A última linha mostra a semântica
  da borda: com o banco parado, o proxy devolve `502` em vez de sumir (a borda conta a falha,
  como o "responde ≠ saudável" da Trilha 0), mas `docker compose ps` continua sendo o sinal
  verdadeiro de saúde da app — `502` do proxy não é healthcheck.
- **Fronteira entre Issues:** a rede interna e o `depends_on` por saúde são entregues pela
  **Issue 02** (`compose-ordenado`) — aqui só se reaproveitam, com o proxy no topo da cadeia
  (proxy espera app, app espera banco). DNS extra entre redes, `networks:` customizados e
  mTLS interno não são desta Issue; a cadeia de dependência (`docker compose stop db` → app
  `unhealthy` → proxy `502`) é a mesma da Trilha 0-04, agora com um nível a mais.

## Como iniciar o modo teach-anything

- "Me ensina por que o TLS termina na borda e não na app, mostrando o `compose.yaml` deste lab com o `ports:` removido da `app`"
- "Me ensina o handshake TLS e o que um certificado autoassinado declara, lendo a saída do `curl -kv https://127.0.0.1/api/v1/actuator/health`"
- "Me ensina o que o `curl -k` ignora exatamente, comparando a falha sem `-k` com o `200` do `-ki` neste lab"
- "Me ensina o trade-off Caddy vs Nginx na config real do proxy: 3 linhas de Caddy contra o `server {}` manual do Nginx"
- "Me ensina a rede interna do compose depois de remover a porta da app — `exec` de dentro, porta morta na host e o `502` do proxy com o banco parado"
