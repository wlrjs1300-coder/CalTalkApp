import { apiRequest } from './client';
import type { LoginRequest, LoginResponse, SignupRequest, SignupResponse } from './types';

export function signup(request: SignupRequest, signal?: AbortSignal): Promise<SignupResponse> {
  return apiRequest('/api/v1/auth/signup', {
    method: 'POST',
    body: request,
    csrf: false,
    signal,
  });
}

export function login(request: LoginRequest, signal?: AbortSignal): Promise<LoginResponse> {
  return apiRequest('/api/v1/auth/login', {
    method: 'POST',
    body: request,
    csrf: false,
    signal,
  });
}

export function logout(signal?: AbortSignal): Promise<void> {
  return apiRequest('/api/v1/auth/logout', { method: 'POST', csrf: true, signal });
}
