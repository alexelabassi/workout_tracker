package com.thesis.workout.gym.web.dto;

import com.thesis.workout.gym.application.EquipmentCommand;
import com.thesis.workout.gym.domain.model.EquipmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipmentRequest(
        @NotBlank @Size(max = 160) String name,
        EquipmentType equipmentType,
        @Size(max = 5000) String notes) {

    public EquipmentCommand toCommand() {
        return new EquipmentCommand(name, equipmentType, notes);
    }
}
