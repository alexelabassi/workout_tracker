package com.thesis.workout.template.web.dto;

import com.thesis.workout.routine.domain.model.RoutineType;
import com.thesis.workout.template.domain.model.TemplateDayRoutine;
import java.util.UUID;

public record TemplateDayRoutineResponse(
        UUID id,
        UUID routineId,
        RoutineType routineType,
        String routineName,
        String routineContent,
        int position) {

    public static TemplateDayRoutineResponse from(TemplateDayRoutine routine) {
        return new TemplateDayRoutineResponse(
                routine.getId(),
                routine.getRoutineId(),
                routine.getRoutineType(),
                routine.getRoutineNameSnapshot(),
                routine.getRoutineContentSnapshot(),
                routine.getPosition());
    }
}
