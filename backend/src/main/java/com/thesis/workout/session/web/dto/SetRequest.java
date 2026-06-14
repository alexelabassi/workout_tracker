package com.thesis.workout.session.web.dto;

import com.thesis.workout.session.application.SetCommand;
import com.thesis.workout.session.domain.model.SetType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record SetRequest(
        SetType setType,
        @PositiveOrZero BigDecimal weight,
        @PositiveOrZero Integer reps,
        @PositiveOrZero Integer durationSeconds,
        @PositiveOrZero BigDecimal distanceMeters,
        @DecimalMin("1.0") @DecimalMax("10.0") BigDecimal rpe,
        @Size(max = 5000) String note,
        UUID equipmentId) {

    public SetCommand toCommand() {
        return new SetCommand(setType, weight, reps, durationSeconds, distanceMeters, rpe, note, equipmentId);
    }
}
