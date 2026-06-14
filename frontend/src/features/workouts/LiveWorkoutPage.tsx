import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { useToast } from "../../shared/ui/ToastProvider";
import { fetchVisibleExercises } from "../exercises/api";
import type { Exercise } from "../exercises/types";
import { fetchEquipment } from "../gyms/api";
import type { Equipment } from "../gyms/types";
import { SessionNotesEditor } from "./SessionNotesEditor";
import { SetLogForm } from "./SetLogForm";
import {
  addExtraExercise,
  cancelWorkout,
  deleteSet,
  fetchActiveWorkout,
  finishWorkout,
} from "./api";
import type { SessionRoutineView, WorkoutDetail } from "./types";

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "none" }
  | { kind: "ready"; workout: WorkoutDetail };

export function LiveWorkoutPage() {
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [state, setState] = useState<LoadState>({ kind: "loading" });
  const [equipment, setEquipment] = useState<Equipment[]>([]);
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [openSetForm, setOpenSetForm] = useState<string | null>(null);
  const [extraExerciseId, setExtraExerciseId] = useState("");

  const load = useCallback(async () => {
    setState({ kind: "loading" });
    try {
      const workout = await fetchActiveWorkout();
      if (!workout) {
        setState({ kind: "none" });
        return;
      }
      setState({ kind: "ready", workout });
      if (workout.gymId) {
        void fetchEquipment(workout.gymId).then(setEquipment).catch(() => setEquipment([]));
      }
    } catch (err) {
      setState({ kind: "error", message: err instanceof ApiError ? err.message : "Could not load your workout." });
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    void fetchVisibleExercises().then(setExercises).catch(() => setExercises([]));
  }, []);

  const reload = (message?: string) => {
    setOpenSetForm(null);
    if (message) {
      showToast(message);
    }
    void load();
  };

  const onDeleteSet = async (setId: string) => {
    try {
      await deleteSet(setId);
      reload("Set deleted.");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not delete the set.", "error");
    }
  };

  const onAddExtra = async (sessionId: string) => {
    if (!extraExerciseId) {
      return;
    }
    try {
      await addExtraExercise(sessionId, extraExerciseId);
      setExtraExerciseId("");
      reload("Exercise added.");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not add the exercise.", "error");
    }
  };

  const onFinish = async (sessionId: string) => {
    try {
      await finishWorkout(sessionId);
      showToast("Workout finished.");
      navigate(`/workouts/${sessionId}`);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not finish the workout.", "error");
    }
  };

  const onCancel = async (sessionId: string) => {
    if (!window.confirm("Cancel this workout? Logged sets are kept but the session is marked cancelled.")) {
      return;
    }
    try {
      await cancelWorkout(sessionId);
      showToast("Workout cancelled.");
      navigate(`/workouts/${sessionId}`);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not cancel the workout.", "error");
    }
  };

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
        <main className="app__main">
          <div className="status status--down">
            <span className="dot" />
            <div>
              <strong>Couldn’t load workout</strong>
              <p className="muted">{state.message}</p>
              <button type="button" className="button button--ghost" onClick={() => void load()}>
                Retry
              </button>
            </div>
          </div>
        </main>
      </div>
    );
  }

  if (state.kind === "none") {
    return (
      <div className="app">
        <header className="app__header">
          <div>
            <h1>Live workout</h1>
            <p className="muted">
              <Link to="/">← Back to dashboard</Link>
            </p>
          </div>
        </header>
        <main className="app__main">
          <section className="card">
            <p className="muted">No workout in progress.</p>
            <Link to="/workouts/start" className="button button--block">
              Start a workout
            </Link>
          </section>
        </main>
      </div>
    );
  }

  const workout = state.workout;
  const startRoutines = workout.routines.filter((routine) => routine.routineType === "START");
  const endRoutines = workout.routines.filter((routine) => routine.routineType === "END");

  const routineCard = (title: string, routines: SessionRoutineView[]) =>
    routines.length > 0 && (
      <section className="card">
        <header className="card__header">
          <h2>{title}</h2>
        </header>
        <ul className="exercise-list">
          {routines.map((routine) => (
            <li key={routine.id} className="exercise-row">
              <div className="exercise-row__main">
                <div className="exercise-row__title">
                  <strong>{routine.routineName}</strong>
                </div>
                <p className="muted exercise-row__muscles">{routine.routineContent}</p>
              </div>
            </li>
          ))}
        </ul>
      </section>
    );

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>{workout.templateName ?? "Workout"}</h1>
          <p className="muted">
            {workout.templateDayName ? `${workout.templateDayName} · ` : ""}
            {workout.gymName ?? "No gym"}
          </p>
        </div>
        <div className="app__actions">
          <button type="button" className="button" onClick={() => void onFinish(workout.id)}>
            Finish
          </button>
          <button type="button" className="button button--ghost" onClick={() => void onCancel(workout.id)}>
            Cancel
          </button>
        </div>
      </header>

      <main className="app__main">
        {routineCard("Warm-up", startRoutines)}

        {workout.exercises.map((exercise) => (
          <section className="card" key={exercise.id}>
            <header className="card__header">
              <h2>
                {exercise.position}. {exercise.exerciseName}
                <span className="badge"> {exercise.exerciseType}</span>
                {exercise.extraExercise ? <span className="badge badge--muted"> Extra</span> : null}
              </h2>
            </header>
            {(exercise.plannedSets != null || exercise.plannedReps) && (
              <p className="muted">
                Target:{" "}
                {[
                  exercise.plannedSets != null && exercise.plannedReps
                    ? `${exercise.plannedSets} × ${exercise.plannedReps}`
                    : exercise.plannedSets != null
                      ? `${exercise.plannedSets} sets`
                      : exercise.plannedReps,
                  exercise.restSeconds != null ? `${exercise.restSeconds}s rest` : null,
                ]
                  .filter(Boolean)
                  .join(" · ")}
              </p>
            )}

            {exercise.sets.length > 0 && (
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
                    <div className="app__actions">
                      <button type="button" className="button button--ghost" onClick={() => void onDeleteSet(set.id)}>
                        Delete
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}

            {openSetForm === exercise.id ? (
              <SetLogForm
                sessionExerciseId={exercise.id}
                equipment={equipment}
                onLogged={() => reload("Set logged.")}
                onError={(message) => showToast(message, "error")}
              />
            ) : (
              <button type="button" className="button button--ghost" onClick={() => setOpenSetForm(exercise.id)}>
                Add set
              </button>
            )}
          </section>
        ))}

        <SessionNotesEditor sessionId={workout.id} initialNotes={workout.notes} onSaved={() => void load()} />

        <section className="card">
          <header className="card__header">
            <h2>Add an extra exercise</h2>
          </header>
          <div className="muscle-add">
            <select value={extraExerciseId} onChange={(event) => setExtraExerciseId(event.target.value)}>
              <option value="">Select an exercise…</option>
              {exercises.map((exercise) => (
                <option key={exercise.id} value={exercise.id}>
                  {exercise.name}
                  {exercise.visibility === "CUSTOM" ? " (custom)" : ""}
                </option>
              ))}
            </select>
            <button type="button" className="button button--ghost" disabled={!extraExerciseId}
              onClick={() => void onAddExtra(workout.id)}>
              Add
            </button>
          </div>
        </section>

        {routineCard("Cool-down", endRoutines)}
      </main>
    </div>
  );
}
