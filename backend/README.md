# ConfigurationManager Backend

Java 21 / Spring Boot 3 API for the Secret Manager application. Handles Auth0-backed cookie authentication, role-based access control, and AES-256-GCM encryption of secret values at rest.

This pass of the project deliberately stops short of connecting to a real database or Auth0 tenant — see "Out of scope for now" below.

## Requirements

- Java 21+
- Maven 3.9+
- A MySQL 8 database created from the scripts in `../database` (not run automatically — see that folder's README)
- An Auth0 application (Regular Web Application) once you're ready to test the login flow

## Configuration

All configuration is env-var driven (see `src/main/resources/application.yml`). Nothing sensitive is committed.

| Env var | Purpose |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `SM_DB_USERNAME`, `SM_DB_PASSWORD` | MySQL connection |
| `SM_AUTH0_DOMAIN`, `SM_AUTH0_CLIENT_ID`, `SM_AUTH0_CLIENT_SECRET`, `SM_AUTH0_AUDIENCE` | From your Auth0 application settings |
| `SM_AUTH0_CALLBACK_URL` | Must exactly match an "Allowed Callback URL" configured in Auth0, e.g. `http://localhost:8089/api/auth/callback` |
| `SM_FRONTEND_BASE_URL` | Where the browser is redirected after login, e.g. `http://localhost:5173` |
| `SM_LOGOUT_URL` | Where the browser is redirected after logout |
| `SM_CORS_ALLOWED_ORIGINS` | Comma-separated list of origins allowed to call this API with credentials — must include every origin the frontend is actually served from |
| `JWT_SIGNING_KEY` | Base64-encoded, ≥256-bit key for signing the session cookie. Generate with `openssl rand -base64 32` |
| `SECRET_ENCRYPTION_KEY` | Base64-encoded 256-bit AES key for encrypting secret values. Generate with `openssl rand -base64 32` |

See `src/main/resources/application.yml` for the full list, including optional ones with sane defaults (`SM_SERVER_PORT`, `JWT_COOKIE_NAME`, `JWT_COOKIE_SECURE`, `JWT_COOKIE_DOMAIN`, `JWT_EXPIRATION_MINUTES`, `SECRET_ENCRYPTION_KEY_VERSION`, `DB_USE_SSL`, `DB_ALLOW_PUBLIC_KEY_RETRIEVAL`).

For local development, copy the pattern into a `.env` you source yourself, or export the variables directly — `application-local.yml` (active by default) only overrides non-secret dev conveniences (verbose SQL logging, non-`Secure` cookie so it works over plain `http://localhost`).

## Running

```bash
mvn spring-boot:run
```

The app will fail fast on startup if `JWT_SIGNING_KEY` or `SECRET_ENCRYPTION_KEY` are missing/invalid, and will refuse to start (`ddl-auto: validate`) unless the schema from `../database` already exists in the target MySQL instance.

## Testing

```bash
mvn test
```

## Out of scope for this pass

Per the current task, this pass does **not**:
- Create or connect to a real MySQL database (see `../database/README.md` for the DDL and how to apply it manually when ready)
- Register or configure a real Auth0 tenant/application
- Deploy anywhere

The code is written to make all of that a config/infrastructure step rather than a code change.
