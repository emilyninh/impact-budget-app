import { describe, it, expect } from "vitest";
import { render, screen, within } from "@testing-library/react";
import { TransactionList } from "./TransactionList";
import type { ScoredTransactionView } from "../types";

describe("TransactionList", () => {
  it("shows an empty state when there are no transactions", () => {
    render(<TransactionList transactions={[]} />);
    expect(screen.getByText(/no scored transactions yet/i)).toBeInTheDocument();
  });

  it("renders a row with merchant, account, category, amount, and a 'local' badge", () => {
    const tx: ScoredTransactionView = {
      merchantName: "Rosie's Cafe",
      category: "Eating Out",
      txnDate: "2026-07-01",
      amount: 12.5,
      localScore: 80,
      sustainabilityScore: 60,
      localIndependent: true,
      institutionName: "Chase",
      excludedFromSpend: false,
    };
    render(<TransactionList transactions={[tx]} />);

    // Scope to the table body so the legend's example "local" pill doesn't match.
    const table = within(screen.getByRole("table"));
    expect(table.getByText("Rosie's Cafe")).toBeInTheDocument();
    expect(table.getByText("Chase")).toBeInTheDocument();
    expect(table.getByText("Eating Out")).toBeInTheDocument();
    expect(table.getByText("$12.50")).toBeInTheDocument();
    expect(table.getByText("local", { selector: ".pill" })).toBeInTheDocument();
  });

  it("filters rows to the selected category and shows a clear control", () => {
    const rows: ScoredTransactionView[] = [
      {
        merchantName: "Rosie's Cafe", category: "Eating Out", txnDate: "2026-07-01", amount: 12.5,
        localScore: 80, sustainabilityScore: 60, localIndependent: true,
        institutionName: "Chase", excludedFromSpend: false,
      },
      {
        merchantName: "Trader Joe's", category: "Groceries", txnDate: "2026-07-02", amount: 40,
        localScore: 50, sustainabilityScore: 55, localIndependent: false,
        institutionName: "Capital One", excludedFromSpend: false,
      },
    ];
    render(<TransactionList transactions={rows} filterCategory="Groceries" />);

    const table = within(screen.getByRole("table"));
    expect(table.getByText("Trader Joe's")).toBeInTheDocument();
    expect(table.queryByText("Rosie's Cafe")).not.toBeInTheDocument();
    expect(screen.getByText(/showing: groceries/i)).toBeInTheDocument();
  });

  it("shows a transfer without score meters", () => {
    const transfer: ScoredTransactionView = {
      merchantName: "Fidelity", category: "Transfers", txnDate: "2026-07-05", amount: 500,
      localScore: 40, sustainabilityScore: 50, localIndependent: false,
      institutionName: "Chase", excludedFromSpend: true,
    };
    render(<TransactionList transactions={[transfer]} />);

    const table = within(screen.getByRole("table"));
    expect(table.getByText("Fidelity")).toBeInTheDocument();
    expect(table.getByText("Transfers")).toBeInTheDocument();
    // Transfers aren't scored — no local/sustainability gauge is rendered.
    expect(table.queryByLabelText(/score \d+ out of 100/i)).not.toBeInTheDocument();
  });
});
