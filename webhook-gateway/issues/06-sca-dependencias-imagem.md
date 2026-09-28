---
aliases: [issue-06, sca-dependencias-imagem]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 06 — SCA em duas camadas: `npm audit` nas dependências e Trivy na imagem Docker

## Contexto

O SAST (Issue 05) enxerga o código que o time escreveu, mas não enxerga o que o time herdou: `package-lock.json` deste app fixa `ioredis`, `dotenv`, `typescript` e `vitest` em versões específicas, e a imagem construída a partir deles carrega o sistema operacional base e as dependências transitivas. Uma CVE chega por dois lados e nenhum deles é o arquivo que a gente editou. Pior: CVE em dependência transitiva de uma imagem só aparece quando alguém olha a imagem — e, sem piso de severidade declarado, o relatório vira lista de 300 itens que ninguém lê e o verde não significa nada.

## Objetivo

Estado final: dois gates de SCA rodando com piso de severidade declarado — `npm audit` sobre o lockfile com `--audit-level` que reprova, e Trivy sobre a imagem construída com falha em CVE alta ou crítica —, ambos executáveis localmente pelo mesmo comando que o gate usa, e uma baseline documentada que separa o que foi aceito do que bloqueia.

## Dependências

- Requer Issue 02 — a imagem que o Trivy varre nasce com o `Dockerfile` deste app
- Requer Issue 03 — o gate roda dentro de uma pipeline, e a lógica de build fica nos scripts que a Issue 03 define

## Escopo

- Gate de dependências com `npm audit` sobre o lockfile e piso de severidade explícito
- Gate de imagem com Trivy, filtrando por severidade e falhando em alta/crítica
- Mesmo comando rodando local e no gate, sem script paralelo
- Baseline documentada: CVE aceito com justificativa, e como a baseline é revista
- Correção de CVE real encontrada (subir versão, trocar dependência ou remover), com a reprovação antes e o verde depois

## Fora de escopo

- SAST sobre o código do app — [Issue 05](05-sast-semgrep.md)
- Detecção de segredo — [Issue 04](04-secrets-hygiene.md)
- Consolidação dos gates e medição de tempo — [Issue 08](08-devsecops-gates.md)
- SBOM e assinatura de artefato: a imagem assinada é trabalho do pipeline endurecido, [Issue 07](07-pipeline-hardening.md)
- Alertas sobre vulnerabilidade em runtime (imagem já em produção) — fora do gate de merge

## Conhecimentos envolvidos

- SCA: CVEs, CVSS, dependência direta e transitiva, e o que o lockfile fixa
- `npm audit` e `--audit-level`, saída JSON e código de saída
- Trivy: registry, filesystem e imagem, severidade e `--exit-code`
- Ruído de SCA: CVE sem caminho de exploração real versus CVE em código rodando
- Baseline: lista de exceções com justificativa, dono e data de revisão

## Estado atual

- `package-lock.json` existe e fixa as versões, mas ninguém roda auditoria sobre ele
- Não existe `Dockerfile` neste app (Issue 02) e portanto não existe imagem para varrer
- Nenhum piso de severidade declarado: sem ele, qualquer escolha de falhar é discutível caso a caso
- Nenhuma baseline: um CVE herdado e não explorável travaria o merge no dia em que a ferramenta aparecer

## Resultado esperado

- Dependência com CVE alta reprova o merge, no lockfile e na imagem
- O mesmo comando roda na máquina de quem desenvolve e no gate
- Toda exceção de baseline tem justificativa escrita e revisão

## Requisitos

- [ ] Definir e escrever o piso de severidade que falha (alta e crítica) e o que só alerta (baixa e moderada), em arquivo versionado
- [ ] Rodar `npm audit` com `--audit-level` correspondente ao piso, sobre o lockfile, com código de saída não zero no caso de bloqueio
- [ ] Escanear a imagem construída deste app com Trivy, filtrando por severidade e com `--exit-code 1` no caso de bloqueio
- [ ] Expor a saída de cada gate em formato legível por máquina e legível por humano, com o nome da CVE e o pacote afetado
- [ ] Fazer o comando local e o passo do gate serem a mesma coisa (o script da Issue 03 é chamado nos dois casos)
- [ ] Gerar e versionar a baseline de CVEs aceitos, cada entrada com CVE, pacote, caminho de dependência, justificativa e data de revisão
- [ ] Documentar como a baseline é revista e o que acontece quando um CVE aceito é corrigido na dependência
- [ ] Corrigir ao menos um CVE real encontrado, com a reprovação do gate antes da correção e o verde depois

## Critérios de aceitação

- [ ] `npm audit` com o piso declarado reprova a execução quando o lockfile contém dependência com CVE alta ou crítica
- [ ] O scan Trivy da imagem reprova a execução quando a imagem contém CVE alta ou crítica, incluindo as que só existem no sistema base
- [ ] O mesmo comando executado localmente e no gate produz o mesmo veredito para o mesmo lockfile e a mesma imagem
- [ ] A baseline está versionada e cada entrada tem CVE, justificativa e data de revisão — nenhuma entrada sem justificativa
- [ ] Remover uma entrada da baseline que não foi corrigida faz o gate reprovar, provando que a baseline é o que segura a exceção
- [ ] A CVE corrigida aparece reprovando o gate antes da correção e verde depois, com as duas execuções registradas
- [ ] Uma CVE de severidade abaixo do piso aparece no relatório sem reprovar a execução

## Validação

- Executar o gate de dependências localmente e conferir o código de saída
- **Build to break:** instalar uma dependência com CVE alta conhecida e confirmar reprovação nos dois gates
- Construir a imagem e rodar o Trivy, conferindo que CVE do sistema base aparece
- Rodar o mesmo comando pelo caminho do gate e comparar o veredito
- Alterar a baseline (remover e reinserir uma entrada) e observar a diferença de veredito
- Corrigir a dependência e repetir os dois gates

## Evidências

- Saída de `npm audit` com a CVE, o pacote e o código de saída
- Saída do Trivy sobre a imagem, com CVE do sistema base e do npm
- Execução do gate na pipeline com o job responsável identificado
- Conteúdo da baseline versionada, com justificativas
- Par reprovação/verde em torno da CVE corrigida
- Comparação do veredito local contra o veredito do gate

## Limitações / notas

- **Piso de severidade sem contexto é ruído com poder de bloqueio:** a diferença entre CVE alta não explorável e CVE alta no caminho autenticado do app é a mesma do lado do CVSS e oposta do lado do risco — a baseline existe para essa distinção, não para esconder achado
- `npm audit` só enxerga o grafo do npm; dependência nativa instalada fora do lockfile escapa dele, e é por isso que o Trivy sobre a imagem não é redundante
- O sistema base da imagem (`node:20-alpine`) envelhece: CVE alta na base pode não ter correção na tag, e aí a decisão é trocar de tag, não afrouxar o piso
- Dependência de desenvolvimento (`vitest`, `typescript`) também aparece no relatório; decidir conscientemente se ela entra no piso é parte desta Issue, e o padrão é não bloquear por CVE que só existe em ferramenta de desenvolvimento
- Ajustar o piso para baixo é decisão registrada, não ajuste de expediente: qualquer mudança em `--audit-level` ou no filtro do Trivy tem de aparecer no histórico do repositório com o motivo
- Adaptar este gate para outro app é trocar o lockfile e a imagem; o `ledger-service` usaria o ecossistema Maven, não `npm audit`
