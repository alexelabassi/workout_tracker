import { useState } from "react";
import { ApiError } from "../../shared/api/client";
import { useToast } from "../../shared/ui/ToastProvider";
import { updateWorkoutNotes } from "./api";

interface SessionNotesEditorProps {
  sessionId: string;
  initialNotes: string | null;
  onSaved?: () => void;
}

export function SessionNotesEditor({ sessionId, initialNotes, onSaved }: SessionNotesEditorProps) {
  const { showToast } = useToast();
  const [notes, setNotes] = useState(initialNotes ?? "");
  const [saving, setSaving] = useState(false);

  const save = async () => {
    setSaving(true);
    try {
      await updateWorkoutNotes(sessionId, notes.trim() ? notes.trim() : null);
      showToast("Workout notes saved.");
      onSaved?.();
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not save notes.", "error");
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="card">
      <header className="card__header">
        <h2>Workout notes</h2>
      </header>
      <label className="field">
        <span>How did the session go?</span>
        <textarea
          rows={4}
          value={notes}
          maxLength={5000}
          placeholder="Energy, pains, PRs, things to change next time…"
          onChange={(event) => setNotes(event.target.value)}
        />
      </label>
      <div className="app__actions">
        <button type="button" className="button" onClick={() => void save()} disabled={saving}>
          {saving ? "Saving…" : "Save notes"}
        </button>
      </div>
    </section>
  );
}
