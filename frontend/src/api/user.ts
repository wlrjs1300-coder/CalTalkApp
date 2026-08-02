import { apiRequest } from './client';
import type { CurrentUser } from './types';

export function getCurrentUser(signal?: AbortSignal): Promise<CurrentUser> {
  return apiRequest('/api/v1/users/me', { signal });
}

export function updateTimezone(timezone: string, signal?: AbortSignal): Promise<CurrentUser> {
  return apiRequest('/api/v1/users/me', {
    method: 'PATCH',
    body: { timezone },
    csrf: true,
    signal,
  });
}
