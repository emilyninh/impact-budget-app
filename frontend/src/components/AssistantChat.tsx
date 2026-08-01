import { useState } from "react";
import type { ChatResponse, ChatTurn } from "../types";

/** The three example questions, offered as one-click chips before the first message. */
const SEED_PROMPTS = [
  "How over budget am I this month?",
  "What did I spend on Groceries?",
  "Is Patagonia a sustainable purchase?",
];

/**
 * Grounded assistant panel. Answers are produced server-side from the user's real budget,
 * categories, and merchant scores — this component only manages the conversation and defers the
 * request to the injected {@link send} function (so it's trivially testable).
 *
 * Chrome stays neutral by design (the Assertion Rule reserves color for figures), and the copy
 * reports rather than coaches.
 */
export function AssistantChat({
  send,
}: {
  send: (messages: ChatTurn[]) => Promise<ChatResponse>;
}) {
  const [messages, setMessages] = useState<ChatTurn[]>([]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function ask(text: string) {
    const question = text.trim();
    if (!question || sending) return;
    setError(null);
    const next: ChatTurn[] = [...messages, { role: "user", content: question }];
    setMessages(next);
    setInput("");
    setSending(true);
    try {
      const res = await send(next);
      setMessages([...next, { role: "assistant", content: res.reply }]);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Couldn’t reach the assistant.");
    } finally {
      setSending(false);
    }
  }

  return (
    <section className="card assistant">
      <h2>Ask about your spending</h2>
      <p className="muted assistant-intro">
        Grounded in your data — answers come from your real budget, categories, and merchant scores.
      </p>

      {messages.length === 0 ? (
        <div className="assistant-seeds">
          {SEED_PROMPTS.map((p) => (
            <button
              key={p}
              type="button"
              className="assistant-seed"
              onClick={() => void ask(p)}
              disabled={sending}
            >
              {p}
            </button>
          ))}
        </div>
      ) : (
        <ul className="assistant-log">
          {messages.map((m, i) => (
            <li key={i} className={`assistant-msg assistant-${m.role}`}>
              <span className="assistant-role">{m.role === "user" ? "You" : "Assistant"}</span>
              <span className="assistant-text">{m.content}</span>
            </li>
          ))}
          {sending && (
            <li className="assistant-msg assistant-assistant" aria-live="polite">
              <span className="assistant-role">Assistant</span>
              <span className="assistant-text muted">Thinking…</span>
            </li>
          )}
        </ul>
      )}

      {error && (
        <div className="error" role="alert">
          {error}
        </div>
      )}

      <form
        className="assistant-form"
        onSubmit={(e) => {
          e.preventDefault();
          void ask(input);
        }}
      >
        <input
          className="assistant-input"
          type="text"
          value={input}
          placeholder="Ask a question…"
          aria-label="Ask the assistant a question"
          onChange={(e) => setInput(e.target.value)}
          disabled={sending}
        />
        <button className="assistant-send" type="submit" disabled={sending || !input.trim()}>
          Send
        </button>
      </form>
    </section>
  );
}
