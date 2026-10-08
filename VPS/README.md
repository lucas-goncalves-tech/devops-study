# NoteMaster - Backend API

Esta é a API RESTful em Java 17 + Spring Boot 3.5.11 que suporta o NoteMaster.

## 🚀 Quick Start

**1. Instale o Banco de Dados e Redis**
Para facilitar o ecossistema e Rate Limiting, utilize o Docker Compose da raiz do projeto:
```bash
docker compose up -db
```

**2. Compile e Inicie a API**
```bash
# Baixa as dependências e roda os testes
mvn clean install

# Roda o servidor na porta 8080 (O contexto padrão é /api/v1)
mvn spring-boot:run
```

## ✨ Features do Backend

- **Autenticação JWT:** Validação robusta de Tokens com `java-jwt` da Auth0.
- **Armazenamento Seguro:** Acesso e escrita a dados no PostgreSQL, orquestrado e migrado via Flyway.
- **Bucket4J Rate Limiting:** Limita requisições abusivas centralizando caches de limite via Jedis.
- **Documentação de API OpenAPI (Swagger):** Auto-gerada e acessível nativamente.

## ⚙️ Configuração

A configuração reside em `application.yml`. Caso deseje sobreescrever:

| Variável | Descrição | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Porta de rodízio da API | 8080 |
| `SPRING_DATASOURCE_URL` | URL JDBC da database | jdbc:postgresql://localhost:5432/notes_db |
| `SPRING_REDIS_HOST` | Host do Rate Limiter Caching | localhost |
| `JWT_SECRET` | Chave secreta de assinar os Tokens | `secret` |

## 📚 API Reference

**URL Base:** `http://localhost:8080/api/v1`

### Rotas de Autenticação (`/auth`)
- `POST /auth/register`: Cria uma nova conta de usuário. Retorna tokens de acesso e refresh.
- `POST /auth/login`: Autentica o usuário existente. 
- `POST /auth/refresh`: Consome um refresh token valido e emite um novo par JWT de acesso.

### Rotas de Notas (`/notes`) - *Requer Token Automático JWT (Bearer)*
- `GET /notes`: Lista todas as anotações encadeadas ao usuário.
- `GET /notes/{id}`: Recupera métricas e o payload de uma nota específica.
- `POST /notes`: Registra uma nova nota.
- `PATCH /notes/{id}`: Modifica uma nota persistente (conteúdo, título ou finalização).
- `DELETE /notes/{id}`: Exclui irrevogavelmente a nota.

## 🗃 Containerização e Testes
O projeto usa o `TestContainers` para testar repositórios limpos sem poluir sua máquina real. Testes são rodados e validados no ciclo do Maven usando Junit Jupiter.

```bash
# Executando um único teste
mvn test -Dtest=NoteControllerTest
```

## 📄 Licença

Aberto sob GPL-3.0 License.
