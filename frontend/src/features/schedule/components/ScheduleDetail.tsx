import type { ScheduleDetail as Detail } from '../../../api/schedule';
import { ApiError } from '../../../api/errors';
import { formatInTimezone } from '../dateTime';
import { DialogShell } from './DialogShell';
import { CalendarIcon, ClockIcon, MapPinIcon } from '../../../components/common/Icons';

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
  const getDateOnly = (iso: string) => {
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return '';
    return new Intl.DateTimeFormat('ko-KR', {
      timeZone,
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      weekday: 'short',
    }).format(date);
  };

  const getTimeOnly = (iso: string) => {
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return '';
    return new Intl.DateTimeFormat('ko-KR', {
      timeZone,
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(date);
  };

  const durationText = schedule
    ? Math.max(0, Math.round((new Date(schedule.endAt).getTime() - new Date(schedule.startAt).getTime()) / 60000))
    : 0;

  const formattedDuration =
    durationText >= 60
      ? `${Math.floor(durationText / 60)}시간 ${durationText % 60}분`
      : `${durationText}분`;

  return (
    <DialogShell
      title="일정 상세"
      description="일정 정보를 한눈에 확인하고 바로 관리할 수 있어요."
      onClose={onClose}
    >
      {loading ? (
        <div className="detail-loading-state" role="status" aria-live="polite">
          <div className="detail-loading-skeleton" />
          <p>일정 정보를 불러오는 중입니다.</p>
        </div>
      ) : null}

      {error ? (
        <div className="alert" role="alert">
          {error instanceof ApiError && error.status === 404
            ? '삭제되었거나 찾을 수 없는 일정입니다.'
            : error instanceof ApiError
              ? error.message
              : '일정 정보를 불러오지 못했습니다.'}
        </div>
      ) : null}

      {schedule ? (
        <div className="detail-content">
          <div className="detail-title-row">
            <span className="detail-badge" aria-hidden="true">
              일정
            </span>
            <h3 className="detail-title">{schedule.title}</h3>
          </div>

          <dl className="detail-meta-list">
            <div className="detail-meta-item">
              <dt>
                <CalendarIcon />
                날짜
              </dt>
              <dd>{getDateOnly(schedule.startAt)}</dd>
            </div>

            <div className="detail-meta-item">
              <dt>
                <ClockIcon />
                시간
              </dt>
              <dd>
                <p>
                  {getTimeOnly(schedule.startAt)} ~ {getTimeOnly(schedule.endAt)}
                </p>
                <span>{formattedDuration}</span>
              </dd>
            </div>

            <div className="detail-meta-item">
              <dt>
                <MapPinIcon />
                장소
              </dt>
              <dd>{schedule.location ?? '등록된 장소가 없습니다.'}</dd>
            </div>

            <div className="detail-meta-item">
              <dt>
                <ClockIcon />
                최근 수정
              </dt>
              <dd>{formatInTimezone(schedule.updatedAt, timeZone)}</dd>
            </div>
          </dl>

          <div className="dialog-actions detail-actions">
            <button type="button" className="secondary-button" onClick={() => onDelete(schedule)}>
              삭제
            </button>
            <button type="button" className="primary-button dialog-primary" onClick={() => onEdit(schedule)}>
              수정
            </button>
          </div>
        </div>
      ) : null}
    </DialogShell>
  );
}
