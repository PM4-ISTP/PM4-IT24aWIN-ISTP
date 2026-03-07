#import "@preview/tablex:0.0.8": tablex, rowspanx, colspanx

// ── Colours ──────────────────────────────────────────────────────────────────
#let zhawblue = rgb(0, 100, 175)
#let lightgray = rgb(245, 245, 245)
#let darkgray = rgb(80, 80, 80)

// ── TODO helper ──────────────────────────────────────────────────────────────
#let todo(msg) = text(fill: red, weight: "bold")[[TODO: #msg]]

// ── Page setup ───────────────────────────────────────────────────────────────
#set page(
  paper: "a4",
  margin: (top: 2.5cm, bottom: 2.5cm, left: 3cm, right: 3cm),
  header: [
    #set text(size: 10pt, font: "New Computer Modern")
    #grid(
      columns: (1fr, 1fr),
      [ISTP – Project Outline],
      align(right)[Software-Project 4 – IT.PM4, FS2026]
    )
    #line(length: 100%, stroke: 0.4pt)
  ],
  footer: [
    #line(length: 100%, stroke: 0.4pt)
    #align(center)[#context counter(page).display()]
  ],
  header-ascent: 30%,
)

#set text(font: "New Computer Modern", size: 12pt, lang: "en")
#set par(justify: true, leading: 0.8em)
#set heading(numbering: "1.1")
#show link: set text(fill: zhawblue)

// ── Heading styles ────────────────────────────────────────────────────────────
#show heading.where(level: 1): it => {
  v(1em)
  text(fill: zhawblue, weight: "bold", size: 13pt)[#it]
  line(length: 100%, stroke: 0.5pt + zhawblue)
  v(0.5em)
}

#show heading.where(level: 2): it => {
  v(0.6em)
  text(fill: darkgray, weight: "bold", size: 12pt)[#it]
  v(0.3em)
}

// ══════════════════════════════════════════════════════════════════════════════
// TITLE PAGE
// ══════════════════════════════════════════════════════════════════════════════
#page(header: none, footer: none)[
  #align(center)[
    #v(2cm)
    #text(size: 28pt, weight: "bold", fill: zhawblue)[Interactive Security Training Platform]
    #v(0.4cm)
    #text(size: 16pt)[Project Outline – Software-Projekt 4]
    #v(0.2cm)
    #text(size: 13pt)[IT.PM4, Spring Semester 2026]
    #v(1.5cm)
    #line(length: 100%, stroke: 0.5pt)
    #v(0.5cm)
    #text(size: 13pt, style: "italic")[
      From Reading to Exploiting \ Hands-on Security Training for Universities
    ]
    #v(0.5cm)
    #line(length: 100%, stroke: 0.5pt)
    #v(2cm)

    #table(
      columns: (auto, auto),
      stroke: none,
      inset: 6pt,
      [*Submission Date:*], [15 March 2026],
      [*Module:*],          [Software-Projekt 4 (IT.PM4)],
      [*Institution:*],     [ZHAW – School of Engineering],
      [*Team:*],            [Biedermann Linus, Calabrese Davide, Hoffmann Lorenz,
                             Kaiser Jan, Schaub Alex, Seeberger Alessio],
    )

    #v(1fr)
    #text(size: 10pt)[
      Zurich University of Applied Sciences \
      School of Engineering – Institute of Applied Information Technology (InIT)
    ]
  ]
]

// ── Table of Contents ─────────────────────────────────────────────────────────
#outline(title: "Table of Contents", indent: 1em)
#pagebreak()

// ══════════════════════════════════════════════════════════════════════════════
= Initial Situation & Idea

== Problem Statement

Application security is a central topic in modern software development. Yet traditional courses
often teach security concepts purely in theory: students read about vulnerabilities like SQL
injection or cross-site scripting without ever actively exploiting them in a safe environment @owasp.
This gap between theory and practice means graduates can name vulnerability classes but struggle
to recognise or fix them in real code.

Platforms like _TryHackMe_ or _HackTheBox_ exist, but run on external cloud infrastructure and
give universities no control over content, data privacy, or grade integration.

== Solution Idea: ISTP

The *Interactive Security Training Platform* (abbreviated as ISTP) is a self-hosted, Kubernetes-based
Capture-the-Flag learning platform that universities can operate on-premises. Students receive
isolated, ephemeral container environments in which they actively exploit real vulnerabilities instead
of just reading about them. Instructors prepare challenges in advance so students can apply what they
learned in class directly in practice.

== Stakeholder Analysis

#figure(
  table(
    columns: (auto, 1fr, 1fr),
    inset: 8pt,
    fill: (_, row) => if row == 0 { zhawblue } else if calc.odd(row) { lightgray } else { white },
    stroke: 0.5pt + gray,
    text(fill: white, weight: "bold")[Stakeholder],
    text(fill: white, weight: "bold")[Need],
    text(fill: white, weight: "bold")[Benefit from ISTP],

    [Students],
    [Hands-on exercises in application security],
    [Active exploit training in a safe sandbox],

    [Instructors / TAs],
    [Simple management of exercises],
    [Admin panel for challenge creation and lifecycle management],

    [IT Administration],
    [Privacy-compliant, maintainable infrastructure],
    [On-premises operation on the university's own Kubernetes cluster],
  ),
  caption: [Stakeholders and their benefits]
)

// ══════════════════════════════════════════════════════════════════════════════
= State of the Art / Competitive Analysis

Several platforms exist for hacking exercises @tryhackme, @hackthebox, @pentesterlab. @tab-konkurrenz summarises
the most relevant alternatives.

#figure(
  table(
    columns: (1fr, 1fr, 1fr, 1fr),
    inset: 8pt,
    fill: (_, row) => if row == 0 { zhawblue } else if calc.odd(row) { lightgray } else { white },
    stroke: 0.5pt + gray,
    text(fill: white, weight: "bold")[Platform],
    text(fill: white, weight: "bold")[Hosting],
    text(fill: white, weight: "bold")[Focus],
    text(fill: white, weight: "bold")[Suitable for Academia?],

    [TryHackMe @tryhackme],
    [Cloud (external)],
    [Broad spectrum],
    [Limited (paid tiers @tryhackme-pricing)],

    [HackTheBox @hackthebox],
    [Cloud (external)],
    [Advanced CTFs],
    [No (no LMS integration)],

    [PentesterLab @pentesterlab],
    [Cloud (external)],
    [Web vulnerabilities],
    [Partially (many exercises are locked behind a paywall @pentesterlab-exercises)],

    text(weight: "bold")[ISTP],
    text(weight: "bold")[On-Premises],
    text(weight: "bold")[Application security (customisable)],
    text(weight: "bold")[Yes, built for academia],
  ),
  caption: [Competitive comparison],
) <tab-konkurrenz>

ISTP is the only solution that runs fully on-premises, offers instructor-controlled
application security challenges, and integrates with academic workflows including role management and
challenge lifecycle.

The fact that ISTP can run fully on-premises means that neither instructors nor students need to
pay for using ISTP. This makes ISTP from a financial point of view, in addition to the already
stated advantages, more suited for educational usage. Currently the ZHAW uses TryHackMe in the
module "IT Security". But as stated in @tab-konkurrenz, TryHackMe has paid tiers. When visiting
"IT Security" we have noticed that we can only use TryHackMe for one hour per day. Once that
hour has passed, many features get locked behind a paywall.

// ══════════════════════════════════════════════════════════════════════════════
= Context Scenario (Main Flow)

The following scenario describes the typical use case from both student and instructor perspective.

#v(0.5em)
*Scenario: «Solving a first CTF challenge»*

A student enrolled in an application security module registers on the ISTP platform using
her university email address. After logging in she sees a dashboard listing all published
challenges, sorted by difficulty and category (e.g. SQL injection, XSS, IDOR).

She selects a beginner challenge and launches a dedicated pod with one click. Within seconds the
system provisions an isolated Kubernetes pod hosting the vulnerable application. The student
receives a link and opens the application directly in her browser.

She analyses the application, identifies the vulnerability and exploits it to extract a hidden flag
(e.g. `FLAG{sql_injection_mastered}`). She submits the flag via the UI. The system validates the
input, updates her score and marks the challenge as solved.

After 60 minutes of inactivity a keep-alive prompt appears. If the student does not confirm it, the
pod terminates automatically to free cluster resources.

#v(0.5em)
*Instructor perspective:* An instructor creates a new challenge by entering a container image, a
task description and the expected flags in the admin panel. After configuring ports and environment
variables she publishes the challenge, which immediately becomes visible and launchable for all
students.

// ══════════════════════════════════════════════════════════════════════════════
= Further Requirements

== Planned Extensions after PM4

- *SSO integration:* Connection to the university LDAP/SAML infrastructure for single sign-on.
- *Extended scoring:* Time bonuses and hint penalties for competitive use.
- *Leaderboard:* Public ranking per course or semester.
- *Challenge marketplace:* Sharing challenges between universities.
- *Multi-cluster support:* Scaling across multiple Kubernetes clusters.

== Security and Maintenance

Since students actively exploit vulnerabilities, namespace isolation, resource limits per pod and
network policies are mandatory. Regular updates of all components are planned as a fixed part
of ongoing operations.

// ══════════════════════════════════════════════════════════════════════════════
= Resources and Timeline

== Team Competencies

#figure(
  table(
    columns: (1fr, 1fr, 1.5fr),
    inset: 8pt,
    fill: (_, row) => if row == 0 { zhawblue } else if calc.odd(row) { lightgray } else { white },
    stroke: 0.5pt + gray,
    text(fill: white, weight: "bold")[Person],
    text(fill: white, weight: "bold")[Focus],
    text(fill: white, weight: "bold")[Main Responsibilities],

    [Biedermann Linus],   [#todo("add focus")], [#todo("add responsibilities")],
    [Calabrese Davide],   [#todo("add focus")], [#todo("add responsibilities")],
    [Hoffmann Lorenz],    [#todo("add focus")], [#todo("add responsibilities")],
    [Kaiser Jan],         [#todo("add focus")], [#todo("add responsibilities")],
    [Schaub Alex],        [#todo("add focus")], [#todo("add responsibilities")],
    [Seeberger Alessio],  [#todo("add focus")], [#todo("add responsibilities")],
  ),
  caption: [Team competencies and responsibilities]
)

== Technology Stack

#figure(
  table(
    columns: (auto, auto, 1fr),
    inset: 8pt,
    fill: (_, row) => if row == 0 { zhawblue } else if calc.odd(row) { lightgray } else { white },
    stroke: 0.5pt + gray,
    text(fill: white, weight: "bold")[Layer],
    text(fill: white, weight: "bold")[Technology],
    text(fill: white, weight: "bold")[Justification],

    [Frontend],       [Next.js 16+ / #todo("maybe not Shadcn")],  [#todo("add justification")],
    [Backend],        [Spring Boot 4.x],          [Robust REST framework with Keycloak integration],
    [Authentication], [Keycloak 26+],             [Role and token management out of the box],
    [Orchestration],  [Kubernetes],               [#todo("add justification")],
    [Database],       [PostgreSQL],               [Reliable relational DB with full JPA support],
    [CI/CD],          [GitHub Actions],           [Automated lint, test and build checks],
  ),
  caption: [Technology stack]
)

== Timeline / Roadmap

#figure(
  table(
    columns: (auto, auto, 1fr),
    inset: 8pt,
    fill: (_, row) => if row == 0 { zhawblue } else if calc.odd(row) { lightgray } else { white },
    stroke: 0.5pt + gray,
    text(fill: white, weight: "bold")[Phase],
    text(fill: white, weight: "bold")[Period (CW)],
    text(fill: white, weight: "bold")[Milestone],

    [Sprint 1], [CW 10-12], [#todo("add milestone")],
    [Sprint 2], [CW 12-14], [#todo("add milestone")],
    [Sprint 3], [CW 14-16], [#todo("add milestone")],
    [Sprint 4], [CW 16-18], [#todo("add milestone")],
    [Sprint 5], [CW 18-20], [#todo("add milestone")],
    [*Project deadline*], [*idk yet*], [*Project submission*]
,
  ),
  caption: [Roadmap with milestones]
)

Each sprint closes with a demo and retrospective. Unfinished work is carried over to the next sprint.

// ══════════════════════════════════════════════════════════════════════════════
= Risks

#todo("Fill in risks based on Vision Doc — see R-1 to R-7")

// ══════════════════════════════════════════════════════════════════════════════
= Economic Viability

ISTP is not a commissioned project from an industry partner. It grew out of a concrete
experience during our own studies: we worked with external platforms where instructors had to
manually check each student's laptop to verify progress. Integrated grading was not possible.

Since the code is hosted on GitHub anyway, ISTP will be released as an open-source
solution. Universities save on licensing costs, keep full control over content and data privacy,
and can tailor challenges to their own learning objectives. Grading runs automatically through
the platform, significantly reducing the workload for instructors.

A single instance is sufficient for multiple courses and semesters. In the medium term, adoption
at other Swiss universities is realistic since the problem is not unique to ZHAW.

// ══════════════════════════════════════════════════════════════════════════════
= Outlook

#todo("Describe what will be delivered at the end of PM4 — working prototype, documentation, etc.")

// ══════════════════════════════════════════════════════════════════════════════
#pagebreak()
#bibliography("refs.bib", style: "ieee", title: "References")