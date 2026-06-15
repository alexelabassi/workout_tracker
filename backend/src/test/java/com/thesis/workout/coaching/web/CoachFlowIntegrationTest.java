package com.thesis.workout.coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

/**
 * Coach-mode MVP: relationship lifecycle, RBAC + ReBAC, IDOR-safe 404s, read-only access, and that a
 * coach's view matches the client's own owner-scoped view. Real PostgreSQL (Testcontainers).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CoachFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret!";
    private static final String OFFICIAL_SQUAT_ID = "00000000-0000-0000-0000-000000000101";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void coachInvitesClientAcceptsAndCoachReadsClientData() throws Exception {
        Account coach = coach();
        Account client = user();
        UUID sessionId = finishedWorkout(client.token());

        // Invite -> client sees a pending invite -> accept -> ACTIVE.
        mockMvc.perform(invite(coach.token(), client.email())).andExpect(status().isCreated());
        JsonNode invites = body(authGet("/api/coaching/invites", client.token()));
        assertThat(invites.size()).isEqualTo(1);
        UUID relationshipId = UUID.fromString(invites.get(0).get("relationshipId").asText());
        assertThat(invites.get(0).get("status").asText()).isEqualTo("PENDING");
        mockMvc.perform(post("/api/coaching/invites/" + relationshipId + "/accept")
                .header("Authorization", "Bearer " + client.token())).andExpect(status().isNoContent());

        // Coach now lists the client and can read history/analytics/session.
        JsonNode clients = body(authGet("/api/coach/clients", coach.token()));
        assertThat(clients.size()).isEqualTo(1);
        assertThat(clients.get(0).get("clientId").asText()).isEqualTo(client.id().toString());

        JsonNode coachHistory = body(authGet("/api/coach/clients/" + client.id() + "/history", coach.token()));
        JsonNode ownHistory = body(authGet("/api/history", client.token()));
        // Coach's view equals the client's own owner-scoped view.
        assertThat(coachHistory.get("totalItems").asInt()).isEqualTo(ownHistory.get("totalItems").asInt());
        assertThat(coachHistory.get("totalItems").asInt()).isGreaterThanOrEqualTo(1);

        mockMvc.perform(get("/api/coach/clients/" + client.id() + "/analytics")
                .header("Authorization", "Bearer " + coach.token())).andExpect(status().isOk());

        JsonNode coachSession = body(authGet(
                "/api/coach/clients/" + client.id() + "/sessions/" + sessionId, coach.token()));
        JsonNode ownSession = body(authGet("/api/workouts/" + sessionId, client.token()));
        assertThat(coachSession.get("id").asText()).isEqualTo(ownSession.get("id").asText());
    }

    @Test
    void clientRevokeRemovesCoachAccess() throws Exception {
        Account coach = coach();
        Account client = user();
        UUID relationshipId = activeRelationship(coach, client);

        mockMvc.perform(get("/api/coach/clients/" + client.id() + "/history")
                .header("Authorization", "Bearer " + coach.token())).andExpect(status().isOk());

        mockMvc.perform(delete("/api/coaching/coaches/" + relationshipId)
                .header("Authorization", "Bearer " + client.token())).andExpect(status().isNoContent());

        // Access disappears: the gate now finds no ACTIVE relationship -> 404.
        mockMvc.perform(get("/api/coach/clients/" + client.id() + "/history")
                .header("Authorization", "Bearer " + coach.token())).andExpect(status().isNotFound());
        assertThat(body(authGet("/api/coach/clients", coach.token())).size()).isZero();
    }

    @Test
    void clientCanRejectInvite() throws Exception {
        Account coach = coach();
        Account client = user();
        mockMvc.perform(invite(coach.token(), client.email())).andExpect(status().isCreated());
        UUID relationshipId = UUID.fromString(
                body(authGet("/api/coaching/invites", client.token())).get(0).get("relationshipId").asText());

        mockMvc.perform(post("/api/coaching/invites/" + relationshipId + "/reject")
                .header("Authorization", "Bearer " + client.token())).andExpect(status().isNoContent());

        assertThat(body(authGet("/api/coaching/invites", client.token())).size()).isZero();
        assertThat(body(authGet("/api/coach/clients", coach.token())).size()).isZero();
    }

    @Test
    void nonCoachIsForbiddenOnCoachApi() throws Exception {
        Account user = user();
        mockMvc.perform(get("/api/coach/clients").header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void coachWithoutActiveRelationshipGetsNotFound() throws Exception {
        Account coach = coach();
        Account stranger = user();
        // No relationship at all -> 404 (IDOR-safe, not 403).
        mockMvc.perform(get("/api/coach/clients/" + stranger.id() + "/history")
                .header("Authorization", "Bearer " + coach.token())).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/coach/clients/" + UUID.randomUUID() + "/analytics")
                .header("Authorization", "Bearer " + coach.token())).andExpect(status().isNotFound());
    }

    @Test
    void coachCannotMutateOrReachClientDataDirectly() throws Exception {
        Account coach = coach();
        Account client = user();
        UUID sessionId = finishedWorkout(client.token());
        activeRelationship(coach, client);

        // The coach's gated read works, but the client's OWN owner-scoped endpoint is not reachable
        // by the coach (scoped to the coach's id) -> 404. There is no coach endpoint that mutates.
        mockMvc.perform(get("/api/coach/clients/" + client.id() + "/sessions/" + sessionId)
                .header("Authorization", "Bearer " + coach.token())).andExpect(status().isOk());
        mockMvc.perform(get("/api/workouts/" + sessionId)
                .header("Authorization", "Bearer " + coach.token())).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/workouts/" + sessionId + "/cancel")
                .header("Authorization", "Bearer " + coach.token())).andExpect(status().isNotFound());
    }

    @Test
    void coachSeesOnlyActiveClients() throws Exception {
        Account coach = coach();
        Account active = user();
        Account pending = user();
        activeRelationship(coach, active);
        invite(coach.token(), pending.email()); // left PENDING

        JsonNode clients = body(authGet("/api/coach/clients", coach.token()));
        assertThat(clients.size()).isEqualTo(1);
        assertThat(clients.get(0).get("clientId").asText()).isEqualTo(active.id().toString());
    }

    @Test
    void inviteValidatesTargetUniquenessAndSelf() throws Exception {
        Account coach = coach();
        Account client = user();

        // Unknown email -> 404.
        mockMvc.perform(invite(coach.token(), "nobody-" + UUID.randomUUID() + "@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("INVITE_TARGET_NOT_FOUND"));
        // Self-invite -> 400.
        mockMvc.perform(invite(coach.token(), coach.email()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CANNOT_COACH_SELF"));
        // First invite OK, duplicate -> 409.
        mockMvc.perform(invite(coach.token(), client.email())).andExpect(status().isCreated());
        mockMvc.perform(invite(coach.token(), client.email()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("RELATIONSHIP_EXISTS"));
    }

    @Test
    void coachApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/coach/clients")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/coaching/invites")).andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private record Account(UUID id, String email, String token) {
    }

    private Account user() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        MvcResult result = register(email);
        return new Account(userId(result), email, token(result));
    }

    /** Registers a user, promotes them to COACH in the DB, and re-logs in to obtain a ROLE_COACH token. */
    private Account coach() throws Exception {
        String email = "coach-" + UUID.randomUUID() + "@example.com";
        MvcResult registered = register(email);
        UUID id = userId(registered);
        jdbcTemplate.update("UPDATE app_users SET role = 'COACH' WHERE id = ?", id);
        return new Account(id, email, login(email));
    }

    private UUID activeRelationship(Account coach, Account client) throws Exception {
        mockMvc.perform(invite(coach.token(), client.email())).andExpect(status().isCreated());
        UUID relationshipId = UUID.fromString(
                body(authGet("/api/coaching/invites", client.token())).get(0).get("relationshipId").asText());
        mockMvc.perform(post("/api/coaching/invites/" + relationshipId + "/accept")
                .header("Authorization", "Bearer " + client.token())).andExpect(status().isNoContent());
        return relationshipId;
    }

    private RequestBuilder invite(String coachToken, String clientEmail) throws Exception {
        return post("/api/coach/clients/invite")
                .header("Authorization", "Bearer " + coachToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("clientEmail", clientEmail)));
    }

    private MvcResult authGet(String path, String token) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
    }

    /** Creates a complete finished workout for the given user; returns the session id. */
    private UUID finishedWorkout(String token) throws Exception {
        UUID template = idOf(authPost("/api/templates", token, Map.of("name", "Prog " + UUID.randomUUID())));
        UUID day = idOf(authPost("/api/templates/" + template + "/days", token, Map.of("dayNumber", 1, "name", "Day A")));
        mockMvc.perform(post("/api/template-days/" + day + "/exercises")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("exerciseId", OFFICIAL_SQUAT_ID, "plannedSets", 3, "plannedReps", "5"))))
                .andExpect(status().isCreated());
        UUID gym = idOf(authPost("/api/gyms", token, Map.of("name", "Gym " + UUID.randomUUID())));

        MvcResult started = mockMvc.perform(post("/api/workouts/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("templateDayId", day.toString(), "gymId", gym.toString()))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode detail = body(started);
        UUID sessionId = UUID.fromString(detail.get("id").asText());
        UUID sessionExerciseId = UUID.fromString(detail.get("exercises").get(0).get("id").asText());
        mockMvc.perform(post("/api/session-exercises/" + sessionExerciseId + "/sets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("setType", "WORKING", "weight", 80, "reps", 5))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/workouts/" + sessionId + "/finish")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        return sessionId;
    }

    private UUID idOf(RequestBuilder request) throws Exception {
        return UUID.fromString(body(mockMvc.perform(request).andExpect(status().isCreated()).andReturn())
                .get("id").asText());
    }

    private RequestBuilder authPost(String path, String token, Map<String, Object> jsonBody) throws Exception {
        return post(path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(jsonBody));
    }

    private MvcResult register(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", PASSWORD, "displayName", "Tester"))))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String login(String email) throws Exception {
        return token(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn());
    }

    private String token(MvcResult result) throws Exception {
        return body(result).get("accessToken").asText();
    }

    private UUID userId(MvcResult result) throws Exception {
        return UUID.fromString(body(result).get("user").get("id").asText());
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
