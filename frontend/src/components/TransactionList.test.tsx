import { describe, it, expect } from "vitest";
import { render, screen, within } from "@testing-library/react";
import { TransactionList } from "./TransactionList";
import type { ScoredTransactionView } from "../types";

describe("TransactionList", () => {
  it("shows an empty state when there are no transactions", () => {
    render(<TransactionList transactions={[]} />);
    expect(screen.getByText(/no scored transactions yet/i)).toBeInTheDocument();
  });

  it("renders a row with merchant, category, amount, and a 'local' badge", () => {
    const tx: ScoredTransactionView = {
      merchantName: "Rosie's Cafe",
      category: "Eating Out",
      txnDate: "2026-07-01",
      amount: 12.5,
      localScore: 80,
      sustainabilityScore: 60,
      localIndependent: true,
    };
    render(<TransactionList transactions={[tx]} />);

    // Scope to the table body so the legend's example "local" pill doesn't match.
    const table = within(screen.getByRole("table"));
    expect(table.getByText("Rosie's Cafe")).toBeInTheDocument();
    expect(table.getByText("Eating Out")).toBeInTheDocument();
    expect(table.getByText("$12.50")).toBeInTheDocument();
    expect(table.getByText("local", { selector: ".pill" })).toBeInTheDocument();
  });
});
