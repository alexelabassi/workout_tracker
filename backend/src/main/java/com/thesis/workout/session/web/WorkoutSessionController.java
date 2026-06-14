package com.thesis.workout.session.web;

import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import com.thesis.workout.session.application.WorkoutSessionService;
import com.thesis.workout.session.web.dto.AddExtraExerciseRequest;
import com.thesis.workout.session.web.dto.SessionExerciseResponse;
import com.thesis.workout.session.web.dto.StartWorkoutRequest;
import com.thesis.workout.session.web.dto.UpdateSessionNotesRequest;
import com.thesis.workout.session.web.dto.WorkoutSessionDetailResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutSessionController {

    private final WorkoutSessionService workoutSessionService;

    public WorkoutSessionController(WorkoutSessionService workoutSessionService) {
        this.workoutSessionService = workoutSessionService;
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutSessionDetailResponse start(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StartWorkoutRequest request) {
        return workoutSessionService.start(principal.id(), request.templateDayId(), request.gymId());
    }

    @GetMapping("/active")
    public ResponseEntity<WorkoutSessionDetailResponse> active(@AuthenticationPrincipal UserPrincipal principal) {
        return workoutSessionService.active(principal.id())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{sessionId}")
    public WorkoutSessionDetailResponse get(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        return workoutSessionService.get(principal.id(), sessionId);
    }

    @PostMapping("/{sessionId}/finish")
    public WorkoutSessionDetailResponse finish(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        return workoutSessionService.finish(principal.id(), sessionId);
    }

    @PostMapping("/{sessionId}/cancel")
    public WorkoutSessionDetailResponse cancel(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        return workoutSessionService.cancel(principal.id(), sessionId);
    }

    @PostMapping("/{sessionId}/extra-exercises")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionExerciseResponse addExtraExercise(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AddExtraExerciseRequest request) {
        return workoutSessionService.addExtraExercise(principal.id(), sessionId, request.exerciseId());
    }

    @PutMapping("/{sessionId}/notes")
    public WorkoutSessionDetailResponse updateNotes(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody UpdateSessionNotesRequest request) {
        return workoutSessionService.updateNotes(principal.id(), sessionId, request.notes());
    }
}
