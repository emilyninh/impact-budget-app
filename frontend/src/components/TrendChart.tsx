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
import { COLORS } from "../theme";

export function TrendChart({ trend }: { trend: BudgetAggregate[] }) {
  const data = trend.map((a) => ({
    month: a.yearMonth,
    Local: a.localImpactPct,
    Sustainability: a.sustainabilityImpactPct,
  }));

  return (
    <section className="card">
      <h2>Shifting your spending over time</h2>
      <div style={{ width: "100%", height: 280 }}>
        <ResponsiveContainer>
          <LineChart data={data} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e6e9ef" />
            <XAxis dataKey="month" tick={{ fontSize: 12 }} />
            <YAxis domain={[0, 100]} unit="%" tick={{ fontSize: 12 }} />
            <Tooltip formatter={(v: number) => `${v.toFixed(1)}%`} />
            <Legend />
            <Line
              type="monotone"
              dataKey="Local"
              stroke={COLORS.local}
              strokeWidth={2.5}
              dot={{ r: 3 }}
            />
            <Line
              type="monotone"
              dataKey="Sustainability"
              stroke={COLORS.sustainability}
              strokeWidth={2.5}
              dot={{ r: 3 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </section>
  );
}
