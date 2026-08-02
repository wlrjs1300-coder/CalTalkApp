export const CSRF_COOKIE_NAME = 'XSRF-TOKEN';
export const CSRF_HEADER_NAME = 'X-XSRF-TOKEN';

export function readCookie(name: string, cookieSource = document.cookie): string | undefined {
  const prefix = `${encodeURIComponent(name)}=`;
  const entry = cookieSource
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith(prefix));

  if (!entry) return undefined;

  const value = entry.slice(prefix.length);
  try {
    return decodeURIComponent(value);
  } catch {
    return undefined;
  }
}

export function readCsrfToken(): string | undefined {
  return readCookie(CSRF_COOKIE_NAME);
}
