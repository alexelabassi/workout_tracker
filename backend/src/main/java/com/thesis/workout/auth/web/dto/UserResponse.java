package com.thesis.workout.auth.web.dto;

import com.thesis.workout.auth.domain.model.AppUser;
import com.thesis.workout.auth.domain.model.Role;
import java.util.UUID;

public record UserResponse(UUID id, String email, String displayName, Role role) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }
}
