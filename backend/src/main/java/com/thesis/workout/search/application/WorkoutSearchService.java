package com.thesis.workout.search.application;

import com.thesis.workout.search.application.document.WorkoutSessionDocument;
import com.thesis.workout.search.application.exception.SearchUnavailableException;
import com.thesis.workout.search.infrastructure.SearchProperties;
import com.thesis.workout.search.web.dto.SearchFacetResponse;
import com.thesis.workout.search.web.dto.SearchResultsResponse;
import com.thesis.workout.search.web.dto.WorkoutSearchItemResponse;
import com.thesis.workout.session.domain.model.SessionStatus;
import com.thesis.workout.session.domain.model.WorkoutSession;
import com.thesis.workout.session.infrastructure.repository.WorkoutSessionRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.aggregations.CalendarInterval;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Workout-history search over the {@code workout_sessions} alias. Always owner-scoped: the owner id
 * is taken from the authenticated principal and added as a mandatory filter, so a user can never
 * see another user's training history regardless of request params. Demonstrates boosted multi-field
 * full-text with typo tolerance over immutable snapshots, structured filters (status / date range /
 * muscle / exercise / gym / equipment / volume / duration), terms + date-histogram facets, and
 * highlighting. Hits are re-validated against PostgreSQL as defense in depth.
 */
@Service
@ConditionalOnProperty(name = "app.search.enabled", havingValue = "true")
public class WorkoutSearchService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final List<String> TEXT_FIELDS = List.of(
            "exerciseNameSnapshots^4", "templateNameSnapshot^2.5", "gymNameSnapshot^2",
            "equipmentNameSnapshots^1.5", "muscleGroupsText^1.5", "templateDayNameSnapshot^1", "notes^1");

    private final OpenSearchClient client;
    private final SearchProperties properties;
    private final WorkoutSessionRepository sessionRepository;

    public WorkoutSearchService(OpenSearchClient client, SearchProperties properties,
            WorkoutSessionRepository sessionRepository) {
        this.client = client;
        this.properties = properties;
        this.sessionRepository = sessionRepository;
    }

    public SearchResultsResponse<WorkoutSearchItemResponse> search(UUID userId, String q, String status,
            String dateFrom, String dateTo, String muscleGroup, String exercise, String gym, String equipment,
            Double minVolume, Double maxVolume, Long minDuration, Long maxDuration, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int from = safePage * safeSize;

        List<Query> filters = new ArrayList<>();
        // Mandatory owner scope — never sourced from request params.
        filters.add(term("ownerUserId", userId.toString()));
        if (hasText(status)) {
            filters.add(term("status", status));
        }
        if (hasText(dateFrom) || hasText(dateTo)) {
            filters.add(Query.of(qq -> qq.range(r -> {
                r.field("startedAt");
                if (hasText(dateFrom)) {
                    r.gte(JsonData.of(dateFrom));
                }
                if (hasText(dateTo)) {
                    r.lte(JsonData.of(dateTo));
                }
                return r;
            })));
        }
        if (hasText(muscleGroup)) {
            filters.add(term("muscleGroups", muscleGroup));
        }
        if (hasText(exercise)) {
            filters.add(term("exerciseNameSnapshots.keyword", exercise));
        }
        if (hasText(gym)) {
            filters.add(term("gymNameSnapshot.keyword", gym));
        }
        if (hasText(equipment)) {
            filters.add(term("equipmentNameSnapshots.keyword", equipment));
        }
        if (minVolume != null || maxVolume != null) {
            filters.add(Query.of(qq -> qq.range(r -> {
                r.field("totalVolume");
                if (minVolume != null) {
                    r.gte(JsonData.of(minVolume));
                }
                if (maxVolume != null) {
                    r.lte(JsonData.of(maxVolume));
                }
                return r;
            })));
        }
        if (minDuration != null || maxDuration != null) {
            filters.add(Query.of(qq -> qq.range(r -> {
                r.field("durationSeconds");
                if (minDuration != null) {
                    r.gte(JsonData.of(minDuration));
                }
                if (maxDuration != null) {
                    r.lte(JsonData.of(maxDuration));
                }
                return r;
            })));
        }

        String queryText = q == null ? "" : q.trim();
        boolean hasQuery = !queryText.isEmpty();

        SearchResponse<WorkoutSessionDocument> response;
        try {
            response = client.search(s -> {
                s.index(properties.sessionsAlias())
                        .from(from)
                        .size(safeSize)
                        // Tolerate a not-yet-built index/alias: return zero hits rather than erroring.
                        .ignoreUnavailable(true)
                        .allowNoIndices(true)
                        .trackTotalHits(t -> t.enabled(true));
                s.query(qb -> qb.bool(b -> {
                    filters.forEach(b::filter);
                    b.must(hasQuery ? textQuery(queryText) : Query.of(m -> m.matchAll(ma -> ma)));
                    return b;
                }));
                // With a query, rank by relevance (default _score). Browsing (no query) is recency-first.
                if (!hasQuery) {
                    s.sort(so -> so.field(f -> f.field("startedAt").order(
                            org.opensearch.client.opensearch._types.SortOrder.Desc)));
                }
                s.aggregations("status", a -> a.terms(t -> t.field("status").size(5)));
                s.aggregations("gym", a -> a.terms(t -> t.field("gymNameSnapshot.keyword").size(30)));
                s.aggregations("muscleGroups", a -> a.terms(t -> t.field("muscleGroups").size(30)));
                s.aggregations("byMonth", a -> a.dateHistogram(d -> d
                        .field("startedAt").calendarInterval(CalendarInterval.Month).minDocCount(1)));
                if (hasQuery) {
                    s.highlight(h -> h.preTags("<mark>").postTags("</mark>")
                            .fields("exerciseNameSnapshots", f -> f)
                            .fields("notes", f -> f)
                            .fields("gymNameSnapshot", f -> f));
                }
                return s;
            }, WorkoutSessionDocument.class);
        } catch (OpenSearchException ex) {
            // 404 = the index/alias has not been built yet (nothing indexed): an empty result set,
            // not an outage. Anything else means OpenSearch is genuinely unhealthy.
            if (ex.status() == 404) {
                return new SearchResultsResponse<>(List.of(), List.of(), safePage, safeSize, 0);
            }
            throw new SearchUnavailableException();
        } catch (IOException ex) {
            throw new SearchUnavailableException();
        }

        List<Hit<WorkoutSessionDocument>> hits = response.hits().hits();
        List<UUID> ids = hits.stream().map(h -> UUID.fromString(h.source().sessionId())).toList();

        // Defense-in-depth: only return sessions the caller owns that are still terminal in PostgreSQL.
        Set<UUID> valid = sessionRepository.findAllById(ids).stream()
                .filter(ses -> ses.getUserId().equals(userId))
                .filter(ses -> ses.getStatus() != SessionStatus.IN_PROGRESS)
                .map(WorkoutSession::getId)
                .collect(Collectors.toSet());

        List<WorkoutSearchItemResponse> items = new ArrayList<>();
        for (Hit<WorkoutSessionDocument> hit : hits) {
            UUID id = UUID.fromString(hit.source().sessionId());
            if (!valid.contains(id)) {
                continue;
            }
            WorkoutSessionDocument doc = hit.source();
            items.add(new WorkoutSearchItemResponse(
                    doc.sessionId(), doc.status(), doc.startedAt(), doc.finishedAt(), doc.durationSeconds(),
                    doc.templateNameSnapshot(), doc.templateDayNameSnapshot(), doc.gymNameSnapshot(),
                    doc.exerciseNameSnapshots(), doc.muscleGroups(), doc.equipmentNameSnapshots(),
                    doc.totalVolume(), doc.setCount(), doc.exerciseCount(),
                    hit.score(), hit.highlight()));
        }

        List<SearchFacetResponse> facets = List.of(
                SearchAggregations.terms("status", response.aggregations()),
                SearchAggregations.terms("gym", response.aggregations()),
                SearchAggregations.terms("muscleGroups", response.aggregations()),
                SearchAggregations.dateHistogram("byMonth", response.aggregations()));

        return new SearchResultsResponse<>(items, facets, safePage, safeSize, response.hits().total().value());
    }

    private Query textQuery(String queryText) {
        return Query.of(q -> q.multiMatch(mm -> {
            mm.query(queryText).fields(TEXT_FIELDS).type(TextQueryType.BestFields);
            if (queryText.length() >= 3) {
                mm.fuzziness("AUTO").prefixLength(1);
            }
            return mm;
        }));
    }

    private static Query term(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(FieldValue.of(value))));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
