package com.thesis.workout.template.web;

import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import com.thesis.workout.template.application.TemplateDayExerciseService;
import com.thesis.workout.template.web.dto.TemplateDayExerciseRequest;
import com.thesis.workout.template.web.dto.TemplateDayExerciseResponse;
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
public class TemplateDayExerciseController {

    private final TemplateDayExerciseService templateDayExerciseService;

    public TemplateDayExerciseController(TemplateDayExerciseService templateDayExerciseService) {
        this.templateDayExerciseService = templateDayExerciseService;
    }

    @PostMapping("/template-days/{templateDayId}/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateDayExerciseResponse add(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateDayId,
            @Valid @RequestBody TemplateDayExerciseRequest request) {
        return templateDayExerciseService.add(principal.id(), templateDayId, request.toCommand());
    }

    @PutMapping("/template-day-exercises/{templateDayExerciseId}")
    public TemplateDayExerciseResponse update(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateDayExerciseId,
            @Valid @RequestBody TemplateDayExerciseRequest request) {
        return templateDayExerciseService.update(principal.id(), templateDayExerciseId, request.toCommand());
    }

    @DeleteMapping("/template-day-exercises/{templateDayExerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateDayExerciseId) {
        templateDayExerciseService.delete(principal.id(), templateDayExerciseId);
    }
}
