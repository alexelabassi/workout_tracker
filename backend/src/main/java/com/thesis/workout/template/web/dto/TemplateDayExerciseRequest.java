package com.thesis.workout.template.web.dto;

import com.thesis.workout.template.application.TemplateDayExerciseCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record TemplateDayExerciseRequest(
        @NotNull UUID exerciseId,
        @Positive Integer plannedSets,
        @Size(max = 50) String plannedReps,
        @PositiveOrZero BigDecimal plannedWeight,
        @PositiveOrZero Integer restSeconds,
        @Size(max = 5000) String note) {

    public TemplateDayExerciseCommand toCommand() {
        return new TemplateDayExerciseCommand(exerciseId, plannedSets, plannedReps, plannedWeight, restSeconds, note);
    }
}
