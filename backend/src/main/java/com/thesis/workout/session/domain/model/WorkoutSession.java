package com.thesis.workout.session.domain.model;

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
 * A live (or completed) workout. All template/gym references are nullable with ON DELETE SET
 * NULL, and every meaningful value is snapshotted, so the session is immutable history: editing
 * or deleting the source template/day/gym never changes what was recorded. Sessions are never
 * physically deleted — cancellation is a status, not a row removal.
 */
@Entity
@Table(name = "workout_sessions")
public class WorkoutSession {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "template_day_id")
    private UUID templateDayId;

    @Column(name = "template_name_snapshot", length = 180)
    private String templateNameSnapshot;

    @Column(name = "template_day_name_snapshot", length = 160)
    private String templateDayNameSnapshot;

    @Column(name = "gym_id")
    private UUID gymId;

    @Column(name = "gym_name_snapshot", length = 160)
    private String gymNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SessionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "notes")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkoutSession() {
    }

    public static WorkoutSession start(UUID userId, UUID templateId, UUID templateDayId,
            String templateNameSnapshot, String templateDayNameSnapshot, UUID gymId, String gymNameSnapshot) {
        WorkoutSession session = new WorkoutSession();
        session.id = UUID.randomUUID();
        session.userId = userId;
        session.templateId = templateId;
        session.templateDayId = templateDayId;
        session.templateNameSnapshot = templateNameSnapshot;
        session.templateDayNameSnapshot = templateDayNameSnapshot;
        session.gymId = gymId;
        session.gymNameSnapshot = gymNameSnapshot;
        session.status = SessionStatus.IN_PROGRESS;
        session.startedAt = Instant.now();
        return session;
    }

    public void finish(Instant when) {
        this.status = SessionStatus.FINISHED;
        this.finishedAt = when;
    }

    public void cancel(Instant when) {
        this.status = SessionStatus.CANCELLED;
        this.finishedAt = when;
    }

    public boolean isActive() {
        return status == SessionStatus.IN_PROGRESS;
    }

    /** User-authored commentary on the workout; editable any time the session is owned. */
    public void updateNotes(String notes) {
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public UUID getTemplateDayId() {
        return templateDayId;
    }

    public String getTemplateNameSnapshot() {
        return templateNameSnapshot;
    }

    public String getTemplateDayNameSnapshot() {
        return templateDayNameSnapshot;
    }

    public UUID getGymId() {
        return gymId;
    }

    public String getGymNameSnapshot() {
        return gymNameSnapshot;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public String getNotes() {
        return notes;
    }
}
