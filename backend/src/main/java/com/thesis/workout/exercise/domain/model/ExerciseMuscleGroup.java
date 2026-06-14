package com.thesis.workout.exercise.domain.model;

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
 * Join row tagging an exercise with a muscle group and the role it plays (PRIMARY/SECONDARY).
 * Modelled as an association entity rather than a {@code @ManyToMany} because the join carries
 * the {@code role} attribute. The muscle group code lives in the embedded id as a plain column;
 * the foreign key to {@code muscle_groups} is enforced by the database.
 */
@Entity
@Table(name = "exercise_muscle_groups")
public class ExerciseMuscleGroup {

    @EmbeddedId
    private ExerciseMuscleGroupId id;

    @MapsId("exerciseId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private MuscleRole role;

    protected ExerciseMuscleGroup() {
    }

    static ExerciseMuscleGroup of(Exercise exercise, String muscleGroupCode, MuscleRole role) {
        ExerciseMuscleGroup link = new ExerciseMuscleGroup();
        link.id = new ExerciseMuscleGroupId(exercise.getId(), muscleGroupCode);
        link.exercise = exercise;
        link.role = role;
        return link;
    }

    public String getMuscleGroupCode() {
        return id.getMuscleGroupCode();
    }

    public MuscleRole getRole() {
        return role;
    }
}
