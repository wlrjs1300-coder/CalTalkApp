package com.caltalk.backend.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
class SessionJdbcIntegrationTests {

    private static final String EMAIL = "jdbc-session@example.com";
    private static final String OTHER_EMAIL = "other-session@example.com";
    private static final String PASSWORD = "session-password";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine")
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SignupService signupService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from spring_session_attributes");
        jdbcTemplate.update("delete from spring_session");
        jdbcTemplate.update("delete from confirmation_requests");
        jdbcTemplate.update("delete from schedule_change_history");
        jdbcTemplate.update("delete from schedules");
        jdbcTemplate.update("delete from users");
        signupService.signup(new SignupRequest(EMAIL, PASSWORD, PASSWORD));
        signupService.signup(new SignupRequest(OTHER_EMAIL, PASSWORD, PASSWORD));
    }

    @Test
    void flywayCreatesOfficialSessionSchemaAndLoginPersistsSecurityContext() throws Exception {
        LoginSession session = login(EMAIL);

        assertThat(tableExists("spring_session")).isTrue();
        assertThat(tableExists("spring_session_attributes")).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '4' and success",
                Integer.class
        )).isOne();
        assertThat(sessionCount(session.sessionId())).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "select max_inactive_interval from spring_session where session_id = ?",
                Integer.class,
                session.sessionId()
        )).isEqualTo(43_200);
        assertThat(jdbcTemplate.queryForObject(
                """
                select count(*) from spring_session_attributes attributes
                join spring_session session on session.primary_id = attributes.session_primary_id
                where session.session_id = ?
                """,
                Integer.class,
                session.sessionId()
        )).isPositive();

        mockMvc.perform(get("/api/v1/users/me").cookie(session.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void logoutRemovesOnlyCurrentJdbcSession() throws Exception {
        LoginSession currentSession = login(EMAIL);
        LoginSession otherSession = login(OTHER_EMAIL);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(currentSession.cookie())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(sessionCount(currentSession.sessionId())).isZero();
        assertThat(sessionCount(otherSession.sessionId())).isOne();
        mockMvc.perform(get("/api/v1/users/me").cookie(otherSession.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(OTHER_EMAIL));
    }

    @Test
    void accountDeletionRemovesOnlyCurrentJdbcSessionAfterDatabaseCommit() throws Exception {
        LoginSession currentSession = login(EMAIL);
        LoginSession otherSession = login(OTHER_EMAIL);

        mockMvc.perform(delete("/api/v1/users/me")
                        .cookie(currentSession.cookie())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"session-password"}
                                """))
                .andExpect(status().isNoContent());

        assertThat(sessionCount(currentSession.sessionId())).isZero();
        assertThat(sessionCount(otherSession.sessionId())).isOne();
    }

    private LoginSession login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader("Set-Cookie"))
                .startsWith("CALTALK_SESSION=")
                .contains("Path=/", "HttpOnly", "SameSite=Lax");
        Cookie cookie = result.getResponse().getCookie("CALTALK_SESSION");
        assertThat(cookie).isNotNull();
        String sessionId = new String(
                Base64.getDecoder().decode(cookie.getValue()),
                StandardCharsets.UTF_8
        );
        return new LoginSession(cookie, sessionId);
    }

    private int sessionCount(String sessionId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from spring_session where session_id = ?",
                Integer.class,
                sessionId
        );
    }

    private boolean tableExists(String tableName) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "select to_regclass('public.' || ?) is not null",
                Boolean.class,
                tableName
        ));
    }

    private record LoginSession(Cookie cookie, String sessionId) {
    }
}
