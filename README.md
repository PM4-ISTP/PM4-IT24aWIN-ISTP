# PM4-IT24aWIN-ISTP
Interactive Security Training Platform

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
