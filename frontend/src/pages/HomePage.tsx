import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { logout } from '../api/auth';
import { ApiError } from '../api/errors';
import { AppLayout } from '../components/layout/AppLayout';
import { currentUserQueryKey, useCurrentUser } from '../features/auth/authQuery';

export function HomePage() {
  const currentUser = useCurrentUser();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const logoutMutation = useMutation({
    mutationFn: () => logout(),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: currentUserQueryKey });
      navigate('/login', { replace: true });
    },
  });

  if (!currentUser.data) return null;

  return (
    <AppLayout>
      <section className="welcome-panel">
        <div>
          <p className="eyebrow">현재 사용자</p>
          <h1>안녕하세요.</h1>
          <p className="user-email">{currentUser.data.email}</p>
        </div>
        <button
          className="secondary-button"
          type="button"
          disabled={logoutMutation.isPending}
          onClick={() => logoutMutation.mutate()}
        >
          {logoutMutation.isPending ? '로그아웃 중…' : '로그아웃'}
        </button>
      </section>

      {logoutMutation.error ? (
        <div className="alert" role="alert">
          {logoutMutation.error instanceof ApiError
            ? logoutMutation.error.message
            : '로그아웃하지 못했습니다.'}
        </div>
      ) : null}

      <section className="dashboard-grid">
        <article className="card profile-card">
          <p className="eyebrow">설정</p>
          <h2>내 시간대</h2>
          <strong>{currentUser.data.timezone}</strong>
          <p className="muted">시간대 변경 화면은 다음 기능 단계에서 연결합니다.</p>
        </article>
        <article className="card schedule-placeholder" aria-disabled="true">
          <p className="eyebrow">다음 단계</p>
          <h2>일정 관리</h2>
          <p>일정 목록과 생성·수정·삭제 화면은 아직 구현되지 않았습니다.</p>
        </article>
      </section>
    </AppLayout>
  );
}
