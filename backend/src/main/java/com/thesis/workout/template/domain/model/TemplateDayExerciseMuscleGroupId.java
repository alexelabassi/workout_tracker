package com.thesis.workout.template.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class TemplateDayExerciseMuscleGroupId implements Serializable {

    @Column(name = "template_day_exercise_id", nullable = false)
    private UUID templateDayExerciseId;

    @Column(name = "muscle_group_code", nullable = false, length = 50)
    private String muscleGroupCode;

    protected TemplateDayExerciseMuscleGroupId() {
    }

    public TemplateDayExerciseMuscleGroupId(UUID templateDayExerciseId, String muscleGroupCode) {
        this.templateDayExerciseId = templateDayExerciseId;
        this.muscleGroupCode = muscleGroupCode;
    }

    public String getMuscleGroupCode() {
        return muscleGroupCode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TemplateDayExerciseMuscleGroupId that)) {
            return false;
        }
        return Objects.equals(templateDayExerciseId, that.templateDayExerciseId)
                && Objects.equals(muscleGroupCode, that.muscleGroupCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateDayExerciseId, muscleGroupCode);
    }
}
