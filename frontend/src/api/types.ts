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

export interface SocialProvider {
  id: 'kakao' | 'naver' | 'google';
  name: string;
}

export interface CurrentUser {
  email: string;
  timezone: string;
  createdAt: string;
  chatPreferences?: ChatPreferences;
}

export interface ChatPreferences {
  replyStyle: 'CONCISE' | 'STANDARD' | 'ASSISTANT' | 'BUSINESS' | 'FRIENDLY';
  replyDensity: 'ESSENTIAL' | 'STANDARD' | 'DETAILED';
  replyLayout: 'COMPACT' | 'BALANCED' | 'SECTIONED';
  emojiLevel: 'NONE' | 'MINIMAL' | 'BALANCED';
  timeFormat: 'TWELVE_HOUR' | 'TWENTY_FOUR_HOUR';
  confirmCreate: boolean;
  confirmUpdate: boolean;
  defaultDurationMinutes: 30 | 60 | 120;
  defaultReminderMinutes: Array<60 | 1440 | 4320 | 10080>;
  defaultQueryRange: 'TODAY' | 'THREE_DAYS' | 'THIS_WEEK' | 'NEXT_FIVE';
  dailySummaryEnabled: boolean;
  dailySummaryTime: string;
  weeklySummaryEnabled: boolean;
  weeklySummaryDay: number;
  weeklySummaryTime: string;
}

export interface KakaoLinkStatus {
  linked: boolean;
  linkedAt: string | null;
}

export interface KakaoConnectionCode {
  code: string;
  expiresAt: string;
}
