package com.caltalk.backend.schedule;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.caltalk.backend.user.User;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("""
            select schedule from Schedule schedule
            where schedule.owner = :owner
              and schedule.startAt < :endAt
              and schedule.endAt > :startAt
            order by schedule.startAt asc, schedule.id asc
            """)
    List<Schedule> findConflicts(
            @Param("owner") User owner,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );

    @Query("""
            select schedule from Schedule schedule
            where schedule.owner = :owner
              and schedule.id <> :excludedId
              and schedule.startAt < :endAt
              and schedule.endAt > :startAt
            order by schedule.startAt asc, schedule.id asc
            """)
    List<Schedule> findConflictsExcluding(
            @Param("owner") User owner,
            @Param("excludedId") Long excludedId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );

    @Query("""
            select schedule from Schedule schedule
            where schedule.owner = :owner
              and schedule.startAt < :to
              and schedule.endAt > :from
            order by schedule.startAt asc, schedule.endAt asc, schedule.id asc
            """)
    List<Schedule> findInRange(
            @Param("owner") User owner,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    Optional<Schedule> findByIdAndOwner(Long id, User owner);
}
