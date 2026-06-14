package com.thesis.workout.routine.web.dto;

import com.thesis.workout.routine.application.RoutineCommand;
import com.thesis.workout.routine.domain.model.RoutineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoutineRequest(
        @NotBlank @Size(max = 160) String name,
        @NotNull RoutineType routineType,
        @NotBlank @Size(max = 5000) String content) {

    public RoutineCommand toCommand() {
        return new RoutineCommand(name, routineType, content);
    }
}
