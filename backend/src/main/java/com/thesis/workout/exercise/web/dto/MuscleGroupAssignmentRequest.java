package com.thesis.workout.exercise.web.dto;

import com.thesis.workout.exercise.domain.model.MuscleRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MuscleGroupAssignmentRequest(
        @NotBlank @Size(max = 50) String code,
        @NotNull MuscleRole role) {
}
