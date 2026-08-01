import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AssistantChat } from "./AssistantChat";
import type { ChatResponse, ChatTurn } from "../types";

function openPanel() {
  return userEvent.click(screen.getByRole("button", { name: /open the spending assistant/i }));
}

describe("AssistantChat", () => {
  it("opens the panel from the launcher and shows seed prompts", async () => {
    render(<AssistantChat send={vi.fn()} />);
    expect(screen.queryByLabelText(/ask the assistant/i)).not.toBeInTheDocument();

    await openPanel();

    expect(screen.getByRole("dialog", { name: /assistant/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/ask the assistant/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /how over budget/i })).toBeInTheDocument();
  });

  it("sends a seed prompt and renders the grounded reply", async () => {
    const send = vi.fn(
      async (): Promise<ChatResponse> => ({
        reply: "You are $106.95 over your $500 budget.",
        toolsUsed: ["get_budget_status"],
      }),
    );
    render(<AssistantChat send={send} />);
    await openPanel();

    await userEvent.click(screen.getByRole("button", { name: /how over budget/i }));

    expect(await screen.findByText(/\$106\.95 over/)).toBeInTheDocument();
    const sentHistory = send.mock.calls[0][0] as ChatTurn[];
    expect(sentHistory).toEqual([{ role: "user", content: "How over budget am I this month?" }]);
  });

  it("sends a typed question and shows the answer", async () => {
    const send = vi.fn(
      async (): Promise<ChatResponse> => ({ reply: "Groceries: $130.70.", toolsUsed: [] }),
    );
    render(<AssistantChat send={send} />);
    await openPanel();

    await userEvent.type(
      screen.getByLabelText(/ask the assistant/i),
      "What did I spend on Groceries?",
    );
    await userEvent.click(screen.getByRole("button", { name: /^send$/i }));

    expect(await screen.findByText(/Groceries: \$130\.70/)).toBeInTheDocument();
  });

  it("surfaces an error when the assistant call fails", async () => {
    const send = vi.fn(async (): Promise<ChatResponse> => {
      throw new Error("boom");
    });
    render(<AssistantChat send={send} />);
    await openPanel();

    await userEvent.click(screen.getByRole("button", { name: /is patagonia/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent(/boom/);
  });

  it("closes on Escape and returns to the launcher", async () => {
    render(<AssistantChat send={vi.fn()} />);
    await openPanel();
    expect(screen.getByRole("dialog")).toBeInTheDocument();

    await userEvent.keyboard("{Escape}");

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /open the spending assistant/i }),
    ).toBeInTheDocument();
  });
});
