import type { ScheduleListItem as ScheduleItem } from '../../../api/schedule';
import { ClockIcon, MapPinIcon } from '../../../components/common/Icons';

interface ScheduleListItemProps {
  schedule: ScheduleItem;
  timeZone: string;
  onSelect: (id: number) => void;
}

export function ScheduleListItem({ schedule, timeZone, onSelect }: ScheduleListItemProps) {
  const start = new Date(schedule.startAt);
  const date = new Intl.DateTimeFormat('ko-KR', {
    month: 'long',
    day: 'numeric',
    weekday: 'short',
    timeZone,
  }).format(start);
  const time = new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    timeZone,
  }).format(start);
  const endTime = new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    timeZone,
  }).format(new Date(schedule.endAt));
  return (
    <li className="schedule-item">
      <button type="button" className="schedule-item-button" onClick={() => onSelect(schedule.id)}>
        <span className="schedule-date">{date}</span>
        <span className="schedule-main">
          <strong>{schedule.title}</strong>
          <span className="schedule-meta">
            <span>
              <ClockIcon />
              {time}–{endTime}
            </span>
            <span>
              <MapPinIcon />
              {schedule.location ?? '장소 미정'}
            </span>
          </span>
        </span>
        <span className="schedule-action" aria-hidden="true">
          ›
        </span>
      </button>
    </li>
  );
}
