---
aliases: [issue-16, devsecops-gates]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 16 — Gates de segredos, SAST e SCA consolidados como barreira da pipeline multi-serviço

## Contexto

Os gates de conteúdo existem isolados na pipeline de serviço único. Com múltiplos serviços na composição, a superfície cresce e um único pipeline não pode deixar de verificar nenhuma dimensão — cada um precisa falhar de forma isolada e acionável.

## Objetivo

Estado final: os três gates (segredos, SAST, SCA) rodam em toda mudança, falham isoladamente com mensagem acionável, e o verde significa ausência de segredo, ausência de violação `ERROR` e ausência de CVE alta ou crítica — com o custo em minutos de cada gate medido.

## Dependências

- Requer Issue 13 — gate de segredos
- Requer Issue 14 — gate de SAST
- Requer Issue 15 — permissões mínimas e pinagem da pipeline que os hospeda
- Requer Issue 10 — pipeline base e gate de SCA por scan de imagem
- Requer Issue 07 — a pipeline passa a cobrir múltiplos serviços

## Escopo

- Três gates obrigatórios: segredos, SAST e SCA
- Falha isolada por gate com mensagem acionável
- Separação entre severidade que falha e severidade que só alerta
- Medição de tempo adicionado por gate e otimização com cache

## Fora de escopo

- Criação dos gates individuais — Issues 13, 14 e 15
- Deploy contínuo — Issue 17
- Monitoramento da aplicação — Issue 06
- Kubernetes e Cloud — Issues 18 e 12

## Conhecimentos envolvidos

- DevSecOps e posicionamento de gates no fluxo
- SCA: CVEs, dependências e triagem de ruído
- Severidade vs política de bloqueio
- Custo de pipeline e cache

## Estado atual

- A pipeline testa, mas não protege de forma consolidada
- Gates existem em módulos separados, sem visão única de verde
- Ninguém sabe quanto tempo cada gate adiciona

## Resultado esperado

- Cada gate falha isolado com mensagem acionável
- Verde significa: sem segredo, sem violação `ERROR`, sem CVE alta ou crítica
- Tempo de execução por gate conhecido e cache aplicado onde couber

## Requisitos

- [ ] Garantir o gate de segredos bloqueando vazamento em toda mudança
- [ ] Garantir o gate de SAST bloqueando padrões inseguros
- [ ] Garantir o gate de SCA bloqueando CVEs altas e críticas
- [ ] Garantir que cada gate falha isolado, com mensagem acionável
- [ ] Diferenciar severidade que falha de severidade que só alerta
- [ ] Medir o tempo adicionado por gate e otimizar com cache

## Critérios de aceitação

- [ ] Uma mudança com segredo, uma com violação `ERROR` e uma com CVE crítica reprovam a pipeline em jobs distintos e identificáveis
- [ ] Um problema em um gate não mascara a execução dos outros
- [ ] Pipeline verde implica ausência de segredo, de violação `ERROR` e de CVE alta ou crítica
- [ ] O tempo de execução de cada gate é medido e registrado

## Validação

- **Build to break:** introduzir separadamente segredo, padrão `ERROR` e dependência com CVE crítica, confirmando falha em cada caso; reverter
- Verificar que os jobs permanecem independentes na saída
- Conferir a saída da pipeline verde com os três gates ativos
- Ler a medição de tempo por gate

## Evidências

- Três execuções falhando, uma por gate, com o job responsável identificado
- Saída da pipeline verde com os três gates
- Registro dos tempos de execução por gate

## Limitações / notas

- Os gates individuais vêm prontos das Issues 13, 14 e 15 — esta Issue consolida e sintoniza, não recria
- Sintonizar para baixo a severidade bloqueante sem registrá-lo transforma o verde em ilusão: qualquer afrouxamento deve aparecer como decisão registrada
- Os critérios de "mensagem acionável" e "severidade que alerta" precisam de artefato observável na saída do gate, não de descrição
- Esta Issue é pré-requisito da Issue 17, que exige os gates como barreira antes do deploy
