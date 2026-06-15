package com.thesis.workout.search.application;

import com.thesis.workout.search.web.dto.SearchFacetResponse;
import com.thesis.workout.search.web.dto.SearchFacetResponse.Bucket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;

/**
 * Converts OpenSearch aggregate results into the flat {@link SearchFacetResponse} the API returns.
 * Handles string-terms, long-terms (e.g. {@code daysPerWeek}) and date-histogram aggregations.
 */
final class SearchAggregations {

    private SearchAggregations() {
    }

    static SearchFacetResponse terms(String name, Map<String, Aggregate> aggregations) {
        List<Bucket> buckets = new ArrayList<>();
        Aggregate aggregate = aggregations.get(name);
        if (aggregate != null) {
            if (aggregate.isSterms()) {
                aggregate.sterms().buckets().array()
                        .forEach(b -> buckets.add(new Bucket(b.key(), b.docCount())));
            } else if (aggregate.isLterms()) {
                aggregate.lterms().buckets().array()
                        .forEach(b -> buckets.add(new Bucket(b.key(), b.docCount())));
            }
        }
        return new SearchFacetResponse(name, buckets);
    }

    static SearchFacetResponse dateHistogram(String name, Map<String, Aggregate> aggregations) {
        List<Bucket> buckets = new ArrayList<>();
        Aggregate aggregate = aggregations.get(name);
        if (aggregate != null && aggregate.isDateHistogram()) {
            aggregate.dateHistogram().buckets().array()
                    .forEach(b -> buckets.add(new Bucket(b.keyAsString(), b.docCount())));
        }
        return new SearchFacetResponse(name, buckets);
    }
}
