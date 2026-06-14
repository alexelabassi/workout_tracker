package com.thesis.workout.session.web;

import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import com.thesis.workout.session.application.WorkoutSetService;
import com.thesis.workout.session.web.dto.SetRequest;
import com.thesis.workout.session.web.dto.WorkoutSetResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WorkoutSetController {

    private final WorkoutSetService workoutSetService;

    public WorkoutSetController(WorkoutSetService workoutSetService) {
        this.workoutSetService = workoutSetService;
    }

    @PostMapping("/session-exercises/{sessionExerciseId}/sets")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutSetResponse addSet(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionExerciseId,
            @Valid @RequestBody SetRequest request) {
        return workoutSetService.addSet(principal.id(), sessionExerciseId, request.toCommand());
    }

    @PutMapping("/sets/{setId}")
    public WorkoutSetResponse updateSet(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID setId,
            @Valid @RequestBody SetRequest request) {
        return workoutSetService.updateSet(principal.id(), setId, request.toCommand());
    }

    @DeleteMapping("/sets/{setId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSet(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID setId) {
        workoutSetService.deleteSet(principal.id(), setId);
    }
}
