package com.thesis.workout.gym.web;

import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import com.thesis.workout.gym.application.GymService;
import com.thesis.workout.gym.web.dto.GymRequest;
import com.thesis.workout.gym.web.dto.GymResponse;
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
@RequestMapping("/api/gyms")
public class GymController {

    private final GymService gymService;

    public GymController(GymService gymService) {
        this.gymService = gymService;
    }

    @GetMapping
    public List<GymResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return gymService.list(principal.id());
    }

    @GetMapping("/{gymId}")
    public GymResponse get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID gymId) {
        return gymService.get(principal.id(), gymId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GymResponse create(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GymRequest request) {
        return gymService.create(principal.id(), request.toCommand());
    }

    @PutMapping("/{gymId}")
    public GymResponse update(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID gymId,
            @Valid @RequestBody GymRequest request) {
        return gymService.update(principal.id(), gymId, request.toCommand());
    }

    @DeleteMapping("/{gymId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID gymId) {
        gymService.delete(principal.id(), gymId);
    }
}
