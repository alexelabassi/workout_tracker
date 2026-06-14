package com.thesis.workout.session.web.dto;

import com.thesis.workout.routine.domain.model.RoutineType;
import com.thesis.workout.session.domain.model.SessionRoutine;
import java.util.UUID;

public record SessionRoutineResponse(
        UUID id,
        RoutineType routineType,
        String routineName,
        String routineContent,
        int position) {

    public static SessionRoutineResponse from(SessionRoutine routine) {
        return new SessionRoutineResponse(
                routine.getId(),
                routine.getRoutineType(),
                routine.getRoutineNameSnapshot(),
                routine.getRoutineContentSnapshot(),
                routine.getPosition());
    }
}
