package com.thesis.workout.search.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the OpenSearch-backed derived read model. The feature is opt-in
 * ({@code enabled}); when off, no OpenSearch client is created, the after-commit indexing
 * listeners are not registered, and {@code /api/search/**} returns 503 so the UI degrades to the
 * authoritative SQL browse/list. Index names are versioned ({@code *_v1}) and addressed through a
 * stable alias so a rebuild can atomically swap to a freshly-built index.
 */
@ConfigurationProperties(prefix = "app.search")
public record SearchProperties(
        boolean enabled,
        String uri,
        int connectTimeoutMs,
        int socketTimeoutMs,
        String templatesIndex,
        String templatesAlias,
        String sessionsIndex,
        String sessionsAlias) {

    public SearchProperties {
        if (uri == null || uri.isBlank()) {
            uri = "http://localhost:9200";
        }
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = 2000;
        }
        if (socketTimeoutMs <= 0) {
            socketTimeoutMs = 5000;
        }
        if (templatesIndex == null || templatesIndex.isBlank()) {
            templatesIndex = "templates_v1";
        }
        if (templatesAlias == null || templatesAlias.isBlank()) {
            templatesAlias = "templates";
        }
        if (sessionsIndex == null || sessionsIndex.isBlank()) {
            sessionsIndex = "workout_sessions_v1";
        }
        if (sessionsAlias == null || sessionsAlias.isBlank()) {
            sessionsAlias = "workout_sessions";
        }
    }
}
