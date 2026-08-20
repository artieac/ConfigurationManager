# Database Scripts

Plain SQL DDL scripts defining the MySQL schema for ConfigurationManager. **These are not executed by any tooling in this repo** — no Flyway/Liquibase dependency is wired up, no CI step runs them, and nothing here connects to a database. They are provided so the schema can be created manually (or plugged into a migration tool later — the `V<N>__description.sql` naming is Flyway-compatible on purpose).

## Scripts (run in order)

| File | Creates |
|---|---|
| `migrations/V1__roles_and_users.sql` | `roles`, `users` |
| `migrations/V2__systems.sql` | `systems` |
| `migrations/V3__secrets.sql` | `secrets` (names only — no value) |
| `migrations/V4__environments.sql` | `environments` |
| `migrations/V5__secret_values.sql` | `secret_values` (the encrypted value, one row per secret+environment) |
| `migrations/V6__secret_value_history.sql` | `secret_value_history` |
| `migrations/V7__system_history.sql` | `system_history` |
| `migrations/V8__api_keys.sql` | `api_keys` |
| `migrations/V9__external_ids.sql` | `systems.external_id`, `environments.external_id` |
| `migrations/V10__system_history_snapshot.sql` | `system_history.external_id`, `system_history.description` |

## To apply manually (when you're ready to stand up a real database)

```bash
mysql -u <admin_user> -p -e "CREATE DATABASE secret_manager CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

mysql -u <admin_user> -p secret_manager < migrations/V1__roles_and_users.sql
mysql -u <admin_user> -p secret_manager < migrations/V2__systems.sql
mysql -u <admin_user> -p secret_manager < migrations/V3__secrets.sql
mysql -u <admin_user> -p secret_manager < migrations/V4__environments.sql
mysql -u <admin_user> -p secret_manager < migrations/V5__secret_values.sql
mysql -u <admin_user> -p secret_manager < migrations/V6__secret_value_history.sql
mysql -u <admin_user> -p secret_manager < migrations/V7__system_history.sql
mysql -u <admin_user> -p secret_manager < migrations/V8__api_keys.sql
mysql -u <admin_user> -p secret_manager < migrations/V9__external_ids.sql
mysql -u <admin_user> -p secret_manager < migrations/V10__system_history_snapshot.sql
```

The backend's `spring.jpa.hibernate.ddl-auto` is set to `validate` — it expects this schema to already exist and will refuse to start if entities don't line up with it. It will never create or alter tables itself.

## Design notes

- **Roles are a lookup table** (`roles`), not a free-text/enum column on `users`, so role names can be added or renamed without a data migration on every user row.
- **A secret's name and its value are separate tables** (`secrets` vs `secret_values`). A secret is a name scoped to a system; each of the system's `environments` can hold its own encrypted value for that name (or none yet — values are sparse, not required per environment). This is what lets the same secret (e.g. `DB_PASSWORD`) carry a different value in Development vs. Production while still being recognizably "the same secret" across them.
- **`secret_value_history` and `system_history` are separate append-only tables**, not versioned rows on `secret_values`/`systems`, so the hot path (reading current values/systems) never scans historical rows. `secret_value_history` is named for what it actually audits — changes to a secret's per-environment *value*, not the secret name itself (renaming a secret is not tracked in it).
- **`secret_values` and `systems` only track `created_by`/`created_at` — no `updated_by`/`updated_at`.** Every create/update/delete is already recorded as its own row in `secret_value_history`/`system_history` with who and when, so a separate "last updated" stamp on the live row would just be the same fact stored twice. (`secrets` and `environments` — the name-only tables — still have `updated_by`/`updated_at`, since renaming one isn't tracked in any history table.)
- **`secret_value_history.secret_id`/`system_id`/`environment_id` and `system_history.system_id` are all nullable (`ON DELETE SET NULL`)** with denormalized name snapshots, so deleting a secret, an environment, or an entire system (ADMIN-only) can never erase its audit trail.
- **Secret values are always encrypted (AES-256-GCM) before they reach this schema.** `secret_values.encrypted_value`/`encryption_iv` and `secret_value_history.encrypted_value_snapshot`/`encryption_iv_snapshot` never contain plaintext. `key_version` supports future key rotation without a schema change. `system_history` carries no such columns — a system has no secret content of its own.
- `encrypted_value_snapshot` is never bundled into the plain history list — it's only ever decrypted and returned one entry at a time, through a dedicated per-entry reveal endpoint requiring READ_WRITE+ (same restriction as revealing a secret's current value), separate from the metadata-only history endpoint everyone with READ_ONLY+ can call.
- **`api_keys` never stores the raw token, only `token_hash` (SHA-256).** The raw token is shown to the caller exactly once, at creation, and can't be recovered afterward — losing it means issuing a new key. Each key is scoped to exactly one `system` and is meant for one client (a CI job, a script), so access can be granted/revoked per consumer instead of sharing one credential.
- **`systems.external_id`/`environments.external_id` are a separate identifier from `name`**, used by the API-key bulk-reveal endpoint instead of the internal numeric id. Renaming a system/environment never breaks a client already configured with its external id, since the two are independent. Defaults to the current `name` at creation (and via a one-time backfill for any pre-existing rows in V9); editable afterward like any other field. `systems.external_id` is globally unique; `environments.external_id` is unique per system, mirroring how `name` is scoped on each table.
- **`system_history.external_id`/`description` (V10) are nullable with no backfill** — rows written before V10 genuinely never captured those fields, so leaving them `NULL` is more honest than fabricating a value. Every row written from V10 onward always populates both, so the UI can diff a row against the one before it to show what actually changed (name/external ID/description), not just that an update happened.
