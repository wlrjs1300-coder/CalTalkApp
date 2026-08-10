import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { AppProviders } from './app/providers/AppProviders';
import { AppRouter } from './app/router/AppRouter';
import './styles/global.css';

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.addEventListener('message', (event) => {
    if (event.data?.type !== 'CALTALK_NAVIGATE' || typeof event.data.url !== 'string') return;
    const target = new URL(event.data.url, window.location.origin);
    if (target.origin === window.location.origin) {
      window.location.assign(`${target.pathname}${target.search}${target.hash}`);
    }
  });
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js?v=4', { updateViaCache: 'none' }).catch(() => undefined);
  });
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppProviders>
      <AppRouter />
    </AppProviders>
  </StrictMode>,
);
