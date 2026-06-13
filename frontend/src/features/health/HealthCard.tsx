import { useCallback, useEffect, useState } from "react";
import { apiGet, ApiError } from "../../shared/api/client";

interface HealthResponse {
  status: string;
  database: string;
  timestamp: string;
}

type LoadState =
  | { kind: "loading" }
  | { kind: "success"; data: HealthResponse }
  | { kind: "error"; message: string };

export function HealthCard() {
  const [state, setState] = useState<LoadState>({ kind: "loading" });

  const load = useCallback(async () => {
    setState({ kind: "loading" });
    try {
      const data = await apiGet<HealthResponse>("/health");
      setState({ kind: "success", data });
    } catch (error) {
      const message =
        error instanceof ApiError ? error.message : "Unexpected error";
      setState({ kind: "error", message });
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <section className="card">
      <header className="card__header">
        <h2>Backend health</h2>
        <button type="button" className="button" onClick={() => void load()}>
          Refresh
        </button>
      </header>

      {state.kind === "loading" && <p className="muted">Checking backend…</p>}

      {state.kind === "error" && (
        <div className="status status--down">
          <span className="dot" />
          <div>
            <strong>Unreachable</strong>
            <p className="muted">{state.message}</p>
          </div>
        </div>
      )}

      {state.kind === "success" && (
        <div
          className={
            state.data.status === "UP"
              ? "status status--up"
              : "status status--down"
          }
        >
          <span className="dot" />
          <div>
            <strong>API {state.data.status}</strong>
            <p className="muted">Database: {state.data.database}</p>
            <p className="muted">
              Checked at {new Date(state.data.timestamp).toLocaleTimeString()}
            </p>
          </div>
        </div>
      )}
    </section>
  );
}
