import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';

import { ApiError, fieldErrorMap } from '../api/errors';
import { logout } from '../api/auth';
import type { CreateScheduleRequest, ReminderMinutes, ScheduleDetail } from '../api/schedule';
import { MapPinIcon } from '../components/common/Icons';
import { FormField } from '../components/common/FormField';
import { AppLayout } from '../components/layout/AppLayout';
import { currentUserQueryKey, useCurrentUser } from '../features/auth/authQuery';
import { DialogShell } from '../features/schedule/components/DialogShell';
import { ConflictDialog, type ConflictState } from '../features/schedule/components/ConflictDialog';
import { getHolidayName } from '../features/schedule/holidays';
import { useScheduleMutations } from '../features/schedule/mutations';
import { useSchedule } from '../features/schedule/queries';
import { buildUpdateRequest, scheduleFormSchema, type ScheduleFormValues } from '../features/schedule/schemas';
import { addMinutesToDateTimeLocal, dateTimeLocalToUtc, utcToDateTimeLocal } from '../features/schedule/dateTime';
import { SettingsPanel } from '../features/user/SettingsPanel';

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

interface EventEditFormProps {
  schedule: ScheduleDetail;
  dateLabel?: string;
  timezone: string;
  onSaved: () => void;
  onDeleted: () => void;
  onCancel: () => void;
}

function localParts(value: string) {
  const [datePart = '', timePart = '09:00'] = value.split('T');
  const [year, month, day] = datePart.split('-').map(Number);
  const [hour = 9, minute = 0] = timePart.split(':').map(Number);
  const fallback = new Date();
  return {
    year: year || fallback.getFullYear(),
    month: month || fallback.getMonth() + 1,
    day: day || fallback.getDate(),
    hour,
    minute,
  };
}

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];

function WheelColumn({
  label,
  items,
  value,
  onChange,
  wide = false,
}: {
  label: string;
  items: Array<{ value: string; label: string }>;
  value: string;
  onChange: (value: string) => void;
  wide?: boolean;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const frame = useRef<number | null>(null);
  const itemHeight = 48;

  useEffect(() => {
    const index = Math.max(0, items.findIndex((item) => item.value === value));
    ref.current?.scrollTo({ top: index * itemHeight });
  }, [items, value]);

  useEffect(() => {
    const target = ref.current;
    if (!target) return;
    const reduceWheelSpeed = (event: globalThis.WheelEvent) => {
      if (!event.cancelable) return;
      event.preventDefault();
      const unit = event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? target.clientHeight : 1;
      target.scrollTop += event.deltaY * unit * 0.28;
    };
    target.addEventListener('wheel', reduceWheelSpeed, { passive: false });
    return () => target.removeEventListener('wheel', reduceWheelSpeed);
  }, []);

  const handleScroll = () => {
    if (frame.current !== null) cancelAnimationFrame(frame.current);
    frame.current = requestAnimationFrame(() => {
      if (!ref.current) return;
      const index = Math.max(0, Math.min(items.length - 1, Math.round(ref.current.scrollTop / itemHeight)));
      const next = items[index]?.value;
      if (next && next !== value) onChange(next);
    });
  };

  return (
    <div className={wide ? 'event-wheel-column is-wide' : 'event-wheel-column'}>
      <span className="event-wheel-label">{label}</span>
      <div className="event-wheel-viewport" ref={ref} onScroll={handleScroll}>
        {items.map((item) => (
          <button
            type="button"
            tabIndex={-1}
            className={item.value === value ? 'is-selected' : ''}
            key={item.value}
            onClick={() => onChange(item.value)}
          >
            {item.label}
          </button>
        ))}
      </div>
    </div>
  );
}

function DateTimePicker({
  id,
  label,
  value,
  error,
  onChange,
}: {
  id: string;
  label: string;
  value: string;
  error?: string;
  onChange: (value: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState(() => localParts(value));
  const [visibleMonth, setVisibleMonth] = useState(() => {
    const parts = localParts(value);
    return new Date(parts.year, parts.month - 1, 1);
  });

  useEffect(() => {
    if (!open) return;
    const close = (event: KeyboardEvent) => event.key === 'Escape' && setOpen(false);
    window.addEventListener('keydown', close);
    return () => window.removeEventListener('keydown', close);
  }, [open]);

  const openPicker = () => {
    const next = localParts(value);
    setDraft(next);
    setVisibleMonth(new Date(next.year, next.month - 1, 1));
    setOpen(true);
  };
  const display = value
    ? new Intl.DateTimeFormat('ko-KR', {
        year: 'numeric', month: 'long', day: 'numeric', weekday: 'short', hour: '2-digit', minute: '2-digit', hour12: false,
      }).format(new Date(`${value}:00`))
    : '날짜와 시간을 선택해 주세요';
  const pad = (number: number) => String(number).padStart(2, '0');
  const draftValue = `${draft.year}-${pad(draft.month)}-${pad(draft.day)}T${pad(draft.hour)}:${pad(draft.minute)}`;
  const hasChanged = draftValue !== value;
  const hourItems = useMemo(() => Array.from({ length: 12 }, (_, index) => ({ value: String(index + 1), label: String(index + 1) })), []);
  const minuteItems = useMemo(() => Array.from({ length: 60 }, (_, minute) => ({ value: String(minute), label: pad(minute) })), []);
  const periodItems = useMemo(() => [{ value: 'am', label: '오전' }, { value: 'pm', label: '오후' }], []);
  const period = draft.hour >= 12 ? 'pm' : 'am';
  const hour12 = draft.hour % 12 || 12;
  const firstWeekday = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth(), 1).getDay();
  const daysInMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() + 1, 0).getDate();
  const calendarCells = Array.from({ length: 42 }, (_, index) => {
    const day = index - firstWeekday + 1;
    return day >= 1 && day <= daysInMonth ? day : null;
  });

  const apply = () => {
    if (!hasChanged) return;
    onChange(draftValue);
    setOpen(false);
  };

  return (
    <div className="event-datetime-field">
      <label htmlFor={id}>{label}</label>
      <button id={id} type="button" className={error ? 'event-datetime-trigger has-error' : 'event-datetime-trigger'} onClick={openPicker}>
        <span>{display}</span><span className="event-datetime-change">변경</span>
      </button>
      {error ? <p className="field-error" role="alert">{error}</p> : null}

      {open ? (
        <div className="event-picker-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && setOpen(false)}>
          <section className="event-picker-dialog" role="dialog" aria-modal="true" aria-label={`${label} 변경`}>
            <div className="event-picker-body">
              <div className="event-picker-date-panel">
                <div className="event-picker-month-nav">
                  <button type="button" aria-label="이전 달" onClick={() => setVisibleMonth(new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() - 1, 1))}>‹</button>
                  <strong>{visibleMonth.getFullYear()}년 {visibleMonth.getMonth() + 1}월</strong>
                  <button type="button" aria-label="다음 달" onClick={() => setVisibleMonth(new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() + 1, 1))}>›</button>
                </div>
                <div className="event-picker-calendar">
                  {WEEKDAYS.map((weekday) => <span className="event-picker-weekday" key={weekday}>{weekday}</span>)}
                  {calendarCells.map((day, index) => day ? (
                    <button
                      type="button"
                      key={`${visibleMonth.getFullYear()}-${visibleMonth.getMonth()}-${day}`}
                      className={draft.year === visibleMonth.getFullYear() && draft.month === visibleMonth.getMonth() + 1 && draft.day === day ? 'is-selected' : ''}
                      onClick={() => setDraft((current) => ({ ...current, year: visibleMonth.getFullYear(), month: visibleMonth.getMonth() + 1, day }))}
                    >{day}</button>
                  ) : <span key={`blank-${index}`} />)}
                </div>
              </div>
              <div className="event-time-wheel-panel">
                <p>시간</p>
                <div className="event-wheel-selection" aria-hidden="true" />
                <WheelColumn
                  label=""
                  items={periodItems}
                  value={period}
                  onChange={(next) => setDraft((current) => ({
                    ...current,
                    hour: next === 'pm' ? (current.hour % 12) + 12 : current.hour % 12,
                  }))}
                />
                <WheelColumn
                  label="시"
                  items={hourItems}
                  value={String(hour12)}
                  onChange={(next) => setDraft((current) => ({
                    ...current,
                    hour: period === 'pm' ? (Number(next) % 12) + 12 : Number(next) % 12,
                  }))}
                />
                <div className="event-wheel-colon" aria-hidden="true">:</div>
                <WheelColumn label="분" items={minuteItems} value={String(draft.minute)} onChange={(next) => setDraft((current) => ({ ...current, minute: Number(next) }))} />
              </div>
            </div>

            <footer className="event-picker-actions">
              <button type="button" className="event-picker-cancel" onClick={() => setOpen(false)}>취소</button>
              <button type="button" className="event-picker-apply" disabled={!hasChanged} onClick={apply}>선택 완료</button>
            </footer>
          </section>
        </div>
      ) : null}
    </div>
  );
}

function EventEditForm({ schedule, dateLabel, timezone, onSaved, onDeleted, onCancel }: EventEditFormProps) {
  const mutations = useScheduleMutations();
  const [reminderMinutes, setReminderMinutes] = useState<ReminderMinutes[]>(schedule.reminderMinutes ?? [1440]);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const form = useForm<ScheduleFormValues>({
    resolver: zodResolver(scheduleFormSchema),
    defaultValues: {
      title: schedule.title,
      startAt: utcToDateTimeLocal(schedule.startAt, timezone),
      endAt: utcToDateTimeLocal(schedule.endAt, timezone),
      location: schedule.location ?? '',
    },
  });
  const startAt = form.watch('startAt');
  const endAt = form.watch('endAt');
  const remindersChanged = JSON.stringify([...reminderMinutes].sort((a, b) => b - a))
    !== JSON.stringify([...(schedule.reminderMinutes ?? [1440])].sort((a, b) => b - a));
  const toggleReminder = (minutes: ReminderMinutes) => setReminderMinutes((current) => current.includes(minutes)
    ? current.filter((value) => value !== minutes)
    : [...current, minutes]);

  useEffect(() => {
    if (!deleteConfirmOpen) return undefined;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !mutations.remove.isPending) setDeleteConfirmOpen(false);
    };
    window.addEventListener('keydown', closeOnEscape);
    return () => window.removeEventListener('keydown', closeOnEscape);
  }, [deleteConfirmOpen, mutations.remove.isPending]);

  const removeSchedule = () => {
    mutations.remove.mutate(
      { id: schedule.id, version: schedule.version },
      { onSuccess: onDeleted },
    );
  };

  const submit = (values: ScheduleFormValues) => {
    form.clearErrors();
    let request;
    try {
      request = buildUpdateRequest(schedule, values, timezone);
      if (remindersChanged) request.reminderMinutes = reminderMinutes;
    } catch {
      form.setError('startAt', { message: '입력한 날짜와 시간을 확인해 주세요.' });
      return;
    }
    mutations.update.mutate(
      { id: schedule.id, request },
      {
        onSuccess: onSaved,
        onError: (error) => {
          const fields = fieldErrorMap(error);
          for (const [field, message] of Object.entries(fields)) {
            if (field === 'title' || field === 'startAt' || field === 'endAt' || field === 'location') {
              form.setError(field, { type: 'server', message });
            }
          }
        },
      },
    );
  };

  const operationError = mutations.remove.error ?? mutations.update.error;
  const generalError = operationError instanceof ApiError
    ? operationError.message
    : operationError ? '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.' : undefined;

  return (
    <div className="event-edit-main">
      <header className="event-edit-heading">
        <p>일정 수정</p>
        <h1>일정 정보를 변경해 보세요.</h1>
        <span>{dateLabel}</span>
      </header>

      {generalError ? <div className="alert event-edit-alert" role="alert">{generalError}</div> : null}

      <form className="event-edit-form" onSubmit={form.handleSubmit(submit)} noValidate>
        <FormField
          id={`event-title-${schedule.id}`}
          label="일정 제목"
          placeholder="일정 제목을 입력해 주세요"
          error={form.formState.errors.title?.message}
          {...form.register('title')}
        />
        <div className="event-edit-time-grid">
          <DateTimePicker
            id={`event-start-${schedule.id}`}
            label="시작 시간"
            value={startAt}
            error={form.formState.errors.startAt?.message}
            onChange={(value) => {
              form.setValue('startAt', value, { shouldDirty: true, shouldValidate: true });
              form.setValue('endAt', addMinutesToDateTimeLocal(value, 60), {
                shouldDirty: true,
                shouldValidate: true,
              });
            }}
          />
          <DateTimePicker
            id={`event-end-${schedule.id}`}
            label="종료 시간"
            value={endAt}
            error={form.formState.errors.endAt?.message}
            onChange={(value) => form.setValue('endAt', value, { shouldDirty: true, shouldValidate: true })}
          />
        </div>
        <div className="event-edit-field-with-icon is-location">
          <MapPinIcon />
          <FormField
            id={`event-location-${schedule.id}`}
            label="장소"
            placeholder="장소를 입력해 주세요 (선택)"
            error={form.formState.errors.location?.message}
            {...form.register('location')}
          />
        </div>

        <p className="event-edit-timezone">입력한 시간은 {timezone} 기준으로 저장됩니다.</p>

        <section className="event-reminder-editor" aria-labelledby={`event-reminder-title-${schedule.id}`}>
          <div>
            <strong id={`event-reminder-title-${schedule.id}`}>일정 알림</strong>
            <span>필요한 시점을 여러 개 선택할 수 있어요.</span>
          </div>
          <div className="event-reminder-options">
            {([[10080, '1주일 전'], [4320, '3일 전'], [1440, '1일 전'], [60, '1시간 전']] as const).map(([minutes, label]) => (
              <button type="button" key={minutes} className={reminderMinutes.includes(minutes) ? 'is-selected' : ''} onClick={() => toggleReminder(minutes)}>
                {label}
              </button>
            ))}
          </div>
          {reminderMinutes.length === 0 ? <small>이 일정은 알림을 보내지 않습니다.</small> : null}
        </section>

        <footer className="event-edit-actions">
          <button type="button" className="event-edit-cancel" disabled={mutations.update.isPending || mutations.remove.isPending} onClick={onCancel}>
            취소
          </button>
          <button type="submit" className="event-edit-save" disabled={mutations.remove.isPending || mutations.update.isPending || (!form.formState.isDirty && !remindersChanged)}>
            {mutations.update.isPending ? '저장 중…' : '변경사항 저장'}
          </button>
        </footer>
        <button
          type="button"
          className="event-edit-delete"
          disabled={mutations.update.isPending || mutations.remove.isPending}
          onClick={() => setDeleteConfirmOpen(true)}
        >
          일정 삭제
        </button>
      </form>

      {deleteConfirmOpen ? (
        <div
          className="event-delete-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !mutations.remove.isPending) setDeleteConfirmOpen(false);
          }}
        >
          <section className="event-delete-dialog" role="alertdialog" aria-modal="true" aria-labelledby="event-delete-title">
            <div className="event-delete-icon" aria-hidden="true">×</div>
            <p className="event-delete-kicker">일정 삭제</p>
            <h2 id="event-delete-title">이 일정을 삭제할까요?</h2>
            <div className="event-delete-summary">
              <strong>{schedule.title}</strong>
              <span>{dateLabel}</span>
            </div>
            <p className="event-delete-warning">삭제한 일정은 다시 복구할 수 없습니다.</p>
            {mutations.remove.error ? <div className="alert event-edit-alert" role="alert">{generalError}</div> : null}
            <footer className="event-delete-actions">
              <button type="button" disabled={mutations.remove.isPending} onClick={() => setDeleteConfirmOpen(false)}>취소</button>
              <button type="button" className="is-danger" disabled={mutations.remove.isPending} onClick={removeSchedule}>
                {mutations.remove.isPending ? '삭제 중…' : '삭제하기'}
              </button>
            </footer>
          </section>
        </div>
      ) : null}
    </div>
  );
}

function EventCreateForm({
  date,
  dateLabel,
  timezone,
  defaultDurationMinutes,
  onSaved,
  onCancel,
  onConflict,
}: {
  date: string;
  dateLabel?: string;
  timezone: string;
  defaultDurationMinutes: number;
  onSaved: () => void;
  onCancel: () => void;
  onConflict: (state: ConflictState) => void;
}) {
  const mutations = useScheduleMutations();
  const [reminderMinutes, setReminderMinutes] = useState<ReminderMinutes[]>([1440]);
  const initialValues = useMemo<ScheduleFormValues>(() => {
    const now = new Date();
    const localToday = new Intl.DateTimeFormat('en-CA', {
      timeZone: timezone, year: 'numeric', month: '2-digit', day: '2-digit',
    }).format(now);
    const rounded = Math.ceil(now.getMinutes() / 30) * 30;
    const hour = date === localToday ? (now.getHours() + Math.floor(rounded / 60)) % 24 : 9;
    const minute = date === localToday ? rounded % 60 : 0;
    const start = new Date(`${date}T${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:00`);
    const end = new Date(start.getTime() + defaultDurationMinutes * 60_000);
    const localValue = (value: Date) => `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}T${String(value.getHours()).padStart(2, '0')}:${String(value.getMinutes()).padStart(2, '0')}`;
    return { title: '', startAt: localValue(start), endAt: localValue(end), location: '' };
  }, [date, defaultDurationMinutes, timezone]);
  const form = useForm<ScheduleFormValues>({ resolver: zodResolver(scheduleFormSchema), defaultValues: initialValues });
  const startAt = form.watch('startAt');
  const endAt = form.watch('endAt');
  const toggleReminder = (minutes: ReminderMinutes) => setReminderMinutes((current) => current.includes(minutes)
    ? current.filter((value) => value !== minutes)
    : [...current, minutes]);

  const submit = (values: ScheduleFormValues) => {
    form.clearErrors();
    let request: CreateScheduleRequest;
    try {
      request = {
        title: values.title.trim(),
        startAt: dateTimeLocalToUtc(values.startAt, timezone),
        endAt: dateTimeLocalToUtc(values.endAt, timezone),
        location: values.location.trim() || null,
        reminderMinutes,
      };
    } catch {
      form.setError('startAt', { message: '입력한 날짜와 시간을 확인해 주세요.' });
      return;
    }
    mutations.create.mutate(request, {
      onSuccess: onSaved,
      onError: (error) => {
        if (error instanceof ApiError && error.code === 'SCHEDULE_CONFLICT' && error.confirmationId !== undefined) {
          onConflict({ confirmationId: error.confirmationId, conflicts: error.conflicts, replacementCount: 0 });
          return;
        }
        const fields = fieldErrorMap(error);
        for (const [field, message] of Object.entries(fields)) {
          if (field === 'title' || field === 'startAt' || field === 'endAt' || field === 'location') {
            form.setError(field, { type: 'server', message });
          }
        }
      },
    });
  };
  const generalError = mutations.create.error instanceof ApiError
    ? mutations.create.error.message
    : mutations.create.error ? '일정을 추가하지 못했습니다. 잠시 후 다시 시도해 주세요.' : undefined;

  return (
    <div className="event-edit-main">
      <header className="event-edit-heading">
        <p>일정 추가</p>
        <h1>새로운 일정을 만들어 보세요.</h1>
        <span>{dateLabel}</span>
      </header>
      {generalError ? <div className="alert event-edit-alert" role="alert">{generalError}</div> : null}
      <form className="event-edit-form" onSubmit={form.handleSubmit(submit)} noValidate>
        <FormField id="new-event-title" label="일정 제목" placeholder="일정 제목을 입력해 주세요" error={form.formState.errors.title?.message} {...form.register('title')} />
        <div className="event-edit-time-grid">
          <DateTimePicker
            id="new-event-start"
            label="시작 시간"
            value={startAt}
            error={form.formState.errors.startAt?.message}
            onChange={(value) => {
              form.setValue('startAt', value, { shouldDirty: true, shouldValidate: true });
              form.setValue('endAt', addMinutesToDateTimeLocal(value, 60), {
                shouldDirty: true,
                shouldValidate: true,
              });
            }}
          />
          <DateTimePicker id="new-event-end" label="종료 시간" value={endAt} error={form.formState.errors.endAt?.message} onChange={(value) => form.setValue('endAt', value, { shouldDirty: true, shouldValidate: true })} />
        </div>
        <div className="event-edit-field-with-icon is-location">
          <MapPinIcon />
          <FormField id="new-event-location" label="장소" placeholder="장소를 입력해 주세요 (선택)" error={form.formState.errors.location?.message} {...form.register('location')} />
        </div>
        <p className="event-edit-timezone">입력한 시간은 {timezone} 기준으로 저장됩니다.</p>
        <section className="event-reminder-editor" aria-labelledby="new-event-reminder-title">
          <div><strong id="new-event-reminder-title">일정 알림</strong><span>필요한 시점을 여러 개 선택할 수 있어요.</span></div>
          <div className="event-reminder-options">
            {([[10080, '1주일 전'], [4320, '3일 전'], [1440, '1일 전'], [60, '1시간 전']] as const).map(([minutes, label]) => (
              <button type="button" key={minutes} className={reminderMinutes.includes(minutes) ? 'is-selected' : ''} onClick={() => toggleReminder(minutes)}>{label}</button>
            ))}
          </div>
          {reminderMinutes.length === 0 ? <small>이 일정은 알림을 보내지 않습니다.</small> : null}
        </section>
        <footer className="event-edit-actions">
          <button type="button" className="event-edit-cancel" disabled={mutations.create.isPending} onClick={onCancel}>취소</button>
          <button type="submit" className="event-edit-save" disabled={mutations.create.isPending}>{mutations.create.isPending ? '추가 중…' : '일정 추가'}</button>
        </footer>
      </form>
    </div>
  );
}

export function DailyScheduleDetailPage() {
  const { date, scheduleId } = useParams<{ date: string; scheduleId: string }>();
  const navigate = useNavigate();
  const currentUser = useCurrentUser();
  const queryClient = useQueryClient();
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [conflict, setConflict] = useState<ConflictState>();
  const mutations = useScheduleMutations();
  const id = Number(scheduleId);
  const isCreateMode = scheduleId === undefined;

  const logoutMutation = useMutation({
    mutationFn: () => logout(),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: currentUserQueryKey });
      navigate('/welcome', { replace: true });
    },
  });

  const detail = useSchedule(!isCreateMode && Number.isFinite(id) ? id : null);
  const isInvalidDate = Boolean(date && !DATE_PATTERN.test(date));
  const holidayName = date ? getHolidayName(date) : undefined;

  const dateParts = useMemo(() => {
    if (!date || !DATE_PATTERN.test(date)) return null;
    const [year, month, day] = date.split('-').map(Number);
    const sample = new Date(year, month - 1, day);
    if (Number.isNaN(sample.getTime())) return null;
    return {
      day: String(day).padStart(2, '0'),
      month: new Intl.DateTimeFormat('ko-KR', { month: 'long' }).format(sample),
      full: new Intl.DateTimeFormat('ko-KR', {
        year: 'numeric', month: 'long', day: 'numeric', weekday: 'long',
      }).format(sample),
    };
  }, [date]);

  if (!currentUser.data) return null;
  const timezone = currentUser.data.timezone;

  return (
    <AppLayout
      email={currentUser.data.email}
      onOpenSettings={() => setSettingsOpen(true)}
      onLogout={() => logoutMutation.mutate()}
      logoutPending={logoutMutation.isPending}
    >
      <section className="daily-schedule-detail-page">
        <nav className="event-detail-nav" aria-label="상세 페이지 이동">
          <Link to={`/day/${date ?? ''}`} className="event-detail-back">
            <span aria-hidden="true">←</span> {dateParts?.month ?? ''} 일정
          </Link>
          <button type="button" className="event-detail-calendar-link" onClick={() => navigate('/')}>
            캘린더
          </button>
        </nav>

        {(isInvalidDate || (!isCreateMode && !Number.isFinite(id))) ? (
          <div className="empty-state daily-empty-state">
            <h3>일정 정보를 확인할 수 없습니다.</h3>
            <p>날짜와 일정 주소가 올바른지 다시 확인해 주세요.</p>
            <button type="button" className="primary-button compact-button" onClick={() => navigate('/')}>
              캘린더로 이동
            </button>
          </div>
        ) : isCreateMode && date && dateParts ? (
          <article className="event-detail-shell event-edit-shell">
            <aside className="event-detail-date" aria-label={dateParts.full}>
              <span className="event-detail-month">{dateParts.month}</span>
              <strong>{dateParts.day}</strong>
              <span className="event-detail-weekday">{dateParts.full.split(' ').at(-1)}</span>
              {holidayName ? <span className="event-detail-holiday">{holidayName}</span> : null}
            </aside>
            <EventCreateForm
              date={date}
              dateLabel={dateParts.full}
              timezone={timezone}
              defaultDurationMinutes={currentUser.data.chatPreferences?.defaultDurationMinutes ?? 60}
              onSaved={() => navigate(`/day/${date}`, { replace: true })}
              onCancel={() => navigate(`/day/${date}`)}
              onConflict={setConflict}
            />
          </article>
        ) : detail.isPending ? (
          <div className="status-state">일정 정보를 불러오고 있습니다.</div>
        ) : detail.error ? (
          <div className="inline-state event-detail-error" role="alert">
            <p>{detail.error instanceof ApiError && detail.error.status === 404
              ? '삭제되었거나 찾을 수 없는 일정입니다.'
              : detail.error instanceof ApiError ? detail.error.message : '일정 상세를 불러오지 못했습니다.'}</p>
            <button type="button" className="secondary-button compact-button" onClick={() => navigate(`/day/${date}`)}>
              일정 목록으로 돌아가기
            </button>
          </div>
        ) : detail.data ? (
          <article className="event-detail-shell event-edit-shell">
            <aside className="event-detail-date" aria-label={dateParts?.full}>
              <span className="event-detail-month">{dateParts?.month}</span>
              <strong>{dateParts?.day}</strong>
              <span className="event-detail-weekday">{dateParts?.full.split(' ').at(-1)}</span>
              {holidayName ? <span className="event-detail-holiday">{holidayName}</span> : null}
            </aside>

            <EventEditForm
              schedule={detail.data}
              dateLabel={dateParts?.full}
              timezone={timezone}
              onSaved={() => navigate(`/day/${date}`, { replace: true })}
              onDeleted={() => navigate(`/day/${date}`, { replace: true })}
              onCancel={() => navigate(`/day/${date}`)}
            />
          </article>
        ) : null}
      </section>

      {settingsOpen ? (
        <DialogShell title="설정" onClose={() => setSettingsOpen(false)}>
          <SettingsPanel onSaved={() => setSettingsOpen(false)} />
        </DialogShell>
      ) : null}

      {conflict ? (
        <ConflictDialog
          state={conflict}
          timeZone={timezone}
          pending={mutations.approve.isPending}
          error={mutations.approve.error}
          onApprove={() => mutations.approve.mutate(conflict.confirmationId, {
            onSuccess: () => navigate(`/day/${date}`, { replace: true }),
          })}
          onClose={() => setConflict(undefined)}
        />
      ) : null}
    </AppLayout>
  );
}
