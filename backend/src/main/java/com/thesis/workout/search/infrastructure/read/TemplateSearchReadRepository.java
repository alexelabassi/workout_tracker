package com.thesis.workout.search.infrastructure.read;

import com.thesis.workout.template.domain.model.Template;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Read-only helper for the search indexer/rebuild paths. Kept separate from the writable template
 * repository so the indexing concern does not leak into the template feature's repository surface.
 */
public interface TemplateSearchReadRepository extends Repository<Template, UUID> {

    /** All active (non-soft-deleted) template ids — the full set to (re)index, both private and public. */
    @Query(value = "SELECT id FROM workout_templates WHERE deleted_at IS NULL ORDER BY created_at", nativeQuery = true)
    List<UUID> findActiveTemplateIds();
}
