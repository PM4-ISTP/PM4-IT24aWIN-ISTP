# PM4-IT24aWIN-ISTP
Interactive Security Training Platform

---

## Development Environment Setup

### Prerequisites

Ensure you have the following installed:
- **Docker** — for PostgreSQL, Keycloak, and Adminer
- **Java 21+** — for the Spring Boot backend
- **Node.js 22+** — for the Next.js frontend
- **k3d** — for local Kubernetes cluster 
- **kubectl** — for interacting with Kubernetes (optional)

### Quick Start

#### 1. Start Docker Compose Services

From the project root, start PostgreSQL, Keycloak, and Adminer:

```bash
cd infra
docker compose up -d
```

To stop services:
```bash
docker compose down
```
---

#### 2. Start the Backend

From the `backend/` directory:

```bash
cd backend
./gradlew bootRun
```

The Spring Boot application starts on `http://localhost:8080`.

**Before committing**, always run code formatting:
```bash
./gradlew spotlessApply
```

---

#### 3. Start the Frontend

From the `frontend/` directory:

```bash
cd frontend
npm install
npm run dev
```

The Next.js application starts on `http://localhost:3000`.

---

### Kubernetes Setup (K3d)

#### Create a K3d Cluster

k3d is a lightweight Kubernetes distribution perfect for local development.

```bash
k3d cluster create istp
```

**Verify the cluster is running:**
```bash
k3d cluster list
kubectl cluster-info
```

#### Download and Configure Kubeconfig

Once the cluster is created, export the kubeconfig to the backend:

```bash
# From the project root (pm4/)
mkdir -p backend/src/main/resources
k3d kubeconfig get istp > backend/src/main/resources/Kubeconfig
```

**Key points:**
- The Kubeconfig file is required by the backend to communicate with the Kubernetes cluster
- The file is already `.gitignored` (contains sensitive cluster credentials)

**Verify the kubeconfig was created:**
```bash
ls -la backend/src/main/resources/Kubeconfig
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
