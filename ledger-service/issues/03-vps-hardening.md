---
aliases: [issue-03, vps-hardening]
tags: [tracker, issue, todo, study-needed]
status: todo
prioridade: alta
---

# Issue 03 — VPS endurecida com acesso só por chave, firewall mínimo e ban de brute-force

## Contexto

O próximo passo do sistema é sair da máquina local para um servidor barato de produção. Um servidor recém-criado vem com root por senha, portas abertas e nenhuma proteção contra força bruta — exposto à internet, isso é inaceitável antes de publicar qualquer serviço.

## Objetivo

Estado final: servidor com login exclusivamente por chave SSH, firewall liberando apenas o necessário, ban automático contra tentativas repetidas e swap configurado para sobreviver a estouro de memória.

## Dependências

- Nenhuma dependência de outra Issue — primeira Issue da trilha que exige um servidor.

## Escopo

- Usuário operacional com sudo e desativação de login por senha
- Firewall restrito a SSH, HTTP e HTTPS
- Proteção contra brute-force com ban temporário
- Swap anti-OOM e auditoria de portas abertas
- Preparo do servidor para a stack: runtime de contêiner instalado, stack copiada e primeira subida saudável

## Fora de escopo

- Reverse proxy, TLS e publicação da API — Issue 04
- Isolamento de redes do Compose e limites de recursos — Issue 06
- Backup do banco — Issue 05
- Monitoramento e alertas — [Issue 08](08-trafego-sintetico-alertas.md) desta trilha
- Kubernetes, Cloud e Terraform

## Conhecimentos envolvidos

- Hardening de SSH
- Firewall baseado em estado (UFW)
- Fail2ban e benchmarks CIS
- Swap e limites de memória em Linux

## Estado atual

- Acesso como root com senha e superfície de ataque aberta
- Portas expostas sem filtro e sem mecanismo de ban
- Sem swap: estouro de memória derruba o processo

## Resultado esperado

- Senha recusada no login; só chave SSH autentica
- Scan externo enxerga apenas as portas essenciais
- Tentativas repetidas de força bruta geram ban registrado em log
- Memória em estouro recorre a swap em vez de matar o processo

## Requisitos

- [ ] Criar usuário operacional com sudo, sem senha direta para root
- [ ] Desativar login por senha, exigir só chave SSH
- [ ] Ativar firewall liberando apenas SSH, HTTP e HTTPS
- [ ] Ativar ban temporário contra brute-force
- [ ] Configurar swap anti-OOM
- [ ] Auditar portas abertas e fechar o que não for público
- [ ] Preparar o servidor para receber a stack: instalar o Docker, copiar a stack da Issue 02 e subir com healthcheck verde

## Critérios de aceitação

- [ ] Tentativa de login com senha é recusada e login com chave autentica
- [ ] Todo firewall lista exatamente SSH, HTTP e HTTPS; nenhuma outra porta aceita conexão externa
- [ ] Após tentativas de senha repetidas, o IP é banido e o ban aparece no log
- [ ] `free -h` mostra swap ativo e o processo sobrevive a consumo de memória além da RAM
- [ ] A stack copiada sobe no servidor preparado e /actuator/health responde 200 UP a partir dele

## Validação

- Tentativa de `ssh` com senha esperada falhar; a mesma conexão com chave deve funcionar
- Varredura de portas de fora do servidor comparada com a lista do firewall
- Sequência de tentativas de senha falhas seguida de inspeção do log do mecanismo de ban
- Leitura de `free -h` e teste de consumo de memória
- Subir a stack copiada no servidor e consultar `/actuator/health`

## Evidências

- Output da tentativa de login por senha (recusada) e por chave (aceita)
- Lista de regras do firewall
- Linha de log com o ban aplicado
- Output de `free -h` mostrando swap configurado
- Saída de `docker compose ps` com serviços `healthy` e do `curl` de `/actuator/health` no servidor

## Limitações / notas

- **Não precisa de VPS pública para começar.** Esta Issue roda inteira numa VM local (VirtualBox/UTM/libvirt com Ubuntu ou Debian): SSH por chave, firewall, `fail2ban` e swap não exigem IP público. VPS pública só é necessária para provar acesso externo real — e nesse caso uma VPS temporária resolve.
- Um servidor real (VPS ou VM) com acesso inicial por provedor é pré-requisito de ambiente, não de código
- O firewall abre HTTP e HTTPS em antecipação à Issue 04; nesses instantes as portas 80/443 não têm serviço atrás e devem ser reavaliadas ao fechar esta Issue
- Este contrato de acesso (`só chave SSH`) é pré-requisito do deploy por chave efêmera da Issue 07
