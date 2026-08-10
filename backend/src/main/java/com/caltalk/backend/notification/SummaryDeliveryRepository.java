package com.caltalk.backend.notification;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

interface SummaryDeliveryRepository extends JpaRepository<SummaryDelivery, Long> {
    boolean existsByUserIdAndTypeAndPeriodStart(Long userId, String type, LocalDate periodStart);
}
