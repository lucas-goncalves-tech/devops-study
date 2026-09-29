---
aliases: [estudo-10]
tags: [estudo]
issue: 10
---

# Estudos — Issue 10: Deploy em Computação Real com Rollback

> Material de apoio da Issue 10. Não é escopo da Issue — a `teach-anything` lê este arquivo para montar a sessão de ensino antes da implementação.


### A — Registro de imagens e versão imutável

- Tag, digest e por que deploy usa digest
  - https://docs.docker.com/registry/spec/api/#content-digest
  - https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry
- Publicar imagem a partir da pipeline
  - https://docs.github.com/en/actions/publishing-packages/publishing-docker-images

**FIM:** sei explicar por que a tag pode mudar e o digest não.

---

### B — Deploy remoto sem credencial em log

- Chave efêmera e segredo de environment
  - https://docs.github.com/en/actions/deployment/managing-environments/about-environments
  - https://docs.github.com/en/actions/security-for-github-actions/security-hardening-your-deployments
- Pull do serviço no destino
  - https://docs.docker.com/compose/reference/pull/
  - https://docs.docker.com/compose/how-tos/start-services/

**FIM:** sei dizer onde a chave vive e por que ela não aparece no log.

---

### C — Healthcheck como gate e rollback

- Healthcheck do serviço e semântica do `/health`
  - https://docs.docker.com/reference/compose-file/services/#healthcheck
- Rollback por digest anterior
  - https://developer.hashicorp.com/terraform/tutorials/cli/state

**FIM:** sei justificar por que a reversão é a resposta certa a um healthcheck que falhou.
