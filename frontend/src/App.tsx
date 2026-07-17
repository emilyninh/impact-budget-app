import { useCallback, useEffect, useState } from "react";
import {
  createGoal,
  fetchBudget,
  fetchGoals,
  fetchSummary,
  fetchTransactions,
  fetchTrend,
  setBudget,
} from "./api";
import type {
  BudgetAggregate,
  BudgetStatus,
  CreateGoalRequest,
  GoalProgress,
  ScoredTransactionView,
} from "./types";
import { ImpactSummary } from "./components/ImpactSummary";
import { BudgetTracker } from "./components/BudgetTracker";
import { TrendChart } from "./components/TrendChart";
import { GoalTracker } from "./components/GoalTracker";
import { TransactionList } from "./components/TransactionList";

export default function App() {
  const [summary, setSummary] = useState<BudgetAggregate | null>(null);
  const [trend, setTrend] = useState<BudgetAggregate[]>([]);
  const [goals, setGoals] = useState<GoalProgress[]>([]);
  const [transactions, setTransactions] = useState<ScoredTransactionView[]>([]);
  const [budget, setBudgetState] = useState<BudgetStatus | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [s, tr, g, tx, b] = await Promise.all([
        fetchSummary(),
        fetchTrend(6),
        fetchGoals(),
        fetchTransactions(),
        fetchBudget(),
      ]);
      setSummary(s);
      setTrend(tr);
      setGoals(g);
      setTransactions(tx);
      setBudgetState(b);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load");
    }
  }, []);

  useEffect(() => {
    void load();
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
        <h1>Impact Budget</h1>
        <p className="tagline">Spending by impact, not by category.</p>
      </header>

      {error && <div className="error">Couldn’t reach the API: {error}</div>}

      {summary && <ImpactSummary summary={summary} />}
      <BudgetTracker budget={budget} onSetBudget={onSetBudget} />
      {trend.length > 0 && <TrendChart trend={trend} />}
      <GoalTracker goals={goals} onCreate={onCreateGoal} />
      <TransactionList transactions={transactions} />
    </div>
  );
}
