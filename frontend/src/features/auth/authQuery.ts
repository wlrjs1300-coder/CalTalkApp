import { queryOptions, useQuery } from '@tanstack/react-query';
import { getCurrentUser } from '../../api/user';

export const currentUserQueryKey = ['current-user'] as const;

export const currentUserQueryOptions = queryOptions({
  queryKey: currentUserQueryKey,
  queryFn: ({ signal }) => getCurrentUser(signal),
  staleTime: 30_000,
  retry: false,
});

export function useCurrentUser() {
  return useQuery(currentUserQueryOptions);
}
