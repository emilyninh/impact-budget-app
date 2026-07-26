# Product

## Register

product

## Platform

web

## Users

The values-driven budgeter: someone who already knows roughly what they spend and wants to know
where it actually went. They open the app in a considered moment, not in a checkout line, and the
job they're doing is reading — reconciling a month of spending against what they say they care
about. They are not being sold sustainability; they arrived already caring, and they want a
measurement they can trust rather than encouragement.

The dashboard is a single surface, so the primary task on it is comprehension: understand this
month's local and sustainable split, against the budget, without asking anyone to explain it.

## Product Purpose

Impact Budget scores discretionary spending on two axes — how much flowed to local independent
businesses versus conglomerates, and how sustainable the purchases were — then lets the user set
goals in those terms ("shift 30% of my discretionary spending to local by Q4") and tracks progress
over months. Success is comprehension speed: someone lands on the dashboard cold and grasps the
local/sustainable split in seconds, with no explanation needed. If the split needs a paragraph to
explain, the interface has failed regardless of how good the scoring is.

Three things currently stand between the dashboard and that bar, in the user's own reading:
the month is never anchored (nothing says you're looking at July, and there's no way to move between
months); budget-versus-actual is buried in a peer card instead of framing the spending it constrains;
and scores arrive as bare numbers with no sign of where they came from or how confident they are.

## Positioning

Spending organized by impact, not by type. Every other budgeter answers "how much on Groceries";
this one answers "who did that money fund, and what did it cost." Categories are scaffolding — a
familiar handle people need to find their way around a month — but the local and sustainable split
is what the dashboard leads with, and the category table earns its place by making that split
legible, never by replacing it.

## Brand Personality

Precise, instrumented, transparent. This is a measuring instrument, not a campaign. It states what
it found and shows its work: provenance, confidence, and source are part of the reading, not
footnotes. The voice is plain and specific — numbers and their origins, no adjectives doing work
the data should do. It never congratulates and never lectures; a user who spent their whole month at
conglomerates should feel informed, not handled.

## Anti-references

Not a gamified habit app. No streaks, badges, confetti, mascots, or points. This is someone's actual
money, and turning it into a scoreboard would undercut the instrument the rest of the product is
trying to be.

## Design Principles

**Comprehension is the feature.** The scoring pipeline is elaborate; the reading of it must not be.
Every design decision is measured against how fast a cold viewer understands the split.

**Impact leads, category grounds.** When the two compete for hierarchy, impact wins. Categories exist
to orient people inside a month, not to become the answer.

**Show your work.** A score without provenance is a black box, and a black box asks for faith. Where
a number came from and how sure it is are part of the number.

**Frame by the constraint.** A month of spending means little in isolation. The budget and the period
are the frame that gives every figure below them meaning, not cards competing beside them.

**Report, don't coach.** State what the data says and stop. The user brought their own motivation;
the product's job is to be worth trusting, not to be encouraging.

## Accessibility & Inclusion

WCAG 2.1 AA. Body text at 4.5:1 or better, visible focus on every interactive element, full keyboard
navigation, and `prefers-reduced-motion` honored throughout. AA includes SC 1.4.1 (Use of Color),
which has real teeth here: the local/sustainable scores and the over/under-budget states currently
lean on color to carry meaning, so each needs a non-color cue — value, label, or shape — alongside it.
