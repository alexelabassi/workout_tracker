import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { fetchHistory } from "./api";
import type { HistoryItem, HistoryPage as HistoryPageData } from "./types";

const PAGE_SIZE = 10;

const STATUS_LABEL: Record<string, string> = {
  IN_PROGRESS: "In progress",
  FINISHED: "Finished",
  CANCELLED: "Cancelled",
};

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; data: HistoryPageData };

function formatDuration(seconds: number | null): string {
  if (seconds == null) {
    return "—";
  }
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) {
    return `${minutes} min`;
  }
  const hours = Math.floor(minutes / 60);
  return `${hours}h ${minutes % 60}m`;
}

export function HistoryPage() {
  const [page, setPage] = useState(0);
  const [state, setState] = useState<LoadState>({ kind: "loading" });

  const load = useCallback(async (which: number) => {
    setState({ kind: "loading" });
    try {
      const data = await fetchHistory(which, PAGE_SIZE);
      setState({ kind: "ready", data });
    } catch (err) {
      setState({ kind: "error", message: err instanceof ApiError ? err.message : "Could not load history." });
    }
  }, []);

  useEffect(() => {
    void load(page);
  }, [load, page]);

  const meta = (item: HistoryItem): string =>
    [
      new Date(item.startedAt).toLocaleDateString(),
      item.templateDayName ?? item.templateName,
      item.gymName,
      `${item.setCount} sets`,
      item.totalVolume > 0 ? `${item.totalVolume} kg vol` : null,
      formatDuration(item.durationSeconds),
    ]
      .filter(Boolean)
      .join(" · ");

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>History</h1>
          <p className="muted">
            <Link to="/">← Back to dashboard</Link>
          </p>
        </div>
        <Link to="/analytics" className="button button--ghost">
          View progress
        </Link>
      </header>

      <main className="app__main">
        <section className="card">
          <header className="card__header">
            <h2>Your workouts</h2>
          </header>

          {state.kind === "loading" && <p className="muted">Loading…</p>}

          {state.kind === "error" && (
            <div className="status status--down">
              <span className="dot" />
              <div>
                <strong>Couldn’t load history</strong>
                <p className="muted">{state.message}</p>
                <button type="button" className="button button--ghost" onClick={() => void load(page)}>
                  Retry
                </button>
              </div>
            </div>
          )}

          {state.kind === "ready" && state.data.items.length === 0 && (
            <p className="muted">No workouts logged yet. Start one to build your history.</p>
          )}

          {state.kind === "ready" && state.data.items.length > 0 && (
            <>
              <ul className="exercise-list">
                {state.data.items.map((item) => (
                  <li key={item.sessionId} className="exercise-row">
                    <div className="exercise-row__main">
                      <div className="exercise-row__title">
                        <strong>{item.templateName ?? "Workout"}</strong>
                        <span className="badge badge--muted">{STATUS_LABEL[item.status] ?? item.status}</span>
                      </div>
                      <p className="muted exercise-row__muscles">{meta(item)}</p>
                    </div>
                    <div className="app__actions">
                      <Link to={`/workouts/${item.sessionId}`} className="button button--ghost">
                        View
                      </Link>
                    </div>
                  </li>
                ))}
              </ul>

              <div className="app__actions" style={{ marginTop: 16 }}>
                <button type="button" className="button button--ghost" disabled={page === 0}
                  onClick={() => setPage((current) => Math.max(current - 1, 0))}>
                  Previous
                </button>
                <button type="button" className="button button--ghost" disabled={!state.data.hasNext}
                  onClick={() => setPage((current) => current + 1)}>
                  Next
                </button>
              </div>
            </>
          )}
        </section>
      </main>
    </div>
  );
}
