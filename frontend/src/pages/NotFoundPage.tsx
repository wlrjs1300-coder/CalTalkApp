import { Link } from 'react-router';

export function NotFoundPage() {
  return (
    <main className="centered-page">
      <section className="card status-card">
        <p className="eyebrow">404</p>
        <h1>페이지를 찾을 수 없습니다</h1>
        <p>주소를 확인하거나 시작 화면으로 돌아가 주세요.</p>
        <Link className="button-link" to="/">
          홈으로 이동
        </Link>
      </section>
    </main>
  );
}
