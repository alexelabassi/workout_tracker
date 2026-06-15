package com.thesis.workout.search.application.event;

import java.util.UUID;

/**
 * Signals that a workout session reached a terminal status and its OpenSearch document must be
 * (re)built after commit. Sessions are append-only history, so the only operations are upsert
 * (FINISHED/CANCELLED) and remove.
 */
public record WorkoutSessionIndexEvent(UUID sessionId, Operation operation) {

    public enum Operation {
        UPSERT,
        REMOVE
    }

    public static WorkoutSessionIndexEvent upsert(UUID sessionId) {
        return new WorkoutSessionIndexEvent(sessionId, Operation.UPSERT);
    }

    public static WorkoutSessionIndexEvent remove(UUID sessionId) {
        return new WorkoutSessionIndexEvent(sessionId, Operation.REMOVE);
    }
}
