import { useState } from "react";
import type { BudgetStatus, BudgetStatusKind } from "../types";
import { COLORS, formatUsd } from "../theme";

const STATUS_COLOR: Record<BudgetStatusKind, string> = {
  ON_TRACK: COLORS.budgetOk,
  AT_RISK: COLORS.budgetWarn,
  OVER: COLORS.budgetOver,
  NO_BUDGET: COLORS.muted,
};

const STATUS_LABEL: Record<BudgetStatusKind, string> = {
  ON_TRACK: "On track",
  AT_RISK: "At risk — pace projects over",
  OVER: "Over budget",
  NO_BUDGET: "No budget set",
};

export function BudgetTracker({
  budget,
  onSetBudget,
}: {
  budget: BudgetStatus | null;
  onSetBudget: (limit: number) => void | Promise<void>;
}) {
  if (!budget) {
    return null;
  }

  const color = STATUS_COLOR[budget.status];
  const hasLimit = budget.monthlyLimit != null;
  const barWidth = Math.min(100, Math.max(0, budget.pctUsed));
  const daysLeft = Math.max(0, budget.daysInMonth - budget.daysElapsed);

  return (
    <section className="card">
      <div className="goal-head">
        <h2 style={{ margin: 0 }}>Monthly budget</h2>
        {hasLimit && (
          <span className="budget-status" style={{ color, borderColor: color }}>
            {STATUS_LABEL[budget.status]}
          </span>
        )}
      </div>

      {hasLimit ? (
        <>
          <div className="budget-headline">
            <span className="stat-value" style={{ color }}>
              {formatUsd(budget.spent)}
            </span>
            <span className="muted"> of {formatUsd(budget.monthlyLimit as number)}</span>
          </div>
          <div className="bar">
            <div className="bar-fill" style={{ width: `${barWidth}%`, background: color }} />
          </div>
          <div className="budget-facts">
            <span>{budget.pctUsed.toFixed(0)}% used</span>
            <span>·</span>
            <span>{daysLeft} days left</span>
            <span>·</span>
            <span>
              projected {formatUsd(budget.projectedSpend)} by month-end
            </span>
            {budget.remaining != null && (
              <>
                <span>·</span>
                <span style={{ color }}>
                  {budget.remaining >= 0
                    ? `${formatUsd(budget.remaining)} left`
                    : `${formatUsd(Math.abs(budget.remaining))} over`}
                </span>
              </>
            )}
          </div>
        </>
      ) : (
        <p className="muted">
          Set a monthly spending limit to track how you're pacing this month.
        </p>
      )}

      <BudgetForm
        current={budget.monthlyLimit}
        onSetBudget={onSetBudget}
      />
    </section>
  );
}

function BudgetForm({
  current,
  onSetBudget,
}: {
  current: number | null;
  onSetBudget: (limit: number) => void | Promise<void>;
}) {
  const [value, setValue] = useState(current != null ? String(current) : "3000");
  const [submitting, setSubmitting] = useState(false);

  return (
    <form
      className="goal-form"
      aria-busy={submitting}
      onSubmit={async (e) => {
        e.preventDefault();
        const limit = Number(value);
        if (submitting || !Number.isFinite(limit) || limit <= 0) {
          return;
        }
        setSubmitting(true);
        try {
          await onSetBudget(limit);
        } finally {
          setSubmitting(false);
        }
      }}
    >
      <label>
        monthly limit ($)
        <input
          type="number"
          min="1"
          step="1"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          style={{ width: 100 }}
        />
      </label>
      <button type="submit" disabled={submitting}>
        {submitting ? "Saving…" : current != null ? "Update budget" : "Set budget"}
      </button>
    </form>
  );
}
