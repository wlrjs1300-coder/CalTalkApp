import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { ApiError } from '../../../api/errors';
import type { CreateScheduleRequest, ScheduleDetail } from '../../../api/schedule';
import { currentUserQueryKey } from '../../auth/authQuery';
import { dateTimeLocalToUtc, scheduleQueryRange } from '../dateTime';
import { useScheduleMutations } from '../mutations';
import { useSchedule, useSchedules } from '../queries';
import { buildUpdateRequest, type ScheduleFormValues } from '../schemas';
import { ConflictDialog, type ConflictState } from './ConflictDialog';
import { DeleteScheduleDialog } from './DeleteScheduleDialog';
import { ScheduleDetail as DetailDialog } from './ScheduleDetail';
import { ScheduleForm } from './ScheduleForm';
import { ScheduleList } from './ScheduleList';

interface ScheduleWorkspaceProps {
  timeZone: string;
}

type FormState = { mode: 'create' } | { mode: 'edit'; original: ScheduleDetail };

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
      ? '선택한 시간은 일광 절약 시간 전환으로 존재하지 않습니다.'
      : '날짜와 시간대를 확인해 주세요.';
  return new ApiError({ status: 422, code: 'INVALID_LOCAL_TIME', message });
}

export function ScheduleWorkspace({ timeZone }: ScheduleWorkspaceProps) {
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

  useEffect(() => {
    if (schedules.error instanceof ApiError && schedules.error.status === 401) {
      queryClient.removeQueries({ queryKey: currentUserQueryKey });
    }
  }, [queryClient, schedules.error]);

  const openCreate = () => {
    mutations.create.reset();
    setFormError(undefined);
    setFormState({ mode: 'create' });
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
        setNotice('일정을 생성했습니다.');
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
          setNotice('일정을 수정했습니다.');
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
          setNotice('일정을 삭제했습니다.');
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
        setNotice('충돌을 확인하고 일정을 저장했습니다.');
      },
      onError: (error) => {
        const replacement = error instanceof ApiError ? conflictFrom(error) : undefined;
        if (replacement && conflict.replacementCount < 3) {
          setConflict({
            ...replacement,
            replacementCount: conflict.replacementCount + 1,
            notice: '일정 정보가 바뀌어 최신 내용으로 다시 확인이 필요합니다.',
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

  return (
    <section className="schedule-section" aria-labelledby="schedule-heading">
      <div className="section-heading">
        <div>
          <p className="eyebrow">나의 캘린더</p>
          <h2 id="schedule-heading">다가오는 일정</h2>
          <p className="muted">가까운 일정부터 시간 순서로 확인하세요.</p>
        </div>
        <button type="button" className="primary-button compact-button" onClick={openCreate}>
          <span aria-hidden="true">＋</span> 새 일정
        </button>
      </div>
      {notice ? (
        <div className="success-notice" role="status">
          {notice}
        </div>
      ) : null}
      <ScheduleList
        schedules={schedules.data?.items}
        timeZone={timeZone}
        isLoading={schedules.isPending}
        error={schedules.error}
        onRetry={() => void schedules.refetch()}
        onSelect={setSelectedId}
        onCreate={openCreate}
      />

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
          pending={
            formState.mode === 'create' ? mutations.create.isPending : mutations.update.isPending
          }
          error={activeFormError}
          onSubmit={(values) =>
            formState.mode === 'create'
              ? submitCreate(values)
              : submitEdit(values, formState.original)
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
    </section>
  );
}
