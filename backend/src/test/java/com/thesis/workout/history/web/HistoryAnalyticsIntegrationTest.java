package com.thesis.workout.history.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.workout.AbstractPostgresIntegrationTest;
import java.math.BigDecimal;
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
class HistoryAnalyticsIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void historyListsAllSessionsNewestFirstWithSummaries() throws Exception {
        String token = registerAndToken();
        UUID dayId = createTemplateWithExercise(token);
        UUID gymId = createGym(token);

        finishedSession(token, dayId, gymId, List.of(new int[] {100, 5}, new int[] {100, 5})); // A: vol 1000
        UUID sessionB = finishedSession(token, dayId, gymId, List.of(new int[] {60, 10}));      // B: vol 600
        cancelledSession(token, dayId, gymId);                                                   // C
        UUID sessionD = startSession(token, dayId, gymId);                                       // D: in progress

        MvcResult result = mockMvc.perform(get("/api/history").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(4))
                .andExpect(jsonPath("$.items.length()").value(4))
                .andReturn();
        JsonNode body = body(result);

        // Newest first: the in-progress session D leads and has no duration yet.
        JsonNode first = body.get("items").get(0);
        assertThat(first.get("sessionId").asText()).isEqualTo(sessionD.toString());
        assertThat(first.get("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(first.get("durationSeconds").isNull()).isTrue();

        JsonNode bItem = findById(body.get("items"), sessionB);
        assertThat(bItem.get("status").asText()).isEqualTo("FINISHED");
        assertThat(bItem.get("setCount").asLong()).isEqualTo(1);
        assertThat(bItem.get("totalVolume").decimalValue()).isEqualByComparingTo(new BigDecimal("600"));
        assertThat(bItem.get("durationSeconds").isNull()).isFalse();
    }

    @Test
    void historyPaginates() throws Exception {
        String token = registerAndToken();
        UUID dayId = createTemplateWithExercise(token);
        UUID gymId = createGym(token);
        finishedSession(token, dayId, gymId, List.of(new int[] {50, 5}));
        finishedSession(token, dayId, gymId, List.of(new int[] {50, 5}));
        finishedSession(token, dayId, gymId, List.of(new int[] {50, 5}));

        MvcResult page0 = mockMvc.perform(get("/api/history?page=0&size=2").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andReturn();
        assertThat(body(page0).get("page").asInt()).isZero();

        mockMvc.perform(get("/api/history?page=1&size=2").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void analyticsAggregatesOnlyFinishedSessions() throws Exception {
        String token = registerAndToken();
        UUID dayId = createTemplateWithExercise(token);
        UUID gymId = createGym(token);

        finishedSession(token, dayId, gymId, List.of(new int[] {100, 5}, new int[] {100, 5})); // vol 1000, 2 sets
        finishedSession(token, dayId, gymId, List.of(new int[] {60, 10}));                      // vol 600, 1 set
        cancelledSession(token, dayId, gymId);                                                   // excluded
        startSession(token, dayId, gymId);                                                       // in progress, excluded

        MvcResult result = mockMvc.perform(get("/api/analytics/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWorkouts").value(2))
                .andReturn();
        JsonNode body = body(result);

        assertThat(body.get("totalVolume").decimalValue()).isEqualByComparingTo(new BigDecimal("1600"));

        // All sessions started on the same day -> one volume point summing every finished session.
        assertThat(body.get("volumeOverTime")).hasSize(1);
        assertThat(body.get("volumeOverTime").get(0).get("volume").decimalValue())
                .isEqualByComparingTo(new BigDecimal("1600"));

        assertThat(body.get("workoutsPerWeek")).hasSize(1);
        assertThat(body.get("workoutsPerWeek").get(0).get("workouts").asLong()).isEqualTo(2);

        JsonNode chest = findByField(body.get("primaryMuscleSetDistribution"), "code", "CHEST");
        assertThat(chest).isNotNull();
        assertThat(chest.get("setCount").asLong()).isEqualTo(3);

        // Best set ranked by estimated 1RM: 100x5 (≈116.7) beats 60x10 (≈80).
        JsonNode best = body.get("bestSets").get(0);
        assertThat(best.get("weight").decimalValue()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(best.get("reps").asInt()).isEqualTo(5);
        assertThat(best.get("estimatedOneRepMax").decimalValue()).isEqualByComparingTo(new BigDecimal("116.7"));

        // 1RM over time: one series (single exercise), one point (both sessions same day),
        // holding that day's best estimated 1RM.
        assertThat(body.get("oneRepMaxOverTime")).hasSize(1);
        JsonNode series = body.get("oneRepMaxOverTime").get(0);
        assertThat(series.get("points")).hasSize(1);
        assertThat(series.get("points").get(0).get("estimatedOneRepMax").decimalValue())
                .isEqualByComparingTo(new BigDecimal("116.7"));
    }

    @Test
    void analyticsAndHistoryAreEmptyForNewUser() throws Exception {
        String token = registerAndToken();

        mockMvc.perform(get("/api/analytics/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWorkouts").value(0))
                .andExpect(jsonPath("$.totalVolume").value(0))
                .andExpect(jsonPath("$.volumeOverTime.length()").value(0))
                .andExpect(jsonPath("$.bestSets.length()").value(0));

        mockMvc.perform(get("/api/history").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void historyOnlyShowsOwnSessions() throws Exception {
        String ownerToken = registerAndToken();
        UUID dayId = createTemplateWithExercise(ownerToken);
        UUID gymId = createGym(ownerToken);
        finishedSession(ownerToken, dayId, gymId, List.of(new int[] {80, 5}));

        String intruderToken = registerAndToken();
        mockMvc.perform(get("/api/history").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    // --- helpers ---

    private UUID finishedSession(String token, UUID dayId, UUID gymId, List<int[]> sets) throws Exception {
        UUID sessionId = startSession(token, dayId, gymId);
        UUID sessionExerciseId = firstSessionExerciseId(token, sessionId);
        for (int[] set : sets) {
            logSet(token, sessionExerciseId, set[0], set[1]);
        }
        mockMvc.perform(post("/api/workouts/" + sessionId + "/finish").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return sessionId;
    }

    private void cancelledSession(String token, UUID dayId, UUID gymId) throws Exception {
        UUID sessionId = startSession(token, dayId, gymId);
        mockMvc.perform(post("/api/workouts/" + sessionId + "/cancel").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private UUID startSession(String token, UUID dayId, UUID gymId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/workouts/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "templateDayId", dayId.toString(), "gymId", gymId.toString()))))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private void logSet(String token, UUID sessionExerciseId, int weight, int reps) throws Exception {
        mockMvc.perform(post("/api/session-exercises/" + sessionExerciseId + "/sets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("weight", weight, "reps", reps))))
                .andExpect(status().isCreated());
    }

    private UUID firstSessionExerciseId(String token, UUID sessionId) throws Exception {
        MvcResult detail = mockMvc.perform(get("/api/workouts/" + sessionId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(body(detail).get("exercises").get(0).get("id").asText());
    }

    private UUID createTemplateWithExercise(String token) throws Exception {
        UUID exerciseId = idOf(mockMvc.perform(post("/api/exercises/custom")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", uniqueName("Bench"), "exerciseType", "STRENGTH",
                                "muscleGroups", List.of(Map.of("code", "CHEST", "role", "PRIMARY"))))))
                .andExpect(status().isCreated())
                .andReturn());
        UUID templateId = idOf(mockMvc.perform(post("/api/templates")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", uniqueName("Program")))))
                .andExpect(status().isCreated())
                .andReturn());
        UUID dayId = idOf(mockMvc.perform(post("/api/templates/" + templateId + "/days")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("dayNumber", 1, "name", "Day"))))
                .andExpect(status().isCreated())
                .andReturn());
        mockMvc.perform(post("/api/template-days/" + dayId + "/exercises")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "exerciseId", exerciseId.toString(), "plannedSets", 3, "plannedReps", "5"))))
                .andExpect(status().isCreated());
        return dayId;
    }

    private UUID createGym(String token) throws Exception {
        return idOf(mockMvc.perform(post("/api/gyms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", uniqueName("Gym")))))
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

    private static JsonNode findById(JsonNode array, UUID id) {
        return findByField(array, "sessionId", id.toString());
    }

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
