import { useEffect, useMemo, useState, type FormEvent } from "react";
import { ApiError } from "../../shared/api/client";
import { createCustomExercise, updateCustomExercise } from "./api";
import {
  EXERCISE_TYPES,
  type CustomExercisePayload,
  type Exercise,
  type ExerciseType,
  type MuscleGroup,
  type MuscleGroupAssignment,
  type MuscleRole,
} from "./types";

interface ExerciseFormProps {
  muscleGroups: MuscleGroup[];
  editing: Exercise | null;
  onSaved: (exercise: Exercise) => void;
  onCancel: () => void;
}

const ROLES: MuscleRole[] = ["PRIMARY", "SECONDARY"];

export function ExerciseForm({ muscleGroups, editing, onSaved, onCancel }: ExerciseFormProps) {
  const [name, setName] = useState("");
  const [exerciseType, setExerciseType] = useState<ExerciseType>("STRENGTH");
  const [description, setDescription] = useState("");
  const [assignments, setAssignments] = useState<MuscleGroupAssignment[]>([]);
  const [pendingCode, setPendingCode] = useState("");
  const [pendingRole, setPendingRole] = useState<MuscleRole>("PRIMARY");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setError(null);
    if (editing) {
      setName(editing.name);
      setExerciseType(editing.exerciseType);
      setDescription(editing.description ?? "");
      setAssignments(editing.muscleGroups.map((group) => ({ code: group.code, role: group.role })));
    } else {
      setName("");
      setExerciseType("STRENGTH");
      setDescription("");
      setAssignments([]);
    }
    setPendingCode("");
    setPendingRole("PRIMARY");
  }, [editing]);

  const nameByCode = useMemo(
    () => new Map(muscleGroups.map((group) => [group.code, group.displayName])),
    [muscleGroups],
  );

  const availableForAdd = muscleGroups.filter(
    (group) => !assignments.some((assignment) => assignment.code === group.code),
  );

  const addAssignment = () => {
    if (!pendingCode) {
      return;
    }
    setAssignments((current) => [...current, { code: pendingCode, role: pendingRole }]);
    setPendingCode("");
    setPendingRole("PRIMARY");
  };

  const removeAssignment = (code: string) => {
    setAssignments((current) => current.filter((assignment) => assignment.code !== code));
  };

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    const payload: CustomExercisePayload = {
      name: name.trim(),
      description: description.trim() ? description.trim() : null,
      exerciseType,
      muscleGroups: assignments,
    };
    try {
      const saved = editing
        ? await updateCustomExercise(editing.id, payload)
        : await createCustomExercise(payload);
      onSaved(saved);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save the exercise.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="card">
      <header className="card__header">
        <h2>{editing ? "Edit exercise" : "New custom exercise"}</h2>
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
            value={exerciseType}
            onChange={(event) => setExerciseType(event.target.value as ExerciseType)}
          >
            {EXERCISE_TYPES.map((type) => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>Description</span>
          <textarea
            rows={3}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
        </label>

        <div className="field">
          <span>Muscle groups</span>
          {assignments.length > 0 ? (
            <ul className="muscle-tags">
              {assignments.map((assignment) => (
                <li key={assignment.code} className="muscle-tag">
                  <span>
                    {nameByCode.get(assignment.code) ?? assignment.code} · {assignment.role}
                  </span>
                  <button type="button" onClick={() => removeAssignment(assignment.code)} aria-label="Remove">
                    ×
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <p className="muted">No muscle groups assigned yet.</p>
          )}

          <div className="muscle-add">
            <select value={pendingCode} onChange={(event) => setPendingCode(event.target.value)}>
              <option value="">Select muscle group…</option>
              {availableForAdd.map((group) => (
                <option key={group.code} value={group.code}>
                  {group.displayName}
                </option>
              ))}
            </select>
            <select value={pendingRole} onChange={(event) => setPendingRole(event.target.value as MuscleRole)}>
              {ROLES.map((role) => (
                <option key={role} value={role}>
                  {role}
                </option>
              ))}
            </select>
            <button type="button" className="button button--ghost" onClick={addAssignment} disabled={!pendingCode}>
              Add
            </button>
          </div>
        </div>

        <div className="app__actions">
          <button type="submit" className="button" disabled={submitting}>
            {submitting ? "Saving…" : editing ? "Save changes" : "Create exercise"}
          </button>
          <button type="button" className="button button--ghost" onClick={onCancel} disabled={submitting}>
            Cancel
          </button>
        </div>
      </form>
    </section>
  );
}
