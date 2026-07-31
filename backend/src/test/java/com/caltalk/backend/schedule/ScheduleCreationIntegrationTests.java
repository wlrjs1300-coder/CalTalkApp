package com.caltalk.backend.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import com.caltalk.backend.confirmation.ConfirmationRequestRepository;
import com.caltalk.backend.schedule.history.ScheduleChangeHistoryRepository;
import com.caltalk.backend.user.UserRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ScheduleCreationIntegrationTests {

    private static final String EMAIL = "schedule-user@example.com";
    private static final String OTHER_EMAIL = "other-schedule-user@example.com";
    private static final String PASSWORD = "example-password";

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
    private UserRepository userRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleChangeHistoryRepository historyRepository;

    @Autowired
    private ConfirmationRequestRepository confirmationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void createUser() {
        confirmationRepository.deleteAll();
        historyRepository.deleteAll();
        scheduleRepository.deleteAll();
        userRepository.deleteAll();
        signupService.signup(new SignupRequest(EMAIL, PASSWORD, PASSWORD));
    }

    @Test
    void createsScheduleAndHistoryWithUtcValuesAndCurrentOwner() throws Exception {
        MvcResult result = createSchedule(
                login(EMAIL),
                "  팀 회의  ",
                "2026-08-01T10:00:00+09:00",
                "2026-08-01T11:00:00+09:00",
                "  회의실 A  "
        )
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.matchesPattern(
                        "/api/v1/schedules/\\d+"
                )))
                .andExpect(jsonPath("$.title").value("팀 회의"))
                .andExpect(jsonPath("$.startAt").value("2026-08-01T01:00:00Z"))
                .andExpect(jsonPath("$.endAt").value("2026-08-01T02:00:00Z"))
                .andExpect(jsonPath("$.location").value("회의실 A"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.ownerUserId").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andReturn();

        Long scheduleId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();
        assertThat(scheduleRepository.count()).isEqualTo(1);
        assertThat(historyRepository.count()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT owner_user_id = (SELECT id FROM users WHERE email = ?) FROM schedules WHERE id = ?",
                Boolean.class,
                EMAIL,
                scheduleId
        )).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT title_before IS NULL
                    AND start_at_before IS NULL
                    AND end_at_before IS NULL
                    AND location_before IS NULL
                    AND title_after = '팀 회의'
                    AND source_channel = 'PWA'
                    AND change_type = 'CREATE'
                FROM schedule_change_history
                WHERE schedule_id = ?
                """,
                Boolean.class,
                scheduleId
        )).isTrue();
    }

    @Test
    void normalizesBlankLocationToNull() throws Exception {
        createSchedule(
                login(EMAIL),
                "일정",
                "2026-08-02T10:00:00+09:00",
                "2026-08-02T11:00:00+09:00",
                "   "
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.location").value(org.hamcrest.Matchers.nullValue()));
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidScheduleInput(String requestBody, String field, String fieldCode) throws Exception {
        mockMvc.perform(post("/api/v1/schedules")
                        .session(login(EMAIL))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == '%s')].code".formatted(field))
                        .value(org.hamcrest.Matchers.hasItem(fieldCode)));

        assertThat(scheduleRepository.count()).isZero();
        assertThat(historyRepository.count()).isZero();
    }

    @Test
    void rejectsOffsetlessTimeAndContractFields() throws Exception {
        MockHttpSession session = login(EMAIL);

        mockMvc.perform(post("/api/v1/schedules")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "일정",
                                  "startAt": "2026-08-01T10:00:00",
                                  "endAt": "2026-08-01T11:00:00"
                                }
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/schedules")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "일정",
                                  "startAt": "2026-08-01T10:00:00+09:00",
                                  "endAt": "2026-08-01T11:00:00+09:00",
                                  "userId": 999,
                                  "description": "금지 필드",
                                  "allDay": true
                                }
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("금지 필드")
                )));
    }

    @Test
    void createsPendingConfirmationAndReturnsSortedOwnConflicts() throws Exception {
        MockHttpSession session = login(EMAIL);
        createSchedule(
                session,
                "나중 일정",
                "2026-08-03T11:00:00+09:00",
                "2026-08-03T12:00:00+09:00",
                null
        ).andExpect(status().isCreated());
        createSchedule(
                session,
                "먼저 일정",
                "2026-08-03T10:00:00+09:00",
                "2026-08-03T11:00:00+09:00",
                null
        ).andExpect(status().isCreated());

        MvcResult result = createSchedule(
                session,
                "충돌 일정",
                "2026-08-03T10:30:00+09:00",
                "2026-08-03T11:30:00+09:00",
                "장소"
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"))
                .andExpect(jsonPath("$.confirmationId").isNumber())
                .andExpect(jsonPath("$.conflicts.length()").value(2))
                .andExpect(jsonPath("$.conflicts[0].title").value("먼저 일정"))
                .andExpect(jsonPath("$.conflicts[1].title").value("나중 일정"))
                .andExpect(jsonPath("$.conflicts[0].version").doesNotExist())
                .andExpect(jsonPath("$.candidateFingerprint").doesNotExist())
                .andExpect(jsonPath("$.conflictSnapshotHash").doesNotExist())
                .andReturn();

        Long confirmationId = confirmationId(result);
        assertThat(confirmationRepository.count()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT status = 'PENDING'
                    AND origin_channel = 'PWA'
                    AND command_type = 'CREATE_EVENT'
                    AND expires_at = created_at + INTERVAL '5 minutes'
                    AND length(candidate_fingerprint) = 64
                    AND length(conflict_snapshot_hash) = 64
                FROM confirmation_requests
                WHERE id = ?
                """,
                Boolean.class,
                confirmationId
        )).isTrue();
        assertThat(scheduleRepository.count()).isEqualTo(2);
        assertThat(historyRepository.count()).isEqualTo(2);
    }

    @Test
    void approvesConfirmationAndConsumesItAtomically() throws Exception {
        MockHttpSession session = login(EMAIL);
        createSchedule(
                session,
                "기존",
                "2026-08-04T10:00:00+09:00",
                "2026-08-04T11:00:00+09:00",
                null
        ).andExpect(status().isCreated());

        Long confirmationId = confirmationId(createSchedule(
                session,
                "겹침",
                "2026-08-04T10:30:00+09:00",
                "2026-08-04T11:30:00+09:00",
                null
        ).andExpect(status().isConflict()).andReturn());

        approve(session, confirmationId)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.title").value("겹침"));

        assertThat(scheduleRepository.count()).isEqualTo(2);
        assertThat(historyRepository.count()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status = 'CONSUMED' AND consumed_at IS NOT NULL FROM confirmation_requests WHERE id = ?",
                Boolean.class,
                confirmationId
        )).isTrue();

        approve(session, confirmationId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONFIRMATION_NOT_FOUND"));
    }

    @Test
    void supersedesConfirmationWhenConflictSetChanges() throws Exception {
        MockHttpSession session = login(EMAIL);
        createSchedule(
                session,
                "기존",
                "2026-08-05T10:00:00+09:00",
                "2026-08-05T11:00:00+09:00",
                null
        ).andExpect(status().isCreated());
        Long confirmationId = confirmationId(createSchedule(
                session,
                "후보",
                "2026-08-05T10:30:00+09:00",
                "2026-08-05T11:30:00+09:00",
                null
        ).andExpect(status().isConflict()).andReturn());

        createSchedule(
                session,
                "새 충돌",
                "2026-08-05T11:00:00+09:00",
                "2026-08-05T12:00:00+09:00",
                null
        ).andExpect(status().isCreated());

        approve(session, confirmationId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFIRMATION_SUPERSEDED"))
                .andExpect(jsonPath("$.confirmationId").isNumber());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status = 'SUPERSEDED' FROM confirmation_requests WHERE id = ?",
                Boolean.class,
                confirmationId
        )).isTrue();
    }

    @Test
    void protectsScheduleAndConfirmationEndpoints() throws Exception {
        String body = scheduleJson(
                "일정",
                "2026-08-06T10:00:00+09:00",
                "2026-08-06T11:00:00+09:00",
                null
        );
        mockMvc.perform(post("/api/v1/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(redirectedUrl(null));

        MockHttpSession session = login(EMAIL);
        mockMvc.perform(post("/api/v1/schedules")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/confirmations/1/approve")
                        .session(session)
                        .with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conflictAcknowledged\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void invalidatesSessionWhenAuthenticatedUserNoLongerExists() throws Exception {
        MockHttpSession session = login(EMAIL);
        userRepository.deleteAll();

        MvcResult result = createSchedule(
                session,
                "일정",
                "2026-08-07T10:00:00+09:00",
                "2026-08-07T11:00:00+09:00",
                null
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andReturn();

        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .startsWith("CALTALK_SESSION=;")
                .contains("Max-Age=0")
                .doesNotContain(EMAIL);
    }

    private org.springframework.test.web.servlet.ResultActions createSchedule(
            MockHttpSession session,
            String title,
            String startAt,
            String endAt,
            String location
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/schedules")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleJson(title, startAt, endAt, location)));
    }

    private org.springframework.test.web.servlet.ResultActions approve(
            MockHttpSession session,
            Long confirmationId
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/confirmations/{confirmationId}/approve", confirmationId)
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conflictAcknowledged\":true}"));
    }

    private MockHttpSession login(String email) throws Exception {
        if (!userRepository.existsByEmail(email)) {
            signupService.signup(new SignupRequest(email, PASSWORD, PASSWORD));
        }
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private Long confirmationId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("confirmationId")
                .asLong();
    }

    private String scheduleJson(String title, String startAt, String endAt, String location) {
        String locationJson = location == null ? "null" : "\"%s\"".formatted(location);
        return """
                {
                  "title": "%s",
                  "startAt": "%s",
                  "endAt": "%s",
                  "location": %s
                }
                """.formatted(title, startAt, endAt, locationJson);
    }

    private static Stream<Arguments> invalidRequests() {
        String validStart = "2026-08-01T10:00:00+09:00";
        String validEnd = "2026-08-01T11:00:00+09:00";
        return Stream.of(
                Arguments.of("""
                        {"startAt":"%s","endAt":"%s"}
                        """.formatted(validStart, validEnd), "title", "REQUIRED"),
                Arguments.of("""
                        {"title":"   ","startAt":"%s","endAt":"%s"}
                        """.formatted(validStart, validEnd), "title", "REQUIRED"),
                Arguments.of("""
                        {"title":"%s","startAt":"%s","endAt":"%s"}
                        """.formatted("a".repeat(201), validStart, validEnd), "title", "MAX_LENGTH"),
                Arguments.of("""
                        {"title":"일정","startAt":"%s","endAt":"%s","location":"%s"}
                        """.formatted(validStart, validEnd, "a".repeat(201)), "location", "MAX_LENGTH"),
                Arguments.of("""
                        {"title":"일정","endAt":"%s"}
                        """.formatted(validEnd), "startAt", "REQUIRED"),
                Arguments.of("""
                        {"title":"일정","startAt":"%s"}
                        """.formatted(validStart), "endAt", "REQUIRED"),
                Arguments.of("""
                        {"title":"일정","startAt":"%s","endAt":"%s"}
                        """.formatted(validStart, validStart), "endAt", "INVALID_TIME_RANGE"),
                Arguments.of("""
                        {"title":"일정","startAt":"%s","endAt":"2026-08-01T09:00:00+09:00"}
                        """.formatted(validStart), "endAt", "INVALID_TIME_RANGE")
        );
    }
}
