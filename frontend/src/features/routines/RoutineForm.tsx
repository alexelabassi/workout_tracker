import { useEffect, useState, type FormEvent } from "react";
import { ApiError } from "../../shared/api/client";
import { createRoutine, updateRoutine } from "./api";
import {
  ROUTINE_TYPES,
  ROUTINE_TYPE_LABELS,
  type Routine,
  type RoutinePayload,
  type RoutineType,
} from "./types";

interface RoutineFormProps {
  editing: Routine | null;
  onSaved: (routine: Routine, isNew: boolean) => void;
  onCancel: () => void;
}

export function RoutineForm({ editing, onSaved, onCancel }: RoutineFormProps) {
  const [name, setName] = useState("");
  const [routineType, setRoutineType] = useState<RoutineType>("START");
  const [content, setContent] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setError(null);
    if (editing) {
      setName(editing.name);
      setRoutineType(editing.routineType);
      setContent(editing.content);
    } else {
      setName("");
      setRoutineType("START");
      setContent("");
    }
  }, [editing]);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    const payload: RoutinePayload = {
      name: name.trim(),
      routineType,
      content: content.trim(),
    };
    try {
      const saved = editing
        ? await updateRoutine(editing.id, payload)
        : await createRoutine(payload);
      onSaved(saved, editing === null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save the routine.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="card">
      <header className="card__header">
        <h2>{editing ? "Edit routine" : "New routine"}</h2>
      </header>

      <form className="exercise-form" onSubmit={onSubmit} noValidate>
        {error && <p className="form-error">{error}</p>}

        <label className="field">
          <span>Name</span>
          <input value={name} onChange={(event) => setName(event.target.value)} required maxLength={160} />
        </label>

        <label className="field">
          <span>Type</span>
          <select value={routineType} onChange={(event) => setRoutineType(event.target.value as RoutineType)}>
            {ROUTINE_TYPES.map((type) => (
              <option key={type} value={type}>
                {ROUTINE_TYPE_LABELS[type]} ({type})
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>Content</span>
          <textarea
            rows={5}
            value={content}
            onChange={(event) => setContent(event.target.value)}
            required
            maxLength={5000}
            placeholder="e.g. 5 min bike, hip openers, banded shoulder dislocates"
          />
        </label>

        <div className="app__actions">
          <button type="submit" className="button" disabled={submitting}>
            {submitting ? "Saving…" : editing ? "Save changes" : "Create routine"}
          </button>
          <button type="button" className="button button--ghost" onClick={onCancel} disabled={submitting}>
            Cancel
          </button>
        </div>
      </form>
    </section>
  );
}
