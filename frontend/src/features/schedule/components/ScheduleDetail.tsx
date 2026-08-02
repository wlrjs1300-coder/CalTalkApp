import type { ScheduleDetail as Detail } from '../../../api/schedule';
import { ApiError } from '../../../api/errors';
import { formatInTimezone } from '../dateTime';
import { DialogShell } from './DialogShell';

interface ScheduleDetailProps {
  schedule?: Detail;
  timeZone: string;
  loading: boolean;
  error: unknown;
  onClose: () => void;
  onEdit: (schedule: Detail) => void;
  onDelete: (schedule: Detail) => void;
}

export function ScheduleDetail({
  schedule,
  timeZone,
  loading,
  error,
  onClose,
  onEdit,
  onDelete,
}: ScheduleDetailProps) {
  return (
    <DialogShell title="일정 상세" onClose={onClose}>
      {loading ? <p className="loading-line">상세 정보를 불러오는 중…</p> : null}
      {error ? (
        <div className="alert" role="alert">
          {error instanceof ApiError && error.status === 404
            ? '일정이 이미 삭제되었거나 접근할 수 없습니다.'
            : error instanceof ApiError
              ? error.message
              : '상세 정보를 불러오지 못했습니다.'}
        </div>
      ) : null}
      {schedule ? (
        <div className="detail-content">
          <h3>{schedule.title}</h3>
          <dl>
            <div>
              <dt>시작</dt>
              <dd>{formatInTimezone(schedule.startAt, timeZone)}</dd>
            </div>
            <div>
              <dt>종료</dt>
              <dd>{formatInTimezone(schedule.endAt, timeZone)}</dd>
            </div>
            <div>
              <dt>장소</dt>
              <dd>{schedule.location ?? '없음'}</dd>
            </div>
          </dl>
          <div className="dialog-actions">
            <button type="button" className="danger-button" onClick={() => onDelete(schedule)}>
              삭제
            </button>
            <button
              type="button"
              className="primary-button dialog-primary"
              onClick={() => onEdit(schedule)}
            >
              수정
            </button>
          </div>
        </div>
      ) : null}
    </DialogShell>
  );
}
