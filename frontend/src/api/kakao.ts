import { apiRequest } from './client';
import type { KakaoConnectionCode, KakaoLinkStatus } from './types';

export function getKakaoLinkStatus(): Promise<KakaoLinkStatus> {
  return apiRequest('/api/v1/kakao/link');
}
export function issueKakaoConnectionCode(): Promise<KakaoConnectionCode> {
  return apiRequest('/api/v1/kakao/link-codes', { method: 'POST', csrf: true });
}
export function revokeKakaoLink(): Promise<void> {
  return apiRequest('/api/v1/kakao/links/revoke', { method: 'POST', csrf: true });
}
