# Code Quality

This project uses separate quality checks for the backend and frontend. CI runs these checks on pushes and pull requests.

## Backend

The backend uses Spotless, Checkstyle, PMD, and SpotBugs through Gradle.

Run commands from `backend/`.

### Daily Workflow

Format backend code before committing:

```bash
./gradlew spotlessApply
```

Check everything without modifying files:

```bash
./gradlew spotlessCheck checkstyleMain pmdMain spotbugsMain
```

### Individual Tools

Format code:

```bash
./gradlew spotlessApply
```

Check formatting:

```bash
./gradlew spotlessCheck
```

Run Checkstyle:

```bash
./gradlew checkstyleMain
```

Checkstyle violations fail the build. The HTML report is written to `build/reports/checkstyle/main.html`.

Run PMD:

```bash
./gradlew pmdMain
```

PMD findings are warnings. The report is written to `build/reports/pmd/main.html`.

Run SpotBugs:

```bash
./gradlew spotbugsMain
```

SpotBugs findings are warnings. The report is written to `build/reports/spotbugs/main.html`.

### Suppressing False Positives

Prefer fixing findings. If a finding is genuinely not applicable, suppress it inline:

```java
@SuppressWarnings("PMD.RuleName")
public void myMethod() { ... }

@SuppressFBWarnings("BUG_PATTERN_CODE")
public void myMethod() { ... }

@SuppressWarnings("checkstyle:RuleName")
public void myMethod() { ... }
```

If the same false positive appears across many files, update the shared config instead:

- PMD: `backend/config/pmd/ruleset.xml`
- SpotBugs: `backend/config/spotbugs/exclude.xml`
- Checkstyle: `backend/config/checkstyle/suppressions.xml`

## Frontend

The frontend uses Prettier and ESLint through npm scripts. Husky and lint-staged run checks on staged files before commits.

Run commands from `frontend/`.

### Daily Workflow

Format frontend code before committing:

```bash
npm run format
```

Check everything without modifying files:

```bash
npm run format:check
npm run lint
```

### Individual Tools

Format code:

```bash
npm run format
```

Check formatting:

```bash
npm run format:check
```

Run ESLint:

```bash
npm run lint
```

Fix auto-fixable ESLint issues:

```bash
npm run lint:fix
```

### Suppressing False Positives

Prefer fixing findings. If a finding is genuinely not applicable, suppress it inline:

```typescript
// eslint-disable-next-line rule-name
const variable = someValue;

/* eslint-disable rule-name */
const anotherValue = someValue;
/* eslint-enable rule-name */
```

If the same false positive appears across many files, update the shared config instead:

- ESLint: `frontend/eslint.config.mjs`
- Prettier: `frontend/.prettierrc` or `package.json`

## CI

The GitHub Actions workflows live in `.github/workflows/`.

Backend quality checks run Spotless, Checkstyle, PMD, and SpotBugs. PMD and SpotBugs produce reports without blocking the build.

Frontend quality checks run Prettier and ESLint.
