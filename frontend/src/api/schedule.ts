import { apiRequest } from './client';

export interface ScheduleListItem {
  id: number;
  title: string;
  startAt: string;
  endAt: string;
  location: string | null;
  version: number;
  reminderMinutes?: ReminderMinutes[];
}

export type ReminderMinutes = 60 | 1440 | 4320 | 10080;

export interface ScheduleDetail extends ScheduleListItem {
  createdAt: string;
  updatedAt: string;
}

export interface ScheduleListResponse {
  items: ScheduleListItem[];
}

export interface CreateScheduleRequest {
  title: string;
  startAt: string;
  endAt: string;
  location: string | null;
  reminderMinutes?: ReminderMinutes[];
}

export interface UpdateScheduleRequest {
  version: number;
  title?: string;
  startAt?: string;
  endAt?: string;
  location?: string | null;
  reminderMinutes?: ReminderMinutes[];
}

export function getSchedules(from: string, to: string, signal?: AbortSignal) {
  const query = new URLSearchParams({ from, to });
  return apiRequest<ScheduleListResponse>(`/api/v1/schedules?${query}`, { signal });
}

export function getSchedule(id: number, signal?: AbortSignal) {
  return apiRequest<ScheduleDetail>(`/api/v1/schedules/${id}`, { signal });
}

export function createSchedule(request: CreateScheduleRequest, signal?: AbortSignal) {
  return apiRequest<ScheduleDetail>('/api/v1/schedules', {
    method: 'POST',
    body: request,
    signal,
  });
}

export function updateSchedule(id: number, request: UpdateScheduleRequest, signal?: AbortSignal) {
  return apiRequest<ScheduleDetail>(`/api/v1/schedules/${id}`, {
    method: 'PATCH',
    body: request,
    signal,
  });
}

export function deleteSchedule(id: number, version: number, signal?: AbortSignal) {
  const query = new URLSearchParams({ version: String(version) });
  return apiRequest<void>(`/api/v1/schedules/${id}?${query}`, {
    method: 'DELETE',
    signal,
  });
}
