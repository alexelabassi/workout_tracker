package com.thesis.workout.template.domain.model;

import com.thesis.workout.exercise.domain.model.ExerciseType;
import com.thesis.workout.exercise.domain.model.MuscleRole;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * An exercise placed on a template day. The {@code exercise_id} reference is kept (nullable, set
 * null if the source exercise is deleted) but name/type/muscle-groups are snapshotted so the
 * template preserves historical truth regardless of later edits to the source exercise.
 */
@Entity
@Table(name = "template_day_exercises")
public class TemplateDayExercise {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "template_day_id", nullable = false)
    private UUID templateDayId;

    @Column(name = "exercise_id")
    private UUID exerciseId;

    @Column(name = "exercise_name_snapshot", nullable = false, length = 160)
    private String exerciseNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type_snapshot", nullable = false, length = 30)
    private ExerciseType exerciseTypeSnapshot;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "planned_sets")
    private Integer plannedSets;

    @Column(name = "planned_reps", length = 50)
    private String plannedReps;

    @Column(name = "planned_weight", precision = 8, scale = 2)
    private BigDecimal plannedWeight;

    @Column(name = "rest_seconds")
    private Integer restSeconds;

    @Column(name = "note")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "templateDayExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TemplateDayExerciseMuscleGroup> muscleGroups = new ArrayList<>();

    protected TemplateDayExercise() {
    }

    public static TemplateDayExercise createFor(UUID templateDayId, UUID exerciseId, String exerciseNameSnapshot,
            ExerciseType exerciseTypeSnapshot, int position, Integer plannedSets, String plannedReps,
            BigDecimal plannedWeight, Integer restSeconds, String note) {
        TemplateDayExercise exercise = new TemplateDayExercise();
        exercise.id = UUID.randomUUID();
        exercise.templateDayId = templateDayId;
        exercise.exerciseId = exerciseId;
        exercise.exerciseNameSnapshot = exerciseNameSnapshot;
        exercise.exerciseTypeSnapshot = exerciseTypeSnapshot;
        exercise.position = position;
        exercise.plannedSets = plannedSets;
        exercise.plannedReps = plannedReps;
        exercise.plannedWeight = plannedWeight;
        exercise.restSeconds = restSeconds;
        exercise.note = note;
        return exercise;
    }

    public void updateSnapshot(UUID exerciseId, String exerciseNameSnapshot, ExerciseType exerciseTypeSnapshot) {
        this.exerciseId = exerciseId;
        this.exerciseNameSnapshot = exerciseNameSnapshot;
        this.exerciseTypeSnapshot = exerciseTypeSnapshot;
    }

    public void updatePlanned(Integer plannedSets, String plannedReps, BigDecimal plannedWeight, Integer restSeconds,
            String note) {
        this.plannedSets = plannedSets;
        this.plannedReps = plannedReps;
        this.plannedWeight = plannedWeight;
        this.restSeconds = restSeconds;
        this.note = note;
    }

    public void addMuscleGroup(String muscleGroupCode, MuscleRole role) {
        muscleGroups.add(TemplateDayExerciseMuscleGroup.of(this, muscleGroupCode, role));
    }

    public void clearMuscleGroups() {
        muscleGroups.clear();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTemplateDayId() {
        return templateDayId;
    }

    public UUID getExerciseId() {
        return exerciseId;
    }

    public String getExerciseNameSnapshot() {
        return exerciseNameSnapshot;
    }

    public ExerciseType getExerciseTypeSnapshot() {
        return exerciseTypeSnapshot;
    }

    public int getPosition() {
        return position;
    }

    public Integer getPlannedSets() {
        return plannedSets;
    }

    public String getPlannedReps() {
        return plannedReps;
    }

    public BigDecimal getPlannedWeight() {
        return plannedWeight;
    }

    public Integer getRestSeconds() {
        return restSeconds;
    }

    public String getNote() {
        return note;
    }

    public List<TemplateDayExerciseMuscleGroup> getMuscleGroups() {
        return muscleGroups;
    }
}
