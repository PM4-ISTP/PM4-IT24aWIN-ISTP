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

== Solution Idea: Interactive Security Training Platform

The *Interactive Security Training Platform* (abbreviated as ISTP) is a self-hosted, Kubernetes-based
Capture-the-Flag learning platform that universities can operate on-premises. Students receive
isolated, ephemeral container environments in which they actively exploit real vulnerabilities instead
of just reading about them. Instructors prepare challenges in advance so students can apply what they
learned in class directly in practice.

== Stakeholder Analysis

@tab-stakeholder-analyse discusses how exactly ISTP is going to benefit the stakeholders of this project.

The column "Need / Problem" discusses what needs the specific stakeholder has in regard to ISTP and what
problems they face when using currently established tools on the market. Those tools meet their needs
only partially. This leads to their needs and problems overlapping. Due to this overlap, the needs and
problems of the stakeholders have been placed into the same column.

The column "Benefits from ISTP" describes which benefits the stakeholders receive by using ISTP and how
their problems get solved.

#figure(
  table(
    columns: (auto, 1fr, 1fr),
    inset: 8pt,
    align: left,
    fill: (_, row) => if row == 0 { zhawblue } else if calc.odd(row) { lightgray } else { white },
    stroke: 0.5pt + gray,
    text(fill: white, weight: "bold")[Stakeholder],
    text(fill: white, weight: "bold")[Need / Problem],
    text(fill: white, weight: "bold")[Benefits from ISTP],

    [Students],
    [They need hands-on exercises in application security. The exercises need to match the lectures they attended. They do not want to spend money on the exercises.],
    [They receive active exploit training in a safe sandbox. The exercises are created by the instructors or TAs. Therefor they are specifically designed for the lectures. Because ISTP is hosted by the university, these exercises cost the students no money.],

    [Instructors / TAs],
    [They require a simple management of the exercises. They do not want to spend money on this task.],
    [Using an instructor panel, they can create challenges and manage their lifecycle. Because ISTP is hosted by the university, the management of the exercises cost the instructors and TAs no money.],

    [IT Administration],
    [They need privacy-compliant and maintainable infrastructure.],
    [They receive an application that they can deploy on-premises on the university's own Kubernetes cluster. Now they have full control over the privacy policy and maintenance of the training platform.],
  ),
  caption: [Stakeholders and their benefits]
) <tab-stakeholder-analyse>

As seen in @tab-stakeholder-analyse, ISTP meets the needs of the stakeholders and solves their problems
with established tools.

// ══════════════════════════════════════════════════════════════════════════════
= State of the Art / Competitive Analysis

Several platforms exist for hacking exercises @tryhackme, @hackthebox, @pentesterlab. @tab-konkurrenz summarises
the most relevant alternatives.

#show table: set par(justify: false)
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
    [Limited (paid tiers, no custom challenges @tryhackme-pricing)],

    [HackTheBox @hackthebox],
    [Cloud (external)],
    [Advanced CTFs],
    [No (advanced level only, no custom challenges, paid features)],

    [PentesterLab @pentesterlab],
    [Cloud (external)],
    [Web vulnerabilities],
    [Partially (many exercises locked behind paywall @pentesterlab-exercises)],

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

The following scenario describes the typical use case from both the student and instructor perspectives.
The perspective of the IT administrator is not important for the typical use cases of this
application. Administrators have additional permissions and can, for example, ban a user.

#v(0.5em)
*Scenario: «Solving a first CTF challenge»*

A student at the university registers on the ISTP platform using
her university email address. After logging, in she is able to see all published
challenges (e.g., SQL injection, XSS, IDOR).

She selects a challenge and launches a dedicated pod with one click. The
system provisions an isolated Kubernetes pod hosting the vulnerable application. The application
then gets automatically opened directly in her browser.

She reads the task description and analyses the application, identifies the vulnerability and
exploits it to extract a hidden flag (e.g., `FLAG{sql_injection_mastered}`). She submits the
flag via the UI. The system validates the input, updates her score and marks the challenge as
solved.

After 60 minutes of inactivity, a keep-alive prompt appears. If the student does not confirm it, the
pod terminates automatically to free cluster resources.

#v(0.5em)
*Instructor perspective:* An instructor creates a new challenge by entering a container image, a
task description and the expected flags in the admin panel. After making some configurations, she
publishes the challenge, which immediately becomes visible and launchable for all students.

// ══════════════════════════════════════════════════════════════════════════════
= Further Requirements

== Planned Extensions after PM4

- *SSO integration:* Connection to the university LDAP/SAML infrastructure for single sign-on.
- *Assigments:* Instructors can assign exercises to students and set a deadline.
- *Grade integration:* The application can automatically grade students based on their assigned exercises. Instructors can export the grades of their students.
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
    columns: (1fr, 1.5fr, 1fr),
    inset: 8pt,
    fill: (_, row) => if row == 0 { zhawblue } else if calc.odd(row) { lightgray } else { white },
    stroke: 0.5pt + gray,
    text(fill: white, weight: "bold")[Person],
    text(fill: white, weight: "bold")[Skills],
    text(fill: white, weight: "bold")[Focus],

    [Biedermann Linus],   [Documentation, Infrastructure, Java, React], [Documentation, Fullstack Dev],
    [Calabrese Davide],   [Java, REST, React, Database], [Fullstack Dev],
    [Hoffmann Lorenz],    [Java, React, SpringBoot, Keycloak], [Security,\
    Fullstack Dev],
    [Kaiser Jan],         [Java, React, Cypress, Database], [Scrum Master, Fullstack Dev, QA],
    [Schaub Alex],        [Java, React, SpringBoot, Database], [Fullstack Dev, Documentation],
    [Seeberger Alessio],  [Infrastructure, Kubernetes, React, CI/CD], [Product Owner, Fullstack Dev, DevOps],
  ),
  caption: [Team competencies and responsibilities]
)

== Technology Stack

@tab-technologie-stack lists the technologies that will be used for each layer and why they have
been chosen.

#figure(
  table(
    columns: (auto, auto, 1fr),
    inset: 8pt,
    fill: (_, row) => if row == 0 { zhawblue } else if calc.odd(row) { lightgray } else { white },
    stroke: 0.5pt + gray,
    text(fill: white, weight: "bold")[Layer],
    text(fill: white, weight: "bold")[Technology],
    text(fill: white, weight: "bold")[Justification],

    [Frontend],       [Next.js 16+],  [Server-side rendering framework],
    [Backend],        [Spring Boot 4.0.3],          [Robust REST framework with Keycloak integration],
    [Authentication], [Keycloak 26+],             [Role and token management out of the box],
    [Orchestration],  [Kubernetes],               [Can run isolated pods for each exercise and enforces resource quotas],
    [Database],       [PostgreSQL],               [Reliable relational DB with full JPA support],
    [CI/CD],          [GitHub Actions],           [Automated lint, test and build checks],
  ),
  caption: [Technology stack]
) <tab-technologie-stack>

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

    [Sprint 1], [CW 10-12], [Authentication],
    [Sprint 2], [CW 12-14], [Course management],
    [Sprint 3], [CW 14-16], [Challenge creation],
    [Sprint 4], [CW 16-18], [Playable Challenges],
    [Sprint 5], [CW 18-20], [Statistics],
    [*Project deadline*], [*24.05.2026*], [*Project submission*]
,
  ),
  caption: [Roadmap with milestones]
)

Each sprint closes with a demo and retrospective. Unfinished work is carried over to the next sprint.

// ══════════════════════════════════════════════════════════════════════════════
= Risks

The following seven risks in @tab-risiken have been identified. The risks are not ordered.

#figure(
  table(
    columns: (1fr, 1fr, 1.5fr),
    inset: 8pt,
    fill: (_, row) => if row == 0 { zhawblue } else if calc.odd(row) { lightgray } else { white },
    stroke: 0.5pt + gray,
    text(fill: white, weight: "bold")[Risk],
    text(fill: white, weight: "bold")[Category],
    text(fill: white, weight: "bold")[Description],

    [Authentication integration], [Technical], [Keycloak needs to be set up for single sign-on and JSON web tokens need to be propagated between the frontend and backend. This might be complicated.],
    [Connecting to pods], [Technical / Security], [To connect the students to their pods, a secure reverse-proxy or WebSocket tunnel is needed. This is complicated and might also be a security risk.],
    [Security of ISTP], [Security], [Students may try to escape their container. This is a security risk.],
    [Resource exhaustion], [Infrastructure], [Due to the fact that many pods need to run in parallel, the resources of the server may get exhausted.],
    [Kubernetes manifest validation], [Security / Stability], [Instructors or TAs might upload malicious or malformed container images for the exercises.],
    [Knowledge gaps], [Team], [Members of the team might lack experience with parts of the technology stack.],
    [Role management complexity], [Product], [Managing multiple roles such as students, instructors, and admins with different permissions can become complex and may lead to misconfigurations or unintended access rights.]
,
  ),
  caption: [Roadmap with milestones]
) <tab-risiken>

// ══════════════════════════════════════════════════════════════════════════════
= Economic Viability

ISTP is not a commissioned project from an industry partner. It grew out of a concrete
experience during our own studies: we worked with external platforms where instructors had to
manually check each student's laptop to verify progress. Integrated grading was not possible.

Since the code is hosted on GitHub, ISTP will be released as an open-source
solution. Universities save on licensing costs, keep full control over content and data privacy
and can tailor challenges to their own learning objectives. Integrated grading is planned as a
future extension.

A single instance is sufficient for multiple courses and semesters. In the medium term, adoption
at other Swiss universities is realistic since the problem is not unique to ZHAW.

// ══════════════════════════════════════════════════════════════════════════════
= Outlook



// ══════════════════════════════════════════════════════════════════════════════
#pagebreak()
#bibliography("refs.bib", style: "ieee", title: "References")