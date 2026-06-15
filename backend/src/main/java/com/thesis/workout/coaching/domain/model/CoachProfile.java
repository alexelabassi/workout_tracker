package com.thesis.workout.coaching.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Marks an {@code app_user} as a coach. In this MVP a coach profile is created by the demo seeder
 * (admin-driven creation is deferred). The profile is informational; authorization for reading a
 * given client is governed by the {@link CoachClientRelationship}, not by this row alone.
 */
@Entity
@Table(name = "coach_profiles")
public class CoachProfile {

    @Id
    @Column(name = "coach_user_id", nullable = false, updatable = false)
    private UUID coachUserId;

    @Column(name = "created_by_admin_id")
    private UUID createdByAdminId;

    @Column(name = "bio")
    private String bio;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CoachProfile() {
    }

    public static CoachProfile create(UUID coachUserId, String bio) {
        CoachProfile profile = new CoachProfile();
        profile.coachUserId = coachUserId;
        profile.bio = bio;
        profile.active = true;
        return profile;
    }

    public UUID getCoachUserId() {
        return coachUserId;
    }

    public boolean isActive() {
        return active;
    }
}
