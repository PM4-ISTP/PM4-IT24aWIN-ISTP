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
cp application-local.properties.example src/main/resources/application-local.properties
```

Windows PowerShell:
`Copy-Item application-local.properties.example src/main/resources/application-local.properties`

Fill in `backend/src/main/resources/application-local.properties`:

- `spring.datasource.password` (must match `infra/.env` -> `POSTGRES_PASSWORD`)
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
npm ci
npm run dev
```

Frontend runs on `http://localhost:3000`.

## 5) Test lab pod lifecycle changes against staging Kubernetes

Use this workflow when you change lab pod start, extend, stop, TTL, reaper, labels,
annotations, ingress, or image-pull behavior and want to run the local app against the
staging cluster.

### Required backend config

Keep the local Postgres and staging Keycloak settings from the sections above, then add
the lab pod settings to `backend/src/main/resources/application-local.properties`:

```properties
# Staging Keycloak JWT validation
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://istp-staging-auth.pm4.init-lab.ch/realms/interactive-security-training-platform
keycloak.admin.base-url=https://istp-staging-auth.pm4.init-lab.ch
keycloak.admin.client-secret=<istp-backend client secret>

# Staging Kubernetes namespace where lab pod deployments/services/ingresses are created
k8s.default.namespace=istp-staging

# Public host settings used when the backend builds lab app URLs.
# Matches the staging overlay: app-staging-<hash>.pm4.init-lab.ch
istp.domain=pm4.init-lab.ch
istp.lab-host-prefix=staging
istp.tls=true

# Lifecycle test knobs. Use short values locally when testing reaper behavior.
istp.pod-extension-seconds=1800
istp.pod-max-extensions=2
istp.pod-reaper.interval-ms=60000
istp.pod-reaper.initial-delay-ms=30000
```

If you set these as environment variables instead of local Spring properties, use:

| Property | Environment variable |
| --- | --- |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` |
| `keycloak.admin.base-url` | `KEYCLOAK_ADMIN_BASE_URL` |
| `keycloak.admin.client-secret` | `KEYCLOAK_ADMIN_CLIENT_SECRET` |
| `k8s.default.namespace` | `KUBERNETES_NAMESPACE` |
| `istp.domain` | `ISTP_DOMAIN` |
| `istp.lab-host-prefix` | `ISTP_LAB_HOST_PREFIX` |
| `istp.tls` | `ISTP_TLS` |
| `istp.pod-extension-seconds` | `ISTP_POD_EXTENSION_SECONDS` |
| `istp.pod-max-extensions` | `ISTP_POD_MAX_EXTENSIONS` |
| `istp.pod-reaper.interval-ms` | `ISTP_POD_REAPER_INTERVAL_MS` |
| `istp.pod-reaper.initial-delay-ms` | `ISTP_POD_REAPER_INITIAL_DELAY_MS` |

PowerShell example for running the local backend against staging lab pod infrastructure:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
$env:KUBERNETES_NAMESPACE="istp-staging"
$env:ISTP_DOMAIN="pm4.init-lab.ch"
$env:ISTP_TLS="true"
$env:ISTP_LAB_HOST_PREFIX="staging"
$env:ISTP_POD_EXTENSION_SECONDS="1800"
$env:ISTP_POD_MAX_EXTENSIONS="2"
$env:CORS_ALLOWED_ORIGIN="http://localhost:3000"
.\gradlew.bat bootRun
```

Use this only when your local backend should create and manage lab pods in the staging Kubernetes namespace. For normal local development, start the backend with only the `local` Spring profile.

For faster lifecycle testing, temporarily set:

```properties
istp.pod-extension-seconds=120
istp.pod-reaper.interval-ms=10000
istp.pod-reaper.initial-delay-ms=10000
```

The base pod TTL comes from the lab when that lab has an override; otherwise it comes from the app
admin configuration. Set the admin default in the admin dashboard together with CPU/memory limits
and the kubeconfig.

### Kubeconfig and admin configuration

1. Get a kubeconfig for the staging cluster from Rancher or the platform administrator.
2. Make sure the kubeconfig context can manage resources in `istp-staging`.
3. Log in locally as an administrator at `http://localhost:3000`.
4. Open `Admin -> Dashboard -> Platform Config`.
5. Upload the staging kubeconfig, set CPU and memory limits, and set a short pod TTL when
   testing expiration or reaper behavior.
6. Save the admin configuration. The backend invalidates its cached Kubernetes client after
   a kubeconfig update.

Before starting a lab pod, verify access from your terminal:

```bash
kubectl --kubeconfig <staging-kubeconfig> -n istp-staging get deployments
kubectl --kubeconfig <staging-kubeconfig> -n istp-staging get ingresses
```

If private GHCR challenge images are involved, the image pull secret must already exist in
`istp-staging`, and the admin configuration must reference that secret name.

### Frontend startup

Use the normal local frontend config:

```bash
cd frontend
cp .env.example .env.local
```

Set:

```dotenv
NEXTAUTH_SECRET=<local random secret>
AUTH_KEYCLOAK_ISSUER=https://istp-staging-auth.pm4.init-lab.ch/realms/interactive-security-training-platform
AUTH_KEYCLOAK_ID=interactive-security-training-platform-app
AUTH_KEYCLOAK_SECRET=<frontend Keycloak client secret>
BACKEND_URL=http://localhost:8080
```

Then run:

```bash
npm ci
npm run dev
```

Keep the backend running with the local profile:

```bash
cd backend
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"; .\gradlew.bat bootRun
```

### Rancher checks: labels vs annotations

Lab pod Kubernetes resources are created as a deployment, service, and ingress with names
derived from `pod-<hash>`.

Selectors and ownership use stable labels. In Rancher, check the deployment, service, ingress,
and pod labels:

```text
app=istp-lab-pod
istp.pm4.ch/user-id=<keycloak user uuid>
istp.pm4.ch/lab-id=<lab uuid>
```

Lifecycle data must be annotations on the deployment, not selector labels:

```text
istp.pm4.ch/created-at-epoch=<unix seconds>
istp.pm4.ch/last-activity-at-epoch=<unix seconds>
istp.pm4.ch/base-ttl-seconds=<seconds>
istp.pm4.ch/ttl-extension-count=<count>
```

This distinction matters: labels are used by selectors and should stay stable; annotations
change when the user starts, polls, extends, or when the backend initializes missing lifecycle
metadata.

Useful `kubectl` checks:

```bash
kubectl --kubeconfig <staging-kubeconfig> -n istp-staging get deploy,svc,ingress -l app=istp-lab-pod
kubectl --kubeconfig <staging-kubeconfig> -n istp-staging describe deploy <pod-name>
kubectl --kubeconfig <staging-kubeconfig> -n istp-staging get deploy <pod-name> -o jsonpath='{.metadata.annotations}'
```

### Expected behavior

Start:

- Starting from a lab page creates one deployment, one service, and one ingress in
  `istp-staging`.
- The UI moves from `NOT_FOUND` to `PROVISIONING`, then to `RUNNING` once a replica is ready.
- Starting the same lab again is idempotent and returns the existing pod.
- Starting a second lab while one is active for the same student fails with
  `Only one lab pod can be active per student.`

Extend:

- Extend is enabled only while the pod is `RUNNING`.
- Each successful extend increments `istp.pm4.ch/ttl-extension-count`.
- The effective TTL is `base-ttl-seconds + extension-count * istp.pod-extension-seconds`.
- The default maximum is two extensions. After that, the UI should show the max-extension state
  and the backend rejects further extension attempts.

Stop:

- Stop deletes the deployment, service, and ingress for that lab pod.
- The UI should return to `NOT_FOUND` after the next status refresh.
- Stopping a missing pod is treated as no-op behavior by the backend endpoint.

Reaper:

- The scheduler starts after `istp.pod-reaper.initial-delay-ms` and repeats every
  `istp.pod-reaper.interval-ms`.
- It lists deployments with `app=istp-lab-pod` in `istp-staging`.
- It compares the current time with `istp.pm4.ch/last-activity-at-epoch` plus the effective TTL.
- Expired pods are deleted by name, including their service and ingress.
- Pods without `istp.pm4.ch/created-at-epoch` are skipped and logged, so add or recreate lifecycle
  annotations before testing reaper cleanup on old resources.

After each test run, clean up remaining staging resources:

```bash
kubectl --kubeconfig <staging-kubeconfig> -n istp-staging get deploy,svc,ingress -l app=istp-lab-pod
kubectl --kubeconfig <staging-kubeconfig> -n istp-staging delete deploy,svc,ingress -l app=istp-lab-pod
```
