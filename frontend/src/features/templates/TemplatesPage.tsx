import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { useToast } from "../../shared/ui/ToastProvider";
import { TemplateForm } from "./TemplateForm";
import { deleteTemplate, fetchTemplates } from "./api";
import type { TemplateDetail, TemplateSummary } from "./types";

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; templates: TemplateSummary[] };

export function TemplatesPage() {
  const { showToast } = useToast();
  const [state, setState] = useState<LoadState>({ kind: "loading" });
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<TemplateSummary | null>(null);

  const load = useCallback(async () => {
    setState({ kind: "loading" });
    try {
      const templates = await fetchTemplates();
      setState({ kind: "ready", templates });
    } catch (err) {
      setState({ kind: "error", message: err instanceof ApiError ? err.message : "Could not load templates." });
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };

  const openEdit = (template: TemplateSummary) => {
    setEditing(template);
    setFormOpen(true);
  };

  const onSaved = (_template: TemplateDetail, isNew: boolean) => {
    setFormOpen(false);
    setEditing(null);
    showToast(isNew ? "Template created." : "Template updated.");
    void load();
  };

  const onDelete = async (template: TemplateSummary) => {
    if (!window.confirm(`Delete "${template.name}"?`)) {
      return;
    }
    try {
      await deleteTemplate(template.id);
      showToast("Template deleted.");
      void load();
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not delete the template.", "error");
    }
  };

  const meta = (template: TemplateSummary): string => {
    const parts = [
      template.splitType,
      template.difficulty,
      template.daysPerWeek != null ? `${template.daysPerWeek}×/week` : null,
      `${template.dayCount} day${template.dayCount === 1 ? "" : "s"}`,
    ].filter((part): part is string => Boolean(part));
    return parts.join(" · ");
  };

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>Templates</h1>
          <p className="muted">
            <Link to="/">← Back to dashboard</Link>
          </p>
        </div>
        <button type="button" className="button" onClick={openCreate}>
          New template
        </button>
      </header>

      <main className="app__main">
        {formOpen && <TemplateForm editing={editing} onSaved={onSaved} onCancel={() => setFormOpen(false)} />}

        <section className="card">
          <header className="card__header">
            <h2>Your programs</h2>
          </header>

          {state.kind === "loading" && <p className="muted">Loading…</p>}

          {state.kind === "error" && (
            <div className="status status--down">
              <span className="dot" />
              <div>
                <strong>Couldn’t load templates</strong>
                <p className="muted">{state.message}</p>
                <button type="button" className="button button--ghost" onClick={() => void load()}>
                  Retry
                </button>
              </div>
            </div>
          )}

          {state.kind === "ready" && state.templates.length === 0 && (
            <p className="muted">No templates yet. Create a program and build it out day by day.</p>
          )}

          {state.kind === "ready" && state.templates.length > 0 && (
            <ul className="exercise-list">
              {state.templates.map((template) => (
                <li key={template.id} className="exercise-row">
                  <div className="exercise-row__main">
                    <div className="exercise-row__title">
                      <strong>{template.name}</strong>
                    </div>
                    <p className="muted exercise-row__muscles">{meta(template)}</p>
                  </div>
                  <div className="app__actions">
                    <Link to={`/templates/${template.id}`} className="button button--ghost">
                      Open builder
                    </Link>
                    <button type="button" className="button button--ghost" onClick={() => openEdit(template)}>
                      Edit
                    </button>
                    <button type="button" className="button button--ghost" onClick={() => void onDelete(template)}>
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
