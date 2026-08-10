package com.caltalk.backend.chatbot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.confirmation.ConfirmationService;
import com.caltalk.backend.schedule.Schedule;
import com.caltalk.backend.schedule.ScheduleRepository;
import com.caltalk.backend.user.User;

@Service
public class ChatbotScheduleDeletionService {
    private final ScheduleRepository schedules;
    private final ConfirmationService confirmations;

    public ChatbotScheduleDeletionService(ScheduleRepository schedules, ConfirmationService confirmations) {
        this.schedules = schedules;
        this.confirmations = confirmations;
    }

    @Transactional
    public String delete(User user, ChatbotCommandStateStore.PendingDelete pending) {
        Schedule schedule = schedules.findByIdAndOwner(pending.scheduleId(), user).orElse(null);
        if (schedule == null) return "이미 삭제되었거나 찾을 수 없는 일정이에요.";
        if (!schedule.getVersion().equals(pending.version())) {
            return "일정이 그 사이 변경되어 삭제하지 않았어요. 다시 요청해 주세요.";
        }
        confirmations.detachPendingUpdates(schedule.getId());
        schedules.delete(schedule);
        schedules.flush();
        return "'%s' 일정을 삭제했어요.".formatted(schedule.getTitle());
    }
}
