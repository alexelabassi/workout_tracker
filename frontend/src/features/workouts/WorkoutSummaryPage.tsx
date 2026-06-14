import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { fetchWorkout } from "./api";
import type { WorkoutDetail } from "./types";

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; workout: WorkoutDetail };

const STATUS_LABEL: Record<string, string> = {
  IN_PROGRESS: "In progress",
  FINISHED: "Finished",
  CANCELLED: "Cancelled",
};

export function WorkoutSummaryPage() {
  const { sessionId = "" } = useParams();
  const [state, setState] = useState<LoadState>({ kind: "loading" });

  const load = useCallback(async () => {
    setState({ kind: "loading" });
    try {
      const workout = await fetchWorkout(sessionId);
      setState({ kind: "ready", workout });
    } catch (err) {
      setState({ kind: "error", message: err instanceof ApiError ? err.message : "Could not load this workout." });
    }
  }, [sessionId]);

  useEffect(() => {
    void load();
  }, [load]);

  if (state.kind === "loading") {
    return (
      <div className="app">
        <main className="app__main">
          <p className="muted">Loading…</p>
        </main>
      </div>
    );
  }

  if (state.kind === "error") {
    return (
      <div className="app">
        <header className="app__header">
          <div>
            <h1>Workout</h1>
            <p className="muted">
              <Link to="/">← Back to dashboard</Link>
            </p>
          </div>
        </header>
        <main className="app__main">
          <div className="status status--down">
            <span className="dot" />
            <div>
              <strong>Couldn’t load workout</strong>
              <p className="muted">{state.message}</p>
            </div>
          </div>
        </main>
      </div>
    );
  }

  const workout = state.workout;

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>{workout.templateName ?? "Workout"}</h1>
          <p className="muted">
            <Link to="/">← Back to dashboard</Link>
            {workout.templateDayName ? ` · ${workout.templateDayName}` : ""}
            {workout.gymName ? ` · ${workout.gymName}` : ""}
          </p>
        </div>
        <span className="badge">{STATUS_LABEL[workout.status] ?? workout.status}</span>
      </header>

      <main className="app__main">
        {workout.exercises.map((exercise) => (
          <section className="card" key={exercise.id}>
            <header className="card__header">
              <h2>
                {exercise.position}. {exercise.exerciseName}
                <span className="badge"> {exercise.exerciseType}</span>
                {exercise.extraExercise ? <span className="badge badge--muted"> Extra</span> : null}
              </h2>
            </header>
            {exercise.sets.length === 0 ? (
              <p className="muted">No sets logged.</p>
            ) : (
              <ul className="exercise-list">
                {exercise.sets.map((set) => (
                  <li key={set.id} className="exercise-row">
                    <div className="exercise-row__main">
                      <div className="exercise-row__title">
                        <strong>Set {set.setNumber}</strong>
                        <span className="badge badge--muted">{set.setType}</span>
                      </div>
                      <p className="muted exercise-row__muscles">
                        {[
                          set.weight != null ? `${set.weight} kg` : null,
                          set.reps != null ? `${set.reps} reps` : null,
                          set.rpe != null ? `RPE ${set.rpe}` : null,
                          set.equipmentName,
                        ]
                          .filter(Boolean)
                          .join(" · ") || "—"}
                      </p>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>
        ))}
      </main>
    </div>
  );
}
