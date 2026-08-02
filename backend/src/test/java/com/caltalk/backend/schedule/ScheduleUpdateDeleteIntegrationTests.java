package com.caltalk.backend.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;

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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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
    @Autowired ObjectMapper objectMapper;

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

    @Test
    void supportsIndependentFieldsLocationStatesAndCombinedUpdate() throws Exception {
        Schedule schedule = schedule("Original", "Room A");
        AuthenticatedSession session = login();

        update(session, schedule.getId(), "{\"title\":\"Title only\",\"version\":0}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Title only"))
                .andExpect(jsonPath("$.location").value("Room A"));
        update(session, schedule.getId(),
                "{\"startAt\":\"2026-08-01T00:30:00Z\",\"version\":1}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
        update(session, schedule.getId(),
                "{\"endAt\":\"2026-08-01T03:00:00Z\",\"version\":2}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(3));
        update(session, schedule.getId(), "{\"location\":null,\"version\":3}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.location").doesNotExist());
        update(session, schedule.getId(), "{\"location\":\" Room B \",\"version\":4}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.location").value("Room B"));
        update(session, schedule.getId(), "{\"location\":\"\",\"version\":5}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.location").doesNotExist());
        update(session, schedule.getId(), "{\"location\":\"Room C\",\"version\":6}")
                .andExpect(status().isOk());
        update(session, schedule.getId(), "{\"location\":\"   \",\"version\":7}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.location").doesNotExist());
        update(session, schedule.getId(), """
                {"title":"Combined","startAt":"2026-08-01T01:00:00Z",
                 "endAt":"2026-08-01T04:00:00Z","location":"Room D","version":8}
                """)
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Combined"))
                .andExpect(jsonPath("$.location").value("Room D"))
                .andExpect(jsonPath("$.version").value(9));
    }

    @Test
    void unchangedUpdatePreservesVersionTimestampAndHistory() throws Exception {
        Schedule schedule = schedule("Original", "Room A");
        Instant updatedAt = jdbcTemplate.queryForObject(
                "select updated_at from schedules where id=?", OffsetDateTime.class, schedule.getId())
                .toInstant();

        update(login(), schedule.getId(),
                "{\"title\":\" Original \",\"location\":\"Room A\",\"version\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.updatedAt").value(updatedAt.toString()));

        Schedule unchanged = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertThat(unchanged.getVersion()).isZero();
        assertThat(unchanged.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(historyRepository.findAll()).isEmpty();
    }

    @Test
    void updateEnforcesOwnershipVersionConflictsAndCsrf() throws Exception {
        Schedule own = schedule("Own", null);
        signupService.signup(new SignupRequest("other-update@example.com", PASSWORD, PASSWORD));
        User other = userRepository.findByEmail("other-update@example.com").orElseThrow();
        Schedule otherSchedule = scheduleRepository.saveAndFlush(new Schedule(
                other, "Other", null, Instant.parse("2026-08-01T04:00:00Z"),
                Instant.parse("2026-08-01T05:00:00Z")));
        AuthenticatedSession session = login();

        update(session, own.getId(), "{\"title\":\"stale\",\"version\":9}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCHEDULE_VERSION_CONFLICT"));
        update(session, otherSchedule.getId(), "{\"title\":\"hidden\",\"version\":0}")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SCHEDULE_NOT_FOUND"));
        mockMvc.perform(patch("/api/v1/schedules/{id}", own.getId()).cookie(session.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"blocked\",\"version\":0}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/schedules/{id}", own.getId()).cookie(session.cookie())
                        .with(csrf().useInvalidToken()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"blocked\",\"version\":0}"))
                .andExpect(status().isForbidden());
        assertThat(scheduleRepository.findById(own.getId()).orElseThrow().getTitle()).isEqualTo("Own");
    }

    @Test
    void deleteValidatesVersionOwnershipRepeatAuthenticationAndCsrf() throws Exception {
        Schedule own = schedule("Delete contract", null);
        signupService.signup(new SignupRequest("other-delete@example.com", PASSWORD, PASSWORD));
        User other = userRepository.findByEmail("other-delete@example.com").orElseThrow();
        Schedule otherSchedule = scheduleRepository.saveAndFlush(new Schedule(
                other, "Preserved", null, Instant.parse("2026-08-01T04:00:00Z"),
                Instant.parse("2026-08-01T05:00:00Z")));
        AuthenticatedSession session = login();

        mockMvc.perform(delete("/api/v1/schedules/{id}", own.getId())
                        .cookie(session.cookie()).with(csrf()))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(delete("/api/v1/schedules/{id}", own.getId()).param("version", "bad")
                        .cookie(session.cookie()).with(csrf()))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(delete("/api/v1/schedules/{id}", own.getId()).param("version", "-1")
                        .cookie(session.cookie()).with(csrf()))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(delete("/api/v1/schedules/{id}", own.getId()).param("version", "9")
                        .cookie(session.cookie()).with(csrf()))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/schedules/{id}", otherSchedule.getId()).param("version", "0")
                        .cookie(session.cookie()).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/schedules/999999").param("version", "0")
                        .cookie(session.cookie()).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/schedules/{id}", own.getId()).param("version", "0")
                        .cookie(session.cookie()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/schedules/{id}", own.getId()).param("version", "0")
                        .cookie(session.cookie()).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/schedules/{id}", own.getId()).param("version", "0").with(csrf()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/schedules/{id}", own.getId()).param("version", "0")
                        .cookie(session.cookie()).with(csrf()))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mockMvc.perform(delete("/api/v1/schedules/{id}", own.getId()).param("version", "0")
                        .cookie(session.cookie()).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(scheduleRepository.findById(otherSchedule.getId())).isPresent();
    }

    @Test
    void updateConflictStoresCompleteDeltaAndApprovalConsumesAtomicUpdate() throws Exception {
        Schedule target = schedule("Target", "Room A");
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        scheduleRepository.saveAndFlush(new Schedule(user, "Conflict", null,
                Instant.parse("2026-08-01T03:00:00Z"), Instant.parse("2026-08-01T05:00:00Z")));
        AuthenticatedSession session = login();

        MvcResult conflict = update(session, target.getId(), """
                {"title":"Changed","startAt":"2026-08-01T02:00:00Z",
                 "endAt":"2026-08-01T04:00:00Z","version":0}
                """).andExpect(status().isConflict()).andReturn();
        long confirmationId = confirmationId(conflict);

        assertThat(jdbcTemplate.queryForMap(
                "select * from confirmation_requests where id=?", confirmationId))
                .containsEntry("command_type", "UPDATE_EVENT")
                .containsEntry("target_schedule_id", target.getId())
                .containsEntry("target_schedule_version", 0L)
                .containsEntry("title", "Changed")
                .containsEntry("location_action", "KEEP")
                .containsEntry("status", "PENDING");
        assertThat(jdbcTemplate.queryForObject(
                "select candidate_fingerprint from confirmation_requests where id=?",
                String.class, confirmationId)).hasSize(64);
        assertThat(jdbcTemplate.queryForObject(
                "select conflict_snapshot_hash from confirmation_requests where id=?",
                String.class, confirmationId)).hasSize(64);
        assertThat(jdbcTemplate.queryForObject(
                "select extract(epoch from (expires_at-created_at)) from confirmation_requests where id=?",
                Double.class, confirmationId)).isEqualTo(300.0);

        mockMvc.perform(post("/api/v1/confirmations/{id}/approve", confirmationId)
                        .cookie(session.cookie()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conflictAcknowledged\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Changed"))
                .andExpect(jsonPath("$.version").value(1));
        assertThat(scheduleRepository.findById(target.getId()).orElseThrow().getEndAt())
                .isEqualTo(Instant.parse("2026-08-01T04:00:00Z"));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from schedule_change_history where schedule_id=? and change_type='UPDATE'",
                Integer.class, target.getId())).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "select status from confirmation_requests where id=?", String.class, confirmationId))
                .isEqualTo("CONSUMED");
        mockMvc.perform(post("/api/v1/confirmations/{id}/approve", confirmationId)
                        .cookie(session.cookie()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conflictAcknowledged\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateConfirmationsPreserveSetRemoveAndOwnershipExpiryTargetGoneContracts() throws Exception {
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        scheduleRepository.saveAndFlush(new Schedule(user, "Conflict", null,
                Instant.parse("2026-08-01T03:00:00Z"), Instant.parse("2026-08-01T05:00:00Z")));
        AuthenticatedSession session = login();

        Schedule setTarget = schedule("Set target", "Old");
        long setId = confirmationId(update(session, setTarget.getId(),
                "{\"endAt\":\"2026-08-01T04:00:00Z\",\"location\":\" New \",\"version\":0}")
                .andExpect(status().isConflict()).andReturn());
        assertThat(jdbcTemplate.queryForMap(
                "select location_action,location_value from confirmation_requests where id=?", setId))
                .containsEntry("location_action", "SET").containsEntry("location_value", "New");

        Schedule removeTarget = schedule("Remove target", "Old");
        long removeId = confirmationId(update(session, removeTarget.getId(),
                "{\"endAt\":\"2026-08-01T04:00:00Z\",\"location\":null,\"version\":0}")
                .andExpect(status().isConflict()).andReturn());
        assertThat(jdbcTemplate.queryForObject(
                "select location_action from confirmation_requests where id=?", String.class, removeId))
                .isEqualTo("REMOVE");

        signupService.signup(new SignupRequest("confirmation-other@example.com", PASSWORD, PASSWORD));
        AuthenticatedSession otherSession = login("confirmation-other@example.com");
        approve(otherSession, setId).andExpect(status().isForbidden());
        jdbcTemplate.update("update confirmation_requests set expires_at=now()-interval '1 second' where id=?", setId);
        approve(session, setId).andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/schedules/{id}", removeTarget.getId()).param("version", "0")
                        .cookie(session.cookie()).with(csrf()))
                .andExpect(status().isNoContent());
        approve(session, removeId).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONFIRMATION_TARGET_GONE"));
    }

    @Test
    void updateConfirmationSupersedesWhenTargetVersionOrConflictSnapshotChanges() throws Exception {
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        Schedule target = schedule("Target", null);
        scheduleRepository.saveAndFlush(new Schedule(user, "Conflict", null,
                Instant.parse("2026-08-01T03:00:00Z"), Instant.parse("2026-08-01T05:00:00Z")));
        AuthenticatedSession session = login();
        long originalId = confirmationId(update(session, target.getId(),
                "{\"endAt\":\"2026-08-01T04:00:00Z\",\"version\":0}")
                .andExpect(status().isConflict()).andReturn());

        target.update("Target changed elsewhere", null, target.getStartAt(), target.getEndAt());
        scheduleRepository.saveAndFlush(target);
        MvcResult versionChanged = approve(session, originalId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFIRMATION_SUPERSEDED"))
                .andReturn();
        long replacementId = confirmationId(versionChanged);
        assertThat(replacementId).isNotEqualTo(originalId);
        assertThat(jdbcTemplate.queryForObject(
                "select status from confirmation_requests where id=?", String.class, originalId))
                .isEqualTo("SUPERSEDED");

        scheduleRepository.saveAndFlush(new Schedule(user, "New conflict", null,
                Instant.parse("2026-08-01T02:30:00Z"), Instant.parse("2026-08-01T03:30:00Z")));
        MvcResult conflictChanged = approve(session, replacementId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFIRMATION_SUPERSEDED"))
                .andReturn();
        assertThat(confirmationId(conflictChanged)).isNotEqualTo(replacementId);
        assertThat(jdbcTemplate.queryForObject(
                "select status from confirmation_requests where id=?", String.class, replacementId))
                .isEqualTo("SUPERSEDED");
    }

    private org.springframework.test.web.servlet.ResultActions update(
            AuthenticatedSession session, Long id, String body) throws Exception {
        return mockMvc.perform(patch("/api/v1/schedules/{id}", id)
                .cookie(session.cookie()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private org.springframework.test.web.servlet.ResultActions approve(
            AuthenticatedSession session, long confirmationId) throws Exception {
        return mockMvc.perform(post("/api/v1/confirmations/{id}/approve", confirmationId)
                .cookie(session.cookie()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"conflictAcknowledged\":true}"));
    }

    private long confirmationId(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("confirmationId").asLong();
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
        return login(EMAIL);
    }

    private AuthenticatedSession login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return AuthenticatedSession.from(result);
    }
}
