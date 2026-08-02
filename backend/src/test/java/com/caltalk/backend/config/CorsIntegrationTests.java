package com.caltalk.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CorsIntegrationTests {

    private static final String DEVELOPMENT_ORIGIN = "http://localhost:5173";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine")
    );

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsCredentialedPreflightForExactDevelopmentOriginAndRequiredMethods() throws Exception {
        for (String method : List.of("GET", "POST", "PATCH", "DELETE")) {
            mockMvc.perform(options("/api/v1/users/me")
                            .header(HttpHeaders.ORIGIN, DEVELOPMENT_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method)
                            .header(
                                    HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                    "Content-Type,X-XSRF-TOKEN,Accept"
                            ))
                    .andExpect(status().isOk())
                    .andExpect(header().string(
                            HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                            DEVELOPMENT_ORIGIN
                    ))
                    .andExpect(header().string(
                            HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                            "true"
                    ));
        }
    }

    @Test
    void rejectsUnlistedOriginWithoutWildcardHeaders() throws Exception {
        var result = mockMvc.perform(options("/api/v1/users/me")
                        .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andReturn();

        assertThat(result.getResponse().getHeaderNames())
                .noneMatch(name -> "*".equals(result.getResponse().getHeader(name)));
    }

    @Test
    void corsDoesNotBypassAuthenticationOnActualProtectedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.ORIGIN, DEVELOPMENT_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        DEVELOPMENT_ORIGIN
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                        "true"
                ));
    }
}
