import { useState, type FormEvent } from "react";
import { ApiError } from "../../shared/api/client";
import type { Routine } from "../routines/types";
import { attachRoutine } from "./api";

interface DayRoutineFormProps {
  dayId: string;
  routines: Routine[];
  onAttached: () => void;
  onCancel: () => void;
}

export function DayRoutineForm({ dayId, routines, onAttached, onCancel }: DayRoutineFormProps) {
  const [routineId, setRoutineId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!routineId) {
      setError("Choose a routine.");
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      await attachRoutine(dayId, routineId);
      onAttached();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not attach the routine.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="exercise-form builder-form" onSubmit={onSubmit} noValidate>
      {error && <p className="form-error">{error}</p>}

      <label className="field">
        <span>Routine</span>
        <select value={routineId} onChange={(event) => setRoutineId(event.target.value)} required>
          <option value="">Select a routine…</option>
          {routines.map((routine) => (
            <option key={routine.id} value={routine.id}>
              {routine.name} ({routine.routineType})
            </option>
          ))}
        </select>
      </label>

      <div className="app__actions">
        <button type="submit" className="button" disabled={submitting}>
          {submitting ? "Attaching…" : "Attach routine"}
        </button>
        <button type="button" className="button button--ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
      </div>
    </form>
  );
}
