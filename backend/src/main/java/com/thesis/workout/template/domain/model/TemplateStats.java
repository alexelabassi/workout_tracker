package com.thesis.workout.template.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Per-template counters. Created zeroed alongside the template; the counters themselves are not
 * exercised in Phase 4 (no votes/saves/uses yet), but the row must exist for later phases.
 */
@Entity
@Table(name = "template_stats")
public class TemplateStats {

    @Id
    @Column(name = "template_id", nullable = false, updatable = false)
    private UUID templateId;

    @Column(name = "upvotes_count", nullable = false)
    private int upvotesCount;

    @Column(name = "downvotes_count", nullable = false)
    private int downvotesCount;

    @Column(name = "saves_count", nullable = false)
    private int savesCount;

    @Column(name = "uses_count", nullable = false)
    private int usesCount;

    @Column(name = "rating_score", nullable = false, precision = 12, scale = 4)
    private BigDecimal ratingScore = BigDecimal.ZERO;

    @Column(name = "trending_score", nullable = false, precision = 12, scale = 4)
    private BigDecimal trendingScore = BigDecimal.ZERO;

    @Column(name = "lock_version", nullable = false)
    private int lockVersion;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TemplateStats() {
    }

    public static TemplateStats zeroFor(UUID templateId) {
        TemplateStats stats = new TemplateStats();
        stats.templateId = templateId;
        stats.ratingScore = BigDecimal.ZERO;
        stats.trendingScore = BigDecimal.ZERO;
        return stats;
    }

    public UUID getTemplateId() {
        return templateId;
    }
}
