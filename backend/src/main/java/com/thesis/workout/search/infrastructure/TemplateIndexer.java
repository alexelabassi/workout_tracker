package com.thesis.workout.search.infrastructure;

import com.thesis.workout.search.application.TemplateDocumentAssembler;
import com.thesis.workout.search.application.document.TemplateDocument;
import com.thesis.workout.template.domain.model.TemplateStats;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Writes template documents to OpenSearch. Two distinct upsert paths encode the consistency model:
 * a <em>structural</em> reindex ({@link #index}) rebuilds the whole document including the
 * analyzer-derived structural fields, while {@link #indexStatsOnly} performs a partial update of
 * just the popularity counters so a vote/save/use never recomputes the structural analysis. Writes
 * are made through the stable alias and use {@code refresh=wait_for} so a write is searchable as
 * soon as the call returns (read-after-write), which keeps the demo and tests deterministic.
 */
@Component
@ConditionalOnProperty(name = "app.search.enabled", havingValue = "true")
public class TemplateIndexer {

    private static final String MAPPING_RESOURCE = "templates_v1.json";

    private final OpenSearchClient client;
    private final OpenSearchAdmin admin;
    private final SearchProperties properties;
    private final TemplateDocumentAssembler assembler;

    public TemplateIndexer(OpenSearchClient client, OpenSearchAdmin admin, SearchProperties properties,
            TemplateDocumentAssembler assembler) {
        this.client = client;
        this.admin = admin;
        this.properties = properties;
        this.assembler = assembler;
    }

    /** Full structural (re)index of one template; if it has been deleted, removes it instead. */
    public void index(UUID templateId) {
        admin.ensureAliasReady(properties.templatesAlias(), properties.templatesIndex(), MAPPING_RESOURCE);
        Optional<TemplateDocument> doc = assembler.assemble(templateId);
        if (doc.isEmpty()) {
            remove(templateId);
            return;
        }
        try {
            client.index(i -> i
                    .index(properties.templatesAlias())
                    .id(templateId.toString())
                    .document(doc.get())
                    .refresh(Refresh.WaitFor));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to index template " + templateId, ex);
        }
    }

    /** Partial update of popularity counters only; falls back to a full reindex if the doc is absent. */
    public void indexStatsOnly(UUID templateId) {
        Optional<TemplateStats> stats = assembler.loadStats(templateId);
        if (stats.isEmpty()) {
            return;
        }
        TemplateStats s = stats.get();
        Map<String, Object> partial = Map.of(
                "ratingScore", s.getRatingScore().doubleValue(),
                "upvotesCount", s.getUpvotesCount(),
                "downvotesCount", s.getDownvotesCount(),
                "savesCount", s.getSavesCount(),
                "usesCount", s.getUsesCount());
        admin.ensureAliasReady(properties.templatesAlias(), properties.templatesIndex(), MAPPING_RESOURCE);
        try {
            client.update(u -> u
                    .index(properties.templatesAlias())
                    .id(templateId.toString())
                    .doc(partial)
                    .refresh(Refresh.WaitFor), Void.class);
        } catch (OpenSearchException ex) {
            // Most likely document_missing (e.g. search was enabled after this template was created):
            // recover by building the full document once.
            index(templateId);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to update template stats " + templateId, ex);
        }
    }

    public void remove(UUID templateId) {
        if (!admin.aliasExists(properties.templatesAlias())) {
            return;
        }
        try {
            client.delete(d -> d.index(properties.templatesAlias()).id(templateId.toString()).refresh(Refresh.WaitFor));
        } catch (OpenSearchException ex) {
            // index_not_found / 404 on a never-indexed template is fine.
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to remove template " + templateId, ex);
        }
    }

    /** Bulk-builds and indexes the given templates into an explicit (rebuild) index. Returns the count written. */
    public int bulkIndexInto(String targetIndex, List<UUID> templateIds) {
        List<BulkOperation> operations = new ArrayList<>();
        for (UUID id : templateIds) {
            assembler.assemble(id).ifPresent(doc -> operations.add(BulkOperation.of(op -> op
                    .index(idx -> idx.index(targetIndex).id(doc.templateId()).document(doc)))));
        }
        if (operations.isEmpty()) {
            return 0;
        }
        try {
            client.bulk(b -> b.index(targetIndex).operations(operations));
            return operations.size();
        } catch (IOException ex) {
            throw new IllegalStateException("Bulk template index failed into " + targetIndex, ex);
        }
    }
}
