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
    [Access Token Lifespan], [5 min], [Short-lived for security; frontend refreshes automatically],
    [SSO Session Idle], [30 min], [User gets logged out after 30 min of inactivity],
    [SSO Session Max], [10 h], [Absolute session limit regardless of activity],
    [Refresh Token Reuse], [0 (single-use)], [Prevents token replay attacks],
    [Brute Force Protection], [Enabled, 10 attempts], [Locks account temporarily after 10 failed logins],
    [SSL Required], [External], [HTTPS enforced for all non-localhost traffic],
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

New users who self-register receive no role by default. An admin must manually assign `ROLE_STUDENT` before they can access content. Roles are included in the JWT under `realm_access.roles`.

== Clients

Two custom clients are configured. All other clients (`account`, `broker`, etc.) are Keycloak built-ins and should not be touched.

*`nextjs`* is used by the Next.js frontend. It uses the Authorization Code Flow (via NextAuth.js) so the user is redirected to Keycloak to log in, then back to the app. It is a confidential client, meaning it has a client secret stored in a Kubernetes Secret.

*`interactive-security-training-platform-app`* is used by the Spring Boot backend. It has a service account enabled so the backend can authenticate itself against Keycloak (Client Credentials Flow) to call the Admin API if needed.

Both clients share the same allowed redirect URIs and CORS origins:

```
http://localhost:3000/*                   (local dev)
https://istp.pm4.init-lab.ch/*           (production)
https://istp-staging.pm4.init-lab.ch/*   (staging)
```

== Setting Up Keycloak from Scratch

The entire realm config is stored in `realm-export.json` in the repository. To restore it:

+ Open the Keycloak Admin Console at `https://<host>/admin`.
+ Click *Create realm* and upload `realm-export.json`.
+ Click *Create*.

After importing, regenerate the client secrets (they are not stored in the export):

+ Go to *Clients* > `nextjs` > *Credentials* > *Regenerate*.
+ Repeat for `interactive-security-training-platform-app`.
+ Store both secrets in the Kubernetes Secrets (see @sec-env).

== Required Environment Variables <sec-env>

*Next.js:*
```bash
KEYCLOAK_CLIENT_ID=nextjs
KEYCLOAK_CLIENT_SECRET=<secret>
KEYCLOAK_ISSUER=https://<keycloak-host>/realms/interactive-security-training-platform
NEXTAUTH_URL=https://istp.pm4.init-lab.ch
NEXTAUTH_SECRET=<random-string>
```

*Spring Boot:*
```bash
KEYCLOAK_CLIENT_ID=interactive-security-training-platform-app
KEYCLOAK_CLIENT_SECRET=<secret>
KEYCLOAK_ISSUER_URI=https://<keycloak-host>/realms/interactive-security-training-platform
```

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

// ─── 4. Setup Checklist ────────────────────────────────────────────────────

= Setup Checklist

#let check = box(width: 10pt, height: 10pt, stroke: 0.7pt)

#table(
  columns: (auto, 1fr),
  stroke: none,
  inset: (x: 4pt, y: 5pt),
  [#check], [Keycloak 26.x running and reachable],
  [#check], [Realm imported from `realm-export.json`],
  [#check], [Client secrets regenerated for both clients],
  [#check], [Secrets stored in Kubernetes Secrets],
  [#check], [Environment variables set for Next.js and Spring Boot],
  [#check], [Redirect URIs updated if using a new domain],
  [#check], [At least one `ROLE_ADMINISTRATOR` user assigned],
  [#check], [HTTPS certificate in place],
)
