package com.thesis.workout.template.infrastructure.repository;

import com.thesis.workout.template.domain.model.TemplateStats;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemplateStatsRepository extends JpaRepository<TemplateStats, UUID> {

    /**
     * Atomically applies a vote delta and recomputes the derived scores in one statement (no
     * read-modify-write race). Counts are clamped with GREATEST(0, ...) so repeated/raced
     * deletes can never drive a count negative (and never trip the non-negative CHECK).
     * {@code rating_score} = upvotes − downvotes. {@code trending_score} is a Hacker-News-style
     * write-time approximation (net votes decayed by age since publish) — NOT a real-time
     * decaying rank; it only refreshes when a vote changes.
     */
    @Modifying
    @Query(value = """
            UPDATE template_stats SET
              upvotes_count = GREATEST(0, upvotes_count + :upDelta),
              downvotes_count = GREATEST(0, downvotes_count + :downDelta),
              rating_score = GREATEST(0, upvotes_count + :upDelta) - GREATEST(0, downvotes_count + :downDelta),
              trending_score = round(
                (GREATEST(0, upvotes_count + :upDelta) - GREATEST(0, downvotes_count + :downDelta))
                / power(extract(epoch FROM (now() - COALESCE(
                    (SELECT wt.published_at FROM workout_templates wt WHERE wt.id = template_stats.template_id),
                    now()))) / 3600.0 + 2, 1.5), 4),
              updated_at = now()
            WHERE template_id = :templateId
            """, nativeQuery = true)
    void applyVoteDelta(@Param("templateId") UUID templateId,
            @Param("upDelta") int upDelta, @Param("downDelta") int downDelta);

    @Modifying
    @Query(value = """
            UPDATE template_stats
            SET saves_count = GREATEST(0, saves_count + :delta), updated_at = now()
            WHERE template_id = :templateId
            """, nativeQuery = true)
    void applySaveDelta(@Param("templateId") UUID templateId, @Param("delta") int delta);

    @Modifying
    @Query(value = """
            UPDATE template_stats
            SET uses_count = uses_count + 1, updated_at = now()
            WHERE template_id = :templateId
            """, nativeQuery = true)
    void incrementUses(@Param("templateId") UUID templateId);
}
