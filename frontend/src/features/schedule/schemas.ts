import { z } from 'zod';
import type { ScheduleDetail, UpdateScheduleRequest } from '../../api/schedule';
import { dateTimeLocalToUtc } from './dateTime';

export const scheduleFormSchema = z
  .object({
    title: z
      .string()
      .trim()
      .min(1, '제목을 입력해 주세요.')
      .max(200, '제목은 200자 이하로 입력해 주세요.'),
    startAt: z.string().min(1, '시작 시간을 입력해 주세요.'),
    endAt: z.string().min(1, '종료 시간을 입력해 주세요.'),
    location: z.string().max(200, '장소는 200자 이하로 입력해 주세요.'),
  })
  .superRefine((values, context) => {
    if (values.startAt && values.endAt && values.startAt >= values.endAt) {
      context.addIssue({
        code: 'custom',
        path: ['endAt'],
        message: '종료 시간은 시작 시간보다 늦어야 합니다.',
      });
    }
  });

export type ScheduleFormValues = z.infer<typeof scheduleFormSchema>;

export function buildUpdateRequest(
  original: ScheduleDetail,
  values: ScheduleFormValues,
  timeZone: string,
): UpdateScheduleRequest {
  const request: UpdateScheduleRequest = { version: original.version };
  const title = values.title.trim();
  const startAt = dateTimeLocalToUtc(values.startAt, timeZone);
  const endAt = dateTimeLocalToUtc(values.endAt, timeZone);
  const location = values.location.trim();

  if (title !== original.title) request.title = title;
  if (startAt !== original.startAt) request.startAt = startAt;
  if (endAt !== original.endAt) request.endAt = endAt;

  const originalLocation = original.location ?? '';
  if (location !== originalLocation) request.location = location === '' ? null : location;
  return request;
}
