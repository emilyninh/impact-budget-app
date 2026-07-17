export interface BudgetAggregate {
  userId: string;
  yearMonth: string;
  totalSpend: number;
  localImpactPct: number;
  sustainabilityImpactPct: number;
  localIndependentSpend: number;
  transactionCount: number;
}

export type Dimension = "LOCAL" | "SUSTAINABLE";

export interface GoalProgress {
  goalId: string;
  dimension: Dimension;
  baselinePct: number;
  targetPct: number;
  currentPct: number;
  targetDate: string;
  progressPct: number;
  achieved: boolean;
}

export interface ScoredTransactionView {
  merchantName: string | null;
  category: string | null;
  txnDate: string;
  amount: number;
  localScore: number;
  sustainabilityScore: number;
  localIndependent: boolean;
}

export interface CreateGoalRequest {
  dimension: Dimension;
  baselinePct: number;
  targetPct: number;
  targetDate: string;
}

export type BudgetStatusKind = "NO_BUDGET" | "OVER" | "AT_RISK" | "ON_TRACK";

export interface BudgetStatus {
  userId: string;
  yearMonth: string;
  monthlyLimit: number | null;
  spent: number;
  remaining: number | null;
  pctUsed: number;
  daysElapsed: number;
  daysInMonth: number;
  projectedSpend: number;
  status: BudgetStatusKind;
}
