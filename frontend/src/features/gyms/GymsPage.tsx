import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { useToast } from "../../shared/ui/ToastProvider";
import { GymForm } from "./GymForm";
import { deleteGym, fetchGyms } from "./api";
import type { Gym } from "./types";

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; gyms: Gym[] };

export function GymsPage() {
  const { showToast } = useToast();
  const [state, setState] = useState<LoadState>({ kind: "loading" });
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Gym | null>(null);

  const load = useCallback(async () => {
    setState({ kind: "loading" });
    try {
      const gyms = await fetchGyms();
      setState({ kind: "ready", gyms });
    } catch (err) {
      setState({ kind: "error", message: err instanceof ApiError ? err.message : "Could not load gyms." });
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };

  const openEdit = (gym: Gym) => {
    setEditing(gym);
    setFormOpen(true);
  };

  const onSaved = (_gym: Gym, isNew: boolean) => {
    setFormOpen(false);
    setEditing(null);
    showToast(isNew ? "Gym created." : "Gym updated.");
    void load();
  };

  const onDelete = async (gym: Gym) => {
    if (!window.confirm(`Delete "${gym.name}" and its equipment?`)) {
      return;
    }
    try {
      await deleteGym(gym.id);
      showToast("Gym deleted.");
      void load();
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not delete the gym.", "error");
    }
  };

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>Gyms</h1>
          <p className="muted">
            <Link to="/">← Back to dashboard</Link>
          </p>
        </div>
        <button type="button" className="button" onClick={openCreate}>
          New gym
        </button>
      </header>

      <main className="app__main">
        {formOpen && <GymForm editing={editing} onSaved={onSaved} onCancel={() => setFormOpen(false)} />}

        <section className="card">
          <header className="card__header">
            <h2>Your gyms</h2>
          </header>

          {state.kind === "loading" && <p className="muted">Loading…</p>}

          {state.kind === "error" && (
            <div className="status status--down">
              <span className="dot" />
              <div>
                <strong>Couldn’t load gyms</strong>
                <p className="muted">{state.message}</p>
                <button type="button" className="button button--ghost" onClick={() => void load()}>
                  Retry
                </button>
              </div>
            </div>
          )}

          {state.kind === "ready" && state.gyms.length === 0 && (
            <p className="muted">No gyms yet. Add the places you train to track their equipment.</p>
          )}

          {state.kind === "ready" && state.gyms.length > 0 && (
            <ul className="exercise-list">
              {state.gyms.map((gym) => (
                <li key={gym.id} className="exercise-row">
                  <div className="exercise-row__main">
                    <div className="exercise-row__title">
                      <strong>{gym.name}</strong>
                    </div>
                    {gym.location && <p className="muted exercise-row__muscles">{gym.location}</p>}
                  </div>
                  <div className="app__actions">
                    <Link to={`/gyms/${gym.id}`} className="button button--ghost">
                      Equipment
                    </Link>
                    <button type="button" className="button button--ghost" onClick={() => openEdit(gym)}>
                      Edit
                    </button>
                    <button type="button" className="button button--ghost" onClick={() => void onDelete(gym)}>
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
