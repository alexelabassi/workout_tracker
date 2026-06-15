import type { SearchFacet } from "./types";

interface SearchFacetsProps {
  facets: SearchFacet[];
  /** Active filter value keyed by facet field (so the matching chip can be highlighted). */
  active: Record<string, string | undefined>;
  /** Clicking a chip toggles that field's filter. */
  onToggle: (field: string, key: string) => void;
  /** Human labels per facet field. */
  labels?: Record<string, string>;
  /** Fields rendered for information only (not clickable), e.g. a date histogram. */
  readOnlyFields?: string[];
}

/**
 * Renders OpenSearch aggregations as facet chips. Clickable facets double as structured filters:
 * selecting a bucket adds the corresponding filter and re-runs the search, demonstrating the
 * facet/filter interplay over the same query.
 */
export function SearchFacets({ facets, active, onToggle, labels = {}, readOnlyFields = [] }: SearchFacetsProps) {
  const visible = facets.filter((facet) => facet.buckets.length > 0);
  if (visible.length === 0) {
    return null;
  }
  return (
    <div className="search-facets">
      {visible.map((facet) => {
        const readOnly = readOnlyFields.includes(facet.field);
        return (
          <div key={facet.field} className="search-facets__group">
            <span className="muted search-facets__label">{labels[facet.field] ?? facet.field}</span>
            <div className="search-facets__chips">
              {facet.buckets.map((bucket) => {
                const isActive = active[facet.field] === bucket.key;
                if (readOnly) {
                  return (
                    <span key={bucket.key} className="badge badge--muted">
                      {bucket.key} · {bucket.count}
                    </span>
                  );
                }
                return (
                  <button
                    key={bucket.key}
                    type="button"
                    className={isActive ? "button" : "button button--ghost"}
                    onClick={() => onToggle(facet.field, bucket.key)}
                  >
                    {bucket.key} · {bucket.count}
                  </button>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
}
