# Estudo — Secrets: o que é segredo, onde o valor já parou de ser só seu, e trocar sem medo

> Material de estudo da Trilha 4. Acompanha a Issue 02 (secrets: inventário, gate e
> rotação), mas não é o contrato daquela Issue: aqui é para entender, lá é para provar.

## Secret, config ou credencial: a pergunta não é "é sensível?", é "o que alguém ganha se virar público?"

- **Por que importa:** classificar errado custa nos dois lados. Tratar `PORT` como segredo
  enche o `.env` de mistério e cria *secreto fatigue* — quando tudo é crítico, nada recebe
  tratamento crítico e o `JWT_SECRET` acaba sendo tratado como "mais uma variável". Já
  tratar `JWT_SECRET` como config é como lab vaza sem barulho: ninguém protege o que acha
  que é conforto. É por isso que "você tem um `.env`?" não é a pergunta da vaga — a
  resposta que interessa é "eu sei o que é segredo e o que não é, e o código mostra onde
  passei essa decisão".
- **Mecanismo:** os três tipos se separam pelo **efeito de o valor ficar público**, não
  pela sensibilidade subjetiva:

| Tipo | O que o valor entrega | No lab | Se virar público |
|---|---|---|---|
| **Config** | descreve *como* o sistema roda; trocar muda comportamento, não acesso | `SPRING_PROFILES_ACTIVE`, `JWT_EXPIRATION_IN_MINUTES=10`, `REDIS_PORT`, `CORS_ALLOWED_ORIGINS` | ninguém entra em lugar nenhum — no máximo descobre topologia |
| **Secret** | é a chave de um **cálculo**: assinar, verificar, cifrar | `JWT_SECRET` | quem tem, tem poder de assinatura — forja token válido e passa por qualquer usuário |
| **Credencial** | identidade **mais** segredo que autentica num serviço | `DB_PASSWORD`, `REDIS_PASSWORD`, `SSH_PRIVATE_KEY`, credencial do receiver da T3-03 | login como aquela identidade: dados do banco, a VM, canal de alerta |

  O caso do JWT mostra por que secret não é "config valioso": em `TokenService`, o mesmo
  valor assina **e** valida —

  ```java
  private Algorithm getAlgorithm() {
      return Algorithm.HMAC256(secret);   // gerar e verificar usam o MESMO valor
  }
  ```

  Não existe chave pública que você possa publicar (como existe em RSA): o `.env.example`
  que cita `jwt.secret: ${JWT_SECRET}` **sem `:` de default** está dizendo "se eu não tenho
  esse valor, não subo" — enquanto `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}`
  diz "se não tiver, uso `dev`". Configuração de conforto ganha default; segredo, nunca.

  ```bash
  # no repo — os placeholders e quem tem default (o ":" entrega quem é o quê)
  grep -nE '\$\{[A-Z_]+(:[^}]*)?\}' VPS/src/main/resources/application.yml
  ```

  Duas fronteiras cinzas que a entrevista gosta de testar: `DB_USERNAME` é credencial sem
  segredo (identidade, não chave), e `CORS_ALLOWED_ORIGINS` *parece* restritivo mas é
  config — quem ignora o navegador (curl, script) não obedece CORS, então ele nunca foi
  credencial de ninguém.

- **Exemplo no app:** o `.env.example` versionado separa os dois com sintaxe, não com
  comentário: `JWT_SECRET=` e `DB_PASSWORD=` vêm sem fallback obrigatório, `REDIS_HOST` e
  a porta vêm com. Quem lê o arquivo lê a política de secrets dele.
- **Fronteira entre Issues:** medir e fechar a superfície da VM (portas, serviços,
  permissões) é **T4-01** (hardening); *onde* cada valor é guardado hoje e *como* trocá-lo
  são os próximos tópicos desta mesma Issue; **vault/SOPS** (substituir o `.env` em disco)
  é estágio AWS, explicitamente fora do escopo daqui.

## `.env` com 600 protege um arquivo — o valor já tem cinco outras cópias, e 600 não alcança nenhuma delas

- **Por que importa:** "estar fora do git **hoje**" é sorte, não processo — e sorte tem
  data de validade. O problema não é o vazamento de amanhã, é o que você não sabe de
  hoje: *entrou em qual commit? está em mais algum lugar? quem já clonou?* Sem essa
  resposta, a reação no dia do estrago é improvisar (apagar commit, rezar, mudar senha de
  tudo ao acaso). E o vazamento que ninguém sabe que já aconteceu é justamente o caso
  normal: só a varredura responde a pergunta "já vazou?".
- **Mecanismo:** permissão `600` é propriedade de **um arquivo, num host, neste instante**.
  O **valor** não tem permissão — ele tem cópias, e cada cópia nasce de um mecanismo
  diferente, com vida útil diferente:

| Cópia do valor | Como nasce | Some sozinha? | Resposta certa |
|---|---|---|---|
| histórico de git | um `git commit` que toca o valor | **não** — `git log -p` guarda para sempre; `--amend` só empurra | revogar o **valor** |
| clone (laptop, runner de CI) | `git clone` | some quando apaga o clone — clones antigos continuam | revogar o valor |
| camada de imagem | `COPY .env` no build / `ENV JWT_SECRET=` | `docker rmi` local não apaga do GHCR | revogar o valor |
| tarball de backup | `tar -czf lab-backup-$DATA.tar.gz /etc /home/lab/dados` + `rsync` pro host | retenção de 7 dias no host destino | revogar o valor |
| log de CI, print, tela compartilhada | `echo $SECRET` num step, screenshot | nada no mundo tira de volta | revogar o valor |

  Note a coluna da direita: **todas** as linhas terminam na mesma ação. Nenhuma cópia é
  "limpada" — o histórico git não se apaga sem reescrever branches (force-push, coordenação
  com quem já clonou), e apagar não desfaz o fato de alguém ter uma cópia. Por isso a
  ordem é: acha o vazamento → **troca o valor**, e só depois (se importar) pensa em
  reescrever passado. E o `tar` do backup não desrespeita o 600 por maldade: ele roda com
  permissão para ler, copia o conteúdo e o manda embutido no tarball para **outro** host.

- **Exemplo no lab:** a varredura que a Issue pede, executada no repo real:

  ```bash
  git log -p --all | grep -nE 'SECRET|PASSWORD|TOKEN' | head     # o que já passou por aqui
  git log --all -S 'JWT_SECRET=' --oneline                       # em qual commit o literal entrou
  git log --all --oneline -- VPS/.env                            # → vazio: o .env nunca foi commitado
  ```

  O primeiro devolve o literal do `JWT_SECRET` em **dois** lugares do histórico: no
  `.env.example` (commit `7ff7432`, o primeiro do repo) e no bloco de exemplo que um estudo
  citou. E aqui está a fronteira que decide se é achado ou ruído:

  ```bash
  diff VPS/.env VPS/.env.example && echo "byte a byte identicos"
  ```

  Enquanto o `.env.example` traz **placeholder** e o `.env` traz outro valor, o arquivo
  versionado é só documentação. No momento em que os dois são o mesmo valor (é o diff
  vazio acima), o segredo de produção está no `git log` de todo mundo que clonou — e a
  resposta não é "apagar o commit", é **rotacionar**. É esse achado que a varredura do
  histórico existe para transformar em decisão registrada (0 achado, ou achado revogado).

- **Fronteira entre Issues:** reescrever histórico (`filter-repo`, force-push, coordenação)
  e retenção/agendamento do backup são assunto de **T0-05**; o gate que impede o *próximo*
  commit de entrar sujo é o tópico seguinte desta Issue; inventário (saber quantas cópias
  e de quê existem) é o último tópico daqui.

## O gate no CI é rede, não vigia — e rede só enxerga literal

- **Por que importa:** segredo entra no **primeiro** commit que o toca. Revisão humana é
  olho para o diff do PR; se o valor passou num commit antigo, num rebase malfeito ou numa
  pressa de sexta-feira, ele já virou histórico e o reviewer de hoje não vê mais nada. Gate
  em CI é a diferença entre "eu prometo não cometer" e "o repo recusa" — e recusa no momento
  em que o custo de corrigir é um commit, não uma revogação.
- **Mecanismo:** o job **falha** (não imprime warning) e a branch protection de **T2-01**
  (check obrigatório) transforma o vermelho em merge impossível. Sem o check, o gate é
  só decoração bonita na aba Actions.

  ```yaml
  # .github/workflows/ci.yml — job ao lado de test / build / deploy
    secrets:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v4
          with:
            fetch-depth: 0          # histórico inteiro: sem isso o gate só vê o último commit
        - uses: gitleaks/gitleaks-action@v2
          env:
            GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
  ```

  Se a ferramenta for só `grep` (escolher e **declarar** é o requisito da Issue), o truque
  é o exit code invertido — `grep` devolve 1 quando não acha, e no CI 1 é falha:

  ```bash
  # no-achado (exit 1) vira sucesso; achado (exit 0) vira falha do job
  ! git grep -nIE '(AKIA[0-9A-Z]{16}|ghp_[A-Za-z0-9]{36}|JWT_SECRET=[A-Za-z0-9+/=_-]{20,}|password:[[:space:]]*[^$<{[:space:]]+)' \
      -- ':!*.example'
  ```

  **Prova negativa** (a evidência de que a rede é de verdade — gate nunca testado é gate
  imaginário):

  ```bash
  git switch -c test-secret-leak
  printf '\nAWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY\n' >> VPS/.env.example
  git commit -am 'teste: dummy secret' && git push -u origin test-secret-leak
  # Actions → job VERMELHO (print aqui) → depois:
  git switch main && git branch -D test-secret-leak && git push origin --delete test-secret-leak
  ```

  O gate enxerga **literal com formato de segredo**. É aí que mora o que ele *não* pega:

| Entra no gate | Passa reto |
|---|---|
| `JWT_SECRET=<valor>` num YAML/commit | `"JWT" + "_SECRET"` montado em código |
| chave AWS (`AKIA...`), token `ghp_...` | valor em base64, hex ou outro re-encode |
| `password: postgres` no compose | valor partido em duas strings concatenadas |
| — | o valor que **nunca** saiu do `.env` da VM (gate varre o repo, não o disco) |

  Causa e efeito: o detector casa **padrão + entropia** no texto commitado; fuja do padrão
  (concatenar, codificar, dividir) e a busca não encontra nada — não porque a ferramenta é
  ruim, mas porque ela lê texto, não intenção. Daí a cadeia da Issue: gate pega o literal
  *na entrada*; inventário registra **"como rotacionar"**, que é o plano B para quando o
  gate falhar — e ele falha.

- **Fronteira entre Issues:** **T4-03** (supply chain) é outra rede — Trivy e assinatura
  cuidam da *imagem* e das dependências, não do texto do repo; OIDC e credencial efêmera
  GitHub → cloud são estágio AWS; o gate aqui não substitui inventário nem rotação (esta
  Issue exige os três: inventário, gate, rotação executada).

## Rotação: trocar é a única prova — e o `401` do token antigo é o sucesso, não o incidente

- **Por que importa:** secret nunca rotacionado não é gestão, é fé. É a mesma lógica do
  restore drill da **T0-05**: backup que nunca foi restaurado não existe; segredo que nunca
  foi trocado significa que, no dia em que o `JWT_SECRET` vazar (histórico, backup, print),
  você vai improvisar a primeira rotação da sua vida no meio do estrago. E é a pergunta que
  separa "tenho `.env`" de "eu consigo me recuperar de um vazamento".
- **Mecanismo:** como `TokenService` usa `HMAC256`, assinar e validar dependem do **mesmo**
  valor e não existe estado de revogação no servidor — o app não guarda lista de tokens
  emitidos. Consequência direta: trocar o segredo invalida **todos** os tokens antigos no
  mesmo instante, sem janela, sem aviso, sem transaction. Dois modelos de rotação existem e
  o JWT é o segundo:

  | Modelo | Ordem | O que acontece com quem já estava autenticado |
  |---|---|---|
  | multi-serviço / assimétrico | distribui o **novo** primeiro, invalida o antigo depois | **janela**: os dois válidos ao mesmo tempo, para não derrubar ninguém |
  | JWT simétrico único (este app) | gere → aplique → reinicie | **quebra seca**: ou tem o valor novo, ou vale o antigo — nunca os dois |

  A "quebra seca" é o comportamento esperado: **usuário desloga**. Em produção isso seria
  uma janela comunicada; em lab sem usuário, o "usuário" é o próprio teste.

  Cadeia completa, na ordem que faz sentido (não adianta testar antes de aplicar nem
  aplicar sem testar):

  ```bash
  # 1. gerar — nunca reutilizar, nunca inventar na mão
  openssl rand -base64 48

  # 2. prova ANTES: só o hash, para comparar sem expor o valor
  ssh lab@<ip-da-vm> 'grep "^JWT_SECRET=" .env | sha256sum'

  # 3. aplicar pelo fluxo da T2-03 (.env novo chega na VM pelo fluxo de deploy)
  ssh lab@<ip-da-vm> 'grep "^JWT_SECRET=" .env | sha256sum'    # → hash DIFERENTE do passo 2

  # 4. reiniciar para a app reler o ambiente (env é lido na subida, não em hot-reload)
  ssh lab@<ip-da-vm> 'cd ~/lab && docker compose restart app && docker compose ps'

  # 5. saúde primeiro: app deu certo a troca?
  curl -ki https://<host>/api/v1/actuator/health               # → 200

  # 6. o ciclo completo da rotação: novo funciona, antigo é recusado
  curl -i -X POST https://<host>/api/v1/auth/login -H 'Content-Type: application/json' \
       -d '{"email":"...","password":"..."}'                    # → 200, token NOVO
  curl -i https://<host>/api/v1/auth/refresh -H 'Cookie: refreshToken=<ANTIGO>'   # → 401
  ```

  Dois detalhes que evitam falsa prova — e aí mora a habilidade:

  - **`401` pode ser mentira.** O access token expira sozinho em 10 min
    (`JWT_EXPIRATION_IN_MINUTES=10`). Se você emitir, tomar café e testar, o `401` prova
    expiração, não rotação. A prova limpa é emitir **antes** de rotacionar e testar em
    seguida — ou usar o refresh token (`JWT_REFRESH_EXPIRATION_IN_DAYS=7`), que é o caminho
    que trata o nulo de propósito: `AuthService.refresh` chama `TokenService.validate`, recebe
    `null` e devolve `UnauthorizedException("Refresh token inválido ou inexistente")` → 401.
  - **Meça o status que a rotação produzir.** No caminho do access token quem lê o token é
    o `SecurityFilter` (`if (!email.isEmpty())` sobre o `null` que `validate` devolve) — se
    esse caminho devolver `500` em vez de `401`, o achado é tratamento de erro no filtro, é
    **app intocado** nesta Issue (a rotação não muda `src/`) e entra como registro, não como
    desculpa para não rotacionar.

- **Exemplo no lab:** o procedimento escrito com os passos **reais** que você executou
  (não os teóricos deste estudo) é metade da entrega — é o "backup testado" dos secrets:
  vale no dia em que for preciso, porque já foi feito uma vez.
- **Fronteira entre Issues:** trocar o app ou o `TokenService` para "melhorar" a rotação é
  **fora de escopo** (app intocado); rotação automática a cada N dias com organização atrás
  também (aqui o ciclo é **declarado** e a execução é manual); a chave SSH da VM e os
  GitHub Secrets **já existem** — aqui são inventariados, não criados.

## O inventário é a planta baixa que o vault futuro precisa — e o que ele não compra

- **Por que importa:** sem inventário, "adotamos um vault" significa mudar o caos de lugar:
  você desconhece quantos segredos existem, onde vivem e quem consome, e a ferramenta nova
  herda esse vazio. É a pergunta da entrevista em forma de artefato — "tem inventário,
  gate e rotação testada" é a resposta; "tenho `.env`" é a resposta que trava.
- **Mecanismo:** o inventário é a lista **sem valores** — e "sem valor" é o ponto, não o
  detalhe: se o documento tiver o valor, ele vira mais uma cópia (tópico 2) e a mesma
  regra manda revogá-lo. Cada linha responde a seis perguntas que a operação faz no dia do
  problema: **nome · onde é usado · onde está guardado · quem consome · como rotacionar ·
  desde quando** (mais o dono e "quem avisa quem" quando a troca afeta gente).

  | Segredo do lab | Usado em | Guardado em | Como rotacionar (o campo que o vault não inventa por você) |
  |---|---|---|---|
  | `JWT_SECRET` | `application.yml` → `TokenService` | `.env` da VM (`600`) | `openssl rand -base64 48` → fluxo T2-03 → restart → 401 no antigo |
  | `DB_PASSWORD` | JDBC do Spring, Flyway | `.env` da VM | troca no `.env` + `ALTER USER` no Postgres (afeta o dado, não só o app) |
  | `REDIS_PASSWORD` | `spring.data.redis.url` | `.env` da VM | troca no `.env` + `requirepass` no Redis |
  | chave SSH de deploy | job `deploy` (T2-03) | secret `SSH_PRIVATE_KEY` do repo | gerar par novo, atualizar `authorized_keys`, revogar a velha |
  | credencial do receiver | rota do Alertmanager (T3-03) | `.env` da VM | trocar no provedor do receiver, depois no `.env` |
  | chave SSH do `lab` | acesso manual à VM | `~/.ssh` da VM + meu laptop | `ssh-keygen` novo, `authorized_keys`, revogar (T0-02) |

  Prova de que o documento é só metadados (é critério da Issue, não preciosismo):

  ```bash
  grep -iE 'secret|password|token' secrets/INVENTARIO.md    # → nomes e procedimentos, zero valor
  ssh lab@<ip-da-vm> 'grep -o "^[A-Z_]*" .env'              # → cada variável aparece no inventário
  ```

  E a fronteira com o futuro: **o que um vault (HashiCorp, SOPS + age) substitui** é o
  "onde está guardado" e o "quem rotaciona" — `.env` em disco vira valor fora do disco, com
  lease, audit log e rotação automática. **O que ele não substitui** é o resto: a definição
  do que é segredo, o gate no CI, o procedimento escrito e a discipline de provar a troca.
  Por isso a Issue diz que o inventário define **o que** o vault substituiria — é a
  especificação da ferramenta que só faz sentido no estágio AWS com IaC.

- **Exemplo no lab:** o inventário versionado revela **nomes** a quem lê o repo — isso é
  aceito e documentado de propósito (metal: saber que existe `DB_PASSWORD` não é vazamento;
  saber o valor é). Valores ficam onde já estão, e o documento passa a ser a prova de que
  dá para saber tudo sem eles.
- **Fronteira entre Issues:** implementar Vault/SOPS/age e External Secrets é **estágio
  AWS**, fora de escopo aqui; criar credencial nova não é desta Issue (a chave SSH e os
  GitHub Secrets existem — inventariar, não criar); e o inventário nasce **dependente da
  T3-03**: sem o receiver configurado, a credencial mais nova do lab já falta na lista.

## Como iniciar o modo teach-anything

- "Me ensina secret vs. config vs. credencial usando o `application.yml` e o `TokenService` deste app"
- "Me ensina por que `.env` com 600 não basta, varrendo o histórico real com `git log -p --all | grep`"
- "Me ensina gate de vazamento no CI com gitleaks/grep no `ci.yml`, incluindo o que ele não pega"
- "Me ensina rotação do `JWT_SECRET` e por que o `401` do token antigo é o comportamento esperado"
- "Me ensina inventário de secrets com o `secrets/INVENTARIO.md` e o que um vault (SOPS/HashiCorp) substitui"
