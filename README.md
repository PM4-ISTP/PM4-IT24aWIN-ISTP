# Interactive Security Training Platform

Interactive Security Training Platform is a web application for creating, managing, and running hands-on security training challenges.

## Tech Stack

- Frontend: Next.js, React, TypeScript
- Backend: Spring Boot, Java 21
- Database: PostgreSQL
- Authentication: Keycloak
- Infrastructure: Docker, Kubernetes/K3d

## Quick Start

Local development uses a hybrid setup:

- PostgreSQL runs locally via Docker Compose.
- Keycloak uses the shared staging instance.

See [LOCAL_DEV.md](LOCAL_DEV.md) for the complete local setup, environment variables, and troubleshooting notes.

## Useful Links

### Staging

| Service | URL |
| --- | --- |
| App | https://istp-staging.pm4.init-lab.ch |
| Keycloak admin console | https://istp-staging-auth.pm4.init-lab.ch/admin/interactive-security-training-platform/console/ |
| Adminer | https://istp-staging-adminer.pm4.init-lab.ch |
| API docs | https://istp-staging.pm4.init-lab.ch/scalar |

### Production

| Service | URL |
| --- | --- |
| App | https://istp.pm4.init-lab.ch |
| Keycloak admin console | https://istp-auth.pm4.init-lab.ch/admin/interactive-security-training-platform/console/ |
| Adminer | https://istp-adminer.pm4.init-lab.ch |
| API docs (login required) | https://istp.pm4.init-lab.ch/api/backend/scalar |

## Common Commands

### Backend

Run commands from `backend/`.

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
./gradlew test
```

Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"; .\gradlew.bat bootRun
```

### Frontend

Run commands from `frontend/`.

```bash
npm ci
npm run dev
```

## Documentation

- [Local development](LOCAL_DEV.md)
- [User manual](docs/ISTP_Manual/ISTP_Manual.pdf)
- [Project vision](docs/Final_Vision_Doc.md)
- [Kubernetes setup](docs/KUBERNETES.md)
- [Challenge Docker images](docs/CHALLENGE_IMAGES.md)
- [Code quality](docs/CODE_QUALITY.md)
- [End-to-end tests](docs/E2E_TESTS.md)
- Kubernetes manifests: [k8s](k8s/)
- Infrastructure and local database setup: [infra](infra/)
- CI workflows: [.github/workflows](.github/workflows/)

## Branch Naming

Use descriptive branch names with one of these prefixes:

| Prefix | Use for |
| --- | --- |
| `feature/` | New features or enhancements |
| `bugfix/` | Bug fixes |
| `docs/` | Documentation changes |
| `refactor/` | Code refactoring without behavior changes |

Examples:

```text
feature/user-authentication
bugfix/login-redirect-loop
docs/update-readme
refactor/extract-auth-service
```

## Secrets

Do not commit real secrets. Keep local values in gitignored files such as:

- `frontend/.env.local`
- `infra/.env`
- `backend/src/main/resources/application-local.properties`

Committed example files should contain placeholders only.

## Checks Before Pull Requests

Run the relevant formatting, linting, and test checks before opening a pull request. See [Code quality](docs/CODE_QUALITY.md) for the full workflow and [End-to-end tests](docs/E2E_TESTS.md) for Playwright setup.
