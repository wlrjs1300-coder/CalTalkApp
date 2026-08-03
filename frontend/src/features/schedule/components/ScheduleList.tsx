import type { ScheduleListItem } from '../../../api/schedule';
import { ApiError } from '../../../api/errors';
import { ScheduleListItem as Item } from './ScheduleListItem';

interface ScheduleListProps {
  schedules?: ScheduleListItem[];
  timeZone: string;
  isLoading: boolean;
  error: unknown;
  onRetry: () => void;
  onSelect: (id: number) => void;
  onCreate: () => void;
}

export function ScheduleList({
  schedules,
  timeZone,
  isLoading,
  error,
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
          ✓
        </span>
        <h3>아직 일정이 없습니다</h3>
        <p>첫 일정을 등록하고 여유로운 하루를 계획해 보세요.</p>
        <button type="button" className="primary-button compact-button" onClick={onCreate}>
          첫 일정 만들기
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
