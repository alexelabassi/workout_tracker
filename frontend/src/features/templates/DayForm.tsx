import { useEffect, useState, type FormEvent } from "react";
import { ApiError } from "../../shared/api/client";
import { createDay, updateDay } from "./api";
import { DAY_FOCUSES, type DayFocus, type DayPayload, type TemplateDay } from "./types";

interface DayFormProps {
  templateId: string;
  editing: TemplateDay | null;
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

export function DayForm({ templateId, editing, onSaved, onCancel }: DayFormProps) {
  const [dayNumber, setDayNumber] = useState("1");
  const [name, setName] = useState("");
  const [focus, setFocus] = useState<DayFocus | "">("");
  const [duration, setDuration] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setError(null);
    setDayNumber(editing ? String(editing.dayNumber) : "1");
    setName(editing?.name ?? "");
    setFocus(editing?.focus ?? "");
    setDuration(editing?.estimatedDurationMinutes != null ? String(editing.estimatedDurationMinutes) : "");
    setNotes(editing?.notes ?? "");
  }, [editing]);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    const payload: DayPayload = {
      dayNumber: Number(dayNumber),
      name: name.trim(),
      focus: focus === "" ? null : focus,
      estimatedDurationMinutes: toNumberOrNull(duration),
      notes: notes.trim() ? notes.trim() : null,
    };
    try {
      if (editing) {
        await updateDay(editing.id, payload);
      } else {
        await createDay(templateId, payload);
      }
      onSaved();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save the day.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="exercise-form builder-form" onSubmit={onSubmit} noValidate>
      {error && <p className="form-error">{error}</p>}

      <label className="field">
        <span>Day number</span>
        <input type="number" min={1} value={dayNumber} onChange={(event) => setDayNumber(event.target.value)} required />
      </label>

      <label className="field">
        <span>Name</span>
        <input value={name} onChange={(event) => setName(event.target.value)} required maxLength={160} />
      </label>

      <label className="field">
        <span>Focus</span>
        <select value={focus} onChange={(event) => setFocus(event.target.value as DayFocus | "")}>
          <option value="">— none —</option>
          {DAY_FOCUSES.map((value) => (
            <option key={value} value={value}>
              {value}
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        <span>Estimated duration (minutes)</span>
        <input type="number" min={1} value={duration} onChange={(event) => setDuration(event.target.value)} />
      </label>

      <label className="field">
        <span>Notes</span>
        <textarea rows={2} value={notes} onChange={(event) => setNotes(event.target.value)} />
      </label>

      <div className="app__actions">
        <button type="submit" className="button" disabled={submitting}>
          {submitting ? "Saving…" : editing ? "Save day" : "Add day"}
        </button>
        <button type="button" className="button button--ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
      </div>
    </form>
  );
}
