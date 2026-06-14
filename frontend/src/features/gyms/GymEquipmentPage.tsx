import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { useToast } from "../../shared/ui/ToastProvider";
import { EquipmentForm } from "./EquipmentForm";
import { deleteEquipment, fetchEquipment, fetchGym } from "./api";
import { EQUIPMENT_TYPE_LABELS, type Equipment, type Gym } from "./types";

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; gym: Gym; equipment: Equipment[] };

export function GymEquipmentPage() {
  const { gymId = "" } = useParams();
  const { showToast } = useToast();
  const [state, setState] = useState<LoadState>({ kind: "loading" });
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Equipment | null>(null);

  const load = useCallback(async () => {
    setState({ kind: "loading" });
    try {
      // Fetch the gym (header + deep-link reload) and its equipment together.
      const [gym, equipment] = await Promise.all([fetchGym(gymId), fetchEquipment(gymId)]);
      setState({ kind: "ready", gym, equipment });
    } catch (err) {
      setState({
        kind: "error",
        message: err instanceof ApiError ? err.message : "Could not load this gym's equipment.",
      });
    }
  }, [gymId]);

  useEffect(() => {
    void load();
  }, [load]);

  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };

  const openEdit = (equipment: Equipment) => {
    setEditing(equipment);
    setFormOpen(true);
  };

  const onSaved = (_equipment: Equipment, isNew: boolean) => {
    setFormOpen(false);
    setEditing(null);
    showToast(isNew ? "Equipment added." : "Equipment updated.");
    void load();
  };

  const onDelete = async (equipment: Equipment) => {
    if (!window.confirm(`Delete "${equipment.name}"?`)) {
      return;
    }
    try {
      await deleteEquipment(equipment.id);
      showToast("Equipment deleted.");
      void load();
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not delete the equipment.", "error");
    }
  };

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>{state.kind === "ready" ? state.gym.name : "Equipment"}</h1>
          <p className="muted">
            <Link to="/gyms">← Back to gyms</Link>
            {state.kind === "ready" && state.gym.location ? ` · ${state.gym.location}` : ""}
          </p>
        </div>
        {state.kind === "ready" && (
          <button type="button" className="button" onClick={openCreate}>
            New equipment
          </button>
        )}
      </header>

      <main className="app__main">
        {formOpen && state.kind === "ready" && (
          <EquipmentForm
            gymId={gymId}
            editing={editing}
            onSaved={onSaved}
            onCancel={() => setFormOpen(false)}
          />
        )}

        <section className="card">
          <header className="card__header">
            <h2>Equipment</h2>
          </header>

          {state.kind === "loading" && <p className="muted">Loading…</p>}

          {state.kind === "error" && (
            <div className="status status--down">
              <span className="dot" />
              <div>
                <strong>Couldn’t load equipment</strong>
                <p className="muted">{state.message}</p>
                <button type="button" className="button button--ghost" onClick={() => void load()}>
                  Retry
                </button>
              </div>
            </div>
          )}

          {state.kind === "ready" && state.equipment.length === 0 && (
            <p className="muted">No equipment yet. Add the machines and free weights available here.</p>
          )}

          {state.kind === "ready" && state.equipment.length > 0 && (
            <ul className="exercise-list">
              {state.equipment.map((item) => (
                <li key={item.id} className="exercise-row">
                  <div className="exercise-row__main">
                    <div className="exercise-row__title">
                      <strong>{item.name}</strong>
                      {item.equipmentType && (
                        <span className="badge">{EQUIPMENT_TYPE_LABELS[item.equipmentType]}</span>
                      )}
                    </div>
                    {item.notes && <p className="muted exercise-row__muscles">{item.notes}</p>}
                  </div>
                  <div className="app__actions">
                    <button type="button" className="button button--ghost" onClick={() => openEdit(item)}>
                      Edit
                    </button>
                    <button type="button" className="button button--ghost" onClick={() => void onDelete(item)}>
                      Delete
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </div>
  );
}
