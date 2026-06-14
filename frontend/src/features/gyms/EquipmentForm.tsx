import { useEffect, useState, type FormEvent } from "react";
import { ApiError } from "../../shared/api/client";
import { createEquipment, updateEquipment } from "./api";
import {
  EQUIPMENT_TYPES,
  EQUIPMENT_TYPE_LABELS,
  type Equipment,
  type EquipmentPayload,
  type EquipmentType,
} from "./types";

interface EquipmentFormProps {
  gymId: string;
  editing: Equipment | null;
  onSaved: (equipment: Equipment, isNew: boolean) => void;
  onCancel: () => void;
}

export function EquipmentForm({ gymId, editing, onSaved, onCancel }: EquipmentFormProps) {
  const [name, setName] = useState("");
  const [equipmentType, setEquipmentType] = useState<EquipmentType | "">("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setError(null);
    setName(editing?.name ?? "");
    setEquipmentType(editing?.equipmentType ?? "");
    setNotes(editing?.notes ?? "");
  }, [editing]);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    const payload: EquipmentPayload = {
      name: name.trim(),
      equipmentType: equipmentType === "" ? null : equipmentType,
      notes: notes.trim() ? notes.trim() : null,
    };
    try {
      const saved = editing
        ? await updateEquipment(editing.id, payload)
        : await createEquipment(gymId, payload);
      onSaved(saved, editing === null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save the equipment.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="card">
      <header className="card__header">
        <h2>{editing ? "Edit equipment" : "New equipment"}</h2>
      </header>

      <form className="exercise-form" onSubmit={onSubmit} noValidate>
        {error && <p className="form-error">{error}</p>}

        <label className="field">
          <span>Name</span>
          <input value={name} onChange={(event) => setName(event.target.value)} required maxLength={160} />
        </label>

        <label className="field">
          <span>Type</span>
          <select
            value={equipmentType}
            onChange={(event) => setEquipmentType(event.target.value as EquipmentType | "")}
          >
            <option value="">— none —</option>
            {EQUIPMENT_TYPES.map((type) => (
              <option key={type} value={type}>
                {EQUIPMENT_TYPE_LABELS[type]}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>Notes</span>
          <textarea
            rows={3}
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            maxLength={5000}
            placeholder="optional"
          />
        </label>

        <div className="app__actions">
          <button type="submit" className="button" disabled={submitting}>
            {submitting ? "Saving…" : editing ? "Save changes" : "Create equipment"}
          </button>
          <button type="button" className="button button--ghost" onClick={onCancel} disabled={submitting}>
            Cancel
          </button>
        </div>
      </form>
    </section>
  );
}
