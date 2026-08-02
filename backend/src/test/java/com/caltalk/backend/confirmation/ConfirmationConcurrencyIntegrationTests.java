package com.caltalk.backend.confirmation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ConfirmationConcurrencyIntegrationTests {

    private static final String EMAIL = "confirmation-race@example.com";
    private static final String PASSWORD = "example-password";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine"));

    @Autowired MockMvc mockMvc;
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
        jdbcTemplate.update("""
                insert into schedules (owner_user_id,title,start_at,end_at)
                values ((select id from users where email=?),'conflict',
                        '2026-08-04T01:00:00Z','2026-08-04T03:00:00Z')
                """, EMAIL);
    }

    @Test
    void concurrentFirstCreationConvergesOnOnePendingConfirmationWithoutPartialCommit()
            throws Exception {
        AuthenticatedSession session = login();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<MvcResult>> futures = List.of(
                    executor.submit(() -> createConflict(session, ready, start)),
                    executor.submit(() -> createConflict(session, ready, start)));
            ready.await();
            start.countDown();

            MvcResult first = futures.get(0).get();
            MvcResult second = futures.get(1).get();
            assertThat(first.getResponse().getStatus()).isEqualTo(409);
            assertThat(second.getResponse().getStatus()).isEqualTo(409);
            assertThat(confirmationId(first)).isEqualTo(confirmationId(second));
        }

        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from confirmation_requests
                where status='PENDING' and command_type='CREATE_EVENT'
                """, Integer.class)).isOne();
        assertThat(jdbcTemplate.queryForObject("select count(*) from schedules", Integer.class)).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from schedule_change_history", Integer.class)).isZero();
    }

    @Test
    void concurrentCreateSupersedesStaleSnapshotAndConvergesOnLatestPending() throws Exception {
        AuthenticatedSession session = login();
        long staleId = confirmationId(createConflict(session, new CountDownLatch(0),
                new CountDownLatch(0)));
        jdbcTemplate.update("update schedules set version=version+1 where title='conflict'");

        List<MvcResult> results = concurrent(() -> createConflict(
                session, new CountDownLatch(0), new CountDownLatch(0)));
        long latestId = confirmationId(results.getFirst());
        assertThat(results).allSatisfy(result -> assertThat(result.getResponse().getStatus()).isEqualTo(409));
        assertThat(confirmationId(results.getLast())).isEqualTo(latestId);
        assertThat(latestId).isNotEqualTo(staleId);
        assertThat(confirmationStatus(staleId)).isEqualTo("SUPERSEDED");
        assertThat(pendingCount("CREATE_EVENT")).isOne();
        assertThat(jdbcTemplate.queryForObject("select count(*) from schedules", Integer.class)).isOne();
    }

    @Test
    void concurrentUpdateSupersedesStaleTargetVersionAndConvergesOnLatestPending()
            throws Exception {
        long targetId = jdbcTemplate.queryForObject("""
                insert into schedules (owner_user_id,title,start_at,end_at)
                values ((select id from users where email=?),'target',
                        '2026-08-04T02:00:00Z','2026-08-04T04:00:00Z') returning id
                """, Long.class, EMAIL);
        AuthenticatedSession session = login();
        long staleId = confirmationId(updateConflict(session, targetId, 0));
        jdbcTemplate.update("update schedules set title='target latest',version=version+1 where id=?", targetId);

        List<MvcResult> results = concurrent(() -> updateConflict(session, targetId, 1));
        long latestId = confirmationId(results.getFirst());
        assertThat(results).allSatisfy(result -> assertThat(result.getResponse().getStatus()).isEqualTo(409));
        assertThat(confirmationId(results.getLast())).isEqualTo(latestId);
        assertThat(latestId).isNotEqualTo(staleId);
        assertThat(confirmationStatus(staleId)).isEqualTo("SUPERSEDED");
        assertThat(jdbcTemplate.queryForObject(
                "select target_schedule_version from confirmation_requests where id=?",
                Long.class, latestId)).isOne();
        assertThat(pendingCount("UPDATE_EVENT")).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from schedule_change_history", Integer.class)).isZero();
    }

    private MvcResult createConflict(
            AuthenticatedSession session,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        start.await();
        return mockMvc.perform(post("/api/v1/schedules")
                        .cookie(session.cookie()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"candidate","startAt":"2026-08-04T02:00:00Z",
                                 "endAt":"2026-08-04T04:00:00Z","location":"room"}
                                """))
                .andReturn();
    }

    private MvcResult updateConflict(AuthenticatedSession session, long targetId, long version)
            throws Exception {
        return mockMvc.perform(patch("/api/v1/schedules/{id}", targetId)
                        .cookie(session.cookie()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"endAt":"2026-08-04T05:00:00Z","version":%d}
                                """.formatted(version)))
                .andReturn();
    }

    private List<MvcResult> concurrent(java.util.concurrent.Callable<MvcResult> request)
            throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<MvcResult>> futures = List.of(
                    executor.submit(() -> runTogether(request, ready, start)),
                    executor.submit(() -> runTogether(request, ready, start)));
            ready.await();
            start.countDown();
            return List.of(futures.getFirst().get(), futures.getLast().get());
        }
    }

    private MvcResult runTogether(
            java.util.concurrent.Callable<MvcResult> request,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        start.await();
        return request.call();
    }

    private long confirmationId(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("code").stringValue()).isEqualTo("SCHEDULE_CONFLICT");
        return body.get("confirmationId").longValue();
    }

    private String confirmationStatus(long id) {
        return jdbcTemplate.queryForObject(
                "select status from confirmation_requests where id=?", String.class, id);
    }

    private int pendingCount(String commandType) {
        return jdbcTemplate.queryForObject("""
                select count(*) from confirmation_requests where status='PENDING' and command_type=?
                """, Integer.class, commandType);
    }

    private AuthenticatedSession login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk()).andReturn();
        return AuthenticatedSession.from(result);
    }
}
