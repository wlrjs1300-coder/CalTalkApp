import { useMutation, useQueryClient } from '@tanstack/react-query';
import { approveConfirmation } from '../../api/confirmation';
import {
  createSchedule,
  deleteSchedule,
  updateSchedule,
  type CreateScheduleRequest,
  type ScheduleDetail,
  type UpdateScheduleRequest,
} from '../../api/schedule';
import { scheduleKeys } from './queries';

export function useScheduleMutations() {
  const queryClient = useQueryClient();
  const refreshSchedules = async (schedule?: ScheduleDetail) => {
    if (schedule) {
      queryClient.setQueryData(scheduleKeys.detail(schedule.id), schedule);
    }
    await queryClient.invalidateQueries({ queryKey: scheduleKeys.all });
  };

  const create = useMutation({
    mutationFn: (request: CreateScheduleRequest) => createSchedule(request),
    onSuccess: (schedule) => refreshSchedules(schedule),
  });
  const update = useMutation({
    mutationFn: ({ id, request }: { id: number; request: UpdateScheduleRequest }) =>
      updateSchedule(id, request),
    onSuccess: (schedule) => refreshSchedules(schedule),
  });
  const remove = useMutation({
    mutationFn: ({ id, version }: { id: number; version: number }) => deleteSchedule(id, version),
    onSuccess: async (_result, variables) => {
      queryClient.removeQueries({ queryKey: scheduleKeys.detail(variables.id) });
      await queryClient.invalidateQueries({ queryKey: scheduleKeys.all });
    },
  });
  const approve = useMutation({
    mutationFn: (confirmationId: number) => approveConfirmation(confirmationId),
    onSuccess: (schedule) => refreshSchedules(schedule),
  });

  return { create, update, remove, approve };
}
