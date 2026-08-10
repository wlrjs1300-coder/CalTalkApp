import { apiRequest } from './client';

interface PushConfig { available: boolean; publicKey: string }
interface SerializedSubscription { endpoint: string; keys: { p256dh: string; auth: string } }

export const getPushConfig = () => apiRequest<PushConfig>('/api/v1/push/config');
export const savePushSubscription = (body: SerializedSubscription) => apiRequest<void>('/api/v1/push/subscriptions', { method: 'POST', body, csrf: true });
export const removePushSubscription = (body: SerializedSubscription) => apiRequest<void>('/api/v1/push/subscriptions', { method: 'DELETE', body, csrf: true });
export const sendTestPush = () => apiRequest<{ sent: number; failed: number; error: string | null }>('/api/v1/push/test', { method: 'POST', csrf: true });

function decodeKey(value: string) {
  const padded = value.padEnd(value.length + (4 - value.length % 4) % 4, '=');
  const bytes = atob(padded.replace(/-/g, '+').replace(/_/g, '/'));
  return Uint8Array.from(bytes, (character) => character.charCodeAt(0));
}

function serialize(subscription: PushSubscription): SerializedSubscription {
  const json = subscription.toJSON();
  if (!json.endpoint || !json.keys?.p256dh || !json.keys.auth) throw new Error('푸시 구독 정보를 확인할 수 없습니다.');
  return { endpoint: json.endpoint, keys: { p256dh: json.keys.p256dh, auth: json.keys.auth } };
}

export async function subscribeCurrentDevice() {
  if (!('serviceWorker' in navigator) || !('PushManager' in window) || !('Notification' in window)) throw new Error('이 기기에서는 PWA 알림을 지원하지 않습니다.');
  const config = await getPushConfig();
  if (!config.available || !config.publicKey) throw new Error('서버 알림 키가 아직 설정되지 않았습니다.');
  if (await Notification.requestPermission() !== 'granted') throw new Error('기기 설정에서 CalTalk 알림을 허용해 주세요.');
  const registration = await navigator.serviceWorker.ready;
  const existing = await registration.pushManager.getSubscription();
  const subscription = existing ?? await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: decodeKey(config.publicKey) });
  await savePushSubscription(serialize(subscription));
}

export async function showDeviceTestNotification() {
  if (!('serviceWorker' in navigator) || !('Notification' in window)) throw new Error('이 기기에서는 알림을 지원하지 않습니다.');
  if (Notification.permission !== 'granted') throw new Error('기기 설정에서 CalTalk 알림을 허용해 주세요.');
  const registration = await navigator.serviceWorker.ready;
  // A freshly activated worker can receive pushes and display notifications
  // before the current page receives its controller. Requiring controller here
  // incorrectly forces a refresh even though the registration is ready.
  if (!registration.active) {
    throw new Error('알림 서비스가 활성화되는 중입니다. 페이지를 한 번 새로고침해 주세요.');
  }
  await registration.showNotification('CalTalk 기기 알림 테스트', {
    body: '브라우저와 운영체제의 알림 표시가 정상이에요.',
    icon: '/caltalk-logo.png',
    badge: '/caltalk-logo.png',
    tag: `caltalk-device-test-${Date.now()}`,
  });
}

export async function unsubscribeCurrentDevice() {
  const registration = await navigator.serviceWorker.ready;
  const subscription = await registration.pushManager.getSubscription();
  if (!subscription) return;
  await removePushSubscription(serialize(subscription));
  await subscription.unsubscribe();
}
