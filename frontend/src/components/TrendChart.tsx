import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { BudgetAggregate } from "../types";
import { cssVar } from "../theme";

export function TrendChart({ trend }: { trend: BudgetAggregate[] }) {
  const data = trend.map((a) => ({
    month: a.yearMonth,
    Local: a.localImpactPct,
    Sustainability: a.sustainabilityImpactPct,
  }));

  const first = data[0];
  const last = data[data.length - 1];
  const summary = data.length
    ? `Line chart of local and sustainable spending share across ${data.length} month${
        data.length > 1 ? "s" : ""
      }, ${first.month} to ${last.month}. Local goes from ${first.Local.toFixed(
        0,
      )}% to ${last.Local.toFixed(0)}%; sustainability from ${first.Sustainability.toFixed(
        0,
      )}% to ${last.Sustainability.toFixed(0)}%.`
    : "Trend chart with no data yet.";

  return (
    <section className="card">
      <h2>Shifting your spending over time</h2>
      <div style={{ width: "100%", height: 280 }} role="img" aria-label={summary}>
        <ResponsiveContainer>
          <LineChart data={data} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke={cssVar("--border")} />
            <XAxis dataKey="month" tick={{ fontSize: 12 }} />
            <YAxis domain={[0, 100]} unit="%" tick={{ fontSize: 12 }} />
            <Tooltip formatter={(v: number) => `${v.toFixed(1)}%`} />
            <Legend />
            {/* Local is solid, Sustainability dashed: the two series stay
                distinguishable without relying on hue alone (WCAG 1.4.1). */}
            <Line
              type="monotone"
              dataKey="Local"
              stroke={cssVar("--color-local")}
              strokeWidth={2.5}
              dot={{ r: 3 }}
            />
            <Line
              type="monotone"
              dataKey="Sustainability"
              stroke={cssVar("--color-sustainable")}
              strokeWidth={2.5}
              strokeDasharray="6 4"
              dot={{ r: 3 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </section>
  );
}
