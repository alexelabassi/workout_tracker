package com.thesis.workout.coaching.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A coach invites a client by their registered email. No account is created if it does not exist. */
public record InviteRequest(
        @NotBlank @Email @Size(max = 320) String clientEmail) {
}
