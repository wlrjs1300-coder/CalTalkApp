import { queryOptions, useQuery } from '@tanstack/react-query';
import { ApiError } from '../../api/errors';
import { getSchedule, getSchedules } from '../../api/schedule';

export const scheduleKeys = {
  all: ['schedules'] as const,
  list: (from: string, to: string) => ['schedules', { from, to }] as const,
  detail: (id: number) => ['schedule', id] as const,
};

export function schedulesQueryOptions(from: string, to: string) {
  return queryOptions({
    queryKey: scheduleKeys.list(from, to),
    queryFn: ({ signal }) => getSchedules(from, to, signal),
    staleTime: 30_000,
  });
}

export function useSchedules(from: string, to: string) {
  return useQuery(schedulesQueryOptions(from, to));
}

export function useSchedule(id: number | null) {
  const enabled = id !== null;
  return useQuery({
    queryKey: scheduleKeys.detail(id ?? 0),
    queryFn: async ({ signal }) => {
      if (id === null) {
        throw new ApiError({
          status: 0,
          code: 'SCHEDULE_ID_MISSING',
          message: '상세 일정을 조회할 ID가 없습니다.',
        });
      }
      const schedule = await getSchedule(id, signal);
      if (schedule === undefined) {
        throw new ApiError({
          status: 0,
          code: 'INVALID_API_RESPONSE',
          message: '일정 상세 응답을 확인할 수 없습니다.',
        });
      }
      return schedule;
    },
    enabled,
    staleTime: 30_000,
  });
}
