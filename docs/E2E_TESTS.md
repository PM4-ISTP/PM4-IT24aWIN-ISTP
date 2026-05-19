# End-to-End Tests

End-to-end tests live in `frontend/tests/` and use Playwright. They run against a live Next.js app and Keycloak.

## Local Run

### 1. Start the App

Make sure the Next.js app is running on `http://localhost:3000`.

See [../LOCAL_DEV.md](../LOCAL_DEV.md) for the full local setup.

### 2. Install Playwright Browsers

First time only:

```bash
cd frontend
npx playwright install
```

### 3. Run Tests

Interactive UI mode:

```bash
cd frontend
npm run test:ui
```

Headless mode:

```bash
cd frontend
npm run test:e2e
```

## Run Against Staging

Pass the staging base URL with `BASE_URL`:

```bash
cd frontend
BASE_URL=https://istp-staging.pm4.init-lab.ch npm run test:e2e
```

On Windows PowerShell:

```powershell
cd frontend
$env:BASE_URL="https://istp-staging.pm4.init-lab.ch"; npm run test:e2e
```
