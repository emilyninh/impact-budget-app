import type { Swap } from "../types";
import { COLORS } from "../theme";

export function GreenerSwaps({ swaps }: { swaps: Swap[] }) {
  if (swaps.length === 0) {
    return null;
  }
  return (
    <section className="card">
      <h2>Greener swaps</h2>
      <p className="muted swaps-intro">
        Higher-impact alternatives in the same category as some of your recent spending.
      </p>
      <ul className="swap-list">
        {swaps.map((s, i) => (
          <li className="swap" key={i}>
            <div className="swap-from">
              <span className="swap-merchant">{s.fromMerchant}</span>
              <span className="category">{s.category}</span>
              <Score value={s.fromScore} />
            </div>
            <div className="swap-arrow" aria-hidden="true">
              →
            </div>
            <div className="swap-suggestions">
              {s.suggestions.map((alt, j) => (
                <div className="swap-alt" key={j}>
                  <span className="swap-merchant">{alt.merchant}</span>
                  <Score value={alt.sustainabilityScore} good />
                  {alt.flags.slice(0, 2).map((f) => (
                    <span className="flag" key={f}>
                      {f}
                    </span>
                  ))}
                </div>
              ))}
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}

function Score({ value, good }: { value: number; good?: boolean }) {
  // `good` marks the higher-impact alternative (teal); the current merchant is muted.
  // aria-label carries that distinction so it doesn't rest on color alone.
  const color = good ? COLORS.local : COLORS.muted;
  const label = good ? "Alternative sustainability score" : "Current sustainability score";
  return (
    <span
      className="score"
      style={{ color, borderColor: color }}
      aria-label={`${label} ${value}`}
    >
      {value}
    </span>
  );
}
