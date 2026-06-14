package com.thesis.workout.routine.web.dto;

import com.thesis.workout.routine.domain.model.Routine;
import com.thesis.workout.routine.domain.model.RoutineType;
import java.time.Instant;
import java.util.UUID;

public record RoutineResponse(
        UUID id,
        String name,
        RoutineType routineType,
        String content,
        Instant updatedAt) {

    public static RoutineResponse from(Routine routine) {
        return new RoutineResponse(
                routine.getId(),
                routine.getName(),
                routine.getRoutineType(),
                routine.getContent(),
                routine.getUpdatedAt());
    }
}
