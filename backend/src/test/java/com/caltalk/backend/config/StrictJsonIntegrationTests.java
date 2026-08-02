package com.caltalk.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.caltalk.backend.auth.AuthenticatedSession;
import com.caltalk.backend.auth.SignupRequest;
import com.caltalk.backend.auth.SignupService;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class StrictJsonIntegrationTests {

    private static final String EMAIL = "strict-json@example.com";
    private static final String PASSWORD = "example-password";
    private static final String UNKNOWN_VALUE = "must-not-be-reflected";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine"));

    @Autowired MockMvc mockMvc;
    @Autowired SignupService signupService;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from confirmation_requests");
        jdbcTemplate.update("delete from schedule_change_history");
        jdbcTemplate.update("delete from schedules");
        jdbcTemplate.update("delete from users");
        signupService.signup(new SignupRequest(EMAIL, PASSWORD, PASSWORD));
    }

    @Test
    void rejectsUnknownSignupAndLoginFieldsWithoutSideEffectsOrReflection() throws Exception {
        int usersBefore = count("users");
        assertUnknown(mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("""
                        "email":"new@example.com","password":"example-password",
                        "passwordConfirmation":"example-password"
                        """))));
        assertThat(count("users")).isEqualTo(usersBefore);

        MvcResult login = assertUnknown(mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("\"email\":\"%s\",\"password\":\"%s\"".formatted(EMAIL, PASSWORD)))));
        assertThat(login.getResponse().getCookie("CALTALK_SESSION")).isNull();
    }

    @Test
    void rejectsUnknownFieldsForEveryAuthenticatedRequestDtoWithoutChangingState() throws Exception {
        AuthenticatedSession session = login();
        long scheduleId = insertSchedule();

        assertUnknown(mockMvc.perform(patch("/api/v1/users/me").cookie(session.cookie()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("\"timezone\":\"Asia/Tokyo\""))));
        assertThat(jdbcTemplate.queryForObject(
                "select timezone from users where email=?", String.class, EMAIL)).isEqualTo("Asia/Seoul");

        assertUnknown(mockMvc.perform(post("/api/v1/schedules").cookie(session.cookie()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("""
                        "title":"new","startAt":"2026-08-03T01:00:00Z",
                        "endAt":"2026-08-03T02:00:00Z","location":"room"
                        """))));
        assertThat(count("schedules")).isOne();

        assertUnknown(mockMvc.perform(patch("/api/v1/schedules/" + scheduleId)
                .cookie(session.cookie()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json("\"title\":\"changed\",\"version\":0"))));
        assertThat(jdbcTemplate.queryForObject(
                "select title from schedules where id=?", String.class, scheduleId)).isEqualTo("original");

        assertUnknown(mockMvc.perform(post("/api/v1/confirmations/999/approve")
                .cookie(session.cookie()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json("\"conflictAcknowledged\":true"))));

        assertUnknown(mockMvc.perform(delete("/api/v1/users/me")
                .cookie(session.cookie()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json("\"currentPassword\":\"%s\"".formatted(PASSWORD)))));
        assertThat(count("users")).isOne();
    }

    private MvcResult assertUnknown(org.springframework.test.web.servlet.ResultActions action)
            throws Exception {
        MvcResult result = action
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("request"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("UNKNOWN_FIELD"))
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(UNKNOWN_VALUE, "<!DOCTYPE", "<html", PASSWORD, EMAIL);
        return result;
    }

    private String json(String fields) {
        return "{" + fields + ",\"contractEscape\":\"" + UNKNOWN_VALUE + "\"}";
    }

    private AuthenticatedSession login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk()).andReturn();
        return AuthenticatedSession.from(result);
    }

    private long insertSchedule() {
        return jdbcTemplate.queryForObject("""
                insert into schedules (owner_user_id,title,start_at,end_at)
                values ((select id from users where email=?),'original',
                        '2026-08-03T03:00:00Z','2026-08-03T04:00:00Z') returning id
                """, Long.class, EMAIL);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }
}
