# notes-service — instruções do serviço

> Complementa o [`CLAUDE.md` da raiz](../CLAUDE.md) (visão do monorepo, contrato, commits,
> infra) e as regras globais de `~/.claude/CLAUDE.md`. Aqui só o que é específico deste
> serviço. Detalhes de uso: [`README.md`](README.md).

## Papel e autoria
- Notas/documentos pessoais — conteúdo colado manualmente pelo usuário (migração de notas
  antigas), **sem schema fixo**, sem relação com outras entidades. Por isso é o **único
  serviço em MongoDB** (os demais são Postgres). Não force relacional aqui.
- *Resource server*: sem login nem emissão de token; valida o Bearer do `workbox-api` por
  introspecção remota (mesmo padrão do `budget-service`).
- Autoria: padrão do monorepo é o desenvolvedor implementar; aqui há **permissão total
  temporária** pro Claude (concedida em 2026-09-16, "como budget-service"). Detalhes e
  limites em [raiz → Divisão de responsabilidade](../CLAUDE.md#divisão-de-responsabilidade).

## Stack e execução
- Java 25 LTS, Spring Boot 3.5.16, Gradle 9.7.1 (`./gradlew`), Spring Data MongoDB (sem
  migrations), Spring Security 6 (OAuth2 resource server, opaque token), springdoc,
  JaCoCo, Lombok.
- Porta: default `PORT=7055` na aplicação; no container é **8082**, exposta no host em
  **7055** pelo `docker-compose.yml`. Profiles: `dev` (default), `prod`, `test` (sem
  dependência externa — só endpoints web, ver testes).
- MongoDB do compose: host **7054**, auth obrigatória (`root`/senha do `.env`,
  banco lógico `notes`). **`authSource=admin` é obrigatório** na URI
  (`MONGODB_URI=mongodb://root:<senha>@localhost:7054/notes?authSource=admin`) — sem ele a
  autenticação falha mesmo com credencial certa. As credenciais só são criadas em volume
  vazio (`.mongodata/`); em volume antigo, criar o usuário à mão (README).
- Comandos: `./gradlew bootRun`, `./gradlew check`, `./gradlew generateOpenApiDocs`.

## Estrutura (`br.com.notes`)
`config/` (`SecurityConfig`, `WorkboxTokenIntrospector`, `MongoConfig`, OpenAPI) ·
`controllers/DocumentController` · `exceptions/` (+ `handler/`) ·
`models/{dto, entities/Document}` · `repositories/` · `services/DocumentService`.
Rota única: `/api/v1/documents` (`GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`).

## Regras e armadilhas
- **Escopo por dono**: `Document.ownerUsername` vem **sempre do usuário autenticado**,
  nunca do payload. Buscar/editar/apagar documento de outro usuário devolve **`404`, não
  `403`** (não revela existência — OWASP API1:2023). Todo método novo do service/
  repositório deve filtrar por dono.
- `@EnableMongoAuditing` fica em `MongoConfig`, **separado da classe principal de
  propósito**: na aplicação principal ele quebra `@WebMvcTest` (o slice não tem
  `mongoMappingContext`). Não mover de volta.
- `tags` é lista opcional (default vazia); `content` é texto livre potencialmente grande —
  atenção a arrays/documentos ilimitados e a índices ao introduzir busca (hoje não há
  índice de texto; só criar índice alinhado à query real).
- Auth: `WorkboxTokenIntrospector` + `introspection.*` (client `notes-service` em
  `workbox.api_clients`, seed `260916_0000_seed_api_clients_notes_service.sql` no
  `workbox-api`). Não decodificar JWT localmente.
- CORS: `cors.allowed-origins` (default `http://localhost:7053`) via Spring Security.
- O profile `dev` expõe `stacktrace`/`exception` nas respostas de erro
  (`server.error.include-*`) — conveniência local; `prod` não. Não copiar pra `prod`.
- CI com `test` → `contract-drift-check` → `build`; ainda sem Sonar nem Cucumber. Ao evoluir, alinhar ao padrão do
  `workbox-api`/`budget-service`.

## Convenção Java deste repo
- Em `workbox-api`/`budget-service` vale a regra "`final` em todo parâmetro e variável
  local"; **aqui o código existente também a segue** (ex.: `DocumentService`) — mantenha
  em código novo por consistência.
- Javadoc/mensagens de negócio em português; Lombok permitido.

## Testes (test-first)
- JUnit 5 + MockMvc `@WebMvcTest` (`DocumentControllerTest`, serviço via `@MockitoBean`,
  auth com `opaqueToken()`) e Mockito no service (`DocumentServiceTest` — lógica de
  escopo por dono, repositório mockado).
- **Não há IT contra Mongo real.** Próximo passo natural (Testcontainers MongoDB) assim que
  houver query além do CRUD; adicionar dependência exige confirmação (global §3).

## Contrato (OpenAPI)
`openapi/openapi.yaml` versionado. Mudou rota/DTO/auth → `./gradlew generateOpenApiDocs`
e commitar junto (o `contract-drift-check` do CI falha se divergir).
O front ainda não consome este serviço (nem `vite.config.ts` nem `nginx.conf.template`
roteiam `/api/v1/documents`).

## Commits
pt-BR, Conventional Commits, conforme o
[CLAUDE.md da raiz](../CLAUDE.md#convenção-de-mensagens-de-commit). Trabalhar em
`develop`; push só com confirmação.
