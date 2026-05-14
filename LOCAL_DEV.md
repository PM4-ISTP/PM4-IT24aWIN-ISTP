# Local Development

Local development uses a **hybrid setup**:

- **PostgreSQL runs locally** via Docker Compose (`infra/`).
- **Keycloak runs remotely on staging** (no local Keycloak).

## 1) Create local config files

### Frontend (Next.js)

```bash
cd frontend
cp .env.example .env.local
```

Fill in at least:

- `NEXTAUTH_SECRET` (generate locally, e.g. `openssl rand -base64 32`)
- `AUTH_KEYCLOAK_SECRET` (from staging Keycloak client credentials)

Optional:

- `BACKEND_URL` (default is `http://localhost:8080`; set to staging if you develop frontend-only)
- `CYPRESS_ADMIN_USERNAME` and `CYPRESS_ADMIN_PASSWORD` (only needed for E2E tests; use a
  dedicated Keycloak test account and keep the values in `.env.local` or CI secrets)

### Infra (Postgres via Docker Compose)

```bash
cd infra
cp .env.example .env
```

Set:

- `POSTGRES_PASSWORD` (used by the local Postgres container)

### Backend (Spring Boot)

```bash
cd backend
cp application-local.properties.example src/main/resources/application-local.properties
```

Windows PowerShell:
`Copy-Item application-local.properties.example src/main/resources/application-local.properties`

Fill in `backend/src/main/resources/application-local.properties`:

- `spring.datasource.password` (must match `infra/.env` → `POSTGRES_PASSWORD`)
- `keycloak.admin.client-secret` (required for admin/profile sync; without it admin user edits / profile updates will fail)

Never commit real values in `frontend/.env.example`, `infra/.env.example`, or
`backend/application-local.properties.example`. These files should contain placeholders only.
Put local secrets in `.env.local`, `infra/.env`, and
`backend/src/main/resources/application-local.properties`; those files are gitignored.

## 2) Start infra (Postgres)

```bash
cd infra
docker compose up -d
```

## 3) Start backend (Spring Boot)

```bash
cd backend
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

Windows PowerShell:
`$env:SPRING_PROFILES_ACTIVE="local"; .\\gradlew.bat bootRun`

Backend runs on `http://localhost:8080`.

## 4) Start frontend (Next.js)

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:3000`.
