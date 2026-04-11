// ============================================================
// Evaluation Rubric – ISTP Promotional Video
// NoTech Deliverable · SW7 Draft · IT.PM4 FS2026
// ============================================================

#set document(
  title: "Evaluation Rubric – ISTP Promotional Video",
  author: "Biedermann, Calabrese, Hoffmann, Kaiser, Schaub, Seeberger",
)

#set page(
  paper: "a4",
  margin: (top: 2.5cm, bottom: 2cm, left: 2cm, right: 2cm),
  footer: context [
    #set text(8pt, fill: luma(120))
    #h(1fr)
    #counter(page).display("1 / 1", both: true)
  ],
)

#set text(font: "New Computer Modern", size: 10pt)
#set par(justify: true)

// ── Title block ──────────────────────────────────────────────
#align(center)[
  #block(width: 100%, inset: (y: 12pt))[
    #text(16pt, weight: "bold")[Evaluation Rubric – Promotional Video] \
    #v(4pt)
    #text(12pt)[Interactive Security Training Platform (ISTP)] \
    #v(6pt)
    #text(10pt, fill: luma(80))[
      NoTech Deliverable · SW7 Draft \
      Module IT.PM4 · Spring Semester 2026 · ZHAW School of Engineering
    ]
  ]
]

#v(8pt)

// ── Metadata ─────────────────────────────────────────────────
#let meta-table = table(
  columns: (auto, 1fr),
  stroke: none,
  inset: (x: 6pt, y: 4pt),
  [*Team:*], [Biedermann Linus, Calabrese Davide, Hoffmann Lorenz, Kaiser Jan, Schaub Alex, Seeberger Alessio],
  [*Product:*], [Promotional video (founder-style talking head)],
  [*Target audience:*], [University instructors and IT administrators],
  [*Scale:*], [1 – 5 points per criterion (1 = inadequate, 5 = excellent)],
  [*Total:*], [100 points (20 criteria × 5 points)],
)

#block(
  width: 100%,
  stroke: 0.5pt + luma(180),
  radius: 3pt,
  inset: 8pt,
  meta-table,
)

#v(12pt)

// ── Helper: category table ───────────────────────────────────
#let category(title, criteria) = {
  // Category header
  block(
    width: 100%,
    fill: rgb("#1a3a5c"),
    radius: (top: 3pt),
    inset: (x: 8pt, y: 6pt),
    text(white, weight: "bold", size: 10.5pt, title),
  )

  // Table
  let header-row = (
    [*\#*], [*Criterion*], [*Evaluation Aspect*], [*Points (1–5)*],
  )
  let rows = header-row
  for (i, c) in criteria.enumerate() {
    rows.push([#{i + 1}])
    rows.push(c.at(0))
    rows.push(c.at(1))
    rows.push([])
  }

  block(
    width: 100%,
    stroke: (
      left: 0.5pt + luma(180),
      right: 0.5pt + luma(180),
      bottom: 0.5pt + luma(180),
    ),
    radius: (bottom: 3pt),
    table(
      columns: (28pt, 1.4fr, 2.6fr, 60pt),
      align: (center, left, left, center),
      stroke: 0.4pt + luma(200),
      inset: (x: 6pt, y: 5pt),
      fill: (_, row) => if row == 0 { luma(235) } else { none },
      ..rows,
    ),
  )

  // Subtotal
  align(right)[
    #text(9pt, fill: luma(80))[
      Subtotal: #box(width: 20pt, stroke: (bottom: 0.5pt), []) / #(criteria.len() * 5)
    ]
  ]

  v(10pt)
}

// ── 1. Content & Structure ───────────────────────────────────
#category("1 · Content & Structure",(
  (
    [Problem presentation],
    [The gap between theoretical and practical security education is clearly explained and made tangible for the viewer.],
  ),
  (
    [Solution presentation],
    [ISTP's core concept (self-hosted, Kubernetes-based CTF platform) is introduced concisely and comprehensibly.],
  ),
  (
    [Value proposition],
    [The benefits for universities (free, on-premises, privacy-compliant, academic workflow integration) are convincingly communicated.],
  ),
  (
    [Narrative arc],
    [The video follows a logical structure (problem → solution → value) with a clear opening hook and a memorable closing.],
  ),
))

// ── 2. Visual Design / Camera ────────────────────────────────
#category("2 · Visual Design & Camera",(
  (
    [Camera perspective & framing],
    [Appropriate shot sizes are chosen (e.g.\ medium close-up for talking head); framing is intentional and consistent.],
  ),
  (
    [Lighting & image quality],
    [Sufficient lighting without harsh shadows on the face; no visible noise or focus issues.],
  ),
  (
    [Visual variety],
    [Effective use of B-roll / cut aways (e.g.\ screen recordings, UI demos, campus shots) to support the narration.],
  ),
))

// ── 3. Audio ─────────────────────────────────────────────────
#category("3 · Audio",(
  (
    [Voice clarity & recording quality],
    [Speech is clearly audible, well-articulated, and free of distortion; external microphone use is evident.],
  ),
  (
    [Background noise & room tone],
    [No distracting ambient noise; silent passages are covered by music or appropriate sound design.],
  ),
  (
    [Music & sound design],
    [Background music (if used) supports the mood without overpowering the voice; volume levels are balanced.],
  ),
))

// ── 4. Language & Expression ─────────────────────────────────
#category("4 · Language & Expression",(
  (
    [Word choice & register],
    [Vocabulary is precise, audience-appropriate (instructors / IT admins), and free of unnecessary jargon.],
  ),
  (
    [Grammar & fluency],
    [Sentences are grammatically correct; delivery is fluent and natural without excessive filler words.],
  ),
  (
    [Persuasiveness & engagement],
    [The speaker conveys credibility and enthusiasm; the tone is direct and inviting (founder-style).],
  ),
))

// ── 5. Editing & Montage ─────────────────────────────────────
#category("5 · Editing & Montage",(
  (
    [Pacing & rhythm],
    [The video maintains an appropriate tempo; no scenes feel rushed or unnecessarily drawn out.],
  ),
  (
    [Transitions & cut quality],
    [Cuts are clean and frame-accurate.],
  ),
  (
    [Title & text overlays],
    [Titles are legible and use an appropriate font and style.],
  ),
  (
    [Credits],
    [The video has a clear credit roll/card at the end listing all contributors and sources.],
  ),
))

// ── 6. Overall Impression ────────────────────────────────────
#category("6 · Overall Impression",(
  (
    [Coherence & consistency],
    [All elements (visuals, audio, language, editing) work together as a unified whole; the video feels cohesive rather than assembled from disconnected parts.],
  ),
  (
    [Target audience fit],
    [The video would convince an instructor or IT admin to take a concrete next step; it goes beyond informing and actually motivates action.],
  ),
  (
    [Memorability & call to action],
    [The viewer retains the key message after watching; the video includes a clear next step (e.g.\ link, QR code, contact).],
  ),
))

// ── Grand total ──────────────────────────────────────────────
#v(4pt)
#line(length: 100%, stroke: 0.8pt + luma(120))
#v(4pt)

#grid(
  columns: (1fr, auto),
  align: (left, right),
  [
    #text(10pt, weight: "bold")[Total Score]
  ],
  [
    #text(12pt, weight: "bold")[
      #box(width: 30pt, stroke: (bottom: 0.8pt), []) / 100
    ]
  ],
)

#v(16pt)

// ── Comments section ─────────────────────────────────────────
#block(
  width: 100%,
  stroke: 0.5pt + luma(180),
  radius: 3pt,
  inset: 10pt,
)[
  #text(weight: "bold")[Comments] \
  #v(60pt)
]

#v(12pt)

#grid(
  columns: (1fr, 1fr),
  column-gutter: 40pt,
  [
    *Evaluator:* \
    #v(20pt)
    #line(length: 100%, stroke: 0.4pt + luma(150))
  ],
  [
    *Date:* \
    #v(20pt)
    #line(length: 100%, stroke: 0.4pt + luma(150))
  ],
)
