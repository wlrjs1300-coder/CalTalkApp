interface LocalDateTimeParts {
  year: number;
  month: number;
  day: number;
  hour: number;
  minute: number;
  second: number;
}

function formatter(timeZone: string, includeSeconds = false): Intl.DateTimeFormat {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone,
    hourCycle: 'h23',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    ...(includeSeconds ? { second: '2-digit' } : {}),
  });
}

function partsAt(instant: Date, timeZone: string): LocalDateTimeParts {
  const values = Object.fromEntries(
    formatter(timeZone, true)
      .formatToParts(instant)
      .filter((part) => part.type !== 'literal')
      .map((part) => [part.type, Number(part.value)]),
  );
  return {
    year: values.year,
    month: values.month,
    day: values.day,
    hour: values.hour,
    minute: values.minute,
    second: values.second,
  };
}

function pad(value: number): string {
  return String(value).padStart(2, '0');
}

function toLocalValue(parts: LocalDateTimeParts): string {
  return `${parts.year}-${pad(parts.month)}-${pad(parts.day)}T${pad(parts.hour)}:${pad(parts.minute)}`;
}

export function utcToDateTimeLocal(iso: string, timeZone: string): string {
  const instant = new Date(iso);
  if (Number.isNaN(instant.getTime())) throw new Error('INVALID_DATE');
  try {
    return toLocalValue(partsAt(instant, timeZone));
  } catch {
    throw new Error('INVALID_TIMEZONE');
  }
}

export function formatInTimezone(iso: string, timeZone: string): string {
  const instant = new Date(iso);
  if (Number.isNaN(instant.getTime())) return '유효하지 않은 시간';
  try {
    return new Intl.DateTimeFormat('ko-KR', {
      timeZone,
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(instant);
  } catch {
    return '유효하지 않은 시간대';
  }
}

function parseLocal(value: string): LocalDateTimeParts | undefined {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/.exec(value);
  if (!match) return undefined;
  const parsed: LocalDateTimeParts = {
    year: Number(match[1]),
    month: Number(match[2]),
    day: Number(match[3]),
    hour: Number(match[4]),
    minute: Number(match[5]),
    second: match[6] === undefined ? 0 : Number(match[6]),
  };
  const check = new Date(
    Date.UTC(parsed.year, parsed.month - 1, parsed.day, parsed.hour, parsed.minute, parsed.second),
  );
  if (
    check.getUTCFullYear() !== parsed.year ||
    check.getUTCMonth() + 1 !== parsed.month ||
    check.getUTCDate() !== parsed.day ||
    parsed.hour > 23 ||
    parsed.minute > 59 ||
    parsed.second > 59
  ) {
    return undefined;
  }
  return parsed;
}

function offsetAt(instantMs: number, timeZone: string): number {
  const parts = partsAt(new Date(instantMs), timeZone);
  const representedAsUtc = Date.UTC(
    parts.year,
    parts.month - 1,
    parts.day,
    parts.hour,
    parts.minute,
    parts.second,
  );
  return representedAsUtc - Math.floor(instantMs / 1000) * 1000;
}

function sameParts(left: LocalDateTimeParts, right: LocalDateTimeParts): boolean {
  return (
    left.year === right.year &&
    left.month === right.month &&
    left.day === right.day &&
    left.hour === right.hour &&
    left.minute === right.minute &&
    left.second === right.second
  );
}

export function dateTimeLocalToUtc(value: string, timeZone: string): string {
  const desired = parseLocal(value);
  if (!desired) throw new Error('INVALID_DATE');

  const wallClockUtc = Date.UTC(
    desired.year,
    desired.month - 1,
    desired.day,
    desired.hour,
    desired.minute,
    desired.second,
  );

  let offsets: Set<number>;
  try {
    offsets = new Set(
      [-180, -1, 0, 1, 180].map((days) => offsetAt(wallClockUtc + days * 86_400_000, timeZone)),
    );
  } catch {
    throw new Error('INVALID_TIMEZONE');
  }

  const candidates = [...offsets]
    .map((offset) => wallClockUtc - offset)
    .filter((candidate) => sameParts(partsAt(new Date(candidate), timeZone), desired))
    .sort((left, right) => left - right);

  if (candidates.length === 0) throw new Error('NONEXISTENT_LOCAL_TIME');
  return new Date(candidates[0]).toISOString();
}

export function scheduleQueryRange(now = new Date()): { from: string; to: string } {
  const from = new Date(now);
  from.setUTCFullYear(from.getUTCFullYear() - 1);
  const to = new Date(now);
  to.setUTCFullYear(to.getUTCFullYear() + 2);
  return { from: from.toISOString(), to: to.toISOString() };
}
