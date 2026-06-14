package com.thesis.workout.marketplace.infrastructure.repository;

import com.thesis.workout.template.domain.model.Template;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-only marketplace browse over PUBLIC, active templates joined to their stats and author.
 * Three fixed sort variants (newest / top / trending) avoid unsafe dynamic ORDER BY; the optional
 * filters use CAST(... AS ...) so null bind parameters are typed for Postgres. Per-row "my vote /
 * saved" state is loaded separately in a batch to keep these queries simple.
 */
public interface MarketplaceTemplateRepository extends Repository<Template, UUID> {

    String SELECT = """
            SELECT t.id AS id, t.name AS name, t.description AS description,
                   t.split_type AS splitType, t.difficulty AS difficulty, t.days_per_week AS daysPerWeek,
                   t.estimated_duration_minutes AS estimatedDurationMinutes,
                   to_char(t.published_at, 'YYYY-MM-DD') AS publishedAt,
                   u.display_name AS authorDisplayName,
                   s.upvotes_count AS upvotes, s.downvotes_count AS downvotes,
                   s.saves_count AS saves, s.uses_count AS uses, s.rating_score AS ratingScore
            FROM workout_templates t
            JOIN template_stats s ON s.template_id = t.id
            JOIN app_users u ON u.id = t.user_id
            """;

    String WHERE = """
            WHERE t.visibility = 'PUBLIC' AND t.deleted_at IS NULL
              AND (CAST(:splitType AS varchar) IS NULL OR t.split_type = CAST(:splitType AS varchar))
              AND (CAST(:difficulty AS varchar) IS NULL OR t.difficulty = CAST(:difficulty AS varchar))
              AND (CAST(:daysPerWeek AS integer) IS NULL OR t.days_per_week = CAST(:daysPerWeek AS integer))
              AND (:savedOnly = FALSE OR EXISTS (
                    SELECT 1 FROM template_saves sv WHERE sv.template_id = t.id AND sv.user_id = :userId))
            """;

    @Query(value = SELECT + WHERE
            + " ORDER BY t.published_at DESC, t.id LIMIT :size OFFSET :offset", nativeQuery = true)
    List<MarketplaceRow> browseNewest(@Param("userId") UUID userId, @Param("splitType") String splitType,
            @Param("difficulty") String difficulty, @Param("daysPerWeek") Integer daysPerWeek,
            @Param("savedOnly") boolean savedOnly, @Param("size") int size, @Param("offset") int offset);

    @Query(value = SELECT + WHERE
            + " ORDER BY s.rating_score DESC, t.published_at DESC, t.id LIMIT :size OFFSET :offset",
            nativeQuery = true)
    List<MarketplaceRow> browseTop(@Param("userId") UUID userId, @Param("splitType") String splitType,
            @Param("difficulty") String difficulty, @Param("daysPerWeek") Integer daysPerWeek,
            @Param("savedOnly") boolean savedOnly, @Param("size") int size, @Param("offset") int offset);

    @Query(value = SELECT + WHERE
            + " ORDER BY s.trending_score DESC, t.published_at DESC, t.id LIMIT :size OFFSET :offset",
            nativeQuery = true)
    List<MarketplaceRow> browseTrending(@Param("userId") UUID userId, @Param("splitType") String splitType,
            @Param("difficulty") String difficulty, @Param("daysPerWeek") Integer daysPerWeek,
            @Param("savedOnly") boolean savedOnly, @Param("size") int size, @Param("offset") int offset);

    @Query(value = "SELECT count(*) FROM workout_templates t "
            + "JOIN template_stats s ON s.template_id = t.id JOIN app_users u ON u.id = t.user_id " + WHERE,
            nativeQuery = true)
    long countPublic(@Param("userId") UUID userId, @Param("splitType") String splitType,
            @Param("difficulty") String difficulty, @Param("daysPerWeek") Integer daysPerWeek,
            @Param("savedOnly") boolean savedOnly);

    interface MarketplaceRow {
        UUID getId();

        String getName();

        String getDescription();

        String getSplitType();

        String getDifficulty();

        Integer getDaysPerWeek();

        Integer getEstimatedDurationMinutes();

        String getPublishedAt();

        String getAuthorDisplayName();

        long getUpvotes();

        long getDownvotes();

        long getSaves();

        long getUses();

        BigDecimal getRatingScore();
    }
}
