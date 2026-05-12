#set document(
  title: "ISTP Manual",
  author: "ISTP Project Team",
)

#set page(
  paper: "a4",
  margin: (top: 2.5cm, bottom: 2.5cm, left: 2.5cm, right: 2.5cm),
  numbering: "1",
  header: [
    #set text(size: 9pt, fill: luma(130))
    #grid(
      columns: (1fr, 1fr),
      align(left)[ISTP Manual],
      align(right)[Interactive Security Training Platform],
    )
    #line(length: 100%, stroke: 0.5pt + luma(200))
  ],
)

#set text(font: "New Computer Modern", size: 11pt)
#set heading(numbering: "1.1.")
#set par(justify: true, leading: 0.65em)

#show heading.where(level: 1): it => {
  v(1.2em)
  text(size: 15pt, weight: "bold", it)
  v(0.4em)
  line(length: 100%, stroke: 1pt + luma(60))
  v(0.2em)
}

#show heading.where(level: 2): it => {
  v(0.8em)
  text(size: 12pt, weight: "bold", it)
  v(0.2em)
}

#show raw.where(block: true): block.with(
  fill: luma(245),
  inset: (x: 10pt, y: 8pt),
  radius: 4pt,
  width: 100%,
)

#show link: set text(fill: blue)
#show link: underline

// ─── Title Page ────────────────────────────────────────────────────────────

#align(center)[
  #v(3cm)
  #text(size: 11pt, fill: luma(100), tracking: 3pt)[ZHAW SCHOOL OF ENGINEERING]
  #v(0.6cm)
  #line(length: 40%, stroke: 0.5pt + luma(180))
  #v(0.6cm)
  #text(size: 32pt, weight: "bold")[ISTP Manual]
  #v(0.4em)
  #text(size: 15pt, fill: luma(60))[Interactive Security Training Platform]
  #v(1.5cm)
  #line(length: 60%, stroke: 1.5pt)
  #v(1cm)
  #text(size: 11pt, fill: luma(40))[
    *Project:* PM4 / ISTP \
    *Version:* 1.0 \
    *Date:* 2025
  ]
  #v(3cm)
  #text(size: 10pt, fill: luma(100))[
    Deployment and configuration manual for the ISTP project. \
    Intended for developers who need to set up or extend the platform.
  ]
]

#pagebreak()
#outline(depth: 2, indent: 1.5em)
#pagebreak()

// ─── 1. Project Overview ───────────────────────────────────────────────────

= Project Overview

ISTP (Interactive Security Training Platform) is a web-based CTF (Capture the Flag) learning platform built as part of the PM4 project at ZHAW. It allows students to solve cybersecurity challenges in an isolated, containerized environment.

The platform consists of the following components:

#figure(
  table(
    columns: (auto, auto, 1fr),
    stroke: 0.5pt + luma(180),
    inset: 8pt,
    fill: (col, row) => if row == 0 { luma(230) } else { white },
    [*Component*], [*Technology*], [*Role*],
    [Frontend], [Next.js], [User interface, login, challenge browser],
    [Backend], [Spring Boot], [REST API, business logic, challenge management],
    [Auth], [Keycloak 26.5.6], [Login, user management, role-based access],
    [Database], [PostgreSQL], [Persistent data storage],
    [Infrastructure], [Kubernetes], [Container orchestration and deployment],
  ),
  caption: [Platform components],
)

The platform is deployed on two environments:

- *Production:* `https://istp.pm4.init-lab.ch`
- *Staging:* `https://istp-staging.pm4.init-lab.ch`

// ─── 2. Keycloak ───────────────────────────────────────────────────────────

= Keycloak

Keycloak handles all authentication and authorization. The realm is called `interactive-security-training-platform` and contains all users, roles, and client configurations.

== Realm Settings

The most important settings in the realm:

#figure(
  table(
    columns: (auto, auto, 1fr),
    stroke: 0.5pt + luma(180),
    inset: 8pt,
    fill: (col, row) => if row == 0 { luma(230) } else { white },
    [*Setting*], [*Value*], [*Why*],
    [Access Token Lifespan], [15 min], [Short-lived for security; frontend refreshes automatically],
    [SSO Session Idle], [1 hour], [User gets logged out after 1 hour of inactivity],
    [SSO Session Max], [10 h], [Absolute session limit regardless of activity],
    [Refresh Token Reuse], [1 (single-use per rotation)], [Prevents token replay attacks],
    [Brute Force Protection], [Enabled, 10 attempts], [Locks account temporarily after 10 failed logins],
    [SSL Required], [All], [HTTPS enforced for all traffic including localhost],
    [Self-Registration], [Enabled], [Users can create their own accounts],
  ),
  caption: [Key realm settings],
)

== Roles

Three custom roles control what users can do on the platform:

#figure(
  table(
    columns: (auto, 1fr),
    stroke: 0.5pt + luma(180),
    inset: 8pt,
    fill: (col, row) => if row == 0 { luma(230) } else { white },
    [*Role*], [*Access*],
    [`ROLE_STUDENT`], [Can access and solve CTF challenges, view own progress],
    [`ROLE_INSTRUCTOR`], [Can create and manage challenges, view all participants],
    [`ROLE_ADMINISTRATOR`], [Full access: user management, roles, all platform resources],
  ),
  caption: [Custom realm roles],
)

*Role policy:* Roles are additive. Every user always keeps `ROLE_STUDENT`. Additional roles (`ROLE_INSTRUCTOR`, `ROLE_ADMINISTRATOR`) are assigned on top and can be revoked individually via the app.

*Self-registration:* New users who self-register automatically receive `ROLE_STUDENT` (configured as a default realm role in Keycloak).

Roles are included in the JWT under `realm_access.roles`.

Note: Keycloak *groups* are not used for authorization in the ISTP backend. Avoid mapping `ROLE_*` via group role-mappings, otherwise users can end up with multiple roles.

== Clients

Two custom clients are configured. All other clients (`account`, `broker`, etc.) are Keycloak built-ins and should not be touched.

*`nextjs`* is the active client used by the Next.js frontend (NextAuth). It uses the Authorization Code Flow so the user is redirected to Keycloak to log in, then back to the app. It is a confidential client, meaning it has a client secret stored in a Kubernetes Secret. The older `interactive-security-training-platform-app` client also exists in the realm but `nextjs` is the one currently in use.

*`istp-backend`* is used by the Spring Boot backend for service-to-service calls to the Keycloak *Admin REST API* (Client Credentials Flow). It is a confidential client with *Service Accounts* enabled. The service account needs `realm-management` roles such as `manage-users` (and usually `view-users` / `query-users`; `view-clients` is optional for session listing).

The frontend client uses the following allowed redirect URIs and web origins:

```
http://localhost:3000/*                   (local dev)
https://istp.pm4.init-lab.ch/*           (production)
https://istp-staging.pm4.init-lab.ch/*   (staging)
```

== Setting Up Keycloak from Scratch

The realm config export is stored in `infra/keycloak-export/interactive-security-training-platform-realm.json` in the repository. To restore it:

+ Open the Keycloak Admin Console at `https://<host>/admin`.
+ Click *Create realm* and upload the realm export JSON.
+ Click *Create*.

After importing, regenerate the client secrets (they are not stored in the export):

+ Go to *Clients* > `nextjs` > *Credentials* > *Regenerate*.
+ Repeat for `istp-backend`.
+ Store both secrets in the Kubernetes Secrets (see @sec-env).

== Required Environment Variables <sec-env>

*Next.js:*
```bash
AUTH_KEYCLOAK_ID=nextjs
AUTH_KEYCLOAK_SECRET=<secret>
AUTH_KEYCLOAK_ISSUER=https://<keycloak-host>/realms/interactive-security-training-platform
NEXTAUTH_URL=https://istp.pm4.init-lab.ch
NEXTAUTH_SECRET=<random-string>
```

*Spring Boot:*
```bash
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://<keycloak-host>/realms/interactive-security-training-platform

KEYCLOAK_ADMIN_BASE_URL=https://<keycloak-host>
KEYCLOAK_ADMIN_REALM=interactive-security-training-platform
KEYCLOAK_ADMIN_CLIENT_ID=istp-backend
KEYCLOAK_ADMIN_CLIENT_SECRET=<secret>

# Used for session listing in the admin dashboard
KEYCLOAK_APP_CLIENT_ID=nextjs
```

*Kubernetes Secrets (recommended):*
- `nextauth-secret`: contains `NEXTAUTH_SECRET` and `AUTH_KEYCLOAK_SECRET`
- `keycloak-admin-api-client`: contains `client-secret` (used as `KEYCLOAK_ADMIN_CLIENT_SECRET` in the backend pod)

== Keycloak / PostgreSQL Synchronization

Keycloak is the source of truth for authentication and base user identity. PostgreSQL stores a *shadow/projection* user row to support fast reads and joins with domain data (courses, enrollments).

*Source of truth:*
- *Keycloak:* password/login, user id (`sub`), realm roles, base profile (`email`, `username`, `firstName`, `lastName`), attributes (`title`, `picture` URL)
- *PostgreSQL:* app domain data + `users` projection + `deletedAt` (soft delete)

*Just-in-time provisioning (first API request after login):*
+ Backend validates JWT and reads `sub` (Keycloak user id).
+ Backend reads roles from `realm_access.roles` and requires an app role (`ROLE_*`).
+ Backend checks if a `users` row exists in Postgres.
+ If missing and the user is `ROLE_STUDENT`, the backend automatically creates the shadow row.
+ If `deletedAt` is set, access is denied.

*Profile updates (via app, not Keycloak Account UI):*
- Endpoint: `PUT /api/v1/users/me/profile`
- Backend updates Keycloak first (Admin API), then updates the Postgres `users` row.
- If the DB update fails, the backend attempts to rollback Keycloak to the previous snapshot.

*Admin user management (via app, not Keycloak console):*
- Create/provision users, assign or revoke roles (additive: `ROLE_STUDENT` always remains), password reset email, set password, list/logout sessions.
- *Disable (reversible):* disables Keycloak user and sets `deletedAt`
- *Restore (reversible):* re-enables Keycloak user and clears `deletedAt`
- *Soft-delete (irreversible):* anonymizes email/username in Keycloak+DB, disables user, sets `deletedAt` (frees up the original email/username for reuse)

== Mail Server

Keycloak is connected to a dedicated Gmail account (`istp.noreply@gmail.com`) for sending system emails such as password resets.

#figure(
  table(
    columns: (auto, 1fr),
    stroke: 0.5pt + luma(180),
    inset: 8pt,
    fill: (col, row) => if row == 0 { luma(230) } else { white },
    [*Setting*], [*Value*],
    [SMTP Host], [`smtp.gmail.com`],
    [Port], [`587` (STARTTLS)],
    [From Address], [`istp.noreply@gmail.com`],
    [Authentication], [Basic (Google App Password)],
  ),
  caption: [SMTP configuration],
)

A dedicated Gmail account was created for this project. The password in Keycloak is a *Google App Password*, not the regular account password -- Google requires this for SMTP access.

*Known issue:* Within the ZHAW network, outbound SMTP on port 587 is blocked. Password reset emails will therefore not be delivered when the platform runs on ZHAW infrastructure. This is a ZHAW network restriction, not a Keycloak misconfiguration.

// ─── 3. Challenge Pods ────────────────────────────────────────────────────

= Challenge Pods

The challenges run in Kubernetes pods. The #link("https://github.com/PM4-ISTP/PM4-IT24aWIN-ISTP/tree/main?tab=readme-ov-file#kubernetes-setup-k3d")[README] explains how you can set up a k3d cluster for local development.

To create a pod, you need to send a request to the backend using a request of the following form:

```json
{
    "containerName": "test",
    "image": "nginx:latest",
    "podName": "nginx",
    "containerPort": 80
}
```

When you create a pod, a JSON response gets returned by the backend server. The response contains two links. `appUrl` leads to the app running in the pod. The app is specified by the `image` attribute from the request. `terminalUrl` leads to the terminal of the app. To login to the terminal, you need to input `student` as the username and the `terminalPassword` from the response as the password.

Following is an example for a response:

```json
{
    "status": "CREATED",
    "podName": "challenge-dfda5bef",
    "namespace": "default",
    "message": "Deployment, Service, and Ingress created successfully",
    "appUrl": "http://app-dfda5bef.127.0.0.1.nip.io",
    "terminalUrl": "http://term-dfda5bef.127.0.0.1.nip.io",
    "terminalPassword": "3627ef4c-405"
}
```

The terminal does not run in the same container as the app. But they run in the same network. While you can access `localhost` of the app from the terminal (e.g., using `curl "localhost"`), you cannot access the file system of the app.

// ─── 4. Permissions Reference ─────────────────────────────────────────────

= Permissions Reference

== Role-Based Access

The following table shows what each Keycloak realm role is allowed to do across the platform. ROLE_STUDENT is always present on every user — ROLE_INSTRUCTOR and ROLE_ADMINISTRATOR are additive on top.

#set text(size: 9.5pt)
#figure(
  table(
    columns: (2.8fr, 1fr, 1fr, 1fr),
    stroke: 0.5pt + luma(180),
    inset: (x: 7pt, y: 6pt),
    align: (left, center, center, center),
    fill: (col, row) => if row == 0 { luma(220) } else if calc.odd(row) { luma(248) } else { white },
    [*Action*], [*Student*], [*Instructor*], [*Administrator*],
    [Browse course catalog],          [✓], [✓], [✓],
    [Enroll in / leave courses],      [✓], [✓], [✓],
    [Play labs & use pods],           [✓], [✓], [✓],
    [Submit flags],                   [✓], [✓], [✓],
    [View own badges],                [✓], [✓], [✓],
    [Create courses],                 [–], [✓], [✓],
    [Create & manage labs],           [–], [✓], [✓],
    [View course submissions],        [–], [✓], [✓],
    [Invite collaborators (own course only)], [–], [✓], [✓],
    [Manage platform topics],         [–], [–], [✓],
    [User management],                [–], [–], [✓],
    [Assign / revoke roles],          [–], [–], [✓],
    [Disable / delete users],         [–], [–], [✓],
    [Admin panel],                    [–], [–], [✓],
  ),
  caption: [Role-based permission matrix],
)
#set text(size: 11pt)

== Course-Level: Owner vs. Collaborator

Within a course, instructors have one of two course-level roles. The creator of a course is automatically the *Owner*. Additional instructors can be invited as *Collaborators* — they must accept the invitation before gaining access.

#set text(size: 9.5pt)
#figure(
  table(
    columns: (2.8fr, 1fr, 1fr),
    stroke: 0.5pt + luma(180),
    inset: (x: 7pt, y: 6pt),
    align: (left, center, center),
    fill: (col, row) => if row == 0 { luma(220) } else if calc.odd(row) { luma(248) } else { white },
    [*Action*], [*Owner*], [*Collaborator*],
    [View course (instructor view)],      [✓], [✓],
    [Edit course settings & description], [✓], [✓],
    [Manage labs (add / remove)],         [✓], [✓],
    [View participants & submissions],    [✓], [✓],
    [Invite / remove collaborators],      [✓], [–],
    [Configure badge],                    [✓], [–],
    [Delete course],                      [✓], [–],
    [Requires invitation acceptance],     [–], [✓],
  ),
  caption: [Course-level permission matrix (Owner vs. Collaborator)],
)
#set text(size: 11pt)

// ─── 5. User Guide: Student ───────────────────────────────────────────────

= User Guide: Student

This section describes the platform from a student's perspective — from creating an account to solving challenges.

== Registration & Login

Open the platform at `https://istp.pm4.init-lab.ch`. Click *Sign in* to be redirected to the Keycloak login page.

*New account:* Click *Register* on the Keycloak login page. Fill in your first name, last name, university email address, and a password. After submitting, Keycloak sends a verification email to the provided address. The account is only active after clicking the confirmation link in that email.

*Password requirements:* minimum 8 characters, at least one uppercase letter, one digit, and one special character. The password may not contain your username.

*Failed logins:* After 10 consecutive failed login attempts the account is temporarily locked. Wait a few minutes before trying again.

After successful login you are redirected to the dashboard.

== Course Catalog

Navigate to *Catalog* in the sidebar. The catalog shows all published courses on the platform. Each course card displays the title, topic, and a short description.

Use the search bar to filter courses by title. Use the topic dropdown to filter by subject area. Results are paginated — use the page controls at the bottom to navigate between pages.

Click on a course card to open the course detail page where you can read the full description and enroll.

== My Courses

Navigate to *My Courses* in the sidebar to see all courses you are currently enrolled in. Click on a course to open it and see its labs and your progress.

To leave a course, open the course detail page and click *Leave course*.

== Starting a Lab (Pod Launcher)

Inside a course, click *Play* on any lab to open the lab view. The screen is split into two panels:

- *Left panel* — lab description, challenge tasks, flag submission
- *Right panel* — the live lab environment (pod)

To start the lab environment, click the play button (▶) in the top-right of the right panel. The pod goes through the following states:

#figure(
  table(
    columns: (auto, 1fr),
    stroke: 0.5pt + luma(180),
    inset: 8pt,
    fill: (col, row) => if row == 0 { luma(230) } else { white },
    [*Status*], [*Meaning*],
    [`NOT_FOUND`], [Pod not started yet],
    [`PROVISIONING`], [Pod is starting up — wait a moment],
    [`RUNNING`], [Pod is ready — lab app link is active],
    [`TERMINATING`], [Pod is shutting down],
    [`FAILED`], [Pod failed to start — click the retry button (↺)],
  ),
  caption: [Pod status overview],
)

Once the status shows *RUNNING*, the *Open app* button becomes active. Click it to open the lab application in a new browser tab.

To stop the pod manually, click the stop button (■) in the right panel header.

== Keep-Alive & Pod Management

While a lab pod is running, the platform polls its status every 60 seconds. If the pod has been idle for an extended period, it will be terminated automatically to free up cluster resources. Return to the lab page and restart the pod to continue working.

== Solving Challenges

Each lab contains one or more challenges. Navigate between them using the stepper at the top of the left panel — all steps are freely accessible regardless of order. A progress bar shows how many challenges you have completed.

There are three challenge types:

*Flag challenge* — Exploit the running lab environment to find a hidden flag. The flag has the format `ISTP{...}`. Enter it in the *Submit Flag* field (you can enter either `ISTP{flag}` or just `flag` — both are accepted) and press *Submit* or hit Enter. A green notification confirms a correct submission; a red notification means the flag is wrong — try again.

*Multiple choice* — Read the question and select one of the options. Click *Submit answer* to confirm. Depending on the lab configuration there are two modes:
- *Unlimited attempts:* wrong answers can be retried.
- *Once:* only one attempt is allowed; the correct answer is revealed after a wrong submission.

*Theory task* — Read the description and click *Mark as done* to complete the task. No flag required.

Each solved challenge shows a *Solved* badge and awards the configured points. When all challenges in a lab are solved, a completion banner is shown.

If a challenge has a hint available, click *Show hint* to reveal it.

== Progress & Points

The left panel shows your current progress for the open lab: solved challenges out of total, and earned points out of maximum points. A teal progress bar fills as you solve challenges. When the lab is fully completed, the bar turns teal and a trophy icon appears.

To view your overall progress across all courses, return to the course page where each lab card shows its completion status.

== Trophy Cabinet (Badges)

ISTP supports *course completion badges*. A badge represents “user X completed course Y” and is shown in the *Trophy Cabinet*.

*What a badge means:*
- Badges are *per course* (not per lab).
- A course is considered *completed* when the user has solved *all challenges across all labs* that belong to the course.

*When a user receives a badge (our decision):*
- A user receives the badge for a course *only if* `badgeEnabled = true` for that course at the time the badge is awarded.
- Badges are awarded in two situations:
  + *On completion while enrolled:* when the user solves the last remaining challenge required to complete the course.
  + *On enrollment (already completed):* when the user enrolls in a course, ISTP checks whether the course is already completed for that user. If the course is already completed and `badgeEnabled = true`, the badge is awarded immediately.

*No retroactive awarding (“no backfill”):*
- If a user completed a course while `badgeEnabled = false` and the instructor enables badges later, ISTP does *not* retroactively award the badge automatically.

*Shared challenges across courses (important):*
- Challenge completions are stored *per user + challenge*, not per course.
- If two courses reference the *same challenge IDs*, solving a challenge once can count as solved in both courses.
- The enrollment-time badge check ensures users still receive the course badge (when enabled) even if there is no “new solve” event inside the second course.

// ─── 5. User Guide: Instructor ────────────────────────────────────────────

= User Guide: Instructor

== Creating a Challenge

== Configuring a Challenge (Ports, Environment Variables, Image)

== Publishing & Archiving a Challenge

== Creating a Course & Adding Challenges

=== Course Badges

Course owners can configure the badge for their course (icon, colors, template) and enable or disable awarding via `badgeEnabled`.

- If `badgeEnabled = true`, students can earn the badge when they complete the course (see Student guide above).
- If `badgeEnabled = false`, ISTP will not award new badges for this course.
- Enabling badges later does not retroactively award badges for users who already completed the course while badges were disabled (no backfill).

// ─── 6. User Guide: Admin ─────────────────────────────────────────────────

= User Guide: Admin

== User Overview

== Assigning & Revoking ROLE_INSTRUCTOR

== Assigning & Revoking ROLE_ADMINISTRATOR

== Deleting & Disabling Users

// ─── 7. Architecture ──────────────────────────────────────────────────────

= Architecture

== Component Overview

== Authentication & Authorization (Keycloak, JWT, OIDC)

== Frontend (Next.js + Mantine)

== Backend (Spring Boot)

== Kubernetes Pod Lifecycle

== Database Schema (PostgreSQL)

// ─── 8. API Documentation ─────────────────────────────────────────────────

= API Documentation

== Authentication & Token Handling

== Challenge Endpoints

== Pod Endpoints

== User Endpoints

== Course Endpoints

// ─── 10. Setup Checklist ───────────────────────────────────────────────────

= Setup Checklist

#let check = box(width: 10pt, height: 10pt, stroke: 0.7pt)

#table(
  columns: (auto, 1fr),
  stroke: none,
  inset: (x: 4pt, y: 5pt),
  [#check], [Keycloak 26.x running and reachable],
  [#check], [Realm imported from `infra/keycloak-export/interactive-security-training-platform-realm.json`],
  [#check], [Client secrets regenerated for `interactive-security-training-platform-app` and `istp-backend`],
  [#check], [Secrets stored in Kubernetes Secrets],
  [#check], [Environment variables set for Next.js and Spring Boot],
  [#check], [Redirect URIs updated if using a new domain],
  [#check], [At least one `ROLE_ADMINISTRATOR` user assigned],
  [#check], [HTTPS certificate in place],
)
