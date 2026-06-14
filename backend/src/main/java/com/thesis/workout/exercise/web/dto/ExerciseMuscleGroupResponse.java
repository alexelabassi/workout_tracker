package com.thesis.workout.exercise.web.dto;

import com.thesis.workout.exercise.domain.model.MuscleRole;

public record ExerciseMuscleGroupResponse(String code, String displayName, MuscleRole role) {
}
