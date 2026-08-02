import type { PropsWithChildren } from 'react';

export function AppLayout({ children }: PropsWithChildren) {
  return (
    <div className="app-shell">
      <header className="app-header">
        <span className="brand">CalTalk</span>
        <span className="stage-label">Backend connected</span>
      </header>
      <main className="app-main">{children}</main>
    </div>
  );
}
