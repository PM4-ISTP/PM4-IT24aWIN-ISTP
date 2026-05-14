# PM4-IT24aWIN-ISTP

Interactive Security Training Platform

---

## Getting Started

This README explains how to run the project locally for development.

Authentication uses the shared staging Keycloak by default (local Keycloak is intentionally not supported).

For the exact local dev steps (hybrid local Postgres + staging Keycloak), see `LOCAL_DEV.md`.


### Prerequisites

- Node.js 22+ (frontend)
- Java 21+ (backend, optional)
- Docker Desktop (optional, only if you run a local DB via Docker Compose)

### Staging

| Service | URL |
| --- | --- |
| App | https://istp-staging.pm4.init-lab.ch |
| Keycloak admin console | https://istp-staging-auth.pm4.init-lab.ch/admin/interactive-security-training-platform/console/ |

Secrets:
- `NEXTAUTH_SECRET`: generate locally (e.g. `openssl rand -base64 32`)
- `AUTH_KEYCLOAK_SECRET`: Keycloak admin console -> Client `interactive-security-training-platform-app` -> Credentials

### Local Development

See `LOCAL_DEV.md`.

For local testing of lab pod lifecycle changes against the staging Kubernetes cluster, see
`LOCAL_DEV.md#5-test-lab-pod-lifecycle-changes-against-staging-kubernetes`.

### Kubernetes Setup (K3d)

#### Create a K3d Cluster

k3d is a lightweight Kubernetes distribution perfect for local development.

```bash
k3d cluster create istp --port "80:80@loadbalancer" --port "443:443@loadbalancer"
```

**Verify the cluster is running:**

```bash
k3d cluster list
kubectl cluster-info
```

#### Adminer (Kubernetes)

Adminer is included in the `k8s/overlays/staging` and `k8s/overlays/prod` overlays as a `ClusterIP` service (not exposed via Ingress).
Access it via port-forwarding:

```bash
kubectl -n istp-prod port-forward svc/adminer 8888:8080
```

Then open `http://localhost:8888` and connect to Postgres using host `postgres` and your namespace credentials.

#### kubectl on Windows 11

If you want to use kubectl on Windows, you need to go to the file `.kube/config` in your local user folder. There you need to replace `server: https://host.docker.internal:62824` with `server: https://127.0.0.1:62824`. The port might differ. Just use the port that is specified by the `config` file.

#### Download and Configure Kubeconfig

Once the cluster is created, export the kubeconfig to the backend.

> **Backend Keycloak Admin API secret (Kubernetes):**
> Create a secret named `keycloak-admin-api-client` with key `client-secret` in your namespace.
> Template: `k8s/secrets/keycloak-admin-api-client.secret.example.yaml`

**Linux:**

On Linux you can run the following command:

```bash
# From the project root (pm4/)
mkdir -p backend/src/main/resources
k3d kubeconfig get istp | sed 's/0\.0\.0\.0/127.0.0.1/g' > backend/src/main/resources/Kubeconfig
```

> **Note:** The `sed` command replaces `0.0.0.0` with `127.0.0.1` in the server address. k3d generates kubeconfigs with `0.0.0.0` as the host, which is a valid bind address for a server but not a valid destination for client connections — the Java Kubernetes client will fail with "Host is down" without this substitution.

**Windows 11:**

If you are on Windows 11, you need to run the following command (in Powershell):

```powershell
# From the project root (pm4/)
New-Item -ItemType Directory -Path backend/src/main/resources -Force
k3d kubeconfig get istp > backend/src/main/resources/Kubeconfig
```

After running the command, please open the Kubeconfig file in `backend/src/main/resources/Kubeconfig` and replace `server: https://host.docker.internal:62824` with `server: https://127.0.0.1:62824`. The port might differ. Just use the port that is specified by your Kubeconfig.

**Key points:**

- The Kubeconfig file is required by the backend to communicate with the Kubernetes cluster
- The file is already `.gitignored` (contains sensitive cluster credentials)
- Re-run this command after every cluster restart — k3d assigns a new random port each time

**Verify the kubeconfig was created:**

```bash
ls -la backend/src/main/resources/Kubeconfig
```

### Challenge Docker Images

Challenge labs use container images from GitHub Container Registry (GHCR). Instructors should
publish images in their own GitHub user or organization namespace, for example:

```text
ghcr.io/school-org/sql-injection-lab:1.0.0
```

The platform intentionally accepts GHCR images from any owner, not only packages from this
repository. This keeps image ownership with the instructor or school that created the lab.

Current policy:

- Images must be public and anonymously readable.
- Private GHCR images are supported when the Kubernetes namespace has a pull secret and the admin
  configuration contains that secret name.
- Tags are supported, but digest references are preferred for reproducible labs, for example
  `ghcr.io/school-org/sql-injection-lab@sha256:<digest>`.
- Examples may live under the project namespace, but production course images should be owned by
  the instructor or school running the course.

To use private GHCR images, create one pull secret in the namespace where challenge pods run:

```bash
kubectl create secret docker-registry ghcr-pull-secret \
  --docker-server=ghcr.io \
  --docker-username=<github-user-or-bot> \
  --docker-password=<personal-access-token> \
  --docker-email=<email>
```

Then enter `ghcr-pull-secret` as the image pull secret in the admin configuration. The GitHub user
or bot behind the token must have `read:packages` access to each private package. For new
instructor-owned private repositories or organizations, the instructor must grant that pull account
access before the lab can start.

How this works:

- The secret is stored only in Kubernetes. The application stores the secret name, not the GHCR
  token.
- When a student starts a challenge, the backend adds `imagePullSecrets` to the generated
  Kubernetes deployment.
- Kubernetes uses that secret to authenticate against `ghcr.io` while pulling the challenge image.
- Public images still work without a pull secret. Private images need both the configured secret and
  package access for the GitHub user or bot in that secret.

Responsibilities:

- Platform/admin team: create the Kubernetes pull secret in the challenge namespace and enter its
  name in the admin configuration.
- Instructor/school: publish the challenge image under their own GitHub user or organization and
  grant the pull account access if the package is private.
- For a new private instructor repository, no code change is needed, but the pull account must be
  allowed to read that package before the lab is assigned.

Troubleshooting:

- `ImagePullBackOff` or `ErrImagePull` usually means the secret is missing from the namespace, the
  secret name in the admin configuration is wrong, or the GitHub user or bot cannot read the
  package.
- A `401` or `403` during image validation without a configured pull secret means the image is
  private and cannot be accepted yet.
- Prefer digest-pinned images for stable course material. Tags like `latest` can move over time.

## Branch Naming

All branches must use one of the following prefixes:

| Prefix      | Use for                                   |
| ----------- | ----------------------------------------- |
| `feature/`  | New features or enhancements              |
| `bugfix/`   | Bug fixes                                 |
| `docs/`     | Documentation changes                     |
| `refactor/` | Code refactoring without behavior changes |

**Examples:**

```
feature/user-authentication
bugfix/login-redirect-loop
docs/add-branch-naming-section
refactor/extract-auth-service
```

---

## Backend Code Quality

The backend uses four tools to enforce consistent formatting, style, and code quality. All tools are integrated into Gradle and run automatically in CI on every push and pull request.

### Daily Workflow

**Before every commit**, run:

```bash
cd backend
./gradlew spotlessApply
```

That's it. Spotless reformats your code in-place so you never have to think about formatting again. Then commit normally.

To check everything is clean without modifying files (e.g. in a pre-push hook):

```bash
./gradlew spotlessCheck checkstyleMain pmdMain spotbugsMain
```

---

### Running Individual Tools

All commands must be run from the `backend/` directory.

**Format code (rewrites files):**

```bash
./gradlew spotlessApply
```

**Check formatting without changing files:**

```bash
./gradlew spotlessCheck
```

Fails if any file is not formatted. This is what CI runs.

**Checkstyle (style & naming):**

```bash
./gradlew checkstyleMain
```

Fails on violations. Human-readable report: `build/reports/checkstyle/main.html`.

**PMD (code smells):**

```bash
./gradlew pmdMain
```

Warns only — does not fail the build. Violations print to the terminal. Report: `build/reports/pmd/main.html`.

**SpotBugs (bug patterns):**

```bash
./gradlew spotbugsMain
```

Warns only — does not fail the build. Report: `build/reports/spotbugs/main.html`.

**Run all tools at once:**

```bash
./gradlew spotlessCheck checkstyleMain pmdMain spotbugsMain
```

---

### Suppressing False Positives

If a finding is genuinely not applicable to your code, suppress it inline rather than weakening the shared config:

```java
// Suppress a PMD rule on a method
@SuppressWarnings("PMD.RuleName")
public void myMethod() { ... }

// Suppress a SpotBugs pattern on a method
@SuppressFBWarnings("BUG_PATTERN_CODE")
public void myMethod() { ... }

// Suppress a Checkstyle rule on a method (rare — prefer fixing the code)
@SuppressWarnings("checkstyle:RuleName")
public void myMethod() { ... }
```

If the same false positive appears across **many files** (e.g. a framework-specific pattern), add it to the shared config file instead:

- PMD → `backend/config/pmd/ruleset.xml` (`<exclude name="..."/>` inside the relevant rule ref)
- SpotBugs → `backend/config/spotbugs/exclude.xml` (add a `<Match>` block)
- Checkstyle → `backend/config/checkstyle/suppressions.xml` (add a `<suppress>` entry)

---

### CI Pipeline

The `backend-lint-and-format` GitHub Actions job runs in parallel with the frontend lint job on every push and pull request.

```
backend-lint-and-format
├── 1. Spotless check     → fails PR if any file is not formatted
├── 2. Checkstyle         → fails PR on any style / naming violation
├── 3. PMD                → logs warnings, never blocks the PR
└── 4. SpotBugs           → logs warnings, never blocks the PR
```

HTML reports for all four tools are uploaded as a GitHub Actions artifact named **`backend-analysis-reports`** and retained for 7 days. Download them from the "Artifacts" section of any workflow run to investigate PMD or SpotBugs findings.

---

## Frontend Code Quality

The frontend uses two tools to enforce consistent formatting, style, and code quality. All tools are integrated into npm and run automatically on commit via Husky hooks.

### Daily Workflow

**Before every commit**, run:

```bash
cd frontend
npm run format
```

That's it. Prettier reformats your code in-place so you never have to think about formatting again. Then commit normally.

Husky automatically runs ESLint and Prettier on staged files before each commit. To check everything is clean without modifying files:

```bash
npm run format:check && npm run lint
```

---

### Running Individual Tools

All commands must be run from the `frontend/` directory.

**Format code (rewrites files):**

```bash
npm run format
```

**Check formatting without changing files:**

```bash
npm run format:check
```

Fails if any file is not formatted. This is what CI runs.

**ESLint (code quality & style):**

```bash
npm run lint
```

Fails on violations. Errors print to the terminal.

**Fix ESLint issues automatically:**

```bash
npm run lint:fix
```

Automatically fixes auto-fixable violations. Some violations require manual intervention.

**Run all tools at once:**

```bash
npm run format:check && npm run lint
```

---

### Suppressing False Positives

If a finding is genuinely not applicable to your code, suppress it inline rather than weakening the shared config:

```typescript
// Suppress an ESLint rule on a line
// eslint-disable-next-line rule-name
const variable = someValue;

// Suppress an ESLint rule on a block
/* eslint-disable rule-name */
const variable = someValue;
/* eslint-enable rule-name */
```

If the same false positive appears across **many files** (e.g. a framework-specific pattern), add it to the shared config file instead:

- ESLint → `frontend/eslint.config.mjs` (add rule overrides or ignore patterns)
- Prettier → `frontend/.prettierrc` or `package.json` (if created)

---

### CI Pipeline

The `frontend-lint` GitHub Actions job runs in parallel with the backend lint job on every push and pull request.

```
frontend-lint
├── 1. Prettier check   → fails PR if any file is not formatted
└── 2. ESLint          → fails PR on any code quality / style violation
```

---

## Playwright E2E Tests

End-to-end tests live in `frontend/tests/` and run against a live Next.js app + Keycloak.

See [Staging URLs](#staging-urls) for the list of staging service URLs.

### Running the tests locally

#### 1. Start the app

Make sure the Next.js app is running on `http://localhost:3000` (see `LOCAL_DEV.md`).

#### 2. Install Playwright browsers (first time only)

```bash
cd frontend
npx playwright install
```

#### 3. Run the tests

```bash
# Interactive UI mode (recommended during development)
cd frontend
npm run test

# Headless mode (CI-style)
npm run test:e2e
```

#### Running against staging

Pass the staging base URL via the `BASE_URL` environment variable:

```bash
BASE_URL=https://istp-staging.pm4.init-lab.ch npm run test:e2e
```
