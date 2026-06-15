package com.thesis.workout.search.infrastructure;

import java.net.URI;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the official low-level OpenSearch Java client on the Apache HttpClient5 transport. The
 * client (and therefore every search bean that depends on it) only exists when
 * {@code app.search.enabled=true}; otherwise the application boots and serves the SQL-backed
 * features with no OpenSearch dependency at all.
 */
@Configuration
@EnableConfigurationProperties(SearchProperties.class)
@ConditionalOnProperty(name = "app.search.enabled", havingValue = "true")
public class OpenSearchClientConfig {

    @Bean(destroyMethod = "close")
    OpenSearchTransport openSearchTransport(SearchProperties properties) {
        HttpHost host = HttpHost.create(URI.create(properties.uri()));
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(properties.connectTimeoutMs()))
                .setResponseTimeout(Timeout.ofMilliseconds(properties.socketTimeoutMs()))
                .build();
        return ApacheHttpClient5TransportBuilder.builder(host)
                .setMapper(new JacksonJsonpMapper())
                .setHttpClientConfigCallback(builder -> builder.setDefaultRequestConfig(requestConfig))
                .build();
    }

    @Bean
    OpenSearchClient openSearchClient(OpenSearchTransport transport) {
        return new OpenSearchClient(transport);
    }
}
