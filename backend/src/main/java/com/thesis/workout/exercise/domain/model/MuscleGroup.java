package com.thesis.workout.exercise.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Reference data seeded by Flyway (see V1 migration). There is no write path: muscle groups
 * are a fixed taxonomy that custom and official exercises tag themselves against.
 */
@Entity
@Table(name = "muscle_groups")
public class MuscleGroup {

    @Id
    @Column(name = "code", nullable = false, updatable = false, length = 50)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    protected MuscleGroup() {
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}
