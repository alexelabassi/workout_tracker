package com.thesis.workout.history.web;

import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import com.thesis.workout.history.application.HistoryService;
import com.thesis.workout.history.web.dto.HistoryItemResponse;
import com.thesis.workout.history.web.dto.PagedResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public PagedResponse<HistoryItemResponse> list(@AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return historyService.list(principal.id(), page, size);
    }
}
