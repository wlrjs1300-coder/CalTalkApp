import { CSRF_HEADER_NAME, readCsrfToken } from './csrf';
import { ApiError, toApiError, toNetworkError } from './errors';
import type { ApiErrorBody } from './types';

export function resolveApiBaseUrl(configured: string | undefined): string {
  return (configured ?? 'http://localhost:8080').trim().replace(/\/$/, '');
}

const API_BASE_URL = resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL);

export function apiUrl(path: string): string {
  return `${API_BASE_URL}${path}`;
}

let csrfBootstrap: Promise<string> | undefined;

interface ApiRequestOptions {
  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE';
  body?: unknown;
  signal?: AbortSignal;
  csrf?: boolean;
}

async function parseJson(response: Response): Promise<unknown> {
  if (response.status === 204) return undefined;
  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.toLowerCase().includes('application/json')) return undefined;
  try {
    return await response.json();
  } catch {
    return undefined;
  }
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function asErrorBody(value: unknown): ApiErrorBody | undefined {
  return isObject(value) ? (value as ApiErrorBody) : undefined;
}

async function bootstrapCsrf(force = false, signal?: AbortSignal): Promise<string> {
  const existing = readCsrfToken();
  if (existing && !force) return existing;

  if (!csrfBootstrap) {
    csrfBootstrap = fetch(`${API_BASE_URL}/api/v1/csrf`, {
      method: 'GET',
      credentials: 'include',
      signal,
    })
      .then(async (response) => {
        if (!response.ok) {
          throw toApiError(response.status, asErrorBody(await parseJson(response)));
        }
        const token = readCsrfToken();
        if (!token) {
          throw new ApiError({
            status: 0,
            code: 'CSRF_TOKEN_MISSING',
            message: '보안 토큰을 발급받지 못했습니다. 다시 시도해 주세요.',
          });
        }
        return token;
      })
      .catch((error: unknown) => {
        throw toNetworkError(error);
      })
      .finally(() => {
        csrfBootstrap = undefined;
      });
  }

  return csrfBootstrap;
}

async function execute<T>(path: string, options: ApiRequestOptions, token?: string): Promise<T> {
  const headers = new Headers();
  if (options.body !== undefined) headers.set('Content-Type', 'application/json');
  if (token) headers.set(CSRF_HEADER_NAME, token);

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: options.method ?? 'GET',
      credentials: 'include',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal,
    });
  } catch (error) {
    throw toNetworkError(error);
  }

  const body = await parseJson(response);
  if (!response.ok) throw toApiError(response.status, asErrorBody(body));
  return body as T;
}

export async function apiRequest<T = void>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const method = options.method ?? 'GET';
  const requiresCsrf = options.csrf ?? ['POST', 'PATCH', 'DELETE'].includes(method);
  if (!requiresCsrf) return execute<T>(path, options);

  const token = await bootstrapCsrf(false, options.signal);
  try {
    return await execute<T>(path, options, token);
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 403) throw error;
    const refreshedToken = await bootstrapCsrf(true, options.signal);
    return execute<T>(path, options, refreshedToken);
  }
}

export function resetCsrfBootstrapForTests(): void {
  csrfBootstrap = undefined;
}
