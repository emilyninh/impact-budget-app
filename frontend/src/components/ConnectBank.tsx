import { useCallback, useEffect, useState } from "react";
import { usePlaidLink, type PlaidLinkOnSuccess } from "react-plaid-link";
import { createPlaidLinkToken, exchangePlaidPublicToken, syncPlaid } from "../api";

/**
 * "Connect a bank" via Plaid Link. Fetches a link_token from the backend, opens the Plaid
 * Link widget, and on success exchanges the public_token (the backend stores the item and runs
 * an initial sync). Calls {@code onLinked} so the dashboard refreshes with the new data.
 *
 * In Plaid sandbox, log in with any institution using username `user_good` / password `pass_good`.
 */
export function ConnectBank({ onLinked }: { onLinked: () => void }) {
  const [linkToken, setLinkToken] = useState<string | null>(null);
  const [status, setStatus] = useState<"idle" | "linking" | "syncing" | "error">("idle");

  // Fetch a fresh link token on mount (tokens are short-lived).
  useEffect(() => {
    createPlaidLinkToken()
      .then((r) => setLinkToken(r.linkToken))
      .catch(() => setStatus("error"));
  }, []);

  const onSuccess = useCallback<PlaidLinkOnSuccess>(
    (publicToken) => {
      if (!publicToken) return;
      setStatus("linking");
      exchangePlaidPublicToken(publicToken)
        .then(() => {
          onLinked();
          setStatus("idle");
        })
        .catch(() => setStatus("error"));
    },
    [onLinked],
  );

  const { open, ready } = usePlaidLink({ token: linkToken, onSuccess });

  const onSync = useCallback(() => {
    setStatus("syncing");
    syncPlaid()
      .then(() => {
        onLinked();
        setStatus("idle");
      })
      .catch(() => setStatus("error"));
  }, [onLinked]);

  const busy = status === "linking" || status === "syncing";
  return (
    <div className="connect-bank">
      <button
        className="logout-btn"
        type="button"
        disabled={!ready || !linkToken || busy}
        onClick={() => open()}
        title="Link a bank account with Plaid"
      >
        {status === "linking" ? "Linking…" : "Connect a bank"}
      </button>
      <button
        className="logout-btn"
        type="button"
        disabled={busy}
        onClick={onSync}
        title="Pull the latest transactions from your linked banks"
      >
        {status === "syncing" ? "Syncing…" : "Sync"}
      </button>
      {status === "error" && (
        <span className="muted connect-bank-error">Plaid unavailable — check credentials</span>
      )}
    </div>
  );
}
