package com.caltalk.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.servlet.http.Cookie;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

public record AuthenticatedSession(Cookie cookie, String sessionId) {

    public static AuthenticatedSession from(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("CALTALK_SESSION");
        if (cookie == null) {
            throw new AssertionError("Login response did not contain CALTALK_SESSION");
        }

        String sessionId = new String(
                Base64.getDecoder().decode(cookie.getValue()),
                StandardCharsets.UTF_8
        );
        return new AuthenticatedSession(cookie, sessionId);
    }

    public int databaseRowCount(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from spring_session where session_id = ?",
                Integer.class,
                sessionId
        );
        return count == null ? 0 : count;
    }
}
