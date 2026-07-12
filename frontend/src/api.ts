import type {
  BudgetAggregate,
  CreateGoalRequest,
  GoalProgress,
  ScoredTransactionView,
} from "./types";

async function get<T>(path: string): Promise<T> {
  const res = await fetch(path);
  if (!res.ok) {
    throw new Error(`GET ${path} failed: ${res.status}`);
  }
  return res.json() as Promise<T>;
}

export function fetchSummary(): Promise<BudgetAggregate> {
  return get<BudgetAggregate>("/api/dashboard/summary");
}

export function fetchTrend(months = 6): Promise<BudgetAggregate[]> {
  return get<BudgetAggregate[]>(`/api/dashboard/trend?months=${months}`);
}

export function fetchTransactions(): Promise<ScoredTransactionView[]> {
  return get<ScoredTransactionView[]>("/api/dashboard/transactions");
}

export function fetchGoals(): Promise<GoalProgress[]> {
  return get<GoalProgress[]>("/api/goals");
}

export async function createGoal(body: CreateGoalRequest): Promise<void> {
  const res = await fetch("/api/goals", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    throw new Error(`Create goal failed: ${res.status}`);
  }
}
