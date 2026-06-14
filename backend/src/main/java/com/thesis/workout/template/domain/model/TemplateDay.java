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
 * A day within a template. Hard-deleted (no soft delete column); deleting a day cascades to its
 * exercises and routines. {@code day_number} is unique per template.
 */
@Entity
@Table(name = "template_days")
public class TemplateDay {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "focus", length = 40)
    private DayFocus focus;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "notes")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TemplateDay() {
    }

    public static TemplateDay createFor(UUID templateId, int dayNumber, String name, DayFocus focus,
            Integer estimatedDurationMinutes, String notes) {
        TemplateDay day = new TemplateDay();
        day.id = UUID.randomUUID();
        day.templateId = templateId;
        day.dayNumber = dayNumber;
        day.name = name;
        day.focus = focus;
        day.estimatedDurationMinutes = estimatedDurationMinutes;
        day.notes = notes;
        return day;
    }

    public void updateDetails(int dayNumber, String name, DayFocus focus, Integer estimatedDurationMinutes,
            String notes) {
        this.dayNumber = dayNumber;
        this.name = name;
        this.focus = focus;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public String getName() {
        return name;
    }

    public DayFocus getFocus() {
        return focus;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public String getNotes() {
        return notes;
    }
}
