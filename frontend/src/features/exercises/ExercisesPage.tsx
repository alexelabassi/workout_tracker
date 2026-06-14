import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { ExerciseForm } from "./ExerciseForm";
import {
  deleteCustomExercise,
  fetchCustomExercises,
  fetchMuscleGroups,
  fetchOfficialExercises,
  fetchVisibleExercises,
} from "./api";
import type { Exercise, MuscleGroup } from "./types";

type Tab = "all" | "official" | "custom";

const TABS: { key: Tab; label: string }[] = [
  { key: "all", label: "All" },
  { key: "official", label: "Official" },
  { key: "custom", label: "My custom" },
];

const LOADERS: Record<Tab, () => Promise<Exercise[]>> = {
  all: fetchVisibleExercises,
  official: fetchOfficialExercises,
  custom: fetchCustomExercises,
};

export function ExercisesPage() {
  const [tab, setTab] = useState<Tab>("all");
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [muscleGroups, setMuscleGroups] = useState<MuscleGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Exercise | null>(null);

  const load = useCallback(async (which: Tab) => {
    setLoading(true);
    setError(null);
    try {
      const data = await LOADERS[which]();
      setExercises(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not load exercises.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load(tab);
  }, [load, tab]);

  useEffect(() => {
    void fetchMuscleGroups()
      .then(setMuscleGroups)
      .catch(() => setMuscleGroups([]));
  }, []);

  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };

  const openEdit = (exercise: Exercise) => {
    setEditing(exercise);
    setFormOpen(true);
  };

  const onSaved = () => {
    setFormOpen(false);
    setEditing(null);
    void load(tab);
  };

  const onDelete = async (exercise: Exercise) => {
    if (!window.confirm(`Delete "${exercise.name}"?`)) {
      return;
    }
    try {
      await deleteCustomExercise(exercise.id);
      void load(tab);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not delete the exercise.");
    }
  };

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>Exercises</h1>
          <p className="muted">
            <Link to="/">← Back to dashboard</Link>
          </p>
        </div>
        <button type="button" className="button" onClick={openCreate}>
          New exercise
        </button>
      </header>

      <main className="app__main">
        {formOpen && (
          <ExerciseForm
            muscleGroups={muscleGroups}
            editing={editing}
            onSaved={onSaved}
            onCancel={() => setFormOpen(false)}
          />
        )}

        <section className="card">
          <header className="card__header">
            <div className="tabs">
              {TABS.map((entry) => (
                <button
                  key={entry.key}
                  type="button"
                  className={entry.key === tab ? "tab tab--active" : "tab"}
                  onClick={() => setTab(entry.key)}
                >
                  {entry.label}
                </button>
              ))}
            </div>
          </header>

          {error && <p className="form-error">{error}</p>}
          {loading && <p className="muted">Loading…</p>}

          {!loading && exercises.length === 0 && (
            <p className="muted">No exercises here yet.</p>
          )}

          {!loading && exercises.length > 0 && (
            <ul className="exercise-list">
              {exercises.map((exercise) => (
                <li key={exercise.id} className="exercise-row">
                  <div className="exercise-row__main">
                    <div className="exercise-row__title">
                      <strong>{exercise.name}</strong>
                      <span className="badge">{exercise.exerciseType}</span>
                      {exercise.visibility === "CUSTOM" && (
                        <span className="badge badge--muted">Custom</span>
                      )}
                    </div>
                    {exercise.muscleGroups.length > 0 && (
                      <p className="muted exercise-row__muscles">
                        {exercise.muscleGroups
                          .map((group) => `${group.displayName} (${group.role.toLowerCase()})`)
                          .join(", ")}
                      </p>
                    )}
                  </div>
                  {exercise.visibility === "CUSTOM" && (
                    <div className="app__actions">
                      <button type="button" className="button button--ghost" onClick={() => openEdit(exercise)}>
                        Edit
                      </button>
                      <button type="button" className="button button--ghost" onClick={() => void onDelete(exercise)}>
                        Delete
                      </button>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </div>
  );
}
