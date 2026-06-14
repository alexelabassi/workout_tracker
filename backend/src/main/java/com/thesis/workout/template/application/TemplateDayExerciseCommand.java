package com.thesis.workout.template.application;

import java.math.BigDecimal;
import java.util.UUID;

public record TemplateDayExerciseCommand(
        UUID exerciseId,
        Integer plannedSets,
        String plannedReps,
        BigDecimal plannedWeight,
        Integer restSeconds,
        String note) {
}
