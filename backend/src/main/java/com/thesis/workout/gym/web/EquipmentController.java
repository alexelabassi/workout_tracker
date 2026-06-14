package com.thesis.workout.gym.web;

import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import com.thesis.workout.gym.application.EquipmentService;
import com.thesis.workout.gym.web.dto.EquipmentRequest;
import com.thesis.workout.gym.web.dto.EquipmentResponse;
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

/**
 * Equipment is gym-scoped for list/create (nested under the gym) but addressed directly by id
 * for update/delete, matching docs/API_CONTRACT.md.
 */
@RestController
@RequestMapping("/api")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping("/gyms/{gymId}/equipment")
    public List<EquipmentResponse> list(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID gymId) {
        return equipmentService.list(principal.id(), gymId);
    }

    @PostMapping("/gyms/{gymId}/equipment")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentResponse create(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID gymId,
            @Valid @RequestBody EquipmentRequest request) {
        return equipmentService.create(principal.id(), gymId, request.toCommand());
    }

    @PutMapping("/equipment/{equipmentId}")
    public EquipmentResponse update(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID equipmentId,
            @Valid @RequestBody EquipmentRequest request) {
        return equipmentService.update(principal.id(), equipmentId, request.toCommand());
    }

    @DeleteMapping("/equipment/{equipmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID equipmentId) {
        equipmentService.delete(principal.id(), equipmentId);
    }
}
