package com.thesis.workout.search.application;

import com.thesis.workout.search.infrastructure.OpenSearchAdmin;
import com.thesis.workout.search.infrastructure.SearchProperties;
import com.thesis.workout.search.infrastructure.TemplateIndexer;
import com.thesis.workout.search.infrastructure.WorkoutSessionIndexer;
import com.thesis.workout.search.infrastructure.read.SessionSearchReadRepository;
import com.thesis.workout.search.infrastructure.read.TemplateSearchReadRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Rebuilds the OpenSearch indexes from the authoritative PostgreSQL state. Because the index is a
 * derived, eventually-consistent read model, it can always be reconstructed: this is how the system
 * recovers from indexing lag, an OpenSearch outage, or a mapping/analyzer change.
 *
 * <p>Each index is rebuilt into a brand-new concrete index and the stable alias is atomically
 * repointed (zero read downtime), after which the old index is dropped. Reads always go through the
 * alias, so single-document indexing keeps working before and after a rebuild.</p>
 */
@Service
@ConditionalOnProperty(name = "app.search.enabled", havingValue = "true")
public class SearchRebuildService {

    private static final Logger log = LoggerFactory.getLogger(SearchRebuildService.class);

    private final OpenSearchAdmin admin;
    private final SearchProperties properties;
    private final TemplateIndexer templateIndexer;
    private final WorkoutSessionIndexer sessionIndexer;
    private final TemplateSearchReadRepository templateReadRepository;
    private final SessionSearchReadRepository sessionReadRepository;

    public SearchRebuildService(OpenSearchAdmin admin, SearchProperties properties,
            TemplateIndexer templateIndexer, WorkoutSessionIndexer sessionIndexer,
            TemplateSearchReadRepository templateReadRepository,
            SessionSearchReadRepository sessionReadRepository) {
        this.admin = admin;
        this.properties = properties;
        this.templateIndexer = templateIndexer;
        this.sessionIndexer = sessionIndexer;
        this.templateReadRepository = templateReadRepository;
        this.sessionReadRepository = sessionReadRepository;
    }

    public RebuildResult rebuildAll() {
        int templates = rebuildTemplates();
        int sessions = rebuildSessions();
        return new RebuildResult(templates, sessions);
    }

    public int rebuildTemplates() {
        List<UUID> ids = templateReadRepository.findActiveTemplateIds();
        return rebuild(properties.templatesAlias(), properties.templatesIndex(), "templates_v1.json",
                target -> templateIndexer.bulkIndexInto(target, ids));
    }

    public int rebuildSessions() {
        List<UUID> ids = sessionReadRepository.findIndexableSessionIds();
        return rebuild(properties.sessionsAlias(), properties.sessionsIndex(), "workout_sessions_v1.json",
                target -> sessionIndexer.bulkIndexInto(target, ids));
    }

    private int rebuild(String alias, String baseIndex, String mappingResource, BulkInto bulk) {
        String newIndex = baseIndex + "_" + System.currentTimeMillis();
        Set<String> previous = admin.indicesBehindAlias(alias);
        admin.createIndex(newIndex, mappingResource);
        int count;
        try {
            count = bulk.into(newIndex);
            admin.refresh(newIndex);
            admin.swapAlias(alias, newIndex, previous);
        } catch (RuntimeException ex) {
            // The new index could not be populated/aliased: drop it so a partial index is never
            // left behind, then rethrow. The previous index (still behind the alias) keeps serving.
            admin.deleteIndexIfExists(newIndex);
            throw ex;
        }
        previous.stream()
                .filter(old -> !old.equals(newIndex))
                .forEach(admin::deleteIndexIfExists);
        log.info("Rebuilt alias '{}' into '{}' with {} documents (dropped {})",
                alias, newIndex, count, previous);
        return count;
    }

    @FunctionalInterface
    private interface BulkInto {
        int into(String targetIndex);
    }

    public record RebuildResult(int templates, int sessions) {
    }
}
