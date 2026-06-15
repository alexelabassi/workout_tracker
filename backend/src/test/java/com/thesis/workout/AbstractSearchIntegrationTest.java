package com.thesis.workout;

import org.opensearch.testcontainers.OpensearchContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for tests that exercise the real OpenSearch-backed search/read model. Boots a real
 * single-node OpenSearch 2.x container alongside the shared PostgreSQL container and turns
 * {@code app.search.enabled} on, so the indexing listeners, indexers and search services are all
 * wired exactly as in production. No mocks, no H2, no in-memory fakes.
 */
public abstract class AbstractSearchIntegrationTest {

    static final OpensearchContainer<?> OPENSEARCH =
            new OpensearchContainer<>(DockerImageName.parse("opensearchproject/opensearch:2.11.1"));

    static {
        // Reuse the singleton PostgreSQL container so the search suite does not start a second DB.
        AbstractPostgresIntegrationTest.POSTGRES.start();
        OPENSEARCH.start();
    }

    @DynamicPropertySource
    static void searchProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", AbstractPostgresIntegrationTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", AbstractPostgresIntegrationTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", AbstractPostgresIntegrationTest.POSTGRES::getPassword);
        registry.add("app.search.enabled", () -> "true");
        registry.add("app.search.uri", AbstractSearchIntegrationTest::openSearchUri);
    }

    private static String openSearchUri() {
        String address = OPENSEARCH.getHttpHostAddress();
        return address.startsWith("http") ? address : "http://" + address;
    }
}
