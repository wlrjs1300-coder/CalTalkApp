export interface FieldError {
  field: string;
  code: string;
  message: string;
}

export interface ConflictingSchedule {
  id: number;
  title: string;
  startAt: string;
  endAt: string;
  location: string | null;
}

export interface ApiErrorBody {
  timestamp?: string;
  status?: number;
  code?: string;
  message?: string;
  fieldErrors?: FieldError[];
  confirmationId?: number;
  conflicts?: ConflictingSchedule[];
}

export interface SignupRequest {
  email: string;
  password: string;
  passwordConfirmation: string;
}

export interface SignupResponse {
  email: string;
  timezone: string;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  email: string;
  timezone: string;
}

export interface CurrentUser {
  email: string;
  timezone: string;
  createdAt: string;
}
