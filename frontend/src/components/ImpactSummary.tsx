import type { BudgetAggregate } from "../types";
import { COLORS, formatUsd } from "../theme";

export function ImpactSummary({ summary }: { summary: BudgetAggregate }) {
  return (
    <section className="card">
      <h2>
        This month <span className="muted">({summary.yearMonth})</span>
      </h2>
      <div className="stat-row">
        <Stat
          label="Local impact"
          value={`${summary.localImpactPct.toFixed(1)}%`}
          color={COLORS.local}
          hint="confidence-weighted local score"
        />
        <Stat
          label="Sustainability"
          value={`${summary.sustainabilityImpactPct.toFixed(1)}%`}
          color={COLORS.sustainability}
          hint="confidence-weighted sustainability score"
        />
        <Stat
          label="Total spend"
          value={formatUsd(summary.totalSpend)}
          color={COLORS.muted}
          hint={`${summary.transactionCount} transactions`}
        />
        <Stat
          label="At local & independent"
          value={formatUsd(summary.localIndependentSpend)}
          color={COLORS.local}
          hint="dollars to independent businesses"
        />
      </div>
      <p className="table-note muted" style={{ margin: "12px 0 0" }}>
        The two impact figures are weighted by how confident each merchant's score is, based on{" "}
        {summary.scoredSharePct.toFixed(0)}% of spend we could score from grounded data; the rest is
        an unverified estimate.
      </p>
    </section>
  );
}

function Stat({
  label,
  value,
  color,
  hint,
}: {
  label: string;
  value: string;
  color: string;
  hint: string;
}) {
  return (
    <div className="stat">
      <div className="stat-label">{label}</div>
      <div className="stat-value" style={{ color }}>
        {value}
      </div>
      <div className="stat-hint">{hint}</div>
    </div>
  );
}
