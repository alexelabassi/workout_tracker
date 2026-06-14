package com.thesis.workout.gym.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A piece of equipment within a gym. {@code userId} is denormalized to the gym's owner so
 * update/delete can be authorized owner-scoped without a join; create/list are gym-scoped.
 * Uniqueness is enforced per (gym, name) while not soft-deleted.
 */
@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "gym_id", nullable = false)
    private UUID gymId;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_type", length = 50)
    private EquipmentType equipmentType;

    @Column(name = "notes")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Equipment() {
    }

    public static Equipment createFor(UUID userId, UUID gymId, String name, EquipmentType equipmentType,
            String notes) {
        Equipment equipment = new Equipment();
        equipment.id = UUID.randomUUID();
        equipment.userId = userId;
        equipment.gymId = gymId;
        equipment.name = name;
        equipment.equipmentType = equipmentType;
        equipment.notes = notes;
        return equipment;
    }

    public void updateDetails(String name, EquipmentType equipmentType, String notes) {
        this.name = name;
        this.equipmentType = equipmentType;
        this.notes = notes;
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

    public UUID getGymId() {
        return gymId;
    }

    public String getName() {
        return name;
    }

    public EquipmentType getEquipmentType() {
        return equipmentType;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
