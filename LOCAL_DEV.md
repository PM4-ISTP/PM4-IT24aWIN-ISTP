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
cp application-secrets.properties.example application-secrets.properties
```

Set:

- `DB_PASSWORD` (must match `infra/.env` → `POSTGRES_PASSWORD`)
- `KEYCLOAK_ADMIN_CLIENT_SECRET` (only required for admin/profile-sync features)

## 2) Start infra (Postgres)

```bash
cd infra
docker compose up -d
```

## 3) Start backend (Spring Boot)

```bash
cd backend
./gradlew bootRun
```

Backend runs on `http://localhost:8080`.

## 4) Start frontend (Next.js)

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:3000`.
