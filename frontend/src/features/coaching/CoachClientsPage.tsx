import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { useToast } from "../../shared/ui/ToastProvider";
import { fetchClients, inviteClient, revokeClient } from "./api";
import type { ClientSummary } from "./types";

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; clients: ClientSummary[] };

export function CoachClientsPage() {
  const { showToast } = useToast();
  const [state, setState] = useState<LoadState>({ kind: "loading" });
  const [email, setEmail] = useState("");

  const load = useCallback(async () => {
    setState({ kind: "loading" });
    try {
      setState({ kind: "ready", clients: await fetchClients() });
    } catch (err) {
      setState({ kind: "error", message: err instanceof ApiError ? err.message : "Could not load clients." });
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const onInvite = async (event: FormEvent) => {
    event.preventDefault();
    if (!email.trim()) {
      return;
    }
    try {
      await inviteClient(email.trim());
      showToast("Invite sent. The client must accept it before you can view their data.");
      setEmail("");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not send the invite.", "error");
    }
  };

  const onRevoke = async (client: ClientSummary) => {
    if (!window.confirm(`Remove ${client.displayName ?? client.email} as a client?`)) {
      return;
    }
    try {
      await revokeClient(client.clientId);
      showToast("Client removed.");
      void load();
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not remove the client.", "error");
    }
  };

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>Coach</h1>
          <p className="muted">
            <Link to="/">← Back to dashboard</Link>
          </p>
        </div>
      </header>

      <main className="app__main">
        <section className="card">
          <header className="card__header">
            <h2>Invite a client</h2>
          </header>
          <p className="muted">
            Invite an existing user by email. They must accept before you can read their training data.
          </p>
          <form className="muscle-add" onSubmit={onInvite}>
            <input
              type="email"
              placeholder="client@email.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              aria-label="Client email"
            />
            <button type="submit" className="button" disabled={!email.trim()}>
              Send invite
            </button>
          </form>
        </section>

        <section className="card">
          <header className="card__header">
            <h2>Your clients</h2>
          </header>

          {state.kind === "loading" && <p className="muted">Loading…</p>}

          {state.kind === "error" && (
            <div className="status status--down">
              <span className="dot" />
              <div>
                <strong>Couldn’t load clients</strong>
                <p className="muted">{state.message}</p>
                <button type="button" className="button button--ghost" onClick={() => void load()}>
                  Retry
                </button>
              </div>
            </div>
          )}

          {state.kind === "ready" && state.clients.length === 0 && (
            <p className="muted">No active clients yet. Invite someone to get started.</p>
          )}

          {state.kind === "ready" && state.clients.length > 0 && (
            <ul className="exercise-list">
              {state.clients.map((client) => (
                <li key={client.clientId} className="exercise-row">
                  <div className="exercise-row__main">
                    <div className="exercise-row__title">
                      <strong>{client.displayName ?? client.email}</strong>
                    </div>
                    <p className="muted exercise-row__muscles">
                      {client.email}
                      {client.activeSince ? ` · since ${new Date(client.activeSince).toLocaleDateString()}` : ""}
                    </p>
                  </div>
                  <div className="app__actions">
                    <Link
                      to={`/coach/clients/${client.clientId}`}
                      state={{ clientName: client.displayName ?? client.email }}
                      className="button button--ghost"
                    >
                      Open
                    </Link>
                    <button type="button" className="button button--ghost" onClick={() => void onRevoke(client)}>
                      Remove
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </div>
  );
}
