#set document(
  title: "ISTP Manual",
  author: "ISTP Project Team",
)

#set page(
  paper: "a4",
  margin: (top: 2.2cm, bottom: 2.2cm, left: 2.2cm, right: 2.2cm),
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

#set text(font: "New Computer Modern", size: 10pt)
#set heading(numbering: "1.1.")
#set par(justify: true, leading: 0.58em)

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
    *Date:* 17.05.2026
  ]
  #v(3cm)
  #text(size: 10pt, fill: luma(100))[
    Complete manual for the ISTP project, covering platform concepts, \
    user guides (student, instructor, admin), Keycloak setup, and deployment.
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

== Platform Concepts

ISTP is built around three nested concepts: *courses*, *labs*, and *challenges*.

A *course* is the top-level unit that students enroll in. It groups one or more independent labs into a coherent learning path. Each course has a topic, a description, and optionally a completion badge. Courses can be *Public* (visible in the catalog, open to everyone) or *Private* (accessible only via a 6-character invite code shared by the instructor). Public courses are typically used for open knowledge-sharing where anyone is welcome to join. Private courses are used for controlled settings such as graded practicals where only a specific group of students should have access.

A *lab* is a self-contained exercise. Each lab runs a Docker image as a Kubernetes pod; the running container is the hands-on environment the student interacts with. One lab can contain multiple challenges, and the same lab can be reused across different courses.

A *challenge* is a single task inside a lab. There are three types:

- *Theory task:* the student reads the description and clicks "Mark as done". No flag required. Used for introductions, explanations, or reading tasks.
- *Flag challenge:* the student must find a hidden flag inside the running lab environment and submit it. The flag has the format `ISTP{...}`. Only the correct flag awards points; wrong submissions score nothing.
- *Multiple Choice:* the student selects one answer from a list. Whether wrong answers are penalised depends on the course setting:
  - *Unlimited attempts (self-learning):* the student can retry as many times as needed until they find the correct answer. Best suited for open learning courses where the goal is knowledge transfer.
  - *Single attempt (assessment):* the student gets one chance. A wrong answer awards no points and the correct answer is revealed. This mode is used when the course is part of a graded assessment or practical where students earn points towards their evaluation.

ISTP tracks points but does not calculate grades; the instructor uses the earned points as input for their own grading process outside the platform.

The platform is deployed on two environments:

- *Production:* `https://istp.pm4.init-lab.ch`
- *Staging:* `https://istp-staging.pm4.init-lab.ch`

// ─── 2. Keycloak ───────────────────────────────────────────────────────────

= Keycloak

Keycloak handles all authentication and authorization. The realm is called `interactive-security-training-platform` and contains all users, roles, and client configurations.

== Realm Settings

The most important settings in the realm:

- Access token lifespan: *15 min*
- SSO session idle: *1 hour*, max: *10 hours*
- Refresh token reuse: *1* (single-use rotation)
- Brute force protection: enabled (*10 attempts*)
- SSL required: *All* (HTTPS enforced)
- Self-registration: enabled
- Email verification: disabled (to avoid deliverability issues across university mail providers)

== Roles

Three custom roles control what users can do on the platform:

- `ROLE_STUDENT`: solve challenges, view own progress
- `ROLE_INSTRUCTOR`: create/manage labs and courses, view course results
- `ROLE_ADMINISTRATOR`: user/admin functions and platform-wide configuration

*Role policy:* Roles are additive. Every user always keeps `ROLE_STUDENT`. Additional roles (`ROLE_INSTRUCTOR`, `ROLE_ADMINISTRATOR`) are assigned on top and can be revoked individually via the app.

*Self-registration:* New users who self-register automatically receive `ROLE_STUDENT` (configured as a default realm role in Keycloak).

Roles are included in the JWT under `realm_access.roles`.

Note: Keycloak *groups* are not used for authorization in the ISTP backend. Avoid mapping `ROLE_*` via group role-mappings, otherwise users can end up with multiple roles.

== Clients

Two custom clients are configured. All other clients (`account`, `broker`, etc.) are Keycloak built-ins and should not be touched.

*`interactive-security-training-platform-app`* is the active client used by the Next.js frontend (NextAuth). It uses the Authorization Code Flow so the user is redirected to Keycloak to log in, then back to the app. It is a confidential client, meaning it has a client secret stored in a Kubernetes Secret.

Note: A `nextjs` client may exist in the realm, but the deployment configuration in this repository expects `interactive-security-training-platform-app`.

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

+ Go to *Clients* > `interactive-security-training-platform-app` > *Credentials* > *Regenerate*.
+ Repeat for `istp-backend`.
+ Store both secrets in the Kubernetes Secrets (see @sec-env).

== Required Environment Variables <sec-env>

*Next.js:*
```bash
AUTH_KEYCLOAK_ID=interactive-security-training-platform-app
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
KEYCLOAK_APP_CLIENT_ID=interactive-security-training-platform-app
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

- SMTP host: `smtp.gmail.com` (STARTTLS `587`)
- From address: `istp.noreply@gmail.com`
- Authentication: Google *App Password* (not the regular account password)

A dedicated Gmail account was created for this project. The password in Keycloak is a *Google App Password*, not the regular account password -- Google requires this for SMTP access.

*Known issue:* Within the ZHAW network, outbound SMTP on port 587 is blocked. Password reset emails will therefore not be delivered when the platform runs on ZHAW infrastructure. This is a ZHAW network restriction, not a Keycloak misconfiguration.

// ─── 3. Lab Pods ────────────────────────────────────────────────────────────

= Lab Pods

Labs run in short-lived Kubernetes pods that are started from the UI (*Play → Start*). For local development cluster setup (k3d), see the repository README.

*What students get while a lab is running:*
- *App URL* (browser access to the lab application)
- *Terminal URL* (browser-based shell in a separate container)

The terminal container shares the network with the app container, so `curl localhost` works, but it does not share the app’s filesystem. Pods are time-limited (TTL) and cleaned up automatically; restarting a pod does not reset solved challenges or points.

// ─── 4. Permissions Reference ─────────────────────────────────────────────

= Access Model

Every account has `ROLE_STUDENT`. Additional roles are additive on top:
- `ROLE_INSTRUCTOR`: create and manage labs/courses, view course results
- `ROLE_ADMINISTRATOR`: user administration and platform-wide configuration

At the course level, the creator is the *Owner*. *Collaborators* must accept an invitation. Both can edit and manage labs for a course; only the owner can invite/remove collaborators, configure the badge, or delete the course.

#let include_permission_matrices = false
#if include_permission_matrices [

== Role-Based Access

The following table shows what each Keycloak realm role is allowed to do across the platform. ROLE_STUDENT is always present on every user ROLE_INSTRUCTOR and ROLE_ADMINISTRATOR are additive on top.

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

Within a course, instructors have one of two course-level roles. The creator of a course is automatically the *Owner*. Additional instructors can be invited as *Collaborators* they must accept the invitation before gaining access.

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
]

// ─── 5. User Guide: Student ───────────────────────────────────────────────

= User Guide: Student

This section describes the platform from a student's perspective, from creating an account to solving challenges.

== Registration & Login

Open the platform at `https://istp.pm4.init-lab.ch`. The landing page is shown first. Click *Login* in the top-right corner to be redirected to the Keycloak login page.

*New account:* Click *Register* on the Keycloak login page. Fill in your first name, last name, email address, and a password. After submitting you are logged in immediately email verification is disabled on this platform.

*Password requirements:* minimum 8 characters, at least one uppercase letter, one digit, and one special character. The password may not contain your username.

*Failed logins:* After 10 consecutive failed login attempts the account is temporarily locked. Wait a few minutes before trying again.

After successful login you are redirected to the dashboard.

== Profile

After logging in, open the *Profile* page via the user menu to update your personal details.

#figure(
  grid(
    columns: (1fr, 1fr),
    gutter: 12pt,
    image("img/login_page.png", width: 100%),
    image("img/profil.png", width: 100%),
  ),
  caption: [Login (left) and profile editing (right)],
)

Your avatar and email address are shown at the top. The email address is read-only and cannot be changed here. The following fields can be edited:

- *First name* and *Last name:* required, shown throughout the platform
- *Title* optional, e.g. "Dr.", "Student" shown on your profile
- *Profile picture URL:* optional link to an image used as your avatar; if left empty, the platform shows your initials instead

Click *Save changes* to apply. Changes are synced to Keycloak immediately.

== Course Catalog

Navigate to *Browse / Catalog* in the sidebar. The catalog shows all *Public* courses on the platform. Each course card displays the title, topic, short description, author, and creation date.

The search bar filters courses across three fields simultaneously: title, short description, and full description. The *Topic* dropdown on the right lets you narrow results to a specific subject area select *All topics* to reset the filter. Both filters can be combined. Results are paginated use the page controls at the bottom to navigate between pages.

Click on a course card to open the course detail page where you can read the full description and enroll.

== Joining a Private Course

Private courses do not appear in the catalog; they are only accessible via a 6-character invite code. The course owner or a collaborator can share this code with students.

To join a private course, click *Join a Course* in the catalog (or on the My Courses page) and enter the 6-character code in the dialog. Click *Join* to enroll. If the code is valid you are taken directly to the course.

== My Courses

Navigate to *My Courses* in the sidebar to see all courses you are currently enrolled in. Click on a course to open the course detail page.

#figure(
  grid(
    columns: (1fr, 1fr),
    gutter: 12pt,
    image("img/course_enroll.png", width: 100%),
    image("img/progress.png", width: 100%),
  ),
  caption: [Course detail (left) and lab progress (right)],
)

The *Course Journey* section shows your overall progress at a glance: how many labs and challenges you have solved and how many remain. The instructor's name and title are shown in the sidebar on the right.

Click *Continue Course* to jump straight back into the course, or scroll down to the *Course Labs* section to see all labs and your per-lab progress.

Each lab card shows the difficulty, a progress bar, the lab description, and a list of all challenges. Completed challenges are shown with a green checkmark and strikethrough. Click *Continue* on any lab card to jump directly into that lab.

*Leaving a course:* Click *Leave Course* on the course detail page to unenroll. Your completed challenges and earned points are saved if you re-enroll in the same course later, your previous progress is still there.

*Progress in shared labs:* Challenge completions are stored per user and per challenge, not per course. If the same lab appears in multiple courses, solving a challenge in one course automatically counts as solved in all other courses that contain the same lab. This means if you have already worked through a lab in another course, it will show as completed when you encounter it again in a new course.

== Starting a Lab (Pod Launcher)

Inside a course, click *Play* on any lab to open the lab view.

#figure(
  grid(
    columns: (1fr, 1fr),
    gutter: 12pt,
    image("img/lab_overview.png", width: 100%),
    image("img/lab_running.png", width: 100%),
  ),
  caption: [Lab view (left) and running pod panel (right)],
)

The screen is split into two panels:

- *Left panel* lab description, challenge tasks, flag submission, hints
- *Right panel* the live lab environment (pod)

To start the lab environment, click the *Start* button in the right panel. The pod can be in the following states:
- `NOT STARTED`: not started yet
- `PENDING`: being scheduled / starting
- `RUNNING`: ready, the *Open app* button is active
- `TERMINATING`: shutting down
- `FAILED`: start failed; retry

Once the status shows *RUNNING*, the *Open app* button becomes active. Click it to open the lab application in a new browser tab. To stop the pod manually, click the stop button in the right panel.

== Pod Lifetime & Extending Time

Every pod has a time limit (TTL). By default a pod runs for *60 minutes* before it is automatically terminated to free up cluster resources. The expiry time is shown in the right panel while the pod is running.

If you need more time, click the *Extend* button in the right panel. Each extension adds *30 minutes* to the pod's lifetime. You can extend a maximum of *2 times* per pod session, giving a total maximum runtime of *2 hours*.

Once both extensions are used, the badge shows *2 / 2 Extensions Used* and no further extensions are possible for this session.

If the pod terminates before you finish, simply click *Start* again to launch a fresh pod. Your submitted flags and solved challenges are saved you do not lose any progress when a pod restarts.

== Solving Challenges

Each lab contains one or more challenges. Navigate between them using the stepper at the top of the left panel all steps are freely accessible, regardless of order. A progress bar shows how many challenges you have completed.

There are three challenge types:

*Flag challenge:* Exploit the running lab environment to find a hidden flag. The flag has the format `ISTP{...}`. Enter it in the *Submit Flag* field (you can enter either `ISTP{flag}` or just `flag` both are accepted) and press *Submit* or hit Enter. A green notification confirms a correct submission; a red notification means the flag is wrong try again.

*Multiple choice:* Read the question and select one of the options. Click *Submit answer* to confirm. The correct answer is highlighted with a checkmark once answered and the challenge shows the *Solved* badge.

#figure(
  image("img/MC.png", width: 70%),
  caption: [Multiple choice challenge correct answer selected and confirmed],
)

Depending on how the course is configured there are two modes:
- *Unlimited attempts:* wrong answers can be retried as many times as needed until the correct option is found. No points are lost for wrong attempts.
- *Single attempt:* only one submission is allowed. A wrong answer awards no points and the correct answer is revealed immediately afterwards.

*Theory task:* Read the description and click *Mark as done* to complete the task. No flag required.

Each solved challenge shows a *Solved* badge and awards the configured points. When all challenges in a lab are solved, a completion banner is shown.

If a challenge has a hint available, click *Show hint* to reveal it.

== Progress & Points

The left panel shows your current progress for the open lab: solved challenges out of total, and earned points out of maximum points. A teal progress bar fills as you solve challenges. When the lab is fully completed, the bar turns teal and a trophy icon appears.

To view your overall progress across all courses, return to the course page where each lab card shows its completion status.

== Trophy Cabinet (Badges)

ISTP supports *course completion badges*. A badge represents “user X completed course Y” and is shown in the *Trophy Cabinet*.

#figure(
  image("img/trophy_cabinet.png", width: 25%),
  caption: [Trophy Cabinet showing an earned course completion badge],
)

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

This section describes the platform from an instructor's perspective, from creating a lab to publishing a course.

#figure(
  image("img/ins_menubar.png", width: 18%),
  caption: [Instructor sidebar navigation],
)

Instructors see additional navigation items compared to students. Under *Course Management* in the sidebar, three views are available:

- *Dashboard* overview of all courses you own or collaborate on
- *Results* submission statistics and student progress across your courses
- *Labs* manage all labs you have created

== Labs

Labs are the core learning units of the platform. Each lab consists of a Docker image that runs as a Kubernetes pod, one or more challenges, and optional metadata such as difficulty level and a time limit (TTL).

=== Creating a Lab

Navigate to *Course Management → Labs* in the sidebar and click *New lab*. Fill in the form; the lower half of the form with the Docker configuration and challenges is shown below.

#figure(
  image("img/challenges.png", width: 80%),
  caption: [Lab configuration Docker image, status, difficulty, challenges, and test panel],
)

*Key fields:*
- *Title* (and optional description)
- *Docker Image* (`ghcr.io/...`) and *Container Port*
- *Status* (`Draft`, `Private`, `Public`) and *Difficulty*
- *Challenges* (at least one) and optional *Pod TTL*

=== Configuring the Docker Image <sec-docker-image>

The Docker image must be hosted on the GitHub Container Registry (`ghcr.io`). The accepted formats are:

```
ghcr.io/<owner>/<image-name>:<tag>
ghcr.io/<owner>/<image-name>@sha256:<digest>
```

As you type, the platform validates the reference in real time. A green checkmark and the message "Public GHCR image found" confirm the image is reachable. Digest references are preferred over tags for reproducible labs because a tag can be re-pointed to a different image layer at any time.

Images must be public. Private GHCR images are supported only when the cluster administrator has configured an image pull secret.

=== Container Port and Pod TTL

The *container port* must match the port the application listens on inside the container. The platform creates a Kubernetes Ingress rule that routes traffic from the public `app-<id>.*` URL to this port.

The *Pod TTL* (time-to-live) defines the maximum lifetime of a running pod in seconds (minimum 60, maximum 86 400). When the TTL expires the pod is terminated automatically. Leave the field blank to use the cluster-wide default configured by the administrator only set a per-lab override for unusually long-running labs.

=== Challenge Types <sec-challenge-types>

Each lab must have at least one challenge. Click *+ Add Challenge* to add more. The total point value across all challenges is shown in the top-right of the Challenges section. Three types are available, shown as coloured badges in the list:

*Challenge types:*
- `INFO`: reading task; student clicks *Mark as done*
- `MC`: multiple choice; one correct option (attempt policy depends on course)
- `FLAG`: student submits a flag `ISTP{...}`

For each challenge, fill in:

- *Title:* shown in the stepper (max 200 characters)
- *Description:* text explaining the task (max 5000 characters)
- *Points* score awarded on correct completion (minimum 1)
- *Hint:* optional hint shown when the student clicks *Show hint* (max 1000 characters)
- *Flag:* for `FLAG` challenges: the exact flag string without the `ISTP{...}` wrapper
- *Options:* for `MC` challenges: at least two options, one marked as correct
- Reorder challenges using the arrow buttons; delete with the trash icon

=== Testing a Lab Before Publishing

At the bottom of the lab form, the *Test this lab* panel allows you to start a live pod directly from the editor without publishing the lab to students.

Click *Start* to spin up a pod. Once the pod is running you can open the app URL and the terminal URL to verify the lab behaves as expected. The pod status is shown next to the button (`NOT STARTED`, `PROVISIONING`, `RUNNING`). Click *Stop* to terminate the pod when done.

Use this to confirm the Docker image starts correctly, the container port is right, and all flags are findable before changing the lab status to `Public`.

=== Lab Status and Visibility <sec-lab-status>

A lab has one of four statuses that controls who can see and add it to courses:

- `DRAFT`: only the creator can see the lab (work-in-progress)
- `PRIVATE`: only the creator can see it, but it can be used in their own courses
- `PUBLIC`: visible to all instructors; can be used in any course
- `SOFT_DELETED`: removed from listings (kept in DB, not accessible via UI)

*Changing status (visibility impact):* Before changing from `PUBLIC` or `PRIVATE` to `DRAFT` or `SOFT_DELETED`, the UI shows how many course assignments would be removed. Confirm the change to proceed.

*Archiving a lab:* Set the status to `DRAFT` to hide a lab from other instructors without deleting it. Set it to `SOFT_DELETED` to permanently remove it from all course listings. Soft-deleted labs cannot be restored through the UI.

=== Updating and Deleting a Lab

Open *My Labs*, click on the lab, and use the *Edit* button to change any field including challenges. Only the creator can update or delete a lab.

To delete a lab, click *Delete lab* in the lab detail view. This removes the lab from all courses it was assigned to.

=== Example: LLM01 Prompt Injection

As part of this project, the "LLM01 - Prompt Injection" lab was implemented as a concrete example of what the platform supports. The lab presents students with an LLM-powered chat interface called *Secure Vault Guardian* and challenges them to extract a hidden flag through prompt injection techniques a real-world vulnerability class described in the OWASP LLM Top 10.

#figure(
  image("img/ki_lab.png", width: 75%),
  caption: [LLM01 - Prompt Injection lab: student attempts to extract the hidden flag from the AI guardian],
)

The student interacts with the AI via a terminal-style chat interface. The guardian is instructed to protect a hidden flag and refuses direct requests for it students must craft creative prompt injection attacks to bypass these instructions and leak the flag.

The lab demonstrates that labs can integrate external AI services via API. In this case the lab uses #link("https://groq.com")[Groq] (free-tier) to power the chat interface; in our testing the free-tier limits were sufficient for around 20–30 concurrent students.

The backend and database already support injecting external secrets (such as API keys) as environment variables into lab pods. This mechanism was introduced specifically for this lab. A dedicated instructor UI for configuring this per lab is not yet implemented and is out of scope for the current project submission.

== Results

The *Results* view shows a per-course breakdown of student progress. Navigate to *Course Management → Results* in the sidebar and select a course to open its results overview.

#figure(
  image("img/result_ovw.png", width: 85%),
  caption: [Results Overview for a course],
)

Four summary cards are shown at the top:

- *Average completion:* mean completion percentage across students/labs
- *On-time:* students who completed within the deadline
- *In progress:* students who started but not finished
- *Participants:* total enrolled students

Below the summary, the *Participants* table lists every enrolled student with their current status, earned points, solved challenges, and a completion progress bar. Use the *All labs* dropdown to filter by a specific lab, and the *All statuses* dropdown to filter by submission status (e.g. show only students who are still in progress). Use the search bar to find a specific participant by name.

Click on any participant row to open the *Submission Details* panel for that student.

#figure(
  image("img/submission_details.png", width: 60%),
  caption: [Submission details for a single student],
)

The panel shows every challenge in the selected lab together with its type (`FLAG` or `MULTIPLE_CHOICE`), whether the student answered correctly, and the points earned. For flag challenges the exact submitted flag is displayed. Use the lab dropdown at the top of the panel to switch between labs within the same course.

== Creating a Course & Adding Labs

Navigate to *My Courses* in the sidebar and click *New course*. A course groups one or more labs into a coherent learning unit that students can enroll in.

#figure(
  image("img/create_course.png", width: 72%),
  caption: [Create Course form],
)

Fill in the fields as described below. Title, Short Description, and Description are required.

*Key fields:*
- *Course Title*, *Short Description*, *Description*
- *Visibility* (`Draft`, `Public`, `Private`)
- *Multiple-Choice Attempts* (`Unlimited` or `Once`)
- Optional: *Topic*, *Course Image URL*, *Collaborators*

*Visibility* has three states:

- *Draft:* only instructors can view the course; students cannot see or join it.
- *Public:* the course appears in the student catalog and anyone can enroll.
- *Private:* the course is hidden from the catalog; students can only join via invite code.

*Multiple-Choice Attempts* controls the retry behavior for all MC challenges in this course:

- *Unlimited (retry until correct, self-learning):* students can keep trying after a wrong answer.
- *Once:* students get one attempt; the correct answer is revealed after a wrong submission.

Click *Create Course* to save. The course is created immediately and you are taken to the course detail view where you can add labs and configure the badge.

=== Adding Labs to a Course

Open a course you own and go to the *Labs* tab. Click *Add lab* to search the lab catalog. The search returns your own labs (any status) and all `PUBLIC` labs from other instructors. Select a lab to add it to the course. Labs are shown in the order they were added; reorder them using the drag handles.

Only `PUBLIC` or `PRIVATE` labs can actually be played by students. Adding a `DRAFT` lab to a course is allowed, but students will not be able to launch it.

=== Inviting Collaborators

Course owners can invite other instructors as collaborators. Open *Course settings* and go to the *Collaborators* tab. Search for an instructor by name or email, then click *Invite*. The invited instructor receives a notification and must accept the invitation before gaining access to the course.

Collaborators can edit the course, manage labs, and view participant submissions. Only the owner can invite or remove collaborators, configure the badge, or delete the course.

=== Course Badges

Course owners can configure the badge for their course (icon, colors, template) and enable or disable awarding via `badgeEnabled`.

- If `badgeEnabled = true`, students can earn the badge when they complete the course (see Student guide above).
- If `badgeEnabled = false`, ISTP will not award new badges for this course.
- Enabling badges later does not retroactively award badges for users who already completed the course while badges were disabled (no backfill).

// ─── 6. User Guide: Admin ─────────────────────────────────────────────────

= User Guide: Admin

Administrators see an additional *Admin* section at the bottom of the sidebar, giving access to the admin dashboard and user management.

#figure(
  image("img/admin_menubar.png", width: 20%),
  caption: [Admin sidebar additional Admin section below Course Management],
)

== User Management Overview

Navigate to *Admin → Users*. The User Management area has four tabs: *User Management*, *Create User*, *Sessions*, and *Keycloak*.

The *User Management* tab shows a paginated list of all registered users. Use the search bar to filter by name or email. Click on a user row to open the user detail view.

== Creating a User

Open the *Create User* tab and fill in the form. Email, username, first name, and last name are required. Title and Picture URL are optional. Click *Create user* to create the account in Keycloak. The new user receives a temporary password and must set their own password on first login.

Note: users created this way are *admin-managed* accounts. They are separate from self-registered accounts and must be provisioned manually before they can access the platform (see @sec-provision).

== User Detail & Role Management

#figure(
  image("img/user_managment.png", width: 70%),
  caption: [User detail view profile, roles, and password management],
)

Click any user in the list to open the detail view. Status badges at the top show the account state. *Provisioned* (green) means the user has a PostgreSQL shadow row and can fully use the platform.

The detail view has three sections:

*Profile* first name, last name, title, and picture URL can be edited. Email and username are read-only after creation.

*Roles:* toggle `ROLE_STUDENT`, `ROLE_INSTRUCTOR`, and `ROLE_ADMINISTRATOR` individually. Click *Save roles* to apply. `ROLE_STUDENT` is always active and cannot be removed additional roles are additive on top of it. The current active roles are shown below the toggles. Role changes take effect on the user's next login (existing JWTs are not invalidated immediately).

*Password:* use *Send reset email* to trigger a Keycloak password reset email (note: may be blocked on ZHAW network, see @sec-env). Use *Set password (manual)* to set a password directly. With *Temporary* checked the user must change the password on next login.

== Provisioning a User <sec-provision>

A user account has two parts: a Keycloak identity and a PostgreSQL shadow row. Self-registered students get their shadow row created automatically on first login. Admin-created accounts and users who somehow skipped the just-in-time flow need to be provisioned manually.

To provision a user, open the user detail view. If the *Provisioned* badge is missing, a *Provision* button is shown. Click it to create the PostgreSQL shadow row. After provisioning the user can access the platform and their progress is tracked.

== Disabling, Restoring & Soft-Deleting Users

Three lifecycle operations are available from the user detail view:

- *Disable (reversible):* disables Keycloak and sets `deletedAt` in PostgreSQL (blocks login)
- *Restore (reversible):* re-enables Keycloak and clears `deletedAt`
- *Soft-delete (irreversible):* anonymizes email/username in Keycloak + DB, disables the account, sets `deletedAt` (frees up the original email/username)

Use *Disable* to temporarily block access without losing any data. Use *Soft-delete* only when the account must be permanently removed for example when a student leaves the university and their personal data must be erased.

*Session management:* The *Sessions* tab lists all active Keycloak sessions for a user. Click *Logout* to immediately invalidate every active token useful when an account is compromised or needs to be locked out instantly.

// ─── 7. Architecture ──────────────────────────────────────────────────────

= Architecture

== Component Overview

ISTP follows a three-tier architecture: a Next.js frontend, a Spring Boot REST backend, and a PostgreSQL database, with Keycloak as a separate identity provider and Kubernetes as the container runtime for student lab pods.

- *Frontend (Next.js):* UI + NextAuth login; talks to backend via REST (`/api/v1/**`)
- *Backend (Spring Boot):* REST API + business logic + Keycloak Admin API + Kubernetes pod lifecycle
- *Keycloak:* OIDC tokens, realm roles, brute-force protection, password resets
- *PostgreSQL:* persistent app data (users projection, courses/labs/challenges, submissions, badges)
- *Kubernetes:* runs isolated lab pods; enforces TTL and resource limits

All runtime services (frontend, backend, Keycloak, PostgreSQL) are deployed as Kubernetes workloads. The frontend and backend are exposed through HTTPS Ingress and communicate with the backend exclusively via REST. The backend validates JWTs issued by Keycloak and enforces roles on all protected endpoints under `/api/v1/**`.

== Database Schema (PostgreSQL)

*Source of truth:* The schema is defined by the JPA entities in the backend (`backend/src/main/java/.../db/entities`). In development the schema is created/updated automatically by Hibernate (`spring.jpa.hibernate.ddl-auto=update`). For production deployments, a migration tool (Flyway/Liquibase) is recommended.

*Core tables:*
- `users` + `user_roles`: app user projection (linked to Keycloak user id), soft-delete fields, online-time tracking
- `courses`, `course_topics`: course metadata and catalog topics
- `labs`, `challenges`, `challenge_options`: labs (Docker image) and their challenges (incl. MC options)
- `course_labs`: assigns labs to courses + ordering + optional due date
- `course_instructors`, `course_enrollments`: instructors (owner/collaborator) and student enrollments
- `challenge_completions`: “solved” state per user + challenge (unique)
- `student_flag_submissions`, `student_option_submissions`: last submission per user + challenge (unique), incl. correctness
- `user_course_badges`: awarded badges per user + course (unique)
- `course_challenge_score_overrides`: per-course overrides for individual challenge scoring
- `admin_config`: cluster-wide admin settings (pod TTL, kubeconfig, image pull secret)

*Key relationships (high-level):*
- Course → many Labs (via `course_labs`)
- Lab → many Challenges → many Options (MC)
- User → enrollments, completions, submissions, badges

== API Documentation

The backend publishes an OpenAPI specification (springdoc).

*Local development (backend on `http://localhost:8080`):*
- JSON: `http://localhost:8080/v3/api-docs`
- YAML: `http://localhost:8080/v3/api-docs.yaml`

*Staging / production (default ingress setup):* The OpenAPI endpoints are reachable through the Next.js API proxy after login:
- Scalar UI: `/api/backend/scalar`
- OpenAPI JSON: `/api/backend/v3/api-docs`
- OpenAPI YAML: `/api/backend/v3/api-docs.yaml`

The frontend generates TypeScript types from the OpenAPI spec via `npm run generate:api` (see `frontend/package.json`).

== Repository Structure

- `frontend/`: Next.js application
- `backend/`: Spring Boot REST service
- `infra/`: Docker/Kubernetes deployment config
- `docs/`: documentation sources and images
