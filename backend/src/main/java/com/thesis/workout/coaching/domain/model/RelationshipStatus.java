package com.thesis.workout.coaching.domain.model;

/**
 * Lifecycle of a coach↔client relationship. A coach invite starts {@code PENDING}; the client
 * accepts ({@code ACTIVE}) or rejects ({@code REJECTED}); either side can later revoke an active
 * relationship ({@code REVOKED}). Only {@code ACTIVE} grants the coach read access.
 */
public enum RelationshipStatus {
    PENDING,
    ACTIVE,
    REVOKED,
    REJECTED
}
