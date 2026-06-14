package com.thesis.workout.session.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SessionExerciseMuscleGroupId implements Serializable {

    @Column(name = "session_exercise_id", nullable = false)
    private UUID sessionExerciseId;

    @Column(name = "muscle_group_code_snapshot", nullable = false, length = 50)
    private String muscleGroupCodeSnapshot;

    protected SessionExerciseMuscleGroupId() {
    }

    public SessionExerciseMuscleGroupId(UUID sessionExerciseId, String muscleGroupCodeSnapshot) {
        this.sessionExerciseId = sessionExerciseId;
        this.muscleGroupCodeSnapshot = muscleGroupCodeSnapshot;
    }

    public String getMuscleGroupCodeSnapshot() {
        return muscleGroupCodeSnapshot;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SessionExerciseMuscleGroupId that)) {
            return false;
        }
        return Objects.equals(sessionExerciseId, that.sessionExerciseId)
                && Objects.equals(muscleGroupCodeSnapshot, that.muscleGroupCodeSnapshot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionExerciseId, muscleGroupCodeSnapshot);
    }
}
