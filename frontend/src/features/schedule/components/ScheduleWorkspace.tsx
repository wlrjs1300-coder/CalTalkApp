import { useEffect, useMemo, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../../../api/errors';
import type { CreateScheduleRequest, ScheduleDetail } from '../../../api/schedule';
import { currentUserQueryKey } from '../../auth/authQuery';
import { dateTimeLocalToUtc, getDateKey, scheduleQueryRange } from '../dateTime';
import { useScheduleMutations } from '../mutations';
import { useSchedule, useSchedules } from '../queries';
import { buildUpdateRequest, type ScheduleFormValues } from '../schemas';
import { ConflictDialog, type ConflictState } from './ConflictDialog';
import { DeleteScheduleDialog } from './DeleteScheduleDialog';
import { ScheduleCalendar } from './ScheduleCalendar';
import { ScheduleDetail as DetailDialog } from './ScheduleDetail';
import { ScheduleForm } from './ScheduleForm';

interface ScheduleWorkspaceProps {
  timeZone: string;
  viewMode?: WorkspaceTab;
  onDateSelect?: (dateKey: string) => void;
}

type FormState = { mode: 'create' } | { mode: 'edit'; original: ScheduleDetail };
type WorkspaceTab = 'list' | 'calendar';

function conflictFrom(error: ApiError): ConflictState | undefined {
  if (
    (error.code !== 'SCHEDULE_CONFLICT' && error.code !== 'CONFIRMATION_SUPERSEDED') ||
    error.confirmationId === undefined
  ) {
    return undefined;
  }
  return {
    confirmationId: error.confirmationId,
    conflicts: error.conflicts,
    replacementCount: 0,
  };
}

function dateError(error: unknown): ApiError {
  const message =
    error instanceof Error && error.message === 'NONEXISTENT_LOCAL_TIME'
      ? '입력하신 시작 시간은 존재하지 않는 시간입니다. 시간을 다시 선택해 주세요.'
      : '입력이 올바르지 않습니다.';
  return new ApiError({ status: 422, code: 'INVALID_LOCAL_TIME', message });
}

export function ScheduleWorkspace({ timeZone, onDateSelect }: ScheduleWorkspaceProps) {
  const sectionRef = useRef<HTMLElement | null>(null);
  const [showScrollTop, setShowScrollTop] = useState(false);
  const [isPwaMode, setIsPwaMode] = useState(false);

  const range = useMemo(() => scheduleQueryRange(), []);
  const schedules = useSchedules(range.from, range.to);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const detail = useSchedule(selectedId);
  const mutations = useScheduleMutations();
  const queryClient = useQueryClient();
  const [formState, setFormState] = useState<FormState>();
  const [formError, setFormError] = useState<unknown>();
  const [deleteTarget, setDeleteTarget] = useState<ScheduleDetail>();
  const [conflict, setConflict] = useState<ConflictState>();
  const [notice, setNotice] = useState<string>();

  const tab: WorkspaceTab = 'calendar';

  useEffect(() => {
    if (schedules.error instanceof ApiError && schedules.error.status === 401) {
      queryClient.removeQueries({ queryKey: currentUserQueryKey });
    }
  }, [queryClient, schedules.error]);

  useEffect(() => {
    const standalone = window.matchMedia('(display-mode: standalone)');
    const mobileViewport = window.matchMedia('(max-width: 900px)');
    const iosStandalone = Boolean((window.navigator as Navigator & { standalone?: boolean }).standalone);
    const checkPwaMode = () => setIsPwaMode(standalone.matches || iosStandalone || mobileViewport.matches);

    checkPwaMode();

    if (typeof standalone.addEventListener === 'function' && typeof mobileViewport.addEventListener === 'function') {
      standalone.addEventListener('change', checkPwaMode);
      mobileViewport.addEventListener('change', checkPwaMode);
      return () => {
        standalone.removeEventListener('change', checkPwaMode);
        mobileViewport.removeEventListener('change', checkPwaMode);
      };
    }

    return undefined;
  }, []);

  useEffect(() => {
    const target = sectionRef.current;
    if (!target) return;
    const onScroll = () => {
      const rect = target.getBoundingClientRect();
      const schedulePassed = rect.top <= -64;
      const isPastSectionStart = window.scrollY > 180;
      setShowScrollTop(schedulePassed && isPastSectionStart);
    };
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const onScrollTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const submitCreate = (values: ScheduleFormValues) => {
    setFormError(undefined);
    let request: CreateScheduleRequest;
    try {
      request = {
        title: values.title.trim(),
        startAt: dateTimeLocalToUtc(values.startAt, timeZone),
        endAt: dateTimeLocalToUtc(values.endAt, timeZone),
        location: values.location.trim() || null,
      };
    } catch (error) {
      setFormError(dateError(error));
      return;
    }
    mutations.create.mutate(request, {
      onSuccess: () => {
        setFormState(undefined);
        setNotice('일정이 생성되었습니다.');
      },
      onError: (error) => {
        const nextConflict = error instanceof ApiError ? conflictFrom(error) : undefined;
        if (nextConflict) {
          setFormState(undefined);
          setConflict(nextConflict);
        } else {
          setFormError(error);
        }
      },
    });
  };

  const submitEdit = (values: ScheduleFormValues, original: ScheduleDetail) => {
    setFormError(undefined);
    let request;
    try {
      request = buildUpdateRequest(original, values, timeZone);
    } catch (error) {
      setFormError(dateError(error));
      return;
    }
    mutations.update.mutate(
      { id: original.id, request },
      {
        onSuccess: (updated) => {
          setFormState(undefined);
          setSelectedId(updated.id);
          setNotice('일정이 수정되었습니다.');
        },
        onError: (error) => {
          const nextConflict = error instanceof ApiError ? conflictFrom(error) : undefined;
          if (nextConflict) {
            setFormState(undefined);
            setSelectedId(null);
            setConflict(nextConflict);
          } else {
            setFormError(error);
          }
        },
      },
    );
  };

  const confirmDelete = () => {
    if (!deleteTarget) return;
    mutations.remove.mutate(
      { id: deleteTarget.id, version: deleteTarget.version },
      {
        onSuccess: () => {
          setDeleteTarget(undefined);
          setSelectedId(null);
          setNotice('일정이 삭제되었습니다.');
        },
      },
    );
  };

  const approve = () => {
    if (!conflict) return;
    mutations.approve.mutate(conflict.confirmationId, {
      onSuccess: (schedule) => {
        setConflict(undefined);
        setSelectedId(schedule.id);
        setNotice('일정 충돌을 확인해 진행했어요.');
      },
      onError: (error) => {
        const replacement = error instanceof ApiError ? conflictFrom(error) : undefined;
        if (replacement && conflict.replacementCount < 3) {
          setConflict({
            ...replacement,
            replacementCount: conflict.replacementCount + 1,
            notice: '일정 변경이 밀려서 다시 확인이 필요해요.',
          });
        }
      },
    });
  };

  const activeFormError =
    formError ?? (formState?.mode === 'create' ? mutations.create.error : mutations.update.error);
  const approvalError =
    mutations.approve.error instanceof ApiError &&
    mutations.approve.error.code === 'CONFIRMATION_SUPERSEDED' &&
    conflict?.replacementCount
      ? undefined
      : mutations.approve.error;

  const scheduleCount = schedules.data?.items.length ?? 0;
  const isEmpty = !schedules.isPending && !schedules.error && scheduleCount === 0;
  const openSchedule = (id: number) => {
    const schedule = schedules.data?.items.find((item) => item.id === id);
    if (onDateSelect && schedule) {
      onDateSelect(getDateKey(schedule.startAt, timeZone));
      return;
    }
    setSelectedId(id);
  };

  return (
    <section
      ref={sectionRef}
      className={`schedule-section${isEmpty ? ' is-empty' : ''} ${tab === 'calendar' ? 'is-calendar-mode' : ''}`}
    >
      {notice ? (
        <div className="success-notice" role="status">
          {notice}
        </div>
      ) : null}

      <ScheduleCalendar schedules={schedules.data?.items} timeZone={timeZone} onSelect={openSchedule} onDateSelect={onDateSelect} />

      {selectedId !== null && !formState && !deleteTarget ? (
        <DetailDialog
          schedule={detail.data}
          timeZone={timeZone}
          loading={detail.isPending}
          error={detail.error}
          onClose={() => setSelectedId(null)}
          onEdit={(schedule) => {
            setFormError(undefined);
            mutations.update.reset();
            setFormState({ mode: 'edit', original: schedule });
          }}
          onDelete={(schedule) => {
            mutations.remove.reset();
            setDeleteTarget(schedule);
          }}
        />
      ) : null}

      {formState ? (
        <ScheduleForm
          mode={formState.mode}
          original={formState.mode === 'edit' ? formState.original : undefined}
          timeZone={timeZone}
          pending={formState.mode === 'create' ? mutations.create.isPending : mutations.update.isPending}
          error={activeFormError}
          onSubmit={(values) =>
            formState.mode === 'create' ? submitCreate(values) : submitEdit(values, formState.original)
          }
          onClose={() => setFormState(undefined)}
        />
      ) : null}

      {deleteTarget ? (
        <DeleteScheduleDialog
          schedule={deleteTarget}
          pending={mutations.remove.isPending}
          error={mutations.remove.error}
          onConfirm={confirmDelete}
          onClose={() => setDeleteTarget(undefined)}
        />
      ) : null}

      {conflict ? (
        <ConflictDialog
          state={conflict}
          timeZone={timeZone}
          pending={mutations.approve.isPending}
          error={approvalError}
          onApprove={approve}
          onClose={() => setConflict(undefined)}
        />
      ) : null}

      {showScrollTop && isPwaMode ? (
        <button type="button" className="scroll-top-button" onClick={onScrollTop} aria-label="맨 위로 이동">
          <span aria-hidden="true">↑</span>
        </button>
      ) : null}
    </section>
  );
}
