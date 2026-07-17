import type {
  AuthResponse,
  BudgetAggregate,
  BudgetStatus,
  CreateGoalRequest,
  GoalProgress,
  ScoredTransactionView,
} from "./types";
import { getToken, handleUnauthorized } from "./session";

const BASE = "/api/v1";

/** Fetch with the bearer token attached; a 401 forces logout via the session handler. */
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getToken();
  const headers = new Headers(init?.headers);
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (init?.body) headers.set("Content-Type", "application/json");

  const res = await fetch(`${BASE}${path}`, { ...init, headers });
  if (res.status === 401) {
    handleUnauthorized();
    throw new Error("Session expired — please sign in again.");
  }
  if (!res.ok) {
    throw new Error(`${init?.method ?? "GET"} ${path} failed: ${res.status}`);
  }
  return (res.status === 204 ? undefined : await res.json()) as T;
}

// --- Auth (public) ---------------------------------------------------------
export function login(email: string, password: string): Promise<AuthResponse> {
  return request<AuthResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export function register(
  email: string,
  password: string,
  displayName: string,
): Promise<AuthResponse> {
  return request<AuthResponse>("/auth/register", {
    method: "POST",
    body: JSON.stringify({ email, password, displayName }),
  });
}

// --- Dashboard (authenticated) ---------------------------------------------
export function fetchSummary(): Promise<BudgetAggregate> {
  return request<BudgetAggregate>("/dashboard/summary");
}

export function fetchTrend(months = 6): Promise<BudgetAggregate[]> {
  return request<BudgetAggregate[]>(`/dashboard/trend?months=${months}`);
}

export function fetchTransactions(): Promise<ScoredTransactionView[]> {
  return request<ScoredTransactionView[]>("/dashboard/transactions");
}

export function fetchGoals(): Promise<GoalProgress[]> {
  return request<GoalProgress[]>("/goals");
}

export async function createGoal(body: CreateGoalRequest): Promise<void> {
  await request<unknown>("/goals", { method: "POST", body: JSON.stringify(body) });
}

export function fetchBudget(): Promise<BudgetStatus> {
  return request<BudgetStatus>("/budget");
}

export function setBudget(monthlyLimit: number): Promise<BudgetStatus> {
  return request<BudgetStatus>("/budget", {
    method: "PUT",
    body: JSON.stringify({ monthlyLimit }),
  });
}
