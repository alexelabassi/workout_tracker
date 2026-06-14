package com.thesis.workout.routine.web;

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
class RoutineFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createReturnsCreatedRoutineAndItAppearsInTheList() throws Exception {
        String token = registerAndToken();
        String name = uniqueName("Warm-up");

        MvcResult created = mockMvc.perform(createRoutine(token, name, "START", "Bike 5 min, mobility drills."))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.routineType").value("START"))
                .andExpect(jsonPath("$.content").value("Bike 5 min, mobility drills."))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn();

        UUID id = idOf(created);
        MvcResult list = mockMvc.perform(get("/api/routines").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode found = findById(body(list), id);
        assertThat(found).isNotNull();
        assertThat(found.get("name").asText()).isEqualTo(name);
    }

    @Test
    void duplicateNameAndTypeIsRejected() throws Exception {
        String token = registerAndToken();
        String name = uniqueName("Cooldown");

        mockMvc.perform(createRoutine(token, name, "END", "Stretch.")).andExpect(status().isCreated());

        mockMvc.perform(createRoutine(token, name.toUpperCase(), "END", "Other text."))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ROUTINE_NAME_TAKEN"));
    }

    @Test
    void sameNameDifferentTypeIsAllowed() throws Exception {
        String token = registerAndToken();
        String name = uniqueName("Morning");

        mockMvc.perform(createRoutine(token, name, "START", "Warm up.")).andExpect(status().isCreated());
        mockMvc.perform(createRoutine(token, name, "END", "Cool down.")).andExpect(status().isCreated());
    }

    @Test
    void updateReplacesNameTypeAndContent() throws Exception {
        String token = registerAndToken();
        UUID id = idOf(mockMvc.perform(createRoutine(token, uniqueName("Draft"), "START", "v1")).andReturn());

        String newName = uniqueName("Final");
        mockMvc.perform(put("/api/routines/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", newName, "routineType", "END", "content", "v2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.routineType").value("END"))
                .andExpect(jsonPath("$.content").value("v2"));
    }

    @Test
    void deleteSoftRemovesAndFreesTheName() throws Exception {
        String token = registerAndToken();
        String name = uniqueName("Temp");
        UUID id = idOf(mockMvc.perform(createRoutine(token, name, "START", "x")).andReturn());

        mockMvc.perform(delete("/api/routines/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        MvcResult afterDelete = mockMvc.perform(get("/api/routines").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(findById(body(afterDelete), id)).isNull();

        // Soft delete excludes the row from the partial unique index, so the name is reusable.
        mockMvc.perform(createRoutine(token, name, "START", "y")).andExpect(status().isCreated());
    }

    @Test
    void deletingAlreadyDeletedRoutineReturnsNotFound() throws Exception {
        String token = registerAndToken();
        UUID id = idOf(mockMvc.perform(createRoutine(token, uniqueName("Once"), "START", "x")).andReturn());

        mockMvc.perform(delete("/api/routines/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/routines/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ROUTINE_NOT_FOUND"));
    }

    @Test
    void anotherUsersRoutineIsNotFound() throws Exception {
        String ownerToken = registerAndToken();
        String intruderToken = registerAndToken();
        UUID id = idOf(mockMvc.perform(createRoutine(ownerToken, uniqueName("Private"), "START", "secret"))
                .andReturn());

        // Intruder cannot see it in their own list.
        MvcResult intruderList = mockMvc.perform(get("/api/routines").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(findById(body(intruderList), id)).isNull();

        // Update / delete by the intruder must be indistinguishable from a missing resource: 404.
        mockMvc.perform(put("/api/routines/" + id)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Hijacked", "routineType", "START", "content", "z"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ROUTINE_NOT_FOUND"));

        mockMvc.perform(delete("/api/routines/" + id).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ROUTINE_NOT_FOUND"));
    }

    @Test
    void invalidPayloadsFailValidation() throws Exception {
        String token = registerAndToken();

        // Blank name.
        mockMvc.perform(createRoutine(token, "  ", "START", "content"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        // Missing content.
        mockMvc.perform(post("/api/routines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "X", "routineType", "START"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        // Bad routine type.
        mockMvc.perform(post("/api/routines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "X", "routineType", "MIDDLE", "content", "c"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void routineEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/routines"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    private MockHttpServletRequestBuilder createRoutine(String token, String name, String type, String content)
            throws Exception {
        return post("/api/routines")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", name, "routineType", type, "content", content)));
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
