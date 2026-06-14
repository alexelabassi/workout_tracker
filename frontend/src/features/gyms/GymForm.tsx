import { useEffect, useState, type FormEvent } from "react";
import { ApiError } from "../../shared/api/client";
import { createGym, updateGym } from "./api";
import type { Gym, GymPayload } from "./types";

interface GymFormProps {
  editing: Gym | null;
  onSaved: (gym: Gym, isNew: boolean) => void;
  onCancel: () => void;
}

export function GymForm({ editing, onSaved, onCancel }: GymFormProps) {
  const [name, setName] = useState("");
  const [location, setLocation] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setError(null);
    setName(editing?.name ?? "");
    setLocation(editing?.location ?? "");
  }, [editing]);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    const payload: GymPayload = {
      name: name.trim(),
      location: location.trim() ? location.trim() : null,
    };
    try {
      const saved = editing ? await updateGym(editing.id, payload) : await createGym(payload);
      onSaved(saved, editing === null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save the gym.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="card">
      <header className="card__header">
        <h2>{editing ? "Edit gym" : "New gym"}</h2>
      </header>

      <form className="exercise-form" onSubmit={onSubmit} noValidate>
        {error && <p className="form-error">{error}</p>}

        <label className="field">
          <span>Name</span>
          <input value={name} onChange={(event) => setName(event.target.value)} required maxLength={160} />
        </label>

        <label className="field">
          <span>Location</span>
          <input
            value={location}
            onChange={(event) => setLocation(event.target.value)}
            maxLength={255}
            placeholder="optional"
          />
        </label>

        <div className="app__actions">
          <button type="submit" className="button" disabled={submitting}>
            {submitting ? "Saving…" : editing ? "Save changes" : "Create gym"}
          </button>
          <button type="button" className="button button--ghost" onClick={onCancel} disabled={submitting}>
            Cancel
          </button>
        </div>
      </form>
    </section>
  );
}
