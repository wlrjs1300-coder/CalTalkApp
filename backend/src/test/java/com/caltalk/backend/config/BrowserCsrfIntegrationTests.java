package com.caltalk.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.caltalk.backend.auth.SignupRequest;
import com.caltalk.backend.auth.SignupService;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class BrowserCsrfIntegrationTests {

    private static final String ORIGIN = "http://localhost:5173";
    private static final String EMAIL = "browser-csrf@example.com";
    private static final String PASSWORD = "example-password";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine"));

    @LocalServerPort int port;
    @Autowired SignupService signupService;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from confirmation_requests");
        jdbcTemplate.update("delete from schedule_change_history");
        jdbcTemplate.update("delete from schedules");
        jdbcTemplate.update("delete from users");
        signupService.signup(new SignupRequest(EMAIL, PASSWORD, PASSWORD));
    }

    @Test
    void browserCookieAndHeaderFlowCreatesUpdatesDeletesAndUpdatesTimezone() throws Exception {
        Browser browser = new Browser();
        HttpResponse<String> csrf = browser.send("GET", "/api/v1/csrf", null, null);
        assertThat(csrf.statusCode()).isEqualTo(204);
        assertThat(csrf.headers().allValues("Set-Cookie"))
                .anySatisfy(value -> assertThat(value)
                        .startsWith("XSRF-TOKEN=")
                        .contains("Path=/", "SameSite=Lax")
                        .doesNotContain("HttpOnly", "Secure", "Domain="));
        HttpCookie csrfCookie = browser.cookie("XSRF-TOKEN");
        assertThat(csrfCookie.getPath()).isEqualTo("/");
        assertThat(csrfCookie.isHttpOnly()).isFalse();
        assertThat(csrfCookie.getSecure()).isFalse();

        assertThat(browser.send("POST", "/api/v1/auth/login", """
                {"email":"browser-csrf@example.com","password":"example-password"}
                """, null).statusCode()).isEqualTo(200);
        HttpCookie sessionCookie = browser.cookie("CALTALK_SESSION");
        assertThat(sessionCookie.isHttpOnly()).isTrue();

        HttpResponse<String> created = browser.send("POST", "/api/v1/schedules", """
                {"title":"Browser flow","startAt":"2026-08-05T01:00:00Z",
                 "endAt":"2026-08-05T02:00:00Z","location":"Room"}
                """, csrfCookie.getValue());
        assertThat(created.statusCode()).isEqualTo(201);
        long scheduleId = objectMapper.readTree(created.body()).get("id").longValue();

        assertThat(browser.send("PATCH", "/api/v1/schedules/" + scheduleId,
                "{\"title\":\"Browser updated\",\"version\":0}",
                csrfCookie.getValue()).statusCode()).isEqualTo(200);
        assertThat(browser.send("PATCH", "/api/v1/users/me",
                "{\"timezone\":\"Asia/Tokyo\"}", csrfCookie.getValue()).statusCode())
                .isEqualTo(200);
        assertThat(browser.send("DELETE", "/api/v1/schedules/" + scheduleId + "?version=1",
                null, csrfCookie.getValue()).statusCode()).isEqualTo(204);
        assertThat(jdbcTemplate.queryForObject("select count(*) from schedules", Integer.class)).isZero();

        assertForbidden(browser.send("PATCH", "/api/v1/users/me",
                "{\"timezone\":\"Asia/Seoul\"}", null));
        assertForbidden(browser.send("PATCH", "/api/v1/users/me",
                "{\"timezone\":\"Asia/Seoul\"}", "invalid-token"));

        HttpResponse<String> missingCookie = raw("PATCH", "/api/v1/users/me",
                "{\"timezone\":\"Asia/Seoul\"}", csrfCookie.getValue(),
                "CALTALK_SESSION=" + sessionCookie.getValue());
        assertForbidden(missingCookie);

        assertThat(browser.send("POST", "/api/v1/auth/logout", null,
                csrfCookie.getValue()).statusCode()).isEqualTo(204);
        HttpResponse<String> reused = raw("POST", "/api/v1/schedules", """
                {"title":"Rejected","startAt":"2026-08-05T03:00:00Z",
                 "endAt":"2026-08-05T04:00:00Z"}
                """, csrfCookie.getValue(), "CALTALK_SESSION=" + sessionCookie.getValue()
                        + "; XSRF-TOKEN=" + csrfCookie.getValue());
        assertThat(reused.statusCode()).isEqualTo(401);
    }

    private void assertForbidden(HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow())
                .startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(objectMapper.readTree(response.body()).get("code").stringValue())
                .isEqualTo("FORBIDDEN");
    }

    private HttpResponse<String> raw(
            String method, String path, String body, String token, String cookie
    ) throws Exception {
        HttpRequest.Builder request = request(method, path, body, token).header("Cookie", cookie);
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String method, String path, String body, String token) {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).header("Origin", ORIGIN);
        if (token != null) {
            request.header("X-XSRF-TOKEN", token);
        }
        if (body != null) {
            request.header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        }
        return request.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private final class Browser {
        private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();

        HttpResponse<String> send(String method, String path, String body, String token)
                throws Exception {
            return client.send(request(method, path, body, token).build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        HttpCookie cookie(String name) {
            return cookies.getCookieStore().getCookies().stream()
                    .filter(cookie -> name.equals(cookie.getName()))
                    .findFirst().orElseThrow();
        }
    }
}
