package com.thesis.workout.search.web.dto;

import java.util.List;

/**
 * Envelope for a search response: the relevance-ranked page of items (already re-validated against
 * PostgreSQL), the facet aggregations, and paging metadata. {@code totalHits} is OpenSearch's match
 * count; the number of {@code items} can be slightly smaller if the defense-in-depth post-filter
 * dropped a stale hit (e.g. a template unpublished after indexing).
 */
public record SearchResultsResponse<T>(
        List<T> items,
        List<SearchFacetResponse> facets,
        int page,
        int size,
        long totalHits) {
}
