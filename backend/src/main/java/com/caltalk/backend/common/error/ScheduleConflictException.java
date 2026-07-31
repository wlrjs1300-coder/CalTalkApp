package com.caltalk.backend.common.error;

import java.util.List;

import com.caltalk.backend.schedule.ConflictingScheduleResponse;

public class ScheduleConflictException extends RuntimeException {

    private final Long confirmationId;
    private final List<ConflictingScheduleResponse> conflicts;

    public ScheduleConflictException(
            Long confirmationId,
            List<ConflictingScheduleResponse> conflicts
    ) {
        super("같은 시간대에 다른 일정이 있습니다.");
        this.confirmationId = confirmationId;
        this.conflicts = List.copyOf(conflicts);
    }

    public Long getConfirmationId() {
        return confirmationId;
    }

    public List<ConflictingScheduleResponse> getConflicts() {
        return conflicts;
    }
}
