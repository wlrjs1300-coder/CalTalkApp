package com.caltalk.backend.user;

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

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UpdateTimezoneIntegrationTests {

    private static final String EMAIL = "timezone-user@example.com";
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

    @BeforeEach
    void createUser() {
        userRepository.deleteAll();
        signupService.signup(new SignupRequest(EMAIL, PASSWORD, PASSWORD));
    }

    @Test
    void updatesOnlyCurrentUsersTimezoneAndReturnsCurrentUserResponse() throws Exception {
        User before = findUser();
        String passwordHash = before.getPasswordHash();
        Instant createdAt = before.getCreatedAt();
        MockHttpSession session = login();

        performPatch(session, "  Asia/Tokyo  ")
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.timezone").value("Asia/Tokyo"))
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.sessionId").doesNotExist())
                .andExpect(jsonPath("$.secret").doesNotExist())
                .andExpect(jsonPath("$.roles").doesNotExist())
                .andExpect(jsonPath("$.authorities").doesNotExist());

        User after = findUser();
        assertThat(after.getEmail()).isEqualTo(EMAIL);
        assertThat(after.getPasswordHash()).isEqualTo(passwordHash);
        assertThat(after.getCreatedAt()).isEqualTo(createdAt);
        assertThat(after.getTimezone()).isEqualTo("Asia/Tokyo");

        mockMvc.perform(get("/api/v1/users/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Asia/Tokyo"));
    }

    @Test
    void acceptsCurrentTimezoneIdempotently() throws Exception {
        User before = findUser();

        performPatch(login(), "Asia/Seoul")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.timezone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.createdAt").value(before.getCreatedAt().toString()));

        User after = findUser();
        assertThat(after.getEmail()).isEqualTo(before.getEmail());
        assertThat(after.getCreatedAt()).isEqualTo(before.getCreatedAt());
    }

    @ParameterizedTest
    @MethodSource("invalidTimezones")
    void rejectsInvalidTimezoneWithoutExposingInputOrInternalErrors(
            String timezone,
            String sensitiveFragment
    ) throws Exception {
        MvcResult result = performPatch(login(), timezone)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("입력한 내용을 다시 확인해주세요."))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("timezone"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("INVALID_TIMEZONE"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("올바른 시간대를 선택해주세요."))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(sensitiveFragment, "ZoneRulesException", "IllegalArgumentException");
        assertThat(findUser().getTimezone()).isEqualTo("Asia/Seoul");
    }

    @Test
    void returnsJsonUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timezone\":\"Asia/Tokyo\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(redirectedUrl(null));
    }

    @Test
    void rejectsMissingAndInvalidCsrfTokens() throws Exception {
        MockHttpSession session = login();
        String body = "{\"timezone\":\"Asia/Tokyo\"}";

        mockMvc.perform(patch("/api/v1/users/me")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(redirectedUrl(null));

        mockMvc.perform(patch("/api/v1/users/me")
                        .session(session)
                        .with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(redirectedUrl(null));
    }

    @Test
    void invalidatesSessionWhenAuthenticatedUserNoLongerExists() throws Exception {
        MockHttpSession session = login();
        userRepository.deleteAll();

        MvcResult result = performPatch(session, "Asia/Tokyo")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andReturn();

        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .isNotNull()
                .startsWith("CALTALK_SESSION=;")
                .contains("Max-Age=0")
                .doesNotContain(EMAIL);
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(EMAIL, "존재", "찾을 수");
    }

    private org.springframework.test.web.servlet.ResultActions performPatch(
            MockHttpSession session,
            String timezone
    ) throws Exception {
        return mockMvc.perform(patch("/api/v1/users/me")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timezone\":\"%s\"}".formatted(timezone)));
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private User findUser() {
        return userRepository.findByEmail(EMAIL).orElseThrow();
    }

    private static Stream<Arguments> invalidTimezones() {
        return Stream.of(
                Arguments.of("", "\"timezone\":\"\""),
                Arguments.of("   ", "\"timezone\":\"   \""),
                Arguments.of("KST", "KST"),
                Arguments.of("GMT+9", "GMT+9"),
                Arguments.of("UTC+09:00", "UTC+09:00"),
                Arguments.of("Seoul", "Seoul"),
                Arguments.of("Not/A_Real_Zone", "Not/A_Real_Zone")
        );
    }
}
