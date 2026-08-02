import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiRequest, resetCsrfBootstrapForTests } from './client';
import { ApiError } from './errors';
import { readCookie } from './csrf';

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function clearCookies(): void {
  for (const cookie of document.cookie.split(';')) {
    const name = cookie.split('=')[0]?.trim();
    if (name) document.cookie = `${name}=; Max-Age=0; Path=/`;
  }
}

describe('API client', () => {
  beforeEach(() => {
    clearCookies();
    resetCsrfBootstrapForTests();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    clearCookies();
  });

  it('parses and URL-decodes an exact cookie name', () => {
    expect(readCookie('XSRF-TOKEN', 'theme=dark; XSRF-TOKEN=a%2Bb%2Fc; XSRF-TOKEN-OLD=x')).toBe(
      'a+b/c',
    );
    expect(readCookie('XSRF-TOKEN', 'XSRF-TOKEN-OLD=x')).toBeUndefined();
  });

  it('bootstraps one token and adds the CSRF header with credentials', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (_input, init) => {
      if (init?.method === 'GET') {
        document.cookie = 'XSRF-TOKEN=issued-token; Path=/';
        return new Response(null, { status: 204 });
      }
      return jsonResponse({ ok: true });
    });
    vi.stubGlobal('fetch', fetchMock);

    await apiRequest('/api/v1/example', { method: 'POST', body: { value: 1 }, csrf: true });

    expect(fetchMock).toHaveBeenCalledTimes(2);
    const [, request] = fetchMock.mock.calls;
    const headers = new Headers(request?.[1]?.headers);
    expect(headers.get('X-XSRF-TOKEN')).toBe('issued-token');
    expect(request?.[1]?.credentials).toBe('include');
  });

  it('returns undefined for a successful 204 response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn<typeof fetch>().mockResolvedValue(new Response(null, { status: 204 })),
    );

    await expect(apiRequest('/api/v1/example')).resolves.toBeUndefined();
  });

  it('converts a JSON backend error without exposing the raw response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn<typeof fetch>().mockResolvedValue(
        jsonResponse(
          {
            status: 422,
            code: 'VALIDATION_ERROR',
            message: '입력 내용을 확인해 주세요.',
            fieldErrors: [{ field: 'email', code: 'INVALID_EMAIL', message: '이메일 오류' }],
          },
          422,
        ),
      ),
    );

    const error = await apiRequest('/api/v1/example').catch((caught: unknown) => caught);
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 422,
      code: 'VALIDATION_ERROR',
      fieldErrors: [{ field: 'email', code: 'INVALID_EMAIL', message: '이메일 오류' }],
    });
  });

  it('retries a CSRF failure only once with a refreshed token', async () => {
    document.cookie = 'XSRF-TOKEN=stale; Path=/';
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({ code: 'FORBIDDEN', message: '거부됨' }, 403))
      .mockImplementationOnce(async () => {
        document.cookie = 'XSRF-TOKEN=fresh; Path=/';
        return new Response(null, { status: 204 });
      })
      .mockResolvedValueOnce(jsonResponse({ code: 'FORBIDDEN', message: '거부됨' }, 403));
    vi.stubGlobal('fetch', fetchMock);

    await expect(
      apiRequest('/api/v1/example', { method: 'DELETE', csrf: true }),
    ).rejects.toMatchObject({ status: 403, code: 'FORBIDDEN' });
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });
});
