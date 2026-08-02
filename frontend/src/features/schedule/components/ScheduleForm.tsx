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
      onClose={onClose}
      closeDisabled={pending}
    >
      <p className="muted">입력 시간은 {timeZone} 기준이며 서버에는 UTC로 저장됩니다.</p>
      {generalError ? (
        <div className="alert" role="alert">
          {generalError}
        </div>
      ) : null}
      <form onSubmit={form.handleSubmit(onSubmit)} noValidate>
        <FormField
          id="schedule-title"
          label="제목"
          error={form.formState.errors.title?.message}
          {...form.register('title')}
        />
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
        <FormField
          id="schedule-location"
          label="장소 (선택)"
          error={form.formState.errors.location?.message}
          {...form.register('location')}
        />
        <div className="dialog-actions">
          <button type="button" className="secondary-button" disabled={pending} onClick={onClose}>
            취소
          </button>
          <button type="submit" className="primary-button dialog-primary" disabled={pending}>
            {pending ? '저장 중…' : '저장'}
          </button>
        </div>
      </form>
    </DialogShell>
  );
}
