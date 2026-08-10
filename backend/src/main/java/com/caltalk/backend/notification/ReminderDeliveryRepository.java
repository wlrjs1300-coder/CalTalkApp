package com.caltalk.backend.notification;

import org.springframework.data.jpa.repository.JpaRepository;

interface ReminderDeliveryRepository extends JpaRepository<ReminderDelivery, Long> {
    boolean existsByScheduleIdAndReminderMinutes(Long scheduleId, int reminderMinutes);
}
