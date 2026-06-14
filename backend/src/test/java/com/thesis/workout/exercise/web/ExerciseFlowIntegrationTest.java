package com.thesis.workout.exercise.web;

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
import java.util.List;
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
class ExerciseFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void muscleGroupsAreSeededAndReadable() throws Exception {
        String token = registerAndToken();

        MvcResult result = mockMvc.perform(get("/api/muscle-groups").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(15))
                .andReturn();

        JsonNode chest = findByField(body(result), "code", "CHEST");
        assertThat(chest).isNotNull();
        assertThat(chest.get("displayName").asText()).isEqualTo("Chest");
    }

    @Test
    void officialCatalogIsSeededAndVisibleToEveryUser() throws Exception {
        String token = registerAndToken();

        MvcResult official = mockMvc.perform(get("/api/exercises/official").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode squat = findByField(body(official), "name", "Barbell Back Squat");
        assertThat(squat).isNotNull();
        assertThat(squat.get("visibility").asText()).isEqualTo("OFFICIAL");
        assertThat(findByField(squat.get("muscleGroups"), "code", "QUADS").get("role").asText()).isEqualTo("PRIMARY");

        // The visible feed (official + own custom) also surfaces the official catalog.
        MvcResult visible = mockMvc.perform(get("/api/exercises").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(findByField(body(visible), "name", "Barbell Deadlift")).isNotNull();
    }

    @Test
    void createCustomExerciseReturnsItWithMuscleGroups() throws Exception {
        String token = registerAndToken();
        String name = uniqueName("Cable Fly");

        MvcResult created = mockMvc.perform(createCustom(token, name, "STRENGTH",
                        List.of(Map.of("code", "CHEST", "role", "PRIMARY"),
                                Map.of("code", "SHOULDERS", "role", "SECONDARY"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.visibility").value("CUSTOM"))
                .andExpect(jsonPath("$.exerciseType").value("STRENGTH"))
                .andExpect(jsonPath("$.muscleGroups.length()").value(2))
                .andReturn();

        UUID id = idOf(created);
        MvcResult custom = mockMvc.perform(get("/api/exercises/custom").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode found = findByField(body(custom), "id", id.toString());
        assertThat(found).isNotNull();
        assertThat(found.get("name").asText()).isEqualTo(name);
    }

    @Test
    void duplicateCustomNameIsRejected() throws Exception {
        String token = registerAndToken();
        String name = uniqueName("Pec Deck");

        mockMvc.perform(createCustom(token, name, "STRENGTH", List.of())).andExpect(status().isCreated());

        mockMvc.perform(createCustom(token, name.toUpperCase(), "STRENGTH", List.of()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EXERCISE_NAME_TAKEN"));
    }

    @Test
    void unknownMuscleGroupIsRejected() throws Exception {
        String token = registerAndToken();

        mockMvc.perform(createCustom(token, uniqueName("Bad Exercise"), "STRENGTH",
                        List.of(Map.of("code", "NOT_A_MUSCLE", "role", "PRIMARY"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_MUSCLE_GROUP"));
    }

    @Test
    void missingNameFailsValidation() throws Exception {
        String token = registerAndToken();

        mockMvc.perform(post("/api/exercises/custom")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("exerciseType", "STRENGTH"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void updateReplacesDetailsAndMuscleGroups() throws Exception {
        String token = registerAndToken();
        UUID id = idOf(mockMvc.perform(createCustom(token, uniqueName("Row Variation"), "STRENGTH",
                        List.of(Map.of("code", "BACK", "role", "PRIMARY"))))
                .andReturn());

        String newName = uniqueName("Seated Row");
        MvcResult updated = mockMvc.perform(put("/api/exercises/custom/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", newName,
                                "exerciseType", "STRENGTH",
                                "muscleGroups", List.of(
                                        Map.of("code", "LATS", "role", "PRIMARY"),
                                        Map.of("code", "BICEPS", "role", "SECONDARY"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.muscleGroups.length()").value(2))
                .andReturn();

        JsonNode muscleGroups = body(updated).get("muscleGroups");
        assertThat(findByField(muscleGroups, "code", "LATS").get("role").asText()).isEqualTo("PRIMARY");
        assertThat(findByField(muscleGroups, "code", "BACK")).isNull();
    }

    @Test
    void deleteSoftRemovesAndFreesTheName() throws Exception {
        String token = registerAndToken();
        String name = uniqueName("Temp Exercise");
        UUID id = idOf(mockMvc.perform(createCustom(token, name, "STRENGTH", List.of())).andReturn());

        mockMvc.perform(delete("/api/exercises/custom/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        MvcResult afterDelete = mockMvc.perform(get("/api/exercises/custom").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(findByField(body(afterDelete), "id", id.toString())).isNull();

        // Soft delete excludes the row from the partial unique index, so the name is reusable.
        mockMvc.perform(createCustom(token, name, "STRENGTH", List.of())).andExpect(status().isCreated());
    }

    @Test
    void anotherUsersCustomExerciseIsNotFound() throws Exception {
        String ownerToken = registerAndToken();
        String intruderToken = registerAndToken();
        UUID id = idOf(mockMvc.perform(createCustom(ownerToken, uniqueName("Private Lift"), "STRENGTH", List.of()))
                .andReturn());

        // Intruder cannot see it in their own custom list.
        MvcResult intruderList = mockMvc.perform(
                        get("/api/exercises/custom").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(findByField(body(intruderList), "id", id.toString())).isNull();

        // Update / delete by the intruder must be indistinguishable from a missing resource: 404.
        mockMvc.perform(put("/api/exercises/custom/" + id)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Hijacked", "exerciseType", "STRENGTH"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EXERCISE_NOT_FOUND"));

        mockMvc.perform(delete("/api/exercises/custom/" + id).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EXERCISE_NOT_FOUND"));
    }

    @Test
    void officialExerciseCannotBeMutatedThroughCustomEndpoints() throws Exception {
        String token = registerAndToken();
        // Fixed seed UUID for "Barbell Back Squat" (V2 migration); it is OFFICIAL (owner null).
        String officialId = "00000000-0000-0000-0000-000000000101";

        mockMvc.perform(delete("/api/exercises/custom/" + officialId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EXERCISE_NOT_FOUND"));
    }

    @Test
    void exerciseEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    private MockHttpServletRequestBuilder createCustom(String token, String name, String type,
            List<Map<String, String>> muscleGroups) throws Exception {
        return post("/api/exercises/custom")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", name,
                        "exerciseType", type,
                        "muscleGroups", muscleGroups)));
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

    /** Returns the first array element whose {@code field} equals {@code value}, or null. */
    private static JsonNode findByField(JsonNode array, String field, String value) {
        for (JsonNode element : array) {
            JsonNode candidate = element.get(field);
            if (candidate != null && candidate.asText().equals(value)) {
                return element;
            }
        }
        return null;
    }

    private static String uniqueName(String base) {
        return base + " " + UUID.randomUUID();
    }
}
