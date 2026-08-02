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
}

export function ScheduleList({
  schedules,
  timeZone,
  isLoading,
  error,
  onRetry,
  onSelect,
}: ScheduleListProps) {
  if (isLoading) return <p className="loading-line">일정을 불러오는 중…</p>;
  if (error) {
    const message =
      error instanceof ApiError && error.status === 401
        ? '로그인 세션이 만료되었습니다.'
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
    return <p className="empty-state">조회 기간에 등록된 일정이 없습니다.</p>;
  }
  return (
    <ul className="schedule-list">
      {schedules.map((schedule) => (
        <Item key={schedule.id} schedule={schedule} timeZone={timeZone} onSelect={onSelect} />
      ))}
    </ul>
  );
}
