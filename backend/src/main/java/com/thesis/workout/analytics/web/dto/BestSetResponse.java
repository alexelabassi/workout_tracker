package com.thesis.workout.analytics.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BestSetResponse(
        String exerciseName,
        BigDecimal weight,
        Integer reps,
        BigDecimal estimatedOneRepMax,
        UUID sessionId,
        String performedAt) {
}
