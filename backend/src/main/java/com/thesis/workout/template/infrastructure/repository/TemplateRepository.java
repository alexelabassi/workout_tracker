package com.thesis.workout.template.infrastructure.repository;

import com.thesis.workout.template.domain.model.Template;
import com.thesis.workout.template.domain.model.TemplateVisibility;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemplateRepository extends JpaRepository<Template, UUID> {

    List<Template> findByUserIdAndDeletedAtIsNullOrderByNameAsc(UUID userId);

    Optional<Template> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    /** Loads a template only if it is public and active — the marketplace read guard. */
    Optional<Template> findByIdAndVisibilityAndDeletedAtIsNull(UUID id, TemplateVisibility visibility);

    /**
     * Recomputes the denormalised aggregate arrays from the template's child rows, entirely
     * Postgres-side (no array binding from Java). Empty results fall back to explicitly-cast
     * empty arrays, and each aggregate is ordered deterministically. Aggregates draw from the
     * exercise snapshots (names, muscle groups survive source deletion); official exercise ids
     * are filtered against the live exercises table. {@code flushAutomatically} ensures pending
     * child writes are visible; {@code clearAutomatically} drops now-stale managed templates.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE workout_templates t SET
              aggregated_muscle_groups = COALESCE((
                SELECT array_agg(DISTINCT mg.muscle_group_code ORDER BY mg.muscle_group_code)
                FROM template_days d
                JOIN template_day_exercises e ON e.template_day_id = d.id
                JOIN template_day_exercise_muscle_groups mg ON mg.template_day_exercise_id = e.id
                WHERE d.template_id = t.id), '{}'::varchar[]),
              aggregated_official_exercise_ids = COALESCE((
                SELECT array_agg(DISTINCT e.exercise_id ORDER BY e.exercise_id)
                FROM template_days d
                JOIN template_day_exercises e ON e.template_day_id = d.id
                WHERE d.template_id = t.id
                  AND e.exercise_id IN (SELECT id FROM exercises WHERE visibility = 'OFFICIAL' AND deleted_at IS NULL)
                ), '{}'::uuid[]),
              aggregated_exercise_names = COALESCE((
                SELECT array_agg(DISTINCT e.exercise_name_snapshot::text ORDER BY e.exercise_name_snapshot::text)
                FROM template_days d
                JOIN template_day_exercises e ON e.template_day_id = d.id
                WHERE d.template_id = t.id), '{}'::text[]),
              updated_at = now()
            WHERE t.id = :templateId
            """, nativeQuery = true)
    void recomputeAggregates(@Param("templateId") UUID templateId);
}
