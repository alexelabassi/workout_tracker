package com.thesis.workout.session.domain.model;

import com.thesis.workout.routine.domain.model.RoutineType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A START/END routine copied into a session at start. Name/content/type are snapshots; the
 * original routine reference is nullable and never read back for the session view.
 */
@Entity
@Table(name = "session_routines")
public class SessionRoutine {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "original_routine_id")
    private UUID originalRoutineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "routine_type", nullable = false, length = 30)
    private RoutineType routineType;

    @Column(name = "routine_name_snapshot", nullable = false, length = 160)
    private String routineNameSnapshot;

    @Column(name = "routine_content_snapshot", nullable = false)
    private String routineContentSnapshot;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    protected SessionRoutine() {
    }

    public static SessionRoutine fromTemplate(UUID sessionId, UUID routineId, RoutineType routineType,
            String routineNameSnapshot, String routineContentSnapshot, int position) {
        SessionRoutine routine = new SessionRoutine();
        routine.id = UUID.randomUUID();
        routine.sessionId = sessionId;
        routine.originalRoutineId = routineId;
        routine.routineType = routineType;
        routine.routineNameSnapshot = routineNameSnapshot;
        routine.routineContentSnapshot = routineContentSnapshot;
        routine.position = position;
        return routine;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public RoutineType getRoutineType() {
        return routineType;
    }

    public String getRoutineNameSnapshot() {
        return routineNameSnapshot;
    }

    public String getRoutineContentSnapshot() {
        return routineContentSnapshot;
    }

    public int getPosition() {
        return position;
    }
}
