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
| `commerce-api` · 8 Issues | AWS — `linux → compose → terraform → CI → observabilidade → S3/EC2` | `01` a entrar; `06` `parked` |
| `webhook-gateway` · 11 Issues | DevSecOps — `pipeline → secrets → SAST → SCA → hardening → gates → DAST → mensageria` | `01` a entrar |

**Status e ordem de execução → `BOARD.md`:** consulte quando precisar saber o que está feito e o que
entra em seguida. **Metodologia, pilares e política de status → `00-visao-geral.md`:** consulte antes
de escrever ou fechar qualquer Issue. A numeração reinicia em `01` por app: `ledger 05` e
`commerce 05` são Issues diferentes.

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
`00-visao-geral.md`, `README.md`, `AGENTS.md`, `archive/`.

Leitura apenas — proponha o diff e aguarde pedido explícito: código dos apps (`<app>/app/`, com
Dockerfile e compose), IaC/Terraform, `.github/workflows/`, `healthcheck.sh`, `docs/`, `.agents/`.
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
