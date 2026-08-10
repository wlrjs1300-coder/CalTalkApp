import { Link } from 'react-router';
import { BrandLogo } from '../components/common/BrandLogo';

export function WelcomePage() {
  return (
    <main className="brand-welcome">
      <header className="brand-welcome-header">
        <div className="brand-welcome-wordmark" aria-label="CalTalk">
          <BrandLogo />
          <span>CalTalk</span>
        </div>
      </header>

      <section className="brand-welcome-hero">
        <div className="brand-welcome-emblem" aria-hidden="true">
          <BrandLogo />
        </div>
        <p className="brand-welcome-kicker">내 일정을 가장 가까운 곳에서</p>
        <h1>
          카톡으로 간편하게
          <br />
          일정을 관리하세요
        </h1>
        <p className="brand-welcome-description">확인하고, 관리하고, 필요한 순간 안내받으세요.</p>

        <div className="brand-welcome-actions">
          <Link className="brand-welcome-primary" to="/login">
            CalTalk 시작하기
          </Link>
        </div>
      </section>

      <p className="brand-welcome-footer">오늘의 일정이 조금 더 가벼워집니다.</p>
    </main>
  );
}
