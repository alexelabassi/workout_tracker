package com.thesis.workout.search.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.stream.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Set;
import org.opensearch.client.json.JsonpDeserializer;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper over the OpenSearch indices API for the operations this project needs: create an
 * index from a bundled JSON mapping, check/lookup the index behind an alias, atomically repoint an
 * alias, and delete an index. Versioned indexes ({@code *_v1}) are always addressed through a
 * stable alias so a rebuild can swap to a freshly-built index with zero read downtime, degrading to
 * delete-and-recreate only when no clean swap is possible.
 */
@Component
@ConditionalOnProperty(name = "app.search.enabled", havingValue = "true")
public class OpenSearchAdmin {

    private final OpenSearchClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenSearchAdmin(OpenSearchClient client) {
        this.client = client;
    }

    /** True if a concrete index with this exact name exists. */
    public boolean indexExists(String index) {
        try {
            return client.indices().exists(e -> e.index(index)).value();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to check index existence: " + index, ex);
        }
    }

    public boolean aliasExists(String alias) {
        try {
            return client.indices().existsAlias(e -> e.name(alias)).value();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to check alias existence: " + alias, ex);
        }
    }

    /**
     * Creates the index with settings + mappings loaded from {@code classpath:search/<resource>}.
     * The bundled JSON holds {@code settings} and {@code mappings} blocks; each is deserialized with
     * its typed deserializer and the index is created under the concrete (versioned) name.
     */
    public void createIndex(String index, String mappingResource) {
        JsonpMapper mapper = client._transport().jsonpMapper();
        JsonNode root;
        try (InputStream mapping = new ClassPathResource("search/" + mappingResource).getInputStream()) {
            root = objectMapper.readTree(mapping);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read index mapping resource: " + mappingResource, ex);
        }
        IndexSettings settings = deserialize(root.get("settings"), IndexSettings._DESERIALIZER, mapper);
        TypeMapping mappings = deserialize(root.get("mappings"), TypeMapping._DESERIALIZER, mapper);
        try {
            client.indices().create(c -> c.index(index).settings(settings).mappings(mappings));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create index: " + index, ex);
        }
    }

    private <T> T deserialize(JsonNode node, JsonpDeserializer<T> deserializer, JsonpMapper mapper) {
        if (node == null || node.isNull()) {
            return null;
        }
        JsonParser parser = mapper.jsonProvider().createParser(new StringReader(node.toString()));
        return deserializer.deserialize(parser, mapper);
    }

    /** Returns the concrete index names the alias currently points at (empty if none). */
    public Set<String> indicesBehindAlias(String alias) {
        try {
            if (!aliasExists(alias)) {
                return Set.of();
            }
            return Set.copyOf(client.indices().getAlias(g -> g.name(alias)).result().keySet());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to resolve alias: " + alias, ex);
        }
    }

    /**
     * Ensures the alias is readable/writable: if neither the alias nor the versioned index exist,
     * the index is created and the alias is pointed at it. Idempotent — safe to call before every
     * single-document write so the first indexing operation works without a full rebuild.
     */
    public void ensureAliasReady(String alias, String index, String mappingResource) {
        if (aliasExists(alias)) {
            return;
        }
        if (!indexExists(index)) {
            createIndex(index, mappingResource);
        }
        addAlias(alias, index);
    }

    public void addAlias(String alias, String index) {
        try {
            client.indices().updateAliases(u -> u.actions(a -> a.add(add -> add.index(index).alias(alias))));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to add alias " + alias + " -> " + index, ex);
        }
    }

    /**
     * Atomically repoints {@code alias} to {@code newIndex}, removing it from every {@code oldIndices}
     * member in the same request so reads never see a missing or doubled alias.
     */
    public void swapAlias(String alias, String newIndex, Set<String> oldIndices) {
        try {
            client.indices().updateAliases(u -> {
                u.actions(a -> a.add(add -> add.index(newIndex).alias(alias)));
                for (String old : oldIndices) {
                    if (!old.equals(newIndex)) {
                        u.actions(a -> a.remove(remove -> remove.index(old).alias(alias)));
                    }
                }
                return u;
            });
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to swap alias " + alias + " -> " + newIndex, ex);
        }
    }

    public void deleteIndexIfExists(String index) {
        try {
            if (indexExists(index)) {
                client.indices().delete(d -> d.index(index));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete index: " + index, ex);
        }
    }

    /** Best-effort refresh so just-indexed documents are immediately searchable (used in tests/rebuild). */
    public void refresh(String aliasOrIndex) {
        try {
            client.indices().refresh(r -> r.index(aliasOrIndex));
        } catch (IOException | OpenSearchException ex) {
            // Refresh is an optimisation; a failure must not break the calling flow.
        }
    }
}
