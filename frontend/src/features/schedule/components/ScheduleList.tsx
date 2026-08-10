import type { ScheduleListItem } from '../../../api/schedule';
import { ApiError } from '../../../api/errors';
import { CalendarIcon } from '../../../components/common/Icons';
import { ScheduleListItem as Item } from './ScheduleListItem';

interface ScheduleListProps {
  schedules?: ScheduleListItem[];
  timeZone: string;
  isLoading: boolean;
  error: unknown;
  windowLabel?: string;
  onRetry: () => void;
  onSelect: (id: number) => void;
  onCreate: () => void;
}

export function ScheduleList({
  schedules,
  timeZone,
  isLoading,
  error,
  windowLabel = '일정',
  onRetry,
  onSelect,
  onCreate,
}: ScheduleListProps) {
  if (isLoading)
    return (
      <div className="schedule-skeleton" role="status" aria-label="일정을 불러오는 중">
        <span />
        <span />
        <span />
      </div>
    );
  if (error) {
    const message =
      error instanceof ApiError && error.status === 401
        ? '로그인이 만료되었습니다. 다시 로그인해 주세요.'
        : error instanceof ApiError
          ? error.message
          : '일정을 불러오지 못했습니다.';
    return (
      <div className="inline-state" role="alert">
        <p>{message}</p>
        <button type="button" className="secondary-button" onClick={onRetry}>
          다시 시도
        </button>
      </div>
    );
  }
  if (!schedules?.length) {
    return (
      <div className="empty-state">
        <span className="empty-illustration" aria-hidden="true">
          <CalendarIcon />
        </span>
        <h3>{windowLabel}이(가) 아직 없어요</h3>
        <p>중요한 약속부터 먼저 기록해서 바로 일정을 시작해 보세요.</p>
        <button type="button" className="primary-button compact-button" onClick={onCreate}>
          <span aria-hidden="true">＋</span> 첫 일정 만들기
        </button>
      </div>
    );
  }
  return (
    <ul className="schedule-list">
      {schedules.map((schedule) => (
        <Item key={schedule.id} schedule={schedule} timeZone={timeZone} onSelect={onSelect} />
      ))}
    </ul>
  );
}
