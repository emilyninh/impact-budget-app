export interface AuthUser {
  userId: string;
  email: string;
  displayName: string | null;
}

export interface AuthResponse extends AuthUser {
  token: string;
  expiresInSeconds: number;
}

export interface BudgetAggregate {
  userId: string;
  yearMonth: string;
  totalSpend: number;
  localImpactPct: number;
  sustainabilityImpactPct: number;
  localIndependentSpend: number;
  transactionCount: number;
  scoredSharePct: number;
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
  institutionName: string | null;
  excludedFromSpend: boolean;
}

export interface CreateGoalRequest {
  dimension: Dimension;
  baselinePct: number;
  targetPct: number;
  targetDate: string;
}

export interface CategoryBreakdown {
  category: string;
  totalSpend: number;
  txnCount: number;
  avgSustainability: number;
}

export interface GreenerAlternative {
  merchant: string;
  sustainabilityScore: number;
  flags: string[];
}

export interface Swap {
  fromMerchant: string;
  category: string;
  fromScore: number;
  suggestions: GreenerAlternative[];
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
