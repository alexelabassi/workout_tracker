package com.thesis.workout.template.domain.model;

import com.thesis.workout.exercise.domain.model.MuscleRole;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * Snapshot of one of an exercise's muscle groups, captured when the exercise is added to a
 * template day. Preserves historical truth even if the source exercise later changes or is
 * deleted.
 */
@Entity
@Table(name = "template_day_exercise_muscle_groups")
public class TemplateDayExerciseMuscleGroup {

    @EmbeddedId
    private TemplateDayExerciseMuscleGroupId id;

    @MapsId("templateDayExerciseId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_day_exercise_id", nullable = false)
    private TemplateDayExercise templateDayExercise;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private MuscleRole role;

    protected TemplateDayExerciseMuscleGroup() {
    }

    static TemplateDayExerciseMuscleGroup of(TemplateDayExercise exercise, String muscleGroupCode, MuscleRole role) {
        TemplateDayExerciseMuscleGroup snapshot = new TemplateDayExerciseMuscleGroup();
        snapshot.id = new TemplateDayExerciseMuscleGroupId(exercise.getId(), muscleGroupCode);
        snapshot.templateDayExercise = exercise;
        snapshot.role = role;
        return snapshot;
    }

    public String getMuscleGroupCode() {
        return id.getMuscleGroupCode();
    }

    public MuscleRole getRole() {
        return role;
    }
}
