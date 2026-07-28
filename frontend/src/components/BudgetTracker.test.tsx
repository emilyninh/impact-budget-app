import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { BudgetTracker } from "./BudgetTracker";
import type { BudgetStatus } from "../types";

describe("BudgetTracker", () => {
  it("renders nothing when there is no budget", () => {
    const { container } = render(<BudgetTracker budget={null} onSetBudget={vi.fn()} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("shows spend vs limit and the status label", () => {
    const budget: BudgetStatus = {
      userId: "u",
      yearMonth: "2026-07",
      monthlyLimit: 500,
      spent: 411.4,
      remaining: 88.6,
      pctUsed: 82.3,
      daysElapsed: 16,
      daysInMonth: 31,
      projectedSpend: 797.09,
      status: "AT_RISK",
    };
    render(<BudgetTracker budget={budget} onSetBudget={vi.fn()} />);

    expect(screen.getByText("$411.40")).toBeInTheDocument();
    expect(screen.getByText(/of \$500\.00/)).toBeInTheDocument();
    expect(screen.getByText(/at risk/i)).toBeInTheDocument();
  });
});
