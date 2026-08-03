import type { ScheduleDetail } from '../../../api/schedule';
import { ApiError } from '../../../api/errors';
import { DialogShell } from './DialogShell';

interface DeleteScheduleDialogProps {
  schedule: ScheduleDetail;
  pending: boolean;
  error: unknown;
  onConfirm: () => void;
  onClose: () => void;
}

export function DeleteScheduleDialog({
  schedule,
  pending,
  error,
  onConfirm,
  onClose,
}: DeleteScheduleDialogProps) {
  const message =
    error instanceof ApiError
      ? error.status === 404
        ? '일정이 이미 삭제되었거나 접근할 수 없습니다.'
        : error.code === 'SCHEDULE_VERSION_CONFLICT'
          ? '일정이 변경되었습니다. 목록을 새로고침한 뒤 다시 시도해 주세요.'
          : error.message
      : error
        ? '일정을 삭제하지 못했습니다.'
        : undefined;
  return (
    <DialogShell
      title="이 일정을 삭제할까요?"
      description="삭제한 일정은 되돌릴 수 없습니다."
      onClose={onClose}
      closeDisabled={pending}
    >
      <div className="delete-target">
        <span>삭제할 일정</span>
        <strong>{schedule.title}</strong>
      </div>
      {message ? (
        <div className="alert" role="alert">
          {message}
        </div>
      ) : null}
      <div className="dialog-actions">
        <button type="button" className="secondary-button" disabled={pending} onClick={onClose}>
          취소
        </button>
        <button type="button" className="danger-button" disabled={pending} onClick={onConfirm}>
          {pending ? '삭제 중…' : '일정 삭제'}
        </button>
      </div>
    </DialogShell>
  );
}
