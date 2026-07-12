import { useState } from "react";
import type { CreateGoalRequest, Dimension, GoalProgress } from "../types";
import { COLORS } from "../theme";

export function GoalTracker({
  goals,
  onCreate,
}: {
  goals: GoalProgress[];
  onCreate: (body: CreateGoalRequest) => void;
}) {
  return (
    <section className="card">
      <h2>Goals</h2>
      {goals.length === 0 && <p className="muted">No goals yet — set one below.</p>}
      <ul className="goal-list">
        {goals.map((g) => (
          <GoalRow key={g.goalId} goal={g} />
        ))}
      </ul>
      <GoalForm onCreate={onCreate} />
    </section>
  );
}

function GoalRow({ goal }: { goal: GoalProgress }) {
  const color = goal.dimension === "LOCAL" ? COLORS.local : COLORS.sustainability;
  return (
    <li className="goal">
      <div className="goal-head">
        <span className="goal-title">
          {goal.dimension === "LOCAL" ? "Local spending" : "Sustainable spending"}
        </span>
        <span className="muted">
          {goal.currentPct.toFixed(1)}% of {goal.targetPct}% target
          {goal.achieved ? " ✓" : ""}
        </span>
      </div>
      <div className="bar">
        <div
          className="bar-fill"
          style={{ width: `${goal.progressPct}%`, background: color }}
        />
      </div>
      <div className="stat-hint">
        from {goal.baselinePct}% baseline · by {goal.targetDate} · {goal.progressPct.toFixed(0)}%
        of the way there
      </div>
    </li>
  );
}

function GoalForm({ onCreate }: { onCreate: (body: CreateGoalRequest) => void }) {
  const [dimension, setDimension] = useState<Dimension>("LOCAL");
  const [baselinePct, setBaseline] = useState(18);
  const [targetPct, setTarget] = useState(30);
  const [targetDate, setTargetDate] = useState("2026-12-31");

  return (
    <form
      className="goal-form"
      onSubmit={(e) => {
        e.preventDefault();
        onCreate({ dimension, baselinePct, targetPct, targetDate });
      }}
    >
      <select value={dimension} onChange={(e) => setDimension(e.target.value as Dimension)}>
        <option value="LOCAL">Local</option>
        <option value="SUSTAINABLE">Sustainable</option>
      </select>
      <label>
        baseline
        <input
          type="number"
          value={baselinePct}
          onChange={(e) => setBaseline(Number(e.target.value))}
        />
      </label>
      <label>
        target
        <input
          type="number"
          value={targetPct}
          onChange={(e) => setTarget(Number(e.target.value))}
        />
      </label>
      <label>
        by
        <input type="date" value={targetDate} onChange={(e) => setTargetDate(e.target.value)} />
      </label>
      <button type="submit">Add goal</button>
    </form>
  );
}
