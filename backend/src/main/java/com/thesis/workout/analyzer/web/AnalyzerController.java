package com.thesis.workout.analyzer.web;

import com.thesis.workout.analyzer.application.TemplateAnalyzerService;
import com.thesis.workout.analyzer.web.dto.AnalysisResponse;
import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/templates")
public class AnalyzerController {

    private final TemplateAnalyzerService analyzerService;

    public AnalyzerController(TemplateAnalyzerService analyzerService) {
        this.analyzerService = analyzerService;
    }

    @GetMapping("/{templateId}/analysis")
    public AnalysisResponse analyze(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateId) {
        return analyzerService.analyze(principal.id(), templateId);
    }
}
