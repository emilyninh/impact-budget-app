import { useCallback, useEffect, useState } from "react";
import { usePlaidLink, type PlaidLinkOnSuccess } from "react-plaid-link";
import { createPlaidLinkToken, exchangePlaidPublicToken, syncPlaid } from "../api";

// Where the in-progress link token is stashed so it survives the OAuth round-trip to the bank.
const LINK_TOKEN_KEY = "plaid_link_token";

// OAuth banks (Chase, Capital One, …) send the browser back here with ?oauth_state_id=… appended.
// On that return leg Link must be re-created with the SAME token plus receivedRedirectUri, then
// reopened — a fresh token won't match the oauth_state_id and Plaid rejects the resume.
const isOAuthRedirect = () => window.location.search.includes("oauth_state_id=");

/**
 * "Connect a bank" via Plaid Link. Fetches a link_token from the backend, opens the Plaid
 * Link widget, and on success exchanges the public_token (the backend stores the item and runs
 * an initial sync). Calls {@code onLinked} so the dashboard refreshes with the new data.
 *
 * Handles OAuth banks: their flow redirects out to the bank and back to PLAID_REDIRECT_URI, at
 * which point this component reuses the stored token and auto-resumes Link.
 *
 * In Plaid sandbox, log in with any institution using username `user_good` / password `pass_good`.
 */
export function ConnectBank({ onLinked }: { onLinked: () => void }) {
  const [linkToken, setLinkToken] = useState<string | null>(null);
  const [status, setStatus] = useState<"idle" | "linking" | "syncing" | "error">("idle");

  useEffect(() => {
    // Returning from an OAuth bank: reuse the token that started the flow.
    if (isOAuthRedirect()) {
      setLinkToken(localStorage.getItem(LINK_TOKEN_KEY));
      return;
    }
    // Fresh visit: fetch a new token (they're short-lived) and stash it for a possible OAuth hop.
    createPlaidLinkToken()
      .then((r) => {
        setLinkToken(r.linkToken);
        localStorage.setItem(LINK_TOKEN_KEY, r.linkToken);
      })
      .catch(() => setStatus("error"));
  }, []);

  const onSuccess = useCallback<PlaidLinkOnSuccess>(
    (publicToken) => {
      if (!publicToken) return;
      setStatus("linking");
      localStorage.removeItem(LINK_TOKEN_KEY);
      exchangePlaidPublicToken(publicToken)
        .then(() => {
          // Drop the ?oauth_state_id=… so a page refresh doesn't retrigger the resume.
          if (isOAuthRedirect()) {
            window.history.replaceState({}, "", window.location.pathname);
          }
          onLinked();
          setStatus("idle");
        })
        .catch(() => setStatus("error"));
    },
    [onLinked],
  );

  const { open, ready } = usePlaidLink({
    token: linkToken,
    onSuccess,
    // Set only on the return leg — Plaid rejects it on the initial open.
    receivedRedirectUri: isOAuthRedirect() ? window.location.href : undefined,
  });

  // Auto-resume Link once it's ready after an OAuth redirect back.
  useEffect(() => {
    if (isOAuthRedirect() && ready) open();
  }, [ready, open]);

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
        {status === "syncing" ? "Refreshing…" : "Refresh"}
      </button>
      {status === "error" && (
        <span className="muted connect-bank-error">Plaid unavailable — check credentials</span>
      )}
    </div>
  );
}
