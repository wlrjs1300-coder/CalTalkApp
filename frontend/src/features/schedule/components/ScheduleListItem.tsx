import type { ScheduleListItem as ScheduleItem } from '../../../api/schedule';


interface ScheduleListItemProps {
  schedule: ScheduleItem;
  timeZone: string;
  onSelect: (id: number) => void;
}

export function ScheduleListItem({ schedule, timeZone, onSelect }: ScheduleListItemProps) {
  const start = new Date(schedule.startAt);
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
  const content = schedule.location?.trim() || '내용 없음';
  return (
    <li className="schedule-item">
      <button type="button" className="schedule-item-button" onClick={() => onSelect(schedule.id)}>
        <span className="schedule-main">
          <strong className="schedule-item-title">{schedule.title}</strong>
          <span className="schedule-item-time">
            {time} ~ {endTime}
          </span>
          <span className="schedule-item-content">{content}</span>
        </span>
      </button>
    </li>
  );
}
