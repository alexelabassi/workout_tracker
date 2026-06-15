import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { searchWorkouts } from "../search/api";
import { Highlight } from "../search/Highlight";
import { SearchFacets } from "../search/SearchFacets";
import type { SearchResults, WorkoutSearchItem } from "../search/types";
import { fetchHistory } from "./api";
import type { HistoryItem, HistoryPage as HistoryPageData } from "./types";

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

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; data: HistoryPageData };

type Filters = { status?: string; gym?: string; muscleGroup?: string };

function formatDuration(seconds: number | null): string {
  if (seconds == null) {
    return "—";
  }
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) {
    return `${minutes} min`;
  }
  const hours = Math.floor(minutes / 60);
  return `${hours}h ${minutes % 60}m`;
}

export function HistoryPage() {
  const [page, setPage] = useState(0);
  const [state, setState] = useState<LoadState>({ kind: "loading" });

  const [queryInput, setQueryInput] = useState("");
  const [committedQuery, setCommittedQuery] = useState("");
  const [filters, setFilters] = useState<Filters>({});
  const [searchResults, setSearchResults] = useState<SearchResults<WorkoutSearchItem> | null>(null);

  const searchActive = committedQuery.trim() !== "" || Object.values(filters).some(Boolean);

  const load = useCallback(async (which: number) => {
    setState({ kind: "loading" });
    try {
      const data = await fetchHistory(which, PAGE_SIZE);
      setState({ kind: "ready", data });
    } catch (err) {
      setState({ kind: "error", message: err instanceof ApiError ? err.message : "Could not load history." });
    }
  }, []);

  useEffect(() => {
    if (!searchActive) {
      setSearchResults(null);
      void load(page);
    }
  }, [load, page, searchActive]);

  useEffect(() => {
    if (!searchActive) {
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        const results = await searchWorkouts({
          q: committedQuery.trim() || undefined,
          status: filters.status,
          gym: filters.gym,
          muscleGroup: filters.muscleGroup,
          size: 25,
        });
        if (!cancelled) {
          setSearchResults(results);
        }
      } catch (err) {
        if (!cancelled) {
          setCommittedQuery("");
          setFilters({});
          setState({
            kind: "error",
            message: err instanceof ApiError ? err.message : "Search unavailable; showing the list.",
          });
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [searchActive, committedQuery, filters]);

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
          <h1>History</h1>
          <p className="muted">
            <Link to="/">← Back to dashboard</Link>
          </p>
        </div>
        <Link to="/analytics" className="button button--ghost">
          View progress
        </Link>
      </header>

      <main className="app__main">
        <section className="card">
          <form className="muscle-add" onSubmit={onSubmitSearch}>
            <input
              type="search"
              placeholder="Search your workouts (exercise, gym, equipment, notes)…"
              value={queryInput}
              onChange={(event) => setQueryInput(event.target.value)}
              aria-label="Search your workouts"
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
            <h2>{searchActive ? "Search results" : "Your workouts"}</h2>
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
                        <Link to={`/workouts/${item.sessionId}`} className="button button--ghost">
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
              {state.kind === "loading" && <p className="muted">Loading…</p>}

              {state.kind === "error" && (
                <div className="status status--down">
                  <span className="dot" />
                  <div>
                    <strong>Couldn’t load history</strong>
                    <p className="muted">{state.message}</p>
                    <button type="button" className="button button--ghost" onClick={() => void load(page)}>
                      Retry
                    </button>
                  </div>
                </div>
              )}

              {state.kind === "ready" && state.data.items.length === 0 && (
                <p className="muted">No workouts logged yet. Start one to build your history.</p>
              )}

              {state.kind === "ready" && state.data.items.length > 0 && (
                <>
                  <ul className="exercise-list">
                    {state.data.items.map((item) => (
                      <li key={item.sessionId} className="exercise-row">
                        <div className="exercise-row__main">
                          <div className="exercise-row__title">
                            <strong>{item.templateName ?? "Workout"}</strong>
                            <span className="badge badge--muted">{STATUS_LABEL[item.status] ?? item.status}</span>
                          </div>
                          <p className="muted exercise-row__muscles">{meta(item)}</p>
                        </div>
                        <div className="app__actions">
                          <Link to={`/workouts/${item.sessionId}`} className="button button--ghost">
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
                    <button type="button" className="button button--ghost" disabled={!state.data.hasNext}
                      onClick={() => setPage((current) => current + 1)}>
                      Next
                    </button>
                  </div>
                </>
              )}
            </>
          )}
        </section>
      </main>
    </div>
  );
}
