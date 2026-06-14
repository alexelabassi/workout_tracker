package com.thesis.workout.gym.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.workout.AbstractPostgresIntegrationTest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class GymFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createReturnsGymAndItAppearsInTheListAndById() throws Exception {
        String token = registerAndToken();
        String name = uniqueName("Iron Temple");

        MvcResult created = mockMvc.perform(createGym(token, name, "Downtown"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.location").value("Downtown"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn();

        UUID id = idOf(created);
        mockMvc.perform(get("/api/gyms/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));

        MvcResult list = mockMvc.perform(get("/api/gyms").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(findById(body(list), id)).isNotNull();
    }

    @Test
    void duplicateNameIsRejected() throws Exception {
        String token = registerAndToken();
        String name = uniqueName("Gym");

        mockMvc.perform(createGym(token, name, null)).andExpect(status().isCreated());
        mockMvc.perform(createGym(token, name.toUpperCase(), null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("GYM_NAME_TAKEN"));
    }

    @Test
    void softDeleteFreesTheName() throws Exception {
        String token = registerAndToken();
        String name = uniqueName("Temp Gym");
        UUID id = idOf(mockMvc.perform(createGym(token, name, null)).andReturn());

        mockMvc.perform(delete("/api/gyms/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        MvcResult afterDelete = mockMvc.perform(get("/api/gyms").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(findById(body(afterDelete), id)).isNull();
        mockMvc.perform(get("/api/gyms/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        mockMvc.perform(createGym(token, name, null)).andExpect(status().isCreated());
    }

    @Test
    void updateChangesNameAndLocation() throws Exception {
        String token = registerAndToken();
        UUID id = idOf(mockMvc.perform(createGym(token, uniqueName("Old"), "A")).andReturn());

        String newName = uniqueName("New");
        mockMvc.perform(put("/api/gyms/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", newName, "location", "B"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.location").value("B"));
    }

    @Test
    void anotherUsersGymIsNotFound() throws Exception {
        String ownerToken = registerAndToken();
        String intruderToken = registerAndToken();
        UUID id = idOf(mockMvc.perform(createGym(ownerToken, uniqueName("Private Gym"), null)).andReturn());

        mockMvc.perform(get("/api/gyms/" + id).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("GYM_NOT_FOUND"));
        mockMvc.perform(put("/api/gyms/" + id)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Hijack"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/gyms/" + id).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidPayloadsFailValidation() throws Exception {
        String token = registerAndToken();

        mockMvc.perform(createGym(token, "  ", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        Map<String, Object> tooLongLocation = new HashMap<>();
        tooLongLocation.put("name", "Valid");
        tooLongLocation.put("location", "x".repeat(256));
        mockMvc.perform(post("/api/gyms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooLongLocation)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void gymEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/gyms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    private MockHttpServletRequestBuilder createGym(String token, String name, String location) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        if (location != null) {
            payload.put("location", location);
        }
        return post("/api/gyms")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload));
    }

    private String registerAndToken() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", PASSWORD, "displayName", "Tester"))))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("accessToken").asText();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UUID idOf(MvcResult result) throws Exception {
        return UUID.fromString(body(result).get("id").asText());
    }

    private static JsonNode findById(JsonNode array, UUID id) {
        for (JsonNode element : array) {
            JsonNode candidate = element.get("id");
            if (candidate != null && candidate.asText().equals(id.toString())) {
                return element;
            }
        }
        return null;
    }

    private static String uniqueName(String base) {
        return base + " " + UUID.randomUUID();
    }
}
