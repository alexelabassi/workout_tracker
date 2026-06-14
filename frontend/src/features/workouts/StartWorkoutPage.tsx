import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { useToast } from "../../shared/ui/ToastProvider";
import { fetchGyms } from "../gyms/api";
import type { Gym } from "../gyms/types";
import { fetchTemplate, fetchTemplates } from "../templates/api";
import type { TemplateDay, TemplateSummary } from "../templates/types";
import { startWorkout } from "./api";

export function StartWorkoutPage() {
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [templates, setTemplates] = useState<TemplateSummary[]>([]);
  const [gyms, setGyms] = useState<Gym[]>([]);
  const [days, setDays] = useState<TemplateDay[]>([]);
  const [templateId, setTemplateId] = useState("");
  const [dayId, setDayId] = useState("");
  const [gymId, setGymId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    Promise.all([fetchTemplates(), fetchGyms()])
      .then(([templateList, gymList]) => {
        setTemplates(templateList);
        setGyms(gymList);
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : "Could not load your data."))
      .finally(() => setLoading(false));
  }, []);

  const onTemplateChange = useCallback(async (value: string) => {
    setTemplateId(value);
    setDayId("");
    setDays([]);
    if (!value) {
      return;
    }
    try {
      const detail = await fetchTemplate(value);
      setDays(detail.days);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not load template days.");
    }
  }, []);

  const onStart = async () => {
    if (!dayId || !gymId) {
      setError("Pick a template day and a gym.");
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      await startWorkout(dayId, gymId);
      showToast("Workout started.");
      navigate("/workouts/live");
    } catch (err) {
      if (err instanceof ApiError && err.code === "ACTIVE_WORKOUT_EXISTS") {
        showToast("You already have a workout in progress.", "error");
        navigate("/workouts/live");
        return;
      }
      setError(err instanceof ApiError ? err.message : "Could not start the workout.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>Start workout</h1>
          <p className="muted">
            <Link to="/">← Back to dashboard</Link>
          </p>
        </div>
      </header>

      <main className="app__main">
        <section className="card">
          <header className="card__header">
            <h2>Choose your session</h2>
          </header>

          {loading && <p className="muted">Loading…</p>}
          {error && <p className="form-error">{error}</p>}

          {!loading && (
            <div className="exercise-form">
              <label className="field">
                <span>Template</span>
                <select value={templateId} onChange={(event) => void onTemplateChange(event.target.value)}>
                  <option value="">Select a template…</option>
                  {templates.map((template) => (
                    <option key={template.id} value={template.id}>
                      {template.name}
                    </option>
                  ))}
                </select>
              </label>

              <label className="field">
                <span>Day</span>
                <select value={dayId} onChange={(event) => setDayId(event.target.value)} disabled={!templateId}>
                  <option value="">Select a day…</option>
                  {days.map((day) => (
                    <option key={day.id} value={day.id}>
                      Day {day.dayNumber}: {day.name} ({day.exercises.length} exercises)
                    </option>
                  ))}
                </select>
              </label>

              <label className="field">
                <span>Gym</span>
                <select value={gymId} onChange={(event) => setGymId(event.target.value)}>
                  <option value="">Select a gym…</option>
                  {gyms.map((gym) => (
                    <option key={gym.id} value={gym.id}>
                      {gym.name}
                    </option>
                  ))}
                </select>
              </label>

              <button type="button" className="button button--block" onClick={() => void onStart()} disabled={submitting}>
                {submitting ? "Starting…" : "Start workout"}
              </button>

              {templates.length === 0 && (
                <p className="muted">
                  You need a template first. <Link to="/templates">Build one</Link>.
                </p>
              )}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
