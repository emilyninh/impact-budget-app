# CLAUDE.md

## Design Context

Before changing anything under `frontend/`, read **[PRODUCT.md](PRODUCT.md)** (strategy: who this is
for and why) and **[DESIGN.md](DESIGN.md)** (the visual system: tokens, type, components, guardrails).
DESIGN.md wins on visual decisions; PRODUCT.md wins on strategic and voice decisions.

**Register:** product (app UI — design serves the task). **Platform:** web.

**Positioning:** spending organized by impact, not by type. Categories are scaffolding; the local and
sustainable split is what the dashboard leads with.

The five design principles from PRODUCT.md:

1. **Comprehension is the feature.** The scoring pipeline is elaborate; the reading of it must not be.
2. **Impact leads, category grounds.** When the two compete for hierarchy, impact wins.
3. **Show your work.** A score without provenance is a black box, and a black box asks for faith.
4. **Frame by the constraint.** The budget and the period give every figure below them meaning.
5. **Report, don't coach.** State what the data says and stop.

**The one anti-reference:** never build anything that reads as a gamified habit app — no streaks,
badges, confetti, mascots, or points. This is someone's actual money.

**Accessibility status** (PRODUCT.md commits to WCAG 2.1 AA). Fixed: contrast now clears AA on every
text/small-graphic pairing (the accent tokens were darkened — see the Contrast Floor Rule in
DESIGN.md; don't lighten them back), and use-of-color is covered (trend lines solid vs dashed, score
cells carry `aria-label`s, errors use `role="alert"`, charts use `role="img"`). Still open: no
component defines `:focus-visible` (UA default rings are intact, so it's operable but not styled), and
no motion has a `prefers-reduced-motion` alternative — both are for the `optimize`/`polish` passes.
