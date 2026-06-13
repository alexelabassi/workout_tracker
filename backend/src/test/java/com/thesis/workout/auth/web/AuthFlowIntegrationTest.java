package com.thesis.workout.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.workout.AbstractPostgresIntegrationTest;
import com.thesis.workout.auth.domain.model.AppUser;
import com.thesis.workout.auth.infrastructure.repository.AppUserRepository;
import jakarta.servlet.http.Cookie;
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
class AuthFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String PASSWORD = "Sup3rSecret!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository userRepository;

    @Test
    void registerReturnsCreatedWithAccessTokenAndRefreshCookie() throws Exception {
        String email = uniqueEmail();

        MvcResult result = mockMvc.perform(register(email, PASSWORD, "Alex"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(cookie().exists(REFRESH_COOKIE))
                .andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
                .andExpect(cookie().path(REFRESH_COOKIE, "/api/auth"))
                .andReturn();

        assertThat(result.getResponse().getCookie(REFRESH_COOKIE).getValue()).isNotBlank();
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(register(email, PASSWORD, "First")).andExpect(status().isCreated());

        mockMvc.perform(register(email, PASSWORD, "Second"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_TAKEN"));
    }

    @Test
    void loginSucceedsWithCorrectPasswordAndFailsOtherwise() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(register(email, PASSWORD, "Alex")).andExpect(status().isCreated());

        mockMvc.perform(login(email, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));

        mockMvc.perform(login(email, "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void meRequiresValidBearerToken() throws Exception {
        String email = uniqueEmail();
        String accessToken = accessTokenFrom(
                mockMvc.perform(register(email, PASSWORD, "Alex")).andReturn());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void refreshRotatesTokenAndRevokesThePreviousOne() throws Exception {
        String email = uniqueEmail();
        Cookie original = refreshCookieFrom(
                mockMvc.perform(register(email, PASSWORD, "Alex")).andReturn());

        mockMvc.perform(post("/api/auth/refresh").cookie(original))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().exists(REFRESH_COOKIE));

        // Reusing the rotated-away token must fail: it was revoked in the same transaction.
        mockMvc.perform(post("/api/auth/refresh").cookie(original))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        String email = uniqueEmail();
        Cookie original = refreshCookieFrom(
                mockMvc.perform(register(email, PASSWORD, "Alex")).andReturn());

        mockMvc.perform(post("/api/auth/logout").cookie(original))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh").cookie(original))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void passwordIsStoredAsBcryptHashNotPlaintext() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(register(email, PASSWORD, "Alex")).andExpect(status().isCreated());

        AppUser stored = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(stored.getPasswordHash()).startsWith("$2");
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder register(
            String email, String password, String displayName) throws Exception {
        return post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", email, "password", password, "displayName", displayName)));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(
            String email, String password) throws Exception {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password)));
    }

    private String accessTokenFrom(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private Cookie refreshCookieFrom(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(cookie).isNotNull();
        return cookie;
    }
}
