package com.thesis.workout.exercise.web.dto;

import com.thesis.workout.exercise.domain.model.MuscleGroup;

public record MuscleGroupResponse(String code, String displayName) {

    public static MuscleGroupResponse from(MuscleGroup muscleGroup) {
        return new MuscleGroupResponse(muscleGroup.getCode(), muscleGroup.getDisplayName());
    }
}
