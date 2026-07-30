import type { ScoredTransactionView } from "../types";
import { COLORS, formatUsd } from "../theme";

export function TransactionList({
  transactions,
  filterCategory = null,
  onClearFilter,
}: {
  transactions: ScoredTransactionView[];
  filterCategory?: string | null;
  onClearFilter?: () => void;
}) {
  const rows = filterCategory
    ? transactions.filter((t) => t.category === filterCategory)
    : transactions;

  return (
    <section className="card">
      <div className="goal-head">
        <h2 style={{ margin: 0 }}>Transactions</h2>
        {filterCategory && (
          <button type="button" className="filter-clear" onClick={onClearFilter}>
            Showing: {filterCategory}
            <span aria-hidden="true"> ✕</span>
            <span className="sr-only"> — clear category filter</span>
          </button>
        )}
      </div>
      {transactions.length === 0 && (
        <p className="muted">No scored transactions yet — link an account to get started.</p>
      )}
      {transactions.length > 0 && rows.length === 0 && (
        <p className="muted">No {filterCategory} transactions this month.</p>
      )}
      {rows.length > 0 && (
        <p className="table-note muted">
          Every merchant is scored 0–100 on two axes — <strong>Local</strong>, how much of your
          money stayed with an independent business, and <strong>Sustainability</strong>, how
          sustainable the purchase is. The bar fills toward 100; higher is more. The{" "}
          <span className="pill">local</span> tag marks merchants confirmed as independent.
          Transfers between your own accounts are shown but not scored or counted as spending.
        </p>
      )}
      {rows.length > 0 && (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th scope="col">Merchant</th>
                <th scope="col">Account</th>
                <th scope="col">Category</th>
                <th scope="col">Date</th>
                <th scope="col" className="num">Amount</th>
                <th scope="col" className="num">Local</th>
                <th scope="col" className="num">Sustainability</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((t, i) => (
                <tr key={i} className={t.excludedFromSpend ? "txn-transfer" : undefined}>
                  <td className="merchant">
                    {t.merchantName ?? "Unknown"}
                    {t.localIndependent && <span className="pill">local</span>}
                  </td>
                  <td>
                    {t.institutionName ? (
                      t.institutionName
                    ) : (
                      <span className="muted" aria-label="unknown account">
                        —
                      </span>
                    )}
                  </td>
                  <td>
                    {t.category ? (
                      <span className="category">{t.category}</span>
                    ) : (
                      <span className="muted" aria-label="uncategorized">
                        —
                      </span>
                    )}
                  </td>
                  <td>{t.txnDate}</td>
                  <td className="num">{formatUsd(t.amount)}</td>
                  {t.excludedFromSpend ? (
                    <>
                      <td className="num">
                        <span className="muted" aria-label="not scored — account transfer">—</span>
                      </td>
                      <td className="num">
                        <span className="muted" aria-label="not scored — account transfer">—</span>
                      </td>
                    </>
                  ) : (
                    <>
                      <td className="num">
                        <ScoreMeter value={t.localScore} dim={COLORS.local} label="Local" />
                      </td>
                      <td className="num">
                        <ScoreMeter
                          value={t.sustainabilityScore}
                          dim={COLORS.sustainability}
                          label="Sustainability"
                        />
                      </td>
                    </>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

// The score cell is a small gauge: a 0–100 track fills to the value (the scale
// anchor, so 82 reads as "high" without doing arithmetic), the number stays for
// precision, and the dimension hue (teal = local, amber = sustainable) is a
// redundant cue. Fill length carries the value — never a good/bad color shift, so
// a low score reads as low, not as a failure. aria-label names the dimension and
// the scale so the meaning never rests on color.
function ScoreMeter({ value, dim, label }: { value: number; dim: string; label: string }) {
  const pct = Math.max(0, Math.min(100, value));
  return (
    <span className="score-meter" aria-label={`${label} score ${value} out of 100`}>
      <span className="score-meter-track">
        <span className="score-meter-fill" style={{ width: `${pct}%`, background: dim }} />
      </span>
      <span className="score-meter-num" style={{ color: dim }}>
        {value}
      </span>
    </span>
  );
}
