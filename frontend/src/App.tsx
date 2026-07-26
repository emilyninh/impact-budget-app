import { useCallback, useEffect, useState } from "react";
import {
  createGoal,
  fetchBudget,
  fetchCategories,
  fetchGoals,
  fetchSummary,
  fetchSwaps,
  fetchTransactions,
  fetchTrend,
  setBudget,
} from "./api";
import type {
  BudgetAggregate,
  BudgetStatus,
  CategoryBreakdown,
  CreateGoalRequest,
  GoalProgress,
  ScoredTransactionView,
  Swap,
} from "./types";
import { getToken } from "./session";
import { ImpactSummary } from "./components/ImpactSummary";
import { BudgetTracker } from "./components/BudgetTracker";
import { CategoryChart } from "./components/CategoryChart";
import { TrendChart } from "./components/TrendChart";
import { GoalTracker } from "./components/GoalTracker";
import { GreenerSwaps } from "./components/GreenerSwaps";
import { TransactionList } from "./components/TransactionList";
import { LoginPage } from "./components/LoginPage";
import { useAuth } from "./AuthContext";

export default function App() {
  const { user } = useAuth();
  if (!user) {
    return <LoginPage />;
  }
  return <Dashboard />;
}

function Dashboard() {
  const { user, logout } = useAuth();
  const [summary, setSummary] = useState<BudgetAggregate | null>(null);
  const [trend, setTrend] = useState<BudgetAggregate[]>([]);
  const [goals, setGoals] = useState<GoalProgress[]>([]);
  const [transactions, setTransactions] = useState<ScoredTransactionView[]>([]);
  const [budget, setBudgetState] = useState<BudgetStatus | null>(null);
  const [categories, setCategories] = useState<CategoryBreakdown[]>([]);
  const [swaps, setSwaps] = useState<Swap[]>([]);
  const [live, setLive] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [s, tr, g, tx, b, cats, sw] = await Promise.all([
        fetchSummary(),
        fetchTrend(6),
        fetchGoals(),
        fetchTransactions(),
        fetchBudget(),
        fetchCategories(),
        fetchSwaps(),
      ]);
      setSummary(s);
      setTrend(tr);
      setGoals(g);
      setTransactions(tx);
      setBudgetState(b);
      setCategories(cats);
      setSwaps(sw);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  // Live updates: subscribe to the SSE stream and refresh (debounced) as the pipeline scores
  // transactions. The pushed payload is the fresh summary, so numbers update instantly.
  useEffect(() => {
    const token = getToken();
    if (!token) return;
    const source = new EventSource(`/api/v1/stream?token=${encodeURIComponent(token)}`);
    let debounce: ReturnType<typeof setTimeout> | undefined;

    source.addEventListener("connected", () => setLive(true));
    source.addEventListener("dashboard-update", (e) => {
      try {
        setSummary(JSON.parse((e as MessageEvent).data) as BudgetAggregate);
      } catch {
        /* ignore malformed frame */
      }
      clearTimeout(debounce);
      debounce = setTimeout(() => void load(), 400); // coalesce bursts, refresh the rest
    });
    source.onerror = () => setLive(false);

    return () => {
      clearTimeout(debounce);
      source.close();
    };
  }, [load]);

  const onCreateGoal = async (body: CreateGoalRequest) => {
    await createGoal(body);
    await load();
  };

  const onSetBudget = async (limit: number) => {
    const updated = await setBudget(limit);
    setBudgetState(updated);
  };

  return (
    <div className="app">
      <header className="app-header">
        <div>
          <h1>Impact Budget</h1>
          <p className="tagline">Spending by impact, not by category.</p>
        </div>
        <div className="app-header-user">
          {live && (
            <span className="live-dot" role="status" title="Live updates on">
              <span aria-hidden="true">●</span> live
            </span>
          )}
          <span className="muted">{user?.displayName ?? user?.email}</span>
          <button className="logout-btn" type="button" onClick={logout}>
            Sign out
          </button>
        </div>
      </header>

      {error && (
        <div className="error" role="alert">
          Couldn’t reach the API: {error}
        </div>
      )}

      {summary && <ImpactSummary summary={summary} />}
      <BudgetTracker budget={budget} onSetBudget={onSetBudget} />
      <CategoryChart categories={categories} />
      {trend.length > 0 && <TrendChart trend={trend} />}
      <GreenerSwaps swaps={swaps} />
      <GoalTracker goals={goals} onCreate={onCreateGoal} />
      <TransactionList transactions={transactions} />
    </div>
  );
}
