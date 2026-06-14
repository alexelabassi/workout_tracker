package com.thesis.workout.gym.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A training location owned by a single user. Equipment hangs off a gym. Uniqueness is enforced
 * per (user, name) while not soft-deleted.
 */
@Entity
@Table(name = "gyms")
public class Gym {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "location", length = 255)
    private String location;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Gym() {
    }

    public static Gym createFor(UUID userId, String name, String location) {
        Gym gym = new Gym();
        gym.id = UUID.randomUUID();
        gym.userId = userId;
        gym.name = name;
        gym.location = location;
        return gym;
    }

    public void updateDetails(String name, String location) {
        this.name = name;
        this.location = location;
    }

    public void softDelete(Instant when) {
        if (deletedAt == null) {
            deletedAt = when;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
