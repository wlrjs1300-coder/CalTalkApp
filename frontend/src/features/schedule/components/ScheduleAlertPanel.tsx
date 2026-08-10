import type { ScheduleListItem } from '../../../api/schedule';
import { BellIcon, CalendarIcon, ClockIcon, MapPinIcon } from '../../../components/common/Icons';

interface ScheduleAlertPanelProps {
  schedules?: ScheduleListItem[];
  isLoading: boolean;
  error: unknown;
  timeZone: string;
  onRetry: () => void;
  onSelect: (id: number) => void;
}
const DAY_MS = 24 * 60 * 60 * 1000;
const WINDOW_DAYS = 7;

function labelForStart(start: Date, now: number) {
  const diffMinutes = Math.max(0, (start.getTime() - now) / 60000);
  if (diffMinutes < 1) return '곧 시작';
  const diffHours = Math.round(diffMinutes / 60);
  if (diffHours < 24) return `약 ${diffHours}시간 후`;
  const diffDays = Math.round(diffHours / 24);
  if (diffDays === 1) return '내일';
  return `약 ${diffDays}일 후`;
}

export function ScheduleAlertPanel({
  schedules,
  isLoading,
  error,
  timeZone,
  onRetry,
  onSelect,
}: ScheduleAlertPanelProps) {
  if (isLoading) {
    return (
      <div className="schedule-skeleton" role="status" aria-label="일정 알림을 불러오는 중">
        <span />
        <span />
        <span />
      </div>
    );
  }

  if (error) {
    return (
      <div className="inline-state" role="alert">
        <p>{error instanceof Error ? error.message : '일정을 불러오지 못했습니다.'}</p>
        <button type="button" className="secondary-button" onClick={onRetry}>
          다시 시도
        </button>
      </div>
    );
  }

  const now = Date.now();
  const endWindow = now + WINDOW_DAYS * DAY_MS;
  const alerts = (schedules ?? [])
    .map((item) => ({ ...item, start: new Date(item.startAt) }))
    .filter((item) => Number.isFinite(item.start.getTime()))
    .filter((item) => item.start.getTime() >= now && item.start.getTime() <= endWindow)
    .sort((a, b) => a.start.getTime() - b.start.getTime());

  const dateTimeFormatter = new Intl.DateTimeFormat('ko-KR', {
    timeZone,
    dateStyle: 'medium',
    timeStyle: 'short',
  });

  const timeFormatter = new Intl.DateTimeFormat('ko-KR', {
    timeZone,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });

  if (!alerts.length) {
    return (
      <div className="empty-state">
        <span className="empty-illustration" aria-hidden="true">
          <BellIcon />
        </span>
        <h3>예정된 알림이 없어요</h3>
        <p>앞으로 7일 내 시작 예정인 일정이 없습니다.</p>
      </div>
    );
  }

  return (
    <ul className="alert-list" role="list">
      {alerts.map((item) => (
        <li key={item.id}>
          <button className="alert-item" type="button" onClick={() => onSelect(item.id)}>
            <div className="alert-item-top">
              <span className="alert-item-label">
                <CalendarIcon />
                {dateTimeFormatter.format(item.start)}
              </span>
              <span className="alert-item-time">{labelForStart(item.start, now)}</span>
            </div>
            <strong>{item.title}</strong>
            <div className="alert-item-meta">
              <span>
                <ClockIcon />
                {timeFormatter.format(item.start)}
              </span>
              <span>
                <MapPinIcon />
                {item.location ?? '장소 미정'}
              </span>
            </div>
            <span className="alert-item-cta">
              <BellIcon />
              일정보기
            </span>
          </button>
        </li>
      ))}
    </ul>
  );
}
