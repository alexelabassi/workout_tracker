package com.thesis.workout.session.web.dto;

import com.thesis.workout.session.domain.model.SetType;
import com.thesis.workout.session.domain.model.WorkoutSet;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WorkoutSetResponse(
        UUID id,
        int setNumber,
        SetType setType,
        BigDecimal weight,
        Integer reps,
        Integer durationSeconds,
        BigDecimal distanceMeters,
        BigDecimal rpe,
        String note,
        UUID equipmentId,
        String equipmentName,
        Instant completedAt) {

    public static WorkoutSetResponse from(WorkoutSet set) {
        return new WorkoutSetResponse(
                set.getId(),
                set.getSetNumber(),
                set.getSetType(),
                set.getWeight(),
                set.getReps(),
                set.getDurationSeconds(),
                set.getDistanceMeters(),
                set.getRpe(),
                set.getNote(),
                set.getEquipmentId(),
                set.getEquipmentNameSnapshot(),
                set.getCompletedAt());
    }
}
