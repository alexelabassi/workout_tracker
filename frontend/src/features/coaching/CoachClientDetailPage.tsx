import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { useToast } from "../../shared/ui/ToastProvider";
import { AnalyticsView } from "../analytics/AnalyticsView";
import type { AnalyticsOverview } from "../analytics/types";
import type { HistoryItem, HistoryPage } from "../history/types";
import { Highlight } from "../search/Highlight";
import { SearchFacets } from "../search/SearchFacets";
import type { SearchResults, WorkoutSearchItem } from "../search/types";
import { fetchClientAnalytics, fetchClientHistory, searchClientWorkouts } from "./api";

const PAGE_SIZE = 10;
const STATUS_LABEL: Record<string, string> = {
  IN_PROGRESS: "In progress",
  FINISHED: "Finished",
  CANCELLED: "Cancelled",
};
const FACET_LABELS: Record<string, string> = {
  status: "Status",
  gym: "Gym",
  muscleGroups: "Muscle group",
  byMonth: "By month",
};

type Tab = "history" | "progress";
type Filters = { status?: string; gym?: string; muscleGroup?: string };

function formatDuration(seconds: number | null): string {
  if (seconds == null) {
    return "—";
  }
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) {
    return `${minutes} min`;
  }
  return `${Math.floor(minutes / 60)}h ${minutes % 60}m`;
}

export function CoachClientDetailPage() {
  const { clientId = "" } = useParams();
  const location = useLocation();
  const { showToast } = useToast();
  const clientName = (location.state as { clientName?: string } | null)?.clientName ?? "Client";

  const [tab, setTab] = useState<Tab>("history");
  const [page, setPage] = useState(0);
  const [history, setHistory] = useState<HistoryPage | null>(null);
  const [analytics, setAnalytics] = useState<AnalyticsOverview | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [queryInput, setQueryInput] = useState("");
  const [committedQuery, setCommittedQuery] = useState("");
  const [filters, setFilters] = useState<Filters>({});
  const [searchResults, setSearchResults] = useState<SearchResults<WorkoutSearchItem> | null>(null);
  const searchActive = committedQuery.trim() !== "" || Object.values(filters).some(Boolean);

  const loadHistory = useCallback(
    async (which: number) => {
      setError(null);
      try {
        setHistory(await fetchClientHistory(clientId, which, PAGE_SIZE));
      } catch (err) {
        setError(err instanceof ApiError ? err.message : "Could not load the client's history.");
      }
    },
    [clientId],
  );

  const loadAnalytics = useCallback(async () => {
    setError(null);
    try {
      setAnalytics(await fetchClientAnalytics(clientId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not load the client's analytics.");
    }
  }, [clientId]);

  useEffect(() => {
    if (tab === "history" && !searchActive) {
      void loadHistory(page);
    } else if (tab === "progress" && analytics === null) {
      void loadAnalytics();
    }
  }, [tab, page, searchActive, loadHistory, loadAnalytics, analytics]);

  useEffect(() => {
    if (tab !== "history" || !searchActive) {
      setSearchResults(null);
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        const results = await searchClientWorkouts(clientId, {
          q: committedQuery.trim() || undefined,
          status: filters.status,
          gym: filters.gym,
          muscleGroup: filters.muscleGroup,
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
  }, [tab, searchActive, committedQuery, filters, clientId, showToast]);

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

  const meta = (item: HistoryItem): string =>
    [
      new Date(item.startedAt).toLocaleDateString(),
      item.templateDayName ?? item.templateName,
      item.gymName,
      `${item.setCount} sets`,
      item.totalVolume > 0 ? `${item.totalVolume} kg vol` : null,
      formatDuration(item.durationSeconds),
    ]
      .filter(Boolean)
      .join(" · ");

  const searchMeta = (item: WorkoutSearchItem): string =>
    [
      item.startedAt != null ? new Date(item.startedAt).toLocaleDateString() : null,
      item.gymNameSnapshot,
      `${item.setCount ?? 0} sets`,
      item.totalVolume && item.totalVolume > 0 ? `${item.totalVolume} kg vol` : null,
      formatDuration(item.durationSeconds),
    ]
      .filter((part): part is string => Boolean(part))
      .join(" · ");

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>{clientName}</h1>
          <p className="muted">
            <Link to="/coach">← Back to clients</Link>
          </p>
        </div>
        <div className="app__actions">
          <button type="button" className={tab === "history" ? "button" : "button button--ghost"}
            onClick={() => setTab("history")}>
            History
          </button>
          <button type="button" className={tab === "progress" ? "button" : "button button--ghost"}
            onClick={() => setTab("progress")}>
            Progress
          </button>
        </div>
      </header>

      <main className="app__main">
        {error && (
          <section className="card">
            <div className="status status--down">
              <span className="dot" />
              <div>
                <strong>Couldn’t load</strong>
                <p className="muted">{error}</p>
              </div>
            </div>
          </section>
        )}

        {tab === "history" && (
          <>
            <section className="card">
              <form className="muscle-add" onSubmit={onSubmitSearch}>
                <input
                  type="search"
                  placeholder="Search this client's workouts (exercise, gym, equipment, notes)…"
                  value={queryInput}
                  onChange={(event) => setQueryInput(event.target.value)}
                  aria-label="Search client workouts"
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
                  active={{ status: filters.status, gym: filters.gym, muscleGroups: filters.muscleGroup }}
                  onToggle={toggleFacet}
                  labels={FACET_LABELS}
                  readOnlyFields={["byMonth"]}
                />
              )}
            </section>

            <section className="card">
              <header className="card__header">
                <h2>{searchActive ? "Search results" : "Workout history"}</h2>
              </header>

              {searchActive ? (
                <>
                  {!searchResults && <p className="muted">Searching…</p>}
                  {searchResults && searchResults.items.length === 0 && (
                    <p className="muted">No workouts match your search.</p>
                  )}
                  {searchResults && searchResults.items.length > 0 && (
                    <ul className="exercise-list">
                      {searchResults.items.map((item) => (
                        <li key={item.sessionId} className="exercise-row">
                          <div className="exercise-row__main">
                            <div className="exercise-row__title">
                              <strong>
                                <Highlight
                                  fragments={item.highlights?.gymNameSnapshot}
                                  fallback={item.templateNameSnapshot ?? "Workout"}
                                />
                              </strong>
                              <span className="badge badge--muted">{STATUS_LABEL[item.status] ?? item.status}</span>
                            </div>
                            <p className="muted exercise-row__muscles">{searchMeta(item)}</p>
                            {item.highlights?.exerciseNameSnapshots && (
                              <p className="muted exercise-row__muscles">
                                <Highlight fragments={item.highlights.exerciseNameSnapshots} fallback="" />
                              </p>
                            )}
                            {item.highlights?.notes && (
                              <p className="muted exercise-row__muscles">
                                Note: <Highlight fragments={item.highlights.notes} fallback="" />
                              </p>
                            )}
                          </div>
                          <div className="app__actions">
                            <Link
                              to={`/coach/clients/${clientId}/sessions/${item.sessionId}`}
                              state={{ clientName }}
                              className="button button--ghost"
                            >
                              View
                            </Link>
                          </div>
                        </li>
                      ))}
                    </ul>
                  )}
                </>
              ) : (
                <>
                  {!history && !error && <p className="muted">Loading…</p>}
                  {history && history.items.length === 0 && <p className="muted">No workouts logged yet.</p>}
                  {history && history.items.length > 0 && (
                    <>
                      <ul className="exercise-list">
                        {history.items.map((item) => (
                          <li key={item.sessionId} className="exercise-row">
                            <div className="exercise-row__main">
                              <div className="exercise-row__title">
                                <strong>{item.templateName ?? "Workout"}</strong>
                                <span className="badge badge--muted">{STATUS_LABEL[item.status] ?? item.status}</span>
                              </div>
                              <p className="muted exercise-row__muscles">{meta(item)}</p>
                            </div>
                            <div className="app__actions">
                              <Link
                                to={`/coach/clients/${clientId}/sessions/${item.sessionId}`}
                                state={{ clientName }}
                                className="button button--ghost"
                              >
                                View
                              </Link>
                            </div>
                          </li>
                        ))}
                      </ul>
                      <div className="app__actions" style={{ marginTop: 16 }}>
                        <button type="button" className="button button--ghost" disabled={page === 0}
                          onClick={() => setPage((current) => Math.max(current - 1, 0))}>
                          Previous
                        </button>
                        <button type="button" className="button button--ghost" disabled={!history.hasNext}
                          onClick={() => setPage((current) => current + 1)}>
                          Next
                        </button>
                      </div>
                    </>
                  )}
                </>
              )}
            </section>
          </>
        )}

        {tab === "progress" && (
          analytics ? <AnalyticsView data={analytics} /> : !error && (
            <section className="card">
              <p className="muted">Loading…</p>
            </section>
          )
        )}
      </main>
    </div>
  );
}
