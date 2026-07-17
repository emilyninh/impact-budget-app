import type { ScoredTransactionView } from "../types";
import { COLORS, formatUsd } from "../theme";

export function TransactionList({ transactions }: { transactions: ScoredTransactionView[] }) {
  return (
    <section className="card">
      <h2>Transactions</h2>
      {transactions.length === 0 && (
        <p className="muted">No scored transactions yet — link an account to get started.</p>
      )}
      {transactions.length > 0 && (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Merchant</th>
                <th>Category</th>
                <th>Date</th>
                <th className="num">Amount</th>
                <th className="num">Local</th>
                <th className="num">Sustainability</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((t, i) => (
                <tr key={i}>
                  <td>
                    {t.merchantName ?? "Unknown"}
                    {t.localIndependent && <span className="pill">local</span>}
                  </td>
                  <td>
                    {t.category ? (
                      <span className="category">{t.category}</span>
                    ) : (
                      <span className="muted">—</span>
                    )}
                  </td>
                  <td>{t.txnDate}</td>
                  <td className="num">{formatUsd(t.amount)}</td>
                  <td className="num">
                    <Score value={t.localScore} color={COLORS.local} />
                  </td>
                  <td className="num">
                    <Score value={t.sustainabilityScore} color={COLORS.sustainability} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function Score({ value, color }: { value: number; color: string }) {
  return (
    <span className="score" style={{ color, borderColor: color }}>
      {value}
    </span>
  );
}
