---
aliases: [issue-11, staging-inseguro]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 11 — Staging inseguro de propósito, fora da produção, com forense de mensageria Redis

## Contexto

Gates verdes provam que o repositório está limpo — não provam que o pipeline de verdade pega o que importa. A prova de um gate é a tentativa de passar por ele com algo que não deveria passar, e até aqui cada gate foi exercitado contra arquivo do repositório, nunca contra um ambiente inteiro montado errado. Some-se a isso a parte de mensageria: o consumidor deste app lê a Stream `payment-events`, cria o consumer group `webhook-dispatcher-group` e valida assinatura HMAC-SHA256 com `crypto.timingSafeEqual` (`src/signer.ts`), e nenhuma dessas garantias foi testada contra Redis fora do ar, stream atrasada ou assinatura inválida. Este ambiente é deliberadamente falho, é público no repositório e nunca toca a stack de produção.

## Objetivo

Estado final: um ambiente de staging separado da produção, com fraquezas propositais registradas (credencial fraca, porta exposta, segredo no artefato, padrão inseguro no código, dependência e workflow vulneráveis) e uma matriz que mostra qual gate pega cada fraqueza; e três incidentes de mensageria reproduzidos com diagnóstico escrito, causa, correção e reexecução.

## Dependências

- Requer Issue 04 — gate de segredos, que é quem tem que pegar o segredo no artefato
- Requer Issue 05 — gate de SAST, que é quem tem que pegar o padrão inseguro no código do staging
- Requer Issue 06 — gate de SCA, que é quem tem que pegar a dependência e a imagem com CVE
- Requer Issue 07 — hardening da pipeline, que é quem tem que pegar a action por tag mutável e a permissão ampla
- Requer Issue 08 — gates consolidados, que é onde as reprovações precisam aparecer em jobs distintos
- Requer Issue 09 — DAST, que varre o staging falho e é a evidência de que a porta exposta é visível de fora
- Requer Issue 10 — Redis Streams, consumer group e HMAC: a forense de mensageria só existe depois que a stack de mensageria está montada

## Escopo

- Ambiente de staging separado por credencial, dados, porta e URL, sem recurso compartilhado com a produção
- Fraquezas propositais registradas com o gate esperado para pegar cada uma
- Matriz de detecção: fraqueza → gate → saída que mostra a reprovação
- Rejeição documentada de cada tentativa de passar, com a evidência do gate acionado
- Três incidentes de mensageria: Redis fora do ar, stream atrasada, assinatura HMAC inválida
- Diagnóstico escrito por incidente: sintoma, métrica ou log que revelou, causa, correção e reexecução
- Baseline de atraso de stream e limite de reprocessamento declarados

## Fora de escopo

- Teste de intrusão de terceiros ou pentest contratado — a lista de fraquezas é conhecida e escolhida aqui
- Corrigir o staging: ele precisa continuar falho enquanto a matriz não estiver completa
- Escalar as fraquezas para produção em qualquer hipótese
- Chaos engineering distribuído e biblioteca de experimentos
- Kubernetes e Terraform novos: o ambiente nasce do que as Issues anteriores já provisionam

## Conhecimentos envolvidos

- Ambiente de teste como isca conhecida: o que é aceitável quebrar e o que nunca pode ser quebrado
- Gate de segredo: o que é baseline legítima e o que é vazamento em artefato de build
- SAST em JavaScript/TypeScript e SCA em `package-lock.json` e imagem
- Hardening de pipeline: tag mutável versus SHA imutável, permissão de escrita por job
- Modos de falha de Redis Streams: conexão perdida, atraso de consumer group (`XPENDING`/`XINFO`), mensagem inválida
- HMAC: verificação em tempo constante, rejeição de assinatura inválida e efeito sobre retry

## Estado atual

- Os gates das Issues 04 a 08 estão especificados e nenhum foi exercitado contra um ambiente montado errado — só contra arquivos do repositório
- Não há relatório de DAST apontando para nenhum alvo falho; o baseline da Issue 09 aponta para o serviço saudável
- A stack de mensageria (Redis, Stream, consumer group, HMAC) só existe depois da Issue 10 e seus modos de falha nunca foram exercitados
- Nenhum incidente de mensageria foi diagnosticado por escrito neste app

## Resultado esperado

- Ambiente falho identificável como staging, fora da produção, com as fraquezas documentadas
- Cada fraqueza com um gate que a reprova, com a saída do gate arquivada
- Três incidentes de mensageria diagnosticados, corrigidos e reexecutados sem reincidência

## Requisitos

- [ ] Provisionar staging com URL, porta, credencial de banco/Redis, volume e namespace próprios, sem recurso compartilhado com a produção
- [ ] Injetar e documentar credencial fraca (segredo `WEBHOOK_SECRET` curto ou senha trivial no `.env` do staging)
- [ ] Injetar e documentar porta exposta do serviço de staging em endereço público, com a exposição visível por varredura externa
- [ ] Injetar e documentar segredo real em artefato de build versionado (por exemplo string de conexão com segredo dentro de um arquivo que a imagem consome)
- [ ] Injetar e documentar padrão inseguro no código do staging detectável por SAST (por exemplo comparação de HMAC que deixa de ser em tempo constante)
- [ ] Injetar e documentar dependência com CVE alta no staging e verificar que o gate de SCA reprova
- [ ] Injetar e documentar fragilidade de pipeline do staging (ação de terceiros por tag mutável e permissão de escrita ampla no job que publica o staging)
- [ ] Montar a matriz de detecção (fraqueza → gate → trecho de saída que reprovou) e arquivá-la no repositório
- [ ] Provocar Redis fora do ar: derrubar o serviço ou cortar a conexão, observar o consumidor e registrar o diagnóstico
- [ ] Provocar atraso de stream: publicar evento sem consumidor ou segurar o processamento, e medir o atraso com `XINFO GROUPS`/`XPENDING`
- [ ] Provocar assinatura HMAC inválida: entregar payload com assinatura adulterada e confirmar a rejeição por `crypto.timingSafeEqual`
- [ ] Registrar para cada incidente de mensageria: sintoma, métrica ou log que revelou, causa raiz, correção e resultado da reexecução
- [ ] Declarar o baseline de atraso aceitável e o limite de reprocessamento, com o que acontece quando o atraso ultrapassa o limite
- [ ] Confirmar que a stack de produção permaneceu intacta durante toda a exercitação (revisão de log e ausência de evento publicado na produção)

## Critérios de aceitação

- [ ] O staging responde em URL e porta próprias, e nenhuma requisição nele toca recurso, credencial ou namespace da produção
- [ ] Existe matriz de detecção versionada, com uma linha por fraqueza injetada, o gate responsável e o trecho de saída que a reprovou
- [ ] Injetar o segredo no artefato reprova o gate de segredos; injetar o padrão inseguo reprova o SAST; injetar a dependência com CVE alta reprova o SCA — cada uma em job identificável e distinto
- [ ] A fragilidade de pipeline do staging (tag mutável, permissão de escrita) é reprovada pela auditoria de hardening
- [ ] Todas as reprovações acontecem no gate consolidado (Issue 08) em jobs distintos, sem um gate mascarar o outro
- [ ] Existe registro da exposição de porta visível por varredura externa, e o DAST da Issue 09 aponta para o mesmo alvo
- [ ] Redis fora do ar produz diagnóstico escrito: sintoma observado, sinal que revelou, causa e correção
- [ ] Stream atrasada é medida com `XINFO GROUPS`/`XPENDING` e o atraso resultante é comparado com o baseline declarado
- [ ] Assinatura HMAC inválida é rejeitada, com o registro da rejeição e sem reprocessamento infinito do payload
- [ ] Cada um dos três incidentes de mensageria tem reexecução registrada, com o comportamento esperado após a correção
- [ ] Nenhum incidente de mensageria foi encerrado por reinício do serviço sem registro de causa
- [ ] A stack de produção permaneceu intacta durante a exercitação, com log sem erro e sem evento de teste publicado nela

## Validação

- Consultar URL e porta do staging e da produção, confirmando a separação de dados e credencial
- Para cada linha da matriz: reintroduzir a fraqueza, rodar o gate correspondente e conferir a reprovação
- Rodar o gate consolidado com as fraquezas todas presentes e conferir que cada uma reprova em job separado
- Varrer a porta do staging de fora e confirmar que responde publicamente (e registrar o dado)
- Derrubar o Redis e observar o comportamento do consumidor e o log do app
- Publicar evento sem consumidor e medir o atraso com `XINFO GROUPS` e `XPENDING`
- Entregar payload com assinatura adulterada e observar a rejeição
- Conferir o log da produção durante toda a janela, procurando erro ou evento de teste

## Evidências

- URL e porta do staging e da produção lado a lado, com a prova de que não compartilham dado
- Conteúdo do staging, com as fraquezas injetadas e comentadas
- Matriz de detecção versionada, com o trecho de saída de cada gate
- Saída de reprovação de cada gate, uma por fraqueza detectada
- Saída do DAST (Issue 09) apontando para o staging falho
- Registro da varredura externa mostrando a porta exposta
- Três diagnósticos de incidente de mensageria, com sintoma, sinal, causa e correção
- Saída de `XINFO GROUPS`/`XPENDING` com o atraso medido e o baseline
- Log de rejeição do HMAC inválido, com a evidência de não haver retry infinito
- Log da produção sem erro e sem evento de teste

## Limitações / notas

- **O staging é falho por desenho e isso precisa estar escrito no próprio ambiente:** sem um arquivo que declara "este ambiente é deliberadamente inseguro", alguém acha que é produção e o leak deixa de ser didático
- Nem toda fraqueza é pega por gate de pipeline: exposição de porta e ausência de TLS são vista por varredura externa e pelo DAST, não pelo gitleaks ou pelo Semgrep. A matriz tem que registrar essa coluna explicitamente em vez de forçar um gate que não existe
- A porta exposta do staging deve estar exposta de verdade: prova de que o ambiente é alcançável é parte do objetivo, mas a exposição é para o endereço do staging, nunca para a interface de produção
- Correção de incidente de mensageria não é reiniciar o serviço: se a causa não está escrita, o critério de reexecução não é cumprido e a reincidência volta na próxima queda do Redis
- A assinatura HMAC inválida precisa ter destino: rejeitar e descartar é um caminho; rejeitar e reprocessar em loop é um incidente diferente, e é por isso que o limite de reprocessamento é declarado
- Depois de concluída a matriz, este ambiente não deve ser promovido nem reaproveitado como pré-produção: a fraqueza é o produto dele
- Adaptar esta prática para o `ledger-service` ou o `commerce-api` é repetir a matriz com as ferramentas do app (Semgrep `p/java`, SCA Maven, Trivy na imagem); o desenho é o mesmo
