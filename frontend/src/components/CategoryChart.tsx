import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { CategoryBreakdown } from "../types";
import { cssVar, formatUsd } from "../theme";

// Ordered categorical palette, resolved from the CSS tokens in :root. The first two
// mirror the impact dimensions; the rest are distinct identifiers with no meaning.
const PALETTE_VARS = [
  "--color-local",
  "--color-sustainable",
  "--chart-3",
  "--chart-4",
  "--chart-5",
  "--chart-6",
];

export function CategoryChart({
  categories,
  selectedCategory = null,
  onSelect,
}: {
  categories: CategoryBreakdown[];
  selectedCategory?: string | null;
  onSelect?: (category: string | null) => void;
}) {
  if (categories.length === 0) {
    return null;
  }
  const palette = PALETTE_VARS.map(cssVar);
  const data = categories.map((c) => ({
    category: c.category,
    spend: c.totalSpend,
    sustainability: c.avgSustainability,
    txns: c.txnCount,
  }));

  const summary = `Bar chart of spending by category. ${data
    .map((d) => `${d.category}, ${formatUsd(d.spend)}`)
    .join("; ")}.`;

  const selected = categories.find((c) => c.category === selectedCategory) ?? null;
  const toggle = (category: string) =>
    onSelect?.(category === selectedCategory ? null : category);

  return (
    <section className="card">
      <div className="goal-head">
        <h2 style={{ margin: 0 }}>Where your money goes</h2>
        {selected && (
          <span className="muted" aria-live="polite">
            {formatUsd(selected.totalSpend)} in {selected.category} · {selected.txnCount}{" "}
            {selected.txnCount === 1 ? "transaction" : "transactions"}
          </span>
        )}
      </div>

      {/* Selectable category chips — the accessible control; clicking a bar does the same. */}
      <div className="filter-chips" role="group" aria-label="Filter by category">
        {data.map((d) => (
          <button
            key={d.category}
            type="button"
            className={`filter-chip${d.category === selectedCategory ? " is-selected" : ""}`}
            aria-pressed={d.category === selectedCategory}
            onClick={() => toggle(d.category)}
          >
            {d.category}
          </button>
        ))}
      </div>

      <div style={{ width: "100%", height: 280 }} role="img" aria-label={summary}>
        <ResponsiveContainer>
          <BarChart data={data} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke={cssVar("--border")} vertical={false} />
            <XAxis dataKey="category" tick={{ fontSize: 12 }} interval={0} />
            <YAxis tickFormatter={(v: number) => `$${v}`} tick={{ fontSize: 12 }} />
            <Tooltip
              formatter={(v: number, name: string) =>
                name === "spend" ? [formatUsd(v), "Spend"] : [v, name]
              }
              labelFormatter={(label: string) => label}
            />
            <Bar dataKey="spend" radius={[6, 6, 0, 0]} onClick={(d: { category: string }) => toggle(d.category)}>
              {data.map((d, i) => (
                <Cell
                  key={i}
                  fill={palette[i % palette.length]}
                  // Dim the unselected bars so the chosen category stands out; no selection = all lit.
                  fillOpacity={selectedCategory && d.category !== selectedCategory ? 0.25 : 1}
                  cursor="pointer"
                />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </section>
  );
}
