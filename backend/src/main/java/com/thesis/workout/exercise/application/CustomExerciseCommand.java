package com.thesis.workout.exercise.application;

import com.thesis.workout.exercise.domain.model.ExerciseType;
import java.util.List;

/** Internal create/update payload for a custom exercise, decoupled from the web request DTOs. */
public record CustomExerciseCommand(
        String name,
        String description,
        ExerciseType exerciseType,
        List<MuscleAssignment> muscleGroups) {
}
