const FALLBACK_TIMEZONES = [
  'Africa/Cairo',
  'America/Chicago',
  'America/Los_Angeles',
  'America/New_York',
  'Asia/Bangkok',
  'Asia/Dubai',
  'Asia/Hong_Kong',
  'Asia/Seoul',
  'Asia/Shanghai',
  'Asia/Singapore',
  'Asia/Tokyo',
  'Australia/Sydney',
  'Europe/Berlin',
  'Europe/London',
  'Pacific/Auckland',
  'UTC',
];

export function supportedTimezones(current: string): string[] {
  const intl = Intl as typeof Intl & { supportedValuesOf?: (key: 'timeZone') => string[] };
  const available = intl.supportedValuesOf?.('timeZone') ?? FALLBACK_TIMEZONES;
  return [...new Set([current, ...available])].sort();
}
