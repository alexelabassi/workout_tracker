import { useCallback, useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import type { WorkoutDetail } from "../workouts/types";
import { fetchClientSession } from "./api";

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; workout: WorkoutDetail };

/** Read-only view of one of a client's sessions, reusing the workout-detail data shape. */
export function CoachClientSessionPage() {
  const { clientId = "", sessionId = "" } = useParams();
  const location = useLocation();
  const clientName = (location.state as { clientName?: string } | null)?.clientName ?? "Client";
  const [state, setState] = useState<LoadState>({ kind: "loading" });

  const load = useCallback(async () => {
    setState({ kind: "loading" });
    try {
      setState({ kind: "ready", workout: await fetchClientSession(clientId, sessionId) });
    } catch (err) {
      setState({ kind: "error", message: err instanceof ApiError ? err.message : "Could not load the session." });
    }
  }, [clientId, sessionId]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>{state.kind === "ready" ? state.workout.templateName ?? "Workout" : "Session"}</h1>
          <p className="muted">
            <Link to={`/coach/clients/${clientId}`} state={{ clientName }}>← Back to {clientName}</Link>
          </p>
        </div>
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
                <strong>Couldn’t load session</strong>
                <p className="muted">{state.message}</p>
              </div>
            </div>
          </section>
        )}

        {state.kind === "ready" && (
          <>
            <section className="card">
              <p className="muted">
                {[
                  new Date(state.workout.startedAt).toLocaleString(),
                  state.workout.templateDayName,
                  state.workout.gymName,
                  state.workout.status,
                ]
                  .filter(Boolean)
                  .join(" · ")}
              </p>
              {state.workout.notes && <p>{state.workout.notes}</p>}
            </section>

            {state.workout.exercises.map((exercise) => (
              <section className="card" key={exercise.id}>
                <header className="card__header">
                  <h2>
                    {exercise.position}. {exercise.exerciseName}
                    <span className="badge"> {exercise.exerciseType}</span>
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
                          {set.note && <p className="muted exercise-row__muscles">Note: {set.note}</p>}
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            ))}
          </>
        )}
      </main>
    </div>
  );
}
