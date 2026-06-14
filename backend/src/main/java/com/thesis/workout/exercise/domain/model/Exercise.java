package com.thesis.workout.exercise.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * An exercise is either OFFICIAL (curated catalog, {@code ownerUserId == null}) or CUSTOM
 * (owned by a single user). The database enforces the visibility/owner invariant; this entity
 * only exposes a factory for the CUSTOM case since official exercises are seeded by Flyway.
 */
@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false, length = 30)
    private ExerciseType exerciseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 30)
    private Visibility visibility;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExerciseMuscleGroup> muscleGroups = new ArrayList<>();

    protected Exercise() {
    }

    public static Exercise createCustom(UUID ownerUserId, String name, String description,
            ExerciseType exerciseType) {
        Exercise exercise = new Exercise();
        exercise.id = UUID.randomUUID();
        exercise.ownerUserId = ownerUserId;
        exercise.name = name;
        exercise.description = description;
        exercise.exerciseType = exerciseType;
        exercise.visibility = Visibility.CUSTOM;
        return exercise;
    }

    public void updateDetails(String name, String description, ExerciseType exerciseType) {
        this.name = name;
        this.description = description;
        this.exerciseType = exerciseType;
    }

    public void addMuscleGroup(String muscleGroupCode, MuscleRole role) {
        muscleGroups.add(ExerciseMuscleGroup.of(this, muscleGroupCode, role));
    }

    public void clearMuscleGroups() {
        muscleGroups.clear();
    }

    public void softDelete(Instant when) {
        if (deletedAt == null) {
            deletedAt = when;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ExerciseType getExerciseType() {
        return exerciseType;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public List<ExerciseMuscleGroup> getMuscleGroups() {
        return muscleGroups;
    }
}
