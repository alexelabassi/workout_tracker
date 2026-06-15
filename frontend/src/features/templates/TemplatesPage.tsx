import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { useToast } from "../../shared/ui/ToastProvider";
import { searchTemplates } from "../search/api";
import { Highlight } from "../search/Highlight";
import { SearchFacets } from "../search/SearchFacets";
import type { SearchResults, TemplateSearchItem } from "../search/types";
import { TemplateForm } from "./TemplateForm";
import { deleteTemplate, fetchTemplates } from "./api";
import type { TemplateDetail, TemplateSummary } from "./types";

const FACET_LABELS: Record<string, string> = {
  difficulty: "Difficulty",
  splitType: "Split",
  muscleGroups: "Muscle group",
  analysisCategory: "Structure",
  daysPerWeek: "Days/week",
};

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; templates: TemplateSummary[] };

type Filters = { difficulty?: string; splitType?: string; muscleGroup?: string; analysisCategory?: string };

export function TemplatesPage() {
  const { showToast } = useToast();
  const [state, setState] = useState<LoadState>({ kind: "loading" });
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<TemplateSummary | null>(null);

  const [queryInput, setQueryInput] = useState("");
  const [committedQuery, setCommittedQuery] = useState("");
  const [filters, setFilters] = useState<Filters>({});
  const [searchResults, setSearchResults] = useState<SearchResults<TemplateSearchItem> | null>(null);

  const searchActive = committedQuery.trim() !== "" || Object.values(filters).some(Boolean);

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

  useEffect(() => {
    if (!searchActive) {
      setSearchResults(null);
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        const results = await searchTemplates({
          scope: "my",
          q: committedQuery.trim() || undefined,
          difficulty: filters.difficulty,
          splitType: filters.splitType,
          muscleGroup: filters.muscleGroup,
          analysisCategory: filters.analysisCategory,
          size: 25,
        });
        if (!cancelled) {
          setSearchResults(results);
        }
      } catch (err) {
        if (!cancelled) {
          showToast(err instanceof ApiError ? err.message : "Search unavailable; showing the list.", "error");
          setCommittedQuery("");
          setFilters({});
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [searchActive, committedQuery, filters, showToast]);

  const onSubmitSearch = (event: FormEvent) => {
    event.preventDefault();
    setCommittedQuery(queryInput);
  };

  const clearSearch = () => {
    setQueryInput("");
    setCommittedQuery("");
    setFilters({});
  };

  const toggleFacet = (field: string, key: string) => {
    const param = field === "muscleGroups" ? "muscleGroup" : field;
    setFilters((current) => {
      const typed = param as keyof Filters;
      return { ...current, [typed]: current[typed] === key ? undefined : key };
    });
  };

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

  const meta = (template: TemplateSummary): string =>
    [
      template.splitType,
      template.difficulty,
      template.daysPerWeek != null ? `${template.daysPerWeek}×/week` : null,
      `${template.dayCount} day${template.dayCount === 1 ? "" : "s"}`,
    ]
      .filter((part): part is string => Boolean(part))
      .join(" · ");

  const searchMeta = (item: TemplateSearchItem): string =>
    [
      item.splitType,
      item.difficulty,
      item.daysPerWeek != null ? `${item.daysPerWeek}×/week` : null,
      item.templateStructureScore != null ? `structure ${item.templateStructureScore}/100` : null,
    ]
      .filter((part): part is string => Boolean(part))
      .join(" · ");

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
          <form className="muscle-add" onSubmit={onSubmitSearch}>
            <input
              type="search"
              placeholder="Search your programs (exercise, muscle, name)…"
              value={queryInput}
              onChange={(event) => setQueryInput(event.target.value)}
              aria-label="Search your templates"
            />
            <button type="submit" className="button">
              Search
            </button>
            {searchActive && (
              <button type="button" className="button button--ghost" onClick={clearSearch}>
                Clear
              </button>
            )}
          </form>
          {searchActive && searchResults && (
            <SearchFacets
              facets={searchResults.facets}
              active={{
                difficulty: filters.difficulty,
                splitType: filters.splitType,
                muscleGroups: filters.muscleGroup,
                analysisCategory: filters.analysisCategory,
              }}
              onToggle={toggleFacet}
              labels={FACET_LABELS}
            />
          )}
        </section>

        <section className="card">
          <header className="card__header">
            <h2>{searchActive ? "Search results" : "Your programs"}</h2>
          </header>

          {searchActive ? (
            <>
              {!searchResults && <p className="muted">Searching…</p>}
              {searchResults && searchResults.items.length === 0 && (
                <p className="muted">No programs match your search.</p>
              )}
              {searchResults && searchResults.items.length > 0 && (
                <ul className="exercise-list">
                  {searchResults.items.map((item) => (
                    <li key={item.templateId} className="exercise-row">
                      <div className="exercise-row__main">
                        <div className="exercise-row__title">
                          <strong>
                            <Highlight fragments={item.highlights?.name} fallback={item.name} />
                          </strong>
                          {item.analysisCategory && <span className="badge badge--muted">{item.analysisCategory}</span>}
                        </div>
                        <p className="muted exercise-row__muscles">{searchMeta(item)}</p>
                        {item.highlights?.exerciseNames && (
                          <p className="muted exercise-row__muscles">
                            <Highlight fragments={item.highlights.exerciseNames} fallback="" />
                          </p>
                        )}
                      </div>
                      <div className="app__actions">
                        <Link to={`/templates/${item.templateId}`} className="button button--ghost">
                          Open builder
                        </Link>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </>
          ) : (
            <>
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
            </>
          )}
        </section>
      </main>
    </div>
  );
}
