import type { ApiErrorBody, ConflictingSchedule, FieldError } from './types';

const STATUS_MESSAGES: Record<number, string> = {
  400: '요청 형식을 확인해 주세요.',
  401: '로그인이 필요합니다.',
  403: '요청 권한 또는 보안 토큰을 확인해 주세요.',
  404: '요청한 정보를 찾을 수 없습니다.',
  409: '현재 상태와 충돌했습니다. 다시 확인해 주세요.',
  422: '입력 내용을 확인해 주세요.',
  500: '서버에서 요청을 처리하지 못했습니다.',
};

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: FieldError[];
  readonly confirmationId?: number;
  readonly conflicts: ConflictingSchedule[];

  constructor(options: {
    status: number;
    code: string;
    message: string;
    fieldErrors?: FieldError[];
    confirmationId?: number;
    conflicts?: ConflictingSchedule[];
    cause?: unknown;
  }) {
    super(options.message, { cause: options.cause });
    this.name = 'ApiError';
    this.status = options.status;
    this.code = options.code;
    this.fieldErrors = options.fieldErrors ?? [];
    this.confirmationId = options.confirmationId;
    this.conflicts = options.conflicts ?? [];
  }
}

export function errorMessageForStatus(status: number): string {
  return STATUS_MESSAGES[status] ?? '요청을 처리하지 못했습니다.';
}

export function toApiError(status: number, body?: ApiErrorBody): ApiError {
  return new ApiError({
    status,
    code: body?.code ?? `HTTP_${status}`,
    message: body?.message ?? errorMessageForStatus(status),
    fieldErrors: Array.isArray(body?.fieldErrors) ? body.fieldErrors : [],
    confirmationId: body?.confirmationId,
    conflicts: Array.isArray(body?.conflicts) ? body.conflicts : [],
  });
}

export function toNetworkError(cause: unknown): ApiError {
  if (cause instanceof ApiError) return cause;
  return new ApiError({
    status: 0,
    code: 'NETWORK_ERROR',
    message: '서버에 연결할 수 없습니다. 네트워크 상태를 확인해 주세요.',
    cause,
  });
}

export function fieldErrorMap(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError)) return {};
  return Object.fromEntries(error.fieldErrors.map((item) => [item.field, item.message]));
}
