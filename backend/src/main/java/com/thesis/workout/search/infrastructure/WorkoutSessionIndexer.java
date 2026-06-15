package com.thesis.workout.search.infrastructure;

import com.thesis.workout.search.application.WorkoutSessionDocumentAssembler;
import com.thesis.workout.search.application.document.WorkoutSessionDocument;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Writes workout-session documents to OpenSearch. A session is indexed when it reaches a terminal
 * status (FINISHED/CANCELLED); an in-progress session is not history and is removed if present.
 */
@Component
@ConditionalOnProperty(name = "app.search.enabled", havingValue = "true")
public class WorkoutSessionIndexer {

    private static final String MAPPING_RESOURCE = "workout_sessions_v1.json";

    private final OpenSearchClient client;
    private final OpenSearchAdmin admin;
    private final SearchProperties properties;
    private final WorkoutSessionDocumentAssembler assembler;

    public WorkoutSessionIndexer(OpenSearchClient client, OpenSearchAdmin admin, SearchProperties properties,
            WorkoutSessionDocumentAssembler assembler) {
        this.client = client;
        this.admin = admin;
        this.properties = properties;
        this.assembler = assembler;
    }

    public void index(UUID sessionId) {
        admin.ensureAliasReady(properties.sessionsAlias(), properties.sessionsIndex(), MAPPING_RESOURCE);
        Optional<WorkoutSessionDocument> doc = assembler.assemble(sessionId);
        if (doc.isEmpty()) {
            remove(sessionId);
            return;
        }
        try {
            client.index(i -> i
                    .index(properties.sessionsAlias())
                    .id(sessionId.toString())
                    .document(doc.get())
                    .refresh(Refresh.WaitFor));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to index session " + sessionId, ex);
        }
    }

    public void remove(UUID sessionId) {
        if (!admin.aliasExists(properties.sessionsAlias())) {
            return;
        }
        try {
            client.delete(d -> d.index(properties.sessionsAlias()).id(sessionId.toString()).refresh(Refresh.WaitFor));
        } catch (OpenSearchException ex) {
            // 404 on a never-indexed session is fine.
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to remove session " + sessionId, ex);
        }
    }

    public int bulkIndexInto(String targetIndex, List<UUID> sessionIds) {
        List<BulkOperation> operations = new ArrayList<>();
        for (UUID id : sessionIds) {
            assembler.assemble(id).ifPresent(doc -> operations.add(BulkOperation.of(op -> op
                    .index(idx -> idx.index(targetIndex).id(doc.sessionId()).document(doc)))));
        }
        if (operations.isEmpty()) {
            return 0;
        }
        try {
            client.bulk(b -> b.index(targetIndex).operations(operations));
            return operations.size();
        } catch (IOException ex) {
            throw new IllegalStateException("Bulk session index failed into " + targetIndex, ex);
        }
    }
}
