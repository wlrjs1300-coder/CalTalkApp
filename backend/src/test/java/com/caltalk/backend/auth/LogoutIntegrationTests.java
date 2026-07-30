package com.caltalk.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import com.caltalk.backend.user.UserRepository;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LogoutIntegrationTests {

    private static final String EMAIL = "logout-user@example.com";
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
    void logsOutAuthenticatedUserAndDeletesConfiguredSessionCookie() throws Exception {
        MockHttpSession session = login();
        String sessionId = session.getId();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.nullValue()))
                .andReturn();

        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .isNotNull()
                .startsWith("CALTALK_SESSION=;")
                .contains("Path=/", "Max-Age=0", "HttpOnly", "SameSite=Lax")
                .doesNotContain("Domain=", "Secure", sessionId, "token", "secret");
    }

    @Test
    void returnsJsonUnauthorizedForProtectedApiAfterLogout() throws Exception {
        MockHttpSession session = login();
        String sessionId = session.getId();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/private")
                        .cookie(new Cookie("CALTALK_SESSION", sessionId)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(redirectedUrl(null));
    }

    @Test
    void treatsUnauthenticatedAndRepeatedLogoutAsIdempotent() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/v1/auth/logout").with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andReturn();

        mockMvc.perform(post("/api/v1/auth/logout").with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(first.getResponse().getContentAsString()).isEmpty();
    }

    @Test
    void rejectsLogoutWithoutCsrfTokenAsJsonForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("요청을 처리할 권한이 없습니다."))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(redirectedUrl(null))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("CALTALK_SESSION")
                )));
    }

    @Test
    void rejectsLogoutWithInvalidCsrfTokenWithoutExposingSensitiveValues() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout").with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("요청을 처리할 권한이 없습니다."))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(redirectedUrl(null))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("CALTALK_SESSION", "sessionId", "csrf", "token", "secret");
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
}
