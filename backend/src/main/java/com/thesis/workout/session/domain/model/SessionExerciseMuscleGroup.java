package com.thesis.workout.session.domain.model;

import com.thesis.workout.exercise.domain.model.MuscleRole;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * Snapshot of a session exercise's muscle group, copied at start (from the template) or when an
 * extra exercise is added. Independent of the live exercise's current muscle groups.
 */
@Entity
@Table(name = "session_exercise_muscle_groups")
public class SessionExerciseMuscleGroup {

    @EmbeddedId
    private SessionExerciseMuscleGroupId id;

    @MapsId("sessionExerciseId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_exercise_id", nullable = false)
    private SessionExercise sessionExercise;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_snapshot", nullable = false, length = 30)
    private MuscleRole roleSnapshot;

    protected SessionExerciseMuscleGroup() {
    }

    static SessionExerciseMuscleGroup of(SessionExercise sessionExercise, String muscleGroupCode, MuscleRole role) {
        SessionExerciseMuscleGroup snapshot = new SessionExerciseMuscleGroup();
        snapshot.id = new SessionExerciseMuscleGroupId(sessionExercise.getId(), muscleGroupCode);
        snapshot.sessionExercise = sessionExercise;
        snapshot.roleSnapshot = role;
        return snapshot;
    }

    public String getMuscleGroupCodeSnapshot() {
        return id.getMuscleGroupCodeSnapshot();
    }

    public MuscleRole getRoleSnapshot() {
        return roleSnapshot;
    }
}
