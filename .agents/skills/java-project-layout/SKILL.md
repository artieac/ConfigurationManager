---
name: java-project-layout
description: Use this skill whenever working in this Java codebase — when adding new files, deciding where code belongs, reviewing placement of classes, or discussing architecture. Covers the layered architecture with data, domainmodel, services, and web layers. Trigger on questions like "where should I put this", "how is the code organized", "what layer does X belong in", or any time new Java classes need to be created.
version: 1.0.0
---

# Java Project Layout

This project is a Spring Boot application. All production code lives under:

```
backend/src/main/java/com/alwaysmoveforward/configurationmanager/
```

The code is organized into four primary layers: `data`, `domainmodel`, `services`, and `web`. Each layer has a strict role and dependencies only flow inward (web → services → domainmodel/data, never upward).

---

## Layer: `data/`

The data layer owns everything related to external systems — databases, third-party APIs, etc. Nothing outside this layer should know that these external systems exist.

### `data/Entities/`

JPA entity classes that mirror the database schema (or an external API contract). These are pure data transfer objects — they carry no business logic.

**Rule:** Entities are private to the data layer. No class outside `data/` should ever import or reference an Entity.

**Existing entities:** `RoleEntity`, `UserEntity`, `SystemEntity` (created-only — see `SystemHistoryEntity`), `EnvironmentEntity`, `SecretEntity` (a name only — see `SecretValueEntity`), `SecretValueEntity` (the encrypted value for one secret in one environment; created-only — see `SecretValueHistoryEntity`), `SecretValueHistoryEntity`, `SystemHistoryEntity`, `ApiKeyEntity` (machine credential scoped to one system — stores only `tokenHash`, never the raw token), `Auth0TokenResponseEntity`, `Auth0UserInfoResponseEntity`

**Created-only pattern:** `SystemEntity` and `SecretValueEntity` track only `createdBy`/`createdAt` — no `updatedBy`/`updatedAt`. Both have a dedicated append-only history table (`SystemHistoryEntity`/`SecretValueHistoryEntity`) that records every create/update/delete with who and when, so a separate "last updated" stamp on the live row would just duplicate that. `EnvironmentEntity` and `SecretEntity` (name-only) still track both `created`/`updated`, since nothing records their renames in a history table.

### `data/repositories/`

Repository classes that perform all I/O against external systems. Repositories translate between the external world (Entities or raw API responses) and the domain (Domain Model objects or primitives).

**Rules:**
- Method parameters must be primitive/base types or Domain Model objects — never Entities.
- Return values must be primitive/base types or Domain Model objects — never Entities.
- Repositories use the `data/mapper/` classes internally to convert between Entities and Domain Models before returning results.
- External API integrations (e.g., `Auth0Repository`) live here alongside database repositories.

**Existing repositories:** `RepositoryBase`, `UserRepository`, `SystemRepository`, `EnvironmentRepository`, `SecretRepository`, `SecretValueRepository`, `SecretValueHistoryRepository`, `SystemHistoryRepository`, `ApiKeyRepository` (also the only place that hashes-and-looks-up a presented token — see `security/apikey/ApiKeyAuthenticationFilter`), `Auth0Repository`

### `data/mapper/`

Mapper classes that define how to convert between Entities and Domain Models. Used internally by repositories — callers of repositories never invoke mappers directly.

**Existing mappers:** `UserMapper`, `SystemMapper`, `EnvironmentMapper`, `SecretMapper`, `SecretValueMapper`, `SecretValueHistoryMapper`, `SystemHistoryMapper`, `ApiKeyMapper`

### `data/dao/`

Spring Data JPA DAO interfaces (extending `JpaRepository` or similar) used by the repository implementations to execute queries.

**Existing DAOs:** `RoleDAO`, `UserDAO`, `SystemDAO`, `EnvironmentDAO`, `SecretDAO`, `SecretValueDAO`, `SecretValueHistoryDAO`, `SystemHistoryDAO`, `ApiKeyDAO`

---

## Layer: `domainmodel/`

Domain Model objects are the core business objects of the system, following Domain-Driven Design principles.

**Rules:**
- Business logic that belongs to a single entity belongs here as methods on the Domain Model class.
- Domain Models know nothing about persistence, HTTP, or external systems.
- Prefer putting business rules on Domain Models over putting them in services.

**Existing domain models:** `User`, `Role` (enum: READ_ONLY/READ_WRITE/ADMIN — user roles only; unrelated to the `ROLE_API_CLIENT` authority API keys authenticate with), `UserRights` (permission policy object), `SecretSystem` (named to avoid shadowing `java.lang.System`; created-only; carries both `name` — freely renameable — and `externalId` — a separate, URL-safe identifier used by machine/API-key lookups, defaults to `name` at creation, editable afterward), `Environment` (a named deployment stage within a system; also carries its own `externalId`, same defaulting/editing rules as `SecretSystem`'s), `Secret` (a NAME scoped to a system — carries no value), `SecretValue` (the encrypted value a `Secret` holds within one `Environment`; sparse — not every secret has a value in every environment; created-only), `ApiKey` (a machine credential scoped to one system — never carries the token or its hash, both stay in `data/`), `SecretValueHistoryEntry`, `SystemHistoryEntry` (snapshots the system's full field set — name/externalId/description — at each change, not just the name, so the UI can diff a row against the one before it to show what actually changed), `HistoryAction` (enum, shared by both history entry types), `EncryptedSecretValue`, `ChangeMetadata`, `Auth0UserProfile`

---

## Layer: `services/`

Domain Services coordinate work across multiple repositories or aggregate roots, and implement business rules that don't cleanly belong to a single Domain Model.

**Rules:**
- Services call repositories — they never interact with Entities directly.
- Services operate on Domain Models, not Entities.
- Cross-cutting business logic that requires combining data from multiple repositories belongs here.

**Existing services:** `ServiceBase`, `AuthService` (Auth0 login/callback orchestration + JWT issuance), `SystemService` (writes a `SystemHistoryEntry` — full field snapshot, not just name — on every create/update/delete; `getSystemByExternalId` looks up by the globally-unique external id, used by the API-key bulk-reveal endpoint), `EnvironmentService` (CRUD for environments; deleting one cascades through `SecretValueService`; `getEnvironmentByExternalId` looks up by (systemId, externalId) — external ids are unique per system), `SecretService` (CRUD for secret NAMES only; deleting one cascades through `SecretValueService`), `SecretValueService` (owns the actual encrypted per-environment content — encrypts on write, writes a `SecretValueHistoryEntry` on every create/update/delete; also the bulk-reveal-for-an-environment path used by API-key clients, still ID-based internally — the controller resolves system/environment name to id first), `UserService` (role management), `ApiKeyService` (list/create/rename/revoke — creating one hands back the raw token exactly once, via `security/apikey/ApiKeyCrypto`, and never again; rename only ever changes the label, the token/hash stays immutable)

---

## Layer: `web/`

The web layer is the outermost layer — it handles HTTP concerns only. It calls services (never repositories directly) and translates between HTTP request/response formats and Domain Models.

### `web/API/`

Spring MVC `@RestController` classes that define all REST endpoints exposed by this application. Controllers should be thin — delegate business logic to services, not implement it. Role enforcement is via `@PreAuthorize` (backed by the `RoleHierarchy` bean in `SecurityConfig`).

**Existing controllers:** `ControllerBase`, `LoginController`, `CallbackController`, `AuthController`, `SystemController` (system CRUD + `/history`), `EnvironmentController`, `SecretController` (secret name CRUD + all per-environment value endpoints: reveal/set/delete value, history — `GET .../history` takes an optional `?environmentId=` to scope to one environment, and `GET .../history/{historyId}/value` decrypts one specific history entry's snapshot on demand, same READ_WRITE+ restriction as the live-value reveal endpoint; bulk-reveal-for-environment is the one exception to "everything is ID-based" — `GET /api/systems/{systemExternalId}/environments/{environmentExternalId}/secrets` takes the system/environment EXTERNAL ID, a separate field from both the internal numeric id and the renameable `name`, since it's the endpoint machine/API-key clients hit and a rename should never break one), `UserController`, `ApiKeyController` (under `/api/systems/{systemId}/api-keys` — list is READ_ONLY+, create/rename are READ_WRITE+, revoke is ADMIN-only; per-method `@PreAuthorize`, same tiered model as the rest of the app, not a special case), `GlobalExceptionHandler`

### `web/Models/`

View Model classes — the response shapes returned by the API controllers, and the request DTOs bound from JSON bodies. These represent the API contract with clients.

**Rule:** No view model except `SecretValueViewModel` and `HistoricSecretValueViewModel` may ever carry a decrypted secret value, and only `IssuedApiKeyViewModel` may ever carry a raw API key token (and only once, from creation). `SecretViewModel` (which instead carries `valuesSetInEnvironmentIds`), `SecretValueHistoryViewModel`, `SystemHistoryViewModel`, and `ApiKeyViewModel` are metadata-only by design — `SecretValueHistoryEntry`'s encrypted snapshot only ever reaches a client through the dedicated per-entry reveal endpoint (`HistoricSecretValueViewModel`), never bundled into the plain history list. `SystemViewModel` is created-only (no `updated` field) — see `SystemHistoryViewModel` for who changed what since. `SystemHistoryViewModel` carries `externalId`/`description` alongside `systemName` (nullable — rows recorded before that snapshot existed have no value for them) so the frontend can diff consecutive entries.

**Existing view models:** `SystemViewModel`, `SystemHistoryViewModel`, `EnvironmentViewModel`, `SecretViewModel`, `SecretValueViewModel`, `SecretValueHistoryViewModel`, `HistoricSecretValueViewModel` (the decrypted value as of one specific history entry, revealed on demand), `UserViewModel`, `CurrentUserViewModel`, `ApiKeyViewModel`, `IssuedApiKeyViewModel`, `ChangeStampViewModel`, `SystemRequest`, `EnvironmentRequest`, `SecretRequest`, `SecretValueRequest`, `ApiKeyRequest`, `UpdateUserRoleRequest`

---

## Layer: `security/`

Cross-cutting authentication and authorization infrastructure. Auth0 integration, JWT cookie management, encryption, and Spring Security configuration live here.

Sub-packages: `Auth0/` (`Auth0Properties`), `jwt/` (`JwtService` — session cookie; `LoginStateService` — signed, self-verifying OAuth `state` token for login CSRF protection without a server-side session; both share key material via `JwtProperties.toSigningKey()`; `JwtProperties`, `JwtCookieAuthenticationFilter`, `AuthenticatedPrincipal`), `crypto/` (`SecretEncryptionService`, `EncryptionProperties` — AES-256-GCM encryption of secret values), `apikey/` (`ApiKeyCrypto` — generates/hashes tokens, SHA-256 not a slow password hash since the token itself is the entropy source; `ApiKeyAuthenticationFilter` — reads `Authorization: Bearer`, the machine-credential counterpart to `JwtCookieAuthenticationFilter`; `ApiClientPrincipal`, carrying `ROLE_API_CLIENT`, a role deliberately outside the user `RoleHierarchy` since it's not comparable to READ_ONLY/READ_WRITE/ADMIN). `SecurityConfig` and `FrontendProperties` live at the `security/` root. CSRF protection is disabled (see `SecurityConfig` for why) — there is no CSRF-specific filter/class to know about.

---

## Cross-cutting: `exceptions/`

Plain runtime exceptions (`NotFoundException`, `ConflictException`) thrown by repositories/services and translated to HTTP status codes by `web/API/GlobalExceptionHandler`. Not tied to any single layer, so it sits outside the four-layer hierarchy.

---

## Decision Guide: Where Does New Code Go?

| What you're building | Where it goes |
|---|---|
| JPA entity / external API response DTO | `data/Entities/` |
| Spring Data JPA interface | `data/dao/` |
| Entity ↔ Domain Model conversion | `data/mapper/` |
| DB query / external API call | `data/repositories/` |
| Core business object | `domainmodel/` |
| Business rule on one object | Method on the Domain Model class |
| Business rule across multiple objects/repos | `services/` |
| REST endpoint (`@RestController`) | `web/API/` |
| API response shape / request DTO | `web/Models/` |
| Auth0/JWT/encryption/Spring Security config | `security/` |

---

## Dependency Rules (strictly enforced)

```
web/API  →  services  →  domainmodel
                     →  data/repositories  →  data/mapper  →  data/Entities
                                          →  data/dao
```

- `web` may import `services` and `domainmodel` — never `data`
- `services` may import `domainmodel` and `data/repositories` — never `data/Entities`
- `data/repositories` is the only code that may import `data/Entities`
- No upward dependencies allowed
