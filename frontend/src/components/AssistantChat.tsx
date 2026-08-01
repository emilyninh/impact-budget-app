import { useEffect, useRef, useState } from "react";
import type { ChatResponse, ChatTurn } from "../types";

/** The three example questions, offered as one-click chips before the first message. */
const SEED_PROMPTS = [
  "How over budget am I this month?",
  "What did I spend on Groceries?",
  "Is Patagonia a sustainable purchase?",
];

/**
 * The grounded assistant, as a floating chat widget — a launcher pinned bottom-right that opens a
 * pop-up panel. Answers are produced server-side from the user's real budget, categories, and
 * merchant scores; this component only manages the conversation and defers the request to the
 * injected {@link send} function (so it stays trivially testable).
 *
 * Chrome stays neutral by design (the Assertion Rule reserves color for figures that make a claim),
 * the copy reports rather than coaches, and it's the app's first floating surface — so it rides the
 * semantic z-index scale + the sanctioned float shadow, and honours prefers-reduced-motion.
 */
export function AssistantChat({
  send,
}: {
  send: (messages: ChatTurn[]) => Promise<ChatResponse>;
}) {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<ChatTurn[]>([]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const launcherRef = useRef<HTMLButtonElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const bodyRef = useRef<HTMLDivElement>(null);
  const wasOpen = useRef(false);

  // Focus the input on open; return focus to the launcher on close.
  useEffect(() => {
    if (open) {
      inputRef.current?.focus();
    } else if (wasOpen.current) {
      launcherRef.current?.focus();
    }
    wasOpen.current = open;
  }, [open]);

  // Keep the newest message in view.
  useEffect(() => {
    if (bodyRef.current) {
      bodyRef.current.scrollTop = bodyRef.current.scrollHeight;
    }
  }, [messages, sending]);

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

  function clearThread() {
    setMessages([]);
    setError(null);
    inputRef.current?.focus();
  }

  if (!open) {
    return (
      <button
        ref={launcherRef}
        type="button"
        className="assistant-launcher"
        aria-label="Open the spending assistant"
        aria-expanded={false}
        aria-controls="assistant-panel"
        onClick={() => setOpen(true)}
      >
        <ChatIcon />
        <span className="assistant-launcher-label">Assistant</span>
      </button>
    );
  }

  return (
    <section
      id="assistant-panel"
      className="assistant-panel"
      role="dialog"
      aria-modal="false"
      aria-label="Assistant"
      onKeyDown={(e) => {
        if (e.key === "Escape") setOpen(false);
      }}
    >
      <header className="assistant-header">
        <div>
          <h2 className="assistant-title">Assistant</h2>
          <p className="assistant-subtitle">Grounded in your data</p>
        </div>
        <div className="assistant-header-actions">
          {messages.length > 0 && (
            <button type="button" className="assistant-clear" onClick={clearThread}>
              Clear
            </button>
          )}
          <button
            type="button"
            className="assistant-close"
            aria-label="Close the assistant"
            onClick={() => setOpen(false)}
          >
            ×
          </button>
        </div>
      </header>

      <div className="assistant-body" ref={bodyRef}>
        {messages.length === 0 ? (
          <>
            <p className="assistant-intro">
              Ask about your budget, categories, or whether a store is a sustainable purchase.
            </p>
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
          </>
        ) : (
          <ul className="assistant-log">
            {messages.map((m, i) => (
              <li key={i} className={`assistant-msg assistant-${m.role}`}>
                <span className="assistant-role">{m.role === "user" ? "You" : "Assistant"}</span>
                <span className="assistant-text">{m.content}</span>
              </li>
            ))}
            {sending && (
              <li className="assistant-msg assistant-assistant">
                <span className="assistant-role">Assistant</span>
                <span className="assistant-thinking" aria-live="polite">
                  <span className="assistant-caret" aria-hidden="true" />
                  Thinking…
                </span>
              </li>
            )}
          </ul>
        )}

        {error && (
          <div className="error" role="alert">
            {error}
          </div>
        )}
      </div>

      <form
        className="assistant-form"
        onSubmit={(e) => {
          e.preventDefault();
          void ask(input);
        }}
      >
        <input
          ref={inputRef}
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

/** Minimal speech-bubble glyph for the launcher (currentColor, 1.6px stroke). */
function ChatIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M21 11.5a8.38 8.38 0 0 1-8.5 8.5 8.5 8.5 0 0 1-3.8-.9L3 21l1.9-5.7a8.5 8.5 0 0 1-.9-3.8A8.38 8.38 0 0 1 12.5 3 8.38 8.38 0 0 1 21 11.5z"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
