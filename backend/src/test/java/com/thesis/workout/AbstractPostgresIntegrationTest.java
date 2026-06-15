package com.thesis.workout;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests. Boots a real PostgreSQL 16 container once per JVM
 * (singleton pattern) so the full Flyway-managed schema is exercised. No H2.
 */
public abstract class AbstractPostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("workout")
                    .withUsername("workout")
                    .withPassword("workout");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // The default search suite runs without OpenSearch: keep the derived read model off so no
        // search beans load and the existing tests never need an OpenSearch node. Search-specific
        // tests opt in via AbstractSearchIntegrationTest.
        registry.add("app.search.enabled", () -> "false");
    }
}
