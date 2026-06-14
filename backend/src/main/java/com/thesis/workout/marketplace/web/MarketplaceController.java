package com.thesis.workout.marketplace.web;

import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import com.thesis.workout.marketplace.application.MarketplaceQueryService;
import com.thesis.workout.marketplace.application.TemplateCopyService;
import com.thesis.workout.marketplace.application.TemplateInteractionService;
import com.thesis.workout.marketplace.web.dto.MarketplaceInteractionResponse;
import com.thesis.workout.marketplace.web.dto.MarketplaceTemplateDetailResponse;
import com.thesis.workout.marketplace.web.dto.MarketplaceTemplateSummaryResponse;
import com.thesis.workout.marketplace.web.dto.VoteRequest;
import com.thesis.workout.shared.web.PagedResponse;
import com.thesis.workout.template.web.dto.TemplateDetailResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marketplace/templates")
public class MarketplaceController {

    private final MarketplaceQueryService queryService;
    private final TemplateInteractionService interactionService;
    private final TemplateCopyService copyService;

    public MarketplaceController(MarketplaceQueryService queryService,
            TemplateInteractionService interactionService, TemplateCopyService copyService) {
        this.queryService = queryService;
        this.interactionService = interactionService;
        this.copyService = copyService;
    }

    @GetMapping
    public PagedResponse<MarketplaceTemplateSummaryResponse> browse(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(required = false) String splitType,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Integer daysPerWeek,
            @RequestParam(defaultValue = "false") boolean savedOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return queryService.browse(principal.id(), sort, splitType, difficulty, daysPerWeek, savedOnly, page, size);
    }

    @GetMapping("/{templateId}")
    public MarketplaceTemplateDetailResponse detail(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateId) {
        return queryService.detail(principal.id(), templateId);
    }

    @PostMapping("/{templateId}/vote")
    public MarketplaceInteractionResponse vote(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateId, @Valid @RequestBody VoteRequest request) {
        return interactionService.vote(principal.id(), templateId, request.voteType());
    }

    @DeleteMapping("/{templateId}/vote")
    public MarketplaceInteractionResponse clearVote(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateId) {
        return interactionService.clearVote(principal.id(), templateId);
    }

    @PostMapping("/{templateId}/save")
    public MarketplaceInteractionResponse save(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateId) {
        return interactionService.save(principal.id(), templateId);
    }

    @DeleteMapping("/{templateId}/save")
    public MarketplaceInteractionResponse unsave(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateId) {
        return interactionService.unsave(principal.id(), templateId);
    }

    @PostMapping("/{templateId}/use")
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateDetailResponse use(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID templateId) {
        return copyService.use(principal.id(), templateId);
    }
}
