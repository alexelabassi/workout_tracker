import { useEffect, useState, type FormEvent } from "react";
import { ApiError } from "../../shared/api/client";
import { createTemplate, updateTemplate } from "./api";
import {
  DIFFICULTIES,
  SPLIT_TYPES,
  type Difficulty,
  type SplitType,
  type TemplateDetail,
  type TemplatePayload,
  type TemplateSummary,
} from "./types";

interface TemplateFormProps {
  editing: TemplateSummary | TemplateDetail | null;
  onSaved: (template: TemplateDetail, isNew: boolean) => void;
  onCancel: () => void;
}

function toNumberOrNull(value: string): number | null {
  if (value.trim() === "") {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

export function TemplateForm({ editing, onSaved, onCancel }: TemplateFormProps) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [splitType, setSplitType] = useState<SplitType | "">("");
  const [daysPerWeek, setDaysPerWeek] = useState("");
  const [difficulty, setDifficulty] = useState<Difficulty | "">("");
  const [duration, setDuration] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setError(null);
    setName(editing?.name ?? "");
    setDescription(editing?.description ?? "");
    setSplitType(editing?.splitType ?? "");
    setDaysPerWeek(editing?.daysPerWeek != null ? String(editing.daysPerWeek) : "");
    setDifficulty(editing?.difficulty ?? "");
    setDuration(editing?.estimatedDurationMinutes != null ? String(editing.estimatedDurationMinutes) : "");
  }, [editing]);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    const payload: TemplatePayload = {
      name: name.trim(),
      description: description.trim() ? description.trim() : null,
      splitType: splitType === "" ? null : splitType,
      daysPerWeek: toNumberOrNull(daysPerWeek),
      difficulty: difficulty === "" ? null : difficulty,
      estimatedDurationMinutes: toNumberOrNull(duration),
    };
    try {
      const saved = editing
        ? await updateTemplate(editing.id, payload)
        : await createTemplate(payload);
      onSaved(saved, editing === null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save the template.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="card">
      <header className="card__header">
        <h2>{editing ? "Edit template" : "New template"}</h2>
      </header>

      <form className="exercise-form" onSubmit={onSubmit} noValidate>
        {error && <p className="form-error">{error}</p>}

        <label className="field">
          <span>Name</span>
          <input value={name} onChange={(event) => setName(event.target.value)} required maxLength={180} />
        </label>

        <label className="field">
          <span>Description</span>
          <textarea rows={3} value={description} onChange={(event) => setDescription(event.target.value)} />
        </label>

        <label className="field">
          <span>Split type</span>
          <select value={splitType} onChange={(event) => setSplitType(event.target.value as SplitType | "")}>
            <option value="">— none —</option>
            {SPLIT_TYPES.map((type) => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>Days per week</span>
          <input
            type="number"
            min={1}
            max={7}
            value={daysPerWeek}
            onChange={(event) => setDaysPerWeek(event.target.value)}
          />
        </label>

        <label className="field">
          <span>Difficulty</span>
          <select value={difficulty} onChange={(event) => setDifficulty(event.target.value as Difficulty | "")}>
            <option value="">— none —</option>
            {DIFFICULTIES.map((level) => (
              <option key={level} value={level}>
                {level}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>Estimated duration (minutes)</span>
          <input type="number" min={1} value={duration} onChange={(event) => setDuration(event.target.value)} />
        </label>

        <div className="app__actions">
          <button type="submit" className="button" disabled={submitting}>
            {submitting ? "Saving…" : editing ? "Save changes" : "Create template"}
          </button>
          <button type="button" className="button button--ghost" onClick={onCancel} disabled={submitting}>
            Cancel
          </button>
        </div>
      </form>
    </section>
  );
}
