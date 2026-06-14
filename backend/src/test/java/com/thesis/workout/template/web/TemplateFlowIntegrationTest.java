package com.thesis.workout.template.web;

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
import java.sql.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TemplateFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret!";
    // Seeded official exercise "Barbell Back Squat" (V2 migration): QUADS/GLUTES/HAMSTRINGS/CORE.
    private static final String OFFICIAL_SQUAT_ID = "00000000-0000-0000-0000-000000000101";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void templateCrudAndDetail() throws Exception {
        String token = registerAndToken();
        String name = uniqueName("PPL");

        MvcResult created = mockMvc.perform(createTemplate(token, name))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.days.length()").value(0))
                .andReturn();
        UUID id = idOf(created);

        mockMvc.perform(get("/api/templates").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/templates/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));

        mockMvc.perform(put("/api/templates/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name + " v2", "splitType", "PPL", "daysPerWeek", 6, "difficulty", "INTERMEDIATE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name + " v2"))
                .andExpect(jsonPath("$.splitType").value("PPL"))
                .andExpect(jsonPath("$.daysPerWeek").value(6));

        mockMvc.perform(delete("/api/templates/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/templates/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TEMPLATE_NOT_FOUND"));
    }

    @Test
    void creatingTemplateCreatesZeroedStatsRow() throws Exception {
        String token = registerAndToken();
        UUID id = idOf(mockMvc.perform(createTemplate(token, uniqueName("Stats"))).andReturn());

        Integer upvotes = jdbcTemplate.queryForObject(
                "SELECT upvotes_count FROM template_stats WHERE template_id = ?", Integer.class, id);
        Integer uses = jdbcTemplate.queryForObject(
                "SELECT uses_count FROM template_stats WHERE template_id = ?", Integer.class, id);
        assertThat(upvotes).isZero();
        assertThat(uses).isZero();
    }

    @Test
    void templateIdorReturns404() throws Exception {
        String ownerToken = registerAndToken();
        String intruderToken = registerAndToken();
        UUID id = idOf(mockMvc.perform(createTemplate(ownerToken, uniqueName("Private Program"))).andReturn());

        mockMvc.perform(get("/api/templates/" + id).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/templates/" + id)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Hijack"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/templates/" + id).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void dayCrudAndDuplicateNumber() throws Exception {
        String token = registerAndToken();
        UUID templateId = idOf(mockMvc.perform(createTemplate(token, uniqueName("Split"))).andReturn());

        UUID dayId = idOf(mockMvc.perform(createDay(token, templateId, 1, "Push")).andReturn());

        mockMvc.perform(createDay(token, templateId, 1, "Duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DAY_NUMBER_TAKEN"));

        mockMvc.perform(put("/api/template-days/" + dayId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("dayNumber", 1, "name", "Push Day", "focus", "PUSH"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Push Day"))
                .andExpect(jsonPath("$.focus").value("PUSH"));

        mockMvc.perform(delete("/api/template-days/" + dayId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void addingOfficialExerciseSnapshotsAndRecomputesAggregates() throws Exception {
        String token = registerAndToken();
        UUID templateId = idOf(mockMvc.perform(createTemplate(token, uniqueName("Strength"))).andReturn());
        UUID dayId = idOf(mockMvc.perform(createDay(token, templateId, 1, "Legs")).andReturn());

        Map<String, Object> payload = new HashMap<>();
        payload.put("exerciseId", OFFICIAL_SQUAT_ID);
        payload.put("plannedSets", 5);
        payload.put("plannedReps", "5");
        MvcResult added = mockMvc.perform(post("/api/template-days/" + dayId + "/exercises")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exerciseName").value("Barbell Back Squat"))
                .andExpect(jsonPath("$.exerciseType").value("STRENGTH"))
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.muscleGroups.length()").value(4))
                .andReturn();
        UUID exerciseRowId = idOf(added);

        // Snapshot is visible through the detail tree.
        MvcResult detail = mockMvc.perform(get("/api/templates/" + templateId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode squat = body(detail).get("days").get(0).get("exercises").get(0);
        assertThat(squat.get("exerciseName").asText()).isEqualTo("Barbell Back Squat");

        // Aggregates recomputed Postgres-side.
        assertThat(stringArray(templateId, "aggregated_muscle_groups"))
                .containsExactlyInAnyOrder("QUADS", "GLUTES", "HAMSTRINGS", "CORE");
        assertThat(stringArray(templateId, "aggregated_official_exercise_ids")).containsExactly(OFFICIAL_SQUAT_ID);
        assertThat(stringArray(templateId, "aggregated_exercise_names")).containsExactly("Barbell Back Squat");

        // Removing the exercise shrinks the aggregates back to empty.
        mockMvc.perform(delete("/api/template-day-exercises/" + exerciseRowId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(stringArray(templateId, "aggregated_muscle_groups")).isEmpty();
        assertThat(stringArray(templateId, "aggregated_official_exercise_ids")).isEmpty();
        assertThat(stringArray(templateId, "aggregated_exercise_names")).isEmpty();
    }

    @Test
    void customExerciseOwnedByAnotherUserCannotBeAdded() throws Exception {
        String ownerToken = registerAndToken();
        UUID foreignExerciseId = idOf(mockMvc.perform(createCustomExercise(ownerToken, uniqueName("Secret Lift")))
                .andReturn());

        String intruderToken = registerAndToken();
        UUID templateId = idOf(mockMvc.perform(createTemplate(intruderToken, uniqueName("Intruder"))).andReturn());
        UUID dayId = idOf(mockMvc.perform(createDay(intruderToken, templateId, 1, "Day")).andReturn());

        mockMvc.perform(post("/api/template-days/" + dayId + "/exercises")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("exerciseId", foreignExerciseId.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EXERCISE_NOT_FOUND"));
    }

    @Test
    void attachingRoutineSnapshotsNameAndContent() throws Exception {
        String token = registerAndToken();
        UUID routineId = idOf(mockMvc.perform(createRoutine(token, uniqueName("Warmup"), "START", "Bike 5 min."))
                .andReturn());
        UUID templateId = idOf(mockMvc.perform(createTemplate(token, uniqueName("WithRoutine"))).andReturn());
        UUID dayId = idOf(mockMvc.perform(createDay(token, templateId, 1, "Day")).andReturn());

        MvcResult attached = mockMvc.perform(post("/api/template-days/" + dayId + "/routines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("routineId", routineId.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routineType").value("START"))
                .andExpect(jsonPath("$.routineContent").value("Bike 5 min."))
                .andReturn();
        UUID dayRoutineId = idOf(attached);

        MvcResult detail = mockMvc.perform(get("/api/templates/" + templateId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode routine = body(detail).get("days").get(0).get("routines").get(0);
        assertThat(routine.get("routineName").asText()).isNotBlank();

        mockMvc.perform(delete("/api/template-day-routines/" + dayRoutineId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void routineOwnedByAnotherUserCannotBeAttached() throws Exception {
        String ownerToken = registerAndToken();
        UUID foreignRoutineId = idOf(mockMvc.perform(createRoutine(ownerToken, uniqueName("Private"), "END", "Stretch."))
                .andReturn());

        String intruderToken = registerAndToken();
        UUID templateId = idOf(mockMvc.perform(createTemplate(intruderToken, uniqueName("Intruder2"))).andReturn());
        UUID dayId = idOf(mockMvc.perform(createDay(intruderToken, templateId, 1, "Day")).andReturn());

        mockMvc.perform(post("/api/template-days/" + dayId + "/routines")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("routineId", foreignRoutineId.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ROUTINE_NOT_FOUND"));
    }

    @Test
    void dayIdorReturns404() throws Exception {
        String ownerToken = registerAndToken();
        UUID templateId = idOf(mockMvc.perform(createTemplate(ownerToken, uniqueName("Owner Prog"))).andReturn());
        UUID dayId = idOf(mockMvc.perform(createDay(ownerToken, templateId, 1, "Day")).andReturn());

        String intruderToken = registerAndToken();
        mockMvc.perform(put("/api/template-days/" + dayId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("dayNumber", 1, "name", "Hijack"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TEMPLATE_DAY_NOT_FOUND"));
        mockMvc.perform(post("/api/template-days/" + dayId + "/exercises")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("exerciseId", OFFICIAL_SQUAT_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TEMPLATE_DAY_NOT_FOUND"));
    }

    @Test
    void invalidPayloadsFailValidation() throws Exception {
        String token = registerAndToken();

        mockMvc.perform(createTemplate(token, "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/templates")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Too many", "daysPerWeek", 8))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        UUID templateId = idOf(mockMvc.perform(createTemplate(token, uniqueName("Valid"))).andReturn());
        mockMvc.perform(post("/api/templates/" + templateId + "/days")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("dayNumber", 1, "name", "  "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void templateEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/templates"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder createTemplate(
            String token, String name) throws Exception {
        return post("/api/templates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", name)));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder createDay(
            String token, UUID templateId, int dayNumber, String name) throws Exception {
        return post("/api/templates/" + templateId + "/days")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("dayNumber", dayNumber, "name", name)));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder createCustomExercise(
            String token, String name) throws Exception {
        return post("/api/exercises/custom")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", name,
                        "exerciseType", "STRENGTH",
                        "muscleGroups", List.of(Map.of("code", "CHEST", "role", "PRIMARY")))));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder createRoutine(
            String token, String name, String type, String content) throws Exception {
        return post("/api/routines")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", name, "routineType", type, "content", content)));
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

    private List<String> stringArray(UUID templateId, String column) {
        return jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM workout_templates WHERE id = ?",
                (rs, rowNum) -> {
                    Array array = rs.getArray(1);
                    if (array == null) {
                        return List.of();
                    }
                    Object[] elements = (Object[]) array.getArray();
                    return Arrays.stream(elements).map(Object::toString).toList();
                },
                templateId);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UUID idOf(MvcResult result) throws Exception {
        return UUID.fromString(body(result).get("id").asText());
    }

    private static String uniqueName(String base) {
        return base + " " + UUID.randomUUID();
    }
}
