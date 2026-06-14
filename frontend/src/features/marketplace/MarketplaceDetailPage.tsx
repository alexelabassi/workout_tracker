import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { useToast } from "../../shared/ui/ToastProvider";
import { TemplateAnalysisPanel } from "../analyzer/TemplateAnalysisPanel";
import {
  fetchMarketplaceDetail,
  saveTemplate,
  unsaveTemplate,
  useTemplate,
  voteTemplate,
} from "./api";
import type { InteractionResult, MarketplaceDetail, VoteType } from "./types";

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; data: MarketplaceDetail };

export function MarketplaceDetailPage() {
  const { templateId = "" } = useParams();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [state, setState] = useState<LoadState>({ kind: "loading" });

  const load = useCallback(async () => {
    setState({ kind: "loading" });
    try {
      const data = await fetchMarketplaceDetail(templateId);
      setState({ kind: "ready", data });
    } catch (err) {
      setState({ kind: "error", message: err instanceof ApiError ? err.message : "Could not load this template." });
    }
  }, [templateId]);

  useEffect(() => {
    void load();
  }, [load]);

  const patch = (result: InteractionResult) =>
    setState((current) =>
      current.kind === "ready"
        ? { kind: "ready", data: { ...current.data, stats: result.stats, myVote: result.myVote, saved: result.saved } }
        : current,
    );

  const onVote = async (voteType: VoteType) => {
    try {
      patch(await voteTemplate(templateId, voteType));
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not vote.", "error");
    }
  };

  const onToggleSave = async (saved: boolean) => {
    try {
      patch(saved ? await unsaveTemplate(templateId) : await saveTemplate(templateId));
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not update save.", "error");
    }
  };

  const onUse = async () => {
    try {
      const copy = await useTemplate(templateId);
      showToast("Copied to your templates.");
      navigate(`/templates/${copy.id}`);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not copy the template.", "error");
    }
  };

  if (state.kind === "loading") {
    return (
      <div className="app">
        <main className="app__main">
          <p className="muted">Loading…</p>
        </main>
      </div>
    );
  }

  if (state.kind === "error") {
    return (
      <div className="app">
        <header className="app__header">
          <div>
            <h1>Template</h1>
            <p className="muted">
              <Link to="/marketplace">← Back to marketplace</Link>
            </p>
          </div>
        </header>
        <main className="app__main">
          <div className="status status--down">
            <span className="dot" />
            <div>
              <strong>Couldn’t load template</strong>
              <p className="muted">{state.message}</p>
            </div>
          </div>
        </main>
      </div>
    );
  }

  const { template, authorDisplayName, stats, myVote, saved } = state.data;

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>{template.name}</h1>
          <p className="muted">
            <Link to="/marketplace">← Back to marketplace</Link> · by {authorDisplayName}
          </p>
        </div>
        <button type="button" className="button" onClick={() => void onUse()}>
          Use this template
        </button>
      </header>

      <main className="app__main">
        <section className="card">
          <p className="muted">
            {[template.splitType, template.difficulty, template.daysPerWeek != null ? `${template.daysPerWeek}×/week` : null]
              .filter(Boolean)
              .join(" · ")}
          </p>
          {template.description && <p>{template.description}</p>}
          {template.muscleGroups.length > 0 && <p className="muted">Targets: {template.muscleGroups.join(", ")}</p>}
          <p className="muted">
            Net score {stats.upvotes - stats.downvotes} · {stats.saves} saves · {stats.uses} uses
          </p>
          <div className="app__actions" style={{ flexWrap: "wrap" }}>
            <button type="button" className={myVote === "UP" ? "button" : "button button--ghost"}
              onClick={() => void onVote("UP")}>
              ▲ Upvote ({stats.upvotes})
            </button>
            <button type="button" className={myVote === "DOWN" ? "button" : "button button--ghost"}
              onClick={() => void onVote("DOWN")}>
              ▼ Downvote ({stats.downvotes})
            </button>
            <button type="button" className="button button--ghost" onClick={() => void onToggleSave(saved)}>
              {saved ? "Saved ✓" : "Save"}
            </button>
          </div>
        </section>

        {template.days.map((day) => (
          <section className="card" key={day.id}>
            <header className="card__header">
              <h2>
                Day {day.dayNumber}: {day.name}
                {day.focus ? <span className="badge badge--muted"> {day.focus}</span> : null}
              </h2>
            </header>
            {day.exercises.length === 0 ? (
              <p className="muted">No exercises.</p>
            ) : (
              <ul className="exercise-list">
                {day.exercises.map((exercise) => (
                  <li key={exercise.id} className="exercise-row">
                    <div className="exercise-row__main">
                      <div className="exercise-row__title">
                        <strong>
                          {exercise.position}. {exercise.exerciseName}
                        </strong>
                        <span className="badge">{exercise.exerciseType}</span>
                      </div>
                      <p className="muted exercise-row__muscles">
                        {[
                          exercise.plannedSets != null && exercise.plannedReps
                            ? `${exercise.plannedSets} × ${exercise.plannedReps}`
                            : null,
                          exercise.muscleGroups.map((group) => group.code).join(", ") || null,
                        ]
                          .filter(Boolean)
                          .join(" · ")}
                      </p>
                    </div>
                  </li>
                ))}
              </ul>
            )}
            {day.routines.length > 0 && (
              <p className="muted">
                Routines: {day.routines.map((routine) => `${routine.routineName} (${routine.routineType})`).join(", ")}
              </p>
            )}
          </section>
        ))}

        <TemplateAnalysisPanel templateId={template.id} />
      </main>
    </div>
  );
}
