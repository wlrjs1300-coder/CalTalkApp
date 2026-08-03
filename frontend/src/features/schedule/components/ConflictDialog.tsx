import type { ConflictingSchedule } from '../../../api/types';
import { ApiError } from '../../../api/errors';
import { formatInTimezone } from '../dateTime';
import { DialogShell } from './DialogShell';
import { ClockIcon, MapPinIcon, WarningIcon } from '../../../components/common/Icons';

export interface ConflictState {
  confirmationId: number;
  conflicts: ConflictingSchedule[];
  replacementCount: number;
  notice?: string;
}

interface ConflictDialogProps {
  state: ConflictState;
  timeZone: string;
  pending: boolean;
  error: unknown;
  onApprove: () => void;
  onClose: () => void;
}

export function ConflictDialog({
  state,
  timeZone,
  pending,
  error,
  onApprove,
  onClose,
}: ConflictDialogProps) {
  const message =
    error instanceof ApiError
      ? error.code === 'CONFIRMATION_NOT_FOUND'
        ? '확인 시간이 지났습니다. 일정을 다시 저장해 주세요.'
        : error.code === 'CONFIRMATION_TARGET_GONE'
          ? '수정 대상 일정이 더 이상 존재하지 않습니다.'
          : error.message
      : error
        ? '일정을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.'
        : undefined;
  return (
    <DialogShell
      title="겹치는 일정이 있습니다"
      description="저장하려는 시간과 이미 등록된 일정을 확인해 주세요."
      onClose={onClose}
      closeDisabled={pending}
    >
      <div className="conflict-summary">
        <span>
          <WarningIcon />
        </span>
        <p>
          <strong>시간을 다시 확인해 주세요</strong>아래 일정과 시간이 겹칩니다. 시간이 맞다면
          그대로 저장할 수 있어요.
        </p>
      </div>
      {state.notice ? (
        <div className="notice" role="status">
          {state.notice}
        </div>
      ) : null}
      {message ? (
        <div className="alert" role="alert">
          {message}
        </div>
      ) : null}
      <ul className="conflict-list">
        {state.conflicts.map((conflict) => (
          <li key={conflict.id}>
            <strong>{conflict.title}</strong>
            <span>
              <ClockIcon />
              {formatInTimezone(conflict.startAt, timeZone)} –{' '}
              {formatInTimezone(conflict.endAt, timeZone)}
            </span>
            <span>
              <MapPinIcon />
              {conflict.location ?? '장소 미정'}
            </span>
          </li>
        ))}
      </ul>
      <div className="dialog-actions">
        <button type="button" className="secondary-button" disabled={pending} onClick={onClose}>
          시간 다시 수정
        </button>
        <button
          type="button"
          className="primary-button dialog-primary"
          disabled={pending}
          onClick={onApprove}
        >
          {pending ? '저장 중…' : '그래도 저장'}
        </button>
      </div>
    </DialogShell>
  );
}
