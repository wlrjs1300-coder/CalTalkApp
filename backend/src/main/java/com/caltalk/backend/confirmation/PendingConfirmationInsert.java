package com.caltalk.backend.confirmation;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class PendingConfirmationInsert {

    private final JdbcTemplate jdbcTemplate;

    PendingConfirmationInsert(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Long createEvent(
            String userEmail,
            String title,
            Instant startAt,
            Instant endAt,
            String location,
            String fingerprint,
            String conflictHash,
            Instant now
    ) {
        return insert("""
                insert into confirmation_requests
                    (user_id, origin_channel, command_type, title, start_at, end_at,
                     location_action, location_value, candidate_fingerprint,
                     conflict_snapshot_hash, status, created_at, expires_at)
                values (?, 'PWA', 'CREATE_EVENT', ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)
                on conflict (user_id, candidate_fingerprint) where status = 'PENDING'
                do nothing returning id
                """, userId(userEmail), title, timestamp(startAt), timestamp(endAt),
                location == null ? "REMOVE" : "SET", location, fingerprint, conflictHash,
                timestamp(now), timestamp(now.plusSeconds(300)));
    }

    Long updateEvent(
            String userEmail,
            Long targetId,
            Long targetVersion,
            String title,
            Instant startAt,
            Instant endAt,
            String locationAction,
            String locationValue,
            String fingerprint,
            String conflictHash,
            Instant now
    ) {
        return insert("""
                insert into confirmation_requests
                    (user_id, origin_channel, command_type, target_schedule_id,
                     target_schedule_version, title, start_at, end_at, location_action,
                     location_value, candidate_fingerprint, conflict_snapshot_hash,
                     status, created_at, expires_at)
                values (?, 'PWA', 'UPDATE_EVENT', ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)
                on conflict (user_id, candidate_fingerprint) where status = 'PENDING'
                do nothing returning id
                """, userId(userEmail), targetId, targetVersion, title, timestamp(startAt),
                timestamp(endAt), locationAction, locationValue, fingerprint, conflictHash,
                timestamp(now), timestamp(now.plusSeconds(300)));
    }

    private Long insert(String sql, Object... arguments) {
        List<Long> ids = jdbcTemplate.query(sql, (resultSet, row) -> resultSet.getLong(1), arguments);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private Long userId(String email) {
        return jdbcTemplate.queryForObject(
                "select id from users where email = ?", Long.class, email);
    }

    private OffsetDateTime timestamp(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
