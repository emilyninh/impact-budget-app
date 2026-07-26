---
name: Impact Budget
description: A budgeting dashboard that scores spending by impact — local vs. conglomerate, sustainable vs. not.
colors:
  independent-teal: "#007c6c"
  sustained-amber: "#a46200"
  over-budget-red: "#c84341"
  neutral-ink: "#1c2230"
  neutral-slate: "#4a5468"
  neutral-muted: "#666f80"
  neutral-border: "#e6e9ef"
  neutral-track: "#eef1f5"
  neutral-hover: "#f0f2f6"
  neutral-bg: "#f6f7f9"
  neutral-card: "#ffffff"
  teal-wash: "#e5f5f2"
  error-ink: "#a12b2b"
  error-border: "#f5b5b5"
  error-wash: "#fdecec"
  chart-indigo: "#4c6ef5"
  chart-orange: "#e8590c"
  chart-violet: "#9c36b5"
  chart-graphite: "#495057"
typography:
  display:
    fontFamily: "system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "28px"
    fontWeight: 700
    lineHeight: 1.2
  headline:
    fontFamily: "system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "24px"
    fontWeight: 700
    lineHeight: 1.25
  metric:
    fontFamily: "system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "26px"
    fontWeight: 700
    lineHeight: 1.2
  title:
    fontFamily: "system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "18px"
    fontWeight: 600
    lineHeight: 1.3
  body:
    fontFamily: "system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: 1.5
  data:
    fontFamily: "system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.4
  label:
    fontFamily: "system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "13px"
    fontWeight: 400
    lineHeight: 1.3
  hint:
    fontFamily: "system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: 1.3
  tag:
    fontFamily: "system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "11px"
    fontWeight: 400
    lineHeight: 1.3
rounded:
  sm: "6px"
  md: "8px"
  lg: "12px"
  pill: "999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "20px"
  xxl: "24px"
components:
  card:
    backgroundColor: "{colors.neutral-card}"
    textColor: "{colors.neutral-ink}"
    rounded: "{rounded.lg}"
    padding: "20px"
  button-primary:
    backgroundColor: "{colors.independent-teal}"
    textColor: "{colors.neutral-card}"
    typography: "{typography.body}"
    rounded: "{rounded.md}"
    padding: "11px"
  button-secondary:
    backgroundColor: "{colors.neutral-ink}"
    textColor: "{colors.neutral-card}"
    typography: "{typography.data}"
    rounded: "{rounded.sm}"
    padding: "8px 14px"
  button-ghost:
    backgroundColor: "{colors.neutral-card}"
    textColor: "{colors.neutral-ink}"
    rounded: "{rounded.sm}"
    padding: "6px 12px"
  button-ghost-hover:
    backgroundColor: "{colors.neutral-hover}"
  input:
    backgroundColor: "{colors.neutral-card}"
    textColor: "{colors.neutral-ink}"
    rounded: "{rounded.md}"
    padding: "10px 12px"
  chip-category:
    backgroundColor: "{colors.neutral-track}"
    textColor: "{colors.neutral-slate}"
    typography: "{typography.hint}"
    rounded: "{rounded.sm}"
    padding: "2px 8px"
  chip-local:
    backgroundColor: "{colors.teal-wash}"
    textColor: "{colors.independent-teal}"
    typography: "{typography.tag}"
    rounded: "{rounded.pill}"
    padding: "1px 8px"
  score:
    backgroundColor: "{colors.neutral-card}"
    rounded: "{rounded.sm}"
    padding: "1px 6px"
  alert-error:
    backgroundColor: "{colors.error-wash}"
    textColor: "{colors.error-ink}"
    rounded: "{rounded.md}"
    padding: "12px 16px"
---

# Design System: Impact Budget

## 1. Overview

**Creative North Star: "The Calibrated Instrument"**

This system is a well-made measuring device, not a campaign. Every reading is legible, every scale
is marked, and nothing on the surface is decorative. The user came to find out where a month of
their money actually went; trust is earned through visible precision, not through reassurance. The
interface behaves like an instrument that has been calibrated and will tell you what it measured
even when the reading is unflattering.

The organizing tension is **quiet until it matters**. Controls, chrome, and containers recede to a
narrow cool-neutral band — a `#f6f7f9` page, white cards, a single `#e6e9ef` hairline. Color is
reserved almost entirely for figures that make a claim: a local score, a sustainability percentage,
a budget that has gone over. Color in this system is an assertion, so it is spent only where a
number is being asserted. A screen where the chrome is as colorful as the data is a broken
instrument.

Density serves reading. The dashboard is a single scrolling column capped at 960px, stacked as
sections a person reads top to bottom in one sitting: this month's impact, then the budget frame,
then where the money went, then the trend, then what to do about it, then the ledger itself. This
system explicitly rejects the **gamified habit app** — no streaks, badges, confetti, mascots, or
points. This is someone's actual money. An instrument that congratulates you is not an instrument.

**Key Characteristics:**
- Cool-neutral chrome, near-monochrome at rest; two semantic hues carry all meaning
- Flat surfaces separated by one 1px hairline, never by shadow
- One type family (system-ui) across every role; hierarchy from weight and size only
- Fixed px type scale, not fluid — a dashboard is read at a consistent distance
- Tabular numerals wherever figures align in a column
- Single-column reading order at a 960px cap; no sidebar, no app chrome

## 2. Colors

A cool near-monochrome page with two semantic hues, one per impact dimension, plus a red reserved
exclusively for a breached budget.

### Primary
- **Independent Teal** (`#007c6c`): The "local / independent" dimension, everywhere it appears — the
  Local impact stat, the local score in the transaction table, the `local` pill on a merchant, the
  greener-swap flags, the live-updates dot, and the primary auth button. When this color is on
  screen, it means money stayed with an independent business. It is never used as decoration. The
  value is calibrated so white-on-teal clears AA on the primary button (5.1:1) and teal-on-white
  clears it as small text (5.1:1).

### Secondary
- **Sustained Amber** (`#a46200`): The "sustainability" dimension, in exact parallel to teal — the
  Sustainability stat, the sustainability score column, and the at-risk budget state. Paired with
  teal because the two dimensions are peers and must never be confused for one another. A deep ochre
  rather than a light gold: the lighter amber failed AA (2.8:1) as text, so the token is darkened to
  4.5:1 while holding its warm hue.

### Tertiary
- **Over-Budget Red** (`#c84341`): One job only. The spend budget has been exceeded, or is projected
  to be. It appears nowhere else on the surface, which is what makes it mean something when it does.
  Calibrated to 4.5:1 so the over-budget status text clears AA.

### Neutral
- **Ink** (`#1c2230`): Body copy, headings, table figures, and the secondary button fill. The
  darkest value in the system and the default for anything that must simply be read.
- **Slate** (`#4a5468`): Category chip text. A single step lighter than Ink, for a label that sits
  on a tinted chip rather than the page.
- **Muted** (`#666f80`): Stat labels, hints, the tagline, form labels, and the swap arrow. Darkened
  from the original `#8a94a6` (2.85:1, failing) to clear 4.5:1 on the tab trough — its darkest
  background — and therefore on the Page and Card as well.
- **Border** (`#e6e9ef`): The universal 1px hairline. Card edges, table rules, section dividers,
  input strokes, and chart gridlines are all this one value.
- **Track** (`#eef1f5`): Unfilled progress-bar track and the category chip fill. The "empty" tone.
- **Hover** (`#f0f2f6`): Ghost-button hover and the auth tab-group trough.
- **Page** (`#f6f7f9`): The body background. The cool tint separating it from card white is the only
  thing carrying figure-from-ground on this surface.
- **Card** (`#ffffff`): Every content surface.
- **Teal Wash** (`#e5f5f2`): The `local` pill and swap-flag fill. A tint of Independent Teal, not a
  neutral.

### Error
- **Error Ink / Border / Wash** (`#a12b2b` / `#f5b5b5` / `#fdecec`): The API-failure banner only.
  Distinct from Over-Budget Red so that "we couldn't reach the server" never reads as "you
  overspent."

### Chart
Beyond teal and amber, the category bar chart draws from **Chart Indigo** (`#4c6ef5`), **Chart
Orange** (`#e8590c`), **Chart Violet** (`#9c36b5`), and **Chart Graphite** (`#495057`). These are
categorical identifiers with no semantic meaning — a category's color says nothing about its impact.

### Named Rules

**The Assertion Rule.** Color is reserved for figures that make a claim. Teal and amber attach to
scores and percentages; red attaches to a breached budget. Chrome — buttons, borders, containers,
labels, nav — stays in the cool neutral band. If you are reaching for a hue to make a container
look more interesting, stop: the container is not making a claim.

**The Two Dimensions Rule.** Teal is local. Amber is sustainable. This mapping is fixed across every
surface — stat, score, chart, flag, legend. Swapping them, or introducing a third hue for a third
impact dimension without adding that dimension to the data model, corrupts the instrument.

**The Contrast Floor Rule.** Every text and small-graphic pairing in this palette clears WCAG 2.1 AA,
and the accent token values are *calibrated* to that floor — they are as light (as vivid) as AA
allows, and no lighter. Do not lighten them back toward the old values; that reintroduces the debt.

| Pairing | Where | Measured | Needs | Verdict |
| --- | --- | --- | --- | --- |
| Muted `#666f80` on tab trough | Inactive auth tab (its darkest bg) | 4.5:1 | 4.5:1 | Passes |
| Muted `#666f80` on Page / Card | Labels, hints, tagline, form labels | 4.7–5.1:1 | 4.5:1 | Passes |
| Teal `#007c6c` on Teal Wash | The 11px `local` pill | 4.6:1 | 4.5:1 | Passes |
| White on Teal `#007c6c` | The 15px/600 primary auth button | 5.1:1 | 4.5:1 | Passes |
| Teal `#007c6c` on Card | Score cells, Local stat | 5.1:1 | 4.5:1 | Passes |
| Amber `#a46200` on Card | Sustainability stat, score, chart bar | 4.9:1 | 4.5:1 | Passes |
| Red `#c84341` on Card | Over-budget status text | 4.8:1 | 4.5:1 | Passes |
| Ink / Slate / Error Ink | Body, chips, error banner | 6.3–15.9:1 | 4.5:1 | Passes |

The history worth remembering: the original palette (`#1f9d8b` / `#d08b2c` / `#8a94a6`) failed on
four pairings, the amber Sustainability stat worst of all at 2.83:1 — below even the relaxed 3:1
large-text bar. The fix darkened each token toward Ink while holding hue (teal stayed at 180°, amber
moved only 70°→66°), so identity survived. The tonal ramps in `.impeccable/design.json` carry the
same hue/chroma at other lightnesses if a variant is ever needed.

This rule governs **contrast (SC 1.4.3)** only. The separate **use-of-color (SC 1.4.1)** gap was
closed in the `harden` pass, not by these values: the two trend lines are now solid (Local) and
dashed (Sustainability) so they read apart without hue, and the score cells carry an `aria-label`
naming their dimension. Keep both cues when touching those components — color stays a redundant
layer, never the sole one.

## 3. Typography

**Display Font:** none — this system deliberately has no display face.
**Body Font:** system-ui (with `-apple-system`, `Segoe UI`, `Roboto`, `sans-serif`)
**Label/Mono Font:** none distinct; tabular figures come from `font-variant-numeric: tabular-nums`
on the body face.

**Character:** One family doing every job, from the 28px page title to the 11px pill. The system
face is the right call here precisely because it is unremarkable: it renders natively at every DPI,
it carries dense tabular data without ceremony, and it leaves the reader's attention on the figures.
There is no pairing to get wrong.

### Hierarchy
- **Display** (700, 28px, 1.2): The page title, once per screen. `Impact Budget` in the header.
- **Headline** (700, 24px, 1.25): The auth screen title. The only place a headline appears outside
  the dashboard.
- **Metric** (700, 26px, 1.2): Stat values and the budget headline figure. Carries the accent color;
  this is the size that earns the ≥3:1 large-text bar.
- **Title** (600, 18px, 1.3): Card headings — `This month`, `Where your money goes`, `Transactions`.
- **Body** (400, 16px, 1.5): Prose and default text. Cap prose at 65–75ch; the 960px column already
  holds it near that.
- **Data** (400, 14px, 1.4): The transaction table, form controls, and the swap list. The working
  size of the interface.
- **Label** (400, 13px, 1.3): Stat labels and form labels.
- **Hint** (400, 12px, 1.3): Stat hints, chart ticks, and category chips.
- **Tag** (400, 11px, 1.3): The `local` pill and swap flags. The floor — nothing goes smaller.

### Named Rules

**The One Family Rule.** system-ui carries every role. Hierarchy comes from weight (400/600/700) and
size alone. A display face, a second sans, or a decorative mono in this UI would be costume, not
information — and product register has no use for costume.

**The Fixed Scale Rule.** Type sizes are fixed px, never `clamp()`. Users read this dashboard at a
consistent distance on a consistent DPI; a fluid heading that shrinks in a narrow column looks worse,
not better. Responsiveness here is structural — the stat grid rewraps, the table scrolls — and never
typographic.

**The Tabular Rule.** Any figure that sits in a column with other figures takes
`font-variant-numeric: tabular-nums`. Digits that shift width between rows make a table unreadable
and an instrument untrustworthy.

## 4. Elevation

This system is flat. Depth is carried by exactly two devices: a tonal step from the `#f6f7f9` page
to the `#ffffff` card, and a single 1px `#e6e9ef` hairline. There is no shadow vocabulary, no
elevation scale, and no z-layered surface language, because there is nothing on this surface that
floats — every card sits in the page's reading order. One faint shadow exists today on the active
auth tab (`0 1px 2px rgba(0, 0, 0, 0.06)`), where it marks selection state rather than height. That
is the only sanctioned shadow in the system.

Any future floating surface (a month picker, a provenance popover, a menu) will need a real
elevation token and a semantic z-index scale — `dropdown → sticky → modal-backdrop → modal → toast →
tooltip`. Neither exists yet. Introduce them deliberately when the first one lands; never with an
arbitrary `z-index: 999`.

### Shadow Vocabulary
- **Selection** (`box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06)`): The active tab in a segmented
  control. Marks *which one is chosen*, not *how high it sits*.

### Named Rules

**The Flat-By-Default Rule.** Surfaces are flat at rest. Separation comes from the 1px hairline and
the page/card tonal step, never from shadow. A shadow may only appear as a response to state
(selection, or a genuinely floating layer) — never to make a card look "lifted." If a card needs a
shadow to feel distinct, the tonal step is wrong; fix the tone, not the elevation.

## 5. Components

Components are quiet until they matter. Everything below is a standard affordance in a standard
shape, because the user is here to read data, not to admire controls.

### Buttons
- **Shape:** Gently curved (`6px` for compact, `8px` for the full-width primary).
- **Primary:** Independent Teal fill, white text, 600 weight, 11px padding, full width. The auth
  screen's sign-in action. Disabled drops to `opacity: 0.6` with `cursor: default`.
- **Secondary:** Ink fill, white text, 14px, `8px 14px` padding. Form submits inside a card (adding
  a goal, setting a budget), where teal would compete with the data.
- **Ghost:** White fill, Ink text, 1px Border stroke, `6px 12px`. Sign-out. Hovers to Hover
  (`#f0f2f6`).
- **Dashed Ghost:** Transparent fill, Muted text, 1px *dashed* Border, `8px` radius. Exactly one
  instance — "Explore the demo account" — where dashed signals "this is a side door, not the front
  door." Hovers to Ink text with a `#c7ccd6` stroke.
- **Focus:** *Not yet defined.* No component in this system declares `:focus-visible`. This is a gap
  against PRODUCT.md's AA commitment, not a style choice.

### Cards / Containers
- **Corner Style:** `12px` — the largest radius in the system, and the only place it appears.
- **Background:** Card white on the Page ground.
- **Shadow Strategy:** None. See Elevation.
- **Border:** 1px Border hairline.
- **Internal Padding:** `20px`, with `16px` between stacked cards.
- **Heading:** Title (600, 18px) with `16px` of space beneath, often carrying a Muted parenthetical
  for scope (`This month (2026-07)`).

### Inputs / Fields
- **Style:** White fill, 1px Border stroke, `8px` radius, `10px 12px` padding, 15px text. Compact
  variants inside card forms drop to `6px 8px` at `6px` radius and 14px.
- **Label:** Label (13px) in Muted, stacked above the field in a flex column with a `4px` gap.
- **Focus:** *Not yet defined* — falls back to the UA default ring. See the gap noted under Buttons.
- **Error / Disabled:** No field-level error or disabled styling exists; errors surface only at the
  page-level banner.

### Chips
- **Category chip:** Track fill, Slate text, 12px, `6px` radius, `2px 8px`, `white-space: nowrap`. A
  neutral identifier — a category is not a judgment.
- **Local pill:** Teal Wash fill, Independent Teal text, 11px, fully rounded (`999px`), `1px 8px`.
  Asserts a fact about the merchant, so it earns the accent.
- **Budget status:** Fully rounded, 12px, 600 weight, 1px stroke, `2px 10px`. Fill and stroke are
  set inline from the budget state (teal on track / amber at risk / red over).

### Navigation
There is none. The authenticated surface is one scrolling column with a header carrying the title,
the live indicator, the user's name, and sign-out. This is a deliberate consequence of the product
being a single reading surface. **If a month picker or a second view lands, it is the first real
navigation decision this system will make** — and it should be resolved as in-page controls (a
period selector in the header) before it is resolved as a nav bar.

### Score cell (signature)
In the transactions table this is a **gauge**: a `44px` × `5px` 0–100 track (Track fill) whose bar
fills to the score in the dimension's hue, next to the numeral (600 weight, tabular, right-aligned so
the column shares an edge). The fill length is the scale anchor — 88 reads as nearly full, 12 as
nearly empty, with no arithmetic — while the number keeps precision. **The fill never shifts to a
good/bad color**: a low score gets a short bar, not a red one, because the product reports rather than
scolds. The dimension hue is a *redundant* cue; an `aria-label` ("Local score 82 out of 100") carries
the dimension and scale for assistive tech, so meaning never rests on color alone (SC 1.4.1). A `13px`
legend under the table heading names both axes and the scale once. The older bordered-numeral form
(`1px` border in the dimension hue, `6px` radius) survives in Greener Swaps, where two scores sit
side by side and the comparison, not the absolute scale, is the point.

### Progress bar
`10px` tall, `6px` radius, Track fill, with an inline-colored fill whose width is the goal's percent.
The fill transitions `width` over 300ms — the one place this system animates a layout property, and
a known performance smell. Prefer `transform: scaleX()` against a `transform-origin: left` when this
is next touched, and pair it with a `prefers-reduced-motion` alternative, which the system currently
lacks entirely.

## 6. Do's and Don'ts

### Do:
- **Do** spend color only on figures that make a claim — scores, percentages, budget state. Chrome
  stays in the cool neutral band (`#f6f7f9` / `#ffffff` / `#e6e9ef` / `#1c2230`).
- **Do** keep teal (`#1f9d8b`) = local and amber (`#d08b2c`) = sustainable on every surface. The
  mapping is fixed.
- **Do** reserve Over-Budget Red (`#d9534f`) for a breached or projected-breached budget, and
  nothing else.
- **Do** carry every type role on system-ui at a fixed px size, with hierarchy from weight and size.
- **Do** apply `font-variant-numeric: tabular-nums` to any figure in a column.
- **Do** separate surfaces with the 1px `#e6e9ef` hairline and the page/card tonal step.
- **Do** pair every color-encoded meaning with a non-color cue — a label, a value, or a shape. AA
  includes SC 1.4.1, and the score cell and budget status currently lean on hue alone.
- **Do** keep the dashboard one scrolling column at a 960px cap, in the order a person reads it.
- **Do** make responsiveness structural (`repeat(auto-fit, minmax(150px, 1fr))` stat grid, scrolling
  table) rather than typographic.

### Don't:
- **Don't** build anything that reads as a **gamified habit app** — no streaks, badges, confetti,
  mascots, or points. PRODUCT.md's only anti-reference, and it is absolute. This is someone's actual
  money, and turning it into a scoreboard would undercut the instrument the product is trying to be.
- **Don't** congratulate or scold. The product reports; it does not coach. No "great job this
  month," no red shaming for a conglomerate purchase.
- **Don't** lighten the accent tokens back toward their old values (`#1f9d8b` / `#d08b2c` /
  `#8a94a6`). They are calibrated to the AA floor per the Contrast Floor Rule; lightening them fails
  contrast on text and the primary button.
- **Don't** rely on the darkened accents to satisfy **use of color** (SC 1.4.1). Contrast is fixed;
  the local-vs-sustainable distinction on score cells and trend lines still needs a non-color cue.
- **Don't** add a shadow to make a card feel lifted. Surfaces are flat; fix the tonal step instead.
- **Don't** introduce a display face, a second sans, or a decorative mono. One family, every role.
- **Don't** use `clamp()` on type in this UI. Fixed px scale.
- **Don't** animate `width`, `height`, `padding`, or `margin`. The goal bar's `transition: width` is
  a known defect, not a precedent — use `transform: scaleX()`.
- **Don't** ship motion without a `prefers-reduced-motion: reduce` alternative.
- **Don't** reach for a modal. This surface has none, and every current job (setting a budget, adding
  a goal) is solved inline. Exhaust inline and progressive disclosure first.
- **Don't** add an arbitrary `z-index: 999`. Build the semantic scale when the first floating layer
  actually lands.
- **Don't** use `border-left`/`border-right` above 1px as a colored accent stripe on cards, rows, or
  the error banner.
- **Don't** add a tiny uppercase tracked eyebrow above card headings. The Title role is the heading;
  a kicker above every card is scaffolding, not hierarchy.
