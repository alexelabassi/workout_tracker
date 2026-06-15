package com.thesis.workout.search.web.dto;

import java.util.List;

/**
 * A single facet (terms or date-histogram aggregation) computed by OpenSearch over the full result
 * set — e.g. how many matching templates fall in each difficulty, or matching workouts per month.
 * Facet counts come from the index (eventually consistent), unlike the {@code items} which are
 * additionally re-validated against PostgreSQL.
 */
public record SearchFacetResponse(String field, List<Bucket> buckets) {

    public record Bucket(String key, long count) {
    }
}
