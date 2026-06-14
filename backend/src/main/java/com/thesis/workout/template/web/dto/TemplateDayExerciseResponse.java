package com.thesis.workout.template.web.dto;

import com.thesis.workout.exercise.domain.model.ExerciseType;
import com.thesis.workout.template.domain.model.TemplateDayExercise;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TemplateDayExerciseResponse(
        UUID id,
        UUID exerciseId,
        String exerciseName,
        ExerciseType exerciseType,
        int position,
        Integer plannedSets,
        String plannedReps,
        BigDecimal plannedWeight,
        Integer restSeconds,
        String note,
        List<TemplateMuscleGroupSnapshotResponse> muscleGroups) {

    public static TemplateDayExerciseResponse from(TemplateDayExercise exercise) {
        List<TemplateMuscleGroupSnapshotResponse> groups = exercise.getMuscleGroups().stream()
                .map(group -> new TemplateMuscleGroupSnapshotResponse(group.getMuscleGroupCode(), group.getRole()))
                .toList();
        return new TemplateDayExerciseResponse(
                exercise.getId(),
                exercise.getExerciseId(),
                exercise.getExerciseNameSnapshot(),
                exercise.getExerciseTypeSnapshot(),
                exercise.getPosition(),
                exercise.getPlannedSets(),
                exercise.getPlannedReps(),
                exercise.getPlannedWeight(),
                exercise.getRestSeconds(),
                exercise.getNote(),
                groups);
    }
}
