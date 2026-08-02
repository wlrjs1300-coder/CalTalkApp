import type { ConflictingSchedule } from '../../../api/types';
import { ApiError } from '../../../api/errors';
import { formatInTimezone } from '../dateTime';
import { DialogShell } from './DialogShell';

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
        ? '확인 요청이 만료되었거나 이미 처리되었습니다.'
        : error.code === 'CONFIRMATION_TARGET_GONE'
          ? '수정 대상 일정이 더 이상 존재하지 않습니다.'
          : error.message
      : error
        ? '확인 요청을 승인하지 못했습니다.'
        : undefined;
  return (
    <DialogShell title="겹치는 일정 확인" onClose={onClose} closeDisabled={pending}>
      <p>같은 시간대에 다음 일정이 있습니다. 그래도 저장하려면 충돌을 확인하고 승인해 주세요.</p>
      <p className="muted">이 확인 요청은 발급 후 5분 동안 유효합니다.</p>
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
              {formatInTimezone(conflict.startAt, timeZone)} –{' '}
              {formatInTimezone(conflict.endAt, timeZone)}
            </span>
            {conflict.location ? <span>{conflict.location}</span> : null}
          </li>
        ))}
      </ul>
      <div className="dialog-actions">
        <button type="button" className="secondary-button" disabled={pending} onClick={onClose}>
          취소
        </button>
        <button
          type="button"
          className="primary-button dialog-primary"
          disabled={pending}
          onClick={onApprove}
        >
          {pending ? '승인 중…' : '충돌 확인 후 저장'}
        </button>
      </div>
    </DialogShell>
  );
}
