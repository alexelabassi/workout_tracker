package com.thesis.workout.gym.web.dto;

import com.thesis.workout.gym.application.GymCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GymRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 255) String location) {

    public GymCommand toCommand() {
        return new GymCommand(name, location);
    }
}
