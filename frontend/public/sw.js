self.addEventListener('install', (event) => {
  event.waitUntil(self.skipWaiting().catch(() => undefined));
});

self.addEventListener('activate', (event) => {
  event.waitUntil(caches.keys().then((keys) => Promise.all(keys.map((key) => caches.delete(key))))
    .then(() => self.clients.claim())
    .catch(() => undefined));
});

self.addEventListener('push', (event) => {
  const fallback = { title: 'CalTalk 일정 알림', body: '예정된 일정을 확인해 주세요.', url: '/' };
  let data = fallback;
  try { data = { ...fallback, ...event.data.json() }; } catch { /* use fallback */ }
  event.waitUntil(self.registration.showNotification(data.title, {
    body: data.body,
    icon: '/caltalk-logo.png',
    badge: '/caltalk-logo.png',
    tag: data.tag || 'caltalk-reminder',
    renotify: false,
    data: { url: data.url || '/' },
  }));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const target = new URL(event.notification.data?.url || '/', self.location.origin).href;
  event.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clients) => {
    const existing = clients.find((client) => client.url.startsWith(self.location.origin));
    if (existing) {
      existing.postMessage({ type: 'CALTALK_NAVIGATE', url: target });
      return existing.focus();
    }
    return self.clients.openWindow(target);
  }).catch(() => self.clients.openWindow(target)));
});
