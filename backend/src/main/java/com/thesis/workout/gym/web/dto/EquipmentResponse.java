package com.thesis.workout.gym.web.dto;

import com.thesis.workout.gym.domain.model.Equipment;
import com.thesis.workout.gym.domain.model.EquipmentType;
import java.time.Instant;
import java.util.UUID;

public record EquipmentResponse(
        UUID id,
        UUID gymId,
        String name,
        EquipmentType equipmentType,
        String notes,
        Instant updatedAt) {

    public static EquipmentResponse from(Equipment equipment) {
        return new EquipmentResponse(
                equipment.getId(),
                equipment.getGymId(),
                equipment.getName(),
                equipment.getEquipmentType(),
                equipment.getNotes(),
                equipment.getUpdatedAt());
    }
}
