package com.thesis.workout.template.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A private workout program owned by a single user. In Phase 4 templates are always PRIVATE
 * ({@code publishedAt} and {@code copiedFromTemplateId} stay null). The denormalised
 * {@code aggregated_*} array columns are deliberately NOT mapped here: they are recomputed
 * Postgres-side by a native query (see {@code TemplateRepository.recomputeAggregates}) to avoid
 * array/{@code ddl-auto: validate} fragility. Their DB defaults ('{}') apply on insert.
 */
@Entity
@Table(name = "workout_templates")
public class Template {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", length = 40)
    private SplitType splitType;

    @Column(name = "days_per_week")
    private Integer daysPerWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", length = 40)
    private Difficulty difficulty;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 30)
    private TemplateVisibility visibility = TemplateVisibility.PRIVATE;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "copied_from_template_id")
    private UUID copiedFromTemplateId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Template() {
    }

    public static Template createPrivate(UUID userId, String name, String description, SplitType splitType,
            Integer daysPerWeek, Difficulty difficulty, Integer estimatedDurationMinutes) {
        Template template = new Template();
        template.id = UUID.randomUUID();
        template.userId = userId;
        template.name = name;
        template.description = description;
        template.splitType = splitType;
        template.daysPerWeek = daysPerWeek;
        template.difficulty = difficulty;
        template.estimatedDurationMinutes = estimatedDurationMinutes;
        template.visibility = TemplateVisibility.PRIVATE;
        return template;
    }

    /** Creates a new PRIVATE template owned by the copier, linked to its source only via copiedFromTemplateId. */
    public static Template createPrivateCopy(UUID userId, String name, String description, SplitType splitType,
            Integer daysPerWeek, Difficulty difficulty, Integer estimatedDurationMinutes, UUID copiedFromTemplateId) {
        Template template = createPrivate(
                userId, name, description, splitType, daysPerWeek, difficulty, estimatedDurationMinutes);
        template.copiedFromTemplateId = copiedFromTemplateId;
        return template;
    }

    public void updateDetails(String name, String description, SplitType splitType, Integer daysPerWeek,
            Difficulty difficulty, Integer estimatedDurationMinutes) {
        this.name = name;
        this.description = description;
        this.splitType = splitType;
        this.daysPerWeek = daysPerWeek;
        this.difficulty = difficulty;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    /** Makes the template publicly visible in the marketplace. The CHECK requires published_at when PUBLIC. */
    public void publish(Instant when) {
        this.visibility = TemplateVisibility.PUBLIC;
        this.publishedAt = when;
    }

    /** Returns the template to PRIVATE and clears its publish timestamp. */
    public void unpublish() {
        this.visibility = TemplateVisibility.PRIVATE;
        this.publishedAt = null;
    }

    public boolean isPublic() {
        return visibility == TemplateVisibility.PUBLIC;
    }

    public void softDelete(Instant when) {
        if (deletedAt == null) {
            deletedAt = when;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public Integer getDaysPerWeek() {
        return daysPerWeek;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public TemplateVisibility getVisibility() {
        return visibility;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public UUID getCopiedFromTemplateId() {
        return copiedFromTemplateId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
