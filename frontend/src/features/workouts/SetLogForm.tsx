import { useState, type FormEvent } from "react";
import { ApiError } from "../../shared/api/client";
import type { Equipment } from "../gyms/types";
import { addSet } from "./api";
import { SET_TYPES, type SetPayload, type SetType } from "./types";

interface SetLogFormProps {
  sessionExerciseId: string;
  equipment: Equipment[];
  onLogged: () => void;
  onError: (message: string) => void;
}

function toNumberOrNull(value: string): number | null {
  if (value.trim() === "") {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

export function SetLogForm({ sessionExerciseId, equipment, onLogged, onError }: SetLogFormProps) {
  const [setType, setSetType] = useState<SetType>("WORKING");
  const [weight, setWeight] = useState("");
  const [reps, setReps] = useState("");
  const [rpe, setRpe] = useState("");
  const [equipmentId, setEquipmentId] = useState("");
  const [note, setNote] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    const payload: SetPayload = {
      setType,
      weight: toNumberOrNull(weight),
      reps: toNumberOrNull(reps),
      durationSeconds: null,
      distanceMeters: null,
      rpe: toNumberOrNull(rpe),
      note: note.trim() ? note.trim() : null,
      equipmentId: equipmentId === "" ? null : equipmentId,
    };
    try {
      await addSet(sessionExerciseId, payload);
      setWeight("");
      setReps("");
      setRpe("");
      setNote("");
      onLogged();
    } catch (err) {
      onError(err instanceof ApiError ? err.message : "Could not log the set.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="set-log" onSubmit={onSubmit}>
      <select value={setType} onChange={(event) => setSetType(event.target.value as SetType)} aria-label="Set type">
        {SET_TYPES.map((type) => (
          <option key={type} value={type}>
            {type}
          </option>
        ))}
      </select>
      <input type="number" min={0} step="0.5" placeholder="kg" value={weight}
        onChange={(event) => setWeight(event.target.value)} aria-label="Weight" />
      <input type="number" min={0} placeholder="reps" value={reps}
        onChange={(event) => setReps(event.target.value)} aria-label="Reps" />
      <input type="number" min={1} max={10} step="0.5" placeholder="RPE" value={rpe}
        onChange={(event) => setRpe(event.target.value)} aria-label="RPE" />
      <select value={equipmentId} onChange={(event) => setEquipmentId(event.target.value)} aria-label="Equipment">
        <option value="">No equipment</option>
        {equipment.map((item) => (
          <option key={item.id} value={item.id}>
            {item.name}
          </option>
        ))}
      </select>
      <input type="text" placeholder="note (optional)" value={note} maxLength={5000}
        onChange={(event) => setNote(event.target.value)} aria-label="Set note" />
      <button type="submit" className="button" disabled={submitting}>
        {submitting ? "…" : "Log set"}
      </button>
    </form>
  );
}
