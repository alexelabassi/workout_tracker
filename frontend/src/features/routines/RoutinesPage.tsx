import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { useToast } from "../../shared/ui/ToastProvider";
import { RoutineForm } from "./RoutineForm";
import { deleteRoutine, fetchRoutines } from "./api";
import { ROUTINE_TYPE_LABELS, type Routine } from "./types";

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; routines: Routine[] };

export function RoutinesPage() {
  const { showToast } = useToast();
  const [state, setState] = useState<LoadState>({ kind: "loading" });
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Routine | null>(null);

  const load = useCallback(async () => {
    setState({ kind: "loading" });
    try {
      const routines = await fetchRoutines();
      setState({ kind: "ready", routines });
    } catch (err) {
      setState({ kind: "error", message: err instanceof ApiError ? err.message : "Could not load routines." });
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };

  const openEdit = (routine: Routine) => {
    setEditing(routine);
    setFormOpen(true);
  };

  const onSaved = (_routine: Routine, isNew: boolean) => {
    setFormOpen(false);
    setEditing(null);
    showToast(isNew ? "Routine created." : "Routine updated.");
    void load();
  };

  const onDelete = async (routine: Routine) => {
    if (!window.confirm(`Delete "${routine.name}"?`)) {
      return;
    }
    try {
      await deleteRoutine(routine.id);
      showToast("Routine deleted.");
      void load();
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not delete the routine.", "error");
    }
  };

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>Routines</h1>
          <p className="muted">
            <Link to="/">← Back to dashboard</Link>
          </p>
        </div>
        <button type="button" className="button" onClick={openCreate}>
          New routine
        </button>
      </header>

      <main className="app__main">
        {formOpen && (
          <RoutineForm editing={editing} onSaved={onSaved} onCancel={() => setFormOpen(false)} />
        )}

        <section className="card">
          <header className="card__header">
            <h2>Your routines</h2>
          </header>

          {state.kind === "loading" && <p className="muted">Loading…</p>}

          {state.kind === "error" && (
            <div className="status status--down">
              <span className="dot" />
              <div>
                <strong>Couldn’t load routines</strong>
                <p className="muted">{state.message}</p>
                <button type="button" className="button button--ghost" onClick={() => void load()}>
                  Retry
                </button>
              </div>
            </div>
          )}

          {state.kind === "ready" && state.routines.length === 0 && (
            <p className="muted">No routines yet. Create a warm-up or cool-down to reuse across workouts.</p>
          )}

          {state.kind === "ready" && state.routines.length > 0 && (
            <ul className="exercise-list">
              {state.routines.map((routine) => (
                <li key={routine.id} className="exercise-row">
                  <div className="exercise-row__main">
                    <div className="exercise-row__title">
                      <strong>{routine.name}</strong>
                      <span className="badge">{ROUTINE_TYPE_LABELS[routine.routineType]}</span>
                    </div>
                    <p className="muted exercise-row__muscles">{routine.content}</p>
                  </div>
                  <div className="app__actions">
                    <button type="button" className="button button--ghost" onClick={() => openEdit(routine)}>
                      Edit
                    </button>
                    <button type="button" className="button button--ghost" onClick={() => void onDelete(routine)}>
                      Delete
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
