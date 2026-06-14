import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { useToast } from "../../shared/ui/ToastProvider";
import { fetchVisibleExercises } from "../exercises/api";
import type { Exercise } from "../exercises/types";
import { fetchRoutines } from "../routines/api";
import type { Routine } from "../routines/types";
import { TemplateAnalysisPanel } from "../analyzer/TemplateAnalysisPanel";
import { DayExerciseForm } from "./DayExerciseForm";
import { DayForm } from "./DayForm";
import { DayRoutineForm } from "./DayRoutineForm";
import { deleteDay, deleteExercise, fetchTemplate, publishTemplate, removeRoutine, unpublishTemplate } from "./api";
import type { TemplateDay, TemplateDayExercise, TemplateDetail } from "./types";

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; template: TemplateDetail };

type ExerciseFormState = { dayId: string; editing: TemplateDayExercise | null };

export function TemplateBuilderPage() {
  const { templateId = "" } = useParams();
  const { showToast } = useToast();
  const [state, setState] = useState<LoadState>({ kind: "loading" });
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [routines, setRoutines] = useState<Routine[]>([]);

  const [dayFormOpen, setDayFormOpen] = useState(false);
  const [editingDay, setEditingDay] = useState<TemplateDay | null>(null);
  const [exerciseForm, setExerciseForm] = useState<ExerciseFormState | null>(null);
  const [routineFormDayId, setRoutineFormDayId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setState({ kind: "loading" });
    try {
      const template = await fetchTemplate(templateId);
      setState({ kind: "ready", template });
    } catch (err) {
      setState({ kind: "error", message: err instanceof ApiError ? err.message : "Could not load this template." });
    }
  }, [templateId]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    void fetchVisibleExercises().then(setExercises).catch(() => setExercises([]));
    void fetchRoutines().then(setRoutines).catch(() => setRoutines([]));
  }, []);

  const closeForms = () => {
    setDayFormOpen(false);
    setEditingDay(null);
    setExerciseForm(null);
    setRoutineFormDayId(null);
  };

  const reload = (message: string) => {
    closeForms();
    showToast(message);
    void load();
  };

  const onDeleteDay = async (day: TemplateDay) => {
    if (!window.confirm(`Delete day "${day.name}" and everything in it?`)) {
      return;
    }
    try {
      await deleteDay(day.id);
      reload("Day deleted.");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not delete the day.", "error");
    }
  };

  const onTogglePublish = async (id: string, currentlyPublic: boolean) => {
    try {
      if (currentlyPublic) {
        await unpublishTemplate(id);
        reload("Template unpublished.");
      } else {
        await publishTemplate(id);
        reload("Template published to the marketplace.");
      }
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not change publish state.", "error");
    }
  };

  const onDeleteExercise = async (exercise: TemplateDayExercise) => {
    try {
      await deleteExercise(exercise.id);
      reload("Exercise removed.");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not remove the exercise.", "error");
    }
  };

  const onRemoveRoutine = async (dayRoutineId: string) => {
    try {
      await removeRoutine(dayRoutineId);
      reload("Routine removed.");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not remove the routine.", "error");
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
        <header className="app__header">
          <div>
            <h1>Template</h1>
            <p className="muted">
              <Link to="/templates">← Back to templates</Link>
            </p>
          </div>
        </header>
        <main className="app__main">
          <div className="status status--down">
            <span className="dot" />
            <div>
              <strong>Couldn’t load template</strong>
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

  const template = state.template;

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>{template.name}</h1>
          <p className="muted">
            <Link to="/templates">← Back to templates</Link>
            {template.visibility === "PUBLIC" ? " · Published" : ""}
            {template.muscleGroups.length > 0 ? ` · ${template.muscleGroups.join(", ")}` : ""}
          </p>
        </div>
        <div className="app__actions">
          <button
            type="button"
            className={template.visibility === "PUBLIC" ? "button button--ghost" : "button"}
            onClick={() => void onTogglePublish(template.id, template.visibility === "PUBLIC")}
          >
            {template.visibility === "PUBLIC" ? "Unpublish" : "Publish"}
          </button>
          <button
            type="button"
            className="button button--ghost"
            onClick={() => {
              setEditingDay(null);
              setDayFormOpen(true);
              setExerciseForm(null);
              setRoutineFormDayId(null);
            }}
          >
            Add day
          </button>
        </div>
      </header>

      <main className="app__main">
        {dayFormOpen && (
          <section className="card">
            <header className="card__header">
              <h2>{editingDay ? "Edit day" : "New day"}</h2>
            </header>
            <DayForm
              templateId={template.id}
              editing={editingDay}
              onSaved={() => reload(editingDay ? "Day updated." : "Day added.")}
              onCancel={closeForms}
            />
          </section>
        )}

        {template.days.length === 0 && !dayFormOpen && (
          <section className="card">
            <p className="muted">No days yet. Add a day to start building this program.</p>
          </section>
        )}

        {template.days.map((day) => (
          <section className="card" key={day.id}>
            <header className="card__header">
              <h2>
                Day {day.dayNumber}: {day.name}
                {day.focus ? <span className="badge badge--muted"> {day.focus}</span> : null}
              </h2>
              <div className="app__actions">
                <button
                  type="button"
                  className="button button--ghost"
                  onClick={() => {
                    setEditingDay(day);
                    setDayFormOpen(true);
                    setExerciseForm(null);
                    setRoutineFormDayId(null);
                  }}
                >
                  Edit day
                </button>
                <button type="button" className="button button--ghost" onClick={() => void onDeleteDay(day)}>
                  Delete day
                </button>
              </div>
            </header>

            <h3 className="builder-subhead">Exercises</h3>
            {day.exercises.length === 0 ? (
              <p className="muted">No exercises yet.</p>
            ) : (
              <ul className="exercise-list">
                {day.exercises.map((exercise) => (
                  <li key={exercise.id} className="exercise-row">
                    <div className="exercise-row__main">
                      <div className="exercise-row__title">
                        <strong>
                          {exercise.position}. {exercise.exerciseName}
                        </strong>
                        <span className="badge">{exercise.exerciseType}</span>
                      </div>
                      <p className="muted exercise-row__muscles">
                        {[
                          exercise.plannedSets != null && exercise.plannedReps
                            ? `${exercise.plannedSets} × ${exercise.plannedReps}`
                            : exercise.plannedSets != null
                              ? `${exercise.plannedSets} sets`
                              : null,
                          exercise.plannedWeight != null ? `${exercise.plannedWeight} kg` : null,
                          exercise.restSeconds != null ? `${exercise.restSeconds}s rest` : null,
                          exercise.muscleGroups.map((group) => group.code).join(", ") || null,
                        ]
                          .filter(Boolean)
                          .join(" · ")}
                      </p>
                    </div>
                    <div className="app__actions">
                      <button
                        type="button"
                        className="button button--ghost"
                        onClick={() => {
                          setExerciseForm({ dayId: day.id, editing: exercise });
                          setRoutineFormDayId(null);
                          setDayFormOpen(false);
                        }}
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        className="button button--ghost"
                        onClick={() => void onDeleteExercise(exercise)}
                      >
                        Remove
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}

            {exerciseForm?.dayId === day.id ? (
              <DayExerciseForm
                dayId={day.id}
                editing={exerciseForm.editing}
                exercises={exercises}
                onSaved={() => reload(exerciseForm.editing ? "Exercise updated." : "Exercise added.")}
                onCancel={closeForms}
              />
            ) : (
              <button
                type="button"
                className="button button--ghost"
                onClick={() => {
                  setExerciseForm({ dayId: day.id, editing: null });
                  setRoutineFormDayId(null);
                  setDayFormOpen(false);
                }}
              >
                Add exercise
              </button>
            )}

            <h3 className="builder-subhead">Routines</h3>
            {day.routines.length === 0 ? (
              <p className="muted">No routines attached.</p>
            ) : (
              <ul className="exercise-list">
                {day.routines.map((routine) => (
                  <li key={routine.id} className="exercise-row">
                    <div className="exercise-row__main">
                      <div className="exercise-row__title">
                        <strong>{routine.routineName}</strong>
                        <span className="badge">{routine.routineType}</span>
                      </div>
                      <p className="muted exercise-row__muscles">{routine.routineContent}</p>
                    </div>
                    <div className="app__actions">
                      <button
                        type="button"
                        className="button button--ghost"
                        onClick={() => void onRemoveRoutine(routine.id)}
                      >
                        Remove
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}

            {routineFormDayId === day.id ? (
              <DayRoutineForm
                dayId={day.id}
                routines={routines}
                onAttached={() => reload("Routine attached.")}
                onCancel={closeForms}
              />
            ) : (
              <button
                type="button"
                className="button button--ghost"
                onClick={() => {
                  setRoutineFormDayId(day.id);
                  setExerciseForm(null);
                  setDayFormOpen(false);
                }}
              >
                Attach routine
              </button>
            )}
          </section>
        ))}

        <TemplateAnalysisPanel templateId={template.id} />
      </main>
    </div>
  );
}
