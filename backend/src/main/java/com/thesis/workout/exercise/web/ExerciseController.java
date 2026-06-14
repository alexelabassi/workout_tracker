package com.thesis.workout.exercise.web;

import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import com.thesis.workout.exercise.application.ExerciseService;
import com.thesis.workout.exercise.web.dto.CreateCustomExerciseRequest;
import com.thesis.workout.exercise.web.dto.ExerciseResponse;
import com.thesis.workout.exercise.web.dto.UpdateCustomExerciseRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @GetMapping
    public List<ExerciseResponse> listVisible(@AuthenticationPrincipal UserPrincipal principal) {
        return exerciseService.listVisible(principal.id());
    }

    @GetMapping("/official")
    public List<ExerciseResponse> listOfficial() {
        return exerciseService.listOfficial();
    }

    @GetMapping("/custom")
    public List<ExerciseResponse> listCustom(@AuthenticationPrincipal UserPrincipal principal) {
        return exerciseService.listCustom(principal.id());
    }

    @PostMapping("/custom")
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseResponse createCustom(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCustomExerciseRequest request) {
        return exerciseService.createCustom(principal.id(), request.toCommand());
    }

    @PutMapping("/custom/{exerciseId}")
    public ExerciseResponse updateCustom(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID exerciseId,
            @Valid @RequestBody UpdateCustomExerciseRequest request) {
        return exerciseService.updateCustom(principal.id(), exerciseId, request.toCommand());
    }

    @DeleteMapping("/custom/{exerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustom(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID exerciseId) {
        exerciseService.deleteCustom(principal.id(), exerciseId);
    }
}
