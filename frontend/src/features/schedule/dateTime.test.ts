import { describe, expect, it } from 'vitest';
import type { ScheduleDetail } from '../../api/schedule';
import { dateTimeLocalToUtc, formatInTimezone, utcToDateTimeLocal } from './dateTime';
import { buildUpdateRequest, type ScheduleFormValues } from './schemas';

const original: ScheduleDetail = {
  id: 1,
  title: '회의',
  startAt: '2026-01-15T00:00:00.000Z',
  endAt: '2026-01-15T01:00:00.000Z',
  location: '회의실',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
  version: 3,
};

const values: ScheduleFormValues = {
  title: '회의',
  startAt: '2026-01-15T09:00',
  endAt: '2026-01-15T10:00',
  location: '회의실',
};

describe('schedule timezone conversion', () => {
  it('formats a UTC instant in the user timezone', () => {
    expect(utcToDateTimeLocal('2026-01-15T00:00:00Z', 'Asia/Seoul')).toBe('2026-01-15T09:00');
    expect(formatInTimezone('invalid', 'Asia/Seoul')).toBe('유효하지 않은 시간');
  });

  it('converts datetime-local using the user timezone offset', () => {
    expect(dateTimeLocalToUtc('2026-01-15T09:00', 'Asia/Seoul')).toBe('2026-01-15T00:00:00.000Z');
  });

  it('rejects a nonexistent DST local time', () => {
    expect(() => dateTimeLocalToUtc('2026-03-08T02:30', 'America/New_York')).toThrow(
      'NONEXISTENT_LOCAL_TIME',
    );
  });

  it('chooses the earlier instant for an overlapping DST local time', () => {
    expect(dateTimeLocalToUtc('2026-11-01T01:30', 'America/New_York')).toBe(
      '2026-11-01T05:30:00.000Z',
    );
  });

  it('rejects invalid date input', () => {
    expect(() => dateTimeLocalToUtc('2026-02-30T10:00', 'Asia/Seoul')).toThrow('INVALID_DATE');
  });
});

describe('location update contract', () => {
  it('omits unchanged location for KEEP', () => {
    expect(buildUpdateRequest(original, values, 'Asia/Seoul')).toEqual({ version: 3 });
  });

  it('sends a changed string for SET', () => {
    expect(
      buildUpdateRequest(original, { ...values, location: '새 회의실' }, 'Asia/Seoul'),
    ).toEqual({
      version: 3,
      location: '새 회의실',
    });
  });

  it('sends null for REMOVE', () => {
    expect(buildUpdateRequest(original, { ...values, location: '' }, 'Asia/Seoul')).toEqual({
      version: 3,
      location: null,
    });
  });
});
