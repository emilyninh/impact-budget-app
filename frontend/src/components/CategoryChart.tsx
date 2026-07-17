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
import { formatUsd } from "../theme";

// Distinct, accessible hues per category (brand-neutral).
const PALETTE = ["#1f9d8b", "#d08b2c", "#4c6ef5", "#e8590c", "#9c36b5", "#495057"];

export function CategoryChart({ categories }: { categories: CategoryBreakdown[] }) {
  if (categories.length === 0) {
    return null;
  }
  const data = categories.map((c) => ({
    category: c.category,
    spend: c.totalSpend,
    sustainability: c.avgSustainability,
    txns: c.txnCount,
  }));

  return (
    <section className="card">
      <h2>Where your money goes</h2>
      <div style={{ width: "100%", height: 280 }}>
        <ResponsiveContainer>
          <BarChart data={data} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e6e9ef" vertical={false} />
            <XAxis dataKey="category" tick={{ fontSize: 12 }} interval={0} />
            <YAxis tickFormatter={(v: number) => `$${v}`} tick={{ fontSize: 12 }} />
            <Tooltip
              formatter={(v: number, name: string) =>
                name === "spend" ? [formatUsd(v), "Spend"] : [v, name]
              }
              labelFormatter={(label: string) => label}
            />
            <Bar dataKey="spend" radius={[6, 6, 0, 0]}>
              {data.map((_, i) => (
                <Cell key={i} fill={PALETTE[i % PALETTE.length]} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </section>
  );
}
