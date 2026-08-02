import type { ScheduleListItem as ScheduleItem } from '../../../api/schedule';
import { formatInTimezone } from '../dateTime';

interface ScheduleListItemProps {
  schedule: ScheduleItem;
  timeZone: string;
  onSelect: (id: number) => void;
}

export function ScheduleListItem({ schedule, timeZone, onSelect }: ScheduleListItemProps) {
  return (
    <li className="schedule-item">
      <button type="button" className="schedule-item-button" onClick={() => onSelect(schedule.id)}>
        <span className="schedule-item-time">{formatInTimezone(schedule.startAt, timeZone)}</span>
        <strong>{schedule.title}</strong>
        <span>
          {formatInTimezone(schedule.startAt, timeZone)} –{' '}
          {formatInTimezone(schedule.endAt, timeZone)}
        </span>
        {schedule.location ? <span className="schedule-location">{schedule.location}</span> : null}
        <span className="schedule-action">상세 보기</span>
      </button>
    </li>
  );
}
