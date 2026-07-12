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
