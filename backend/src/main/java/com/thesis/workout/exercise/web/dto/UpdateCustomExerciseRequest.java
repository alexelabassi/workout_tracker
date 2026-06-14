package com.thesis.workout.exercise.web.dto;

import com.thesis.workout.exercise.application.CustomExerciseCommand;
import com.thesis.workout.exercise.domain.model.ExerciseType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateCustomExerciseRequest(
        @NotBlank @Size(max = 160) String name,
        String description,
        @NotNull ExerciseType exerciseType,
        @Valid List<MuscleGroupAssignmentRequest> muscleGroups) {

    public CustomExerciseCommand toCommand() {
        return new CustomExerciseCommand(
                name, description, exerciseType,
                CreateCustomExerciseRequest.mapAssignments(muscleGroups));
    }
}
