package com.caltalk.backend.schedule.history;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleChangeHistoryRepository
        extends JpaRepository<ScheduleChangeHistory, Long> {
}
