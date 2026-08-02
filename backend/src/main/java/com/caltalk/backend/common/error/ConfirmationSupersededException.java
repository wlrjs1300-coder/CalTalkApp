package com.caltalk.backend.common.error;

import java.util.List;

import com.caltalk.backend.schedule.ConflictingScheduleResponse;

public class ConfirmationSupersededException extends RuntimeException {

    private final Long confirmationId;
    private final List<ConflictingScheduleResponse> conflicts;

    public ConfirmationSupersededException(
            Long confirmationId,
            List<ConflictingScheduleResponse> conflicts
    ) {
        super("정보가 변경되어 다시 확인이 필요합니다.");
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
