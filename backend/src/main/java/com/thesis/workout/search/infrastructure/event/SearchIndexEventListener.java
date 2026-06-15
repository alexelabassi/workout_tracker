package com.thesis.workout.search.infrastructure.event;

import com.thesis.workout.search.application.event.TemplateIndexEvent;
import com.thesis.workout.search.application.event.WorkoutSessionIndexEvent;
import com.thesis.workout.search.infrastructure.TemplateIndexer;
import com.thesis.workout.search.infrastructure.WorkoutSessionIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Keeps the OpenSearch read model eventually consistent with PostgreSQL. Listeners fire only
 * {@link TransactionPhase#AFTER_COMMIT}, so the index never reflects rolled-back work and the
 * indexer always reads committed state. This is intentionally <em>not</em> a transactional outbox:
 * if OpenSearch is briefly unavailable the failure is logged and swallowed (PostgreSQL stays the
 * source of truth), and the admin reindex endpoint can rebuild the index from scratch.
 *
 * <p>The whole listener bean only exists when {@code app.search.enabled=true}; when search is off,
 * the events are published into the void and indexing is a no-op.</p>
 */
@Component
@ConditionalOnProperty(name = "app.search.enabled", havingValue = "true")
public class SearchIndexEventListener {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexEventListener.class);

    private final TemplateIndexer templateIndexer;
    private final WorkoutSessionIndexer workoutSessionIndexer;

    public SearchIndexEventListener(TemplateIndexer templateIndexer, WorkoutSessionIndexer workoutSessionIndexer) {
        this.templateIndexer = templateIndexer;
        this.workoutSessionIndexer = workoutSessionIndexer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTemplateChanged(TemplateIndexEvent event) {
        try {
            switch (event.operation()) {
                case STRUCTURAL -> templateIndexer.index(event.templateId());
                case STATS -> templateIndexer.indexStatsOnly(event.templateId());
                case REMOVE -> templateIndexer.remove(event.templateId());
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to index template {} ({}); PostgreSQL remains authoritative, reindex to recover",
                    event.templateId(), event.operation(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionChanged(WorkoutSessionIndexEvent event) {
        try {
            switch (event.operation()) {
                case UPSERT -> workoutSessionIndexer.index(event.sessionId());
                case REMOVE -> workoutSessionIndexer.remove(event.sessionId());
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to index session {} ({}); PostgreSQL remains authoritative, reindex to recover",
                    event.sessionId(), event.operation(), ex);
        }
    }
}
