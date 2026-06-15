package com.thesis.workout.search.web;

import com.thesis.workout.search.application.SearchRebuildService;
import com.thesis.workout.search.application.SearchRebuildService.RebuildResult;
import com.thesis.workout.search.application.exception.SearchUnavailableException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual full rebuild of the search indexes from PostgreSQL. Locked to ROLE_ADMIN by the security
 * configuration ({@code /api/admin/**}); ordinary users can never trigger a rebuild. For the
 * local/demo stack the same rebuild runs in-process from the demo seeder, so the index is populated
 * even without an admin account.
 */
@RestController
@RequestMapping("/api/admin/search")
public class AdminSearchController {

    private final ObjectProvider<SearchRebuildService> rebuildService;

    public AdminSearchController(ObjectProvider<SearchRebuildService> rebuildService) {
        this.rebuildService = rebuildService;
    }

    @PostMapping("/reindex")
    public RebuildResult reindex() {
        SearchRebuildService service = rebuildService.getIfAvailable();
        if (service == null) {
            throw new SearchUnavailableException();
        }
        return service.rebuildAll();
    }
}
