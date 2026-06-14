package com.thesis.workout.template.web;

import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import com.thesis.workout.template.application.TemplateService;
import com.thesis.workout.template.web.dto.TemplateDetailResponse;
import com.thesis.workout.template.web.dto.TemplateRequest;
import com.thesis.workout.template.web.dto.TemplateSummaryResponse;
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
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public List<TemplateSummaryResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return templateService.list(principal.id());
    }

    @GetMapping("/{templateId}")
    public TemplateDetailResponse get(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateId) {
        return templateService.get(principal.id(), templateId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateDetailResponse create(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TemplateRequest request) {
        return templateService.create(principal.id(), request.toCommand());
    }

    @PutMapping("/{templateId}")
    public TemplateDetailResponse update(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateId,
            @Valid @RequestBody TemplateRequest request) {
        return templateService.update(principal.id(), templateId, request.toCommand());
    }

    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID templateId) {
        templateService.delete(principal.id(), templateId);
    }
}
