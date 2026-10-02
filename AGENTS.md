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
├── .github/             # CI (construção do usuário)
├── .agents/skills/      # Matt Pocock engineering skills
└── .agents/memory/      # memória persistente — o que já foi aprendido
```

Antes de mexer num app, abra o `AGENTS.md` dele (`ledger-service/AGENTS.md`): é onde vive a
arquitetura, as variáveis de ambiente e o contrato de infra que este arquivo não repete. O
`README.md` do app diz como rodar; `issues/` e `estudos/` são o trabalho.

## Apps, trilhas e estágio

| App | Trilha (ordem da sequência) | Estágio |
|---|---|---|
| `ledger-service` · 9 Issues | VPS — `linux → hardening → caddy → backups → isolamento → deploy → incidente` | `01`,`02` `done`; `03` a entrar |
| `commerce-api` · 10 Issues | AWS — `linux → compose → terraform → CI → observabilidade → S3/EC2 → apply → deploy` | `01` a entrar; `06` `parked` |
| `webhook-gateway` · 12 Issues | DevSecOps — `pipeline → secrets → SAST → SCA → hardening → gates → DAST → mensageria → integração` | `01` a entrar |

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

## Memória

`.agents/memory/` guarda o que já foi aprendido — é o que faz uma sessão nova saber o que você sabe
mesmo que o tracker suma. Formato, taxonomia e o que nunca salvar: `.agents/skills/memory-system/`.

- **Startup:** antes da primeira resposta, leia `.agents/memory/MEMORY.md` e aplique o que for
  relevante em silêncio. Recite só se perguntado.
- **Issue pronta:** antes de virar `status: done`, grave em `.agents/memory/progress.md` o que se
  aprendeu até aquele ponto — o que a próxima Issue reusa (comandos, caminhos), o que travou e como
  resolveu, decisões e o porquê — e acrescente a linha no índice `MEMORY.md`. Issue fechada sem essa
  entrada está incompleta.
- **Pedido explícito** ("guarda isso", "não esquece", "lembra disso") → skill `/remember`.

## Escopo de escrita

Gravável: `ledger-service/`, `commerce-api/`, `webhook-gateway/`, `BOARD.md`,
`00-visao-geral.md`, `README.md`, `AGENTS.md`, `archive/`, `.agents/memory/` (só a regra de
Memória escreve lá).

Leitura apenas — proponha o diff e aguarde pedido explícito: código dos apps (`<app>/app/`, com
Dockerfile e compose), IaC/Terraform, `.github/workflows/`, `<app>/scripts/`, `.agents/`
(exceto `.agents/memory/`).
Enquanto ensinando, a skill sobrepõe o escopo: zero escrita em qualquer lugar.

Padrão da casa: containers non-root e nenhuma porta de serviço publicada em `0.0.0.0/0` — vale para os 3 apps.

## Skills e routing

1. **Startup:** invoque `using-superpowers` antes da primeira resposta. Terminado quando o
   framework de skills está carregado.
2. **Roteador:** o usuário digita `/ask-matt` quando precisa saber qual skill usar. O agente não
   invoca.
3. **`/teach` só quando digitado.** A skill `teach` (curso longo, workspace stateful em
   `.learning/<missão>/`) entra apenas se a mensagem do usuário contiver `/teach`. Pedido de
   ensino em prosa — "me ensina X", "explica", "próxima issue" — segue com a `teach-anything`,
   que tem gatilhos próprios. `consolidate` só dentro de um modo de ensino já ativo.
4. **Grilling:** com `grilling`/`grill-me`/`grill-with-docs`, pergunte sempre pela tool
   `question` — nunca em texto plano na resposta.

## Trilha ≠ tecnologia

Cada linha do board descreve **o que a Issue resolve**, não a ferramenta que ela usa. Linux, Docker,
CI/CD e observabilidade se repetem em cada app porque cada trilha precisa atravessá-los por conta
própria — repetir a capacidade é o estudo. O que outra trilha já construiu vale como leitura e
exemplo, **nunca como pré-requisito de uma Issue**. Nenhuma Issue antecipa tecnologia cujo
problema ela não resolve.

**Única exceção:** a integração de produção (`webhook-gateway/12`) entra na stack do `ledger` — é o
único `Requer` cross-app permitido no repo. E scripts de check e troubleshooting moram em
`<app>/scripts/` — um diretório por trilha, nunca na raiz, nunca compartilhado entre trilhas.
