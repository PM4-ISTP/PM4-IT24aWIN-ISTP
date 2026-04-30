# Keycloak ↔ PostgreSQL Sync (Admin API + JIT Provisioning)

This project uses **Keycloak** as the *source of truth* for identity/auth and keeps a **shadow/projection** row in **PostgreSQL** (`users` table) for app/domain joins (courses, enrollments, …).

The **backend** is the only component allowed to synchronize between Keycloak and Postgres (via the **Keycloak Admin REST API**; no direct access to Keycloak’s internal DB).

## Source of truth

**Keycloak**
- Login / Password
- User ID (`sub` / UUID)
- Roles / Groups
- Base profile: `email`, `username`, `firstName`, `lastName`
- Custom profile attributes (stored on the Keycloak user):
  - `title` (attribute)
  - `picture` (attribute; stores only a **URL**, not the image file)

**PostgreSQL**
- Domain/app data (courses, enrollments, …)
- `users` shadow/projection row:
  - `id` = Keycloak user id (`token.sub`)
  - denormalized copy of profile fields for reads/search/joins
- Soft delete (`deletedAt`) for app access control & auditability

## Runtime flow (simple)

### 1) Self-registration (Keycloak)
Users can register themselves in Keycloak. Keycloak assigns a default role/group (e.g. `ROLE_STUDENT`) via Keycloak configuration.

### 2) Login / first API request (Just-in-Time provisioning)
On the first request after login, the backend:
1. Validates the JWT (issuer + signature)
2. Reads `token.sub` (Keycloak user UUID)
3. Reads roles/groups from the token
4. Checks if the user has an allowed app role (e.g. `ROLE_STUDENT` / `ROLE_INSTRUCTOR` / `ROLE_ADMINISTRATOR`)
5. Checks if `users.id = token.sub` exists in Postgres
6. If missing **and** the user is a `ROLE_STUDENT`, the backend auto-creates the shadow `users` row
7. If `users.deletedAt` is set, access is denied

### 3) Profile editing (via app, not Keycloak Account UI)
Users update their profile through the app. The backend keeps both systems consistent:
1. Reads Keycloak user as a **snapshot**
2. Updates Keycloak via Admin API
3. Updates Postgres `users` row
4. If Postgres update fails, backend attempts to roll back Keycloak to the snapshot

Synced fields:
- `firstName`, `lastName`
- `title` (Keycloak attribute)
- `picture` URL (Keycloak attribute)
- `email` / `username` (only if you allow editing; typically restricted)

## Backend endpoints

### User profile
- `GET /api/v1/users/me/profile`
- `PUT /api/v1/users/me/profile`
- `PUT /api/v1/users/{userId}/profile` (admin-only in service logic)

### Admin user management (Keycloak + Postgres combined)
- `GET /api/admin/users` (paged list from Postgres)
- `GET /api/admin/users/{userId}` (Postgres + Keycloak snapshot)
- `POST /api/admin/users` (create Keycloak user + create Postgres shadow row; returns a temporary password)
- `POST /api/admin/users/{userId}/provision` (provision an existing Keycloak user into Postgres shadow row)
- `PUT /api/admin/users/{userId}/roles` (set realm role via Keycloak Admin API)

### Admin account actions
**1) Disable (reversible)**
- `POST /api/admin/users/{userId}/disable`
  - Keycloak: `enabled=false`
  - Postgres: `deletedAt=now()`

**2) Restore (reversible, only for disabled users)**
- `POST /api/admin/users/{userId}/restore`
  - Keycloak: `enabled=true`
  - Postgres: `deletedAt=null`

**3) Soft-delete (irreversible)**
- `POST /api/admin/users/{userId}/soft-delete`
  - Keycloak: changes `email` + `username` to a `deleted_*` value, then `enabled=false`
  - Postgres: same anonymization + `deletedAt=now()`
  - Goal: free up the original email/username for a future registration

### Admin sessions & password
- `GET /api/admin/users/{userId}/sessions` (list active Keycloak sessions for a user)
- `POST /api/admin/users/{userId}/logout` (logout user sessions in Keycloak)
- `POST /api/admin/users/{userId}/password-reset-email` (Keycloak sends reset mail)
- `PUT /api/admin/users/{userId}/password` (admin sets password; Keycloak enforces password policy)

## Configuration (local dev)

In `backend/src/main/resources/application.properties`:
- `spring.security.oauth2.resourceserver.jwt.issuer-uri` (must match your Keycloak realm)
- `keycloak.admin.*` (Admin API client credentials)
- `keycloak.app.client-id` (OIDC clientId used by the app; used for session listing)

Secrets:
- `keycloak.admin.client-secret` should be provided via env var `KEYCLOAK_ADMIN_CLIENT_SECRET` (recommended).

## How to test (manual)

1. Start infra (Postgres + Keycloak) from `infra/` and start backend + frontend.
2. Self-register a new user in Keycloak.
3. Login in the app:
   - First API call should JIT-create the `users` shadow row (student only).
4. Open profile page, update `firstName/lastName/title/pictureUrl`:
   - Should update Keycloak + Postgres.
5. As admin, open Admin → User Management:
   - Create user, change roles, send reset email, set password, disable/restore/soft-delete.

