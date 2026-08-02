package com.caltalk.backend.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.caltalk.backend.schedule.history.ScheduleChangeHistoryRepository;
import com.caltalk.backend.user.User;
import com.caltalk.backend.user.UserRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ScheduleQueryIntegrationTests {

    private static final String EMAIL = "query-user@example.com";
    private static final String OTHER_EMAIL = "other-query-user@example.com";
    private static final String PASSWORD = "example-password";

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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleChangeHistoryRepository historyRepository;

    @Autowired
    private ConfirmationRequestRepository confirmationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void createUsers() {
        confirmationRepository.deleteAll();
        historyRepository.deleteAll();
        scheduleRepository.deleteAll();
        userRepository.deleteAll();
        signupService.signup(new SignupRequest(EMAIL, PASSWORD, PASSWORD));
        signupService.signup(new SignupRequest(OTHER_EMAIL, PASSWORD, PASSWORD));
    }

    @Test
    void returnsOnlyCurrentUserSchedulesInRequiredOrderAndShape() throws Exception {
        User owner = user(EMAIL);
        User other = user(OTHER_EMAIL);
        schedule(owner, "세 번째", "2026-08-01T02:00:00Z", "2026-08-01T03:00:00Z", null);
        schedule(owner, "두 번째", "2026-08-01T01:00:00Z", "2026-08-01T02:00:00Z", "B");
        schedule(owner, "첫 번째", "2026-08-01T01:00:00Z", "2026-08-01T01:30:00Z", "A");
        schedule(other, "다른 사용자", "2026-08-01T00:30:00Z", "2026-08-01T03:30:00Z", null);

        mockMvc.perform(get("/api/v1/schedules")
                        .cookie(login(EMAIL).cookie())
                        .param("from", "2026-08-01T09:00:00+09:00")
                        .param("to", "2026-08-01T13:00:00+09:00"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].title").value("첫 번째"))
                .andExpect(jsonPath("$.items[1].title").value("두 번째"))
                .andExpect(jsonPath("$.items[2].title").value("세 번째"))
                .andExpect(jsonPath("$.items[0].startAt").value("2026-08-01T01:00:00Z"))
                .andExpect(jsonPath("$.items[0].endAt").value("2026-08-01T01:30:00Z"))
                .andExpect(jsonPath("$.items[0].version").value(0))
                .andExpect(jsonPath("$.items[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.items[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$.items[0].ownerUserId").doesNotExist())
                .andExpect(jsonPath("$.items[0].userId").doesNotExist())
                .andExpect(jsonPath("$.items[0].email").doesNotExist())
                .andExpect(jsonPath("$.items[0].confirmationId").doesNotExist());
    }

    @Test
    void appliesOverlapBoundariesIncludingScheduleEndingAtTo() throws Exception {
        User owner = user(EMAIL);
        schedule(owner, "시작에서 끝남", "2026-08-09T23:00:00Z", "2026-08-10T00:00:00Z", null);
        schedule(owner, "시작에서 시작", "2026-08-10T00:00:00Z", "2026-08-10T01:00:00Z", null);
        schedule(owner, "종료에서 끝남", "2026-08-10T23:00:00Z", "2026-08-11T00:00:00Z", null);
        schedule(owner, "종료에서 시작", "2026-08-11T00:00:00Z", "2026-08-11T01:00:00Z", null);
        schedule(owner, "범위를 감쌈", "2026-08-09T22:00:00Z", "2026-08-11T02:00:00Z", null);
        schedule(owner, "겹치지 않음", "2026-08-12T00:00:00Z", "2026-08-12T01:00:00Z", null);

        mockMvc.perform(get("/api/v1/schedules")
                        .cookie(login(EMAIL).cookie())
                        .param("from", "2026-08-10T00:00:00Z")
                        .param("to", "2026-08-11T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[*].title").value(org.hamcrest.Matchers.contains(
                        "범위를 감쌈",
                        "시작에서 시작",
                        "종료에서 끝남"
                )));
    }

    @Test
    void returnsEmptyItemsWithOkStatus() throws Exception {
        mockMvc.perform(get("/api/v1/schedules")
                        .cookie(login(EMAIL).cookie())
                        .param("from", "2026-08-01T00:00:00Z")
                        .param("to", "2026-08-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @ParameterizedTest
    @MethodSource("invalidRangeRequests")
    void rejectsInvalidRangeParameters(String requestUri, String field, String fieldCode) throws Exception {
        mockMvc.perform(get(requestUri).cookie(login(EMAIL).cookie()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field))
                .andExpect(jsonPath("$.fieldErrors[0].code").value(fieldCode));
    }

    @Test
    void returnsOwnedScheduleDetailWithUtcValuesAndSafeFields() throws Exception {
        Schedule schedule = schedule(
                user(EMAIL),
                "상세 일정",
                "2026-08-20T01:00:00Z",
                "2026-08-20T02:00:00Z",
                "회의실"
        );

        mockMvc.perform(get("/api/v1/schedules/{scheduleId}", schedule.getId())
                        .cookie(login(EMAIL).cookie()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.id").value(schedule.getId()))
                .andExpect(jsonPath("$.title").value("상세 일정"))
                .andExpect(jsonPath("$.startAt").value("2026-08-20T01:00:00Z"))
                .andExpect(jsonPath("$.endAt").value("2026-08-20T02:00:00Z"))
                .andExpect(jsonPath("$.location").value("회의실"))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.updatedAt").isString())
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.owner").doesNotExist())
                .andExpect(jsonPath("$.ownerUserId").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.confirmationId").doesNotExist());
    }

    @Test
    void hidesWhetherMissingOrOwnedByAnotherUser() throws Exception {
        Schedule otherSchedule = schedule(
                user(OTHER_EMAIL),
                "비공개 일정",
                "2026-08-21T01:00:00Z",
                "2026-08-21T02:00:00Z",
                null
        );
        AuthenticatedSession session = login(EMAIL);

        JsonNode missing = notFoundBody(session, Long.MAX_VALUE);
        JsonNode forbidden = notFoundBody(session, otherSchedule.getId());

        assertThat(missing.get("status").asInt()).isEqualTo(404);
        assertThat(missing.get("code").stringValue()).isEqualTo("SCHEDULE_NOT_FOUND");
        assertThat(missing.get("message")).isEqualTo(forbidden.get("message"));
        assertThat(missing.get("status")).isEqualTo(forbidden.get("status"));
        assertThat(missing.get("code")).isEqualTo(forbidden.get("code"));
        assertThat(missing.get("fieldErrors")).isEqualTo(forbidden.get("fieldErrors"));
        assertThat(missing.get("fieldErrors").size()).isZero();
    }

    @Test
    void rejectsMalformedScheduleIdAsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/schedules/not-a-number")
                        .cookie(login(EMAIL).cookie()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("scheduleId"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("INVALID_FORMAT"));
    }

    @Test
    void requiresAuthenticationWithoutRedirectOrHtml() throws Exception {
        mockMvc.perform(get("/api/v1/schedules")
                        .param("from", "2026-08-01T00:00:00Z")
                        .param("to", "2026-08-02T00:00:00Z"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(redirectedUrl(null));

        mockMvc.perform(get("/api/v1/schedules/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(redirectedUrl(null));
    }

    @Test
    void invalidatesSessionWhenAuthenticatedUserNoLongerExists() throws Exception {
        AuthenticatedSession session = login(EMAIL);
        userRepository.deleteAll();

        MvcResult result = mockMvc.perform(get("/api/v1/schedules")
                        .cookie(session.cookie())
                        .param("from", "2026-08-01T00:00:00Z")
                        .param("to", "2026-08-02T00:00:00Z"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andReturn();

        assertThat(session.databaseRowCount(jdbcTemplate)).isZero();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .startsWith("CALTALK_SESSION=;")
                .contains("Max-Age=0")
                .doesNotContain(EMAIL);
    }

    private Schedule schedule(
            User owner,
            String title,
            String startAt,
            String endAt,
            String location
    ) {
        return scheduleRepository.saveAndFlush(new Schedule(
                owner,
                title,
                location,
                Instant.parse(startAt),
                Instant.parse(endAt)
        ));
    }

    private User user(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    private AuthenticatedSession login(String email) throws Exception {
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
        return AuthenticatedSession.from(result);
    }

    private JsonNode notFoundBody(AuthenticatedSession session, Long scheduleId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/schedules/{scheduleId}", scheduleId)
                        .cookie(session.cookie()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SCHEDULE_NOT_FOUND"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static Stream<Arguments> invalidRangeRequests() {
        return Stream.of(
                Arguments.of(
                        "/api/v1/schedules?to=2026-08-02T00:00:00Z",
                        "from",
                        "REQUIRED"
                ),
                Arguments.of(
                        "/api/v1/schedules?from=2026-08-01T00:00:00Z",
                        "to",
                        "REQUIRED"
                ),
                Arguments.of(
                        "/api/v1/schedules?from=invalid&to=2026-08-02T00:00:00Z",
                        "from",
                        "INVALID_FORMAT"
                ),
                Arguments.of(
                        "/api/v1/schedules?from=2026-08-01T00:00:00&to=2026-08-02T00:00:00Z",
                        "from",
                        "INVALID_FORMAT"
                ),
                Arguments.of(
                        "/api/v1/schedules?from=2026-08-01T00:00:00Z&to=2026-08-01T00:00:00Z",
                        "to",
                        "INVALID_TIME_RANGE"
                ),
                Arguments.of(
                        "/api/v1/schedules?from=2026-08-02T00:00:00Z&to=2026-08-01T00:00:00Z",
                        "to",
                        "INVALID_TIME_RANGE"
                )
        );
    }
}
