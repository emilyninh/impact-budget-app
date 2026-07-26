// The palette lives in CSS custom properties (:root in styles.css) as the single
// source of truth. This module exposes those tokens to the two JS contexts that
// can't read the cascade on their own:
//
//   1. React inline styles — accept a var() reference string, resolved at use time.
//      (color / borderColor / background props on a style object.)
//   2. Recharts stroke/fill — SVG presentation attributes, which do NOT substitute
//      var(). Those need a resolved literal, read from the cascade via cssVar().
//
// Nothing here holds a hex value; changing a color means editing :root, once.

// var() references for inline-style consumers. Keys map the impact/budget vocabulary
// used across components to the CSS tokens.
export const COLORS = {
  local: "var(--color-local)",
  sustainability: "var(--color-sustainable)",
  muted: "var(--muted)",
  budgetOk: "var(--budget-ok)",
  budgetWarn: "var(--budget-warn)",
  budgetOver: "var(--budget-over)",
} as const;

// Resolve a CSS custom property to its literal value, for SVG-attribute contexts
// (Recharts) where var() would not be substituted. Safe to call during render:
// styles.css is imported in main.tsx before the app mounts, so the cascade is live.
export function cssVar(name: string): string {
  if (typeof document === "undefined") return "";
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

export function formatUsd(value: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
  }).format(value);
}
