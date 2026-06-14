import { useEffect, useState, type FormEvent } from "react";
import { ApiError } from "../../shared/api/client";
import type { Exercise } from "../exercises/types";
import { addExercise, updateExercise } from "./api";
import type { ExercisePayload, TemplateDayExercise } from "./types";

interface DayExerciseFormProps {
  dayId: string;
  editing: TemplateDayExercise | null;
  exercises: Exercise[];
  onSaved: () => void;
  onCancel: () => void;
}

function toNumberOrNull(value: string): number | null {
  if (value.trim() === "") {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

export function DayExerciseForm({ dayId, editing, exercises, onSaved, onCancel }: DayExerciseFormProps) {
  const [exerciseId, setExerciseId] = useState("");
  const [sets, setSets] = useState("");
  const [reps, setReps] = useState("");
  const [weight, setWeight] = useState("");
  const [rest, setRest] = useState("");
  const [note, setNote] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setError(null);
    setExerciseId(editing?.exerciseId ?? "");
    setSets(editing?.plannedSets != null ? String(editing.plannedSets) : "");
    setReps(editing?.plannedReps ?? "");
    setWeight(editing?.plannedWeight != null ? String(editing.plannedWeight) : "");
    setRest(editing?.restSeconds != null ? String(editing.restSeconds) : "");
    setNote(editing?.note ?? "");
  }, [editing]);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!exerciseId) {
      setError("Choose an exercise.");
      return;
    }
    setError(null);
    setSubmitting(true);
    const payload: ExercisePayload = {
      exerciseId,
      plannedSets: toNumberOrNull(sets),
      plannedReps: reps.trim() ? reps.trim() : null,
      plannedWeight: toNumberOrNull(weight),
      restSeconds: toNumberOrNull(rest),
      note: note.trim() ? note.trim() : null,
    };
    try {
      if (editing) {
        await updateExercise(editing.id, payload);
      } else {
        await addExercise(dayId, payload);
      }
      onSaved();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save the exercise.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="exercise-form builder-form" onSubmit={onSubmit} noValidate>
      {error && <p className="form-error">{error}</p>}

      <label className="field">
        <span>Exercise</span>
        <select value={exerciseId} onChange={(event) => setExerciseId(event.target.value)} required>
          <option value="">Select an exercise…</option>
          {exercises.map((exercise) => (
            <option key={exercise.id} value={exercise.id}>
              {exercise.name}
              {exercise.visibility === "CUSTOM" ? " (custom)" : ""}
            </option>
          ))}
        </select>
      </label>

      <div className="muscle-add">
        <label className="field">
          <span>Sets</span>
          <input type="number" min={1} value={sets} onChange={(event) => setSets(event.target.value)} />
        </label>
        <label className="field">
          <span>Reps</span>
          <input value={reps} onChange={(event) => setReps(event.target.value)} maxLength={50} placeholder="e.g. 8-12" />
        </label>
        <label className="field">
          <span>Weight</span>
          <input type="number" min={0} step="0.5" value={weight} onChange={(event) => setWeight(event.target.value)} />
        </label>
        <label className="field">
          <span>Rest (s)</span>
          <input type="number" min={0} value={rest} onChange={(event) => setRest(event.target.value)} />
        </label>
      </div>

      <label className="field">
        <span>Note</span>
        <textarea rows={2} value={note} onChange={(event) => setNote(event.target.value)} />
      </label>

      <div className="app__actions">
        <button type="submit" className="button" disabled={submitting}>
          {submitting ? "Saving…" : editing ? "Save exercise" : "Add exercise"}
        </button>
        <button type="button" className="button button--ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
      </div>
    </form>
  );
}
