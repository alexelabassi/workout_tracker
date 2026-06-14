import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { useToast } from "../../shared/ui/ToastProvider";
import { fetchMarketplace, saveTemplate, unsaveTemplate, useTemplate, voteTemplate } from "./api";
import type { InteractionResult, MarketplacePageData, MarketplaceSort, MarketplaceSummary, VoteType } from "./types";

const PAGE_SIZE = 12;
const SORTS: { key: MarketplaceSort; label: string }[] = [
  { key: "newest", label: "Newest" },
  { key: "top", label: "Top rated" },
  { key: "trending", label: "Trending" },
];

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; data: MarketplacePageData };

export function MarketplacePage() {
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [sort, setSort] = useState<MarketplaceSort>("newest");
  const [savedOnly, setSavedOnly] = useState(false);
  const [page, setPage] = useState(0);
  const [state, setState] = useState<LoadState>({ kind: "loading" });

  const load = useCallback(async () => {
    setState({ kind: "loading" });
    try {
      const data = await fetchMarketplace(sort, savedOnly, page, PAGE_SIZE);
      setState({ kind: "ready", data });
    } catch (err) {
      setState({ kind: "error", message: err instanceof ApiError ? err.message : "Could not load the marketplace." });
    }
  }, [sort, savedOnly, page]);

  useEffect(() => {
    void load();
  }, [load]);

  const patchItem = (id: string, result: InteractionResult) => {
    setState((current) => {
      if (current.kind !== "ready") {
        return current;
      }
      const items = current.data.items.map((item) =>
        item.id === id ? { ...item, stats: result.stats, myVote: result.myVote, saved: result.saved } : item,
      );
      return { kind: "ready", data: { ...current.data, items } };
    });
  };

  const onVote = async (item: MarketplaceSummary, voteType: VoteType) => {
    try {
      patchItem(item.id, await voteTemplate(item.id, voteType));
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not vote.", "error");
    }
  };

  const onToggleSave = async (item: MarketplaceSummary) => {
    try {
      const result = item.saved ? await unsaveTemplate(item.id) : await saveTemplate(item.id);
      patchItem(item.id, result);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not update save.", "error");
    }
  };

  const onUse = async (item: MarketplaceSummary) => {
    try {
      const copy = await useTemplate(item.id);
      showToast("Copied to your templates.");
      navigate(`/templates/${copy.id}`);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Could not copy the template.", "error");
    }
  };

  const meta = (item: MarketplaceSummary): string =>
    [item.splitType, item.difficulty, item.daysPerWeek != null ? `${item.daysPerWeek}×/week` : null]
      .filter((part): part is string => Boolean(part))
      .join(" · ");

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>Marketplace</h1>
          <p className="muted">
            <Link to="/">← Back to dashboard</Link>
          </p>
        </div>
      </header>

      <main className="app__main">
        <section className="card">
          <div className="app__actions" style={{ flexWrap: "wrap" }}>
            {SORTS.map((option) => (
              <button
                key={option.key}
                type="button"
                className={option.key === sort ? "button" : "button button--ghost"}
                onClick={() => {
                  setPage(0);
                  setSort(option.key);
                }}
              >
                {option.label}
              </button>
            ))}
            <button
              type="button"
              className={savedOnly ? "button" : "button button--ghost"}
              onClick={() => {
                setPage(0);
                setSavedOnly((value) => !value);
              }}
            >
              Saved only
            </button>
          </div>
        </section>

        {state.kind === "loading" && (
          <section className="card">
            <p className="muted">Loading…</p>
          </section>
        )}

        {state.kind === "error" && (
          <section className="card">
            <div className="status status--down">
              <span className="dot" />
              <div>
                <strong>Couldn’t load the marketplace</strong>
                <p className="muted">{state.message}</p>
                <button type="button" className="button button--ghost" onClick={() => void load()}>
                  Retry
                </button>
              </div>
            </div>
          </section>
        )}

        {state.kind === "ready" && state.data.items.length === 0 && (
          <section className="card">
            <p className="muted">{savedOnly ? "You haven't saved any templates yet." : "No public templates yet."}</p>
          </section>
        )}

        {state.kind === "ready" &&
          state.data.items.map((item) => (
            <section className="card" key={item.id}>
              <header className="card__header">
                <h2>
                  <Link to={`/marketplace/${item.id}`}>{item.name}</Link>
                </h2>
                <span className="badge">▲ {item.stats.upvotes - item.stats.downvotes}</span>
              </header>
              <p className="muted">
                by {item.authorDisplayName}
                {meta(item) ? ` · ${meta(item)}` : ""}
              </p>
              {item.description && <p>{item.description}</p>}
              <p className="muted">
                {item.stats.saves} saves · {item.stats.uses} uses
              </p>
              <div className="app__actions" style={{ flexWrap: "wrap" }}>
                <button
                  type="button"
                  className={item.myVote === "UP" ? "button" : "button button--ghost"}
                  onClick={() => void onVote(item, "UP")}
                >
                  ▲ Upvote
                </button>
                <button
                  type="button"
                  className={item.myVote === "DOWN" ? "button" : "button button--ghost"}
                  onClick={() => void onVote(item, "DOWN")}
                >
                  ▼ Downvote
                </button>
                <button type="button" className="button button--ghost" onClick={() => void onToggleSave(item)}>
                  {item.saved ? "Saved ✓" : "Save"}
                </button>
                <button type="button" className="button" onClick={() => void onUse(item)}>
                  Use this template
                </button>
              </div>
            </section>
          ))}

        {state.kind === "ready" && (
          <div className="app__actions">
            <button type="button" className="button button--ghost" disabled={page === 0}
              onClick={() => setPage((current) => Math.max(current - 1, 0))}>
              Previous
            </button>
            <button type="button" className="button button--ghost" disabled={!state.data.hasNext}
              onClick={() => setPage((current) => current + 1)}>
              Next
            </button>
          </div>
        )}
      </main>
    </div>
  );
}
