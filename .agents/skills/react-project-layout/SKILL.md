---
name: react-project-layout
description: Use this skill whenever working in this React codebase — when adding new files, deciding where code belongs, or discussing frontend architecture. Trigger on questions like "where should I put this component", "how is the frontend organized", or any time new frontend files need to be created.
version: 1.0.0
---

# ConfigurationManager Frontend — Project Layout

React + TypeScript, built with Vite. All source lives under `frontend/src/`. This is a single-purpose app (not a multi-app repo), so the layout is flatter than a typical "Apps/" split.

## `api/`

**All backend API integrations are done through repository modules here.** Each file wraps a specific domain and exports plain async functions that call the REST API via `RestClient.ts`.

- Never call `axios`/`fetch` directly from components or pages — always go through a repository module here.
- Current repositories: `AuthRepository`, `SystemRepository` (system CRUD + history — `updateSystem` takes `externalId` alongside `name`/`description`), `EnvironmentRepository` (`updateEnvironment` likewise takes `externalId` alongside `name`; `createEnvironment` doesn't — the backend defaults `externalId` to `name` on create), `SecretRepository` (secret name CRUD + per-environment value operations: reveal/set/delete value, history — `getSecretValueHistory` takes an optional `environmentId` to scope the list, and `revealHistoricSecretValue` decrypts one specific history entry's value on demand, mirroring `revealSecretValue`'s "only called on explicit click" contract), `UserRepository`, `ApiKeyRepository` (list/create/rename/revoke — create is the only call that ever returns a raw token), `RestClient` (the shared axios instance — handles `withCredentials`; no CSRF header, CSRF is disabled backend-side).

## `pages/`

One file per route-level view: `LoginLandingPage`, `SystemsPage`, `SystemDetailPage`, `UsersAdminPage`, `UnauthorizedPage`. A page composes `components/` to build its view; component(s) used only by one page are defined in that page's own file rather than promoted to `components/`.

**Inline row editing, not a rename modal:** `SystemsPage` and `SystemDetailPage`'s `ManageEnvironmentsModal` both edit systems/environments via an edit (✎) icon per row — clicking it swaps that row's cells for inputs in place, with ✓/✕ icons to save/cancel, gated on `canWrite`. This replaced an earlier `NameForm`-in-a-`Modal` rename flow for environments; systems never had any edit UI before this. Both `SystemDto`/`EnvironmentDto` carry an `externalId` field editable the same way (see `models/` below) — the shared `EXTERNAL_ID_PATTERN`/`EXTERNAL_ID_TITLE` constants (one copy per file, not promoted to a shared module) back the input's HTML `pattern`/`title` for a client-side hint of the same regex the backend enforces.

**System history shows the actual diff, not just who/when:** `SystemsPage`'s history modal has a `describeSystemHistoryChange(entry, previousEntry)` helper — `SystemHistoryDto` is returned newest-first, so for row `i` the "previous" (older) entry is `history[i + 1]`. For an `UPDATED` row it diffs name/externalId/description against that older entry and renders only the fields that actually changed (`"old" → "new"`); for `CREATED`/`DELETED`, or a row with no older entry to diff against, it just shows the snapshot at that point. Fields null on either side are skipped rather than diffed, since rows written before `system_history` tracked externalId/description have no snapshot for them — diffing against a fabricated `null` would be misleading. The per-row descriptions are precomputed into a `Map` via `useMemo` (keyed by history entry id) rather than computed inline in the `DataTable` `render` callback, since `DataTableColumn.render` only receives the row, not its index.

`SystemDetailPage` is the most involved page: it renders an environment tab bar (switching the active environment scopes which per-environment value the secrets table shows/edits) plus an environment management modal, the secrets table, and an API Keys section — `EnvironmentTabs`, `ManageEnvironmentsModal` (still modal-gated behind "Manage Environments"), `ApiKeysSection` (rendered directly on the page, no "Manage" button/modal wrapper — the list itself is always visible to anyone with page access; rename and revoke are per-row icon buttons, ✎ and 🗑, using the same inline-row-edit pattern as `ManageEnvironmentsModal`/`SystemsPage` for rename, gated on `canWrite`/`canDelete` respectively; creating a key still pops a one-time-reveal `Modal` for the raw token, matching the backend's one-time-issuance contract), `NameForm`, and `ValueForm` are small helper components defined in that same file rather than promoted to `components/`, since they're only meaningful in this page's context. The secret history modal (`openHistory`) is scoped to the currently active environment (passes `activeEnvironmentId` to `getSecretValueHistory`, resets whenever the modal reopens) and shows each entry's actual value — masked behind a per-row Reveal button gated on `canRevealSecretValue`, same as the live-value reveal in the main table, backed by a separate `revealedHistory` map keyed by history entry id (not the `revealed` map the live-value column uses).

## `components/`

Generic, reusable UI primitives with no knowledge of a specific page or API shape: `DataTable`, `Modal`, `ConfirmDialog`, `RoleGuard` (hides UI the current user's role can't use), `LoadingSpinner`, `Layout` (nav bar + page chrome for authenticated routes).

## `context/`

React Context providers for cross-cutting app state. Currently just `AuthContext` — loads `GET /api/auth/me` on boot and exposes `{ user, loading, refresh, logout }` via the `useAuth()` hook.

## `routes/`

Routing setup (`react-router-dom`): `AppRoutes` (route table), `ProtectedRoute` (redirects to the login landing page when unauthenticated), `RequireRole` (redirects to `/unauthorized` when the current user's role doesn't satisfy a predicate).

## `models/`

TypeScript interfaces/types for API request and response shapes, mirroring the backend's `web/Models/` view models: `SystemDto` (created-only)/`SystemHistoryDto` (carries nullable `externalId`/`description` snapshots alongside `systemName`, used to diff consecutive entries — see `SystemsPage` in `pages/` above), `EnvironmentDto`, `SecretDto` (carries `valuesSetInEnvironmentIds`, never a value)/`SecretValueDto`, `SecretValueHistoryDto` (metadata only — no value, decrypted or otherwise; `SecretRepository.revealHistoricSecretValue` is the only call that returns one, and only for the one history entry explicitly revealed), `UserDto`/`CurrentUserDto`, `ApiKeyDto` (metadata only)/`IssuedApiKeyDto` (the only shape carrying a raw token, from creation only), `ChangeStamp`, `Role`, `HistoryAction` (shared by both history DTOs). `SystemDto`/`EnvironmentDto` both carry `externalId` — a separate, URL-safe identifier from `name`, used by machine/API-key clients (see backend `SecretController#revealSecretsForEnvironment`) so a rename never breaks one.

---

## Decision Guide

| I need to... | Put it in... |
|---|---|
| Call a backend API | `api/` |
| Add a new route-level view | `pages/` |
| Add a component only one page uses | Define it in that page's file, or a co-located file next to it |
| Add a generic, reusable UI primitive | `components/` |
| Gate UI by the current user's role | `components/RoleGuard.tsx` (UI-only — the backend still enforces via `@PreAuthorize`) |
| Add cross-cutting app state | `context/` |
| Add a TypeScript interface/type for API data | `models/` |
