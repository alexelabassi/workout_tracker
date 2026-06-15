package com.thesis.workout.search.application;

import com.thesis.workout.auth.domain.model.AppUser;
import com.thesis.workout.auth.infrastructure.repository.AppUserRepository;
import com.thesis.workout.marketplace.domain.model.TemplateVote;
import com.thesis.workout.marketplace.domain.model.VoteType;
import com.thesis.workout.marketplace.infrastructure.repository.TemplateSaveRepository;
import com.thesis.workout.marketplace.infrastructure.repository.TemplateVoteRepository;
import com.thesis.workout.search.application.document.TemplateDocument;
import com.thesis.workout.search.application.exception.SearchUnavailableException;
import com.thesis.workout.search.infrastructure.SearchProperties;
import com.thesis.workout.search.web.dto.SearchFacetResponse;
import com.thesis.workout.search.web.dto.SearchResultsResponse;
import com.thesis.workout.search.web.dto.TemplateSearchItemResponse;
import com.thesis.workout.template.domain.model.Template;
import com.thesis.workout.template.domain.model.TemplateVisibility;
import com.thesis.workout.template.infrastructure.repository.TemplateRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.query_dsl.FieldValueFactorModifier;
import org.opensearch.client.opensearch._types.query_dsl.FunctionBoostMode;
import org.opensearch.client.opensearch._types.query_dsl.FunctionScoreMode;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.json.JsonData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Template / marketplace search over the {@code templates} alias. Demonstrates boosted multi-field
 * full-text with typo tolerance, structured filters, terms facets, highlighting and (for the
 * marketplace scope) a popularity {@code function_score}. Security is enforced as a mandatory
 * filter clause that cannot be overridden by request params: {@code scope=marketplace} only ever
 * matches PUBLIC templates, {@code scope=my} only ever matches the caller's own. As defense in
 * depth the returned page of ids is re-validated against PostgreSQL before anything is returned, so
 * a hit that became stale after indexing (e.g. unpublished/deleted) is dropped.
 */
@Service
@ConditionalOnProperty(name = "app.search.enabled", havingValue = "true")
public class TemplateSearchService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final List<String> TEXT_FIELDS = List.of(
            "name^4", "exerciseNames^3", "muscleGroupsText^2", "dayNames^1.5",
            "description^1", "routineNameSnapshots^0.5", "routineContentSnapshots^0.5");
    private static final String FALLBACK_AUTHOR = "Community user";

    private final OpenSearchClient client;
    private final SearchProperties properties;
    private final TemplateRepository templateRepository;
    private final AppUserRepository appUserRepository;
    private final TemplateVoteRepository voteRepository;
    private final TemplateSaveRepository saveRepository;

    public TemplateSearchService(OpenSearchClient client, SearchProperties properties,
            TemplateRepository templateRepository, AppUserRepository appUserRepository,
            TemplateVoteRepository voteRepository, TemplateSaveRepository saveRepository) {
        this.client = client;
        this.properties = properties;
        this.templateRepository = templateRepository;
        this.appUserRepository = appUserRepository;
        this.voteRepository = voteRepository;
        this.saveRepository = saveRepository;
    }

    public SearchResultsResponse<TemplateSearchItemResponse> search(UUID userId, String scope, String q,
            String difficulty, String splitType, Integer daysPerWeek, String muscleGroup, String analysisCategory,
            Integer minScore, int page, int size) {
        boolean marketplace = "marketplace".equalsIgnoreCase(scope);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int from = safePage * safeSize;

        List<Query> filters = new ArrayList<>();
        // Mandatory security predicate — derived from the JWT, never from request params.
        if (marketplace) {
            filters.add(term("visibility", TemplateVisibility.PUBLIC.name()));
        } else {
            filters.add(term("ownerUserId", userId.toString()));
        }
        if (hasText(difficulty)) {
            filters.add(term("difficulty", difficulty));
        }
        if (hasText(splitType)) {
            filters.add(term("splitType", splitType));
        }
        if (daysPerWeek != null) {
            filters.add(Query.of(qq -> qq.term(
                    t -> t.field("daysPerWeek").value(FieldValue.of(daysPerWeek.longValue())))));
        }
        if (hasText(muscleGroup)) {
            filters.add(term("muscleGroups", muscleGroup));
        }
        if (hasText(analysisCategory)) {
            filters.add(term("analysisCategory", analysisCategory));
        }
        if (minScore != null) {
            filters.add(Query.of(qq -> qq.range(r -> r.field("templateStructureScore").gte(JsonData.of(minScore)))));
        }

        String queryText = q == null ? "" : q.trim();
        boolean hasQuery = !queryText.isEmpty();

        SearchResponse<TemplateDocument> response;
        try {
            response = client.search(s -> {
                s.index(properties.templatesAlias())
                        .from(from)
                        .size(safeSize)
                        // Tolerate a not-yet-built index/alias: return zero hits rather than erroring.
                        .ignoreUnavailable(true)
                        .allowNoIndices(true)
                        .trackTotalHits(t -> t.enabled(true));
                s.query(qb -> qb.bool(b -> {
                    filters.forEach(b::filter);
                    Query inner = hasQuery ? textQuery(queryText) : Query.of(m -> m.matchAll(ma -> ma));
                    b.must(marketplace ? popularityBoost(inner) : inner);
                    return b;
                }));
                s.aggregations("difficulty", a -> a.terms(t -> t.field("difficulty").size(10)));
                s.aggregations("splitType", a -> a.terms(t -> t.field("splitType").size(20)));
                s.aggregations("daysPerWeek", a -> a.terms(t -> t.field("daysPerWeek").size(10)));
                s.aggregations("muscleGroups", a -> a.terms(t -> t.field("muscleGroups").size(30)));
                s.aggregations("analysisCategory", a -> a.terms(t -> t.field("analysisCategory").size(5)));
                if (hasQuery) {
                    s.highlight(h -> h.preTags("<mark>").postTags("</mark>")
                            .fields("name", f -> f)
                            .fields("description", f -> f)
                            .fields("exerciseNames", f -> f));
                }
                return s;
            }, TemplateDocument.class);
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

        List<Hit<TemplateDocument>> hits = response.hits().hits();
        List<UUID> ids = hits.stream()
                .map(h -> UUID.fromString(h.source().templateId()))
                .toList();

        // Defense-in-depth: re-validate the page against PostgreSQL with the same security rule.
        Map<UUID, Template> byId = templateRepository.findAllById(ids).stream()
                .filter(t -> t.getDeletedAt() == null)
                .filter(t -> marketplace ? t.isPublic() : t.getUserId().equals(userId))
                .collect(Collectors.toMap(Template::getId, t -> t));

        List<UUID> validIds = ids.stream().filter(byId::containsKey).toList();
        Map<UUID, VoteType> myVotes = marketplace && !validIds.isEmpty()
                ? voteRepository.findByUserAndTemplateIds(userId, validIds).stream()
                        .collect(Collectors.toMap(v -> v.getId().getTemplateId(), TemplateVote::getVoteType))
                : Map.of();
        Set<UUID> saved = marketplace && !validIds.isEmpty()
                ? Set.copyOf(saveRepository.findSavedTemplateIds(userId, validIds))
                : Set.of();
        Map<UUID, String> authorNames = marketplace ? resolveAuthors(byId.values()) : Map.of();

        List<TemplateSearchItemResponse> items = new ArrayList<>();
        for (Hit<TemplateDocument> hit : hits) {
            UUID id = UUID.fromString(hit.source().templateId());
            if (!byId.containsKey(id)) {
                continue;
            }
            TemplateDocument doc = hit.source();
            items.add(new TemplateSearchItemResponse(
                    doc.templateId(), doc.name(), doc.description(), doc.visibility(), doc.splitType(),
                    doc.difficulty(), doc.daysPerWeek(), doc.estimatedDurationMinutes(), doc.muscleGroups(),
                    doc.exerciseNames(), doc.ratingScore(), doc.savesCount(), doc.usesCount(),
                    doc.templateStructureScore(), doc.analysisCategory(),
                    marketplace ? authorNames.getOrDefault(byId.get(id).getUserId(), FALLBACK_AUTHOR) : null,
                    myVotes.containsKey(id) ? myVotes.get(id).name() : null,
                    saved.contains(id),
                    hit.score(),
                    hit.highlight()));
        }

        List<SearchFacetResponse> facets = List.of(
                SearchAggregations.terms("difficulty", response.aggregations()),
                SearchAggregations.terms("splitType", response.aggregations()),
                SearchAggregations.terms("daysPerWeek", response.aggregations()),
                SearchAggregations.terms("muscleGroups", response.aggregations()),
                SearchAggregations.terms("analysisCategory", response.aggregations()));

        return new SearchResultsResponse<>(items, facets, safePage, safeSize, response.hits().total().value());
    }

    private Query textQuery(String queryText) {
        return Query.of(q -> q.multiMatch(mm -> {
            mm.query(queryText).fields(TEXT_FIELDS).type(TextQueryType.BestFields);
            // Typo tolerance, but only for queries long enough that fuzzy matching is not noise.
            if (queryText.length() >= 3) {
                mm.fuzziness("AUTO").prefixLength(1);
            }
            return mm;
        }));
    }

    private Query popularityBoost(Query inner) {
        return Query.of(q -> q.functionScore(fs -> fs
                .query(inner)
                .functions(fn -> fn.fieldValueFactor(fv -> fv
                        .field("savesCount").factor(1.0).modifier(FieldValueFactorModifier.Log1p).missing(0.0)))
                .functions(fn -> fn.fieldValueFactor(fv -> fv
                        .field("usesCount").factor(0.7).modifier(FieldValueFactorModifier.Log1p).missing(0.0)))
                .scoreMode(FunctionScoreMode.Sum)
                .boostMode(FunctionBoostMode.Sum)));
    }

    private Map<UUID, String> resolveAuthors(Iterable<Template> templates) {
        Set<UUID> ownerIds = new java.util.HashSet<>();
        templates.forEach(t -> ownerIds.add(t.getUserId()));
        Map<UUID, String> names = new LinkedHashMap<>();
        appUserRepository.findAllById(ownerIds)
                .forEach(user -> names.put(user.getId(), displayName(user)));
        return names;
    }

    private static String displayName(AppUser user) {
        String name = user.getDisplayName();
        return name == null || name.isBlank() ? FALLBACK_AUTHOR : name;
    }

    private static Query term(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(FieldValue.of(value))));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
