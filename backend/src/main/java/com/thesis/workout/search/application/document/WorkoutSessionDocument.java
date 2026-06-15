package com.thesis.workout.search.application.document;

import java.util.List;

/**
 * Denormalised workout-session document stored in the {@code workout_sessions_v1} index. Built
 * entirely from the session's immutable snapshots (exercise/gym/equipment/muscle names captured at
 * the time of training), so search results reflect historical truth and never depend on live
 * templates, exercises, gyms or equipment. Only FINISHED/CANCELLED sessions are indexed.
 */
public record WorkoutSessionDocument(
        String sessionId,
        String ownerUserId,
        String status,
        Long startedAt,
        Long finishedAt,
        Long durationSeconds,
        String templateNameSnapshot,
        String templateDayNameSnapshot,
        String gymNameSnapshot,
        List<String> exerciseNameSnapshots,
        List<String> muscleGroups,
        String muscleGroupsText,
        List<String> equipmentNameSnapshots,
        String notes,
        Double totalVolume,
        Integer setCount,
        Integer exerciseCount) {
}
