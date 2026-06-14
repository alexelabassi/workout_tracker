package com.thesis.workout.routine.application;

import com.thesis.workout.routine.domain.model.RoutineType;

/** Internal create/update payload for a routine, decoupled from the web request DTO. */
public record RoutineCommand(String name, RoutineType routineType, String content) {
}
