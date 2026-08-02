package com.caltalk.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.caltalk.backend.auth.SignupRequest;
import com.caltalk.backend.auth.SignupService;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AccountDeletionIntegrationTests {

    private static final String EMAIL = "delete-account@example.com";
    private static final String OTHER_EMAIL = "preserved-account@example.com";
    private static final String PASSWORD = "account-password";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine")
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SignupService signupService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from confirmation_requests");
        jdbcTemplate.update("delete from schedule_change_history");
        jdbcTemplate.update("delete from schedules");
        jdbcTemplate.update("delete from users");
        signupService.signup(new SignupRequest(EMAIL, PASSWORD, PASSWORD));
        signupService.signup(new SignupRequest(OTHER_EMAIL, PASSWORD, PASSWORD));
    }

    @Test
    void deletesOnlyCurrentUsersDataAndEndsCurrentSession() throws Exception {
        long userId = userId(EMAIL);
        long otherUserId = userId(OTHER_EMAIL);
        long scheduleId = insertSchedule(userId, "Delete schedule");
        long otherScheduleId = insertSchedule(otherUserId, "Keep schedule");
        insertHistory(scheduleId, userId);
        insertHistory(otherScheduleId, otherUserId);
        insertConfirmation(userId, scheduleId, "delete-confirmation");
        insertConfirmation(otherUserId, otherScheduleId, "keep-confirmation");
        MockHttpSession session = login(EMAIL);
        String sessionId = session.getId();

        MvcResult result = deleteAccount(session, PASSWORD)
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andReturn();

        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .startsWith("CALTALK_SESSION=;")
                .contains("Path=/", "Max-Age=0", "HttpOnly", "SameSite=Lax")
                .doesNotContain("Domain=", EMAIL, sessionId);
        assertThat(count("users", "id", userId)).isZero();
        assertThat(count("schedules", "owner_user_id", userId)).isZero();
        assertThat(count("confirmation_requests", "user_id", userId)).isZero();
        assertThat(count("schedule_change_history", "schedule_id", scheduleId)).isZero();
        assertThat(count("users", "id", otherUserId)).isOne();
        assertThat(count("schedules", "owner_user_id", otherUserId)).isOne();
        assertThat(count("confirmation_requests", "user_id", otherUserId)).isOne();
        assertThat(count("schedule_change_history", "schedule_id", otherScheduleId)).isOne();

        mockMvc.perform(get("/api/v1/users/me")
                        .cookie(new Cookie("CALTALK_SESSION", sessionId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsWrongPasswordWithoutDeletingDataOrEndingSession() throws Exception {
        long userId = userId(EMAIL);
        long scheduleId = insertSchedule(userId, "Keep schedule");
        insertConfirmation(userId, scheduleId, "keep-on-failure");
        MockHttpSession session = login(EMAIL);

        MvcResult result = deleteAccount(session, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andReturn();

        assertThat(session.isInvalid()).isFalse();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(count("users", "id", userId)).isOne();
        assertThat(count("schedules", "owner_user_id", userId)).isOne();
        assertThat(count("confirmation_requests", "user_id", userId)).isOne();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("wrong-password", EMAIL, "password_hash");
    }

    @Test
    void rejectsMissingBlankOversizedAndUnknownFields() throws Exception {
        MockHttpSession session = login(EMAIL);
        String[] bodies = {
                "{}",
                "{\"currentPassword\":null}",
                "{\"currentPassword\":\"\"}",
                "{\"currentPassword\":\"   \"}",
                "{\"currentPassword\":\"" + "x".repeat(65) + "\"}",
                "{\"currentPassword\":\"value\",\"email\":\"hidden@example.com\"}"
        };

        for (String body : bodies) {
            mockMvc.perform(delete("/api/v1/users/me")
                            .session(session)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        assertThat(count("users", "id", userId(EMAIL))).isOne();
        assertThat(session.isInvalid()).isFalse();
    }

    @Test
    void requiresAuthenticationAndCsrfWithoutRedirects() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody(PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(redirectedUrl(null));

        MockHttpSession session = login(EMAIL);
        mockMvc.perform(delete("/api/v1/users/me")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody(PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(delete("/api/v1/users/me")
                        .session(session)
                        .with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody(PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void invalidatesAuthenticatedSessionWhenDatabaseUserIsGone() throws Exception {
        MockHttpSession session = login(EMAIL);
        jdbcTemplate.update("delete from users where email = ?", EMAIL);

        MvcResult result = deleteAccount(session, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andReturn();

        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .startsWith("CALTALK_SESSION=;")
                .contains("Max-Age=0");
    }

    @Test
    void rollsBackAllDeletionAndKeepsSessionWhenDatabaseDeletionFails() throws Exception {
        long userId = userId(EMAIL);
        long otherUserId = userId(OTHER_EMAIL);
        long scheduleId = insertSchedule(userId, "Referenced schedule");
        insertConfirmation(userId, scheduleId, "owned-confirmation");
        insertConfirmation(otherUserId, scheduleId, "blocking-confirmation");
        MockHttpSession session = login(EMAIL);

        MvcResult result = deleteAccount(session, PASSWORD)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andReturn();

        assertThat(session.isInvalid()).isFalse();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(count("users", "id", userId)).isOne();
        assertThat(count("schedules", "owner_user_id", userId)).isOne();
        assertThat(count("confirmation_requests", "user_id", userId)).isOne();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("confirmation_requests", "schedules", "constraint", EMAIL);
    }

    private org.springframework.test.web.servlet.ResultActions deleteAccount(
            MockHttpSession session,
            String password
    ) throws Exception {
        return mockMvc.perform(delete("/api/v1/users/me")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordBody(password)));
    }

    private MockHttpSession login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private long userId(String email) {
        return jdbcTemplate.queryForObject(
                "select id from users where email = ?",
                Long.class,
                email
        );
    }

    private long insertSchedule(long userId, String title) {
        return jdbcTemplate.queryForObject("""
                insert into schedules (owner_user_id, title, start_at, end_at)
                values (?, ?, ?, ?)
                returning id
                """, Long.class, userId, title,
                OffsetDateTime.ofInstant(Instant.parse("2026-08-01T01:00:00Z"), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(Instant.parse("2026-08-01T02:00:00Z"), ZoneOffset.UTC));
    }

    private void insertHistory(long scheduleId, long userId) {
        jdbcTemplate.update("""
                insert into schedule_change_history
                    (schedule_id, changed_by_user_id, source_channel, change_type)
                values (?, ?, 'PWA', 'CREATE')
                """, scheduleId, userId);
    }

    private void insertConfirmation(long userId, long scheduleId, String fingerprintSeed) {
        String fingerprint = String.format("%-64s", fingerprintSeed).replace(' ', '0');
        jdbcTemplate.update("""
                insert into confirmation_requests
                    (user_id, origin_channel, command_type, target_schedule_id,
                     target_schedule_version, location_action, candidate_fingerprint,
                     conflict_snapshot_hash, status, expires_at)
                values (?, 'PWA', 'UPDATE_EVENT', ?, 0, 'KEEP', ?, ?, 'PENDING', ?)
                """, userId, scheduleId, fingerprint, "0".repeat(64),
                OffsetDateTime.ofInstant(Instant.now().plusSeconds(300), ZoneOffset.UTC));
    }

    private int count(String table, String column, long value) {
        return jdbcTemplate.queryForObject(
                "select count(*) from " + table + " where " + column + " = ?",
                Integer.class,
                value
        );
    }

    private String passwordBody(String password) {
        return "{\"currentPassword\":\"" + password + "\"}";
    }
}
