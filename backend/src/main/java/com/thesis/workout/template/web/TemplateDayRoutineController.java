package com.thesis.workout.template.web;

import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import com.thesis.workout.template.application.TemplateDayRoutineService;
import com.thesis.workout.template.web.dto.TemplateDayRoutineRequest;
import com.thesis.workout.template.web.dto.TemplateDayRoutineResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TemplateDayRoutineController {

    private final TemplateDayRoutineService templateDayRoutineService;

    public TemplateDayRoutineController(TemplateDayRoutineService templateDayRoutineService) {
        this.templateDayRoutineService = templateDayRoutineService;
    }

    @PostMapping("/template-days/{templateDayId}/routines")
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateDayRoutineResponse attach(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateDayId,
            @Valid @RequestBody TemplateDayRoutineRequest request) {
        return templateDayRoutineService.attach(principal.id(), templateDayId, request.routineId());
    }

    @DeleteMapping("/template-day-routines/{templateDayRoutineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateDayRoutineId) {
        templateDayRoutineService.remove(principal.id(), templateDayRoutineId);
    }
}
