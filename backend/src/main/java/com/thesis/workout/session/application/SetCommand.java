package com.thesis.workout.session.application;

import com.thesis.workout.session.domain.model.SetType;
import java.math.BigDecimal;
import java.util.UUID;

/** Internal create/update payload for a logged set. {@code setType} defaults to WORKING upstream. */
public record SetCommand(
        SetType setType,
        BigDecimal weight,
        Integer reps,
        Integer durationSeconds,
        BigDecimal distanceMeters,
        BigDecimal rpe,
        String note,
        UUID equipmentId) {
}
