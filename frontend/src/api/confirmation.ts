import { apiRequest } from './client';
import type { ScheduleDetail } from './schedule';

export function approveConfirmation(confirmationId: number, signal?: AbortSignal) {
  return apiRequest<ScheduleDetail>(`/api/v1/confirmations/${confirmationId}/approve`, {
    method: 'POST',
    body: { conflictAcknowledged: true },
    signal,
  });
}
