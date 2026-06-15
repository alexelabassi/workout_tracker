import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { AnalyticsView } from "./AnalyticsView";
import { fetchAnalyticsOverview } from "./api";
import type { AnalyticsOverview } from "./types";

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; data: AnalyticsOverview };

export function AnalyticsPage() {
  const [state, setState] = useState<LoadState>({ kind: "loading" });

  const load = useCallback(async () => {
    setState({ kind: "loading" });
    try {
      const data = await fetchAnalyticsOverview();
      setState({ kind: "ready", data });
    } catch (err) {
      setState({ kind: "error", message: err instanceof ApiError ? err.message : "Could not load analytics." });
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>Progress</h1>
          <p className="muted">
            <Link to="/">← Back to dashboard</Link>
          </p>
        </div>
        <Link to="/history" className="button button--ghost">
          View history
        </Link>
      </header>

      <main className="app__main">
        {state.kind === "loading" && (
          <section className="card">
            <p className="muted">Loading…</p>
          </section>
        )}

        {state.kind === "error" && (
          <section className="card">
            <div className="status status--down">
              <span className="dot" />
              <div>
                <strong>Couldn’t load analytics</strong>
                <p className="muted">{state.message}</p>
                <button type="button" className="button button--ghost" onClick={() => void load()}>
                  Retry
                </button>
              </div>
            </div>
          </section>
        )}

        {state.kind === "ready" && (
          <AnalyticsView
            data={state.data}
            emptyAction={
              <Link to="/workouts/start" className="button button--block">
                Start a workout
              </Link>
            }
          />
        )}
      </main>
    </div>
  );
}
