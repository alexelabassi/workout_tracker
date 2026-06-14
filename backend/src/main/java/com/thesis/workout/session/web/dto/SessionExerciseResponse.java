package com.thesis.workout.session.web.dto;

import com.thesis.workout.exercise.domain.model.ExerciseType;
import com.thesis.workout.session.domain.model.SessionExercise;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SessionExerciseResponse(
        UUID id,
        String exerciseName,
        ExerciseType exerciseType,
        int position,
        boolean extraExercise,
        Integer plannedSets,
        String plannedReps,
        BigDecimal plannedWeight,
        Integer restSeconds,
        String templateNote,
        List<SessionMuscleGroupResponse> muscleGroups,
        List<WorkoutSetResponse> sets) {

    public static SessionExerciseResponse from(SessionExercise exercise, List<WorkoutSetResponse> sets) {
        List<SessionMuscleGroupResponse> groups = exercise.getMuscleGroups().stream()
                .map(group -> new SessionMuscleGroupResponse(
                        group.getMuscleGroupCodeSnapshot(), group.getRoleSnapshot()))
                .toList();
        return new SessionExerciseResponse(
                exercise.getId(),
                exercise.getExerciseNameSnapshot(),
                exercise.getExerciseTypeSnapshot(),
                exercise.getPosition(),
                exercise.isExtraExercise(),
                exercise.getPlannedSetsSnapshot(),
                exercise.getPlannedRepsSnapshot(),
                exercise.getPlannedWeightSnapshot(),
                exercise.getRestSecondsSnapshot(),
                exercise.getTemplateNoteSnapshot(),
                groups,
                sets);
    }
}
