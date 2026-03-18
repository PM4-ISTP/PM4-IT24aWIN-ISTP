# Product Vision — Interactive Security Training Platform

> **Version:** 1.0
> **Date:** 2026-03-17
> **Status:** Done

---

## 1. Project Overview

The **Interactive Security Training Platform** (abbreviated as ISTP) is a self-hosted, Kubernetes-powered Capture-the-Flag (CTF) learning platform designed for university students studying application security. It provides isolated, ephemeral container environments where students actively exploit real-world vulnerabilities — rather than passively consuming theory.

Unlike commercial platforms such as TryHackMe or HackTheBox, ISTP is deployed **on-premises at educational institutions**, focuses exclusively on **application security (OWASP Top 10 and beyond)**, and is tailored to academic workflows with instructor-driven challenge management.

---

## 2. Target Audience

The following table describes the three user groups of ISTP:

| User Group | Description |
|---|---|
| **Student** | University student in an application security course; exploits vulnerabilities, submits flags, tracks progress |
| **Instructor** | Lecturer or teaching assistant; creates and publishes challenges via admin panel |
| **Platform Admin** | Technical staff |

A person can only register themselves as a user on ISTP if the domain of the email address they use to register themselves with matches the predefined domain of the university. This solution is both effective and simple to implement, due to ISTP being hosted on-premises by the universities themselves. Other registration barriers, like an invitation code, were discussed by the group. However, they were discarded in favor of the previously mentioned solution for the reasons stated above.

---

## 3. Main Features

### 3.1 Student-Facing
- **Registration & Login** — Authentication with student / instructor role assignment (Clarification: all users start out as students. Instructor role is assigned by admin (see [section 3.3](#33-admin-facing)).)
- **Challenge Browser** — List of published challenges with title, description, difficulty, and category 
- **Pod Launcher** — One-click spin-up of a dedicated, namespaced Kubernetes pod per challenge per student
- **Keep-Alive Mechanism** — Prompt shown ~every 60 minutes of inactivity; pod auto-terminates if not acknowledged
- **Flag Submission** — Discovered flags are submitted online and validated against stored solution; points are awarded accordingly.
- **Progress Dashboard** — View solved challenges and current score

## 3.2 Instructor-Facing

- **Challenge Creation** — Submit a container image (registry + image name), an  exercise description, and the expected flag(s) to be discovered
    
- **Challenge Configuration** — Define how the challenge container runs: exposed ports, environment variables, and any additional runtime parameters
    
- **Challenge Lifecycle Management** — Publish and archive challenges; only published challenges are visible to students
    

## 3.3 Admin-Facing

- **User & Role Management** — Promote students to instructor role, demote or delete accounts.
    
---

## 4. Objectives (Success Criteria)

| # | Objective | Acceptance Criteria |
|---|---|---|
| O-1 | User authentication & role management working | Students and instructors can register with their university email address (validation of the email address domain), log in, and access role-appropriate views |
| O-2 | Challenge delivery pipeline functional | Instructor can publish a challenge; student can launch it as an isolated pod and connect to it |
| O-3 | Minimum viable challenge set | 3–5 challenges are playable end-to-end |
| O-4 | Challenge Browser | Students can see all the challenges in the challenge browser |
| O-5 | Pod isolation & cleanup | Each pod runs a single challenge; auto-terminates after inactivity timeout |
| O-6 | Flag submission & scoring | Students submit flags via UI; correct submissions update score |
| O-7 | Instructor workflow complete | Instructors can create, publish, and manage challenges |
| O-8 | On-premises deployment | Platform is fully deployable on a university-operated single Kubernetes cluster with documented setup steps |

---

## 5. Key Risks

| ID | Risk | Category | Mitigation Strategy |
|---|---|---|---|
| R-1 | **Student → Pod connectivity** — Connecting students to running pods (SSH / browser terminal) requires a secure reverse-proxy or WebSocket tunnel | Technical / Security | Investigate `kubectl exec` via backend proxy or a lightweight terminal-in-browser solution (e.g., ttyd, Wetty) |
| R-2 | **Security of the platform itself** — Students are actively exploiting vulnerabilities; lateral movement or container escape is possible | Security | Namespace isolation, resource quotas, NetworkPolicies, read-only root filesystems where possible; accept residual risk for on-prem academic use |
| R-3 | **Resource exhaustion** — Many concurrent pods may overwhelm cluster resources | Infrastructure | Define resource requests/limits per pod manifest; set max-pods-per-student quota |
| R-4 | **Kubernetes Manifest Validation** — Malicious or malformed manifests submitted by instructors | Security / Stability | Validate manifests against a whitelist of allowed fields/resources; reject privileged containers |
| R-5 | **Knowledge gaps** — Team unfamiliar with parts of the stack (Keycloak, Springboot) | Team | Accept as a learning objective; allocate spike tasks per unknown area in early sprints |
| R-6 | **Role Management complexity** — Fine-grained permissions may be under-specified | Product | Use Keycloak realm roles (student / instructor / admin) as the single source of truth; keep permission model simple |

---

## 6. Base Architecture


### Component Responsibilities

| Component | Role |
|---|---|
| **Next.js + Mantine** | Student & instructor UI; communicates with Spring Boot REST API |
| **Spring Boot** | Business logic, K8s pod lifecycle management (via `fabric8`), flag validation, scoring |
| **Keycloak** | OIDC/OAuth2 provider; issues JWTs; manages roles (student, instructor, admin) |
| **Kubernetes** | Runs isolated challenge pods per student in dedicated namespace; enforces resource quotas |
| **PostgreSQL** | Persistent storage for users, challenges, submissions, scores (Spring Data JPA / Code-First via Hibernate) |

---

## 7. Technology Evaluation

| Layer | Technology | Justification |
|---|---|---|
| **Frontend** | Next.js 16+ (App Router) | SSR/SSG flexibility, strong ecosystem, team familiarity |
| **UI Components** | Mantine | Accessible, lightweight components; fast to customize |
| **Backend** | Spring Boot 4.0.3 | Robust REST framework; excellent Keycloak integration |
| **Auth** | Keycloak 26+ | Industry-standard OIDC; handles SSO, roles, and token management out of the box |
| **K8s Client** | fabric8 Kubernetes Client | Programmatic pod lifecycle management from Spring Boot |
| **Database** | PostgreSQL | Reliable relational DB; full JPA/Hibernate support |
| **ORM** | Spring Data JPA (Hibernate) — Code First | Entities define schema; simplifies development iteration |
| **Infrastructure** | Kubernetes (single cluster) | Namespace-level isolation; native resource management |
| **CI/CD** | GitHub Actions | Automated lint, test, and build checks before merge |
| **Formatter (FE)** | Prettier | Enforced via pre-commit hook and CI |
| **Formatter (BE)** | Checkstyle (Feisthammel PM2) | Consistent Java style; integrated in Gradle build |

---

## 8. Coding Standards

### 8.1 Git Workflow
- **One branch per issue/user story** — branch name format: `feature/short-description`, `bugfix/short-description`, `docs/short-description`, `refactor/short-description`
- **One developer per branch** — no shared feature branches
- **Commit messages follow conventional commits**
- **No direct pushes to `main`** — all changes go through PRs

### 8.2 Pull Requests
- **Minimum 2 approving reviews** before merge
- PR must include:
    - Reference to the related issue (`Closes #<id>`)
    - Brief description of what changed and why
    - Notes on how to test the change manually (if applicable)
- **CI/CD pipeline must pass** (lint, format check, tests) before merge is allowed

### 8.3 Frontend (Next.js)
- **Formatter:** Prettier — enforced via `.prettierrc` and `eslint-config-prettier`; run automatically on save and in CI
- **Linter:** ESLint with Next.js recommended rules
- **Component structure:** One component per file; file name matches component name (PascalCase)
- **No `any` types** in TypeScript — use explicit types or `unknown` with narrowing
- **API calls** centralized in a `/lib/api/` directory; no raw `fetch` inside components

### 8.4 Backend (Spring Boot)
- **Style guide:** Feisthammel Coding Style PM2 (as defined by course); enforced via Checkstyle Gradle plugin — build fails on violations
- **No business logic in controllers** — controllers delegate to services only

### 8.5 Testing
- **Backend:** Unit tests for all service-layer methods (JUnit 5 + Mockito)
- **Frontend:** Component tests for critical UI flows (e.g., flag submission, pod launch) using Vitest + React Testing Library
- **Integration tests** are encouraged but not mandatory for MVP
- **CI blocks merge** if any test fails

### 8.6 CI/CD Pipeline (runs before merge)
1. Lint & format check (Prettier / ESLint / Checkstyle)
2. Unit test suite
3. Build (Next.js build + Gradle package)

### 8.7 General
- **No secrets in code or repository** — use environment variables; `.env.example` documents required keys
- **All environment-specific config** (DB URLs, Keycloak realm, K8s namespace prefix) goes into `application.yml` profiles or `.env` files, never hardcoded

---

## 9. Open Questions 

These are critical points that need clarification before or during Sprint 1:

1. **Authentication flow with the university** — Will students use an existing university SSO (e.g., LDAP/SAML), or will Keycloak manage its own user database? This impacts registration flow significantly.
    - We have decided not to use an existing university SSO. For the registration, we will only allow email addresses whose domain matches that of the university.
2. **Pod connectivity method** — How exactly do students interact with their running pod? Via HTTP (challenge exposes a web app), SSH, or a browser-based terminal? This is the core UX decision and affects backend architecture. *(Flagged: clarify with Alessio)*
