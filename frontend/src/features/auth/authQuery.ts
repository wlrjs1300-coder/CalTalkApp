import { queryOptions, useQuery } from '@tanstack/react-query';
import { ApiError } from '../../api/errors';
import { getCurrentUser } from '../../api/user';

export const currentUserQueryKey = ['current-user'] as const;

const BACKEND_WAKE_RETRY_LIMIT = 18;

export function isBackendStartingError(error: unknown): boolean {
  return error instanceof ApiError && [0, 502, 503, 504].includes(error.status);
}

export const currentUserQueryOptions = queryOptions({
  queryKey: currentUserQueryKey,
  queryFn: ({ signal }) => getCurrentUser(signal),
  staleTime: 30_000,
  retry: (failureCount, error) =>
    isBackendStartingError(error) && failureCount < BACKEND_WAKE_RETRY_LIMIT,
  retryDelay: (attemptIndex) => Math.min(2_000 + attemptIndex * 1_000, 10_000),
});

export function useCurrentUser() {
  return useQuery(currentUserQueryOptions);
}
