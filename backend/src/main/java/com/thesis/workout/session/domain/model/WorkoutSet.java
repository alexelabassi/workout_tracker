package com.thesis.workout.session.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A logged set within a session exercise. Equipment is optional; when present its name is
 * snapshotted so the set survives equipment edits/deletes. {@code completedAt} is set when the
 * set is first logged and preserved across edits.
 */
@Entity
@Table(name = "workout_sets")
public class WorkoutSet {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "session_exercise_id", nullable = false)
    private UUID sessionExerciseId;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "set_type", nullable = false, length = 30)
    private SetType setType;

    @Column(name = "weight", precision = 8, scale = 2)
    private BigDecimal weight;

    @Column(name = "reps")
    private Integer reps;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "distance_meters", precision = 10, scale = 2)
    private BigDecimal distanceMeters;

    @Column(name = "rpe", precision = 3, scale = 1)
    private BigDecimal rpe;

    @Column(name = "note")
    private String note;

    @Column(name = "equipment_id")
    private UUID equipmentId;

    @Column(name = "equipment_name_snapshot", length = 160)
    private String equipmentNameSnapshot;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkoutSet() {
    }

    public static WorkoutSet log(UUID sessionExerciseId, int setNumber, SetType setType, BigDecimal weight,
            Integer reps, Integer durationSeconds, BigDecimal distanceMeters, BigDecimal rpe, String note,
            UUID equipmentId, String equipmentNameSnapshot, Instant completedAt) {
        WorkoutSet set = new WorkoutSet();
        set.id = UUID.randomUUID();
        set.sessionExerciseId = sessionExerciseId;
        set.setNumber = setNumber;
        set.setType = setType;
        set.weight = weight;
        set.reps = reps;
        set.durationSeconds = durationSeconds;
        set.distanceMeters = distanceMeters;
        set.rpe = rpe;
        set.note = note;
        set.equipmentId = equipmentId;
        set.equipmentNameSnapshot = equipmentNameSnapshot;
        set.completedAt = completedAt;
        return set;
    }

    /** Updates the set's metrics and equipment. {@code completedAt} and {@code setNumber} are preserved. */
    public void update(SetType setType, BigDecimal weight, Integer reps, Integer durationSeconds,
            BigDecimal distanceMeters, BigDecimal rpe, String note, UUID equipmentId, String equipmentNameSnapshot) {
        this.setType = setType;
        this.weight = weight;
        this.reps = reps;
        this.durationSeconds = durationSeconds;
        this.distanceMeters = distanceMeters;
        this.rpe = rpe;
        this.note = note;
        this.equipmentId = equipmentId;
        this.equipmentNameSnapshot = equipmentNameSnapshot;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionExerciseId() {
        return sessionExerciseId;
    }

    public int getSetNumber() {
        return setNumber;
    }

    public SetType getSetType() {
        return setType;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public Integer getReps() {
        return reps;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public BigDecimal getDistanceMeters() {
        return distanceMeters;
    }

    public BigDecimal getRpe() {
        return rpe;
    }

    public String getNote() {
        return note;
    }

    public UUID getEquipmentId() {
        return equipmentId;
    }

    public String getEquipmentNameSnapshot() {
        return equipmentNameSnapshot;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
