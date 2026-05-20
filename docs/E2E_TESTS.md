# End-to-End Tests

End-to-end tests live in `frontend/tests/` and use Playwright. They run against a live Next.js app and Keycloak. The
tests cover the happy path of the following main user flows:

* Login / logout
* Lab pod lifecycle (start, extend, stop)
* Browse course catalog
* User dashboard rendering
* Instructor dashboard (overview of courses for which an instructor is owner or collaborator) rendering
* Several lab flows (e.g. play lab, create lab)
* Several course flows (e.g. join course, create course)

Some flows have been tested in different states (e.g. zero, one or multiple entries). Edge cases or error states are not
covered. All other flows are not or only partially covered.

The file [data.ts](../frontend/tests/data.ts) contains expected test data according the test data defined in
[loadTestdata.sql](../backend/src/main/resources/loadTestdata.sql).

Playwright tests are quite flaky. To counter this, the CI reruns failed tests up to 2 times.

## Test case lifecyle

Every test case follows the same lifecycle:

1. Load test data into test database (Not test users. They are loaded separately, see below)
2. Run test case
3. Clean up test data from test database

The backend provides two endpoints for loading and cleaning up test data. The two scripts are stored in the
[resources](../backend/src/main/resources) folder of the backend. These two endpoints are only available in the local
and staging environments, not in production. This is ensured by the property `istp.features.staging-endpoint-enabled`
(see [application.properties](../backend/src/main/resources/application.properties) and
[application-local.properties.example](../backend/application-local.properties.example)). This property is only enabled
locally and on staging. If this property is disabled, the endpoints are not reachable.

## Local Run

### 1. Start the App

Make sure the Next.js app is running on `http://localhost:3000`. If you want to use the local backend for testing, you
need to start the local backend too.

See [../LOCAL_DEV.md](../LOCAL_DEV.md) for the full local setup.

### 2. Install Playwright Browsers

First time only:

```bash
cd frontend
npx playwright install
```

### 3. Load test users

Test users need to be loaded into the test database. You can do this by running the
[testUsers.sql](../infra/seed/testing/testUsers.sql) script against your local Postgres instance (or the staging
database, should it be missing the test users).

The test users are already defined in [data.ts](../frontend/tests/data.ts).

**Note:** This only modifies the PostgreSQL database. The test users still need to exist in the staging Keycloak.

### 4. Run Tests

Before you can run the tests, you need to make sure, that the required environment variables are set in `.env.local`.
You need to set the following variables (in addition to the ones mentioned in [LOCAL_DEV.md](../LOCAL_DEV.md)):

* `BACKEND_URL`: This is the backend you want to use for testing (either local or staging).
* `DATABASE_USERNAME`: This is the username for the test database. This is needed for the test data load and cleanup endpoints to work. For local testing database, this is the username of your local Postgres instance. For staging, this is the username from the Rancher secret `postgres-credentials`.
* `DATABASE_PASSWORD`: This is the password for the test database. This is needed for the test data load and cleanup endpoints to work. For local testing database, this is the password of your local Postgres instance. For staging, this is the password from the Rancher secret `postgres-credentials`.

#### Interactive UI mode:

```bash
cd frontend
npm run test:ui
```

#### Headless mode:

```bash
cd frontend
npm run test:e2e
```