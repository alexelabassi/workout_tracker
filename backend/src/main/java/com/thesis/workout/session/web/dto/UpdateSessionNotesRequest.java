package com.thesis.workout.session.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateSessionNotesRequest(@Size(max = 5000) String notes) {
}
