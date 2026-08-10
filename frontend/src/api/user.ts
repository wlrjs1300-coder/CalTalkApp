import { apiRequest } from './client';
import type { ChatPreferences, CurrentUser } from './types';

export function getCurrentUser(signal?: AbortSignal): Promise<CurrentUser> {
  return apiRequest('/api/v1/users/me', { signal });
}

export function updateChatPreferences(preferences: ChatPreferences, signal?: AbortSignal): Promise<CurrentUser> {
  return apiRequest('/api/v1/users/me/chat-preferences', {
    method: 'PATCH',
    body: preferences,
    csrf: true,
    signal,
  });
}

export function updateTimezone(timezone: string, signal?: AbortSignal): Promise<CurrentUser> {
  return apiRequest('/api/v1/users/me', {
    method: 'PATCH',
    body: { timezone },
    csrf: true,
    signal,
  });
}
