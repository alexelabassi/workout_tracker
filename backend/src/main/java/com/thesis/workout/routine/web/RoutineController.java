package com.thesis.workout.routine.web;

import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import com.thesis.workout.routine.application.RoutineService;
import com.thesis.workout.routine.web.dto.RoutineRequest;
import com.thesis.workout.routine.web.dto.RoutineResponse;
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
@RequestMapping("/api/routines")
public class RoutineController {

    private final RoutineService routineService;

    public RoutineController(RoutineService routineService) {
        this.routineService = routineService;
    }

    @GetMapping
    public List<RoutineResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return routineService.list(principal.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoutineResponse create(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RoutineRequest request) {
        return routineService.create(principal.id(), request.toCommand());
    }

    @PutMapping("/{routineId}")
    public RoutineResponse update(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID routineId,
            @Valid @RequestBody RoutineRequest request) {
        return routineService.update(principal.id(), routineId, request.toCommand());
    }

    @DeleteMapping("/{routineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID routineId) {
        routineService.delete(principal.id(), routineId);
    }
}
