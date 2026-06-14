package com.thesis.workout.exercise.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ExerciseMuscleGroupId implements Serializable {

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "muscle_group_code", nullable = false, length = 50)
    private String muscleGroupCode;

    protected ExerciseMuscleGroupId() {
    }

    public ExerciseMuscleGroupId(UUID exerciseId, String muscleGroupCode) {
        this.exerciseId = exerciseId;
        this.muscleGroupCode = muscleGroupCode;
    }

    public UUID getExerciseId() {
        return exerciseId;
    }

    public String getMuscleGroupCode() {
        return muscleGroupCode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ExerciseMuscleGroupId that)) {
            return false;
        }
        return Objects.equals(exerciseId, that.exerciseId)
                && Objects.equals(muscleGroupCode, that.muscleGroupCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exerciseId, muscleGroupCode);
    }
}
