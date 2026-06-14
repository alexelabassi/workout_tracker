package com.thesis.workout.gym.application;

import com.thesis.workout.gym.domain.model.EquipmentType;

/** Internal create/update payload for a piece of equipment, decoupled from the web request DTO. */
public record EquipmentCommand(String name, EquipmentType equipmentType, String notes) {
}
