# SecurePay — monorepo DevOps

3 apps, 3 trilhas DevSecOps. Cada app = uma sequência **completa e autônoma** do serviço local até
produção. O papel do agente é tracker (Issues, estudos), board e docs — **a infraestrutura é
construção do usuário**.

## Layout

```
securepay-devops/
├── AGENTS.md            # este arquivo — routing
├── README.md            # índice público
├── BOARD.md             # status e ordem de execução das 3 trilhas
├── 00-visao-geral.md    # contexto consolidado, template de Issue, política de status
├── ledger-service/  commerce-api/  webhook-gateway/   # os 3 apps — trilhas em `BOARD.md`
├── archive/             # Issues arquivadas por decisão
├── docs/                # specs e planos do trabalho
├── .github/             # CI (construção do usuário)
├── healthcheck.sh
└── .agents/skills/      # Matt Pocock engineering skills
```

Antes de mexer num app, abra o `AGENTS.md` dele (`ledger-service/AGENTS.md`): é onde vive a
arquitetura, as variáveis de ambiente e o contrato de infra que este arquivo não repete. O
`README.md` do app diz como rodar; `issues/` e `estudos/` são o trabalho.

## Apps, trilhas e estágio

| App | Trilha (ordem da sequência) | Estágio |
|---|---|---|
| `ledger-service` · 8 Issues | VPS — `linux → hardening → caddy → backups → isolamento → deploy` | `01`,`02` `done`; `03` a entrar |
| `commerce-api` · 13 Issues | AWS — `linux → compose → terraform → CI → observabilidade → S3/EC2`, mais a sequência complementar `09 → 13` de capacidade e performance | `01` a entrar; `06` `parked` |
| `webhook-gateway` · 11 Issues | DevSecOps — `pipeline → secrets → SAST → SCA → hardening → gates → DAST → mensageria` | `01` a entrar |
| `image-forge` · 12 Issues *(previstas)* | AWS hardening — `app → infra insana → detecção → correção → gates → observabilidade → performance → custo` | ⛔ **pendente** — ver seção própria abaixo; **não criar nada** |

> A linha `image-forge` é **rastro de planejamento, não de execução**: a spec e o plano existem em
> `docs/superpowers/`, o app não. O que autoriza a criar é o levantamento da pendência descrito na
> seção seguinte.

**Status e ordem de execução → `BOARD.md`:** consulte quando precisar saber o que está feito e o que
entra em seguida. **Metodologia, pilares e política de status → `00-visao-geral.md`:** consulte antes
de escrever ou fechar qualquer Issue. A numeração reinicia em `01` por app: `ledger 05` e
`commerce 05` são Issues diferentes.

## ⛔ Pendente: 4º app (`image-forge`) — não executar antes das 3 trilhas

A spec e o plano do 4º app **existem e estão versionados**; o app não, e não deve nascer antes da
hora.

| Artefato | Caminho | `status:` |
|---|---|---|
| Spec | [`docs/superpowers/specs/2026-09-29-image-forge-quarta-app-design.md`](docs/superpowers/specs/2026-09-29-image-forge-quarta-app-design.md) | `pendente` |
| Plano | [`docs/superpowers/plans/2026-09-29-image-forge-quarta-app.md`](docs/superpowers/plans/2026-09-29-image-forge-quarta-app.md) | `pendente` |

**O que ele é:** uma API de imagem com fetch por URL que **funciona** e nasce insegura e lenta por
construção — 12 Issues, ~22 defeitos rastreados cada um a um **detector** e a uma ferramenta que o
acha, com o `archive/18-kubernetes-helm` re-homed para rodar nele. Ele consolida o que as 3 trilhas
ensinaram: é o índice do que você sabe, escrito para não se perder.

**Por que espera:** é o corpo de prova das trilhas, não mais uma delas. Abrir agora transformaria a
consolidação em dependência — e uma trilha que depende de outra não é autônoma, que é a regra da
`## Trilha ≠ tecnologia` logo abaixo.

**Enquanto esta seção existir, a trava vale:** `image-forge/` **não** está no escopo de escrita,
nenhum Issue dele pode ser criado, e a regra de `0.0.0.0/0` abaixo continua valendo para os 3 apps
sem ressalva. A presença desta seção **é** a trava.

### Regra de levantamento — explícita e verificável

A pendência sai **só** quando as três condições forem verdadeiras. Todas são checáveis por comando,
não por impressão:

**1 · As 3 trilhas concluídas** — incluindo a sequência complementar `commerce 09 → 13`, que precisa
existir porque a `image-forge 09` **reusa o método** dela. `parked` conta como fechado (fecha por
decisão, não por entrega); `blocked` e `todo` não contam.

```bash
# saída vazia = nenhuma linha pendente fora de um subsection "Parked"
awk '/^### /{p=($0 ~ /Parked/)} /^- \[ \]/ && !p{print}' BOARD.md
```

**2 · Spec e plano revisados depois das trilhas** — porque o que elas ensinarem pode mudar o
desenho. Os dois `status:` no frontmatter passam de `pendente` para `aprovada`.

**3 · `AGENTS.md` escrito para o app, na mesma mudança** — levantar a pendência é isto, e nenhuma
das três partes pode ficar de fora:

- esta seção é apagada por inteiro;
- `image-forge/` entra na lista de gravável e a linha da tabela de estágio acima passa de
  `⛔ pendente` para `01 a entrar`;
- a regra de `0.0.0.0/0` é reescrita com a **exceção nomeada e datada** da spec (D-3): limitada aos
  IDs do `image-forge/docs/inventory.md`, datada do dia do levantamento, e com a Issue que remove
  cada um. **Exceção sem data vira regra em seis meses** — por isso ela não foi concedida agora:
  conceder `0.0.0.0/0` a um app que ainda não existe é uma permissão que ninguém audita e que não
  tem recurso algum pendurado nela.

**Critério de que saiu:** o comando do passo 1 retorna vazio, os dois documentos dizem `aprovada`, e
`grep -c 'Pendente: 4º app' AGENTS.md` retorna `0`. Enquanto **qualquer** dos três não for
verdadeiro, a pendência continua — e nada do `image-forge` pode ser criado.


## Tracker

- Issue = uma capacidade, escrita como RFC (problema → escopo → critérios observáveis → validação →
  evidências). Template fixo e política de status: `00-visao-geral.md`.
- Uma Issue não é aula. Material de estudo vive em `<app>/estudos/`, fora da Issue.
- Sem tutorial, FAQ, navegação (`Prev`/`Next`) nem sub-etapas (`1A`, `2B`) dentro da Issue.
- Fechar uma Issue exige a saída real do comando de validação colada em `Evidências` e o
  `status:` do frontmatter atualizado junto com o checkbox do board. Limitação de ambiente
  registrada em `Limitações / notas` não vale como evidência.
- Custo zero por regra: nenhuma Issue exige recurso pago para ser concluída. Infra local, LocalStack
  e `terraform plan` cobrem quase tudo; VPS pública e conta AWS só como prova final.

## Escopo de escrita

Gravável: `ledger-service/`, `commerce-api/`, `webhook-gateway/`, `BOARD.md`,
`00-visao-geral.md`, `README.md`, `AGENTS.md`, `archive/`, `docs/`, `.superpowers/`.

`docs/` e `.superpowers/` são escrita do agente: guardam spec, plano e doc de design — nunca
infraestrutura nem código de app. Escrever lá não abre exceção para o resto da lista de leitura.

Leitura apenas — proponha o diff e aguarde pedido explícito: código dos apps (`<app>/app/`, com
Dockerfile e compose), IaC/Terraform, `.github/workflows/`, `healthcheck.sh`, `.agents/`.
Enquanto ensinando, a skill sobrepõe o escopo: zero escrita em qualquer lugar.

Padrão da casa: containers non-root e nenhuma porta de serviço publicada em `0.0.0.0/0` — vale para os 3 apps.

## Skills e routing

1. **Startup:** invoque `using-superpowers` antes da primeira resposta. Terminado quando o
   framework de skills está carregado.
2. **Roteador:** o usuário digita `/ask-matt` quando precisa saber qual skill usar. O agente não
   invoca.
3. **Dúvida pontual** ("dúvida", "explica", "como funciona", "não entendi") → `teach-anything`
   automaticamente: resposta `EXPLICAÇÃO` no chat, read-only, nenhum arquivo tocado (exceto
   `.md` via `consolidate`).
4. **Curso longo:** o usuário digita `/teach` — nunca o agente (a skill é `disable-model-invocation`).
   Modo curso grava só em `.learning/<missão>/`.
5. **Grilling:** com `grilling`/`grill-me`/`grill-with-docs`, pergunte sempre pela tool
   `question` — nunca em texto plano na resposta.

## Trilha ≠ tecnologia

Cada linha do board descreve **o que a Issue resolve**, não a ferramenta que ela usa. Linux, Docker,
CI/CD e observabilidade se repetem em cada app porque cada trilha precisa atravessá-los por conta
própria; quando uma trilha já resolveu, as outras referenciam em vez de repetir. Nenhuma Issue
antecipa tecnologia cujo problema ela não resolve.
