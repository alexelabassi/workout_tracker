package com.thesis.workout.routine.domain.model;

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
 * A reusable START (warm-up) or END (cool-down) routine owned by a single user. {@code content}
 * is free text. Uniqueness is enforced per (user, type, name) while not soft-deleted.
 */
@Entity
@Table(name = "routines")
public class Routine {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "routine_type", nullable = false, length = 30)
    private RoutineType routineType;

    @Column(name = "content", nullable = false)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Routine() {
    }

    public static Routine createFor(UUID userId, String name, RoutineType routineType, String content) {
        Routine routine = new Routine();
        routine.id = UUID.randomUUID();
        routine.userId = userId;
        routine.name = name;
        routine.routineType = routineType;
        routine.content = content;
        return routine;
    }

    public void updateDetails(String name, RoutineType routineType, String content) {
        this.name = name;
        this.routineType = routineType;
        this.content = content;
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

    public RoutineType getRoutineType() {
        return routineType;
    }

    public String getContent() {
        return content;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
