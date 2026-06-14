package com.thesis.workout.analyzer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.workout.AbstractPostgresIntegrationTest;
import java.util.ArrayList;
import java.util.HashMap;
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

@SpringBootTest
@AutoConfigureMockMvc
class TemplateAnalyzerIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void balancedTemplateScoresWellWithNoHighWarnings() throws Exception {
        String token = registerAndToken();
        UUID templateId = createTemplate(token, uniqueName("Balanced"));
        UUID upper = createDay(token, templateId, 1, "Upper");
        addStrength(token, upper, "CHEST", 5);
        addStrength(token, upper, "BACK", 6);
        addStrength(token, upper, "SHOULDERS", 4);
        addStrength(token, upper, "BICEPS", 4);
        addStrength(token, upper, "TRICEPS", 4);
        UUID lower = createDay(token, templateId, 2, "Lower");
        addStrength(token, lower, "QUADS", 5);
        addStrength(token, lower, "HAMSTRINGS", 5);
        addStrength(token, lower, "GLUTES", 4);
        addStrength(token, lower, "CALVES", 4);

        JsonNode body = body(analyze(token, templateId).andExpect(status().isOk()).andReturn());
        assertThat(body.get("overallScore").asInt()).isGreaterThanOrEqualTo(55);
        assertThat(body.get("category").asText()).isNotEqualTo("NEEDS_REVIEW");
        assertThat(severities(body)).doesNotContain("HIGH");
        assertThat(limitationsText(body)).anyMatch(l -> l.contains("RIR"));
        assertThat(body.get("disclaimer").asText()).contains("not medical advice");
    }

    @Test
    void missingLowerBodyProducesHighWarning() throws Exception {
        String token = registerAndToken();
        UUID templateId = createTemplate(token, uniqueName("UpperOnly"));
        UUID day = createDay(token, templateId, 1, "Day");
        addStrength(token, day, "CHEST", 6);
        addStrength(token, day, "BACK", 6);

        JsonNode warning = findWarning(body(analyze(token, templateId).andReturn()), "NO_LOWER_BODY");
        assertThat(warning).isNotNull();
        assertThat(warning.get("severity").asText()).isEqualTo("HIGH");
    }

    @Test
    void missingPullProducesHighWarning() throws Exception {
        String token = registerAndToken();
        UUID templateId = createTemplate(token, uniqueName("NoPull"));
        UUID day = createDay(token, templateId, 1, "Day");
        addStrength(token, day, "CHEST", 6);
        addStrength(token, day, "QUADS", 6);

        JsonNode warning = findWarning(body(analyze(token, templateId).andReturn()), "NO_PULL");
        assertThat(warning).isNotNull();
        assertThat(warning.get("severity").asText()).isEqualTo("HIGH");
    }

    @Test
    void excessiveVolumeProducesWarning() throws Exception {
        String token = registerAndToken();
        UUID templateId = createTemplate(token, uniqueName("Excessive"));
        UUID day = createDay(token, templateId, 1, "Day");
        addStrength(token, day, "CHEST", 30);

        assertThat(findWarning(body(analyze(token, templateId).andReturn()), "MUSCLE_VOLUME_EXCESSIVE")).isNotNull();
    }

    @Test
    void veryLowVolumeProducesWarning() throws Exception {
        String token = registerAndToken();
        UUID templateId = createTemplate(token, uniqueName("LowChest"));
        UUID day = createDay(token, templateId, 1, "Day");
        addStrength(token, day, "CHEST", 2);
        addStrength(token, day, "BACK", 8);
        addStrength(token, day, "QUADS", 8);

        JsonNode warning = findWarning(body(analyze(token, templateId).andReturn()), "MUSCLE_VOLUME_VERY_LOW");
        assertThat(warning).isNotNull();
        assertThat(warning.get("affectedMuscleGroups").get(0).asText()).isEqualTo("CHEST");
    }

    @Test
    void concentratedVolumeProducesWarning() throws Exception {
        String token = registerAndToken();
        UUID templateId = createTemplate(token, uniqueName("Concentrated"));
        UUID day = createDay(token, templateId, 1, "Day");
        addStrength(token, day, "CHEST", 12);
        addStrength(token, day, "BACK", 8);
        addStrength(token, day, "QUADS", 8);

        assertThat(findWarning(body(analyze(token, templateId).andReturn()), "VOLUME_CONCENTRATED")).isNotNull();
    }

    @Test
    void pushPullImbalanceProducesWarning() throws Exception {
        String token = registerAndToken();
        UUID templateId = createTemplate(token, uniqueName("PushHeavy"));
        UUID day = createDay(token, templateId, 1, "Day");
        addStrength(token, day, "CHEST", 12);
        addStrength(token, day, "BACK", 1); // present but tiny → imbalance, not "no pull"
        addStrength(token, day, "QUADS", 6);
        addStrength(token, day, "HAMSTRINGS", 6);

        assertThat(findWarning(body(analyze(token, templateId).andReturn()), "PUSH_PULL_IMBALANCE")).isNotNull();
    }

    @Test
    void missingPlannedSetsMarksVolumeIncomplete() throws Exception {
        String token = registerAndToken();
        UUID templateId = createTemplate(token, uniqueName("NoSets"));
        UUID day = createDay(token, templateId, 1, "Day");
        addStrength(token, day, "CHEST", null); // no planned sets

        JsonNode body = body(analyze(token, templateId).andReturn());
        assertThat(findWarning(body, "VOLUME_DATA_INCOMPLETE")).isNotNull();
        assertThat(limitationsText(body)).anyMatch(l -> l.contains("incomplete"));
        JsonNode chest = findVolume(body, "CHEST");
        assertThat(chest.get("volumeDataIncomplete").asBoolean()).isTrue();
        assertThat(chest.get("weeklyWeightedSets").asDouble()).isZero();
    }

    @Test
    void ownerPublicAndAccessRules() throws Exception {
        String owner = registerAndToken();
        UUID templateId = createTemplate(owner, uniqueName("Access"));
        UUID day = createDay(owner, templateId, 1, "Day");
        addStrength(owner, day, "CHEST", 6);

        // Owner can analyze their private template.
        analyze(owner, templateId).andExpect(status().isOk());

        // Non-owner cannot analyze a private template.
        String intruder = registerAndToken();
        analyze(intruder, templateId).andExpect(status().isNotFound());

        // After publishing, any authenticated user can analyze it.
        mockMvc.perform(post("/api/templates/" + templateId + "/publish").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk());
        analyze(intruder, templateId).andExpect(status().isOk());

        // Unauthenticated → 401.
        mockMvc.perform(get("/api/templates/" + templateId + "/analysis"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void analysisIsDeterministic() throws Exception {
        String token = registerAndToken();
        UUID templateId = createTemplate(token, uniqueName("Deterministic"));
        UUID day = createDay(token, templateId, 1, "Day");
        addStrength(token, day, "CHEST", 6);
        addStrength(token, day, "BACK", 6);
        addStrength(token, day, "QUADS", 6);

        JsonNode first = body(analyze(token, templateId).andReturn());
        JsonNode second = body(analyze(token, templateId).andReturn());
        assertThat(first.get("overallScore").asInt()).isEqualTo(second.get("overallScore").asInt());
        assertThat(severities(first)).isEqualTo(severities(second));
    }

    // --- helpers ---

    private org.springframework.test.web.servlet.ResultActions analyze(String token, UUID templateId)
            throws Exception {
        return mockMvc.perform(get("/api/templates/" + templateId + "/analysis")
                .header("Authorization", "Bearer " + token));
    }

    private void addStrength(String token, UUID dayId, String muscleCode, Integer plannedSets) throws Exception {
        UUID exerciseId = createSingleMuscleExercise(token, muscleCode);
        Map<String, Object> payload = new HashMap<>();
        payload.put("exerciseId", exerciseId.toString());
        payload.put("plannedReps", "8");
        if (plannedSets != null) {
            payload.put("plannedSets", plannedSets);
        }
        mockMvc.perform(post("/api/template-days/" + dayId + "/exercises")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    private UUID createSingleMuscleExercise(String token, String muscleCode) throws Exception {
        return idOf(mockMvc.perform(post("/api/exercises/custom")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", muscleCode + " Lift " + UUID.randomUUID(),
                                "exerciseType", "STRENGTH",
                                "muscleGroups", List.of(Map.of("code", muscleCode, "role", "PRIMARY"))))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private UUID createTemplate(String token, String name) throws Exception {
        return idOf(mockMvc.perform(post("/api/templates")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private UUID createDay(String token, UUID templateId, int dayNumber, String name) throws Exception {
        return idOf(mockMvc.perform(post("/api/templates/" + templateId + "/days")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("dayNumber", dayNumber, "name", name))))
                .andExpect(status().isCreated())
                .andReturn());
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

    private static JsonNode findWarning(JsonNode body, String code) {
        for (JsonNode w : body.get("warnings")) {
            if (w.get("code").asText().equals(code)) {
                return w;
            }
        }
        return null;
    }

    private static JsonNode findVolume(JsonNode body, String muscleGroup) {
        for (JsonNode v : body.get("muscleGroupVolumes")) {
            if (v.get("muscleGroup").asText().equals(muscleGroup)) {
                return v;
            }
        }
        return null;
    }

    private static List<String> severities(JsonNode body) {
        List<String> out = new ArrayList<>();
        body.get("warnings").forEach(w -> out.add(w.get("severity").asText()));
        return out;
    }

    private static List<String> limitationsText(JsonNode body) {
        List<String> out = new ArrayList<>();
        body.get("limitations").forEach(l -> out.add(l.asText()));
        return out;
    }

    private static String uniqueName(String base) {
        return base + " " + UUID.randomUUID();
    }
}
