package com.thesis.workout.gym.web.dto;

import com.thesis.workout.gym.domain.model.Gym;
import java.time.Instant;
import java.util.UUID;

public record GymResponse(UUID id, String name, String location, Instant updatedAt) {

    public static GymResponse from(Gym gym) {
        return new GymResponse(gym.getId(), gym.getName(), gym.getLocation(), gym.getUpdatedAt());
    }
}
