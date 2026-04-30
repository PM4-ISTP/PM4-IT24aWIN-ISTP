# Keycloak <-> Postgres User Sync (Keycloak Admin API)

## Goal
Manage users and profile data **through the ISTP app** while keeping **Keycloak** and the **application database (Postgres)** consistent.

All writes to Keycloak happen via the **Keycloak Admin REST API** (no direct DB access to Keycloak).

## Source of Truth
**Keycloak is the source of truth** for:
- Login / password
- User ID (`sub` / Keycloak UUID)
- Realm roles / groups (authorization)
- Base profile: `email`, `username`, `firstName`, `lastName`

**Postgres is the source of truth** for:
- App/domain data (courses, enrollments, joins)
- Shadow/projection table `users` (id = Keycloak user id)
- Disable state (`deletedAt`)
- Irreversible soft-delete marker (`anonymizedAt`)

The backend is the only component allowed to synchronize both systems.

## Identity & ID Mapping
- `users.id` in Postgres is **exactly** the Keycloak user id (UUID), taken from `token.sub`.
- The backend does **not** allow access based on "valid token only"; it checks **app roles** + **deletedAt**.

## Self-registration and Just-in-Time Provisioning
Self-registration in Keycloak is allowed. New users get `ROLE_STUDENT` automatically (Keycloak configuration).

On the first API request after login:
1. Backend validates the JWT (`issuer-uri`).
2. Backend reads Keycloak user id from `token.sub`.
3. Backend reads app roles from the JWT (`ROLE_STUDENT`, `ROLE_INSTRUCTOR`, `ROLE_ADMINISTRATOR`).
4. Backend checks if `users.id = sub` exists in Postgres.
5. If missing **and** user has `ROLE_STUDENT`, the backend creates the shadow DB row (Just-in-Time provisioning).
6. If missing and user is not `ROLE_STUDENT`, backend returns **403**:
   - `{"error":"User not provisioned. Contact an administrator."}`
7. If `users.deletedAt` is set, access is denied.

## Profile Field Mapping (Keycloak -> DB projection)
- `firstName` -> `users.first_name`
- `lastName` -> `users.last_name`
- `attributes.title[0]` -> `users.title`
- `attributes.picture[0]` -> `users.picture` (URL only)
- `users.name` is derived as `firstName + " " + lastName` (fallbacks to username/email if missing)

## Profile Update Flow (Consistency)
Users update profile data via the app (not the Keycloak account UI).

Flow (write Keycloak first, then DB):
1. Read current Keycloak user representation (snapshot)
2. Update Keycloak via Admin API
3. Update the `users` row in Postgres
4. If (3) fails, attempt to rollback Keycloak back to the snapshot

HTTP behavior:
- Keycloak Admin API failures -> `502 Bad Gateway` (`Keycloak update failed: ...`)
- Local DB sync failures -> `500 Internal Server Error` (after rollback attempt)

## User Lifecycle Admin Actions
The app supports two separate admin actions:

### 1) Disable (reversible)
- Keycloak: `enabled = false`
- DB: `deletedAt = now()`
- Can be reversed with **Restore**
  - Keycloak: `enabled = true`
  - DB: `deletedAt = NULL`

### 2) Soft-delete (irreversible, frees identifiers)
- Keycloak:
  - email and username are anonymized (timestamped)
  - `enabled = false`
- DB:
  - same anonymized email/username
  - `deletedAt = now()`
  - `anonymizedAt = now()`
- No restore and no provisioning/editing allowed afterwards.

This is used when the original email/username should be reusable for a new registration (Keycloak typically blocks duplicates while the original user still exists).

## Backend Endpoints (current)
User:
- `GET /api/v1/users/me/profile`
- `PUT /api/v1/users/me/profile`
- `PUT /api/v1/users/{userId}/profile` (admin only)

Admin users:
- `GET /api/admin/users/directory` (list/search Keycloak users with provisioned status)
- `GET /api/admin/users/{userId}` (combined Keycloak + DB view)
- `PUT /api/admin/users/{userId}/roles` (normalize to 1 managed realm role)
- `POST /api/admin/users` (create user in Keycloak + create DB row; returns temporary password once)
- `POST /api/admin/users/{userId}/provision` (provision an existing Keycloak user into the app DB; blocked for soft-deleted users)
- `POST /api/admin/users/{userId}/disable` (reversible disable)
- `POST /api/admin/users/{userId}/restore` (re-enable + restore DB)
- `POST /api/admin/users/{userId}/soft-delete` (irreversible anonymize + disable)
- `POST /api/admin/users/{userId}/password-reset-email` (triggers Keycloak email action `UPDATE_PASSWORD`)
- `PUT /api/admin/users/{userId}/password` (manual reset via Admin API, supports `temporary=true`)

Admin sessions:
- `GET /api/admin/sessions` (active sessions for the configured app client)
- `DELETE /api/admin/sessions/{sessionId}` (logout a specific session)

## Authorization Rules
- A user may only update their **own** profile.
- Only `ROLE_ADMINISTRATOR` can manage other users (admin endpoints).
- `ROLE_ADMINISTRATOR` / `ROLE_INSTRUCTOR` are never auto-granted to self-registered users.

## Profile Pictures
The backend stores only a **URL/reference** (`pictureUrl`). The actual image should live in object storage (S3/MinIO/Azure Blob/etc.).

## Configuration
Backend config: `backend/src/main/resources/application.properties`

- JWT validation:
  - `spring.security.oauth2.resourceserver.jwt.issuer-uri`
- Keycloak Admin API (service account):
  - `keycloak.admin.base-url`
  - `keycloak.admin.realm`
  - `keycloak.admin.client-id`
  - `keycloak.admin.client-secret` (use `KEYCLOAK_ADMIN_CLIENT_SECRET`)
- Keycloak app client (sessions listing):
  - `keycloak.app.client-id`

Keycloak requirements:
- Create a confidential client for backend service-to-service calls
- Enable **Service Accounts**
- Grant the service account sufficient permissions (realm-management), typically:
  - `view-users`, `query-users`, `manage-users` (plus what your setup requires for role mapping and sessions)

