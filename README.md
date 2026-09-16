# notes-service

Microserviço de estudo (notas/documentos pessoais) do [monorepo `workbox`](../README.md)
— **resource server**: valida os access tokens emitidos pelo
[`workbox-api`](../workbox-api/README.md) via introspecção remota (client credentials),
sem fluxo de login próprio e sem conhecer nenhum segredo de assinatura de JWT. Mesmo
padrão de auth do [`budget-service`](../budget-service/README.md).

Guarda conteúdo colado manualmente pelo usuário (migração de notas antigas de outro
app) — sem schema fixo por natureza, daí a escolha de **MongoDB** em vez de Postgres:
título e conteúdo livre, tags opcionais, sem relacionamento com outras entidades do
monorepo.

Também espelhado no [GitHub](https://github.com/juniiorliimatt/notes-service) — todo
push pro GitLab é replicado automaticamente via git hook. Ver
[README raiz](../README.md#espelho-no-github--git-hooks).

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem / runtime | Java 25 LTS (toolchain Gradle) |
| Framework | Spring Boot 3.5.16 |
| Build | Gradle 9.7.1 |
| Persistência | Spring Data MongoDB — schema livre por documento, sem migrations |
| Banco | MongoDB (dev/prod) |
| Segurança | Spring Security 6 (OAuth2 resource server, opaque token), valida token via introspecção remota no workbox-api |
| Documentação de API | springdoc-openapi (Swagger UI + contrato versionado) |
| Cobertura | JaCoCo |
| Testes | JUnit 5 + MockMvc (`@WebMvcTest`), Mockito (unitário do service) |

## Estrutura de pacotes

```
br.com.notes
├── config/            OpenAPI, Security (resource server)
├── controllers/       DocumentController
├── exceptions/        Exceções de domínio + handler global (RestExceptionHandler)
├── models/
│   ├── dto/           DTOs de entrada/saída
│   └── entities/      Document
├── repositories/      Spring Data MongoDB
└── services/          DocumentService
```

`Document`: `title`, `content` (texto livre colado pelo usuário), `tags` (opcional),
`ownerUsername` (nunca aceito do payload — sempre do usuário autenticado), `createdAt`/
`updatedAt` (auditoria automática via `@EnableMongoAuditing`). Todo acesso é escopado por
dono — buscar/editar/apagar documento de outro usuário devolve `404`, nunca `403` (não
revela existência do recurso alheio, OWASP API1:2023).

## Autenticação

Este serviço **não emite tokens** — confia nos access tokens emitidos por
`POST /api/v1/auth/login` no `workbox-api`, mas **não os decodifica localmente**: valida
cada um via introspecção remota (`POST /api/v1/auth/introspect` no `workbox-api`, com
client credentials HTTP Basic — `INTROSPECTION_CLIENT_ID`/`INTROSPECTION_CLIENT_SECRET`,
tem que bater com uma linha ativa em `workbox.api_clients`). Peça um token no
`workbox-api` e mande em `Authorization: Bearer <token>` aqui, como sempre.

## Rodando localmente

```bash
./gradlew bootRun                                          # profile dev, exige MongoDB local
./gradlew bootRun --args='--spring.profiles.active=test'   # sem dependência externa (endpoints web-only)
```

Sobe em `PORT` (default **8082** internamente no container; host-exposto na porta
**7055** via `docker-compose.yml` da raiz — próxima livre na faixa 7050+ do monorepo).

MongoDB local sobe via `docker-compose.yml` na raiz do monorepo, host-exposto na porta
**7054** — passe `MONGODB_URI=mongodb://localhost:7054/notes` pra rodar fora do Docker.

CORS: `cors.allowed-origins` (default `http://localhost:7053,http://127.0.0.1:7053`,
mesma origem do `workbox-app` em dev) via Spring Security nativo.

## Contrato de API (OpenAPI)

`openapi/openapi.yaml` é o contrato REST versionado. Regenerar:

```bash
./gradlew generateOpenApiDocs
git diff openapi/openapi.yaml
```

## Convenção de commits

Sempre em português (pt-BR), Conventional Commits com o prefixo de tipo em inglês — regra
completa em [AGENTS.md](../AGENTS.md#convenção-de-mensagens-de-commit).

## Testes

```bash
./gradlew check
```

JUnit 5 + Spring Boot Test + MockMvc (`@WebMvcTest`, serviço mockado via `@MockitoBean`),
autenticação simulada via `SecurityMockMvcRequestPostProcessors.opaqueToken()`.
`DocumentServiceTest` cobre a lógica de escopo por dono com `Mockito` mockando o
repositório. Ainda **sem IT contra Mongo real via Testcontainers** — próximo passo
natural quando a superfície de query crescer além do CRUD básico.
