# Keycloak Profile Sync (Admin API)

## Goal
Allow users to update profile data in the web app while keeping **Keycloak** and the **application database** consistent, using the **Keycloak Admin REST API** (no direct DB access to Keycloak).

## Source Of Truth
**Keycloak is the source of truth** for the editable profile fields:

- `firstName` (Keycloak user field)
- `lastName` (Keycloak user field)
- `title` (Keycloak user attribute + OIDC claim mapper)
- `picture` (Keycloak user attribute + OIDC claim mapper; stores only a URL/reference)

The application database stores a **denormalized copy** for fast reads/search and domain joins. It is updated by:

- `UserProvisioningFilter` (Just-in-Time provisioning for students + sync on requests from JWT/userinfo)
- The profile update flow (writes Keycloak first, then writes the app DB)
- Admin provisioning flows (create/provision/disable)

## Keycloak Self-Registration vs App Access
Self-registration in Keycloak is allowed. New users get `ROLE_STUDENT` automatically (Keycloak configuration).

Flow:
1. User registers in Keycloak
2. User logs in
3. Backend validates JWT and checks the user has an application role (`ROLE_STUDENT`, `ROLE_INSTRUCTOR`, `ROLE_ADMINISTRATOR`)
4. Backend checks `users.id == token.sub` in Postgres
5. If missing and the user has `ROLE_STUDENT`: backend **auto-creates** the shadow DB row (Just-in-Time provisioning)
6. If missing and the user is not a student (e.g. admin/instructor): backend returns **403** with `{"error":"User not provisioned. Contact an administrator."}`

If `users.deletedAt` is set, access is always denied.

## Keycloak Field Mapping
- `firstName` → `User.firstName`
- `lastName` → `User.lastName`
- `title` → Keycloak user `attributes.title[0]` → `User.title`
- `picture` → Keycloak user `attributes.picture[0]` → `User.picture`

`User.name` is derived as `"firstName lastName"` when updating via the app.

## Backend API Endpoints
- `GET /api/v1/users/me/profile`
- `PUT /api/v1/users/me/profile`
- `PUT /api/v1/users/{userId}/profile` (admin-only via app authorization logic)

Admin endpoints:
- `POST /api/admin/users` (create in Keycloak + provision DB row; returns temporary password)
- `POST /api/admin/users/{userId}/provision` (approve/provision existing Keycloak user)
- `POST /api/admin/users/{userId}/disable` (disable in Keycloak + soft-delete in DB)

## Authorization Rules
- A user may only update their **own** profile.
- A user with `ROLE_ADMINISTRATOR` may update other users’ profiles.

## Consistency & Error Handling
The update flow is designed to avoid partial writes:

1. Read current Keycloak user representation (used as rollback snapshot)
2. Update Keycloak via Admin API
3. Update application DB user row
4. If step (3) fails, **attempt to rollback** Keycloak to the snapshot

HTTP behavior:
- Keycloak Admin API failures return `502 Bad Gateway`
- Local DB sync failures return `500 Internal Server Error` (after rollback attempt)

## Profile Pictures
The backend only stores a **URL/reference** (`pictureUrl`). The actual file should live in object/file storage (e.g. S3/MinIO, Azure Blob, etc.).

## Configuration
Backend properties (`backend/src/main/resources/application.properties`):

- `keycloak.admin.base-url` (e.g. `http://localhost:9090`)
- `keycloak.admin.realm`
- `keycloak.admin.client-id`
- `keycloak.admin.client-secret`

Keycloak requirements:
- Create a confidential client for backend service-to-service calls
- Enable **Service Accounts**
- Grant the service account sufficient permissions to read/update users (e.g. realm-management roles such as `manage-users` / `view-users` depending on your setup)
