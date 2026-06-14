package com.thesis.workout.template.web;

import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import com.thesis.workout.template.application.TemplateDayService;
import com.thesis.workout.template.web.dto.TemplateDayRequest;
import com.thesis.workout.template.web.dto.TemplateDayResponse;
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
public class TemplateDayController {

    private final TemplateDayService templateDayService;

    public TemplateDayController(TemplateDayService templateDayService) {
        this.templateDayService = templateDayService;
    }

    @PostMapping("/templates/{templateId}/days")
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateDayResponse create(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateId,
            @Valid @RequestBody TemplateDayRequest request) {
        return templateDayService.create(principal.id(), templateId, request.toCommand());
    }

    @PutMapping("/template-days/{templateDayId}")
    public TemplateDayResponse update(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateDayId,
            @Valid @RequestBody TemplateDayRequest request) {
        return templateDayService.update(principal.id(), templateDayId, request.toCommand());
    }

    @DeleteMapping("/template-days/{templateDayId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID templateDayId) {
        templateDayService.delete(principal.id(), templateDayId);
    }
}
