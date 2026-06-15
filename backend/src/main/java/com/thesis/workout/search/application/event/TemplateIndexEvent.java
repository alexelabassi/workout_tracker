package com.thesis.workout.search.application.event;

import java.util.UUID;

/**
 * Signals that a template's OpenSearch document must be refreshed after the surrounding database
 * transaction commits. The {@link Operation} distinguishes a full structural reindex (which
 * recomputes the analyzer-derived fields) from a cheap counters-only update (vote/save/use) and a
 * removal (soft delete). Published by the template/marketplace services and consumed by the
 * after-commit listener; it carries only the id, never entity state, so the listener always reads
 * the committed truth from PostgreSQL.
 */
public record TemplateIndexEvent(UUID templateId, Operation operation) {

    public enum Operation {
        /** Content/visibility changed: rebuild the whole document including analyzer fields. */
        STRUCTURAL,
        /** Only popularity counters changed: partial update, no analyzer recompute. */
        STATS,
        /** Template was soft-deleted: drop it from the index. */
        REMOVE
    }

    public static TemplateIndexEvent structural(UUID templateId) {
        return new TemplateIndexEvent(templateId, Operation.STRUCTURAL);
    }

    public static TemplateIndexEvent stats(UUID templateId) {
        return new TemplateIndexEvent(templateId, Operation.STATS);
    }

    public static TemplateIndexEvent remove(UUID templateId) {
        return new TemplateIndexEvent(templateId, Operation.REMOVE);
    }
}
