package com.caltalk.backend.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
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
import com.caltalk.backend.confirmation.ConfirmationRequestRepository;
import com.caltalk.backend.schedule.history.ScheduleChangeHistory;
import com.caltalk.backend.schedule.history.ScheduleChangeHistoryRepository;
import com.caltalk.backend.user.User;
import com.caltalk.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ScheduleUpdateDeleteIntegrationTests {

    private static final String EMAIL = "update-delete@example.com";
    private static final String PASSWORD = "example-password";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine")
    );

    @Autowired MockMvc mockMvc;
    @Autowired SignupService signupService;
    @Autowired UserRepository userRepository;
    @Autowired ScheduleRepository scheduleRepository;
    @Autowired ScheduleChangeHistoryRepository historyRepository;
    @Autowired ConfirmationRequestRepository confirmationRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        confirmationRepository.deleteAll();
        historyRepository.deleteAll();
        scheduleRepository.deleteAll();
        userRepository.deleteAll();
        signupService.signup(new SignupRequest(EMAIL, PASSWORD, PASSWORD));
    }

    @Test
    void partiallyUpdatesAndStoresCompleteUpdateSnapshot() throws Exception {
        Schedule schedule = schedule("Original", "Room A");
        AuthenticatedSession session = login();

        mockMvc.perform(patch("/api/v1/schedules/{id}", schedule.getId())
                        .cookie(session.cookie())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" Updated ","location":null,"version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.location").doesNotExist())
                .andExpect(jsonPath("$.version").value(1));

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from schedule_change_history where schedule_id = ? and change_type = 'UPDATE'",
                Integer.class,
                schedule.getId()
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select title_before from schedule_change_history where schedule_id = ? and change_type = 'UPDATE'",
                String.class,
                schedule.getId()
        )).isEqualTo("Original");
    }

    @Test
    void hardDeletesWithoutDeleteHistoryAndCascadesExistingHistory() throws Exception {
        Schedule schedule = schedule("Delete me", null);
        historyRepository.saveAndFlush(ScheduleChangeHistory.created(
                schedule, userRepository.findByEmail(EMAIL).orElseThrow()));

        mockMvc.perform(delete("/api/v1/schedules/{id}", schedule.getId())
                        .param("version", "0")
                        .cookie(login().cookie())
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));

        assertThat(scheduleRepository.findById(schedule.getId())).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from schedule_change_history where schedule_id = ?",
                Integer.class,
                schedule.getId()
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from schedule_change_history where change_type = 'DELETE'",
                Integer.class
        )).isZero();
    }

    private Schedule schedule(String title, String location) {
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        return scheduleRepository.saveAndFlush(new Schedule(
                user,
                title,
                location,
                Instant.parse("2026-08-01T01:00:00Z"),
                Instant.parse("2026-08-01T02:00:00Z")
        ));
    }

    private AuthenticatedSession login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return AuthenticatedSession.from(result);
    }
}
