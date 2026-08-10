import type { PropsWithChildren } from 'react';
import { BrandLogo } from '../common/BrandLogo';
import { SettingsIcon } from '../common/Icons';

interface AppLayoutProps extends PropsWithChildren {
  email: string;
  mainClassName?: string;
  onOpenSettings: () => void;
  onLogout: () => void;
  logoutPending?: boolean;
}

export function AppLayout({
  children,
  email,
  mainClassName,
  onOpenSettings,
  onLogout,
  logoutPending,
}: AppLayoutProps) {
  return (
    <div className="app-shell">
      <header className="app-header">
        <a className="brand" href="/" aria-label="CalTalk 홈">
          <BrandLogo compact />
          <span className="brand-wordmark">
            Cal<strong>Talk</strong>
          </span>
        </a>
        <nav className="header-actions" aria-label="사용자 메뉴">
          <span className="header-email" title={email}>
            {email}
          </span>
          <button className="header-button" type="button" onClick={onOpenSettings}>
            <SettingsIcon />
            <span>설정</span>
          </button>
          <button
            className="header-button logout-button"
            type="button"
            disabled={logoutPending}
            onClick={onLogout}
          >
            {logoutPending ? '로그아웃 중…' : '로그아웃'}
          </button>
        </nav>
      </header>
      <main className={`app-main${mainClassName ? ` ${mainClassName}` : ''}`}>{children}</main>
    </div>
  );
}
