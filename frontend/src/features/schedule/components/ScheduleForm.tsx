import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import type { ScheduleDetail } from '../../../api/schedule';
import { ApiError, fieldErrorMap } from '../../../api/errors';
import { FormField } from '../../../components/common/FormField';
import { utcToDateTimeLocal } from '../dateTime';
import { scheduleFormSchema, type ScheduleFormValues } from '../schemas';
import { DialogShell } from './DialogShell';

interface ScheduleFormProps {
  mode: 'create' | 'edit';
  original?: ScheduleDetail;
  timeZone: string;
  pending: boolean;
  error: unknown;
  onSubmit: (values: ScheduleFormValues) => void;
  onClose: () => void;
}

function initialValues(original: ScheduleDetail | undefined, timeZone: string): ScheduleFormValues {
  if (!original) return { title: '', startAt: '', endAt: '', location: '' };
  return {
    title: original.title,
    startAt: utcToDateTimeLocal(original.startAt, timeZone),
    endAt: utcToDateTimeLocal(original.endAt, timeZone),
    location: original.location ?? '',
  };
}

export function ScheduleForm({
  mode,
  original,
  timeZone,
  pending,
  error,
  onSubmit,
  onClose,
}: ScheduleFormProps) {
  const form = useForm<ScheduleFormValues>({
    resolver: zodResolver(scheduleFormSchema),
    defaultValues: initialValues(original, timeZone),
  });

  useEffect(() => {
    for (const [field, message] of Object.entries(fieldErrorMap(error))) {
      if (field === 'title' || field === 'startAt' || field === 'endAt' || field === 'location') {
        form.setError(field, { type: 'server', message });
      }
    }
  }, [error, form]);

  const generalError =
    error instanceof ApiError && error.code !== 'SCHEDULE_CONFLICT' ? error.message : undefined;

  return (
    <DialogShell
      title={mode === 'create' ? '새 일정' : '일정 수정'}
      description={
        mode === 'create'
          ? '새로운 약속의 날짜와 시간을 입력하세요.'
          : '변경할 내용을 확인하고 저장하세요.'
      }
      onClose={onClose}
      closeDisabled={pending}
    >
      <div className="timezone-context">
        표시 시간대 · <strong>{timeZone}</strong>
      </div>
      {generalError ? (
        <div className="alert" role="alert">
          {generalError}
        </div>
      ) : null}
      <form onSubmit={form.handleSubmit(onSubmit)} noValidate>
        <FormField
          id="schedule-title"
          label="제목"
          placeholder="예: 프로젝트 주간 회의"
          error={form.formState.errors.title?.message}
          {...form.register('title')}
        />
        <div className="form-row">
          <FormField
            id="schedule-start"
            label="시작"
            type="datetime-local"
            error={form.formState.errors.startAt?.message}
            {...form.register('startAt')}
          />
          <FormField
            id="schedule-end"
            label="종료"
            type="datetime-local"
            error={form.formState.errors.endAt?.message}
            {...form.register('endAt')}
          />
        </div>
        <FormField
          id="schedule-location"
          label="장소 (선택)"
          placeholder="예: 3층 회의실 또는 화상 회의"
          error={form.formState.errors.location?.message}
          {...form.register('location')}
        />
        <div className="dialog-actions">
          <button type="button" className="secondary-button" disabled={pending} onClick={onClose}>
            취소
          </button>
          <button type="submit" className="primary-button dialog-primary" disabled={pending}>
            {pending ? '저장 중…' : mode === 'create' ? '일정 만들기' : '변경사항 저장'}
          </button>
        </div>
      </form>
    </DialogShell>
  );
}
