package com.thesis.workout.benchmark;

import com.thesis.workout.search.infrastructure.OpenSearchAdmin;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The OpenSearch side of the benchmark. Indexes the identical synthetic corpus into a dedicated
 * {@code bench_templates} index (reusing the production analyzers/mappings) and runs queries that
 * mirror the real {@code TemplateSearchService} — boosted {@code multi_match}, fuzzy, filtered,
 * faceted — so the comparison against {@link PostgresSearchBaseline} is like-for-like.
 */
@Component
@Profile("benchmark")
@ConditionalOnProperty(name = "app.search.enabled", havingValue = "true")
class OpenSearchBenchmark {

    private static final String INDEX = "bench_templates";
    private static final String MAPPING = "templates_v1.json";
    private static final int BATCH = 3000;
    private static final List<String> TEXT_FIELDS = List.of(
            "name^4", "exerciseNames^3", "muscleGroupsText^2", "description^1");

    private final OpenSearchClient client;
    private final OpenSearchAdmin admin;

    OpenSearchBenchmark(OpenSearchClient client, OpenSearchAdmin admin) {
        this.client = client;
        this.admin = admin;
    }

    /** Drops and recreates a clean index so each corpus size starts empty. */
    void reset() {
        admin.deleteIndexIfExists(INDEX);
        admin.createIndex(INDEX, MAPPING);
    }

    /** Bulk-indexes a batch (split into BATCH-sized bulk requests). */
    void insertBatch(List<SearchDoc> docs) {
        List<BulkOperation> ops = new ArrayList<>(Math.min(BATCH, docs.size()));
        for (SearchDoc doc : docs) {
            Map<String, Object> source = toSource(doc);
            ops.add(BulkOperation.of(op -> op.index(idx -> idx.index(INDEX).id(doc.id()).document(source))));
            if (ops.size() == BATCH) {
                flush(ops);
                ops = new ArrayList<>(BATCH);
            }
        }
        if (!ops.isEmpty()) {
            flush(ops);
        }
    }

    /** Make all indexed docs searchable. */
    void finishLoad() {
        try {
            client.indices().refresh(r -> r.index(INDEX));
        } catch (IOException ex) {
            throw new IllegalStateException("refresh failed", ex);
        }
    }

    private void flush(List<BulkOperation> ops) {
        try {
            client.bulk(b -> b.index(INDEX).operations(ops));
        } catch (IOException ex) {
            throw new IllegalStateException("bulk index failed", ex);
        }
    }

    private Map<String, Object> toSource(SearchDoc doc) {
        Map<String, Object> source = new HashMap<>();
        source.put("templateId", doc.id());
        source.put("ownerUserId", doc.ownerUserId());
        source.put("visibility", doc.visibility());
        source.put("name", doc.name());
        source.put("description", doc.description());
        source.put("splitType", doc.splitType());
        source.put("difficulty", doc.difficulty());
        source.put("daysPerWeek", doc.daysPerWeek());
        source.put("exerciseNames", doc.exerciseNames());
        source.put("muscleGroups", doc.muscleGroups());
        source.put("muscleGroupsText", doc.muscleGroupsText());
        source.put("savesCount", doc.savesCount());
        source.put("usesCount", doc.usesCount());
        source.put("templateStructureScore", doc.structureScore());
        source.put("analysisCategory", doc.analysisCategory());
        return source;
    }

    // --- queries (mirror TemplateSearchService; return hit count, latency measured by harness) ---

    long fullText(String query, int topK) {
        return search(b -> b
                .filter(term("visibility", "PUBLIC"))
                .must(multiMatch(query, false)), topK, false);
    }

    long fuzzy(String query, int topK) {
        return search(b -> b
                .filter(term("visibility", "PUBLIC"))
                .must(multiMatch(query, true)), topK, false);
    }

    long filtered(String query, String difficulty, String splitType, int topK) {
        return search(b -> b
                .filter(term("visibility", "PUBLIC"))
                .filter(term("difficulty", difficulty))
                .filter(term("splitType", splitType))
                .must(multiMatch(query, false)), topK, false);
    }

    /** Faceted: terms aggregation on difficulty over the match set (size=0, returns bucket count). */
    int facetByDifficulty(String query) {
        try {
            SearchResponse<Void> response = client.search(s -> s
                    .index(INDEX)
                    .size(0)
                    .query(q -> q.bool(b -> b
                            .filter(term("visibility", "PUBLIC"))
                            .must(multiMatch(query, false))))
                    .aggregations("difficulty", a -> a.terms(t -> t.field("difficulty").size(10))), Void.class);
            var agg = response.aggregations().get("difficulty");
            return agg != null && agg.isSterms() ? agg.sterms().buckets().array().size() : 0;
        } catch (IOException ex) {
            throw new IllegalStateException("facet query failed", ex);
        }
    }

    private long search(java.util.function.Consumer<org.opensearch.client.opensearch._types.query_dsl.BoolQuery.Builder> bool,
            int topK, boolean trackTotal) {
        try {
            SearchResponse<Void> response = client.search(s -> s
                    .index(INDEX)
                    .size(topK)
                    .trackTotalHits(t -> t.enabled(trackTotal))
                    .query(q -> q.bool(b -> {
                        bool.accept(b);
                        return b;
                    })), Void.class);
            return response.hits().hits().size();
        } catch (IOException ex) {
            throw new IllegalStateException("search failed", ex);
        }
    }

    private Query multiMatch(String query, boolean fuzzy) {
        return Query.of(q -> q.multiMatch(mm -> {
            mm.query(query).fields(TEXT_FIELDS).type(TextQueryType.BestFields);
            if (fuzzy && query.length() >= 3) {
                mm.fuzziness("AUTO").prefixLength(1);
            }
            return mm;
        }));
    }

    private static Query term(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(FieldValue.of(value))));
    }

    long storageBytes() {
        try {
            return client.indices().stats(s -> s.index(INDEX)).all().total().store().sizeInBytes();
        } catch (IOException | RuntimeException ex) {
            return -1L;
        }
    }
}
