package com.caltalk.backend.ai;

import java.time.Instant;
import java.time.LocalDate;

public record ResolvedDateRange(LocalDate fromDate, LocalDate toDateExclusive, Instant from, Instant to) {
}
