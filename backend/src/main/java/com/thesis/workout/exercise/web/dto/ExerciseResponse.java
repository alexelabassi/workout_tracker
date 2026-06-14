package com.thesis.workout.exercise.web.dto;

import com.thesis.workout.exercise.domain.model.Exercise;
import com.thesis.workout.exercise.domain.model.ExerciseType;
import com.thesis.workout.exercise.domain.model.Visibility;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ExerciseResponse(
        UUID id,
        String name,
        String description,
        ExerciseType exerciseType,
        Visibility visibility,
        List<ExerciseMuscleGroupResponse> muscleGroups) {

    /**
     * @param muscleGroupNames code -> display name, resolved once per request so mapping the
     *     join rows does not trigger a per-row lookup.
     */
    public static ExerciseResponse from(Exercise exercise, Map<String, String> muscleGroupNames) {
        List<ExerciseMuscleGroupResponse> groups = exercise.getMuscleGroups().stream()
                .map(link -> new ExerciseMuscleGroupResponse(
                        link.getMuscleGroupCode(),
                        muscleGroupNames.get(link.getMuscleGroupCode()),
                        link.getRole()))
                .toList();
        return new ExerciseResponse(
                exercise.getId(),
                exercise.getName(),
                exercise.getDescription(),
                exercise.getExerciseType(),
                exercise.getVisibility(),
                groups);
    }
}
