package com.thesis.workout.session.web.dto;

import com.thesis.workout.exercise.domain.model.MuscleRole;

public record SessionMuscleGroupResponse(String code, MuscleRole role) {
}
