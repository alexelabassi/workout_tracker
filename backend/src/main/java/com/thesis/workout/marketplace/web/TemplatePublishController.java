package com.thesis.workout.marketplace.web;

import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import com.thesis.workout.marketplace.application.TemplatePublishingService;
import com.thesis.workout.template.web.dto.TemplateDetailResponse;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/templates")
public class TemplatePublishController {

    private final TemplatePublishingService publishingService;

    public TemplatePublishController(TemplatePublishingService publishingService) {
        this.publishingService = publishingService;
    }

    @PostMapping("/{templateId}/publish")
    public TemplateDetailResponse publish(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateId) {
        return publishingService.publish(principal.id(), templateId);
    }

    @PostMapping("/{templateId}/unpublish")
    public TemplateDetailResponse unpublish(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateId) {
        return publishingService.unpublish(principal.id(), templateId);
    }
}
